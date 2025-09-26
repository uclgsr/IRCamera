package com.mpdc4gsr.libunified.app.bean

/**
 * Bean class for camera item configurations and types
 */
data class CameraItemBean(
    var type: Int = TYPE_NORMAL,
    var time: Int = 0,
    var isSel: Boolean = false,
    var name: String = "",
    var code: Int = 0
) {
    companion object {
        // Basic camera item types
        const val TYPE_NORMAL = 0
        const val TYPE_DELAY = 1
        const val TYPE_ZDKM = 2      // Auto mode
        const val TYPE_SDKM = 3      // Shutter mode
        const val TYPE_AUDIO = 4     // Audio mode
        
        // Temperature modes
        const val TYPE_TMP_C = 10    // Celsius
        const val TYPE_TMP_F = 11    // Fahrenheit
        const val TYPE_TMP_K = 12    // Kelvin
        
        // Delay times
        const val DELAY_TIME_0 = 0
        const val DELAY_TIME_3 = 3
        const val DELAY_TIME_5 = 5
        const val DELAY_TIME_10 = 10
    }
}