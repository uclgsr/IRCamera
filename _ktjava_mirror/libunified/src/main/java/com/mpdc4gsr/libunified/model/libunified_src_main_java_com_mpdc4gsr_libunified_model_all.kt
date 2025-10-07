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


