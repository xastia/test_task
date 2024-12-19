package com.xastia.test.domain.repository

import com.xastia.test.domain.models.Result
import kotlinx.coroutines.flow.Flow

interface ResultRepository {
    suspend fun clearHistory()
    fun addResult(result: Result)
    fun showHistory(): Flow<List<Result>>
}