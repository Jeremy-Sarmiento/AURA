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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aura.model.Song
import com.example.ui.theme.AuraBadge
import com.example.ui.theme.AuraCardBorder
import com.example.ui.theme.AuraDarkBackground
import com.example.ui.theme.AuraDarkSurfaceVariant
import com.example.ui.theme.AuraPrimaryPurple
import com.example.ui.theme.AuraPrimaryPurpleLight
import com.example.ui.theme.AuraScreenTitle
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.AuraTextSecondary
import com.example.ui.theme.GenreElectro
import com.example.ui.theme.GenreFocus
import com.example.ui.theme.GenreIndie
import com.example.ui.theme.GenreParty
import com.example.ui.theme.GenrePop
import com.example.ui.theme.GenreRock

private data class ArtistFilter(val name: String, val color: Color)

private val filterColors = listOf(
    GenrePop, GenreRock, GenreElectro, GenreIndie, GenreFocus, GenreParty
)

@Composable
fun SearchScreen(
    query: String,
    selectedArtist: String?,
    songs: List<Song>,
    results: List<Song>,
    onQueryChange: (String) -> Unit,
    onSelectArtist: (String?) -> Unit,
    onSongClick: (Song) -> Unit
) {
    // Filtros derivados de la biblioteca real: nunca ofrecen artistas sin pistas.
    val artists = songs.map { it.artist }
        .distinct()
        .sorted()
        .take(6)
        .mapIndexed { index, name -> ArtistFilter(name, filterColors[index % filterColors.size]) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraDarkBackground)
            .padding(horizontal = 20.dp)
            .testTag("search_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
    ) {
        item {
            // Screen title "BUSCAR"
            Text(
                text = "BUSCAR",
                style = AuraScreenTitle,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .semantics { heading() }
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .testTag("search_text_input"),
                label = { Text("Buscar") },
                placeholder = {
                    Text(
                        text = "Canciones, artistas, FLAC...",
                        color = AuraTextSecondary,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = AuraTextSecondary
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty() || selectedArtist != null) {
                        IconButton(onClick = {
                            onQueryChange("")
                            onSelectArtist(null)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Borrar búsqueda",
                                tint = AuraTextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = AuraDarkSurfaceVariant,
                    unfocusedContainerColor = AuraDarkSurfaceVariant,
                    focusedBorderColor = AuraPrimaryPurple,
                    unfocusedBorderColor = AuraCardBorder
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (artists.isNotEmpty()) {
                Text(
                    text = "Artistas de tu biblioteca",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(modifier = Modifier.height(10.dp))

                artists.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        row.forEach { artist ->
                            ArtistCard(
                                title = artist.name,
                                cardColor = artist.color,
                                isSelected = selectedArtist == artist.name,
                                modifier = Modifier.weight(1f),
                                onClick = { onSelectArtist(artist.name) }
                            )
                        }
                        if (row.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Results Header
            Text(
                text = when {
                    selectedArtist != null -> "Artista: $selectedArtist (${results.size})"
                    query.isNotBlank() -> "Resultados (${results.size})"
                    else -> "Todas las pistas disponibles"
                },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier
                    .semantics {
                        heading()
                        liveRegion = LiveRegionMode.Polite
                    }
            )

            Spacer(modifier = Modifier.height(10.dp))
        }

        if (results.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (songs.isEmpty()) {
                            "Tu biblioteca está vacía. Escanea o importa música desde Inicio."
                        } else {
                            "No se encontraron pistas que coincidan"
                        },
                        color = AuraTextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(results, key = { it.id }) { song ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Reproducir ${song.title}"
                        ) { onSongClick(song) },
                    color = AuraDarkSurfaceVariant.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(song.coverColorHex)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

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
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(AuraPrimaryPurple.copy(alpha = 0.25f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = song.format,
                                            style = AuraBadge,
                                            color = AuraPrimaryPurpleLight
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = song.durationFormatted,
                            style = MaterialTheme.typography.bodySmall,
                            color = AuraTextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistCard(
    title: String,
    cardColor: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cardColor)
            .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                onClick = onClick
            )
            .then(
                if (isSelected) {
                    Modifier
                        .background(Color.White.copy(alpha = 0.18f))
                        .semantics { contentDescription = "$title, seleccionado" }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            ),
            // Texto oscuro: pasa AA en los seis colores de tarjeta (4.76:1 a 8.06:1).
            color = AuraDarkBackground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}
