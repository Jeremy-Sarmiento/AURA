package com.example

import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import androidx.lifecycle.ViewModelProvider
import com.example.aura.auth.AppLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityLockTest {
    @Test
    fun `external audio intents remain pending while locked and previews are protected`() {
        val uri = Uri.parse("content://test/music.mp3")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        Robolectric.buildActivity(MainActivity::class.java, intent).use { controller ->
            val activity = controller.setup().get()
            assertFalse(ViewModelProvider(activity)[AppLock::class.java].isUnlocked)
            assertEquals(uri, activity.intent.data)
            assertTrue(activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)

            val nextUri = Uri.parse("content://test/next.mp3")
            controller.newIntent(Intent(Intent.ACTION_VIEW, nextUri))
            assertEquals(nextUri, activity.intent.data)
            assertFalse(ViewModelProvider(activity)[AppLock::class.java].isUnlocked)
        }
    }
}
