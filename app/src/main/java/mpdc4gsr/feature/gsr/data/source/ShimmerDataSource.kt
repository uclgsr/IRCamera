package mpdc4gsr.feature.gsr.data.source

import kotlinx.coroutines.flow.Flow
import mpdc4gsr.core.data.model.DeviceInfo
import mpdc4gsr.core.data.model.GSRSample

/**
 * Data source interface for Shimmer3 GSR+ device SDK integration.
 *
 * Abstracts the Shimmer SDK to provide a clean interface for data operations.
 * This allows for:
 * - Easy testing with mock implementations
 * - SDK version independence
 * - Clear separation of SDK logic from business logic
 */
interface ShimmerDataSource {

    /**
     * Scan for available Shimmer devices via Bluetooth
     * @return Flow of discovered devices
     */
    suspend fun scanForDevices(): Flow<List<DeviceInfo>>

    /**
     * Connect to a Shimmer device
     * @param deviceAddress MAC address of the device
     * @return Result indicating success or failure
     */
    suspend fun connect(deviceAddress: String): Result<Unit>

    /**
     * Disconnect from a Shimmer device
     * @param deviceAddress MAC address of the device
     */
    suspend fun disconnect(deviceAddress: String)

    /**
     * Start streaming GSR data from connected device
     * @param deviceAddress MAC address of the device
     * @return Flow of GSR samples
     */
    suspend fun startStreaming(deviceAddress: String): Flow<GSRSample>

    /**
     * Stop streaming GSR data
     * @param deviceAddress MAC address of the device
     */
    suspend fun stopStreaming(deviceAddress: String)

    /**
     * Get connection state of a device
     * @param deviceAddress MAC address of the device
     * @return True if connected, false otherwise
     */
    fun isConnected(deviceAddress: String): Boolean

    /**
     * Get battery level of connected device
     * @param deviceAddress MAC address of the device
     * @return Battery level percentage (0-100) or null if not available
     */
    suspend fun getBatteryLevel(deviceAddress: String): Int?
}
