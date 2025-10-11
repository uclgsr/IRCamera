package com.mpdc4gsr.component.shared.model

// Development: Minimal contract for thermal temperature correction configuration
interface IRTempConfig {
    val emissivity: Float
    val distance: Float
    val ambientTemperature: Float
    val humidity: Float
    val transmittance: Float
}


