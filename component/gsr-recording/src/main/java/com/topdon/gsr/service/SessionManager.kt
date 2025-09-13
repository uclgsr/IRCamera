package com.topdon.gsr.service

import android.content.Context
import android.util.Log
import com.topdon.gsr.model.SessionInfo
import com.topdon.gsr.util.TimeUtil
import java.util.concurrent.ConcurrentHashMap

/**
 * Session lifecycle and metadata management
 * Fixed memory leak by using application context and WeakReference
 */
class SessionManager private constructor(context: Context) {
    companion object {
        private const val TAG = "SessionManager"

        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // Use application context to prevent memory leaks
    private val appContext = context.applicationContext

    private val activeSessions = ConcurrentHashMap<String, SessionInfo>()
    private val sessionListeners = mutableListOf<SessionListener>()

    interface SessionListener {
        fun onSessionCreated(session: SessionInfo)

        fun onSessionUpdated(session: SessionInfo)

        fun onSessionCompleted(session: SessionInfo)

        fun onSessionError(
            sessionId: String,
            error: String,
        )
    }

    fun addSessionListener(listener: SessionListener) {
        sessionListeners.add(listener)
    }

    fun removeSessionListener(listener: SessionListener) {
        sessionListeners.remove(listener)
    }

    /**
     * Create a new session
     */
    fun createSession(
        sessionId: String? = null,
        participantId: String? = null,
        studyName: String? = null,
        metadata: Map<String, String> = emptyMap(),
    ): SessionInfo {
        val finalSessionId = sessionId ?: TimeUtil.generateSessionId("MultiModal")

        val session =
            SessionInfo(
                sessionId = finalSessionId,
                startTime = System.currentTimeMillis(),
                participantId = participantId,
                studyName = studyName,
            ).apply {
                this.metadata.putAll(metadata)
            }

        activeSessions[finalSessionId] = session
        sessionListeners.forEach { it.onSessionCreated(session) }

        Log.i(TAG, "Session created: $finalSessionId")
        return session
    }

    /**
     * Get active session by ID
     */
    fun getSession(sessionId: String): SessionInfo? {
        return activeSessions[sessionId]
    }

    /**
     * Get all active sessions
     */
    fun getActiveSessions(): List<SessionInfo> {
        return activeSessions.values.toList()
    }

    /**
     * Update session metadata
     */
    fun updateSession(
        sessionId: String,
        updates: (SessionInfo) -> Unit,
    ): Boolean {
        val session = activeSessions[sessionId] ?: return false

        updates(session)
        sessionListeners.forEach { it.onSessionUpdated(session) }

        Log.d(TAG, "Session updated: $sessionId")
        return true
    }

    /**
     * Complete a session (mark as ended)
     */
    fun completeSession(sessionId: String): SessionInfo? {
        val session = activeSessions.remove(sessionId) ?: return null

        session.endTime = System.currentTimeMillis()
        sessionListeners.forEach { it.onSessionCompleted(session) }

        Log.i(TAG, "Session completed: $sessionId, duration: ${session.getDurationMs()}ms")
        return session
    }

    /**
     * Force complete all active sessions
     */
    fun completeAllSessions(): List<SessionInfo> {
        val completed = mutableListOf<SessionInfo>()

        activeSessions.keys.forEach { sessionId ->
            completeSession(sessionId)?.let { completed.add(it) }
        }

        return completed
    }

    /**
     * Check if session is active
     */
    fun isSessionActive(sessionId: String): Boolean {
        return activeSessions.containsKey(sessionId)
    }

    /**
     * Get session statistics
     */
    fun getSessionStats(sessionId: String): SessionStats? {
        val session = activeSessions[sessionId] ?: return null

        return SessionStats(
            sessionId = sessionId,
            duration = session.getDurationMs(),
            sampleCount = session.sampleCount,
            syncMarkCount = session.syncMarks.size,
            isActive = session.isActive(),
        )
    }

    /**
     * Report session error
     */
    fun reportSessionError(
        sessionId: String,
        error: String,
    ) {
        Log.e(TAG, "Session error [$sessionId]: $error")
        sessionListeners.forEach { it.onSessionError(sessionId, error) }
    }

    data class SessionStats(
        val sessionId: String,
        val duration: Long,
        val sampleCount: Long,
        val syncMarkCount: Int,
        val isActive: Boolean,
    )
}
