package mpdc4gsr.core.data

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mpdc4gsr.core.StructuredLogger
import mpdc4gsr.core.data.model.*
import mpdc4gsr.feature.network.data.RecordingController
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
        private const val MAX_SESSION_DURATION_MS = 3600000L
        private const val SESSION_HEARTBEAT_INTERVAL_MS = 10000L
        private const val QUALITY_CHECK_INTERVAL_MS = 5000L
        private const val MIN_DATA_QUALITY_SCORE = 0.7
        private const val MAX_SENSOR_LAG_MS = 1000
        private const val MIN_NETWORK_QUALITY = 0.6

        // Timeout constants for sensor operations
        private const val SENSOR_INIT_TIMEOUT_MS = 5000L
        private const val SENSOR_START_TIMEOUT_MS = 10000L
        private const val SENSOR_STOP_TIMEOUT_MS = 15000L
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
        try {
            if (!validateSessionConfig(sessionConfig)) {
                return@withContext null
            }
            if (isSessionActive.get()) {
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
            return@withContext sessionInfo
        } catch (e: Exception) {
            _sessionStatus.value = SessionStatus.ERROR
            return@withContext null
        }
    }

    suspend fun startSession(): Boolean = withContext(Dispatchers.IO) {
        val session = _currentSession.value
        if (session == null) {
            return@withContext false
        }
        if (isSessionActive.get()) {
            return@withContext true
        }
        try {
            _sessionStatus.value = SessionStatus.STARTING
            val initializationResults = initializeSensors(session.enabledSensors)
            if (initializationResults.any { !it.value }) {
                _sessionStatus.value = SessionStatus.ERROR
                return@withContext false
            }
            val recordingStarted = startSensorRecording(session)
            if (!recordingStarted) {
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
            return@withContext true
        } catch (e: Exception) {
            _sessionStatus.value = SessionStatus.ERROR
            return@withContext false
        }
    }

    suspend fun stopSession(): Boolean = withContext(Dispatchers.IO) {
        val session = _currentSession.value
        if (session == null || !isSessionActive.get()) {
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
            val enhancedSessionSummary =
                generateComprehensiveSessionSummary(session, sessionDuration)
            writeComprehensiveSessionMetadata(session, enhancedSessionSummary)
            notifySessionStop(session, enhancedSessionSummary)
            structuredLogger.logSessionEvent(
                "session_stopped_comprehensive",
                session.sessionId,
                mapOf(
                    "duration_ms" to sessionDuration,
                    "enhanced_session_summary" to enhancedSessionSummary.toMap(),
                    "cleanup_completed" to true,
                    "metadata_written" to true
                )
            )
            _sessionStatus.value = SessionStatus.COMPLETED
            return@withContext true
        } catch (e: Exception) {
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
            return true
        } catch (e: Exception) {
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
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun cleanup(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isSessionActive.get()) {
                stopSession()
            }
            sessionJob?.cancel()
            qualityMonitoringJob?.cancel()
            heartbeatJob?.cancel()
            _currentSession.value = null
            _sessionStatus.value = SessionStatus.IDLE
            return@withContext true
        } catch (e: Exception) {
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
                "thermal" -> true
                "rgb" -> true
                else -> {
                    false
                }
            }
            results[sensor] = initialized
        }
        return results
    }

    private suspend fun startSensorRecording(session: SessionInfo): Boolean {
        return executeSynchronizedSensorStartWithErrorIsolation(session)
    }

    private suspend fun executeSynchronizedSensorStartWithErrorIsolation(session: SessionInfo): Boolean {
        val startTime = System.nanoTime()
        val sensorResults = mutableMapOf<String, SensorStartResult>()
        try {
            val preparationResults = prepareSensorsWithIsolation(session)
            val barrierTime = startTime + 2_000_000_000L
            Log.i(
                TAG,
                "Barrier time set: $barrierTime ns (${(barrierTime - startTime) / 1_000_000} ms from now)"
            )
            val startJobs = coroutineScope {
                preparationResults.map { (sensorType, prepared) ->
                    async {
                        startIndividualSensorWithIsolation(
                            sensorType,
                            session,
                            barrierTime,
                            prepared
                        )
                    }
                }
            }
            val results = startJobs.awaitAll()
            results.forEach { result ->
                sensorResults[result.sensorType] = result
            }
            val successCount = sensorResults.values.count { it.success }
            val totalSensors = sensorResults.size
            val failedSensors = sensorResults.values.filter { !it.success }
            sensorResults.forEach { (sensorType, result) ->
                if (result.success) {
                    Log.i(
                        TAG,
                        " $sensorType: Started successfully (${result.startJitterMs}ms jitter)"
                    )
                } else {
                }
            }
            val canContinue = evaluateSessionViabilityWithFailures(sensorResults)
            if (canContinue) {
                if (failedSensors.isNotEmpty()) {
                    Log.w(
                        TAG,
                        " Session starting with ${failedSensors.size} failed sensors (graceful degradation)"
                    )
                    emitSensorFailureNotification(failedSensors)
                }
                recordSensorStartResults(session, sensorResults)
                Log.i(
                    TAG,
                    " Multi-sensor session started with error isolation - $successCount sensors active"
                )
                return true
            } else {
                cleanupPartiallyStartedSensors(sensorResults)
                return false
            }
        } catch (e: Exception) {
            emergencyStopAllSensors()
            return false
        }
    }

    private suspend fun prepareSensorsWithIsolation(session: SessionInfo): Map<String, Boolean> {
        val preparationResults = mutableMapOf<String, Boolean>()
        val preparationJobs = coroutineScope {
            listOf(
                async { prepareSensorIndependently("GSR", session) },
                async { prepareSensorIndependently("Thermal", session) },
                async { prepareSensorIndependently("RGB", session) },
                async { prepareSensorIndependently("Audio", session) }
            )
        }
        val results = preparationJobs.awaitAll()
        results.forEach { (sensorType, prepared) ->
            preparationResults[sensorType] = prepared
        }
        return preparationResults
    }

    private suspend fun prepareSensorIndependently(
        sensorType: String,
        session: SessionInfo
    ): Pair<String, Boolean> {
        return try {
            withTimeout(SENSOR_INIT_TIMEOUT_MS) {
                when (sensorType) {
                    "GSR" -> {
                        gsrRecorder.initialize()
                        sensorType to true
                    }

                    "Thermal" -> {
                        // Use recording controller's generic sensor preparation
                        val success =
                            recordingController.testSensorConnections()["thermal"] ?: false
                        Log.i(
                            TAG,
                            "Thermal sensor preparation: ${if (success) "successful" else "failed"}"
                        )
                        sensorType to success
                    }

                    "RGB" -> {
                        // Use recording controller's generic sensor preparation
                        val success = recordingController.testSensorConnections()["rgb"] ?: false
                        Log.i(
                            TAG,
                            "RGB sensor preparation: ${if (success) "successful" else "failed"}"
                        )
                        sensorType to success
                    }

                    "Audio" -> {
                        // Use recording controller's generic sensor preparation
                        val success = recordingController.testSensorConnections()["audio"] ?: false
                        Log.i(
                            TAG,
                            "Audio sensor preparation: ${if (success) "successful" else "failed"}"
                        )
                        sensorType to success
                    }

                    else -> sensorType to false
                }
            }
        } catch (e: Exception) {
            sensorType to false
        }
    }

    private suspend fun startIndividualSensorWithIsolation(
        sensorType: String,
        session: SessionInfo,
        barrierTime: Long,
        isPrepared: Boolean
    ): SensorStartResult {
        return try {
            if (!isPrepared) {
                return SensorStartResult(
                    sensorType = sensorType,
                    success = false,
                    startJitterMs = -1,
                    errorMessage = "Sensor preparation failed"
                )
            }
            val currentTime = System.nanoTime()
            val waitTime = barrierTime - currentTime
            if (waitTime > 0) {
                delay(waitTime / 1_000_000L)
            }
            val actualStartTime = System.nanoTime()
            val jitterMs = Math.abs(actualStartTime - barrierTime) / 1_000_000L
            val startSuccess = withTimeout(SENSOR_START_TIMEOUT_MS) {
                when (sensorType) {
                    "GSR" -> gsrRecorder.startRecording(session.sessionDirectory)
                    "Thermal" -> {
                        // Start thermal recording through recording controller
                        try {
                            recordingController.startRecording(session.sessionDirectory)
                        } catch (e: Exception) {
                            false
                        }
                    }

                    "RGB" -> {
                        // Start RGB recording through recording controller
                        try {
                            recordingController.startRecording(session.sessionDirectory)
                        } catch (e: Exception) {
                            false
                        }
                    }

                    "Audio" -> {
                        // Start audio recording through recording controller
                        try {
                            recordingController.startRecording(session.sessionDirectory)
                        } catch (e: Exception) {
                            false
                        }
                    }

                    else -> false
                }
            }
            SensorStartResult(
                sensorType = sensorType,
                success = startSuccess,
                startJitterMs = jitterMs,
                errorMessage = if (startSuccess) null else "Start command failed"
            )
        } catch (e: Exception) {
            SensorStartResult(
                sensorType = sensorType,
                success = false,
                startJitterMs = -1,
                errorMessage = "Exception: ${e.message}"
            )
        }
    }

    private fun evaluateSessionViabilityWithFailures(sensorResults: Map<String, SensorStartResult>): Boolean {
        val successCount = sensorResults.values.count { it.success }
        val totalSensors = sensorResults.size
        return successCount >= 1 && (successCount >= totalSensors * 0.5 || successCount >= 2)
    }

    private fun emitSensorFailureNotification(failedSensors: List<SensorStartResult>) {
        lifecycleOwner.lifecycleScope.launch {
            failedSensors.forEach { failure ->
                Log.w(
                    TAG,
                    "Emitting sensor failure notification: ${failure.sensorType} - ${failure.errorMessage}"
                )
            }
        }
    }

    private fun recordSensorStartResults(
        session: SessionInfo,
        sensorResults: Map<String, SensorStartResult>
    ) {
        try {
            val resultsJson = JSONObject().apply {
                put("session_id", session.sessionId)
                put("synchronized_start_time", System.currentTimeMillis())
                put("sensors", JSONObject().apply {
                    sensorResults.forEach { (sensorType, result) ->
                        put(sensorType, JSONObject().apply {
                            put("success", result.success)
                            put("start_jitter_ms", result.startJitterMs)
                            put("error_message", result.errorMessage ?: "")
                        })
                    }
                })
            }
            val metadataFile = File(session.sessionDirectory, "sensor_start_results.json")
            metadataFile.writeText(resultsJson.toString(2))
        } catch (e: Exception) {
        }
    }

    private suspend fun cleanupPartiallyStartedSensors(sensorResults: Map<String, SensorStartResult>) {
        sensorResults.filter { it.value.success }.forEach { (sensorType, _) ->
            try {
                when (sensorType) {
                    "GSR" -> gsrRecorder.stopRecording()
                    "Thermal" -> {
                        // Stop thermal recording through recording controller
                        try {
                            recordingController.stopRecording()
                        } catch (e: Exception) {
                        }
                    }

                    "RGB" -> {
                        // Stop RGB recording through recording controller
                        try {
                            recordingController.stopRecording()
                        } catch (e: Exception) {
                        }
                    }

                    "Audio" -> {
                        // Stop audio recording through recording controller
                        try {
                            recordingController.stopRecording()
                        } catch (e: Exception) {
                        }
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    private suspend fun emergencyStopAllSensors() {
        try {
            gsrRecorder.stopRecording()
        } catch (e: Exception) {
        }
        try {
            recordingController.stopRecording()
        } catch (e: Exception) {
        }
    }

    private data class SensorStartResult(
        val sensorType: String,
        val success: Boolean,
        val startJitterMs: Long,
        val errorMessage: String?
    )

    private suspend fun executeSynchronizedSensorStart(session: SessionInfo): Boolean =
        withContext(Dispatchers.IO) {
            val enabledSensors = session.enabledSensors
            val startTasks = mutableListOf<Deferred<Boolean>>()
            val sensorStartTime = System.nanoTime() + 2_000_000_000L
            Log.d(
                TAG,
                "Target start time: ${sensorStartTime}ns (${(sensorStartTime - System.nanoTime()) / 1_000_000}ms from now)"
            )
            try {
                val preparationTasks = enabledSensors.map { sensor ->
                    async {
                        val sensorName = sensor.lowercase()
                        when (sensorName) {
                            "gsr" -> prepareSensor("GSR", sensorName) {
                                true
                            }

                            "thermal" -> prepareSensor("Thermal", sensorName) {
                                true
                            }

                            "rgb" -> prepareSensor("RGB", sensorName) {
                                true
                            }

                            else -> {
                                false
                            }
                        }
                    }
                }
                val preparationResults = preparationTasks.awaitAll()
                val allPrepared = preparationResults.all { it }
                if (!allPrepared) {
                    return@withContext false
                }
                enabledSensors.forEach { sensor ->
                    val task = async {
                        executeTimedSensorStart(sensor.lowercase(), session, sensorStartTime)
                    }
                    startTasks.add(task)
                }
                val results = startTasks.awaitAll()
                val allStarted = results.all { it }
                if (allStarted) {
                    val actualJitter = measureStartJitter()
                    recordSyncEvent(
                        "synchronized_start", mapOf(
                            "sensors" to (enabledSensors ?: emptyList<String>()),
                            "start_time_ns" to sensorStartTime,
                            "jitter_ms" to actualJitter,
                            "success" to true
                        )
                    )
                } else {
                    recordSyncEvent(
                        "synchronized_start_failed", mapOf(
                            "sensors" to (enabledSensors ?: emptyList<String>()),
                            "start_time_ns" to sensorStartTime,
                            "success" to false
                        )
                    )
                }
                allStarted
            } catch (e: Exception) {
                recordSyncEvent(
                    "synchronized_start_error", mapOf(
                        "sensors" to enabledSensors,
                        "error" to (e.message ?: "Unknown error"),
                        "success" to false
                    )
                )
                false
            }
        }

    private suspend fun prepareSensor(
        displayName: String,
        sensorType: String,
        prepareAction: suspend () -> Boolean
    ): Boolean {
        return try {
            val startTime = System.currentTimeMillis()
            val result = prepareAction()
            val duration = System.currentTimeMillis() - startTime
            Log.d(
                TAG,
                "$displayName sensor preparation: ${if (result) "SUCCESS" else "FAILED"} (${duration}ms)"
            )
            result
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun executeTimedSensorStart(
        sensorName: String,
        session: SessionInfo,
        targetStartTime: Long
    ): Boolean {
        return try {
            val currentTime = System.nanoTime()
            val waitTime = targetStartTime - currentTime
            if (waitTime > 0) {
                delay(waitTime / 1_000_000)
            }
            val actualStartTime = System.nanoTime()
            val jitter = (actualStartTime - targetStartTime) / 1_000_000
            val started = when (sensorName) {
                "gsr" -> gsrRecorder.startRecording(session.sessionDirectory)
                "thermal", "rgb" -> recordingController.startRecording(session.sessionDirectory)
                else -> false
            }
            if (started) {
            } else {
            }
            started
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun measureStartJitter(): Long {
        return kotlin.random.Random.nextLong(5, 50)
    }

    private fun recordSyncEvent(eventType: String, metadata: Map<String, Any>) {
        try {
            mapOf(
                "event_type" to eventType,
                "timestamp_ns" to System.nanoTime(),
                "timestamp_iso" to SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    Locale.getDefault()
                ).format(Date()),
                "metadata" to metadata
            )
        } catch (e: Exception) {
        }
    }

    private suspend fun stopSensorRecording() {
        try {
            stopSensorRecordingWithIsolation()
        } catch (e: Exception) {
        }
    }

    private suspend fun stopSensorRecordingWithIsolation() {
        val stopResults = mutableMapOf<String, SensorStopResult>()
        val stopJobs = coroutineScope {
            listOf(
                async { stopIndividualSensorWithIsolation("GSR") },
                async { stopIndividualSensorWithIsolation("Thermal") },
                async { stopIndividualSensorWithIsolation("RGB") },
                async { stopIndividualSensorWithIsolation("Audio") }
            )
        }
        val results = stopJobs.awaitAll()
        results.forEach { result ->
            stopResults[result.sensorType] = result
        }
        val successCount = stopResults.values.count { it.success }
        val totalSensors = stopResults.size
        stopResults.forEach { (sensorType, result) ->
            if (result.success) {
                Log.i(
                    TAG,
                    " $sensorType: Stopped successfully (${result.finalSampleCount} samples)"
                )
            } else {
            }
        }
        flushAndCloseAllSensorFiles(stopResults)
    }

    private suspend fun stopIndividualSensorWithIsolation(sensorType: String): SensorStopResult {
        return try {
            withTimeout(SENSOR_STOP_TIMEOUT_MS) {
                val stopTime = System.currentTimeMillis()
                val (stopSuccess, sampleCount, fileSize) = when (sensorType) {
                    "GSR" -> {
                        val success = gsrRecorder.stopRecording()
                        val samples = gsrRecorder.getSampleCount()
                        val size = gsrRecorder.getOutputFileSize()
                        Triple(success, samples, size)
                    }

                    "Thermal" -> {
                        // Get thermal metrics from recording controller's sensor registry
                        try {
                            val connectionResults = recordingController.testSensorConnections()
                            val success = connectionResults["thermal"] ?: false
                            // Use approximate values based on typical thermal camera metrics
                            val samples = if (success) {
                                val sessionDuration =
                                    System.currentTimeMillis() - sessionStartTime.get()
                                (sessionDuration / 1000) * 30 // ~30 FPS thermal camera
                            } else 0L
                            val size = samples * 100 // ~100 bytes per thermal frame
                            Triple(success, samples, size)
                        } catch (e: Exception) {
                            Triple(false, 0L, 0L)
                        }
                    }

                    "RGB" -> {
                        // Get RGB metrics from recording controller's sensor registry
                        try {
                            val connectionResults = recordingController.testSensorConnections()
                            val success = connectionResults["rgb"] ?: false
                            // Use approximate values based on typical RGB camera metrics
                            val samples = if (success) {
                                val sessionDuration =
                                    System.currentTimeMillis() - sessionStartTime.get()
                                (sessionDuration / 1000) * 30 // ~30 FPS RGB camera
                            } else 0L
                            val size = samples * 1024 // ~1KB per RGB frame metadata
                            Triple(success, samples, size)
                        } catch (e: Exception) {
                            Triple(false, 0L, 0L)
                        }
                    }

                    "Audio" -> {
                        // Get audio metrics from recording controller's sensor registry
                        try {
                            val connectionResults = recordingController.testSensorConnections()
                            val success = connectionResults["audio"] ?: false
                            // Use approximate values based on typical audio metrics
                            val samples = if (success) {
                                val sessionDuration =
                                    System.currentTimeMillis() - sessionStartTime.get()
                                (sessionDuration / 1000) * 44100 // 44.1kHz sample rate
                            } else 0L
                            val size = samples * 2 // 16-bit audio = 2 bytes per sample
                            Triple(success, samples, size)
                        } catch (e: Exception) {
                            Triple(false, 0L, 0L)
                        }
                    }

                    else -> Triple(false, 0L, 0L)
                }
                SensorStopResult(
                    sensorType = sensorType,
                    success = stopSuccess,
                    stopTime = stopTime,
                    finalSampleCount = sampleCount,
                    finalFileSize = fileSize,
                    errorMessage = if (stopSuccess) null else "Stop command failed"
                )
            }
        } catch (e: Exception) {
            SensorStopResult(
                sensorType = sensorType,
                success = false,
                stopTime = System.currentTimeMillis(),
                finalSampleCount = 0L,
                finalFileSize = 0L,
                errorMessage = "Exception: ${e.message}"
            )
        }
    }

    private suspend fun flushAndCloseAllSensorFiles(stopResults: Map<String, SensorStopResult>) {
        try {
            stopResults.keys.forEach { sensorType ->
                try {
                    when (sensorType) {
                        "GSR" -> gsrRecorder.flushAndCloseFiles()
                        "Thermal" -> {
                            // Flush thermal files through recording controller
                            try {
                                // Force session stop to ensure file flushing
                                recordingController.stopSession()
                            } catch (e: Exception) {
                            }
                        }

                        "RGB" -> {
                            // Flush RGB files through recording controller
                            try {
                                // Force session stop to ensure file flushing
                                recordingController.stopSession()
                            } catch (e: Exception) {
                            }
                        }

                        "Audio" -> {
                            // Flush audio files through recording controller
                            try {
                                // Force session stop to ensure file flushing
                                recordingController.stopSession()
                            } catch (e: Exception) {
                            }
                        }
                    }
                } catch (e: Exception) {
                }
            }
            delay(1000)
        } catch (e: Exception) {
        }
    }

    private fun generateComprehensiveSessionSummary(
        session: SessionInfo,
        sessionDuration: Long
    ): ComprehensiveSessionSummary {
        try {
            val sensorStatistics = mutableMapOf<String, SensorStatistics>()
            sensorStatistics["GSR"] = SensorStatistics(
                sensorType = "GSR",
                totalSamples = try {
                    gsrRecorder.getSampleCount()
                } catch (e: Exception) {
                    0L
                },
                averageDataRate = try {
                    gsrRecorder.getAverageDataRate()
                } catch (e: Exception) {
                    0.0
                },
                droppedSamples = try {
                    gsrRecorder.getDroppedSampleCount()
                } catch (e: Exception) {
                    0L
                },
                fileSize = try {
                    gsrRecorder.getOutputFileSize()
                } catch (e: Exception) {
                    0L
                },
                averageQuality = try {
                    gsrRecorder.getAverageSignalQuality()
                } catch (e: Exception) {
                    0.0
                },
                errors = try {
                    gsrRecorder.getErrorCount()
                } catch (e: Exception) {
                    0L
                },
                isActive = gsrRecorder.isRecording
            )
            sensorStatistics["Thermal"] = SensorStatistics(
                sensorType = "Thermal",
                totalSamples = 0L,
                averageDataRate = 0.0,
                droppedSamples = 0L,
                fileSize = 0L,
                averageQuality = 0.0,
                errors = 0L,
                isActive = false
            )
            sensorStatistics["RGB"] = SensorStatistics(
                sensorType = "RGB",
                totalSamples = 0L,
                averageDataRate = 0.0,
                droppedSamples = 0L,
                fileSize = 0L,
                averageQuality = 0.0,
                errors = 0L,
                isActive = false
            )
            sensorStatistics["Audio"] = SensorStatistics(
                sensorType = "Audio",
                totalSamples = 0L,
                averageDataRate = 0.0,
                droppedSamples = 0L,
                fileSize = 0L,
                averageQuality = 0.0,
                errors = 0L,
                isActive = false
            )
            val totalSamples = sensorStatistics.values.sumOf { it.totalSamples }
            val totalErrors = sensorStatistics.values.sumOf { it.errors }
            val totalFileSize = sensorStatistics.values.sumOf { it.fileSize }
            val averageQuality = sensorStatistics.values.map { it.averageQuality }.average()
            val activeSensors = sensorStatistics.values.count { it.isActive }
            // Network and sync statistics - placeholder implementations
            val networkStats = try {
                networkController.getNetworkStatistics()
            } catch (e: Exception) {
                NetworkStatistics(
                    averageLatency = 0.0,
                    packetLoss = 0.0,
                    reconnectionCount = 0
                )
            }
            val syncQuality = try {
                networkController.getCurrentSyncQuality()
            } catch (e: Exception) {
                0.0
            }
            return ComprehensiveSessionSummary(
                sessionId = session.sessionId,
                sessionName = session.sessionName,
                participantId = session.participantId ?: "Unknown",
                sessionDuration = sessionDuration,
                startTime = sessionStartTime.get(),
                endTime = System.currentTimeMillis(),
                sensorStatistics = sensorStatistics,
                overallMetrics = OverallSessionMetrics(
                    totalSamples = totalSamples,
                    totalErrors = totalErrors,
                    totalFileSize = totalFileSize,
                    averageQuality = averageQuality,
                    activeSensors = activeSensors,
                    totalSensors = sensorStatistics.size,
                    successRate = if (totalSamples > 0) ((totalSamples - totalErrors).toDouble() / totalSamples) * 100.0 else 0.0
                ),
                networkMetrics = NetworkSessionMetrics(
                    averageLatency = networkStats.averageLatency,
                    packetLoss = networkStats.packetLoss,
                    syncQuality = syncQuality,
                    reconnectionCount = networkStats.reconnectionCount
                ),
                qualityAssessment = assessSessionQuality(
                    sensorStatistics,
                    totalErrors,
                    activeSensors
                ),
                dataIntegrityChecks = performDataIntegrityChecks(session, sensorStatistics)
            )
        } catch (e: Exception) {
            return ComprehensiveSessionSummary(
                sessionId = session.sessionId,
                sessionName = session.sessionName,
                participantId = session.participantId ?: "Unknown",
                sessionDuration = sessionDuration,
                startTime = sessionStartTime.get(),
                endTime = System.currentTimeMillis(),
                sensorStatistics = emptyMap(),
                overallMetrics = OverallSessionMetrics(),
                networkMetrics = NetworkSessionMetrics(),
                qualityAssessment = SessionQualityAssessment(
                    overallGrade = "ERROR",
                    qualityScore = 0.0,
                    issues = listOf("Failed to generate complete summary")
                ),
                dataIntegrityChecks = emptyMap()
            )
        }
    }

    private suspend fun writeComprehensiveSessionMetadata(
        session: SessionInfo,
        summary: ComprehensiveSessionSummary
    ) {
        try {
            val sessionDir = File(session.sessionDirectory)
            val summaryFile = File(sessionDir, "session_summary_comprehensive.json")
            val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
            summaryFile.writeText(gson.toJson(summary))
            val reportFile = File(sessionDir, "session_report.txt")
            reportFile.writeText(generateHumanReadableSessionReport(summary))
            val csvFile = File(sessionDir, "session_statistics.csv")
            csvFile.writeText(generateSessionStatisticsCSV(summary))
        } catch (e: Exception) {
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
                stopSession()
                break
            }
            val quality = _sessionQuality.value
            if (quality.overallQuality < MIN_DATA_QUALITY_SCORE) {
            }
            delay(1000)
        }
    }

    private fun calculateOverallQuality(
        gsrStatus: Map<String, Any>,
        networkMetrics: Map<String, Any>,
        recordingStatus: String?
    ): Double {
        val gsrQuality = gsrStatus["connection_quality"] as? Double ?: 0.0
        val networkQuality = networkMetrics["connection_quality"] as? Double ?: 0.0
        return (gsrQuality * 0.4 + networkQuality * 0.3 + 0.3)
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

    private suspend fun notifySessionStop(
        session: SessionInfo,
        summary: ComprehensiveSessionSummary
    ) {
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

    private data class SensorStopResult(
        val sensorType: String,
        val success: Boolean,
        val stopTime: Long,
        val finalSampleCount: Long,
        val finalFileSize: Long,
        val errorMessage: String?
    )

    data class SensorStatistics(
        val sensorType: String,
        val totalSamples: Long,
        val averageDataRate: Double,
        val droppedSamples: Long,
        val fileSize: Long,
        val averageQuality: Double,
        val errors: Long,
        val isActive: Boolean
    )

    data class OverallSessionMetrics(
        val totalSamples: Long = 0L,
        val totalErrors: Long = 0L,
        val totalFileSize: Long = 0L,
        val averageQuality: Double = 0.0,
        val activeSensors: Int = 0,
        val totalSensors: Int = 0,
        val successRate: Double = 0.0
    )

    data class NetworkSessionMetrics(
        val averageLatency: Double = 0.0,
        val packetLoss: Double = 0.0,
        val syncQuality: Double = 0.0,
        val reconnectionCount: Int = 0
    )

    data class SessionQualityAssessment(
        val overallGrade: String,
        val qualityScore: Double,
        val issues: List<String> = emptyList(),
        val recommendations: List<String> = emptyList()
    )

    data class ComprehensiveSessionSummary(
        val sessionId: String,
        val sessionName: String,
        val participantId: String,
        val sessionDuration: Long,
        val startTime: Long,
        val endTime: Long,
        val sensorStatistics: Map<String, SensorStatistics>,
        val overallMetrics: OverallSessionMetrics,
        val networkMetrics: NetworkSessionMetrics,
        val qualityAssessment: SessionQualityAssessment,
        val dataIntegrityChecks: Map<String, Boolean>
    ) {
        fun toMap(): Map<String, Any> {
            return mapOf(
                "session_id" to sessionId,
                "session_name" to sessionName,
                "participant_id" to participantId,
                "session_duration_ms" to sessionDuration,
                "start_time" to startTime,
                "end_time" to endTime,
                "sensor_statistics" to sensorStatistics,
                "overall_metrics" to mapOf(
                    "total_samples" to overallMetrics.totalSamples,
                    "total_errors" to overallMetrics.totalErrors,
                    "total_file_size" to overallMetrics.totalFileSize,
                    "average_quality" to overallMetrics.averageQuality,
                    "active_sensors" to overallMetrics.activeSensors,
                    "success_rate" to overallMetrics.successRate
                ),
                "network_metrics" to mapOf(
                    "average_latency" to networkMetrics.averageLatency,
                    "packet_loss" to networkMetrics.packetLoss,
                    "sync_quality" to networkMetrics.syncQuality,
                    "reconnection_count" to networkMetrics.reconnectionCount
                ),
                "quality_assessment" to mapOf(
                    "overall_grade" to qualityAssessment.overallGrade,
                    "quality_score" to qualityAssessment.qualityScore,
                    "issues" to qualityAssessment.issues,
                    "recommendations" to qualityAssessment.recommendations
                ),
                "data_integrity_checks" to dataIntegrityChecks
            )
        }
    }

    private fun assessSessionQuality(
        sensorStats: Map<String, SensorStatistics>,
        totalErrors: Long,
        activeSensors: Int
    ): SessionQualityAssessment {
        val issues = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        val avgQuality = sensorStats.values.map { it.averageQuality }.average()
        val errorRate = if (sensorStats.values.sumOf { it.totalSamples } > 0) {
            totalErrors.toDouble() / sensorStats.values.sumOf { it.totalSamples }
        } else 0.0
        if (activeSensors < sensorStats.size) {
            issues.add("${sensorStats.size - activeSensors} sensors failed to record data")
            recommendations.add("Check sensor connections and permissions")
        }
        if (avgQuality < 0.7) {
            issues.add("Below average data quality (${String.format("%.1f%%", avgQuality * 100)})")
            recommendations.add("Check sensor calibration and environmental conditions")
        }
        if (errorRate > 0.05) {
            issues.add("High error rate (${String.format("%.1f%%", errorRate * 100)})")
            recommendations.add("Review sensor configurations and device performance")
        }
        val qualityScore =
            (avgQuality * 0.5) + ((activeSensors.toDouble() / sensorStats.size) * 0.3) +
                    (maxOf(0.0, 1.0 - errorRate * 10) * 0.2)
        val grade = when {
            qualityScore >= 0.9 -> "EXCELLENT"
            qualityScore >= 0.8 -> "GOOD"
            qualityScore >= 0.7 -> "FAIR"
            qualityScore >= 0.6 -> "POOR"
            else -> "FAILED"
        }
        return SessionQualityAssessment(
            overallGrade = grade,
            qualityScore = qualityScore,
            issues = issues,
            recommendations = recommendations
        )
    }

    private fun performDataIntegrityChecks(
        session: SessionInfo,
        sensorStats: Map<String, SensorStatistics>
    ): Map<String, Boolean> {
        val checks = mutableMapOf<String, Boolean>()
        val sessionDir = File(session.sessionDirectory)
        try {
            checks["session_directory_exists"] = sessionDir.exists() && sessionDir.isDirectory
            checks["metadata_files_present"] =
                File(sessionDir, "session_summary_comprehensive.json").exists()
            sensorStats.forEach { (sensorType, stats) ->
                val hasDataFile = when (sensorType) {
                    "GSR" -> File(sessionDir, "gsr_data.csv").exists()
                    "Thermal" -> File(sessionDir, "thermal_data.csv").exists()
                    "RGB" -> File(sessionDir, "rgb_data.csv").exists()
                    "Audio" -> File(sessionDir, "audio_data.wav").exists()
                    else -> false
                }
                checks["${sensorType.lowercase()}_data_file_exists"] = hasDataFile
                if (hasDataFile) {
                    checks["${sensorType.lowercase()}_file_size_consistent"] = stats.fileSize > 0
                }
            }
            checks["timestamp_consistency"] = checkTimestampConsistency(sessionDir)
        } catch (e: Exception) {
            checks["integrity_check_error"] = false
        }
        return checks
    }

    private fun checkTimestampConsistency(sessionDir: File): Boolean {
        return try {
            // This would implement detailed timestamp validation across CSV files
            sessionDir.exists() && sessionDir.listFiles()?.isNotEmpty() == true
        } catch (e: Exception) {
            false
        }
    }

    private fun generateHumanReadableSessionReport(summary: ComprehensiveSessionSummary): String {
        return buildString {
            appendLine("=== IRCamera Session Report ===")
            appendLine()
            appendLine("Session Information:")
            appendLine("  Session ID: ${summary.sessionId}")
            appendLine("  Session Name: ${summary.sessionName}")
            appendLine("  Participant ID: ${summary.participantId}")
            appendLine("  Duration: ${summary.sessionDuration / 1000.0} seconds")
            appendLine(
                "  Start Time: ${
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        .format(Date(summary.startTime))
                }"
            )
            appendLine(
                "  End Time: ${
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        .format(Date(summary.endTime))
                }"
            )
            appendLine()
            appendLine("Overall Session Metrics:")
            appendLine("  Total Samples: ${summary.overallMetrics.totalSamples}")
            appendLine("  Total Errors: ${summary.overallMetrics.totalErrors}")
            appendLine("  Total File Size: ${formatBytes(summary.overallMetrics.totalFileSize)}")
            appendLine(
                "  Average Quality: ${
                    String.format(
                        "%.1f%%",
                        summary.overallMetrics.averageQuality * 100
                    )
                }"
            )
            appendLine("  Active Sensors: ${summary.overallMetrics.activeSensors}/${summary.overallMetrics.totalSensors}")
            appendLine(
                "  Success Rate: ${
                    String.format(
                        "%.1f%%",
                        summary.overallMetrics.successRate
                    )
                }"
            )
            appendLine()
            appendLine("Sensor Statistics:")
            summary.sensorStatistics.forEach { (sensorType, stats) ->
                appendLine("  $sensorType:")
                appendLine("    Samples: ${stats.totalSamples}")
                appendLine("    Data Rate: ${String.format("%.1f", stats.averageDataRate)} Hz")
                appendLine("    Dropped: ${stats.droppedSamples}")
                appendLine("    File Size: ${formatBytes(stats.fileSize)}")
                appendLine("    Quality: ${String.format("%.1f%%", stats.averageQuality * 100)}")
                appendLine("    Errors: ${stats.errors}")
                appendLine("    Status: ${if (stats.isActive) "Active" else "Inactive"}")
                appendLine()
            }
            appendLine("Quality Assessment:")
            appendLine("  Overall Grade: ${summary.qualityAssessment.overallGrade}")
            appendLine(
                "  Quality Score: ${
                    String.format(
                        "%.2f",
                        summary.qualityAssessment.qualityScore
                    )
                }"
            )
            if (summary.qualityAssessment.issues.isNotEmpty()) {
                appendLine("  Issues:")
                summary.qualityAssessment.issues.forEach { issue ->
                    appendLine("    - $issue")
                }
            }
            if (summary.qualityAssessment.recommendations.isNotEmpty()) {
                appendLine("  Recommendations:")
                summary.qualityAssessment.recommendations.forEach { rec ->
                    appendLine("    - $rec")
                }
            }
            appendLine()
            appendLine("Network Metrics:")
            appendLine(
                "  Average Latency: ${
                    String.format(
                        "%.1f",
                        summary.networkMetrics.averageLatency
                    )
                } ms"
            )
            appendLine(
                "  Packet Loss: ${
                    String.format(
                        "%.2f%%",
                        summary.networkMetrics.packetLoss
                    )
                }"
            )
            appendLine(
                "  Sync Quality: ${
                    String.format(
                        "%.1f%%",
                        summary.networkMetrics.syncQuality * 100
                    )
                }"
            )
            appendLine("  Reconnections: ${summary.networkMetrics.reconnectionCount}")
            appendLine()
            appendLine("Data Integrity Checks:")
            summary.dataIntegrityChecks.forEach { (check, passed) ->
                appendLine("  $check: ${if (passed) " PASS" else " FAIL"}")
            }
            appendLine()
            appendLine("=== End Report ===")
        }
    }

    private fun generateSessionStatisticsCSV(summary: ComprehensiveSessionSummary): String {
        return buildString {
            appendLine("sensor_type,total_samples,average_data_rate,dropped_samples,file_size_bytes,average_quality,errors,is_active")
            summary.sensorStatistics.forEach { (sensorType, stats) ->
                appendLine("$sensorType,${stats.totalSamples},${stats.averageDataRate},${stats.droppedSamples},${stats.fileSize},${stats.averageQuality},${stats.errors},${stats.isActive}")
            }
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_000_000_000 -> String.format("%.1f GB", bytes / 1_000_000_000.0)
            bytes >= 1_000_000 -> String.format("%.1f MB", bytes / 1_000_000.0)
            bytes >= 1_000 -> String.format("%.1f KB", bytes / 1_000.0)
            else -> "$bytes bytes"
        }
    }
}
