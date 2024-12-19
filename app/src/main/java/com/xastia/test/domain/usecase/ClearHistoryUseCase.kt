package com.xastia.test.domain.usecase

import com.xastia.test.domain.repository.ResultRepository

class ClearHistoryUseCase(private val resultRepository: ResultRepository) {
    suspend fun execute() {
        resultRepository.clearHistory()
    }
}