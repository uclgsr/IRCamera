package mpdc4gsr.feature.network.domain.model

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data class Connected(val controllerInfo: ControllerInfo) : ConnectionState()
    data class Error(val error: NetworkError) : ConnectionState()
}
