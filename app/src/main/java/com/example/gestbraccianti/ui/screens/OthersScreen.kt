package com.example.gestbraccianti.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.room.withTransaction
import com.example.gestbraccianti.data.AppDatabase
import com.example.gestbraccianti.data.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OthersScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var backupFiles by remember { mutableStateOf(emptyList<File>()) }
    
    // Gestione dati proprietario con SharedPreferences per semplicità
    val prefs = remember { context.getSharedPreferences("owner_prefs", Context.MODE_PRIVATE) }
    var ownerName by remember { mutableStateOf(prefs.getString("owner_name", "") ?: "") }
    var ownerSurname by remember { mutableStateOf(prefs.getString("owner_surname", "") ?: "") }
    var ownerPhone by remember { mutableStateOf(prefs.getString("owner_phone", "") ?: "") }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact(),
        onResult = { uri ->
            uri?.let { contactUri ->
                val projection = arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.HAS_PHONE_NUMBER
                )
                context.contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                        val displayName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
                        val hasPhone = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0

                        if (hasPhone) {
                            val phoneCursor = context.contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                                arrayOf(contactId),
                                null
                            )
                            phoneCursor?.use { pc ->
                                if (pc.moveToFirst()) {
                                    ownerPhone = pc.getString(0).replace(" ", "").replace("-", "")
                                }
                            }
                        }
                        
                        val nameProjection = arrayOf(
                            ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                            ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME
                        )
                        val where = "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
                        val args = arrayOf(contactId, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)

                        try {
                            context.contentResolver.query(ContactsContract.Data.CONTENT_URI, nameProjection, where, args, null)?.use { nameCursor ->
                                if (nameCursor.moveToFirst()) {
                                    ownerName = nameCursor.getString(nameCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME)) ?: ""
                                    ownerSurname = nameCursor.getString(nameCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME)) ?: ""
                                } else {
                                    val parts = displayName.split(" ", limit = 2)
                                    ownerName = parts.getOrNull(0) ?: ""
                                    ownerSurname = parts.getOrNull(1) ?: ""
                                }
                            }
                        } catch (e: Exception) {
                            val parts = displayName.split(" ", limit = 2)
                            ownerName = parts.getOrNull(0) ?: ""
                            ownerSurname = parts.getOrNull(1) ?: ""
                        }
                        
                        // Salva nelle preferenze
                        prefs.edit()
                            .putString("owner_name", ownerName)
                            .putString("owner_surname", ownerSurname)
                            .putString("owner_phone", ownerPhone)
                            .apply()
                    }
                }
            }
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                contactPickerLauncher.launch(null)
            }
        }
    )

    fun refreshBackupList() {
        val backupDir = File(context.getExternalFilesDir(null), "backups")
        if (!backupDir.exists()) backupDir.mkdirs()
        backupFiles = backupDir.listFiles()?.filter { it.extension == "csv" || it.extension == "txt" }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    LaunchedEffect(Unit) {
        refreshBackupList()
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri ->
            uri?.let { destUri ->
                scope.launch {
                    val success = exportToCsv(context, destUri)
                    if (success) {
                        // Salva anche una copia interna per la cronologia
                        withContext(Dispatchers.IO) {
                            try {
                                val sdf = SimpleDateFormat("yyyyMMdd_HHmm", Locale.ITALY)
                                val timestamp = sdf.format(Date())
                                val internalBackupDir = File(context.getExternalFilesDir(null), "backups")
                                if (!internalBackupDir.exists()) internalBackupDir.mkdirs()
                                val internalFile = File(internalBackupDir, "backup_$timestamp.csv")
                                
                                context.contentResolver.openInputStream(destUri)?.use { input ->
                                    FileOutputStream(internalFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        Toast.makeText(context, "Dati esportati!", Toast.LENGTH_SHORT).show()
                        refreshBackupList()
                    }
                }
            }
        }
    )

    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    val success = importFromCsv(context, it)
                    if (success) {
                        Toast.makeText(context, "Dati importati!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Errore durante l'importazione del file di testo.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Impostazioni e Dati", style = MaterialTheme.typography.headlineSmall)

        // Sezione Proprietario
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Proprietario", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        when (PackageManager.PERMISSION_GRANTED) {
                            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) -> {
                                contactPickerLauncher.launch(null)
                            }
                            else -> {
                                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        }
                    }) {
                        Icon(Icons.Default.ContactPage, contentDescription = "Importa da Contatti")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ownerSurname,
                        onValueChange = { 
                            ownerSurname = it
                            prefs.edit().putString("owner_surname", it).apply()
                        },
                        label = { Text("Cognome (Obbligatorio)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { 
                            ownerName = it
                            prefs.edit().putString("owner_name", it).apply()
                        },
                        label = { Text("Nome") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        }

        Text("Gestione Database", style = MaterialTheme.typography.titleMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Esportazione e Backup (CSV)", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Formato leggibile compatibile con Excel o Blocco Note. I dati vengono salvati anche nella cronologia interna.", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val sdf = SimpleDateFormat("yyyyMMdd_HHmm", Locale.ITALY)
                            val timestamp = sdf.format(Date())
                            csvExportLauncher.launch("gest_braccianti_$timestamp.csv")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Esporta CSV")
                    }
                    
                    OutlinedButton(
                        onClick = { csvImportLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*")) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Importa CSV")
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Strumenti di Test", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Simula la ricezione di SMS dai lavoratori per testare l'importazione automatica.", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                val count = simulateSmsReception(context)
                                if (count > 0) {
                                    Toast.makeText(context, "Simulati $count SMS per oggi!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Nessun lavoratore trovato con numero di telefono.", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Simula SMS Oggi")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    AppDatabase.getDatabase(context).mockSmsDao().deleteAll()
                                }
                                Toast.makeText(context, "Mock SMS eliminati.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Pulisci Mock")
                    }
                }
            }
        }

        Text("Cronologia Backup CSV", style = MaterialTheme.typography.titleMedium)
        
        if (backupFiles.isEmpty()) {
            Text("Nessun backup CSV salvato internamente.", style = MaterialTheme.typography.bodySmall)
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(backupFiles) { file ->
                    BackupFileItem(
                        file = file,
                        onShare = { shareFile(context, file) },
                        onDelete = {
                            file.delete()
                            refreshBackupList()
                        },
                        onRestore = {
                            scope.launch {
                                val success = importFromCsv(context, Uri.fromFile(file))
                                if (success) {
                                    Toast.makeText(context, "Dati ripristinati con successo!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Errore durante il ripristino.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BackupFileItem(
    file: File,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(file.name, style = MaterialTheme.typography.bodyMedium)
                Text(
                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY).format(Date(file.lastModified())),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = "Condividi")
            }
            IconButton(onClick = onRestore) {
                Icon(Icons.Default.FileDownload, contentDescription = "Ripristina")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

fun shareFile(context: Context, file: File) {
    val uri = androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_STREAM, uri)
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(android.content.Intent.createChooser(intent, "Invia Backup"))
}

suspend fun simulateSmsReception(context: Context): Int = withContext(Dispatchers.IO) {
    val db = AppDatabase.getDatabase(context)
    val workers = db.workerDao().getAllWorkersStatic().filter { it.phoneNumber.isNotBlank() }
    if (workers.isEmpty()) return@withContext 0

    val now = Calendar.getInstance()
    val random = Random()
    var smsCount = 0

    workers.forEach { worker ->
        // Simulate Inizio
        if (random.nextBoolean()) {
            val cal = now.clone() as Calendar
            val hour = if (random.nextBoolean()) 8 else 13
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, random.nextInt(15)) // 0-14 min delay
            
            db.mockSmsDao().insert(MockSms(
                address = worker.phoneNumber,
                body = "Inizio",
                date = cal.timeInMillis
            ))
            smsCount++
        }

        // Simulate Fine
        if (random.nextBoolean()) {
            val cal = now.clone() as Calendar
            val hour = if (random.nextBoolean()) 12 else 17
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, random.nextInt(15))
            
            db.mockSmsDao().insert(MockSms(
                address = worker.phoneNumber,
                body = "Fine",
                date = cal.timeInMillis
            ))
            smsCount++
        }
    }
    smsCount
}

suspend fun exportToCsv(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
    val db = AppDatabase.getDatabase(context)
    try {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            OutputStreamWriter(output).use { writer ->
                // Workers
                writer.write("TIPO;ID;NOME;COGNOME;TELEFONO;ARCHIVIATO\n")
                db.workerDao().getAllWorkersStatic().forEach {
                    writer.write("W;${it.id};${it.name};${it.surname};${it.phoneNumber};${if (it.isArchived) 1 else 0}\n")
                }
                // Years
                writer.write("TIPO;ID;CORRENTE\n")
                db.harvestYearDao().getAllYearsStatic().forEach {
                    writer.write("Y;${it.id};${if (it.isCurrent) 1 else 0}\n")
                }
                // Configs
                writer.write("TIPO;LAV_ID;ANNO_ID;TARIFFA\n")
                db.workerYearConfigDao().getAllConfigsStatic().forEach { conf ->
                    writer.write("C;${conf.workerId};${conf.harvestYearId};${conf.hourlyRate}\n")
                }
                // Logs
                writer.write("TIPO;LAV_ID;ANNO_ID;DATA;M_IN;M_OUT;P_IN;P_OUT;ORE\n")
                db.workLogDao().getAllLogsStatic().forEach { log ->
                    writer.write("L;${log.workerId};${log.harvestYearId};${log.date};${log.morningStart ?: ""};${log.morningEnd ?: ""};${log.afternoonStart ?: ""};${log.afternoonEnd ?: ""};${log.totalHours}\n")
                }
                // Plantations
                writer.write("TIPO;ID;NOME;ARCHIVIATO\n")
                db.plantationDao().getAllPlantationsStatic().forEach {
                    writer.write("P;${it.id};${it.name};${if (it.isArchived) 1 else 0}\n")
                }
                // Groups
                writer.write("TIPO;ID;NOME;ANNO_ID\n")
                db.workerGroupDao().getAllGroupsStatic().forEach {
                    writer.write("G;${it.id};${it.name};${it.yearId}\n")
                }
                // CrossRefs
                writer.write("TIPO;LAV_ID;GRP_ID\n")
                db.workerGroupDao().getAllCrossRefsStatic().forEach {
                    writer.write("X;${it.workerId};${it.groupId}\n")
                }
            }
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
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
                        val parts = line!!.split(";")
                        if (parts.isEmpty()) continue
                        when (parts[0]) {
                            "W" -> if (parts.size >= 6) db.workerDao().insertWorker(Worker(id = parts[1].toLong(), name = parts[2], surname = parts[3], phoneNumber = parts[4], isArchived = parts[5] == "1"))
                            "Y" -> if (parts.size >= 3) db.harvestYearDao().insertYear(HarvestYear(id = parts[1].toInt(), isCurrent = parts[2] == "1"))
                            "C" -> if (parts.size >= 4) db.workerYearConfigDao().insertConfig(WorkerYearConfig(workerId = parts[1].toLong(), harvestYearId = parts[2].toInt(), hourlyRate = parts[3].toDouble()))
                            "L" -> if (parts.size >= 9) db.workLogDao().insertLog(WorkLog(workerId = parts[1].toLong(), harvestYearId = parts[2].toInt(), date = parts[3].toLong(), morningStart = parts[4].ifBlank { null }, morningEnd = parts[5].ifBlank { null }, afternoonStart = parts[6].ifBlank { null }, afternoonEnd = parts[7].ifBlank { null }, totalHours = parts[8].toDouble()))
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
        e.printStackTrace()
        false
    }
}
