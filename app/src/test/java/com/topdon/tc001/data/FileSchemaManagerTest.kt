package com.topdon.tc001.data

import com.topdon.tc001.data.FileSchemaManager
import com.topdon.tc001.data.SessionMetadata
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.json.JSONObject
import java.io.File

@RunWith(RobolectricTestRunner::class)
class FileSchemaManagerTest {

    private lateinit var fileSchemaManager: FileSchemaManager
    private lateinit var testDirectory: File

    @Before
    fun setup() {
        fileSchemaManager = FileSchemaManager()
        testDirectory = File("/tmp/test-schema-${System.currentTimeMillis()}")
        testDirectory.mkdirs()
    }

    @Test
    fun `should validate GSR CSV schema correctly`() = runTest {
        // Setup - create valid GSR CSV file
        val gsrFile = File(testDirectory, "gsr_data.csv")
        gsrFile.writeText("""
            timestamp,gsr_value,skin_conductance,device_id,session_id
            1640995200000,0.5,2.5,shimmer_001,session_123
            1640995201000,0.6,2.7,shimmer_001,session_123
        """.trimIndent())
        
        // Execute
        val isValid = fileSchemaManager.validateGSRSchema(gsrFile)
        
        // Verify
        assertTrue("GSR CSV should be valid", isValid)
        
        // Cleanup
        gsrFile.delete()
    }

    @Test
    fun `should detect invalid GSR CSV schema`() = runTest {
        // Setup - create invalid GSR CSV file (missing required columns)
        val invalidGsrFile = File(testDirectory, "invalid_gsr_data.csv")
        invalidGsrFile.writeText("""
            timestamp,invalid_column
            1640995200000,0.5
        """.trimIndent())
        
        // Execute
        val isValid = fileSchemaManager.validateGSRSchema(invalidGsrFile)
        
        // Verify
        assertFalse("Invalid GSR CSV should be rejected", isValid)
        
        // Cleanup
        invalidGsrFile.delete()
    }

    @Test
    fun `should validate RGB frames CSV schema correctly`() = runTest {
        // Setup - create valid RGB frames CSV file
        val rgbFile = File(testDirectory, "rgb_frames.csv")
        rgbFile.writeText("""
            frame_number,timestamp,frame_path,width,height,fps
            1,1640995200000,frames/frame_001.jpg,3840,2160,30
            2,1640995200033,frames/frame_002.jpg,3840,2160,30
        """.trimIndent())
        
        // Execute
        val isValid = fileSchemaManager.validateRGBSchema(rgbFile)
        
        // Verify
        assertTrue("RGB CSV should be valid", isValid)
        
        // Cleanup
        rgbFile.delete()
    }

    @Test
    fun `should validate thermal data CSV schema correctly`() = runTest {
        // Setup - create valid thermal CSV file
        val thermalFile = File(testDirectory, "thermal_data.csv")
        thermalFile.writeText("""
            frame_number,timestamp,image_path,min_temp,max_temp,avg_temp
            1,1640995200000,thermal_images/thermal_001.png,20.5,35.2,28.1
            2,1640995200100,thermal_images/thermal_002.png,20.8,34.9,27.8
        """.trimIndent())
        
        // Execute
        val isValid = fileSchemaManager.validateThermalSchema(thermalFile)
        
        // Verify
        assertTrue("Thermal CSV should be valid", isValid)
        
        // Cleanup
        thermalFile.delete()
    }

    @Test
    fun `should generate session metadata schema`() = runTest {
        // Setup
        val sessionMetadata = SessionMetadata(
            sessionId = "test_session_123",
            startTime = System.currentTimeMillis(),
            deviceModel = "Samsung Galaxy S22",
            appVersion = "1.0.0",
            sensors = listOf("GSR", "RGB", "Thermal")
        )
        
        // Execute
        val schema = fileSchemaManager.generateSessionMetadata(sessionMetadata)
        
        // Verify
        assertNotNull("Schema should not be null", schema)
        assertTrue("Schema should contain session ID", 
            schema.contains("test_session_123"))
        assertTrue("Schema should contain device model", 
            schema.contains("Samsung Galaxy S22"))
        assertTrue("Schema should contain sensors", 
            schema.contains("GSR"))
    }

    @Test
    fun `should validate complete session file structure`() = runTest {
        // Setup - create complete session directory structure
        val sessionDir = File(testDirectory, "session_123")
        sessionDir.mkdirs()
        
        // Create required files
        File(sessionDir, "gsr_data.csv").writeText("timestamp,gsr_value\n1640995200000,0.5")
        File(sessionDir, "rgb_frames.csv").writeText("frame_number,timestamp\n1,1640995200000")
        File(sessionDir, "thermal_data.csv").writeText("frame_number,timestamp\n1,1640995200000")
        File(sessionDir, "session_metadata.json").writeText("{\"sessionId\":\"session_123\"}")
        
        // Create subdirectories
        File(sessionDir, "frames").mkdirs()
        File(sessionDir, "thermal_images").mkdirs()
        
        // Execute
        val isValid = fileSchemaManager.validateSessionStructure(sessionDir)
        
        // Verify
        assertTrue("Session structure should be valid", isValid)
        
        // Cleanup
        sessionDir.deleteRecursively()
    }

    @Test
    fun `should detect incomplete session file structure`() = runTest {
        // Setup - create incomplete session directory (missing files)
        val incompleteSessionDir = File(testDirectory, "incomplete_session")
        incompleteSessionDir.mkdirs()
        
        // Only create GSR file, missing others
        File(incompleteSessionDir, "gsr_data.csv").writeText("timestamp,gsr_value\n1640995200000,0.5")
        
        // Execute
        val isValid = fileSchemaManager.validateSessionStructure(incompleteSessionDir)
        
        // Verify
        assertFalse("Incomplete session structure should be invalid", isValid)
        
        // Cleanup
        incompleteSessionDir.deleteRecursively()
    }

    @Test
    fun `should generate file integrity checksum`() = runTest {
        // Setup - create test file with known content
        val testFile = File(testDirectory, "test_data.csv")
        val testContent = "timestamp,value\n1640995200000,123\n1640995201000,456"
        testFile.writeText(testContent)
        
        // Execute
        val checksum1 = fileSchemaManager.generateChecksum(testFile)
        val checksum2 = fileSchemaManager.generateChecksum(testFile)
        
        // Verify
        assertNotNull("Checksum should not be null", checksum1)
        assertEquals("Checksums should be consistent", checksum1, checksum2)
        assertTrue("Checksum should be valid format (hex)", 
            checksum1.matches(Regex("[a-f0-9]+")))
        
        // Test different content produces different checksum
        testFile.writeText("different content")
        val checksum3 = fileSchemaManager.generateChecksum(testFile)
        assertNotEquals("Different content should produce different checksum", 
            checksum1, checksum3)
        
        // Cleanup
        testFile.delete()
    }

    @Test
    fun `should validate data consistency across files`() = runTest {
        // Setup - create session with consistent timestamps
        val sessionDir = File(testDirectory, "consistent_session")
        sessionDir.mkdirs()
        
        val baseTime = 1640995200000L
        
        // Create GSR file
        File(sessionDir, "gsr_data.csv").writeText("""
            timestamp,gsr_value
            $baseTime,0.5
            ${baseTime + 1000},0.6
        """.trimIndent())
        
        // Create RGB file with overlapping time range
        File(sessionDir, "rgb_frames.csv").writeText("""
            frame_number,timestamp
            1,$baseTime
            2,${baseTime + 1000}
        """.trimIndent())
        
        // Execute
        val isConsistent = fileSchemaManager.validateDataConsistency(sessionDir)
        
        // Verify
        assertTrue("Data should be consistent across files", isConsistent)
        
        // Cleanup
        sessionDir.deleteRecursively()
    }

    @Test
    fun `should detect data inconsistency across files`() = runTest {
        // Setup - create session with inconsistent timestamps
        val sessionDir = File(testDirectory, "inconsistent_session")
        sessionDir.mkdirs()
        
        // Create GSR file with one time range
        File(sessionDir, "gsr_data.csv").writeText("""
            timestamp,gsr_value
            1640995200000,0.5
        """.trimIndent())
        
        // Create RGB file with completely different time range
        File(sessionDir, "rgb_frames.csv").writeText("""
            frame_number,timestamp
            1,1641000000000
        """.trimIndent())
        
        // Execute
        val isConsistent = fileSchemaManager.validateDataConsistency(sessionDir)
        
        // Verify
        assertFalse("Inconsistent data should be detected", isConsistent)
        
        // Cleanup
        sessionDir.deleteRecursively()
    }

    @Test
    fun `should generate comprehensive data quality report`() = runTest {
        // Setup - create complete session with all files
        val sessionDir = File(testDirectory, "quality_test_session")
        sessionDir.mkdirs()
        
        // Create all required files
        File(sessionDir, "gsr_data.csv").writeText("timestamp,gsr_value\n1640995200000,0.5\n1640995201000,0.6")
        File(sessionDir, "rgb_frames.csv").writeText("frame_number,timestamp\n1,1640995200000\n2,1640995201000")
        File(sessionDir, "thermal_data.csv").writeText("frame_number,timestamp\n1,1640995200000\n2,1640995201000")
        
        // Execute
        val qualityReport = fileSchemaManager.generateDataQualityReport(sessionDir)
        
        // Verify
        assertNotNull("Quality report should not be null", qualityReport)
        assertTrue("Report should contain file validation results", 
            qualityReport.contains("gsr_data.csv"))
        assertTrue("Report should contain consistency check", 
            qualityReport.contains("consistency"))
        assertTrue("Report should contain summary", 
            qualityReport.contains("summary"))
        
        // Cleanup
        sessionDir.deleteRecursively()
    }

    @After
    fun tearDown() {
        // Clean up test directory
        if (testDirectory.exists()) {
            testDirectory.deleteRecursively()
        }
    }
}