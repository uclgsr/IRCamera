package com.topdon.tc001.network

import android.content.Context
import android.util.Log
import com.topdon.tc001.controller.RecordingController
import com.topdon.tc001.utils.TimeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class EnhancedNetworkClient(
    private val context: Context,
    private val recordingController: RecordingController,
) {
    companion object {
        private const val TAG = "EnhancedNetworkClient"
        private const val PC_CONTROLLER_PORT = 8080
        private const val TIME_SYNC_PORT = 8082
        private const val FILE_TRANSFER_PORT = 8083
        private const val CONNECTION_TIMEOUT_MS = 10000L
        private const val HEARTBEAT_INTERVAL_MS = 5000L
        private const val STATUS_REPORT_INTERVAL_MS = 2000L
    }

    private var socket: Socket? = null
    private var outputStream: DataOutputStream? = null
    private var inputStream: DataInputStream? = null
    private val isConnected = AtomicBoolean(false)

    private val networkScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val timeManager = TimeManager.getInstance(context)

    private var connectedControllerInfo: NetworkClient.ControllerInfo? = null
    private var deviceId: String =
        android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID,
        )

    private val _connectionStateFlow = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionStateFlow: StateFlow<ConnectionState> = _connectionStateFlow.asStateFlow()

    private val _messageFlow = MutableSharedFlow<NetworkMessage>()
    val messageFlow: SharedFlow<NetworkMessage> = _messageFlow.asSharedFlow()

    private var heartbeatJob: Job? = null
    private var statusReportJob: Job? = null
    private var messageListenerJob: Job? = null

    suspend fun connectToController(
        ipAddress: String,
        port: Int = PC_CONTROLLER_PORT,
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (isConnected.get()) {
                    disconnect()
                }

                Log.i(TAG, "Connecting to PC Controller at $ipAddress:$port")
                _connectionStateFlow.value = ConnectionState.CONNECTING

                socket =
                    Socket().apply {
                        connect(InetSocketAddress(ipAddress, port), CONNECTION_TIMEOUT_MS.toInt())
                        soTimeout = 30000 // 30 second read timeout
                    }

                outputStream = DataOutputStream(socket!!.getOutputStream())
                inputStream = DataInputStream(socket!!.getInputStream())

                val registrationSuccess = registerEnhancedDevice()
                if (!registrationSuccess) {
                    Log.e(TAG, "Device registration failed")
                    disconnect()
                    return@withContext false
                }

                val timeSyncSuccess = timeManager.synchronizeWithPC(ipAddress, TIME_SYNC_PORT)
                if (!timeSyncSuccess) {
                    Log.w(TAG, "Time synchronization failed, continuing with local time")
                }

                isConnected.set(true)
                _connectionStateFlow.value = ConnectionState.CONNECTED

                connectedControllerInfo =
                    NetworkClient.ControllerInfo(
                        ipAddress = ipAddress,
                        port = port,
                        deviceName = "PC Controller",
                        capabilities = listOf("hub", "aggregation", "sync"),
                    )

                startMessageListener()
                startHeartbeat()
                startStatusReporting()

                Log.i(TAG, "Successfully connected to PC Controller")
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to PC Controller", e)
                _connectionStateFlow.value = ConnectionState.ERROR
                disconnect()
                return@withContext false
            }
        }
    }

    suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Disconnecting from PC Controller")

                isConnected.set(false)
                _connectionStateFlow.value = ConnectionState.DISCONNECTING

                heartbeatJob?.cancel()
                statusReportJob?.cancel()
                messageListenerJob?.cancel()

                if (outputStream != null) {
                    try {
                        val disconnectMessage = createMessage("device_disconnect")
                        sendMessage(disconnectMessage)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to send disconnect message", e)
                    }
                }

                outputStream?.close()
                inputStream?.close()
                socket?.close()

                outputStream = null
                inputStream = null
                socket = null
                connectedControllerInfo = null

                _connectionStateFlow.value = ConnectionState.DISCONNECTED
                Log.i(TAG, "Disconnected from PC Controller")
            } catch (e: Exception) {
                Log.e(TAG, "Error during disconnect", e)
            }
        }
    }

    suspend fun startCoordinatedSession(sessionDirectory: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!isConnected.get()) {
                    Log.e(TAG, "Not connected to PC Controller")
                    return@withContext false
                }

                Log.i(TAG, "Starting coordinated recording session")

                val sessionStartMessage =
                    createMessage("session_start_request").apply {
                        put("session_directory", sessionDirectory)
                        put("device_capabilities", getDeviceCapabilities())
                        put("time_sync_quality", timeManager.getSyncQuality().level.name)
                    }

                sendMessage(sessionStartMessage)

                val response = receiveMessageWithTimeout(10000L)
                if (response?.optString("message_type") != "session_start_confirmed") {
                    Log.e(TAG, "PC Controller did not confirm session start")
                    return@withContext false
                }

                val recordingSuccess = recordingController.startRecording(sessionDirectory)
                if (!recordingSuccess) {
                    Log.e(TAG, "Failed to start local recording")

                    val failureMessage =
                        createMessage("session_start_failed").apply {
                            put("reason", "Local recording failed to start")
                        }
                    sendMessage(failureMessage)
                    return@withContext false
                }

                val confirmMessage = createMessage("session_started")
                sendMessage(confirmMessage)

                Log.i(TAG, "Coordinated recording session started successfully")
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start coordinated session", e)
                return@withContext false
            }
        }
    }

    suspend fun stopCoordinatedSession(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!isConnected.get()) {
                    Log.w(TAG, "Not connected to PC Controller, stopping local recording only")
                    return@withContext recordingController.stopRecording()
                }

                Log.i(TAG, "Stopping coordinated recording session")

                val finalSyncTimestamp = timeManager.getCurrentTimestampNs()
                recordingController.addSyncMarker("session_end", finalSyncTimestamp)

                val sessionStopMessage =
                    createMessage("session_stop_request").apply {
                        put("final_sync_timestamp", finalSyncTimestamp)
                        put("session_stats", getSessionStatistics())
                    }

                sendMessage(sessionStopMessage)

                val recordingSuccess = recordingController.stopRecording()

                val completionMessage =
                    createMessage("session_stopped").apply {
                        put("success", recordingSuccess)
                        put("final_stats", getSessionStatistics())
                    }
                sendMessage(completionMessage)

                Log.i(TAG, "Coordinated recording session stopped")
                return@withContext recordingSuccess
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop coordinated session", e)
                return@withContext recordingController.stopRecording()
            }
        }
    }

    suspend fun distributeSyncMarker(
        markerType: String,
        metadata: Map<String, String> = emptyMap(),
    ) {
        networkScope.launch {
            try {
                val syncTimestamp = timeManager.getCurrentTimestampNs()

                recordingController.addSyncMarker(markerType, syncTimestamp, metadata)

                if (isConnected.get()) {
                    val syncMessage =
                        createMessage("sync_marker").apply {
                            put("marker_type", markerType)
                            put("timestamp_ns", syncTimestamp)
                            put("metadata", JSONObject(metadata))
                            put("source_device", deviceId)
                        }

                    sendMessage(syncMessage)
                    Log.i(TAG, "Sync marker distributed: $markerType")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to distribute sync marker", e)
            }
        }
    }

    private suspend fun registerEnhancedDevice(): Boolean {
        return try {
            val registrationMessage =
                createMessage("enhanced_device_register").apply {
                    put("device_type", "android_sensor_node")
                    put("device_capabilities", getDeviceCapabilities())
                    put("api_version", "2.0")
                    put("recording_controller_version", "1.0")
                    put("time_sync_capable", true)
                    put("available_sensors", getAvailableSensors())
                }

            sendMessage(registrationMessage)

            val response = receiveMessageWithTimeout(5000L)
            response?.optString("message_type") == "enhanced_registration_ack"
        } catch (e: Exception) {
            Log.e(TAG, "Enhanced device registration failed", e)
            false
        }
    }

    private fun startMessageListener() {
        messageListenerJob =
            networkScope.launch {
                while (isConnected.get() && isActive) {
                    try {
                        val message = receiveMessageWithTimeout(1000L)
                        message?.let {
                            handleIncomingMessage(it)
                            _messageFlow.emit(NetworkMessage.fromJSON(it))
                        }
                    } catch (e: Exception) {
                        if (isConnected.get()) {
                            Log.e(TAG, "Message listener error", e)
                        }
                        break
                    }
                }
            }
    }

    private fun startHeartbeat() {
        heartbeatJob =
            networkScope.launch {
                while (isConnected.get() && isActive) {
                    try {
                        val heartbeatMessage =
                            createMessage("enhanced_heartbeat").apply {
                                put("recording_active", recordingController.isRecording)
                                put("time_sync_quality", timeManager.getSyncQuality().level.name)
                                put("device_status", "operational")
                            }

                        sendMessage(heartbeatMessage)
                        delay(HEARTBEAT_INTERVAL_MS)
                    } catch (e: Exception) {
                        if (isConnected.get()) {
                            Log.e(TAG, "Heartbeat failed", e)
                        }
                        break
                    }
                }
            }
    }

    private fun startStatusReporting() {
        statusReportJob =
            networkScope.launch {
                while (isConnected.get() && isActive) {
                    try {
                        if (recordingController.isRecording) {
                            val statusMessage =
                                createMessage("recording_status").apply {
                                    put("session_stats", getSessionStatistics())
                                    put("sensor_status", getSensorStatusArray())
                                    put(
                                        "sync_events",
                                        recordingController.syncEventFlow.replayCache.size
                                    )
                                }

                            sendMessage(statusMessage)
                        }

                        delay(STATUS_REPORT_INTERVAL_MS)
                    } catch (e: Exception) {
                        if (isConnected.get()) {
                            Log.w(TAG, "Status reporting error", e)
                        }
                    }
                }
            }
    }

    private suspend fun handleIncomingMessage(message: JSONObject) {
        when (message.optString("message_type")) {
            "session_start_command" -> {
                val sessionDirectory = message.optString("session_directory")
                if (sessionDirectory.isNotEmpty()) {
                    Log.i(TAG, "Received session start command from PC Controller")
                    recordingController.startRecording(sessionDirectory)
                }
            }

            "session_stop_command" -> {
                Log.i(TAG, "Received session stop command from PC Controller")
                recordingController.stopRecording()
            }

            "sync_marker_command" -> {
                val markerType = message.optString("marker_type")
                val timestampNs = message.optLong("timestamp_ns")
                val metadata =
                    message.optJSONObject("metadata")?.let { json ->
                        mutableMapOf<String, String>().apply {
                            json.keys().forEach { key ->
                                put(key, json.optString(key))
                            }
                        }
                    } ?: emptyMap()

                recordingController.addSyncMarker(markerType, timestampNs, metadata)
                Log.i(TAG, "Applied sync marker from PC Controller: $markerType")
            }

            "time_sync_request" -> {

                val syncResult = timeManager.getSyncQuality()
                val response =
                    createMessage("time_sync_response").apply {
                        put("sync_quality", syncResult.level.name)
                        put("offset_ns", syncResult.offsetNs)
                        put("quality_ms", syncResult.qualityMs)
                    }
                sendMessage(response)
            }

            "ping" -> {
                val pongMessage = createMessage("pong")
                sendMessage(pongMessage)
            }

            else -> {
                Log.w(TAG, "Unknown message type: ${message.optString("message_type")}")
            }
        }
    }

    private suspend fun sendMessage(message: JSONObject) {
        withContext(Dispatchers.IO) {
            val output = outputStream ?: throw IOException("Not connected")
            val messageData = message.toString().toByteArray(Charsets.UTF_8)

            output.writeInt(messageData.size)
            output.write(messageData)
            output.flush()
        }
    }

    private suspend fun receiveMessageWithTimeout(timeoutMs: Long): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                withTimeoutOrNull(timeoutMs) {
                    val input = inputStream ?: return@withTimeoutOrNull null

                    val messageLength = input.readInt()
                    if (messageLength > 10 * 1024 * 1024) { // 10MB limit
                        throw IOException("Message too large: $messageLength bytes")
                    }

                    val messageData = ByteArray(messageLength)
                    input.readFully(messageData)

                    JSONObject(String(messageData, Charsets.UTF_8))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to receive message", e)
                null
            }
        }
    }

    private fun createMessage(messageType: String): JSONObject {
        return JSONObject().apply {
            put("message_type", messageType)
            put("device_id", deviceId)
            put("timestamp_ns", timeManager.getCurrentTimestampNs())
            put("local_timestamp", System.currentTimeMillis())
        }
    }

    private fun getDeviceCapabilities(): JSONObject {
        return JSONObject().apply {
            put("recording_coordination", true)
            put("time_synchronization", true)
            put("multi_modal_recording", true)
            put("real_time_monitoring", true)
            put("error_recovery", true)
        }
    }

    private fun getAvailableSensors(): JSONObject {
        val sensors = recordingController.getAvailableSensors()
        return JSONObject().apply {
            sensors.forEach { sensor ->
                put(
                    sensor.sensorId,
                    JSONObject().apply {
                        put("type", sensor.sensorType)
                        put("sampling_rate", sensor.samplingRate)
                        put("recording", sensor.isRecording)
                    },
                )
            }
        }
    }

    private fun getSessionStatistics(): JSONObject {
        val stats = recordingController.getRecordingStatistics()
        return JSONObject().apply {
            put("is_recording", stats.isRecording)
            put("session_duration_seconds", stats.sessionDurationSeconds)
            put("active_sensors", stats.activeSensors)
            put("total_samples", stats.totalSamplesRecorded)
            put("storage_used_mb", stats.totalStorageUsedMB)
            put("dropped_samples", stats.totalDroppedSamples)
        }
    }

    private fun getSensorStatusArray(): JSONObject {
        val sensorStats = recordingController.getRecordingStatistics().sensorStatistics
        return JSONObject().apply {
            sensorStats.forEach { stats ->
                put(
                    stats.sensorId,
                    JSONObject().apply {
                        put("type", stats.sensorType)
                        put(
                            "recording",
                            recordingController.getAvailableSensors()
                                .find { it.sensorId == stats.sensorId }?.isRecording ?: false,
                        )
                        put("samples", stats.totalSamplesRecorded)
                        put("data_rate", stats.averageDataRate)
                        put("storage_mb", stats.storageUsedMB)
                    },
                )
            }
        }
    }

    suspend fun cleanup() {
        disconnect()
        networkScope.cancel()
        Log.i(TAG, "Enhanced network client cleaned up")
    }

    fun isConnected(): Boolean = isConnected.get()

    fun getConnectedController(): NetworkClient.ControllerInfo? = connectedControllerInfo
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR,
}

data class NetworkMessage(
    val messageType: String,
    val deviceId: String,
    val timestampNs: Long,
    val content: JSONObject,
) {
    companion object {
        fun fromJSON(json: JSONObject): NetworkMessage {
            return NetworkMessage(
                messageType = json.optString("message_type"),
                deviceId = json.optString("device_id"),
                timestampNs = json.optLong("timestamp_ns"),
                content = json,
            )
        }
    }
}
