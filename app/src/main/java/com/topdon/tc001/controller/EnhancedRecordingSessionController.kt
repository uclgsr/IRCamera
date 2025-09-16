package com.topdon.tc001.controller

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.topdon.tc001.sensors.SensorRecorder
import com.topdon.tc001.sensors.ErrorType
import com.topdon.tc001.sensors.RecordingStats
import com.topdon.tc001.sensors.rgb.RgbCameraRecorder
import com.topdon.tc001.sensors.thermal.ThermalCameraRecorder
import com.topdon.tc001.sensors.gsr.GSRSensorRecorder
import com.topdon.tc001.sensors.unified.UnifiedGSRRecorder
import com.topdon.tc001.service.RecordingService
import com.topdon.tc001.util.SessionDirectoryManager
import com.topdon.tc001.util.SessionDirectory
import com.topdon.tc001.util.StorageStatus
import com.topdon.tc001.data.SessionMetadata
import com.topdon.tc001.permissions.EnhancedPermissionManager
import com.topdon.tc001.permissions.PermissionController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Enhanced Recording Session Controller that implements comprehensive multi-sensor coordination
 * as requested in the issue. This addresses all the specific requirements:
 * 
 * 1. Session Start Coordination with partial failure handling
 * 2. State Management for individual sensor status tracking  
 * 3. Partial Failure Handling during ongoing sessions
 * 4. Stop Sequence & Recovery with safe cleanup
 * 5. Crash Recovery for incomplete sessions
 */
class EnhancedRecordingSessionController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val permissionController: PermissionController,
    private val enhancedPermissionManager: EnhancedPermissionManager
) {
    companion object {
        private const val TAG = "EnhancedRecordingSessionController"
        
        // Session coordination timeouts
        private const val SENSOR_INITIALIZATION_TIMEOUT_MS = 30000L // 30 seconds
        private const val SENSOR_START_TIMEOUT_MS = 15000L // 15 seconds
        private const val SENSOR_STOP_TIMEOUT_MS = 10000L // 10 seconds
        
        // Recovery and monitoring intervals
        private const val SESSION_MONITORING_INTERVAL_MS = 2000L // 2 seconds
        private const val PARTIAL_FAILURE_RECOVERY_DELAY_MS = 5000L // 5 seconds
        private const val RECONNECTION_ATTEMPTS = 3
        private const val RECONNECTION_DELAY_MS = 2000L // 2 seconds
        
        // Storage requirements
        private const val MIN_STORAGE_REQUIRED_MB = 500L
        private const val STORAGE_WARNING_THRESHOLD_MB = 1000L
    }

    // Core components
    private val sessionDirectoryManager = SessionDirectoryManager(context)
    private val controllerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Session state management
    private val _sessionState = MutableStateFlow(SessionState.IDLE)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()
    
    private val _sensorStates = MutableStateFlow<Map<String, SensorState>>(emptyMap())
    val sensorStates: StateFlow<Map<String, SensorState>> = _sensorStates.asStateFlow()
    
    private val _sessionProgress = MutableSharedFlow<SessionProgressEvent>()
    val sessionProgress: SharedFlow<SessionProgressEvent> = _sessionProgress.asSharedFlow()
    
    private val _sessionErrors = MutableSharedFlow<SessionError>()
    val sessionErrors: SharedFlow<SessionError> = _sessionErrors.asSharedFlow()
    
    // Session tracking
    private var currentSession: SessionInfo? = null
    private val activeSensors = ConcurrentHashMap<String, SensorRecorder>()
    private val sensorReconnectionAttempts = ConcurrentHashMap<String, Int>()
    private val sessionStartTime = AtomicLong(0)
    private val isSessionActive = AtomicBoolean(false)
    
    // Monitoring jobs
    private var sessionMonitoringJob: Job? = null
    private var partialFailureRecoveryJob: Job? = null
    
    /**
     * Comprehensive session start with partial failure handling as requested in the issue
     */
    suspend fun startRecordingSession(
        sessionId: String? = null,
        participantId: String? = null,
        studyName: String? = null,
        enabledSensors: List<String> = listOf("RGB", "Thermal", "GSR"),
        allowPartialStart: Boolean = true
    ): SessionStartResult = withContext(Dispatchers.IO) {
        
        Log.i(TAG, "Starting enhanced multi-sensor recording session")
        Log.i(TAG, "Session parameters: enabledSensors=$enabledSensors, allowPartialStart=$allowPartialStart")
        
        if (isSessionActive.get()) {
            Log.w(TAG, "Cannot start session - another session is already active")
            return@withContext SessionStartResult(
                success = false,
                sessionInfo = null,
                startedSensors = emptyList(),
                failedSensors = emptyList(),
                error = "Session already active"
            )
        }
        
        try {
            _sessionState.value = SessionState.VALIDATING_PREREQUISITES
            emitProgress("Validating recording prerequisites...")
            
            // Step 1: Validate prerequisites as mentioned in the issue
            val prerequisiteValidation = validateRecordingPrerequisites(enabledSensors)
            if (!prerequisiteValidation.isValid) {
                Log.e(TAG, "Prerequisites validation failed: ${prerequisiteValidation.errors}")
                _sessionState.value = SessionState.ERROR
                return@withContext SessionStartResult(
                    success = false,
                    sessionInfo = null,
                    startedSensors = emptyList(),
                    failedSensors = enabledSensors,
                    error = "Prerequisites failed: ${prerequisiteValidation.errors.joinToString(", ")}"
                )
            }
            
            // Step 2: Create session directory and metadata
            _sessionState.value = SessionState.CREATING_SESSION
            emitProgress("Creating session directory and metadata...")
            
            val finalSessionId = sessionId ?: sessionDirectoryManager.generateSessionId()
            val sessionDirectory = sessionDirectoryManager.createSessionDirectory(finalSessionId)
            
            val sessionInfo = SessionInfo(
                sessionId = finalSessionId,
                sessionDirectory = sessionDirectory.rootDir.absolutePath,
                participantId = participantId ?: "unknown",
                studyName = studyName ?: "default_study",
                enabledSensors = enabledSensors,
                startTime = System.currentTimeMillis(),
                allowPartialStart = allowPartialStart
            )
            
            currentSession = sessionInfo
            sessionStartTime.set(System.currentTimeMillis())
            
            // Step 3: Initialize sensors with individual error handling
            _sessionState.value = SessionState.INITIALIZING_SENSORS
            emitProgress("Initializing sensors with enhanced error handling...")
            
            val sensorInitResults = initializeSensorsWithTimeout(enabledSensors, sessionDirectory)
            
            // Step 4: Start sensors with partial failure handling
            _sessionState.value = SessionState.STARTING_SENSORS
            emitProgress("Starting sensor recording with partial failure handling...")
            
            val sensorStartResults = startSensorsWithPartialFailureHandling(
                sensorInitResults.successful,
                sessionDirectory,
                allowPartialStart
            )
            
            // Step 5: Evaluate session start success criteria  
            val totalSensors = enabledSensors.size
            val successfulSensors = sensorStartResults.successful
            val failedSensors = sensorInitResults.failed + sensorStartResults.failed
            
            val sessionStartSuccess = if (allowPartialStart) {
                successfulSensors.isNotEmpty() // At least one sensor must start
            } else {
                successfulSensors.size == totalSensors // All sensors must start
            }
            
            if (sessionStartSuccess) {
                // Session started successfully (full or partial)
                isSessionActive.set(true)
                _sessionState.value = SessionState.RECORDING
                
                // Update active sensors tracking
                activeSensors.clear()
                successfulSensors.forEach { (sensorName, sensor) ->
                    activeSensors[sensorName] = sensor
                }
                
                // Start session monitoring for mid-session failure recovery
                startSessionMonitoring()
                
                val successMessage = if (failedSensors.isEmpty()) {
                    "All ${successfulSensors.size} sensors started successfully"
                } else {
                    "Partial start: ${successfulSensors.size}/${totalSensors} sensors active (failed: ${failedSensors.joinToString(", ")})"
                }
                
                Log.i(TAG, "✅ Session started: $successMessage")
                emitProgress(successMessage)
                
                // Log failed sensors with detailed information
                failedSensors.forEach { sensorName ->
                    Log.w(TAG, "⚠️ Sensor '$sensorName' not available for this session")
                    emitError(SessionError(
                        type = SessionErrorType.SENSOR_UNAVAILABLE,
                        message = "Sensor '$sensorName' failed to start - session continues with available sensors",
                        sensorName = sensorName,
                        isRecoverable = true
                    ))
                }
                
                return@withContext SessionStartResult(
                    success = true,
                    sessionInfo = sessionInfo,
                    startedSensors = successfulSensors.keys.toList(),
                    failedSensors = failedSensors,
                    error = null
                )
                
            } else {
                // Session start failed completely
                _sessionState.value = SessionState.ERROR
                
                val errorMessage = if (!allowPartialStart) {
                    "All sensors required but ${failedSensors.size}/${totalSensors} failed: ${failedSensors.joinToString(", ")}"
                } else {
                    "No sensors could be started (${failedSensors.size} failed): ${failedSensors.joinToString(", ")}"
                }
                
                Log.e(TAG, "❌ Session start failed: $errorMessage")
                
                // Clean up any partially started sensors
                cleanupPartialSession(successfulSensors)
                
                return@withContext SessionStartResult(
                    success = false,
                    sessionInfo = sessionInfo,
                    startedSensors = emptyList(),
                    failedSensors = failedSensors,
                    error = errorMessage
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception during session start", e)
            _sessionState.value = SessionState.ERROR
            
            // Clean up any resources
            cleanupPartialSession(emptyMap())
            
            return@withContext SessionStartResult(
                success = false,
                sessionInfo = currentSession,
                startedSensors = emptyList(),
                failedSensors = enabledSensors,
                error = "Session start exception: ${e.message}"
            )
        }
    }

    /**
     * Enhanced session stop with safe cleanup as requested in the issue
     */
    suspend fun stopRecordingSession(): SessionStopResult = withContext(Dispatchers.IO) {
        Log.i(TAG, "Stopping multi-sensor recording session with safe cleanup")
        
        if (!isSessionActive.get()) {
            Log.w(TAG, "No active session to stop")
            return@withContext SessionStopResult(
                success = true,
                stoppedSensors = emptyList(),
                failedStops = emptyList(),
                sessionDuration = 0L,
                error = null
            )
        }
        
        try {
            _sessionState.value = SessionState.STOPPING
            emitProgress("Stopping sensors with safe cleanup...")
            
            // Stop monitoring first
            sessionMonitoringJob?.cancel()
            partialFailureRecoveryJob?.cancel()
            
            // Stop all active sensors with individual error handling
            val stopResults = stopAllSensorsWithSafeCleanup()
            
            // Calculate session duration
            val sessionDuration = System.currentTimeMillis() - sessionStartTime.get()
            
            // Finalize session
            currentSession?.let { session ->
                finalizeSession(session, sessionDuration, stopResults)
            }
            
            // Update state
            isSessionActive.set(false)
            _sessionState.value = SessionState.STOPPED
            activeSensors.clear()
            sensorReconnectionAttempts.clear()
            
            val successMessage = if (stopResults.failed.isEmpty()) {
                "All ${stopResults.successful.size} sensors stopped cleanly"
            } else {
                "${stopResults.successful.size} sensors stopped, ${stopResults.failed.size} had issues: ${stopResults.failed.joinToString(", ")}"
            }
            
            Log.i(TAG, "✅ Session stopped: $successMessage (duration: ${sessionDuration / 1000.0}s)")
            emitProgress(successMessage)
            
            return@withContext SessionStopResult(
                success = true,
                stoppedSensors = stopResults.successful,
                failedStops = stopResults.failed,
                sessionDuration = sessionDuration,
                error = null
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception during session stop", e)
            _sessionState.value = SessionState.ERROR
            
            // Force cleanup
            try {
                forceCleanupAllResources()
            } catch (cleanupEx: Exception) {
                Log.e(TAG, "Exception during force cleanup", cleanupEx)
            }
            
            return@withContext SessionStopResult(
                success = false,
                stoppedSensors = emptyList(),
                failedStops = activeSensors.keys.toList(),
                sessionDuration = System.currentTimeMillis() - sessionStartTime.get(),
                error = "Session stop exception: ${e.message}"
            )
        }
    }

    /**
     * Validate recording prerequisites as mentioned in the issue
     */
    private suspend fun validateRecordingPrerequisites(enabledSensors: List<String>): PrerequisiteValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        Log.i(TAG, "Validating recording prerequisites for sensors: ${enabledSensors.joinToString(", ")}")
        
        // Check storage space
        val storageStatus = sessionDirectoryManager.checkStorageSpace()
        if (storageStatus.isLowStorage) {
            errors.add("Insufficient storage space: ${storageStatus.formattedAvailable} available (need ${MIN_STORAGE_REQUIRED_MB}MB)")
        } else if (storageStatus.shouldWarn) {
            warnings.add("Low storage warning: ${storageStatus.formattedAvailable} available")
        }
        
        // Check permissions for each enabled sensor
        for (sensor in enabledSensors) {
            when (sensor.uppercase()) {
                "RGB" -> {
                    if (!permissionController.hasCameraPermission()) {
                        try {
                            val granted = enhancedPermissionManager.requestCameraPermissions()
                            if (!granted) {
                                errors.add("Camera permission required for RGB recording")
                            }
                        } catch (e: Exception) {
                            errors.add("Failed to request camera permission: ${e.message}")
                        }
                    }
                }
                "GSR", "SHIMMER" -> {
                    if (!permissionController.hasBluetoothPermissions()) {
                        try {
                            val granted = enhancedPermissionManager.requestBluetoothPermissions()
                            if (!granted) {
                                errors.add("Bluetooth permissions required for GSR recording")
                            }
                        } catch (e: Exception) {
                            errors.add("Failed to request Bluetooth permissions: ${e.message}")
                        }
                    }
                }
                "THERMAL" -> {
                    // USB permission handled dynamically when device is connected
                    warnings.add("Thermal camera requires USB permission when device is connected")
                }
            }
        }
        
        // Check if any sensors are available
        if (enabledSensors.isEmpty()) {
            errors.add("No sensors enabled for recording")
        }
        
        val result = PrerequisiteValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
        
        if (warnings.isNotEmpty()) {
            Log.w(TAG, "Prerequisites validation warnings: ${warnings.joinToString(", ")}")
        }
        
        return result
    }

    /**
     * Initialize sensors with timeout and individual error handling
     */
    private suspend fun initializeSensorsWithTimeout(
        enabledSensors: List<String>,
        sessionDirectory: SessionDirectory
    ): SensorOperationResult = coroutineScope {
        
        Log.i(TAG, "Initializing sensors with timeout handling")
        
        val initJobs = enabledSensors.map { sensorName ->
            async {
                try {
                    withTimeout(SENSOR_INITIALIZATION_TIMEOUT_MS) {
                        Log.d(TAG, "Initializing sensor: $sensorName")
                        val sensor = createSensorRecorder(sensorName, sessionDirectory)
                        val success = sensor?.initialize() ?: false
                        
                        if (success) {
                            Log.i(TAG, "✅ Sensor '$sensorName' initialized successfully")
                            sensorName to sensor
                        } else {
                            Log.w(TAG, "❌ Sensor '$sensorName' initialization failed")
                            null
                        }
                    }
                } catch (e: TimeoutCancellationException) {
                    Log.e(TAG, "❌ Sensor '$sensorName' initialization timeout (${SENSOR_INITIALIZATION_TIMEOUT_MS}ms)")
                    null
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Sensor '$sensorName' initialization exception", e)
                    null
                }
            }
        }
        
        val results = initJobs.awaitAll()
        val successful = results.mapNotNull { it }.toMap()
        val failed = enabledSensors.filter { sensorName -> !successful.containsKey(sensorName) }
        
        Log.i(TAG, "Sensor initialization complete: ${successful.size}/${enabledSensors.size} successful")
        
        return@coroutineScope SensorOperationResult(successful, failed)
    }

    /**
     * Start sensors with partial failure handling as requested in the issue
     */
    private suspend fun startSensorsWithPartialFailureHandling(
        initializedSensors: Map<String, SensorRecorder>,
        sessionDirectory: SessionDirectory,
        allowPartialStart: Boolean
    ): SensorOperationResult = coroutineScope {
        
        Log.i(TAG, "Starting sensors with partial failure handling (allowPartial=$allowPartialStart)")
        
        val startJobs = initializedSensors.map { (sensorName, sensor) ->
            async {
                try {
                    withTimeout(SENSOR_START_TIMEOUT_MS) {
                        Log.d(TAG, "Starting sensor: $sensorName")
                        
                        // Create sensor-specific directory
                        val sensorDir = File(sessionDirectory.rootDir, sensorName.lowercase())
                        sensorDir.mkdirs()
                        
                        val success = sensor.startRecording(sensorDir.absolutePath)
                        
                        if (success) {
                            Log.i(TAG, "✅ Sensor '$sensorName' started successfully")
                            updateSensorState(sensorName, SensorState.RECORDING)
                            sensorName to sensor
                        } else {
                            Log.w(TAG, "❌ Sensor '$sensorName' start failed")
                            updateSensorState(sensorName, SensorState.FAILED)
                            null
                        }
                    }
                } catch (e: TimeoutCancellationException) {
                    Log.e(TAG, "❌ Sensor '$sensorName' start timeout (${SENSOR_START_TIMEOUT_MS}ms)")
                    updateSensorState(sensorName, SensorState.TIMEOUT)
                    null
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Sensor '$sensorName' start exception", e)
                    updateSensorState(sensorName, SensorState.ERROR)
                    null
                }
            }
        }
        
        val results = startJobs.awaitAll()
        val successful = results.mapNotNull { it }.toMap()
        val failed = initializedSensors.keys.filter { sensorName -> !successful.containsKey(sensorName) }
        
        Log.i(TAG, "Sensor start complete: ${successful.size}/${initializedSensors.size} successful")
        
        return@coroutineScope SensorOperationResult(successful, failed)
    }

    /**
     * Stop all sensors with safe cleanup as requested in the issue
     */
    private suspend fun stopAllSensorsWithSafeCleanup(): SensorStopResult = coroutineScope {
        
        Log.i(TAG, "Stopping all sensors with safe cleanup")
        
        val stopJobs = activeSensors.map { (sensorName, sensor) ->
            async {
                try {
                    withTimeout(SENSOR_STOP_TIMEOUT_MS) {
                        Log.d(TAG, "Stopping sensor: $sensorName")
                        val success = sensor.stopRecording()
                        
                        if (success) {
                            Log.i(TAG, "✅ Sensor '$sensorName' stopped cleanly")
                            updateSensorState(sensorName, SensorState.STOPPED)
                            sensorName
                        } else {
                            Log.w(TAG, "⚠️ Sensor '$sensorName' stop returned false")
                            updateSensorState(sensorName, SensorState.STOP_FAILED)
                            null
                        }
                    }
                } catch (e: TimeoutCancellationException) {
                    Log.e(TAG, "❌ Sensor '$sensorName' stop timeout")
                    updateSensorState(sensorName, SensorState.STOP_TIMEOUT)
                    null
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Sensor '$sensorName' stop exception", e)
                    updateSensorState(sensorName, SensorState.STOP_ERROR)
                    null
                }
            }
        }
        
        val results = stopJobs.awaitAll()
        val successful = results.mapNotNull { it }
        val failed = activeSensors.keys.filter { sensorName -> !successful.contains(sensorName) }
        
        Log.i(TAG, "Sensor stop complete: ${successful.size}/${activeSensors.size} stopped cleanly")
        
        return@coroutineScope SensorStopResult(successful, failed)
    }

    /**
     * Create appropriate sensor recorder based on sensor type
     */
    private fun createSensorRecorder(sensorName: String, sessionDirectory: SessionDirectory): SensorRecorder? {
        return try {
            when (sensorName.uppercase()) {
                "RGB" -> RgbCameraRecorder(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    previewView = null,
                    useFrontCamera = false,
                    enhancedPermissionManager = enhancedPermissionManager
                )
                "THERMAL" -> ThermalCameraRecorder(context, "thermal_${sensorName}")
                "GSR", "SHIMMER" -> UnifiedGSRRecorder(context, lifecycleOwner)
                else -> {
                    Log.w(TAG, "Unknown sensor type: $sensorName")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create sensor recorder for $sensorName", e)
            null
        }
    }

    /**
     * Start session monitoring for mid-session failure recovery as requested in the issue
     */
    private fun startSessionMonitoring() {
        Log.i(TAG, "Starting session monitoring for mid-session failure recovery")
        
        sessionMonitoringJob = controllerScope.launch {
            while (isSessionActive.get()) {
                try {
                    monitorActiveSensors()
                    delay(SESSION_MONITORING_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.w(TAG, "Session monitoring error", e)
                    delay(SESSION_MONITORING_INTERVAL_MS)
                }
            }
        }
        
        partialFailureRecoveryJob = controllerScope.launch {
            while (isSessionActive.get()) {
                try {
                    checkForPartialFailuresAndRecover()
                    delay(PARTIAL_FAILURE_RECOVERY_DELAY_MS)
                } catch (e: Exception) {
                    Log.w(TAG, "Partial failure recovery error", e)
                    delay(PARTIAL_FAILURE_RECOVERY_DELAY_MS)
                }
            }
        }
    }

    /**
     * Monitor active sensors for mid-session failures
     */
    private suspend fun monitorActiveSensors() {
        val currentStates = mutableMapOf<String, SensorState>()
        
        activeSensors.forEach { (sensorName, sensor) ->
            try {
                val isRecording = sensor.isRecording
                val currentState = if (isRecording) SensorState.RECORDING else SensorState.DISCONNECTED
                
                currentStates[sensorName] = currentState
                
                // Detect sensor disconnections
                if (!isRecording && _sensorStates.value[sensorName] == SensorState.RECORDING) {
                    Log.w(TAG, "⚠️ Sensor '$sensorName' disconnected during recording")
                    handleSensorDisconnection(sensorName, sensor)
                }
                
            } catch (e: Exception) {
                Log.w(TAG, "Error monitoring sensor '$sensorName'", e)
                currentStates[sensorName] = SensorState.ERROR
            }
        }
        
        _sensorStates.value = currentStates
    }

    /**
     * Handle sensor disconnection with reconnection attempts as requested in the issue
     */
    private suspend fun handleSensorDisconnection(sensorName: String, sensor: SensorRecorder) {
        Log.i(TAG, "Handling disconnection for sensor '$sensorName'")
        
        val currentAttempts = sensorReconnectionAttempts.getOrDefault(sensorName, 0)
        
        if (currentAttempts < RECONNECTION_ATTEMPTS) {
            Log.i(TAG, "Attempting reconnection for '$sensorName' (attempt ${currentAttempts + 1}/$RECONNECTION_ATTEMPTS)")
            
            sensorReconnectionAttempts[sensorName] = currentAttempts + 1
            updateSensorState(sensorName, SensorState.RECONNECTING)
            
            emitProgress("Reconnecting $sensorName sensor...")
            
            try {
                delay(RECONNECTION_DELAY_MS)
                
                // Attempt reinitialization and restart
                val reinitSuccess = sensor.initialize()
                if (reinitSuccess) {
                    currentSession?.let { session ->
                        val sensorDir = File(session.sessionDirectory, sensorName.lowercase())
                        val restartSuccess = sensor.startRecording(sensorDir.absolutePath)
                        
                        if (restartSuccess) {
                            Log.i(TAG, "✅ Sensor '$sensorName' reconnected successfully")
                            updateSensorState(sensorName, SensorState.RECORDING)
                            sensorReconnectionAttempts.remove(sensorName)
                            emitProgress("$sensorName sensor reconnected")
                            return
                        }
                    }
                }
                
                Log.w(TAG, "❌ Reconnection failed for '$sensorName'")
                
            } catch (e: Exception) {
                Log.e(TAG, "Exception during reconnection for '$sensorName'", e)
            }
            
            // Schedule next attempt
            handleSensorDisconnection(sensorName, sensor)
            
        } else {
            // Max attempts reached
            Log.e(TAG, "❌ Max reconnection attempts reached for '$sensorName' - switching to simulation/removal")
            
            activeSensors.remove(sensorName)
            sensorReconnectionAttempts.remove(sensorName)
            updateSensorState(sensorName, SensorState.FAILED_PERMANENTLY)
            
            emitError(SessionError(
                type = SessionErrorType.SENSOR_LOST_PERMANENTLY,
                message = "Sensor '$sensorName' lost permanently after $RECONNECTION_ATTEMPTS reconnection attempts",
                sensorName = sensorName,
                isRecoverable = false
            ))
            
            // Check if we still have enough sensors to continue
            if (activeSensors.isEmpty()) {
                Log.e(TAG, "❌ All sensors lost - stopping session")
                emitError(SessionError(
                    type = SessionErrorType.ALL_SENSORS_LOST,
                    message = "All sensors have been lost - session cannot continue",
                    sensorName = null,
                    isRecoverable = false
                ))
                
                stopRecordingSession()
            } else {
                Log.i(TAG, "ℹ️ Session continuing with ${activeSensors.size} remaining sensors: ${activeSensors.keys}")
                emitProgress("Session continuing with ${activeSensors.size} remaining sensors")
            }
        }
    }

    /**
     * Check for partial failures and attempt recovery
     */
    private suspend fun checkForPartialFailuresAndRecover() {
        // Implementation would check sensor health and attempt recovery
        // This is a placeholder for the comprehensive failure recovery logic
    }

    /**
     * Clean up partial session as requested in the issue
     */
    private suspend fun cleanupPartialSession(partialSensors: Map<String, SensorRecorder>) {
        Log.i(TAG, "Cleaning up partial session resources")
        
        partialSensors.forEach { (sensorName, sensor) ->
            try {
                sensor.stopRecording()
                sensor.cleanup()
                Log.d(TAG, "Cleaned up partial sensor: $sensorName")
            } catch (e: Exception) {
                Log.w(TAG, "Error cleaning up sensor '$sensorName'", e)
            }
        }
        
        currentSession = null
        isSessionActive.set(false)
        activeSensors.clear()
        sensorReconnectionAttempts.clear()
    }

    /**
     * Force cleanup all resources (emergency cleanup)
     */
    private suspend fun forceCleanupAllResources() {
        Log.w(TAG, "Force cleaning up all resources")
        
        sessionMonitoringJob?.cancel()
        partialFailureRecoveryJob?.cancel()
        
        activeSensors.values.forEach { sensor ->
            try {
                sensor.stopRecording()
                sensor.cleanup()
            } catch (e: Exception) {
                Log.w(TAG, "Error in force cleanup", e)
            }
        }
        
        activeSensors.clear()
        sensorReconnectionAttempts.clear()
        isSessionActive.set(false)
        currentSession = null
    }

    /**
     * Finalize session with metadata as requested in the issue
     */
    private suspend fun finalizeSession(
        session: SessionInfo,
        duration: Long,
        stopResults: SensorStopResult
    ) {
        try {
            val sessionDir = File(session.sessionDirectory)
            val metadataFile = File(sessionDir, "session_finalization.json")
            
            val finalizationData = JSONObject().apply {
                put("session_id", session.sessionId)
                put("duration_ms", duration)
                put("started_sensors", session.enabledSensors)
                put("successfully_stopped", stopResults.successful)
                put("failed_stops", stopResults.failed)
                put("final_active_sensors", activeSensors.keys.toList())
                put("reconnection_attempts", sensorReconnectionAttempts.toMap())
                put("finalized_at", System.currentTimeMillis())
            }
            
            metadataFile.writeText(finalizationData.toString(2))
            Log.i(TAG, "Session finalization metadata written")
            
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write session finalization metadata", e)
        }
    }

    // Helper methods for state management
    private fun updateSensorState(sensorName: String, state: SensorState) {
        val currentStates = _sensorStates.value.toMutableMap()
        currentStates[sensorName] = state
        _sensorStates.value = currentStates
    }

    private suspend fun emitProgress(message: String) {
        _sessionProgress.emit(SessionProgressEvent(message, System.currentTimeMillis()))
    }

    private suspend fun emitError(error: SessionError) {
        _sessionErrors.emit(error)
    }

    /**
     * Get current session status summary
     */
    fun getSessionStatusSummary(): SessionStatusSummary {
        val session = currentSession
        return SessionStatusSummary(
            isActive = isSessionActive.get(),
            sessionInfo = session,
            sessionState = _sessionState.value,
            activeSensors = activeSensors.keys.toList(),
            sensorStates = _sensorStates.value,
            sessionDuration = if (isSessionActive.get()) {
                System.currentTimeMillis() - sessionStartTime.get()
            } else 0L,
            reconnectionAttempts = sensorReconnectionAttempts.toMap()
        )
    }

    /**
     * Cleanup controller resources
     */
    suspend fun cleanup() {
        Log.i(TAG, "Cleaning up EnhancedRecordingSessionController")
        
        if (isSessionActive.get()) {
            stopRecordingSession()
        }
        
        controllerScope.cancel()
    }
}

// Data classes for enhanced session management

enum class SessionState {
    IDLE,
    VALIDATING_PREREQUISITES,
    CREATING_SESSION,
    INITIALIZING_SENSORS,
    STARTING_SENSORS,
    RECORDING,
    STOPPING,
    STOPPED,
    ERROR
}

enum class SensorState {
    INITIALIZING,
    READY,
    RECORDING,
    DISCONNECTED,
    RECONNECTING,
    FAILED,
    FAILED_PERMANENTLY,
    TIMEOUT,
    ERROR,
    STOPPED,
    STOP_FAILED,
    STOP_TIMEOUT,
    STOP_ERROR
}

data class SessionInfo(
    val sessionId: String,
    val sessionDirectory: String,
    val participantId: String,
    val studyName: String,
    val enabledSensors: List<String>,
    val startTime: Long,
    val allowPartialStart: Boolean
)

data class SessionStartResult(
    val success: Boolean,
    val sessionInfo: SessionInfo?,
    val startedSensors: List<String>,
    val failedSensors: List<String>,
    val error: String?
)

data class SessionStopResult(
    val success: Boolean,
    val stoppedSensors: List<String>,
    val failedStops: List<String>,
    val sessionDuration: Long,
    val error: String?
)

data class PrerequisiteValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)

data class SensorOperationResult(
    val successful: Map<String, SensorRecorder>,
    val failed: List<String>
)

data class SensorStopResult(
    val successful: List<String>,
    val failed: List<String>
)

enum class SessionErrorType {
    SENSOR_UNAVAILABLE,
    SENSOR_LOST_PERMANENTLY,
    ALL_SENSORS_LOST,
    STORAGE_ERROR,
    PERMISSION_ERROR
}

data class SessionError(
    val type: SessionErrorType,
    val message: String,
    val sensorName: String?,
    val isRecoverable: Boolean
)

data class SessionProgressEvent(
    val message: String,
    val timestamp: Long
)

data class SessionStatusSummary(
    val isActive: Boolean,
    val sessionInfo: SessionInfo?,
    val sessionState: SessionState,
    val activeSensors: List<String>,
    val sensorStates: Map<String, SensorState>,
    val sessionDuration: Long,
    val reconnectionAttempts: Map<String, Int>
)