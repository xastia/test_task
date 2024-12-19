package com.xastia.test.presentation

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.WindowManager
import com.xastia.test.R
import com.xastia.test.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        }
        binding.lottieLoading.apply {
            setMinProgress(0.0f)
            setMaxProgress(1.0f)
            repeatCount = 0
            playAnimation()
        }
        binding.lottieLoading.addAnimatorUpdateListener { animation ->
            if (animation.animatedFraction == 1f) {
                val intent = Intent(this, Onboarding1::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}