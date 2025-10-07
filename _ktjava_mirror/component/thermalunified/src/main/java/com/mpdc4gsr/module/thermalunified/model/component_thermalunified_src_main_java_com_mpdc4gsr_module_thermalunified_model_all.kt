// Merged .kt under 'component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\model' subtree
// Files: 1; Generated 2025-10-07 23:07:44


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\model\ThermalModels.kt =====

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


