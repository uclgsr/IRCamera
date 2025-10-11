package mpdc4gsr.core.data

import android.content.Context
import com.mpdc4gsr.component.shared.app.repository.BaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class SensorDataRepository(
    private val context: Context,
) : BaseRepository() {
    companion object {
        private const val DEVICE_STATUS_CACHE_KEY = "device_status"
        private const val DEVICE_STATUS_TTL = 30 * 1000L // 30 seconds for device status
    }

    // Data classes for type-safe sensor data
    data class GSRSensorData(
        val timestamp: Long,
        val gsrValue: Double,
        val resistance: Double,
        val conductance: Double,
        val quality: DataQuality,
        val deviceId: String,
        val batteryLevel: Int? = null,
    )

    data class ThermalSensorData(
        val timestamp: Long,
        val frameData: ByteArray,
        val width: Int,
        val height: Int,
        val minTemp: Float,
        val maxTemp: Float,
        val avgTemp: Float,
        val deviceId: String,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as ThermalSensorData
            return timestamp == other.timestamp && deviceId == other.deviceId
        }

        override fun hashCode(): Int = timestamp.hashCode() * 31 + deviceId.hashCode()
    }

    data class DeviceStatus(
        val deviceId: String,
        val deviceType: DeviceType,
        val isConnected: Boolean,
        val batteryLevel: Int?,
        val signalStrength: Int?,
        val lastSeen: Long,
        val firmwareVersion: String?,
    )

    enum class DataQuality {
        EXCELLENT,
        GOOD,
        FAIR,
        POOR,
        UNKNOWN,
    }

    enum class DeviceType {
        TC007,
        TS004,
        SHIMMER_GSR,
        UNKNOWN,
    }

    enum class SensorType {
        GSR,
        THERMAL,
        PPG,
        ACCELEROMETER,
    }

    fun getGSRDataStream(deviceId: String): Flow<BaseRepository.Result<GSRSensorData>> =
        safeFlow {
            throw NotImplementedError("GSR data stream requires actual sensor connection. Simulation removed.")
        }

    fun getThermalDataStream(deviceId: String): Flow<BaseRepository.Result<ThermalSensorData>> =
        safeFlow {
            throw NotImplementedError("Thermal data stream requires actual sensor connection. Simulation removed.")
        }

    fun getDeviceStatus(deviceId: String): Flow<BaseRepository.Result<DeviceStatus>> =
        safeFlow {
            val cacheKey = "${DEVICE_STATUS_CACHE_KEY}_$deviceId"
            getCachedOrExecute(cacheKey, DEVICE_STATUS_TTL) {
                fetchDeviceStatus(deviceId)
            }
        }

    fun getCombinedSensorData(deviceIds: List<String>): Flow<BaseRepository.Result<CombinedSensorData>> {
        val gsrStreams = deviceIds.map { getGSRDataStream(it) }
        val thermalStreams = deviceIds.map { getThermalDataStream(it) }
        return combine(gsrStreams + thermalStreams) { results ->
            val gsrData =
                results.take(deviceIds.size).mapNotNull { result ->
                    if (result is BaseRepository.Result.Success<*>) {
                        @Suppress("UNCHECKED_CAST")
                        (result.data as? GSRSensorData)
                    } else {
                        null
                    }
                }
            val thermalData =
                results.drop(deviceIds.size).mapNotNull { result ->
                    if (result is BaseRepository.Result.Success<*>) {
                        @Suppress("UNCHECKED_CAST")
                        (result.data as? ThermalSensorData)
                    } else {
                        null
                    }
                }
            BaseRepository.Result.Success(CombinedSensorData(gsrData, thermalData))
        }
    }

    data class CombinedSensorData(
        val gsrData: List<GSRSensorData>,
        val thermalData: List<ThermalSensorData>,
    ) {
        val timestamp: Long = System.currentTimeMillis()
        val hasData: Boolean = gsrData.isNotEmpty() || thermalData.isNotEmpty()
    }

    private suspend fun fetchDeviceStatus(deviceId: String): DeviceStatus =
        DeviceStatus(
            deviceId = deviceId,
            deviceType =
                when {
                    deviceId.contains("TC007") -> DeviceType.TC007
                    deviceId.contains("TS004") -> DeviceType.TS004
                    deviceId.contains("SHIMMER") -> DeviceType.SHIMMER_GSR
                    else -> DeviceType.UNKNOWN
                },
            isConnected = false,
            batteryLevel = null,
            signalStrength = null,
            lastSeen = 0L,
            firmwareVersion = null,
        )
}


