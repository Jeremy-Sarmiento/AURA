package com.example.aura.auth

/**
 * ARCHIVO: AppLockTest.kt
 * FUNCIÓN: Pruebas unitarias de la máquina de estado AppLock: resultados de credenciales sin
 *          solicitud pendiente, margen de 2 minutos, bloqueo al pasar a segundo plano y entrega
 *          única de resultados SAF retenidos.
 */

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockTest {

    // Un resultado de credenciales solo vale si antes se inició ese flujo concreto.
    @Test
    fun `device credential results cannot unlock without a pending system request`() {
        val lock = AppLock()
        lock.deviceCredentialResult(true)
        assertFalse(lock.isUnlocked)
        assertFalse(lock.beginDeviceCredential())
        lock.beginAuthentication()
        assertTrue(lock.beginDeviceCredential())
        assertFalse(lock.beginDeviceCredential())
        lock.authenticated()
        assertFalse(lock.isUnlocked)
        lock.deviceCredentialResult(true)
        assertTrue(lock.isUnlocked)
        assertFalse(lock.isConfirmingDeviceCredential)
        assertTrue(lock.hasAttemptedAuthentication)
    }

    // Cancelar la autenticación mantiene la app bloqueada y los selectores retenidos.
    @Test
    fun `cancelled credentials keep access and pending imports locked`() {
        val lock = AppLock()
        lock.beginAuthentication()
        lock.beginDeviceCredential()
        lock.deferActivityResult(12, -1, null)
        lock.deviceCredentialResult(false)
        assertFalse(lock.isUnlocked)
        assertFalse(lock.isAuthenticating)
        assertFalse(lock.isConfirmingDeviceCredential)
        assertTrue(lock.hasAttemptedAuthentication)
        assertTrue(lock.takeActivityResults().isEmpty())
        lock.deviceCredentialResult(true)
        assertFalse(lock.isUnlocked)
    }

    // Salir y volver en menos de 2 minutos conserva la sesión sin pedir autenticación.
    @Test
    fun `a brief background visit restores the authenticated session without a prompt`() {
        val lock = AppLock()
        lock.beginAuthentication()
        lock.authenticated()
        lock.onBackground(10_000L)
        assertFalse(lock.isUnlocked)
        lock.onForeground(11_000L)
        assertTrue(lock.isUnlocked)
        assertFalse(lock.beginAuthentication())
    }

    // El límite exacto es 120,000 ms: por debajo conserva la sesión, al alcanzarlo la exige.
    @Test
    fun `a return just before two minutes keeps the session but the limit requires authentication`() {
        for (elapsed in listOf(119_999L, 120_000L, 120_001L)) {
            val lock = AppLock()
            lock.beginAuthentication()
            lock.authenticated()
            lock.onBackground(1_000L)
            lock.onForeground(1_000L + elapsed)
            assertEquals("elapsed=$elapsed", elapsed < 120_000L, lock.isUnlocked)
            assertEquals("elapsed=$elapsed", elapsed >= 120_000L, lock.beginAuthentication())
        }
    }

    // Solo se aplica el margen si la salida ocurrió con la sesión ya autenticada.
    @Test
    fun `an unauthenticated or cancelled session cannot use the grace period`() {
        val lock = AppLock()
        lock.beginAuthentication()
        lock.onBackground(1_000L)
        lock.authenticated()
        lock.onForeground(2_000L)
        assertFalse(lock.isUnlocked)
        lock.beginAuthentication()
        lock.authenticationError("Cancelado")
        lock.onBackground(3_000L)
        lock.onForeground(4_000L)
        assertFalse(lock.isUnlocked)
    }

    // Cada salida reinicia el contador; estando en primer plano el margen no se amplía.
    @Test
    fun `each exit starts a new grace period and foreground events do not extend it`() {
        val lock = AppLock()
        lock.beginAuthentication()
        lock.authenticated()
        lock.onBackground(0L)
        lock.onForeground(119_999L)
        lock.onForeground(120_000L)
        assertTrue(lock.isUnlocked)
        lock.onBackground(130_000L)
        lock.onForeground(250_000L)
        assertFalse(lock.isUnlocked)
        lock.onForeground(250_001L)
        assertFalse(lock.isUnlocked)
    }

    // Al volver dentro del margen se entregan los resultados SAF pendientes sin otro prompt.
    @Test
    fun `pending picker results are released after a short return without another fingerprint`() {
        val lock = AppLock()
        lock.beginAuthentication()
        lock.authenticated()
        lock.onBackground(1_000L)
        lock.deferActivityResult(12, -1, null)
        assertTrue(lock.takeActivityResults().isEmpty())
        lock.onForeground(2_000L)
        assertEquals(12, lock.takeActivityResults().single().requestCode)
        assertTrue(lock.takeActivityResults().isEmpty())
    }

    // Los resultados de selectores esperan al desbloqueo real y se consumen una sola vez.
    @Test
    fun `file picker results wait for authentication and are delivered only once`() {
        val lock = AppLock()
        lock.deferActivityResult(12, -1, null)
        assertTrue(lock.takeActivityResults().isEmpty())
        lock.beginAuthentication()
        lock.authenticationError("Cancelado")
        assertTrue(lock.takeActivityResults().isEmpty())
        lock.beginAuthentication()
        lock.authenticated()
        val results = lock.takeActivityResults()
        assertEquals(12, results.single().requestCode)
        assertEquals(-1, results.single().resultCode)
        assertTrue(lock.takeActivityResults().isEmpty())
    }

    // Estado inicial bloqueado: un éxito sin intento activo se ignora.
    @Test
    fun `starts locked and unlocks only after an active authentication succeeds`() {
        val lock = AppLock()
        assertFalse(lock.isUnlocked)
        assertFalse(lock.hasUnlockedOnce)
        lock.authenticated()
        assertFalse(lock.isUnlocked)
        assertTrue(lock.beginAuthentication())
        assertFalse(lock.beginAuthentication())
        lock.authenticated()
        assertTrue(lock.isUnlocked)
        assertTrue(lock.hasUnlockedOnce)
        assertFalse(lock.isAuthenticating)
    }

    // Un error o cancelación no concede acceso pero permite volver a intentarlo.
    @Test
    fun `cancellation and errors keep the app locked and allow retry`() {
        val lock = AppLock()
        lock.beginAuthentication()
        lock.authenticationError("Cancelado")
        assertFalse(lock.isUnlocked)
        assertFalse(lock.isAuthenticating)
        assertTrue(lock.beginAuthentication())
    }

    // lock() revoca el acceso y un éxito posterior ya no puede desbloquear.
    @Test
    fun `backgrounding revokes access and ignores late success`() {
        val lock = AppLock()
        lock.beginAuthentication()
        lock.authenticated()
        lock.lock()
        assertFalse(lock.isUnlocked)
        assertTrue(lock.hasUnlockedOnce)
        lock.beginAuthentication()
        lock.lock()
        lock.authenticated()
        assertFalse(lock.isUnlocked)
        assertFalse(AppLock().isUnlocked)
    }
}
