package com.mpdc4gsr.gsr.service

import android.content.Context
import android.util.Log
import com.mpdc4gsr.gsr.model.SessionInfo
import com.mpdc4gsr.gsr.util.TimeUtils
import java.util.concurrent.ConcurrentHashMap

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

    fun createSession(
        sessionId: String? = null,
        participantId: String? = null,
        studyName: String? = null,
        metadata: Map<String, String> = emptyMap(),
    ): SessionInfo {
        val finalSessionId = sessionId ?: TimeUtils.generateSessionId("MultiModal")
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

    fun getSession(sessionId: String): SessionInfo? {
        return activeSessions[sessionId]
    }

    fun getActiveSessions(): List<SessionInfo> {
        return activeSessions.values.toList()
    }

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

    fun completeSession(sessionId: String): SessionInfo? {
        val session = activeSessions.remove(sessionId) ?: return null
        session.endTime = System.currentTimeMillis()
        sessionListeners.forEach { it.onSessionCompleted(session) }
        Log.i(TAG, "Session completed: $sessionId, duration: ${session.getDurationMs()}ms")
        return session
    }

    fun completeAllSessions(): List<SessionInfo> {
        val completed = mutableListOf<SessionInfo>()
        activeSessions.keys.forEach { sessionId ->
            completeSession(sessionId)?.let { completed.add(it) }
        }
        return completed
    }

    fun isSessionActive(sessionId: String): Boolean {
        return activeSessions.containsKey(sessionId)
    }

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
