// Merged .kt under 'app\src\main\java\mpdc4gsr\feature\thermal\domain\usecase' subtree
// Files: 1; Generated 2025-10-07 23:07:39


// ===== app\src\main\java\mpdc4gsr\feature\thermal\domain\usecase\ThermalUseCases.kt =====

package mpdc4gsr.feature.thermal.domain.usecase

import kotlinx.coroutines.flow.Flow
import mpdc4gsr.feature.thermal.data.source.ThermalFrameData
import mpdc4gsr.feature.thermal.data.source.ThermalSnapshot
import mpdc4gsr.feature.thermal.domain.repository.ThermalRepository

class ConnectThermalCameraUseCase(
    private val repository: ThermalRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.connectCamera()
    }
}

class DisconnectThermalCameraUseCase(
    private val repository: ThermalRepository
) {
    suspend operator fun invoke() {
        repository.disconnectCamera()
    }
}

class StartThermalStreamingUseCase(
    private val repository: ThermalRepository
) {
    suspend operator fun invoke(): Flow<ThermalFrameData> {
        if (!repository.isCameraConnected()) {
            throw IllegalStateException("Thermal camera not connected")
        }
        return repository.getThermalStream()
    }
}

class StopThermalStreamingUseCase(
    private val repository: ThermalRepository
) {
    suspend operator fun invoke() {
        repository.stopStream()
    }
}

class CaptureThermalSnapshotUseCase(
    private val repository: ThermalRepository
) {
    suspend operator fun invoke(): Result<ThermalSnapshot> {
        if (!repository.isCameraConnected()) {
            return Result.failure(IllegalStateException("Thermal camera not connected"))
        }
        return repository.captureSnapshot()
    }
}

class StartThermalRecordingUseCase(
    private val repository: ThermalRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        if (!repository.isCameraConnected()) {
            return Result.failure(IllegalStateException("Thermal camera not connected"))
        }
        return repository.startRecording()
    }
}

class StopThermalRecordingUseCase(
    private val repository: ThermalRepository
) {
    suspend operator fun invoke(): Result<String> {
        return repository.stopRecording()
    }
}

class SetTemperatureRangeUseCase(
    private val repository: ThermalRepository
) {
    suspend operator fun invoke(minTemp: Float, maxTemp: Float): Result<Unit> {
        if (minTemp >= maxTemp) {
            return Result.failure(IllegalArgumentException("Min temperature must be less than max temperature"))
        }
        return repository.setTemperatureRange(minTemp, maxTemp)
    }
}

class CheckCameraConnectionUseCase(
    private val repository: ThermalRepository
) {
    operator fun invoke(): Boolean {
        return repository.isCameraConnected()
    }
}


