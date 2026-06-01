package com.xastia.test.presentation

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.xastia.test.data.repository.CameraRepositoryImpl
import com.xastia.test.databinding.ActivityMeasureBinding
import com.xastia.test.domain.pulse.PpgProcessor
import com.xastia.test.domain.usecase.StartCameraUseCase
import com.xastia.test.presentation.ext.applyStatusBarTopPadding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Активне вимірювання пульсу.
 *
 * Pipeline:
 *  - CameraX + torch on → стабільне освітлення пальця
 *  - кожен кадр: усереднення R-каналу в центральній ROI → семпл у PpgProcessor
 *  - кожну секунду: показуємо поточну оцінку BPM на UI (поки сигнал не стабілізується,
 *    показуємо "—")
 *  - коли Lottie loading дотікає до кінця (~20 секунд) → фінальний BPM,
 *    перехід на ResultActivity
 */
class MeasureActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMeasureBinding
    private val ppgProcessor = PpgProcessor()
    @Volatile private var isMeasuring = true
    private lateinit var handler: Handler
    private lateinit var cameraRepository: CameraRepositoryImpl

    private val animators = mutableListOf<Animator>()
    private var torchEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMeasureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applyStatusBarTopPadding(extraDp = 12)

        handler = Handler(Looper.getMainLooper())
        cameraRepository = CameraRepositoryImpl(this, Executors.newSingleThreadExecutor())

        CoroutineScope(Dispatchers.Default).launch {
            startCamera()
        }

        startUpdatingBpm()
        startAllAnimations()

        // Lottie loading драйвить тривалість вимірювання.
        // На його завершенні (~20 сек) — фіксуємо BPM і йдемо на результат.
        binding.lottieLoading.apply {
            setMinProgress(0.0f)
            setMaxProgress(1.0f)
            speed = 0.5f
            repeatCount = 0
            playAnimation()
        }

        binding.lottieLoading.addAnimatorUpdateListener { animation ->
            if (animation.animatedFraction == 1f && isMeasuring) {
                isMeasuring = false
                cameraRepository.enableTorch(false)
                val bpm = ppgProcessor.calculateBpm()
                val intent = Intent(this, ResultActivity::class.java).apply {
                    putExtra("BPM", bpm)
                }
                startActivity(intent)
                finish()
            }
        }
    }

    private suspend fun startCamera() {
        val startCameraUseCase = StartCameraUseCase(cameraRepository)
        startCameraUseCase.execute(binding.preview).collect { imageProxy ->
            // Включаємо torch після першого кадру — на цей момент Camera-об'єкт
            // уже точно існує і cameraControl.enableTorch() спрацює.
            if (!torchEnabled) {
                cameraRepository.enableTorch(true)
                torchEnabled = true
            }
            if (isMeasuring) {
                val red = cameraRepository.analyzeRedChannel(imageProxy)
                ppgProcessor.addSample(System.currentTimeMillis(), red)
            }
            imageProxy.close()
        }
    }

    /**
     * Кожну секунду — оновлюємо поточну оцінку BPM у центральному кружку.
     * Поки сигнал не стабілізувався (PpgProcessor повертає 0) — показуємо "—".
     */
    private fun startUpdatingBpm() {
        val updateTask = object : Runnable {
            override fun run() {
                val bpm = ppgProcessor.calculateBpm()
                binding.textView8.text = if (bpm > 0) bpm.toString() else "—"
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(updateTask)
    }

    // ----- Decorative animations (без змін з task #7) -----------------------

    private fun startAllAnimations() {
        startPulseRings()
        startLoadingDots()
        startWaveform()
    }

    private fun startPulseRings() {
        animators += createPulseRingAnimator(binding.pulseRing1, 0L)
        animators += createPulseRingAnimator(binding.pulseRing2, 800L)
    }

    private fun createPulseRingAnimator(view: View, startDelay: Long): ObjectAnimator {
        return ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat("scaleX", 0.85f, 1.6f),
            PropertyValuesHolder.ofFloat("scaleY", 0.85f, 1.6f),
            PropertyValuesHolder.ofFloat("alpha", 0.55f, 0f)
        ).apply {
            duration = 1600
            this.startDelay = startDelay
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun startLoadingDots() {
        animators += createDotAnimator(binding.dot1, 0L)
        animators += createDotAnimator(binding.dot2, 250L)
        animators += createDotAnimator(binding.dot3, 500L)
    }

    private fun createDotAnimator(view: View, startDelay: Long): ObjectAnimator {
        return ObjectAnimator.ofFloat(view, "alpha", 0.3f, 1f, 0.3f).apply {
            duration = 1400
            this.startDelay = startDelay
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun startWaveform() {
        val barConfigs = listOf(
            Triple(binding.bar1, 8, 22),
            Triple(binding.bar2, 22, 8),
            Triple(binding.bar3, 12, 26),
            Triple(binding.bar4, 28, 10),
            Triple(binding.bar5, 14, 24),
            Triple(binding.bar6, 24, 12),
            Triple(binding.bar7, 10, 22)
        )

        barConfigs.forEachIndexed { index, (view, fromDp, toDp) ->
            val fromPx = dpToPx(fromDp)
            val toPx = dpToPx(toDp)
            val animator = ValueAnimator.ofInt(fromPx, toPx).apply {
                duration = 1200
                startDelay = (index * 80).toLong()
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { animation ->
                    val params = view.layoutParams as LinearLayout.LayoutParams
                    params.height = animation.animatedValue as Int
                    view.layoutParams = params
                }
                start()
            }
            animators += animator
        }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        super.onDestroy()
        cameraRepository.enableTorch(false)
        handler.removeCallbacksAndMessages(null)
        animators.forEach { it.cancel() }
        animators.clear()
    }
}
