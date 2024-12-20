package com.xastia.test.presentation

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.xastia.test.data.repository.CameraRepositoryImpl
import com.xastia.test.databinding.ActivityHomepage3Binding
import com.xastia.test.domain.usecase.StartCameraUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors


class Homepage3 : AppCompatActivity() {
    private lateinit var binding: ActivityHomepage3Binding
    private val brightnessData = mutableListOf<Double>()
    private var isMeasuring = true
    private lateinit var handler: Handler
    private lateinit var cameraRepository: CameraRepositoryImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomepage3Binding.inflate(layoutInflater)
        setContentView(binding.root)

        handler = Handler(Looper.getMainLooper())
        cameraRepository = CameraRepositoryImpl(this, Executors.newSingleThreadExecutor())

        CoroutineScope(Dispatchers.Default).launch {
            startCamera()
        }


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
                    putExtra("BPM", bpm)
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

    private suspend fun startCamera() {
        val startCameraUseCase = StartCameraUseCase(cameraRepository)
        startCameraUseCase.execute(binding.preview).collect { imageProxy ->
            if (isMeasuring) {
                val brightness = cameraRepository.analyzeImage(imageProxy)
                brightnessData.add(brightness)

                if (brightnessData.size > 150) {
                    isMeasuring = false
                }
            }
            imageProxy.close()
        }
    }

    private fun calculatePulse(): Int {
        if (brightnessData.isEmpty()) return 0

        val smoothedData = brightnessData.windowed(5, 1) { it.average() }
        val peaks = smoothedData.zipWithNext { prev, curr -> curr > prev }.count { it }

        return (peaks / 5.0 * 60).toInt()
    }

    private fun startUpdatingBPM() {
        val updateTask = object : Runnable {
            override fun run() {
                val bpm = calculatePulse()
                binding.textView8.text = bpm.toString()
                handler.postDelayed(this, 1000)
            }
        }

        handler.post(updateTask)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}


