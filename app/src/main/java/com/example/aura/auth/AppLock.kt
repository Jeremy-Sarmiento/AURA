package com.example.aura.auth

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

// Estado solo en memoria: nunca restaurar una autorización desde disco o un Bundle.
class AppLock : ViewModel() {
    data class ActivityResult(val requestCode: Int, val resultCode: Int, val data: Intent?)

    private val pendingActivityResults = mutableListOf<ActivityResult>()
    var isUnlocked by mutableStateOf(false)
        private set
    var hasUnlockedOnce by mutableStateOf(false)
        private set
    var isAuthenticating by mutableStateOf(false)
        private set
    var message by mutableStateOf("Usa la huella registrada en tu teléfono para abrir AURA.")
        private set

    fun beginAuthentication(): Boolean {
        if (isUnlocked || isAuthenticating) return false
        isAuthenticating = true
        message = "Confirma tu identidad en el diálogo de Android."
        return true
    }

    fun authenticated() {
        if (!isAuthenticating) return
        isAuthenticating = false
        isUnlocked = true
        hasUnlockedOnce = true
    }

    fun authenticationError(message: String) {
        isAuthenticating = false
        this.message = message
    }

    fun lock() {
        isUnlocked = false
        isAuthenticating = false
    }

    fun deferActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        pendingActivityResults.add(ActivityResult(requestCode, resultCode, data))
    }

    fun takeActivityResults(): List<ActivityResult> {
        if (!isUnlocked) return emptyList()
        return pendingActivityResults.toList().also { pendingActivityResults.clear() }
    }
}
