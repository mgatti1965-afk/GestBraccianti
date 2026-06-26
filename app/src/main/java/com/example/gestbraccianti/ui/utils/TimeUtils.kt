package com.example.gestbraccianti.ui.utils

import java.util.Locale

import kotlin.math.roundToInt

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

