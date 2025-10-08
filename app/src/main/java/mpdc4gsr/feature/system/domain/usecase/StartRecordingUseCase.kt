package mpdc4gsr.feature.system.domain.usecase

import mpdc4gsr.feature.system.domain.repository.SystemRepository

class StartRecordingUseCase(
    private val repository: SystemRepository
) {
    suspend operator fun invoke(
        sessionId: String,
        participantId: String? = null,
        studyName: String? = null
    ): Result<Boolean> {
        if (sessionId.isBlank()) {
            return Result.failure(IllegalArgumentException("Session ID cannot be empty"))
        }
        return repository.startRecording(sessionId, participantId, studyName)
    }
}
