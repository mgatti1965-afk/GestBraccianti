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

import com.example.gestbraccianti.data.dao.WorkerGroupDao
import com.example.gestbraccianti.data.entity.WorkerGroup
import com.example.gestbraccianti.data.entity.WorkerGroupCrossRef

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        HarvestYear::class, 
        Worker::class, 
        WorkLog::class, 
        Plantation::class, 
        WorkerYearConfig::class,
        WorkerGroup::class,
        WorkerGroupCrossRef::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun harvestYearDao(): HarvestYearDao
    abstract fun workerDao(): WorkerDao
    abstract fun workLogDao(): WorkLogDao
    abstract fun plantationDao(): PlantationDao
    abstract fun workerYearConfigDao(): WorkerYearConfigDao
    abstract fun workerGroupDao(): WorkerGroupDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE work_logs ADD COLUMN hourlyRate REAL NOT NULL DEFAULT 0.0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gest_braccianti_db"
                )
                    .addMigrations(MIGRATION_5_6)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
