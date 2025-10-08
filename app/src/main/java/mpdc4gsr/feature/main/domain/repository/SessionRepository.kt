package mpdc4gsr.feature.main.domain.repository

import com.mpdc4gsr.gsr.model.SessionInfo
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    suspend fun createSession(
        sessionId: String?,
        participantId: String?,
        studyName: String?,
        metadata: Map<String, String>
    ): SessionInfo
    
    suspend fun completeSession(sessionId: String)
    
    fun getCurrentSession(): Flow<SessionInfo?>
}
