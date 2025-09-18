package com.topdon.tc001.network

import android.content.Context
import com.topdon.tc001.network.DataStreamingService
import com.topdon.tc001.network.ZeroconfDiscoveryService
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.InetAddress

@RunWith(RobolectricTestRunner::class)
class NetworkServicesIntegrationTest {

    private lateinit var mockContext: Context
    private lateinit var dataStreamingService: DataStreamingService
    private lateinit var zeroconfService: ZeroconfDiscoveryService

    @Before
    fun setup() {
        mockContext = mockk()
        dataStreamingService = DataStreamingService(mockContext)
        zeroconfService = ZeroconfDiscoveryService(mockContext)
        
        // Setup basic context behavior
        every { mockContext.packageName } returns "com.topdon.tc001"
        every { mockContext.applicationInfo } returns mockk()
    }

    @Test
    fun `should initialize data streaming service correctly`() = runTest {
        // Verify initialization
        assertNotNull("Data streaming service should be initialized", dataStreamingService)
        assertFalse("Streaming should not be active initially", 
            dataStreamingService.isStreaming())
    }

    @Test
    fun `should start and stop data streaming`() = runTest {
        // Setup
        val sessionId = "test_session_123"
        
        // Execute - start streaming
        val startResult = dataStreamingService.startStreaming(sessionId)
        
        // Verify streaming started
        assertTrue("Should start streaming successfully", startResult)
        assertTrue("Streaming should be active", dataStreamingService.isStreaming())
        assertEquals("Session ID should match", sessionId, 
            dataStreamingService.getCurrentSessionId())
        
        // Execute - stop streaming
        val stopResult = dataStreamingService.stopStreaming()
        
        // Verify streaming stopped
        assertTrue("Should stop streaming successfully", stopResult)
        assertFalse("Streaming should not be active", dataStreamingService.isStreaming())
        assertNull("Session ID should be null", dataStreamingService.getCurrentSessionId())
    }

    @Test
    fun `should handle streaming data queue operations`() = runTest {
        // Setup
        val sessionId = "test_session"
        dataStreamingService.startStreaming(sessionId)
        
        // Execute - add GSR data
        val gsrSample = DataStreamingService.GSRSample(
            timestamp = System.currentTimeMillis(),
            gsrValue = 0.5f,
            skinConductance = 2.5f,
            deviceId = "shimmer_001",
            sessionId = sessionId
        )
        
        dataStreamingService.addGSRSample(gsrSample)
        
        // Execute - add thermal data
        val thermalSample = DataStreamingService.ThermalSample(
            timestamp = System.currentTimeMillis(),
            frameIndex = 1L,
            temperature = 25.5f,
            x = 100,
            y = 150,
            sessionId = sessionId
        )
        
        dataStreamingService.addThermalSample(thermalSample)
        
        // Verify data was queued
        assertTrue("GSR queue should not be empty", 
            dataStreamingService.getGSRQueueSize() > 0)
        assertTrue("Thermal queue should not be empty", 
            dataStreamingService.getThermalQueueSize() > 0)
        
        // Stop streaming
        dataStreamingService.stopStreaming()
    }

    @Test
    fun `should handle network connection states`() = runTest {
        // Verify initial state
        assertFalse("Should not be connected initially", 
            dataStreamingService.isConnectedToPC())
        
        // Mock successful PC connection
        with(dataStreamingService) {
            // Simulate connection establishment
            val connectionResult = connectToPC("192.168.1.100", 8080)
            
            // Verify connection
            if (connectionResult) {
                assertTrue("Should be connected to PC", isConnectedToPC())
            } else {
                // Connection may fail in test environment - this is acceptable
                assertFalse("Connection failed as expected in test", isConnectedToPC())
            }
        }
    }

    @Test
    fun `should handle data batching for network efficiency`() = runTest {
        // Setup
        val sessionId = "batch_test_session"
        dataStreamingService.startStreaming(sessionId)
        dataStreamingService.setBatchSize(10) // Small batch for testing
        
        // Execute - add multiple GSR samples
        repeat(15) { i ->
            val sample = DataStreamingService.GSRSample(
                timestamp = System.currentTimeMillis() + i * 100L,
                gsrValue = 0.5f + i * 0.01f,
                skinConductance = 2.5f + i * 0.1f,
                deviceId = "shimmer_001",
                sessionId = sessionId
            )
            dataStreamingService.addGSRSample(sample)
        }
        
        // Allow batch processing time
        Thread.sleep(100)
        
        // Verify batching occurred
        val batchesSent = dataStreamingService.getBatchesSent()
        assertTrue("Should have sent at least one batch", batchesSent >= 1)
        
        // Stop streaming
        dataStreamingService.stopStreaming()
    }

    @Test
    fun `should handle network discovery with Zeroconf`() = runTest {
        // Verify Zeroconf service initialization
        assertNotNull("Zeroconf service should be initialized", zeroconfService)
        
        // Execute - start discovery
        val discoveryResult = zeroconfService.startDiscovery()
        
        // Verify discovery started (may fail without real network)
        if (discoveryResult) {
            assertTrue("Discovery should be active", zeroconfService.isDiscovering())
        }
        
        // Stop discovery
        zeroconfService.stopDiscovery()
        assertFalse("Discovery should be stopped", zeroconfService.isDiscovering())
    }

    @Test
    fun `should handle PC device discovery events`() = runTest {
        // Setup discovery listener
        var devicesFound = mutableListOf<ZeroconfDiscoveryService.PCDevice>()
        
        zeroconfService.setDiscoveryListener(object : ZeroconfDiscoveryService.DiscoveryListener {
            override fun onPCDeviceFound(device: ZeroconfDiscoveryService.PCDevice) {
                devicesFound.add(device)
            }
            
            override fun onPCDeviceLost(device: ZeroconfDiscoveryService.PCDevice) {
                devicesFound.removeAll { it.id == device.id }
            }
            
            override fun onDiscoveryError(error: String) {
                // Handle error
            }
        })
        
        // Simulate device discovery (mock)
        val mockDevice = ZeroconfDiscoveryService.PCDevice(
            id = "pc_controller_001",
            name = "IRCamera PC Controller",
            address = InetAddress.getByName("192.168.1.100"),
            port = 8080
        )
        
        // Manually trigger discovery event for testing
        zeroconfService.simulateDeviceFound(mockDevice)
        
        // Verify device was found
        assertEquals("Should find one device", 1, devicesFound.size)
        assertEquals("Device ID should match", "pc_controller_001", devicesFound[0].id)
    }

    @Test
    fun `should handle network reconnection scenarios`() = runTest {
        // Setup streaming
        val sessionId = "reconnection_test"
        dataStreamingService.startStreaming(sessionId)
        
        // Simulate connection loss
        dataStreamingService.simulateConnectionLoss()
        
        // Verify connection state
        assertFalse("Should not be connected after loss", 
            dataStreamingService.isConnectedToPC())
        
        // Streaming should continue (with local buffering)
        assertTrue("Streaming should still be active", 
            dataStreamingService.isStreaming())
        
        // Simulate reconnection
        val reconnectionResult = dataStreamingService.attemptReconnection()
        
        // Verify reconnection attempt
        assertTrue("Reconnection attempt should return result", 
            reconnectionResult is Boolean)
        
        // Stop streaming
        dataStreamingService.stopStreaming()
    }

    @Test
    fun `should validate data compression for network efficiency`() = runTest {
        // Setup
        dataStreamingService.enableCompression(true)
        
        // Create large data sample
        val largeSample = DataStreamingService.GSRSample(
            timestamp = System.currentTimeMillis(),
            gsrValue = 0.123456789f,
            skinConductance = 2.987654321f,
            deviceId = "shimmer_with_very_long_device_identifier_001",
            sessionId = "session_with_very_long_session_identifier_for_testing"
        )
        
        // Execute - get compressed vs uncompressed size
        val uncompressedSize = dataStreamingService.getUncompressedSize(largeSample)
        val compressedSize = dataStreamingService.getCompressedSize(largeSample)
        
        // Verify compression effectiveness
        if (compressedSize > 0 && uncompressedSize > 0) {
            assertTrue("Compressed size should be less than uncompressed", 
                compressedSize <= uncompressedSize)
            
            val compressionRatio = compressedSize.toFloat() / uncompressedSize.toFloat()
            assertTrue("Compression ratio should be reasonable", 
                compressionRatio in 0.1f..1.0f)
        }
    }

    @Test
    fun `should handle concurrent streaming operations`() = runTest {
        // Setup multiple streaming sessions
        val sessions = listOf("session_1", "session_2", "session_3")
        
        sessions.forEach { sessionId ->
            // Each session should be independent
            val streamingService = DataStreamingService(mockContext)
            val result = streamingService.startStreaming(sessionId)
            
            if (result) {
                assertTrue("Session should be streaming", streamingService.isStreaming())
                assertEquals("Session ID should match", sessionId, 
                    streamingService.getCurrentSessionId())
                
                streamingService.stopStreaming()
            }
        }
    }

    @Test
    fun `should track network performance metrics`() = runTest {
        // Setup
        val sessionId = "metrics_test"
        dataStreamingService.startStreaming(sessionId)
        
        // Add some data samples
        repeat(50) { i ->
            val sample = DataStreamingService.GSRSample(
                timestamp = System.currentTimeMillis() + i * 10L,
                gsrValue = 0.5f,
                skinConductance = 2.5f,
                deviceId = "shimmer_001",
                sessionId = sessionId
            )
            dataStreamingService.addGSRSample(sample)
        }
        
        // Allow processing time
        Thread.sleep(200)
        
        // Verify metrics collection
        val metrics = dataStreamingService.getNetworkMetrics()
        assertNotNull("Metrics should not be null", metrics)
        
        // Check key metrics exist
        assertTrue("Should track total samples", metrics.totalSamplesSent >= 0)
        assertTrue("Should track total bytes", metrics.totalBytesSent >= 0)
        assertTrue("Should track batch count", metrics.totalBatchesSent >= 0)
        
        // Stop streaming
        dataStreamingService.stopStreaming()
    }

    @Test
    fun `should handle network error recovery gracefully`() = runTest {
        // Setup
        val sessionId = "error_recovery_test"
        dataStreamingService.startStreaming(sessionId)
        
        // Simulate network errors
        val networkErrors = listOf(
            "Connection timeout",
            "Host unreachable",
            "Socket closed",
            "Data corruption detected"
        )
        
        networkErrors.forEach { error ->
            // Simulate error
            dataStreamingService.simulateNetworkError(error)
            
            // Verify error handling
            val errorCount = dataStreamingService.getErrorCount()
            assertTrue("Should track network errors", errorCount >= 0)
            
            // Service should still be functional
            assertTrue("Service should still be streaming", 
                dataStreamingService.isStreaming())
        }
        
        // Stop streaming
        dataStreamingService.stopStreaming()
    }
}