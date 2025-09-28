package mpdc4gsr.network.managers

import kotlinx.coroutines.runBlocking
import mpdc4gsr.network.ProtocolHandler
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CommandServerTest {

    private lateinit var commandServer: CommandServer
    private lateinit var mockCallback: MockCommandCallback

    @Before
    fun setup() {
        commandServer = CommandServer()
        mockCallback = MockCommandCallback()
        commandServer.setCommandCallback(mockCallback)
    }

    @Test
    fun testHandleStartRecordingSuccess() = runBlocking {
        // Arrange
        mockCallback.startRecordingResult = true
        val sessionId = "test-session-123"

        // Act
        val result = commandServer.handleStartRecording(sessionId)

        // Assert
        assertTrue("Start recording should succeed", result.success)
        assertEquals("Recording started", result.message)
        assertEquals(sessionId, result.data["session_id"])
        assertTrue("Callback should be called", mockCallback.startRecordingCalled)
        assertEquals(sessionId, mockCallback.lastSessionId)
    }

    @Test
    fun testHandleStartRecordingFailure() = runBlocking {
        // Arrange
        mockCallback.startRecordingResult = false
        val sessionId = "test-session-456"

        // Act
        val result = commandServer.handleStartRecording(sessionId)

        // Assert
        assertFalse("Start recording should fail", result.success)
        assertEquals("Recording start failed", result.message)
        assertEquals(sessionId, result.data["session_id"])
        assertTrue("Callback should be called", mockCallback.startRecordingCalled)
    }

    @Test
    fun testHandleStartRecordingNoCallback() = runBlocking {
        // Arrange
        commandServer.setCommandCallback(null)
        val sessionId = "test-session-789"

        // Act
        val result = commandServer.handleStartRecording(sessionId)

        // Assert
        assertFalse("Start recording should fail without callback", result.success)
        assertEquals("Command callback not available", result.message)
    }

    @Test
    fun testHandleStopRecordingSuccess() = runBlocking {
        // Arrange
        mockCallback.stopRecordingResult = true
        val sessionId = "test-session-123"

        // Act
        val result = commandServer.handleStopRecording(sessionId)

        // Assert
        assertTrue("Stop recording should succeed", result.success)
        assertEquals("Recording stopped", result.message)
        assertEquals(sessionId, result.data["session_id"])
        assertTrue("Callback should be called", mockCallback.stopRecordingCalled)
    }

    @Test
    fun testHandleStopRecordingFailure() = runBlocking {
        // Arrange
        mockCallback.stopRecordingResult = false
        val sessionId = "test-session-456"

        // Act
        val result = commandServer.handleStopRecording(sessionId)

        // Assert
        assertFalse("Stop recording should fail", result.success)
        assertEquals("Recording stop failed", result.message)
        assertTrue("Callback should be called", mockCallback.stopRecordingCalled)
    }

    @Test
    fun testHandleSyncRequestSuccess() = runBlocking {
        // Arrange
        mockCallback.syncRequestResult = true
        val pcTimestamp = 1640995200000L

        // Act
        val result = commandServer.handleSyncRequest(pcTimestamp)

        // Assert
        assertTrue("Sync request should succeed", result.success)
        assertTrue("Phone timestamp should be set", result.phoneTimestamp > 0)
        assertTrue("Callback should be called", mockCallback.syncRequestCalled)
    }

    @Test
    fun testHandleSyncRequestFailure() = runBlocking {
        // Arrange
        mockCallback.syncRequestResult = false
        val pcTimestamp = 1640995200000L

        // Act
        val result = commandServer.handleSyncRequest(pcTimestamp)

        // Assert
        assertFalse("Sync request should fail", result.success)
        assertTrue("Callback should be called", mockCallback.syncRequestCalled)
    }

    @Test
    fun testHandleSyncRequestNoCallback() = runBlocking {
        // Arrange
        commandServer.setCommandCallback(null)
        val pcTimestamp = 1640995200000L

        // Act
        val result = commandServer.handleSyncRequest(pcTimestamp)

        // Assert
        assertFalse("Sync request should fail without callback", result.success)
    }

    /**
     * Mock implementation of CommandCallback for testing
     */
    private class MockCommandCallback : ProtocolHandler.CommandCallback {
        var startRecordingResult = true
        var stopRecordingResult = true
        var syncRequestResult = true

        var startRecordingCalled = false
        var stopRecordingCalled = false
        var syncRequestCalled = false

        var lastSessionId: String? = null
        var lastConfiguration: JSONObject? = null
        var lastPcAddress: String? = null

        override suspend fun onStartRecording(sessionId: String, configuration: JSONObject): Boolean {
            startRecordingCalled = true
            lastSessionId = sessionId
            lastConfiguration = configuration
            return startRecordingResult
        }

        override suspend fun onStopRecording(): Boolean {
            stopRecordingCalled = true
            return stopRecordingResult
        }

        override suspend fun onSyncRequest(pcAddress: String): Boolean {
            syncRequestCalled = true
            lastPcAddress = pcAddress
            return syncRequestResult
        }
    }
}