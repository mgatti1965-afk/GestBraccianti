package com.example.gestbraccianti.ui.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import kotlin.math.roundToInt

private val italianLocale = Locale.ITALY

object TimeUtils {
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", italianLocale)
    val dateTimeFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", italianLocale)
    val fileTimestampFormatter = SimpleDateFormat("yyyyMMdd_HHmm", italianLocale)
    val monthYearFormatter = SimpleDateFormat("MMMM yyyy", italianLocale)
    val dayMonthFormatter = SimpleDateFormat("dd/MM", italianLocale)
    val fullDateFormatter = SimpleDateFormat("EEEE d MMMM yyyy", italianLocale)
    val timeFormatter = SimpleDateFormat("HH:mm", italianLocale)
    val shortDateDayFormatter = SimpleDateFormat("EEE dd/MM/yyyy", italianLocale)
    val dayNameFormatter = SimpleDateFormat("EEEE", italianLocale)
    val monthShortFormatter = SimpleDateFormat("MMM", italianLocale)
    val dayMonthShortFormatter = SimpleDateFormat("d MMM", italianLocale)
    val dayShortFullDateFormatter = SimpleDateFormat("EEE d MMMM yyyy", italianLocale)
    val yearFormatter = SimpleDateFormat("yyyy", italianLocale)
    val weekYearFormatter = SimpleDateFormat("'Settimana' w, yyyy", italianLocale)

    fun format(date: Long, formatter: SimpleDateFormat): String {
        return formatter.format(Date(date))
    }
    
    fun formatMonth(date: Long): String {
        return monthYearFormatter.format(Date(date)).replaceFirstChar { it.uppercase() }
    }
}

fun formatDecimalHours(hours: Double): String {
    return String.format(Locale.US, "%.2f", hours)
}

fun parseTimeToDouble(timeStr: String): Double {
    val cleanStr = timeStr.trim()
    return try {
        if (cleanStr.contains(":")) {
            val parts = cleanStr.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            h.toDouble() + (m.toDouble() / 60.0)
        } else {
            // Support both comma and dot for decimal input
            cleanStr.replace(",", ".").toDoubleOrNull() ?: 0.0
        }
    } catch (e: Exception) {
        0.0
    }
}

