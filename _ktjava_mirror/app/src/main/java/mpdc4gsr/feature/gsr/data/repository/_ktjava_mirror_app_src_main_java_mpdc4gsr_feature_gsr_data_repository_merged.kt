// Merged ALL .kt and .java files from the '_ktjava_mirror\app\src\main\java\mpdc4gsr\feature\gsr\data\repository' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:41


// ===== FROM: _ktjava_mirror\app\src\main\java\mpdc4gsr\feature\gsr\data\repository\app_src_main_java_mpdc4gsr_feature_gsr_data_repository_all.kt =====

// Merged .kt under 'app\src\main\java\mpdc4gsr\feature\gsr\data\repository' subtree
// Files: 1; Generated 2025-10-07 23:07:39


// ===== app\src\main\java\mpdc4gsr\feature\gsr\data\repository\ShimmerRepositoryImpl.kt =====

package mpdc4gsr.feature.gsr.data.repository

import kotlinx.coroutines.flow.Flow
import mpdc4gsr.core.data.model.DeviceInfo
import mpdc4gsr.core.data.model.GSRSample
import mpdc4gsr.feature.gsr.data.source.ShimmerDataSource
import mpdc4gsr.feature.gsr.domain.repository.ShimmerRepository

class ShimmerRepositoryImpl(
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