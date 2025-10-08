// Merged ALL .kt and .java files from the '_ktjava_mirror\component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\model' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:43


// ===== FROM: _ktjava_mirror\component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\model\component_thermalunified_src_main_java_com_mpdc4gsr_module_thermalunified_model_all.kt =====

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