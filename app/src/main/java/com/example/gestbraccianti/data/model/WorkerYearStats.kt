package com.example.gestbraccianti.data.model

data class WorkerYearStats(
    val workerId: Long,
    val name: String,
    val surname: String,
    val hourlyRate: Double,
    val totalHours: Double,
    val totalEarnings: Double
)
