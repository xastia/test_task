package com.xastia.test.presentation

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import com.xastia.test.databinding.ActivityMainBinding

/**
 * Splash екран.
 * - Серце статичне (як на референс-мокапі)
 * - Три крапочки анімовано — спалахують послідовно (loading-dots ефект)
 * - lottieLoading у layout невидимий, але драйвить навігацію
 *   до OnboardingActivity коли його анімація завершується
 */
class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private val animators = mutableListOf<Animator>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }

        startSplashDotsAnimation()

        binding.lottieLoading.apply {
            setMinProgress(0.0f)
            setMaxProgress(1.0f)
            repeatCount = 0
            playAnimation()
        }
        binding.lottieLoading.addAnimatorUpdateListener { animation ->
            if (animation.animatedFraction == 1f) {
                val intent = Intent(this, OnboardingActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    /**
     * Три крапочки спалахують послідовно — той самий "loading dots" ефект,
     * що і на екрані вимірювання. Інтервал між точками 250мс, повний цикл 1400мс.
     */
    private fun startSplashDotsAnimation() {
        animators += createDotAnimator(binding.splashDot1, 0L)
        animators += createDotAnimator(binding.splashDot2, 250L)
        animators += createDotAnimator(binding.splashDot3, 500L)
    }

    private fun createDotAnimator(view: View, startDelay: Long): ObjectAnimator {
        return ObjectAnimator.ofFloat(view, "alpha", 0.25f, 1f, 0.25f).apply {
            duration = 1400
            this.startDelay = startDelay
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        animators.forEach { it.cancel() }
        animators.clear()
    }
}
