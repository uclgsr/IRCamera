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

class TimeSyncManagerEnhancedTest {

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
        testSessionDirectory = System.getProperty("java.io.tmpdir") + "/enhanced_test_" + System.nanoTime()
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
    fun testTimestampValidation() = runBlocking {
        timeSyncManager.initializeSession(testSessionDirectory)

        // Test with valid timestamp
        val validTimestamp = System.currentTimeMillis() + 1000 // 1 second in future
        val validResult = timeSyncManager.performSyncResponse(validTimestamp)
        assertTrue("Valid timestamp should succeed", validResult.success)

        // Test with timestamp too far in future (6 minutes)
        val futureTimestamp = System.currentTimeMillis() + 360_000L
        val futureResult = timeSyncManager.performSyncResponse(futureTimestamp)
        assertFalse("Future timestamp should fail", futureResult.success)
        assertNotNull("Should have error message", futureResult.errorMessage)

        timeSyncManager.finalizeSession()
    }

    @Test
    fun testSyncConfiguration() {
        val customConfig = TimeSyncManager.SyncConfiguration(
            periodicSyncIntervalMs = 60_000L, // 1 minute
            maxSyncRetries = 5,
            syncTimeoutMs = 10_000L,
            enableJsonLogging = false,
            enableCsvLogging = true
        )

        timeSyncManager.updateSyncConfiguration(customConfig)

        val retrievedConfig = timeSyncManager.getSyncConfiguration()
        assertEquals("Periodic interval should be updated", 60_000L, retrievedConfig.periodicSyncIntervalMs)
        assertEquals("Max retries should be updated", 5, retrievedConfig.maxSyncRetries)
        assertEquals("Timeout should be updated", 10_000L, retrievedConfig.syncTimeoutMs)
        assertFalse("JSON logging should be disabled", retrievedConfig.enableJsonLogging)
        assertTrue("CSV logging should be enabled", retrievedConfig.enableCsvLogging)
    }

    @Test
    fun testSyncQualityCalculation() = runBlocking {
        timeSyncManager.initializeSession(testSessionDirectory)

        // Simulate excellent quality sync (low RTT, no retries)
        timeSyncManager.completeSyncCalculation(
            t1 = 1640995200000L,
            t2 = 1640995200005L,
            t3 = 1640995200010L,
            offsetMs = 5L,
            rttMs = 5L, // Excellent RTT
            syncIndex = 1
        )

        // Allow time for async processing
        Thread.sleep(100)

        val qualityMetrics = timeSyncManager.getSyncQualityMetrics()
        assertTrue("Should have recorded syncs", (qualityMetrics["total_syncs"] as Int) > 0)
        assertTrue("Should have excellent quality count", (qualityMetrics["excellent_count"] as Int) > 0)

        timeSyncManager.finalizeSession()
    }

    @Test
    fun testEnhancedLogging() = runBlocking {
        // Configure with both JSON and CSV logging
        val config = TimeSyncManager.SyncConfiguration(
            enableJsonLogging = true,
            enableCsvLogging = true
        )
        timeSyncManager.updateSyncConfiguration(config)
        timeSyncManager.initializeSession(testSessionDirectory)

        // Perform sync calculation
        timeSyncManager.completeSyncCalculation(
            t1 = 1640995200000L,
            t2 = 1640995200050L,
            t3 = 1640995200100L,
            offsetMs = 50L,
            rttMs = 100L,
            syncIndex = 1
        )

        // Allow time for async logging
        Thread.sleep(200)

        val syncLogFile = timeSyncManager.getSyncLogFile()
        assertNotNull("Sync log file should exist", syncLogFile)
        assertTrue("Sync log file should exist on disk", syncLogFile!!.exists())

        val logContent = syncLogFile.readText()
        assertTrue("Log should contain JSON entry", logContent.contains("// JSON:"))
        assertTrue("Log should contain CSV entry", logContent.contains("1,"))
        assertTrue("Log should contain sync quality", logContent.contains("FAIR") || logContent.contains("POOR"))

        timeSyncManager.finalizeSession()
    }

    @Test
    fun testSyncStatsWithQualityMetrics() {
        timeSyncManager.initializeSession(testSessionDirectory)

        val stats = timeSyncManager.getSyncStats()

        assertTrue("Stats should contain quality metrics", stats.containsKey("sync_quality_metrics"))
        assertTrue("Stats should contain configuration", stats.containsKey("configuration"))

        val config = stats["configuration"] as Map<String, Any>
        assertTrue("Config should contain periodic interval", config.containsKey("periodic_interval_ms"))
        assertTrue("Config should contain max retries", config.containsKey("max_retries"))
        assertTrue("Config should contain timeout", config.containsKey("timeout_ms"))

        timeSyncManager.finalizeSession()
    }

    @Test
    fun testRetryLogic() = runBlocking {
        // Configure with minimal retries for faster testing
        val config = TimeSyncManager.SyncConfiguration(
            maxSyncRetries = 2,
            retryDelayMs = 10L // Very short delay for testing
        )
        timeSyncManager.updateSyncConfiguration(config)
        timeSyncManager.initializeSession(testSessionDirectory)

        // Test with valid timestamp to ensure retry logic can succeed
        val result = timeSyncManager.performSyncResponse(System.currentTimeMillis())

        assertTrue("Sync should eventually succeed", result.success)
        assertTrue("Retry count should be tracked", result.retryCount >= 0)

        timeSyncManager.finalizeSession()
    }
}