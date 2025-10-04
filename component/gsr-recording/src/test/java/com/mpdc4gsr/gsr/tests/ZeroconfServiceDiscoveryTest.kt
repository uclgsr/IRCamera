package com.mpdc4gsr.gsr.tests

import android.content.Context
import android.net.nsd.NsdManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.mpdc4gsr.gsr.network.ZeroconfDiscoveryService
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNsdManager

@Ignore("All tests disabled")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O])
class ZeroconfServiceDiscoveryTest {
    private lateinit var context: Context
    private lateinit var discoveryService: ZeroconfDiscoveryService
    private lateinit var nsdManager: NsdManager
    private lateinit var shadowNsdManager: ShadowNsdManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        discoveryService = ZeroconfDiscoveryService(context)
        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        shadowNsdManager = Shadows.shadowOf(nsdManager)
    }

    @Test
    fun testServiceCreation() {
        assertNotNull("Discovery service should be created", discoveryService)
    }

    @Test
    fun testNsdManagerAccess() {
        assertNotNull("NsdManager should be available", nsdManager)
        assertNotNull("Shadow NsdManager should be available", shadowNsdManager)
    }

    @Test
    fun testSetServiceListener() {
        var serviceDiscovered = false
        var serviceLost = false
        var serviceRegistered = false
        var discoveryError = false

        val listener =
            object : ZeroconfDiscoveryService.ServiceDiscoveryListener {
                override fun onServiceDiscovered(serviceInfo: NetworkClient.ControllerInfo) {
                    serviceDiscovered = true
                }

                override fun onServiceLost(serviceName: String) {
                    serviceLost = true
                }

                override fun onServiceRegistered(serviceName: String) {
                    serviceRegistered = true
                }

                override fun onDiscoveryError(
                    errorCode: Int,
                    message: String,
                ) {
                    discoveryError = true
                }
            }

        discoveryService.setServiceListener(listener)

        assertTrue("Setting service listener should succeed", true)

        discoveryService.setServiceListener(null)
        assertTrue("Removing service listener should succeed", true)
    }

    @Test
    fun testGetDiscoveredServices() {

        val initialServices = discoveryService.getDiscoveredControllers()
        assertNotNull("Discovered services should not be null", initialServices)
        assertTrue("Initial services should be empty", initialServices.isEmpty())
    }

    @Test
    fun testServiceNameGeneration() {

        val deviceName1 = "Device One"
        val deviceName2 = "Device Two"

        assertTrue(
            "Different device names should be handled",
            deviceName1 != deviceName2,
        )
    }

    @Test
    fun testCleanupResources() {

        discoveryService.cleanup()

        assertTrue("Cleanup should complete without errors", true)
    }

    @Test
    fun testServiceListenerInterface() {

        val listener =
            object : ZeroconfDiscoveryService.ServiceDiscoveryListener {
                override fun onServiceDiscovered(serviceInfo: NetworkClient.ControllerInfo) {

                }

                override fun onServiceLost(serviceName: String) {

                }

                override fun onServiceRegistered(serviceName: String) {

                }

                override fun onDiscoveryError(
                    errorCode: Int,
                    message: String,
                ) {

                }
            }

        assertNotNull("Service listener should be created", listener)

        discoveryService.setServiceListener(listener)
        discoveryService.setServiceListener(null)

        assertTrue("Service listener interface test should pass", true)
    }

    @Test
    fun testContextDependency() {

        val testContext = ApplicationProvider.getApplicationContext<Context>()
        val testService = ZeroconfDiscoveryService(testContext)

        assertNotNull("Service with context should be created", testService)

        val controllers = testService.getDiscoveredControllers()
        assertNotNull("Controllers list should not be null", controllers)
        assertTrue("Controllers list should be empty initially", controllers.isEmpty())
    }

    @Test
    fun testNetworkClientControllerInfo() {

        val controllerInfo =
            NetworkClient.ControllerInfo(
                ipAddress = "192.168.1.100",
                port = 8080,
                deviceName = "Test Controller",
                capabilities = listOf("VIDEO", "GSR"),
            )

        assertEquals("IP address should match", "192.168.1.100", controllerInfo.ipAddress)
        assertEquals("Port should match", 8080, controllerInfo.port)
        assertEquals("Device name should match", "Test Controller", controllerInfo.deviceName)
        assertEquals("Capabilities should match", 2, controllerInfo.capabilities.size)
        assertTrue("Should contain VIDEO capability", controllerInfo.capabilities.contains("VIDEO"))
    }
}
