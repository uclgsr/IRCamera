package com.topdon.tc001.camera.core

import android.util.Size

/**
 * Device capabilities detected once at camera open
 * As specified in the implementation plan
 */
data class DeviceCaps(
    val supportsRaw: Boolean,
    val rawSize: Size,  
    val supports4k60: Boolean, // true only if 2160p@60 present
    val sensorOrientation: Int
)