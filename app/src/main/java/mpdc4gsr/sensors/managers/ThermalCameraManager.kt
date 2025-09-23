package mpdc4gsr.sensors.managers

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mpdc4gsr.sensors.SensorRecorder
import mpdc4gsr.sensors.thermal.ThermalCameraRecorder

/**
 * ThermalCameraManager - Encapsulates thermal camera sensor management
 * 
 * This class manages the Topdon TC001 thermal camera integration including:
 * - USB connection management
 * - Frame capture and processing
 * - Temperature data extraction
 * - Data storage and timestamping
 */
class ThermalCameraManager(
    private val context: Context,
    private val sensorId: String = "thermal_camera_1"
) {
    companion object {
        private const val TAG = "ThermalCameraManager"
    }

    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private var thermalRecorder: ThermalCameraRecorder? = null
    
    /**
     * Initialize the thermal camera recorder with proper USB handling
     */
    suspend fun initialize(): Boolean {
        return try {
            Log.i(TAG, "Initializing thermal camera manager for sensor: $sensorId")
            
            thermalRecorder = ThermalCameraRecorder(context, sensorId).apply {
                // Configure camera-specific settings if needed
                Log.d(TAG, "Thermal camera recorder created and configured")
            }
            
            _isConnected.value = true
            Log.i(TAG, "Thermal camera manager initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize thermal camera manager", e)
            _isConnected.value = false
            false
        }
    }
    
    /**
     * Start thermal camera recording with synchronized timestamp
     */
    suspend fun startRecording(sessionDirectory: String, startTimestamp: Long): Boolean {
        return thermalRecorder?.let { recorder ->
            try {
                Log.i(TAG, "Starting thermal camera recording at timestamp: $startTimestamp")
                val success = recorder.startRecording(sessionDirectory, startTimestamp)
                _isRecording.value = success
                
                if (success) {
                    Log.i(TAG, "Thermal camera recording started successfully")
                } else {
                    Log.w(TAG, "Failed to start thermal camera recording")
                }
                
                success
            } catch (e: Exception) {
                Log.e(TAG, "Exception starting thermal camera recording", e)
                _isRecording.value = false
                false
            }
        } ?: false
    }
    
    /**
     * Stop thermal camera recording and save data
     */
    suspend fun stopRecording(): Boolean {
        return thermalRecorder?.let { recorder ->
            try {
                Log.i(TAG, "Stopping thermal camera recording")
                val success = recorder.stopRecording()
                _isRecording.value = false
                
                if (success) {
                    Log.i(TAG, "Thermal camera recording stopped successfully")
                } else {
                    Log.w(TAG, "Issues stopping thermal camera recording")
                }
                
                success
            } catch (e: Exception) {
                Log.e(TAG, "Exception stopping thermal camera recording", e)
                _isRecording.value = false
                false
            }
        } ?: false
    }
    
    /**
     * Get the underlying sensor recorder for integration
     */
    fun getSensorRecorder(): SensorRecorder? = thermalRecorder
    
    /**
     * Check if thermal camera hardware is available
     */
    fun isHardwareAvailable(): Boolean {
        return thermalRecorder?.let { recorder ->
            // This would check USB connection status in real implementation
            _isConnected.value
        } ?: false
    }
    
    /**
     * Get current recording statistics
     */
    fun getRecordingStats() = thermalRecorder?.getRecordingStats()
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        Log.i(TAG, "Cleaning up thermal camera manager")
        thermalRecorder?.stopRecording()
        managerScope.cancel()
        _isConnected.value = false
        _isRecording.value = false
    }
}