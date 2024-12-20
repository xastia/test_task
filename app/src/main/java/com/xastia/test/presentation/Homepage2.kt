package com.xastia.test.presentation

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.xastia.test.R
import com.xastia.test.data.repository.CameraRepositoryImpl
import com.xastia.test.databinding.ActivityHomepage2Binding
import com.xastia.test.domain.usecase.StartCameraUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Homepage2 : AppCompatActivity() {
    private lateinit var binding: ActivityHomepage2Binding
    private lateinit var cameraRepository: CameraRepositoryImpl
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomepage2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraRepository = CameraRepositoryImpl(this, cameraExecutor)
        val startCameraUseCase = StartCameraUseCase(cameraRepository)

        CoroutineScope(Dispatchers.Default).launch {
            startCameraUseCase.execute(binding.preview).collect { imageProxy ->
                val brightness = cameraRepository.analyzeImage(imageProxy)
                if (brightness < 20) {
                    withContext(Dispatchers.Main) {
                        val intent = Intent(this@Homepage2, Homepage3::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
                imageProxy.close()
            }
        }


        binding.imageButton.setOnClickListener {
            val intent = Intent(this, Homepage1::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
