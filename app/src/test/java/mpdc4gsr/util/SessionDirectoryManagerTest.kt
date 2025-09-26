package mpdc4gsr.util

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import mpdc4gsr.utils.SessionDirectoryManager
import mpdc4gsr.utils.StorageStatus
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import java.io.File

@RunWith(MockitoJUnitRunner::class)
class SessionDirectoryManagerTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockPackageManager: PackageManager

    @Mock
    private lateinit var mockPackageInfo: PackageInfo

    private lateinit var tempDir: File
    private lateinit var sessionDirectoryManager: SessionDirectoryManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        // Create temporary directory for testing
        tempDir = File.createTempFile("test", "sessions").apply {
            delete()
            mkdirs()
        }

        // Mock context behavior
        `when`(mockContext.getExternalFilesDir(null)).thenReturn(tempDir)
        `when`(mockContext.packageName).thenReturn("com.mpdc4gsr.test")
        `when`(mockContext.packageManager).thenReturn(mockPackageManager)

        mockPackageInfo.versionName = "1.0.0-test"
        `when`(mockPackageManager.getPackageInfo("com.mpdc4gsr.test", 0)).thenReturn(
            mockPackageInfo
        )

        sessionDirectoryManager = SessionDirectoryManager(mockContext)
    }

    @Test
    fun testGenerateSessionId() {
        val sessionId = sessionDirectoryManager.generateSessionId()


        val parts = sessionId.split("_")
        assertEquals(5, parts.size, "Session ID should have 5 parts separated by underscores")


        assertTrue(parts[0].matches(Regex("\\d{8}")), "First part should be 8-digit date")


        assertTrue(parts[1].matches(Regex("\\d{6}")), "Second part should be 6-digit time")


        assertTrue(parts[2].matches(Regex("\\d{3}")), "Third part should be 3-digit milliseconds")


        assertTrue(
            parts[3].matches(Regex("[a-zA-Z0-9]+")),
            "Fourth part should be alphanumeric device model"
        )


        assertEquals(8, parts[4].length, "Fifth part should be 8-character UUID")
    }

    @Test
    fun testCreateSessionDirectory() {
        val sessionId = "20231201_120000_123_TestDevice_abcd1234"
        val sessionDir = sessionDirectoryManager.createSessionDirectory(sessionId)


        assertEquals(sessionId, sessionDir.sessionId)
        assertTrue(sessionDir.rootDir.exists(), "Root directory should exist")
        assertTrue(sessionDir.rgbDir.exists(), "RGB directory should exist")
        assertTrue(sessionDir.thermalDir.exists(), "Thermal directory should exist")
        assertTrue(sessionDir.shimmerDir.exists(), "Shimmer directory should exist")


        assertEquals("RGB", sessionDir.rgbDir.name)
        assertEquals("Thermal", sessionDir.thermalDir.name)
        assertEquals("Shimmer", sessionDir.shimmerDir.name)
    }

    @Test
    fun testCreateSessionMetadata() {
        val sessionId = "20231201_120000_123_TestDevice_abcd1234"
        val sessionDir = sessionDirectoryManager.createSessionDirectory(sessionId)

        val metadata = SessionMetadata(
            startTime = System.currentTimeMillis(),
            enabledSensors = listOf("RGB", "Thermal", "Shimmer"),
            participantId = "TEST001",
            studyName = "Test Study",
            customMetadata = mapOf("test_key" to "test_value")
        )

        val metadataFile = sessionDirectoryManager.createSessionMetadata(sessionDir, metadata)

        assertTrue(metadataFile.exists(), "Metadata file should exist")
        assertEquals(SessionDirectoryManager.SESSION_METADATA_FILE, metadataFile.name)


        val jsonContent = JSONObject(metadataFile.readText())
        assertEquals(sessionId, jsonContent.getString("session_id"))
        assertEquals("TEST001", jsonContent.getString("participant_id"))
        assertEquals("Test Study", jsonContent.getString("study_name"))
        assertEquals("ACTIVE", jsonContent.getString("status"))
        assertEquals("1.0.0-test", jsonContent.getString("app_version"))

        val enabledSensors = jsonContent.getJSONArray("enabled_sensors")
        assertEquals(3, enabledSensors.length())
        assertEquals("RGB", enabledSensors.getString(0))
    }

    @Test
    fun testUpdateSessionMetadata() {
        val sessionId = "20231201_120000_123_TestDevice_abcd1234"
        val sessionDir = sessionDirectoryManager.createSessionDirectory(sessionId)

        val initialMetadata = SessionMetadata(
            startTime = 1000L,
            enabledSensors = listOf("RGB"),
            participantId = "TEST001"
        )

        sessionDirectoryManager.createSessionMetadata(sessionDir, initialMetadata)


        val endTime = 5000L
        val errors = mapOf("sensor1" to "Connection failed")
        sessionDirectoryManager.updateSessionMetadata(sessionDir, endTime, "COMPLETED", errors)


        val metadataFile = File(sessionDir.rootDir, SessionDirectoryManager.SESSION_METADATA_FILE)
        val jsonContent = JSONObject(metadataFile.readText())

        assertEquals(endTime, jsonContent.getLong("end_time"))
        assertEquals("COMPLETED", jsonContent.getString("status"))
        assertEquals(4000L, jsonContent.getLong("duration_ms"))

        val errorsJson = jsonContent.getJSONObject("errors")
        assertEquals("Connection failed", errorsJson.getString("sensor1"))

        assertTrue(jsonContent.has("files"), "Should have files information")
    }

    @Test
    fun testCheckStorageSpace() {
        val storageStatus = sessionDirectoryManager.checkStorageSpace()

        assertNotNull(storageStatus)
        assertTrue(storageStatus.availableMB >= 0, "Available MB should be >= 0")
        assertTrue(storageStatus.totalMB >= 0, "Total MB should be >= 0")
        assertTrue(storageStatus.usagePercentage in 0..100, "Usage percentage should be 0-100")
        assertNotNull(storageStatus.formattedAvailable)
    }

    @Test
    fun testCleanupFailedSessions() {

        val failedSessionId = "20231201_120000_123_TestDevice_failed01"
        val failedSessionDir = sessionDirectoryManager.createSessionDirectory(failedSessionId)


        val successSessionId = "20231201_120000_123_TestDevice_success"
        val successSessionDir = sessionDirectoryManager.createSessionDirectory(successSessionId)
        val metadata = SessionMetadata(
            startTime = System.currentTimeMillis(),
            enabledSensors = listOf("RGB")
        )
        sessionDirectoryManager.createSessionMetadata(successSessionDir, metadata)

        // Add some dummy data to successful session
        File(
            successSessionDir.rgbDir,
            "test_data.txt"
        ).writeText("Some test data that makes this session valid")


        val cleanedSessions = sessionDirectoryManager.cleanupFailedSessions()


        assertFalse(failedSessionDir.rootDir.exists(), "Failed session should be cleaned up")
        assertTrue(successSessionDir.rootDir.exists(), "Successful session should remain")

        assertTrue(
            cleanedSessions.contains(failedSessionId),
            "Cleaned sessions should include failed session"
        )
        assertFalse(
            cleanedSessions.contains(successSessionId),
            "Cleaned sessions should not include successful session"
        )
    }

    @Test
    fun testGetStandardFilePath() {
        val sessionId = "20231201_120000_123_TestDevice_abcd1234"
        val sessionDir = sessionDirectoryManager.createSessionDirectory(sessionId)


        val rgbFile =
            sessionDirectoryManager.getStandardFilePath(sessionDir, "RGB", "test_video.mp4")
        assertEquals(sessionDir.rgbDir, rgbFile.parentFile)
        assertEquals("test_video.mp4", rgbFile.name)


        val thermalFile =
            sessionDirectoryManager.getStandardFilePath(sessionDir, "thermal", "thermal_data.csv")
        assertEquals(sessionDir.thermalDir, thermalFile.parentFile)
        assertEquals("thermal_data.csv", thermalFile.name)


        val shimmerFile =
            sessionDirectoryManager.getStandardFilePath(sessionDir, "Shimmer3", "gsr_data.csv")
        assertEquals(sessionDir.shimmerDir, shimmerFile.parentFile)
        assertEquals("gsr_data.csv", shimmerFile.name)


        val unknownFile =
            sessionDirectoryManager.getStandardFilePath(sessionDir, "unknown", "test.txt")
        assertEquals(sessionDir.rootDir, unknownFile.parentFile)
        assertEquals("test.txt", unknownFile.name)
    }

    @Test
    fun testStorageStatusFormatting() {
        val storageStatus = StorageStatus(
            availableMB = 1536L,
            totalMB = 2048L,
            isLowStorage = false,
            shouldWarn = false
        )

        assertEquals("1.5 GB", storageStatus.formattedAvailable)
        assertEquals(25, storageStatus.usagePercentage)

        val smallStorage = StorageStatus(
            availableMB = 512L,
            totalMB = 1024L,
            isLowStorage = false,
            shouldWarn = false
        )

        assertEquals("512 MB", smallStorage.formattedAvailable)
        assertEquals(50, smallStorage.usagePercentage)
    }
}
