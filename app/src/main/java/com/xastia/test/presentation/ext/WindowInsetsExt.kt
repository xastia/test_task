package com.xastia.test.presentation.ext

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Встановлює верхнє padding view рівним висоті статус-бара плюс додаткові [extraDp].
 * Це дозволяє не хардкодити paddingTop у XML — кожен пристрій (notch, без notch,
 * Pixel, Samsung, Xiaomi) отримує правильний відступ автоматично.
 *
 * Викликати з onCreate() Activity після setContentView().
 */
fun View.applyStatusBarTopPadding(extraDp: Int = 12) {
    val density = resources.displayMetrics.density
    val extraPx = (extraDp * density).toInt()
    val originalPaddingBottom = paddingBottom
    val originalPaddingLeft = paddingLeft
    val originalPaddingRight = paddingRight

    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.setPadding(
            originalPaddingLeft,
            sysBars.top + extraPx,
            originalPaddingRight,
            originalPaddingBottom
        )
        insets
    }
    // Запит на доставку insets — інакше listener може ніколи не викликатись
    // якщо view вже attached
    if (isAttachedToWindow) {
        ViewCompat.requestApplyInsets(this)
    }
}
