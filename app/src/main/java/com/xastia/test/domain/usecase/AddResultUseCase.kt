package com.xastia.test.domain.usecase

import com.xastia.test.domain.models.Result
import com.xastia.test.domain.repository.ResultRepository

class AddResultUseCase(private val resultRepository: ResultRepository) {
    fun execute(result: Result) {
        resultRepository.addResult(result)
    }
}