package com.example

/**
 * ARCHIVO: MainActivity.kt
 * FUNCIÓN: Punto de entrada de la aplicación. Crea la actividad principal, solicita los permisos
 *          de audio/notificaciones, monta el tema y la interfaz Compose (AuraApp) y gestiona los
 *          intents con los que se abre un archivo de audio desde otros gestores de archivos.
 * NAVEGACIÓN: MainActivity -> AuraApp -> AuraViewModel -> AudioPlayerManager / LocalFileManager.
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
import com.example.aura.ui.AuraApp
import com.example.aura.ui.AuraViewModel
import com.example.ui.theme.AuraDarkBackground
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    // Referencia al ViewModel para usarla fuera de Compose (onNewIntent / onDestroy).
    private var viewModelInstance: AuraViewModel? = null

    /**
     * onCreate: inicializa la actividad, activa edge-to-edge, comprueba/piden permisos,
     * arranca el escaneo inicial de música y renderiza AuraApp dentro del tema de la app.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val viewModel: AuraViewModel = viewModel()
                viewModelInstance = viewModel

                // Lanzador de permisos: acceso a audio (MediaStore) y notificaciones (Android 13+).
                val permissionsToRequest = buildList {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.READ_MEDIA_AUDIO)
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val audioGranted = permissions[Manifest.permission.READ_MEDIA_AUDIO] == true ||
                            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
                    if (audioGranted) {
                        viewModel.scanDeviceStorage()
                    }
                }

                // Al primer render: pide permisos pendientes o escanea directamente si ya están concedidos.
                LaunchedEffect(Unit) {
                    val missing = permissionsToRequest.filter { perm ->
                        ContextCompat.checkSelfPermission(this@MainActivity, perm) != PackageManager.PERMISSION_GRANTED
                    }

                    if (missing.isNotEmpty()) {
                        permissionLauncher.launch(missing.toTypedArray())
                    } else {
                        viewModel.scanDeviceStorage()
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
