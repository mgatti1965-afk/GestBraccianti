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
    version = 9,
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

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add 'notes' column to 'harvest_years' if it doesn't exist
                try {
                    db.execSQL("ALTER TABLE harvest_years ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {
                    // Column may already exist, ignore exception
                }

                // Drop legacy indices that Room no longer expects
                db.execSQL("DROP INDEX IF EXISTS `index_work_logs_date`")
                db.execSQL("DROP INDEX IF EXISTS `index_work_logs_workerId_harvestYearId`")
                db.execSQL("DROP INDEX IF EXISTS `index_worker_year_configs_workerId`")
                db.execSQL("DROP INDEX IF EXISTS `index_worker_group_cross_ref_workerId`")

                // Ensure indices match current Entity definitions
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_work_logs_workerId` ON `work_logs` (`workerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_work_logs_harvestYearId` ON `work_logs` (`harvestYearId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_worker_year_configs_harvestYearId` ON `worker_year_configs` (`harvestYearId`)")

                // Create new tables for Worker Groups if they don't exist
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `worker_groups` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `yearId` INTEGER NOT NULL, 
                        FOREIGN KEY(`yearId`) REFERENCES `harvest_years`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_worker_groups_yearId` ON `worker_groups` (`yearId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `worker_group_cross_ref` (
                        `workerId` INTEGER NOT NULL, 
                        `groupId` INTEGER NOT NULL, 
                        PRIMARY KEY(`workerId`, `groupId`), 
                        FOREIGN KEY(`workerId`) REFERENCES `workers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                        FOREIGN KEY(`groupId`) REFERENCES `worker_groups`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_worker_group_cross_ref_groupId` ON `worker_group_cross_ref` (`groupId`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE work_logs ADD COLUMN hourlyRate REAL NOT NULL DEFAULT 0.0")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE work_logs ADD COLUMN ordinaryHours REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE work_logs ADD COLUMN extraHours REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE work_logs ADD COLUMN holidayHours REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE work_logs ADD COLUMN ordinaryAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE work_logs ADD COLUMN extraAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE work_logs ADD COLUMN holidayAmount REAL NOT NULL DEFAULT 0.0")
                
                // Popolamento iniziale per coerenza storica:
                // 1. Log feriali: tutto su ordinario
                db.execSQL("UPDATE work_logs SET ordinaryHours = totalHours, ordinaryAmount = totalAmount WHERE isManualHoliday = 0")
                // 2. Log festivi: tutto su festivo
                db.execSQL("UPDATE work_logs SET holidayHours = totalHours, holidayAmount = totalAmount WHERE isManualHoliday = 1")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // worker_year_configs
                db.execSQL("ALTER TABLE worker_year_configs ADD COLUMN extraHourlyRate REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE worker_year_configs ADD COLUMN holidayHourlyRate REAL NOT NULL DEFAULT 0.0")
                db.execSQL("UPDATE worker_year_configs SET extraHourlyRate = hourlyRate, holidayHourlyRate = hourlyRate")

                // work_logs
                db.execSQL("ALTER TABLE work_logs ADD COLUMN extraHourlyRate REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE work_logs ADD COLUMN holidayHourlyRate REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE work_logs ADD COLUMN isManualHoliday INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE work_logs ADD COLUMN totalAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("UPDATE work_logs SET extraHourlyRate = hourlyRate, holidayHourlyRate = hourlyRate, totalAmount = totalHours * hourlyRate")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gest_braccianti_db"
                )
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
