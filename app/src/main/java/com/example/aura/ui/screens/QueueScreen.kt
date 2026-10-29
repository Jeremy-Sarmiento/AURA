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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.aura.model.Song
import com.example.ui.theme.AuraBadge
import com.example.ui.theme.AuraDarkBackground
import com.example.ui.theme.AuraDarkSurfaceVariant
import com.example.ui.theme.AuraPrimaryPurple
import com.example.ui.theme.AuraPrimaryPurpleLight
import com.example.ui.theme.AuraScreenTitle
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.AuraTextSecondary

@Composable
fun QueueScreen(
    currentSong: Song?,
    queue: List<Song>,
    currentIndex: Int,
    onBack: () -> Unit,
    onSongClick: (Song) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onBackToLibrary: () -> Unit
) {
    val upcomingSongs = queue.filterIndexed { index, _ -> index > currentIndex }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraDarkBackground)
            .padding(horizontal = 20.dp)
            .testTag("queue_screen")
    ) {
        Spacer(modifier = Modifier.height(16.dp))

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
                text = "COLA DE REPRODUCCIÓN",
                style = AuraScreenTitle.copy(fontSize = 16.sp, letterSpacing = 1.5.sp),
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )

            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "REPRODUCIENDO",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = AuraTextSecondary,
            modifier = Modifier.semantics { heading() }
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (currentSong != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)),
                color = AuraDarkSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, AuraPrimaryPurple.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(currentSong.coverColorHex)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentSong.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentSong.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = AuraTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (currentSong.isLossless) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentSong.format,
                                    style = AuraBadge,
                                    color = AuraPrimaryPurpleLight
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "SIGUIENTE (${upcomingSongs.size})",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = AuraTextSecondary,
            modifier = Modifier.semantics { heading() }
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (upcomingSongs.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No hay canciones siguientes en la cola",
                    color = AuraTextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onBackToLibrary,
                    colors = ButtonDefaults.buttonColors(containerColor = AuraPrimaryPurple)
                ) {
                    Icon(Icons.Default.LibraryMusic, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ir a la biblioteca")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                itemsIndexed(upcomingSongs) { index, song ->
                    val actualQueueIndex = currentIndex + 1 + index

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
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
                                    .size(42.dp)
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
                                Text(
                                    text = song.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AuraTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(onClick = { onRemoveFromQueue(actualQueueIndex) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Quitar ${song.title} de la cola",
                                    tint = AuraTextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
