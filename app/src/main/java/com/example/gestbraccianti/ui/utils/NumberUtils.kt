package com.example.gestbraccianti.ui.utils

import java.text.NumberFormat
import java.util.Locale

import kotlin.math.roundToInt

private val italianLocale = Locale.ITALY

/**
 * Formatta un importo come valuta italiana (€) con separatore migliaia (punto) e decimali (virgola).
 */
fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(italianLocale)
    return formatter.format(amount)
}

/**
 * Formatta un numero con stile italiano (punto per le migliaia, virgola per i decimali).
 */
fun formatDecimal(value: Double, decimals: Int = 2): String {
    val formatter = NumberFormat.getNumberInstance(italianLocale)
    formatter.minimumFractionDigits = decimals
    formatter.maximumFractionDigits = decimals
    formatter.isGroupingUsed = true
    return formatter.format(value)
}

/**
 * Formatta le ore in formato leggibile (es: 8,75 -> 8h 45m).
 */
fun formatHours(hours: Double): String {
    val h = hours.toInt()
    val m = ((hours - h) * 60).roundToInt()
    return "${h}h ${m}'"
}

/**
 * Formatta le ore in formato decimale italiano (es: 1.250,50).
 */
fun formatHoursDecimal(hours: Double): String {
    return formatDecimal(hours, 2)
}
