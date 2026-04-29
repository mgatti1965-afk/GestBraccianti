package com.example.gestbraccianti.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.gestbraccianti.data.dao.HarvestYearDao
import com.example.gestbraccianti.data.dao.PlantationDao
import com.example.gestbraccianti.data.dao.WorkLogDao
import com.example.gestbraccianti.data.dao.WorkerDao
import com.example.gestbraccianti.data.dao.WorkerYearConfigDao
import com.example.gestbraccianti.data.entity.HarvestYear
import com.example.gestbraccianti.data.entity.Plantation
import com.example.gestbraccianti.data.entity.WorkLog
import com.example.gestbraccianti.data.entity.Worker
import com.example.gestbraccianti.data.entity.WorkerYearConfig

@Database(
    entities = [HarvestYear::class, Worker::class, WorkLog::class, Plantation::class, WorkerYearConfig::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun harvestYearDao(): HarvestYearDao
    abstract fun workerDao(): WorkerDao
    abstract fun workLogDao(): WorkLogDao
    abstract fun plantationDao(): PlantationDao
    abstract fun workerYearConfigDao(): WorkerYearConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gest_braccianti_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
