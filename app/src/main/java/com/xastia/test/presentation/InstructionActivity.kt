package com.xastia.test.presentation

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.xastia.test.data.repository.CameraRepositoryImpl
import com.xastia.test.databinding.ActivityInstructionBinding
import com.xastia.test.domain.usecase.StartCameraUseCase
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
 *  1) Прогрів камери (WARMUP_FRAMES перших кадрів ігноруємо — поки автоекспозиція
 *     і автобаланс білого не стабілізувались, значення яскравості ненадійні).
 *  2) Вмикаємо torch.
 *  3) Стежимо за середньою яскравістю в ROI. Палець вважаємо «прикладеним»
 *     лише коли REQUIRED_LOW_FRAMES підряд мають brightness нижче порогу
 *     (~0.5 секунди стабільно низького Y-каналу).
 *  4) Переходимо до MeasureActivity. Перевіряємо hasNavigated щоб не запустити
 *     перехід двічі.
 */
class InstructionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInstructionBinding
    private lateinit var cameraRepository: CameraRepositoryImpl
    private lateinit var cameraExecutor: ExecutorService

    // Поріг яскравості (0-255), нижче якого вважаємо палець прикладеним.
    // ~25 — з ввімкненим torch і пальцем щільно на лінзі камера бачить
    // переважно темно-червоний/чорний, Y-канал у такому випадку < 25.
    private val fingerDetectedThreshold = 25.0

    // Скільки перших кадрів пропустити (поки камера встановить експозицію).
    // При ~25-30 fps це ~1 секунда.
    private val warmupFrames = 30

    // Скільки кадрів підряд має триматися низька яскравість, щоб упевнитись
    // що палець справді прикладений, а не випадково потрапив у кадр.
    // ~0.5 секунди при 30 fps.
    private val requiredLowFrames = 15

    private val hasNavigated = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInstructionBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

                    val brightness = cameraRepository.analyzeImage(imageProxy)

                    if (brightness < fingerDetectedThreshold) {
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
                        // Як тільки яскравість повернулась — скидаємо лічильник,
                        // щоб не зарахувати випадковий короткочасний "темний кадр"
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
