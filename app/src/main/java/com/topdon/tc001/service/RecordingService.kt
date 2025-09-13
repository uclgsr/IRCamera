package com.topdon.tc001.service

import android.app.*
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
import com.topdon.tc001.controller.RecordingController
import com.topdon.tc001.controller.RecordingState
import com.topdon.gsr.model.SessionInfo
import com.topdon.gsr.model.SyncMark
import com.topdon.tc001.network.EnhancedNetworkClient
import com.topdon.tc001.network.NetworkClient
import com.topdon.tc001.network.NetworkServer
import com.topdon.tc001.utils.TimeManager
import com.csl.irCamera.R
// Phase 0 baseline imports
import com.topdon.tc001.config.FeatureFlags
import com.topdon.tc001.config.ProtocolVersion
import com.topdon.tc001.logging.StructuredLogger
import com.topdon.tc001.supervisor.CrashSafeSupervisor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.*
import java.net.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Background service for multi-modal sensor recording with PC-to-Phone server socket support.
 * 
 * This service ensures continuous recording operation even when the app is in the background.
 * It manages the RecordingController and provides status updates through notifications.
 * 
 * Key Features:
 * - Foreground service for uninterrupted recording
 * - Persistent server socket for PC connections
 * - Multi-connection support with re-accept loop
 * - NSD service advertisement for discovery
 * - Real-time status notifications
 * - Automatic recovery from errors
 * - Integration with PC Controller communication
 * - Power management awareness
 * - Graceful shutdown and resource management
 * 
 * @author IRCamera Android Sensor Node (Spoke)
 */
class RecordingService : LifecycleService() {

    companion object {
        private const val TAG = "RecordingService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "recording_service_channel"
        
        // Server socket configuration
        private const val SERVER_PORT = 8080
        private const val SERVICE_TYPE = "_ircamera._tcp."
        private const val SERVICE_NAME = "IRCamera-Android"
        
        // Actions
        const val ACTION_START_RECORDING = "com.topdon.tc001.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.topdon.tc001.STOP_RECORDING"
        const val ACTION_ADD_SYNC_MARKER = "com.topdon.tc001.ADD_SYNC_MARKER"
<<<<<<< HEAD
        const val ACTION_START_SERVER = "com.topdon.tc001.START_SERVER"
        const val ACTION_STOP_SERVER = "com.topdon.tc001.STOP_SERVER"
=======
        const val ACTION_CONNECT_PC = "com.topdon.tc001.CONNECT_PC"
        const val ACTION_DISCONNECT_PC = "com.topdon.tc001.DISCONNECT_PC"
        const val ACTION_START_DISCOVERY = "com.topdon.tc001.START_DISCOVERY"
>>>>>>> dev
        
        // Extras
        const val EXTRA_SESSION_DIRECTORY = "session_directory"
        const val EXTRA_MARKER_TYPE = "marker_type"
        const val EXTRA_TIMESTAMP_NS = "timestamp_ns"
        const val EXTRA_PC_IP = "pc_ip"
        const val EXTRA_PC_PORT = "pc_port"
        
        /**
         * Start the recording service
         */
        fun startRecording(context: Context, sessionDirectory: String) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_START_RECORDING
                putExtra(EXTRA_SESSION_DIRECTORY, sessionDirectory)
            }
            context.startForegroundService(intent)
        }
        
        /**
         * Stop the recording service
         */
        fun stopRecording(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_STOP_RECORDING
            }
            context.startService(intent)
        }
        
        /**
         * Start server socket for PC connections
         */
        fun startServer(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_START_SERVER
            }
            context.startForegroundService(intent)
        }
        
        /**
         * Stop server socket
         */
        fun stopServer(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_STOP_SERVER
            }
            context.startService(intent)
        }
        
        /**
         * Add sync marker through service
         */
        fun addSyncMarker(context: Context, markerType: String, timestampNs: Long) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_ADD_SYNC_MARKER
                putExtra(EXTRA_MARKER_TYPE, markerType)
                putExtra(EXTRA_TIMESTAMP_NS, timestampNs)
            }
            context.startService(intent)
        }
        
        /**
         * Connect to PC Controller
         */
        fun connectToPC(context: Context, ipAddress: String, port: Int = 8080) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_CONNECT_PC
                putExtra(EXTRA_PC_IP, ipAddress)
                putExtra(EXTRA_PC_PORT, port)
            }
            context.startService(intent)
        }
        
        /**
         * Disconnect from PC Controller
         */
        fun disconnectFromPC(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_DISCONNECT_PC
            }
            context.startService(intent)
        }
        
        /**
         * Start PC Controller discovery
         */
        fun startDiscovery(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_START_DISCOVERY
            }
            context.startService(intent)
        }
    }

    // Service binding
    private val binder = RecordingServiceBinder()
    
    // Recording controller
    private lateinit var recordingController: RecordingController
    private var isInitialized = false
    
    // Network communication - both client and server capabilities
    private lateinit var networkClient: NetworkClient
    private lateinit var networkServer: NetworkServer
    private var isNetworkInitialized = false
    private var isConnectedToPC = false
    
    // Current session
    private var currentSessionDirectory: String? = null
    private var recordingStartTime: Long = 0
    
    // Notification manager
    private lateinit var notificationManager: NotificationManager
    
    // Server socket components
    private var serverSocket: ServerSocket? = null
    private var isServerRunning = AtomicBoolean(false)
    private var serverJob: Job? = null
    private val activeConnections = ConcurrentHashMap<String, ClientConnection>()
    
    // NSD components
    private var nsdManager: NsdManager? = null
    private var nsdServiceInfo: NsdServiceInfo? = null
    private var isServiceRegistered = false
    
    // Phase 0 baseline components
    private lateinit var structuredLogger: StructuredLogger
    private lateinit var crashSafeSupervisor: CrashSafeSupervisor
    
    /**
     * Represents an active client connection from PC
     */
    private data class ClientConnection(
        val socket: Socket,
        val clientId: String,
        val inputStream: DataInputStream,
        val outputStream: DataOutputStream,
        val job: Job
    )

    inner class RecordingServiceBinder : Binder() {
        fun getService(): RecordingService = this@RecordingService
        fun getRecordingController(): RecordingController = recordingController
        fun getNetworkServer(): NetworkServer = networkServer
        fun isConnectedToPC(): Boolean = isConnectedToPC
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "RecordingService created")
        
        // Initialize Phase 0 baseline components
        initializePhase0Baseline()
        
        // Initialize notification manager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        
        // Initialize NSD manager
        nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager
        
        // Initialize recording controller
        recordingController = RecordingController(this, this)
        
<<<<<<< HEAD
        // Initialize sensors under supervision
        crashSafeSupervisor.registerJob(
            id = "recording_service_init",
            name = "RecordingService Initialization",
            critical = true,
            restartable = false
        ) { stopToken ->
=======
        // Initialize both network client and server for maximum compatibility
        networkClient = NetworkClient(this)
        networkServer = NetworkServer(this, 8080)
        
        // Initialize sensors and dual network architecture
        lifecycleScope.launch {
>>>>>>> dev
            try {
                val sensorsSuccess = recordingController.initializeSensors()
                isInitialized = sensorsSuccess
                
<<<<<<< HEAD
                if (success) {
                    structuredLogger.log(
                        StructuredLogger.LogLevel.INFO,
                        "RecordingService",
                        "service_initialized"
                    )
                    setupStatusMonitoring()
                    
                    // Start server socket automatically if enabled
                    if (FeatureFlags.MDNS_ENABLE) {
                        startServerSocket()
=======
                val networkSuccess = initializeNetworkClient()
                isNetworkInitialized = networkSuccess
                
                if (sensorsSuccess) {
                    Log.i(TAG, "Recording service initialized successfully")
                    setupStatusMonitoring()
                    setupNetworkServer()
                    
                    if (networkSuccess) {
                        Log.i(TAG, "Network client initialized successfully")
                        startNetworkDiscovery()
                    } else {
                        Log.w(TAG, "Network client initialization failed - running in server-only mode")
>>>>>>> dev
                    }
                } else {
                    structuredLogger.log(
                        StructuredLogger.LogLevel.ERROR,
                        "RecordingService",
                        "initialization_failed"
                    )
                    stopSelf()
                }
            } catch (e: Exception) {
                structuredLogger.log(
                    StructuredLogger.LogLevel.ERROR,
                    "RecordingService",
                    "initialization_exception",
                    mapOf("error" to e.message)
                )
                stopSelf()
                throw e
            }
        }
    }
    
    /**
     * Initialize Phase 0 baseline components for the service
     */
    private fun initializePhase0Baseline() {
        try {
            // Initialize feature flags if not already done
            FeatureFlags.initialize(this)
            
            // Initialize structured logger
            structuredLogger = StructuredLogger.getInstance(this)
            
            // Initialize crash-safe supervisor
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
            
            ACTION_STOP_RECORDING -> {
                stopRecordingSession()
            }
            
            ACTION_START_SERVER -> {
                startServerSocket()
            }
            
            ACTION_STOP_SERVER -> {
                stopServerSocket()
            }
            
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
            
            ACTION_DISCONNECT_PC -> {
                disconnectFromPC()
            }
            
            ACTION_START_DISCOVERY -> {
                startPCDiscovery()
            }
        }
        
        return START_STICKY // Changed to STICKY to ensure server persistence
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
        
        lifecycleScope.launch {
            try {
<<<<<<< HEAD
                // Stop server socket first
                stopServerSocket()
                
                // Stop recording if active
                recordingController.cleanup()
                
                structuredLogger.log(
                    StructuredLogger.LogLevel.INFO,
                    "RecordingService",
                    "service_cleanup_completed"
                )
            } catch (e: Exception) {
                structuredLogger.log(
                    StructuredLogger.LogLevel.ERROR,
                    "RecordingService",
                    "service_cleanup_error",
                    mapOf("error" to e.message)
                )
            } finally {
                // Cleanup Phase 0 components
                try {
                    crashSafeSupervisor.shutdown()
                } catch (e: Exception) {
                    Log.e(TAG, "Error shutting down supervisor", e)
=======
                networkServer.stop()
                isConnectedToPC = false
                Log.i(TAG, "Network server stopped")
                updateNotification("Network server stopped")
                
                // Clean up network client resources if initialized
                if (isNetworkInitialized) {
                    networkClient.disconnect()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during service cleanup", e)
            } finally {
                try {
                    recordingController.cleanup()
                } catch (e: Exception) {
                    Log.e(TAG, "Error during recording controller cleanup", e)
>>>>>>> dev
                }
            }
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
            Log.e(TAG, "Service not initialized, cannot start recording")
            return
        }
        
        lifecycleScope.launch {
            try {
                // Create session directory
                val sessionDir = File(sessionDirectory)
                if (!sessionDir.exists()) {
                    sessionDir.mkdirs()
                }
                
                currentSessionDirectory = sessionDirectory
                recordingStartTime = System.nanoTime()
                
                // Start foreground service
                startForeground(NOTIFICATION_ID, createRecordingNotification("Starting recording..."))
                
                // Start recording
                val success = recordingController.startRecording(sessionDirectory)
                
                if (success) {
                    Log.i(TAG, "Recording session started: $sessionDirectory")
                    updateNotification("Recording in progress")
                } else {
                    Log.e(TAG, "Failed to start recording session")
                    updateNotification("Recording failed to start")
                    stopRecordingSession()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error starting recording session", e)
                updateNotification("Recording error occurred")
                stopRecordingSession()
            }
        }
    }

    private fun stopRecordingSession() {
        lifecycleScope.launch {
            try {
                updateNotification("Stopping recording...")
                
                val success = recordingController.stopRecording()
                
                if (success) {
                    val sessionDuration = if (recordingStartTime > 0) {
                        (System.nanoTime() - recordingStartTime) / 1_000_000_000.0
                    } else 0.0
                    
                    Log.i(TAG, "Recording session stopped (duration: ${sessionDuration}s)")
                    updateNotification("Recording completed (${String.format("%.1f", sessionDuration)}s)")
                    
                    // Stop foreground service after a brief delay to show completion message
                    kotlinx.coroutines.delay(2000)
                    stopForeground(true)
                    stopSelf()
                } else {
                    Log.e(TAG, "Failed to stop recording session cleanly")
                    updateNotification("Recording stop failed")
                }
                
                currentSessionDirectory = null
                recordingStartTime = 0
                
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recording session", e)
                updateNotification("Recording stop error")
            }
        }
    }

    private fun addSyncMarker(markerType: String, timestampNs: Long) {
        lifecycleScope.launch {
            try {
                recordingController.addSyncMarker(markerType, timestampNs)
                Log.i(TAG, "Sync marker added: $markerType")
                
                // Briefly update notification to show sync event
                val originalText = "Recording in progress"
                updateNotification("Sync marker: $markerType")
                kotlinx.coroutines.delay(1000)
                updateNotification(originalText)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error adding sync marker", e)
            }
        }
    }

    private fun setupStatusMonitoring() {
        // Monitor recording state changes
        recordingController.recordingStateFlow
            .onEach { state ->
                when (state) {
                    RecordingState.STARTING -> updateNotification("Starting sensors...")
                    RecordingState.RECORDING -> updateNotification("Recording in progress")
                    RecordingState.STOPPING -> updateNotification("Stopping sensors...")
                    RecordingState.STOPPED -> updateNotification("Recording stopped")
                    RecordingState.ERROR -> updateNotification("Recording error")
                }
            }
            .launchIn(lifecycleScope)
        
        // Monitor sensor status
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
        
        // Monitor errors
        recordingController.errorFlow
            .onEach { error ->
                Log.w(TAG, "Recording controller error: ${error.message}")
                
                if (!error.isRecoverable) {
                    updateNotification("Critical error: ${error.message}")
                    stopRecordingSession()
                } else {
                    // Show temporary error notification
                    updateNotification("Warning: ${error.message}")
                    kotlinx.coroutines.delay(3000)
                    updateNotification("Recording in progress")
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
            .setSmallIcon(android.R.drawable.ic_media_play) // Use system icon for recording
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_media_pause, // Use system stop icon
                "Stop",
                stopPendingIntent
            )
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun updateNotification(contentText: String) {
        try {
            val notification = createRecordingNotification(contentText)
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update notification", e)
        }
    }

    /**
     * Get the recording controller instance
     */
    fun getRecordingController(): RecordingController = recordingController

    /**
     * Check if service is initialized and ready
     */
    fun isInitialized(): Boolean = isInitialized

    }
    
    /**
     * Initialize network client and set up command handlers
     */
<<<<<<< HEAD
    fun getCurrentSession(): SessionInfo? {
        return currentSessionDirectory?.let { directory ->
            SessionInfo(
                sessionId = directory.substringAfterLast("/"),
                startTime = recordingStartTime
            )
        }
    }
    
    // ==================== SERVER SOCKET IMPLEMENTATION ====================
    
    /**
     * Start persistent server socket for PC connections with supervision
     */
    private fun startServerSocket() {
        if (isServerRunning.get()) {
            structuredLogger.logServerEvent("server_already_running", mapOf("port" to SERVER_PORT))
            return
        }
        
        // Register server socket under supervision
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
    
    /**
     * Run server socket under supervision
     */
    private suspend fun runServerSocketSupervised(stopToken: CrashSafeSupervisor.StopToken) {
        try {
            // Create server socket
            serverSocket = ServerSocket(SERVER_PORT)
            isServerRunning.set(true)
            
            structuredLogger.logServerEvent(
                "server_socket_started",
                mapOf("port" to SERVER_PORT)
            )
            
            // Register NSD service if enabled
            if (FeatureFlags.MDNS_ENABLE) {
                registerNsdService()
            }
            
            // Start as foreground service if not already
            if (!isServiceForeground()) {
                startForeground(NOTIFICATION_ID, createServerNotification("Server listening for PC connections"))
            }
            
            // Run accept loop
            while (!stopToken.isStopRequested() && isServerRunning.get()) {
                try {
                    val clientSocket = withContext(Dispatchers.IO) {
                        serverSocket?.accept()
                    }
                    
                    if (clientSocket != null && isServerRunning.get()) {
                        val clientId = "${clientSocket.inetAddress.hostAddress}:${clientSocket.port}"
                        
                        structuredLogger.logConnection(
                            "pc_client_connected",
                            clientId,
                            mapOf("client_address" to clientSocket.inetAddress.hostAddress)
                        )
                        
                        // Handle client connection
                        handleNewClientConnection(clientSocket, clientId)
                        
                        // Update notification
                        withContext(Dispatchers.Main) {
                            updateNotification("Connected PCs: ${activeConnections.size}")
                        }
                    }
                } catch (e: SocketException) {
                    if (isServerRunning.get() && !stopToken.isStopRequested()) {
                        structuredLogger.logServerEvent(
                            "accept_socket_error",
                            mapOf("error" to e.message)
                        )
                        delay(1000)
                    }
                } catch (e: Exception) {
                    structuredLogger.logServerEvent(
                        "accept_unexpected_error", 
                        mapOf("error" to e.message)
                    )
                    if (isServerRunning.get() && !stopToken.isStopRequested()) {
                        delay(5000) // Longer delay for unexpected errors
                    }
                }
            }
            
        } catch (e: Exception) {
            structuredLogger.logServerEvent(
                "server_socket_failed",
                mapOf("error" to e.message)
            )
            isServerRunning.set(false)
            throw e
        } finally {
            // Cleanup
            structuredLogger.logServerEvent("server_socket_cleanup_started")
            cleanupServerSocket()
        }
    }
    
    /**
     * Clean up server socket resources
     */
    private fun cleanupServerSocket() {
        isServerRunning.set(false)
        
        // Cancel server job
        serverJob?.cancel()
        serverJob = null
        
        // Close all active connections
        activeConnections.values.forEach { connection ->
            try {
                connection.job.cancel()
                connection.socket.close()
            } catch (e: Exception) {
                structuredLogger.logConnection(
                    "connection_cleanup_error",
                    connection.clientId,
                    mapOf("error" to e.message)
                )
            }
        }
        activeConnections.clear()
        
        // Close server socket
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            structuredLogger.logServerEvent(
                "server_socket_close_error",
                mapOf("error" to e.message)
            )
        } finally {
            serverSocket = null
        }
        
        // Unregister NSD service
        unregisterNsdService()
        
        structuredLogger.logServerEvent("server_socket_cleanup_completed")
    }
    
    /**
     * Stop server socket and cleanup
     */
    private fun stopServerSocket() {
        if (!isServerRunning.get()) {
            structuredLogger.logServerEvent("server_not_running")
            return
        }
        
        structuredLogger.logServerEvent("server_socket_stop_requested")
        
        // Unregister from supervisor
        crashSafeSupervisor.unregisterJob("server_socket")
        
        cleanupServerSocket()
        
        structuredLogger.logServerEvent("server_socket_stopped")
    }
    
    /**
     * Start accept loop to handle multiple PC connections
     */
    private fun startAcceptLoop() {
        serverJob = lifecycleScope.launch(Dispatchers.IO) {
            while (isServerRunning.get() && !currentCoroutineContext().isActive.not()) {
                try {
                    val clientSocket = serverSocket?.accept()
                    if (clientSocket != null && isServerRunning.get()) {
                        val clientId = "${clientSocket.inetAddress.hostAddress}:${clientSocket.port}"
                        Log.i(TAG, "PC client connected: $clientId")
                        
                        // Handle client connection
                        handleNewClientConnection(clientSocket, clientId)
                        
                        // Update notification
                        withContext(Dispatchers.Main) {
                            updateNotification("Connected PCs: ${activeConnections.size}")
                        }
                    }
                } catch (e: SocketException) {
                    if (isServerRunning.get()) {
                        Log.w(TAG, "Server socket accept error", e)
                        // Brief delay before retry
                        delay(1000)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error in accept loop", e)
                    if (isServerRunning.get()) {
                        delay(5000) // Longer delay for unexpected errors
                    }
                }
            }
            Log.i(TAG, "Accept loop terminated")
        }
    }
    
    /**
     * Handle new PC client connection
     */
    private suspend fun handleNewClientConnection(clientSocket: Socket, clientId: String) {
        try {
            // Set socket timeout
            clientSocket.soTimeout = 30000 // 30 second timeout
            
            // Create streams
            val inputStream = DataInputStream(clientSocket.getInputStream())
            val outputStream = DataOutputStream(clientSocket.getOutputStream())
            
            // Create client handler job
            val clientJob = lifecycleScope.launch(Dispatchers.IO) {
                try {
                    handleClientMessages(clientId, inputStream, outputStream)
                } catch (e: Exception) {
                    Log.w(TAG, "Client $clientId handler error", e)
                } finally {
                    // Clean up connection
                    activeConnections.remove(clientId)
                    try {
                        clientSocket.close()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error closing client socket", e)
                    }
                    Log.i(TAG, "PC client disconnected: $clientId")
                    
                    // Update notification
                    withContext(Dispatchers.Main) {
                        updateNotification("Connected PCs: ${activeConnections.size}")
                    }
                }
            }
            
            // Store connection
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
            } catch (e: Exception) {
                Log.w(TAG, "Error closing failed client socket", e)
            }
        }
    }
    
    /**
     * Handle messages from PC client
     */
    private suspend fun handleClientMessages(
        clientId: String, 
        inputStream: DataInputStream, 
        outputStream: DataOutputStream
    ) {
        while (isServerRunning.get() && currentCoroutineContext().isActive) {
            try {
                // Read message length
                val messageLength = inputStream.readInt()
                if (messageLength > 1024 * 1024) { // 1MB limit
                    Log.w(TAG, "Message too large from $clientId: $messageLength bytes")
                    break
                }
                
                // Read message data
                val messageData = ByteArray(messageLength)
                inputStream.readFully(messageData)
                
                // Parse JSON message
                val message = JSONObject(String(messageData, Charsets.UTF_8))
                
                // Process message
                processClientMessage(clientId, message, outputStream)
                
            } catch (e: SocketTimeoutException) {
                // Send keepalive
                sendKeepAlive(outputStream)
            } catch (e: EOFException) {
                Log.i(TAG, "Client $clientId disconnected normally")
                break
            } catch (e: Exception) {
                Log.w(TAG, "Error handling message from $clientId", e)
                break
=======
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
    
    /**
     * Set up network command handlers for PC Controller communication
     */
    private fun setupNetworkCommandHandlers() {
        // Set up command handlers for legacy NetworkClient compatibility
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
    
    /**
     * Start network discovery to find PC Controllers
     */
    private fun startNetworkDiscovery() {
        lifecycleScope.launch {
            try {
                Log.i(TAG, "Network discovery started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting network discovery", e)
            }
        }
    }
    
    /**
     * Handle start recording command from PC Controller
     */
    private fun handleStartRecordingCommand(message: JSONObject) {
        lifecycleScope.launch {
            try {
                val sessionId = message.optString("session_id", "session_${System.currentTimeMillis()}")
                val sessionDirectory = "/storage/emulated/0/IRCamera_Sessions/$sessionId"
                
                Log.i(TAG, "Received start recording command from PC Controller")
                startRecordingSession(sessionDirectory)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling start recording command", e)
            }
        }
    }
    
    /**
     * Handle stop recording command from PC Controller
     */
    private fun handleStopRecordingCommand(message: JSONObject) {
        lifecycleScope.launch {
            try {
                Log.i(TAG, "Received stop recording command from PC Controller")
                stopRecordingSession()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling stop recording command", e)
            }
        }
    }
    
    /**
     * Handle sync flash command from PC Controller
     */
    private fun handleSyncFlashCommand(message: JSONObject) {
        lifecycleScope.launch {
            try {
                val durationMs = message.optInt("flash_duration_ms", 100)
                val timestamp = System.nanoTime()
                
                Log.i(TAG, "Received sync flash command from PC Controller")
                addSyncMarker("flash_sync", timestamp)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling sync flash command", e)
            }
        }
    }
    
    /**
     * Handle query capabilities command from PC Controller
     */
    private fun handleQueryCapabilitiesCommand(message: JSONObject) {
        lifecycleScope.launch {
            try {
                Log.i(TAG, "Received query capabilities command from PC Controller")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling query capabilities command", e)
            }
        }
    }
    
    /**
     * Handle query status command from PC Controller  
     */
    private fun handleQueryStatusCommand(message: JSONObject) {
        lifecycleScope.launch {
            try {
                Log.i(TAG, "Received query status command from PC Controller")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling query status command", e)
            }
        }
    }
    
    /**
     * Get network client instance (for testing/debugging)
     */
    fun getNetworkClient(): NetworkClient = networkClient
    
    // Network server setup and management
    
    private fun setupNetworkServer() {
        lifecycleScope.launch {
            try {
                // Start the TCP server immediately when service initializes
                val serverStarted = networkServer.start()
                
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
        
        // Monitor connection state
        lifecycleScope.launch {
            networkServer.connectionStateFlow.collect { connected ->
                isConnectedToPC = connected
                if (connected) {
                    Log.i(TAG, "PC Controller connected to network server")
                    updateNotification("PC Controller connected")
                } else {
                    Log.i(TAG, "PC Controller disconnected, still listening on port 8080")
                    updateNotification("Listening for PC Controller on port 8080")
                }
            }
        }
        
        // Monitor incoming messages from PC Controller
        lifecycleScope.launch {
            networkServer.messageFlow.collect { message ->
                handlePCCommand(message)
            }
        }
    }
    
    private fun connectToPC(ipAddress: String, port: Int) {
        // Dual approach: try client connection first, fallback to server mode
        lifecycleScope.launch {
            try {
                Log.i(TAG, "Attempting connection to PC Controller at $ipAddress:$port")
                
                // Ensure our server is running for PC to connect to us
                if (!networkServer.isRunning()) {
                    val started = networkServer.start()
                    if (started) {
                        Log.i(TAG, "Network server started, ready for PC Controller connection")
                        updateNotification("Ready for PC Controller connection")
                    } else {
                        Log.e(TAG, "Failed to start network server")
                        updateNotification("Failed to start network server")
                    }
                } else {
                    Log.i(TAG, "Network server already running, ready for PC Controller")
                    updateNotification("Network server ready")
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
                // Disconnect client if connected
                if (isNetworkInitialized) {
                    networkClient.disconnect()
                }
                
                // Stop the network server, which will disconnect any connected PC
                networkServer.stop()
                isConnectedToPC = false
                Log.i(TAG, "Disconnected from PC Controller")
                updateNotification("Disconnected from PC Controller") 
            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting from PC", e)
>>>>>>> dev
            }
        }
    }
    
<<<<<<< HEAD
    /**
     * Process message from PC client with protocol validation and structured logging
     */
    private suspend fun processClientMessage(
        clientId: String, 
        message: JSONObject, 
        outputStream: DataOutputStream
    ) {
        val messageType = message.optString("message_type")
        val messageId = message.optString("msg_id", "unknown")
        
        structuredLogger.logProtocolMessage(
            "message_received",
            messageId,
            clientId,
            mapOf(
                "message_type" to messageType,
                "protocol_version" to message.optString("protocol_version", "unknown")
            )
        )
        
        // Validate protocol version
        if (!ProtocolVersion.validateMessageVersion(message)) {
            val errorMsg = "Unsupported protocol version"
            structuredLogger.logProtocolMessage(
                "protocol_version_error", 
                messageId,
                clientId,
                mapOf("error" to errorMsg)
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
                            "handshake_success",
                            messageId,
                            clientId,
                            mapOf(
                                "negotiated_version" to (handshakeResult.negotiatedVersion ?: "unknown"),
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
                            "handshake_failed",
                            messageId,
                            clientId,
                            mapOf("error" to (handshakeResult.error ?: "unknown"))
                        )
                        sendError(outputStream, handshakeResult.error ?: "Handshake failed")
                    }
                }
                
                "session_start" -> {
                    val sessionId = message.optString("session_id", "remote_${System.currentTimeMillis()}")
                    val sessionName = message.optString("session_name", "PC Remote Session")
                    
                    structuredLogger.logSessionEvent(
                        "remote_session_start_request",
                        sessionId,
                        mapOf("session_name" to sessionName, "client_id" to clientId)
                    )
                    
                    // Create session directory
                    val baseDir = File(getExternalFilesDir(null), "recordings")
                    val sessionDir = File(baseDir, sessionId)
                    
                    withContext(Dispatchers.Main) {
                        startRecordingSession(sessionDir.absolutePath)
                    }
                    
                    // Send acknowledgment
                    val ackMessage = ProtocolVersion.createProtocolMessage("ack", JSONObject().apply {
                        put("ack_for", "session_start")
                        put("result", "Recording started")
                        put("session_id", sessionId)
                    })
                    sendMessage(outputStream, ackMessage)
                }
                
                "session_stop" -> {
                    structuredLogger.logSessionEvent(
                        "remote_session_stop_request",
                        "current",
                        mapOf("client_id" to clientId)
                    )
                    
                    withContext(Dispatchers.Main) {
                        stopRecordingSession()
                    }
                    
                    val ackMessage = ProtocolVersion.createProtocolMessage("ack", JSONObject().apply {
                        put("ack_for", "session_stop")
                        put("result", "Recording stopped")
                    })
                    sendMessage(outputStream, ackMessage)
                }
                
                "sync_flash" -> {
                    val durationMs = message.optInt("duration_ms", 100)
                    
                    structuredLogger.log(
                        StructuredLogger.LogLevel.INFO,
                        "SyncFlash",
                        "remote_sync_flash_request",
                        mapOf("duration_ms" to durationMs, "client_id" to clientId)
                    )
                    
                    withContext(Dispatchers.Main) {
                        performSyncFlash(durationMs)
                    }
                    
                    val ackMessage = ProtocolVersion.createProtocolMessage("ack", JSONObject().apply {
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
                        "heartbeat_received",
                        messageId,
                        clientId,
                        mapOf("timestamp" to message.optLong("timestamp", 0))
                    )
                    
                    val ackMessage = ProtocolVersion.createProtocolMessage("ack", JSONObject().apply {
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
                    
                    val errorMessage = ProtocolVersion.createProtocolMessage("error", JSONObject().apply {
                        put("error", "Unknown message type: $messageType")
                    })
                    sendMessage(outputStream, errorMessage)
                }
            }
        } catch (e: Exception) {
            structuredLogger.logProtocolMessage(
                "message_processing_error",
                messageId,
                clientId,
                mapOf(
                    "message_type" to messageType,
                    "error" to e.message
                )
            )
            
            val errorMessage = ProtocolVersion.createProtocolMessage("error", JSONObject().apply {
                put("error", "Error processing $messageType: ${e.message}")
            })
            sendMessage(outputStream, errorMessage)
        }
    }
    
    /**
     * Send acknowledgment to PC client
     */
    private suspend fun sendAck(outputStream: DataOutputStream, messageType: String, result: String) {
        val ackMessage = JSONObject().apply {
            put("message_type", "ack")
            put("ack_for", messageType)
            put("result", result)
            put("timestamp", System.currentTimeMillis())
        }
        sendMessage(outputStream, ackMessage)
    }
    
    /**
     * Send error to PC client
     */
    private suspend fun sendError(outputStream: DataOutputStream, error: String) {
        val errorMessage = JSONObject().apply {
            put("message_type", "error")
            put("error", error)
            put("timestamp", System.currentTimeMillis())
        }
        sendMessage(outputStream, errorMessage)
    }
    
    /**
     * Send status response to PC client
     */
    private suspend fun sendStatusResponse(outputStream: DataOutputStream) {
        val statusMessage = JSONObject().apply {
            put("message_type", "status_response")
            put("device_id", android.provider.Settings.Secure.getString(
                contentResolver, android.provider.Settings.Secure.ANDROID_ID))
            put("recording_active", recordingController.isRecording)
            put("connected_clients", activeConnections.size)
            put("server_running", isServerRunning.get())
            put("sensors_initialized", isInitialized)
            put("current_session", currentSessionDirectory)
            put("timestamp", System.currentTimeMillis())
        }
        sendMessage(outputStream, statusMessage)
    }
    
    /**
     * Send keepalive to PC client
     */
    private suspend fun sendKeepAlive(outputStream: DataOutputStream) {
        val keepAliveMessage = JSONObject().apply {
            put("message_type", "keepalive")
            put("timestamp", System.currentTimeMillis())
        }
        sendMessage(outputStream, keepAliveMessage)
    }
    
    /**
     * Send message to PC client with protocol versioning
     */
    private suspend fun sendMessage(outputStream: DataOutputStream, message: JSONObject) {
        withContext(Dispatchers.IO) {
            try {
                // Ensure protocol version is included
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
                    mapOf("error" to e.message)
                )
                throw e
=======
    private fun startPCDiscovery() {
        lifecycleScope.launch {
            try {
                if (isNetworkInitialized) {
                    // Use client discovery
                    startNetworkDiscovery()
                } else {
                    // TODO: Implement PC discovery using zeroconf/mDNS
                    // For now, log that discovery was requested
                    Log.i(TAG, "PC Controller discovery requested")
                    updateNotification("Searching for PC Controller...")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting PC discovery", e)
            }
        }
    }
    
    private suspend fun handlePCCommand(message: JSONObject) {
        try {
            val messageType = message.optString("message_type")
            Log.i(TAG, "Processing PC command: $messageType")
            
            when (messageType) {
                "enhanced_device_registration" -> {
                    Log.i(TAG, "PC Controller device registration request")
                    sendResponseToPC("enhanced_registration_ack", JSONObject().apply {
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
                        Log.i(TAG, "Received remote start command from PC for session: $sessionDirectory")
                        startRecordingSession(sessionDirectory)
                        sendResponseToPC("session_start_response", JSONObject().apply {
                            put("status", "started")
                            put("session_directory", sessionDirectory)
                        })
                    }
                }
                
                "session_stop_command" -> {
                    Log.i(TAG, "Received remote stop command from PC")
                    stopRecordingSession()
                    sendResponseToPC("session_stop_response", JSONObject().apply {
                        put("status", "stopped")
                    })
                }
                
                "sync_marker_command" -> {
                    val markerType = message.optString("marker_type")
                    val timestampNs = message.optLong("timestamp_ns", System.nanoTime())
                    if (markerType.isNotEmpty()) {
                        Log.i(TAG, "Received remote sync marker from PC: $markerType")
                        addSyncMarker(markerType, timestampNs)
                        sendResponseToPC("sync_marker_response", JSONObject().apply {
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
    
    private suspend fun sendResponseToPC(messageType: String, data: JSONObject = JSONObject()) {
        try {
            val response = JSONObject().apply {
                put("message_type", messageType)
                put("device_id", android.provider.Settings.Secure.getString(
                    contentResolver, android.provider.Settings.Secure.ANDROID_ID))
                put("timestamp_ns", System.nanoTime())
                // Merge additional data
                data.keys().forEach { key ->
                    put(key, data.get(key))
                }
            }
            
            networkServer.sendMessage(response)
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
    
<<<<<<< HEAD
    /**
     * Initialize network client and set up command handlers
     */
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
    
    /**
     * Set up network command handlers for PC Controller communication
     */
    private fun setupNetworkCommandHandlers() {
        // Set up command handlers
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
        
        // Set up network event listener for automatic connection handling
        networkClient.setEventListener(object : NetworkClient.NetworkEventListener {
            override fun onControllerDiscovered(controller: NetworkClient.ControllerInfo) {
                Log.i(TAG, "PC Controller discovered: ${controller.deviceName} at ${controller.ipAddress}")
                
                // Automatically attempt to connect to discovered PC Controllers
                lifecycleScope.launch {
                    networkClient.connectToController(controller.ipAddress, controller.port) { success ->
                        if (success) {
                            Log.i(TAG, "Successfully connected to PC Controller: ${controller.deviceName}")
                        } else {
                            Log.w(TAG, "Failed to connect to PC Controller: ${controller.deviceName}")
                        }
                    }
                }
            }
            
            override fun onConnected(controller: NetworkClient.ControllerInfo) {
                Log.i(TAG, "Connected to PC Controller: ${controller.deviceName}")
                updateNotification("Connected to PC Controller")
            }
            
            override fun onDisconnected(reason: String) {
                Log.i(TAG, "Disconnected from PC Controller: $reason")
                updateNotification("Disconnected from PC Controller") 
            }
            
            override fun onRemoteMeasurementRequest(sessionInfo: com.topdon.gsr.model.SessionInfo) {
                Log.i(TAG, "Received remote measurement request")
                // Handle remote measurement requests if needed
            }
            
            override fun onSyncFlash(durationMs: Int) {
                Log.i(TAG, "Sync flash request: ${durationMs}ms")
                // Handle sync flash requests
            }
            
            override fun onTimeSynchronized(offsetNanoseconds: Long) {
                Log.i(TAG, "Time synchronized with PC Controller (offset: ${offsetNanoseconds}ns)")
            }
            
            override fun onDataStreamingStarted() {
                Log.i(TAG, "Data streaming started")
            }
            
            override fun onDataStreamingStopped() {
                Log.i(TAG, "Data streaming stopped")
            }
            
            override fun onError(operation: String, error: String) {
                Log.e(TAG, "Network error in $operation: $error")
                updateNotification("Network error: $operation")
            }
        })
    }
    
    /**
     * Start network discovery to find PC Controllers
     */
    private fun startNetworkDiscovery() {
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
    
    /**
     * Handle start recording command from PC Controller
     */
    private fun handleStartRecordingCommand(message: JSONObject) {
        lifecycleScope.launch {
            try {
                val sessionId = message.optString("session_id", "session_${System.currentTimeMillis()}")
                val sessionDirectory = "/storage/emulated/0/IRCamera_Sessions/$sessionId"
                
                Log.i(TAG, "Received start recording command from PC Controller")
                startRecordingSession(sessionDirectory)
                
                // Send acknowledgment back to PC Controller
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
                
                // Send error response
                val response = JSONObject().apply {
                    put("message_type", "response")
                    put("response_to", "start_recording")
                    put("status", "error")
                    put("message", "Failed to start recording: ${e.message}")
                }
                networkClient.sendMessage(response)
>>>>>>> dev
            }
        }
    }
    
    /**
<<<<<<< HEAD
     * Perform sync flash for PC synchronization
     */
    private fun performSyncFlash(durationMs: Int) {
        // This would need to be handled by the main activity
        // For now, just add a sync marker
        addSyncMarker("pc_sync_flash", System.nanoTime())
    }
    
    // ==================== NSD SERVICE MANAGEMENT ====================
    
    /**
     * Register NSD service for PC discovery
     */
    private fun registerNsdService() {
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
            
            nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {
                    Log.i(TAG, "NSD service registered: ${serviceInfo?.serviceName}")
                    nsdServiceInfo = serviceInfo
                    isServiceRegistered = true
                }
                
                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                    Log.e(TAG, "NSD service registration failed: $errorCode")
                }
                
                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {
                    Log.i(TAG, "NSD service unregistered: ${serviceInfo?.serviceName}")
                    isServiceRegistered = false
                }
                
                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                    Log.e(TAG, "NSD service unregistration failed: $errorCode")
                }
            })
            
        } catch (e: Exception) {
            Log.e(TAG, "Error registering NSD service", e)
        }
    }
    
    /**
     * Unregister NSD service
     */
    private fun unregisterNsdService() {
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
    
    /**
     * Check if service is running in foreground
     */
    private fun isServiceForeground(): Boolean {
        // This is a simplified check - in production you might want more sophisticated detection
        return currentSessionDirectory != null || isServerRunning.get()
    }
    
    /**
     * Create notification for server status
     */
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
    
    /**
     * Get server connection status
     */
    fun getServerStatus(): String {
        return if (isServerRunning.get()) {
            "Running on port $SERVER_PORT (${activeConnections.size} clients)"
        } else {
            "Stopped"
        }
    }
    
    /**
     * Get list of connected PC clients
     */
    fun getConnectedClients(): List<String> {
        return activeConnections.keys.toList()
    }
}
=======
     * Handle stop recording command from PC Controller
     */
    private fun handleStopRecordingCommand(message: JSONObject) {
        lifecycleScope.launch {
            try {
                Log.i(TAG, "Received stop recording command from PC Controller")
                stopRecordingSession()
                
                // Send acknowledgment back to PC Controller
                val response = JSONObject().apply {
                    put("message_type", "response")
                    put("response_to", "stop_recording")
                    put("status", "success")
                    put("message", "Recording stopped successfully")
                }
                networkClient.sendMessage(response)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling stop recording command", e)
                
                // Send error response
                val response = JSONObject().apply {
                    put("message_type", "response")
                    put("response_to", "stop_recording") 
                    put("status", "error")
                    put("message", "Failed to stop recording: ${e.message}")
                }
                networkClient.sendMessage(response)
            }
        }
    }
    
    /**
     * Handle sync flash command from PC Controller
     */
    private fun handleSyncFlashCommand(message: JSONObject) {
        lifecycleScope.launch {
            try {
                val durationMs = message.optInt("flash_duration_ms", 100)
                val timestamp = System.nanoTime()
                
                Log.i(TAG, "Received sync flash command from PC Controller")
                
                // Add sync marker to recording
                addSyncMarker("flash_sync", timestamp)
                
                // TODO: Implement screen flash functionality
                // This would require UI interaction which is complex from a background service
                
                // Send acknowledgment back to PC Controller
                val response = JSONObject().apply {
                    put("message_type", "response")
                    put("response_to", "sync_flash")
                    put("status", "success")
                    put("timestamp_ns", timestamp)
                    put("message", "Sync flash executed")
                }
                networkClient.sendMessage(response)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling sync flash command", e)
            }
        }
    }
    
    /**
     * Handle query capabilities command from PC Controller
     */
    private fun handleQueryCapabilitiesCommand(message: JSONObject) {
        lifecycleScope.launch {
            try {
                Log.i(TAG, "Received query capabilities command from PC Controller")
                
                val capabilities = JSONObject().apply {
                    put("rgb_camera", true)
                    put("thermal_camera", true) // Assuming thermal camera is available
                    put("gsr_sensor", true)     // Assuming GSR sensor is available
                    put("sync_flash", true)
                    put("background_recording", true)
                }
                
                // Send capabilities response back to PC Controller
                val response = JSONObject().apply {
                    put("message_type", "response")
                    put("response_to", "query_capabilities")
                    put("status", "capabilities_data")
                    put("capabilities", capabilities)
                    put("device_model", android.os.Build.MODEL)
                    put("android_version", android.os.Build.VERSION.RELEASE)
                }
                networkClient.sendMessage(response)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling query capabilities command", e)
            }
        }
    }
    
    /**
     * Handle query status command from PC Controller  
     */
    private fun handleQueryStatusCommand(message: JSONObject) {
        lifecycleScope.launch {
            try {
                Log.i(TAG, "Received query status command from PC Controller")
                
                val currentSession = getCurrentSession()
                val status = JSONObject().apply {
                    put("is_recording", recordingController.isRecording)
                    put("is_initialized", isInitialized)
                    put("current_session", currentSession?.directory ?: "")
                    put("recording_start_time", currentSession?.startTime ?: 0)
                    put("uptime_ms", System.currentTimeMillis())
                }
                
                // Send status response back to PC Controller
                val response = JSONObject().apply {
                    put("message_type", "response")
                    put("response_to", "query_status")
                    put("status", "status_data")
                    put("data", status)
                }
                networkClient.sendMessage(response)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling query status command", e)
            }
        }
    }
    
    /**
     * Get network client instance (for testing/debugging)
     */
    fun getNetworkClient(): NetworkClient = networkClient
    
    /**
     * Manually connect to a PC Controller using IP address
     * This can be used as a fallback when automatic discovery fails
     */
    fun connectToPC(ipAddress: String, port: Int = 8080, callback: ((Boolean) -> Unit)? = null) {
        if (!isNetworkInitialized) {
            Log.e(TAG, "Network client not initialized")
            callback?.invoke(false)
            return
        }
        
        lifecycleScope.launch {
            try {
                Log.i(TAG, "Attempting manual connection to PC Controller at $ipAddress:$port")
                
                val success = networkClient.connectToController(ipAddress, port)
                
                if (success) {
                    Log.i(TAG, "Manual connection to PC Controller successful")
                    updateNotification("Connected to PC Controller ($ipAddress)")
                } else {
                    Log.w(TAG, "Manual connection to PC Controller failed")
                    updateNotification("Failed to connect to PC ($ipAddress)")
                }
                
                callback?.invoke(success)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during manual PC connection", e)
                callback?.invoke(false)
            }
        }
    }
=======
    // Network server setup and management
    
    private fun setupNetworkServer() {
        lifecycleScope.launch {
            try {
                // Start the TCP server immediately when service initializes
                val serverStarted = networkServer.start()
                
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
        
        // Monitor connection state
        lifecycleScope.launch {
            networkServer.connectionStateFlow.collect { connected ->
                isConnectedToPC = connected
                if (connected) {
                    Log.i(TAG, "PC Controller connected to network server")
                    updateNotification("PC Controller connected")
                } else {
                    Log.i(TAG, "PC Controller disconnected, still listening on port 8080")
                    updateNotification("Listening for PC Controller on port 8080")
                }
            }
        }
        
        // Monitor incoming messages from PC Controller
        lifecycleScope.launch {
            networkServer.messageFlow.collect { message ->
                handlePCCommand(message)
            }
        }
    }
    
    private fun connectToPC(ipAddress: String, port: Int) {
        // With server architecture, we don't "connect" to PC
        // Instead, we ensure our server is running and ready for PC to connect to us
        lifecycleScope.launch {
            try {
                Log.i(TAG, "Ensuring network server is ready for PC Controller connection")
                
                if (!networkServer.isRunning()) {
                    val started = networkServer.start()
                    if (started) {
                        Log.i(TAG, "Network server started, ready for PC Controller at any IP")
                        updateNotification("Ready for PC Controller connection")
                    } else {
                        Log.e(TAG, "Failed to start network server")
                        updateNotification("Failed to start network server")
                    }
                } else {
                    Log.i(TAG, "Network server already running, ready for PC Controller")
                    updateNotification("Network server ready")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error ensuring network server is ready", e)
                updateNotification("Network server error: ${e.message}")
            }
        }
    }
    
    private fun disconnectFromPC() {
        lifecycleScope.launch {
            try {
                // Stop the network server, which will disconnect any connected PC
                networkServer.stop()
                isConnectedToPC = false
                Log.i(TAG, "Network server stopped, PC Controller disconnected")
                updateNotification("Network server stopped") 
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping network server", e)
            }
        }
    }
    
    private fun startPCDiscovery() {
        lifecycleScope.launch {
            try {
                // TODO: Implement PC discovery using zeroconf/mDNS
                // For now, log that discovery was requested
                Log.i(TAG, "PC Controller discovery requested")
                updateNotification("Searching for PC Controller...")
                
                // This could be extended to use NetworkDiscoveryService
                // or implement manual discovery logic here
            } catch (e: Exception) {
                Log.e(TAG, "Error starting PC discovery", e)
            }
        }
    }
    
    private suspend fun handlePCCommand(message: JSONObject) {
        try {
            val messageType = message.optString("message_type")
            Log.i(TAG, "Processing PC command: $messageType")
            
            when (messageType) {
                "enhanced_device_registration" -> {
                    Log.i(TAG, "PC Controller device registration request")
                    sendResponseToPC("enhanced_registration_ack", JSONObject().apply {
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
                        Log.i(TAG, "Received remote start command from PC for session: $sessionDirectory")
                        startRecordingSession(sessionDirectory)
                        sendResponseToPC("session_start_response", JSONObject().apply {
                            put("status", "started")
                            put("session_directory", sessionDirectory)
                        })
                    }
                }
                
                "session_stop_command" -> {
                    Log.i(TAG, "Received remote stop command from PC")
                    stopRecordingSession()
                    sendResponseToPC("session_stop_response", JSONObject().apply {
                        put("status", "stopped")
                    })
                }
                
                "sync_marker_command" -> {
                    val markerType = message.optString("marker_type")
                    val timestampNs = message.optLong("timestamp_ns", System.nanoTime())
                    if (markerType.isNotEmpty()) {
                        Log.i(TAG, "Received remote sync marker from PC: $markerType")
                        addSyncMarker(markerType, timestampNs)
                        sendResponseToPC("sync_marker_response", JSONObject().apply {
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
    
    private suspend fun sendResponseToPC(messageType: String, data: JSONObject = JSONObject()) {
        try {
            val response = JSONObject().apply {
                put("message_type", messageType)
                put("device_id", android.provider.Settings.Secure.getString(
                    contentResolver, android.provider.Settings.Secure.ANDROID_ID))
                put("timestamp_ns", System.nanoTime())
                // Merge additional data
                data.keys().forEach { key ->
                    put(key, data.get(key))
                }
            }
            
            networkServer.sendMessage(response)
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
>>>>>>> dev
}

/**
 * Current session information
 */
data class SessionInfo(
    val directory: String,
    val startTime: Long,
    val isRecording: Boolean
)
>>>>>>> dev
