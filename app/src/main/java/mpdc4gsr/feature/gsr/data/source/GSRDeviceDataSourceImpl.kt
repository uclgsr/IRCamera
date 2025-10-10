package mpdc4gsr.feature.gsr.data.source

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import mpdc4gsr.core.sensors.gsr.GsrDeviceManager
import mpdc4gsr.core.sensors.gsr.model.DeviceInfo
import mpdc4gsr.core.sensors.gsr.model.GSRSample
import mpdc4gsr.core.sensors.gsr.toGsrSamplePayload
import javax.inject.Inject

class GSRDeviceDataSourceImpl
@Inject
constructor(
    private val gsrDeviceManager: GsrDeviceManager,
) : GSRDeviceDataSource {
    companion object {
        private const val TAG = "GSRDeviceDataSourceImpl"
        private const val DEFAULT_DEVICE_NAME = "Shimmer3"
        private const val DEFAULT_DEVICE_TYPE = "Shimmer3-GSR"
        private const val DEFAULT_RSSI = -50
    }

    private val deviceCache = mutableMapOf<String, DeviceInfo>()

    override suspend fun scanForDevices(): Flow<List<DeviceInfo>> =
        gsrDeviceManager.scanResults
            .onStart {
                gsrDeviceManager.startDeviceScanning()
            }
            .onEach { devices ->
                devices.forEach { device ->
                    deviceCache[device.address] = device
                }
            }
            .onCompletion {
                gsrDeviceManager.stopDeviceScanning()
            }

    override suspend fun connect(deviceAddress: String): Result<Unit> =
        try {
            val deviceInfo =
                deviceCache[deviceAddress]
                    ?: gsrDeviceManager.getDeviceInfo(deviceAddress)
                    ?: DeviceInfo(
                        address = deviceAddress,
                        name = DEFAULT_DEVICE_NAME,
                        deviceType = DEFAULT_DEVICE_TYPE,
                        rssi = DEFAULT_RSSI,
                        isGSRCapable = true,
                    )
            val connected = gsrDeviceManager.connectToDevice(deviceInfo)
            if (connected) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Connection failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    fun cacheDeviceInfo(devices: List<DeviceInfo>) {
        devices.forEach { device ->
            deviceCache[device.address] = device
        }
    }

    override suspend fun disconnect(deviceAddress: String) {
        try {
            gsrDeviceManager.disconnectDevice(deviceAddress)
        } catch (e: Exception) {
            mpdc4gsr.core.utils.AppLogger
                .e("GSRDeviceDataSourceImpl", "Unexpected Exception in GSRDeviceDataSourceImpl catch block", e)
        }
    }

    override suspend fun startStreaming(deviceAddress: String): Flow<GSRSample> =
        gsrDeviceManager.dataEvents
            .filter { (mac, _) -> mac.equals(deviceAddress, ignoreCase = true) }
            .mapNotNull { (_, cluster) -> cluster.toGsrSamplePayload()?.sample }
            .onStart {
                gsrDeviceManager.getConnectedShimmer(deviceAddress)?.startStreaming()
            }

    override suspend fun stopStreaming(deviceAddress: String) {
        try {
            gsrDeviceManager.getConnectedShimmer(deviceAddress)?.stopStreaming()
        } catch (e: Exception) {
            mpdc4gsr.core.utils.AppLogger
                .e("GSRDeviceDataSourceImpl", "Unexpected Exception in GSRDeviceDataSourceImpl catch block", e)
        }
    }

    override fun isConnected(deviceAddress: String): Boolean =
        gsrDeviceManager.isDeviceConnected(deviceAddress)

    override suspend fun getBatteryLevel(deviceAddress: String): Int? =
        try {
            val shimmer = gsrDeviceManager.getConnectedShimmer(deviceAddress)
            shimmer?.batteryVoltage?.let { voltage ->
                calculateBatteryPercentage(voltage)
            }
        } catch (e: Exception) {
            null
        }

    private fun calculateBatteryPercentage(voltage: Double): Int {
        val minVoltage = 3.0
        val maxVoltage = 4.2
        val percentage = ((voltage - minVoltage) / (maxVoltage - minVoltage) * 100.0).coerceIn(0.0, 100.0)
        return percentage.toInt()
    }
}
