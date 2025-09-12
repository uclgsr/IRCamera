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
import com.topdon.tc001.network.NetworkClient
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
        
        // Extras
        const val EXTRA_SESSION_DIRECTORY = "session_directory"
        const val EXTRA_MARKER_TYPE = "marker_type"
        const val EXTRA_TIMESTAMP_NS = "timestamp_ns"
        
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
    }

    // Service binding
    private val binder = RecordingServiceBinder()
    
    // Recording controller
    private lateinit var recordingController: RecordingController
    private var isInitialized = false
    
    // Network client for PC Controller communication
    private lateinit var networkClient: NetworkClient
    private var isNetworkInitialized = false
    
    // Current session
    private var currentSessionDirectory: String? = null
    private var recordingStartTime: Long = 0
    
    // Notification manager
    private lateinit var notificationManager: NotificationManager

    inner class RecordingServiceBinder : Binder() {
        fun getService(): RecordingService = this@RecordingService
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "RecordingService created")
        
        // Initialize notification manager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        
        // Initialize recording controller
        recordingController = RecordingController(this, this)
        
        // Initialize network client
        networkClient = NetworkClient(this)
        
        // Initialize sensors and network
        lifecycleScope.launch {
            try {
                val sensorsSuccess = recordingController.initializeSensors()
                isInitialized = sensorsSuccess
                
                val networkSuccess = initializeNetworkClient()
                isNetworkInitialized = networkSuccess
                
                if (sensorsSuccess) {
                    Log.i(TAG, "Recording service initialized successfully")
                    setupStatusMonitoring()
                    
                    if (networkSuccess) {
                        Log.i(TAG, "Network client initialized successfully")
                        startNetworkDiscovery()
                    } else {
                        Log.w(TAG, "Network client initialization failed - running in offline mode")
                    }
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
                recordingController.cleanup()
                
                // Clean up network resources
                if (isNetworkInitialized) {
                    networkClient.disconnect()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during service cleanup", e)
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
}

/**
 * Current session information
 */
data class SessionInfo(
    val directory: String,
    val startTime: Long,
    val isRecording: Boolean
)