package com.topdon.tc001.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.*
import java.net.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.*

/**
 * Unit tests for NetworkController JSON command protocol
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NetworkControllerTest {
    
    private lateinit var context: Context
    private lateinit var networkController: NetworkController
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        networkController = NetworkController(context)
    }
    
    @After
    fun tearDown() = runBlocking {
        networkController.stop()
    }
    
    @Test
    fun testNetworkControllerStartStop() = runTest {
        // Test starting the server
        val started = networkController.start(8081) // Use different port for testing
        assertTrue(started, "NetworkController should start successfully")
        assertTrue(networkController.isRunning(), "NetworkController should be running")
        
        // Test stopping the server
        networkController.stop()
        assertFalse(networkController.isRunning(), "NetworkController should be stopped")
    }
    
    @Test
    fun testJsonCommandParsing() = runTest {
        val port = 8082
        val latch = CountDownLatch(1)
        var receivedSessionId: String? = null
        var receivedModalities: List<String>? = null
        
        // Set up event listener
        networkController.setEventListener(object : NetworkController.NetworkControllerListener {
            override fun onStartRecordingCommand(sessionId: String, modalities: List<String>, options: Map<String, Any>) {
                receivedSessionId = sessionId
                receivedModalities = modalities
                latch.countDown()
            }
            
            override fun onStopRecordingCommand() {}
            override fun onClientConnected(clientId: String, clientInfo: String) {}
            override fun onClientDisconnected(clientId: String, reason: String) {}
            override fun onError(operation: String, error: String) {}
        })
        
        // Start server
        val started = networkController.start(port)
        assertTrue(started, "Server should start")
        
        // Wait a moment for server to be ready
        Thread.sleep(100)
        
        // Send JSON command
        val socket = Socket("localhost", port)
        val output = PrintWriter(socket.getOutputStream(), true)
        val input = BufferedReader(InputStreamReader(socket.getInputStream()))
        
        val command = """{"command": "start_recording", "session_id": "TEST123", "modalities": ["thermal", "GSR"]}"""
        output.println(command)
        
        // Wait for response
        val response = input.readLine()
        assertNotNull(response, "Should receive response")
        assertTrue(response.contains("recording_started"), "Response should indicate recording started")
        
        // Verify command was processed
        assertTrue(latch.await(1, TimeUnit.SECONDS), "Should receive command callback")
        assertEquals("TEST123", receivedSessionId)
        assertEquals(listOf("thermal", "GSR"), receivedModalities)
        
        socket.close()
    }
    
    @Test
    fun testPingCommand() = runTest {
        val port = 8083
        
        // Start server
        val started = networkController.start(port)
        assertTrue(started, "Server should start")
        
        Thread.sleep(100) // Wait for server to be ready
        
        // Send ping command
        val socket = Socket("localhost", port)
        val output = PrintWriter(socket.getOutputStream(), true)
        val input = BufferedReader(InputStreamReader(socket.getInputStream()))
        
        output.println("""{"command": "ping"}""")
        
        val response = input.readLine()
        assertNotNull(response, "Should receive response")
        assertTrue(response.contains("pong"), "Response should be pong")
        
        socket.close()
    }
    
    @Test
    fun testInvalidCommand() = runTest {
        val port = 8084
        
        // Start server
        val started = networkController.start(port)
        assertTrue(started, "Server should start")
        
        Thread.sleep(100)
        
        // Send invalid command
        val socket = Socket("localhost", port)
        val output = PrintWriter(socket.getOutputStream(), true)
        val input = BufferedReader(InputStreamReader(socket.getInputStream()))
        
        output.println("""{"command": "invalid_command"}""")
        
        val response = input.readLine()
        assertNotNull(response, "Should receive error response")
        assertTrue(response.contains("unknown_command"), "Response should indicate unknown command")
        
        socket.close()
    }
    
    @Test
    fun testStopRecordingCommand() = runTest {
        val port = 8085
        val latch = CountDownLatch(1)
        var stopCommandReceived = false
        
        // Set up event listener
        networkController.setEventListener(object : NetworkController.NetworkControllerListener {
            override fun onStartRecordingCommand(sessionId: String, modalities: List<String>, options: Map<String, Any>) {}
            
            override fun onStopRecordingCommand() {
                stopCommandReceived = true
                latch.countDown()
            }
            
            override fun onClientConnected(clientId: String, clientInfo: String) {}
            override fun onClientDisconnected(clientId: String, reason: String) {}
            override fun onError(operation: String, error: String) {}
        })
        
        // Start server
        val started = networkController.start(port)
        assertTrue(started, "Server should start")
        
        Thread.sleep(100)
        
        // Send stop command
        val socket = Socket("localhost", port)
        val output = PrintWriter(socket.getOutputStream(), true)
        val input = BufferedReader(InputStreamReader(socket.getInputStream()))
        
        output.println("""{"command": "stop_recording"}""")
        
        val response = input.readLine()
        assertNotNull(response, "Should receive response")
        assertTrue(response.contains("recording_stopped"), "Response should indicate recording stopped")
        
        // Verify command was processed
        assertTrue(latch.await(1, TimeUnit.SECONDS), "Should receive stop command callback")
        assertTrue(stopCommandReceived, "Stop command should be received")
        
        socket.close()
    }
}
