package mpdc4gsr.gsr.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.time.Instant
import java.util.UUID

/**
 * Shared model definitions for the rewritten GSR module. These models are consumed by both
 * capture orchestrators and UI layers to ensure a consistent contract across the subsystem.
 */

/** Represents an attached sensor-capable endpoint (Android device local or remote). */
@Immutable
data class DeviceDescriptor(
    val id: String,
    val displayName: String,
    val type: DeviceType,
    val connectionState: ConnectionState,
    val batteryPercent: Int?,
    val supportsThermal: Boolean,
    val supportsRgb: Boolean,
    val supportsAudio: Boolean,
    val shimmerMacAddress: String?,
    val lastHeartbeat: Instant?,
    val timeOffsetMillis: Double?,
    val tags: Set<String> = emptySet(),
)

/** Device classification used by dashboards. */
enum class DeviceType {
    ANDROID_HOST,
    PC_HOST,
    SHIMMER_GSR_SENSOR,
    SIMULATED_SOURCE,
}

/** Simplified connection lifecycle used throughout the UI and logs. */
enum class ConnectionState {
    DISCOVERED,
    CONNECTING,
    CONNECTED,
    READY,
    RECORDING,
    DISCONNECTED,
    ERROR,
}

/** Recorder states surfaced to the UI telemetry timeline. */
enum class RecorderState {
    IDLE,
    PREPARING,
    RECORDING,
    STOPPING,
    FAILED,
}

/** Consolidated view of the active session. */
data class SessionSnapshot(
    val sessionId: String,
    val label: String,
    val startedAt: Instant,
    val plannedDurationSeconds: Long?,
    val elapsedMillis: Long,
    val isRecording: Boolean,
    val connectedDevices: List<DeviceDescriptor>,
    val recorderStates: Map<RecorderKind, RecorderState>,
    val faults: List<SessionFault>,
    val previewStreams: Map<RecorderKind, PreviewDescriptor>,
    val globalTimeline: TimelineEstimate,
    val commandsPending: Int,
)

/** Modalities supported by the platform. */
enum class RecorderKind {
    GSR,
    RGB_VIDEO,
    THERMAL_VIDEO,
    AUDIO,
}

/** Structured description of a fault or warning. */
data class SessionFault(
    val id: UUID = UUID.randomUUID(),
    val severity: FaultSeverity,
    val source: RecorderKind?,
    val message: String,
    val details: Map<String, Any?> = emptyMap(),
    val occurredAt: Instant = Instant.now(),
)

enum class FaultSeverity { INFO, WARNING, ERROR }

/** Metadata for a preview stream (live low-bandwidth feed). */
data class PreviewDescriptor(
    val streamId: String,
    val kind: RecorderKind,
    val width: Int,
    val height: Int,
    val frameRate: Double,
    val lastFrameUri: Uri?,
    val subscribers: Int,
)

/** Global time alignment estimate as computed by the time sync client. */
data class TimelineEstimate(
    val referenceEpochMillis: Long,
    val offsetMillis: Double,
    val roundTripMillis: Double,
    val driftPpm: Double,
    val accuracyMillis: Double,
    val lastUpdated: Instant,
)

/** Pending recording output file entry in the manifest. */
data class RecordingOutput(
    val file: File,
    val modality: RecorderKind,
    val mimeType: String,
    val startTimestampMillis: Long,
    val endTimestampMillis: Long?,
    val sizeBytes: Long,
    val checksumSha256: String?,
)

/** Raw sample emitted by the Shimmer pipeline. */
data class GsrSample(
    val deviceId: String,
    val timestampMillis: Long,
    val gsrMicrosiemens: Double,
    val gsrRaw: Int? = null,
    val ppgRaw: Int? = null,
    val qualityScore: Double? = null,
    val connectionRssi: Int? = null,
    val resistanceOhms: Double? = null,
    val skinTemperatureCelsius: Double? = null,
    val sequenceNumber: Long = 0L,
) {
    val isValid: Boolean
        get() = qualityScore?.let { it >= 0.5 } ?: true
}

/** Aggregated telemetry published to Compose layers. */
data class TelemetryState(
    val gsrMicrosiemens: Float?,
    val skinTemperatureCelsius: Float?,
    val thermalSpotCelsius: Float?,
    val audioLevelDb: Float?,
    val frameRate: Double?,
    val droppedFrames: Int,
    val batteryPercent: Int?,
    val rssi: Int?,
    val ispActive: Boolean? = null,
)

/**
 * Container bridging capture services and UI for state observation. It is intentionally simple and
 * leverages a [MutableStateFlow] internally so the ViewModel surface can expose immutable state.
 */
class SessionStateStore {
    val sessionSnapshot = MutableStateFlow<SessionSnapshot?>(null)
    val deviceTelemetry = MutableStateFlow<Map<String, TelemetryState>>(emptyMap())
}
