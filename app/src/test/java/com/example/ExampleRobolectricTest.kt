package com.example

/**
 * ARCHIVO: ExampleRobolectricTest.kt
 * FUNCIÓN: Pruebas unitarias con Robolectric. Verifica el nombre de la app y el formato de la
 *          insignia de calidad Hi-Res de una canción lossless.
 */

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.aura.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  // Comprueba que el recurso string app_name sea "AURA".
  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AURA", appName)
  }

  // Comprueba que Song.qualityBadge genere la etiqueta HI-RES LOSSLESS correcta.
  @Test
  fun `song lossless quality badge formatting`() {
    val song = Song(
      id = "test_flac",
      title = "Midnight Echoes",
      artist = "Aura Artist",
      format = "FLAC",
      sampleRate = 96000,
      bitDepth = 24,
      isLossless = true
    )

    assertTrue(song.isLossless)
    assertTrue(song.qualityBadge.contains("HI-RES LOSSLESS"))
    assertTrue(song.qualityBadge.contains("FLAC 24b/96kHz"))
  }
}
