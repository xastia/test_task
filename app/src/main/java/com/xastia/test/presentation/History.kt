package com.xastia.test.presentation

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xastia.test.data.MainDb
import com.xastia.test.domain.models.Result
import com.xastia.test.data.ResultAdapter
import com.xastia.test.data.repository.ResultRepositoryImpl
import com.xastia.test.databinding.ActivityHistoryBinding
import com.xastia.test.domain.usecase.ClearHistoryUseCase
import com.xastia.test.domain.usecase.ShowHistoryUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class History : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding
    private lateinit var resHistory: List<Result>
    private lateinit var adapter: ResultAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val startList = emptyList<Result>()
        //adapter = ResultAdapter(startList)

        val db = MainDb.getDb(this)
        val resultRepository = ResultRepositoryImpl(db)
        val cleanHistoryUseCase = ClearHistoryUseCase(resultRepository)
        val showHistoryUseCase = ShowHistoryUseCase(resultRepository)


        CoroutineScope(Dispatchers.IO).launch {
            resultRepository.addResult(Result(null, 44, "test", "test"))
        }

        showHistoryUseCase.execute().asLiveData().observe(this) { results ->
            resHistory = results
//            adapter.updateData(resHistory)
            adapter = ResultAdapter(resHistory)

            if (resHistory.isEmpty()) {
                binding.delete.visibility = View.GONE
                binding.noResults.visibility = View.VISIBLE
            } else if (resHistory.size <= 7) {
                binding.delete.visibility = View.VISIBLE
            } else {
                binding.delete.visibility = View.GONE
            }
        }




        binding.delete.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                cleanHistoryUseCase.execute()
            }
        }

        binding.rcView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                val totalItemCount = layoutManager.itemCount

                if (lastVisibleItemPosition == totalItemCount - 1) {
                    binding.delete.visibility = View.VISIBLE
                } else {
                    binding.delete.visibility = View.GONE
                }
            }
        })



        binding.arrowBtn.setOnClickListener {
            val intent = Intent(this, Homepage1::class.java)
            startActivity(intent)
            finish()
        }

    }

    private fun init() {
        binding.apply {
            rcView.layoutManager = LinearLayoutManager(this@History)
            rcView.adapter = adapter
        }
    }


}