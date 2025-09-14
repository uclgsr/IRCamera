package com.topdon.tc001.sensors.thermal

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.*

/**
 * Unit tests for ThermalRecorder functionality
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ThermalRecorderTest {
    
    private lateinit var context: Context
    private lateinit var thermalRecorder: ThermalRecorder
    private lateinit var testSessionDir: String
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        thermalRecorder = ThermalRecorder(context)
        
        // Create test session directory
        testSessionDir = "${context.cacheDir}/test_session_${System.currentTimeMillis()}"
        File(testSessionDir).mkdirs()
    }
    
    @Test
    fun testStartStopRecording() = runTest {
        // Test starting recording
        val started = thermalRecorder.startRecording(testSessionDir, saveImages = false)
        assertTrue(started, "Recording should start successfully")
        assertTrue(thermalRecorder.isRecording(), "Should be recording")
        
        // Test stopping recording
        val stopped = thermalRecorder.stopRecording()
        assertTrue(stopped, "Recording should stop successfully")
        assertFalse(thermalRecorder.isRecording(), "Should not be recording")
    }
    
    @Test
    fun testFrameProcessing() = runTest {
        val latch = CountDownLatch(1)
        var processedStats: ThermalRecorder.ThermalFrameStats? = null
        
        // Set up frame listener
        thermalRecorder.setFrameListener(object : ThermalRecorder.ThermalFrameListener {
            override fun onFrameProcessed(stats: ThermalRecorder.ThermalFrameStats) {
                processedStats = stats
                latch.countDown()
            }
            
            override fun onError(error: String) {
                fail("Should not have error: $error")
            }
        })
        
        // Start recording
        thermalRecorder.startRecording(testSessionDir, saveImages = false)
        
        // Process a test frame
        val width = 256
        val height = 192
        val frameData = ByteArray(width * height)
        
        // Fill with test data (simulate temperature range 20-30°C)
        for (i in frameData.indices) {
            frameData[i] = (20 + (i % 10)).toByte() // Values 20-29
        }
        
        thermalRecorder.processFrameFromIntensity(
            frameData, width, height, 
            minTempRange = 20f, maxTempRange = 30f
        )
        
        // Wait for processing
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Should process frame")
        
        // Verify results
        assertNotNull(processedStats, "Should have processed stats")
        processedStats?.let { stats ->
            assertEquals(1L, stats.frameSequence, "Should be first frame")
            assertTrue(stats.minTemp >= 20f, "Min temp should be in range")
            assertTrue(stats.maxTemp <= 30f, "Max temp should be in range")
            assertTrue(stats.avgTemp >= 20f && stats.avgTemp <= 30f, "Avg temp should be in range")
            assertTrue(stats.pixelCount > 0, "Should have valid pixels")
        }
    }
    
    @Test
    fun testCsvOutput() = runTest {
        // Start recording
        thermalRecorder.startRecording(testSessionDir, saveImages = false)
        
        // Process a frame
        val frameData = ByteArray(100) // Small test frame
        for (i in frameData.indices) {
            frameData[i] = 25.toByte() // 25°C
        }
        
        thermalRecorder.processFrameFromIntensity(
            frameData, 10, 10,
            minTempRange = 20f, maxTempRange = 30f
        )
        
        // Stop recording to flush data
        thermalRecorder.stopRecording()
        
        // Check CSV file was created
        val sessionDir = File(testSessionDir)
        val csvFiles = sessionDir.listFiles { _, name -> name.startsWith("thermal_stats_") && name.endsWith(".csv") }
        
        assertNotNull(csvFiles, "Should have CSV files")
        assertTrue(csvFiles.isNotEmpty(), "Should have at least one CSV file")
        
        // Check CSV content
        val csvFile = csvFiles.first()
        val lines = csvFile.readLines()
        
        assertTrue(lines.size >= 2, "Should have header + at least one data line")
        
        val header = lines[0]
        assertTrue(header.contains("timestamp_ns"), "Header should contain timestamp")
        assertTrue(header.contains("min_temp_c"), "Header should contain min_temp")
        assertTrue(header.contains("avg_temp_c"), "Header should contain avg_temp")
        assertTrue(header.contains("max_temp_c"), "Header should contain max_temp")
        
        val dataLine = lines[1]
        val parts = dataLine.split(",")
        assertTrue(parts.size >= 5, "Data line should have required columns")
        
        // Verify data format
        val timestamp = parts[0].toLongOrNull()
        assertNotNull(timestamp, "Timestamp should be valid long")
        
        val frameSeq = parts[1].toLongOrNull()
        assertEquals(1L, frameSeq, "Frame sequence should be 1")
        
        val minTemp = parts[2].toFloatOrNull()
        assertNotNull(minTemp, "Min temp should be valid float")
    }
    
    @Test
    fun testImageSaving() = runTest {
        // Start recording with image saving
        thermalRecorder.startRecording(testSessionDir, saveImages = true)
        
        // Process a frame
        val frameData = ByteArray(100)
        for (i in frameData.indices) {
            frameData[i] = (i % 256).toByte()
        }
        
        thermalRecorder.processFrameFromIntensity(
            frameData, 10, 10,
            minTempRange = 0f, maxTempRange = 255f
        )
        
        // Give some time for async image saving
        Thread.sleep(500)
        
        thermalRecorder.stopRecording()
        
        // Check image files were created
        val sessionDir = File(testSessionDir)
        val imageFiles = sessionDir.listFiles { _, name -> 
            name.startsWith("thermal_frame_") && name.endsWith(".png") 
        }
        
        assertNotNull(imageFiles, "Should have image files")
        assertTrue(imageFiles.isNotEmpty(), "Should have at least one image file")
        
        // Verify image file is not empty
        val imageFile = imageFiles.first()
        assertTrue(imageFile.length() > 0, "Image file should not be empty")
    }
    
    @Test
    fun testFrameCounter() = runTest {
        thermalRecorder.startRecording(testSessionDir, saveImages = false)
        
        assertEquals(0L, thermalRecorder.getFrameCount(), "Initial frame count should be 0")
        
        // Process multiple frames
        val frameData = ByteArray(100)
        repeat(5) { i ->
            frameData[0] = i.toByte()
            thermalRecorder.processFrameFromIntensity(
                frameData, 10, 10,
                minTempRange = 0f, maxTempRange = 100f
            )
        }
        
        // Give time for processing
        Thread.sleep(100)
        
        assertEquals(5L, thermalRecorder.getFrameCount(), "Frame count should be 5")
        
        thermalRecorder.stopRecording()
    }
    
    @Test
    fun testErrorHandling() = runTest {
        val latch = CountDownLatch(1)
        var errorReceived = false
        
        thermalRecorder.setFrameListener(object : ThermalRecorder.ThermalFrameListener {
            override fun onFrameProcessed(stats: ThermalRecorder.ThermalFrameStats) {}
            
            override fun onError(error: String) {
                errorReceived = true
                latch.countDown()
            }
        })
        
        // Try to process frame without starting recording
        val frameData = ByteArray(100)
        thermalRecorder.processFrameFromIntensity(
            frameData, 10, 10,
            minTempRange = 0f, maxTempRange = 100f
        )
        
        // Should not generate error for frame processing when not recording
        // (it just ignores the frame)
        assertFalse(thermalRecorder.isRecording(), "Should not be recording")
    }
}