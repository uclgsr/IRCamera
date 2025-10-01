package mpdc4gsr.feature.gsr.domain.usecase

import kotlinx.coroutines.flow.Flow
import mpdc4gsr.feature.gsr.domain.repository.ShimmerRepository
import mpdc4gsr.sensors.unified.model.DeviceInfo
import mpdc4gsr.sensors.unified.model.GSRSample

/**
 * Use case for scanning Shimmer devices
 * 
 * Encapsulates business logic for device discovery.
 */
class ScanShimmerDevicesUseCase(
    private val repository: ShimmerRepository
) {
    suspend operator fun invoke(): Flow<List<DeviceInfo>> {
        return repository.scanForDevices()
    }
}

/**
 * Use case for connecting to a Shimmer device
 * 
 * Handles connection logic and validation.
 */
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

/**
 * Use case for disconnecting from a Shimmer device
 */
class DisconnectShimmerDeviceUseCase(
    private val repository: ShimmerRepository
) {
    suspend operator fun invoke(deviceAddress: String) {
        repository.disconnectDevice(deviceAddress)
    }
}

/**
 * Use case for streaming GSR data from Shimmer device
 * 
 * Handles data streaming and validation.
 */
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

/**
 * Use case for stopping GSR data streaming
 */
class StopGSRStreamingUseCase(
    private val repository: ShimmerRepository
) {
    suspend operator fun invoke(deviceAddress: String) {
        repository.stopStreaming(deviceAddress)
    }
}

/**
 * Use case for checking device connection status
 */
class CheckDeviceConnectionUseCase(
    private val repository: ShimmerRepository
) {
    operator fun invoke(deviceAddress: String): Boolean {
        return repository.isDeviceConnected(deviceAddress)
    }
}

/**
 * Use case for getting device battery level
 */
class GetDeviceBatteryUseCase(
    private val repository: ShimmerRepository
) {
    suspend operator fun invoke(deviceAddress: String): Int? {
        return repository.getDeviceBatteryLevel(deviceAddress)
    }
}
