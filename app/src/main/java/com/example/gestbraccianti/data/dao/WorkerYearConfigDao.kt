package com.example.gestbraccianti.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gestbraccianti.data.entity.Worker
import com.example.gestbraccianti.data.entity.WorkerYearConfig
import com.example.gestbraccianti.data.model.WorkerYearStats
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerYearConfigDao {
    @Query("""
        SELECT 
            w.id as workerId, 
            w.name, 
            w.surname, 
            wyc.hourlyRate,
            IFNULL(SUM(wl.totalHours), 0.0) as totalHours,
            IFNULL(SUM(wl.totalHours) * wyc.hourlyRate, 0.0) as totalEarnings
        FROM workers w
        INNER JOIN worker_year_configs wyc ON w.id = wyc.workerId
        LEFT JOIN work_logs wl ON w.id = wl.workerId AND wl.harvestYearId = :yearId
        WHERE wyc.harvestYearId = :yearId
        GROUP BY w.id
    """)
    fun getWorkerStatsForYear(yearId: Int): Flow<List<WorkerYearStats>>

    @Query("""
        SELECT workers.* FROM workers 
        INNER JOIN worker_year_configs ON workers.id = worker_year_configs.workerId 
        WHERE worker_year_configs.harvestYearId = :yearId
    """)
    fun getWorkersForYear(yearId: Int): Flow<List<Worker>>

    @Query("SELECT * FROM worker_year_configs WHERE workerId = :workerId AND harvestYearId = :yearId")
    suspend fun getConfig(workerId: Long, yearId: Int): WorkerYearConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: WorkerYearConfig)
    
    @Query("SELECT * FROM worker_year_configs WHERE harvestYearId = :yearId")
    suspend fun getConfigsForYear(yearId: Int): List<WorkerYearConfig>
}
