package com.example.gestbraccianti.ui.utils

import java.util.Locale

import kotlin.math.roundToInt

fun formatDecimalHours(hours: Double): String {
    val totalMinutes = (hours * 60).roundToInt()
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return String.format(Locale.ITALY, "%02d:%02d", h, m)
}

fun parseTimeToDouble(timeStr: String): Double {
    return try {
        if (timeStr.contains(":")) {
            val parts = timeStr.split(":")
            val h = parts[0].toIntOrNull() ?: 0
            val m = parts[1].toIntOrNull() ?: 0
            h.toDouble() + (m.toDouble() / 60.0)
        } else {
            // Fallback for old decimal format
            timeStr.toDoubleOrNull() ?: 0.0
        }
    } catch (e: Exception) {
        0.0
    }
}

