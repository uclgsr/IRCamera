package mpdc4gsr.feature.system.domain.usecase

import mpdc4gsr.feature.system.domain.repository.SystemRepository

class SyncClocksUseCase(
    private val repository: SystemRepository
) {
    suspend operator fun invoke(): Result<Long> {
        return repository.syncClocks()
    }
}
