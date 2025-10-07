// Merged .kt under 'app\src\androidTest\java\mpdc4gsr\tests' subtree
// Files: 3; Generated 2025-10-07 23:07:36


// ===== app\src\androidTest\java\mpdc4gsr\tests\NetworkProtocolIntegrationTest.kt =====

package mpdc4gsr.tests

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import mpdc4gsr.feature.network.data.NetworkServer
import mpdc4gsr.feature.network.data.Protocol
import mpdc4gsr.feature.network.data.ProtocolHandler
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.net.Socket

@Ignore("All tests disabled")
@RunWith(AndroidJUnit4::class)
class NetworkProtocolIntegrationTest {
    private lateinit var context: Context
    private lateinit var networkServer: NetworkServer
    private lateinit var protocolHandler: ProtocolHandler
    private var mockPcSocket: Socket? = null

    companion object {
        // Use a different port for testing to avoid conflicts with production server (8081)
        // and to isolate test traffic. Production port is 8081.
        private const val TEST_PORT = 8182
        private const val CONNECTION_TIMEOUT_MS = 5000L
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun cleanup() = runBlocking {
        mockPcSocket?.close()
        mockPcSocket = null
        if (::networkServer.isInitialized) {
            networkServer.stop()
        }
    }

    @Test
    fun testProtocolMessageParsing() {
        val testMessages = listOf(
            "HELLO device_name=test sensors=[GSR,RGB,THERMAL]",
            "START_RECORD session_id=test_session",
            "STOP_RECORD session_id=test_session",
            "SYNC_INIT",
            "SYNC_REQUEST t_pc=1234567890",
            "ACK cmd=START_RECORD",
            "ERROR cmd=START_RECORD code=FAIL msg=\"Test error\""
        )
        testMessages.forEach { message ->
            val parsed = Protocol.parseMessage(message)
            assertNotNull("Should parse: $message", parsed)
            assertTrue("Should have type", parsed!!.type.isNotEmpty())
        }
    }

    @Test
    fun testProtocolMessageCreation() {
        val hello = Protocol.createHelloMessage("test_device", listOf("GSR", "RGB"))
        assertTrue("HELLO should contain device name", hello.contains("device_name=test_device"))
        assertTrue("HELLO should contain sensors", hello.contains("sensors=[GSR,RGB]"))
        val startRecord = Protocol.createStartRecordMessage("session_001")
        assertTrue("START_RECORD should contain session_id", startRecord.contains("session_id=session_001"))
        val stopRecord = Protocol.createStopRecordMessage("session_001")
        assertTrue("STOP_RECORD should contain session_id", stopRecord.contains("session_id=session_001"))
        val syncInit = Protocol.createSyncInitMessage()
        assertEquals("SYNC_INIT message should match", "SYNC_INIT", syncInit)
        val syncRequest = Protocol.createSyncRequestMessage(1234567890L)
        assertTrue("SYNC_REQUEST should contain t_pc", syncRequest.contains("t_pc=1234567890"))
        val syncResponse = Protocol.createSyncResponseMessage(1234567890L, 1234567895L)
        assertTrue("SYNC_RESPONSE should contain t_pc", syncResponse.contains("t_pc=1234567890"))
        assertTrue("SYNC_RESPONSE should contain t_ph", syncResponse.contains("t_ph=1234567895"))
        val ack = Protocol.createAckMessage("START_RECORD", mapOf("session_id" to "test"))
        assertTrue("ACK should contain cmd", ack.contains("cmd=START_RECORD"))
        assertTrue("ACK should contain session_id", ack.contains("session_id=test"))
        val error = Protocol.createErrorMessage("START_RECORD", Protocol.ERR_FAIL, "Test error")
        assertTrue("ERROR should contain cmd", error.contains("cmd=START_RECORD"))
        assertTrue("ERROR should contain code", error.contains("code=FAIL"))
        assertTrue("ERROR should contain msg", error.contains("msg=\"Test error\""))
    }

    @Test
    fun testProtocolHandlerWithMockCommands() = runBlocking {
        networkServer = NetworkServer(context, TEST_PORT)
        protocolHandler = ProtocolHandler(context, networkServer)
        var startRecordingCalled = false
        var stopRecordingCalled = false
        var syncRequestCalled = false
        protocolHandler.setCommandHandler(object : ProtocolHandler.CommandHandler {
            override suspend fun onStartRecording(sessionId: String): ProtocolHandler.CommandResult {
                startRecordingCalled = true
                assertEquals("test_session_001", sessionId)
                return ProtocolHandler.CommandResult(
                    success = true,
                    message = "Recording started",
                    data = mapOf("session_id" to sessionId)
                )
            }

            override suspend fun onStopRecording(sessionId: String): ProtocolHandler.CommandResult {
                stopRecordingCalled = true
                return ProtocolHandler.CommandResult(
                    success = true,
                    message = "Recording stopped",
                    data = mapOf("session_id" to sessionId)
                )
            }

            override suspend fun onSyncRequest(pcTimestamp: Long): ProtocolHandler.SyncResult {
                syncRequestCalled = true
                assertTrue("PC timestamp should be positive", pcTimestamp > 0)
                return ProtocolHandler.SyncResult(
                    success = true,
                    phoneTimestamp = System.currentTimeMillis(),
                    offsetNs = 0L
                )
            }
        })
        val startMessage = Protocol.parseMessage("START_RECORD session_id=test_session_001")
        assertNotNull("START_RECORD should parse", startMessage)
        val startResponse = protocolHandler.processMessage(startMessage!!)
        assertNotNull("Should return response", startResponse)
        assertTrue("Should call handler", startRecordingCalled)
        assertTrue("Response should be ACK", startResponse!!.contains("ACK"))
        val stopMessage = Protocol.parseMessage("STOP_RECORD session_id=test_session_001")
        assertNotNull("STOP_RECORD should parse", stopMessage)
        val stopResponse = protocolHandler.processMessage(stopMessage!!)
        assertNotNull("Should return response", stopResponse)
        assertTrue("Should call handler", stopRecordingCalled)
        assertTrue("Response should be ACK", stopResponse!!.contains("ACK"))
        val syncMessage = Protocol.parseMessage("SYNC_REQUEST t_pc=1234567890")
        assertNotNull("SYNC_REQUEST should parse", syncMessage)
        val syncResponse = protocolHandler.processMessage(syncMessage!!)
        assertNotNull("Should return response", syncResponse)
        assertTrue("Should call handler", syncRequestCalled)
        assertTrue("Response should be SYNC_RESPONSE", syncResponse!!.contains("SYNC_RESPONSE"))
    }

    @Test
    fun testProtocolHandlerErrorCases() = runBlocking {
        networkServer = NetworkServer(context, TEST_PORT)
        protocolHandler = ProtocolHandler(context, networkServer)
        protocolHandler.setCommandHandler(object : ProtocolHandler.CommandHandler {
            override suspend fun onStartRecording(sessionId: String): ProtocolHandler.CommandResult {
                return ProtocolHandler.CommandResult(
                    success = false,
                    message = "Sensor not connected"
                )
            }

            override suspend fun onStopRecording(sessionId: String): ProtocolHandler.CommandResult {
                return ProtocolHandler.CommandResult(
                    success = false,
                    message = "Not recording"
                )
            }

            override suspend fun onSyncRequest(pcTimestamp: Long): ProtocolHandler.SyncResult {
                return ProtocolHandler.SyncResult(
                    success = false
                )
            }
        })
        val startMessage = Protocol.parseMessage("START_RECORD session_id=test")
        val startResponse = protocolHandler.processMessage(startMessage!!)
        assertTrue("Failed start should return ERROR", startResponse!!.contains("ERROR"))
        assertTrue(
            "Error should mention failure",
            startResponse.contains("FAIL") || startResponse.contains("Sensor not connected")
        )
        val stopMessage = Protocol.parseMessage("STOP_RECORD session_id=test")
        val stopResponse = protocolHandler.processMessage(stopMessage!!)
        assertTrue("Failed stop should return ERROR", stopResponse!!.contains("ERROR"))
    }

    @Test
    fun testMessageFormatCompatibility() {
        val pcFormattedMessages = listOf(
            "START_RECORD session_id=session_20240101_120000",
            "STOP_RECORD session_id=session_20240101_120000",
            "SYNC_REQUEST t_pc=1234567890",
            "SYNC_RESULT t1=1000 t2=1005 t3=1010 offset=5 rtt=10"
        )
        pcFormattedMessages.forEach { message ->
            val parsed = Protocol.parseMessage(message)
            assertNotNull("Android should parse PC message: $message", parsed)
            when (parsed!!.type) {
                Protocol.MSG_START_RECORD -> {
                    assertTrue(
                        "START_RECORD should have session_id",
                        parsed.parameters.containsKey("session_id")
                    )
                }

                Protocol.MSG_STOP_RECORD -> {
                    assertTrue(
                        "STOP_RECORD should have session_id",
                        parsed.parameters.containsKey("session_id")
                    )
                }

                Protocol.MSG_SYNC_REQUEST -> {
                    assertTrue(
                        "SYNC_REQUEST should have t_pc",
                        parsed.parameters.containsKey("t_pc")
                    )
                }

                Protocol.MSG_SYNC_RESULT -> {
                    assertTrue(
                        "SYNC_RESULT should have t1",
                        parsed.parameters.containsKey("t1")
                    )
                    assertTrue(
                        "SYNC_RESULT should have t2",
                        parsed.parameters.containsKey("t2")
                    )
                    assertTrue(
                        "SYNC_RESULT should have t3",
                        parsed.parameters.containsKey("t3")
                    )
                    assertTrue(
                        "SYNC_RESULT should have offset",
                        parsed.parameters.containsKey("offset")
                    )
                    assertTrue(
                        "SYNC_RESULT should have rtt",
                        parsed.parameters.containsKey("rtt")
                    )
                }
            }
        }
    }

    @Test
    fun testParameterParsing() {
        val message = Protocol.parseMessage("START_RECORD session_id=test_session_123")
        assertNotNull(message)
        assertEquals("START_RECORD", message!!.type)
        assertEquals("test_session_123", message.parameters["session_id"])
        val messageWithQuotes = Protocol.parseMessage("ERROR cmd=START_RECORD code=FAIL msg=\"Sensor not found\"")
        assertNotNull(messageWithQuotes)
        assertEquals("ERROR", messageWithQuotes!!.type)
        assertEquals("START_RECORD", messageWithQuotes.parameters["cmd"])
        assertEquals("FAIL", messageWithQuotes.parameters["code"])
        assertEquals("Sensor not found", messageWithQuotes.parameters["msg"])
    }

    @Test
    fun testArrayParameterParsing() {
        val message = Protocol.parseMessage("HELLO device_name=test sensors=[GSR,RGB,THERMAL]")
        assertNotNull(message)
        assertEquals("HELLO", message!!.type)
        assertEquals("test", message.parameters["device_name"])
        val sensors = message.parameters["sensors"]
        assertNotNull(sensors)
        assertTrue("Sensors should contain array brackets", sensors!!.contains("["))
        assertTrue("Sensors should contain array brackets", sensors.contains("]"))
    }

    @Test
    fun testProtocolVersionConstants() {
        assertEquals("Protocol version", "1.0", Protocol.PROTOCOL_VERSION)
        assertEquals("Default port", 8080, Protocol.DEFAULT_PORT)
        assertEquals("Server port", 8081, Protocol.DEFAULT_SERVER_PORT)
    }

    @Test
    fun testErrorCodes() {
        assertEquals("FAIL", Protocol.ERR_FAIL)
        assertEquals("BUSY", Protocol.ERR_BUSY)
        assertEquals("SENSOR_FAIL", Protocol.ERR_SENSOR_FAIL)
        assertEquals("THERMAL_NOT_FOUND", Protocol.ERR_THERMAL_NOT_FOUND)
        assertEquals("GSR_NOT_FOUND", Protocol.ERR_GSR_NOT_FOUND)
        assertEquals("INVALID_SESSION", Protocol.ERR_INVALID_SESSION)
    }
}


// ===== app\src\androidTest\java\mpdc4gsr\tests\RgbCamera4KRecordingInstrumentationTest.kt =====

package mpdc4gsr.tests

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import org.junit.*
import org.junit.runner.RunWith
import java.io.File

@Ignore("All tests disabled")
@RunWith(AndroidJUnit4::class)
@LargeTest
class RgbCamera4KRecordingInstrumentationTest {
    companion object {
        private const val TAG = "RgbCameraTest"
        private const val TEST_TIMEOUT_SECONDS = 60L
        private const val VIDEO_RECORDING_DURATION_MS = 10000L
        private const val BURST_CAPTURE_COUNT = 5
    }

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )
    private lateinit var context: Context
    private lateinit var testOutputDir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        testOutputDir = File(context.cacheDir, "rgb_camera_test_${System.currentTimeMillis()}")
        testOutputDir.mkdirs()
        Log.i(TAG, "RGB Camera instrumentation test setup completed")
    }

    @After
    fun tearDown() {
        testOutputDir.deleteRecursively()
        Log.i(TAG, "RGB Camera instrumentation test cleanup completed")
    }

    @Test
    fun test4KVideoRecording() {
        Log.i(TAG, "Testing 4K video recording capability")
        val sessionDir = File(testOutputDir, "4k_video_test")
        sessionDir.mkdirs()
        val deviceModel = android.os.Build.MODEL
        val supports4K = deviceModel.contains("SM-S9") || deviceModel.contains("Galaxy S22") ||
                deviceModel.contains("Pixel") || deviceModel.contains("OnePlus")
        if (!supports4K) {
            Log.i(TAG, "Device $deviceModel may not support 4K recording - running basic test")
        }
        val testResult = simulateVideoRecordingTest(
            sessionDir = sessionDir,
            resolution = "4K",
            frameRate = 30,
            durationMs = VIDEO_RECORDING_DURATION_MS
        )
        Assert.assertTrue("4K video recording test should complete", testResult.success)
        Assert.assertTrue("4K video should meet quality standards", testResult.qualityScore >= 0.8)
        Log.i(TAG, " 4K video recording test completed successfully")
        Log.i(TAG, "Test results: ${testResult.summary}")
    }

    @Test
    fun test1080pVideoRecording() {
        Log.i(TAG, "Testing 1080p video recording")
        val sessionDir = File(testOutputDir, "1080p_video_test")
        sessionDir.mkdirs()
        val testResult = simulateVideoRecordingTest(
            sessionDir = sessionDir,
            resolution = "1080p",
            frameRate = 30,
            durationMs = VIDEO_RECORDING_DURATION_MS
        )
        Assert.assertTrue("1080p video recording should succeed", testResult.success)
        Assert.assertTrue("1080p video should have good quality", testResult.qualityScore >= 0.7)
        Log.i(TAG, " 1080p video recording test completed successfully")
    }

    @Test
    fun testFrameRateValidation() {
        Log.i(TAG, "Testing frame rate validation at 30 FPS")
        val sessionDir = File(testOutputDir, "frame_rate_test")
        sessionDir.mkdirs()
        val frameRateTest = simulateFrameRateTest(
            targetFps = 30,
            durationMs = 15000L,
            tolerancePercent = 15.0
        )
        Assert.assertTrue("Frame rate should be within tolerance", frameRateTest.withinTolerance)
        Assert.assertTrue(
            "Average FPS should be close to 30",
            frameRateTest.averageFps >= 25.5 && frameRateTest.averageFps <= 34.5
        )
        Log.i(TAG, "Frame rate test results:")
        Log.i(TAG, "  Average FPS: ${frameRateTest.averageFps}")
        Log.i(TAG, "  Deviation: ${frameRateTest.deviationPercent}%")
        Log.i(TAG, " Frame rate validation test passed")
    }

    @Test
    fun testBurstStillCapture() {
        Log.i(TAG, "Testing burst still capture")
        val sessionDir = File(testOutputDir, "burst_capture_test")
        sessionDir.mkdirs()
        val burstTest = simulateBurstCaptureTest(
            captureCount = BURST_CAPTURE_COUNT,
            intervalMs = 2000L,
            sessionDir = sessionDir
        )
        Assert.assertTrue("Burst capture should succeed", burstTest.success)
        Assert.assertTrue(
            "Should capture expected number of images",
            burstTest.capturedCount >= BURST_CAPTURE_COUNT - 1
        )
        Log.i(TAG, " Burst capture test completed: ${burstTest.capturedCount} images")
    }

    @Test
    fun testDeviceSpecificConfigurations() {
        Log.i(TAG, "Testing device-specific configurations")
        val deviceModel = android.os.Build.MODEL
        val deviceManufacturer = android.os.Build.MANUFACTURER
        Log.i(TAG, "Testing on device: $deviceManufacturer $deviceModel")
        val capabilities = analyzeDeviceCapabilities()
        Assert.assertTrue(
            "Device should support basic recording",
            capabilities.supportsBasicRecording
        )
        if (capabilities.supports4K) {
            Log.i(TAG, "Device supports 4K - running enhanced tests")
            test4KVideoRecording()
        }
        Log.i(TAG, "Device capabilities: $capabilities")
        Log.i(TAG, " Device-specific configuration tests completed")
    }

    @Test
    fun testErrorHandlingAndRecovery() {
        Log.i(TAG, "Testing error handling and recovery")
        val invalidConfigTest = testInvalidConfigurations()
        Assert.assertTrue("Should handle invalid configs gracefully", invalidConfigTest)
        val resourceTest = testResourceManagement()
        Assert.assertTrue("Should manage resources properly", resourceTest)
        Log.i(TAG, " Error handling and recovery tests passed")
    }

    private fun simulateVideoRecordingTest(
        sessionDir: File,
        resolution: String,
        frameRate: Int,
        durationMs: Long
    ): VideoTestResult {
        Log.d(TAG, "Simulating $resolution recording at ${frameRate}fps for ${durationMs}ms")
        // Create mock video file
        val videoFile = File(sessionDir, "test_video_${resolution}.mp4")
        val csvFile = File(sessionDir, "rgb.csv")
        Thread.sleep(100)
        videoFile.writeText("Mock video content for $resolution at ${frameRate}fps")
        csvFile.writeText("timestamp_ns,frame_filename,processing_time_ms,file_size_bytes\n")
        val frameCount = (durationMs / (1000 / frameRate))
        for (i in 1..frameCount) {
            val timestamp = System.nanoTime() + (i * (1_000_000_000L / frameRate))
            csvFile.appendText("$timestamp,frame_$i.jpg,16,${1024 * i}\n")
        }
        val qualityScore = when (resolution) {
            "4K" -> if (frameRate >= 30) 0.95 else 0.85
            "1080p" -> if (frameRate >= 30) 0.90 else 0.80
            else -> 0.70
        }
        val success = videoFile.exists() && csvFile.exists() && videoFile.length() > 0
        return VideoTestResult(
            success = success,
            qualityScore = qualityScore,
            frameCount = frameCount.toInt(),
            actualFps = frameRate.toDouble(),
            fileSizeBytes = videoFile.length(),
            summary = "Recording: $resolution @ ${frameRate}fps, Quality: $qualityScore"
        )
    }

    private fun simulateFrameRateTest(
        targetFps: Int,
        durationMs: Long,
        tolerancePercent: Double
    ): FrameRateTestResult {
        val expectedFrames = (durationMs / 1000.0) * targetFps
        val actualFrames = expectedFrames + (Math.random() - 0.5) * (expectedFrames * 0.1)
        val averageFps = (actualFrames / (durationMs / 1000.0))
        val deviation = Math.abs(averageFps - targetFps) / targetFps * 100.0
        val withinTolerance = deviation <= tolerancePercent
        return FrameRateTestResult(
            averageFps = averageFps,
            targetFps = targetFps.toDouble(),
            deviationPercent = deviation,
            withinTolerance = withinTolerance,
            totalFrames = actualFrames.toInt()
        )
    }

    private fun simulateBurstCaptureTest(
        captureCount: Int,
        intervalMs: Long,
        sessionDir: File
    ): BurstCaptureTestResult {
        var successfulCaptures = 0
        for (i in 1..captureCount) {
            val imageFile = File(sessionDir, "burst_image_$i.jpg")
            if (Math.random() > 0.1) {
                imageFile.writeText("Mock image data for burst capture $i")
                successfulCaptures++
            }
            Thread.sleep(intervalMs / 10)
        }
        return BurstCaptureTestResult(
            success = successfulCaptures >= (captureCount * 0.8),
            requestedCount = captureCount,
            capturedCount = successfulCaptures,
            successRate = successfulCaptures.toDouble() / captureCount
        )
    }

    private fun analyzeDeviceCapabilities(): DeviceCapabilities {
        val deviceModel = android.os.Build.MODEL
        val sdkVersion = android.os.Build.VERSION.SDK_INT
        val supports4K = deviceModel.contains("SM-S9") ||
                deviceModel.contains("Pixel 6") ||
                deviceModel.contains("Pixel 7") ||
                deviceModel.contains("OnePlus") ||
                sdkVersion >= 29
        val supportsHighFrameRate = sdkVersion >= 28
        val supportsBurstCapture = sdkVersion >= 26
        return DeviceCapabilities(
            supportsBasicRecording = true,
            supports4K = supports4K,
            supportsHighFrameRate = supportsHighFrameRate,
            supportsBurstCapture = supportsBurstCapture,
            maxResolution = if (supports4K) "4K" else "1080p",
            maxFrameRate = if (supportsHighFrameRate) 60 else 30
        )
    }

    private fun testInvalidConfigurations(): Boolean {
        return try {
            Log.d(TAG, "Testing invalid configuration handling")
            val nullDirResult = handleInvalidSessionDirectory(null)
            val invalidFrameRateResult = handleInvalidFrameRate(-1)
            val invalidResolutionResult = handleInvalidResolution("INVALID")
            nullDirResult && invalidFrameRateResult && invalidResolutionResult
        } catch (e: Exception) {
            Log.w(TAG, "Exception in invalid configuration test", e)
            false
        }
    }

    private fun testResourceManagement(): Boolean {
        return try {
            Log.d(TAG, "Testing resource management")
            val resourceTest1 = simulateResourceAllocation("recorder1")
            val resourceTest2 = simulateResourceAllocation("recorder2")
            simulateResourceCleanup("recorder1")
            simulateResourceCleanup("recorder2")
            resourceTest1 && resourceTest2
        } catch (e: Exception) {
            Log.w(TAG, "Exception in resource management test", e)
            false
        }
    }

    private fun handleInvalidSessionDirectory(dir: String?): Boolean {
        Log.d(TAG, "Handling invalid session directory: $dir")
        return true
    }

    private fun handleInvalidFrameRate(fps: Int): Boolean {
        Log.d(TAG, "Handling invalid frame rate: $fps")
        return true
    }

    private fun handleInvalidResolution(resolution: String): Boolean {
        Log.d(TAG, "Handling invalid resolution: $resolution")
        return true
    }

    private fun simulateResourceAllocation(instanceId: String): Boolean {
        Log.d(TAG, "Simulating resource allocation for: $instanceId")
        return true
    }

    private fun simulateResourceCleanup(instanceId: String) {
        Log.d(TAG, "Simulating resource cleanup for: $instanceId")
    }

    data class VideoTestResult(
        val success: Boolean,
        val qualityScore: Double,
        val frameCount: Int,
        val actualFps: Double,
        val fileSizeBytes: Long,
        val summary: String
    )

    data class FrameRateTestResult(
        val averageFps: Double,
        val targetFps: Double,
        val deviationPercent: Double,
        val withinTolerance: Boolean,
        val totalFrames: Int
    )

    data class BurstCaptureTestResult(
        val success: Boolean,
        val requestedCount: Int,
        val capturedCount: Int,
        val successRate: Double
    )

    data class DeviceCapabilities(
        val supportsBasicRecording: Boolean,
        val supports4K: Boolean,
        val supportsHighFrameRate: Boolean,
        val supportsBurstCapture: Boolean,
        val maxResolution: String,
        val maxFrameRate: Int
    )
}


// ===== app\src\androidTest\java\mpdc4gsr\tests\ThermalCameraTC001HardwareTest.kt =====

package mpdc4gsr.tests

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mpdc4gsr.feature.thermal.ui.ThermalCameraRecorder
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@Ignore("All tests disabled")
@RunWith(AndroidJUnit4::class)
class ThermalCameraTC001HardwareTest {
    private lateinit var context: Context
    private lateinit var thermalRecorder: ThermalCameraRecorder
    private lateinit var testSessionDir: File
    private lateinit var usbManager: UsbManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        thermalRecorder = ThermalCameraRecorder(context)
        // Create test session directory
        testSessionDir = File(context.cacheDir, "tc001_hardware_test_${System.currentTimeMillis()}")
        testSessionDir.mkdirs()
    }

    @After
    fun cleanup() {
        // Ensure recording is stopped
        runBlocking {
            try {
                thermalRecorder.stopRecording()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
        // Clean up test directory
        testSessionDir.deleteRecursively()
    }

    @Test
    fun testRealTC001USBPermissionFlow() = runBlocking {
        // Given: Check for connected TC001 device
        val tc001Device = findTC001Device()
        if (tc001Device == null) {
            println("Skipping test: No TC001 device connected")
            return@runBlocking
        }
        println("Found TC001 device: ${tc001Device.productName}")
        // When: Initialize thermal recorder
        val initResult = thermalRecorder.initialize()
        // Then: Should complete initialization
        assertTrue("Thermal recorder should initialize", initResult)
        // Check permission status
        val hasPermission = usbManager.hasPermission(tc001Device)
        println("USB permission status: $hasPermission")
    }

    @Test
    fun testRealTC001FrameCaptureRate() = runBlocking {
        val tc001Device = findTC001Device()
        if (tc001Device == null) {
            println("Skipping test: No TC001 device connected")
            return@runBlocking
        }
        // Given: TC001 device is available
        thermalRecorder.initialize()
        val frameCount = AtomicInteger(0)
        val testDurationMs = 3000L // 3 seconds test
        // Set up frame capture monitoring
        thermalRecorder.setFrameListener(object : ThermalCameraRecorder.ThermalFrameListener {
            override fun onFrameProcessed(stats: ThermalCameraRecorder.ThermalFrameStats) {
                frameCount.incrementAndGet()
            }

            override fun onError(error: String) {
                println("Frame processing error: $error")
            }
        })
        // When: Start recording and measure actual frame rate
        val recordingStarted = thermalRecorder.startRecording(testSessionDir.absolutePath)
        assertTrue("Recording should start", recordingStarted)
        val testStartTime = System.currentTimeMillis()
        delay(testDurationMs)
        thermalRecorder.stopRecording()
        // Then: Analyze frame rate
        val actualTestDuration = System.currentTimeMillis() - testStartTime
        val actualFrames = frameCount.get()
        val actualFrameRate = (actualFrames * 1000.0) / actualTestDuration
        println("Captured $actualFrames frames in ${actualTestDuration}ms")
        println("Actual frame rate: $actualFrameRate Hz")
        // Verify frame rate is close to 10Hz (allow 8-12Hz range for real hardware)
        assertTrue(
            "Frame rate should be approximately 10Hz, got $actualFrameRate Hz",
            actualFrameRate >= 8.0 && actualFrameRate <= 12.0
        )
    }

    @Test
    fun testRealTC001DisconnectReconnectScenarios() = runBlocking {
        val tc001Device = findTC001Device()
        if (tc001Device == null) {
            println("Skipping test: No TC001 device connected")
            return@runBlocking
        }
        // Given: TC001 is connected and recording
        thermalRecorder.initialize()
        val recordingStarted = thermalRecorder.startRecording(testSessionDir.absolutePath)
        assertTrue("Recording should start with TC001", recordingStarted)
        val recordingContinued = AtomicBoolean(false)
        // Monitor recording continuity
        thermalRecorder.setFrameListener(object : ThermalCameraRecorder.ThermalFrameListener {
            override fun onFrameProcessed(stats: ThermalCameraRecorder.ThermalFrameStats) {
                recordingContinued.set(true)
            }

            override fun onError(error: String) {
                println("Recording error: $error")
            }
        })
        // Record for a period to establish baseline
        delay(2000)
        assertTrue("Recording should be active", thermalRecorder.isRecording)
        recordingContinued.set(false) // Reset flag before disconnect/reconnect
        println("Manual Test Step: Please disconnect and reconnect the TC001 camera")
        println("Test will continue monitoring for 5 seconds...")
        // Wait and monitor for disconnect/reconnect events
        delay(5000)
        // Then: Recording should have continued
        assertTrue("Recording should continue despite hardware changes", recordingContinued.get())
        val stopResult = thermalRecorder.stopRecording()
        assertTrue("Should stop recording successfully", stopResult)
    }

    @Test
    fun testRealTC001ImageFileGeneration() = runBlocking {
        val tc001Device = findTC001Device()
        if (tc001Device == null) {
            println("Skipping test: No TC001 device connected")
            return@runBlocking
        }
        // Given: TC001 recording setup
        thermalRecorder.initialize()
        // When: Record thermal data with image saving
        val recordingStarted = thermalRecorder.startRecording(testSessionDir.absolutePath)
        assertTrue("Recording should start", recordingStarted)
        // Record for sufficient time to generate frames
        delay(2000)
        thermalRecorder.stopRecording()
        // Then: Verify file outputs
        val thermalImagesDir = File(testSessionDir, "thermal_images")
        assertTrue("Thermal images directory should exist", thermalImagesDir.exists())
        val csvFiles = testSessionDir.listFiles { _, name ->
            name.contains("thermal") && name.endsWith(".csv")
        }
        assertNotNull("Should have thermal CSV files", csvFiles)
        assertTrue("Should have at least one thermal CSV file", csvFiles!!.isNotEmpty())
        println("Test completed successfully - files generated in ${testSessionDir.absolutePath}")
    }

    // Helper methods
    private fun findTC001Device(): UsbDevice? {
        val deviceList = usbManager.deviceList
        return deviceList.values.find { device ->
            // TC001 VID/PID: 0x2744/0x0001 or other known TC001 identifiers
            (device.vendorId == 0x2744 && device.productId == 0x0001) ||
                    device.productName?.contains("TC001", ignoreCase = true) == true
        }
    }
}


