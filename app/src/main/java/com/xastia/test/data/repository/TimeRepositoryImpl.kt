package com.xastia.test.data.repository

import com.xastia.test.domain.repository.TimeRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TimeRepositoryImpl: TimeRepository {
    val currentDateTime: LocalDateTime = LocalDateTime.now()

    override fun getCurrentTime(): String {
        val formatterTime = DateTimeFormatter.ofPattern("HH:mm")
        return currentDateTime.format(formatterTime)
    }

    override fun getCurrentDate(): String {
        val formatterDate = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        return currentDateTime.format(formatterDate)
    }
}