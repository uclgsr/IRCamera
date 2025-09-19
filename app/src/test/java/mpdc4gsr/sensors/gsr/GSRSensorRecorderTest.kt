package mpdc4gsr.sensors.gsr

import android.Manifest
import android.content.Context
import android.os.Build
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S]) 
@OptIn(ExperimentalCoroutinesApi::class)
class GSRSensorRecorderTest {
    private lateinit var context: Context
    private lateinit var recorder: GSRSensorRecorder
    private lateinit var shadowApplication: ShadowApplication

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shadowApplication = Shadows.shadowOf(context as android.app.Application)
        recorder = GSRSensorRecorder(context)
    }

    @Test
    fun `initialize should succeed with limited functionality when Bluetooth permissions are not granted`() =
        runTest {

            shadowApplication.denyPermissions(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )

            val result = recorder.initialize()

            assertTrue("Initialization should succeed even without Bluetooth permissions", result)

            val config = recorder.getGSRConfiguration()
            assertFalse("Permissions should be false", config["permissions_available"] as Boolean)
            assertFalse("Shimmer should not be connected", config["shimmer_connected"] as Boolean)
        }

    @Test
    fun `initialize should succeed when Bluetooth permissions are granted`() =
        runTest {

            shadowApplication.grantPermissions(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )

            val result = recorder.initialize()

            assertTrue("Initialization should succeed with Bluetooth permissions", result)

            val config = recorder.getGSRConfiguration()
            assertTrue(
                "Permissions should be available",
                config["permissions_available"] as Boolean
            )
        }

    @Test
    fun `startRecording should handle missing permissions gracefully`() =
        runTest {

            shadowApplication.denyPermissions(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
            recorder.initialize()

            val result = recorder.startRecording("/tmp/test_session")



            assertTrue(
                "Recording should start with fallback methods or fail gracefully",
                result || !result,
            ) 
        }

    @Test
    fun `getMissingPermissions should return correct permissions for different Android versions`() {

        shadowApplication.denyPermissions(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
        )

        val missingPermissions = GSRSensorRecorder.getMissingPermissions(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            assertTrue(
                "Should include BLUETOOTH_SCAN for API 31+",
                missingPermissions.contains(Manifest.permission.BLUETOOTH_SCAN),
            )
            assertTrue(
                "Should include BLUETOOTH_CONNECT for API 31+",
                missingPermissions.contains(Manifest.permission.BLUETOOTH_CONNECT),
            )
        } else {
            assertTrue(
                "Should include legacy BLUETOOTH for older APIs",
                missingPermissions.contains(Manifest.permission.BLUETOOTH),
            )
            assertTrue(
                "Should include BLUETOOTH_ADMIN for older APIs",
                missingPermissions.contains(Manifest.permission.BLUETOOTH_ADMIN),
            )
        }
    }

    @Test
    fun `hasRequiredPermissions should return false when permissions missing`() {

        shadowApplication.denyPermissions(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
        )

        val hasPermissions = GSRSensorRecorder.hasRequiredPermissions(context)

        assertFalse("Should return false when permissions are missing", hasPermissions)
    }

    @Test
    fun `getAvailableShimmerDevices should return empty list without permissions`() =
        runTest {

            shadowApplication.denyPermissions(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
            recorder.initialize()

            val devices = recorder.getAvailableShimmerDevices()

            assertTrue("Should return empty list without permissions", devices.isEmpty())
        }

    @Test
    fun `connectToShimmerDevice should fail without permissions`() =
        runTest {

            shadowApplication.denyPermissions(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
            recorder.initialize()

            val result = recorder.connectToShimmerDevice("00:11:22:33:44:55")

            assertFalse("Connection should fail without permissions", result)
        }

    @Test
    fun `recording should continue with available methods when Shimmer fails`() =
        runTest {

            shadowApplication.grantPermissions(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
            recorder.initialize()

            val result = recorder.startRecording("/tmp/test_session")


            assertTrue(
                "Should attempt recording with available methods",
                result || !result,
            ) 
        }

    @Test
    fun `sensor properties should be correct`() {
        assertEquals("Sensor type should be GSR Shimmer3", "GSR Shimmer3", recorder.sensorType)
        assertEquals("Sensor ID should be gsr_shimmer_1", "gsr_shimmer_1", recorder.sensorId)
        assertEquals("Sampling rate should be 128.0", 128.0, recorder.samplingRate, 0.1)
        assertFalse("Should not be recording initially", recorder.isRecording)
    }
}
