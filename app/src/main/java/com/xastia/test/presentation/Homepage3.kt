package com.xastia.test.presentation

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.xastia.test.R
import com.xastia.test.databinding.ActivityHomepage3Binding


class Homepage3 : AppCompatActivity() {
    private lateinit var binding: ActivityHomepage3Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomepage3Binding.inflate(layoutInflater)
        setContentView(binding.root)

        startCamera()

        binding.lottieLoading.apply {
            setMinProgress(0.0f)
            setMaxProgress(1.0f)
            repeatCount = 0
            playAnimation()
        }

        binding.lottieLoading.addAnimatorUpdateListener { animation ->
            if (animation.animatedFraction == 1f) {
                val intent = Intent(this, ResultActivity::class.java)
                startActivity(intent)
                finish()
            }
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

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview
                )
            } catch(e: Exception){

            }
        },
            ContextCompat.getMainExecutor(this))
    }

}