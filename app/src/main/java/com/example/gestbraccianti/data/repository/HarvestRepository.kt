package com.example.gestbraccianti.data.repository

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

    suspend fun getCurrentYear(): HarvestYear? = harvestYearDao.getCurrentYear()
    
    suspend fun createNewYear(
        year: Int,
        migrateFromYear: Int? = null,
        migrateWorkers: Boolean = false,
        migrateGroups: Boolean = false
    ) {
        harvestYearDao.clearCurrentYear()
        harvestYearDao.insertYear(HarvestYear(id = year, isCurrent = true))

        if (migrateFromYear != null) {
            if (migrateWorkers) {
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

            if (migrateGroups) {
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
