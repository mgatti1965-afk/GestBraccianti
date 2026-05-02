package com.example.gestbraccianti.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gestbraccianti.data.entity.HarvestYear
import kotlinx.coroutines.flow.Flow

@Dao
interface HarvestYearDao {
    @Query("SELECT * FROM harvest_years ORDER BY id DESC")
    fun getAllHarvestYears(): Flow<List<HarvestYear>>

    @Query("SELECT * FROM harvest_years")
    suspend fun getAllYearsStatic(): List<HarvestYear>

    @Query("SELECT * FROM harvest_years WHERE isCurrent = 1 LIMIT 1")
    suspend fun getCurrentYear(): HarvestYear?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertYear(harvestYear: HarvestYear)

    @Query("UPDATE harvest_years SET isCurrent = 0")
    suspend fun clearCurrentYear()

    @Query("UPDATE harvest_years SET isCurrent = 1 WHERE id = :yearId")
    suspend fun setCurrentYear(yearId: Int)

    @Query("DELETE FROM harvest_years WHERE id = :yearId")
    suspend fun deleteYear(yearId: Int)
}
