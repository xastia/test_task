package com.xastia.test.presentation

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.xastia.test.R
import com.xastia.test.databinding.ActivityOnboarding1Binding

class Onboarding1 : AppCompatActivity() {
    private lateinit var binding: ActivityOnboarding1Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboarding1Binding.inflate(layoutInflater)
        setContentView(binding.root)



        binding.button2.setOnClickListener {
            binding.lottieAnimationView.apply {
                setMinProgress(0.0f)
                setMaxProgress(0.5f)
                speed = 2.0f
                repeatCount = 0
                playAnimation()
            }

            binding.lottieAnimationView.addAnimatorUpdateListener { animation ->
                if (animation.animatedFraction == 1f) {
                    binding.apply{
                        imageView3.setImageResource(R.drawable.image2)
                        title.text = resources.getString(R.string.title2)
                        info.text = resources.getString(R.string.info2)
                        button2.visibility = View.GONE
                        buttonContinue.visibility = View.VISIBLE

                    }
                }
            }

        }

        binding.buttonContinue.setOnClickListener {
            binding.lottieAnimationView.apply {
                setMinProgress(0.5f)
                setMaxProgress(1.0f)
                speed = 2.0f
                repeatCount = 0
                playAnimation()
            }

            binding.lottieAnimationView.addAnimatorUpdateListener { animation ->
                if (animation.animatedFraction == 1f) {
                    binding.apply{
                        imageView3.setImageResource(R.drawable.image3)
                        title.text = resources.getString(R.string.title3)
                        info.text = resources.getString(R.string.info3)
                        buttonContinue.visibility = View.GONE
                        buttonStartChecking.visibility = View.VISIBLE

                    }
                }
            }
        }

        binding.buttonStartChecking.setOnClickListener {
            val intent = Intent(this, Homepage1::class.java)
            startActivity(intent)
            finish()
        }

    }
}