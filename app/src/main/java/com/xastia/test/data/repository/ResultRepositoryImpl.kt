package com.xastia.test.data.repository

import com.xastia.test.data.MainDb
import com.xastia.test.domain.models.Result
import com.xastia.test.domain.repository.ResultRepository
import kotlinx.coroutines.flow.Flow

class ResultRepositoryImpl(private val db:MainDb): ResultRepository {
    override suspend fun clearHistory() {
        db.getDao().clearResults()
    }

    override fun addResult(result: Result) {
        db.getDao().insertResult(result)
    }

    override fun showHistory(): Flow<List<Result>> {
        return db.getDao().getResultHistory()
    }

}