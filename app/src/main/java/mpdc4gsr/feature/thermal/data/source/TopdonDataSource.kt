package mpdc4gsr.feature.thermal.data.source

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow

interface TopdonDataSource {

    suspend fun connectDevice(): Result<Unit>

    suspend fun disconnectDevice()

    suspend fun startStreaming(): Flow<ThermalFrameData>

    suspend fun stopStreaming()

    suspend fun captureSnapshot(): Result<ThermalSnapshot>

    suspend fun startRecording(): Result<Unit>

    suspend fun stopRecording(): Result<String>

    fun isConnected(): Boolean

    suspend fun setTemperatureRange(min: Float, max: Float): Result<Unit>
}

data class ThermalFrameData(
    val timestamp: Long,
    val bitmap: Bitmap,
    val temperatureMatrix: Array<FloatArray>,
    val minTemp: Float,
    val maxTemp: Float,
    val centerTemp: Float
)

data class ThermalSnapshot(
    val bitmap: Bitmap,
    val temperatureMatrix: Array<FloatArray>,
    val minTemp: Float,
    val maxTemp: Float,
    val timestamp: Long,
    val location: String? = null
)
