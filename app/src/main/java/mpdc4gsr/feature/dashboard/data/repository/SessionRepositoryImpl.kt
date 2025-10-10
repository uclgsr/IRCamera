package mpdc4gsr.feature.dashboard.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import mpdc4gsr.core.recording.session.SessionInfo
import mpdc4gsr.core.recording.session.SessionManager
import mpdc4gsr.feature.dashboard.domain.repository.SessionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepositoryImpl
@Inject
constructor(
    @ApplicationContext private val context: Context,
) : SessionRepository {
    private val _currentSession = MutableStateFlow<SessionInfo?>(null)
    private val sessionManager: SessionManager by lazy {
        sessionManager.getInstance(context)
    }

    override suspend fun createSession(
        sessionId: String?,
        participantId: String?,
        studyName: String?,
        metadata: Map<String, String>,
    ): SessionInfo {
        val session =
            sessionManager.createSession(
                sessionId = sessionId,
                participantId = participantId,
                studyName = studyName,
                metadata = metadata,
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

