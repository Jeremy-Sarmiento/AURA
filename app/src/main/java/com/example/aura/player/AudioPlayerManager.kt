package com.example.aura.player

/**
 * ARCHIVO: AudioPlayerManager.kt
 * FUNCIÓN: Núcleo de reproducción. Envuelve MediaPlayer, mantiene el estado observable
 *          (pista actual, play/pause, posición, cola, shuffle, repeat, sleep timer) y expone
 *          las acciones de control. Es un singleton compartido con el servicio de notificación.
 * NAVEGACIÓN: AuraViewModel/AuraApp/PlayerScreen -> AudioPlayerManager -> MusicPlaybackService.
 */

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.example.aura.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class RepeatMode {
    OFF, ALL, ONE
}

/**
 * AudioPlayerManager: gestor único de reproducción de la app.
 * Todos los estados son StateFlow para que Compose se actualice automáticamente.
 */
class AudioPlayerManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private var sleepTimerJob: Job? = null
    private val _sleepTimerRemainingMs = MutableStateFlow(0L)
    val sleepTimerRemainingMs: StateFlow<Long> = _sleepTimerRemainingMs.asStateFlow()

    // Programa el apagado automático: al agotarse, detiene la reproducción y el servicio.
    fun setSleepTimer(minutes: Int) {
        cancelSleepTimer()
        if (minutes <= 0) return
        val totalMs = minutes * 60 * 1000L
        _sleepTimerRemainingMs.value = totalMs
        sleepTimerJob = scope.launch {
            var remaining = totalMs
            while (isActive && remaining > 0) {
                delay(1000L)
                remaining -= 1000L
                _sleepTimerRemainingMs.value = remaining.coerceAtLeast(0L)
            }
            if (isActive && remaining <= 0) {
                stop()
                try {
                    com.example.aura.service.MusicPlaybackService.stopService(context)
                } catch (_: Exception) {}
            }
        }
    }

    // Cancela el temporizador de apagado y reinicia la cuenta atrás.
    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingMs.value = 0L
    }

    init {
        initMediaPlayer()
    }

    // Crea/reinicia MediaPlayer y registra los listeners de preparación, fin y error.
    private fun initMediaPlayer() {
        releasePlayer()
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnCompletionListener {
                handleTrackCompletion()
            }
            setOnErrorListener { _, what, extra ->
                Log.e("AudioPlayerManager", "MediaPlayer error: what=$what extra=$extra")
                _isPlaying.value = false
                true
            }
            setOnPreparedListener { mp ->
                _durationMs.value = mp.duration.toLong().coerceAtLeast(0L)
                mp.start()
                _isPlaying.value = true
                startProgressTracker()
                try {
                    com.example.aura.service.MusicPlaybackService.startService(context)
                } catch (e: Exception) {
                    Log.w("AudioPlayerManager", "Notification service start: ${e.message}")
                }
            }
        }
    }

    // Reproduce una canción y, si se indica, reemplaza la cola actual.
    fun playSong(song: Song, newQueue: List<Song>? = null) {
        if (newQueue != null && newQueue.isNotEmpty()) {
            _queue.value = newQueue
            val idx = newQueue.indexOfFirst { it.id == song.id }
            _currentIndex.value = if (idx >= 0) idx else 0
        } else if (_queue.value.none { it.id == song.id }) {
            _queue.value = _queue.value + song
            _currentIndex.value = _queue.value.lastIndex
        } else {
            val idx = _queue.value.indexOfFirst { it.id == song.id }
            if (idx >= 0) _currentIndex.value = idx
        }

        playTrack(song)
    }

    // Carga la URI de la pista en MediaPlayer y prepara la reproducción asíncrona.
    private fun playTrack(song: Song) {
        _currentSong.value = song
        _currentPositionMs.value = 0L
        _durationMs.value = song.durationMs

        try {
            if (mediaPlayer == null) {
                initMediaPlayer()
            } else {
                mediaPlayer?.reset()
            }

            val uri = Uri.parse(song.uriString)
            mediaPlayer?.setDataSource(context, uri)
            mediaPlayer?.prepareAsync()
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Failed to play ${song.title}", e)
            _isPlaying.value = false
        }
    }

    // Alterna entre reproducir y pausar; si no hay pista activa, arranca la primera de la cola.
    fun togglePlayPause() {
        val mp = mediaPlayer ?: return
        if (_currentSong.value == null && _queue.value.isNotEmpty()) {
            playTrack(_queue.value.first())
            return
        }

        if (mp.isPlaying) {
            mp.pause()
            _isPlaying.value = false
            stopProgressTracker()
        } else {
            mp.start()
            _isPlaying.value = true
            startProgressTracker()
        }
        try {
            com.example.aura.service.MusicPlaybackService.startService(context)
        } catch (_: Exception) {}
    }

    // Pausa y vuelve al inicio de la pista actual.
    fun stop() {
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.pause()
                }
                mp.seekTo(0)
            }
        } catch (_: Exception) {}
        _isPlaying.value = false
        stopProgressTracker()
    }

    // Salta a una posición concreta de la pista (ms).
    fun seekTo(positionMs: Long) {
        val mp = mediaPlayer ?: return
        try {
            mp.seekTo(positionMs.toInt())
            _currentPositionMs.value = positionMs
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Seek error", e)
        }
    }

    // Pasa a la siguiente pista (aleatoria si shuffle está activo; respeta repeat ALL).
    fun next() {
        val q = _queue.value
        if (q.isEmpty()) return

        if (_isShuffle.value && q.size > 1) {
            var nextIdx = (q.indices).random()
            if (nextIdx == _currentIndex.value) {
                nextIdx = (nextIdx + 1) % q.size
            }
            _currentIndex.value = nextIdx
            playTrack(q[nextIdx])
            return
        }

        val nextIdx = _currentIndex.value + 1
        if (nextIdx < q.size) {
            _currentIndex.value = nextIdx
            playTrack(q[nextIdx])
        } else if (_repeatMode.value == RepeatMode.ALL) {
            _currentIndex.value = 0
            playTrack(q[0])
        } else {
            mediaPlayer?.pause()
            _isPlaying.value = false
            stopProgressTracker()
        }
    }

    // Vuelve a la pista anterior; si han pasado >3 s, reinicia la actual.
    fun previous() {
        val q = _queue.value
        if (q.isEmpty()) return

        // If played more than 3 seconds, replay current track
        if (_currentPositionMs.value > 3000L) {
            seekTo(0L)
            return
        }

        val prevIdx = _currentIndex.value - 1
        if (prevIdx >= 0) {
            _currentIndex.value = prevIdx
            playTrack(q[prevIdx])
        } else if (_repeatMode.value == RepeatMode.ALL) {
            _currentIndex.value = q.lastIndex
            playTrack(q[q.lastIndex])
        } else {
            seekTo(0L)
        }
    }

    // Callback de fin de pista: repite una o avanza según el modo repeat.
    private fun handleTrackCompletion() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                _currentSong.value?.let { playTrack(it) }
            }
            RepeatMode.ALL, RepeatMode.OFF -> {
                next()
            }
        }
    }

    // Activa/desactiva la reproducción aleatoria.
    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    // Ciclo del modo repeat: OFF -> ALL -> ONE -> OFF.
    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    // Añade una pista al final de la cola; si no hay nada sonando, la reproduce.
    fun addToQueue(song: Song) {
        _queue.value = _queue.value + song
        if (_currentSong.value == null) {
            playSong(song)
        }
    }

    // Elimina una pista de la cola y ajusta el índice actual si es necesario.
    fun removeFromQueue(index: Int) {
        val curList = _queue.value.toMutableList()
        if (index in curList.indices) {
            curList.removeAt(index)
            _queue.value = curList
            if (index < _currentIndex.value) {
                _currentIndex.value = (_currentIndex.value - 1).coerceAtLeast(0)
            } else if (index == _currentIndex.value && curList.isNotEmpty()) {
                val nextIdx = index.coerceAtMost(curList.lastIndex)
                _currentIndex.value = nextIdx
                playTrack(curList[nextIdx])
            } else if (curList.isEmpty()) {
                mediaPlayer?.stop()
                _currentSong.value = null
                _isPlaying.value = false
            }
        }
    }

    // Reordena la cola moviendo una pista de una posición a otra.
    fun reorderQueue(from: Int, to: Int) {
        val curList = _queue.value.toMutableList()
        if (from in curList.indices && to in curList.indices) {
            val item = curList.removeAt(from)
            curList.add(to, item)
            _queue.value = curList
            val currentSongId = _currentSong.value?.id
            val newCurrentIndex = curList.indexOfFirst { it.id == currentSongId }
            if (newCurrentIndex >= 0) {
                _currentIndex.value = newCurrentIndex
            }
        }
    }

    // Bucle que publica la posición actual cada 250 ms mientras suena la pista.
    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                try {
                    val pos = mediaPlayer?.currentPosition?.toLong() ?: 0L
                    _currentPositionMs.value = pos
                    val dur = mediaPlayer?.duration?.toLong() ?: 0L
                    if (dur > 0) _durationMs.value = dur
                } catch (_: Exception) {}
                delay(250L)
            }
        }
    }

    // Detiene el bucle de seguimiento de progreso.
    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    // Libera MediaPlayer y todos los recursos asociados (llamado al cerrar la app/ViewModel).
    fun releasePlayer() {
        stopProgressTracker()
        cancelSleepTimer()
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {}
    }

    companion object {
        @Volatile
        private var INSTANCE: AudioPlayerManager? = null

        // Singleton: una sola instancia de reproducción compartida por UI y servicio.
        fun getInstance(context: Context): AudioPlayerManager {
            return INSTANCE ?: synchronized(this) {
                val instance = AudioPlayerManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
