package mpdc4gsr.sensors.gsr

import android.content.Context
import com.mpdc4gsr.gsr.service.ShimmerDeviceFactory
import com.mpdc4gsr.gsr.service.ShimmerDeviceInterface
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test to validate that RealShimmerDeviceFactory can be instantiated
 * and creates proper ShimmerDeviceInterface instances
 */
@RunWith(RobolectricTestRunner::class)
class RealShimmerDeviceFactoryTest {

    private val mockContext: Context = mockk(relaxed = true)

    @Test
    fun `factory creates ShimmerDeviceInterface instance`() {
        // Given
        val factory: ShimmerDeviceFactory = RealShimmerDeviceFactory(mockContext)

        // When
        val device: ShimmerDeviceInterface = factory.createShimmerDevice()

        // Then
        assertNotNull("Device should not be null", device)
        assertTrue("Device should be RealShimmerDevice instance", device is RealShimmerDevice)
    }

    @Test
    fun `device interface has expected methods`() {
        // Given
        val factory = RealShimmerDeviceFactory(mockContext)
        val device = factory.createShimmerDevice()

        // When/Then - Test that interface methods are present
        assertNotNull("connect method should be present", device::connect)
        assertNotNull("startStreaming method should be present", device::startStreaming)
        assertNotNull("stopStreaming method should be present", device::stopStreaming)
        assertNotNull("disconnect method should be present", device::disconnect)
        assertNotNull("isConnected method should be present", device::isConnected)
        assertNotNull("setDataCallback method should be present", device::setDataCallback)
        assertNotNull(
            "setConnectionCallback method should be present",
            device::setConnectionCallback
        )
    }

    @Test
    fun `device initially not connected`() {
        // Given
        val factory = RealShimmerDeviceFactory(mockContext)
        val device = factory.createShimmerDevice()

        // When
        val isConnected = device.isConnected()

        // Then
        assertFalse("Device should not be connected initially", isConnected)
    }
}
}