package com.xastia.test.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xastia.test.R
import com.xastia.test.databinding.ResultItemBinding
import com.xastia.test.domain.models.Result

class ResultAdapter(var historyList: List<Result>) :
    RecyclerView.Adapter<ResultAdapter.ResultHolder>() {

    class ResultHolder(item: View) : RecyclerView.ViewHolder(item) {
        val binding = ResultItemBinding.bind(item)
        fun bind(result: Result) = with(binding) {
            resultText.text = "${result.result} BPM"
            time.text = result.time
            date.text = result.date

            val markerRes = when {
                result.result < 60 -> R.drawable.bg_status_marker_amber
                result.result in 60..100 -> R.drawable.bg_status_marker_sage
                else -> R.drawable.bg_status_marker_coral
            }
            statusMarker.setBackgroundResource(markerRes)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.result_item, parent, false)
        return ResultHolder(view)
    }

    override fun getItemCount(): Int = historyList.size

    override fun onBindViewHolder(holder: ResultHolder, position: Int) {
        holder.bind(historyList[position])
    }

    fun updateData(newResults: List<Result>) {
        historyList = newResults
        notifyDataSetChanged()
    }
}
