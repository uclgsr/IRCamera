package mpdc4gsr.feature.thermal.domain.usecase

import kotlinx.coroutines.flow.Flow
import mpdc4gsr.feature.thermal.data.source.ThermalFrameData
import mpdc4gsr.feature.thermal.data.source.ThermalSnapshot
import mpdc4gsr.feature.thermal.domain.repository.ThermalRepository

/**
 * Use case for connecting to thermal camera
 */
class ConnectThermalCameraUseCase(
    private val repository: ThermalRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.connectCamera()
    }
}

/**
 * Use case for disconnecting from thermal camera
 */
class DisconnectThermalCameraUseCase(
    private val repository: ThermalRepository
) {
    suspend operator fun invoke() {
        repository.disconnectCamera()
    }
}

/**
 * Use case for streaming thermal frames
 */
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

/**
 * Use case for stopping thermal stream
 */
class StopThermalStreamingUseCase(
    private val repository: ThermalRepository
) {
    suspend operator fun invoke() {
        repository.stopStream()
    }
}

/**
 * Use case for capturing thermal snapshot
 */
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

/**
 * Use case for starting thermal recording
 */
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

/**
 * Use case for stopping thermal recording
 */
class StopThermalRecordingUseCase(
    private val repository: ThermalRepository
) {
    suspend operator fun invoke(): Result<String> {
        return repository.stopRecording()
    }
}

/**
 * Use case for setting temperature range
 */
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

/**
 * Use case for checking camera connection
 */
class CheckCameraConnectionUseCase(
    private val repository: ThermalRepository
) {
    operator fun invoke(): Boolean {
        return repository.isCameraConnected()
    }
}
