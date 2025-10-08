package mpdc4gsr.feature.network.domain.usecase

import mpdc4gsr.feature.network.domain.repository.NetworkRepository
import javax.inject.Inject

class ConnectToControllerUseCase @Inject constructor(
    private val repository: NetworkRepository
) {
    suspend operator fun invoke(
        ipAddress: String,
        port: Int,
        useSecure: Boolean = true
    ): Result<Unit> {
        return repository.connectToController(ipAddress, port, useSecure)
    }
}
