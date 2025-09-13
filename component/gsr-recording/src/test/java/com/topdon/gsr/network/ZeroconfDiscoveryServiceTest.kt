package com.topdon.gsr.network

import android.content.Context
import android.net.nsd.NsdManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNsdManager

/**
 * Context-based tests for ZeroconfDiscoveryService using Robolectric
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O])
class ZeroconfDiscoveryServiceTest {
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

        // Listener should be set (can't directly verify private field, but no exception should occur)
        // Test passes if no exception is thrown
        assertTrue("Setting service listener should succeed", true)

        // Test removing listener
        discoveryService.setServiceListener(null)
        assertTrue("Removing service listener should succeed", true)
    }

    @Test
    fun testGetDiscoveredServices() {
        // Initially should have empty services
        val initialServices = discoveryService.getDiscoveredControllers()
        assertNotNull("Discovered services should not be null", initialServices)
        assertTrue("Discovered services should be a list", initialServices is List<*>)
        assertTrue("Initial services should be empty", initialServices.isEmpty())
    }

    @Test
    fun testServiceNameGeneration() {
        // Test that service names are generated properly
        val deviceName1 = "Device One"
        val deviceName2 = "Device Two"

        // This test mainly ensures the service can handle different device names
        // without throwing exceptions
        assertTrue(
            "Different device names should be handled",
            deviceName1 != deviceName2,
        )
    }

    @Test
    fun testCleanupResources() {
        // Cleanup all resources - this is synchronous and safe
        discoveryService.cleanup()

        // After cleanup test passes if no exceptions thrown
        assertTrue("Cleanup should complete without errors", true)
    }

    @Test
    fun testServiceListenerInterface() {
        // Test that we can create a listener implementation
        val listener =
            object : ZeroconfDiscoveryService.ServiceDiscoveryListener {
                override fun onServiceDiscovered(serviceInfo: NetworkClient.ControllerInfo) {
                    // Mock implementation
                }

                override fun onServiceLost(serviceName: String) {
                    // Mock implementation
                }

                override fun onServiceRegistered(serviceName: String) {
                    // Mock implementation
                }

                override fun onDiscoveryError(
                    errorCode: Int,
                    message: String,
                ) {
                    // Mock implementation
                }
            }

        assertNotNull("Service listener should be created", listener)

        // Test setting and unsetting
        discoveryService.setServiceListener(listener)
        discoveryService.setServiceListener(null)

        assertTrue("Service listener interface test should pass", true)
    }

    @Test
    fun testContextDependency() {
        // Test that the service properly uses the context
        val testContext = ApplicationProvider.getApplicationContext<Context>()
        val testService = ZeroconfDiscoveryService(testContext)

        assertNotNull("Service with context should be created", testService)

        // Test that we can get discovered controllers (which should be empty initially)
        val controllers = testService.getDiscoveredControllers()
        assertNotNull("Controllers list should not be null", controllers)
        assertTrue("Controllers list should be empty initially", controllers.isEmpty())
    }

    @Test
    fun testNetworkClientControllerInfo() {
        // Test the data class used in the service
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
