package mpdc4gsr.feature.camera.data

import android.util.Size

data class DeviceCaps(
    val supportsRaw: Boolean,
    val rawSize: Size,
    val supports4k60: Boolean,
    val sensorOrientation: Int,
)
