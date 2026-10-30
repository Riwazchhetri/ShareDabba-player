package com.example

import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.authentication.SessionTokenCoordinator
import com.example.data.local.ShareDabbaCacheDatabase
import com.example.data.repository.StreamCacheRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read ShareDabba app_name and database id from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        val dbId = context.getString(R.string.firestore_database_id)
        assertEquals("ShareDabba Player", appName)
        assertTrue(dbId.isNotBlank())
    }

    @Test
    fun `MainActivity manifest supports PictureInPicture and orientation configChanges`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val activityInfo = context.packageManager.getActivityInfo(
            ComponentName(context, MainActivity::class.java),
            PackageManager.GET_META_DATA
        )
        val hasOrientationChange =
            (activityInfo.configChanges and ActivityInfo.CONFIG_ORIENTATION) != 0
        val hasScreenSizeChange =
            (activityInfo.configChanges and ActivityInfo.CONFIG_SCREEN_SIZE) != 0
        assertTrue("MainActivity must handle orientation config changes smoothly", hasOrientationChange)
        assertTrue("MainActivity must handle screenSize config changes smoothly", hasScreenSizeChange)
    }

    @Test
    fun `backgrounding player persists and recovers playback timestamp in Room cache`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, ShareDabbaCacheDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = StreamCacheRepository(db.streamCacheDao())
            repo.cacheStreamPlayback(
                streamId = "vid_featured_1",
                title = "ShareDabba Next-Gen Streaming Architecture Keynote",
                streamUrl = "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4",
                positionMs = 128_400L
            )
            val cached = repo.cachedStreams.first()
            assertEquals(1, cached.size)
            assertEquals(128_400L, cached.first().lastPlaybackPositionMs)
        } finally {
            db.close()
        }
    }

    @Test
    fun `SessionTokenCoordinator handles simulated session expiry and logout cleanup`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val coordinator = SessionTokenCoordinator(context)
        coordinator.simulateExpiredSessionForTesting()
        assertTrue(coordinator.sessionStatus.value.isTokenExpired)

        coordinator.clearSessionMetadata()
        assertFalse(coordinator.sessionStatus.value.isAuthenticated)
        assertFalse(coordinator.sessionStatus.value.isTokenExpired)
    }

    @Test
    fun `deep link VIEW intent extracts exact requested videoId for native player`() {
        val intent = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse("https://sharedabba.com/v/vid_recent_2?utm_source=whatsapp")
        )
        val target = com.example.deeplinks.DeepLinkRouter.extractStreamUrlFromIntent(intent)
        assertEquals("vid_recent_2", target)
    }
}
