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

/**
 * Очікування того, що користувач закриє об'єктив камери пальцем.
 * Ліхтарик вмикається після першого кадру, після чого ми моніторимо
 * Y-канал — низька яскравість означає, що палець закрив об'єктив,
 * можна переходити до вимірювання.
 */
class InstructionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInstructionBinding
    private lateinit var cameraRepository: CameraRepositoryImpl
    private lateinit var cameraExecutor: ExecutorService

    // Поріг яскравості, нижче якого вважаємо що палець закрив камеру.
    // 20 = майже чорна картинка, з ввімкненим torch це означає прилягання шкіри.
    private val fingerDetectedThreshold = 20.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInstructionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraRepository = CameraRepositoryImpl(this, cameraExecutor)
        val startCameraUseCase = StartCameraUseCase(cameraRepository)

        // Локальний прапорець щоб увімкнути torch лише раз (після першого кадру —
        // на цей момент CameraX точно встиг створити Camera-об'єкт)
        var torchEnabled = false

        CoroutineScope(Dispatchers.Default).launch {
            startCameraUseCase.execute(binding.preview).collect { imageProxy ->
                if (!torchEnabled) {
                    cameraRepository.enableTorch(true)
                    torchEnabled = true
                }
                val brightness = cameraRepository.analyzeImage(imageProxy)
                if (brightness < fingerDetectedThreshold) {
                    withContext(Dispatchers.Main) {
                        // Torch лишаємо ввімкненим — MeasureActivity його перевключить,
                        // але між Activity-переходами він на мить вимикається (CameraX
                        // unbind → re-bind). Це нормально.
                        val intent = Intent(this@InstructionActivity, MeasureActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
                imageProxy.close()
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
