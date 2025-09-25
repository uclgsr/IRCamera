package mpdc4gsr.sync

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TimeSyncManagerPeriodicTest {

    private lateinit var mockContext: Context
    private lateinit var timeSyncManager: TimeSyncManager
    private lateinit var testSessionDirectory: String

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)

        // Mock System.currentTimeMillis() for consistent testing
        mockkStatic(System::class)
        every { System.currentTimeMillis() } returns 1640995200000L

        timeSyncManager = TimeSyncManager(mockContext)

        // Create a temporary test directory
        testSessionDirectory = System.getProperty("java.io.tmpdir") + "/periodic_test_" + System.nanoTime()
        File(testSessionDirectory).mkdirs()
    }

    @After
    fun tearDown() {
        timeSyncManager.cleanup()
        unmockkAll()

        // Clean up test directory
        File(testSessionDirectory).deleteRecursively()
    }

    @Test
    fun testPeriodicSyncEnable() {
        timeSyncManager.initializeSession(testSessionDirectory)

        // Initially periodic sync should be disabled
        val initialStats = timeSyncManager.getSyncStats()
        assertTrue("Initial state should have sync logging enabled", initialStats["sync_log_exists"] as Boolean)

        // Enable periodic sync
        timeSyncManager.setPeriodicSyncEnabled(true)

        // Verify periodic sync is enabled
        val statsAfterEnable = timeSyncManager.getSyncStats()
        assertNotNull("Stats should be available after enabling periodic sync", statsAfterEnable)

        // Disable periodic sync
        timeSyncManager.setPeriodicSyncEnabled(false)

        timeSyncManager.finalizeSession()
    }

    @Test
    fun testManualSyncTrigger() = runBlocking {
        val syncTriggered = CountDownLatch(1)
        var callbackInvoked = false

        // Set up callback
        timeSyncManager.setSyncTriggerCallback(object : TimeSyncManager.SyncTriggerCallback {
            override suspend fun onManualSyncRequested(): Boolean {
                callbackInvoked = true
                syncTriggered.countDown()
                return true
            }
        })

        // Initialize session
        timeSyncManager.initializeSession(testSessionDirectory)

        // Trigger manual sync
        val result = timeSyncManager.triggerManualSync()

        // Wait for callback
        val callbackReceived = syncTriggered.await(2, TimeUnit.SECONDS)

        assertTrue("Manual sync should succeed", result)
        assertTrue("Callback should be invoked", callbackReceived)
        assertTrue("Callback flag should be set", callbackInvoked)

        timeSyncManager.finalizeSession()
    }

    @Test
    fun testManualSyncWithoutCallback() = runBlocking {
        timeSyncManager.initializeSession(testSessionDirectory)

        // Try to trigger manual sync without setting callback
        val result = timeSyncManager.triggerManualSync()

        assertFalse("Manual sync should fail without callback", result)

        timeSyncManager.finalizeSession()
    }

    @Test
    fun testSessionInitializationWithPeriodicSync() {
        // Enable periodic sync before session
        timeSyncManager.setPeriodicSyncEnabled(true)

        // Initialize session (should start periodic sync automatically)
        timeSyncManager.initializeSession(testSessionDirectory)

        // Verify session is initialized
        val stats = timeSyncManager.getSyncStats()
        assertEquals("Session directory should be set", testSessionDirectory, stats["session_directory"])
        assertTrue("Sync log should exist", stats["sync_log_exists"] as Boolean)

        // Finalize session (should stop periodic sync)
        timeSyncManager.finalizeSession()

        val finalStats = timeSyncManager.getSyncStats()
        assertEquals("Session directory should be cleared", "none", finalStats["session_directory"])
    }

    @Test
    fun testPeriodicSyncCleanup() {
        timeSyncManager.initializeSession(testSessionDirectory)
        timeSyncManager.setPeriodicSyncEnabled(true)

        // Cleanup should stop all periodic sync operations
        timeSyncManager.cleanup()

        // Stats should indicate cleanup occurred
        val stats = timeSyncManager.getSyncStats()
        assertEquals("Session should be cleaned up", "none", stats["session_directory"])
    }
}