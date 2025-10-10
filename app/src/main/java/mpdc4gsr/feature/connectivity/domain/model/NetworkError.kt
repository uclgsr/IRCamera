package mpdc4gsr.feature.connectivity.domain.model

sealed class NetworkError(open val message: String) {
    data class ConnectionFailed(override val message: String) : NetworkError(message)
    data class DiscoveryFailed(override val message: String) : NetworkError(message)
    data class SendFailed(override val message: String) : NetworkError(message)
    data class AuthenticationFailed(override val message: String) : NetworkError(message)
    data class Timeout(override val message: String) : NetworkError(message)
    data class Unknown(override val message: String) : NetworkError(message)
}

