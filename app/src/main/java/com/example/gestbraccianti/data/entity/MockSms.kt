package com.example.gestbraccianti.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mock_sms")
data class MockSms(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val address: String,
    val body: String,
    val date: Long
)
