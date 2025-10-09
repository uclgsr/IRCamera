package mpdc4gsr.data.source

import kotlinx.coroutines.flow.Flow
import mpdc4gsr.domain.model.DeviceInfo
import mpdc4gsr.domain.model.GSRSample

interface GSRDeviceDataSource {

    suspend fun scanForDevices(): Flow<List<DeviceInfo>>

    suspend fun connect(deviceAddress: String): Result<Unit>

    suspend fun disconnect(deviceAddress: String)

    suspend fun startStreaming(deviceAddress: String): Flow<GSRSample>

    suspend fun stopStreaming(deviceAddress: String)

    fun isConnected(deviceAddress: String): Boolean

    suspend fun getBatteryLevel(deviceAddress: String): Int?
}
