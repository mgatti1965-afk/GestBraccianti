package com.example.gestbraccianti.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "worker_groups",
    foreignKeys = [
        ForeignKey(
            entity = HarvestYear::class,
            parentColumns = ["id"],
            childColumns = ["yearId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("yearId")]
)
data class WorkerGroup(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val yearId: Int // Group tied to a specific year
)

@Entity(
    tableName = "worker_group_cross_ref",
    primaryKeys = ["workerId", "groupId"],
    foreignKeys = [
        ForeignKey(
            entity = Worker::class,
            parentColumns = ["id"],
            childColumns = ["workerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WorkerGroup::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("groupId")]
)
data class WorkerGroupCrossRef(
    val workerId: Long,
    val groupId: Long
)
