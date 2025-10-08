// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\model' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:38


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\model\IRTempConfig.kt =====

package com.mpdc4gsr.libunified.model

// Development: Minimal contract for thermal temperature correction configuration
interface IRTempConfig {
    val emissivity: Float
    val distance: Float
    val ambientTemperature: Float
    val humidity: Float
    val transmittance: Float
}