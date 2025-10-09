package mpdc4gsr.domain.usecase

import kotlinx.coroutines.flow.Flow
import mpdc4gsr.domain.model.DeviceInfo
import mpdc4gsr.domain.model.GSRSample
import mpdc4gsr.domain.repository.GSRDeviceRepository

class ScanGSRDevicesUseCase(
    private val repository: GSRDeviceRepository
) {
    suspend operator fun invoke(): Flow<List<DeviceInfo>> {
        return repository.scanForDevices()
    }
}

class ConnectGSRDeviceUseCase(
    private val repository: GSRDeviceRepository
) {
    suspend operator fun invoke(deviceAddress: String): Result<Unit> {
        if (deviceAddress.isBlank()) {
            return Result.failure(IllegalArgumentException("Device address cannot be empty"))
        }
        return repository.connectDevice(deviceAddress)
    }
}

class DisconnectGSRDeviceUseCase(
    private val repository: GSRDeviceRepository
) {
    suspend operator fun invoke(deviceAddress: String) {
        repository.disconnectDevice(deviceAddress)
    }
}

class StartGSRStreamingUseCase(
    private val repository: GSRDeviceRepository
) {
    suspend operator fun invoke(deviceAddress: String): Flow<GSRSample> {
        if (!repository.isDeviceConnected(deviceAddress)) {
            throw IllegalStateException("Device not connected: $deviceAddress")
        }
        return repository.streamGSRData(deviceAddress)
    }
}

class StopGSRStreamingUseCase(
    private val repository: GSRDeviceRepository
) {
    suspend operator fun invoke(deviceAddress: String) {
        repository.stopStreaming(deviceAddress)
    }
}

class CheckGSRDeviceConnectionUseCase(
    private val repository: GSRDeviceRepository
) {
    operator fun invoke(deviceAddress: String): Boolean {
        return repository.isDeviceConnected(deviceAddress)
    }
}

class GetGSRDeviceBatteryUseCase(
    private val repository: GSRDeviceRepository
) {
    suspend operator fun invoke(deviceAddress: String): Int? {
        return repository.getDeviceBatteryLevel(deviceAddress)
    }
}
