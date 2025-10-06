package mpdc4gsr.feature.gsr.data.source

import kotlinx.coroutines.flow.Flow
import mpdc4gsr.core.data.model.DeviceInfo
import mpdc4gsr.core.data.model.GSRSample

interface ShimmerDataSource {

    suspend fun scanForDevices(): Flow<List<DeviceInfo>>

    suspend fun connect(deviceAddress: String): Result<Unit>

    suspend fun disconnect(deviceAddress: String)

    suspend fun startStreaming(deviceAddress: String): Flow<GSRSample>

    suspend fun stopStreaming(deviceAddress: String)

    fun isConnected(deviceAddress: String): Boolean

    suspend fun getBatteryLevel(deviceAddress: String): Int?
}
