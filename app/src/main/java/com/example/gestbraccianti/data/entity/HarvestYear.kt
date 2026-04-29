package com.example.gestbraccianti.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "harvest_years")
data class HarvestYear(
    @PrimaryKey val id: Int, // The year itself can be the ID, e.g., 2024
    val isCurrent: Boolean = false
)
