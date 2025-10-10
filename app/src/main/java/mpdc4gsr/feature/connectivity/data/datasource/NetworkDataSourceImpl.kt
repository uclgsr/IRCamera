package mpdc4gsr.feature.connectivity.data.datasource

import mpdc4gsr.feature.connectivity.data.NetworkClient
import org.json.JSONObject

class NetworkDataSourceImpl(
    private val networkClient: NetworkClient,
) : NetworkDataSource {

    override suspend fun discoverControllers(): List<NetworkClient.ControllerInfo> =
        networkClient.discoverControllers()

    override suspend fun connectToController(
        ipAddress: String,
        port: Int,
        useSecure: Boolean,
    ): Boolean = networkClient.connectToController(ipAddress, port, useSecure)

    override suspend fun disconnect() {
        networkClient.disconnect()
    }

    override suspend fun sendMessage(message: JSONObject): Boolean =
        networkClient.sendMessage(message)

    override suspend fun sendMeasurementData(
        sessionId: String,
        data: JSONObject,
    ): Boolean = networkClient.sendMeasurementData(sessionId, data)

    override fun getDiscoveredControllers(): List<NetworkClient.ControllerInfo> =
        networkClient.getDiscoveredControllers()

    override fun setEventListener(listener: NetworkClient.NetworkEventListener) {
        networkClient.setEventListener(listener)
    }

    override fun isConnected(): Boolean = networkClient.isConnected()

    override fun isSecureConnection(): Boolean = networkClient.isSecureConnection()
}

