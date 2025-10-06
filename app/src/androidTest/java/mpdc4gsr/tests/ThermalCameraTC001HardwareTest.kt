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