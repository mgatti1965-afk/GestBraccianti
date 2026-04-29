package com.example.gestbraccianti

import android.app.Application
import com.example.gestbraccianti.data.AppDatabase
import com.example.gestbraccianti.data.repository.*

class GestBracciantiApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    
    val harvestRepository by lazy { 
        HarvestRepository(database.harvestYearDao(), database.workerYearConfigDao()) 
    }
    val workerRepository by lazy { WorkerRepository(database.workerDao()) }
    val workLogRepository by lazy { WorkLogRepository(database.workLogDao()) }
    val plantationRepository by lazy { PlantationRepository(database.plantationDao()) }
    val workerYearConfigRepository by lazy { WorkerYearConfigRepository(database.workerYearConfigDao()) }
    val workerGroupRepository by lazy { WorkerGroupRepository(database.workerGroupDao()) }
}
