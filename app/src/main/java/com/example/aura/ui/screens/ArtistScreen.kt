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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.aura.model.Artist
import com.example.aura.model.Song
import com.example.ui.theme.AuraDarkBackground
import com.example.ui.theme.AuraDarkSurfaceVariant
import com.example.ui.theme.AuraPrimaryPurple
import com.example.ui.theme.AuraPrimaryPurpleLight
import com.example.ui.theme.AuraScreenTitle
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.AuraTextSecondary

@Composable
fun ArtistScreen(
    artist: Artist,
    songs: List<Song>,
    onBack: () -> Unit,
    onFollowClick: () -> Unit,
    onSongClick: (Song) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraDarkBackground)
            .padding(horizontal = 20.dp)
            .testTag("artist_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
    ) {
        item {
            // Top Header: Back arrow & "ARTISTA" title
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
                    text = "ARTISTA",
                    style = AuraScreenTitle,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() }
                )

                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(AuraDarkSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = AuraPrimaryPurpleLight,
                        modifier = Modifier.size(72.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "${songs.size} pistas en tu biblioteca",
                    style = MaterialTheme.typography.bodySmall,
                    color = AuraTextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (artist.isFollowing) {
                    OutlinedButton(
                        onClick = onFollowClick,
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, AuraPrimaryPurple),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("Siguiendo", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onFollowClick,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AuraPrimaryPurple,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Seguir", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "POPULARES",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = AuraTextSecondary,
                modifier = Modifier.semantics { heading() }
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        if (songs.isEmpty()) {
            item {
                Text(
                    text = "No hay pistas de este artista en tu biblioteca",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AuraTextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                )
            }
        } else {
            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
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
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = AuraTextSecondary,
                            modifier = Modifier.width(26.dp)
                        )

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(song.coverColorHex)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
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
                                text = song.album,
                                style = MaterialTheme.typography.bodySmall,
                                color = AuraTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
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
