package mpdc4gsr.feature.network.data.datasource

import mpdc4gsr.feature.network.data.NetworkClient
import org.json.JSONObject

interface NetworkDataSource {
    suspend fun discoverControllers(): List<NetworkClient.ControllerInfo>
    suspend fun connectToController(ipAddress: String, port: Int, useSecure: Boolean): Boolean
    suspend fun disconnect()
    suspend fun sendMessage(message: JSONObject): Boolean
    suspend fun sendMeasurementData(sessionId: String, data: JSONObject): Boolean
    fun getDiscoveredControllers(): List<NetworkClient.ControllerInfo>
    fun isConnected(): Boolean
    fun isSecureConnection(): Boolean
    fun setEventListener(listener: NetworkClient.NetworkEventListener?)
}
