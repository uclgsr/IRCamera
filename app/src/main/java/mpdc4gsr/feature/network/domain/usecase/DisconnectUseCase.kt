package mpdc4gsr.feature.network.domain.usecase

import mpdc4gsr.feature.network.domain.repository.NetworkRepository
import javax.inject.Inject

class DisconnectUseCase @Inject constructor(
    private val repository: NetworkRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.disconnect()
    }
}
