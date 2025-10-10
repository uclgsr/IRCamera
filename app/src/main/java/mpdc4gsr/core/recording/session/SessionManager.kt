package mpdc4gsr.core.recording.session

import android.content.Context
import java.util.concurrent.ConcurrentHashMap

class SessionManager private constructor(
    context: Context,
) {
    companion object {
        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
    }

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
        return session
    }

    fun getSession(sessionId: String): SessionInfo? = activeSessions[sessionId]

    fun getActiveSessions(): List<SessionInfo> = activeSessions.values.toList()

    fun updateSession(
        sessionId: String,
        updates: (SessionInfo) -> Unit,
    ): Boolean {
        val session = activeSessions[sessionId] ?: return false
        updates(session)
        sessionListeners.forEach { it.onSessionUpdated(session) }
        return true
    }

    fun completeSession(sessionId: String): SessionInfo? {
        val session = activeSessions.remove(sessionId) ?: return null
        session.endTime = System.currentTimeMillis()
        sessionListeners.forEach { it.onSessionCompleted(session) }
        return session
    }

    fun completeAllSessions(): List<SessionInfo> {
        val completed = mutableListOf<SessionInfo>()
        activeSessions.keys.forEach { sessionId ->
            completeSession(sessionId)?.let { completed.add(it) }
        }
        return completed
    }

    fun isSessionActive(sessionId: String): Boolean = activeSessions.containsKey(sessionId)

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
        sessionListeners.forEach { it.onSessionError(sessionId, error) }
    }

    fun addSyncMark(
        sessionId: String,
        syncMark: SyncMark,
    ) {
        val session = activeSessions[sessionId] ?: return
        session.syncMarks.add(syncMark)
        sessionListeners.forEach { it.onSessionUpdated(session) }
    }

    data class SessionStats(
        val sessionId: String,
        val duration: Long,
        val sampleCount: Long,
        val syncMarkCount: Int,
        val isActive: Boolean,
    )
}
