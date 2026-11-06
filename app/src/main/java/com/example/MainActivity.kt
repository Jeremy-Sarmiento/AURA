package com.example

/**
 * ARCHIVO: MainActivity.kt
 * FUNCIÓN: Punto de entrada de la aplicación. Bloquea el acceso con biometría, solicita el
 *          permiso de notificaciones, monta la interfaz Compose (AuraApp) y gestiona los intents
 *          con los que se abre un archivo de audio desde otros gestores de archivos.
 * NAVEGACIÓN: MainActivity -> AuraApp -> AuraViewModel -> AudioPlayerManager / LocalFileManager.
 *
 * La música local ya no se detecta con MediaStore: AURA solo lee las carpetas SAF que el usuario
 * autoriza desde el gestor de archivos.
 */

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aura.auth.AppLock
import com.example.aura.data.storage.takePersistableReadPermission
import com.example.aura.ui.AuraApp
import com.example.aura.ui.AuraViewModel
import com.example.ui.theme.AuraDarkBackground
import com.example.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {

    private val appLock by lazy { ViewModelProvider(this)[AppLock::class.java] }
    private lateinit var biometricPrompt: BiometricPrompt

    // Referencia al ViewModel para usarla fuera de Compose (onNewIntent / onDestroy).
    private var viewModelInstance: AuraViewModel? = null

    /**
     * onCreate: inicializa la actividad, activa edge-to-edge, pide el permiso de notificaciones
     * (Android 13+) y renderiza AuraApp dentro del tema de la app.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Evita que la vista reciente muestre la biblioteca después de bloquearla.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        biometricPrompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    appLock.authenticated()
                    processPendingActions()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    appLock.authenticationError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    // Android mantiene el diálogo abierto y permite otro intento.
                }
            })

        setContent {
            MyApplicationTheme {
                if (!appLock.isUnlocked) {
                    Dialog(
                        onDismissRequest = { finish() },
                        properties = DialogProperties(
                            dismissOnClickOutside = false,
                            usePlatformDefaultWidth = false
                        )
                    ) {
                        Surface(modifier = Modifier.fillMaxSize(), color = AuraDarkBackground) {
                            Column(
                                modifier = Modifier.safeDrawingPadding()
                                    .verticalScroll(rememberScrollState()).padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text("AURA bloqueada", style = MaterialTheme.typography.headlineLarge)
                                Text(appLock.message)
                                Button(onClick = ::authenticate, enabled = !appLock.isAuthenticating) {
                                    Text("Desbloquear con huella")
                                }
                                TextButton(onClick = ::openBiometricSettings) {
                                    Text("Configurar huella en el teléfono")
                                }
                            }
                        }
                    }
                }
                // Mantiene los selectores SAF y su estado al volver del segundo plano.
                if (appLock.hasUnlockedOnce) {
                    val viewModel: AuraViewModel = viewModel()
                    viewModelInstance = viewModel

                    // Solo se piden notificaciones: las carpetas de música usan permisos SAF.
                    val permissionsToRequest = buildList {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { }

                    // Procesa aperturas pendientes solo después de desbloquear.
                    LaunchedEffect(appLock.isUnlocked) {
                        if (!appLock.isUnlocked) return@LaunchedEffect
                        val missing = permissionsToRequest.filter { permission ->
                            ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                permission
                            ) != PackageManager.PERMISSION_GRANTED
                        }
                        if (missing.isNotEmpty()) {
                            permissionLauncher.launch(missing.toTypedArray())
                        }

                        // Comprueba si la app se abrió con un archivo de audio (ACTION_VIEW / ACTION_SEND).
                        handleIncomingIntent(intent, viewModel)
                    }

                    // Contenedor raíz de la interfaz Compose.
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = AuraDarkBackground
                    ) {
                        AuraApp(viewModel = viewModel)
                    }
                }
            }
        }
    }

    override fun onStart() {
        // Evalúa la sesión antes de que se entreguen resultados de actividades externas.
        appLock.onForeground(SystemClock.elapsedRealtime())
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        processPendingActions()
        authenticate()
    }

    private fun processPendingActions() {
        appLock.takeActivityResults().forEach { pending ->
            deliverActivityResult(pending.requestCode, pending.resultCode, pending.data)
        }
        viewModelInstance?.let { handleIncomingIntent(intent, it) }
    }

    // Los selectores externos no deben importar archivos antes de volver a autenticarse.
    @Deprecated("Callback de compatibilidad usado por ActivityResultRegistry")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (appLock.isUnlocked) {
            deliverActivityResult(requestCode, resultCode, data)
        } else {
            appLock.deferActivityResult(requestCode, resultCode, data)
        }
    }

    private fun deliverActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onStop() {
        if (!isChangingConfigurations) {
            appLock.onBackground(SystemClock.elapsedRealtime())
            biometricPrompt.cancelAuthentication()
        }
        super.onStop()
    }

    private fun authenticate() {
        if (!appLock.beginAuthentication()) return
        when (BiometricManager.from(this).canAuthenticate(BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> biometricPrompt.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Desbloquear AURA")
                    .setSubtitle("Usa la biometría registrada en tu teléfono")
                    .setAllowedAuthenticators(BIOMETRIC_STRONG)
                    .setNegativeButtonText("Cancelar")
                    .build()
            )
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> appLock.authenticationError(
                "Primero registra una huella en los ajustes de seguridad del teléfono."
            )
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> appLock.authenticationError(
                "Este teléfono no tiene un sensor biométrico compatible. AURA permanece bloqueada."
            )
            else -> appLock.authenticationError(
                "La biometría no está disponible. Desbloquea el teléfono e inténtalo de nuevo."
            )
        }
    }

    private fun openBiometricSettings() {
        val settingsIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL)
                .putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BIOMETRIC_STRONG)
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
        try {
            startActivity(settingsIntent)
        } catch (_: android.content.ActivityNotFoundException) {
            appLock.authenticationError("Abre los ajustes del teléfono y registra una huella en Seguridad.")
        }
    }

    /**
     * onNewIntent: la actividad usa launchMode="singleTop"; aquí se recibe un nuevo archivo
     * abierto mientras la app ya estaba en ejecución.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (appLock.isUnlocked) {
            viewModelInstance?.let { handleIncomingIntent(intent, it) }
        }
    }

    /**
     * handleIncomingIntent: si el intent trae una URI de audio (VIEW o SEND), importa y
     * reproduce esa pista a través del ViewModel.
     */
    private fun handleIncomingIntent(intent: Intent?, viewModel: AuraViewModel) {
        if (!appLock.isUnlocked || intent == null) return
        val action = intent.action
        val data: Uri? = intent.data

        if ((Intent.ACTION_VIEW == action || Intent.ACTION_SEND == action) && data != null) {
            takePersistableReadPermission(data)
            viewModel.importSongFromUri(data)
            // No volver a importar ni reproducir el mismo archivo al desbloquear otra vez.
            setIntent(Intent(this, MainActivity::class.java))
        }
    }

    /**
     * onDestroy: si la actividad se cierra definitivamente, detiene el reproductor y el
     * servicio de reproducción en primer plano.
     */
    override fun onDestroy() {
        if (isFinishing) {
            viewModelInstance?.playerManager?.stop()
            com.example.aura.service.MusicPlaybackService.stopService(this)
        }
        super.onDestroy()
    }
}
