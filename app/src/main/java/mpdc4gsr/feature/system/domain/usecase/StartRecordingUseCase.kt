package mpdc4gsr.feature.system.domain.usecase

import mpdc4gsr.feature.system.domain.repository.SystemRepository
import javax.inject.Inject

class StartRecordingUseCase @Inject constructor(
    private val systemRepository: SystemRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return systemRepository.startRecording()
    }
}
