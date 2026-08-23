package com.example.gestbraccianti.data.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.room.withTransaction
import com.example.gestbraccianti.data.AppDatabase
import com.example.gestbraccianti.data.entity.*
import com.example.gestbraccianti.ui.utils.formatDecimalHours
import com.example.gestbraccianti.ui.utils.parseTimeToDouble
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*

object CsvUtils {

    suspend fun exportToCsv(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        try {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                OutputStreamWriter(output).use { writer ->
                    writeCsvData(db, writer)
                }
            }
            true
        } catch (e: Exception) {
            Log.e("CsvUtils", "Errore esportazione CSV", e)
            false
        }
    }

    suspend fun createInternalBackup(context: Context): Boolean = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        try {
            val backupDir = File(context.getExternalFilesDir(null), "backups")
            if (!backupDir.exists()) backupDir.mkdirs()
            
            val timestamp = com.example.gestbraccianti.ui.utils.TimeUtils.fileTimestampFormatter.format(java.util.Date())
            val file = File(backupDir, "GestBraccianti_AutoBkp_$timestamp.csv")
            
            FileWriter(file).use { writer ->
                writeCsvData(db, writer)
            }
            true
        } catch (e: Exception) {
            Log.e("CsvUtils", "Errore backup interno", e)
            false
        }
    }

    private suspend fun writeCsvData(db: AppDatabase, writer: Writer) {
        // Workers
        val workers = db.workerDao().getAllWorkersStatic()
        if (workers.isNotEmpty()) {
            writer.write("TIPO;ID;NOME;COGNOME;TELEFONO;ARCHIVIATO\n")
            workers.forEach {
                writer.write("W;${it.id};${it.name};${it.surname};${it.phoneNumber};${if (it.isArchived) 1 else 0}\n")
            }
        }
        // Years
        val years = db.harvestYearDao().getAllYearsStatic()
        if (years.isNotEmpty()) {
            writer.write("TIPO;ID;CORRENTE\n")
            years.forEach {
                writer.write("Y;${it.id};${if (it.isCurrent) 1 else 0}\n")
            }
        }
        // Configs
        val configs = db.workerYearConfigDao().getAllConfigsStatic()
        if (configs.isNotEmpty()) {
            writer.write("TIPO;LAV_ID;ANNO_ID;TARIFFA;STRAORD;FESTIVO\n")
            configs.forEach { conf ->
                writer.write("C;${conf.workerId};${conf.harvestYearId};${conf.hourlyRate};${conf.extraHourlyRate};${conf.holidayHourlyRate}\n")
            }
        }
        // Logs
        val logs = db.workLogDao().getAllLogsStatic()
        if (logs.isNotEmpty()) {
            writer.write("TIPO;LAV_ID;ANNO_ID;DATA;M_IN;M_OUT;P_IN;P_OUT;ORE;TARIFFA;STRAORD;FESTIVO;MAN_FEST;TOTALE;ORD_H;EXT_H;HOL_H;ORD_A;EXT_A;HOL_A\n")
            logs.forEach { log ->
                val line = StringBuilder("L;${log.workerId};${log.harvestYearId};${log.date};")
                    .append("${log.morningStart ?: ""};${log.morningEnd ?: ""};")
                    .append("${log.afternoonStart ?: ""};${log.afternoonEnd ?: ""};")
                    .append("${formatDecimalHours(log.totalHours)};${log.hourlyRate};")
                    .append("${log.extraHourlyRate};${log.holidayHourlyRate};")
                    .append("${if (log.isManualHoliday) 1 else 0};${log.totalAmount};")
                    .append("${log.ordinaryHours};${log.extraHours};${log.holidayHours};")
                    .append("${log.ordinaryAmount};${log.extraAmount};${log.holidayAmount}\n")
                writer.write(line.toString())
            }
        }
        // Plantations
        val plantations = db.plantationDao().getAllPlantationsStatic()
        if (plantations.isNotEmpty()) {
            writer.write("TIPO;ID;NOME;ARCHIVIATO\n")
            plantations.forEach {
                writer.write("P;${it.id};${it.name};${if (it.isArchived) 1 else 0}\n")
            }
        }
        // Groups
        val groups = db.workerGroupDao().getAllGroupsStatic()
        if (groups.isNotEmpty()) {
            writer.write("TIPO;ID;NOME;ANNO_ID\n")
            groups.forEach {
                writer.write("G;${it.id};${it.name};${it.yearId}\n")
            }
        }
        // CrossRefs
        val crossRefs = db.workerGroupDao().getAllCrossRefsStatic()
        if (crossRefs.isNotEmpty()) {
            writer.write("TIPO;LAV_ID;GRP_ID\n")
            crossRefs.forEach {
                writer.write("X;${it.workerId};${it.groupId}\n")
            }
        }
    }

    suspend fun importFromCsv(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input)).use { reader ->
                    db.withTransaction {
                        db.clearAllTables()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val parts = line!!.split(";").map { it.trim() }
                            if (parts.isEmpty() || parts[0].startsWith("TIPO")) continue
                            when (parts[0]) {
                                "W" -> if (parts.size >= 6) db.workerDao().insertWorker(Worker(id = parts[1].toLong(), name = parts[2], surname = parts[3], phoneNumber = parts[4], isArchived = parts[5] == "1"))
                                "Y" -> if (parts.size >= 3) db.harvestYearDao().insertYear(HarvestYear(id = parts[1].toInt(), isCurrent = parts[2] == "1"))
                                "C" -> if (parts.size >= 4) {
                                    val rate = parts[3].toDoubleOrNull() ?: 0.0
                                    db.workerYearConfigDao().insertConfig(
                                        WorkerYearConfig(
                                            workerId = parts[1].toLong(),
                                            harvestYearId = parts[2].toInt(),
                                            hourlyRate = rate,
                                            extraHourlyRate = if (parts.size >= 5) parts[4].toDoubleOrNull() ?: rate else rate,
                                            holidayHourlyRate = if (parts.size >= 6) parts[5].toDoubleOrNull() ?: rate else rate
                                        )
                                    )
                                }
                                "L" -> if (parts.size >= 9) {
                                    val isLegacy = parts.size < 20
                                    
                                    val rate = if (parts.size >= 10) parts[9].toDoubleOrNull() ?: 0.0 else 0.0
                                    
                                    val extraRate: Double
                                    val holidayRate: Double
                                    val isManFest: Boolean
                                    val totalAmt: Double
                                    
                                    val ordH: Double
                                    val extH: Double
                                    val holH: Double
                                    val ordA: Double
                                    val extA: Double
                                    val holA: Double

                                    if (isLegacy) {
                                        // Legacy handling (pre-v8 breakdown or even older)
                                        val totalH = parseTimeToDouble(parts[8])
                                        extraRate = if (parts.size >= 11) parts[10].toDoubleOrNull() ?: rate else rate
                                        holidayRate = if (parts.size >= 12) parts[11].toDoubleOrNull() ?: rate else rate
                                        isManFest = if (parts.size >= 13) parts[12] == "1" else false
                                        
                                        val rawAmt = if (parts.size >= 14) parts[13].toDoubleOrNull() ?: 0.0 else 0.0
                                        totalAmt = if (rawAmt == 0.0 && totalH > 0) {
                                            totalH * (if (isManFest) holidayRate else rate)
                                        } else {
                                            rawAmt
                                        }
                                        
                                        // Simple defaults for breakdown on import legacy (consistent with MIGRATION_7_8)
                                        if (isManFest) {
                                            ordH = 0.0; extH = 0.0; holH = totalH
                                            ordA = 0.0; extA = 0.0; holA = totalAmt
                                        } else {
                                            ordH = totalH; extH = 0.0; holH = 0.0
                                            ordA = totalAmt; extA = 0.0; holA = 0.0
                                        }
                                    } else {
                                        extraRate = parts[10].toDoubleOrNull() ?: rate
                                        holidayRate = parts[11].toDoubleOrNull() ?: rate
                                        isManFest = parts[12] == "1"
                                        totalAmt = parts[13].toDoubleOrNull() ?: 0.0
                                        
                                        ordH = parts[14].toDoubleOrNull() ?: 0.0
                                        extH = parts[15].toDoubleOrNull() ?: 0.0
                                        holH = parts[16].toDoubleOrNull() ?: 0.0
                                        ordA = parts[17].toDoubleOrNull() ?: 0.0
                                        extA = parts[18].toDoubleOrNull() ?: 0.0
                                        holA = parts[19].toDoubleOrNull() ?: 0.0
                                    }

                                    db.workLogDao().insertLog(
                                        WorkLog(
                                            workerId = parts[1].toLong(),
                                            harvestYearId = parts[2].toInt(),
                                            date = parts[3].toLong(),
                                            morningStart = parts[4].ifBlank { null },
                                            morningEnd = parts[5].ifBlank { null },
                                            afternoonStart = parts[6].ifBlank { null },
                                            afternoonEnd = parts[7].ifBlank { null },
                                            totalHours = parseTimeToDouble(parts[8]),
                                            hourlyRate = rate,
                                            extraHourlyRate = extraRate,
                                            holidayHourlyRate = holidayRate,
                                            isManualHoliday = isManFest,
                                            totalAmount = totalAmt,
                                            ordinaryHours = ordH,
                                            extraHours = extH,
                                            holidayHours = holH,
                                            ordinaryAmount = ordA,
                                            extraAmount = extA,
                                            holidayAmount = holA
                                        )
                                    )
                                }
                                "P" -> if (parts.size >= 4) db.plantationDao().insertPlantation(Plantation(id = parts[1].toLong(), name = parts[2], isArchived = parts[3] == "1"))
                                "G" -> if (parts.size >= 4) db.workerGroupDao().insertGroup(WorkerGroup(id = parts[1].toLong(), name = parts[2], yearId = parts[3].toInt()))
                                "X" -> if (parts.size >= 3) db.workerGroupDao().insertWorkerToGroup(WorkerGroupCrossRef(workerId = parts[1].toLong(), groupId = parts[2].toLong()))
                            }
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e("CsvUtils", "Errore importazione CSV", e)
            false
        }
    }
}
