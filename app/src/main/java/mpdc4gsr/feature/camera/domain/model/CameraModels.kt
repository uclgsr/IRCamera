package mpdc4gsr.feature.camera.domain.model

import android.util.Size

enum class CameraMode {
    RAW_50MP,
    VIDEO_4K,
    PREVIEW_ONLY
}

data class CameraCapabilities(
    val supportsRaw: Boolean,
    val rawSize: Size,
    val supports4k60: Boolean,
    val sensorOrientation: Int
)

data class RecordingResult(
    val success: Boolean,
    val filePath: String? = null,
    val fileCount: Int = 0,
    val error: String? = null
)
