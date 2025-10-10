package mpdc4gsr.gsr.network

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import mpdc4gsr.gsr.device.ShimmerDeviceController
import mpdc4gsr.gsr.model.TelemetryState

/**
 * Low-bandwidth telemetry publisher responsible for updating the PC UI with latest sensor and
 * preview statistics (FR6). The actual RGB/thermal preview frames are handled by dedicated
 * services; this class sends summary metadata and numeric readings.
 */
class PreviewPublisher(
    private val commandClient: CommandClient,
    private val telemetryState: StateFlow<Map<String, TelemetryState>>,
    private val dispatcher: CoroutineDispatcher,
) : AutoCloseable {

    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    @Volatile private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            while (started) {
                publishTelemetry()
                delay(500)
            }
        }
    }

    private suspend fun publishTelemetry() {
        val payload: JsonObject =
            buildJsonObject {
                telemetryState.value.forEach { (deviceId, telemetry) ->
                    putJsonObject(deviceId) {
                        telemetry.gsrMicrosiemens?.let { put("gsr_microsiemens", it) }
                        telemetry.skinTemperatureCelsius?.let { put("skin_temp_celsius", it) }
                        telemetry.thermalSpotCelsius?.let { put("thermal_spot_celsius", it) }
                        telemetry.audioLevelDb?.let { put("audio_level_db", it) }
                        telemetry.frameRate?.let { put("frame_rate", it) }
                        put("dropped_frames", telemetry.droppedFrames)
                        telemetry.batteryPercent?.let { put("battery_percent", it) }
                        telemetry.rssi?.let { put("rssi", it) }
                    }
                }
            }
        commandClient.sendCommand(
            CommandEnvelope(
                type = "telemetry_update",
                payload = payload,
            ),
        )
    }

    override fun close() {
        started = false
        scope.cancel()
    }
}
