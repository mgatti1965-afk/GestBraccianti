package com.example.gestbraccianti.data.repository

import com.example.gestbraccianti.data.dao.HarvestYearDao
import com.example.gestbraccianti.data.dao.WorkerYearConfigDao
import com.example.gestbraccianti.data.entity.HarvestYear
import com.example.gestbraccianti.data.entity.WorkerYearConfig
import kotlinx.coroutines.flow.Flow

class HarvestRepository(
    private val harvestYearDao: HarvestYearDao,
    private val workerYearConfigDao: WorkerYearConfigDao
) {
    val allYears: Flow<List<HarvestYear>> = harvestYearDao.getAllHarvestYears()

    suspend fun getCurrentYear(): HarvestYear? = harvestYearDao.getCurrentYear()
    
    suspend fun createNewYear(year: Int, migrateFromYear: Int? = null) {
        harvestYearDao.clearCurrentYear()
        harvestYearDao.insertYear(HarvestYear(id = year, isCurrent = true))
        
        if (migrateFromYear != null) {
            val previousConfigs = workerYearConfigDao.getConfigsForYear(migrateFromYear)
            previousConfigs.forEach { config ->
                workerYearConfigDao.insertConfig(
                    WorkerYearConfig(
                        workerId = config.workerId,
                        harvestYearId = year,
                        hourlyRate = config.hourlyRate
                    )
                )
            }
        }
    }

    suspend fun switchYear(yearId: Int) {
        harvestYearDao.clearCurrentYear()
        harvestYearDao.setCurrentYear(yearId)
    }

    suspend fun deselectYear() {
        harvestYearDao.clearCurrentYear()
    }

    suspend fun deleteYear(yearId: Int) {
        harvestYearDao.deleteYear(yearId)
    }
}
