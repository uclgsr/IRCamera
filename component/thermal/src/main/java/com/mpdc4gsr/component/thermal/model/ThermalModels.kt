package com.mpdc4gsr.component.thermal.model

data class LogEntry(
    val timestamp: String,
    val temperature: Float,
    val location: String,
    val notes: String = "",
)

data class AlbumItem(
    val imagePath: String,
    val title: String,
    val description: String = "",
    val imageCount: Int = 0,
)

