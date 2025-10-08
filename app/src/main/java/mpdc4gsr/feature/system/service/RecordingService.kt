package mpdc4gsr.feature.system.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
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
import mpdc4gsr.core.CrashRecoveryManager
import mpdc4gsr.core.CrashSafeSupervisor
import mpdc4gsr.core.monitoring.StructuredLogger
import mpdc4gsr.core.ui.PermissionManager
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
                return (
                    val syncInitMessage = Protocol.createSyncInitMessage()
                    val sent = networkServer.sendMessage(syncInitMessage)
                    if (sent) {
                    } else {
                    }
                    sent
                )
                    false
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
                    // Check for crashed sessions on startup using session orchestration crash recovery
                    checkForCrashedSessionsOnStartup()
                    val sensorsSuccess = recordingController.initializeSensors()
                    val networkSuccess = initializeNetworkClient()
                    isInitialized = sensorsSuccess
                    isNetworkInitialized = networkSuccess
                    if (sensorsSuccess) {
                            TAG,
                            "Recording service initialized successfully with ${recordingController.getAvailableSensors().size} sensors"
                        )
                        setupStatusMonitoring()
                        setupNetworkServer()
                        if (FeatureFlags.MDNS_ENABLE) {
                            startServerSocket()
                        }
                        if (networkSuccess) {
                            startNetworkDiscovery()
                        } else {
                                TAG,
                                "Network client initialization failed - running in server-only mode"
                            )
                        }
                    } else {
                        stopSelf()
                    }
                    stopSelf()
                }
            }
        }
    }

    private fun initializePhase0Baseline() {
            FeatureFlags.initialize(this)
            structuredLogger = StructuredLogger.getInstance(this)
            crashSafeSupervisor = CrashSafeSupervisor.getInstance(this)
            crashSafeSupervisor.initialize()
            // Initialize TimeSyncManager (optional due to compilation issues)
            timeSyncManager = (
                TimeSyncManager(this)
                null
            }
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
                    manager.cleanup()
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
            // Cancel all coroutines launched in this service's scope
            serviceJob.cancel()
        }
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
            return
        }
        launch {
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
                startForegroundWithType(
                    NOTIFICATION_ID,
                    createRecordingNotification("Starting recording session..."),
                    forRecording = true
                )
                // Perform session start sync
                launch {
                        timeSyncManager?.performSessionStartSync()
                    }
                }
                val success = recordingController.startSession(sessionDirectory)
                if (success) {
                    val activeSensors = recordingController.getActiveSensorCount()
                    val totalSensors = recordingController.getAvailableSensors().size
                        TAG,
                        "Recording session started successfully with $activeSensors/$totalSensors sensors"
                    )
                    updateNotification("Recording: $activeSensors sensors active")
                } else {
                    updateNotification("Recording failed to start")
                    stopRecordingSession()
                }
                updateNotification("Recording error occurred")
                stopRecordingSession()
            }
        }
    }

    private fun stopRecordingSession() {
        launch {
                updateNotification("Stopping recording session...")
                val success = recordingController.stopSession()
                if (success) {
                    val sessionDuration = if (recordingStartTime > 0) {
                        (System.nanoTime() - recordingStartTime) / 1_000_000_000.0
                    } else 0.0
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
                    delay(2000)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    } else {
                        @Suppress("DEPRECATION")
                        stopForeground(true)
                    }
                } else {
                    updateNotification("Recording stop failed")
                }
                currentSessionDirectory = null
                recordingStartTime = 0
                // Finalize TimeSyncManager session
                    timeSyncManager?.finalizeSession()
                }
                if (!isServerRunning.get()) {
                    stopSelf()
                }
                updateNotification("Recording stop error")
            }
        }
    }

    // Enhanced recording methods with trigger source support for session orchestration
    private suspend fun startRecordingSessionWithTrigger(
        sessionDirectory: String,
        triggerSource: TriggerSource
    ): Boolean {
        if (!isInitialized) {
            return false
        }
        return (
            val sessionDir = File(sessionDirectory)
            if (!sessionDir.exists()) {
                sessionDir.mkdirs()
            }
            currentSessionDirectory = sessionDirectory
            recordingStartTime = System.nanoTime()
            // Initialize TimeSyncManager for this session
            timeSyncManager?.initializeSession(sessionDirectory)
            timeSyncManager?.setPeriodicSyncEnabled(true)
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
            } else {
                updateNotification("Recording start failed")
                currentSessionDirectory = null
                recordingStartTime = 0
            }
            success
        )
            updateNotification("Recording start error")
            currentSessionDirectory = null
            recordingStartTime = 0
            false
        }
    }

    private suspend fun stopRecordingSessionWithTrigger(triggerSource: TriggerSource): Boolean {
        return (
            updateNotification("Stopping recording session...")
            val success = recordingController.stopRecording(triggerSource = triggerSource)
            if (success) {
                val sessionDuration = if (recordingStartTime > 0) {
                    (System.nanoTime() - recordingStartTime) / 1_000_000_000.0
                } else 0.0
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
                delay(2000)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
            } else {
                updateNotification("Recording stop failed")
            }
            currentSessionDirectory = null
            recordingStartTime = 0
            // Finalize TimeSyncManager session
                timeSyncManager?.finalizeSession()
            }
            if (!isServerRunning.get()) {
                stopSelf()
            }
            success
            updateNotification("Recording stop error")
            false
        }
    }

    // Session manifest saving
    private fun saveSessionManifest(manifest: SessionManifest) {
            currentSessionDirectory?.let { sessionDir ->
                val manifestFile = File(sessionDir, "session_manifest.json")
                val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
                manifestFile.writeText(gson.toJson(manifest))
            }
        }
    }

    // Crash recovery integration for session orchestration
    private suspend fun checkForCrashedSessionsOnStartup() {
            val crashRecoveryResult = crashRecoveryManager.checkForCrashedSessions()
            if (crashRecoveryResult.hasCrashedSession) {
                val recoveredSession = crashRecoveryResult.recoveredSession!!
                // Perform recovery
                val recoveryResult = crashRecoveryManager.recoverCrashedSession(recoveredSession)
                if (recoveryResult.success) {
                        TAG,
                        "Successfully recovered crashed session: ${recoveredSession.sessionId}"
                    )
                } else {
                }
            } else {
            }
        }
    }

    private fun addSyncMarker(markerType: String, timestampNs: Long) {
        launch {
                recordingController.addSyncMarker(markerType, timestampNs)
                if (recordingController.isRecording) {
                    val originalText = "Recording in progress"
                    updateNotification("Sync marker: $markerType")
                    delay(1000)
                    updateNotification(originalText)
                }
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
            if (isServiceForeground()) {
                val notification = when {
                    recordingController.isRecording -> createRecordingNotification(contentText)
                    isServerRunning.get() -> createServerNotification(contentText)
                    else -> null
                }
                notification?.let { notificationManager.notify(NOTIFICATION_ID, it) }
            }
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
            // Ensure any previous socket is fully released
            delay(100)
            // Find an available port starting from the preferred port
            actualServerPort = (
                if (NetworkUtils.isPortAvailable(SERVER_PORT)) {
                    SERVER_PORT
                } else {
                    NetworkUtils.findAvailablePort(SERVER_PORT + 1, 10)
                }
            )
            }
            serverSocket = ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress(actualServerPort))
            }
            isServerRunning.set(true)
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
                    val clientSocket = withContext(Dispatchers.IO) {
                        serverSocket?.accept()
                    }
                    if (clientSocket != null && isServerRunning.get()) {
                        val clientId =
                            "${clientSocket.inetAddress.hostAddress}:${clientSocket.port}"
                        handleNewClientConnection(clientSocket, clientId)
                        withContext(Dispatchers.Main) {
                            updateNotification("Connected PCs: ${activeConnections.size}")
                        }
                    }
                    if (isServerRunning.get() && !stopToken.isStopRequested()) {
                        delay(1000)
                    }
                    if (isServerRunning.get() && !stopToken.isStopRequested()) {
                        delay(5000)
                    }
                }
            }
            isServerRunning.set(false)
            isServerRunning.set(false)
            cleanupServerSocket()
        }
    }

    private fun cleanupServerSocket() {
        isServerRunning.set(false)
        serverJob?.cancel()
        serverJob = null
        activeConnections.values.forEach { connection ->
                connection.job.cancel()
                connection.socket.close()
            }
        }
        activeConnections.clear()
            serverSocket?.let { socket ->
                if (!socket.isClosed) {
                    socket.close()
                }
            }
            serverSocket = null
        }
        unregisterNsdService()
    }

    private fun stopServerSocket() {
        if (!isServerRunning.get()) {
            return
        }
        crashSafeSupervisor.unregisterJob("server_socket")
        cleanupServerSocket()
    }

    private fun startAcceptLoop() {
        serverJob = launch(Dispatchers.IO) {
            while (isServerRunning.get() && isActive) {
                    val clientSocket = serverSocket?.accept()
                    if (clientSocket != null && isServerRunning.get()) {
                        val clientId =
                            "${clientSocket.inetAddress.hostAddress}:${clientSocket.port}"
                        handleNewClientConnection(clientSocket, clientId)
                        withContext(Dispatchers.Main) {
                            updateNotification("Connected PCs: ${activeConnections.size}")
                        }
                    }
                    if (isServerRunning.get()) {
                        delay(1000)
                    }
                    if (isServerRunning.get()) {
                        delay(5000)
                    }
                }
            }
        }
    }

    private suspend fun handleNewClientConnection(clientSocket: Socket, clientId: String) {
            clientSocket.soTimeout = 30000
            val inputStream = DataInputStream(clientSocket.getInputStream())
            val outputStream = DataOutputStream(clientSocket.getOutputStream())
            val clientJob = launch(Dispatchers.IO) {
                    handleClientMessages(clientId, inputStream, outputStream)
                    activeConnections.remove(clientId)
                        clientSocket.close()
                    }
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
                clientSocket.close()
            }
        }
    }

    private suspend fun handleClientMessages(
        clientId: String,
        inputStream: DataInputStream,
        outputStream: DataOutputStream
    ) {
        while (isServerRunning.get() && currentCoroutineContext().isActive) {
                val messageLength = inputStream.readInt()
                if (messageLength > 1024 * 1024) {
                    break
                }
                val messageData = ByteArray(messageLength)
                inputStream.readFully(messageData)
                val message = JSONObject(String(messageData, Charsets.UTF_8))
                processClientMessage(clientId, message, outputStream)
                sendKeepAlive(outputStream)
                break
                break
            }
        }
    }

    private suspend fun initializeNetworkClient(): Boolean {
        return (
            val success = networkClient.initialize()
            if (success) {
                setupNetworkCommandHandlers()
            } else {
            }
            success
        )
            false
        }
    }

    private fun setupNetworkCommandHandlers() {
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
        }
    }

    fun getNetworkClient(): NetworkClient = networkClient
    private fun setupNetworkServer() {
        launch(Dispatchers.IO) {
                val serverStarted = connectionManager.startServer()
                if (serverStarted) {
                    withContext(Dispatchers.Main) {
                        updateNotification("Listening for PC Controller on port 8080")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        updateNotification("Network server failed to start")
                    }
                }
                withContext(Dispatchers.Main) {
                }
            }
        }
        launch(Dispatchers.IO) {
            connectionManager.connectionState.collect { state ->
                when (state) {
                    NetworkConnectionManager.ConnectionState.CONNECTED -> {
                        isConnectedToPC = true
                        withContext(Dispatchers.Main) {
                            updateNotification("PC Controller connected")
                        }
                        previewStreamer.startStreaming()
                        previewDataAdapter.startDataPolling()
                    }

                    NetworkConnectionManager.ConnectionState.DISCONNECTED -> {
                        isConnectedToPC = false
                        withContext(Dispatchers.Main) {
                            updateNotification("Listening for PC Controller on port 8080")
                        }
                        previewDataAdapter.stopDataPolling()
                        previewStreamer.stopStreaming()
                    }

                    NetworkConnectionManager.ConnectionState.ERROR -> {
                        isConnectedToPC = false
                        withContext(Dispatchers.Main) {
                            updateNotification("Network connection error")
                        }
                    }

                    NetworkConnectionManager.ConnectionState.RECONNECTING -> {
                        isConnectedToPC = false
                        withContext(Dispatchers.Main) {
                            updateNotification("Reconnecting to PC Controller...")
                        }
                    }

                    NetworkConnectionManager.ConnectionState.CONNECTING -> {
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
                return (
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
                )
                    return ProtocolHandler.CommandResult(
                        false,
                    )
                }
            }

            override suspend fun onStopRecording(sessionId: String): ProtocolHandler.CommandResult {
                return (
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
                )
                    return ProtocolHandler.CommandResult(
                        false,
                    )
                }
            }

            override suspend fun onSyncRequest(pcTimestamp: Long): ProtocolHandler.SyncResult {
                return (
                    val timeManager = mpdc4gsr.core.data.utils.TimeManager.getInstance(this@RecordingService)
                    val phoneTimestamp =
                        timeManager.getCurrentTimestampNs() / 1_000_000 // Convert to ms
                    // Calculate offset for immediate response (PC time - Phone time)
                    val offsetNs = (pcTimestamp - phoneTimestamp) * 1_000_000 // Convert to ns
                    // Update TimeManager with the calculated offset if needed
                    // Note: This is a simplified sync. For full sync, TimeManager.synchronizeWithPC should be used
                    ProtocolHandler.SyncResult(
                        success = true,
                        phoneTimestamp = phoneTimestamp,
                        offsetNs = offsetNs
                    )
                    ProtocolHandler.SyncResult(false)
                }
            }
        })
    }

    private fun connectToPC(ipAddress: String, port: Int) {
        launch {
                val serverStarted = connectionManager.startServer()
                if (serverStarted) {
                    updateNotification("Ready for PC Controller connection")
                } else {
                    updateNotification("Failed to start network server")
                }
            }
        }
    }

    private fun disconnectFromPC() {
        launch {
                if (isNetworkInitialized) {
                    networkClient.disconnect()
                }
                connectionManager.stopServer()
                isConnectedToPC = false
                updateNotification("Disconnected from PC Controller")
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
        if (!ProtocolVersion.validateMessageVersion(message)) {
            val errorMsg = "Unsupported protocol version"
            sendError(outputStream, errorMsg)
            return
        }
            when (messageType) {
                "protocol_handshake" -> {
                    val handshakeResult = ProtocolVersion.validateHandshakeResponse(message)
                    if (handshakeResult.success) {
                        val responseMessage = ProtocolVersion.createHandshakeMessage(
                            android.provider.Settings.Secure.getString(
                                contentResolver,
                                android.provider.Settings.Secure.ANDROID_ID
                            )
                        )
                        sendMessage(outputStream, responseMessage)
                    } else {
                        sendError(outputStream, handshakeResult.error ?: "Handshake failed")
                    }
                }

                "session_start" -> {
                    val sessionId =
                        message.optString("session_id", "remote_${System.currentTimeMillis()}")
                    val sessionName = message.optString("session_name", "PC Remote Session")
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
                    sendStatusResponse(outputStream)
                }

                "heartbeat" -> {
                    val ackMessage = ProtocolVersion.createProtocolMessage(
                        "ack",
                        JSONObject().apply {
                            put("ack_for", "heartbeat")
                            put("result", "alive")
                        })
                    sendMessage(outputStream, ackMessage)
                }

                else -> {
                    val errorMessage = ProtocolVersion.createProtocolMessage(
                        "error",
                        JSONObject().apply {
                            put("error", "Unknown message type: $messageType")
                        })
                    sendMessage(outputStream, errorMessage)
                }
            }
            val errorMessage = ProtocolVersion.createProtocolMessage(
                "error",
                JSONObject().apply {
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
                if (!message.has("protocol_version")) {
                    message.put("protocol_version", ProtocolVersion.CURRENT_VERSION)
                }
                val messageData = message.toString().toByteArray(Charsets.UTF_8)
                outputStream.writeInt(messageData.size)
                outputStream.write(messageData)
                outputStream.flush()
            }
        }
    }

    private fun startPCDiscovery() {
        launch {
                if (isNetworkInitialized) {
                    startNetworkDiscovery()
                } else {
                    updateNotification("Searching for PC Controller...")
                }
            }
        }
    }

    private suspend fun handleProtocolMessage(message: mpdc4gsr.feature.network.data.Protocol.ProtocolMessage) {
            val response = protocolHandler.processMessage(message)
            if (response != null) {
                networkServer.sendMessage(response)
            }
            val errorResponse = mpdc4gsr.feature.network.data.Protocol.createErrorMessage(
                message.type,
                mpdc4gsr.feature.network.data.Protocol.ERR_FAIL,
            )
            networkServer.sendMessage(errorResponse)
        }
    }

    private suspend fun handlePCCommand(message: JSONObject) {
            val messageType = message.optString("message_type")
            when (messageType) {
                "enhanced_device_registration" -> {
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
                    sendResponseToPC("pong", JSONObject().apply {
                        put("timestamp_ns", System.nanoTime())
                    })
                }

                "status_request" -> {
                    sendStatusToPC()
                }

                "start_preview_streaming" -> {
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
                    launch {
                        previewStreamer.stopStreaming()
                        sendResponseToPC("preview_streaming_response", JSONObject().apply {
                            put("status", "stopped")
                            put("message", "Preview streaming stopped")
                        })
                    }
                }

                "configure_preview_streaming" -> {
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
                    sendResponseToPC("error", JSONObject().apply {
                        put("message", "Unknown command: $messageType")
                    })
                }
            }
            sendResponseToPC("error", JSONObject().apply {
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
                networkClient.startDiscovery { success ->
                    if (success) {
                    } else {
                    }
                }
            }
        }
    }

    fun handleStartRecordingCommand(message: JSONObject) {
        launch {
                val sessionId =
                    message.optString("session_id", "session_${System.currentTimeMillis()}")
                val sessionDirectory = "/storage/emulated/0/IRCamera_Sessions/$sessionId"
                startRecordingSession(sessionDirectory)
                val response = JSONObject().apply {
                    put("message_type", "response")
                    put("response_to", "start_recording")
                    put("status", "success")
                    put("session_id", sessionId)
                    put("message", "Recording started successfully")
                }
                networkClient.sendMessage(response)
                val response = JSONObject().apply {
                    put("message_type", "response")
                    put("response_to", "start_recording")
                    put("status", "error")
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
            return
        }
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
                        nsdServiceInfo = registeredServiceInfo
                        isServiceRegistered = true
                    }

                    override fun onRegistrationFailed(
                        failedServiceInfo: NsdServiceInfo?,
                        errorCode: Int
                    ) {
                    }

                    override fun onServiceUnregistered(unregisteredServiceInfo: NsdServiceInfo?) {
                            TAG,
                            "NSD service unregistered: ${unregisteredServiceInfo?.serviceName}"
                        )
                        isServiceRegistered = false
                    }

                    override fun onUnregistrationFailed(
                        failedServiceInfo: NsdServiceInfo?,
                        errorCode: Int
                    ) {
                    }
                })
        }
    }

    fun unregisterNsdService() {
        if (!isServiceRegistered || nsdServiceInfo == null) {
            return
        }
            nsdManager?.unregisterService(object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {}
                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {}
                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {
                    isServiceRegistered = false
                    nsdServiceInfo = null
                }

                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                }
            })
        }
    }

    private suspend fun sendResponseToPC(
        messageType: String,
        data: JSONObject = JSONObject()
    ) {
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
        }
    }

    private suspend fun sendStatusToPC() {
            val statusData = JSONObject().apply {
                put("is_recording", recordingController.isRecording)
                put("current_session", currentSessionDirectory ?: "")
                put("recording_start_time", recordingStartTime)
                put("service_initialized", isInitialized)
                put("network_server_running", networkServer.isRunning())
                put("pc_connected", isConnectedToPC)
            }
            sendResponseToPC("status_response", statusData)
        }
    }

    private fun handleQueryStatusCommand(message: JSONObject) {
            launch {
                sendStatusToPC()
            }
        }
    }

    private fun handleSyncFlashCommand(message: JSONObject) {
            val durationMs = message.optInt("duration_ms", 100)
            addSyncMarker("pc_sync_flash", System.nanoTime())
            launch {
                sendResponseToPC("sync_flash_response", JSONObject().apply {
                    put("status", "completed")
                    put("duration_ms", durationMs)
                })
            }
        }
    }

    private fun handleQueryCapabilitiesCommand(message: JSONObject) {
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
        }
    }

    private fun handleStopRecordingCommand(message: JSONObject) {
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
        }
    }

    // New client-side PC connection methods
    private fun connectToPCClient(ipAddress: String, port: Int) {
        launch {
                updateNotification("Connecting to PC server...")
                val success = networkManager.connectWifi(ipAddress, port)
                if (success) {
                    updateNotification("Connected to PC server")
                    isConnectedToPC = true
                } else {
                    updateNotification("Failed to connect to PC server")
                    isConnectedToPC = false
                }
                isConnectedToPC = false
            }
        }
    }

    private fun connectToPCBluetooth(bluetoothDevice: android.bluetooth.BluetoothDevice) {
        launch {
                updateNotification("Connecting to PC via Bluetooth...")
                val success = networkManager.connectBluetooth(bluetoothDevice)
                if (success) {
                    updateNotification("Connected to PC via Bluetooth")
                    isConnectedToPC = true
                } else {
                    updateNotification("Failed to connect via Bluetooth")
                    isConnectedToPC = false
                }
                isConnectedToPC = false
            }
        }
    }

    private fun disconnectFromPCClient() {
        launch {
                updateNotification("Disconnecting from PC...")
                networkManager.disconnect()
                isConnectedToPC = false
                updateNotification("Disconnected from PC server")
            }
        }
    }
}
