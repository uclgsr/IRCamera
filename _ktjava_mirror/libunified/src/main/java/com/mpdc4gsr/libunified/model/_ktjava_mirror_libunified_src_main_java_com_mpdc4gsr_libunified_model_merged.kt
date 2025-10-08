// Merged ALL .kt and .java files from the '_ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\model' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:45


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\model\libunified_src_main_java_com_mpdc4gsr_libunified_model_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\model' subtree
// Files: 1; Generated 2025-10-07 23:07:48


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\model\IRTempConfig.kt =====

package com.mpdc4gsr.libunified.model

// Development: Minimal contract for thermal temperature correction configuration
interface IRTempConfig {
    val emissivity: Float
    val distance: Float
    val ambientTemperature: Float
    val humidity: Float
    val transmittance: Float
}