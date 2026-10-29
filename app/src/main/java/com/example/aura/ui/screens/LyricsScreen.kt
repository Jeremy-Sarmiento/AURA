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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aura.model.LyricLine
import com.example.aura.model.Song
import com.example.ui.theme.AuraDarkBackground
import com.example.ui.theme.AuraPrimaryPurple
import com.example.ui.theme.AuraPrimaryPurpleLight
import com.example.ui.theme.AuraScreenTitle
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.AuraTextSecondary

@Composable
fun LyricsScreen(
    currentSong: Song?,
    lyrics: List<LyricLine>,
    currentPositionMs: Long,
    onBack: () -> Unit,
    onSeekTo: (Long) -> Unit
) {
    val listState = rememberLazyListState()

    // Find active lyric index
    val activeIndex = lyrics.indexOfLast { it.timeMs <= currentPositionMs }.coerceAtLeast(0)

    LaunchedEffect(activeIndex) {
        if (lyrics.isNotEmpty() && activeIndex in lyrics.indices) {
            listState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraDarkBackground)
            .padding(horizontal = 24.dp)
            .testTag("lyrics_screen")
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
                text = "LETRAS",
                style = AuraScreenTitle,
                color = Color.White,
                modifier = Modifier.semantics { heading() }
            )

            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (currentSong != null) {
            Text(
                text = currentSong.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = currentSong.artist,
                style = MaterialTheme.typography.bodySmall,
                color = AuraTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        if (lyrics.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Esta pista no tiene letras sincronizadas",
                        style = MaterialTheme.typography.bodyLarge,
                        color = AuraTextMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "AURA lee letras en formato .lrc cuando están disponibles.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AuraTextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = AuraPrimaryPurple)
                    ) {
                        Text("Volver al reproductor")
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }

                itemsIndexed(lyrics) { index, line ->
                    val isActive = index == activeIndex

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(
                                role = Role.Button,
                                onClickLabel = "Ir a este verso"
                            ) { onSeekTo(line.timeMs) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = line.text,
                            style = if (isActive) {
                                MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 24.sp,
                                    lineHeight = 32.sp
                                )
                            } else {
                                MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 18.sp,
                                    lineHeight = 26.sp
                                )
                            },
                            color = if (isActive) AuraPrimaryPurpleLight else AuraTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(140.dp))
                }
            }
        }
    }
}
