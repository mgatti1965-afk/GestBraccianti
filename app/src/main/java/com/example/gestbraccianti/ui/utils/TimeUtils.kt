package com.example.gestbraccianti.ui.utils

import java.util.Locale

import kotlin.math.roundToInt

fun formatDecimalHours(hours: Double): String {
    val totalMinutes = (hours * 60).roundToInt()
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return String.format(Locale.ITALY, "%02d:%02d", h, m)
}
