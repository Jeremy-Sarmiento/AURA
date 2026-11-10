package com.example.aura.player

/**
 * ARCHIVO: ShuffleTest.kt
 * FUNCIÓN: Pruebas unitarias de pickShuffleIndex: evita repetir pistas ya sonadas, termina la cola
 *          con repeat OFF, reinicia el ciclo con repeat ALL y cubre los casos de una sola pista
 *          y cola vacía.
 * NAVEGACIÓN: AudioPlayerManager.next() usa pickShuffleIndex; estas pruebas fijan su contrato.
 */

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ShuffleTest {

    // Con 0, 1 y 2 ya reproducidas, solo queda el índice 3 como candidato válido.
    @Test
    fun `no repite pistas ya reproducidas`() {
        val index = pickShuffleIndex(
            queueSize = 4,
            currentIndex = 0,
            playedIndices = setOf(0, 1, 2),
            repeatAll = false,
            random = Random(0)
        )
        assertEquals(3, index)
    }

    // Sin repeat, cuando ya sonó todo, la cola termina (null).
    @Test
    fun `termina la cola con repeat OFF cuando ya sono todo`() {
        val index = pickShuffleIndex(
            queueSize = 4,
            currentIndex = 2,
            playedIndices = setOf(0, 1, 2, 3),
            repeatAll = false
        )
        assertNull(index)
    }

    // Con repeat ALL se reinicia el ciclo y se elige cualquier índice distinto del actual.
    @Test
    fun `reinicia el ciclo con repeat ALL`() {
        val index = pickShuffleIndex(
            queueSize = 4,
            currentIndex = 2,
            playedIndices = setOf(0, 1, 2, 3),
            repeatAll = true,
            random = Random(0)
        )
        assertTrue(index in setOf(0, 1, 3))
    }

    // Caso límite de una sola pista: repeat ALL la repite, repeat OFF detiene la cola.
    @Test
    fun `una sola pista se repite con repeat ALL y para con repeat OFF`() {
        assertEquals(0, pickShuffleIndex(1, 0, setOf(0), repeatAll = true))
        assertNull(pickShuffleIndex(1, 0, setOf(0), repeatAll = false))
    }

    // Una cola vacía nunca produce un índice reproducible.
    @Test
    fun `cola vacia no devuelve indice`() {
        assertNull(pickShuffleIndex(0, 0, emptySet(), repeatAll = true))
    }
}
