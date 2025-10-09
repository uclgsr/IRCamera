package mpdc4gsr.feature.main.data.repository

import android.content.Context
import com.mpdc4gsr.gsr.model.SessionInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import mpdc4gsr.core.SessionManager
import mpdc4gsr.core.StructuredLogger
import mpdc4gsr.feature.main.domain.repository.SessionRepository
import com.mpdc4gsr.gsr.service.SessionManager as GSRSessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SessionRepository {

    private val _currentSession = MutableStateFlow<SessionInfo?>(null)
    private val gsrSessionManager: GSRSessionManager by lazy {
        GSRSessionManager.getInstance(context)
    }

    override suspend fun createSession(
        sessionId: String?,
        participantId: String?,
        studyName: String?,
        metadata: Map<String, String>
    ): SessionInfo {
        val session = gsrSessionManager.createSession(
            sessionId = sessionId,
            participantId = participantId,
            studyName = studyName,
            metadata = metadata
        )
        _currentSession.value = session
        return session
    }

    override suspend fun completeSession(sessionId: String) {
        gsrSessionManager.completeSession(sessionId)
        _currentSession.value = null
    }

    override fun getCurrentSession(): Flow<SessionInfo?> = _currentSession
}
