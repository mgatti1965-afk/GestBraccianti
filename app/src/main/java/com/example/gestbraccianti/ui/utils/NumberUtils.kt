package com.example.gestbraccianti.ui.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val symbols = DecimalFormatSymbols(Locale.ITALY).apply {
    groupingSeparator = '.'
    decimalSeparator = ','
}

/**
 * Formats a double as currency with thousands separator (dot) and decimal (comma).
 * Example: 1234.56 -> "1.234,56 €"
 */
fun formatCurrency(amount: Double): String {
    val formatter = DecimalFormat("#,##0.00 '€'", symbols)
    return formatter.format(amount)
}

/**
 * Formats a double with thousands separator (dot) and a fixed number of decimals (comma).
 * Example: 1234.5 -> "1.234,50"
 */
fun formatDecimal(value: Double, decimals: Int = 2): String {
    val pattern = StringBuilder("#,##0")
    if (decimals > 0) {
        pattern.append(".")
        repeat(decimals) { pattern.append("0") }
    }
    val formatter = DecimalFormat(pattern.toString(), symbols)
    return formatter.format(value)
}

/**
 * Specific formatter for hours, ensuring thousands separator is present for large totals.
 */
fun formatHours(hours: Double): String {
    return formatDecimal(hours, 2)
}
