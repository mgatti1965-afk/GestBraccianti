package com.example.gestbraccianti

import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.gestbraccianti.ui.navigation.Screen
import com.example.gestbraccianti.ui.screens.*
import com.example.gestbraccianti.ui.components.GlobalHelpDialog
import com.example.gestbraccianti.ui.components.SmallStatChip
import com.example.gestbraccianti.ui.components.DonationDialog
import com.example.gestbraccianti.ui.components.launchDonationIntent
import com.example.gestbraccianti.ui.theme.GestBracciantiTheme
import com.example.gestbraccianti.ui.utils.MessageBarManager
import com.example.gestbraccianti.ui.viewmodel.HarvestViewModel
import com.example.gestbraccianti.ui.viewmodel.HarvestViewModelFactory
import com.example.gestbraccianti.ui.viewmodel.WorkLogViewModel
import com.example.gestbraccianti.ui.viewmodel.WorkLogViewModelFactory
import com.example.gestbraccianti.ui.viewmodel.WorkerViewModel
import com.example.gestbraccianti.ui.viewmodel.WorkerViewModelFactory
import com.example.gestbraccianti.ui.viewmodel.WorkerGroupViewModel
import com.example.gestbraccianti.ui.viewmodel.WorkerGroupViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var isExiting by remember { mutableStateOf(false) }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (isExiting) {
        LaunchedEffect(Unit) {
            delay(3000)
            context.findActivity()?.finish()
        }
        ExitGreetingScreen(
            title = "Grazie per aver usato l'app!",
            message = "Se ti è stata utile, consigliala a parenti ed amici."
        )
        return
    }

    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentYear by harvestViewModel.currentYear.collectAsState()
    var showGlobalHelp by remember { mutableStateOf(false) }
    
    val prefs = remember { context.getSharedPreferences("donation_prefs", android.content.Context.MODE_PRIVATE) }
    var donationCount by remember { mutableIntStateOf(prefs.getInt("donation_count", 0)) }
    var showDonationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        MessageBarManager.messages.collect { appMessage ->
            snackbarHostState.showSnackbar(
                message = appMessage.message,
                duration = appMessage.duration,
                withDismissAction = true,
                actionLabel = if (appMessage.isError) "ERRORE" else null
            )
        }
    }

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

    BackHandler(enabled = !isExiting) {
        val isEditingScreen = currentRoute == Screen.WorkerRegistry.route || 
                             currentRoute?.startsWith("work_day_detail") == true ||
                             currentRoute == Screen.Others.route

        if (isEditingScreen) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBackPressTime < 2000) {
                isExiting = true
            } else {
                lastBackPressTime = currentTime
                scope.launch {
                    MessageBarManager.showMessage("Modifiche non salvate. Premi ancora per uscire.")
                }
            }
        } else {
            if (navController.previousBackStackEntry == null) {
                isExiting = true
            } else {
                navController.popBackStack()
            }
        }
    }

    val screenTitle = remember(currentRoute, currentYear) {
        val yearSuffix = currentYear?.id?.let { " - $it" } ?: ""
        when {
            currentRoute == Screen.WorkerRegistry.route -> "Registro Braccianti"
            currentRoute == Screen.Others.route -> "Altre Funzioni"
            currentRoute == Screen.DailyLogging.route || currentRoute == Screen.Home.route || 
            currentRoute?.startsWith("work_day_detail") == true -> "Ore Lavorate$yearSuffix"
            currentRoute == Screen.FinancialSummary.route -> "Riepilogo$yearSuffix"
            else -> "GestBraccianti"
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                val isError = data.visuals.actionLabel == "ERRORE"
                Snackbar(
                    snackbarData = data,
                    containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.inverseSurface,
                    contentColor = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.inverseOnSurface,
                    dismissActionContentColor = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.inverseOnSurface,
                    actionColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        },
        topBar = {
            if (currentYear != null) {
                CenterAlignedTopAppBar(
                    title = { Text(screenTitle) },
                    actions = {
                        IconButton(
                            onClick = { showDonationDialog = true },
                            modifier = Modifier.background(Color.Gray.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Text(text = "☕", fontSize = 20.sp)
                        }
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
                    workLogViewModel.updateReferenceDate(date)
                    navController.navigate(Screen.WorkDayDetail.createRoute(date))
                }
            }
            composable(Screen.DailyLogging.route) {
                DailyLoggingScreen(workLogViewModel) { date ->
                    workLogViewModel.updateReferenceDate(date)
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

    if (showDonationDialog) {
        DonationDialog(
            donationCount = donationCount,
            appName = "GestBraccianti",
            onDismiss = { showDonationDialog = false },
            onConfirm = {
                showDonationDialog = false
                launchDonationIntent(context, "GestBraccianti")
                val newCount = donationCount + 1
                prefs.edit().putInt("donation_count", newCount).apply()
                donationCount = newCount
            }
        )
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
            val isSelected = currentDestination?.hierarchy?.any { it.route == route } == true ||
                (route == Screen.DailyLogging.route && (
                    currentDestination?.route == Screen.Home.route || 
                    currentDestination?.route == Screen.WorkDayDetail.route
                ))

            NavigationBarItem(
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
                selected = isSelected,
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

@Composable
fun ExitGreetingScreen(
    title: String,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp
                ),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(48.dp))
            Icon(
                imageVector = Icons.Default.Agriculture,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
        }
    }
}

private fun Context.findActivity(): ComponentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    return null
}
