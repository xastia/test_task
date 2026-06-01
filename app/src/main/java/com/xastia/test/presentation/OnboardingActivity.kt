package com.xastia.test.presentation

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.xastia.test.R
import com.xastia.test.databinding.ActivityOnboardingBinding

/**
 * Хост онбордингу.
 *
 * Slide transition анімація — вбудована у ViewPager2 (swipe + smooth scroll).
 * Кнопка внизу змінює текст: на останньому слайді стає "Почати" і веде на головну.
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewPager.adapter = OnboardingPagerAdapter(this)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateIndicators(position)
                updateButtonText(position)
            }
        })

        binding.btnNext.setOnClickListener {
            val currentItem = binding.viewPager.currentItem
            if (currentItem < (binding.viewPager.adapter?.itemCount ?: 0) - 1) {
                binding.viewPager.currentItem = currentItem + 1
            } else {
                goToHome()
            }
        }
    }

    private fun updateIndicators(position: Int) {
        binding.dot1.setBackgroundResource(
            if (position == 0) R.drawable.bg_indicator_dot_active else R.drawable.bg_indicator_dot
        )
        binding.dot2.setBackgroundResource(
            if (position == 1) R.drawable.bg_indicator_dot_active else R.drawable.bg_indicator_dot
        )
        // Виправляємо ширину активної точки (16dp) і неактивної (6dp)
        val dotSizeDp = if (position == 0) 16 else 6
        val params1 = binding.dot1.layoutParams
        params1.width = dpToPx(if (position == 0) 16 else 6)
        binding.dot1.layoutParams = params1

        val params2 = binding.dot2.layoutParams
        params2.width = dpToPx(if (position == 1) 16 else 6)
        binding.dot2.layoutParams = params2
    }

    private fun updateButtonText(position: Int) {
        val lastIndex = (binding.viewPager.adapter?.itemCount ?: 1) - 1
        binding.btnNext.text = getString(
            if (position == lastIndex) R.string.onb_start else R.string.onb_next
        )
    }

    private fun goToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    /**
     * 2 слайди: інтро (про вимірювання) і про історію.
     * Параметри слайдів — у newInstance(...).
     */
    private class OnboardingPagerAdapter(activity: FragmentActivity) :
        FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> OnboardingFragment.newInstance(
                iconRes = R.drawable.ic_heartbeat,
                cardBgRes = R.drawable.bg_card_coral_light,
                iconTintRes = R.color.coral,
                titleRes = R.string.onb_intro_title,
                bodyRes = R.string.onb_intro_body
            )
            else -> OnboardingFragment.newInstance(
                iconRes = R.drawable.ic_hand_finger,
                cardBgRes = R.drawable.bg_card_sage_light,
                iconTintRes = R.color.sage,
                titleRes = R.string.onb_history_title,
                bodyRes = R.string.onb_history_body
            )
        }
    }
}
