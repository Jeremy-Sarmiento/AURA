package com.example.aura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aura.model.Playlist
import com.example.aura.model.Song
import com.example.ui.theme.AuraAccentPink
import com.example.ui.theme.AuraBadge
import com.example.ui.theme.AuraDarkBackground
import com.example.ui.theme.AuraDarkSurface
import com.example.ui.theme.AuraDarkSurfaceVariant
import com.example.ui.theme.AuraDanger
import com.example.ui.theme.AuraPrimaryPurple
import com.example.ui.theme.AuraPrimaryPurpleLight
import com.example.ui.theme.AuraScreenTitle
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.AuraTextSecondary

@Composable
fun PlaylistScreen(
    playlist: Playlist,
    songs: List<Song>,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onSongClick: (Song) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRemoveSong: ((String) -> Unit)? = null,
    onDeletePlaylist: (() -> Unit)? = null
) {
    var confirmDelete by remember { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = AuraDarkSurface,
            title = { Text("Eliminar lista", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "\"${playlist.name}\" y su contenido se eliminarán. Esta acción no se puede deshacer.",
                    color = AuraTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        onDeletePlaylist?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraDanger)
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Cancelar", color = AuraTextSecondary)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraDarkBackground)
            .padding(horizontal = 20.dp)
            .testTag("playlist_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
    ) {
        item {
            // Top Header: Back arrow & "PLAYLIST" title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White
                    )
                }

                Text(
                    text = "PLAYLIST",
                    style = AuraScreenTitle,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() }
                )

                if (onDeletePlaylist != null) {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Eliminar lista",
                            tint = AuraTextSecondary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Playlist name & details
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${songs.size} pistas • ${playlist.description.ifEmpty { "Audio de alta fidelidad" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AuraTextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Play-all button: disabled and visually muted when the list is empty.
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .shadow(
                            elevation = if (songs.isEmpty()) 0.dp else 20.dp,
                            shape = CircleShape,
                            spotColor = AuraPrimaryPurple,
                            ambientColor = AuraPrimaryPurple
                        )
                        .clip(CircleShape)
                        .background(if (songs.isEmpty()) AuraDarkSurfaceVariant else AuraPrimaryPurple)
                        .clickable(
                            enabled = songs.isNotEmpty(),
                            role = Role.Button,
                            onClickLabel = "Reproducir lista"
                        ) { onPlayAll() }
                        .testTag("playlist_play_all_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Reproducir lista",
                        tint = if (songs.isEmpty()) AuraTextMuted else Color.White,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        if (songs.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No hay pistas en esta lista",
                        style = MaterialTheme.typography.bodyLarge,
                        color = AuraTextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Añade canciones desde Biblioteca con el menú de cada pista.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AuraTextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                val numberFormatted = "%02d".format(index + 1)

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Reproducir ${song.title}"
                        ) { onSongClick(song) },
                    color = AuraDarkSurfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = numberFormatted,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = AuraTextSecondary,
                            modifier = Modifier.width(32.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = song.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AuraTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (song.isLossless) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = song.format,
                                        style = AuraBadge,
                                        color = AuraPrimaryPurpleLight
                                    )
                                }
                            }
                        }

                        if (onRemoveSong != null) {
                            IconButton(onClick = { onRemoveSong(song.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Quitar ${song.title} de la lista",
                                    tint = AuraTextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        IconToggleButton(
                            checked = song.isFavorite,
                            onCheckedChange = { onToggleFavorite(song.id) }
                        ) {
                            Icon(
                                imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorito",
                                tint = if (song.isFavorite) AuraAccentPink else AuraTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
