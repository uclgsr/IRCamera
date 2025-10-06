package mpdc4gsr.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class SessionManager(
    private val context: Context,
    private val logger: StructuredLogger,
) {
    companion object {
        private const val TAG = "SessionManager"
        private const val SESSION_HEARTBEAT_INTERVAL_MS = 10000L
        private const val SESSION_TIMEOUT_MS = 60000L
        private const val MAX_DEVICES_PER_SESSION = 10
        private const val STATE_SYNC_INTERVAL_MS = 5000L
        private const val SYNC_TO_RECORDING_DELAY_MS = 2000L
        private const val DEVICE_DISCOVERY_DELAY_MS = 2000L
        private const val DEVICE_CONNECTION_DELAY_MS = 3000L
        private const val TIME_SYNC_DELAY_MS = 1000L
        private const val RECORDING_SETUP_DELAY_MS = 1000L
    }

    private val currentSession = AtomicReference<SessionInfo?>(null)
    private val connectedDevices = ConcurrentHashMap<String, DeviceInfo>()
    private val sessionHistory = ConcurrentHashMap<String, SessionInfo>()
    private val isRunning = AtomicReference(false)
    private val sessionJob = AtomicReference<Job?>(null)
    private val sessionId = AtomicReference<String?>(null)
    private val sessionStartTime = AtomicLong(0L)
    private val _sessionWorkflowState = MutableStateFlow(SessionWorkflowState.IDLE)
    val sessionWorkflowState: StateFlow<SessionWorkflowState> = _sessionWorkflowState.asStateFlow()
    private val workflowSteps = mutableListOf<WorkflowStep>()
    private var currentStepIndex = 0
    private val sessionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    enum class SessionWorkflowState {
        IDLE,
        INITIALIZING,
        PERMISSION_CHECK,
        DEVICE_DISCOVERY,
        DEVICE_CONNECTION,
        TIME_SYNCHRONIZATION,
        RECORDING_SETUP,
        RECORDING_ACTIVE,
        STOPPING,
        CLEANUP,
        ERROR
    }

    data class WorkflowStep(
        val name: String,
        val action: suspend () -> Boolean,
        val timeout: Long = 30000L,
        val required: Boolean = true
    )

    private var onSessionStateChanged: ((SessionState) -> Unit)? = null
    private var onDeviceJoined: ((DeviceInfo) -> Unit)? = null
    private var onDeviceLeft: ((DeviceInfo) -> Unit)? = null
    private var onSyncRequired: ((List<DeviceInfo>) -> Unit)? = null
    private var onWorkflowStateChanged: ((SessionWorkflowState) -> Unit)? = null
    private var onWorkflowStepCompleted: ((String, Boolean) -> Unit)? = null

    data class SessionInfo(
        val id: String,
        val startTime: Long,
        val endTime: Long = 0L,
        val state: SessionState,
        val participants: List<DeviceInfo>,
        val metadata: Map<String, Any> = emptyMap(),
        val syncQuality: Double = 0.0,
        val recordingActive: Boolean = false,
    )

    data class DeviceInfo(
        val deviceId: String,
        val deviceType: String,
        val joinTime: Long,
        val lastSeen: Long,
        val capabilities: Set<String>,
        val syncOffset: Long,
        val connectionQuality: ConnectionQuality,
        val isRecording: Boolean = false,
    )

    enum class SessionState {
        IDLE,
        INITIALIZING,
        ACTIVE,
        SYNCING,
        RECORDING,
        PAUSED,
        ENDING,
        ENDED,
        ERROR,
    }

    enum class ConnectionQuality {
        EXCELLENT,
        GOOD,
        ACCEPTABLE,
        POOR,
        UNSTABLE,
    }

    fun start(
        onSessionStateChanged: (SessionState) -> Unit,
        onDeviceJoined: (DeviceInfo) -> Unit,
        onDeviceLeft: (DeviceInfo) -> Unit,
        onSyncRequired: (List<DeviceInfo>) -> Unit,
    ) {
        if (isRunning.get()) {
            AppLogger.w(TAG, "Session manager already running")
            return
        }
        this.onSessionStateChanged = onSessionStateChanged
        this.onDeviceJoined = onDeviceJoined
        this.onDeviceLeft = onDeviceLeft
        this.onSyncRequired = onSyncRequired
        isRunning.set(true)
        sessionJob.set(
            sessionScope.launch {
                logger.log(
                    StructuredLogger.LogLevel.INFO,
                    "SessionManager",
                    "service_started",
                    emptyMap()
                )
                try {
                    while (isRunning.get()) {
                        updateSessionState()
                        checkDeviceHeartbeats()
                        performStateSynchronization()
                        delay(SESSION_HEARTBEAT_INTERVAL_MS)
                    }
                } catch (e: CancellationException) {
                    AppLogger.i(TAG, "Session manager cancelled")
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Session manager error", e)
                    logger.log(
                        StructuredLogger.LogLevel.ERROR,
                        "SessionManager",
                        "service_error",
                        mapOf(
                            "error" to e.message.orEmpty(),
                        ),
                    )
                }
            },
        )
        AppLogger.i(TAG, "Session management service started")
    }

    fun stop() {
        if (!isRunning.get()) return
        val session = currentSession.get()
        if (session != null && session.state != SessionState.ENDED) {
            endSession(session.id, "Service stopping")
        }
        isRunning.set(false)
        sessionJob.get()?.cancel()
        sessionJob.set(null)
        // Cancel the sessionScope to cleanup all coroutines
        sessionScope.cancel()
        logger.log(StructuredLogger.LogLevel.INFO, "SessionManager", "service_stopped", emptyMap())
        AppLogger.i(TAG, "Session management service stopped")
    }

    fun createSession(metadata: Map<String, Any> = emptyMap()): String {
        val id = generateSessionId()
        val startTime = System.currentTimeMillis()
        val session =
            SessionInfo(
                id = id,
                startTime = startTime,
                state = SessionState.INITIALIZING,
                participants = emptyList(),
                metadata = metadata,
            )
        currentSession.set(session)
        sessionId.set(id)
        sessionStartTime.set(startTime)
        sessionHistory[id] = session
        updateSessionState(SessionState.ACTIVE)
        logger.log(
            StructuredLogger.LogLevel.INFO,
            "SessionManager",
            "session_created",
            mapOf(
                "session_id" to id,
                "metadata_keys" to metadata.keys.joinToString(","),
            ),
        )
        return id
    }

    fun joinDevice(
        deviceId: String,
        deviceType: String,
        capabilities: Set<String>,
    ): Boolean {
        val session = currentSession.get() ?: return false
        if (connectedDevices.size >= MAX_DEVICES_PER_SESSION) {
            AppLogger.w(TAG, "Maximum devices reached for session ${session.id}")
            return false
        }
        val deviceInfo =
            DeviceInfo(
                deviceId = deviceId,
                deviceType = deviceType,
                joinTime = System.currentTimeMillis(),
                lastSeen = System.currentTimeMillis(),
                capabilities = capabilities,
                syncOffset = 0L,
                connectionQuality = ConnectionQuality.GOOD,
            )
        connectedDevices[deviceId] = deviceInfo
        updateSessionParticipants()
        onDeviceJoined?.invoke(deviceInfo)
        logger.log(
            StructuredLogger.LogLevel.INFO,
            "SessionManager",
            "device_joined",
            mapOf(
                "session_id" to session.id,
                "device_id" to deviceId,
                "device_type" to deviceType,
                "capabilities" to capabilities.joinToString(","),
                "total_devices" to connectedDevices.size.toString(),
            ),
        )
        return true
    }

    fun removeDevice(
        deviceId: String,
        reason: String = "Unknown",
    ) {
        val deviceInfo = connectedDevices.remove(deviceId)
        if (deviceInfo != null) {
            updateSessionParticipants()
            onDeviceLeft?.invoke(deviceInfo)
            logger.log(
                StructuredLogger.LogLevel.INFO,
                "SessionManager",
                "device_left",
                mapOf(
                    "device_id" to deviceId,
                    "reason" to reason,
                    "remaining_devices" to connectedDevices.size.toString(),
                ),
            )
        }
    }

    fun startSyncRecording(): Boolean {
        val session = currentSession.get() ?: return false
        val devices = connectedDevices.values.toList()
        if (devices.isEmpty()) {
            AppLogger.w(TAG, "No devices available for synchronized recording")
            return false
        }
        val recordingCapableDevices = devices.filter { "recording" in it.capabilities }
        if (recordingCapableDevices.isEmpty()) {
            AppLogger.w(TAG, "No recording-capable devices in session")
            return false
        }
        updateSessionState(SessionState.SYNCING)
        onSyncRequired?.invoke(recordingCapableDevices)
        sessionScope.launch {
            delay(SYNC_TO_RECORDING_DELAY_MS)
            if (currentSession.get()?.state == SessionState.SYNCING) {
                updateSessionState(SessionState.RECORDING)
                recordingCapableDevices.forEach { device ->
                    connectedDevices[device.deviceId] = device.copy(isRecording = true)
                }
                logger.log(
                    StructuredLogger.LogLevel.INFO,
                    "SessionManager",
                    "sync_recording_started",
                    mapOf(
                        "session_id" to session.id,
                        "recording_devices" to recordingCapableDevices.size.toString(),
                    ),
                )
            }
        }
        return true
    }

    fun stopSyncRecording() {
        val session = currentSession.get() ?: return
        connectedDevices.keys.forEach { deviceId ->
            val device = connectedDevices[deviceId]
            if (device != null && device.isRecording) {
                connectedDevices[deviceId] = device.copy(isRecording = false)
            }
        }
        updateSessionState(SessionState.ACTIVE)
        logger.log(
            StructuredLogger.LogLevel.INFO,
            "SessionManager",
            "sync_recording_stopped",
            mapOf(
                "session_id" to session.id,
            ),
        )
    }

    fun endSession(
        sessionId: String,
        reason: String = "User requested",
    ) {
        val session = currentSession.get()
        if (session == null || session.id != sessionId) return
        updateSessionState(SessionState.ENDING)
        if (session.state == SessionState.RECORDING) {
            stopSyncRecording()
        }
        connectedDevices.clear()
        val endedSession =
            session.copy(
                endTime = System.currentTimeMillis(),
                state = SessionState.ENDED,
                participants = emptyList(),
            )
        sessionHistory[session.id] = endedSession
        currentSession.set(null)
        this.sessionId.set(null)
        sessionStartTime.set(0L)
        updateSessionState(SessionState.IDLE)
        logger.log(
            StructuredLogger.LogLevel.INFO,
            "SessionManager",
            "session_ended",
            mapOf(
                "session_id" to session.id,
                "reason" to reason,
                "duration_ms" to (endedSession.endTime - endedSession.startTime).toString(),
            ),
        )
    }

    fun updateDeviceHeartbeat(
        deviceId: String,
        syncOffset: Long,
        quality: ConnectionQuality,
    ) {
        val device = connectedDevices[deviceId]
        if (device != null) {
            connectedDevices[deviceId] =
                device.copy(
                    lastSeen = System.currentTimeMillis(),
                    syncOffset = syncOffset,
                    connectionQuality = quality,
                )
        }
    }

    fun getCurrentSession(): SessionInfo? = currentSession.get()
    fun getConnectedDevices(): List<DeviceInfo> = connectedDevices.values.toList()
    fun getSessionHistory(): List<SessionInfo> = sessionHistory.values.toList()
    fun getDiagnostics(): JSONObject {
        val session = currentSession.get()
        return JSONObject().apply {
            put("is_running", isRunning.get())
            put("current_session_id", session?.id ?: "none")
            put("session_state", session?.state?.name ?: "IDLE")
            put("connected_devices", connectedDevices.size)
            put(
                "session_duration_ms",
                if (session != null) System.currentTimeMillis() - session.startTime else 0
            )
            put("total_sessions", sessionHistory.size)
            put("recording_active", session?.recordingActive ?: false)
        }
    }

    private fun generateSessionId(): String {
        return "session_${UUID.randomUUID().toString().replace("-", "")}"
    }

    private fun updateSessionState(newState: SessionState? = null) {
        val session = currentSession.get() ?: return
        val updatedState = newState ?: determineSessionState(session)
        if (session.state != updatedState) {
            val updatedSession = session.copy(state = updatedState)
            currentSession.set(updatedSession)
            sessionHistory[session.id] = updatedSession
            onSessionStateChanged?.invoke(updatedState)
            logger.log(
                StructuredLogger.LogLevel.DEBUG,
                "SessionManager",
                "state_changed",
                mapOf(
                    "session_id" to session.id,
                    "old_state" to session.state.name,
                    "new_state" to updatedState.name,
                ),
            )
        }
    }

    private fun determineSessionState(session: SessionInfo): SessionState {
        return when {
            connectedDevices.isEmpty() -> SessionState.IDLE
            connectedDevices.values.any { it.isRecording } -> SessionState.RECORDING
            else -> SessionState.ACTIVE
        }
    }

    private fun updateSessionParticipants() {
        val session = currentSession.get() ?: return
        val participants = connectedDevices.values.toList()
        val updatedSession = session.copy(participants = participants)
        currentSession.set(updatedSession)
        sessionHistory[session.id] = updatedSession
    }

    private fun checkDeviceHeartbeats() {
        val currentTime = System.currentTimeMillis()
        val staleDevices = mutableListOf<String>()
        connectedDevices.forEach { (deviceId, device) ->
            if (currentTime - device.lastSeen > SESSION_TIMEOUT_MS) {
                staleDevices.add(deviceId)
            }
        }
        staleDevices.forEach { deviceId ->
            removeDevice(deviceId, "Heartbeat timeout")
        }
    }

    private fun performStateSynchronization() {
        val session = currentSession.get() ?: return
        val devices = connectedDevices.values.toList()
        if (devices.isEmpty()) return
        val needsSync =
            devices.any { device ->
                device.connectionQuality == ConnectionQuality.POOR ||
                        device.connectionQuality == ConnectionQuality.UNSTABLE ||
                        kotlin.math.abs(device.syncOffset) > 5_000_000L
            }
        if (needsSync && session.state == SessionState.ACTIVE) {
            logger.log(
                StructuredLogger.LogLevel.DEBUG,
                "SessionManager",
                "sync_required",
                mapOf(
                    "session_id" to session.id,
                    "device_count" to devices.size.toString(),
                ),
            )
            onSyncRequired?.invoke(devices)
        }
    }

    suspend fun initializeSessionWithWorkflow(
        sessionConfig: SessionConfig,
        permissionController: mpdc4gsr.core.ui.PermissionController? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            logger.log(
                StructuredLogger.LogLevel.INFO,
                "SessionManager",
                "workflow_init_start",
                mapOf("session_id" to sessionConfig.sessionId)
            )
            _sessionWorkflowState.value = SessionWorkflowState.INITIALIZING
            setupWorkflowSteps(sessionConfig, permissionController)
            executeWorkflow()
        } catch (e: Exception) {
            logger.log(
                StructuredLogger.LogLevel.ERROR,
                "SessionManager",
                "workflow_init_failed",
                mapOf("error" to (e.message ?: "Unknown error"))
            )
            _sessionWorkflowState.value = SessionWorkflowState.ERROR
            false
        }
    }

    private suspend fun setupWorkflowSteps(
        config: SessionConfig,
        permissionController: mpdc4gsr.core.ui.PermissionController?
    ) {
        workflowSteps.clear()
        currentStepIndex = 0
        workflowSteps.add(
            WorkflowStep(
                name = "Permission Check",
                action = {
                    _sessionWorkflowState.value = SessionWorkflowState.PERMISSION_CHECK
                    permissionController?.hasAllRequiredPermissions() ?: true
                },
                timeout = 15000L
            )
        )
        workflowSteps.add(
            WorkflowStep(
                name = "Device Discovery",
                action = {
                    _sessionWorkflowState.value = SessionWorkflowState.DEVICE_DISCOVERY
                    discoverDevices(config.expectedDevices)
                },
                timeout = 20000L
            )
        )
        workflowSteps.add(
            WorkflowStep(
                name = "Device Connection",
                action = {
                    _sessionWorkflowState.value = SessionWorkflowState.DEVICE_CONNECTION
                    connectToDevices()
                },
                timeout = 30000L
            )
        )
        workflowSteps.add(
            WorkflowStep(
                name = "Time Synchronization",
                action = {
                    _sessionWorkflowState.value = SessionWorkflowState.TIME_SYNCHRONIZATION
                    performTimeSynchronization()
                },
                timeout = 15000L
            )
        )
        workflowSteps.add(
            WorkflowStep(
                name = "Recording Setup",
                action = {
                    _sessionWorkflowState.value = SessionWorkflowState.RECORDING_SETUP
                    setupRecording(config)
                },
                timeout = 10000L
            )
        )
        logger.log(
            StructuredLogger.LogLevel.INFO,
            "SessionManager",
            "workflow_configured",
            mapOf("steps" to workflowSteps.size.toString())
        )
    }

    private suspend fun executeWorkflow(): Boolean {
        for ((index, step) in workflowSteps.withIndex()) {
            currentStepIndex = index
            logger.log(
                StructuredLogger.LogLevel.INFO,
                "SessionManager",
                "workflow_step_start",
                mapOf(
                    "step" to step.name,
                    "index" to "${index + 1}/${workflowSteps.size}"
                )
            )
            try {
                val success = withContext(Dispatchers.IO) {
                    kotlinx.coroutines.withTimeout(step.timeout) {
                        step.action()
                    }
                }
                onWorkflowStepCompleted?.invoke(step.name, success)
                if (!success && step.required) {
                    logger.log(
                        StructuredLogger.LogLevel.ERROR,
                        "SessionManager",
                        "workflow_step_failed",
                        mapOf("step" to step.name, "required" to "true")
                    )
                    _sessionWorkflowState.value = SessionWorkflowState.ERROR
                    return false
                } else if (!success) {
                    logger.log(
                        StructuredLogger.LogLevel.WARNING,
                        "SessionManager",
                        "workflow_step_failed",
                        mapOf("step" to step.name, "required" to "false")
                    )
                }
                logger.log(
                    StructuredLogger.LogLevel.INFO,
                    "SessionManager",
                    "workflow_step_success",
                    mapOf("step" to step.name)
                )
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                logger.log(
                    StructuredLogger.LogLevel.ERROR,
                    "SessionManager",
                    "workflow_step_timeout",
                    mapOf("step" to step.name, "timeout" to step.timeout.toString())
                )
                onWorkflowStepCompleted?.invoke(step.name, false)
                if (step.required) {
                    _sessionWorkflowState.value = SessionWorkflowState.ERROR
                    return false
                }
            } catch (e: Exception) {
                logger.log(
                    StructuredLogger.LogLevel.ERROR,
                    "SessionManager",
                    "workflow_step_exception",
                    mapOf("step" to step.name, "error" to (e.message ?: "Unknown"))
                )
                onWorkflowStepCompleted?.invoke(step.name, false)
                if (step.required) {
                    _sessionWorkflowState.value = SessionWorkflowState.ERROR
                    return false
                }
            }
        }
        _sessionWorkflowState.value = SessionWorkflowState.RECORDING_ACTIVE
        logger.log(
            StructuredLogger.LogLevel.INFO,
            "SessionManager",
            "workflow_completed",
            emptyMap()
        )
        return true
    }

    private suspend fun discoverDevices(expectedDevices: List<String>): Boolean {
        delay(DEVICE_DISCOVERY_DELAY_MS)
        logger.log(
            StructuredLogger.LogLevel.INFO,
            "SessionManager",
            "device_discovery",
            mapOf("expected" to expectedDevices.joinToString(","))
        )
        return true
    }

    private suspend fun connectToDevices(): Boolean {
        delay(DEVICE_CONNECTION_DELAY_MS)
        logger.log(
            StructuredLogger.LogLevel.INFO,
            "SessionManager",
            "device_connection",
            emptyMap()
        )
        return true
    }

    private suspend fun performTimeSynchronization(): Boolean {
        delay(TIME_SYNC_DELAY_MS)
        logger.log(
            StructuredLogger.LogLevel.INFO,
            "SessionManager",
            "time_sync",
            emptyMap()
        )
        return true
    }

    private suspend fun setupRecording(config: SessionConfig): Boolean {
        delay(RECORDING_SETUP_DELAY_MS)
        logger.log(
            StructuredLogger.LogLevel.INFO,
            "SessionManager",
            "recording_setup",
            mapOf("session_id" to config.sessionId)
        )
        return true
    }

    fun setWorkflowStateChangeCallback(callback: (SessionWorkflowState) -> Unit) {
        onWorkflowStateChanged = callback
    }

    fun setWorkflowStepCallback(callback: (String, Boolean) -> Unit) {
        onWorkflowStepCompleted = callback
    }

    data class SessionConfig(
        val sessionId: String,
        val expectedDevices: List<String> = listOf("RGB", "Thermal", "Shimmer"),
        val recordingDuration: Long? = null,
        val participantId: String? = null,
        val studyName: String? = null
    )
}
