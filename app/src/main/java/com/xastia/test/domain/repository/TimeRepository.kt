package com.xastia.test.domain.repository

interface TimeRepository {
    fun getCurrentTime(): String
    fun getCurrentDate(): String
}