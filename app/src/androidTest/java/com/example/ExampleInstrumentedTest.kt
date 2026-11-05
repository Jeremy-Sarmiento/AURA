package com.example

/**
 * ARCHIVO: ExampleInstrumentedTest.kt
 * FUNCIÓN: Prueba instrumentada de ejemplo que se ejecuta en un dispositivo/emulador Android.
 */

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
  // Verifica que el package name del contexto coincida con BuildConfig.APPLICATION_ID.
  @Test
  fun useAppContext() {
    // Context of the app under test.
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    assertEquals(BuildConfig.APPLICATION_ID, appContext.packageName)
  }
}
