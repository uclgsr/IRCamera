package mpdc4gsr.feature.main.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import mpdc4gsr.feature.main.domain.repository.NetworkConnectionState
import mpdc4gsr.feature.main.domain.repository.NetworkRepository
import mpdc4gsr.feature.network.data.NetworkClient
import mpdc4gsr.feature.network.data.NetworkController
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkRepository {

    private val _connectionState = MutableStateFlow(NetworkConnectionState.DISCONNECTED)
    private val _connectedController = MutableStateFlow<NetworkClient.ControllerInfo?>(null)

    private val networkClient: NetworkClient by lazy { NetworkClient(context) }
    private val networkController: NetworkController by lazy { NetworkController(context) }

    override suspend fun startServer(): Boolean {
        return networkController.start() ?: false
    }

    override suspend fun stopServer() {
        networkController.stop()
    }

    override suspend fun discoverControllers(): List<NetworkClient.ControllerInfo> {
        _connectionState.value = NetworkConnectionState.DISCOVERING
        return networkClient.discoverControllers()
    }

    override suspend fun connectToController(ipAddress: String, port: Int): Boolean {
        _connectionState.value = NetworkConnectionState.CONNECTING
        val connected = networkClient.connectToController(ipAddress, port)
        if (connected) {
            _connectionState.value = NetworkConnectionState.CONNECTED
        } else {
            _connectionState.value = NetworkConnectionState.ERROR
        }
        return connected
    }

    override fun getConnectionState(): Flow<NetworkConnectionState> = _connectionState

    override fun getConnectedController(): Flow<NetworkClient.ControllerInfo?> = _connectedController
}
