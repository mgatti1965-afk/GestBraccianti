package com.example.gestbraccianti.ui.utils

import java.util.Locale

/**
 * Converte una stringa in minuscolo con la prima lettera di ogni parola in maiuscolo.
 * Esempio: "MARIO ROSSI" -> "Mario Rossi", "de luca" -> "De Luca"
 */
fun String.capitalizeWords(): String {
    if (this.isBlank()) return this
    return this.lowercase(Locale.getDefault())
        .split(" ")
        .filter { it.isNotEmpty() }
        .joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
}
