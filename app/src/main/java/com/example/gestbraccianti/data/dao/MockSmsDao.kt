package com.example.gestbraccianti.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.gestbraccianti.data.entity.MockSms

@Dao
interface MockSmsDao {
    @Insert
    suspend fun insert(sms: MockSms)

    @Query("SELECT * FROM mock_sms WHERE date >= :start AND date <= :end")
    suspend fun getMockSmsForRange(start: Long, end: Long): List<MockSms>

    @Query("DELETE FROM mock_sms")
    suspend fun deleteAll()
}
