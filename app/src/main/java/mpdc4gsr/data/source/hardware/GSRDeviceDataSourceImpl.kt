package mpdc4gsr.data.source

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mpdc4gsr.core.data.model.DeviceInfo
import mpdc4gsr.core.data.model.GSRSample
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
import javax.inject.Inject

class GSRDeviceDataSourceImpl @Inject constructor(
    private val shimmerManager: ShimmerBluetoothManagerAndroid
) : GSRDeviceDataSource {
    companion object {
        private const val TAG = "GSRDeviceDataSourceImpl"
        private const val DEFAULT_DEVICE_NAME = "Shimmer3"
        private const val DEFAULT_DEVICE_TYPE = "Shimmer3-GSR"
        private const val DEFAULT_RSSI = -50
    }

    private val scannedDevices = mutableMapOf<String, DeviceInfo>()
    override suspend fun scanForDevices(): Flow<List<DeviceInfo>> {
        // Use Shimmer SDK to scan for devices
        return flow {
            emit(scannedDevices.values.toList())
        }
    }

    override suspend fun connect(deviceAddress: String): Result<Unit> {
        return try {
            val deviceInfo = scannedDevices[deviceAddress] ?: run {
                DeviceInfo(
                    address = deviceAddress,
                    name = DEFAULT_DEVICE_NAME,
                    deviceType = DEFAULT_DEVICE_TYPE,
                    rssi = DEFAULT_RSSI,
                    isGSRCapable = true
                )
            }
            val shimmer = shimmerManager.getShimmerDeviceBtConnectedFromMac(deviceAddress)
            if (shimmer != null) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Connection failed"))
            }
        } catch (e: Exception) {
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
            val shimmer = shimmerManager.getShimmerDeviceBtConnectedFromMac(deviceAddress)
            shimmer?.disconnect()
        } catch (e: Exception) {
        }
    }

    override suspend fun startStreaming(deviceAddress: String): Flow<GSRSample> {
        return flow {
            val shimmer = shimmerManager.getShimmerDeviceBtConnectedFromMac(deviceAddress)
            shimmer?.let {
                it.startStreaming()
            }
        }
    }

    override suspend fun stopStreaming(deviceAddress: String) {
        try {
            val shimmer = shimmerManager.getShimmerDeviceBtConnectedFromMac(deviceAddress)
            shimmer?.stopStreaming()
        } catch (e: Exception) {
        }
    }

    override fun isConnected(deviceAddress: String): Boolean {
        val connected = shimmerManager.getShimmerDeviceBtConnectedFromMac(deviceAddress)?.let { shimmer ->
            shimmer.isConnected
        } ?: false
        return connected
    }

    override suspend fun getBatteryLevel(deviceAddress: String): Int? {
        return try {
            val shimmer = shimmerManager.getShimmerDeviceBtConnectedFromMac(deviceAddress)
            shimmer?.let {
                val batteryVoltage = it.batteryVoltage
                val batteryPercentage = calculateBatteryPercentage(batteryVoltage)
                batteryPercentage
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateBatteryPercentage(voltage: Double): Int {
        val minVoltage = 3.0
        val maxVoltage = 4.2
        val percentage = ((voltage - minVoltage) / (maxVoltage - minVoltage) * 100.0).coerceIn(0.0, 100.0)
        return percentage.toInt()
    }
}
