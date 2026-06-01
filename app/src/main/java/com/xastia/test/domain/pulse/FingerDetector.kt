package com.xastia.test.domain.pulse

/**
 * Контейнер для усереднених значень трьох колірних каналів кадру.
 * Кожне значення у діапазоні 0..255.
 */
data class RgbChannels(val r: Double, val g: Double, val b: Double)

/**
 * Детектор «палець на лінзі» по фізичній сигнатурі PPG-сигналу.
 *
 * Логіка:
 *   Коли палець щільно прикладений до камери з ввімкненим torch, світло
 *   проходить крізь тканину. Гемоглобін КРОВІ ПОГЛИНАЄ зелено-блакитне світло
 *   значно сильніше за червоне (закон Бера-Ламберта — у Розділі 1.3 диплому).
 *   Тому до сенсора повертається переважно червоне → R набагато більше за G і B.
 *
 * Цей тест точніший за просте «R > порогу», бо звичайні червоні об'єкти
 * (тепла лампа, абажур, шкіра обличчя під torch) мають всі три канали відносно
 * високими — у них R не настільки сильно домінує над G і B.
 */
class FingerDetector {

    /**
     * Суворий тест — для першої детекції (InstructionActivity).
     * Має чітко відрізнити палець від випадкових червоних об'єктів.
     */
    fun isFingerStrict(channels: RgbChannels): Boolean {
        if (channels.r < STRICT_MIN_RED) return false
        val g = channels.g.coerceAtLeast(1.0)
        val b = channels.b.coerceAtLeast(1.0)
        return channels.r / g >= STRICT_R_TO_G &&
                channels.r / b >= STRICT_R_TO_B
    }

    /**
     * М'який тест — для continuous-перевірки під час вимірювання (MeasureActivity),
     * коли палець уже підтверджений у InstructionActivity.
     * Менш суворі пороги — hysteresis запобігає миготінню overlay коли ratio
     * коливається біля строгого порогу через automatic-exposure.
     */
    fun isFingerLenient(channels: RgbChannels): Boolean {
        if (channels.r < LENIENT_MIN_RED) return false
        val g = channels.g.coerceAtLeast(1.0)
        val b = channels.b.coerceAtLeast(1.0)
        return channels.r / g >= LENIENT_R_TO_G &&
                channels.r / b >= LENIENT_R_TO_B
    }

    /** Збережено для зворотної сумісності — еквівалент isFingerStrict. */
    fun isFinger(channels: RgbChannels): Boolean = isFingerStrict(channels)

    companion object {
        const val STRICT_MIN_RED = 110.0
        const val STRICT_R_TO_G = 1.4
        const val STRICT_R_TO_B = 1.4

        const val LENIENT_MIN_RED = 90.0
        const val LENIENT_R_TO_G = 1.15
        const val LENIENT_R_TO_B = 1.15
    }
}
