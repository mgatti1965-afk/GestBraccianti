package com.example.gestbraccianti

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.gestbraccianti.ui.navigation.Screen
import com.example.gestbraccianti.ui.screens.*
import com.example.gestbraccianti.ui.theme.GestBracciantiTheme
import com.example.gestbraccianti.ui.viewmodel.HarvestViewModel
import com.example.gestbraccianti.ui.viewmodel.HarvestViewModelFactory
import com.example.gestbraccianti.ui.viewmodel.WorkLogViewModel
import com.example.gestbraccianti.ui.viewmodel.WorkLogViewModelFactory
import com.example.gestbraccianti.ui.viewmodel.WorkerViewModel
import com.example.gestbraccianti.ui.viewmodel.WorkerViewModelFactory
import com.example.gestbraccianti.ui.viewmodel.WorkerGroupViewModel
import com.example.gestbraccianti.ui.viewmodel.WorkerGroupViewModelFactory

class MainActivity : ComponentActivity() {
    private val harvestViewModel: HarvestViewModel by viewModels {
        HarvestViewModelFactory((application as GestBracciantiApplication).harvestRepository)
    }
    
    private val workerViewModel: WorkerViewModel by viewModels {
        val app = application as GestBracciantiApplication
        WorkerViewModelFactory(app.workerRepository, app.workerYearConfigRepository)
    }

    private val workLogViewModel: WorkLogViewModel by viewModels {
        val app = application as GestBracciantiApplication
        WorkLogViewModelFactory(app.workLogRepository, app.workerYearConfigRepository)
    }

    private val workerGroupViewModel: WorkerGroupViewModel by viewModels {
        val app = application as GestBracciantiApplication
        WorkerGroupViewModelFactory(app.workerGroupRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GestBracciantiTheme {
                MainApp(harvestViewModel, workerViewModel, workLogViewModel, workerGroupViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    harvestViewModel: HarvestViewModel,
    workerViewModel: WorkerViewModel,
    workLogViewModel: WorkLogViewModel,
    workerGroupViewModel: WorkerGroupViewModel
) {
    val navController = rememberNavController()
    val currentYear by harvestViewModel.currentYear.collectAsState()
    var showGlobalHelp by remember { mutableStateOf(false) }

    LaunchedEffect(currentYear) {
        currentYear?.let {
            workerViewModel.setSelectedYear(it.id)
            workLogViewModel.setSelectedYear(it.id)
            workerGroupViewModel.setSelectedYear(it.id)
        }
    }

    val startDestination = if (currentYear == null) Screen.YearSelection.route else Screen.Home.route
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        topBar = {
            if (currentYear != null) {
                CenterAlignedTopAppBar(
                    title = { Text("GestBraccianti ${currentYear?.id ?: ""}") },
                    actions = {
                        IconButton(onClick = { showGlobalHelp = true }) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Guida")
                        }
                        IconButton(onClick = { 
                            harvestViewModel.deselectYear()
                            navController.navigate(Screen.YearSelection.route) {
                                popUpTo(0)
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Esci dall'annata")
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (currentYear != null) {
                AppBottomNavigation(navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.YearSelection.route) {
                YearSelectionScreen(harvestViewModel) { _ ->
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.YearSelection.route) { inclusive = true }
                    }
                }
            }
            composable(Screen.Home.route) {
                DailyLoggingScreen(workLogViewModel) { date ->
                    navController.navigate(Screen.WorkDayDetail.createRoute(date))
                }
            }
            composable(Screen.DailyLogging.route) {
                DailyLoggingScreen(workLogViewModel) { date ->
                    navController.navigate(Screen.WorkDayDetail.createRoute(date))
                }
            }
            composable(
                route = Screen.WorkDayDetail.route,
                arguments = listOf(androidx.navigation.navArgument("date") { type = androidx.navigation.NavType.LongType })
            ) { backStackEntry ->
                val date = backStackEntry.arguments?.getLong("date") ?: 0L
                WorkDayDetailScreen(
                    date = date,
                    yearId = currentYear?.id ?: 0,
                    workLogViewModel = workLogViewModel,
                    workerViewModel = workerViewModel,
                    groupViewModel = workerGroupViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.WorkerRegistry.route) { 
                WorkerRegistryScreen(workerViewModel, workerGroupViewModel, currentYear?.id ?: 0)
            }
            composable(Screen.FinancialSummary.route) { 
                FinancialSummaryScreen(workLogViewModel, workerGroupViewModel)
            }
            composable(Screen.Others.route) {
                OthersScreen(workerViewModel, currentYear?.id ?: 0)
            }
        }
    }

    if (showGlobalHelp) {
        GlobalHelpDialog(route = currentRoute, onDismiss = { showGlobalHelp = false })
    }
}

@Composable
fun GlobalHelpDialog(route: String?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Manuale Rapido")
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when {
                    route == Screen.YearSelection.route -> {
                        Text("Selezione Annata", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        HelpRow(Icons.Default.Add, "Nuova Annata", "Crea una cartella per i dati di un nuovo anno (es. 2024).")
                        HelpRow(Icons.Default.FolderOpen, "Apri", "Seleziona un'annata esistente per iniziare a lavorare.")
                        HelpRow(Icons.Default.Delete, "Elimina", "Rimuove l'annata e tutti i suoi dati (azione irreversibile).")
                    }
                    route == Screen.Home.route || route == Screen.DailyLogging.route -> {
                        Text("Calendario Presenze", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        HelpRow(Icons.Default.Today, "Giorno", "Tocca un giorno per inserire o modificare i braccianti.")
                        HelpRow(Icons.Default.EventNote, "Icone", "I pallini colorati indicano la presenza di registrazioni.")
                        HelpRow(Icons.Default.ChevronLeft, "Navigazione", "Usa le frecce per cambiare mese.")
                    }
                    route?.startsWith("work_day_detail") == true -> {
                        Text("Dettaglio Giornata", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        HelpRow(Icons.Default.PersonAdd, "Aggiungi", "Inserisci un singolo bracciante o un'intera squadra.")
                        HelpRow(Icons.Default.Add, "Tasti +/-", "Variazione rapida di 15 min (pressione lunga per scorrimento).")
                        HelpRow(Icons.Default.Edit, "Manuale", "Tocca l'orario per inserirlo con la tastiera.")
                        HelpRow(Icons.Default.DateRange, "Espandi", "Copia gli orari su più giorni consecutivi.")
                    }
                    route == Screen.WorkerRegistry.route -> {
                        Text("Anagrafica Braccianti", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        HelpRow(Icons.Default.PersonAdd, "Nuovo", "Registra un bracciante con la sua tariffa oraria.")
                        HelpRow(Icons.Default.GroupAdd, "Squadre", "Crea gruppi per aggiungere più persone contemporaneamente.")
                        HelpRow(Icons.Default.Edit, "Modifica", "Tocca un bracciante per cambiare dati o tariffa.")
                    }
                    route == Screen.FinancialSummary.route -> {
                        Text("Riepilogo e Pagamenti", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        HelpRow(Icons.Default.Group, "Vista Squadre", "Passa dai singoli braccianti ai totali per gruppo.")
                        HelpRow(Icons.Default.ListAlt, "Dettagli", "Mostra l'elenco analitico di tutte le giornate lavorate.")
                        HelpRow(Icons.Default.Share, "Condividi", "Invia il riepilogo via WhatsApp o genera un PDF.")
                    }
                    route == Screen.Others.route -> {
                        Text("Impostazioni e Backup", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        HelpRow(Icons.Default.Badge, "Dati Titolare", "Imposta i tuoi dati che appariranno nei PDF.")
                        HelpRow(Icons.Default.Backup, "Backup CSV", "Esporta i dati in Excel per sicurezza o archiviazione.")
                        HelpRow(Icons.Default.Storage, "Ripristino", "Importa i dati da un file CSV precedentemente salvato.")
                    }
                    else -> {
                        Text("Benvenuto", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        Text("Usa il menù in basso per navigare tra le sezioni. In ogni schermata troverai questa guida specifica.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Ho capito") }
        }
    )
}

@Composable
fun HelpRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(desc, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun AppBottomNavigation(navController: androidx.navigation.NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val items = listOf(
        Triple(Screen.DailyLogging.route, "Ore", Icons.Default.History),
        Triple(Screen.FinancialSummary.route, "Riepilogo", Icons.Default.Calculate),
        Triple(Screen.WorkerRegistry.route, "Braccianti", Icons.Default.Group),
        Triple(Screen.Others.route, "Varie", Icons.Default.MoreHoriz)
    )

    NavigationBar {
        items.forEach { (route, label, icon) ->
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
                selected = currentDestination?.hierarchy?.any { it.route == route } == true,
                onClick = {
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
