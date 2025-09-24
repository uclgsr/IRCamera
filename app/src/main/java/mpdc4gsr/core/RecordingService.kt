package mpdc4gsr.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.mpdc4gsr.gsr.model.SessionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mpdc4gsr.config.FeatureFlags
import mpdc4gsr.config.ProtocolVersion
import mpdc4gsr.controller.ComprehensiveRecordingController
import mpdc4gsr.controller.RecordingState
import mpdc4gsr.core.StructuredLogger
import mpdc4gsr.network.NetworkClient
import mpdc4gsr.network.NetworkConnectionManager
import mpdc4gsr.network.NetworkServer
import mpdc4gsr.network.PreviewDataAdapter
import mpdc4gsr.network.PreviewStreamer
import mpdc4gsr.network.ProtocolHandler
import mpdc4gsr.core.CrashSafeSupervisor
import org.json.JSONArray
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.File
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class RecordingService : LifecycleService() {

    companion object {
        private const val TAG = "RecordingService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "recording_service_channel"

        private const val SERVER_PORT = 8080
        private const val SERVICE_TYPE = "_ircamera._tcp."
        private const val SERVICE_NAME = "IRCamera-Android"

        const val ACTION_START_RECORDING = "${com.csl.irCamera.BuildConfig.APPLICATION_ID}.START_RECORDING"
        const val ACTION_STOP_RECORDING = "${com.csl.irCamera.BuildConfig.APPLICATION_ID}.STOP_RECORDING"
        const val ACTION_ADD_SYNC_MARKER = "${com.csl.irCamera.BuildConfig.APPLICATION_ID}.ADD_SYNC_MARKER"
        const val ACTION_START_SERVER = "${com.csl.irCamera.BuildConfig.APPLICATION_ID}.START_SERVER"
        const val ACTION_STOP_SERVER = "${com.csl.irCamera.BuildConfig.APPLICATION_ID}.STOP_SERVER"
        const val ACTION_CONNECT_PC = "${com.csl.irCamera.BuildConfig.APPLICATION_ID}.CONNECT_PC"
        const val ACTION_DISCONNECT_PC = "${com.csl.irCamera.BuildConfig.APPLICATION_ID}.DISCONNECT_PC"
        const val ACTION_START_DISCOVERY = "${com.csl.irCamera.BuildConfig.APPLICATION_ID}.START_DISCOVERY"

        const val EXTRA_SESSION_DIRECTORY = "session_directory"
        const val EXTRA_MARKER_TYPE = "marker_type"
        const val EXTRA_TIMESTAMP_NS = "timestamp_ns"
        const val EXTRA_PC_IP = "pc_ip"
        const val EXTRA_PC_PORT = "pc_port"

        fun startRecording(context: Context, sessionDirectory: String) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_START_RECORDING
                putExtra(EXTRA_SESSION_DIRECTORY, sessionDirectory)
            }
            context.startForegroundService(intent)
        }

        fun stopRecording(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_STOP_RECORDING
            }
            context.startService(intent)
        }

        fun startServer(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_START_SERVER
            }
            context.startForegroundService(intent)
        }

        fun stopServer(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_STOP_SERVER
            }
            context.startService(intent)
        }

        fun addSyncMarker(context: Context, markerType: String, timestampNs: Long) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_ADD_SYNC_MARKER
                putExtra(EXTRA_MARKER_TYPE, markerType)
                putExtra(EXTRA_TIMESTAMP_NS, timestampNs)
            }
            context.startService(intent)
        }

        fun connectToPC(context: Context, ipAddress: String, port: Int = 8080) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_CONNECT_PC
                putExtra(EXTRA_PC_IP, ipAddress)
                putExtra(EXTRA_PC_PORT, port)
            }
            context.startService(intent)
        }

        fun disconnectFromPC(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_DISCONNECT_PC
            }
            context.startService(intent)
        }

        fun startDiscovery(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_START_DISCOVERY
            }
            context.startService(intent)
        }
    }

    private val binder = RecordingServiceBinder()

    private lateinit var recordingController: ComprehensiveRecordingController
    private var isInitialized = false

    private lateinit var networkClient: NetworkClient
    private lateinit var networkServer: NetworkServer
    private lateinit var protocolHandler: ProtocolHandler
    private lateinit var connectionManager: NetworkConnectionManager
    private lateinit var previewStreamer: PreviewStreamer
    private lateinit var previewDataAdapter: PreviewDataAdapter
    private var isNetworkInitialized = false
    private var isConnectedToPC = false

    private var currentSessionDirectory: String? = null
    private var recordingStartTime: Long = 0

    private lateinit var notificationManager: NotificationManager

    private var serverSocket: ServerSocket? = null
    private var isServerRunning = AtomicBoolean(false)
    private var serverJob: Job? = null
    private val activeConnections = ConcurrentHashMap<String, ClientConnection>()

    private var nsdManager: NsdManager? = null
    private var nsdServiceInfo: NsdServiceInfo? = null
    private var isServiceRegistered = false

    private lateinit var structuredLogger: StructuredLogger
    private lateinit var crashSafeSupervisor: CrashSafeSupervisor

    private data class ClientConnection(
        val socket: Socket,
        val clientId: String,
        val inputStream: DataInputStream,
        val outputStream: DataOutputStream,
        val job: Job
    )

    inner class RecordingServiceBinder : Binder() {
        fun getService(): RecordingService = this@RecordingService
        fun getRecordingController(): ComprehensiveRecordingController = recordingController
        fun getNetworkServer(): NetworkServer = networkServer
        fun getPreviewStreamer(): PreviewStreamer = previewStreamer
        fun getPreviewDataAdapter(): PreviewDataAdapter = previewDataAdapter
        fun isConnectedToPC(): Boolean = this@RecordingService.isConnectedToPC
        fun getServerStatus(): String {
            return if (isServerRunning.get()) {
                "Running on port $SERVER_PORT (${activeConnections.size} clients)"
            } else {
                "Stopped"
            }
        }

        fun getConnectedClients(): List<String> {
            return activeConnections.keys.toList()
        }

        fun getNetworkClient(): NetworkClient? = if (isNetworkInitialized) networkClient else null
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "RecordingService created")

        initializePhase0Baseline()

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager

        // Initialize ComprehensiveRecordingController without PermissionManager for service
        recordingController = ComprehensiveRecordingController(this, this)

        networkClient = NetworkClient(this)
        networkServer = NetworkServer(this, 8080)
        protocolHandler = ProtocolHandler(this, networkServer)
        connectionManager = NetworkConnectionManager(this, networkServer, protocolHandler)
        previewStreamer = PreviewStreamer(networkServer)
        previewDataAdapter = PreviewDataAdapter(previewStreamer, this)

        crashSafeSupervisor.registerJob(
            id = "recording_service_init",
            name = "RecordingService Initialization",
            critical = true,
            restartable = false
        ) {
            lifecycleScope.launch {
                try {
                    Log.i(TAG, "Initializing RecordingService with enhanced fault tolerance")

                    // Check for crashed sessions on startup
                    recordingController.checkForCrashedSessions()

                    val sensorsSuccess = recordingController.initializeSensors()
                    val networkSuccess = initializeNetworkClient()


                    isInitialized = sensorsSuccess
                    isNetworkInitialized = networkSuccess

                    if (sensorsSuccess) {
                        Log.i(
                            TAG,
                            "Recording service initialized successfully with ${recordingController.getAvailableSensors().size} sensors"
                        )
                        structuredLogger.log(
                            StructuredLogger.LogLevel.INFO,
                            "RecordingService",
                            "service_initialized",
                            mapOf(
                                "available_sensors" to recordingController.getAvailableSensors()
                                    .map { it.sensorId },
                                "sensor_count" to recordingController.getAvailableSensors().size
                            )
                        )
                        setupStatusMonitoring()
                        setupNetworkServer()

                        if (FeatureFlags.MDNS_ENABLE) {
                            startServerSocket()
                        }

                        if (networkSuccess) {
                            Log.i(TAG, "Network client initialized successfully")
                            startNetworkDiscovery()
                        } else {
                            Log.w(
                                TAG,
                                "Network client initialization failed - running in server-only mode"
                            )
                        }
                    } else {
                        Log.e(TAG, "No sensors could be initialized - service cannot operate")
                        structuredLogger.log(
                            StructuredLogger.LogLevel.ERROR,
                            "RecordingService",
                            "initialization_failed",
                            mapOf("reason" to "no_sensors_available")
                        )
                        stopSelf()
                    }
                } catch (e: Exception) {
                    structuredLogger.log(
                        StructuredLogger.LogLevel.ERROR,
                        "RecordingService",
                        "initialization_exception",
                        mapOf("error" to (e.message ?: "Unknown error"))
                    )
                    stopSelf()
                    throw e
                }
            }
        }
    }

    private fun initializePhase0Baseline() {
        try {
            FeatureFlags.initialize(this)
            structuredLogger = StructuredLogger.getInstance(this)
            crashSafeSupervisor = CrashSafeSupervisor.getInstance(this)
            crashSafeSupervisor.initialize()

            structuredLogger.log(
                StructuredLogger.LogLevel.INFO,
                "RecordingService",
                "phase0_baseline_initialized",
                mapOf(
                    "feature_flags" to FeatureFlags.getAllFlags(),
                    "protocol_version" to ProtocolVersion.CURRENT_VERSION
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Phase 0 baseline in service", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START_RECORDING -> {
                val sessionDirectory = intent.getStringExtra(EXTRA_SESSION_DIRECTORY)
                if (sessionDirectory != null) {
                    startRecordingSession(sessionDirectory)
                } else {
                    Log.e(TAG, "No session directory provided for recording")
                }
            }

            ACTION_STOP_RECORDING -> stopRecordingSession()
            ACTION_START_SERVER -> startServerSocket()
            ACTION_STOP_SERVER -> stopServerSocket()
            ACTION_ADD_SYNC_MARKER -> {
                val markerType = intent.getStringExtra(EXTRA_MARKER_TYPE)
                val timestampNs = intent.getLongExtra(EXTRA_TIMESTAMP_NS, System.nanoTime())
                if (markerType != null) {
                    addSyncMarker(markerType, timestampNs)
                }
            }

            ACTION_CONNECT_PC -> {
                val ipAddress = intent.getStringExtra(EXTRA_PC_IP)
                val port = intent.getIntExtra(EXTRA_PC_PORT, 8080)
                if (ipAddress != null) {
                    connectToPC(ipAddress, port)
                }
            }

            ACTION_DISCONNECT_PC -> disconnectFromPC()
            ACTION_START_DISCOVERY -> startPCDiscovery()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        structuredLogger.log(
            StructuredLogger.LogLevel.INFO,
            "RecordingService",
            "service_destroying"
        )
        try {
            if (recordingController.isRecording) {
                stopRecordingSession()
            }
            stopServerSocket()
            if (isNetworkInitialized) {
                networkClient.disconnect()
            }

            if (::previewStreamer.isInitialized) {
                previewStreamer.cleanup()
            }

            if (::previewDataAdapter.isInitialized) {
                previewDataAdapter.cleanup()
            }

            if (::connectionManager.isInitialized) {
                connectionManager.cleanup()
            }

            lifecycleScope.launch {
                recordingController.cleanup()
            }
            crashSafeSupervisor.shutdown()
        } catch (e: Exception) {
            structuredLogger.log(
                StructuredLogger.LogLevel.ERROR,
                "RecordingService",
                "service_cleanup_error",
                mapOf("error" to (e.message ?: "Unknown error"))
            )
        }
        Log.i(TAG, "RecordingService destroyed")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recording Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Multi-modal sensor recording service"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startRecordingSession(sessionDirectory: String) {
        if (!isInitialized) {
            Log.e(TAG, "Service not initialized, cannot start recording")
            structuredLogger.log(
                StructuredLogger.LogLevel.ERROR,
                "RecordingService",
                "recording_start_failed",
                mapOf("reason" to "service_not_initialized")
            )
            return
        }

        lifecycleScope.launch {
            try {
                val sessionDir = File(sessionDirectory)
                if (!sessionDir.exists()) {
                    sessionDir.mkdirs()
                }

                currentSessionDirectory = sessionDirectory
                recordingStartTime = System.nanoTime()

                Log.i(TAG, "Starting recording session: $sessionDirectory")
                structuredLogger.log(
                    StructuredLogger.LogLevel.INFO,
                    "RecordingService",
                    "recording_session_start",
                    mapOf(
                        "session_directory" to sessionDirectory,
                        "available_sensors" to recordingController.getAvailableSensors()
                            .map { it.sensorId }
                    )
                )

                startForeground(
                    NOTIFICATION_ID,
                    createRecordingNotification("Starting recording session...")
                )


                val success = recordingController.startSession(sessionDirectory)

                if (success) {
                    val activeSensors = recordingController.getActiveSensorCount()
                    val totalSensors = recordingController.getAvailableSensors().size
                    Log.i(
                        TAG,
                        "Recording session started successfully with $activeSensors/$totalSensors sensors"
                    )
                    updateNotification("Recording: $activeSensors sensors active")

                    structuredLogger.log(
                        StructuredLogger.LogLevel.INFO,
                        "RecordingService",
                        "recording_session_started",
                        mapOf(
                            "active_sensors" to activeSensors,
                            "total_sensors" to totalSensors,
                            "session_directory" to sessionDirectory
                        )
                    )
                } else {
                    Log.e(TAG, "Failed to start recording session")
                    updateNotification("Recording failed to start")
                    structuredLogger.log(
                        StructuredLogger.LogLevel.ERROR,
                        "RecordingService",
                        "recording_session_start_failed"
                    )
                    stopRecordingSession()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting recording session", e)
                updateNotification("Recording error occurred")
                structuredLogger.log(
                    StructuredLogger.LogLevel.ERROR,
                    "RecordingService",
                    "recording_session_start_exception",
                    mapOf("error" to (e.message ?: "Unknown error"))
                )
                stopRecordingSession()
            }
        }
    }

    private fun stopRecordingSession() {
        lifecycleScope.launch {
            try {
                updateNotification("Stopping recording session...")
                Log.i(TAG, "Stopping recording session")


                val success = recordingController.stopSession()

                if (success) {
                    val sessionDuration = if (recordingStartTime > 0) {
                        (System.nanoTime() - recordingStartTime) / 1_000_000_000.0
                    } else 0.0

                    Log.i(
                        TAG,
                        "Recording session stopped successfully (duration: ${
                            String.format(
                                "%.1f",
                                sessionDuration
                            )
                        }s)"
                    )
                    updateNotification(
                        "Recording completed (${String.format("%.1f", sessionDuration)}s)"
                    )

                    structuredLogger.log(
                        StructuredLogger.LogLevel.INFO,
                        "RecordingService",
                        "recording_session_stopped",
                        mapOf(
                            "session_duration_seconds" to sessionDuration,
                            "session_directory" to (currentSessionDirectory ?: "unknown")
                        )
                    )

                    delay(2000)
                    stopForeground(true)
                } else {
                    Log.e(TAG, "Failed to stop recording session cleanly")
                    updateNotification("Recording stop failed")
                    structuredLogger.log(
                        StructuredLogger.LogLevel.ERROR,
                        "RecordingService",
                        "recording_session_stop_failed"
                    )
                }

                currentSessionDirectory = null
                recordingStartTime = 0


                if (!isServerRunning.get()) {
                    stopSelf()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recording session", e)
                updateNotification("Recording stop error")
                structuredLogger.log(
                    StructuredLogger.LogLevel.ERROR,
                    "RecordingService",
                    "recording_session_stop_exception",
                    mapOf("error" to (e.message ?: "Unknown error"))
                )
            }
        }
    }

    private fun addSyncMarker(markerType: String, timestampNs: Long) {
        lifecycleScope.launch {
            try {
                recordingController.addSyncMarker(markerType, timestampNs)
                Log.i(TAG, "Sync marker added: $markerType")

                if (recordingController.isRecording) {
                    val originalText = "Recording in progress"
                    updateNotification("Sync marker: $markerType")
                    delay(1000)
                    updateNotification(originalText)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding sync marker", e)
            }
        }
    }

    private fun setupStatusMonitoring() {
        recordingController.recordingStateFlow
            .onEach { state ->
                when (state) {
                    RecordingState.IDLE -> updateNotification("Ready")
                    RecordingState.STARTING -> updateNotification("Starting sensors...")
                    RecordingState.RECORDING -> updateNotification("Recording in progress")
                    RecordingState.STOPPING -> updateNotification("Stopping sensors...")
                    RecordingState.STOPPED -> updateNotification("Recording stopped")
                    RecordingState.ERROR -> updateNotification("Recording error")
                }
            }
            .launchIn(lifecycleScope)

        recordingController.sensorStatusFlow
            .onEach { statusList ->
                val activeSensors = statusList.count { it.isRecording }
                val totalSamples = statusList.sumOf { it.samplesRecorded }
                val totalStorage = statusList.sumOf { it.storageUsedMB }

                if (activeSensors > 0) {
                    val statusText = "Recording: $activeSensors sensors, " +
                            "${totalSamples} samples, " +
                            "${String.format("%.1f", totalStorage)}MB"
                    updateNotification(statusText)
                }
            }
            .launchIn(lifecycleScope)

        recordingController.errorFlow
            .onEach { error ->
                Log.w(TAG, "Recording controller error: ${error.message}")
                if (!error.isRecoverable) {
                    updateNotification("Critical error: ${error.message}")
                    stopRecordingSession()
                } else {
                    updateNotification("Warning: ${error.message}")
                    delay(3000)
                    if (recordingController.isRecording) {
                        updateNotification("Recording in progress")
                    }
                }
            }
            .launchIn(lifecycleScope)
    }

    private fun createRecordingNotification(contentText: String): Notification {
        val stopIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_STOP_RECORDING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("IRCamera Recording")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop",
                stopPendingIntent
            )
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun updateNotification(contentText: String) {
        try {

            if (isServiceForeground()) {
                val notification = when {
                    recordingController.isRecording -> createRecordingNotification(contentText)
                    isServerRunning.get() -> createServerNotification(contentText)
                    else -> null
                }
                notification?.let { notificationManager.notify(NOTIFICATION_ID, it) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update notification", e)
        }
    }

    fun getRecordingController(): ComprehensiveRecordingController = recordingController

    fun isInitialized(): Boolean = isInitialized

    fun getCurrentSession(): SessionInfo? {
        return currentSessionDirectory?.let { directory ->
            SessionInfo(
                sessionId = directory.substringAfterLast("/"),
                startTime = recordingStartTime
            )
        }
    }

    private fun startServerSocket() {
        if (isServerRunning.get()) {
            structuredLogger.logServerEvent(
                "server_already_running",
                mapOf("port" to SERVER_PORT)
            )
            return
        }

        crashSafeSupervisor.registerJob(
            id = "server_socket",
            name = "Server Socket",
            critical = true,
            restartable = true,
            healthCheck = object : CrashSafeSupervisor.HealthCheck {
                override suspend fun checkHealth(): CrashSafeSupervisor.HealthStatus {
                    return if (isServerRunning.get() && serverSocket?.isClosed == false) {
                        CrashSafeSupervisor.HealthStatus(
                            isHealthy = true,
                            message = "Server socket running normally",
                            details = mapOf(
                                "port" to SERVER_PORT,
                                "active_connections" to activeConnections.size,
                                "nsd_registered" to isServiceRegistered
                            )
                        )
                    } else {
                        CrashSafeSupervisor.HealthStatus(
                            isHealthy = false,
                            message = "Server socket not running or closed"
                        )
                    }
                }
            }
        ) { stopToken ->
            runServerSocketSupervised(stopToken)
        }
    }

    private suspend fun runServerSocketSupervised(stopToken: CrashSafeSupervisor.StopToken) {
        try {
            serverSocket = ServerSocket(SERVER_PORT)
            isServerRunning.set(true)
            structuredLogger.logServerEvent(
                "server_socket_started",
                mapOf("port" to SERVER_PORT)
            )
            if (FeatureFlags.MDNS_ENABLE) {
                registerNsdService()
            }
            if (!isServiceForeground()) {
                startForeground(
                    NOTIFICATION_ID,
                    createServerNotification("Server listening for PC connections")
                )
            }
            while (!stopToken.isStopRequested() && isServerRunning.get()) {
                try {
                    val clientSocket = withContext(Dispatchers.IO) {
                        serverSocket?.accept()
                    }
                    if (clientSocket != null && isServerRunning.get()) {
                        val clientId =
                            "${clientSocket.inetAddress.hostAddress}:${clientSocket.port}"
                        structuredLogger.logConnection(
                            "pc_client_connected",
                            clientId,
                            mapOf("client_address" to clientSocket.inetAddress.hostAddress)
                        )
                        handleNewClientConnection(clientSocket, clientId)
                        withContext(Dispatchers.Main) {
                            updateNotification("Connected PCs: ${activeConnections.size}")
                        }
                    }
                } catch (e: SocketException) {
                    if (isServerRunning.get() && !stopToken.isStopRequested()) {
                        structuredLogger.logServerEvent(
                            "accept_socket_error",
                            mapOf("error" to (e.message ?: "Unknown error"))
                        )
                        delay(1000)
                    }
                } catch (e: Exception) {
                    structuredLogger.logServerEvent(
                        "accept_unexpected_error",
                        mapOf("error" to (e.message ?: "Unknown error"))
                    )
                    if (isServerRunning.get() && !stopToken.isStopRequested()) {
                        delay(5000)
                    }
                }
            }
        } catch (e: Exception) {
            structuredLogger.logServerEvent(
                "server_socket_failed",
                mapOf("error" to (e.message ?: "Unknown error"))
            )
            isServerRunning.set(false)
            throw e
        } finally {
            structuredLogger.logServerEvent("server_socket_cleanup_started")
            cleanupServerSocket()
        }
    }

    private fun cleanupServerSocket() {
        isServerRunning.set(false)
        serverJob?.cancel()
        serverJob = null

        activeConnections.values.forEach { connection ->
            try {
                connection.job.cancel()
                connection.socket.close()
            } catch (e: Exception) {
                structuredLogger.logConnection(
                    "connection_cleanup_error",
                    connection.clientId,
                    mapOf("error" to (e.message ?: "Unknown error"))
                )
            }
        }
        activeConnections.clear()

        try {
            serverSocket?.close()
        } catch (e: Exception) {
            structuredLogger.logServerEvent(
                "server_socket_close_error",
                mapOf("error" to (e.message ?: "Unknown error"))
            )
        } finally {
            serverSocket = null
        }
        unregisterNsdService()
        structuredLogger.logServerEvent("server_socket_cleanup_completed")
    }

    private fun stopServerSocket() {
        if (!isServerRunning.get()) {
            structuredLogger.logServerEvent("server_not_running")
            return
        }
        structuredLogger.logServerEvent("server_socket_stop_requested")
        crashSafeSupervisor.unregisterJob("server_socket")
        cleanupServerSocket()
        structuredLogger.logServerEvent("server_socket_stopped")
    }

    private fun startAcceptLoop() {
        serverJob = lifecycleScope.launch(Dispatchers.IO) {
            while (isServerRunning.get() && isActive) {
                try {
                    val clientSocket = serverSocket?.accept()
                    if (clientSocket != null && isServerRunning.get()) {
                        val clientId =
                            "${clientSocket.inetAddress.hostAddress}:${clientSocket.port}"
                        Log.i(TAG, "PC client connected: $clientId")
                        handleNewClientConnection(clientSocket, clientId)
                        withContext(Dispatchers.Main) {
                            updateNotification("Connected PCs: ${activeConnections.size}")
                        }
                    }
                } catch (e: SocketException) {
                    if (isServerRunning.get()) {
                        Log.w(TAG, "Server socket accept error", e)
                        delay(1000)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error in accept loop", e)
                    if (isServerRunning.get()) {
                        delay(5000)
                    }
                }
            }
            Log.i(TAG, "Accept loop terminated")
        }
    }

    private suspend fun handleNewClientConnection(clientSocket: Socket, clientId: String) {
        try {
            clientSocket.soTimeout = 30000
            val inputStream = DataInputStream(clientSocket.getInputStream())
            val outputStream = DataOutputStream(clientSocket.getOutputStream())

            val clientJob = lifecycleScope.launch(Dispatchers.IO) {
                try {
                    handleClientMessages(clientId, inputStream, outputStream)
                } catch (e: Exception) {
                    Log.w(TAG, "Client $clientId handler error", e)
                } finally {
                    activeConnections.remove(clientId)
                    try {
                        clientSocket.close()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error closing client socket", e)
                    }
                    Log.i(TAG, "PC client disconnected: $clientId")
                    withContext(Dispatchers.Main) {
                        updateNotification("Connected PCs: ${activeConnections.size}")
                    }
                }
            }
            val connection = ClientConnection(
                socket = clientSocket,
                clientId = clientId,
                inputStream = inputStream,
                outputStream = outputStream,
                job = clientJob
            )
            activeConnections[clientId] = connection
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up client connection", e)
            try {
                clientSocket.close()
            } catch (closeEx: Exception) {
                Log.w(TAG, "Error closing failed client socket", closeEx)
            }
        }
    }

    private suspend fun handleClientMessages(
        clientId: String,
        inputStream: DataInputStream,
        outputStream: DataOutputStream
    ) {
        while (isServerRunning.get() && currentCoroutineContext().isActive) {
            try {
                val messageLength = inputStream.readInt()
                if (messageLength > 1024 * 1024) {
                    Log.w(TAG, "Message too large from $clientId: $messageLength bytes")
                    break
                }
                val messageData = ByteArray(messageLength)
                inputStream.readFully(messageData)
                val message = JSONObject(String(messageData, Charsets.UTF_8))
                processClientMessage(clientId, message, outputStream)
            } catch (e: SocketTimeoutException) {
                sendKeepAlive(outputStream)
            } catch (e: EOFException) {
                Log.i(TAG, "Client $clientId disconnected normally")
                break
            } catch (e: Exception) {
                Log.w(TAG, "Error handling message from $clientId", e)
                break
            }
        }
    }

    private suspend fun initializeNetworkClient(): Boolean {
        return try {
            val success = networkClient.initialize()
            if (success) {
                setupNetworkCommandHandlers()
                Log.i(TAG, "Network client initialized successfully")
            } else {
                Log.w(TAG, "Network client initialization failed")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing network client", e)
            false
        }
    }

    private fun setupNetworkCommandHandlers() {
        try {
            networkClient.setMessageHandler("start_recording") { message ->
                handleStartRecordingCommand(message)
            }
            networkClient.setMessageHandler("stop_recording") { message ->
                handleStopRecordingCommand(message)
            }
            networkClient.setMessageHandler("sync_flash") { message ->
                handleSyncFlashCommand(message)
            }
            networkClient.setMessageHandler("query_capabilities") { message ->
                handleQueryCapabilitiesCommand(message)
            }
            networkClient.setMessageHandler("query_status") { message ->
                handleQueryStatusCommand(message)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network client message handlers setup failed: ${e.message}")
        }
    }

    fun getNetworkClient(): NetworkClient = networkClient

    private fun setupNetworkServer() {
        lifecycleScope.launch {
            try {
                val serverStarted = connectionManager.startServer()
                if (serverStarted) {
                    Log.i(TAG, "Network server started automatically, listening on port 8080")
                    updateNotification("Listening for PC Controller on port 8080")
                } else {
                    Log.e(TAG, "Failed to start network server automatically")
                    updateNotification("Network server failed to start")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up network server", e)
                updateNotification("Network server error: ${e.message}")
            }
        }

        lifecycleScope.launch {
            connectionManager.connectionState.collect { state ->
                when (state) {
                    NetworkConnectionManager.ConnectionState.CONNECTED -> {
                        isConnectedToPC = true
                        Log.i(TAG, "PC Controller connected to network server")
                        updateNotification("PC Controller connected")
                        previewStreamer.startStreaming()
                        previewDataAdapter.startDataPolling()
                    }

                    NetworkConnectionManager.ConnectionState.DISCONNECTED -> {
                        isConnectedToPC = false
                        Log.i(TAG, "PC Controller disconnected, still listening on port 8080")
                        updateNotification("Listening for PC Controller on port 8080")
                        previewDataAdapter.stopDataPolling()
                        previewStreamer.stopStreaming()
                    }

                    NetworkConnectionManager.ConnectionState.ERROR -> {
                        isConnectedToPC = false
                        Log.e(TAG, "Network connection error")
                        updateNotification("Network connection error")
                    }

                    NetworkConnectionManager.ConnectionState.RECONNECTING -> {
                        isConnectedToPC = false
                        Log.i(TAG, "Attempting to reconnect to PC Controller")
                        updateNotification("Reconnecting to PC Controller...")
                    }

                    NetworkConnectionManager.ConnectionState.CONNECTING -> {
                        Log.i(TAG, "Connecting to PC Controller...")
                        updateNotification("Connecting...")
                    }
                }
            }
        }

        lifecycleScope.launch {
            networkServer.messageFlow.collect { message ->
                handleProtocolMessage(message)
            }
        }

        // Set up protocol handler with command callbacks
        protocolHandler.setCommandHandler(object : ProtocolHandler.CommandHandler {
            override suspend fun onStartRecording(sessionId: String): ProtocolHandler.CommandResult {
                return try {
                    Log.i(TAG, "Remote start recording command received for session: $sessionId")
                    startRecordingSession(sessionId)
                    ProtocolHandler.CommandResult(
                        success = true,
                        message = "Recording started",
                        data = mapOf("start_time" to System.currentTimeMillis().toString())
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start recording via PC command", e)
                    ProtocolHandler.CommandResult(false, "Start recording failed: ${e.message}")
                }
            }

            override suspend fun onStopRecording(sessionId: String): ProtocolHandler.CommandResult {
                return try {
                    Log.i(TAG, "Remote stop recording command received for session: $sessionId")
                    stopRecordingSession()
                    ProtocolHandler.CommandResult(
                        success = true,
                        message = "Recording stopped",
                        data = mapOf("stop_time" to System.currentTimeMillis().toString())
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to stop recording via PC command", e)
                    ProtocolHandler.CommandResult(false, "Stop recording failed: ${e.message}")
                }
            }

            override suspend fun onSyncRequest(pcTimestamp: Long): ProtocolHandler.SyncResult {
                return try {
                    val timeManager = mpdc4gsr.utils.TimeManager.getInstance(this@RecordingService)
                    val phoneTimestamp = timeManager.getCurrentTimestampNs() / 1_000_000 // Convert to ms

                    Log.d(TAG, "Time sync request: PC=$pcTimestamp, Phone=$phoneTimestamp")

                    // Calculate offset for immediate response (PC time - Phone time)
                    val offsetNs = (pcTimestamp - phoneTimestamp) * 1_000_000 // Convert to ns

                    // Update TimeManager with the calculated offset if needed
                    // Note: This is a simplified sync. For full sync, TimeManager.synchronizeWithPC should be used

                    ProtocolHandler.SyncResult(
                        success = true,
                        phoneTimestamp = phoneTimestamp,
                        offsetNs = offsetNs
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Time sync failed", e)
                    ProtocolHandler.SyncResult(false)
                }
            }
        })
    }

    private fun connectToPC(ipAddress: String, port: Int) {
        lifecycleScope.launch {
            try {
                Log.i(TAG, "Attempting connection to PC Controller at $ipAddress:$port")
                val serverStarted = connectionManager.startServer()
                if (serverStarted) {
                    Log.i(TAG, "Network server started, ready for PC Controller connection")
                    updateNotification("Ready for PC Controller connection")
                } else {
                    Log.e(TAG, "Failed to start network server")
                    updateNotification("Failed to start network server")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during PC connection attempt", e)
                updateNotification("Connection error: ${e.message}")
            }
        }
    }

    private fun disconnectFromPC() {
        lifecycleScope.launch {
            try {
                if (isNetworkInitialized) {
                    networkClient.disconnect()
                }
                connectionManager.stopServer()
                isConnectedToPC = false
                Log.i(TAG, "Disconnected from PC Controller")
                updateNotification("Disconnected from PC Controller")
            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting from PC", e)
            }
        }
    }

    private suspend fun processClientMessage(
        clientId: String,
        message: JSONObject,
        outputStream: DataOutputStream
    ) {
        val messageType = message.optString("message_type")
        val messageId = message.optString("msg_id", "unknown")

        structuredLogger.logProtocolMessage(
            "message_received", messageId, clientId,
            mapOf(
                "message_type" to messageType,
                "protocol_version" to message.optString("protocol_version", "unknown")
            )
        )
        if (!ProtocolVersion.validateMessageVersion(message)) {
            val errorMsg = "Unsupported protocol version"
            structuredLogger.logProtocolMessage(
                "protocol_version_error", messageId, clientId, mapOf("error" to errorMsg)
            )
            sendError(outputStream, errorMsg)
            return
        }
        try {
            when (messageType) {
                "protocol_handshake" -> {
                    val handshakeResult = ProtocolVersion.validateHandshakeResponse(message)
                    if (handshakeResult.success) {
                        structuredLogger.logProtocolMessage(
                            "handshake_success", messageId, clientId,
                            mapOf(
                                "negotiated_version" to (handshakeResult.negotiatedVersion
                                    ?: "unknown"),
                                "capabilities" to handshakeResult.commonCapabilities.joinToString(",")
                            )
                        )
                        val responseMessage = ProtocolVersion.createHandshakeMessage(
                            android.provider.Settings.Secure.getString(
                                contentResolver,
                                android.provider.Settings.Secure.ANDROID_ID
                            )
                        )
                        sendMessage(outputStream, responseMessage)
                    } else {
                        structuredLogger.logProtocolMessage(
                            "handshake_failed", messageId, clientId,
                            mapOf("error" to (handshakeResult.error ?: "unknown"))
                        )
                        sendError(outputStream, handshakeResult.error ?: "Handshake failed")
                    }
                }

                "session_start" -> {
                    val sessionId =
                        message.optString("session_id", "remote_${System.currentTimeMillis()}")
                    val sessionName = message.optString("session_name", "PC Remote Session")
                    structuredLogger.logSessionEvent(
                        "remote_session_start_request", sessionId,
                        mapOf("session_name" to sessionName, "client_id" to clientId)
                    )
                    val baseDir = File(getExternalFilesDir(null), "recordings")
                    val sessionDir = File(baseDir, sessionId)
                    withContext(Dispatchers.Main) {
                        startRecordingSession(sessionDir.absolutePath)
                    }
                    val ackMessage = ProtocolVersion.createProtocolMessage(
                        "ack",
                        JSONObject().apply {
                            put("ack_for", "session_start")
                            put("result", "Recording started")
                            put("session_id", sessionId)
                        })
                    sendMessage(outputStream, ackMessage)
                }

                "session_stop" -> {
                    structuredLogger.logSessionEvent(
                        "remote_session_stop_request", "current", mapOf("client_id" to clientId)
                    )
                    withContext(Dispatchers.Main) {
                        stopRecordingSession()
                    }
                    val ackMessage = ProtocolVersion.createProtocolMessage(
                        "ack",
                        JSONObject().apply {
                            put("ack_for", "session_stop")
                            put("result", "Recording stopped")
                        })
                    sendMessage(outputStream, ackMessage)
                }

                "sync_flash" -> {
                    val durationMs = message.optInt("duration_ms", 100)
                    structuredLogger.log(
                        StructuredLogger.LogLevel.INFO, "SyncFlash", "remote_sync_flash_request",
                        mapOf("duration_ms" to durationMs, "client_id" to clientId)
                    )
                    withContext(Dispatchers.Main) {
                        performSyncFlash(durationMs)
                    }
                    val ackMessage = ProtocolVersion.createProtocolMessage(
                        "ack",
                        JSONObject().apply {
                            put("ack_for", "sync_flash")
                            put("result", "Flash performed")
                        })
                    sendMessage(outputStream, ackMessage)
                }

                "status_request" -> {
                    structuredLogger.logProtocolMessage(
                        "status_request_received",
                        messageId,
                        clientId
                    )
                    sendStatusResponse(outputStream)
                }

                "heartbeat" -> {
                    structuredLogger.logProtocolMessage(
                        "heartbeat_received", messageId, clientId,
                        mapOf("timestamp" to message.optLong("timestamp", 0))
                    )
                    val ackMessage = ProtocolVersion.createProtocolMessage(
                        "ack",
                        JSONObject().apply {
                            put("ack_for", "heartbeat")
                            put("result", "alive")
                        })
                    sendMessage(outputStream, ackMessage)
                }

                else -> {
                    structuredLogger.logProtocolMessage(
                        "unknown_message_type",
                        messageId,
                        clientId,
                        mapOf("message_type" to messageType)
                    )
                    val errorMessage = ProtocolVersion.createProtocolMessage(
                        "error",
                        JSONObject().apply {
                            put("error", "Unknown message type: $messageType")
                        })
                    sendMessage(outputStream, errorMessage)
                }
            }
        } catch (e: Exception) {
            structuredLogger.logProtocolMessage(
                "message_processing_error", messageId, clientId,
                mapOf<String, Any>(
                    "message_type" to messageType,
                    "error" to (e.message ?: "Unknown error")
                )
            )
            val errorMessage = ProtocolVersion.createProtocolMessage(
                "error",
                JSONObject().apply {
                    put("error", "Error processing $messageType: ${e.message}")
                })
            sendMessage(outputStream, errorMessage)
        }
    }

    private suspend fun sendAck(
        outputStream: DataOutputStream,
        messageType: String,
        result: String
    ) {
        val ackMessage = JSONObject().apply {
            put("message_type", "ack")
            put("ack_for", messageType)
            put("result", result)
            put("timestamp", System.currentTimeMillis())
        }
        sendMessage(outputStream, ackMessage)
    }

    private suspend fun sendError(outputStream: DataOutputStream, error: String) {
        val errorMessage = JSONObject().apply {
            put("message_type", "error")
            put("error", error)
            put("timestamp", System.currentTimeMillis())
        }
        sendMessage(outputStream, errorMessage)
    }

    private suspend fun sendStatusResponse(outputStream: DataOutputStream) {
        val statusMessage = JSONObject().apply {
            put("message_type", "status_response")
            put(
                "device_id", android.provider.Settings.Secure.getString(
                    contentResolver, android.provider.Settings.Secure.ANDROID_ID
                )
            )
            put("recording_active", recordingController.isRecording)
            put("connected_clients", activeConnections.size)
            put("server_running", isServerRunning.get())
            put("sensors_initialized", isInitialized)
            put("current_session", currentSessionDirectory)
            put("timestamp", System.currentTimeMillis())
        }
        sendMessage(outputStream, statusMessage)
    }

    private suspend fun sendKeepAlive(outputStream: DataOutputStream) {
        val keepAliveMessage = JSONObject().apply {
            put("message_type", "keepalive")
            put("timestamp", System.currentTimeMillis())
        }
        sendMessage(outputStream, keepAliveMessage)
    }

    private suspend fun sendMessage(
        outputStream: DataOutputStream,
        message: JSONObject
    ) {
        withContext(Dispatchers.IO) {
            try {
                if (!message.has("protocol_version")) {
                    message.put("protocol_version", ProtocolVersion.CURRENT_VERSION)
                }
                val messageData = message.toString().toByteArray(Charsets.UTF_8)
                outputStream.writeInt(messageData.size)
                outputStream.write(messageData)
                outputStream.flush()
                structuredLogger.log(
                    StructuredLogger.LogLevel.DEBUG,
                    "ServerSocket",
                    "message_sent",
                    mapOf(
                        "message_type" to message.optString("message_type", "unknown"),
                        "size_bytes" to messageData.size
                    )
                )
            } catch (e: Exception) {
                structuredLogger.log(
                    StructuredLogger.LogLevel.ERROR,
                    "ServerSocket",
                    "message_send_error",
                    mapOf("error" to (e.message ?: "Unknown error"))
                )
                throw e
            }
        }
    }

    private fun startPCDiscovery() {
        lifecycleScope.launch {
            try {
                if (isNetworkInitialized) {
                    startNetworkDiscovery()
                } else {
                    Log.i(TAG, "PC Controller discovery requested")
                    updateNotification("Searching for PC Controller...")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting PC discovery", e)
            }
        }
    }

    private suspend fun handleProtocolMessage(message: mpdc4gsr.network.Protocol.ProtocolMessage) {
        try {
            val response = protocolHandler.processMessage(message)
            if (response != null) {
                networkServer.sendMessage(response)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing protocol message: ${message.type}", e)
            val errorResponse = mpdc4gsr.network.Protocol.createErrorMessage(
                message.type,
                mpdc4gsr.network.Protocol.ERR_FAIL,
                "Processing error: ${e.message}"
            )
            networkServer.sendMessage(errorResponse)
        }
    }

    private suspend fun handlePCCommand(message: JSONObject) {
        try {
            val messageType = message.optString("message_type")
            Log.i(TAG, "Processing PC command: $messageType")
            when (messageType) {
                "enhanced_device_registration" -> {
                    Log.i(TAG, "PC Controller device registration request")
                    sendResponseToPC(
                        "enhanced_registration_ack",
                        JSONObject().apply {
                            put("status", "success")
                            put("device_type", "android_sensor_node")
                            put("capabilities", JSONObject().apply {
                                put("recording", true)
                                put("sensors", arrayOf("rgb_camera", "thermal_camera", "gsr"))
                            })
                        })
                }

                "session_start_command" -> {
                    val sessionDirectory = message.optString("session_directory")
                    if (sessionDirectory.isNotEmpty()) {
                        Log.i(
                            TAG,
                            "Received remote start command from PC for session: $sessionDirectory"
                        )
                        startRecordingSession(sessionDirectory)
                        sendResponseToPC(
                            "session_start_response",
                            JSONObject().apply {
                                put("status", "started")
                                put("session_directory", sessionDirectory)
                            })
                    }
                }

                "session_stop_command" -> {
                    Log.i(TAG, "Received remote stop command from PC")
                    stopRecordingSession()
                    sendResponseToPC(
                        "session_stop_response",
                        JSONObject().apply {
                            put("status", "stopped")
                        })
                }

                "sync_marker_command" -> {
                    val markerType = message.optString("marker_type")
                    val timestampNs = message.optLong("timestamp_ns", System.nanoTime())
                    if (markerType.isNotEmpty()) {
                        Log.i(TAG, "Received remote sync marker from PC: $markerType")
                        addSyncMarker(markerType, timestampNs)
                        sendResponseToPC(
                            "sync_marker_response",
                            JSONObject().apply {
                                put("status", "added")
                                put("marker_type", markerType)
                            })
                    }
                }

                "ping" -> {
                    Log.d(TAG, "Received ping from PC Controller")
                    sendResponseToPC("pong", JSONObject().apply {
                        put("timestamp_ns", System.nanoTime())
                    })
                }

                "status_request" -> {
                    Log.d(TAG, "PC Controller requested status")
                    sendStatusToPC()
                }

                "start_preview_streaming" -> {
                    Log.i(TAG, "PC Controller requested to start preview streaming")
                    lifecycleScope.launch {
                        val success = previewStreamer.startStreaming()
                        sendResponseToPC("preview_streaming_response", JSONObject().apply {
                            put("status", if (success) "started" else "failed")
                            put(
                                "message",
                                if (success) "Preview streaming started" else "Failed to start preview streaming"
                            )
                        })
                    }
                }

                "stop_preview_streaming" -> {
                    Log.i(TAG, "PC Controller requested to stop preview streaming")
                    lifecycleScope.launch {
                        previewStreamer.stopStreaming()
                        sendResponseToPC("preview_streaming_response", JSONObject().apply {
                            put("status", "stopped")
                            put("message", "Preview streaming stopped")
                        })
                    }
                }

                "configure_preview_streaming" -> {
                    Log.i(TAG, "PC Controller requested to configure preview streaming")
                    val frameInterval = message.optLong("frame_interval_ms", 1000L)
                    val sensorInterval = message.optLong("sensor_interval_ms", 1000L)
                    val previewWidth = message.optInt("preview_width", 320)
                    val previewHeight = message.optInt("preview_height", 240)
                    val jpegQuality = message.optInt("jpeg_quality", 70)

                    previewStreamer.configure(
                        frameInterval,
                        sensorInterval,
                        previewWidth,
                        previewHeight,
                        jpegQuality
                    )
                    sendResponseToPC("preview_config_response", JSONObject().apply {
                        put("status", "configured")
                        put("frame_interval_ms", frameInterval)
                        put("sensor_interval_ms", sensorInterval)
                        put("preview_width", previewWidth)
                        put("preview_height", previewHeight)
                        put("jpeg_quality", jpegQuality)
                    })
                }

                else -> {
                    Log.w(TAG, "Unknown command from PC Controller: $messageType")
                    sendResponseToPC("error", JSONObject().apply {
                        put("message", "Unknown command: $messageType")
                    })
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling PC command", e)
            sendResponseToPC("error", JSONObject().apply {
                put("message", "Error processing command: ${e.message}")
            })
        }
    }

    private fun isServiceForeground(): Boolean {

        return recordingController.isRecording || isServerRunning.get()
    }

    private fun createServerNotification(contentText: String): Notification {
        val stopIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_STOP_SERVER
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("IRCamera Server")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop Server",
                stopPendingIntent
            )
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    fun startNetworkDiscovery() {
        lifecycleScope.launch {
            try {
                networkClient.startDiscovery { success ->
                    if (success) {
                        Log.i(TAG, "Network discovery started successfully")
                    } else {
                        Log.w(TAG, "Network discovery failed to start")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting network discovery", e)
            }
        }
    }

    fun handleStartRecordingCommand(message: JSONObject) {
        lifecycleScope.launch {
            try {
                val sessionId =
                    message.optString("session_id", "session_${System.currentTimeMillis()}")
                val sessionDirectory = "/storage/emulated/0/IRCamera_Sessions/$sessionId"
                Log.i(TAG, "Received start recording command from PC Controller")
                startRecordingSession(sessionDirectory)
                val response = JSONObject().apply {
                    put("message_type", "response")
                    put("response_to", "start_recording")
                    put("status", "success")
                    put("session_id", sessionId)
                    put("message", "Recording started successfully")
                }
                networkClient.sendMessage(response)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling start recording command", e)
                val response = JSONObject().apply {
                    put("message_type", "response")
                    put("response_to", "start_recording")
                    put("status", "error")
                    put("message", "Failed to start recording: ${e.message}")
                }
                networkClient.sendMessage(response)
            }
        }
    }

    fun performSyncFlash(durationMs: Int) {
        // This is a placeholder for the actual flash logic.

        this.addSyncMarker("pc_sync_flash", System.nanoTime())
    }

    fun registerNsdService() {
        if (isServiceRegistered) {
            Log.i(TAG, "NSD service already registered")
            return
        }
        try {
            val serviceInfo = NsdServiceInfo().apply {
                serviceName = SERVICE_NAME
                serviceType = SERVICE_TYPE
                port = SERVER_PORT
            }
            nsdManager?.registerService(
                serviceInfo,
                NsdManager.PROTOCOL_DNS_SD,
                object : NsdManager.RegistrationListener {
                    override fun onServiceRegistered(registeredServiceInfo: NsdServiceInfo?) {
                        Log.i(TAG, "NSD service registered: ${registeredServiceInfo?.serviceName}")
                        nsdServiceInfo = registeredServiceInfo
                        isServiceRegistered = true
                    }

                    override fun onRegistrationFailed(
                        failedServiceInfo: NsdServiceInfo?,
                        errorCode: Int
                    ) {
                        Log.e(TAG, "NSD service registration failed: $errorCode")
                    }

                    override fun onServiceUnregistered(unregisteredServiceInfo: NsdServiceInfo?) {
                        Log.i(
                            TAG,
                            "NSD service unregistered: ${unregisteredServiceInfo?.serviceName}"
                        )
                        isServiceRegistered = false
                    }

                    override fun onUnregistrationFailed(
                        failedServiceInfo: NsdServiceInfo?,
                        errorCode: Int
                    ) {
                        Log.e(TAG, "NSD service unregistration failed: $errorCode")
                    }
                })
        } catch (e: Exception) {
            Log.e(TAG, "Error registering NSD service", e)
        }
    }

    fun unregisterNsdService() {
        if (!isServiceRegistered || nsdServiceInfo == null) {
            return
        }
        try {
            nsdManager?.unregisterService(object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {}
                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {}
                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {
                    Log.i(TAG, "NSD service unregistered successfully")
                    isServiceRegistered = false
                    nsdServiceInfo = null
                }

                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                    Log.e(TAG, "NSD service unregistration failed: $errorCode")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering NSD service", e)
        }
    }

    private suspend fun sendResponseToPC(
        messageType: String,
        data: JSONObject = JSONObject()
    ) {
        try {
            val response = JSONObject().apply {
                put("message_type", messageType)
                put(
                    "device_id",
                    android.provider.Settings.Secure.getString(
                        contentResolver,
                        android.provider.Settings.Secure.ANDROID_ID
                    )
                )
                put("timestamp_ns", System.nanoTime())
                data.keys().forEach { key ->
                    put(key, data.get(key))
                }
            }
            networkServer.sendMessage(response.toString())
            Log.d(TAG, "Sent response to PC: $messageType")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending response to PC", e)
        }
    }

    private suspend fun sendStatusToPC() {
        try {
            val statusData = JSONObject().apply {
                put("is_recording", recordingController.isRecording)
                put("current_session", currentSessionDirectory ?: "")
                put("recording_start_time", recordingStartTime)
                put("service_initialized", isInitialized)
                put("network_server_running", networkServer.isRunning())
                put("pc_connected", isConnectedToPC)
            }
            sendResponseToPC("status_response", statusData)
            Log.i(TAG, "Status sent to PC Controller")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending status to PC", e)
        }
    }

    private fun handleQueryStatusCommand(message: JSONObject) {
        try {
            Log.d(TAG, "Handling query status command")
            lifecycleScope.launch {
                sendStatusToPC()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling query status command", e)
        }
    }

    private fun handleSyncFlashCommand(message: JSONObject) {
        try {
            val durationMs = message.optInt("duration_ms", 100)
            Log.d(TAG, "Handling sync flash command: ${durationMs}ms")
            addSyncMarker("pc_sync_flash", System.nanoTime())
            lifecycleScope.launch {
                sendResponseToPC("sync_flash_response", JSONObject().apply {
                    put("status", "completed")
                    put("duration_ms", durationMs)
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling sync flash command", e)
        }
    }

    private fun handleQueryCapabilitiesCommand(message: JSONObject) {
        try {
            Log.d(TAG, "Handling query capabilities command")
            val capabilities = JSONObject().apply {
                put("sensors", JSONArray().apply {
                    put("RGB_Camera")
                    put("Thermal_Camera")
                    put("GSR_Sensor")
                })
                put("recording_formats", JSONArray().apply {
                    put("mp4")
                    put("csv")
                    put("jpg")
                })
                put("sync_capabilities", JSONArray().apply {
                    put("flash_sync")
                    put("timestamp_sync")
                })
            }
            lifecycleScope.launch {
                sendResponseToPC("capabilities_response", capabilities)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling query capabilities command", e)
        }
    }

    private fun handleStopRecordingCommand(message: JSONObject) {
        try {
            Log.d(TAG, "Handling stop recording command")
            lifecycleScope.launch {
                if (recordingController.isRecording) {
                    stopRecordingSession()
                    sendResponseToPC("stop_recording_response", JSONObject().apply {
                        put("status", "stopped")
                        put("session_directory", currentSessionDirectory ?: "")
                    })
                } else {
                    sendResponseToPC("stop_recording_response", JSONObject().apply {
                        put("status", "not_recording")
                    })
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling stop recording command", e)
        }
    }
}
