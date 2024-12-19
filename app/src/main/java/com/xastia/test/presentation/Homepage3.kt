package com.xastia.test.presentation

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.xastia.test.R
import com.xastia.test.databinding.ActivityHomepage3Binding


class Homepage3 : AppCompatActivity() {
    private lateinit var binding: ActivityHomepage3Binding
    private val brightnessData = mutableListOf<Double>()
    private var isMeasuring = true
    private lateinit var handler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomepage3Binding.inflate(layoutInflater)
        setContentView(binding.root)

        handler = Handler(Looper.getMainLooper())
        startCamera()
        startUpdatingBPM()

        binding.lottieLoading.apply {
            setMinProgress(0.0f)
            setMaxProgress(1.0f)
            speed = 0.5f
            repeatCount = 0
            playAnimation()
        }

        binding.lottieLoading.addAnimatorUpdateListener { animation ->
            if (animation.animatedFraction == 1f) {
                val bpm = calculatePulse()
                val intent = Intent(this, ResultActivity::class.java).apply {
                    putExtra("BPM", bpm) // Передача результату пульсу
                }
                startActivity(intent)
                finish()
            }
        }

        binding.lottieHeart.apply {
            setMinProgress(0.0f)
            setMaxProgress(1.0f)
            repeatCount = 20
            playAnimation()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
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
        if (!isMeasuring) return

        val buffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        // Обчислення середньої яскравості
        val averageBrightness = bytes.map { it.toInt() and 0xFF }.average()
        brightnessData.add(averageBrightness)

        // Обмеження розміру списку даних
        if (brightnessData.size > 150) { // Приблизно 5 секунд даних при 30 кадрах за секунду
            isMeasuring = false
        }
    }

    private fun calculatePulse(): Int {
        if (brightnessData.isEmpty()) return 0

        // Згладжування даних
        val smoothedData = brightnessData.windowed(5, 1) { it.average() }
        val peaks = smoothedData.zipWithNext { prev, curr -> curr > prev }.count { it }

        // Розрахунок пульсу
        return (peaks / 5.0 * 60).toInt() // 5 секунд -> 12 разів множимо на 60 для BPM
    }

    private fun startUpdatingBPM() {
        val updateTask = object : Runnable {
            override fun run() {
                val bpm = calculatePulse()
                binding.textView8.text = bpm.toString()
                handler.postDelayed(this, 1000) // Оновлювати кожну секунду
            }
        }

        handler.post(updateTask)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}

