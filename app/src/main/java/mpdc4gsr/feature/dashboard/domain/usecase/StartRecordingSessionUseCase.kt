package mpdc4gsr.feature.dashboard.domain.usecase

import mpdc4gsr.core.recording.session.SessionInfo
import mpdc4gsr.feature.dashboard.domain.repository.SessionRepository
import javax.inject.Inject

class StartRecordingSessionUseCase
@Inject
constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(
        sessionId: String? = null,
        participantId: String? = null,
        studyName: String? = null,
        metadata: Map<String, String> = emptyMap(),
    ): SessionInfo =
        sessionRepository.createSession(
            sessionId = sessionId,
            participantId = participantId,
            studyName = studyName,
            metadata = metadata,
        )
}

