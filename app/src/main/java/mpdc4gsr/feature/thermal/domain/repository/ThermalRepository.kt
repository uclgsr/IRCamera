package mpdc4gsr.feature.thermal.domain.repository

import kotlinx.coroutines.flow.Flow
import mpdc4gsr.feature.thermal.data.source.ThermalFrameData
import mpdc4gsr.feature.thermal.data.source.ThermalSnapshot

/**
 * Repository interface for thermal camera operations.
 * 
 * Domain layer interface for thermal camera functionality.
 */
interface ThermalRepository {
    
    /**
     * Connect to thermal camera
     * @return Result indicating success or failure
     */
    suspend fun connectCamera(): Result<Unit>
    
    /**
     * Disconnect from thermal camera
     */
    suspend fun disconnectCamera()
    
    /**
     * Stream thermal frames from camera
     * @return Flow of thermal frame data
     */
    suspend fun getThermalStream(): Flow<ThermalFrameData>
    
    /**
     * Stop thermal stream
     */
    suspend fun stopStream()
    
    /**
     * Capture thermal snapshot
     * @return Result with snapshot data
     */
    suspend fun captureSnapshot(): Result<ThermalSnapshot>
    
    /**
     * Start recording thermal video
     * @return Result indicating success or failure
     */
    suspend fun startRecording(): Result<Unit>
    
    /**
     * Stop recording and get file path
     * @return Result with recording file path
     */
    suspend fun stopRecording(): Result<String>
    
    /**
     * Check camera connection status
     * @return True if connected
     */
    fun isCameraConnected(): Boolean
    
    /**
     * Configure temperature range
     * @param minTemp Minimum temperature for range
     * @param maxTemp Maximum temperature for range
     */
    suspend fun setTemperatureRange(minTemp: Float, maxTemp: Float): Result<Unit>
}
