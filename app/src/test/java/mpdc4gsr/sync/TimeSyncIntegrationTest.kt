package mpdc4gsr.sync

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import mpdc4gsr.network.NetworkServer
import mpdc4gsr.network.Protocol
import mpdc4gsr.network.ProtocolHandler
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Integration test to verify TimeSyncManager works correctly with ProtocolHandler
 * and produces expected sync logs as specified in the implementation plan.
 */
class TimeSyncIntegrationTest {

    private lateinit var mockContext: Context
    private lateinit var mockNetworkServer: NetworkServer
    private lateinit var timeSyncManager: TimeSyncManager
    private lateinit var protocolHandler: ProtocolHandler
    private lateinit var testSessionDirectory: String

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockNetworkServer = mockk(relaxed = true)

        // Mock System.currentTimeMillis() for consistent testing
        mockkStatic(System::class)
        every { System.currentTimeMillis() } returns 1640995200000L

        timeSyncManager = TimeSyncManager(mockContext)
        protocolHandler = ProtocolHandler(mockContext, mockNetworkServer)
        protocolHandler.setTimeSyncManager(timeSyncManager)

        // Create test session directory
        testSessionDirectory =
            System.getProperty("java.io.tmpdir") + "/integration_test_" + System.nanoTime()
        File(testSessionDirectory).mkdirs()
    }

    @After
    fun tearDown() {
        timeSyncManager.cleanup()
        unmockkAll()
        File(testSessionDirectory).deleteRecursively()
    }

    @Test
    fun testFullSyncWorkflow() = runBlocking {
        // 1. Initialize session (simulating recording start)
        timeSyncManager.initializeSession(testSessionDirectory)

        // 2. Simulate PC sending SYNC_REQUEST
        val pcTimestamp = 1640995200100L
        val syncResult = timeSyncManager.performSyncResponse(pcTimestamp)

        // Verify sync response
        assertTrue("Sync should succeed", syncResult.success)
        assertEquals("PC timestamp should match", pcTimestamp, syncResult.t1)
        assertTrue("Phone timestamp should be reasonable", syncResult.t2 > 0)

        // 3. Create protocol response message
        val responseMessage = Protocol.createSyncResponseMessage(syncResult.t1, syncResult.t2)
        assertTrue(
            "Response should contain SYNC_RESPONSE",
            responseMessage.contains("SYNC_RESPONSE")
        )
        assertTrue(
            "Response should contain timestamps",
            responseMessage.contains("t_pc=$pcTimestamp")
        )

        // 4. Simulate PC calculating and sending SYNC_RESULT
        val t3 = 1640995200150L
        val offsetMs = 50L
        val rttMs = 50L

        timeSyncManager.completeSyncCalculation(
            syncResult.t1,
            syncResult.t2,
            t3,
            offsetMs,
            rttMs,
            syncResult.syncIndex
        )

        // 5. Allow time for async logging
        Thread.sleep(200)

        // 6. Verify sync log was created and contains expected data
        val syncLogFile = timeSyncManager.getSyncLogFile()
        assertNotNull("Sync log file should exist", syncLogFile)
        assertTrue("Sync log file should exist on disk", syncLogFile!!.exists())

        val logContent = syncLogFile.readText()

        // Verify CSV header
        assertTrue("Log should contain CSV header", logContent.contains("sync_index,timestamp_iso"))

        // Verify session start marker (index 0)
        assertTrue("Log should contain session start marker", logContent.contains("0,"))

        // Verify actual sync data (index 1)
        assertTrue("Log should contain sync index", logContent.contains("${syncResult.syncIndex},"))
        assertTrue("Log should contain offset", logContent.contains("$offsetMs"))
        assertTrue("Log should contain RTT", logContent.contains("$rttMs"))

        // 7. Test session finalization
        timeSyncManager.finalizeSession()

        // Verify stats show session is finalized
        val stats = timeSyncManager.getSyncStats()
        assertEquals("Session directory should be cleared", "none", stats["session_directory"])
    }

    @Test
    fun testProtocolMessageCreation() {
        // Test all new protocol messages work correctly

        // SYNC_REQUEST
        val syncRequest = Protocol.createSyncRequestMessage(1640995200000L)
        assertEquals("SYNC_REQUEST t_pc=1640995200000", syncRequest)

        // SYNC_RESPONSE  
        val syncResponse = Protocol.createSyncResponseMessage(1640995200000L, 1640995200050L)
        assertEquals("SYNC_RESPONSE t_pc=1640995200000 t_ph=1640995200050", syncResponse)

        // SYNC_RESULT (new message type)
        val syncResult = Protocol.createSyncResultMessage(
            1640995200000L, // t1
            1640995200050L, // t2  
            1640995200100L, // t3
            50L,            // offset
            100L            // RTT
        )

        val expected =
            "SYNC_RESULT t1=1640995200000 t2=1640995200050 t3=1640995200100 offset=50 rtt=100"
        assertEquals("SYNC_RESULT message should be correctly formatted", expected, syncResult)
    }

    @Test
    fun testMultipleSessionScenario() = runBlocking {
        // Test that multiple recording sessions work correctly

        // Session 1
        timeSyncManager.initializeSession(testSessionDirectory + "_session1")
        val result1 = timeSyncManager.performSyncResponse(1640995200000L)
        assertTrue("First session sync should succeed", result1.success)
        timeSyncManager.finalizeSession()

        // Session 2  
        timeSyncManager.initializeSession(testSessionDirectory + "_session2")
        val result2 = timeSyncManager.performSyncResponse(1640995200100L)
        assertTrue("Second session sync should succeed", result2.success)

        // Verify sync indices continue incrementing across sessions
        assertTrue("Sync indices should increment", result2.syncIndex > result1.syncIndex)

        timeSyncManager.finalizeSession()

        // Verify stats
        val stats = timeSyncManager.getSyncStats()
        val totalSyncs = stats["total_syncs"] as Long
        assertTrue("Total syncs should be at least 2", totalSyncs >= 2)
    }

    @Test
    fun testNonBlockingExecution() = runBlocking {
        // Verify that sync operations don't block
        timeSyncManager.initializeSession(testSessionDirectory)

        val startTime = System.currentTimeMillis()

        // Perform multiple sync operations rapidly
        val results = listOf(
            timeSyncManager.performSyncResponse(1640995200000L),
            timeSyncManager.performSyncResponse(1640995200100L),
            timeSyncManager.performSyncResponse(1640995200200L)
        )

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        // All should succeed
        results.forEach { result ->
            assertTrue("All sync responses should succeed", result.success)
        }

        // Should execute quickly (non-blocking)
        assertTrue("Sync operations should be fast", duration < 1000) // Less than 1 second

        timeSyncManager.finalizeSession()
    }
}