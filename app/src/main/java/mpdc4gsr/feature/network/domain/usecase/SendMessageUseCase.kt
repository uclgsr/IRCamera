package mpdc4gsr.feature.network.domain.usecase

import mpdc4gsr.feature.network.domain.repository.NetworkRepository
import org.json.JSONObject
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val repository: NetworkRepository
) {
    suspend operator fun invoke(message: JSONObject): Result<Unit> {
        return repository.sendMessage(message)
    }
}
