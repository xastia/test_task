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
import com.xastia.test.databinding.ActivityHomepage2Binding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Homepage2 : AppCompatActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var binding: ActivityHomepage2Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomepage2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        startCamera()

        cameraExecutor = Executors.newSingleThreadExecutor()

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

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
                .also {
                    it.setSurfaceProvider(findViewById<PreviewView>(R.id.preview).surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                        processImage(imageProxy)
                        imageProxy.close()
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImage(imageProxy: ImageProxy) {
        val buffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        // Обчислення середньої яскравості
        val averageBrightness = bytes.map { it.toInt() and 0xFF }.average()

        // Логіка перевірки: якщо яскравість нижча за поріг, запустити нову активність
        if (averageBrightness < 20) { // Поріг можна налаштувати
            runOnUiThread {
                //Toast.makeText(this, "Finger is on Camera", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, Homepage3::class.java)
                startActivity(intent)
                finish()
            }
        }
    }




}