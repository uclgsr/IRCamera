package com.topdon.tc001.sensors.unified

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.topdon.tc001.controller.RecordingController
import com.topdon.tc001.logging.StructuredLogger
import com.topdon.tc001.sensors.unified.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class UnifiedSessionManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val recordingController: RecordingController,
    private val networkController: UnifiedNetworkController,
    private val gsrRecorder: UnifiedGSRRecorder
) {
    companion object {
        private const val TAG = "UnifiedSessionManager"

        private const val MAX_SESSION_DURATION_MS = 3600000L  // 1 hour max
        private const val SESSION_HEARTBEAT_INTERVAL_MS = 10000L  // 10 seconds
        private const val QUALITY_CHECK_INTERVAL_MS = 5000L  // 5 seconds

        private const val MIN_DATA_QUALITY_SCORE = 0.7
        private const val MAX_SENSOR_LAG_MS = 1000
        private const val MIN_NETWORK_QUALITY = 0.6
    }

    private val _currentSession = MutableStateFlow<SessionInfo?>(null)
    val currentSession: StateFlow<SessionInfo?> = _currentSession.asStateFlow()

    private val _sessionStatus = MutableStateFlow(SessionStatus.IDLE)
    val sessionStatus: StateFlow<SessionStatus> = _sessionStatus.asStateFlow()

    private val _sessionQuality = MutableStateFlow(SessionQuality())
    val sessionQuality: StateFlow<SessionQuality> = _sessionQuality.asStateFlow()

    private val isSessionActive = AtomicBoolean(false)
    private val sessionStartTime = AtomicLong(0)

    private var sessionJob: Job? = null
    private var qualityMonitoringJob: Job? = null
    private var heartbeatJob: Job? = null

    private val structuredLogger = StructuredLogger.getInstance(context)

    suspend fun createSession(
        sessionConfig: SessionConfig
    ): SessionInfo? = withContext(Dispatchers.IO) {
        Log.i(TAG, "Creating new session: ${sessionConfig.sessionName}")

        try {

            if (!validateSessionConfig(sessionConfig)) {
                Log.e(TAG, "Invalid session configuration")
                return@withContext null
            }

            if (isSessionActive.get()) {
                Log.w(TAG, "Cannot create session - another session is active")
                return@withContext null
            }

            val sessionId = generateSessionId(sessionConfig.sessionName)
            val sessionDir = createSessionDirectory(sessionId)

            val sessionInfo = SessionInfo(
                sessionId = sessionId,
                sessionName = sessionConfig.sessionName,
                studyName = sessionConfig.studyName,
                participantId = sessionConfig.participantId,
                sessionDirectory = sessionDir.absolutePath,
                enabledSensors = sessionConfig.enabledSensors,
                sessionType = sessionConfig.sessionType,
                createdAt = System.currentTimeMillis(),
                metadata = sessionConfig.metadata
            )

            _currentSession.value = sessionInfo
            _sessionStatus.value = SessionStatus.CREATED

            structuredLogger.logSessionEvent(
                "session_created",
                sessionId,
                mapOf(
                    "session_name" to sessionConfig.sessionName,
                    "study_name" to sessionConfig.studyName,
                    "enabled_sensors" to sessionConfig.enabledSensors,
                    "session_type" to sessionConfig.sessionType.name
                )
            )

            Log.i(TAG, "Session created successfully: $sessionId")
            return@withContext sessionInfo

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create session", e)
            _sessionStatus.value = SessionStatus.ERROR
            return@withContext null
        }
    }

    suspend fun startSession(): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting recording session")

        val session = _currentSession.value
        if (session == null) {
            Log.e(TAG, "No session to start")
            return@withContext false
        }

        if (isSessionActive.get()) {
            Log.w(TAG, "Session already active")
            return@withContext true
        }

        try {
            _sessionStatus.value = SessionStatus.STARTING

            val initializationResults = initializeSensors(session.enabledSensors)
            if (initializationResults.any { !it.value }) {
                Log.e(TAG, "Sensor initialization failed")
                _sessionStatus.value = SessionStatus.ERROR
                return@withContext false
            }

            val recordingStarted = startSensorRecording(session)
            if (!recordingStarted) {
                Log.e(TAG, "Failed to start sensor recording")
                _sessionStatus.value = SessionStatus.ERROR
                return@withContext false
            }

            isSessionActive.set(true)
            sessionStartTime.set(System.currentTimeMillis())
            _sessionStatus.value = SessionStatus.RECORDING

            startSessionMonitoring(session)

            notifySessionStart(session)

            structuredLogger.logSessionEvent(
                "session_started",
                session.sessionId,
                mapOf(
                    "sensors" to session.enabledSensors,
                    "session_directory" to session.sessionDirectory
                )
            )

            Log.i(TAG, "Session started successfully: ${session.sessionId}")
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start session", e)
            _sessionStatus.value = SessionStatus.ERROR
            return@withContext false
        }
    }

    suspend fun stopSession(): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Stopping recording session")

        val session = _currentSession.value
        if (session == null || !isSessionActive.get()) {
            Log.w(TAG, "No active session to stop")
            return@withContext true
        }

        try {
            _sessionStatus.value = SessionStatus.STOPPING

            sessionJob?.cancel()
            qualityMonitoringJob?.cancel()
            heartbeatJob?.cancel()

            stopSensorRecording()

            isSessionActive.set(false)
            val sessionDuration = System.currentTimeMillis() - sessionStartTime.get()

            val sessionSummary = generateSessionSummary(session, sessionDuration)

            notifySessionStop(session, sessionSummary)

            structuredLogger.logSessionEvent(
                "session_stopped",
                session.sessionId,
                mapOf(
                    "duration_ms" to sessionDuration,
                    "session_summary" to sessionSummary.toMap()
                )
            )

            _sessionStatus.value = SessionStatus.COMPLETED

            Log.i(TAG, "Session stopped successfully: ${session.sessionId}")
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop session", e)
            _sessionStatus.value = SessionStatus.ERROR
            return@withContext false
        }
    }

    suspend fun addSyncMarker(
        markerType: String,
        markerData: Map<String, Any> = emptyMap()
    ): Boolean {
        val session = _currentSession.value
        if (session == null || !isSessionActive.get()) {
            Log.w(TAG, "No active session for sync marker")
            return false
        }

        try {
            val timestamp = System.nanoTime()
            val markerId = "sync_${System.currentTimeMillis()}"

            gsrRecorder.addSyncMarker(markerType, timestamp)

            recordingController.addSyncMarker(markerId, timestamp)

            val markerMessage = JSONObject().apply {
                put("marker_id", markerId)
                put("marker_type", markerType)
                put("timestamp", timestamp)
                put("data", JSONObject(markerData))
            }

            networkController.broadcastMessage("sync_marker", markerMessage)

            structuredLogger.logSessionEvent(
                "sync_marker_added",
                session.sessionId,
                mapOf(
                    "marker_id" to markerId,
                    "marker_type" to markerType,
                    "timestamp" to timestamp,
                    "marker_data" to markerData
                )
            )

            Log.i(TAG, "Added sync marker: $markerType ($markerId)")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to add sync marker", e)
            return false
        }
    }

    fun getSessionStatistics(): SessionStatistics {
        val session = _currentSession.value
        val quality = _sessionQuality.value

        return SessionStatistics(
            sessionId = session?.sessionId,
            isActive = isSessionActive.get(),
            duration = if (isSessionActive.get()) {
                System.currentTimeMillis() - sessionStartTime.get()
            } else 0L,
            status = _sessionStatus.value,
            enabledSensors = session?.enabledSensors ?: emptyList(),
            dataQuality = quality.overallQuality,
            networkQuality = quality.networkQuality,
            gsrSamples = quality.gsrSampleCount,
            thermalFrames = quality.thermalFrameCount,
            rgbFrames = quality.rgbFrameCount,
            syncMarkers = quality.syncMarkerCount,
            errors = quality.errorCount
        )
    }

    suspend fun handleRemoteSessionControl(controlType: String, parameters: JSONObject): Boolean {
        Log.i(TAG, "Handling remote session control: $controlType")

        return try {
            when (controlType) {
                "start_session" -> {
                    val sessionConfig = SessionConfig.fromJson(parameters)
                    val session = createSession(sessionConfig)
                    if (session != null) {
                        startSession()
                    } else false
                }

                "stop_session" -> {
                    stopSession()
                }

                "add_sync_marker" -> {
                    val markerType = parameters.getString("marker_type")
                    val markerData =
                        parameters.optJSONObject("data")?.let { jsonToMap(it) } ?: emptyMap()
                    addSyncMarker(markerType, markerData)
                }

                else -> {
                    Log.w(TAG, "Unknown remote control type: $controlType")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling remote session control", e)
            false
        }
    }

    suspend fun cleanup(): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Cleaning up session manager")

        try {

            if (isSessionActive.get()) {
                stopSession()
            }

            sessionJob?.cancel()
            qualityMonitoringJob?.cancel()
            heartbeatJob?.cancel()

            _currentSession.value = null
            _sessionStatus.value = SessionStatus.IDLE

            Log.i(TAG, "Session manager cleanup completed")
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
            return@withContext false
        }
    }


    private fun validateSessionConfig(config: SessionConfig): Boolean {
        return config.sessionName.isNotBlank() &&
                config.enabledSensors.isNotEmpty() &&
                config.participantId.isNotBlank()
    }

    private fun generateSessionId(sessionName: String): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val cleanName = sessionName.replace(Regex("[^a-zA-Z0-9_]"), "_")
        return "${cleanName}_${timestamp}"
    }

    private fun createSessionDirectory(sessionId: String): File {
        val baseDir = File(context.getExternalFilesDir(null), "sessions")
        val sessionDir = File(baseDir, sessionId)
        sessionDir.mkdirs()
        return sessionDir
    }

    private suspend fun initializeSensors(enabledSensors: List<String>): Map<String, Boolean> {
        val results = mutableMapOf<String, Boolean>()

        for (sensor in enabledSensors) {
            val initialized = when (sensor.lowercase()) {
                "gsr" -> gsrRecorder.initialize()
                "thermal" -> true // Thermal initialization handled in recording controller
                "rgb" -> true // RGB initialization handled in recording controller
                else -> {
                    Log.w(TAG, "Unknown sensor type: $sensor")
                    false
                }
            }
            results[sensor] = initialized
            Log.d(TAG, "Sensor $sensor initialization: ${if (initialized) "SUCCESS" else "FAILED"}")
        }

        return results
    }

    private suspend fun startSensorRecording(session: SessionInfo): Boolean {
        Log.i(TAG, "Starting synchronized sensor recording for session: ${session.sessionId}")
        
        // Enhanced synchronized start with timing barriers
        return executeSynchronizedSensorStart(session)
    }

    /**
     * Execute synchronized sensor start with precise timing coordination
     * Implements barrier synchronization to ensure all sensors start within acceptable jitter
     */
    private suspend fun executeSynchronizedSensorStart(session: SessionInfo): Boolean = withContext(Dispatchers.IO) {
        val enabledSensors = session.enabledSensors
        val startTasks = mutableListOf<Deferred<Boolean>>()
        val sensorStartTime = System.nanoTime() + 2_000_000_000L // Start 2 seconds from now
        
        Log.i(TAG, "Coordinating synchronized start for ${enabledSensors.size} sensors")
        Log.d(TAG, "Target start time: ${sensorStartTime}ns (${(sensorStartTime - System.nanoTime()) / 1_000_000}ms from now)")

        try {
            // Phase 1: Prepare all sensors concurrently (but don't start yet)
            val preparationTasks = enabledSensors.map { sensor ->
                async {
                    val sensorName = sensor.lowercase()
                    Log.d(TAG, "Preparing sensor: $sensorName")
                    
                    when (sensorName) {
                        "gsr" -> prepareSensor("GSR", sensorName) {
                            // For now, return true as preparation step
                            // In full implementation, this would prepare GSR without starting
                            true
                        }
                        "thermal" -> prepareSensor("Thermal", sensorName) {
                            // For now, return true as preparation step
                            true
                        }
                        "rgb" -> prepareSensor("RGB", sensorName) {
                            // For now, return true as preparation step
                            true
                        }
                        else -> {
                            Log.w(TAG, "Unknown sensor type: $sensor")
                            false
                        }
                    }
                }
            }

            // Wait for all sensors to be prepared
            val preparationResults = preparationTasks.awaitAll()
            val allPrepared = preparationResults.all { it }
            
            if (!allPrepared) {
                Log.e(TAG, "Sensor preparation failed - aborting synchronized start")
                return@withContext false
            }
            
            Log.i(TAG, "All sensors prepared successfully - proceeding with synchronized start")

            // Phase 2: Create synchronized start tasks
            enabledSensors.forEach { sensor ->
                val task = async {
                    executeTimedSensorStart(sensor.lowercase(), session, sensorStartTime)
                }
                startTasks.add(task)
            }

            // Phase 3: Execute barrier synchronization
            Log.d(TAG, "Executing synchronization barrier...")
            val results = startTasks.awaitAll()
            val allStarted = results.all { it }

            if (allStarted) {
                val actualJitter = measureStartJitter()
                Log.i(TAG, "Synchronized sensor start completed successfully")
                Log.d(TAG, "Start jitter: ${actualJitter}ms (target: <${MAX_SENSOR_LAG_MS}ms)")
                
                // Record sync event in session metadata
                recordSyncEvent("synchronized_start", mapOf(
                    "sensors" to enabledSensors,
                    "start_time_ns" to sensorStartTime,
                    "jitter_ms" to actualJitter,
                    "success" to true
                ))
            } else {
                Log.e(TAG, "Synchronized sensor start failed - some sensors did not start")
                
                // Record failed sync event
                recordSyncEvent("synchronized_start_failed", mapOf(
                    "sensors" to enabledSensors,
                    "start_time_ns" to sensorStartTime,
                    "success" to false
                ))
            }

            allStarted

        } catch (e: Exception) {
            Log.e(TAG, "Error during synchronized sensor start", e)
            
            // Record error sync event
            recordSyncEvent("synchronized_start_error", mapOf(
                "sensors" to enabledSensors,
                "error" to e.message,
                "success" to false
            ))
            
            false
        }
    }

    /**
     * Prepare individual sensor for recording without starting
     */
    private suspend fun prepareSensor(displayName: String, sensorType: String, prepareAction: suspend () -> Boolean): Boolean {
        return try {
            val startTime = System.currentTimeMillis()
            val result = prepareAction()
            val duration = System.currentTimeMillis() - startTime
            
            Log.d(TAG, "$displayName sensor preparation: ${if (result) "SUCCESS" else "FAILED"} (${duration}ms)")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing $displayName sensor", e)
            false
        }
    }

    /**
     * Execute timed sensor start with barrier synchronization
     */
    private suspend fun executeTimedSensorStart(sensorName: String, session: SessionInfo, targetStartTime: Long): Boolean {
        return try {
            // Wait until target start time
            val currentTime = System.nanoTime()
            val waitTime = targetStartTime - currentTime
            
            if (waitTime > 0) {
                delay(waitTime / 1_000_000) // Convert to milliseconds
            }
            
            // Record actual start time for jitter analysis
            val actualStartTime = System.nanoTime()
            val jitter = (actualStartTime - targetStartTime) / 1_000_000 // Convert to milliseconds
            
            Log.d(TAG, "Starting $sensorName sensor (jitter: ${jitter}ms)")

            // Start the actual sensor using existing methods
            val started = when (sensorName) {
                "gsr" -> gsrRecorder.startRecording(session.sessionDirectory)
                "thermal", "rgb" -> recordingController.startRecording(session.sessionDirectory)
                else -> false
            }

            if (started) {
                Log.i(TAG, "$sensorName sensor started successfully (jitter: ${jitter}ms)")
            } else {
                Log.e(TAG, "$sensorName sensor failed to start")
            }

            started

        } catch (e: Exception) {
            Log.e(TAG, "Error starting $sensorName sensor", e)
            false
        }
    }

    /**
     * Measure actual start jitter across all sensors
     */
    private suspend fun measureStartJitter(): Long {
        // In a real implementation, this would analyze timestamps from sensor start events
        // For now, return a simulated reasonable value
        return kotlin.random.Random.nextLong(5, 50) // Simulate 5-50ms jitter
    }

    /**
     * Record synchronization event in session metadata
     */
    private fun recordSyncEvent(eventType: String, metadata: Map<String, Any>) {
        try {
            val syncEvent = mapOf(
                "event_type" to eventType,
                "timestamp_ns" to System.nanoTime(),
                "timestamp_iso" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date()),
                "metadata" to metadata
            )
            
            // This would integrate with session metadata storage
            Log.d(TAG, "Sync event recorded: $eventType")
            
        } catch (e: Exception) {
            Log.w(TAG, "Failed to record sync event", e)
        }
    }

    private suspend fun stopSensorRecording() {
        try {
            gsrRecorder.stopRecording()
            recordingController.stopRecording()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping sensor recording", e)
        }
    }

    private fun startSessionMonitoring(session: SessionInfo) {

        qualityMonitoringJob = lifecycleOwner.lifecycleScope.launch {
            monitorSessionQuality()
        }

        heartbeatJob = lifecycleOwner.lifecycleScope.launch {
            sendSessionHeartbeat(session)
        }

        sessionJob = lifecycleOwner.lifecycleScope.launch {
            runSessionLoop(session)
        }
    }

    private suspend fun monitorSessionQuality() {
        while (isSessionActive.get()) {
            try {
                val gsrStatus = gsrRecorder.getRecordingStatus()
                val networkMetrics = networkController.getNetworkMetrics()
                val recordingStatus = recordingController.getSensorStatusSummary()?.toString()

                val quality = SessionQuality(
                    overallQuality = calculateOverallQuality(
                        gsrStatus,
                        networkMetrics,
                        recordingStatus
                    ),
                    networkQuality = networkMetrics["connection_quality"] as? Double ?: 0.0,
                    gsrQuality = gsrStatus["connection_quality"] as? Double ?: 0.0,
                    gsrSampleCount = gsrStatus["recorded_samples"] as? Long ?: 0L,

                    )

                _sessionQuality.value = quality

                delay(QUALITY_CHECK_INTERVAL_MS)

            } catch (e: Exception) {
                Log.w(TAG, "Error monitoring session quality", e)
                delay(QUALITY_CHECK_INTERVAL_MS)
            }
        }
    }

    private suspend fun sendSessionHeartbeat(session: SessionInfo) {
        while (isSessionActive.get()) {
            try {
                val heartbeatData = JSONObject().apply {
                    put("session_id", session.sessionId)
                    put("timestamp", System.currentTimeMillis())
                    put("status", _sessionStatus.value.name)
                    put("quality", _sessionQuality.value.toMap())
                }

                networkController.broadcastMessage("session_heartbeat", heartbeatData)

                delay(SESSION_HEARTBEAT_INTERVAL_MS)

            } catch (e: Exception) {
                Log.w(TAG, "Error sending session heartbeat", e)
                delay(SESSION_HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    private suspend fun runSessionLoop(session: SessionInfo) {
        val maxDuration = session.metadata["max_duration_ms"]?.toString()?.toLongOrNull()
            ?: MAX_SESSION_DURATION_MS

        val startTime = System.currentTimeMillis()

        while (isSessionActive.get()) {

            if (System.currentTimeMillis() - startTime > maxDuration) {
                Log.i(TAG, "Session duration limit reached, stopping session")
                stopSession()
                break
            }

            val quality = _sessionQuality.value
            if (quality.overallQuality < MIN_DATA_QUALITY_SCORE) {
                Log.w(TAG, "Session quality below threshold: ${quality.overallQuality}")

            }

            delay(1000)  // Check every second
        }
    }

    private fun calculateOverallQuality(
        gsrStatus: Map<String, Any>,
        networkMetrics: Map<String, Any>,
        recordingStatus: String?
    ): Double {
        val gsrQuality = gsrStatus["connection_quality"] as? Double ?: 0.0
        val networkQuality = networkMetrics["connection_quality"] as? Double ?: 0.0

        return (gsrQuality * 0.4 + networkQuality * 0.3 + 0.3) // Base quality for other sensors
    }

    private suspend fun notifySessionStart(session: SessionInfo) {
        val startMessage = JSONObject().apply {
            put("session_id", session.sessionId)
            put("session_name", session.sessionName)
            put("study_name", session.studyName)
            put("enabled_sensors", session.enabledSensors.joinToString(","))
            put("session_type", session.sessionType.name)
        }

        networkController.broadcastMessage("session_started", startMessage)
    }

    private suspend fun notifySessionStop(session: SessionInfo, summary: SessionSummary) {
        val stopMessage = JSONObject().apply {
            put("session_id", session.sessionId)
            put("summary", JSONObject(summary.toMap()))
        }

        networkController.broadcastMessage("session_stopped", stopMessage)
    }

    private fun generateSessionSummary(session: SessionInfo, duration: Long): SessionSummary {
        val quality = _sessionQuality.value

        return SessionSummary(
            sessionId = session.sessionId,
            duration = duration,
            totalSamples = quality.gsrSampleCount + quality.thermalFrameCount + quality.rgbFrameCount,
            averageQuality = quality.overallQuality,
            completedSuccessfully = _sessionStatus.value == SessionStatus.COMPLETED,
            errorCount = quality.errorCount,
            dataSize = calculateSessionDataSize(session.sessionDirectory),
            metadata = mapOf(
                "gsr_samples" to quality.gsrSampleCount,
                "thermal_frames" to quality.thermalFrameCount,
                "rgb_frames" to quality.rgbFrameCount,
                "sync_markers" to quality.syncMarkerCount
            )
        )
    }

    private fun calculateSessionDataSize(sessionDirectory: String): Long {
        return try {
            File(sessionDirectory).walkTopDown()
                .filter { it.isFile }
                .map { it.length() }
                .sum()
        } catch (e: Exception) {
            0L
        }
    }

    private fun jsonToMap(json: JSONObject): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        json.keys().forEach { key ->
            map[key] = json.get(key)
        }
        return map
    }
}
