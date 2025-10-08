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
        return repository.startRecording(sessionId, participantId, studyName)
    }
}
