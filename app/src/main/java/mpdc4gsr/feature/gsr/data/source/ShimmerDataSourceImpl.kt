package mpdc4gsr.feature.gsr.data.source

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mpdc4gsr.core.data.ShimmerDeviceManager
import mpdc4gsr.core.data.model.DeviceInfo
import mpdc4gsr.core.data.model.GSRSample
import android.util.Log

/**
 * Implementation of ShimmerDataSource that wraps ShimmerDeviceManager.
 * 
 * This class adapts the existing ShimmerDeviceManager to conform to the
 * ShimmerDataSource interface, providing a clean abstraction over the Shimmer SDK.
 */
class ShimmerDataSourceImpl(
    private val deviceManager: ShimmerDeviceManager
) : ShimmerDataSource {
    
    companion object {
        private const val TAG = "ShimmerDataSourceImpl"
    }
    
    override suspend fun scanForDevices(): Flow<List<DeviceInfo>> {
        deviceManager.initialize()
        deviceManager.startDeviceScanning()
        return deviceManager.scanResults
    }
    
    override suspend fun connect(deviceAddress: String): Result<Unit> {
        return try {
            Log.d(TAG, "Connecting to device: $deviceAddress")
            val deviceInfo = DeviceInfo(
                address = deviceAddress,
                name = "Shimmer3",
                deviceType = "Shimmer3-GSR",
                rssi = -50,
                isGSRCapable = true
            )
            val success = deviceManager.connectToDevice(deviceInfo)
            if (success) {
                Log.i(TAG, "Successfully connected to device: $deviceAddress")
                Result.success(Unit)
            } else {
                Log.e(TAG, "Failed to connect to device: $deviceAddress")
                Result.failure(Exception("Connection failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to device: $deviceAddress", e)
            Result.failure(e)
        }
    }
    
    override suspend fun disconnect(deviceAddress: String) {
        try {
            Log.d(TAG, "Disconnecting device: $deviceAddress")
            deviceManager.disconnectDevice(deviceAddress)
            Log.i(TAG, "Successfully disconnected device: $deviceAddress")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting device: $deviceAddress", e)
        }
    }
    
    override suspend fun startStreaming(deviceAddress: String): Flow<GSRSample> {
        return flow {
            Log.d(TAG, "Starting GSR streaming for device: $deviceAddress")
            Log.w(TAG, "Note: GSR streaming implementation requires Shimmer SDK callback integration")
            Log.w(TAG, "This is a placeholder that emits no data - actual implementation requires")
            Log.w(TAG, "registering callbacks with ShimmerBluetoothManagerAndroid for data packets")
        }
    }
    
    override suspend fun stopStreaming(deviceAddress: String) {
        try {
            Log.d(TAG, "Stopping streaming for device: $deviceAddress")
            Log.w(TAG, "Note: Stopping streaming requires Shimmer SDK integration")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping streaming for device: $deviceAddress", e)
        }
    }
    
    override fun isConnected(deviceAddress: String): Boolean {
        val connected = deviceManager.shimmerBluetoothManager?.let { mgr ->
            mgr.getShimmerDeviceBtConnectedFromMac(deviceAddress)?.let { shimmer ->
                shimmer.isConnected
            }
        } ?: false
        Log.d(TAG, "Device $deviceAddress connection status: $connected")
        return connected
    }
    
    override suspend fun getBatteryLevel(deviceAddress: String): Int? {
        return try {
            val shimmer = deviceManager.shimmerBluetoothManager?.getShimmerDeviceBtConnectedFromMac(deviceAddress)
            if (shimmer != null) {
                Log.d(TAG, "Shimmer device found for battery query: $deviceAddress")
                Log.w(TAG, "Note: Battery level reading requires Shimmer SDK state parsing")
                null
            } else {
                Log.w(TAG, "Shimmer device not found for battery query: $deviceAddress")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting battery level for device: $deviceAddress", e)
            null
        }
    }
}
