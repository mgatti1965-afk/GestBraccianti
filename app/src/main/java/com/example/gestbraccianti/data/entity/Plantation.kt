package com.example.gestbraccianti.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plantations")
data class Plantation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val isArchived: Boolean = false
)
