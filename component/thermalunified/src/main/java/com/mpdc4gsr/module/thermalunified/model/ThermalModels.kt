package com.mpdc4gsr.module.thermalunified.model

/**
 * Data models for thermal module
 * Centralized location for data classes used across thermal activities
 */

/**
 * Log entry for monitoring history
 */
data class LogEntry(
    val timestamp: String,
    val temperature: Float,
    val location: String,
    val notes: String = ""
)

/**
 * Album item for gallery and report preview
 */
data class AlbumItem(
    val imagePath: String,
    val title: String,
    val description: String = "",
    val imageCount: Int = 0
)
