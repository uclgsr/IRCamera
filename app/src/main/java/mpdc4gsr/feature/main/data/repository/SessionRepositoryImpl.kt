package mpdc4gsr.feature.main.data.repository

import android.content.Context
import mpdc4gsr.core.session.SessionInfo
import mpdc4gsr.core.session.SessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import mpdc4gsr.feature.main.domain.repository.SessionRepository

@Singleton
class SessionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SessionRepository {

    private val _currentSession = MutableStateFlow<SessionInfo?>(null)
    private val sessionManager: SessionManager by lazy {
        sessionManager.getInstance(context)
    }

    override suspend fun createSession(
        sessionId: String?,
        participantId: String?,
        studyName: String?,
        metadata: Map<String, String>
    ): SessionInfo {
        val session = sessionManager.createSession(
            sessionId = sessionId,
            participantId = participantId,
            studyName = studyName,
            metadata = metadata
        )
        _currentSession.value = session
        return session
    }

    override suspend fun completeSession(sessionId: String) {
        sessionManager.completeSession(sessionId)
        _currentSession.value = null
    }

    override fun getCurrentSession(): Flow<SessionInfo?> = _currentSession
}
