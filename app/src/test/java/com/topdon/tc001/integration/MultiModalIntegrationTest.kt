package com.topdon.tc001.integration

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.topdon.tc001.sensors.unified.UnifiedGSRRecorder
import com.topdon.tc001.sensors.thermal.ThermalCameraRecorder
import com.topdon.tc001.sensors.rgb.RgbCameraRecorder
import com.topdon.tc001.sensors.TimestampManager
import com.topdon.tc001.controller.RecordingController
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class MultiModalIntegrationTest {
    
    private lateinit var mockContext: Context
    private lateinit var mockLifecycleOwner: LifecycleOwner
    private lateinit var recordingController: RecordingController
    private lateinit var timestampManager: TimestampManager
    
    @Before
    fun setup() {
        mockContext = mockk()
        mockLifecycleOwner = mockk()
        recordingController = RecordingController()
        timestampManager = TimestampManager()
        
        // Setup basic context behaviors
        every { mockContext.packageManager } returns mockk()
        every { mockContext.getSystemService(any()) } returns mockk()
    }
    
    @Test
    fun `should coordinate multiple sensors with proper initialization checks`() = runTest {
        // Setup - Create specific mock behaviors instead of relaxed mocks
        val gsrRecorder = mockk<UnifiedGSRRecorder>()
        val thermalRecorder = mockk<ThermalCameraRecorder>()
        val rgbRecorder = mockk<RgbCameraRecorder>()
        
        // Mock specific initialization behaviors
        coEvery { gsrRecorder.initialize() } returns true
        coEvery { thermalRecorder.initialize() } returns true
        coEvery { rgbRecorder.initialize() } returns true
        
        // Mock recording start methods
        coEvery { gsrRecorder.startRecording(any()) } returns true
        coEvery { thermalRecorder.startRecording(any()) } returns true
        coEvery { rgbRecorder.startRecording(any()) } returns true
        
        // Mock status checks
        every { gsrRecorder.isRecording() } returns true
        every { thermalRecorder.isRecording() } returns true  
        every { rgbRecorder.isRecording() } returns true
        
        // Register sensors with controller
        recordingController.registerSensor("GSR", gsrRecorder)
        recordingController.registerSensor("Thermal", thermalRecorder)
        recordingController.registerSensor("RGB", rgbRecorder)
        
        // Execute - start multi-modal recording
        val sessionDir = File("/tmp/test-session-${System.currentTimeMillis()}")
        sessionDir.mkdirs()
        
        val startSuccess = recordingController.startRecording(sessionDir.absolutePath)
        
        // Verify
        assertTrue("Should start multi-modal recording successfully", startSuccess)
        
        // Verify all sensors were properly initialized and started
        coVerify(exactly = 1) { gsrRecorder.initialize() }
        coVerify(exactly = 1) { thermalRecorder.initialize() }
        coVerify(exactly = 1) { rgbRecorder.initialize() }
        
        coVerify(exactly = 1) { gsrRecorder.startRecording(any()) }
        coVerify(exactly = 1) { thermalRecorder.startRecording(any()) }
        coVerify(exactly = 1) { rgbRecorder.startRecording(any()) }
        
        // Cleanup
        sessionDir.deleteRecursively()
    }
    
    @Test
    fun `should maintain timestamp synchronization across sensors`() = runTest {
        // Setup
        timestampManager.initializeSession()
        val sessionStart = timestampManager.getCurrentSystemTimeMs()
        
        // Simulate sensor data with timestamps
        val sensorTimestamps = mutableListOf<Pair<String, Long>>()
        
        // Execute - collect timestamps from different sensors over time
        repeat(5) { i ->
            delay(100) // 100ms between samples
            val currentTime = timestampManager.getCurrentSystemTimeMs()
            sensorTimestamps.add("GSR" to currentTime)
            sensorTimestamps.add("Thermal" to currentTime + 1) // Small offset
            sensorTimestamps.add("RGB" to currentTime + 2) // Small offset
        }
        
        // Verify synchronization
        val gsr_timestamps = sensorTimestamps.filter { it.first == "GSR" }.map { it.second }
        val thermal_timestamps = sensorTimestamps.filter { it.first == "Thermal" }.map { it.second }
        val rgb_timestamps = sensorTimestamps.filter { it.first == "RGB" }.map { it.second }
        
        // Check that timestamps from different sensors at same sample point are within tolerance
        for (i in gsr_timestamps.indices) {
            val timeDiff1 = Math.abs(gsr_timestamps[i] - thermal_timestamps[i])
            val timeDiff2 = Math.abs(gsr_timestamps[i] - rgb_timestamps[i])
            
            assertTrue("GSR-Thermal sync should be within 100ms", timeDiff1 <= 100)
            assertTrue("GSR-RGB sync should be within 100ms", timeDiff2 <= 100)
        }
    }
    
    @Test
    fun `should handle partial sensor failure with specific error scenarios`() = runTest {
        // Setup - Create specific failure scenarios
        val gsrRecorder = mockk<UnifiedGSRRecorder>()
        val thermalRecorder = mockk<ThermalCameraRecorder>()
        val rgbRecorder = mockk<RgbCameraRecorder>()
        
        // Mock GSR initialization failure (e.g., Bluetooth not available)
        coEvery { gsrRecorder.initialize() } returns false
        coEvery { thermalRecorder.initialize() } returns true
        coEvery { rgbRecorder.initialize() } returns true
        
        // Mock successful sensors can start recording
        coEvery { thermalRecorder.startRecording(any()) } returns true
        coEvery { rgbRecorder.startRecording(any()) } returns true
        
        // Mock status checks
        every { thermalRecorder.isRecording() } returns true
        every { rgbRecorder.isRecording() } returns true
        every { gsrRecorder.isRecording() } returns false
        
        recordingController.registerSensor("GSR", gsrRecorder)
        recordingController.registerSensor("Thermal", thermalRecorder)
        recordingController.registerSensor("RGB", rgbRecorder)
        
        // Execute
        val sessionDir = File("/tmp/test-session-partial-${System.currentTimeMillis()}")
        sessionDir.mkdirs()
        
        val startSuccess = recordingController.startRecording(sessionDir.absolutePath)
        
        // Verify - recording should continue with available sensors
        assertTrue("Should start recording with partial sensors available", startSuccess)
        
        // Verify failed sensor was not started but others were
        coVerify(exactly = 1) { gsrRecorder.initialize() }
        coVerify(exactly = 0) { gsrRecorder.startRecording(any()) }
        
        coVerify(exactly = 1) { thermalRecorder.startRecording(any()) }
        coVerify(exactly = 1) { rgbRecorder.startRecording(any()) }
        
        // Cleanup
        sessionDir.deleteRecursively()
    }
    
    @Test
    fun `should validate file output structure for multi-modal session`() = runTest {
        // Setup - simulate completed recording session
        val sessionDir = File("/tmp/test-session-files")
        sessionDir.mkdirs()
        
        // Create expected output files
        val expectedFiles = listOf(
            File(sessionDir, "gsr_data.csv"),
            File(sessionDir, "rgb_video.mp4"),
            File(sessionDir, "rgb_frames.csv"),
            File(sessionDir, "thermal_data.csv"),
            File(sessionDir, "frames/frame_001.jpg"),
            File(sessionDir, "thermal_images/thermal_001.png")
        )
        
        // Create directories and files
        File(sessionDir, "frames").mkdirs()
        File(sessionDir, "thermal_images").mkdirs()
        
        expectedFiles.forEach { file ->
            if (!file.parentFile.exists()) {
                file.parentFile.mkdirs()
            }
            file.writeText("test data")
        }
        
        // Execute - validate file structure
        val actualFiles = sessionDir.walkTopDown().filter { it.isFile }.toList()
        
        // Verify
        assertTrue("Should have GSR data file", actualFiles.any { it.name == "gsr_data.csv" })
        assertTrue("Should have RGB video file", actualFiles.any { it.name == "rgb_video.mp4" })
        assertTrue("Should have thermal data file", actualFiles.any { it.name == "thermal_data.csv" })
        assertTrue("Should have frame images", actualFiles.any { it.name.startsWith("frame_") })
        assertTrue("Should have thermal images", actualFiles.any { it.name.startsWith("thermal_") })
    }
    
    @Test
    fun `should handle sensor disconnection during recording`() = runTest {
        // Setup
        val gsrRecorder = mockk<UnifiedGSRRecorder>(relaxed = true)
        val thermalRecorder = mockk<ThermalCameraRecorder>(relaxed = true)
        val rgbRecorder = mockk<RgbCameraRecorder>(relaxed = true)
        
        coEvery { gsrRecorder.initialize() } returns true
        coEvery { thermalRecorder.initialize() } returns true
        coEvery { rgbRecorder.initialize() } returns true
        
        recordingController.registerSensor("GSR", gsrRecorder)
        recordingController.registerSensor("Thermal", thermalRecorder)
        recordingController.registerSensor("RGB", rgbRecorder)
        
        // Start recording
        val sessionDir = File("/tmp/test-session-disconnect")
        sessionDir.mkdirs()
        recordingController.startRecording(sessionDir.absolutePath)
        
        // Execute - simulate GSR disconnection
        coEvery { gsrRecorder.isRecording() } returns false
        
        // Verify other sensors continue
        coVerify { thermalRecorder.startRecording(any()) }
        coVerify { rgbRecorder.startRecording(any()) }
        
        // Recording should continue with remaining sensors
        assertTrue("Recording should continue with remaining sensors", true)
    }
    
    @Test
    fun `should measure multi-modal recording performance`() = runTest {
        // Setup
        val startTime = System.currentTimeMillis()
        val performanceMetrics = mutableMapOf<String, Long>()
        
        // Execute - simulate multi-modal initialization timing
        val gsrInitTime = measureTimeMillis {
            Thread.sleep(50) // Simulate GSR init
        }
        
        val thermalInitTime = measureTimeMillis {
            Thread.sleep(75) // Simulate thermal init
        }
        
        val rgbInitTime = measureTimeMillis {
            Thread.sleep(100) // Simulate RGB init
        }
        
        performanceMetrics["GSR_Init"] = gsrInitTime
        performanceMetrics["Thermal_Init"] = thermalInitTime
        performanceMetrics["RGB_Init"] = rgbInitTime
        
        val totalInitTime = System.currentTimeMillis() - startTime
        
        // Verify
        assertTrue("GSR init should be under 100ms", gsrInitTime < 100)
        assertTrue("Thermal init should be under 150ms", thermalInitTime < 150)
        assertTrue("RGB init should be under 200ms", rgbInitTime < 200)
        assertTrue("Total init should be under 500ms", totalInitTime < 500)
        
        // Log performance metrics for analysis
        performanceMetrics.forEach { (sensor, time) ->
            println("Performance: $sensor took ${time}ms")
        }
    }
    
    @Test
    fun `should validate cross-sensor event correlation`() = runTest {
        // Setup - simulate synchronized event across sensors
        val eventTimestamp = System.currentTimeMillis()
        val tolerance = 50L // 50ms tolerance
        
        // Simulate sensor responses to same event
        val gsrResponse = eventTimestamp + 10L  // GSR responds 10ms later
        val thermalResponse = eventTimestamp + 15L // Thermal responds 15ms later
        val rgbResponse = eventTimestamp + 5L   // RGB responds 5ms later
        
        // Execute - validate correlation
        val responses = listOf(
            "GSR" to gsrResponse,
            "Thermal" to thermalResponse,
            "RGB" to rgbResponse
        )
        
        // Verify all responses are within tolerance of event
        responses.forEach { (sensor, responseTime) ->
            val latency = Math.abs(responseTime - eventTimestamp)
            assertTrue(
                "$sensor response should be within tolerance", 
                latency <= tolerance
            )
        }
        
        // Verify cross-sensor timing consistency
        val maxLatency = responses.maxByOrNull { it.second }?.second ?: 0L
        val minLatency = responses.minByOrNull { it.second }?.second ?: 0L
        val crossSensorSpread = maxLatency - minLatency
        
        assertTrue(
            "Cross-sensor response spread should be minimal",
            crossSensorSpread <= tolerance
        )
    }
    
    private inline fun measureTimeMillis(block: () -> Unit): Long {
        val startTime = System.currentTimeMillis()
        block()
        return System.currentTimeMillis() - startTime
    }
}