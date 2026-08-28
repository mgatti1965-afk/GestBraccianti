package com.example.gestbraccianti.ui.utils

import androidx.compose.material3.SnackbarDuration
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class AppMessage(
    val message: String,
    val isError: Boolean = false,
    val duration: SnackbarDuration = SnackbarDuration.Short
)

object MessageBarManager {
    private val _messages = MutableSharedFlow<AppMessage>()
    val messages = _messages.asSharedFlow()

    suspend fun showMessage(message: String, isError: Boolean = false, duration: SnackbarDuration = SnackbarDuration.Short) {
        _messages.emit(AppMessage(message, isError, duration))
    }
}
