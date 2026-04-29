package com.example.gestbraccianti.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "worker_groups")
data class WorkerGroup(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val yearId: Int // Group tied to a specific year
)

@Entity(tableName = "worker_group_cross_ref", primaryKeys = ["workerId", "groupId"])
data class WorkerGroupCrossRef(
    val workerId: Long,
    val groupId: Long
)
