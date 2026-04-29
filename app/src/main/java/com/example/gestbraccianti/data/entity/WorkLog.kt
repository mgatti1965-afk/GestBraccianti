package com.example.gestbraccianti.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "work_logs",
    foreignKeys = [
        ForeignKey(
            entity = Worker::class,
            parentColumns = ["id"],
            childColumns = ["workerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = HarvestYear::class,
            parentColumns = ["id"],
            childColumns = ["harvestYearId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workerId"), Index("harvestYearId")]
)
data class WorkLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workerId: Long,
    val harvestYearId: Int,
    val date: Long, // Midnight timestamp for the day
    val morningStart: String? = null,
    val morningEnd: String? = null,
    val afternoonStart: String? = null,
    val afternoonEnd: String? = null,
    val totalHours: Double = 0.0
)
