package mpdc4gsr.feature.main.domain.repository

import kotlinx.coroutines.flow.Flow
import mpdc4gsr.feature.network.data.NetworkClient

interface NetworkRepository {
    suspend fun startServer(): Boolean
    suspend fun stopServer()
    suspend fun discoverControllers(): List<NetworkClient.ControllerInfo>
    suspend fun connectToController(ipAddress: String, port: Int): Boolean
    fun getConnectionState(): Flow<NetworkConnectionState>
    fun getConnectedController(): Flow<NetworkClient.ControllerInfo?>
}

enum class NetworkConnectionState {
    DISCONNECTED, DISCOVERING, CONNECTING, CONNECTED, ERROR
}
