package com.example.aura.data.local

/**
 * ARCHIVO: Daos.kt
 * FUNCIÓN: Interfaces DAO de Room con las consultas de canciones y listas de reproducción.
 *          SongDao -> tabla songs; PlaylistDao -> tablas playlists / playlist_songs.
 * NAVEGACIÓN: AuraDatabase crea los DAO y AuraViewModel los consume en sus corrutinas.
 */

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * SongDao: acceso a la tabla "songs".
 * Expone Flow para observación reactiva e inserciones/borrados suspensos.
 */
@Dao
interface SongDao {
    // Todas las canciones, las más recientes primero.
    @Query("SELECT * FROM songs ORDER BY dateAdded DESC")
    fun getAllSongs(): Flow<List<SongEntity>>

    // Solo las canciones marcadas como favoritas, orden alfabético.
    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteSongs(): Flow<List<SongEntity>>

    // Busca una canción concreta por su id.
    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    suspend fun getSongById(id: String): SongEntity?

    // IGNORE: re-escanear o reimportar no debe borrar isFavorite ni dateAdded de una pista existente.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSongs(songs: List<SongEntity>)

    // IGNORE: igual para una sola canción.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSong(song: SongEntity)

    // Activa/desactiva el favorito de una canción.
    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :songId")
    suspend fun setFavorite(songId: String, isFavorite: Boolean)

    // Elimina una canción por id.
    @Query("DELETE FROM songs WHERE id = :songId")
    suspend fun deleteSong(songId: String)

    // Elimina audios cortos (menos de 1 minuto / 60,000 ms), notas de voz de WhatsApp y tonos.
    @Query("DELETE FROM songs WHERE durationMs < :minDurationMs OR title LIKE 'PTT-%' OR title LIKE 'AUD-%' OR filePath LIKE '%WhatsApp%' OR filePath LIKE '%Ringtones%' OR filePath LIKE '%Notifications%'")
    suspend fun deleteShortAudioAndVoiceNotes(minDurationMs: Long = 60000L)

    // Vacía la tabla de canciones (reinicio de biblioteca).
    @Query("DELETE FROM songs")
    suspend fun clearAll()
}

/**
 * PlaylistDao: acceso a listas de reproducción y su relación con canciones.
 */
@Dao
interface PlaylistDao {
    // Todas las listas, las más recientes primero.
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    // Busca una lista concreta por su id.
    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistById(id: String): PlaylistEntity?

    // Todas las relaciones lista-canción (se agrupan en el ViewModel).
    @Query("SELECT * FROM playlist_songs")
    fun getAllPlaylistSongs(): Flow<List<PlaylistSongEntity>>

    // Crea o actualiza una lista.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    // Elimina la lista (sus relaciones se limpian aparte).
    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    // Ids de canciones de una lista, respetando sortOrder.
    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId ORDER BY sortOrder ASC")
    fun getSongIdsForPlaylist(playlistId: String): Flow<List<String>>

    // Añade o actualiza una canción dentro de una lista.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSong(item: PlaylistSongEntity)

    // Quita una canción concreta de una lista.
    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String)

    // Vacía todas las canciones de una lista.
    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: String)
}
