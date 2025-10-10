package mpdc4gsr.feature.capture.gsr.data.repository

import kotlinx.coroutines.flow.Flow
import mpdc4gsr.core.hardware.gsr.model.DeviceInfo
import mpdc4gsr.core.hardware.gsr.model.GSRSample
import mpdc4gsr.feature.capture.gsr.data.source.GSRDeviceDataSource
import mpdc4gsr.feature.capture.gsr.domain.repository.GSRDeviceRepository
import javax.inject.Inject

class GSRDeviceRepositoryImpl
    @Inject
    constructor(
        private val gsrDeviceDataSource: GSRDeviceDataSource,
    ) : GSRDeviceRepository {
        override suspend fun scanForDevices(): Flow<List<DeviceInfo>> = gsrDeviceDataSource.scanForDevices()

        override suspend fun connectDevice(deviceAddress: String): Result<Unit> = gsrDeviceDataSource.connect(deviceAddress)

        override suspend fun disconnectDevice(deviceAddress: String) {
            gsrDeviceDataSource.disconnect(deviceAddress)
        }

        override suspend fun streamGSRData(deviceAddress: String): Flow<GSRSample> = gsrDeviceDataSource.startStreaming(deviceAddress)

        override suspend fun stopStreaming(deviceAddress: String) {
            gsrDeviceDataSource.stopStreaming(deviceAddress)
        }

        override fun isDeviceConnected(deviceAddress: String): Boolean = gsrDeviceDataSource.isConnected(deviceAddress)

        override suspend fun getDeviceBatteryLevel(deviceAddress: String): Int? = gsrDeviceDataSource.getBatteryLevel(deviceAddress)
    }
