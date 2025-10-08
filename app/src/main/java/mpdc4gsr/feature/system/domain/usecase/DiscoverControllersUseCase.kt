package mpdc4gsr.feature.system.domain.usecase

import kotlinx.coroutines.flow.Flow
import mpdc4gsr.core.data.model.PCControllerInfo
import mpdc4gsr.feature.system.domain.repository.SystemRepository
import javax.inject.Inject

class DiscoverControllersUseCase @Inject constructor(
    private val systemRepository: SystemRepository
) {
    suspend operator fun invoke(): Flow<List<PCControllerInfo>> {
        return systemRepository.discoverControllers()
    }
}
