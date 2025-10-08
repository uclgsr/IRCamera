package mpdc4gsr.feature.network.domain.usecase

import kotlinx.coroutines.flow.Flow
import mpdc4gsr.feature.network.domain.model.ConnectionState
import mpdc4gsr.feature.network.domain.repository.NetworkRepository
import javax.inject.Inject

class ObserveConnectionStateUseCase @Inject constructor(
    private val repository: NetworkRepository
) {
    operator fun invoke(): Flow<ConnectionState> {
        return repository.observeConnectionState()
    }
}
