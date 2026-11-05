package com.example

/**
 * ARCHIVO: MainActivity.kt
 * FUNCIÓN: Punto de entrada de la aplicación. Crea la actividad principal, solicita únicamente el
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
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aura.data.storage.takePersistableReadPermission
import com.example.aura.ui.AuraApp
import com.example.aura.ui.AuraViewModel
import com.example.ui.theme.AuraDarkBackground
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    // Referencia al ViewModel para usarla fuera de Compose (onNewIntent / onDestroy).
    private var viewModelInstance: AuraViewModel? = null

    /**
     * onCreate: inicializa la actividad, activa edge-to-edge, pide el permiso de notificaciones
     * (Android 13+) y renderiza AuraApp dentro del tema de la app.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
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

                // Al primer render: pide permisos pendientes y procesa el intent de apertura.
                LaunchedEffect(Unit) {
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

    /**
     * onNewIntent: la actividad usa launchMode="singleTop"; aquí se recibe un nuevo archivo
     * abierto mientras la app ya estaba en ejecución.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModelInstance?.let { handleIncomingIntent(intent, it) }
    }

    /**
     * handleIncomingIntent: si el intent trae una URI de audio (VIEW o SEND), importa y
     * reproduce esa pista a través del ViewModel.
     */
    private fun handleIncomingIntent(intent: Intent?, viewModel: AuraViewModel) {
        if (intent == null) return
        val action = intent.action
        val data: Uri? = intent.data

        if ((Intent.ACTION_VIEW == action || Intent.ACTION_SEND == action) && data != null) {
            takePersistableReadPermission(data)
            viewModel.importSongFromUri(data)
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
