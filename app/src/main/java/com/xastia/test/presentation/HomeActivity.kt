package com.xastia.test.presentation

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.xastia.test.R
import com.xastia.test.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_CODE = 16
        private val PERMISSION = arrayOf(Manifest.permission.CAMERA)
    }

    private fun permissionGranted() = PERMISSION.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private lateinit var binding: ActivityHomeBinding
    private var breathingAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startBreathingAnimation()

        binding.heartButton.setOnClickListener {
            if (permissionGranted()) {
                val intent = Intent(this, InstructionActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    PERMISSION,
                    PERMISSION_CODE
                )
            }
        }

        binding.historyButton.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    /**
     * Breathing-анімація для центрального серця: масштабує коло від 1.0 до 1.08
     * туди-сюди, 1500мс кожна фаза. Створює відчуття спокійного дихання.
     */
    private fun startBreathingAnimation() {
        breathingAnimator = ObjectAnimator.ofPropertyValuesHolder(
            binding.breathingHeartCircle,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1.0f, 1.08f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1.0f, 1.08f)
        ).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        breathingAnimator?.cancel()
        breathingAnimator = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_CODE) {
            if (permissionGranted()) {
                val intent = Intent(this, InstructionActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
