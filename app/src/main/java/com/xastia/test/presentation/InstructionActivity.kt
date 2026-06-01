package com.xastia.test.presentation

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.xastia.test.data.repository.CameraRepositoryImpl
import com.xastia.test.databinding.ActivityInstructionBinding
import com.xastia.test.domain.pulse.FingerDetector
import com.xastia.test.domain.usecase.StartCameraUseCase
import com.xastia.test.presentation.ext.applyStatusBarTopPadding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Очікування того, що користувач закриє об'єктив камери пальцем.
 *
 * Pipeline:
 *  1) Прогрів камери (warmupFrames перших кадрів ігноруємо — поки автоекспозиція
 *     і автобаланс білого не стабілізувались, значення кольорів ненадійні).
 *  2) Вмикаємо torch.
 *  3) Стежимо за середнім значенням R-каналу в ROI. Коли палець ЩІЛЬНО на лінзі
 *     з ввімкненим torch — світло проходить через тканину і назад розсіюється
 *     переважно червоним (гемоглобін поглинає зелено-блакитне).
 *     Тому R > fingerDetectedRedThreshold = "палець на лінзі".
 *  4) Палець вважаємо «прикладеним» лише коли requiredLowFrames підряд мають
 *     R вище порогу (~0.5 секунди стабільно червоного зображення).
 *  5) Переходимо до MeasureActivity. Прапор hasNavigated не дає запустити
 *     перехід двічі (race condition при швидкому колбеку).
 *
 * Чому R-канал, а НЕ Y (luma): з torch+пальцем камера бачить НЕ "темно",
 * а "темно-червоно". Y у такому випадку 50-90 (далеко не <25). R у цьому
 * ж випадку 150-220 — це чітка сигнатура яку легко виявити.
 */
class InstructionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInstructionBinding
    private lateinit var cameraRepository: CameraRepositoryImpl
    private lateinit var cameraExecutor: ExecutorService
    private val fingerDetector = FingerDetector()

    // Скільки перших кадрів пропустити (поки камера встановить експозицію).
    // При ~25-30 fps це ~1 секунда.
    private val warmupFrames = 30

    // Скільки кадрів підряд має триматися «палець виявлений» щоб не плутати з
    // випадковим спалахом червоного у кадрі. ~0.5 секунди при 30 fps.
    private val requiredLowFrames = 15

    private val hasNavigated = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInstructionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applyStatusBarTopPadding(extraDp = 12)

        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraRepository = CameraRepositoryImpl(this, cameraExecutor)
        val startCameraUseCase = StartCameraUseCase(cameraRepository)

        var torchEnabled = false
        var framesSeen = 0
        var consecutiveLowFrames = 0

        CoroutineScope(Dispatchers.Default).launch {
            startCameraUseCase.execute(binding.preview).collect { imageProxy ->
                try {
                    if (!torchEnabled) {
                        cameraRepository.enableTorch(true)
                        torchEnabled = true
                    }

                    framesSeen++
                    // Прогрів — поки камера встановить експозицію
                    if (framesSeen < warmupFrames) {
                        return@collect
                    }

                    val rgb = cameraRepository.analyzeRgb(imageProxy)

                    if (fingerDetector.isFinger(rgb)) {
                        consecutiveLowFrames++
                        if (consecutiveLowFrames >= requiredLowFrames &&
                            hasNavigated.compareAndSet(false, true)
                        ) {
                            withContext(Dispatchers.Main) {
                                val intent = Intent(this@InstructionActivity, MeasureActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                        }
                    } else {
                        // Як тільки R-канал впав — скидаємо лічильник, щоб не
                        // зарахувати випадковий проблиск червоного у кадрі
                        consecutiveLowFrames = 0
                    }
                } finally {
                    imageProxy.close()
                }
            }
        }

        binding.imageButton.setOnClickListener {
            cameraRepository.enableTorch(false)
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraRepository.enableTorch(false)
        cameraExecutor.shutdown()
    }
}
