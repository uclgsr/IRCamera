package com.mpdc4gsr.module.thermalunified.feature.device

import kotlinx.coroutines.flow.StateFlow

data class ThermalDeviceStatus(
    val isConnected: Boolean = false,
    val isStreaming: Boolean = false,
    val deviceLabel: String = "No Device",
    val lastError: String? = null,
    val capabilities: Set<ThermalDeviceCapability> = emptySet(),
)

enum class ThermalDeviceCapability {
    Radiometric,
    DualStream,
    ManualCalibration,
    VideoRecording,
    AudioOverlay,
}

data class ThermalDeviceConfig(
    val requestedFrameRateHz: Float = 9f,
    val colorPalette: ThermalColorPalette = ThermalColorPalette.Ironbow,
    val gainMode: ThermalGainMode = ThermalGainMode.High,
    val enableNoiseReduction: Boolean = true,
)

enum class ThermalColorPalette {
    Ironbow,
    Rainbow,
    WhiteHot,
    BlackHot,
}

enum class ThermalGainMode {
    High,
    Low,
    Auto,
}

interface ThermalDeviceManager {
    val status: StateFlow<ThermalDeviceStatus>
    suspend fun connect(): Result<Unit>
    suspend fun disconnect()
    suspend fun startStream(config: ThermalDeviceConfig = ThermalDeviceConfig()): Result<Unit>
    suspend fun stopStream()
    suspend fun triggerManualCalibration(): Result<Unit>
}
