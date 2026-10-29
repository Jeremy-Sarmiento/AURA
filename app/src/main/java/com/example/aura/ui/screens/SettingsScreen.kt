package com.example.aura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AuraCardBorder
import com.example.ui.theme.AuraDarkBackground
import com.example.ui.theme.AuraDarkSurfaceVariant
import com.example.ui.theme.AuraPrimaryPurple
import com.example.ui.theme.AuraTextSecondary

@Composable
fun SettingsScreen(
    hifiLosslessEnabled: Boolean,
    notificationsEnabled: Boolean,
    onToggleHiFi: (Boolean) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onOpenFileExplorer: () -> Unit
) {
    var accountEnabled by remember { mutableStateOf(true) }
    var downloadsLocalEnabled by remember { mutableStateOf(true) }
    var showHelpDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraDarkBackground)
            .padding(horizontal = 24.dp)
            .testTag("settings_screen")
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Screen title "AJUSTES" matching Mockup 10
        Text(
            text = "AJUSTES",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontSize = 18.sp
            ),
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(30.dp))

        // 5 Setting Rows matching Mockup 10:
        // 1. Cuenta
        // 2. Notificaciones
        // 3. Sonido
        // 4. Descargas (Gestor de Archivos)
        // 5. Ayuda

        SettingItemRow(
            title = "Cuenta",
            subtitle = "Perfil de audio local",
            icon = Icons.Default.Person,
            checked = accountEnabled,
            onCheckedChange = { accountEnabled = it }
        )

        Spacer(modifier = Modifier.height(18.dp))

        SettingItemRow(
            title = "Notificaciones",
            subtitle = "Controles en pantalla de bloqueo",
            icon = Icons.Default.Notifications,
            checked = notificationsEnabled,
            onCheckedChange = onToggleNotifications
        )

        Spacer(modifier = Modifier.height(18.dp))

        SettingItemRow(
            title = "Sonido",
            subtitle = "Modo Sin Pérdida Hi-Fi",
            icon = Icons.Default.Settings,
            checked = hifiLosslessEnabled,
            onCheckedChange = onToggleHiFi
        )

        Spacer(modifier = Modifier.height(18.dp))

        SettingItemRow(
            title = "Descargas",
            subtitle = "Gestor de archivos de música y carpetas",
            icon = Icons.Default.Download,
            checked = downloadsLocalEnabled,
            onCheckedChange = { downloadsLocalEnabled = it },
            onItemClick = onOpenFileExplorer
        )

        Spacer(modifier = Modifier.height(18.dp))

        SettingItemRow(
            title = "Ayuda",
            subtitle = "Formatos soportados, FLAC y soporte",
            icon = Icons.Default.HelpOutline,
            checked = false,
            showSwitch = false,
            onCheckedChange = {},
            onItemClick = { showHelpDialog = true }
        )

        if (showHelpDialog) {
            AlertDialog(
                onDismissRequest = { showHelpDialog = false },
                containerColor = AuraDarkSurfaceVariant,
                title = {
                    Text(
                        text = "Acerca de AURA",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "AURA es un reproductor de música de alta fidelidad diseñado para reproducir audio local y archivos sin pérdida.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Text(
                            text = "• Formatos soportados: FLAC (hasta 24-bit/192kHz), WAV (PCM), ALAC, AIFF, MP3, AAC, OGG.\n• Gestor de archivos integrado para explorar y escanear la música de tu teléfono sin interrupciones.\n• Listas de reproducción personalizadas y letras sincronizadas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AuraTextSecondary
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showHelpDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = AuraPrimaryPurple)
                    ) {
                        Text("Entendido", color = Color.White)
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingItemRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    showSwitch: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
    onItemClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (onItemClick != null) Modifier.clickable { onItemClick() }
                else Modifier
            ),
        color = AuraDarkSurfaceVariant.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(1.dp, AuraCardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = AuraPrimaryPurple,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AuraTextSecondary
                )
            }

            if (showSwitch) {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AuraPrimaryPurple,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.7f),
                        uncheckedTrackColor = AuraCardBorder
                    )
                )
            }
        }
    }
}
