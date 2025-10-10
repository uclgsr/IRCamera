package mpdc4gsr.feature.main.domain.usecase

import mpdc4gsr.feature.main.domain.repository.NetworkRepository
import mpdc4gsr.feature.network.data.NetworkClient
import javax.inject.Inject

class StartNetworkDiscoveryUseCase
@Inject
constructor(
    private val networkRepository: NetworkRepository,
) {
    suspend operator fun invoke(): List<NetworkClient.ControllerInfo> = networkRepository.discoverControllers()
}
