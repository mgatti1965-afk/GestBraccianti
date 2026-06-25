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
    var helpRoute by remember { mutableStateOf<String?>(null) }

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
                    onBack = { navController.popBackStack() },
                    onShowHelp = { route ->
                        helpRoute = route
                        showGlobalHelp = true
                    }
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
        GlobalHelpDialog(route = helpRoute ?: currentRoute, onDismiss = { 
            showGlobalHelp = false 
            helpRoute = null
        })
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
                val isModificaOrari = route == "modifica_orari"

                if (!isModificaOrari) {
                    Text("MANUALE RAPIDO", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    HelpRow(Icons.AutoMirrored.Filled.Logout, "Annata", "Usa la freccia a destra nella barra in alto per tornare alla selezione dell'anno o crearne uno nuovo.")
                    HelpRow(Icons.Default.History, "ORE", "Registra presenze e orari nel calendario.")
                    HelpRow(Icons.Default.Calculate, "RIEPILOGO", "Controlla i totali e genera PDF/WhatsApp.")
                    HelpRow(Icons.Default.Group, "BRACCIANTI", "Gestisci l'anagrafica, le tariffe e i gruppi.")
                    HelpRow(Icons.Default.MoreHoriz, "VARIE", "Imposta i tuoi dati e gestisci i Backup CSV.")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }

                when {
                    route == Screen.Home.route || route == Screen.DailyLogging.route -> {
                        Text("1. ORE", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        HelpRow(Icons.Default.CalendarToday, "Calendario", "I giorni colorati indicano ore già registrate. Tocca un giorno per inserire o modificare i dati.")
                        HelpRow(Icons.Default.SyncAlt, "Navigazione", "Usa le frecce in alto per scorrere i mesi dell'annata selezionata.")
                        HelpRow(Icons.Default.List, "Elenco", "Sotto il calendario vedi il riepilogo rapido delle giornate lavorate nel mese. Tocca per vedere il dettaglio della giornata.")
                    }
                    route?.startsWith("work_day_detail") == true -> {
                        Text("2. DETTAGLIO GIORNATA", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        HelpRow(Icons.Default.PersonAdd, "Bracciante / Gruppi", "Usa i tasti in basso per aggiungere lavoratori/gruppi alla giornata.")
                        HelpRow(Icons.Default.Edit, "Modifica", "Tocca un bracciante nell'elenco per variare i suoi orari.")
                        HelpRow(Icons.Default.Delete, "Elimina", "Usa l'icona del cestino per rimuovere un lavoratore inserito per errore.")
                    }
                    route == Screen.FinancialSummary.route -> {
                        Text("3. RIEPILOGO", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        HelpRow(Icons.Default.FilterList, "Filtri", "Filtra per periodo, singolo bracciante o gruppo per isolare i dati che ti servono.")
                        HelpRow(Icons.Default.Group, "Bracc./Gruppi", "Visualizza i costi e le ore per ogni lavoratore o gruppo.")
                        HelpRow(Icons.Default.ListAlt, "Vista (📝/📊)", "Passa dal dettaglio ai totali per lavoratore/gruppo.")
                        HelpRow(Icons.Default.Share, "Esporta", "Genera il PDF professionale o invia il riepilogo testuale su WhatsApp.")
                    }
                    route == Screen.WorkerRegistry.route -> {
                        Text("4. BRACCIANTI", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        HelpRow(Icons.Default.Badge, "Anagrafica", "Gestisci nomi e paga oraria.")
                        HelpRow(Icons.Default.Groups, "Gruppi", "Crea gruppi di lavoratori (es. 'Squadra A') per aggiungere gli orari di tutti i componenti nelle giornate di lavoro.")
                        HelpRow(Icons.Default.Search, "Ricerca", "Usa la lente d'ingrandimento per trovare velocemente un bracciante nell'elenco.")
                    }
                    route == Screen.Others.route -> {
                        Text("5. VARIE", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        HelpRow(Icons.Default.Business, "Dati Azienda", "Configura l'intestazione (Nome, P.IVA, etc.) che apparirà nei tuoi documenti PDF.")
                        HelpRow(Icons.Default.Backup, "Backup", "Esporta i dati in formato CSV per sicurezza o per aprirli in Excel/Google Sheets.")
                    }
                    isModificaOrari -> {
                        Text("6. MODIFICA ORARI", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        HelpRow(Icons.Default.Add, "Tasti +/-", "Modifica l'orario a scatti di 15 minuti. Tieni premuto per uno scorrimento veloce.")
                        HelpRow(Icons.Default.Edit, "Manuale", "Tocca l'orario (testo) per inserire le ore precise con la tastiera.")
                        HelpRow(Icons.Default.DateRange, "Espandi", "Check che consente di accedere ad un selettore dove è possibile impostare una data futura. Alla conferma gli orari verranno replicati fino alla data scelta.")
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
