package mpdc4gsr.feature.main.domain.repository

import kotlinx.coroutines.flow.Flow
import mpdc4gsr.core.session.SessionInfo

interface SessionRepository {
    suspend fun createSession(
        sessionId: String?,
        participantId: String?,
        studyName: String?,
        metadata: Map<String, String>,
    ): SessionInfo

    suspend fun completeSession(sessionId: String)

    fun getCurrentSession(): Flow<SessionInfo?>
}
