package com.example.gestbraccianti.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gestbraccianti.data.entity.Worker
import com.example.gestbraccianti.data.entity.WorkerGroup
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
    val tabs = listOf("Anagrafica", "Gruppi")

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

@Composable
fun WorkerListTab(viewModel: WorkerViewModel, yearId: Int) {
    val workers by viewModel.workersForCurrentYear.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var selectedWorker by remember { mutableStateOf<Worker?>(null) }
    var currentRate by remember { mutableStateOf(0.0) }

    if (showDialog) {
        AddEditWorkerDialog(
            worker = selectedWorker,
            initialRate = currentRate,
            onDismiss = { showDialog = false },
            onConfirm = { name, surname, rate ->
                if (selectedWorker == null) {
                    viewModel.addWorkerToYear(name, surname, rate, yearId)
                } else {
                    viewModel.updateWorkerInfo(selectedWorker!!.id, name, surname, yearId, rate)
                }
                showDialog = false
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (workers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nessun bracciante registrato.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(workers) { worker ->
                    var rate by remember { mutableStateOf(0.0) }
                    LaunchedEffect(worker.id) {
                        rate = viewModel.getWorkerConfig(worker.id, yearId)?.hourlyRate ?: 0.0
                    }
                    Card(modifier = Modifier.fillMaxWidth().clickable {
                        selectedWorker = worker
                        currentRate = rate
                        showDialog = true
                    }) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "${worker.surname} ${worker.name}".trim(), style = MaterialTheme.typography.titleMedium)
                                Text(text = String.format(Locale.ITALY, "Tariffa: %.2f €/h", rate), style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.Default.Edit, contentDescription = "Modifica")
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { selectedWorker = null; currentRate = 0.0; showDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) { Icon(Icons.Default.Add, contentDescription = "Aggiungi Bracciante") }
    }
}

@Composable
fun GroupListTab(groupViewModel: WorkerGroupViewModel, workerViewModel: WorkerViewModel, yearId: Int) {
    val groups by groupViewModel.groupsForYear.collectAsState()
    val allWorkers by workerViewModel.workersForCurrentYear.collectAsState()
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var groupToEditMembers by remember { mutableStateOf<WorkerGroup?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (groups.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nessun gruppo creato.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(groups) { group ->
                    val members by groupViewModel.getWorkersInGroup(group.id).collectAsState(initial = emptyList())
                    Card(modifier = Modifier.fillMaxWidth().clickable { groupToEditMembers = group }) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = group.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                IconButton(onClick = { groupViewModel.deleteGroup(group) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            Text(text = "${members.size} membri", style = MaterialTheme.typography.bodySmall)
                            if (members.isNotEmpty()) {
                                Text(
                                    text = members.sortedWith(compareBy({ it.surname }, { it.name }))
                                        .joinToString { "${it.surname} ${it.name}".trim() },
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { showAddGroupDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) { Icon(Icons.Default.Add, contentDescription = "Crea Gruppo") }
    }

    if (showAddGroupDialog) {
        var groupName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddGroupDialog = false },
            title = { Text("Nuovo Gruppo") },
            text = { TextField(value = groupName, onValueChange = { groupName = it }, label = { Text("Nome Gruppo") }) },
            confirmButton = {
                Button(onClick = {
                    if (groupName.isNotBlank()) {
                        groupViewModel.createGroup(groupName)
                        showAddGroupDialog = false
                    }
                }) { Text("Crea") }
            },
            dismissButton = { TextButton(onClick = { showAddGroupDialog = false }) { Text("Annulla") } }
        )
    }

    if (groupToEditMembers != null) {
        val group = groupToEditMembers!!
        val members by groupViewModel.getWorkersInGroup(group.id).collectAsState(initial = emptyList())
        AlertDialog(
            onDismissRequest = { groupToEditMembers = null },
            title = { Text("Membri Gruppo: ${group.name}") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(allWorkers) { worker ->
                        val isMember = members.any { it.id == worker.id }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                            if (isMember) groupViewModel.removeWorkerFromGroup(worker.id, group.id)
                            else groupViewModel.addWorkerToGroup(worker.id, group.id)
                        }.padding(8.dp)) {
                            Checkbox(checked = isMember, onCheckedChange = null)
                            Spacer(Modifier.width(8.dp))
                            Text("${worker.surname} ${worker.name}".trim())
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { groupToEditMembers = null }) { Text("Chiudi") } }
        )
    }
}

@Composable
fun AddEditWorkerDialog(
    worker: Worker?,
    initialRate: Double,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double) -> Unit
) {
    var name by remember { mutableStateOf(worker?.name ?: "") }
    var surname by remember { mutableStateOf(worker?.surname ?: "") }
    var rate by remember { mutableStateOf(if (initialRate > 0) String.format(Locale.ITALY, "%.2f", initialRate) else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (worker == null) "Nuovo Bracciante" else "Modifica Bracciante") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Nome (Obbligatorio)") })
                TextField(value = surname, onValueChange = { surname = it }, label = { Text("Cognome") })
                TextField(
                    value = rate,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("""^\d*[.,]?\d{0,2}$"""))) {
                            rate = input.replace(',', '.')
                        }
                    },
                    label = { Text("Paga Oraria (€)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val r = rate.toDoubleOrNull() ?: 0.0
                if (name.isNotBlank()) {
                    onConfirm(name, surname, r)
                }
            }) { Text(if (worker == null) "Aggiungi" else "Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}
