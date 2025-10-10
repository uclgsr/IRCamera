package mpdc4gsr.feature.connectivity.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import mpdc4gsr.core.common.telemetry.TelemetryManager
import mpdc4gsr.core.recording.session.SessionInfo
import mpdc4gsr.feature.connectivity.data.NetworkClient
import mpdc4gsr.feature.connectivity.data.datasource.NetworkDataSource
import mpdc4gsr.feature.connectivity.domain.model.ConnectionState
import mpdc4gsr.feature.connectivity.domain.model.ControllerInfo
import mpdc4gsr.feature.connectivity.domain.model.NetworkError
import mpdc4gsr.feature.connectivity.domain.repository.NetworkRepository
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkRepositoryImpl @Inject constructor(
    private val dataSource: NetworkDataSource
) : NetworkRepository {

    private val _connectionState = MutableSharedFlow<ConnectionState>(replay = 1)
    private val _discoveredControllers = MutableSharedFlow<List<ControllerInfo>>(replay = 1)

    init {
        setupEventListener()
    }


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


    private fun setupEventListener() {
        val listener = object : NetworkClient.NetworkEventListener {
            override fun onControllerDiscovered(controller: NetworkClient.ControllerInfo) {
                _discoveredControllers.tryEmit(dataSource.getDiscoveredControllers().map { it.toDomainModel() })
            }


            override fun onConnected(controller: NetworkClient.ControllerInfo) {
                _connectionState.tryEmit(ConnectionState.Connected(controller.toDomainModel()))
            }


            override fun onDisconnected(reason: String) {
                _connectionState.tryEmit(ConnectionState.Disconnected)
            }


            override fun onRemoteMeasurementRequest(sessionInfo: SessionInfo) {
                TelemetryManager.trackEvent(
                    "remote_measurement_request",
                    mapOf("session_id" to sessionInfo.sessionId),
                )
            }


            override fun onSyncFlash(durationMs: Int) {
                TelemetryManager.trackEvent(
                    "sync_flash_requested",
                    mapOf("duration_ms" to durationMs),
                )
            }


            override fun onTimeSynchronized(offsetNanoseconds: Long) {
                TelemetryManager.setProperty("network_time_offset_ns", offsetNanoseconds.toString())
            }


            override fun onDataStreamingStarted() {
                TelemetryManager.trackEvent("data_streaming_started")
            }


            override fun onDataStreamingStopped() {
                TelemetryManager.trackEvent("data_streaming_stopped")
            }


            override fun onError(operation: String, error: String) {
                _connectionState.tryEmit(ConnectionState.Error(NetworkError.Unknown(error)))
            }
        }


        dataSource.setEventListener(listener)
    }


    override fun observeConnectionState(): Flow<ConnectionState> = _connectionState.asSharedFlow()

    override fun observeDiscoveredControllers(): Flow<List<ControllerInfo>> = _discoveredControllers.asSharedFlow()

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

