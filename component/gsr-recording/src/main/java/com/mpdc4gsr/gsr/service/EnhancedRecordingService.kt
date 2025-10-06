package com.mpdc4gsr.gsr.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mpdc4gsr.gsr.R
import com.mpdc4gsr.gsr.model.GSRSample
import com.mpdc4gsr.gsr.model.SessionInfo
import com.mpdc4gsr.gsr.model.SyncMark
import com.mpdc4gsr.gsr.network.DataStreamingService
import com.mpdc4gsr.gsr.network.NetworkClient
import com.mpdc4gsr.gsr.network.ZeroconfDiscoveryService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class EnhancedRecordingService : Service() {
    companion object {
        private const val TAG = "EnhancedRecordingService"
        private const val NOTIFICATION_ID = 12346
        private const val CHANNEL_ID = "enhanced_recording_channel"
        private const val WAKE_LOCK_TAG = "IRCamera:EnhancedRecording"
        private const val ACTION_START_RECORDING = "action_start_recording"
        private const val ACTION_STOP_RECORDING = "action_stop_recording"
        private const val ACTION_CONNECT_PC = "action_connect_pc"
        private const val ACTION_DISCONNECT_PC = "action_disconnect_pc"
        private const val ACTION_START_DISCOVERY = "action_start_discovery"
        private const val ACTION_STOP_DISCOVERY = "action_stop_discovery"
        private const val EXTRA_SESSION_ID = "extra_session_id"
        private const val EXTRA_PARTICIPANT_ID = "extra_participant_id"
        private const val EXTRA_STUDY_NAME = "extra_study_name"
        private const val EXTRA_PC_IP = "extra_pc_ip"
        private const val EXTRA_PC_PORT = "extra_pc_port"
        fun startRecording(
            context: Context,
            sessionId: String,
            participantId: String? = null,
            studyName: String? = null,
        ) {
            val intent =
                Intent(context, EnhancedRecordingService::class.java).apply {
                    action = ACTION_START_RECORDING
                    putExtra(EXTRA_SESSION_ID, sessionId)
                    putExtra(EXTRA_PARTICIPANT_ID, participantId)
                    putExtra(EXTRA_STUDY_NAME, studyName)
                }
            startForegroundService(context, intent)
        }

        fun stopRecording(context: Context) {
            val intent =
                Intent(context, EnhancedRecordingService::class.java).apply {
                    action = ACTION_STOP_RECORDING
                }
            context.startService(intent)
        }

        fun connectToPC(
            context: Context,
            ipAddress: String,
            port: Int = 8080,
        ) {
            val intent =
                Intent(context, EnhancedRecordingService::class.java).apply {
                    action = ACTION_CONNECT_PC
                    putExtra(EXTRA_PC_IP, ipAddress)
                    putExtra(EXTRA_PC_PORT, port)
                }
            context.startService(intent)
        }

        fun disconnectFromPC(context: Context) {
            val intent =
                Intent(context, EnhancedRecordingService::class.java).apply {
                    action = ACTION_DISCONNECT_PC
                }
            context.startService(intent)
        }

        fun startDiscovery(context: Context) {
            val intent =
                Intent(context, EnhancedRecordingService::class.java).apply {
                    action = ACTION_START_DISCOVERY
                }
            context.startService(intent)
        }

        private fun startForegroundService(
            context: Context,
            intent: Intent,
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private lateinit var gsrRecorder: GSRRecorder
    private lateinit var sessionManager: SessionManager
    private lateinit var networkClient: NetworkClient
    private lateinit var dataStreamingService: DataStreamingService
    private lateinit var discoveryService: ZeroconfDiscoveryService
    private var isRecording = false
    private var isConnectedToPC = false
    private var isStreamingData = false
    private var currentSessionId: String? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val binder = EnhancedRecordingBinder()

    inner class EnhancedRecordingBinder : Binder() {
        fun getService(): EnhancedRecordingService = this@EnhancedRecordingService
    }

    interface ServiceEventListener {
        fun onRecordingStateChanged(
            isRecording: Boolean,
            sessionId: String?,
        )

        fun onNetworkStateChanged(
            isConnected: Boolean,
            controllerInfo: NetworkClient.ControllerInfo?,
        )

        fun onDataStreamingStateChanged(isStreaming: Boolean)
        fun onServiceError(
            operation: String,
            error: String,
        )

        fun onDiscoveryResult(controllers: List<NetworkClient.ControllerInfo>)
    }

    private var eventListener: ServiceEventListener? = null
    fun setEventListener(listener: ServiceEventListener?) {
        eventListener = listener
    }

    override fun onCreate() {
        super.onCreate()
        initializeComponents()
        setupNetworkListeners()
        createNotificationChannel()
        acquireWakeLock()
        Log.i(TAG, "Enhanced recording service created")
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: return START_NOT_STICKY
                val participantId = intent.getStringExtra(EXTRA_PARTICIPANT_ID)
                val studyName = intent.getStringExtra(EXTRA_STUDY_NAME)
                startRecording(sessionId, participantId, studyName)
            }

            ACTION_STOP_RECORDING -> stopRecording()
            ACTION_CONNECT_PC -> {
                val ipAddress = intent.getStringExtra(EXTRA_PC_IP) ?: return START_NOT_STICKY
                val port = intent.getIntExtra(EXTRA_PC_PORT, 8080)
                connectToPC(ipAddress, port)
            }

            ACTION_DISCONNECT_PC -> disconnectFromPC()
            ACTION_START_DISCOVERY -> startPCDiscovery()
            ACTION_STOP_DISCOVERY -> stopPCDiscovery()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder
    private fun initializeComponents() {
        gsrRecorder = GSRRecorder(this, ShimmerDeviceFactoryResolver.createFactory(this))
        sessionManager = SessionManager.getInstance(this)
        networkClient = NetworkClient(this)
        dataStreamingService = DataStreamingService(this, networkClient)
        discoveryService = ZeroconfDiscoveryService(this)
    }

    private fun setupNetworkListeners() {
        gsrRecorder.addListener(
            object : GSRRecorder.GSRRecordingListener {
                override fun onRecordingStarted(sessionInfo: SessionInfo) {
                    Log.i(TAG, "GSR recording started: ${sessionInfo.sessionId}")
                    updateNotification("Recording started - ${sessionInfo.sessionId}")
                    eventListener?.onRecordingStateChanged(true, sessionInfo.sessionId)
                    if (isConnectedToPC) {
                        serviceScope.launch {
                            val streamingStarted =
                                dataStreamingService.startStreaming(sessionInfo.sessionId)
                            if (streamingStarted) {
                                isStreamingData = true
                                eventListener?.onDataStreamingStateChanged(true)
                            }
                        }
                    }
                }

                override fun onRecordingStopped(sessionInfo: SessionInfo) {
                    Log.i(TAG, "GSR recording stopped: ${sessionInfo.sessionId}")
                    isRecording = false
                    currentSessionId = null
                    if (isStreamingData) {
                        serviceScope.launch {
                            dataStreamingService.stopStreaming()
                            isStreamingData = false
                            eventListener?.onDataStreamingStateChanged(false)
                        }
                    }
                    eventListener?.onRecordingStateChanged(false, null)
                    updateNotification("Recording stopped")
                }

                override fun onSampleRecorded(sample: GSRSample) {
                    if (isStreamingData) {
                        dataStreamingService.queueGSRSample(sample)
                    }
                    if (sample.sampleIndex % 1280 == 0L) {
                        updateNotification("Recording... ${sample.sampleIndex} samples")
                    }
                }

                override fun onSyncMarkAdded(syncMark: SyncMark) {
                    Log.d(TAG, "Sync mark added: ${syncMark.eventType}")
                }

                override fun onError(error: String) {
                    Log.e(TAG, "GSR recording error: $error")
                    eventListener?.onServiceError("gsr_recording", error)
                }
            },
        )
        networkClient.setEventListener(
            object : NetworkClient.NetworkEventListener {
                override fun onControllerDiscovered(controller: NetworkClient.ControllerInfo) {
                    Log.i(
                        TAG,
                        "PC Controller discovered: ${controller.deviceName} at ${controller.ipAddress}"
                    )
                }

                override fun onConnected(controller: NetworkClient.ControllerInfo) {
                    Log.i(TAG, "Connected to PC Controller: ${controller.deviceName}")
                    isConnectedToPC = true
                    updateNotification("Connected to ${controller.deviceName}")
                    eventListener?.onNetworkStateChanged(true, controller)
                }

                override fun onDisconnected(reason: String) {
                    Log.i(TAG, "Disconnected from PC Controller: $reason")
                    isConnectedToPC = false
                    if (isStreamingData) {
                        serviceScope.launch {
                            dataStreamingService.stopStreaming()
                            isStreamingData = false
                            eventListener?.onDataStreamingStateChanged(false)
                        }
                    }
                    updateNotification("Disconnected from PC")
                    eventListener?.onNetworkStateChanged(false, null)
                }

                override fun onRemoteMeasurementRequest(sessionInfo: SessionInfo) {
                    Log.i(TAG, "Remote measurement request: ${sessionInfo.sessionId}")
                    if (!isRecording) {
                        startRecording(
                            sessionInfo.sessionId,
                            sessionInfo.participantId,
                            sessionInfo.studyName
                        )
                    }
                }

                override fun onSyncFlash(durationMs: Int) {
                    Log.i(TAG, "Sync flash requested: ${durationMs}ms")
                    if (isRecording) {
                        gsrRecorder.triggerSyncEvent("SYNC_FLASH_${durationMs}ms")
                    }
                }

                override fun onTimeSynchronized(offsetNanoseconds: Long) {
                    Log.i(TAG, "Time synchronized: offset=${offsetNanoseconds}ns")
                    updateNotification("Time synchronized (offset: ${offsetNanoseconds / 1000000}ms)")
                }

                override fun onDataStreamingStarted() {
                    Log.i(TAG, "Data streaming to PC started")
                    updateNotification("Streaming data to PC")
                }

                override fun onDataStreamingStopped() {
                    Log.i(TAG, "Data streaming to PC stopped")
                    updateNotification("Data streaming stopped")
                }

                override fun onError(
                    operation: String,
                    error: String,
                ) {
                    Log.e(TAG, "Network error in $operation: $error")
                    eventListener?.onServiceError("network_$operation", error)
                }

                override fun onPairingRequested(
                    controllerId: String,
                    controllerName: String,
                ) {
                    Log.i(TAG, "Pairing requested by controller: $controllerName ($controllerId)")
                    updateNotification("Pairing requested by $controllerName")
                }

                override fun onPairingCompleted(
                    controllerId: String,
                    success: Boolean,
                ) {
                    if (success) {
                        Log.i(TAG, "Pairing completed successfully with controller: $controllerId")
                        updateNotification("Paired with controller")
                    } else {
                        Log.w(TAG, "Pairing failed with controller: $controllerId")
                        updateNotification("Pairing failed")
                    }
                }

                override fun onAuthenticationRequired(controllerId: String) {
                    Log.w(TAG, "Authentication required for controller: $controllerId")
                    updateNotification("Authentication required")
                }
            },
        )
        dataStreamingService.setEventListener(
            object : DataStreamingService.StreamingEventListener {
                override fun onStreamingStarted(sessionId: String) {
                    Log.i(TAG, "Data streaming started for session: $sessionId")
                    isStreamingData = true
                    eventListener?.onDataStreamingStateChanged(true)
                }

                override fun onStreamingStopped(sessionId: String) {
                    Log.i(TAG, "Data streaming stopped for session: $sessionId")
                    isStreamingData = false
                    eventListener?.onDataStreamingStateChanged(false)
                }

                override fun onBatchSent(
                    batchSize: Int,
                    dataType: String,
                ) {
                    Log.d(TAG, "Sent $dataType batch: $batchSize samples")
                }

                override fun onStreamingError(error: String) {
                    Log.e(TAG, "Data streaming error: $error")
                    eventListener?.onServiceError("data_streaming", error)
                }

                override fun onQueueFull(
                    dataType: String,
                    droppedSamples: Int,
                ) {
                    Log.w(TAG, "Queue full for $dataType: dropped $droppedSamples samples")
                }
            },
        )
        discoveryService.setServiceListener(
            object : ZeroconfDiscoveryService.ServiceDiscoveryListener {
                override fun onServiceDiscovered(serviceInfo: NetworkClient.ControllerInfo) {
                    Log.i(TAG, "mDNS service discovered: ${serviceInfo.deviceName}")
                }

                override fun onServiceLost(serviceName: String) {
                    Log.i(TAG, "mDNS service lost: $serviceName")
                }

                override fun onServiceRegistered(serviceName: String) {
                    Log.i(TAG, "mDNS service registered: $serviceName")
                }

                override fun onDiscoveryError(
                    errorCode: Int,
                    message: String,
                ) {
                    Log.e(TAG, "mDNS discovery error: $message (code: $errorCode)")
                    eventListener?.onServiceError("mdns_discovery", message)
                }
            },
        )
    }

    private fun startRecording(
        sessionId: String,
        participantId: String?,
        studyName: String?,
    ) {
        if (isRecording) {
            Log.w(TAG, "Recording already in progress")
            return
        }
        serviceScope.launch {
            try {
                sessionManager.createSession(sessionId, participantId, studyName)
                startForeground(NOTIFICATION_ID, createNotification("Starting recording..."))
                if (gsrRecorder.startRecording(sessionId, participantId, studyName)) {
                    isRecording = true
                    currentSessionId = sessionId
                    Log.i(TAG, "Enhanced recording started: $sessionId")
                } else {
                    Log.e(TAG, "Failed to start GSR recording")
                    eventListener?.onServiceError(
                        "start_recording",
                        "Failed to start GSR recording"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting recording", e)
                eventListener?.onServiceError("start_recording", e.message ?: "Unknown error")
            }
        }
    }

    private fun stopRecording() {
        if (!isRecording) {
            Log.w(TAG, "No recording in progress")
            return
        }
        serviceScope.launch {
            try {
                val session = gsrRecorder.stopRecording()
                session?.let {
                    sessionManager.completeSession(it.sessionId)
                }
                Log.i(TAG, "Enhanced recording stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recording", e)
                eventListener?.onServiceError("stop_recording", e.message ?: "Unknown error")
            }
        }
    }

    private fun connectToPC(
        ipAddress: String,
        port: Int,
    ) {
        serviceScope.launch {
            try {
                val success = networkClient.connectToController(ipAddress, port)
                if (!success) {
                    eventListener?.onServiceError(
                        "connect_pc",
                        "Failed to connect to $ipAddress:$port"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to PC", e)
                eventListener?.onServiceError("connect_pc", e.message ?: "Unknown error")
            }
        }
    }

    private fun disconnectFromPC() {
        networkClient.disconnect()
    }

    private fun startPCDiscovery() {
        serviceScope.launch {
            try {
                val success = discoveryService.startDiscovery()
                if (success) {
                    updateNotification("Discovering PC Controllers...")
                    discoveryService.registerService(
                        deviceId =
                            android.provider.Settings.Secure.getString(
                                contentResolver,
                                android.provider.Settings.Secure.ANDROID_ID,
                            ),
                        port = 0,
                    )
                } else {
                    eventListener?.onServiceError("start_discovery", "Failed to start discovery")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting discovery", e)
                eventListener?.onServiceError("start_discovery", e.message ?: "Unknown error")
            }
        }
    }

    private fun stopPCDiscovery() {
        discoveryService.stopDiscovery()
        updateNotification("Discovery stopped")
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock =
            powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                WAKE_LOCK_TAG,
            ).apply {
                acquire(10 * 60 * 1000L)
            }
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Enhanced Recording Service",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Multi-modal physiological data recording with PC communication"
                    setSound(null, null)
                }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Enhanced Recording Service")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_fast_forward)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(content: String) {
        val notification = createNotification(content)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    fun getConnectionStatus(): Boolean = isConnectedToPC
    fun getRecordingStatus(): Boolean = isRecording
    fun getStreamingStatus(): Boolean = isStreamingData
    fun getCurrentSessionId(): String? = currentSessionId
    fun getDiscoveredControllers(): List<NetworkClient.ControllerInfo> =
        discoveryService.getDiscoveredControllers()

    fun getQueueSizes(): Map<String, Int> = dataStreamingService.getQueueSizes()
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.launch {
            if (isRecording) {
                stopRecording()
            }
            if (isConnectedToPC) {
                disconnectFromPC()
            }
            dataStreamingService.cleanup()
            discoveryService.cleanup()
        }
        serviceJob.cancel()
        releaseWakeLock()
        Log.i(TAG, "Enhanced recording service destroyed")
    }
}
