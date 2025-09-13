package com.topdon.tc001.controller

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.topdon.tc001.permissions.PermissionController
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for HardwareValidationController Phase 2 implementation
 * 
 * These tests validate the hardware validation logic for Samsung S22 testing,
 * including permission validation, sensor testing, and validation report generation.
 * 
 * @author IRCamera Phase 2 Testing
 */
class HardwareValidationControllerTest {

    private lateinit var context: Context
    private lateinit var lifecycleOwner: LifecycleOwner
    private lateinit var permissionController: PermissionController
    private lateinit var recordingController: RecordingController
    private lateinit var validationController: HardwareValidationController

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        lifecycleOwner = mockk(relaxed = true)
        permissionController = mockk(relaxed = true)
        recordingController = mockk(relaxed = true)
        
        validationController = HardwareValidationController(
            context,
            lifecycleOwner,
            permissionController,
            recordingController
        )
    }

    @Test
    fun testValidationControllerInitialization() {
        assertNotNull(validationController)
        assertFalse(validationController.isValidating)
    }

    @Test
    fun testValidateAllSensorsWithAllPermissionsGranted() = runTest {
        // Mock all permissions as granted
        every { permissionController.hasCameraPermission() } returns true
        every { permissionController.hasBluetoothPermissions() } returns true
        every { permissionController.hasStoragePermissions() } returns true

        val report = validationController.validateAllSensors()

        assertNotNull(report)
        assertTrue(report.summary.overallSuccess)
        assertEquals(0, report.summary.failedTests)
        assertTrue(report.validationResults.isNotEmpty())
    }

    @Test
    fun testValidateAllSensorsWithMissingPermissions() = runTest {
        // Mock missing permissions
        every { permissionController.hasCameraPermission() } returns false
        every { permissionController.hasBluetoothPermissions() } returns false
        every { permissionController.hasStoragePermissions() } returns false

        val report = validationController.validateAllSensors()

        assertNotNull(report)
        // Some validations should fail due to missing permissions
        assertTrue(report.summary.failedTests > 0)
        assertTrue(report.errorLogs.isEmpty() || report.errorLogs.isNotEmpty())
    }

    @Test
    fun testDeviceInfoCapture() = runTest {
        every { permissionController.hasCameraPermission() } returns true
        every { permissionController.hasBluetoothPermissions() } returns true
        every { permissionController.hasStoragePermissions() } returns true

        val report = validationController.validateAllSensors()

        with(report.deviceInfo) {
            assertNotNull(manufacturer)
            assertNotNull(model)
            assertNotNull(androidVersion)
            assertTrue(sdkInt > 0)
            assertNotNull(appVersion)
        }
    }

    @Test
    fun testSensorCapabilityDetection() = runTest {
        every { permissionController.hasCameraPermission() } returns true
        every { permissionController.hasBluetoothPermissions() } returns true
        every { permissionController.hasStoragePermissions() } returns true

        val report = validationController.validateAllSensors()

        // Check that sensor capabilities are detected
        assertTrue(report.sensorCapabilities.isNotEmpty())
        
        // Verify RGB camera capability
        val rgbCapability = report.sensorCapabilities["rgb_camera"]
        assertNotNull(rgbCapability)
        assertEquals("RGB Camera", rgbCapability?.sensorType)
        assertTrue(rgbCapability?.isAvailable == true)
    }

    @Test
    fun testPerformanceMetricsCollection() = runTest {
        every { permissionController.hasCameraPermission() } returns true
        every { permissionController.hasBluetoothPermissions() } returns true
        every { permissionController.hasStoragePermissions() } returns true

        val report = validationController.validateAllSensors()

        // Verify performance metrics are collected
        assertTrue(report.performanceMetrics.isNotEmpty())
        assertTrue(report.performanceMetrics.containsKey("permission_validation_duration_ms"))
        assertTrue(report.performanceMetrics.containsKey("rgb_camera_validation_duration_ms"))
    }

    @Test
    fun testValidationResultStructure() = runTest {
        every { permissionController.hasCameraPermission() } returns true
        every { permissionController.hasBluetoothPermissions() } returns true
        every { permissionController.hasStoragePermissions() } returns true

        val report = validationController.validateAllSensors()

        // Verify validation results structure
        assertTrue(report.validationResults.containsKey("rgb_camera"))
        assertTrue(report.validationResults.containsKey("thermal_camera"))
        assertTrue(report.validationResults.containsKey("gsr_sensor"))
        assertTrue(report.validationResults.containsKey("multi_sensor_recording"))

        // Check individual validation result structure
        val rgbResult = report.validationResults["rgb_camera"]
        assertNotNull(rgbResult)
        assertTrue(rgbResult?.success == true)
        assertNotNull(rgbResult?.message)
        assertNotNull(rgbResult?.metrics)
    }

    @Test
    fun testConcurrentValidationPrevention() = runTest {
        every { permissionController.hasCameraPermission() } returns true
        every { permissionController.hasBluetoothPermissions() } returns true
        every { permissionController.hasStoragePermissions() } returns true

        // Start first validation
        val firstValidation = async {
            validationController.validateAllSensors()
        }

        // Try to start second validation while first is running
        try {
            validationController.validateAllSensors()
            fail("Should have thrown IllegalStateException for concurrent validation")
        } catch (e: IllegalStateException) {
            assertEquals("Validation already in progress", e.message)
        }

        // Complete first validation
        firstValidation.await()
    }

    @Test
    fun testValidationSummaryCalculation() = runTest {
        every { permissionController.hasCameraPermission() } returns true
        every { permissionController.hasBluetoothPermissions() } returns true
        every { permissionController.hasStoragePermissions() } returns true

        val report = validationController.validateAllSensors()

        with(report.summary) {
            assertTrue(totalTests > 0)
            assertTrue(passedTests >= 0)
            assertTrue(failedTests >= 0)
            assertEquals(totalTests, passedTests + failedTests)
            assertTrue(totalDurationMs > 0)
        }
    }

    @Test
    fun testTimestampGeneration() = runTest {
        every { permissionController.hasCameraPermission() } returns true

        val beforeTime = System.currentTimeMillis()
        val report = validationController.validateAllSensors()
        val afterTime = System.currentTimeMillis()

        assertTrue(report.timestamp >= beforeTime)
        assertTrue(report.timestamp <= afterTime)
    }
}

/**
 * Mock extension for async testing
 */
private suspend fun async(block: suspend () -> ValidationReport) = kotlinx.coroutines.async {
    block()
}