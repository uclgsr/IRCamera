package mpdc4gsr.feature.main.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import mpdc4gsr.feature.main.domain.repository.NetworkConnectionState
import mpdc4gsr.feature.main.domain.repository.NetworkRepository
import mpdc4gsr.feature.network.data.NetworkClient
import mpdc4gsr.feature.network.data.NetworkController
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkRepositoryImpl
@Inject
constructor(
    @ApplicationContext private val context: Context,
) : NetworkRepository {
    private val _connectionState = MutableStateFlow(NetworkConnectionState.DISCONNECTED)
    private val _connectedController = MutableStateFlow<NetworkClient.ControllerInfo?>(null)

    private val networkClient: NetworkClient by lazy { NetworkClient(context) }
    private val networkController: NetworkController by lazy { NetworkController(context) }

    override suspend fun startServer(): Boolean {
        val started = networkController.start() ?: false
        if (started) {
            _connectionState.value = NetworkConnectionState.CONNECTED
        }
        return started
    }

    override suspend fun stopServer() {
        networkController.stop()
        _connectionState.value = NetworkConnectionState.DISCONNECTED
        _connectedController.value = null
    }

    override suspend fun discoverControllers(): List<NetworkClient.ControllerInfo> {
        _connectionState.value = NetworkConnectionState.DISCOVERING
        val controllers = networkClient.discoverControllers()
        if (controllers.isEmpty()) {
            _connectionState.value = NetworkConnectionState.DISCONNECTED
            _connectedController.value = null
        } else {
            // Surface the most relevant controller for quick-connect scenarios
            _connectedController.value = controllers.maxByOrNull { it.lastSeen }
        }
        return controllers
    }

    override suspend fun connectToController(
        ipAddress: String,
        port: Int,
    ): Boolean {
        _connectionState.value = NetworkConnectionState.CONNECTING
        val connected = networkClient.connectToController(ipAddress, port)
        if (connected) {
            val info =
                networkClient
                    .getDiscoveredControllers()
                    .firstOrNull { it.ipAddress == ipAddress && it.port == port }
                    ?: NetworkClient.ControllerInfo(
                        ipAddress = ipAddress,
                        port = port,
                        deviceName = "Controller @ $ipAddress",
                        capabilities = emptyList(),
                    )
            _connectedController.value = info.copy(lastSeen = System.currentTimeMillis())
            _connectionState.value = NetworkConnectionState.CONNECTED
        } else {
            _connectedController.value = null
            _connectionState.value = NetworkConnectionState.ERROR
        }
        return connected
    }

    override fun getConnectionState(): Flow<NetworkConnectionState> = _connectionState

    override fun getConnectedController(): Flow<NetworkClient.ControllerInfo?> = _connectedController
}
