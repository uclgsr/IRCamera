package com.topdon.module.thermal.ir.video

/**
 * Video record utility class for thermal imaging operations.
 * Provides helper functions and common functionality.
 */
abstract class VideoRecord {
    abstract fun startRecord()

    abstract fun startRecord(fileDir: String)

    abstract fun stopRecord()

    abstract fun updateAudioState(audioRecord: Boolean)
}
