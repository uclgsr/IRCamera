package mpdc4gsr.feature.gsr.domain.usecase

import kotlinx.coroutines.flow.Flow
import mpdc4gsr.core.data.model.DeviceInfo
import mpdc4gsr.core.data.model.GSRSample
import mpdc4gsr.feature.gsr.domain.repository.ShimmerRepository

class ScanShimmerDevicesUseCase(
    private val repository: ShimmerRepository
) {
    suspend operator fun invoke(): Flow<List<DeviceInfo>> {
        return repository.scanForDevices()
    }
}

class ConnectShimmerDeviceUseCase(
    private val repository: ShimmerRepository
) {
    suspend operator fun invoke(deviceAddress: String): Result<Unit> {
        if (deviceAddress.isBlank()) {
            return Result.failure(IllegalArgumentException("Device address cannot be empty"))
        }
        return repository.connectDevice(deviceAddress)
    }
}

class DisconnectShimmerDeviceUseCase(
    private val repository: ShimmerRepository
) {
    suspend operator fun invoke(deviceAddress: String) {
        repository.disconnectDevice(deviceAddress)
    }
}

class StartGSRStreamingUseCase(
    private val repository: ShimmerRepository
) {
    suspend operator fun invoke(deviceAddress: String): Flow<GSRSample> {
        if (!repository.isDeviceConnected(deviceAddress)) {
            throw IllegalStateException("Device not connected: $deviceAddress")
        }
        return repository.streamGSRData(deviceAddress)
    }
}

class StopGSRStreamingUseCase(
    private val repository: ShimmerRepository
) {
    suspend operator fun invoke(deviceAddress: String) {
        repository.stopStreaming(deviceAddress)
    }
}

class CheckDeviceConnectionUseCase(
    private val repository: ShimmerRepository
) {
    operator fun invoke(deviceAddress: String): Boolean {
        return repository.isDeviceConnected(deviceAddress)
    }
}

class GetDeviceBatteryUseCase(
    private val repository: ShimmerRepository
) {
    suspend operator fun invoke(deviceAddress: String): Int? {
        return repository.getDeviceBatteryLevel(deviceAddress)
    }
}
