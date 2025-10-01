package mpdc4gsr.camera.core

import android.util.Size

data class DeviceCaps(
    val supportsRaw: Boolean,
    val rawSize: Size,
    val supports4k60: Boolean,
    val sensorOrientation: Int,
)
