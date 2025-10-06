package mpdc4gsr.core
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.csl.irCamera.R
import com.mpdc4gsr.gsr.model.SessionInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mpdc4gsr.core.data.FeatureFlags
import mpdc4gsr.core.data.ProtocolVersion
import mpdc4gsr.core.data.TimeSyncManager
import mpdc4gsr.core.ui.PermissionManager
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import mpdc4gsr.feature.network.data.*
import mpdc4gsr.feature.network.data.Protocol
import org.json.JSONArray
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.File
import java.net.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
class RecordingService : Service(), CoroutineScope {
    private val serviceJob = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + serviceJob
    companion object {
        private const val TAG = "RecordingService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "recording_service_channel"
        private const val SERVER_PORT =
            8081  // Use different port to avoid conflicts with NetworkController
        private const val SERVICE_TYPE = "_ircamera._tcp."
        private const val SERVICE_NAME = "IRCamera-Android"
        @get:androidx.annotation.RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        private val FOREGROUND_SERVICE_TYPES
            get() = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
        const val ACTION_START_RECORDING =
            "${com.csl.irCamera.BuildConfig.APPLICATION_ID}.START_RECORDING"
        const val ACTION_STOP_RECORDING =
            "${com.csl.irCamera.BuildConfig.APPLICATION_ID}.STOP_RECORDING"
        const val ACTION_ADD_SYNC_MARKER =
            "${com.csl.irCamera.BuildConfig.APPLICATION_ID}.ADD_SYNC_MARKER"
        const val ACTION_START_SERVER =
            "${com.csl.irCamera.BuildConfig.APPLICATION_ID}.START_SERVER"
        const val ACTION_STOP_SERVER = "${com.csl.irCamera.BuildConfig.APPLICATION_ID}.STOP_SERVER"
        const val ACTION_CONNECT_PC = "${com.csl.irCamera.BuildConfig.APPLICATION_ID}.CONNECT_PC"
        const val ACTION_DISCONNECT_PC =
            "${com.csl.irCamera.BuildConfig.APPLICATION_ID}.DISCONNECT_PC"
        const val ACTION_CONNECT_PC_CLIENT =
            "${com.csl.irCamera.BuildConfig.APPLICATION_ID}.CONNECT_PC_CLIENT"
        const val ACTION_DISCONNECT_PC_CLIENT =
            "${com.csl.irCamera.BuildConfig.APPLICATION_ID}.DISCONNECT_PC_CLIENT"
        const val ACTION_CONNECT_PC_BLUETOOTH =
            "${com.csl.irCamera.BuildConfig.APPLICATION_ID}.CONNECT_PC_BLUETOOTH"
        const val ACTION_START_DISCOVERY =
            "${com.csl.irCamera.BuildConfig.APPLICATION_ID}.START_DISCOVERY"
        const val EXTRA_SESSION_DIRECTORY = "session_directory"
        const val EXTRA_MARKER_TYPE = "marker_type"
        const val EXTRA_TIMESTAMP_NS = "timestamp_ns"
        const val EXTRA_PC_IP = "pc_ip"
        const val EXTRA_PC_PORT = "pc_port"
        const val EXTRA_BLUETOOTH_DEVICE = "bluetooth_device"
        // Type aliases for compatibility
        typealias SessionManifest = mpdc4gsr.feature.network.data.SessionManifest
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
        fun connectToPCClient(context: Context, ipAddress: String, port: Int = 8080) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_CONNECT_PC_CLIENT
                putExtra(EXTRA_PC_IP, ipAddress)
                putExtra(EXTRA_PC_PORT, port)
            }
            context.startService(intent)
        }
        fun connectToPCBluetooth(
            context: Context,
            bluetoothDevice: android.bluetooth.BluetoothDevice
        ) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_CONNECT_PC_BLUETOOTH
                putExtra(EXTRA_BLUETOOTH_DEVICE, bluetoothDevice)
            }
            context.startService(intent)
        }
        fun disconnectFromPC(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_DISCONNECT_PC
            }
            context.startService(intent)
        }
        fun disconnectFromPCClient(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_DISCONNECT_PC_CLIENT
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
    private var permissionManager: PermissionManager? = null
    private var isInitialized = false
    private lateinit var crashRecoveryManager: CrashRecoveryManager
    private lateinit var networkClient: NetworkClient
    private lateinit var networkServer: NetworkServer
    private lateinit var networkManager: NetworkManager
    private lateinit var protocolHandler: ProtocolHandler
    private lateinit var connectionManager: NetworkConnectionManager
    internal lateinit var previewStreamer: PreviewStreamer
    internal lateinit var previewDataAdapter: PreviewDataAdapter
    private var isNetworkInitialized = false
    internal var isConnectedToPC = false
    private var currentSessionDirectory: String? = null
    private var recordingStartTime: Long = 0
    private lateinit var notificationManager: NotificationManager
    private var serverSocket: ServerSocket? = null
    private var actualServerPort: Int = SERVER_PORT
    private var isServerRunning = AtomicBoolean(false)
    private var serverJob: Job? = null
    private val activeConnections = ConcurrentHashMap<String, ClientConnection>()
    private var nsdManager: NsdManager? = null
    private var nsdServiceInfo: NsdServiceInfo? = null
    private var isServiceRegistered = false
    private lateinit var structuredLogger: StructuredLogger
    private lateinit var crashSafeSupervisor: CrashSafeSupervisor
    private var timeSyncManager: TimeSyncManager? = null
    private data class ClientConnection(
        val socket: Socket,
        val clientId: String,
        val inputStream: DataInputStream,
        val outputStream: DataOutputStream,
        val job: Job
    )
    inner class RecordingServiceBinder : Binder() {
        fun getService(): RecordingService = this@RecordingService
        fun getRecordingController(): ComprehensiveRecordingController? =
            if (::recordingController.isInitialized) recordingController else null
        fun getNetworkServer(): NetworkServer? =
            if (::networkServer.isInitialized) networkServer else null
        fun getPreviewStreamer(): PreviewStreamer? =
            if (::previewStreamer.isInitialized) previewStreamer else null
        fun getPreviewDataAdapter(): PreviewDataAdapter? =
            if (::previewDataAdapter.isInitialized) previewDataAdapter else null
        fun isConnectedToPC(): Boolean = this@RecordingService.isConnectedToPC
        fun getServerStatus(): String {
            return if (isServerRunning.get()) {
                "Running on port $actualServerPort (${activeConnections.size} clients)"
            } else {
                "Stopped"
            }
        }
        fun getActualServerPort(): Int = actualServerPort
        fun getConnectedClients(): List<String> {
            return activeConnections.keys.toList()
        }
        fun getNetworkClient(): NetworkClient? = if (isNetworkInitialized) networkClient else null
        fun getNetworkManager(): NetworkManager? =
            if (::networkManager.isInitialized) networkManager else null
    }
    override fun onCreate() {
        super.onCreate()
        AppLogger.i(TAG, "RecordingService created")
        initializePhase0Baseline()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        // Call startForeground immediately to satisfy Android's foreground service requirements
        // This must be called within 5-10 seconds of startForegroundService()
        // Use ServiceCompat to include the FGS type for Android 14+
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("IRCamera Service")
            .setContentText("Initializing service...")
            .setSmallIcon(R.drawable.ic_info)
            .setOngoing(true)
            .build()
        startForegroundWithType(NOTIFICATION_ID, notification)
        nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager
        // Initialize ComprehensiveRecordingController without PermissionManager for service context
        // PermissionManager requires ComponentActivity which is not available in service context
        // Note: Second parameter needs LifecycleOwner - passing null for service context
        recordingController = ComprehensiveRecordingController(this, null, null)
        // Initialize crash recovery manager for session orchestration
        crashRecoveryManager = CrashRecoveryManager(this)
        networkClient = NetworkClient(this)
        networkServer =
            NetworkServer(this, Protocol.DEFAULT_PORT)  // Use Protocol.DEFAULT_PORT (8080)
        networkManager = NetworkManager(this, recordingController)
        protocolHandler = ProtocolHandler(this, networkServer)
        protocolHandler.setTimeSyncManager(timeSyncManager)
        // Set up sync trigger callback for manual sync requests (if TimeSyncManager is available)
        timeSyncManager?.setSyncTriggerCallback(object : TimeSyncManager.SyncTriggerCallback {
            override suspend fun onManualSyncRequested(): Boolean {
                return try {
                    AppLogger.i(TAG, "Manual sync requested - sending SYNC_INIT to PC")
                    val syncInitMessage = Protocol.createSyncInitMessage()
                    val sent = networkServer.sendMessage(syncInitMessage)
                    if (sent) {
                        AppLogger.i(TAG, "SYNC_INIT message sent to PC successfully")
                    } else {
                        AppLogger.w(TAG, "Failed to send SYNC_INIT message to PC (no connection?)")
                    }
                    sent
                } catch (e: java.io.IOException) {
                    AppLogger.e(TAG, "Network I/O error during manual sync trigger", e)
                    false
                } catch (e: IllegalStateException) {
                    AppLogger.e(TAG, "Invalid state during manual sync trigger", e)
                    false
                }
            }
        })
        connectionManager = NetworkConnectionManager(this, networkServer, protocolHandler)
        previewStreamer = PreviewStreamer(networkServer)
        previewDataAdapter = PreviewDataAdapter(previewStreamer, this)
        crashSafeSupervisor.registerJob(
            id = "recording_service_init",
            name = "RecordingService Initialization",
            critical = true,
            restartable = false
        ) {
            launch {
                try {
                    AppLogger.i(TAG, "Initializing RecordingService with enhanced fault tolerance")
                    // Check for crashed sessions on startup using session orchestration crash recovery
                    checkForCrashedSessionsOnStartup()
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
                            AppLogger.i(TAG, "Network client initialized successfully")
                            startNetworkDiscovery()
                        } else {
                            Log.w(
                                TAG,
                                "Network client initialization failed - running in server-only mode"
                            )
                        }
                    } else {
                        AppLogger.e(TAG, "No sensors could be initialized - service cannot operate")
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
            // Initialize TimeSyncManager (optional due to compilation issues)
            timeSyncManager = try {
                TimeSyncManager(this)
            } catch (e: Exception) {
                AppLogger.w(TAG, "Failed to initialize TimeSyncManager, continuing without sync", e)
                null
            }
            structuredLogger.log(
                StructuredLogger.LogLevel.INFO,
                "RecordingService",
                "phase0_baseline_initialized",
                mapOf(
                    "feature_flags" to FeatureFlags.getAllFlags(),
                    "protocol_version" to ProtocolVersion.CURRENT_VERSION,
                    "time_sync_manager" to "initialized"
                )
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to initialize Phase 0 baseline in service", e)
        }
    }

    private fun startForegroundWithType(id: Int, notification: Notification, forRecording: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val serviceType = if (forRecording) {
                // When recording, we need camera and data sync types
                // Note: Microphone type removed - audio recording handled by RgbCameraRecorder
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                // For server/networking only, just use dataSync
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            }
            ServiceCompat.startForeground(this, id, notification, serviceType)
        } else {
            startForeground(id, notification)
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
                    AppLogger.e(TAG, "No session directory provided for recording")
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
            ACTION_CONNECT_PC_CLIENT -> {
                val ipAddress = intent.getStringExtra(EXTRA_PC_IP)
                val port = intent.getIntExtra(EXTRA_PC_PORT, 8080)
                if (ipAddress != null) {
                    connectToPCClient(ipAddress, port)
                }
            }
            ACTION_CONNECT_PC_BLUETOOTH -> {
                val bluetoothDevice =
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(
                            EXTRA_BLUETOOTH_DEVICE,
                            android.bluetooth.BluetoothDevice::class.java
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(EXTRA_BLUETOOTH_DEVICE)
                    }
                if (bluetoothDevice != null) {
                    connectToPCBluetooth(bluetoothDevice)
                }
            }
            ACTION_DISCONNECT_PC -> disconnectFromPC()
            ACTION_DISCONNECT_PC_CLIENT -> disconnectFromPCClient()
            ACTION_START_DISCOVERY -> startPCDiscovery()
        }
        return START_STICKY
    }
    override fun onBind(intent: Intent): IBinder {
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
            // Cleanup TimeSyncManager
            timeSyncManager?.let { manager ->
                try {
                    manager.cleanup()
                } catch (e: Exception) {
                    AppLogger.w(TAG, "TimeSyncManager cleanup failed", e)
                }
            }
            if (::networkManager.isInitialized) {
                networkManager.cleanup()
            }
            if (::recordingController.isInitialized) {
                launch {
                    recordingController.cleanup()
                }
            }
            if (::crashSafeSupervisor.isInitialized) {
                crashSafeSupervisor.shutdown()
            }
        } catch (e: Exception) {
            structuredLogger.log(
                StructuredLogger.LogLevel.ERROR,
                "RecordingService",
                "service_cleanup_error",
                mapOf("error" to (e.message ?: "Unknown error"))
            )
        } finally {
            // Cancel all coroutines launched in this service's scope
            serviceJob.cancel()
        }
        AppLogger.i(TAG, "RecordingService destroyed")
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
            AppLogger.e(TAG, "Service not initialized, cannot start recording")
            structuredLogger.log(
                StructuredLogger.LogLevel.ERROR,
                "RecordingService",
                "recording_start_failed",
                mapOf("reason" to "service_not_initialized")
            )
            return
        }
        launch {
            try {
                val sessionDir = File(sessionDirectory)
                if (!sessionDir.exists()) {
                    sessionDir.mkdirs()
                }
                currentSessionDirectory = sessionDirectory
                recordingStartTime = System.nanoTime()
                // Initialize TimeSyncManager for this session
                timeSyncManager?.initializeSession(sessionDirectory)
                // Enable periodic sync for long recording sessions
                timeSyncManager?.setPeriodicSyncEnabled(true)
                AppLogger.i(TAG, "Starting recording session: $sessionDirectory")
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
                startForegroundWithType(
                    NOTIFICATION_ID,
                    createRecordingNotification("Starting recording session..."),
                    forRecording = true
                )
                // Perform session start sync
                launch {
                    try {
                        timeSyncManager?.performSessionStartSync()
                    } catch (e: Exception) {
                        AppLogger.w(TAG, "Session start sync failed, continuing without sync", e)
                    }
                }
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
                    AppLogger.e(TAG, "Failed to start recording session")
                    updateNotification("Recording failed to start")
                    structuredLogger.log(
                        StructuredLogger.LogLevel.ERROR,
                        "RecordingService",
                        "recording_session_start_failed"
                    )
                    stopRecordingSession()
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error starting recording session", e)
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
        launch {
            try {
                updateNotification("Stopping recording session...")
                AppLogger.i(TAG, "Stopping recording session")
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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    } else {
                        @Suppress("DEPRECATION")
                        stopForeground(true)
                    }
                } else {
                    AppLogger.e(TAG, "Failed to stop recording session cleanly")
                    updateNotification("Recording stop failed")
                    structuredLogger.log(
                        StructuredLogger.LogLevel.ERROR,
                        "RecordingService",
                        "recording_session_stop_failed"
                    )
                }
                currentSessionDirectory = null
                recordingStartTime = 0
                // Finalize TimeSyncManager session
                try {
                    timeSyncManager?.finalizeSession()
                } catch (e: Exception) {
                    AppLogger.w(TAG, "TimeSyncManager finalize session failed", e)
                }
                if (!isServerRunning.get()) {
                    stopSelf()
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error stopping recording session", e)
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
    // Enhanced recording methods with trigger source support for session orchestration
    private suspend fun startRecordingSessionWithTrigger(
        sessionDirectory: String,
        triggerSource: TriggerSource
    ): Boolean {
        if (!isInitialized) {
            AppLogger.e(TAG, "Service not initialized, cannot start recording")
            structuredLogger.log(
                StructuredLogger.LogLevel.ERROR,
                "RecordingService",
                "recording_start_failed",
                mapOf(
                    "reason" to "service_not_initialized",
                    "trigger_source" to triggerSource.toString()
                )
            )
            return false
        }
        return try {
            val sessionDir = File(sessionDirectory)
            if (!sessionDir.exists()) {
                sessionDir.mkdirs()
            }
            currentSessionDirectory = sessionDirectory
            recordingStartTime = System.nanoTime()
            // Initialize TimeSyncManager for this session
            timeSyncManager?.initializeSession(sessionDirectory)
            timeSyncManager?.setPeriodicSyncEnabled(true)
            AppLogger.i(TAG, "Starting recording session: $sessionDirectory (trigger: $triggerSource)")
            structuredLogger.log(
                StructuredLogger.LogLevel.INFO,
                "RecordingService",
                "recording_session_start",
                mapOf(
                    "session_directory" to sessionDirectory,
                    "trigger_source" to triggerSource.toString(),
                    "available_sensors" to recordingController.getAvailableSensors()
                        .map { it.sensorId }
                )
            )
            // Update notification for different trigger sources
            val notificationText = when (triggerSource) {
                TriggerSource.REMOTE_PC -> "Starting recording session (PC Command)..."
                TriggerSource.LOCAL_NOTIFICATION -> "Starting recording session (Notification)..."
                else -> "Starting recording session..."
            }
            startForegroundWithType(NOTIFICATION_ID, createRecordingNotification(notificationText), forRecording = true)
            // Start session with enhanced orchestration
            val success = recordingController.startRecording(
                sessionId = sessionDir.name,
                triggerSource = triggerSource
            )
            if (success) {
                AppLogger.i(TAG, "Recording session started successfully via $triggerSource")
                // Update notification to show recording is active
                val activeNotificationText = when (triggerSource) {
                    TriggerSource.REMOTE_PC -> "Recording (PC Command) - Tap to stop"
                    TriggerSource.LOCAL_NOTIFICATION -> "Recording (Notification) - Tap to stop"
                    else -> "Recording - Tap to stop"
                }
                updateNotification(activeNotificationText)
                // Mark session as active for crash recovery
                crashRecoveryManager.markSessionActive(
                    sessionId = sessionDir.name,
                    sessionDirectory = sessionDirectory,
                    activeSensors = recordingController.getAvailableSensors().map { it.sensorId }
                )
                structuredLogger.log(
                    StructuredLogger.LogLevel.INFO,
                    "RecordingService",
                    "recording_session_started",
                    mapOf(
                        "session_id" to sessionDir.name,
                        "trigger_source" to triggerSource.toString()
                    )
                )
            } else {
                AppLogger.e(TAG, "Failed to start recording session via $triggerSource")
                updateNotification("Recording start failed")
                currentSessionDirectory = null
                recordingStartTime = 0
            }
            success
        } catch (e: Exception) {
            AppLogger.e(TAG, "Exception starting recording session via $triggerSource", e)
            updateNotification("Recording start error")
            structuredLogger.log(
                StructuredLogger.LogLevel.ERROR,
                "RecordingService",
                "recording_session_start_exception",
                mapOf(
                    "error" to (e.message ?: "Unknown error"),
                    "trigger_source" to triggerSource.toString()
                )
            )
            currentSessionDirectory = null
            recordingStartTime = 0
            false
        }
    }
    private suspend fun stopRecordingSessionWithTrigger(triggerSource: TriggerSource): Boolean {
        return try {
            updateNotification("Stopping recording session...")
            AppLogger.i(TAG, "Stopping recording session (trigger: $triggerSource)")
            val success = recordingController.stopRecording(triggerSource = triggerSource)
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
                    }s, trigger: $triggerSource)"
                )
                val completedNotificationText = when (triggerSource) {
                    TriggerSource.REMOTE_PC -> "Recording completed via PC (${
                        String.format(
                            "%.1f",
                            sessionDuration
                        )
                    }s)"
                    TriggerSource.LOCAL_NOTIFICATION -> "Recording completed via notification (${
                        String.format(
                            "%.1f",
                            sessionDuration
                        )
                    }s)"
                    else -> "Recording completed (${String.format("%.1f", sessionDuration)}s)"
                }
                updateNotification(completedNotificationText)
                // Generate and save session manifest
                val manifest = recordingController.generateSessionManifest()
                saveSessionManifest(manifest)
                // Mark session as completed for crash recovery
                currentSessionDirectory?.let { sessionDir ->
                    crashRecoveryManager.markSessionCompleted(File(sessionDir).name)
                }
                structuredLogger.log(
                    StructuredLogger.LogLevel.INFO,
                    "RecordingService",
                    "recording_session_stopped",
                    mapOf(
                        "session_duration_seconds" to sessionDuration,
                        "session_directory" to (currentSessionDirectory ?: "unknown"),
                        "trigger_source" to triggerSource.toString()
                    )
                )
                delay(2000)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
            } else {
                AppLogger.e(TAG, "Failed to stop recording session cleanly (trigger: $triggerSource)")
                updateNotification("Recording stop failed")
                structuredLogger.log(
                    StructuredLogger.LogLevel.ERROR,
                    "RecordingService",
                    "recording_session_stop_failed",
                    mapOf("trigger_source" to triggerSource.toString())
                )
            }
            currentSessionDirectory = null
            recordingStartTime = 0
            // Finalize TimeSyncManager session
            try {
                timeSyncManager?.finalizeSession()
            } catch (e: Exception) {
                AppLogger.w(TAG, "TimeSyncManager finalize session failed", e)
            }
            if (!isServerRunning.get()) {
                stopSelf()
            }
            success
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error stopping recording session via $triggerSource", e)
            updateNotification("Recording stop error")
            structuredLogger.log(
                StructuredLogger.LogLevel.ERROR,
                "RecordingService",
                "recording_session_stop_exception",
                mapOf(
                    "error" to (e.message ?: "Unknown error"),
                    "trigger_source" to triggerSource.toString()
                )
            )
            false
        }
    }
    // Session manifest saving
    private fun saveSessionManifest(manifest: SessionManifest) {
        try {
            currentSessionDirectory?.let { sessionDir ->
                val manifestFile = File(sessionDir, "session_manifest.json")
                val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
                manifestFile.writeText(gson.toJson(manifest))
                AppLogger.i(TAG, "Session manifest saved: ${manifestFile.absolutePath}")
                structuredLogger.log(
                    StructuredLogger.LogLevel.INFO,
                    "RecordingService",
                    "session_manifest_saved",
                    mapOf(
                        "manifest_file" to manifestFile.absolutePath,
                        "session_state" to manifest.sessionState.toString(),
                        "trigger_source" to manifest.triggerSource.toString()
                    )
                )
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to save session manifest", e)
            structuredLogger.log(
                StructuredLogger.LogLevel.ERROR,
                "RecordingService",
                "session_manifest_save_failed",
                mapOf("error" to (e.message ?: "Unknown error"))
            )
        }
    }
    // Crash recovery integration for session orchestration
    private suspend fun checkForCrashedSessionsOnStartup() {
        try {
            AppLogger.i(TAG, "Checking for crashed sessions on service startup")
            val crashRecoveryResult = crashRecoveryManager.checkForCrashedSessions()
            if (crashRecoveryResult.hasCrashedSession) {
                val recoveredSession = crashRecoveryResult.recoveredSession!!
                AppLogger.w(TAG, "Found crashed session: ${recoveredSession.sessionId}")
                AppLogger.i(TAG, "Crashed session analysis: ${recoveredSession.analysis.summary}")
                structuredLogger.log(
                    StructuredLogger.LogLevel.WARNING,
                    "RecordingService",
                    "crashed_session_detected",
                    mapOf(
                        "session_id" to recoveredSession.sessionId,
                        "session_age_ms" to recoveredSession.sessionAge.toString(),
                        "active_sensors" to recoveredSession.activeSensors.joinToString(","),
                        "has_partial_data" to (recoveredSession.analysis.partialDataSize > 0).toString(),
                        "partial_data_size" to recoveredSession.analysis.partialDataSize.toString()
                    )
                )
                // Perform recovery
                val recoveryResult = crashRecoveryManager.recoverCrashedSession(recoveredSession)
                if (recoveryResult.success) {
                    Log.i(
                        TAG,
                        "Successfully recovered crashed session: ${recoveredSession.sessionId}"
                    )
                    AppLogger.i(TAG, "Recovery actions performed: ${recoveryResult.recoveryActions.size}")
                    structuredLogger.log(
                        StructuredLogger.LogLevel.INFO,
                        "RecordingService",
                        "crashed_session_recovered",
                        mapOf(
                            "session_id" to recoveredSession.sessionId,
                            "recovery_actions" to recoveryResult.recoveryActions.size.toString()
                        )
                    )
                } else {
                    AppLogger.e(TAG, "Failed to recover crashed session: ${recoveryResult.error}")
                    structuredLogger.log(
                        StructuredLogger.LogLevel.ERROR,
                        "RecordingService",
                        "crashed_session_recovery_failed",
                        mapOf(
                            "session_id" to recoveredSession.sessionId,
                            "error" to (recoveryResult.error ?: "Unknown error")
                        )
                    )
                }
            } else {
                AppLogger.i(TAG, "No crashed sessions found")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error during crash recovery check", e)
            structuredLogger.log(
                StructuredLogger.LogLevel.ERROR,
                "RecordingService",
                "crash_recovery_check_failed",
                mapOf("error" to (e.message ?: "Unknown error"))
            )
        }
    }
    private fun addSyncMarker(markerType: String, timestampNs: Long) {
        launch {
            try {
                recordingController.addSyncMarker(markerType, timestampNs)
                AppLogger.i(TAG, "Sync marker added: $markerType")
                if (recordingController.isRecording) {
                    val originalText = "Recording in progress"
                    updateNotification("Sync marker: $markerType")
                    delay(1000)
                    updateNotification(originalText)
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error adding sync marker", e)
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
            .launchIn(this)
        recordingController.sensorStatusFlow
            .onEach { statusList ->
                val activeSensors = statusList.count { it.isActive }
                val totalSamples = statusList.sumOf { it.samplesRecorded }
                val totalStorage = statusList.sumOf {
                    // Calculate storage based on samples (rough estimate)
                    (it.samplesRecorded * 0.001) // ~1KB per sample estimate
                }
                if (activeSensors > 0) {
                    val statusText = "Recording: $activeSensors sensors, " +
                            "${totalSamples} samples, " +
                            "${String.format("%.1f", totalStorage)}MB"
                    updateNotification(statusText)
                }
            }
            .launchIn(this)
        recordingController.errorFlow
            .onEach { error ->
                error?.let {
                    AppLogger.w(TAG, "Recording controller error: ${it.message}")
                    if (!it.isRecoverable) {
                        updateNotification("Critical error: ${it.message}")
                        stopRecordingSession()
                    } else {
                        updateNotification("Warning: ${it.message}")
                        delay(3000)
                        if (recordingController.isRecording) {
                            updateNotification("Recording in progress")
                        }
                    }
                }
            }
            .launchIn(this)
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
            .setSmallIcon(R.drawable.ic_play)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_pause,
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
            AppLogger.w(TAG, "Failed to update notification", e)
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
            // Ensure any previous socket is fully released
            delay(100)
            // Find an available port starting from the preferred port
            actualServerPort = try {
                if (NetworkUtils.isPortAvailable(SERVER_PORT)) {
                    SERVER_PORT
                } else {
                    AppLogger.w(TAG, "Preferred port $SERVER_PORT is in use, finding alternative")
                    NetworkUtils.findAvailablePort(SERVER_PORT + 1, 10)
                }
            } catch (e: IllegalStateException) {
                AppLogger.e(TAG, "Could not find available port starting from $SERVER_PORT", e)
                throw e
            }
            serverSocket = ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress(actualServerPort))
            }
            isServerRunning.set(true)
            structuredLogger.logServerEvent(
                "server_socket_started",
                mapOf("port" to actualServerPort, "preferred_port" to SERVER_PORT)
            )
            Log.i(
                TAG,
                "Server socket bound to port $actualServerPort${if (actualServerPort != SERVER_PORT) " (preferred port $SERVER_PORT was in use)" else ""}"
            )
            if (FeatureFlags.MDNS_ENABLE) {
                registerNsdService()
            }
            if (!isServiceForeground()) {
                startForegroundWithType(
                    NOTIFICATION_ID,
                    createServerNotification("Server listening for PC connections on port $actualServerPort")
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
                            mapOf(
                                "client_address" to (clientSocket.inetAddress.hostAddress
                                    ?: "unknown")
                            )
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
        } catch (e: java.net.BindException) {
            structuredLogger.logServerEvent(
                "server_socket_bind_failed",
                mapOf(
                    "port" to SERVER_PORT,
                    "error" to (e.message ?: "Port already in use")
                )
            )
            AppLogger.e(TAG, "Failed to bind to port $SERVER_PORT - address already in use", e)
            isServerRunning.set(false)
            throw e
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
            serverSocket?.let { socket ->
                if (!socket.isClosed) {
                    socket.close()
                }
            }
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
        serverJob = launch(Dispatchers.IO) {
            while (isServerRunning.get() && isActive) {
                try {
                    val clientSocket = serverSocket?.accept()
                    if (clientSocket != null && isServerRunning.get()) {
                        val clientId =
                            "${clientSocket.inetAddress.hostAddress}:${clientSocket.port}"
                        AppLogger.i(TAG, "PC client connected: $clientId")
                        handleNewClientConnection(clientSocket, clientId)
                        withContext(Dispatchers.Main) {
                            updateNotification("Connected PCs: ${activeConnections.size}")
                        }
                    }
                } catch (e: SocketException) {
                    if (isServerRunning.get()) {
                        AppLogger.w(TAG, "Server socket accept error", e)
                        delay(1000)
                    }
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Unexpected error in accept loop", e)
                    if (isServerRunning.get()) {
                        delay(5000)
                    }
                }
            }
            AppLogger.i(TAG, "Accept loop terminated")
        }
    }
    private suspend fun handleNewClientConnection(clientSocket: Socket, clientId: String) {
        try {
            clientSocket.soTimeout = 30000
            val inputStream = DataInputStream(clientSocket.getInputStream())
            val outputStream = DataOutputStream(clientSocket.getOutputStream())
            val clientJob = launch(Dispatchers.IO) {
                try {
                    handleClientMessages(clientId, inputStream, outputStream)
                } catch (e: Exception) {
                    AppLogger.w(TAG, "Client $clientId handler error", e)
                } finally {
                    activeConnections.remove(clientId)
                    try {
                        clientSocket.close()
                    } catch (e: Exception) {
                        AppLogger.w(TAG, "Error closing client socket", e)
                    }
                    AppLogger.i(TAG, "PC client disconnected: $clientId")
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
            AppLogger.e(TAG, "Error setting up client connection", e)
            try {
                clientSocket.close()
            } catch (closeEx: Exception) {
                AppLogger.w(TAG, "Error closing failed client socket", closeEx)
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
                    AppLogger.w(TAG, "Message too large from $clientId: $messageLength bytes")
                    break
                }
                val messageData = ByteArray(messageLength)
                inputStream.readFully(messageData)
                val message = JSONObject(String(messageData, Charsets.UTF_8))
                processClientMessage(clientId, message, outputStream)
            } catch (e: SocketTimeoutException) {
                sendKeepAlive(outputStream)
            } catch (e: EOFException) {
                AppLogger.i(TAG, "Client $clientId disconnected normally")
                break
            } catch (e: Exception) {
                AppLogger.w(TAG, "Error handling message from $clientId", e)
                break
            }
        }
    }
    private suspend fun initializeNetworkClient(): Boolean {
        return try {
            val success = networkClient.initialize()
            if (success) {
                setupNetworkCommandHandlers()
                AppLogger.i(TAG, "Network client initialized successfully")
            } else {
                AppLogger.w(TAG, "Network client initialization failed")
            }
            success
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error initializing network client", e)
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
            AppLogger.w(TAG, "Network client message handlers setup failed: ${e.message}")
        }
    }
    fun getNetworkClient(): NetworkClient = networkClient
    private fun setupNetworkServer() {
        launch(Dispatchers.IO) {
            try {
                val serverStarted = connectionManager.startServer()
                if (serverStarted) {
                    AppLogger.i(TAG, "Network server started automatically, listening on port 8080")
                    withContext(Dispatchers.Main) {
                        updateNotification("Listening for PC Controller on port 8080")
                    }
                } else {
                    AppLogger.e(TAG, "Failed to start network server automatically")
                    withContext(Dispatchers.Main) {
                        updateNotification("Network server failed to start")
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error setting up network server", e)
                withContext(Dispatchers.Main) {
                    updateNotification("Network server error: ${e.message}")
                }
            }
        }
        launch(Dispatchers.IO) {
            connectionManager.connectionState.collect { state ->
                when (state) {
                    NetworkConnectionManager.ConnectionState.CONNECTED -> {
                        isConnectedToPC = true
                        AppLogger.i(TAG, "PC Controller connected to network server")
                        withContext(Dispatchers.Main) {
                            updateNotification("PC Controller connected")
                        }
                        previewStreamer.startStreaming()
                        previewDataAdapter.startDataPolling()
                    }
                    NetworkConnectionManager.ConnectionState.DISCONNECTED -> {
                        isConnectedToPC = false
                        AppLogger.i(TAG, "PC Controller disconnected, still listening on port 8080")
                        withContext(Dispatchers.Main) {
                            updateNotification("Listening for PC Controller on port 8080")
                        }
                        previewDataAdapter.stopDataPolling()
                        previewStreamer.stopStreaming()
                    }
                    NetworkConnectionManager.ConnectionState.ERROR -> {
                        isConnectedToPC = false
                        AppLogger.e(TAG, "Network connection error")
                        withContext(Dispatchers.Main) {
                            updateNotification("Network connection error")
                        }
                    }
                    NetworkConnectionManager.ConnectionState.RECONNECTING -> {
                        isConnectedToPC = false
                        AppLogger.i(TAG, "Attempting to reconnect to PC Controller")
                        withContext(Dispatchers.Main) {
                            updateNotification("Reconnecting to PC Controller...")
                        }
                    }
                    NetworkConnectionManager.ConnectionState.CONNECTING -> {
                        AppLogger.i(TAG, "Connecting to PC Controller...")
                        withContext(Dispatchers.Main) {
                            updateNotification("Connecting...")
                        }
                    }
                }
            }
        }
        launch(Dispatchers.IO) {
            networkServer.messageFlow.collect { message ->
                handleProtocolMessage(message)
            }
        }
        // Set up protocol handler with command callbacks
        protocolHandler.setCommandHandler(object : ProtocolHandler.CommandHandler {
            override suspend fun onStartRecording(sessionId: String): ProtocolHandler.CommandResult {
                return try {
                    AppLogger.i(TAG, "Remote start recording command received for session: $sessionId")
                    // Use REMOTE_PC trigger source for session orchestration
                    val success = startRecordingSessionWithTrigger(
                        sessionId,
                        TriggerSource.REMOTE_PC
                    )
                    if (success) {
                        ProtocolHandler.CommandResult(
                            success = true,
                            message = "Recording started via PC command",
                            data = mapOf(
                                "start_time" to System.currentTimeMillis().toString(),
                                "session_id" to sessionId,
                                "trigger_source" to "REMOTE_PC"
                            )
                        )
                    } else {
                        ProtocolHandler.CommandResult(
                            success = false,
                            message = "Recording start failed - may already be recording or prerequisites not met"
                        )
                    }
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Failed to start recording via PC command", e)
                    return ProtocolHandler.CommandResult(
                        false,
                        "Start recording failed: ${e.message}"
                    )
                }
            }
            override suspend fun onStopRecording(sessionId: String): ProtocolHandler.CommandResult {
                return try {
                    AppLogger.i(TAG, "Remote stop recording command received for session: $sessionId")
                    // Use REMOTE_PC trigger source for session orchestration  
                    val success =
                        stopRecordingSessionWithTrigger(TriggerSource.REMOTE_PC)
                    if (success) {
                        ProtocolHandler.CommandResult(
                            success = true,
                            message = "Recording stopped via PC command",
                            data = mapOf(
                                "stop_time" to System.currentTimeMillis().toString(),
                                "session_id" to sessionId,
                                "trigger_source" to "REMOTE_PC"
                            )
                        )
                    } else {
                        ProtocolHandler.CommandResult(
                            success = false,
                            message = "Stop recording failed - may not be recording"
                        )
                    }
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Failed to stop recording via PC command", e)
                    return ProtocolHandler.CommandResult(
                        false,
                        "Stop recording failed: ${e.message}"
                    )
                }
            }
            override suspend fun onSyncRequest(pcTimestamp: Long): ProtocolHandler.SyncResult {
                return try {
                    val timeManager = mpdc4gsr.core.data.utils.TimeManager.getInstance(this@RecordingService)
                    val phoneTimestamp =
                        timeManager.getCurrentTimestampNs() / 1_000_000 // Convert to ms
                    AppLogger.d(TAG, "Time sync request: PC=$pcTimestamp, Phone=$phoneTimestamp")
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
                    AppLogger.e(TAG, "Time sync failed", e)
                    ProtocolHandler.SyncResult(false)
                }
            }
        })
    }
    private fun connectToPC(ipAddress: String, port: Int) {
        launch {
            try {
                AppLogger.i(TAG, "Attempting connection to PC Controller at $ipAddress:$port")
                val serverStarted = connectionManager.startServer()
                if (serverStarted) {
                    AppLogger.i(TAG, "Network server started, ready for PC Controller connection")
                    updateNotification("Ready for PC Controller connection")
                } else {
                    AppLogger.e(TAG, "Failed to start network server")
                    updateNotification("Failed to start network server")
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error during PC connection attempt", e)
                updateNotification("Connection error: ${e.message}")
            }
        }
    }
    private fun disconnectFromPC() {
        launch {
            try {
                if (isNetworkInitialized) {
                    networkClient.disconnect()
                }
                connectionManager.stopServer()
                isConnectedToPC = false
                AppLogger.i(TAG, "Disconnected from PC Controller")
                updateNotification("Disconnected from PC Controller")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error disconnecting from PC", e)
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
        launch {
            try {
                if (isNetworkInitialized) {
                    startNetworkDiscovery()
                } else {
                    AppLogger.i(TAG, "PC Controller discovery requested")
                    updateNotification("Searching for PC Controller...")
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error starting PC discovery", e)
            }
        }
    }
    private suspend fun handleProtocolMessage(message: mpdc4gsr.feature.network.data.Protocol.ProtocolMessage) {
        try {
            val response = protocolHandler.processMessage(message)
            if (response != null) {
                networkServer.sendMessage(response)
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error processing protocol message: ${message.type}", e)
            val errorResponse = mpdc4gsr.feature.network.data.Protocol.createErrorMessage(
                message.type,
                mpdc4gsr.feature.network.data.Protocol.ERR_FAIL,
                "Processing error: ${e.message}"
            )
            networkServer.sendMessage(errorResponse)
        }
    }
    private suspend fun handlePCCommand(message: JSONObject) {
        try {
            val messageType = message.optString("message_type")
            AppLogger.i(TAG, "Processing PC command: $messageType")
            when (messageType) {
                "enhanced_device_registration" -> {
                    AppLogger.i(TAG, "PC Controller device registration request")
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
                    AppLogger.i(TAG, "Received remote stop command from PC")
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
                        AppLogger.i(TAG, "Received remote sync marker from PC: $markerType")
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
                    AppLogger.d(TAG, "Received ping from PC Controller")
                    sendResponseToPC("pong", JSONObject().apply {
                        put("timestamp_ns", System.nanoTime())
                    })
                }
                "status_request" -> {
                    AppLogger.d(TAG, "PC Controller requested status")
                    sendStatusToPC()
                }
                "start_preview_streaming" -> {
                    AppLogger.i(TAG, "PC Controller requested to start preview streaming")
                    launch {
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
                    AppLogger.i(TAG, "PC Controller requested to stop preview streaming")
                    launch {
                        previewStreamer.stopStreaming()
                        sendResponseToPC("preview_streaming_response", JSONObject().apply {
                            put("status", "stopped")
                            put("message", "Preview streaming stopped")
                        })
                    }
                }
                "configure_preview_streaming" -> {
                    AppLogger.i(TAG, "PC Controller requested to configure preview streaming")
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
                    AppLogger.w(TAG, "Unknown command from PC Controller: $messageType")
                    sendResponseToPC("error", JSONObject().apply {
                        put("message", "Unknown command: $messageType")
                    })
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error handling PC command", e)
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
            .setSmallIcon(R.drawable.ic_info)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_pause,
                "Stop Server",
                stopPendingIntent
            )
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }
    fun startNetworkDiscovery() {
        launch {
            try {
                networkClient.startDiscovery { success ->
                    if (success) {
                        AppLogger.i(TAG, "Network discovery started successfully")
                    } else {
                        AppLogger.w(TAG, "Network discovery failed to start")
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error starting network discovery", e)
            }
        }
    }
    fun handleStartRecordingCommand(message: JSONObject) {
        launch {
            try {
                val sessionId =
                    message.optString("session_id", "session_${System.currentTimeMillis()}")
                val sessionDirectory = "/storage/emulated/0/IRCamera_Sessions/$sessionId"
                AppLogger.i(TAG, "Received start recording command from PC Controller")
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
                AppLogger.e(TAG, "Error handling start recording command", e)
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
            AppLogger.i(TAG, "NSD service already registered")
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
                        AppLogger.i(TAG, "NSD service registered: ${registeredServiceInfo?.serviceName}")
                        nsdServiceInfo = registeredServiceInfo
                        isServiceRegistered = true
                    }
                    override fun onRegistrationFailed(
                        failedServiceInfo: NsdServiceInfo?,
                        errorCode: Int
                    ) {
                        AppLogger.e(TAG, "NSD service registration failed: $errorCode")
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
                        AppLogger.e(TAG, "NSD service unregistration failed: $errorCode")
                    }
                })
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error registering NSD service", e)
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
                    AppLogger.i(TAG, "NSD service unregistered successfully")
                    isServiceRegistered = false
                    nsdServiceInfo = null
                }
                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                    AppLogger.e(TAG, "NSD service unregistration failed: $errorCode")
                }
            })
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error unregistering NSD service", e)
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
            AppLogger.d(TAG, "Sent response to PC: $messageType")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error sending response to PC", e)
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
            AppLogger.i(TAG, "Status sent to PC Controller")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error sending status to PC", e)
        }
    }
    private fun handleQueryStatusCommand(message: JSONObject) {
        try {
            AppLogger.d(TAG, "Handling query status command")
            launch {
                sendStatusToPC()
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error handling query status command", e)
        }
    }
    private fun handleSyncFlashCommand(message: JSONObject) {
        try {
            val durationMs = message.optInt("duration_ms", 100)
            AppLogger.d(TAG, "Handling sync flash command: ${durationMs}ms")
            addSyncMarker("pc_sync_flash", System.nanoTime())
            launch {
                sendResponseToPC("sync_flash_response", JSONObject().apply {
                    put("status", "completed")
                    put("duration_ms", durationMs)
                })
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error handling sync flash command", e)
        }
    }
    private fun handleQueryCapabilitiesCommand(message: JSONObject) {
        try {
            AppLogger.d(TAG, "Handling query capabilities command")
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
            launch {
                sendResponseToPC("capabilities_response", capabilities)
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error handling query capabilities command", e)
        }
    }
    private fun handleStopRecordingCommand(message: JSONObject) {
        try {
            AppLogger.d(TAG, "Handling stop recording command")
            launch {
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
            AppLogger.e(TAG, "Error handling stop recording command", e)
        }
    }
    // New client-side PC connection methods
    private fun connectToPCClient(ipAddress: String, port: Int) {
        launch {
            try {
                AppLogger.i(TAG, "Connecting to PC server as client at $ipAddress:$port")
                updateNotification("Connecting to PC server...")
                val success = networkManager.connectWifi(ipAddress, port)
                if (success) {
                    AppLogger.i(TAG, "Successfully connected to PC server as client")
                    updateNotification("Connected to PC server")
                    isConnectedToPC = true
                } else {
                    AppLogger.e(TAG, "Failed to connect to PC server as client")
                    updateNotification("Failed to connect to PC server")
                    isConnectedToPC = false
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error during PC client connection", e)
                updateNotification("Connection error: ${e.message}")
                isConnectedToPC = false
            }
        }
    }
    private fun connectToPCBluetooth(bluetoothDevice: android.bluetooth.BluetoothDevice) {
        launch {
            try {
                AppLogger.i(TAG, "Connecting to PC via Bluetooth: ${bluetoothDevice.name}")
                updateNotification("Connecting to PC via Bluetooth...")
                val success = networkManager.connectBluetooth(bluetoothDevice)
                if (success) {
                    AppLogger.i(TAG, "Successfully connected to PC via Bluetooth")
                    updateNotification("Connected to PC via Bluetooth")
                    isConnectedToPC = true
                } else {
                    AppLogger.e(TAG, "Failed to connect to PC via Bluetooth")
                    updateNotification("Failed to connect via Bluetooth")
                    isConnectedToPC = false
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error during Bluetooth connection", e)
                updateNotification("Bluetooth connection error: ${e.message}")
                isConnectedToPC = false
            }
        }
    }
    private fun disconnectFromPCClient() {
        launch {
            try {
                AppLogger.i(TAG, "Disconnecting from PC server")
                updateNotification("Disconnecting from PC...")
                networkManager.disconnect()
                isConnectedToPC = false
                AppLogger.i(TAG, "Disconnected from PC server")
                updateNotification("Disconnected from PC server")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error during disconnect", e)
                updateNotification("Disconnect error: ${e.message}")
            }
        }
    }
}
