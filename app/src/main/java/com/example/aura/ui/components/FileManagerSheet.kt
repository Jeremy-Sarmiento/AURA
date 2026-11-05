package com.example.aura.ui.components

/**
 * ARCHIVO: FileManagerSheet.kt
 * FUNCIÓN: Hoja inferior para elegir las carpetas de música que AURA puede leer. Cada carpeta se
 *          guarda como tree URI SAF, se re-escanea al abrir el gestor y al tocarla, y se conserva
 *          entre actualizaciones de la app.
 * NAVEGACIÓN: AuraApp (showFileManagerSheet) -> FileManagerSheet -> AuraViewModel/LocalFileManager.
 */

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aura.data.storage.MusicFolder
import com.example.aura.data.storage.takePersistableReadPermission
import com.example.ui.theme.AuraAccentPink
import com.example.ui.theme.AuraAccentTeal
import com.example.ui.theme.AuraCardBorder
import com.example.ui.theme.AuraDarkSurface
import com.example.ui.theme.AuraDarkSurfaceVariant
import com.example.ui.theme.AuraPrimaryPurple
import com.example.ui.theme.AuraSecondaryBlue
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.AuraTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
/**
 * FileManagerSheet: lista de carpetas autorizadas y acciones para añadir, quitar o re-escanear.
 */
fun FileManagerSheet(
    sheetState: SheetState,
    isScanning: Boolean,
    statusMessage: String?,
    musicFolders: List<MusicFolder>,
    onDismiss: () -> Unit,
    onAddMusicFolder: (Uri) -> Unit,
    onRemoveMusicFolder: (MusicFolder) -> Unit,
    onRescanMusicFolder: (MusicFolder) -> Unit,
    onRescanMusicFolders: () -> Unit,
    onImportUri: (Uri) -> Unit
) {
    val context = LocalContext.current

    // Selector de archivos sueltos (importación manual, fuera de las carpetas autorizadas).
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            context.takePersistableReadPermission(uri)
            onImportUri(uri)
        }
    }

    // Selector de carpeta: el permiso persistente se toma en el ViewModel.
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            onAddMusicFolder(treeUri)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AuraDarkSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AuraPrimaryPurple),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Carpetas de música",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Carpetas de Música",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.semantics { heading() }
                        )
                        Text(
                            text = "AURA solo leerá las rutas que autorices",
                            style = MaterialTheme.typography.bodySmall,
                            color = AuraTextSecondary
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = AuraTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Estado del escaneo
            if (isScanning || statusMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = AuraDarkSurfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = AuraPrimaryPurple,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Text(
                            text = statusMessage ?: "Buscando archivos nuevos...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            modifier = Modifier.semantics {
                                liveRegion = LiveRegionMode.Polite
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Carpetas autorizadas",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = musicFolders.size.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = AuraTextSecondary
                )
            }
            Text(
                text = "Toca una carpeta para buscar archivos nuevos. La ruta se guarda y se mantiene al actualizar la app.",
                style = MaterialTheme.typography.bodySmall,
                color = AuraTextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (musicFolders.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = AuraDarkSurfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, AuraCardBorder)
                ) {
                    Box(
                        modifier = Modifier.padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = AuraSecondaryBlue,
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Aún no hay carpetas autorizadas",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Añade la ruta donde guardas tu música (por ejemplo, Música o una carpeta de tu tarjeta SD).",
                                style = MaterialTheme.typography.bodySmall,
                                color = AuraTextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(musicFolders, key = { it.uriString }) { folder ->
                        MusicFolderRow(
                            folder = folder,
                            onRescan = { onRescanMusicFolder(folder) },
                            onRemove = { onRemoveMusicFolder(folder) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ActionCard(
                title = "Añadir carpeta de música",
                subtitle = "Elige la ruta que AURA podrá leer y escanear",
                icon = Icons.Default.CreateNewFolder,
                iconColor = AuraPrimaryPurple,
                onClick = { folderPicker.launch(null) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRescanMusicFolders,
                    enabled = musicFolders.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Buscar nuevos",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedButton(
                    onClick = { filePicker.launch(arrayOf("audio/*")) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Archivos sueltos",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
/**
 * MusicFolderRow: carpeta autorizada. Al tocar la fila se buscan archivos nuevos; los iconos
 * permiten re-escanear o quitar la carpeta de la biblioteca.
 */
private fun MusicFolderRow(
    folder: MusicFolder,
    onRescan: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                role = Role.Button,
                onClickLabel = "Buscar archivos nuevos en ${folder.displayName}"
            ) { onRescan() },
        color = AuraDarkSurfaceVariant.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, AuraCardBorder)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AuraSecondaryBlue.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = AuraSecondaryBlue,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = folder.pathLabel.ifBlank { folder.uriString },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = AuraTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!folder.hasReadPermission) {
                    Text(
                        text = "Permiso pendiente · vuelve a añadir esta carpeta",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = AuraAccentPink
                    )
                }
            }

            IconButton(onClick = onRescan) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Buscar archivos nuevos en ${folder.displayName}",
                    tint = AuraAccentTeal,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Quitar carpeta ${folder.displayName}",
                    tint = AuraTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
/**
 * ActionCard: tarjeta de acción principal del gestor.
 */
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        color = AuraDarkSurfaceVariant,
        border = BorderStroke(1.dp, AuraCardBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AuraTextSecondary
                )
            }
        }
    }
}
