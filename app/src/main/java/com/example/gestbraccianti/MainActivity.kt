package com.example.gestbraccianti

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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

    // Sincronizza l'anno selezionato nei ViewModel
    LaunchedEffect(currentYear) {
        currentYear?.let {
            workerViewModel.setSelectedYear(it.id)
            workLogViewModel.setSelectedYear(it.id)
            workerGroupViewModel.setSelectedYear(it.id)
        }
    }

    // Redirect to YearSelection if no year is selected
    val startDestination = if (currentYear == null) Screen.YearSelection.route else Screen.Home.route

    Scaffold(
        topBar = {
            if (currentYear != null) {
                TopAppBar(
                    title = { Text("GestBraccianti ${currentYear?.id ?: ""}") },
                    actions = {
                        IconButton(onClick = { 
                            harvestViewModel.deselectYear()
                            navController.navigate(Screen.YearSelection.route) {
                                popUpTo(0)
                            }
                        }) {
                            Icon(Icons.Default.Logout, contentDescription = "Cambia Annata")
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
                YearSelectionScreen(harvestViewModel) { yearId ->
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
                FinancialSummaryScreen(workLogViewModel) 
            }
        }
    }
}

@Composable
fun AppBottomNavigation(navController: androidx.navigation.NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val items = listOf(
        Triple(Screen.DailyLogging.route, "Ore", Icons.Default.History),
        Triple(Screen.WorkerRegistry.route, "Braccianti", Icons.Default.Group),
        Triple(Screen.FinancialSummary.route, "Riepilogo", Icons.Default.Calculate)
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
