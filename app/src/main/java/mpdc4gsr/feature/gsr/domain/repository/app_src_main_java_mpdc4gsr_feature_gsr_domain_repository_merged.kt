// Merged ALL .kt and .java files from the 'app\src\main\java\mpdc4gsr\feature\gsr\domain\repository' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:33


// ===== FROM: app\src\main\java\mpdc4gsr\feature\gsr\domain\repository\ShimmerRepository.kt =====

package mpdc4gsr.feature.gsr.domain.repository

import kotlinx.coroutines.flow.Flow
import mpdc4gsr.core.data.model.DeviceInfo
import mpdc4gsr.core.data.model.GSRSample

interface ShimmerRepository {

    suspend fun scanForDevices(): Flow<List<DeviceInfo>>

    suspend fun connectDevice(deviceAddress: String): Result<Unit>

    suspend fun disconnectDevice(deviceAddress: String)

    suspend fun streamGSRData(deviceAddress: String): Flow<GSRSample>

    suspend fun stopStreaming(deviceAddress: String)

    fun isDeviceConnected(deviceAddress: String): Boolean

    suspend fun getDeviceBatteryLevel(deviceAddress: String): Int?
}