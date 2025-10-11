package mpdc4gsr.gsr.network

import android.util.Log
import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import mpdc4gsr.gsr.model.ConnectionState
import mpdc4gsr.gsr.model.DeviceDescriptor
import mpdc4gsr.gsr.model.DeviceType
import mpdc4gsr.gsr.model.FaultSeverity
import mpdc4gsr.gsr.model.RecorderKind
import mpdc4gsr.gsr.model.SessionFault
import mpdc4gsr.gsr.model.SessionStateStore
import mpdc4gsr.gsr.model.TimelineEstimate
import mpdc4gsr.gsr.session.SessionCommand
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
/**
 * Persistent command channel to the PC orchestrator. Uses a TLS WebSocket with JSON payloads. The
 * client automatically reconnects and emits parsed [SessionCommand] instances to the session layer.
 */
class CommandClient(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val endpoint: String,
    private val deviceId: String,
    private val dispatcher: CoroutineDispatcher,
    private val sessionStateStore: SessionStateStore,
) {

    private val scope = CoroutineScope(dispatcher)
    private val _events = MutableSharedFlow<SessionCommand>(extraBufferCapacity = 16)
    val events: Flow<SessionCommand> = _events.asSharedFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: Flow<ConnectionState> = _connectionState

    @Volatile
    private var webSocket: WebSocket? = null

    @Volatile
    private var retryAttempts = 0

    private val outboundQueue: Channel<String> = Channel(capacity = Channel.UNLIMITED)

    @Volatile
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch { connect() }
        scope.launch { flushOutbound() }
    }

    fun stop() {
        if (!started) return
        started = false
        scope.launch {
            try {
                outboundQueue.close()
                webSocket?.close(NORMAL_CLOSURE, "client_shutdown")
                webSocket = null
                _connectionState.value = ConnectionState.DISCONNECTED
            } catch (_: CancellationException) {
                // ignored
            }
        }
    }

    suspend fun sendCommand(payload: CommandEnvelope) {
        outboundQueue.send(json.encodeToString(payload))
    }

    private suspend fun connect() {
        withContext(dispatcher) {
            while (true) {
                _connectionState.value = ConnectionState.CONNECTING
                val request =
                    Request.Builder()
                        .url("$endpoint/ws/commands?deviceId=$deviceId")
                        .header("X-Device-ID", deviceId)
                        .build()
                webSocket = okHttpClient.newWebSocket(request, Listener())
                retryAttempts = 0
                _connectionState.value = ConnectionState.CONNECTED
                break
            }
        }
    }

    private suspend fun flushOutbound() {
        for (payload in outboundQueue) {
            val socket = webSocket ?: continue
            val success = socket.send(payload)
            if (!success) {
                Log.w(TAG, "Failed to send command payload; scheduling reconnect")
                socket.close(ABNORMAL_CLOSURE, "send_failure")
                connect()
                outboundQueue.send(payload)
            }
        }
    }

    private suspend fun delayReconnect() {
        retryAttempts++
        val delayMs = backoff(retryAttempts)
        kotlinx.coroutines.delay(delayMs)
    }

    private inner class Listener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
            Log.i(TAG, "Command channel opened: $endpoint")
            scope.launch {
                val helloPayload =
                    buildJsonObject {
                        put("deviceId", JsonPrimitive(deviceId))
                        put(
                            "capabilities",
                            JsonArray(listOf("GSR", "RGB", "THERMAL", "AUDIO").map(::JsonPrimitive)),
                        )
                    }
                sendCommand(CommandEnvelope(type = "hello", payload = helloPayload))
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            scope.launch {
                try {
                    val envelope = json.decodeFromString<CommandEnvelope>(text)
                    handleEnvelope(envelope)
                } catch (ex: Exception) {
                    Log.e(TAG, "Failed to parse command payload: $text", ex)
                }
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            // Binary commands not used currently. Potential future use for compressed frames.
            Log.w(TAG, "Unexpected binary command payload (${bytes.size} bytes)")
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.i(TAG, "Command channel closed: $code $reason")
            _connectionState.value = ConnectionState.DISCONNECTED
            scope.launch { connect() }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
            Log.e(TAG, "Command channel failure", t)
            _connectionState.value = ConnectionState.ERROR
            scope.launch {
                delayReconnect()
                connect()
            }
        }
    }

    private suspend fun handleEnvelope(envelope: CommandEnvelope) {
        when (envelope.type) {
            "command" -> handleCommand(envelope)
            "command_ack" -> Log.d(TAG, "Command ack received for ${envelope.commandId}")
            "start_recording" -> handleStartRecording(envelope.payload)
            "stop_recording" -> handleStopRecording(envelope.payload)
            "stimulus_marker" -> handleStimulus("stimulus_marker", envelope.payload)
            "device_snapshot" -> {
                val snapshot = parseDeviceSnapshot(envelope.payload)
                _events.emit(SessionCommand.ApplyDeviceSnapshot(snapshot))
            }
            "timeline_update" -> {
                val timeline = parseTimeline(envelope.payload)
                sessionStateStore.sessionSnapshot.value?.let {
                    _events.emit(SessionCommand.AppendFault(SessionFaultFactory.timeline(timeline)))
                }
            }
            else -> Log.w(TAG, "Unhandled command type: ${envelope.type}")
        }
    }

    private suspend fun handleCommand(envelope: CommandEnvelope) {
        val name = envelope.command?.lowercase() ?: run {
            Log.w(TAG, "Command envelope missing command field: $envelope")
            return
        }
        val commandId = envelope.commandId
        val requiresAck = envelope.requiresAck
        try {
            when (name) {
                "start_recording" -> handleStartRecording(envelope.payload)
                "stop_recording" -> handleStopRecording(envelope.payload)
                "stimulus_marker" -> handleStimulus("stimulus_marker", envelope.payload)
                "flash_sync" -> handleStimulus("flash_sync", envelope.payload)
                "audio_beep" -> handleStimulus("audio_beep", envelope.payload)
                "set_simulation_mode" -> handleSimulationCommand(envelope.payload)
                else -> {
                    Log.w(TAG, "Unknown command type received: $name")
                    if (requiresAck && commandId != null) {
                        sendAck(commandId, "error", "unknown_command")
                    }
                    return
                }
            }
            if (requiresAck && commandId != null) {
                sendAck(commandId, "ok")
            }
        } catch (error: Exception) {
            Log.e(TAG, "Failed to handle command '$name'", error)
            if (requiresAck && commandId != null) {
                sendAck(commandId, "error", error.message ?: "error")
            }
        }
    }

    private suspend fun handleStartRecording(payload: JsonObject) {
        val sessionId =
            payload["session_id"]?.jsonPrimitive?.content
                ?: payload["sessionId"]?.jsonPrimitive?.content
                ?: "unknown"
        val modalities =
            payload["modalities"]?.jsonArray?.mapNotNull {
                it.jsonPrimitive.contentOrNull?.let { name ->
                    runCatching { RecorderKind.valueOf(name.uppercase()) }.getOrNull()
                }
            }?.toSet()
                ?: RecorderKind.values().toSet()
        val scheduled = payload["scheduledTimeMillis"]?.jsonPrimitive?.longOrNull ?: 0L
        val parameters = payload.toKotlinMap()
        val command =
            SessionCommand.StartRecording(
                sessionId = sessionId,
                modalities = modalities,
                scheduledTimeMillis = scheduled,
                parameters = parameters,
            )
        _events.emit(command)
    }

    private suspend fun handleStopRecording(payload: JsonObject) {
        val command =
            SessionCommand.StopRecording(
                sessionId =
                    payload["session_id"]?.jsonPrimitive?.content
                        ?: payload["sessionId"]?.jsonPrimitive?.content
                        ?: "unknown",
                scheduledTimeMillis = payload["scheduledTimeMillis"]?.jsonPrimitive?.longOrNull ?: 0L,
            )
        _events.emit(command)
    }

    private suspend fun handleStimulus(markerId: String, payload: JsonObject) {
        val metadata = payload.toKotlinMap() + ("origin" to "pc")
        val scheduled =
            payload["scheduledTimeMillis"]?.jsonPrimitive?.longOrNull
                ?: payload["timestamp"]?.jsonPrimitive?.longOrNull
                ?: payload["timestamp"]?.jsonPrimitive?.doubleOrNull?.toLong()
                ?: 0L
        val marker =
            SessionCommand.StimulusMarker(
                markerId = markerId,
                scheduledTimeMillis = scheduled,
                metadata = metadata,
            )
        _events.emit(marker)
    }

    private suspend fun handleSimulationCommand(payload: JsonObject) {
        val enabled = payload["enabled"]?.jsonPrimitive?.booleanOrNull ?: false
        _events.emit(SessionCommand.SetSimulationMode(enabled))
    }

    suspend fun emitEventMarker(
        code: String,
        timestampMillis: Long,
        metadata: Map<String, Any?>,
        sessionId: String?,
    ) {
        val payload =
            buildJsonObject {
                put("code", JsonPrimitive(code))
                put("timestamp", JsonPrimitive(timestampMillis))
                sessionId?.let { put("session_id", JsonPrimitive(it)) }
                metadata.forEach { (key, value) ->
                    value.toJsonElement()?.let { put(key, it) }
                }
            }
        sendCommand(CommandEnvelope(type = "event_marker", payload = payload))
    }

    private suspend fun sendAck(commandId: String, status: String, message: String? = null) {
        val payload =
            buildJsonObject {
                put("commandId", JsonPrimitive(commandId))
                put("status", JsonPrimitive(status))
                put("deviceId", JsonPrimitive(deviceId))
                message?.let { put("message", JsonPrimitive(it)) }
            }
        sendCommand(CommandEnvelope(type = "command_ack", payload = payload))
    }

    private fun parseDeviceSnapshot(payload: JsonObject): List<DeviceDescriptor> =
        payload["devices"]?.jsonArray
            ?.mapNotNull { DeviceDescriptorFactory.fromPayload(it.jsonObject) }
            ?: emptyList()

    private fun parseTimeline(payload: JsonObject): TimelineEstimate =
        TimelineEstimateFactory.fromPayload(payload)

    companion object {
        private const val TAG = "GsrCommandClient"
        private const val NORMAL_CLOSURE = 1000
        private const val ABNORMAL_CLOSURE = 1006

        private fun backoff(attempt: Int): Long {
            val capped = minOf(attempt, 6)
            return TimeUnit.SECONDS.toMillis(1L shl capped)
        }
    }
}

@Serializable
data class CommandEnvelope(
    val type: String,
    val command: String? = null,
    val commandId: String? = null,
    val requiresAck: Boolean = false,
    val payload: JsonObject = JsonObject(emptyMap()),
)

private object SessionFaultFactory {
    fun timeline(timeline: TimelineEstimate): SessionFault =
        SessionFault(
            severity = FaultSeverity.INFO,
            source = null,
            message = "Timeline updated: offset=${timeline.offsetMillis}ms drift=${timeline.driftPpm}ppm",
        )
}

private object DeviceDescriptorFactory {
    fun fromPayload(payload: JsonObject): DeviceDescriptor? {
        val id = payload["id"]?.jsonPrimitive?.content ?: return null
        val state =
            payload["state"]?.jsonPrimitive?.contentOrNull?.uppercase()?.let {
                runCatching { ConnectionState.valueOf(it) }.getOrNull()
            } ?: ConnectionState.DISCOVERED
        return DeviceDescriptor(
            id = id,
            displayName = payload["displayName"]?.jsonPrimitive?.contentOrNull ?: id,
            type =
                payload["type"]?.jsonPrimitive?.contentOrNull?.uppercase()?.let {
                    runCatching { DeviceType.valueOf(it) }.getOrNull()
                } ?: DeviceType.ANDROID_HOST,
            connectionState = state,
            batteryPercent = payload["batteryPercent"]?.jsonPrimitive?.intOrNull,
            supportsThermal = payload["supportsThermal"]?.jsonPrimitive?.booleanOrNull ?: false,
            supportsRgb = payload["supportsRgb"]?.jsonPrimitive?.booleanOrNull ?: false,
            supportsAudio = payload["supportsAudio"]?.jsonPrimitive?.booleanOrNull ?: false,
            shimmerMacAddress = payload["shimmerMacAddress"]?.jsonPrimitive?.contentOrNull,
            lastHeartbeat =
                payload["lastHeartbeatEpochMillis"]?.jsonPrimitive?.longOrNull?.let {
                    Instant.ofEpochMilli(it)
                },
            timeOffsetMillis = payload["timeOffsetMillis"]?.jsonPrimitive?.doubleOrNull,
            tags =
                payload["tags"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }?.toSet()
                    ?: emptySet(),
        )
    }
}

private object TimelineEstimateFactory {
    fun fromPayload(payload: JsonObject): TimelineEstimate =
        TimelineEstimate(
            referenceEpochMillis = payload["referenceEpochMillis"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis(),
            offsetMillis = payload["offsetMillis"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            roundTripMillis = payload["roundTripMillis"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            driftPpm = payload["driftPpm"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            accuracyMillis = payload["accuracyMillis"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            lastUpdated = Instant.now(),
        )
}

private fun Any?.toJsonElement(): JsonElement? =
    when (this) {
        null -> null
        is JsonElement -> this
        is String -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is Map<*, *> -> {
            val content =
                this.entries.mapNotNull { (key, value) ->
                    (key as? String)?.let { stringKey ->
                        value.toJsonElement()?.let { stringKey to it }
                    }
                }.toMap()
            JsonObject(content)
        }
        is Iterable<*> -> JsonArray(this.mapNotNull { it.toJsonElement() })
        is Array<*> -> JsonArray(this.mapNotNull { it.toJsonElement() })
        is Enum<*> -> JsonPrimitive(this.name)
        else -> JsonPrimitive(this.toString())
    }

private fun JsonObject.toKotlinMap(): Map<String, Any?> =
    entries.associate { it.key to it.value.toKotlinValue() }

private fun JsonElement.toKotlinValue(): Any? =
    when (this) {
        is JsonPrimitive -> when {
            this.booleanOrNull != null -> this.booleanOrNull
            this.longOrNull != null -> this.longOrNull
            this.doubleOrNull != null -> this.doubleOrNull
            this.floatOrNull != null -> this.floatOrNull
            this.contentOrNull != null -> this.contentOrNull
            else -> null
        }
        is JsonArray -> this.mapNotNull { it.toKotlinValue() }
        is JsonObject -> this.toKotlinMap()
        else -> null
    }



