package com.mpdc4gsr.libunified.app.bean

/**
 * Bean class for continuous recording/scanning configurations
 */
data class ContinuousBean(
    var isEnabled: Boolean = false,
    var interval: Long = 1000L,
    var duration: Long = 0L,
    var maxFiles: Int = 100,
    var autoStart: Boolean = false,
    var saveLocation: String = ""
) {
    companion object {
        const val DEFAULT_INTERVAL = 1000L
        const val DEFAULT_MAX_FILES = 100
    }
}