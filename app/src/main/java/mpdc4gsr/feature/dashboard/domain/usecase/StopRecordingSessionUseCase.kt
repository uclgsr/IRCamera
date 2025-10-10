package mpdc4gsr.feature.dashboard.domain.usecase

import mpdc4gsr.feature.dashboard.domain.repository.SessionRepository
import javax.inject.Inject

class StopRecordingSessionUseCase
@Inject
constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(sessionId: String) {
        sessionRepository.completeSession(sessionId)
    }
}

