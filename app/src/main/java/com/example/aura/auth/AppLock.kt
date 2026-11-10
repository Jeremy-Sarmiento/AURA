package com.example.aura.auth

/**
 * ARCHIVO: AppLock.kt
 * FUNCIÓN: Máquina de estado del bloqueo de AURA. Mantiene en memoria si la app está desbloqueada,
 *          controla la autenticación biométrica o con credenciales del teléfono, aplica el margen
 *          de 2 minutos al volver del segundo plano y retiene los resultados de actividades
 *          externas (selectores SAF) hasta que el usuario se autentica.
 * NAVEGACIÓN: MainActivity crea este ViewModel y consulta su estado; AuraApp y MainActivity usan
 *             isUnlocked para decidir qué se puede mostrar o importar.
 */

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

// Tiempo máximo fuera de la app sin volver a pedir autenticación (2 minutos).
private const val REAUTHENTICATION_TIMEOUT_MS = 120_000L

/**
 * AppLock: estado solo en memoria. Nunca se restaura una autorización desde disco o un Bundle,
 * de modo que reiniciar el proceso siempre exige autenticarse de nuevo.
 */
class AppLock : ViewModel() {
    /**
     * ActivityResult: resultado de un selector externo recibido mientras la app estaba bloqueada.
     * Se guarda hasta que MainActivity pueda entregarlo tras el desbloqueo.
     */
    data class ActivityResult(val requestCode: Int, val resultCode: Int, val data: Intent?)

    // Resultados pendientes de selectores SAF (se entregan una sola vez y solo desbloqueado).
    private val pendingActivityResults = mutableListOf<ActivityResult>()

    // Momento monotónico (elapsedRealtime) en que se salió estando desbloqueado; habilita el margen.
    private var authenticatedExitAt: Long? = null

    // Estado observable por Compose: acceso concedido en esta sesión.
    var isUnlocked by mutableStateOf(false)
        private set

    // Indica si AURA ya mostró la UI al menos una vez (los selectores SAF se conservan al volver).
    var hasUnlockedOnce by mutableStateOf(false)
        private set

    // Verdadero mientras hay un BiometricPrompt activo; evita lanzar dos autenticaciones.
    var isAuthenticating by mutableStateOf(false)
        private set

    // Verdadero mientras se espera el resultado del PIN/patrón/contraseña nativo de Android.
    var isConfirmingDeviceCredential = false
        private set

    // Evita relanzar el diálogo automáticamente tras un error o cancelación del usuario.
    var hasAttemptedAuthentication = false
        private set

    // Texto mostrado en la pantalla de bloqueo para explicar el estado actual.
    var message by mutableStateOf("Usa la huella, el PIN, el patrón o la contraseña del teléfono para abrir AURA.")
        private set

    /**
     * beginAuthentication: autoriza un nuevo intento de autenticación. Devuelve false si ya está
     * desbloqueado o si hay un diálogo en curso, para no abrir dos prompts a la vez.
     */
    fun beginAuthentication(): Boolean {
        if (isUnlocked || isAuthenticating) return false
        isAuthenticating = true
        hasAttemptedAuthentication = true
        message = "Confirma tu identidad en el diálogo de Android."
        return true
    }

    /**
     * authenticated: concede acceso solo si había una autenticación biométrica en curso y no se
     * está esperando la confirmación de credenciales del sistema. Ignora éxitos tardíos.
     */
    fun authenticated() {
        if (!isAuthenticating || isConfirmingDeviceCredential) return
        isAuthenticating = false
        isUnlocked = true
        hasUnlockedOnce = true
    }

    /**
     * authenticationError: vuelve a bloquear la app y conserva el mensaje de error para que el
     * usuario pueda reintentar desde la pantalla de bloqueo.
     */
    fun authenticationError(message: String) {
        lock()
        hasAttemptedAuthentication = true
        this.message = message
    }

    /**
     * beginDeviceCredential: marca el inicio del flujo nativo de PIN/patrón/contraseña.
     * Solo se permite después de beginAuthentication y nunca con otro flujo ya activo.
     */
    fun beginDeviceCredential(): Boolean {
        if (!isAuthenticating || isConfirmingDeviceCredential) return false
        isConfirmingDeviceCredential = true
        return true
    }

    /**
     * deviceCredentialResult: procesa el resultado del flujo nativo de credenciales. Solo un
     * RESULT_OK real desbloquea; cancelar mantiene AURA bloqueada.
     */
    fun deviceCredentialResult(success: Boolean) {
        if (!isConfirmingDeviceCredential) return
        isConfirmingDeviceCredential = false
        if (success) authenticated()
        else authenticationError("No se confirmó tu identidad. AURA permanece bloqueada.")
    }

    /**
     * lock: revoca el acceso y limpia todos los indicadores de autenticación. El margen de
     * reentrada también se descarta para que el próximo onBackground empiece de cero.
     */
    fun lock() {
        isUnlocked = false
        isAuthenticating = false
        isConfirmingDeviceCredential = false
        hasAttemptedAuthentication = false
        authenticatedExitAt = null
    }

    /**
     * onBackground: al salir de la app se bloquea de inmediato. Si la salida ocurrió estando
     * desbloqueado, se guarda el instante para aplicar el margen de 2 minutos al volver.
     */
    fun onBackground(now: Long) {
        val wasUnlocked = isUnlocked
        lock()
        authenticatedExitAt = now.takeIf { wasUnlocked }
    }

    /**
     * onForeground: al volver, restaura la sesión solo si el usuario salió desbloqueado y han
     * pasado menos de 2 minutos; de lo contrario permanece bloqueado y se pedirá autenticación.
     */
    fun onForeground(now: Long) {
        val exitedAt = authenticatedExitAt ?: return
        authenticatedExitAt = null
        isUnlocked = now - exitedAt in 0L until REAUTHENTICATION_TIMEOUT_MS
    }

    /**
     * deferActivityResult: guarda el resultado de un selector externo recibido mientras la app
     * está bloqueada, para no importar archivos antes de autenticarse.
     */
    fun deferActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        pendingActivityResults.add(ActivityResult(requestCode, resultCode, data))
    }

    /**
     * takeActivityResults: devuelve y vacía los resultados pendientes. Si la app sigue bloqueada
     * devuelve una lista vacía, de modo que se entregarán en el siguiente intento desbloqueado.
     */
    fun takeActivityResults(): List<ActivityResult> {
        if (!isUnlocked) return emptyList()
        return pendingActivityResults.toList().also { pendingActivityResults.clear() }
    }
}
