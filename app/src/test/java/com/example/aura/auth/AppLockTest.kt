package com.example.aura.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockTest {
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

    @Test
    fun `cancellation and errors keep the app locked and allow retry`() {
        val lock = AppLock()
        lock.beginAuthentication()
        lock.authenticationError("Cancelado")
        assertFalse(lock.isUnlocked)
        assertFalse(lock.isAuthenticating)
        assertTrue(lock.beginAuthentication())
    }

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
