package com.example.aura.model

import android.net.Uri

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "Álbum Desconocido",
    val durationMs: Long = 0L,
    val uriString: String = "",
    val filePath: String? = null,
    val format: String = "FLAC", // FLAC, WAV, ALAC, MP3, AAC, OGG
    val sampleRate: Int = 44100, // 44100, 48000, 96000, 192000
    val bitDepth: Int = 16, // 16, 24, 32
    val bitrateKbps: Int = 1411,
    val isLossless: Boolean = true,
    val isFavorite: Boolean = false,
    val lyrics: String = "",
    val coverColorHex: Long = 0xFF8A5CF6,
    val dateAdded: Long = System.currentTimeMillis()
) {
    val durationFormatted: String
        get() {
            val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }

    val qualityBadge: String
        get() {
            return if (isLossless) {
                if (sampleRate >= 88200 || bitDepth >= 24) {
                    "HI-RES LOSSLESS • $format ${bitDepth}b/${sampleRate / 1000}kHz"
                } else {
                    "LOSSLESS • $format ${bitDepth}b/${sampleRate / 1000}kHz"
                }
            } else {
                "$format • ${bitrateKbps} kbps"
            }
        }
}

data class Playlist(
    val id: String,
    val name: String,
    val description: String = "",
    val coverColorHex: Long = 0xFF9155FD,
    val songIds: List<String> = emptyList(),
    val isCustom: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class Artist(
    val id: String,
    val name: String,
    val bio: String = "",
    val monthlyListeners: String = "1.2M oyentes",
    val isFollowing: Boolean = false,
    val popularSongIds: List<String> = emptyList()
)

data class LyricLine(
    val timeMs: Long,
    val text: String
)
