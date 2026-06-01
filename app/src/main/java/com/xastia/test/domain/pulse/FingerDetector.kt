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
     * Повертає true, якщо канали відповідають сигнатурі «палець з torch на лінзі».
     *
     * Критерії:
     *  - R абсолютно високий (> minRed) — щоб відсіяти темні сцени
     *  - R/G > 1.5 — червоне домінує над зеленим
     *  - R/B > 1.5 — червоне домінує над синім
     */
    fun isFinger(channels: RgbChannels): Boolean {
        if (channels.r < MIN_RED) return false
        val g = channels.g.coerceAtLeast(1.0)
        val b = channels.b.coerceAtLeast(1.0)
        return channels.r / g >= MIN_R_TO_G_RATIO &&
                channels.r / b >= MIN_R_TO_B_RATIO
    }

    companion object {
        const val MIN_RED = 120.0
        const val MIN_R_TO_G_RATIO = 1.5
        const val MIN_R_TO_B_RATIO = 1.5
    }
}
