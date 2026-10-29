package com.example.aura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aura.model.Song
import com.example.aura.player.RepeatMode
import com.example.ui.theme.AuraBadge
import com.example.ui.theme.AuraCardBorder
import com.example.ui.theme.AuraDarkBackground
import com.example.ui.theme.AuraDarkSurfaceVariant
import com.example.ui.theme.AuraPrimaryPurple
import com.example.ui.theme.AuraPrimaryPurpleLight
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.AuraTextSecondary

@Composable
fun PlayerScreen(
    currentSong: Song?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    sleepTimerRemainingMs: Long,
    hasLyrics: Boolean,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenSongInfo: () -> Unit,
    onOpenArtist: (String) -> Unit
) {
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val effectiveDuration = if (durationMs > 0) durationMs else currentSong?.durationMs ?: 1L
    val currentProgress = if (isDraggingSlider) {
        dragProgress
    } else {
        (currentPositionMs.toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f)
    }

    val elapsedText = formatMs(if (isDraggingSlider) (dragProgress * effectiveDuration).toLong() else currentPositionMs)
    val totalText = formatMs(effectiveDuration)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraDarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .testTag("player_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("player_close_button")
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Cerrar",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = "REPRODUCTOR",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontSize = 16.sp
                ),
                color = Color.White,
                modifier = Modifier.semantics { heading() }
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenSleepTimer) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Temporizador",
                        tint = if (sleepTimerRemainingMs > 0) AuraPrimaryPurple else AuraPrimaryPurpleLight,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = onOpenSongInfo) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Información de pista",
                        tint = AuraPrimaryPurpleLight,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Album art acotada: en pantallas compactas y en horizontal no debe empujar los controles fuera.
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .heightIn(max = 280.dp)
                .aspectRatio(1f)
                .shadow(
                    elevation = 28.dp,
                    shape = RoundedCornerShape(28.dp),
                    spotColor = Color(currentSong?.coverColorHex ?: 0xFF9155FD),
                    ambientColor = Color(currentSong?.coverColorHex ?: 0xFF9155FD)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(Color(currentSong?.coverColorHex ?: 0xFF9155FD)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(110.dp)
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Title & Artist
        Text(
            text = currentSong?.title ?: "Sin pista seleccionada",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = currentSong?.artist ?: "AURA Music",
            style = MaterialTheme.typography.bodyLarge,
            color = AuraTextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = if (currentSong != null) {
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Ver artista ${currentSong.artist}"
                    ) { onOpenArtist(currentSong.artist) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            } else {
                Modifier
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Hi-Res Lossless quality badge
        if (currentSong != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AuraDarkSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, AuraCardBorder)
            ) {
                Text(
                    text = currentSong.qualityBadge,
                    style = AuraBadge,
                    color = AuraPrimaryPurpleLight,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Progress scrubber
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = currentProgress,
                onValueChange = {
                    isDraggingSlider = true
                    dragProgress = it
                },
                onValueChangeFinished = {
                    val targetMs = (dragProgress * effectiveDuration).toLong()
                    onSeekTo(targetMs)
                    isDraggingSlider = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Posición de reproducción"
                        stateDescription = "$elapsedText de $totalText"
                    },
                colors = SliderDefaults.colors(
                    thumbColor = AuraPrimaryPurple,
                    activeTrackColor = AuraPrimaryPurple,
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = elapsedText,
                    style = MaterialTheme.typography.bodySmall,
                    color = AuraTextMuted
                )
                Text(
                    text = totalText,
                    style = MaterialTheme.typography.bodySmall,
                    color = AuraTextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Playback controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleShuffle,
                modifier = Modifier.semantics {
                    stateDescription = if (isShuffle) "Activado" else "Desactivado"
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Aleatorio",
                    tint = if (isShuffle) AuraPrimaryPurpleLight else AuraTextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                onClick = onPrevious,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Anterior",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = CircleShape,
                        spotColor = AuraPrimaryPurple,
                        ambientColor = AuraPrimaryPurple
                    )
                    .clip(CircleShape)
                    .background(AuraPrimaryPurple)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = if (isPlaying) "Pausar" else "Reproducir"
                    ) { onTogglePlayPause() }
                    .testTag("player_play_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Siguiente",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(
                onClick = onToggleRepeat,
                modifier = Modifier.semantics {
                    stateDescription = when (repeatMode) {
                        RepeatMode.OFF -> "Desactivado"
                        RepeatMode.ALL -> "Repetir todo"
                        RepeatMode.ONE -> "Repetir una pista"
                    }
                }
            ) {
                Icon(
                    imageVector = when (repeatMode) {
                        RepeatMode.ONE -> Icons.Default.RepeatOne
                        RepeatMode.ALL, RepeatMode.OFF -> Icons.Default.Repeat
                    },
                    contentDescription = "Repetir",
                    tint = when (repeatMode) {
                        RepeatMode.OFF -> AuraTextSecondary
                        else -> AuraPrimaryPurpleLight
                    },
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Bottom shortcuts. "Letras" solo aparece si la pista tiene letras.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (hasLyrics) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(role = Role.Button, onClickLabel = "Abrir letras") { onOpenLyrics() }
                        .testTag("player_lyrics_shortcut"),
                    color = AuraDarkSurfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = null,
                            tint = AuraPrimaryPurpleLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Letras",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(role = Role.Button, onClickLabel = "Abrir cola") { onOpenQueue() }
                    .testTag("player_queue_shortcut"),
                color = AuraDarkSurfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = null,
                        tint = AuraPrimaryPurpleLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cola",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
