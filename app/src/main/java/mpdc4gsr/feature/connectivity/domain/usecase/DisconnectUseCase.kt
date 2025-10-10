package mpdc4gsr.feature.connectivity.domain.usecase

import mpdc4gsr.feature.connectivity.domain.repository.NetworkRepository
import javax.inject.Inject

class DisconnectUseCase @Inject constructor(
    private val repository: NetworkRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.disconnect()
    }
}

