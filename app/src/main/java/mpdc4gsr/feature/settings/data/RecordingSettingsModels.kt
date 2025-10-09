package mpdc4gsr.feature.settings.data

/**
 * Describes the available recording quality presets. The @[preferenceValue] is stored in
 * SharedPreferences, while @[displayName] is shown to the user.
 */
enum class RecordingQuality(
    val preferenceValue: String,
    val displayName: String
) {
    LOW("Low", "Low"),
    MEDIUM("Medium", "Medium"),
    HIGH("High", "High"),
    ULTRA("Ultra", "Ultra");

    companion object {
        fun fromPreference(value: String?): RecordingQuality =
            entries.firstOrNull { it.preferenceValue.equals(value, ignoreCase = true) } ?: HIGH

        fun fromDisplayName(displayName: String): RecordingQuality? =
            entries.firstOrNull { it.displayName.equals(displayName, ignoreCase = true) }
    }
}

/**
 * Snapshot of the user's multi-modal recording settings.
 */
data class RecordingSettings(
    val autoRecording: Boolean = false,
    val recordingQuality: RecordingQuality = RecordingQuality.HIGH,
    val videoFrameRate: Int = 30,
    val audioEnabled: Boolean = true,
    val simultaneousRecording: Boolean = true,
    val timestampSync: Boolean = true,
    val videoFormat: String = RecordingSettingsDefaults.VIDEO_FORMAT,
    val audioFormat: String = RecordingSettingsDefaults.AUDIO_FORMAT,
    val sensorDataFormat: String = RecordingSettingsDefaults.SENSOR_DATA_FORMAT
)

/**
 * Computed configuration for a given recording quality preset.
 */
data class QualityConfig(
    val videoBitrate: Int,
    val videoWidth: Int,
    val videoHeight: Int,
    val preferredFps: Int
)

/**
 * Canonical defaults used throughout the recording settings feature.
 */
object RecordingSettingsDefaults {
    const val VIDEO_FORMAT = "MP4 (H.264)"
    const val AUDIO_FORMAT = "AAC"
    const val SENSOR_DATA_FORMAT = "CSV"
}
