package com.xastia.test.presentation

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.xastia.test.data.MainDb
import com.xastia.test.data.repository.ResultRepositoryImpl
import com.xastia.test.databinding.ActivityResultBinding
import com.xastia.test.domain.models.Result
import com.xastia.test.domain.usecase.AddResultUseCase
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
        val bpm = intent.getIntExtra("BPM", 0)
        val db = MainDb.getDb(this)
        val resultRepository = ResultRepositoryImpl(db)
        val addResultUseCase = AddResultUseCase(resultRepository)

        val currentDateTime = LocalDateTime.now()
        val formatterTime = DateTimeFormatter.ofPattern("HH:mm")
        val formatterDate = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val date = currentDateTime.format(formatterDate)
        val time = currentDateTime.format(formatterTime)

        binding.date.text = time + "\n" + date
        CoroutineScope(Dispatchers.IO).launch {
            addResultUseCase.execute(Result(null, bpm, time, date))

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
}