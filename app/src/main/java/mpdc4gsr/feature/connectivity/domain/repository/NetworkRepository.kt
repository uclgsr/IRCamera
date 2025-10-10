package mpdc4gsr.feature.connectivity.domain.repository

import kotlinx.coroutines.flow.Flow
import mpdc4gsr.feature.connectivity.domain.model.ConnectionState
import mpdc4gsr.feature.connectivity.domain.model.ControllerInfo
import org.json.JSONObject

interface NetworkRepository {
    suspend fun discoverControllers(): Result<List<ControllerInfo>>
    suspend fun connectToController(
        ipAddress: String,
        port: Int,
        useSecure: Boolean
    ): Result<Unit>

    suspend fun disconnect(): Result<Unit>
    suspend fun sendMessage(message: JSONObject): Result<Unit>
    suspend fun sendMeasurementData(sessionId: String, data: JSONObject): Result<Unit>
    fun observeConnectionState(): Flow<ConnectionState>
    fun observeDiscoveredControllers(): Flow<List<ControllerInfo>>
    fun isConnected(): Boolean
    fun isSecureConnection(): Boolean
}

