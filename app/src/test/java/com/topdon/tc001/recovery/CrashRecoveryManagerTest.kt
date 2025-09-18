package com.topdon.tc001.recovery

import android.content.Context
import android.content.SharedPreferences
import com.topdon.tc001.recovery.CrashRecoveryManager
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class CrashRecoveryManagerTest {
    
    private lateinit var mockContext: Context
    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var crashRecoveryManager: CrashRecoveryManager
    
    @Before
    fun setup() {
        mockContext = mockk()
        mockSharedPreferences = mockk()
        mockEditor = mockk()
        
        // Setup specific SharedPreferences behaviors
        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPreferences
        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.putLong(any(), any()) } returns mockEditor
        every { mockEditor.putBoolean(any(), any()) } returns mockEditor
        every { mockEditor.remove(any()) } returns mockEditor
        every { mockEditor.apply() } just runs
        every { mockEditor.commit() } returns true
        
        crashRecoveryManager = CrashRecoveryManager(mockContext)
    }
    
    @Test
    fun `should detect incomplete recording session with MVP recovery data`() = runTest {
        // Setup - simulate real incomplete session scenario
        val sessionId = "session_$(System.currentTimeMillis())"
        val sessionStartTime = System.currentTimeMillis() - 300000L // 5 minutes ago
        
        every { mockSharedPreferences.contains("last_session_id") } returns true
        every { mockSharedPreferences.getString("last_session_id", null) } returns sessionId
        every { mockSharedPreferences.getLong("last_session_start", 0L) } returns sessionStartTime
        every { mockSharedPreferences.getBoolean("last_session_completed", false) } returns false
        
        // Execute
        val hasIncompleteSession = crashRecoveryManager.checkForIncompleteSession()
        
        // Verify - should detect the incomplete session
        assertTrue("Should detect incomplete session from previous app run", hasIncompleteSession)
        
        // Verify specific SharedPreferences interactions
        verify(exactly = 1) { mockSharedPreferences.contains("last_session_id") }
        verify(exactly = 1) { mockSharedPreferences.getString("last_session_id", null) }
        verify(exactly = 1) { mockSharedPreferences.getLong("last_session_start", 0L) }
        verify(exactly = 1) { mockSharedPreferences.getBoolean("last_session_completed", false) }
    }
    
    @Test
    fun `should clean up incomplete session data`() = runTest {
        // Setup
        val incompleteSessionId = "session_20240101_120000"
        every { mockSharedPreferences.getString("last_session_id", null) } returns incompleteSessionId
        every { mockSharedPreferences.getBoolean("last_session_completed", false) } returns false
        
        // Execute
        crashRecoveryManager.cleanupIncompleteSession()
        
        // Verify
        verify { mockEditor.putString("last_session_id", incompleteSessionId + "_FAILED") }
        verify { mockEditor.putBoolean("last_session_completed", true) }
        verify { mockEditor.apply() }
    }
    
    @Test
    fun `should mark session as started with recovery data`() = runTest {
        // Setup
        val sessionId = "session_20240101_120000"
        val startTime = System.currentTimeMillis()
        
        // Execute
        crashRecoveryManager.markSessionStarted(sessionId, startTime)
        
        // Verify
        verify { mockEditor.putString("last_session_id", sessionId) }
        verify { mockEditor.putLong("last_session_start", startTime) }
        verify { mockEditor.putBoolean("last_session_completed", false) }
        verify { mockEditor.apply() }
    }
    
    @Test
    fun `should mark session as completed successfully`() = runTest {
        // Setup
        val sessionId = "session_20240101_120000"
        
        // Execute
        crashRecoveryManager.markSessionCompleted(sessionId)
        
        // Verify
        verify { mockEditor.putBoolean("last_session_completed", true) }
        verify { mockEditor.apply() }
    }
    
    @Test
    fun `should release device locks after crash`() = runTest {
        // Setup
        val deviceLocks = listOf("gsr_device", "thermal_camera", "rgb_camera")
        every { mockSharedPreferences.getStringSet("active_device_locks", setOf()) } returns deviceLocks.toSet()
        
        // Execute
        val releasedLocks = crashRecoveryManager.releaseDeviceLocks()
        
        // Verify
        assertEquals("Should release all device locks", deviceLocks.size, releasedLocks.size)
        verify { mockEditor.remove("active_device_locks") }
        verify { mockEditor.apply() }
    }
    
    @Test
    fun `should handle session timeout scenarios`() = runTest {
        // Setup - simulate session started 2 hours ago
        val oldSessionStart = System.currentTimeMillis() - (2 * 60 * 60 * 1000L) // 2 hours ago
        every { mockSharedPreferences.contains("last_session_id") } returns true
        every { mockSharedPreferences.getLong("last_session_start", 0L) } returns oldSessionStart
        every { mockSharedPreferences.getBoolean("last_session_completed", false) } returns false
        
        val sessionTimeoutMs = 60 * 60 * 1000L // 1 hour timeout
        
        // Execute
        val isTimedOut = crashRecoveryManager.isSessionTimedOut(sessionTimeoutMs)
        
        // Verify
        assertTrue("Should detect timed out session", isTimedOut)
    }
    
    @Test
    fun `should preserve partial session data for analysis`() = runTest {
        // Setup
        val sessionId = "session_20240101_120000"
        val sessionData = mapOf(
            "gsr_samples" to "150",
            "rgb_frames" to "45",
            "thermal_frames" to "30"
        )
        
        // Execute
        crashRecoveryManager.preservePartialSessionData(sessionId, sessionData)
        
        // Verify
        sessionData.forEach { (key, value) ->
            verify { mockEditor.putString("partial_${sessionId}_${key}", value) }
        }
        verify { mockEditor.apply() }
    }
    
    @Test
    fun `should generate crash recovery report`() = runTest {
        // Setup
        val sessionId = "session_20240101_120000_FAILED"
        val recoveryData = mapOf(
            "crash_time" to System.currentTimeMillis().toString(),
            "session_duration" to "180000", // 3 minutes
            "devices_active" to "gsr,thermal"
        )
        
        every { mockSharedPreferences.all } returns recoveryData
        
        // Execute
        val report = crashRecoveryManager.generateRecoveryReport(sessionId)
        
        // Verify
        assertNotNull("Should generate recovery report", report)
        assertTrue("Report should contain session ID", report.contains(sessionId))
        assertTrue("Report should contain crash information", report.contains("crash"))
    }
    
    @Test
    fun `should handle multiple concurrent crash scenarios`() = runTest {
        // Setup - simulate multiple failed sessions
        val failedSessions = listOf(
            "session_20240101_120000_FAILED",
            "session_20240101_130000_FAILED",
            "session_20240101_140000_FAILED"
        )
        
        // Execute
        var recoveredSessions = 0
        failedSessions.forEach { sessionId ->
            if (crashRecoveryManager.recoverSession(sessionId)) {
                recoveredSessions++
            }
        }
        
        // Verify
        assertEquals("Should recover all failed sessions", failedSessions.size, recoveredSessions)
    }
    
    @Test
    fun `should prevent new sessions during recovery`() = runTest {
        // Setup
        every { mockSharedPreferences.getBoolean("recovery_in_progress", false) } returns true
        
        // Execute
        val canStartNewSession = crashRecoveryManager.canStartNewSession()
        
        // Verify
        assertFalse("Should prevent new session during recovery", canStartNewSession)
    }
    
    @Test
    fun `should clean up old recovery data`() = runTest {
        // Setup - simulate old recovery data
        val oldRecoveryData = mapOf(
            "session_20240101_120000_FAILED" to "old_data_1",
            "session_20240101_130000_FAILED" to "old_data_2",
            "session_20240103_120000_FAILED" to "recent_data" // Recent, should be kept
        )
        
        val cutoffTime = System.currentTimeMillis() - (48 * 60 * 60 * 1000L) // 48 hours ago
        
        // Execute
        val cleanedCount = crashRecoveryManager.cleanupOldRecoveryData(cutoffTime)
        
        // Verify
        assertTrue("Should clean up old recovery data", cleanedCount > 0)
    }
}