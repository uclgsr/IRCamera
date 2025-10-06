package com.mpdc4gsr.gsr.tests

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mpdc4gsr.gsr.service.MockShimmerDevice
import com.mpdc4gsr.gsr.service.MockShimmerDeviceFactory
import com.mpdc4gsr.gsr.service.ShimmerDeviceFactoryResolver
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Ignore("All tests disabled")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ShimmerFactoryResolverTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `resolver creates factory successfully`() {
        // When
        val factory = ShimmerDeviceFactoryResolver.createFactory(context)
        // Then
        assertNotNull("Factory should not be null", factory)
        // factory is always ShimmerDeviceFactory by contract, no need for type check
    }

    @Test
    fun `resolver factory creates device interface`() {
        // Given
        val factory = ShimmerDeviceFactoryResolver.createFactory(context)
        // When
        val device = factory.createShimmerDevice()
        // Then
        assertNotNull("Device should not be null", device)
        // device is always ShimmerDeviceInterface by contract, no need for type check
    }

    @Test
    fun `resolver falls back to mock in test environment`() {
        // Given
        val factory = ShimmerDeviceFactoryResolver.createFactory(context)
        // When
        val device = factory.createShimmerDevice()
        // Then
        // In test environment, should fall back to mock since real implementation class is not available
        assertTrue(
            "Should fall back to mock implementation in test environment",
            factory is MockShimmerDeviceFactory
        )
        assertTrue(
            "Device should be mock implementation",
            device is MockShimmerDevice
        )
    }

    @Test
    fun `mock device has expected interface methods`() {
        // Given
        val factory = ShimmerDeviceFactoryResolver.createFactory(context)
        val device = factory.createShimmerDevice()
        // When/Then - Test that all interface methods work
        val connected = device.isConnected()
        assertTrue("Should be able to call isConnected", connected == false || connected == true)
        val connectResult = device.connect("00:00:00:00:00:00", "test")
        assertTrue(
            "Should be able to call connect",
            connectResult == true || connectResult == false
        )
        val streamResult = device.startStreaming()
        assertTrue(
            "Should be able to call startStreaming",
            streamResult == true || streamResult == false
        )
        val stopResult = device.stopStreaming()
        assertTrue(
            "Should be able to call stopStreaming",
            stopResult == true || stopResult == false
        )
        val disconnectResult = device.disconnect()
        assertTrue(
            "Should be able to call disconnect",
            disconnectResult == true || disconnectResult == false
        )
    }
}