package com.xastia.test.presentation

import android.content.Intent
import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        var bpm = intent.getIntExtra("BPM", 0)
        val db = MainDb.getDb(this)
        val resultRepository = ResultRepositoryImpl(db)
        val addResultUseCase = AddResultUseCase(resultRepository)

        val timeRepository = TimeRepositoryImpl()
        val getTimeUseCase = GetTimeUseCase(timeRepository)
        val getDateUseCase = GetDateUseCase(timeRepository)
        val date = getDateUseCase.execute()
        val time = getTimeUseCase.execute()

        bpm = 100

        binding.date.text = time + "\n" + date
        CoroutineScope(Dispatchers.IO).launch {
            addResultUseCase.execute(Result(null, bpm, time, date))

        }

        binding.resBar.apply {
            setMinProgress(0.0f)
            setMaxProgress(getDuration(bpm))
            repeatCount = 0
            speed = 0.25f
            playAnimation()
        }

        binding.ready.setOnClickListener {
            val intent = Intent(this, Homepage1::class.java)
            startActivity(intent)
            finish()
        }

        binding.historyButton.setOnClickListener {
            val intent = Intent(this, History::class.java)
            startActivity(intent)
            finish()
        }
    }

    private  fun getDuration(bpm: Int): Float{
        var duration = 0.0f
        if(bpm < 60) {
            binding.textView5.text = getString(R.string.slow)
            binding.textView5.setTextColor(ContextCompat.getColor(this, R.color.blue))
            binding.textSlow.setTextColor(ContextCompat.getColor(this, R.color.black))
            duration = ( bpm * 0.11f ) / 60
        }
        else if(bpm in 60..100) {
            binding.textView5.text = getString(R.string.normal)
            binding.textView5.setTextColor(ContextCompat.getColor(this, R.color.green))
            binding.textNormal.setTextColor(ContextCompat.getColor(this, R.color.black))
            duration = ( bpm * 0.33f ) / 100
        }
        else {
            binding.textView5.text = getString(R.string.fast)
            binding.textView5.setTextColor(ContextCompat.getColor(this, R.color.orange))
            binding.textFast.setTextColor(ContextCompat.getColor(this, R.color.black))
            duration = ( bpm * 0.66f ) / 160
        }

        return duration
    }
}