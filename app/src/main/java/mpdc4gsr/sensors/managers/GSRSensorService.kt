package mpdc4gsr.sensors.managers

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.controller.RecordingController
import mpdc4gsr.sensors.SensorRecorder
import mpdc4gsr.sensors.gsr.GSRSensorRecorder

/**
 * GSRSensorService - Encapsulates GSR sensor management via Shimmer3 BLE
 * 
 * This class manages the Shimmer3 GSR+ sensor integration including:
 * - Bluetooth Low Energy connection management
 * - GSR data streaming and processing
 * - Sample rate configuration
 * - Data storage and timestamping
 */
class GSRSensorService(
    private val context: Context,
    private val recordingController: RecordingController,
    private val sensorId: String = "gsr_shimmer_1",
    private val samplingRateHz: Int = 128
) {
    companion object {
        private const val TAG = "GSRSensorService"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _currentGSRValue = MutableStateFlow(0.0)
    val currentGSRValue: StateFlow<Double> = _currentGSRValue.asStateFlow()
    
    private var gsrRecorder: GSRSensorRecorder? = null
    
    /**
     * Initialize the GSR sensor recorder with BLE configuration
     */
    suspend fun initialize(): Boolean {
        return try {
            Log.i(TAG, "Initializing GSR sensor service for sensor: $sensorId")
            
            gsrRecorder = GSRSensorRecorder(
                context, 
                sensorId, 
                samplingRateHz,
                recordingController
            ).apply {
                Log.d(TAG, "GSR sensor recorder created with sampling rate: ${samplingRateHz}Hz")
            }
            
            // Monitor BLE connection status
            serviceScope.launch {
                gsrRecorder?.statusFlow?.collect { status ->
                    _isConnected.value = status.isHealthy()
                    _isRecording.value = status.isRecording()
                }
            }
            
            _isConnected.value = true
            Log.i(TAG, "GSR sensor service initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize GSR sensor service", e)
            _isConnected.value = false
            false
        }
    }
    
    /**
     * Start GSR recording with synchronized timestamp
     */
    suspend fun startRecording(sessionDirectory: String, startTimestamp: Long): Boolean {
        return gsrRecorder?.let { recorder ->
            try {
                Log.i(TAG, "Starting GSR recording at timestamp: $startTimestamp")
                val success = recorder.startRecording(sessionDirectory, startTimestamp)
                _isRecording.value = success
                
                if (success) {
                    Log.i(TAG, "GSR recording started successfully")
                    startGSRMonitoring()
                } else {
                    Log.w(TAG, "Failed to start GSR recording")
                }
                
                success
            } catch (e: Exception) {
                Log.e(TAG, "Exception starting GSR recording", e)
                _isRecording.value = false
                false
            }
        } ?: false
    }
    
    /**
     * Stop GSR recording and save data
     */
    suspend fun stopRecording(): Boolean {
        return gsrRecorder?.let { recorder ->
            try {
                Log.i(TAG, "Stopping GSR recording")
                val success = recorder.stopRecording()
                _isRecording.value = false
                
                if (success) {
                    Log.i(TAG, "GSR recording stopped successfully")
                } else {
                    Log.w(TAG, "Issues stopping GSR recording")
                }
                
                success
            } catch (e: Exception) {
                Log.e(TAG, "Exception stopping GSR recording", e)
                _isRecording.value = false
                false
            }
        } ?: false
    }
    
    /**
     * Start monitoring GSR values for live updates
     */
    private fun startGSRMonitoring() {
        serviceScope.launch {
            gsrRecorder?.let { recorder ->
                // Monitor GSR data flow for live values
                // This would connect to the recorder's data stream
                Log.d(TAG, "Started GSR value monitoring")
            }
        }
    }
    
    /**
     * Get the underlying sensor recorder for integration
     */
    fun getSensorRecorder(): SensorRecorder? = gsrRecorder
    
    /**
     * Check if GSR hardware (Shimmer3) is available via BLE
     */
    fun isHardwareAvailable(): Boolean {
        return gsrRecorder?.let { recorder ->
            // This would check BLE connection and Shimmer device availability
            _isConnected.value
        } ?: false
    }
    
    /**
     * Get current recording statistics
     */
    fun getRecordingStats() = gsrRecorder?.getRecordingStats()
    
    /**
     * Set sampling rate (requires restart if recording)
     */
    suspend fun setSamplingRate(rateHz: Int): Boolean {
        return gsrRecorder?.let { recorder ->
            try {
                Log.i(TAG, "Setting GSR sampling rate to ${rateHz}Hz")
                // Implementation would reconfigure the Shimmer device
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set sampling rate", e)
                false
            }
        } ?: false
    }
    
    /**
     * Cleanup resources and disconnect BLE
     */
    fun cleanup() {
        Log.i(TAG, "Cleaning up GSR sensor service")
        gsrRecorder?.stopRecording()
        serviceScope.cancel()
        _isConnected.value = false
        _isRecording.value = false
        _currentGSRValue.value = 0.0
    }
}