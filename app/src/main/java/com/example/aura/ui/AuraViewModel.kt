package com.example.aura.ui

/**
 * ARCHIVO: AuraViewModel.kt
 * FUNCIÓN: Cerebro de la app. Mantiene el estado (pantalla, canciones, listas, búsqueda, archivos,
 *          ajustes), conecta Room + LocalFileManager + AudioPlayerManager y expone StateFlows y
 *          acciones a la UI Compose.
 * NAVEGACIÓN: MainActivity/AuraApp -> AuraViewModel -> data/local (Room), data/storage, player.
 */

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aura.data.local.AuraDatabase
import com.example.aura.data.local.PlaylistEntity
import com.example.aura.data.local.PlaylistSongEntity
import com.example.aura.data.local.SongEntity
import com.example.aura.data.storage.LocalFileManager
import com.example.aura.data.storage.MusicFolder
import com.example.aura.data.storage.releasePersistableReadPermission
import com.example.aura.data.storage.takePersistableReadPermission
import com.example.aura.model.Artist
import com.example.aura.model.LyricLine
import com.example.aura.model.Playlist
import com.example.aura.model.Song
import com.example.aura.player.AudioPlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * AuraScreen: pantallas navegables de la app. AuraApp hace el "when" sobre este enum.
 */
enum class AuraScreen {
    HOME,
    SEARCH,
    LIBRARY,
    PLAYLIST_DETAIL,
    ARTIST_DETAIL,
    PLAYER,
    LYRICS,
    QUEUE,
    SETTINGS
}

/**
 * AuraViewModel: estado central de AURA y única fuente de verdad para la UI.
 * Inicializa la base de datos, el escáner de archivos y el reproductor singleton.
 */
class AuraViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        // La selección vive en SharedPreferences: se conserva al actualizar la app.
        private const val MUSIC_FOLDERS_PREF_KEY = "music_folders_json"
    }

    private val db = AuraDatabase.getInstance(application)
    private val songDao = db.songDao()
    private val playlistDao = db.playlistDao()
    val fileManager = LocalFileManager(application)
    val playerManager = AudioPlayerManager.getInstance(application)

    // Current screen navigation
    private val _currentScreen = MutableStateFlow(AuraScreen.HOME)
    val currentScreen: StateFlow<AuraScreen> = _currentScreen.asStateFlow()

    // Navigation back stack for sub-screens
    private val screenStack = mutableListOf<AuraScreen>()

    // Songs from DB
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    // Playlists
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    // Selected playlist detail
    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    // Selected artist detail
    private val _selectedArtist = MutableStateFlow<Artist?>(null)
    val selectedArtist: StateFlow<Artist?> = _selectedArtist.asStateFlow()

    // Search query & active genre
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGenre = MutableStateFlow<String?>(null)
    val selectedGenre: StateFlow<String?> = _selectedGenre.asStateFlow()

    // Carpetas de música autorizadas por el usuario (SAF).
    private val _musicFolders = MutableStateFlow<List<MusicFolder>>(emptyList())
    val musicFolders: StateFlow<List<MusicFolder>> = _musicFolders.asStateFlow()

    // Evita que dos escaneos (inicio, abrir gestor, tocar carpeta) se pisen.
    private val scanMutex = Mutex()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Settings toggles
    private val settingsPrefs = application.getSharedPreferences("aura_settings", 0)
    private val _notificationsEnabled = MutableStateFlow(
        settingsPrefs.getBoolean("lockscreen_controls", true)
    )
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    // Filtered songs for Search screen
    val searchResults: StateFlow<List<Song>> = combine(_songs, _searchQuery, _selectedGenre) { allSongs, query, genre ->
        if (query.isBlank() && genre == null) {
            allSongs
        } else {
            allSongs.filter { song ->
                val matchesQuery = query.isBlank() ||
                        song.title.contains(query, ignoreCase = true) ||
                        song.artist.contains(query, ignoreCase = true) ||
                        song.album.contains(query, ignoreCase = true) ||
                        song.format.contains(query, ignoreCase = true)

                val matchesGenre = genre == null ||
                        song.album.contains(genre, ignoreCase = true) ||
                        song.title.contains(genre, ignoreCase = true) ||
                        song.artist.contains(genre, ignoreCase = true)

                matchesQuery && matchesGenre
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Parsed lyrics for current song
    val parsedLyrics: StateFlow<List<LyricLine>> = combine(playerManager.currentSong) { (song) ->
        val rawLyrics = song?.lyrics ?: ""
        parseLrcLyrics(rawLyrics)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        com.example.aura.service.MusicPlaybackService.showOnLockscreen = _notificationsEnabled.value
        loadInitialData()
        restoreMusicFolders()
    }

    /**
     * loadInitialData: crea las listas del sistema (Favoritas y Lossless) y abre dos flujos
     * reactivos: canciones de la BD y listas con sus ids de canciones.
     */
    private fun loadInitialData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (playlistDao.getPlaylistById("playlist_favorites") == null) {
                    playlistDao.insertPlaylist(
                        PlaylistEntity(
                            id = "playlist_favorites",
                            name = "Mis Favoritas",
                            description = "Tus temas preferidos siempre a mano",
                            coverColorHex = 0xFFE85D9E,
                            createdAt = System.currentTimeMillis() - 1000
                        )
                    )
                }
                if (playlistDao.getPlaylistById("playlist_lossless") == null) {
                    playlistDao.insertPlaylist(
                        PlaylistEntity(
                            id = "playlist_lossless",
                            name = "Audio Sin Pérdida (Hi-Res)",
                            description = "Colección de pistas FLAC y WAV de alta fidelidad",
                            coverColorHex = 0xFF9155FD,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("AuraViewModel", "Error creating default playlists", e)
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                songDao.deleteShortAudioAndVoiceNotes(60000L)
            } catch (_: Exception) {}

            // Cargar canciones de la BD
            songDao.getAllSongs().collect { entities ->
                _songs.value = entities.map { it.toDomain() }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            combine(
                playlistDao.getAllPlaylists(),
                playlistDao.getAllPlaylistSongs()
            ) { playlistEntities, playlistSongs ->
                val songsMap = playlistSongs.groupBy({ it.playlistId }, { it.songId })
                playlistEntities.map { p ->
                    Playlist(
                        id = p.id,
                        name = p.name,
                        description = p.description,
                        coverColorHex = p.coverColorHex,
                        songIds = songsMap[p.id] ?: emptyList(),
                        createdAt = p.createdAt
                    )
                }
            }.collect { list ->
                _playlists.value = list
                // Mantiene el detalle abierto al día al añadir o quitar canciones.
                _selectedPlaylist.value = _selectedPlaylist.value?.let { selected ->
                    list.find { it.id == selected.id }
                }
            }
        }
    }

    // Navigation
    // Cambia de pantalla y guarda la anterior en la pila para poder volver.
    fun navigateTo(screen: AuraScreen) {
        if (_currentScreen.value != screen) {
            screenStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    // Las pestañas son raíz: cambiar de pestaña no debe apilar historial.
    fun selectTab(screen: AuraScreen) {
        screenStack.clear()
        _currentScreen.value = screen
    }

    // Retrocede en la pila; si está vacía vuelve a HOME. Devuelve false si ya no puede volver.
    fun navigateBack(): Boolean {
        return if (screenStack.isNotEmpty()) {
            _currentScreen.value = screenStack.removeAt(screenStack.lastIndex)
            true
        } else if (_currentScreen.value != AuraScreen.HOME) {
            _currentScreen.value = AuraScreen.HOME
            true
        } else {
            false
        }
    }

    // Abre la pantalla de detalle de una lista de reproducción.
    fun openPlaylist(playlist: Playlist) {
        _selectedPlaylist.value = playlist
        navigateTo(AuraScreen.PLAYLIST_DETAIL)
    }

    // Construye un Artist a partir de las canciones locales y abre su detalle.
    fun openArtist(artistName: String) {
        val artistSongs = _songs.value.filter { it.artist.equals(artistName, ignoreCase = true) }
        val artist = Artist(
            id = "artist_${artistName.hashCode()}",
            name = artistName,
            bio = "Artista en tu biblioteca local de AURA",
            monthlyListeners = "${(100..999).random()}K oyentes locales",
            isFollowing = false,
            popularSongIds = artistSongs.map { it.id }
        )
        _selectedArtist.value = artist
        navigateTo(AuraScreen.ARTIST_DETAIL)
    }

    // Alterna el estado "siguiendo" del artista seleccionado (solo en memoria).
    fun toggleFollowArtist() {
        val current = _selectedArtist.value ?: return
        _selectedArtist.value = current.copy(isFollowing = !current.isFollowing)
    }

    // Search
    // Actualiza el texto de búsqueda; searchResults se recalcula automáticamente.
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Selecciona/deselecciona un género para filtrar los resultados.
    fun selectGenre(genre: String?) {
        _selectedGenre.value = if (_selectedGenre.value == genre) null else genre
    }

    // Song favorite toggle (Namida style: direct song property flag)
    // Marca/desmarca una canción como favorita y actualiza BD y memoria.
    fun toggleFavorite(songId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val song = _songs.value.find { it.id == songId } ?: return@launch
            val newFav = !song.isFavorite
            songDao.setFavorite(songId, newFav)
        }
    }

    // Playlists creation & management
    // Crea una lista personalizada con nombre, descripción y color.
    fun createPlaylist(name: String, description: String = "", colorHex: Long = 0xFF9155FD) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = "playlist_${UUID.randomUUID()}"
            val entity = PlaylistEntity(
                id = id,
                name = name,
                description = description,
                coverColorHex = colorHex,
                createdAt = System.currentTimeMillis()
            )
            playlistDao.insertPlaylist(entity)
            _statusMessage.value = "Lista creada: $name"
        }
    }

    // Elimina una lista y sus relaciones; si era la abierta, vuelve atrás.
    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistDao.deletePlaylist(playlistId)
            playlistDao.clearPlaylistSongs(playlistId)
            if (_selectedPlaylist.value?.id == playlistId) {
                withContext(Dispatchers.Main) { navigateBack() }
            }
            _statusMessage.value = "Lista eliminada"
        }
    }

    // Añade una canción a una lista.
    fun addSongToPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistDao.insertPlaylistSong(
                PlaylistSongEntity(
                    playlistId = playlistId,
                    songId = songId,
                    sortOrder = System.currentTimeMillis().toInt()
                )
            )
            _statusMessage.value = "Canción agregada a la lista"
        }
    }

    // Quita una canción de una lista.
    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistDao.removeSongFromPlaylist(playlistId, songId)
            _statusMessage.value = "Canción removida de la lista"
        }
    }

    // Local Storage Scanner & File Manager
    // Carga las carpetas guardadas y las re-escanea al iniciar la app.
    private fun restoreMusicFolders() {
        viewModelScope.launch(Dispatchers.IO) {
            val storedFolders = loadStoredMusicFolders()
            _musicFolders.value = storedFolders

            // La detección global anterior ya no forma parte de la biblioteca.
            try {
                songDao.deleteMediaStoreSongs()
            } catch (_: Exception) {}

            if (storedFolders.isNotEmpty()) {
                scanAndSyncFolders(storedFolders, announce = false)
            }
        }
    }

    // Vuelve a buscar archivos nuevos en todas las carpetas autorizadas.
    fun refreshMusicFolders() {
        val folders = _musicFolders.value
        if (folders.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            scanAndSyncFolders(folders, announce = true)
        }
    }

    // Escanea una sola carpeta; se usa al tocar la carpeta en el gestor.
    fun rescanMusicFolder(folder: MusicFolder) {
        viewModelScope.launch(Dispatchers.IO) {
            scanAndSyncFolders(listOf(folder), announce = true)
        }
    }

    // Añade (o vuelve a autorizar) una carpeta elegida con SAF y la escanea al momento.
    fun addMusicFolder(treeUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            getApplication<Application>().takePersistableReadPermission(treeUri)
            val folder = fileManager.describeFolder(treeUri)
            val current = _musicFolders.value
            val updated = if (current.any { it.uri == treeUri }) {
                current.map { if (it.uri == treeUri) folder else it }
            } else {
                current + folder
            }
            _musicFolders.value = updated
            saveMusicFolders(updated)
            scanAndSyncFolders(listOf(folder), announce = true)
        }
    }

    // Quita una carpeta, libera su permiso y elimina solo las pistas que pertenecían a ella.
    fun removeMusicFolder(folder: MusicFolder) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = _musicFolders.value.filterNot { it.uri == folder.uri }
            _musicFolders.value = updated
            saveMusicFolders(updated)
            getApplication<Application>().releasePersistableReadPermission(folder.uri)

            scanMutex.withLock {
                try {
                    val remainingPrefixes = updated.map { it.songUriPrefix }
                    val songs = songDao.getSongsByUriPrefix(folder.songUriPrefix)
                    // Si hay una subcarpeta todavía autorizada, sus canciones no se borran.
                    val songIds = songs
                        .filter { song ->
                            remainingPrefixes.none { prefix -> song.uriString.startsWith(prefix) }
                        }
                        .map { it.id }
                    if (songIds.isNotEmpty()) {
                        songDao.deleteSongsByIds(songIds)
                        playlistDao.deletePlaylistSongsForSongIds(songIds)
                    }
                    _statusMessage.value = "Carpeta quitada: ${folder.displayName}"
                } catch (e: Exception) {
                    _statusMessage.value = "Carpeta quitada. Error al limpiar sus canciones: ${e.message}"
                }
            }
        }
    }

    /**
     * scanAndSyncFolders: recorre cada carpeta con la lógica de Namida (BFS por SAF), inserta las
     * pistas nuevas y elimina de Room las que ya no existen. Respeta favoritos porque la inserción
     * usa IGNORE y solo borra las pistas ausentes.
     */
    private suspend fun scanAndSyncFolders(folders: List<MusicFolder>, announce: Boolean) {
        if (folders.isEmpty()) return

        scanMutex.withLock {
            _isScanning.value = true
            var newCount = 0
            var scannedCount = 0
            val failedFolders = mutableListOf<MusicFolder>()
            val accessibleFolders = mutableSetOf<String>()

            try {
                for (folder in folders) {
                    try {
                        val existing = songDao.getSongsByUriPrefix(folder.songUriPrefix)
                        val existingIds = existing.map { it.id }.toSet()
                        val scanResult = fileManager.scanFolderFromTree(folder.uri)
                        val scanned = scanResult.songs

                        val newSongs = scanned.filter { it.id !in existingIds }
                        if (scanned.isNotEmpty()) {
                            songDao.insertSongs(scanned.map { SongEntity.fromDomain(it) })
                        }

                        // Solo se borra lo que ya no existe; si un archivo existe pero falló la
                        // lectura de metadatos, se conserva en la biblioteca.
                        val missingIds = existing
                            .filter { it.uriString !in scanResult.discoveredUriStrings }
                            .map { it.id }
                        if (missingIds.isNotEmpty()) {
                            songDao.deleteSongsByIds(missingIds)
                            playlistDao.deletePlaylistSongsForSongIds(missingIds)
                        }

                        newCount += newSongs.size
                        scannedCount += scanned.size
                        accessibleFolders.add(folder.uriString)
                    } catch (e: Exception) {
                        android.util.Log.e("AuraViewModel", "Error scanning folder ${folder.displayName}", e)
                        failedFolders.add(folder)
                    }
                }

                // Una carpeta escaneada en esta sesión se muestra accesible aunque el proveedor
                // no soporte permisos persistentes.
                _musicFolders.value = _musicFolders.value.map { folder ->
                    folder.copy(
                        hasReadPermission = folder.uriString in accessibleFolders ||
                            fileManager.hasPersistedReadPermission(folder.uri)
                    )
                }

                val status = when {
                    scannedCount == 0 && failedFolders.isNotEmpty() ->
                        "No se pudo leer ${failedFolders.size} carpeta(s). Vuelve a autorizarlas."
                    newCount > 0 && failedFolders.isNotEmpty() ->
                        "Se encontraron $newCount archivos nuevos. ${failedFolders.size} carpeta(s) sin permiso."
                    newCount > 0 -> "Se encontraron $newCount archivos nuevos"
                    failedFolders.isNotEmpty() ->
                        "Biblioteca actualizada. ${failedFolders.size} carpeta(s) sin permiso."
                    announce -> "Biblioteca actualizada: no hay archivos nuevos"
                    else -> null
                }
                if (status != null) {
                    _statusMessage.value = status
                }
            } finally {
                _isScanning.value = false
            }
        }
    }

    private fun loadStoredMusicFolders(): List<MusicFolder> {
        val raw = settingsPrefs.getString(MUSIC_FOLDERS_PREF_KEY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val uriString = item.optString("uri").takeIf { it.isNotBlank() } ?: continue
                    val uri = Uri.parse(uriString)
                    val storedName = item.optString("name").takeIf { it.isNotBlank() }
                    val storedPath = item.optString("path").takeIf { it.isNotBlank() }
                    add(
                        MusicFolder(
                            uri = uri,
                            displayName = storedName ?: fileManager.getFolderDisplayName(uri),
                            pathLabel = storedPath ?: fileManager.getFolderPathLabel(uri),
                            hasReadPermission = fileManager.hasPersistedReadPermission(uri)
                        )
                    )
                }
            }.distinctBy { it.uriString }
        } catch (e: Exception) {
            android.util.Log.e("AuraViewModel", "Error reading saved music folders", e)
            emptyList()
        }
    }

    private fun saveMusicFolders(folders: List<MusicFolder>) {
        val array = JSONArray()
        folders.forEach { folder ->
            array.put(
                JSONObject()
                    .put("uri", folder.uriString)
                    .put("name", folder.displayName)
                    .put("path", folder.pathLabel)
            )
        }
        settingsPrefs.edit().putString(MUSIC_FOLDERS_PREF_KEY, array.toString()).apply()
    }

    // Importa una pista desde una URI concreta (intent, selector de archivos) y la reproduce.
    fun importSongFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            try {
                val song = fileManager.parseSongFromUri(uri)
                if (song != null) {
                    songDao.insertSong(SongEntity.fromDomain(song))
                    _statusMessage.value = "Pista importada: ${song.title}"
                    // No cortar lo que ya suena al abrir un archivo desde el gestor.
                    if (playerManager.currentSong.value == null) {
                        // MediaPlayer debe crearse/prepararse en el hilo principal.
                        withContext(Dispatchers.Main) {
                            playerManager.playSong(song, _songs.value + song)
                        }
                    }
                } else {
                    _statusMessage.value = "No se pudo leer el archivo de audio"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error importando: ${e.message}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    // Limpia el mensaje de estado tras mostrarlo en el Snackbar.
    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    // Activa/desactiva las notificaciones de reproducción (preferencia).
    fun toggleNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        settingsPrefs.edit().putBoolean("lockscreen_controls", enabled).apply()
        com.example.aura.service.MusicPlaybackService.showOnLockscreen = enabled
    }

    // Lyrics parser
    // Convierte texto LRC ([mm:ss.xx]texto) en una lista de LyricLine sincronizadas.
    private fun parseLrcLyrics(lrc: String): List<LyricLine> {
        if (lrc.isBlank()) {
            return emptyList()
        }

        val lines = mutableListOf<LyricLine>()
        val regex = Regex("""\[(\d{2}):(\d{2})\.?(\d{0,3})\](.*)""")

        for (rawLine in lrc.lines()) {
            val match = regex.find(rawLine.trim())
            if (match != null) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val msStr = match.groupValues[3].padEnd(3, '0')
                val ms = msStr.toLongOrNull() ?: 0L
                val totalMs = min * 60000L + sec * 1000L + ms
                val text = match.groupValues[4].trim()
                if (text.isNotEmpty()) {
                    lines.add(LyricLine(totalMs, text))
                }
            } else if (rawLine.isNotBlank()) {
                lines.add(LyricLine(lines.size * 4000L, rawLine.trim()))
            }
        }

        return lines
    }

    // Al destruirse el ViewModel, detiene y libera el reproductor y el servicio.
    override fun onCleared() {
        super.onCleared()
        playerManager.stop()
        playerManager.releasePlayer()
        com.example.aura.service.MusicPlaybackService.stopService(getApplication())
    }
}
