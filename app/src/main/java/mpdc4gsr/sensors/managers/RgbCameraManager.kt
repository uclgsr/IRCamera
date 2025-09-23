package mpdc4gsr.sensors.managers

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mpdc4gsr.sensors.SensorRecorder
import mpdc4gsr.sensors.RgbCameraRecorder

/**
 * RgbCameraManager - Encapsulates RGB camera management
 * 
 * This class manages the phone's RGB camera integration including:
 * - Camera2 API integration
 * - Video recording with H.264 encoding
 * - Preview streaming coordination
 * - Resolution and frame rate configuration
 */
class RgbCameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val sensorId: String = "rgb_camera_1"
) {
    companion object {
        private const val TAG = "RgbCameraManager"
    }

    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()
    
    private var rgbRecorder: RgbCameraRecorder? = null
    
    /**
     * Initialize the RGB camera recorder with Camera2 API
     */
    suspend fun initialize(previewSurfaceView: android.view.SurfaceView? = null): Boolean {
        return try {
            Log.i(TAG, "Initializing RGB camera manager for sensor: $sensorId")
            
            rgbRecorder = RgbCameraRecorder(context, lifecycleOwner, previewSurfaceView).apply {
                Log.d(TAG, "RGB camera recorder created with preview integration")
            }
            
            // Check camera permissions
            val hasPermission = rgbRecorder?.hasCameraPermission() ?: false
            _hasPermission.value = hasPermission
            _isConnected.value = hasPermission
            
            if (hasPermission) {
                Log.i(TAG, "RGB camera manager initialized successfully")
            } else {
                Log.w(TAG, "RGB camera manager initialized but missing camera permission")
            }
            
            hasPermission
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize RGB camera manager", e)
            _isConnected.value = false
            _hasPermission.value = false
            false
        }
    }
    
    /**
     * Start RGB camera recording with synchronized timestamp
     */
    suspend fun startRecording(sessionDirectory: String, startTimestamp: Long): Boolean {
        return rgbRecorder?.let { recorder ->
            try {
                Log.i(TAG, "Starting RGB camera recording at timestamp: $startTimestamp")
                val success = recorder.startRecording(sessionDirectory, startTimestamp)
                _isRecording.value = success
                
                if (success) {
                    Log.i(TAG, "RGB camera recording started successfully")
                } else {
                    Log.w(TAG, "Failed to start RGB camera recording")
                }
                
                success
            } catch (e: Exception) {
                Log.e(TAG, "Exception starting RGB camera recording", e)
                _isRecording.value = false
                false
            }
        } ?: false
    }
    
    /**
     * Stop RGB camera recording and save video
     */
    suspend fun stopRecording(): Boolean {
        return rgbRecorder?.let { recorder ->
            try {
                Log.i(TAG, "Stopping RGB camera recording")
                val success = recorder.stopRecording()
                _isRecording.value = false
                
                if (success) {
                    Log.i(TAG, "RGB camera recording stopped successfully")
                } else {
                    Log.w(TAG, "Issues stopping RGB camera recording")
                }
                
                success
            } catch (e: Exception) {
                Log.e(TAG, "Exception stopping RGB camera recording", e)
                _isRecording.value = false
                false
            }
        } ?: false
    }
    
    /**
     * Get the underlying sensor recorder for integration
     */
    fun getSensorRecorder(): SensorRecorder? = rgbRecorder
    
    /**
     * Check if RGB camera hardware is available
     */
    fun isHardwareAvailable(): Boolean {
        return rgbRecorder?.let { recorder ->
            recorder.hasCameraPermission() && _isConnected.value
        } ?: false
    }
    
    /**
     * Get current recording statistics
     */
    fun getRecordingStats() = rgbRecorder?.getRecordingStats()
    
    /**
     * Configure recording resolution and frame rate
     */
    suspend fun configureRecording(width: Int = 1280, height: Int = 720, fps: Int = 30): Boolean {
        return try {
            Log.i(TAG, "Configuring RGB recording: ${width}x${height} @ ${fps}fps")
            // Implementation would configure the Camera2 capture session
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure RGB recording", e)
            false
        }
    }
    
    /**
     * Enable or disable preview streaming
     */
    fun setPreviewEnabled(enabled: Boolean) {
        Log.d(TAG, "Setting preview enabled: $enabled")
        // Implementation would control preview surface
    }
    
    /**
     * Cleanup resources and release camera
     */
    fun cleanup() {
        Log.i(TAG, "Cleaning up RGB camera manager")
        rgbRecorder?.stopRecording()
        managerScope.cancel()
        _isConnected.value = false
        _isRecording.value = false
        _hasPermission.value = false
    }
}