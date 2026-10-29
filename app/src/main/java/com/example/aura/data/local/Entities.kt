package com.example.aura.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.aura.model.Song

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uriString: String,
    val filePath: String?,
    val format: String,
    val sampleRate: Int,
    val bitDepth: Int,
    val bitrateKbps: Int,
    val isLossless: Boolean,
    val isFavorite: Boolean,
    val lyrics: String,
    val coverColorHex: Long,
    val dateAdded: Long
) {
    fun toDomain(): Song = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        uriString = uriString,
        filePath = filePath,
        format = format,
        sampleRate = sampleRate,
        bitDepth = bitDepth,
        bitrateKbps = bitrateKbps,
        isLossless = isLossless,
        isFavorite = isFavorite,
        lyrics = lyrics,
        coverColorHex = coverColorHex,
        dateAdded = dateAdded
    )

    companion object {
        fun fromDomain(song: Song): SongEntity = SongEntity(
            id = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            durationMs = song.durationMs,
            uriString = song.uriString,
            filePath = song.filePath,
            format = song.format,
            sampleRate = song.sampleRate,
            bitDepth = song.bitDepth,
            bitrateKbps = song.bitrateKbps,
            isLossless = song.isLossless,
            isFavorite = song.isFavorite,
            lyrics = song.lyrics,
            coverColorHex = song.coverColorHex,
            dateAdded = song.dateAdded
        )
    }
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val coverColorHex: Long,
    val createdAt: Long
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"]
)
data class PlaylistSongEntity(
    val playlistId: String,
    val songId: String,
    val sortOrder: Int
)
