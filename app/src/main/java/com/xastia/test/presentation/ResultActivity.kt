package com.xastia.test.presentation

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.xastia.test.R
import com.xastia.test.data.MainDb
import com.xastia.test.data.repository.ResultRepositoryImpl
import com.xastia.test.data.repository.TimeRepositoryImpl
import com.xastia.test.databinding.ActivityResultBinding
import com.xastia.test.domain.models.Result
import com.xastia.test.domain.usecase.AddResultUseCase
import com.xastia.test.domain.usecase.GetDateUseCase
import com.xastia.test.domain.usecase.GetTimeUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding
    private val animators = mutableListOf<Animator>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bpm = intent.getIntExtra("BPM", 0)
        val db = MainDb.getDb(this)
        val resultRepository = ResultRepositoryImpl(db)
        val addResultUseCase = AddResultUseCase(resultRepository)

        val timeRepository = TimeRepositoryImpl()
        val getTimeUseCase = GetTimeUseCase(timeRepository)
        val getDateUseCase = GetDateUseCase(timeRepository)
        val date = getDateUseCase.execute()
        val time = getTimeUseCase.execute()

        binding.date.text = "$time · $date"

        CoroutineScope(Dispatchers.IO).launch {
            addResultUseCase.execute(Result(null, bpm, time, date))
        }

        applyStatusStyling(bpm)

        // Спершу позиціонуємо точку на шкалі, потім запускаємо counter
        binding.scaleContainer.post {
            positionScaleDot(bpm)
            animateBpmCounter(bpm)
        }

        binding.ready.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.historyButton.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    /**
     * Number counter: BPM рахується від 0 до фінального значення за 1.4 секунди.
     * Decelerate interpolator — швидко на початку, плавно сповільнюється у кінці.
     */
    private fun animateBpmCounter(targetBpm: Int) {
        val animator = ValueAnimator.ofInt(0, targetBpm).apply {
            duration = 1400
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                binding.bpmNumber.text = animation.animatedValue.toString()
            }
            start()
        }
        animators += animator
    }

    /**
     * Колір pill-бейджа і точки на шкалі залежить від діапазону BPM:
     *  <60       — Уповільнено (amber)
     *  60..100   — У нормі (sage)
     *  >100      — Прискорено (coral)
     */
    private fun applyStatusStyling(bpm: Int) {
        when {
            bpm < 60 -> {
                binding.statusPill.setText(R.string.result_status_slow)
                binding.statusPill.setBackgroundResource(R.drawable.bg_status_pill_amber)
                binding.statusPill.setTextColor(ContextCompat.getColor(this, R.color.amber_dark))
                binding.scaleDot.setBackgroundResource(R.drawable.bg_scale_dot_amber)
            }
            bpm in 60..100 -> {
                binding.statusPill.setText(R.string.result_status_normal)
                binding.statusPill.setBackgroundResource(R.drawable.bg_status_pill_sage)
                binding.statusPill.setTextColor(ContextCompat.getColor(this, R.color.sage_dark))
                binding.scaleDot.setBackgroundResource(R.drawable.bg_scale_dot_sage)
            }
            else -> {
                binding.statusPill.setText(R.string.result_status_fast)
                binding.statusPill.setBackgroundResource(R.drawable.bg_status_pill_coral)
                binding.statusPill.setTextColor(ContextCompat.getColor(this, R.color.coral_dark))
                binding.scaleDot.setBackgroundResource(R.drawable.bg_scale_dot_coral)
            }
        }
    }

    /**
     * Позиція точки на шкалі (діапазон 40..140 BPM → 0..1 на ширині треку).
     * Точку зміщуємо marginStart, не змінюючи її розміру.
     */
    private fun positionScaleDot(bpm: Int) {
        val clamped = bpm.coerceIn(40, 140)
        val ratio = (clamped - 40) / 100f
        val trackWidth = binding.scaleContainer.width
        val dotWidth = binding.scaleDot.width
        val targetX = (ratio * (trackWidth - dotWidth)).toInt()

        val params = binding.scaleDot.layoutParams as FrameLayout.LayoutParams
        params.marginStart = targetX
        binding.scaleDot.layoutParams = params
    }

    override fun onDestroy() {
        super.onDestroy()
        animators.forEach { it.cancel() }
        animators.clear()
    }
}
