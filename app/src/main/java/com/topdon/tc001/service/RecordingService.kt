package com.topdon.tc001.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.topdon.tc001.controller.RecordingController
import com.topdon.tc001.controller.RecordingState
import com.topdon.tc001.network.EnhancedNetworkClient
import com.topdon.tc001.network.NetworkClient
import com.topdon.tc001.network.NetworkServer
import com.topdon.tc001.utils.TimeManager
import com.csl.irCamera.R
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

/**
 * Background service for multi-modal sensor recording.
 * 
 * This service ensures continuous recording operation even when the app is in the background.
 * It manages the RecordingController and provides status updates through notifications.
 * 
 * Key Features:
 * - Foreground service for uninterrupted recording
 * - Real-time status notifications
 * - Automatic recovery from errors
 * - Integration with PC Controller communication
 * - Power management awareness
 * 
 * @author IRCamera Android Sensor Node (Spoke)
 */
class RecordingService : LifecycleService() {

    companion object {
        private const val TAG = "RecordingService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "recording_service_channel"
        
        // Actions
        const val ACTION_START_RECORDING = "com.topdon.tc001.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.topdon.tc001.STOP_RECORDING"
        const val ACTION_ADD_SYNC_MARKER = "com.topdon.tc001.ADD_SYNC_MARKER"
        const val ACTION_CONNECT_PC = "com.topdon.tc001.CONNECT_PC"
        const val ACTION_DISCONNECT_PC = "com.topdon.tc001.DISCONNECT_PC"
        const val ACTION_START_DISCOVERY = "com.topdon.tc001.START_DISCOVERY"
        
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
    
    // Network communication - TCP Server for PC connections
    private lateinit var networkServer: NetworkServer
    private var isConnectedToPC = false
    
    // Current session
    private var currentSessionDirectory: String? = null
    private var recordingStartTime: Long = 0
    
    // Notification manager
    private lateinit var notificationManager: NotificationManager

    inner class RecordingServiceBinder : Binder() {
        fun getService(): RecordingService = this@RecordingService
        fun getRecordingController(): RecordingController = recordingController
        fun getNetworkServer(): NetworkServer = networkServer
        fun isConnectedToPC(): Boolean = isConnectedToPC
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "RecordingService created")
        
        // Initialize notification manager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        
        // Initialize recording controller
        recordingController = RecordingController(this, this)
        
        // Initialize network server to listen for PC Controller connections
        networkServer = NetworkServer(this, 8080)
        
        // Initialize sensors
        lifecycleScope.launch {
            try {
                val success = recordingController.initializeSensors()
                isInitialized = success
                
                if (success) {
                    Log.i(TAG, "Recording service initialized successfully")
                    setupStatusMonitoring()
                    setupNetworkServer()
                } else {
                    Log.e(TAG, "Failed to initialize recording service")
                    stopSelf()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing recording service", e)
                stopSelf()
            }
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
        
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "RecordingService destroyed")
        
        lifecycleScope.launch {
            try {
                networkServer.stop()
                isConnectedToPC = false
                Log.i(TAG, "Network server stopped")
                updateNotification("Network server stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error during service cleanup", e)
            } finally {
                try {
                    recordingController.cleanup()
                } catch (e: Exception) {
                    Log.e(TAG, "Error during recording controller cleanup", e)
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

    /**
     * Get current session information
     */
    fun getCurrentSession(): SessionInfo? {
        return currentSessionDirectory?.let { directory ->
            SessionInfo(
                directory = directory,
                startTime = recordingStartTime,
                isRecording = recordingController.isRecording
            )
        }
    }
    
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
}

/**
 * Current session information
 */
data class SessionInfo(
    val directory: String,
    val startTime: Long,
    val isRecording: Boolean
)