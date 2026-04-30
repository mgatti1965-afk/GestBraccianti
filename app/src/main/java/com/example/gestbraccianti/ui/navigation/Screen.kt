package com.example.gestbraccianti.ui.navigation

sealed class Screen(val route: String) {
    object YearSelection : Screen("year_selection")
    object Home : Screen("home")
    object WorkerRegistry : Screen("worker_registry")
    object DailyLogging : Screen("daily_logging")
    object FinancialSummary : Screen("financial_summary")
    object Others : Screen("others")
    object WorkDayDetail : Screen("work_day_detail/{date}") {
        fun createRoute(date: Long) = "work_day_detail/$date"
    }
}
