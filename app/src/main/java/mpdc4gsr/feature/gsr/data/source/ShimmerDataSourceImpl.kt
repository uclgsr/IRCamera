package mpdc4gsr.feature.gsr.data.source

import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mpdc4gsr.core.data.ShimmerDeviceManager
import mpdc4gsr.core.data.model.DeviceInfo
import mpdc4gsr.core.data.model.GSRSample

class ShimmerDataSourceImpl(
    private val deviceManager: ShimmerDeviceManager
) : ShimmerDataSource {
    companion object {
        private const val TAG = "ShimmerDataSourceImpl"
        private const val DEFAULT_DEVICE_NAME = "Shimmer3"
        private const val DEFAULT_DEVICE_TYPE = "Shimmer3-GSR"
        private const val DEFAULT_RSSI = -50
    }

    private val scannedDevices = mutableMapOf<String, DeviceInfo>()
    override suspend fun scanForDevices(): Flow<List<DeviceInfo>> {
        deviceManager.initialize()
        deviceManager.startDeviceScanning()
        return deviceManager.scanResults
    }

    override suspend fun connect(deviceAddress: String): Result<Unit> {
        return try {
            AppLogger.d(TAG, "Connecting to device: $deviceAddress")
            val deviceInfo = scannedDevices[deviceAddress] ?: run {
                AppLogger.w(TAG, "Device info not found in scan results, using defaults for: $deviceAddress")
                DeviceInfo(
                    address = deviceAddress,
                    name = DEFAULT_DEVICE_NAME,
                    deviceType = DEFAULT_DEVICE_TYPE,
                    rssi = DEFAULT_RSSI,
                    isGSRCapable = true
                )
            }
            val success = deviceManager.connectToDevice(deviceInfo)
            if (success) {
                AppLogger.i(TAG, "Successfully connected to device: $deviceAddress")
                Result.success(Unit)
            } else {
                AppLogger.e(TAG, "Failed to connect to device: $deviceAddress")
                Result.failure(Exception("Connection failed"))
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error connecting to device: $deviceAddress", e)
            Result.failure(e)
        }
    }

    fun cacheDeviceInfo(devices: List<DeviceInfo>) {
        devices.forEach { device ->
            scannedDevices[device.address] = device
        }
    }

    override suspend fun disconnect(deviceAddress: String) {
        try {
            AppLogger.d(TAG, "Disconnecting device: $deviceAddress")
            deviceManager.disconnectDevice(deviceAddress)
            AppLogger.i(TAG, "Successfully disconnected device: $deviceAddress")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error disconnecting device: $deviceAddress", e)
        }
    }

    override suspend fun startStreaming(deviceAddress: String): Flow<GSRSample> {
        return flow {
            AppLogger.d(TAG, "Starting GSR streaming for device: $deviceAddress")
            AppLogger.w(TAG, "Note: GSR streaming implementation requires Shimmer SDK callback integration")
            AppLogger.w(TAG, "This is a placeholder that emits no data - actual implementation requires")
            AppLogger.w(TAG, "registering callbacks with ShimmerBluetoothManagerAndroid for data packets")
        }
    }

    override suspend fun stopStreaming(deviceAddress: String) {
        try {
            AppLogger.d(TAG, "Stopping streaming for device: $deviceAddress")
            AppLogger.w(TAG, "Note: Stopping streaming requires Shimmer SDK integration")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error stopping streaming for device: $deviceAddress", e)
        }
    }

    override fun isConnected(deviceAddress: String): Boolean {
        val connected = deviceManager.shimmerBluetoothManager?.let { mgr ->
            mgr.getShimmerDeviceBtConnectedFromMac(deviceAddress)?.let { shimmer ->
                shimmer.isConnected
            }
        } ?: false
        AppLogger.d(TAG, "Device $deviceAddress connection status: $connected")
        return connected
    }

    override suspend fun getBatteryLevel(deviceAddress: String): Int? {
        return try {
            val shimmer = deviceManager.shimmerBluetoothManager?.getShimmerDeviceBtConnectedFromMac(deviceAddress)
            if (shimmer != null) {
                AppLogger.d(TAG, "Shimmer device found for battery query: $deviceAddress")
                AppLogger.w(TAG, "Note: Battery level reading requires Shimmer SDK state parsing")
                null
            } else {
                AppLogger.w(TAG, "Shimmer device not found for battery query: $deviceAddress")
                null
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error getting battery level for device: $deviceAddress", e)
            null
        }
    }
}
