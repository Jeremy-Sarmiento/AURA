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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aura.model.Song
import com.example.ui.theme.AuraCardBorder
import com.example.ui.theme.AuraDarkBackground
import com.example.ui.theme.AuraDarkSurfaceVariant
import com.example.ui.theme.AuraPrimaryPurple
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.AuraTextSecondary
import com.example.ui.theme.GenreElectro
import com.example.ui.theme.GenreFocus
import com.example.ui.theme.GenreIndie
import com.example.ui.theme.GenreParty
import com.example.ui.theme.GenrePop
import com.example.ui.theme.GenreRock

data class GenreItem(val name: String, val color: Color)

@Composable
fun SearchScreen(
    query: String,
    selectedGenre: String?,
    results: List<Song>,
    onQueryChange: (String) -> Unit,
    onSelectGenre: (String?) -> Unit,
    onSongClick: (Song) -> Unit
) {
    val genres = listOf(
        GenreItem("Pop", GenrePop),
        GenreItem("Rock", GenreRock),
        GenreItem("Electro", GenreElectro),
        GenreItem("Indie", GenreIndie),
        GenreItem("Focus", GenreFocus),
        GenreItem("Party", GenreParty)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraDarkBackground)
            .padding(horizontal = 20.dp)
            .testTag("search_screen")
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Screen title "BUSCAR" matching Mockup 03
            Text(
                text = "BUSCAR",
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

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar matching Mockup 03
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .testTag("search_text_input"),
                placeholder = {
                    Text(
                        text = "Buscar canciones, artistas, FLAC...",
                        color = AuraTextSecondary,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = AuraTextSecondary
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty() || selectedGenre != null) {
                        IconButton(onClick = {
                            onQueryChange("")
                            onSelectGenre(null)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Borrar",
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

            // Grid of 6 Genre Cards matching Mockup 03 exactly
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Row 1: Pop & Rock
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    GenreCard(
                        title = genres[0].name,
                        cardColor = genres[0].color,
                        isSelected = selectedGenre == genres[0].name,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectGenre(genres[0].name) }
                    )
                    GenreCard(
                        title = genres[1].name,
                        cardColor = genres[1].color,
                        isSelected = selectedGenre == genres[1].name,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectGenre(genres[1].name) }
                    )
                }

                // Row 2: Electro & Indie
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    GenreCard(
                        title = genres[2].name,
                        cardColor = genres[2].color,
                        isSelected = selectedGenre == genres[2].name,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectGenre(genres[2].name) }
                    )
                    GenreCard(
                        title = genres[3].name,
                        cardColor = genres[3].color,
                        isSelected = selectedGenre == genres[3].name,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectGenre(genres[3].name) }
                    )
                }

                // Row 3: Focus & Party
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    GenreCard(
                        title = genres[4].name,
                        cardColor = genres[4].color,
                        isSelected = selectedGenre == genres[4].name,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectGenre(genres[4].name) }
                    )
                    GenreCard(
                        title = genres[5].name,
                        cardColor = genres[5].color,
                        isSelected = selectedGenre == genres[5].name,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectGenre(genres[5].name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Results Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedGenre != null) "Género: $selectedGenre (${results.size})"
                    else if (query.isNotBlank()) "Resultados (${results.size})"
                    else "Todas las pistas disponibles",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

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
                        text = "No se encontraron pistas que coincidan",
                        color = AuraTextMuted,
                        style = MaterialTheme.typography.bodyMedium
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
                        .clickable { onSongClick(song) },
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
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = AuraPrimaryPurple
                                        )
                                    }
                                }
                            }
                        }

                        IconButton(onClick = { onSongClick(song) }) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Reproducir",
                                tint = AuraPrimaryPurple
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun GenreCard(
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
            .background(if (isSelected) cardColor else cardColor.copy(alpha = 0.88f))
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.background(Color.White.copy(alpha = 0.15f))
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            ),
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}
