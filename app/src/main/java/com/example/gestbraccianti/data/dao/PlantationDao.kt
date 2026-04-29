package com.example.gestbraccianti.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.gestbraccianti.data.entity.Plantation
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantationDao {
    @Query("SELECT * FROM plantations WHERE isArchived = 0 ORDER BY name ASC")
    fun getActivePlantations(): Flow<List<Plantation>>

    @Query("SELECT * FROM plantations ORDER BY name ASC")
    fun getAllPlantations(): Flow<List<Plantation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlantation(plantation: Plantation)

    @Update
    suspend fun updatePlantation(plantation: Plantation)

    @Delete
    suspend fun deletePlantation(plantation: Plantation)
}
