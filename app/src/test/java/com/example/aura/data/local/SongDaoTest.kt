package com.example.aura.data.local

/**
 * ARCHIVO: SongDaoTest.kt
 * FUNCIÓN: Pruebas de integración de SongDao con una base Room en memoria. Verifican que
 *          OnConflictStrategy.IGNORE conserve el favorito y la fecha original al reimportar o
 *          re-escanear una pista ya existente.
 * NAVEGACIÓN: SongDao -> AuraDatabase (Room en memoria); no toca el archivo real de la app.
 */

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SongDaoTest {

    private lateinit var db: AuraDatabase
    private lateinit var dao: SongDao

    // Base de datos aislada por prueba; allowMainThreadQueries simplifica el runBlocking.
    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AuraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.songDao()
    }

    // Libera la base en memoria al terminar cada prueba.
    @After
    fun tearDown() {
        db.close()
    }

    // Fábrica de SongEntity con valores válidos y parámetros ajustables por prueba.
    private fun song(
        id: String = "song_1",
        title: String = "Original",
        isFavorite: Boolean = true,
        dateAdded: Long = 123L
    ) = SongEntity(
        id = id,
        title = title,
        artist = "Artista",
        album = "Álbum",
        durationMs = 180_000L,
        uriString = "content://media/song_1",
        filePath = null,
        format = "FLAC",
        sampleRate = 44_100,
        bitDepth = 16,
        bitrateKbps = 900,
        isLossless = true,
        isFavorite = isFavorite,
        lyrics = "",
        coverColorHex = 0xFF9155FD,
        dateAdded = dateAdded
    )

    // Reimportar la misma pista no sobrescribe isFavorite ni dateAdded (conflicto IGNORE).
    @Test
    fun `reimportar una pista conserva favorito y fecha`() = runBlocking {
        dao.insertSong(song())

        dao.insertSong(song(title = "Reimportada", isFavorite = false, dateAdded = 999L))

        val stored = dao.getSongById("song_1")!!
        assertEquals("Original", stored.title)
        assertTrue(stored.isFavorite)
        assertEquals(123L, stored.dateAdded)
    }

    // Un re-escaneo inserta solo pistas nuevas y conserva los favoritos de las existentes.
    @Test
    fun `re-escanear la biblioteca conserva los favoritos`() = runBlocking {
        dao.insertSongs(listOf(song(id = "song_1"), song(id = "song_2")))

        dao.insertSongs(
            listOf(
                song(id = "song_1", title = "Reescaneada", isFavorite = false),
                song(id = "song_3")
            )
        )

        assertTrue(dao.getSongById("song_1")!!.isFavorite)
        assertEquals("Original", dao.getSongById("song_1")!!.title)
        assertTrue(dao.getSongById("song_3") != null)
    }
}
