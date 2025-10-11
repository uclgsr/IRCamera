package mpdc4gsr.integration

import android.Manifest
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mpdc4gsr.gsr.capture.SimulationSource
import mpdc4gsr.gsr.device.ShimmerDeviceController
import mpdc4gsr.gsr.model.ConnectionState
import mpdc4gsr.gsr.session.TimelineClock
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Optional instrumentation test that exercises a real Shimmer device. It is skipped unless the test
 * run is configured with instrumentation arguments:
 *
 * ```
 * ./gradlew connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.runRealHardware=true \
 *   -Pandroid.testInstrumentationRunnerArguments.shimmer_mac=AA:BB:CC:DD:EE:FF
 * ```
 */
@RunWith(AndroidJUnit4::class)
class RealHardwareShimmerIntegrationTest {

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
        )

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context by lazy { instrumentation.targetContext }
    private val dispatcher = Dispatchers.IO

    private lateinit var timelineClock: TimelineClock
    private lateinit var simulationSource: SimulationSource
    private lateinit var shimmerController: ShimmerDeviceController

    private class ResumedLifecycleOwner : LifecycleOwner {
        private val registry =
            LifecycleRegistry(this).apply { currentState = Lifecycle.State.RESUMED }

        override val lifecycle: Lifecycle
            get() = registry
    }

    @Before
    fun setUp() {
        timelineClock = TimelineClock(dispatcher)
        simulationSource = SimulationSource(dispatcher)
        shimmerController =
            ShimmerDeviceController(
                context = context,
                lifecycleOwner = ResumedLifecycleOwner(),
                clock = timelineClock,
                dispatcher = dispatcher,
                simulationSource = simulationSource,
            )
    }

    @After
    fun tearDown() {
        shimmerController.close()
        simulationSource.close()
        timelineClock.close()
    }

    @Test
    fun shimmerStreamsSamplesWhenRealHardwareIsPresent() =
        runBlocking {
            val args = InstrumentationRegistry.getArguments()
            val runHardware = args.getString("runRealHardware")?.toBoolean() ?: false
            val macAddress = args.getString("shimmer_mac")

            assumeTrue("Real hardware test disabled", runHardware)
            assumeTrue("shimmer_mac instrumentation argument is required", !macAddress.isNullOrBlank())

            val connectResult = shimmerController.connect(macAddress!!)
            assertTrue("Expected connect() to initiate BLE handshake", connectResult)

            withTimeout(30_000) {
                shimmerController.devices
                    .filter { descriptors ->
                        descriptors[macAddress]?.connectionState == ConnectionState.READY
                    }
                    .first()
            }

            shimmerController.startStreaming(macAddress)

            val sample =
                withTimeout(20_000) {
                    shimmerController.samples
                        .filter { it.deviceId.equals(macAddress, ignoreCase = true) }
                        .first()
                }

            shimmerController.stopStreaming(macAddress)
            shimmerController.disconnect(macAddress)

            assertNotNull("Expected at least one sample from the Shimmer GSR device", sample)
        }
}
