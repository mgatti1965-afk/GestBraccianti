package com.example.gestbraccianti.data.model

import androidx.room.Embedded
import com.example.gestbraccianti.data.entity.Worker

data class WorkerWithRate(
    @Embedded val worker: Worker,
    val hourlyRate: Double,
    val extraHourlyRate: Double,
    val holidayHourlyRate: Double
)
