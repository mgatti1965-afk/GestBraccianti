package com.example.gestbraccianti.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import android.widget.Toast
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.stringResource
import com.example.gestbraccianti.R
import com.example.gestbraccianti.data.entity.Worker
import com.example.gestbraccianti.data.entity.WorkerGroup
import com.example.gestbraccianti.ui.utils.formatCurrency
import com.example.gestbraccianti.ui.utils.formatDecimal
import com.example.gestbraccianti.ui.viewmodel.WorkerGroupViewModel
import com.example.gestbraccianti.ui.viewmodel.WorkerViewModel
import java.util.Locale

@Composable
fun WorkerRegistryScreen(
    workerViewModel: WorkerViewModel,
    groupViewModel: WorkerGroupViewModel,
    yearId: Int
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(stringResource(R.string.tab_workers), stringResource(R.string.tab_groups))

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTab) {
            0 -> WorkerListTab(workerViewModel, yearId)
            1 -> GroupListTab(groupViewModel, workerViewModel, yearId)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WorkerListTab(viewModel: WorkerViewModel, yearId: Int) {
    val context = LocalContext.current
    val workersWithRate by viewModel.workersWithRateForCurrentYear.collectAsState()
    val duplicates by viewModel.duplicatesFound.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    var showDialog by remember { mutableStateOf(false) }
    var selectedWorker by remember { mutableStateOf<Worker?>(null) }
    var currentRates by remember { mutableStateOf(Triple(0.0, 0.0, 0.0)) }
    var searchQuery by remember { mutableStateOf("") }
    
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val filteredWorkers = remember(workersWithRate, searchQuery) {
        if (searchQuery.isBlank()) workersWithRate
        else workersWithRate.filter {
            it.worker.name.contains(searchQuery, ignoreCase = true) ||
                    it.worker.surname.contains(searchQuery, ignoreCase = true) ||
                    it.worker.phoneNumber.contains(searchQuery)
        }
    }

    if (showDialog) {
        AddEditWorkerDialog(
            worker = selectedWorker,
            initialRates = currentRates,
            onDismiss = { showDialog = false },
            onConfirm = { name, surname, phone, rate, extraRate, holidayRate ->
                val isDuplicate = workersWithRate.any {
                    it.worker.surname.equals(surname, ignoreCase = true) &&
                            it.worker.name.equals(name, ignoreCase = true) &&
                            it.worker.id != selectedWorker?.id
                }

                if (isDuplicate) {
                    Toast.makeText(context, context.getString(R.string.error_duplicate_worker), Toast.LENGTH_SHORT).show()
                } else {
                    if (selectedWorker == null) {
                        viewModel.addWorkerToYear(name, surname, phone, rate, extraRate, holidayRate, yearId)
                    } else {
                        viewModel.updateWorkerInfo(selectedWorker!!.id, name, surname, phone, yearId, rate, extraRate, holidayRate)
                    }
                    showDialog = false
                }
            },
            onDelete = {
                showDeleteConfirm = true
                showDialog = false
            }
        )
    }

    if (showDeleteConfirm && selectedWorker != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_worker_confirm_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.delete_worker_confirm_msg, "${selectedWorker!!.surname} ${selectedWorker!!.name}")) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteWorker(selectedWorker!!)
                        showDeleteConfirm = false
                        selectedWorker = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.confirm_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel_btn))
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (duplicates.isNotEmpty()) {
                duplicates.forEach { group ->
                    DuplicatesAlert(
                        duplicateGroup = group,
                        onMerge = {
                            viewModel.mergeDuplicateGroup(group)
                        }
                    )
                }
            }

            if (workersWithRate.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text(stringResource(R.string.search_worker_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_search_desc))
                            }
                        }
                    },
                    singleLine = true
                )
            }

            if (workersWithRate.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.no_workers_msg))
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.copyWorkersFromPreviousYear(yearId) { count ->
                                    Toast.makeText(context, context.getString(R.string.toast_workers_copied, count), Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.copy_prev_year_btn))
                        }
                    }
                }
            } else if (filteredWorkers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_search_results, searchQuery))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredWorkers, key = { it.worker.id }) { item ->
                        val worker = item.worker
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                                .clickable {
                                    selectedWorker = worker
                                    currentRates = Triple(item.hourlyRate, item.extraHourlyRate, item.holidayHourlyRate)
                                    showDialog = true
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar with initial
                                Surface(
                                    modifier = Modifier.size(48.dp),
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = worker.surname.take(1).uppercase(),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${worker.surname} ${worker.name}".trim(),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val rateStyle = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(text = "Ord: ${formatCurrency(item.hourlyRate)}", style = rateStyle)
                                        Text(text = "Str: ${formatCurrency(item.extraHourlyRate)}", style = rateStyle)
                                        Text(text = "Fes: ${formatCurrency(item.holidayHourlyRate)}", style = rateStyle)
                                    }
                                }
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.edit_desc),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { selectedWorker = null; currentRates = Triple(0.0, 0.0, 0.0); showDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_worker_desc)) }

    }
}

@Composable
fun GroupListTab(groupViewModel: WorkerGroupViewModel, workerViewModel: WorkerViewModel, yearId: Int) {
    val context = LocalContext.current
    val groups by groupViewModel.groupsForYear.collectAsState()
    val allWorkers by workerViewModel.workersForCurrentYear.collectAsState()
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var groupToEditMembers by remember { mutableStateOf<WorkerGroup?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (groups.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.no_groups_msg))
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            groupViewModel.copyGroupsFromPreviousYear(yearId) { count ->
                                Toast.makeText(context, context.getString(R.string.toast_groups_copied, count), Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.copy_prev_year_btn))
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(), 
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp), 
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(groups, key = { it.id }) { group ->
                    val members by groupViewModel.getWorkersInGroup(group.id).collectAsState(initial = emptyList())
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { groupToEditMembers = group }
                            .padding(horizontal = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = group.name, 
                                    style = MaterialTheme.typography.titleLarge, 
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { groupViewModel.deleteGroup(group) }) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_desc), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            Text(
                                text = stringResource(R.string.group_members_count, members.size), 
                                style = MaterialTheme.typography.titleMedium, 
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            if (members.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.group_members_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                // Mostriamo i membri in modo più visibile
                                members.sortedWith(compareBy({ it.surname }, { it.name })).chunked(2).forEach { rowMembers ->
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        rowMembers.forEach { member ->
                                            Text(
                                                text = "• ${member.surname} ${member.name}".trim(),
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1
                                            )
                                        }
                                        if (rowMembers.size == 1) Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { showAddGroupDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_group_desc)) }

        if (groups.isNotEmpty()) {
            SmallFloatingActionButton(
                onClick = {
                    groupViewModel.copyGroupsFromPreviousYear(yearId) { count ->
                        if (count > 0) {
                            Toast.makeText(context, context.getString(R.string.toast_new_groups_copied, count), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, context.getString(R.string.toast_no_new_groups), Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 80.dp, end = 16.dp),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.copy_prev_year_btn))
            }
        }
    }

    if (showAddGroupDialog) {
        var groupName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddGroupDialog = false },
            title = { Text(stringResource(R.string.new_group_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
            text = { 
                TextField(
                    value = groupName, 
                    onValueChange = { groupName = it }, 
                    label = { Text(stringResource(R.string.group_name_label)) },
                    textStyle = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.fillMaxWidth()
                ) 
            },
            confirmButton = {
                Button(onClick = {
                    if (groupName.isNotBlank()) {
                        groupViewModel.createGroup(groupName)
                        showAddGroupDialog = false
                    }
                }) { Text(stringResource(R.string.btn_create)) }
            },
            dismissButton = { TextButton(onClick = { showAddGroupDialog = false }) { Text(stringResource(R.string.cancel_btn)) } }
        )
    }

    if (groupToEditMembers != null) {
        val group = groupToEditMembers!!
        val members by groupViewModel.getWorkersInGroup(group.id).collectAsState(initial = emptyList())
        AlertDialog(
            onDismissRequest = { groupToEditMembers = null },
            title = { Text(stringResource(R.string.group_members_dialog_title, group.name), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 500.dp)) {
                    items(allWorkers, key = { it.id }) { worker ->
                        val isMember = members.any { it.id == worker.id }
                        Row(
                            verticalAlignment = Alignment.CenterVertically, 
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isMember) groupViewModel.removeWorkerFromGroup(worker.id, group.id)
                                    else groupViewModel.addWorkerToGroup(worker.id, group.id)
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            Checkbox(
                                checked = isMember, 
                                onCheckedChange = null,
                                modifier = Modifier.scale(1.5f)
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = "${worker.surname} ${worker.name}".trim(),
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { groupToEditMembers = null }) { Text(stringResource(R.string.btn_close)) } }
        )
    }
}

@Composable
fun DuplicatesAlert(
    duplicateGroup: List<Worker>,
    onMerge: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Delete, contentDescription = null) // Using Delete as an indicator of cleanup
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.duplicates_alert_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${duplicateGroup.first().surname} ${duplicateGroup.first().name}: " + 
                       stringResource(R.string.duplicates_alert_msg),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onMerge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.btn_merge_now))
            }
        }
    }
}

@Composable
fun AddEditWorkerDialog(
    worker: Worker?,
    initialRates: Triple<Double, Double, Double>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Double, Double, Double) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember(worker) { mutableStateOf(worker?.name ?: "") }
    var surname by remember(worker) { mutableStateOf(worker?.surname ?: "") }
    var phoneNumber by remember(worker) { mutableStateOf(worker?.phoneNumber ?: "") }
    var rate by remember(initialRates.first) { mutableStateOf(if (initialRates.first > 0) formatDecimal(initialRates.first) else "") }
    var extraRate by remember(initialRates.second) { mutableStateOf(if (initialRates.second > 0) formatDecimal(initialRates.second) else "") }
    var holidayRate by remember(initialRates.third) { mutableStateOf(if (initialRates.third > 0) formatDecimal(initialRates.third) else "") }
    val context = LocalContext.current

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
                                    phoneNumber = pc.getString(0).replace(" ", "").replace("-", "")
                                }
                            }
                        }

                        // Try to get structured name (Given Name and Family Name)
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
                                    name = nameCursor.getString(givenNameIndex) ?: ""
                                    surname = nameCursor.getString(familyNameIndex) ?: ""
                                } else {
                                    val parts = displayName.split(" ", limit = 2)
                                    name = parts.getOrNull(0) ?: ""
                                    surname = parts.getOrNull(1) ?: ""
                                }
                            }
                        } catch (e: Exception) {
                            val parts = displayName.split(" ", limit = 2)
                            name = parts.getOrNull(0) ?: ""
                            surname = parts.getOrNull(1) ?: ""
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (worker == null) stringResource(R.string.new_worker_title) else stringResource(R.string.edit_worker_title), 
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
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
                if (worker != null && onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete_desc),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TextField(
                    value = surname, 
                    onValueChange = { surname = it }, 
                    label = { Text(stringResource(R.string.surname_required_label), style = MaterialTheme.typography.labelLarge) },
                    textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth(),
                    isError = surname.isBlank()
                )
                if (surname.isBlank()) {
                    Text(stringResource(R.string.surname_error_msg), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
                TextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { Text(stringResource(R.string.name_label), style = MaterialTheme.typography.labelLarge) },
                    textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = phoneNumber, 
                    onValueChange = { phoneNumber = it }, 
                    label = { Text(stringResource(R.string.phone_label), style = MaterialTheme.typography.labelLarge) }, 
                    textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
                )
                TextField(
                    value = rate,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("""^\d*[.,]?\d{0,2}$"""))) {
                            val oldRate = rate
                            rate = input.replace('.', ',')
                            
                            // Replica automatica: se Straordinario o Festivo sono uguali alla vecchia paga base 
                            // (o sono vuoti), li aggiorniamo insieme alla paga base.
                            if (extraRate.isEmpty() || extraRate == oldRate) {
                                extraRate = rate
                            }
                            if (holidayRate.isEmpty() || holidayRate == oldRate) {
                                holidayRate = rate
                            }
                        }
                    },
                    label = { Text(stringResource(R.string.hourly_rate_label), style = MaterialTheme.typography.labelLarge) },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                )
                TextField(
                    value = extraRate,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("""^\d*[.,]?\d{0,2}$"""))) {
                            extraRate = input.replace('.', ',')
                        }
                    },
                    label = { Text(stringResource(R.string.extra_hourly_rate_label), style = MaterialTheme.typography.labelLarge) },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                )
                TextField(
                    value = holidayRate,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("""^\d*[.,]?\d{0,2}$"""))) {
                            holidayRate = input.replace('.', ',')
                        }
                    },
                    label = { Text(stringResource(R.string.holiday_hourly_rate_label), style = MaterialTheme.typography.labelLarge) },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val r = rate.replace(',', '.').toDoubleOrNull() ?: 0.0
                    val er = extraRate.replace(',', '.').toDoubleOrNull() ?: 0.0
                    val hr = holidayRate.replace(',', '.').toDoubleOrNull() ?: 0.0
                    if (surname.isNotBlank()) {
                        onConfirm(name, surname, phoneNumber, r, er, hr)
                    }
                },
                enabled = surname.isNotBlank()
            ) { Text(if (worker == null) stringResource(R.string.btn_add) else stringResource(R.string.btn_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_btn)) } }
    )
}
