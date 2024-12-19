package com.xastia.test.domain.usecase

import com.xastia.test.domain.models.Result
import com.xastia.test.domain.repository.ResultRepository
import kotlinx.coroutines.flow.Flow

class ShowHistoryUseCase(private val resultRepository: ResultRepository) {
    fun execute(): Flow<List<Result>> {
       return resultRepository.showHistory()
    }
}