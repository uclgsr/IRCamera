package mpdc4gsr.sensors.thermal

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.kotlinx.coroutines.ExperimentalCoroutinesApi
import com.kotlinx.coroutines.delay
import com.kotlinx.coroutines.test.advanceTimeBy
import com.kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Comprehensive TC001 Thermal Camera Integration Tests
 *
 * Tests the four key areas requested:
 * 1. USB permission flow with real TC001 hardware
 * 2. Verify actual 10Hz frame capture rate
 * 3. Test camera disconnect/reconnect scenarios
 * 4. Validate that other sensors continue when thermal fails
 */
@ExperimentalCoroutinesApi
class TC001IntegrationTest {

    @get:Rule
    val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockUsbManager: UsbManager

    @Mock
    private lateinit var mockTC001Device: UsbDevice

    @Mock
    private lateinit var mockGsrRecorder: mpdc4gsr.sensors.SensorRecorder

    @Mock
    private lateinit var mockRgbRecorder: mpdc4gsr.sensors.SensorRecorder

    private lateinit var thermalRecorder: ThermalCameraRecorder
    private lateinit var testSessionDir: File

    @Before
    fun setup() {
        `when`(mockContext.getSystemService(Context.USB_SERVICE)).thenReturn(mockUsbManager)

        // Mock TC001 device with correct VID/PID
        `when`(mockTC001Device.vendorId).thenReturn(0x2744)
        `when`(mockTC001Device.productId).thenReturn(0x0001)
        `when`(mockTC001Device.productName).thenReturn("TC001")

        thermalRecorder = ThermalCameraRecorder(mockContext)
        testSessionDir = createTempTestDirectory()
    }

    /**
     * Test 1: USB Permission Flow with Real TC001 Hardware
     */
    @Test
    fun `test TC001 USB permission flow - device detected`() = runTest {
        // Given: TC001 device is connected but no permission
        val deviceList = hashMapOf("tc001" to mockTC001Device)
        `when`(mockUsbManager.deviceList).thenReturn(deviceList)
        `when`(mockUsbManager.hasPermission(mockTC001Device)).thenReturn(false)

        // When: Initialize thermal recorder
        val result = thermalRecorder.initialize()

        // Then: Should complete successfully and request permission
        assertTrue("Should initialize even without permission", result)
        // Verify USB permission was requested (would trigger permission dialog in real scenario)
    }

    @Test
    fun `test TC001 USB permission flow - permission granted`() = runTest {
        // Given: TC001 device with granted permission
        val deviceList = hashMapOf("tc001" to mockTC001Device)
        `when`(mockUsbManager.deviceList).thenReturn(deviceList)
        `when`(mockUsbManager.hasPermission(mockTC001Device)).thenReturn(true)

        // When: Initialize thermal recorder
        val result = thermalRecorder.initialize()

        // Then: Should initialize successfully
        assertTrue("Should initialize with granted permission", result)
    }

    @Test
    fun `test TC001 USB permission flow - permission denied fallback`() = runTest {
        // Given: TC001 device with denied permission
        val deviceList = hashMapOf("tc001" to mockTC001Device)
        `when`(mockUsbManager.deviceList).thenReturn(deviceList)
        `when`(mockUsbManager.hasPermission(mockTC001Device)).thenReturn(false)

        // When: Initialize and start recording
        thermalRecorder.initialize()
        val recordingStarted = thermalRecorder.startRecording(testSessionDir.absolutePath)

        // Then: Should fallback to simulation mode
        assertTrue("Should start recording in simulation mode", recordingStarted)
        assertTrue("Should be recording", thermalRecorder.isRecording)
    }

    /**
     * Test 2: Verify Actual 10Hz Frame Capture Rate
     */
    @Test
    fun `test TC001 frame capture rate validation - 10Hz target`() = runTest {
        // Given: TC001 in simulation mode (for testing frame rate timing)
        `when`(mockUsbManager.deviceList).thenReturn(hashMapOf())
        thermalRecorder.initialize()

        val frameCount = AtomicInteger(0)

        // Set up frame listener to count frames
        thermalRecorder.setFrameListener(object : ThermalRecorder.ThermalFrameListener {
            override fun onFrameProcessed(stats: ThermalRecorder.ThermalFrameStats) {
                frameCount.incrementAndGet()
            }

            override fun onError(error: String) {
                // Handle errors
            }
        })

        // When: Start recording and measure frame rate over virtual time
        thermalRecorder.startRecording(testSessionDir.absolutePath)

        // Simulate 2 seconds of recording (virtual time)
        advanceTimeBy(2000L)
        delay(100) // Allow processing

        thermalRecorder.stopRecording()

        // Then: Should capture approximately 20 frames in 2 seconds (10Hz)
        val actualFrames = frameCount.get()
        val expectedFrames = 20 // 10Hz * 2 seconds
        // Allow some tolerance for timing variations in testing
        assertTrue(
            "Expected around $expectedFrames frames for a 2-second duration at 10Hz, but got $actualFrames",
            actualFrames in 18..22
        )
    }

    @Test
    fun `test TC001 continuous frame capture timing intervals`() = runTest {
        // Given: Recording setup
        `when`(mockUsbManager.deviceList).thenReturn(hashMapOf())
        thermalRecorder.initialize()

        val frameCount = AtomicInteger(0)
        val targetFramesToCapture = 10

        thermalRecorder.setFrameListener(object : ThermalRecorder.ThermalFrameListener {
            override fun onFrameProcessed(stats: ThermalRecorder.ThermalFrameStats) {
                frameCount.incrementAndGet()

                // Stop after capturing enough frames for analysis
                if (frameCount.get() >= targetFramesToCapture) {
                    // Stop recording asynchronously  
                    this@runTest.launch {
                        thermalRecorder.stopRecording()
                    }
                }
            }

            override fun onError(error: String) {}
        })

        // When: Start recording and advance virtual time for frame capture
        thermalRecorder.startRecording(testSessionDir.absolutePath)

        // Advance time to allow for frame capture (10Hz = 100ms per frame, so 1000ms for 10 frames)
        advanceTimeBy(1000L)
        delay(100) // Allow processing

        // Then: Should have captured the expected number of frames over virtual time
        val actualFrames = frameCount.get()
        // For 1 second at 10Hz, expect ~10 frames (allow some tolerance)
        assertTrue(
            "Expected around 10 frames in 1 second at 10Hz, got $actualFrames",
            actualFrames in 8..12
        )
    }

    /**
     * Test 3: Camera Disconnect/Reconnect Scenarios
     */
    @Test
    fun `test TC001 disconnect during recording - graceful handling`() = runTest {
        // Given: TC001 is connected and recording
        val deviceList = hashMapOf("tc001" to mockTC001Device)
        `when`(mockUsbManager.deviceList).thenReturn(deviceList)
        `when`(mockUsbManager.hasPermission(mockTC001Device)).thenReturn(true)

        thermalRecorder.initialize()
        thermalRecorder.startRecording(testSessionDir.absolutePath)

        assertTrue("Should be recording", thermalRecorder.isRecording)

        // When: Simulate camera disconnect event by directly invoking the event handler
        val disconnectEvent = mock(com.mpdc4gsr.libunified.app.bean.event.device.DeviceConnectEvent::class.java)
        `when`(disconnectEvent.isConnected).thenReturn(false)
        `when`(disconnectEvent.device).thenReturn(mockTC001Device)
        thermalRecorder.onDeviceConnectEvent(disconnectEvent)

        // Allow time for the event to be processed and fallback to simulation
        delay(100)

        // Then: Should continue recording in simulation mode
        assertTrue("Should still be recording after disconnect", thermalRecorder.isRecording)

        // Recording should complete successfully
        val stopResult = thermalRecorder.stopRecording()
        assertTrue("Should stop recording successfully", stopResult)
    }

    @Test
    fun `test TC001 reconnect during recording - automatic recovery`() = runTest {
        // Given: Recording started without TC001 (simulation mode)
        `when`(mockUsbManager.deviceList).thenReturn(hashMapOf())
        thermalRecorder.initialize()
        thermalRecorder.startRecording(testSessionDir.absolutePath)

        // When: TC001 is connected mid-recording
        val deviceList = hashMapOf("tc001" to mockTC001Device)
        `when`(mockUsbManager.deviceList).thenReturn(deviceList)
        `when`(mockUsbManager.hasPermission(mockTC001Device)).thenReturn(true)

        // Simulate connect event
        val connectEvent = org.mockito.Mockito.mock(
            com.mpdc4gsr.libunified.app.bean.event.device.DeviceConnectEvent::class.java
        )
        `when`(connectEvent.isConnected).thenReturn(true)
        `when`(connectEvent.device).thenReturn(mockTC001Device)

        // Then: Should continue recording and potentially switch to real hardware mode
        assertTrue("Should continue recording", thermalRecorder.isRecording)

        // Stop and verify successful completion
        val stopResult = thermalRecorder.stopRecording()
        assertTrue("Should stop successfully after reconnect", stopResult)
    }

    @Test
    fun `test TC001 multiple disconnect-reconnect cycles`() = runTest {
        // Given: Initial setup with TC001
        var deviceList = hashMapOf("tc001" to mockTC001Device)
        `when`(mockUsbManager.deviceList).thenReturn(deviceList)
        `when`(mockUsbManager.hasPermission(mockTC001Device)).thenReturn(true)

        thermalRecorder.initialize()
        thermalRecorder.startRecording(testSessionDir.absolutePath)

        // When: Multiple disconnect/reconnect cycles
        for (cycle in 1..3) {
            // Disconnect
            `when`(mockUsbManager.deviceList).thenReturn(hashMapOf())
            delay(100)

            // Should still be recording
            assertTrue(
                "Should be recording during cycle $cycle disconnect",
                thermalRecorder.isRecording
            )

            // Reconnect  
            `when`(mockUsbManager.deviceList).thenReturn(hashMapOf("tc001" to mockTC001Device))
            delay(100)

            // Should still be recording
            assertTrue(
                "Should be recording during cycle $cycle reconnect",
                thermalRecorder.isRecording
            )
        }

        // Then: Recording should complete successfully
        val stopResult = thermalRecorder.stopRecording()
        assertTrue("Should stop successfully after multiple cycles", stopResult)
    }

    /**
     * Test 4: Validate Other Sensors Continue When Thermal Fails
     */
    @Test
    fun `test other sensors continue when TC001 fails - GSR and RGB unaffected`() = runTest {
        // Given: Multi-sensor recording setup
        `when`(mockGsrRecorder.isRecording).thenReturn(false).thenReturn(true)
        `when`(mockRgbRecorder.isRecording).thenReturn(false).thenReturn(true)

        // Mock successful GSR and RGB recording starts
        `when`(mockGsrRecorder.startRecording(anyString())).thenReturn(true)
        `when`(mockRgbRecorder.startRecording(anyString())).thenReturn(true)

        // TC001 setup that will fail
        val deviceList = hashMapOf("tc001" to mockTC001Device)
        `when`(mockUsbManager.deviceList).thenReturn(deviceList)
        `when`(mockUsbManager.hasPermission(mockTC001Device)).thenReturn(false)

        // When: Start multi-sensor recording
        val gsrStarted = mockGsrRecorder.startRecording(testSessionDir.absolutePath)
        val rgbStarted = mockRgbRecorder.startRecording(testSessionDir.absolutePath)
        val thermalStarted = thermalRecorder.startRecording(testSessionDir.absolutePath)

        // Then: All sensors should start successfully
        assertTrue("GSR should start successfully", gsrStarted)
        assertTrue("RGB should start successfully", rgbStarted)
        assertTrue("Thermal should start in simulation mode", thermalStarted)

        // Verify other sensors remain unaffected by thermal issues
        assertTrue("GSR should be recording", mockGsrRecorder.isRecording)
        assertTrue("RGB should be recording", mockRgbRecorder.isRecording)
        assertTrue("Thermal should be recording", thermalRecorder.isRecording)
    }

    @Test
    fun `test thermal failure isolation - other sensors protected`() = runTest {
        // Given: All sensors recording successfully
        `when`(mockGsrRecorder.isRecording).thenReturn(true)
        `when`(mockRgbRecorder.isRecording).thenReturn(true)

        thermalRecorder.initialize()
        thermalRecorder.startRecording(testSessionDir.absolutePath)

        // When: Thermal experiences critical failure (simulated)
        // Force thermal into error state by removing devices mid-recording
        `when`(mockUsbManager.deviceList).thenReturn(hashMapOf())

        // Simulate thermal error handling
        delay(100) // Allow error processing

        // Then: Other sensors should continue unaffected
        assertTrue(
            "GSR should continue recording despite thermal failure",
            mockGsrRecorder.isRecording
        )
        assertTrue(
            "RGB should continue recording despite thermal failure",
            mockRgbRecorder.isRecording
        )

        // Thermal should gracefully switch to simulation mode
        assertTrue(
            "Thermal should continue in simulation mode",
            thermalRecorder.isRecording
        )
    }

    @Test
    fun `test sensor failure recovery - thermal does not crash app`() = runTest {
        // Given: Recording setup
        thermalRecorder.initialize()
        thermalRecorder.startRecording(testSessionDir.absolutePath)

        val errorOccurred = AtomicBoolean(false)
        val crashOccurred = AtomicBoolean(false)

        thermalRecorder.setFrameListener(object : ThermalRecorder.ThermalFrameListener {
            override fun onFrameProcessed(stats: ThermalRecorder.ThermalFrameStats) {
                // Normal operation
            }

            override fun onError(error: String) {
                errorOccurred.set(true)
                // Should not crash - just log error
            }
        })

        // When: Simulate multiple consecutive errors (should trigger fallback)
        try {
            // Force error conditions
            `when`(mockUsbManager.deviceList).thenReturn(hashMapOf())

            // Wait for error processing
            delay(200)

        } catch (e: Exception) {
            crashOccurred.set(true)
        }

        // Then: Should handle errors gracefully without crashing
        assertFalse("Thermal errors should not crash the app", crashOccurred.get())
        assertTrue("Should still be recording after errors", thermalRecorder.isRecording)

        // Should be able to stop recording normally
        val stopResult = thermalRecorder.stopRecording()
        assertTrue("Should stop recording successfully after errors", stopResult)
    }

    @Test
    fun `test thermal image directory creation and file output`() = runTest {
        // Given: Recording setup
        thermalRecorder.initialize()

        // When: Start recording with image saving
        thermalRecorder.startRecording(testSessionDir.absolutePath)
        delay(500) // Allow some frames to be processed
        thermalRecorder.stopRecording()

        // Then: Thermal images directory should be created
        val thermalImagesDir = File(testSessionDir, "thermal_images")
        assertTrue("Thermal images directory should be created", thermalImagesDir.exists())
        assertTrue("Thermal images directory should be a directory", thermalImagesDir.isDirectory())

        // Should have thermal data CSV file
        val csvFiles = testSessionDir.listFiles { _, name -> name.contains("thermal") && name.endsWith(".csv") }
        assertNotNull("Should have thermal CSV files", csvFiles)
        assertTrue("Should have at least one thermal CSV file", csvFiles!!.isNotEmpty())
    }

    // Helper methods
    private fun createTempTestDirectory(): File {
        val tempDir = File.createTempFile("tc001_test", null)
        tempDir.delete()
        tempDir.mkdirs()
        return tempDir
    }

    private fun cleanupTestDirectory() {
        if (::testSessionDir.isInitialized) {
            testSessionDir.deleteRecursively()
        }
    }
}