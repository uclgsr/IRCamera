package mpdc4gsr.feature.gsr.domain.repository

import kotlinx.coroutines.flow.Flow
import mpdc4gsr.core.data.model.DeviceInfo
import mpdc4gsr.core.data.model.GSRSample

/**
 * Repository interface for Shimmer device operations.
 * 
 * Domain layer interface that defines the contract for Shimmer data operations.
 * Implementation is in the data layer, allowing for:
 * - Dependency inversion (domain doesn't depend on data)
 * - Easy testing with fake repositories
 * - Multiple implementation strategies (caching, offline, etc.)
 */
interface ShimmerRepository {
    
    /**
     * Scan for available Shimmer devices
     * @return Flow of discovered devices
     */
    suspend fun scanForDevices(): Flow<List<DeviceInfo>>
    
    /**
     * Connect to a Shimmer device
     * @param deviceAddress MAC address of the device
     * @return Result indicating success or failure with error details
     */
    suspend fun connectDevice(deviceAddress: String): Result<Unit>
    
    /**
     * Disconnect from a Shimmer device
     * @param deviceAddress MAC address of the device
     */
    suspend fun disconnectDevice(deviceAddress: String)
    
    /**
     * Stream GSR data from connected device
     * @param deviceAddress MAC address of the device
     * @return Flow of GSR samples
     */
    suspend fun streamGSRData(deviceAddress: String): Flow<GSRSample>
    
    /**
     * Stop streaming GSR data
     * @param deviceAddress MAC address of the device
     */
    suspend fun stopStreaming(deviceAddress: String)
    
    /**
     * Check if device is connected
     * @param deviceAddress MAC address of the device
     * @return True if connected
     */
    fun isDeviceConnected(deviceAddress: String): Boolean
    
    /**
     * Get device battery level
     * @param deviceAddress MAC address of the device
     * @return Battery percentage or null
     */
    suspend fun getDeviceBatteryLevel(deviceAddress: String): Int?
}
