package mpdc4gsr.repository

import android.content.Context
import com.mpdc4gsr.libunified.app.repository.BaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.delay

/**
 * Modern Sensor Data Repository demonstrating advanced MVVM patterns
 * Consolidates all sensor data operations with proper caching and error handling
 */
class SensorDataRepository(
    private val context: Context
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
        val batteryLevel: Int? = null
    )

    data class ThermalSensorData(
        val timestamp: Long,
        val frameData: ByteArray,
        val width: Int,
        val height: Int,
        val minTemp: Float,
        val maxTemp: Float,
        val avgTemp: Float,
        val deviceId: String
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as ThermalSensorData
            return timestamp == other.timestamp && deviceId == other.deviceId
        }

        override fun hashCode(): Int {
            return timestamp.hashCode() * 31 + deviceId.hashCode()
        }
    }

    data class DeviceStatus(
        val deviceId: String,
        val deviceType: DeviceType,
        val isConnected: Boolean,
        val batteryLevel: Int?,
        val signalStrength: Int?,
        val lastSeen: Long,
        val firmwareVersion: String?
    )

    enum class DataQuality {
        EXCELLENT, GOOD, FAIR, POOR, UNKNOWN
    }

    enum class DeviceType {
        TC007, TS004, SHIMMER_GSR, UNKNOWN
    }

    enum class SensorType {
        GSR, THERMAL, PPG, ACCELEROMETER
    }

    /**
     * Get real-time GSR sensor data stream
     */
    fun getGSRDataStream(deviceId: String): Flow<BaseRepository.Result<GSRSensorData>> = safeFlow {
        simulateGSRDataStream(deviceId)
    }

    /**
     * Get thermal sensor data stream
     */
    fun getThermalDataStream(deviceId: String): Flow<BaseRepository.Result<ThermalSensorData>> = safeFlow {
        simulateThermalDataStream(deviceId)
    }

    /**
     * Get device status with caching
     */
    fun getDeviceStatus(deviceId: String): Flow<BaseRepository.Result<DeviceStatus>> = safeFlow {
        val cacheKey = "${DEVICE_STATUS_CACHE_KEY}_$deviceId"
        
        getCachedOrExecute(cacheKey, DEVICE_STATUS_TTL) {
            fetchDeviceStatus(deviceId)
        }
    }

    /**
     * Get combined sensor data from multiple sources
     */
    fun getCombinedSensorData(deviceIds: List<String>): Flow<BaseRepository.Result<CombinedSensorData>> {
        val gsrStreams = deviceIds.map { getGSRDataStream(it) }
        val thermalStreams = deviceIds.map { getThermalDataStream(it) }
        
        return combine(gsrStreams + thermalStreams) { results ->
            val gsrData = results.take(deviceIds.size).mapNotNull { 
                if (it is BaseRepository.Result.Success) it.data else null 
            }
            val thermalData = results.drop(deviceIds.size).mapNotNull { 
                if (it is BaseRepository.Result.Success) it.data else null 
            }
            
            BaseRepository.Result.Success(CombinedSensorData(gsrData, thermalData))
        }
    }

    data class CombinedSensorData(
        val gsrData: List<GSRSensorData>,
        val thermalData: List<ThermalSensorData>
    ) {
        val timestamp: Long = System.currentTimeMillis()
        val hasData: Boolean = gsrData.isNotEmpty() || thermalData.isNotEmpty()
    }

    // Private helper methods for simulation
    private suspend fun simulateGSRDataStream(deviceId: String): GSRSensorData {
        delay(100) // Simulate network delay
        
        val gsrValue = 5.0 + (Math.random() * 10.0) // 5-15 µS
        val resistance = 1_000_000 / gsrValue // Ohm's law approximation
        val conductance = 1.0 / resistance * 1_000_000 // µS
        
        return GSRSensorData(
            timestamp = System.currentTimeMillis(),
            gsrValue = gsrValue,
            resistance = resistance,
            conductance = conductance,
            quality = DataQuality.GOOD,
            deviceId = deviceId,
            batteryLevel = (70..100).random()
        )
    }

    private suspend fun simulateThermalDataStream(deviceId: String): ThermalSensorData {
        delay(50) // Simulate faster thermal data
        
        val width = 640
        val height = 480
        val frameData = ByteArray(width * height * 2) // Simulated 16-bit thermal data
        
        return ThermalSensorData(
            timestamp = System.currentTimeMillis(),
            frameData = frameData,
            width = width,
            height = height,
            minTemp = 20.0f + (Math.random() * 5).toFloat(),
            maxTemp = 35.0f + (Math.random() * 10).toFloat(),
            avgTemp = 25.0f + (Math.random() * 8).toFloat(),
            deviceId = deviceId
        )
    }

    private suspend fun fetchDeviceStatus(deviceId: String): DeviceStatus {
        delay(200) // Simulate network request
        
        return DeviceStatus(
            deviceId = deviceId,
            deviceType = when {
                deviceId.contains("TC007") -> DeviceType.TC007
                deviceId.contains("TS004") -> DeviceType.TS004
                deviceId.contains("SHIMMER") -> DeviceType.SHIMMER_GSR
                else -> DeviceType.UNKNOWN
            },
            isConnected = Math.random() > 0.2, // 80% chance of being connected
            batteryLevel = (30..100).random(),
            signalStrength = (60..100).random(),
            lastSeen = System.currentTimeMillis() - (Math.random() * 60000).toLong(),
            firmwareVersion = "1.${(0..9).random()}.${(0..9).random()}"
        )
    }
}