package mpdc4gsr.feature.gsr.data.source

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mpdc4gsr.core.data.ShimmerDeviceManager
import mpdc4gsr.core.data.model.DeviceInfo
import mpdc4gsr.core.data.model.GSRSample

class ShimmerDataSourceImpl(
    private val deviceManager: ShimmerDeviceManager
) : ShimmerDataSource {
    companion object {
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
        return (
            val deviceInfo = scannedDevices[deviceAddress] ?: run {
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
                Result.success(Unit)
            } else {
                Result.failure(Exception("Connection failed"))
            }
            Result.failure(e)
        }
    }

    fun cacheDeviceInfo(devices: List<DeviceInfo>) {
        devices.forEach { device ->
            scannedDevices[device.address] = device
        }
    }

    override suspend fun disconnect(deviceAddress: String) {
            deviceManager.disconnectDevice(deviceAddress)
        }
    }

    override suspend fun startStreaming(deviceAddress: String): Flow<GSRSample> {
        return flow {
        }
    }

    override suspend fun stopStreaming(deviceAddress: String) {
        }
    }

    override fun isConnected(deviceAddress: String): Boolean {
        val connected = deviceManager.shimmerBluetoothManager?.let { mgr ->
            mgr.getShimmerDeviceBtConnectedFromMac(deviceAddress)?.let { shimmer ->
                shimmer.isConnected
            }
        } ?: false
        return connected
    }

    override suspend fun getBatteryLevel(deviceAddress: String): Int? {
        return (
            val shimmer = deviceManager.shimmerBluetoothManager?.getShimmerDeviceBtConnectedFromMac(deviceAddress)
            if (shimmer != null) {
                null
            } else {
                null
            }
            null
        }
    }
}
