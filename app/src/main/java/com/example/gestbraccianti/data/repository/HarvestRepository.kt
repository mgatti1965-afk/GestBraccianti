package com.example.gestbraccianti.data.repository

import android.util.Log
import com.example.gestbraccianti.data.dao.HarvestYearDao
import com.example.gestbraccianti.data.dao.WorkerYearConfigDao
import com.example.gestbraccianti.data.dao.WorkerGroupDao
import com.example.gestbraccianti.data.entity.HarvestYear
import com.example.gestbraccianti.data.entity.WorkerGroup
import com.example.gestbraccianti.data.entity.WorkerGroupCrossRef
import com.example.gestbraccianti.data.entity.WorkerYearConfig
import kotlinx.coroutines.flow.Flow

class HarvestRepository(
    private val harvestYearDao: HarvestYearDao,
    private val workerYearConfigDao: WorkerYearConfigDao,
    private val workerGroupDao: WorkerGroupDao
) {
    val allYears: Flow<List<HarvestYear>> = harvestYearDao.getAllHarvestYears()

    suspend fun getCurrentYear(): HarvestYear? = try {
        harvestYearDao.getCurrentYear()
    } catch (e: Exception) {
        Log.e("HarvestRepository", "Error getting current year", e)
        null
    }
    
    suspend fun createNewYear(
        year: Int,
        notes: String = "",
        migrateFromYear: Int? = null,
        migrateWorkers: Boolean = false,
        migrateGroups: Boolean = false
    ) {
        try {
            Log.d("HarvestRepository", "Creating new year: $year")
            
            // Check if year already exists
            val existingYears = harvestYearDao.getAllYearsStatic()
            if (existingYears.any { it.id == year }) {
                throw IllegalArgumentException("L'anno $year esiste già nel database.")
            }

            harvestYearDao.clearCurrentYear()
            harvestYearDao.insertYear(HarvestYear(id = year, isCurrent = true, notes = notes))

            if (migrateFromYear != null) {
                if (migrateWorkers) {
                    Log.d("HarvestRepository", "Migrating workers from $migrateFromYear to $year")
                    val previousConfigs = workerYearConfigDao.getConfigsForYear(migrateFromYear)
                    previousConfigs.forEach { config ->
                        workerYearConfigDao.insertConfig(
                            WorkerYearConfig(
                                workerId = config.workerId,
                                harvestYearId = year,
                                hourlyRate = config.hourlyRate,
                                extraHourlyRate = config.extraHourlyRate,
                                holidayHourlyRate = config.holidayHourlyRate
                            )
                        )
                    }
                }

                if (migrateGroups) {
                    Log.d("HarvestRepository", "Migrating groups from $migrateFromYear to $year")
                    val previousGroups = workerGroupDao.getGroupsForYearStatic(migrateFromYear)
                    previousGroups.forEach { group ->
                        val newGroupId = workerGroupDao.insertGroup(
                            WorkerGroup(name = group.name, yearId = year)
                        )
                        // Migra i membri del gruppo
                        val members = workerGroupDao.getCrossRefsForGroupStatic(group.id)
                        members.forEach { member ->
                            workerGroupDao.insertWorkerToGroup(
                                WorkerGroupCrossRef(workerId = member.workerId, groupId = newGroupId)
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("HarvestRepository", "Error creating new year", e)
            throw e
        }
    }

    suspend fun switchYear(yearId: Int) {
        try {
            harvestYearDao.clearCurrentYear()
            harvestYearDao.setCurrentYear(yearId)
        } catch (e: Exception) {
            Log.e("HarvestRepository", "Error switching year to $yearId", e)
        }
    }

    suspend fun deselectYear() {
        try {
            harvestYearDao.clearCurrentYear()
        } catch (e: Exception) {
            Log.e("HarvestRepository", "Error deselecting year", e)
        }
    }

    suspend fun deleteYear(yearId: Int) {
        try {
            harvestYearDao.deleteYear(yearId)
        } catch (e: Exception) {
            Log.e("HarvestRepository", "Error deleting year $yearId", e)
        }
    }
}
