package mpdc4gsr.feature.connectivity.domain.usecase

import mpdc4gsr.feature.connectivity.domain.model.ControllerInfo
import mpdc4gsr.feature.connectivity.domain.repository.NetworkRepository
import javax.inject.Inject

class DiscoverControllersUseCase @Inject constructor(
    private val repository: NetworkRepository
) {
    suspend operator fun invoke(): Result<List<ControllerInfo>> {
        return repository.discoverControllers()
    }
}

