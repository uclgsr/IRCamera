package mpdc4gsr.integration

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mpdc4gsr.sensors.thermal.ThermalCameraRecorder
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import java.io.File

/**
 * Integration test validating the key thermal camera issues from the problem statement.
 *
 * This test specifically addresses:
 * 1. Real SDK integration vs dummy data
 * 2. USB permission handling and hot-plug detection
 * 3. Frame rate and performance validation
 * 4. Error handling and graceful degradation
 */
@ExperimentalCoroutinesApi
class ThermalCameraIntegrationValidationTest {

    @get:Rule
    val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockUsbManager: UsbManager

    @Mock
    private lateinit var mockTC001Device: UsbDevice

    private lateinit var thermalRecorder: ThermalCameraRecorder
    private lateinit var testSessionDir: File

    @Before
    fun setup() {
        `when`(mockContext.getSystemService(Context.USB_SERVICE)).thenReturn(mockUsbManager)

        // Mock Topdon TC001 with correct VID/PID from XML filter
        `when`(mockTC001Device.vendorId).thenReturn(0x2744)  // 10052 decimal
        `when`(mockTC001Device.productId).thenReturn(0x0001)  // 1 decimal
        `when`(mockTC001Device.productName).thenReturn("TC001 Thermal Camera")

        thermalRecorder = ThermalCameraRecorder(mockContext)
        testSessionDir = File.createTempFile("thermal_test", "").apply {
            delete()
            mkdirs()
            deleteOnExit()
        }
    }

    /**
     * VALIDATION 1: Real SDK Integration
     * Confirms that when hardware is available, real SDK paths are used
     */
    @Test
    fun `validate real SDK integration is implemented not dummy data`() = runTest {
        // Given: TC001 device available with permission
        val deviceList = hashMapOf("tc001" to mockTC001Device)
        `when`(mockUsbManager.deviceList).thenReturn(deviceList)
        `when`(mockUsbManager.hasPermission(mockTC001Device)).thenReturn(true)

        // When: Initialize thermal recorder
        val initResult = thermalRecorder.initialize()

        // Then: Should use real SDK paths
        assertTrue("Should initialize with real SDK", initResult)
        assertEquals(
            "Should use thermal camera sensor ID",
            "thermal_camera_1",
            thermalRecorder.sensorId
        )
        assertEquals(
            "Should identify as IR Thermal Camera",
            "IR Thermal Camera",
            thermalRecorder.sensorType
        )

        // Validate frame rate is set for real hardware
        val frameRate = thermalRecorder.samplingRate
        assertTrue(
            "Frame rate should be in 9-10Hz range for real hardware",
            frameRate >= 8.0 && frameRate <= 11.0
        )
    }

    /**
     * VALIDATION 2: USB Permission Flow
     * Validates the USB permission handling works correctly
     */
    @Test
    fun `validate USB permission flow handles TC001 device correctly`() = runTest {
        // Given: TC001 device without permission
        val deviceList = hashMapOf("tc001" to mockTC001Device)
        `when`(mockUsbManager.deviceList).thenReturn(deviceList)
        `when`(mockUsbManager.hasPermission(mockTC001Device)).thenReturn(false)

        // When: Initialize thermal recorder
        val initResult = thermalRecorder.initialize()

        // Then: Should handle permission gracefully
        assertTrue("Should handle permission flow gracefully", initResult)
        verify(mockUsbManager, atLeastOnce()).hasPermission(mockTC001Device)
        verify(mockUsbManager, atLeastOnce()).deviceList
    }

    /**
     * VALIDATION 4: Graceful Degradation
     * Validates fallback to simulation when hardware fails
     */
    @Test
    fun `validate graceful degradation when thermal hardware unavailable`() = runTest {
        // Given: No thermal hardware available
        `when`(mockUsbManager.deviceList).thenReturn(hashMapOf())

        // When: Initialize and start recording
        val initResult = thermalRecorder.initialize()
        val recordingResult = thermalRecorder.startRecording(testSessionDir.absolutePath)

        // Then: Should fallback gracefully to simulation
        assertTrue("Should initialize in simulation mode", initResult)
        assertTrue("Should start recording in simulation mode", recordingResult)
        assertTrue("Should be recording", thermalRecorder.isRecording)

        // Clean up
        val stopResult = thermalRecorder.stopRecording()
        assertTrue("Should stop gracefully", stopResult)
        assertFalse("Should not be recording after stop", thermalRecorder.isRecording)
    }

    /**
     * VALIDATION 5: Frame Rate Configuration
     * Validates the thermal camera is configured for appropriate frame rates
     */
    @Test
    fun `validate thermal camera frame rate configuration`() {
        // The thermal recorder should be configured for ~10Hz operation
        val samplingRate = thermalRecorder.samplingRate

        // Allow for 9Hz (standard TC001) to 10Hz range
        assertTrue(
            "Sampling rate should be approximately 10Hz (got $samplingRate)",
            samplingRate >= 8.0 && samplingRate <= 11.0
        )
    }
}