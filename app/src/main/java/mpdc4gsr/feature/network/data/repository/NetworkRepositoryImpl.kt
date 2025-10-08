package mpdc4gsr.feature.network.data.repository

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import mpdc4gsr.feature.network.data.NetworkClient
import mpdc4gsr.feature.network.data.datasource.NetworkDataSource
import mpdc4gsr.feature.network.domain.model.ConnectionState
import mpdc4gsr.feature.network.domain.model.ControllerInfo
import mpdc4gsr.feature.network.domain.model.NetworkError
import mpdc4gsr.feature.network.domain.repository.NetworkRepository
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkRepositoryImpl @Inject constructor(
    private val dataSource: NetworkDataSource
) : NetworkRepository {

    override suspend fun discoverControllers(): Result<List<ControllerInfo>> {
        return try {
            val controllers = dataSource.discoverControllers()
            Result.success(controllers.map { it.toDomainModel() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun connectToController(
        ipAddress: String,
        port: Int,
        useSecure: Boolean
    ): Result<Unit> {
        return try {
            val success = dataSource.connectToController(ipAddress, port, useSecure)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Connection failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun disconnect(): Result<Unit> {
        return try {
            dataSource.disconnect()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendMessage(message: JSONObject): Result<Unit> {
        return try {
            val success = dataSource.sendMessage(message)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Send message failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendMeasurementData(sessionId: String, data: JSONObject): Result<Unit> {
        return try {
            val success = dataSource.sendMeasurementData(sessionId, data)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Send measurement data failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeConnectionState(): Flow<ConnectionState> = callbackFlow {
        val listener = object : NetworkClient.NetworkEventListener {
            override fun onControllerDiscovered(controller: NetworkClient.ControllerInfo) {}

            override fun onConnected(controller: NetworkClient.ControllerInfo) {
                trySend(ConnectionState.Connected(controller.toDomainModel()))
            }

            override fun onDisconnected(reason: String) {
                trySend(ConnectionState.Disconnected)
            }

            override fun onRemoteMeasurementRequest(sessionInfo: com.mpdc4gsr.gsr.model.SessionInfo) {}

            override fun onSyncFlash(durationMs: Int) {}

            override fun onTimeSynchronized(offsetNanoseconds: Long) {}

            override fun onDataStreamingStarted() {}

            override fun onDataStreamingStopped() {}

            override fun onError(operation: String, error: String) {
                trySend(ConnectionState.Error(NetworkError.Unknown(error)))
            }
        }

        networkClient.setEventListener(listener)
        
        awaitClose {
            networkClient.setEventListener(null)
        }
    }

    override fun observeDiscoveredControllers(): Flow<List<ControllerInfo>> = callbackFlow {
        val listener = object : NetworkClient.NetworkEventListener {
            override fun onControllerDiscovered(controller: NetworkClient.ControllerInfo) {
                trySend(dataSource.getDiscoveredControllers().map { it.toDomainModel() })
            }

            override fun onConnected(controller: NetworkClient.ControllerInfo) {}
            override fun onDisconnected(reason: String) {}
            override fun onRemoteMeasurementRequest(sessionInfo: com.mpdc4gsr.gsr.model.SessionInfo) {}
            override fun onSyncFlash(durationMs: Int) {}
            override fun onTimeSynchronized(offsetNanoseconds: Long) {}
            override fun onDataStreamingStarted() {}
            override fun onDataStreamingStopped() {}
            override fun onError(operation: String, error: String) {}
        }

        dataSource.setEventListener(listener)
        
        awaitClose {
            dataSource.setEventListener(null)
        }
    }

    override fun isConnected(): Boolean = dataSource.isConnected()

    override fun isSecureConnection(): Boolean = dataSource.isSecureConnection()

    private fun NetworkClient.ControllerInfo.toDomainModel() = ControllerInfo(
        ipAddress = ipAddress,
        port = port,
        deviceName = deviceName,
        capabilities = capabilities,
        lastSeen = lastSeen
    )
}
