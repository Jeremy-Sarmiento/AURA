package com.example

/**
 * ARCHIVO: ExampleUnitTest.kt
 * FUNCIÓN: Prueba unitaria básica de ejemplo que valida la configuración de JUnit en el módulo.
 */

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  // Prueba trivial de humo para verificar que el runner de tests funciona.
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }
}
