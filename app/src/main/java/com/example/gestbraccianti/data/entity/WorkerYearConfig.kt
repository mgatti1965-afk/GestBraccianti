package com.example.gestbraccianti.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "worker_year_configs",
    primaryKeys = ["workerId", "harvestYearId"],
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
    indices = [Index("harvestYearId")]
)
data class WorkerYearConfig(
    val workerId: Long,
    val harvestYearId: Int,
    val hourlyRate: Double
)
