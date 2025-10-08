// Merged ALL .kt and .java files from the 'component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\model' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:35


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\model\ThermalModels.kt =====

package com.mpdc4gsr.module.thermalunified.model

data class LogEntry(
    val timestamp: String,
    val temperature: Float,
    val location: String,
    val notes: String = ""
)

data class AlbumItem(
    val imagePath: String,
    val title: String,
    val description: String = "",
    val imageCount: Int = 0
)