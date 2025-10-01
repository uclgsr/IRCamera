package mpdc4gsr.feature.thermal.data.source

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow

/**
 * Data source interface for Topdon TC001/TC007 thermal camera SDK integration.
 *
 * Abstracts the Topdon SDK to provide a clean interface for thermal camera operations.
 */
interface TopdonDataSource {

    /**
     * Connect to thermal camera device via USB
     * @return Result indicating success or failure
     */
    suspend fun connectDevice(): Result<Unit>

    /**
     * Disconnect from thermal camera
     */
    suspend fun disconnectDevice()

    /**
     * Start streaming thermal frames
     * @return Flow of thermal frame data
     */
    suspend fun startStreaming(): Flow<ThermalFrameData>

    /**
     * Stop streaming thermal frames
     */
    suspend fun stopStreaming()

    /**
     * Capture a thermal snapshot
     * @return Result with bitmap and temperature data
     */
    suspend fun captureSnapshot(): Result<ThermalSnapshot>

    /**
     * Start recording thermal video
     * @return Result indicating success or failure
     */
    suspend fun startRecording(): Result<Unit>

    /**
     * Stop recording and save thermal video
     * @return Result with file path
     */
    suspend fun stopRecording(): Result<String>

    /**
     * Check if device is connected
     */
    fun isConnected(): Boolean

    /**
     * Set temperature range for visualization
     */
    suspend fun setTemperatureRange(min: Float, max: Float): Result<Unit>
}

/**
 * Thermal frame data from camera
 */
data class ThermalFrameData(
    val timestamp: Long,
    val bitmap: Bitmap,
    val temperatureMatrix: Array<FloatArray>,
    val minTemp: Float,
    val maxTemp: Float,
    val centerTemp: Float
)

/**
 * Thermal snapshot with metadata
 */
data class ThermalSnapshot(
    val bitmap: Bitmap,
    val temperatureMatrix: Array<FloatArray>,
    val minTemp: Float,
    val maxTemp: Float,
    val timestamp: Long,
    val location: String? = null
)
