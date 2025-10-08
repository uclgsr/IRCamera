package mpdc4gsr.feature.system.domain.usecase

import kotlinx.coroutines.flow.Flow
import mpdc4gsr.core.data.model.PCControllerInfo
import mpdc4gsr.feature.system.domain.repository.SystemRepository

class DiscoverControllersUseCase(
    private val repository: SystemRepository
) {
    suspend operator fun invoke(): Flow<List<PCControllerInfo>> {
        return repository.discoverControllers()
    }
}
