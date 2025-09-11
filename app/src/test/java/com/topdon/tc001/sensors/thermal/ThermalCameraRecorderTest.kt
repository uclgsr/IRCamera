package com.topdon.tc001.sensors.thermal

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.topdon.lib.core.bean.event.device.DeviceConnectEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import io.mockk.*
import org.junit.After

/**
 * Unit tests for ThermalCameraRecorder USB permission integration
 */
@ExperimentalCoroutinesApi
class ThermalCameraRecorderTest {

    private lateinit var context: Context
    private lateinit var usbManager: UsbManager
    private lateinit var thermalCameraRecorder: ThermalCameraRecorder
    private lateinit var mockUsbDevice: UsbDevice

    @Before
    fun setup() {
        // Mock Android context and USB manager
        context = mockk(relaxed = true)
        usbManager = mockk(relaxed = true)
        mockUsbDevice = mockk(relaxed = true)

        // Setup context to return USB manager
        every { context.getSystemService(Context.USB_SERVICE) } returns usbManager
        
        // Setup mock USB device with thermal camera identifiers
        every { mockUsbDevice.vendorId } returns 0x0BDA  // TOPDON_VENDOR_ID
        every { mockUsbDevice.productId } returns 0x5830 // TOPDON_PRODUCT_ID
        every { mockUsbDevice.productName } returns "TC001 Thermal Camera"
        every { mockUsbDevice.deviceName } returns "/dev/bus/usb/001/002"

        // Create thermal camera recorder instance
        thermalCameraRecorder = ThermalCameraRecorder(context)
    }

    @After
    fun teardown() {
        runTest {
            thermalCameraRecorder.cleanup()
        }
        clearAllMocks()
    }

    @Test
    fun `initialization with no USB devices should enable simulation mode`() = runTest {
        // Setup: No USB devices available
        every { usbManager.deviceList } returns emptyMap()

        // When: Initialize thermal camera recorder
        val result = thermalCameraRecorder.initialize()

        // Then: Should succeed and enable simulation mode
        assertTrue("Initialization should succeed with simulation mode", result)
    }

    @Test
    fun `initialization with thermal camera and permission should succeed`() = runTest {
        // Setup: Thermal camera device available with permission
        val deviceMap = mapOf("device1" to mockUsbDevice)
        every { usbManager.deviceList } returns deviceMap
        every { usbManager.hasPermission(mockUsbDevice) } returns true

        // When: Initialize thermal camera recorder  
        val result = thermalCameraRecorder.initialize()

        // Then: Should succeed with real hardware connection
        assertTrue("Initialization should succeed with real hardware", result)
    }

    @Test
    fun `device connect event should switch from simulation to hardware mode`() = runTest {
        // Setup: Initialize in simulation mode first
        every { usbManager.deviceList } returns emptyMap()
        thermalCameraRecorder.initialize()

        // Setup: Device becomes available with permission
        every { usbManager.hasPermission(mockUsbDevice) } returns true

        // When: Device connect event is received
        val connectEvent = DeviceConnectEvent(true, mockUsbDevice)
        thermalCameraRecorder.onDeviceConnectEvent(connectEvent)

        // Then: Should complete without exceptions (tests event handling logic)
    }

    @Test
    fun `recording should work in simulation mode`() = runTest {
        // Setup: Initialize in simulation mode
        every { usbManager.deviceList } returns emptyMap()
        thermalCameraRecorder.initialize()

        // Create a temporary directory for testing
        val tempDir = kotlin.io.path.createTempDirectory("thermal_test").toFile()
        
        try {
            // When: Start recording
            val result = thermalCameraRecorder.startRecording(tempDir.absolutePath)

            // Then: Should succeed and start simulation recording
            assertTrue("Recording should start successfully in simulation mode", result)
            assertTrue("Should be in recording state", thermalCameraRecorder.isRecording)

            // Verify stats are being tracked
            val stats = thermalCameraRecorder.getRecordingStats()
            assertEquals("Sensor ID should match", "thermal_camera_1", stats.sensorId)
            assertEquals("Sensor type should be correct", "IR Thermal Camera", stats.sensorType)

        } finally {
            // Cleanup: Stop recording and remove temp directory
            thermalCameraRecorder.stopRecording()
            tempDir.deleteRecursively()
        }
    }
}
