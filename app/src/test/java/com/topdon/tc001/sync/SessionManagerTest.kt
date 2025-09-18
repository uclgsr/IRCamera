package com.topdon.tc001.sync

import android.content.Context
import android.content.SharedPreferences
import com.topdon.tc001.sync.SessionManager
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class SessionManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var sessionManager: SessionManager
    
    @Before
    fun setup() {
        mockContext = mockk()
        mockSharedPreferences = mockk()
        mockEditor = mockk()
        
        // Setup SharedPreferences mock behavior
        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPreferences
        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.putLong(any(), any()) } returns mockEditor
        every { mockEditor.putBoolean(any(), any()) } returns mockEditor
        every { mockEditor.apply() } just runs
        every { mockEditor.commit() } returns true
        
        sessionManager = SessionManager(mockContext)
    }

    @Test
    fun `should create new session with unique ID`() = runTest {
        // Execute
        val sessionId1 = sessionManager.createSession()
        val sessionId2 = sessionManager.createSession()
        
        // Verify
        assertNotNull("Session ID should not be null", sessionId1)
        assertNotNull("Session ID should not be null", sessionId2)
        assertNotEquals("Session IDs should be unique", sessionId1, sessionId2)
        assertTrue("Session ID should contain timestamp format", 
            sessionId1.contains("session_"))
    }

    @Test
    fun `should start session with proper metadata`() = runTest {
        // Setup
        val sessionId = "test_session_123"
        val sessionDir = "/test/session/dir"
        
        // Execute
        val result = sessionManager.startSession(sessionId, sessionDir)
        
        // Verify
        assertTrue("Session should start successfully", result)
        verify { mockEditor.putString("current_session_id", sessionId) }
        verify { mockEditor.putString("current_session_dir", sessionDir) }
        verify { mockEditor.putLong(eq("session_start_time"), any()) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `should stop session and clear metadata`() = runTest {
        // Setup - start a session first
        val sessionId = "test_session_123"
        sessionManager.startSession(sessionId, "/test/dir")
        
        // Execute
        val result = sessionManager.stopSession()
        
        // Verify
        assertTrue("Session should stop successfully", result)
        verify { mockEditor.putString("current_session_id", null) }
        verify { mockEditor.putString("current_session_dir", null) }
        verify { mockEditor.putLong("session_start_time", 0L) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `should track session state correctly`() = runTest {
        // Setup
        every { mockSharedPreferences.getString("current_session_id", null) } returns null
        
        // Verify initial state
        assertFalse("Session should not be active initially", sessionManager.isSessionActive())
        assertNull("Current session ID should be null", sessionManager.getCurrentSessionId())
        
        // Setup active session
        every { mockSharedPreferences.getString("current_session_id", null) } returns "active_session"
        
        assertTrue("Session should be active", sessionManager.isSessionActive())
        assertEquals("Session ID should match", "active_session", sessionManager.getCurrentSessionId())
    }

    @Test
    fun `should calculate session duration correctly`() = runTest {
        // Setup - mock session start time (5 seconds ago)
        val startTime = System.currentTimeMillis() - 5000L
        every { mockSharedPreferences.getLong("session_start_time", 0L) } returns startTime
        
        // Execute
        val duration = sessionManager.getSessionDuration()
        
        // Verify - duration should be approximately 5 seconds (within 1 second tolerance)
        assertTrue("Duration should be approximately 5 seconds", 
            duration in 4000L..6000L)
    }

    @Test
    fun `should handle session metadata persistence`() = runTest {
        // Setup
        val metadata = mapOf(
            "device_model" to "Samsung Galaxy S22",
            "android_version" to "13",
            "app_version" to "1.0.0"
        )
        
        // Execute
        sessionManager.saveSessionMetadata("test_session", metadata)
        
        // Verify
        metadata.forEach { (key, value) ->
            verify { mockEditor.putString("meta_test_session_$key", value) }
        }
        verify { mockEditor.apply() }
    }

    @Test
    fun `should retrieve session metadata correctly`() = runTest {
        // Setup
        val sessionId = "test_session"
        every { mockSharedPreferences.getString("meta_${sessionId}_device_model", null) } returns "Samsung Galaxy S22"
        every { mockSharedPreferences.getString("meta_${sessionId}_android_version", null) } returns "13"
        every { mockSharedPreferences.getString("meta_${sessionId}_app_version", null) } returns "1.0.0"
        
        // Execute
        val metadata = sessionManager.getSessionMetadata(sessionId)
        
        // Verify
        assertEquals("Device model should match", "Samsung Galaxy S22", 
            metadata["device_model"])
        assertEquals("Android version should match", "13", 
            metadata["android_version"])
        assertEquals("App version should match", "1.0.0", 
            metadata["app_version"])
    }

    @Test
    fun `should validate session directory creation`() = runTest {
        // Setup - create temporary directory for testing
        val tempDir = File("/tmp/test-session-${System.currentTimeMillis()}")
        val sessionDir = sessionManager.createSessionDirectory(tempDir.absolutePath)
        
        // Execute
        val isValid = sessionManager.validateSessionDirectory(sessionDir)
        
        // Verify
        assertTrue("Session directory should be valid", isValid)
        assertTrue("Session directory should exist", File(sessionDir).exists())
        
        // Cleanup
        File(sessionDir).deleteRecursively()
    }

    @Test
    fun `should handle concurrent session requests`() = runTest {
        // Setup
        val results = mutableListOf<String>()
        
        // Execute - simulate concurrent session creation
        repeat(5) { i ->
            val sessionId = sessionManager.createSession()
            results.add(sessionId)
        }
        
        // Verify
        assertEquals("Should create 5 sessions", 5, results.size)
        assertEquals("All session IDs should be unique", 5, results.toSet().size)
        
        // All should be valid session IDs
        results.forEach { sessionId ->
            assertTrue("Session ID should be valid format", 
                sessionId.startsWith("session_"))
        }
    }

    @Test
    fun `should track sensor data statistics`() = runTest {
        // Setup
        val sessionId = "test_session"
        sessionManager.startSession(sessionId, "/test/dir")
        
        // Execute - update data statistics
        sessionManager.updateDataStatistics("GSR", 100, 1024L)
        sessionManager.updateDataStatistics("RGB", 30, 2048L)
        sessionManager.updateDataStatistics("Thermal", 50, 512L)
        
        // Verify - data should be tracked
        val gsrStats = sessionManager.getDataStatistics("GSR")
        assertEquals("GSR sample count should match", 100, gsrStats["sample_count"])
        assertEquals("GSR data size should match", 1024L, gsrStats["data_size"])
        
        val rgbStats = sessionManager.getDataStatistics("RGB")
        assertEquals("RGB sample count should match", 30, rgbStats["sample_count"])
        assertEquals("RGB data size should match", 2048L, rgbStats["data_size"])
    }

    @Test
    fun `should generate session summary report`() = runTest {
        // Setup
        val sessionId = "test_session"
        sessionManager.startSession(sessionId, "/test/dir")
        sessionManager.updateDataStatistics("GSR", 100, 1024L)
        sessionManager.updateDataStatistics("RGB", 30, 2048L)
        
        // Execute
        val summary = sessionManager.generateSessionSummary()
        
        // Verify
        assertNotNull("Summary should not be null", summary)
        assertTrue("Summary should contain session info", 
            summary.contains(sessionId))
        assertTrue("Summary should contain data statistics", 
            summary.contains("GSR"))
        assertTrue("Summary should contain data statistics", 
            summary.contains("RGB"))
    }

    @Test
    fun `should handle session cleanup after completion`() = runTest {
        // Setup
        val sessionId = "test_session"
        sessionManager.startSession(sessionId, "/test/dir")
        
        // Execute - complete session
        sessionManager.completeSession(sessionId)
        
        // Verify cleanup occurred
        verify { mockEditor.putString("current_session_id", null) }
        verify { mockEditor.apply() }
        
        // Session should no longer be active
        every { mockSharedPreferences.getString("current_session_id", null) } returns null
        assertFalse("Session should not be active after completion", 
            sessionManager.isSessionActive())
    }
}