package com.xastia.test.domain.pulse

import kotlin.math.PI
import kotlin.math.sqrt

/**
 * Обчислення частоти серцевих скорочень (ЧСС) із сирого PPG-сигналу,
 * отриманого з камери смартфона (середнє значення R-каналу по кадру).
 *
 * Pipeline:
 *   1) збір (timestamp, R-value) семплів у реальному часі
 *   2) detrend — віднімання ковзного середнього (прибирає baseline drift)
 *   3) bandpass IIR-фільтр 0.7–3.5 Hz (фізіологічний діапазон ЧСС 42–210 BPM)
 *   4) peak detection із обмеженням мінімальної відстані між піками
 *   5) обчислення BPM на основі IBI (Inter-Beat Intervals) — медіана для стійкості
 *      до випадкових промахів детектора
 *
 * Цей клас не залежить від Android-фреймворку — pure Kotlin, можна юніт-тестити.
 */
class PpgProcessor {

    data class Sample(val timestampMs: Long, val redValue: Double)

    private val samples = mutableListOf<Sample>()

    fun addSample(timestampMs: Long, redValue: Double) {
        samples.add(Sample(timestampMs, redValue))
    }

    fun clear() {
        samples.clear()
    }

    fun size(): Int = samples.size

    /**
     * Повертає поточну попередню оцінку BPM на основі вже зібраних семплів.
     * Якщо семплів замало або сигнал зашумлений — повертає 0.
     */
    fun calculateBpm(): Int {
        // Потрібно як мінімум 5 секунд даних і 30 семплів
        if (samples.size < 30) return 0

        val firstTs = samples.first().timestampMs
        val lastTs = samples.last().timestampMs
        val durationSec = (lastTs - firstTs) / 1000.0
        if (durationSec < MIN_DURATION_SEC) return 0

        // Оцінка частоти дискретизації (фактичної — CameraX зазвичай дає ~25-30 fps)
        val fs = samples.size / durationSec

        val raw = samples.map { it.redValue }

        // 1) Detrend — віднімаємо ковзне середнє з вікном ≈1 секунда.
        //    Це прибирає baseline (повільні зміни освітлення, тиску пальця).
        val windowSize = fs.toInt().coerceAtLeast(5)
        val detrended = detrend(raw, windowSize)

        // 1.5) Quality check — амплітуда пульсації має бути достатньою.
        //      Якщо std dev детрендованого сигналу замала — сигнал плоский
        //      (палець занадто слабко притиснутий, або кровопостачання слабке,
        //      або там взагалі не палець). У такому випадку число буде «з пальця».
        //      Краще повернути 0 (UI покаже "—"), ніж обманути користувача.
        if (stdDev(detrended) < MIN_STD_DEV) return 0

        // 2) Bandpass-фільтр 0.7–3.5 Hz.
        //    0.7 Hz = 42 BPM (нижня межа фізіологічно правдоподібного пульсу)
        //    3.5 Hz = 210 BPM (верхня межа)
        val filtered = bandpass(detrended, fs, LOW_HZ, HIGH_HZ)

        // 3) Peak detection. Мінімальна відстань між піками — 0.35 секунди
        //    (відповідає максимуму ~170 BPM, з невеликим запасом).
        val minPeakDistance = (fs * MIN_PEAK_DISTANCE_SEC).toInt().coerceAtLeast(1)
        val peakIndices = findPeaks(filtered, minPeakDistance)

        if (peakIndices.size < 3) return 0

        // 4) Обчислюємо BPM з IBI. Медіана стійкіша за середнє до випадкових
        //    помилкових/пропущених піків.
        val peakTimes = peakIndices.map { samples[it].timestampMs }
        val ibis = peakTimes.zipWithNext { a, b -> (b - a).toDouble() }
        val medianIbi = median(ibis)
        if (medianIbi <= 0.0) return 0

        val bpm = 60_000.0 / medianIbi
        return bpm.toInt().coerceIn(MIN_BPM, MAX_BPM)
    }

    // ----- Allgorithm helpers ------------------------------------------------

    /**
     * Віднімання ковзного середнього (centered moving average).
     * Залишає лише пульсації навколо локального baseline.
     */
    private fun detrend(values: List<Double>, windowSize: Int): List<Double> {
        val half = windowSize / 2
        return values.mapIndexed { i, v ->
            val from = (i - half).coerceAtLeast(0)
            val to = (i + half + 1).coerceAtMost(values.size)
            val baseline = values.subList(from, to).average()
            v - baseline
        }
    }

    /**
     * Каскад single-pole IIR: high-pass (зріз lowHz) → low-pass (зріз highHz).
     * Простіша за повний Butterworth другого порядку, але дає достатню
     * вибірковість для PPG (де сигнал і так досить чистий після detrend).
     *
     * Високочастотний фільтр прибирає залишковий baseline,
     * низькочастотний — шум від мерехтіння джерела світла, тремтіння рук тощо.
     */
    private fun bandpass(values: List<Double>, fs: Double, lowHz: Double, highHz: Double): DoubleArray {
        val dt = 1.0 / fs
        val rcHigh = 1.0 / (2.0 * PI * lowHz)
        val alphaHigh = rcHigh / (rcHigh + dt)
        val rcLow = 1.0 / (2.0 * PI * highHz)
        val alphaLow = dt / (rcLow + dt)

        val high = DoubleArray(values.size)
        var prevIn = values[0]
        var prevOut = 0.0
        for (i in values.indices) {
            val x = values[i]
            val y = if (i == 0) 0.0 else alphaHigh * (prevOut + x - prevIn)
            high[i] = y
            prevIn = x
            prevOut = y
        }

        val out = DoubleArray(values.size)
        var lpPrev = high[0]
        for (i in high.indices) {
            val x = high[i]
            val y = if (i == 0) x else lpPrev + alphaLow * (x - lpPrev)
            out[i] = y
            lpPrev = y
        }
        return out
    }

    /**
     * Локальні максимуми із enforced мінімальною відстанню.
     * Якщо два піки занадто близько — лишаємо лише вищий.
     */
    private fun findPeaks(values: DoubleArray, minDistance: Int): List<Int> {
        if (values.size < 3) return emptyList()
        val peaks = mutableListOf<Int>()
        for (i in 1 until values.size - 1) {
            if (values[i] > values[i - 1] && values[i] > values[i + 1]) {
                if (peaks.isEmpty() || i - peaks.last() >= minDistance) {
                    peaks.add(i)
                } else if (values[i] > values[peaks.last()]) {
                    // Сусідній пік вищий — заміняємо
                    peaks[peaks.size - 1] = i
                }
            }
        }
        return peaks
    }

    private fun median(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val n = sorted.size
        return if (n % 2 == 1) sorted[n / 2]
        else (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
    }

    /**
     * Стандартне відхилення вибірки. Показник «жвавості» сигналу:
     * для чистого PPG з detrend ≈ 0.5–3 (видимі пульсації);
     * для шуму / плоского сигналу ≈ 0.0–0.2.
     */
    private fun stdDev(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        var sumSq = 0.0
        for (v in values) {
            val d = v - mean
            sumSq += d * d
        }
        return sqrt(sumSq / values.size)
    }

    companion object {
        private const val LOW_HZ = 0.7              // 42 BPM
        private const val HIGH_HZ = 3.5             // 210 BPM
        private const val MIN_PEAK_DISTANCE_SEC = 0.35
        private const val MIN_DURATION_SEC = 5.0
        private const val MIN_BPM = 40
        private const val MAX_BPM = 200
        // Мінімальна амплітуда пульсації — нижче неї сигнал визнаємо
        // «непридатним» і повертаємо 0. Підбирається емпірично:
        // 0.3 пропускає слабкі реальні PPG, але блокує плоский шум.
        private const val MIN_STD_DEV = 0.3
    }
}
