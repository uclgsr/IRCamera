package mpdc4gsr.feature.network.data.datasource

import mpdc4gsr.feature.network.data.NetworkClient
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkDataSourceImpl @Inject constructor(
    private val networkClient: NetworkClient
) : NetworkDataSource {

    override suspend fun discoverControllers(): List<NetworkClient.ControllerInfo> {
        return networkClient.discoverControllers()
    }

    override suspend fun connectToController(
        ipAddress: String,
        port: Int,
        useSecure: Boolean
    ): Boolean {
        return networkClient.connectToController(ipAddress, port, useSecure)
    }

    override suspend fun disconnect() {
        networkClient.disconnect()
    }

    override suspend fun sendMessage(message: JSONObject): Boolean {
        return networkClient.sendMessage(message)
    }

    override suspend fun sendMeasurementData(sessionId: String, data: JSONObject): Boolean {
        return networkClient.sendMeasurementData(sessionId, data)
    }

    override fun getDiscoveredControllers(): List<NetworkClient.ControllerInfo> {
        return networkClient.getDiscoveredControllers()
    }

    override fun isConnected(): Boolean {
        return networkClient.isConnected()
    }

    override fun isSecureConnection(): Boolean {
        return networkClient.isSecureConnection()
    }

    override fun setEventListener(listener: NetworkClient.NetworkEventListener?) {
        networkClient.setEventListener(listener)
    }
}
