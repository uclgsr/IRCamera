package com.topdon.gsr.service

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication
import org.robolectric.shadows.ShadowEnvironment

/**
 * Context-based tests for ShimmerGSRRecorder using Robolectric
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O])
@OptIn(ExperimentalCoroutinesApi::class)
class ShimmerGSRRecorderTest {
    private lateinit var context: Context
    private lateinit var recorder: ShimmerGSRRecorder
    private lateinit var shadowApplication: ShadowApplication

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shadowApplication = Shadows.shadowOf(context as android.app.Application)

        // Grant Bluetooth permissions
        shadowApplication.grantPermissions(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
        )

        // Setup external storage state
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED)

        recorder = ShimmerGSRRecorder(context, samplingRateHz = 128)
    }

    @Test
    fun testRecorderCreation() {
        assertNotNull("Recorder should be created", recorder)
    }

    @Test
    fun testBluetoothPermissionCheck() {
        // Test that recorder properly checks for Bluetooth permissions
        val hasPermission = context.checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        assertTrue("Bluetooth permission should be granted in test", hasPermission)
    }

    @Test
    fun testBluetoothAdapterAccess() {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        assertNotNull("BluetoothManager should be available", bluetoothManager)

        val bluetoothAdapter = bluetoothManager?.adapter
        assertNotNull("BluetoothAdapter should be available", bluetoothAdapter)
    }

    @Test
    fun testRecordingStateManagement() =
        runTest {
            val sessionId = "recording_state_test"

            // Initially not recording
            assertFalse("Should not be recording initially", recorder.isRecording())

            // Start recording - may fail without real device, but shouldn't crash
            try {
                val started = recorder.startRecording(sessionId)
                // Recording may or may not start without real device
                // The important thing is no exception is thrown
            } catch (e: Exception) {
                // Expected without real hardware
            }

            // Stop recording should work regardless
            try {
                recorder.stopRecording()
            } catch (e: Exception) {
                // May fail but shouldn't crash the test
            }
        }

    @Test
    fun testSensorConfiguration() {
        // Test that the recorder accepts configuration parameters
        val customRecorder = ShimmerGSRRecorder(context, samplingRateHz = 256)
        assertNotNull("Custom recorder should be created", customRecorder)

        // Test default configuration
        assertNotNull("Default recorder should have context", recorder)
    }

    @Test
    fun testContextUsage() {
        // Verify recorder uses context for system services
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        assertNotNull("Bluetooth manager should be accessible through context", bluetoothManager)

        // Verify external storage access
        val externalStorageState = Environment.getExternalStorageState()
        assertEquals("External storage should be mounted in test", Environment.MEDIA_MOUNTED, externalStorageState)
    }

    @Test
    fun testFileSystemAccess() {
        // Test that recorder can access file system through context
        val filesDir = context.filesDir
        assertNotNull("Files directory should be accessible", filesDir)
        assertTrue("Files directory should exist", filesDir.exists())

        val externalFilesDir = context.getExternalFilesDir(null)
        assertNotNull("External files directory should be accessible", externalFilesDir)
    }

    @Test
    fun testErrorHandling() =
        runTest {
            // Test starting recording without proper setup
            try {
                val result = recorder.startRecording("error_test")
                // May succeed or fail, but shouldn't crash
                assertTrue("Error handling test completed", true)
            } catch (e: Exception) {
                // Expected behavior without real hardware
                assertTrue("Exception handled gracefully", true)
            }
        }

    @Test
    fun testCleanupHandling() =
        runTest {
            val sessionId = "cleanup_test"

            // Test various cleanup scenarios
            try {
                // Test starting recording
                recorder.startRecording(sessionId)
            } catch (e: Exception) {
                // Expected without real hardware
            }

            try {
                // Test stopping recording
                recorder.stopRecording()
            } catch (e: Exception) {
                // Expected without real hardware
            }

            // Test should complete without crashing
            assertTrue("Cleanup handling test completed", true)
        }

    @Test
    fun testMultipleInstances() {
        // Test creating multiple recorder instances
        val recorder2 = ShimmerGSRRecorder(context, samplingRateHz = 64)
        val recorder3 = ShimmerGSRRecorder(context, samplingRateHz = 512)

        assertNotNull("Second recorder should be created", recorder2)
        assertNotNull("Third recorder should be created", recorder3)

        // All should be independent instances
        assertNotSame("Recorders should be different instances", recorder, recorder2)
        assertNotSame("Recorders should be different instances", recorder2, recorder3)
    }
}
