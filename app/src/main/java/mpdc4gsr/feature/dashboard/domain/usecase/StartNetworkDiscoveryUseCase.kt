package mpdc4gsr.feature.dashboard.domain.usecase

import mpdc4gsr.feature.dashboard.domain.repository.NetworkRepository
import mpdc4gsr.feature.connectivity.data.NetworkClient
import javax.inject.Inject

class StartNetworkDiscoveryUseCase
@Inject
constructor(
    private val networkRepository: NetworkRepository,
) {
    suspend operator fun invoke(): List<NetworkClient.ControllerInfo> = networkRepository.discoverControllers()
}

