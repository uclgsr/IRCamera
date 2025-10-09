package mpdc4gsr.domain.usecase

import kotlinx.coroutines.flow.Flow
import mpdc4gsr.feature.thermal.data.source.ThermalFrameData
import mpdc4gsr.feature.thermal.data.source.ThermalSnapshot
import mpdc4gsr.domain.repository.ThermalRepository

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

data class ThermalCoreUseCases(
    val connectCamera: ConnectThermalCameraUseCase,
    val disconnectCamera: DisconnectThermalCameraUseCase,
    val startStreaming: StartThermalStreamingUseCase,
    val stopStreaming: StopThermalStreamingUseCase,
    val captureSnapshot: CaptureThermalSnapshotUseCase,
    val startRecording: StartThermalRecordingUseCase,
    val stopRecording: StopThermalRecordingUseCase,
    val setTemperatureRange: SetTemperatureRangeUseCase,
    val checkConnection: CheckCameraConnectionUseCase
)
