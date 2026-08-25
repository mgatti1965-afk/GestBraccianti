package com.example.gestbraccianti.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.gestbraccianti.data.entity.Worker
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerDao {
    @Query("SELECT * FROM workers WHERE isArchived = 0 ORDER BY surname, name ASC")
    fun getActiveWorkers(): Flow<List<Worker>>

    @Query("SELECT * FROM workers ORDER BY surname, name ASC")
    fun getAllWorkers(): Flow<List<Worker>>

    @Query("SELECT * FROM workers")
    suspend fun getAllWorkersStatic(): List<Worker>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorker(worker: Worker): Long

    @Update
    suspend fun updateWorker(worker: Worker)

    @Delete
    suspend fun deleteWorker(worker: Worker)

    @Query("SELECT * FROM workers WHERE id = :id")
    suspend fun getWorkerById(id: Long): Worker?

    @Query("UPDATE work_logs SET workerId = :targetId WHERE workerId = :sourceId")
    suspend fun migrateWorkLogs(sourceId: Long, targetId: Long)

    @Query("UPDATE worker_year_configs SET workerId = :targetId WHERE workerId = :sourceId")
    suspend fun migrateYearConfigs(sourceId: Long, targetId: Long)

    @Query("UPDATE worker_group_cross_ref SET workerId = :targetId WHERE workerId = :sourceId")
    suspend fun migrateGroupRefs(sourceId: Long, targetId: Long)

    @androidx.room.Transaction
    suspend fun mergeWorkerData(sourceId: Long, targetId: Long) {
        // 1. Migrate logs
        migrateWorkLogs(sourceId, targetId)
        
        // 2. Migrate group refs
        migrateGroupRefs(sourceId, targetId)
        
        // 3. Migrate Year Configs safely (handle duplicates)
        val sourceConfigs = getYearConfigsForWorker(sourceId)
        val targetConfigs = getYearConfigsForWorker(targetId)
        val targetYearIds = targetConfigs.map { it.harvestYearId }.toSet()
        
        sourceConfigs.forEach { config ->
            if (config.harvestYearId !in targetYearIds) {
                // If target doesn't have a config for this year, we can just update the workerId
                updateYearConfigWorkerId(config.workerId, config.harvestYearId, targetId)
            } else {
                // If target already has a config for this year, delete the source one
                deleteYearConfig(config.workerId, config.harvestYearId)
            }
        }
        
        // 4. Delete the source worker
        val sourceWorker = getWorkerById(sourceId)
        if (sourceWorker != null) {
            deleteWorker(sourceWorker)
        }
    }

    @Query("SELECT * FROM worker_year_configs WHERE workerId = :workerId")
    suspend fun getYearConfigsForWorker(workerId: Long): List<com.example.gestbraccianti.data.entity.WorkerYearConfig>

    @Query("UPDATE worker_year_configs SET workerId = :newWorkerId WHERE workerId = :oldWorkerId AND harvestYearId = :yearId")
    suspend fun updateYearConfigWorkerId(oldWorkerId: Long, yearId: Int, newWorkerId: Long)

    @Query("DELETE FROM worker_year_configs WHERE workerId = :workerId AND harvestYearId = :yearId")
    suspend fun deleteYearConfig(workerId: Long, yearId: Int)

    @Query("""
        SELECT w.*, wyc.hourlyRate, wyc.extraHourlyRate, wyc.holidayHourlyRate
        FROM workers w
        INNER JOIN worker_year_configs wyc ON w.id = wyc.workerId
        WHERE wyc.harvestYearId = :yearId
        ORDER BY w.surname, w.name ASC
    """)
    fun getWorkersWithRateForYear(yearId: Int): Flow<List<com.example.gestbraccianti.data.model.WorkerWithRate>>
}
