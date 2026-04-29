package com.example.gestbraccianti.data.repository

import com.example.gestbraccianti.data.dao.PlantationDao
import com.example.gestbraccianti.data.entity.Plantation
import kotlinx.coroutines.flow.Flow

class PlantationRepository(private val plantationDao: PlantationDao) {
    val activePlantations: Flow<List<Plantation>> = plantationDao.getActivePlantations()
    val allPlantations: Flow<List<Plantation>> = plantationDao.getAllPlantations()

    suspend fun insertPlantation(plantation: Plantation) = plantationDao.insertPlantation(plantation)
    suspend fun updatePlantation(plantation: Plantation) = plantationDao.updatePlantation(plantation)
    suspend fun deletePlantation(plantation: Plantation) = plantationDao.deletePlantation(plantation)
}
