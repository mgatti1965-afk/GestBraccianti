package com.example.gestbraccianti.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.gestbraccianti.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import android.os.Environment
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OthersScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var backupFiles by remember { mutableStateOf(emptyList<File>()) }

    fun refreshBackupList() {
        val backupDir = File(context.getExternalFilesDir(null), "backups")
        if (!backupDir.exists()) backupDir.mkdirs()
        backupFiles = backupDir.listFiles()?.filter { it.extension == "db" }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    LaunchedEffect(Unit) {
        refreshBackupList()
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
        onResult = { uri ->
            uri?.let { destUri ->
                // First save internally to keep track, then copy to destination
                scope.launch(Dispatchers.IO) {
                    val dbFile = context.getDatabasePath("gest_braccianti_db")
                    val sdf = SimpleDateFormat("yyyyMMdd_HHmm", Locale.ITALY)
                    val timestamp = sdf.format(Date())
                    val internalBackupDir = File(context.getExternalFilesDir(null), "backups")
                    if (!internalBackupDir.exists()) internalBackupDir.mkdirs()
                    val internalFile = File(internalBackupDir, "backup_$timestamp.db")
                    
                    try {
                        // Save internal copy
                        FileInputStream(dbFile).use { input ->
                            FileOutputStream(internalFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        
                        // Copy to selected destination (URI)
                        context.contentResolver.openOutputStream(destUri)?.use { outputStream ->
                            FileInputStream(dbFile).use { inputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }

                        // Also save to Downloads folder using MediaStore
                        saveToDownloads(context, dbFile, "gest_braccianti_backup_$timestamp.db")

                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Export completato (Interno + Downloads)!", Toast.LENGTH_SHORT).show()
                            refreshBackupList()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    val success = importDatabase(context, it)
                    if (success) {
                        Toast.makeText(context, "Database importato! Riavvia l'app.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Errore durante l'importazione.", Toast.LENGTH_SHORT).show()
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
        Text("Gestione Dati", style = MaterialTheme.typography.headlineSmall)
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Backup e Ripristino", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Esporta i tuoi dati in un file per sicurezza o importali da un backup precedente.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val sdf = SimpleDateFormat("yyyyMMdd_HHmm", Locale.ITALY)
                            val timestamp = sdf.format(Date())
                            exportLauncher.launch("gest_braccianti_backup_$timestamp.db")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Esporta")
                    }
                    
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/octet-stream", "*/*")) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Importa")
                    }
                }
            }
        }

        Text("Storico Backup Interni", style = MaterialTheme.typography.titleMedium)
        
        if (backupFiles.isEmpty()) {
            Text("Nessun backup salvato internamente.", style = MaterialTheme.typography.bodySmall)
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
                                val success = importDatabase(context, Uri.fromFile(file))
                                if (success) {
                                    Toast.makeText(context, "Ripristino completato! Riavvia l'app.", Toast.LENGTH_LONG).show()
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
        type = "application/octet-stream"
        putExtra(android.content.Intent.EXTRA_STREAM, uri)
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(android.content.Intent.createChooser(intent, "Invia Backup"))
}

fun saveToDownloads(context: Context, sourceFile: File, fileName: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                FileInputStream(sourceFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
    } else {
        // Fallback for older versions: use the public downloads directory directly
        // Note: This requires WRITE_EXTERNAL_STORAGE permission on API < 29
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val destFile = File(downloadsDir, fileName)
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // If it fails (e.g. permission denied), the user still has the FilePicker export
        }
    }
}


fun exportDatabase(context: Context, destinationUri: Uri) {
    val dbFile = context.getDatabasePath("gest_braccianti_db")
    try {
        context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
            FileInputStream(dbFile).use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        Toast.makeText(context, "Database esportato con successo!", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

suspend fun importDatabase(context: Context, sourceUri: Uri): Boolean = withContext(Dispatchers.IO) {
    val dbFile = context.getDatabasePath("gest_braccianti_db")
    val dbShm = File(dbFile.path + "-shm")
    val dbWal = File(dbFile.path + "-wal")

    try {
        // Chiudi il database prima dell'importazione
        AppDatabase.getDatabase(context).close()

        if (sourceUri.scheme == "file") {
            val sourceFile = File(sourceUri.path!!)
            FileInputStream(sourceFile).use { inputStream ->
                FileOutputStream(dbFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } else {
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(dbFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
        // Elimina i file temporanei del database per forzare il ricaricamento
        if (dbShm.exists()) dbShm.delete()
        if (dbWal.exists()) dbWal.delete()
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
