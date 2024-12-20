package com.xastia.test.domain.usecase

import com.xastia.test.domain.repository.TimeRepository

class GetDateUseCase(val timeRepository: TimeRepository) {
    fun execute(): String {
        return timeRepository.getCurrentDate()
    }
}