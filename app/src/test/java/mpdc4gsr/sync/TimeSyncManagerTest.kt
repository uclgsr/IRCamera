package mpdc4gsr.sync

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileReader

class TimeSyncManagerTest {

    private lateinit var mockContext: Context
    private lateinit var timeSyncManager: TimeSyncManager
    private lateinit var testSessionDirectory: String

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)

        // Mock System.currentTimeMillis() for consistent testing
        mockkStatic(System::class)
        every { System.currentTimeMillis() } returns 1640995200000L // Fixed timestamp for testing

        timeSyncManager = TimeSyncManager(mockContext)

        // Create a temporary test directory
        testSessionDirectory = System.getProperty("java.io.tmpdir") + "/test_session_" + System.nanoTime()
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
    fun testInitializeSession() {
        // Test session initialization
        timeSyncManager.initializeSession(testSessionDirectory)

        // Verify sync log file was created
        val syncLogFile = timeSyncManager.getSyncLogFile()
        assertNotNull("Sync log file should be created", syncLogFile)
        assertTrue("Sync log file should exist", syncLogFile!!.exists())

        // Verify CSV header was written
        val content = FileReader(syncLogFile).readText()
        assertTrue("CSV header should be present", content.contains("sync_index,timestamp_iso"))
    }

    @Test
    fun testPerformSyncResponse() = runBlocking {
        // Initialize session first
        timeSyncManager.initializeSession(testSessionDirectory)

        val pcTimestamp = 1640995200100L

        // Test sync response
        val result = timeSyncManager.performSyncResponse(pcTimestamp)

        assertTrue("Sync response should be successful", result.success)
        assertEquals("PC timestamp should match", pcTimestamp, result.t1)
        assertTrue("Phone timestamp should be set", result.t2 > 0)
        assertTrue("Sync index should be positive", result.syncIndex > 0)
    }

    @Test
    fun testCompleteSyncCalculation() = runBlocking {
        // Initialize session first
        timeSyncManager.initializeSession(testSessionDirectory)

        val t1 = 1640995200000L
        val t2 = 1640995200050L
        val t3 = 1640995200100L
        val offsetMs = 50L
        val rttMs = 100L
        val syncIndex = 1

        // Complete sync calculation
        timeSyncManager.completeSyncCalculation(t1, t2, t3, offsetMs, rttMs, syncIndex)

        // Allow some time for async logging
        Thread.sleep(100)

        // Verify sync log was written
        val syncLogFile = timeSyncManager.getSyncLogFile()
        assertNotNull("Sync log file should exist", syncLogFile)

        val content = FileReader(syncLogFile!!).readText()
        assertTrue("Log should contain sync data", content.contains("$syncIndex,"))
        assertTrue("Log should contain offset", content.contains("$offsetMs"))
        assertTrue("Log should contain RTT", content.contains("$rttMs"))
    }

    @Test
    fun testPerformSessionStartSync() = runBlocking {
        // Initialize session first
        timeSyncManager.initializeSession(testSessionDirectory)

        // Perform session start sync
        val success = timeSyncManager.performSessionStartSync()

        assertTrue("Session start sync should succeed", success)

        // Allow some time for async logging
        Thread.sleep(100)

        // Verify sync log contains session start marker
        val syncLogFile = timeSyncManager.getSyncLogFile()
        assertNotNull("Sync log file should exist", syncLogFile)

        val content = FileReader(syncLogFile!!).readText()
        assertTrue("Log should contain session start marker", content.contains("0,")) // sync index 0 for session start
    }

    @Test
    fun testGetSyncStats() {
        // Initialize session first
        timeSyncManager.initializeSession(testSessionDirectory)

        val stats = timeSyncManager.getSyncStats()

        assertTrue("Stats should contain total_syncs", stats.containsKey("total_syncs"))
        assertTrue("Stats should contain session_directory", stats.containsKey("session_directory"))
        assertTrue("Stats should contain session_start_time", stats.containsKey("session_start_time"))
        assertTrue("Stats should contain sync_log_exists", stats.containsKey("sync_log_exists"))

        assertEquals("Session directory should match", testSessionDirectory, stats["session_directory"])
        assertEquals("Sync log should exist", true, stats["sync_log_exists"])
    }

    @Test
    fun testFinalizeSession() {
        // Initialize session first
        timeSyncManager.initializeSession(testSessionDirectory)

        val statsBefore = timeSyncManager.getSyncStats()
        assertNotEquals("Session directory should be set", "none", statsBefore["session_directory"])

        // Finalize session
        timeSyncManager.finalizeSession()

        val statsAfter = timeSyncManager.getSyncStats()
        assertEquals("Session directory should be cleared", "none", statsAfter["session_directory"])
    }

    @Test
    fun testMultipleSyncOperations() = runBlocking {
        // Initialize session
        timeSyncManager.initializeSession(testSessionDirectory)

        // Perform multiple sync responses
        val result1 = timeSyncManager.performSyncResponse(1640995200000L)
        val result2 = timeSyncManager.performSyncResponse(1640995200100L)
        val result3 = timeSyncManager.performSyncResponse(1640995200200L)

        assertTrue("First sync should succeed", result1.success)
        assertTrue("Second sync should succeed", result2.success)
        assertTrue("Third sync should succeed", result3.success)

        // Verify sync indices are incremented
        assertTrue("Sync indices should increment", result2.syncIndex > result1.syncIndex)
        assertTrue("Sync indices should increment", result3.syncIndex > result2.syncIndex)

        val stats = timeSyncManager.getSyncStats()
        val totalSyncs = stats["total_syncs"] as Long
        assertTrue("Total syncs should be at least 3", totalSyncs >= 3)
    }
}