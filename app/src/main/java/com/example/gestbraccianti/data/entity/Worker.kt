package com.example.gestbraccianti.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workers")
data class Worker(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val surname: String,
    val isArchived: Boolean = false // Useful for workers not active in current year
)
