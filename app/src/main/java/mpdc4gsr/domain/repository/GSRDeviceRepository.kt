package mpdc4gsr.domain.repository

import kotlinx.coroutines.flow.Flow
import mpdc4gsr.domain.model.DeviceInfo
import mpdc4gsr.domain.model.GSRSample

interface GSRDeviceRepository {

    suspend fun scanForDevices(): Flow<List<DeviceInfo>>

    suspend fun connectDevice(deviceAddress: String): Result<Unit>

    suspend fun disconnectDevice(deviceAddress: String)

    suspend fun streamGSRData(deviceAddress: String): Flow<GSRSample>

    suspend fun stopStreaming(deviceAddress: String)

    fun isDeviceConnected(deviceAddress: String): Boolean

    suspend fun getDeviceBatteryLevel(deviceAddress: String): Int?
}
