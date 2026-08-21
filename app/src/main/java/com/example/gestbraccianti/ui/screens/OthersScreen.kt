package com.example.gestbraccianti.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.example.gestbraccianti.R
import com.example.gestbraccianti.data.utils.CsvUtils
import com.example.gestbraccianti.ui.utils.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.util.*

@Composable
fun OthersScreen(
    workerViewModel: com.example.gestbraccianti.ui.viewmodel.WorkerViewModel,
    yearId: Int
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var backupFiles by remember { mutableStateOf(emptyList<File>()) }
    
    val prefs = remember { context.getSharedPreferences("owner_prefs", Context.MODE_PRIVATE) }
    var ownerName by remember { mutableStateOf(prefs.getString("owner_name", "") ?: "") }
    var ownerSurname by remember { mutableStateOf(prefs.getString("owner_surname", "") ?: "") }
    var ownerPhone by remember { mutableStateOf(prefs.getString("owner_phone", "") ?: "") }

    var extraHoursThreshold by remember { 
        mutableStateOf(prefs.getFloat("extra_hours_threshold", 8.0f).toString().replace(".", ",")) 
    }
    var festiveDaysType by remember { 
        mutableIntStateOf(prefs.getInt("festive_days_type", 0)) 
    }

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
                        val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
                        val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)
                        val hasPhoneIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER)

                        val contactId = cursor.getString(idIndex)
                        val displayName = cursor.getString(nameIndex)
                        val hasPhone = cursor.getInt(hasPhoneIndex) > 0

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
                                    val givenNameIndex = nameCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME)
                                    val familyNameIndex = nameCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME)
                                    ownerName = nameCursor.getString(givenNameIndex) ?: ""
                                    ownerSurname = nameCursor.getString(familyNameIndex) ?: ""
                                } else {
                                    val parts = displayName.split(" ", limit = 2)
                                    ownerName = parts.getOrNull(0) ?: ""
                                    ownerSurname = parts.getOrNull(1) ?: ""
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("OthersScreen", "Errore nel recupero nome strutturato", e)
                            val parts = displayName.split(" ", limit = 2)
                            ownerName = parts.getOrNull(0) ?: ""
                            ownerSurname = parts.getOrNull(1) ?: ""
                        }
                        
                        prefs.edit {
                            putString("owner_name", ownerName)
                            putString("owner_surname", ownerSurname)
                            putString("owner_phone", ownerPhone)
                        }
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
                    val success = CsvUtils.exportToCsv(context, destUri)
                    if (success) {
                        withContext(Dispatchers.IO) {
                            try {
                                val timestamp = TimeUtils.fileTimestampFormatter.format(Date())
                                val internalBackupDir = File(context.getExternalFilesDir(null), "backups")
                                if (!internalBackupDir.exists()) internalBackupDir.mkdirs()
                                val internalFile = File(internalBackupDir, "GestBraccianti_Bkp_$timestamp.csv")
                                
                                context.contentResolver.openInputStream(destUri)?.use { input ->
                                    FileOutputStream(internalFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("OthersScreen", "Errore salvataggio backup interno", e)
                            }
                        }
                        Toast.makeText(context, context.getString(R.string.toast_exported), Toast.LENGTH_SHORT).show()
                        refreshBackupList()
                    }
                }
            }
        }
    )

    var importUri by remember { mutableStateOf<Uri?>(null) }
    var showImportConfirmation by remember { mutableStateOf(false) }
    var importDateStr by remember { mutableStateOf("") }

    fun prepareImport(uri: Uri, fileName: String) {
        val regex = Regex("(\\d{8})_\\d{4}")
        val match = regex.find(fileName)
        
        val isRecognized = fileName.startsWith("GestBraccianti_Bkp_", ignoreCase = true) || 
                          fileName.startsWith("gest_braccianti_", ignoreCase = true) || 
                          fileName.startsWith("backup_", ignoreCase = true)

        if (isRecognized) {
            if (match != null) {
                val datePart = match.groupValues[1]
                try {
                    val date = TimeUtils.yearFormatter.apply { applyPattern("yyyyMMdd") }.parse(datePart)
                    importDateStr = if (date != null) TimeUtils.dateFormatter.format(date) else "data sconosciuta"
                } catch (e: Exception) {
                    importDateStr = "data non valida"
                }
            } else {
                importDateStr = "data non rilevata"
            }
            importUri = uri
            showImportConfirmation = true
        } else {
            Toast.makeText(context, context.getString(R.string.toast_invalid_file), Toast.LENGTH_LONG).show()
        }
    }

    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                val fileName = getFileName(context, it) ?: ""
                prepareImport(it, fileName)
            }
        }
    )

    if (showImportConfirmation && importUri != null) {
        AlertDialog(
            onDismissRequest = { showImportConfirmation = false },
            title = { Text(stringResource(R.string.confirm_import_title)) },
            text = { Text(stringResource(R.string.confirm_import_text, importDateStr)) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            // Auto-backup before import
                            CsvUtils.createInternalBackup(context)

                            val success = CsvUtils.importFromCsv(context, importUri!!)
                            if (success) {
                                Toast.makeText(context, context.getString(R.string.toast_imported), Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, context.getString(R.string.toast_import_error), Toast.LENGTH_SHORT).show()
                            }
                            showImportConfirmation = false
                            importUri = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.confirm_btn)) }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showImportConfirmation = false 
                    importUri = null
                }) { Text(stringResource(R.string.cancel_btn)) }
            }
        )
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.screen_others_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        val isOwner = remember(ownerPhone, ownerName, ownerSurname) {
            val phone = ownerPhone.replace("+39", "").replace(" ", "")
            val s = ownerSurname.trim().lowercase()
            val n = ownerName.trim().lowercase()
            val full = "$s $n".trim()
            
            s == "x" || 
            phone == "3286449326" || 
            full == "gatti marco" || 
            full == "marco gatti" || 
            full == "marco cell" || 
            full == "cell marco" ||
            full.contains("marcogatti") ||
            full.contains("marcocell")
        }

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(stringResource(R.string.tab_data)) })
            if (isOwner) {
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(stringResource(R.string.tab_test)) })
            }
        }

        val currentTab = if (selectedTab == 1 && !isOwner) 0 else selectedTab

        when (currentTab) {
            0 -> {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.owner_card_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
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
                                    Icon(Icons.Default.ContactPage, contentDescription = stringResource(R.string.import_contacts_desc))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = ownerSurname,
                                    onValueChange = {
                                        ownerSurname = it
                                        prefs.edit { putString("owner_surname", it) }
                                    },
                                    label = { Text(stringResource(R.string.owner_surname_label), style = MaterialTheme.typography.labelLarge) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = ownerName,
                                    onValueChange = {
                                        ownerName = it
                                        prefs.edit { putString("owner_name", it) }
                                    },
                                    label = { Text(stringResource(R.string.owner_name_label), style = MaterialTheme.typography.labelLarge) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.settings_plant_title), style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            OutlinedTextField(
                                value = extraHoursThreshold,
                                onValueChange = { input ->
                                    if (input.isEmpty() || input.matches(Regex("""^\d*[.,]?\d{0,1}$"""))) {
                                        extraHoursThreshold = input.replace('.', ',')
                                        val value = extraHoursThreshold.replace(',', '.').toFloatOrNull() ?: 8.0f
                                        prefs.edit { putFloat("extra_hours_threshold", value) }
                                    }
                                },
                                label = { Text(stringResource(R.string.settings_extra_threshold_label)) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                                ),
                                singleLine = true
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = stringResource(R.string.settings_festive_days_label),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val options = listOf(
                                stringResource(R.string.settings_festive_none),
                                stringResource(R.string.settings_festive_saturday),
                                stringResource(R.string.settings_festive_sunday),
                                stringResource(R.string.settings_festive_sat_sun)
                            )
                            
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                options.forEachIndexed { index, label ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { 
                                                festiveDaysType = index
                                                prefs.edit { putInt("festive_days_type", index) }
                                            }
                                    ) {
                                        RadioButton(
                                            selected = festiveDaysType == index,
                                            onClick = {
                                                festiveDaysType = index
                                                prefs.edit { putInt("festive_days_type", index) }
                                            }
                                        )
                                        Text(text = label, style = MaterialTheme.typography.bodyLarge)
                                    }
                                }
                            }
                        }
                    }

                    DatabaseTab(
                        onExport = {
                            val timestamp = TimeUtils.fileTimestampFormatter.format(Date())
                            csvExportLauncher.launch("GestBraccianti_Bkp_$timestamp.csv")
                        },
                        onImport = { csvImportLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*")) },
                        onRestore = { file -> prepareImport(Uri.fromFile(file), file.name) },
                        backupFiles = backupFiles,
                        onRefresh = { refreshBackupList() }
                    )
                }
            }
            1 -> {
                TestTab(workerViewModel, yearId, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun TestTab(
    workerViewModel: com.example.gestbraccianti.ui.viewmodel.WorkerViewModel?,
    yearId: Int,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(stringResource(R.string.test_area_title), style = MaterialTheme.typography.titleMedium)

        Button(
            onClick = {
                scope.launch {
                        for (i in 1..10) {
                        workerViewModel?.addWorkerToYear(
                            name = "Bracciante",
                            surname = "$i",
                            phoneNumber = "331000000$i",
                            hourlyRate = 10.0,
                            extraRate = 12.0,
                            holidayRate = 15.0,
                            yearId = yearId
                        )
                    }
                    Toast.makeText(context, context.getString(R.string.toast_test_workers_created), Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.btn_create_test_workers))
        }

        Text(
            stringResource(R.string.test_area_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun DatabaseTab(
    onExport: () -> Unit,
    onImport: () -> Unit,
    onRestore: (File) -> Unit,
    backupFiles: List<File>,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.csv_card_title), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.csv_card_desc), style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onExport, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.btn_export))
                    }
                    OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.btn_import))
                    }
                }
            }
        }

        Text(stringResource(R.string.internal_backups_title), style = MaterialTheme.typography.titleMedium)
        
        if (backupFiles.isEmpty()) {
            Text(stringResource(R.string.no_backups_msg), style = MaterialTheme.typography.bodySmall)
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(backupFiles, key = { it.absolutePath }) { file ->
                    BackupFileItem(
                        file = file,
                        onShare = { shareFile(context, file) },
                        onDelete = {
                            file.delete()
                            onRefresh()
                        },
                        onRestore = { onRestore(file) }
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
                    TimeUtils.dateTimeFormatter.format(Date(file.lastModified())),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share_desc))
            }
            IconButton(onClick = onRestore) {
                Icon(Icons.Default.FileDownload, contentDescription = stringResource(R.string.restore_desc))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_desc), tint = MaterialTheme.colorScheme.error)
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
    context.startActivity(android.content.Intent.createChooser(intent, context.getString(R.string.share_backup_title)))
}

fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}
