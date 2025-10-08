package mpdc4gsr.feature.system.domain.usecase

import mpdc4gsr.feature.system.domain.repository.SystemRepository

class StopRecordingUseCase(
    private val repository: SystemRepository
) {
    suspend operator fun invoke(): Result<Map<String, Boolean>> {
        if (!repository.isRecording()) {
            return Result.failure(IllegalStateException("No recording in progress"))
        }
        return repository.stopRecording()
    }
}
