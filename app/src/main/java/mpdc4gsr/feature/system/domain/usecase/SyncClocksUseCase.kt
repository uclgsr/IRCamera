package mpdc4gsr.feature.system.domain.usecase

import mpdc4gsr.feature.system.domain.repository.SystemRepository
import javax.inject.Inject

class SyncClocksUseCase @Inject constructor(
    private val systemRepository: SystemRepository
) {
    suspend operator fun invoke(controllerId: String): Result<Unit> {
        return systemRepository.syncClocks(controllerId)
    }
}
