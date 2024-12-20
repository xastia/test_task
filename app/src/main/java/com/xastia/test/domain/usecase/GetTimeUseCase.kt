package com.xastia.test.domain.usecase

import com.xastia.test.domain.repository.TimeRepository

class GetTimeUseCase(val timeRepository: TimeRepository) {
    fun execute(): String {
        return timeRepository.getCurrentTime()
    }
}