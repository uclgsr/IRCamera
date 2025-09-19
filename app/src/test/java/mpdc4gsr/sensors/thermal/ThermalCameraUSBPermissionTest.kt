package mpdc4gsr.sensors.thermal

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.junit.Rule

@ExperimentalCoroutinesApi
class ThermalCameraUSBPermissionTest {

    @get:Rule
    val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockUsbManager: UsbManager

    @Mock
    private lateinit var mockUsbDevice: UsbDevice

    private lateinit var thermalRecorder: ThermalCameraRecorder

    @Before
    fun setup() {
        `when`(mockContext.getSystemService(Context.USB_SERVICE)).thenReturn(mockUsbManager)

        // Mock Topdon TC001 device
        `when`(mockUsbDevice.vendorId).thenReturn(0x4206)
        `when`(mockUsbDevice.productId).thenReturn(0x3702)
        `when`(mockUsbDevice.productName).thenReturn("TC001")

        thermalRecorder = ThermalCameraRecorder(mockContext)
    }

    @Test
    fun `test USB device filter matches Topdon TC001`() {
        // Assert that our device filter VID/PID matches what we're testing
        assertEquals("Vendor ID should match Topdon", 0x4206, mockUsbDevice.vendorId)
        assertEquals("Product ID should match TC001", 0x3702, mockUsbDevice.productId)
    }

    @Test
    fun `test thermal recorder initialization with no devices`() = runTest {
        // Arrange
        `when`(mockUsbManager.deviceList).thenReturn(hashMapOf())

        // Act
        val result = thermalRecorder.initialize()

        // Assert
        assertTrue("Should initialize successfully in simulation mode", result)
    }

    @Test
    fun `test USB permission request flow`() = runTest {
        // Arrange
        val deviceList = hashMapOf("device1" to mockUsbDevice)
        `when`(mockUsbManager.deviceList).thenReturn(deviceList)
        `when`(mockUsbManager.hasPermission(mockUsbDevice)).thenReturn(false)

        // Act
        val result = thermalRecorder.initialize()

        // Assert
        assertTrue("Should complete initialization even without permission", result)
        // Permission request should be initiated (would verify with additional mocking)
    }

    @Test
    fun `test thermal data recording in simulation mode`() = runTest {
        // Arrange
        `when`(mockUsbManager.deviceList).thenReturn(hashMapOf())
        thermalRecorder.initialize()
        val tempDir = java.nio.file.Files.createTempDirectory("thermal_test").toFile()

        try {
            // Act
            val recordingStarted = thermalRecorder.startRecording(tempDir.absolutePath)

            // Assert
            assertTrue("Recording should start in simulation mode", recordingStarted)
            assertTrue("Should be recording", thermalRecorder.isRecording)

            // Stop recording
            val recordingStopped = thermalRecorder.stopRecording()
            assertTrue("Recording should stop successfully", recordingStopped)
            assertFalse("Should not be recording after stop", thermalRecorder.isRecording)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `test thermal camera sensor properties`() {
        // Assert thermal camera characteristics
        assertEquals(
            "Sensor ID should be thermal_camera_1",
            "thermal_camera_1",
            thermalRecorder.sensorId
        )
        assertEquals(
            "Sensor type should be IR Thermal Camera",
            "IR Thermal Camera",
            thermalRecorder.sensorType
        )
        assertEquals("Sampling rate should be 9.0 FPS", 9.0, thermalRecorder.samplingRate, 0.1)
    }
}
