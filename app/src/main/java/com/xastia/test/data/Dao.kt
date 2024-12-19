package com.xastia.test.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.xastia.test.domain.models.Result
import kotlinx.coroutines.flow.Flow

@Dao
interface Dao {
    @Insert
    fun insertResult(result: Result)

    @Query("SELECT * FROM results")
    fun getResultHistory(): Flow<List<Result>>

    @Query("DELETE FROM results")
    suspend fun clearResults()
}