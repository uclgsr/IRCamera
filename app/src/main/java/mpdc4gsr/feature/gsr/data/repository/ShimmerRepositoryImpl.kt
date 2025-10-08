package mpdc4gsr.feature.gsr.data.repository

import kotlinx.coroutines.flow.Flow
import mpdc4gsr.core.data.model.DeviceInfo
import mpdc4gsr.core.data.model.GSRSample
import mpdc4gsr.feature.gsr.data.source.ShimmerDataSource
import mpdc4gsr.feature.gsr.domain.repository.ShimmerRepository
import javax.inject.Inject

class ShimmerRepositoryImpl @Inject constructor(
    private val shimmerDataSource: ShimmerDataSource
) : ShimmerRepository {
    override suspend fun scanForDevices(): Flow<List<DeviceInfo>> {
        return shimmerDataSource.scanForDevices()
    }

    override suspend fun connectDevice(deviceAddress: String): Result<Unit> {
        return shimmerDataSource.connect(deviceAddress)
    }

    override suspend fun disconnectDevice(deviceAddress: String) {
        shimmerDataSource.disconnect(deviceAddress)
    }

    override suspend fun streamGSRData(deviceAddress: String): Flow<GSRSample> {
        return shimmerDataSource.startStreaming(deviceAddress)
    }

    override suspend fun stopStreaming(deviceAddress: String) {
        shimmerDataSource.stopStreaming(deviceAddress)
    }

    override fun isDeviceConnected(deviceAddress: String): Boolean {
        return shimmerDataSource.isConnected(deviceAddress)
    }

    override suspend fun getDeviceBatteryLevel(deviceAddress: String): Int? {
        return shimmerDataSource.getBatteryLevel(deviceAddress)
    }
}
