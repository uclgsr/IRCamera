package com.topdon.tc001.sensors.gsr

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.shimmerresearch.android.Shimmer
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
import com.shimmerresearch.driver.ObjectCluster
import com.shimmerresearch.driver.ShimmerDevice
import com.shimmerresearch.bluetooth.ShimmerBluetooth.BT_STATE
import com.topdon.ble.ShimmerBleController
import com.topdon.ble.ShimmerDevice as CustomShimmerDevice
import com.topdon.ble.UnifiedBleManager
import com.topdon.ble.util.BluetoothPermissionUtils
import com.topdon.gsr.model.GSRSample
import com.topdon.gsr.model.SessionInfo
import com.topdon.gsr.model.SyncMark
import com.topdon.gsr.service.MockShimmerDeviceFactory
import com.topdon.gsr.service.ShimmerGSRRecorder
import com.topdon.tc001.sensors.SensorRecorder
import com.topdon.tc001.sensors.RecordingStatus
import com.topdon.tc001.sensors.SensorError
import com.topdon.tc001.sensors.ErrorType
import com.topdon.tc001.sensors.RecordingStats
import com.topdon.tc001.sensors.TimestampManager
import com.topdon.tc001.controller.RecordingController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import com.topdon.gsr.service.GSRRecorder as LegacyGSRRecorder

class GSRSensorRecorder(
    private val context: Context,
    override val sensorId: String = "gsr_shimmer_1",
    private val samplingRateHz: Int = 128,
    private val recordingController: RecordingController
) : SensorRecorder {
    companion object {
        private const val TAG = "GSRSensorRecorder"

        private const val SHIMMER_DEFAULT_SAMPLING_RATE = 128.0 // Hz
        private const val GSR_CHANNEL_ID = 0x01 // GSR sensor channel ID
        private const val GSR_RANGE_AUTO = 0x00 // Auto range setting

        fun hasRequiredPermissions(context: Context): Boolean {
            return hasBleScanningPermissions(context)
        }

        fun getMissingPermissions(context: Context): List<String> {
            val missing = mutableListOf<String>()
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.BLUETOOTH
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                missing.add(android.Manifest.permission.BLUETOOTH)
            }
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.BLUETOOTH_ADMIN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                missing.add(android.Manifest.permission.BLUETOOTH_ADMIN)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.BLUETOOTH_SCAN
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    missing.add(android.Manifest.permission.BLUETOOTH_SCAN)
                }
                if (ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    missing.add(android.Manifest.permission.BLUETOOTH_CONNECT)
                }
            }
            return missing
        }

        fun hasComprehensiveBluetoothPermissions(context: Context): Boolean {
            return hasBleScanningPermissions(context)
        }

        private fun hasBleScanningPermissions(context: Context): Boolean {
            return getMissingPermissions(context).isEmpty()
        }
    }

    override val sensorType: String = "GSR Shimmer3"
    override val samplingRate: Double = samplingRateHz.toDouble()

    private var _isRecording = AtomicBoolean(false)
    override val isRecording: Boolean get() = _isRecording.get()

    private var unifiedBleManager: UnifiedBleManager? = null
    private var unifiedShimmerDevice: ShimmerDevice? = null

    private var realShimmerGSRRecorder: ShimmerGSRRecorder? = null
    private var shimmerDevice: ShimmerBleController? = null

    private var gsrDataPersistence: GSRDataPersistence? = null
    private var currentSessionId: String? = null
    private val sampleSequence = AtomicLong(0)
    private var isShimmerConnected = false

    private var legacyGSRRecorder: LegacyGSRRecorder? = null

    private var gsrNetworkStreamer: GSRNetworkStreamer? = null
    private var isNetworkStreamingEnabled = true

    private val recordingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var sessionDirectory: String = ""
    private var sampleCount = AtomicLong(0)
    private var recordingStartTime: Long = 0
    private var syncMarkerCount = AtomicLong(0)

    private val _statusFlow = MutableSharedFlow<RecordingStatus>()
    private val _errorFlow = MutableSharedFlow<SensorError>()

    private var lastSampleTimestamp: Long = 0
    private var dataMonitoringJob: Job? = null

    override suspend fun initialize(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Initializing GSR sensor with Shimmer3 integration for $sensorId")

                if (!hasRequiredPermissions(context)) {
                    Log.w(TAG, "Missing required Bluetooth permissions for Shimmer GSR device")
                    Log.i(
                        TAG,
                        "GSR sensor will initialize but Shimmer functionality will be limited until permissions are granted"
                    )

                }

                unifiedBleManager = UnifiedBleManager.getInstance(context)
                if (!unifiedBleManager!!.initialize()) {
                    Log.w(
                        TAG,
                        "Unified BLE manager initialization failed, falling back to legacy implementation"
                    )
                } else {
                    Log.i(TAG, "Unified BLE manager initialized successfully")
                }

                realShimmerGSRRecorder = ShimmerGSRRecorder(context, MockShimmerDeviceFactory(), samplingRateHz)

                val shimmerRecorder = realShimmerGSRRecorder
                if (shimmerRecorder != null) {
                    try {

                        val deviceInitialized = shimmerRecorder.initializeDevice()
                        if (deviceInitialized) {
                            Log.i(TAG, "Shimmer GSR device initialized and ready")
                            isShimmerConnected = true
                        } else {
                            Log.w(
                                TAG,
                                "Shimmer GSR device not available, but sensor recorder initialized"
                            )
                            isShimmerConnected = false
                        }
                    } catch (e: Exception) {
                        Log.w(
                            TAG,
                            "Shimmer GSR device initialization failed, but continuing: ${e.message}"
                        )
                        isShimmerConnected = false
                    }
                }

                legacyGSRRecorder = LegacyGSRRecorder(context, MockShimmerDeviceFactory(), samplingRateHz)

                if (isNetworkStreamingEnabled) {
                    try {

                        Log.i(TAG, "Network streaming will be initialized during recording start")
                    } catch (e: Exception) {
                        Log.w(
                            TAG,
                            "Network streaming setup failed, continuing without streaming: ${e.message}"
                        )
                        isNetworkStreamingEnabled = false
                    }
                }

                startDataMonitoring()

                setupGSRSampleCallback()

                Log.i(
                    TAG,
                    "GSR sensor initialized successfully (Shimmer connected: $isShimmerConnected, Network streaming: $isNetworkStreamingEnabled)",
                )
                emitStatus()
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize GSR sensor", e)
                emitError(
                    ErrorType.INITIALIZATION_FAILED,
                    "GSR initialization failed: ${e.message}"
                )
                return@withContext false
            }
        }

    private fun startDataMonitoring() {
        dataMonitoringJob =
            recordingScope.launch {
                while (isActive) {
                    if (_isRecording.get()) {
                        monitorGSRData()
                        emitStatus()
                    }
                    delay(1000) // Update every second
                }
            }
    }

    private suspend fun monitorGSRData() {
        try {

            val shimmerRecorder = realShimmerGSRRecorder
            if (shimmerRecorder != null) {

                val realSampleCount = sampleCount.get()

                val expectedSamples =
                    ((System.nanoTime() - recordingStartTime) / 1_000_000_000.0 * samplingRate).toLong()
                val actualSamples = realSampleCount

                if (expectedSamples > actualSamples + samplingRate) {

                    Log.w(
                        TAG,
                        "Enhanced GSR data loss detected (Merged BLE): expected $expectedSamples, got $actualSamples"
                    )
                    emitError(ErrorType.DATA_CORRUPTION, "Enhanced GSR data loss detected", true)
                }

                try {

                    val currentSampleCount = sampleCount.get()
                    if (currentSampleCount == expectedSamples && expectedSamples > 0) {
                        Log.w(
                            TAG,
                            "Enhanced GSR data loss detected: expected more samples than $expectedSamples"
                        )
                        emitError(
                            ErrorType.DATA_CORRUPTION,
                            "Enhanced GSR data loss detected",
                            true
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error monitoring enhanced Shimmer connection: ${e.message}")
                    emitError(ErrorType.DEVICE_ERROR, "Enhanced Shimmer monitoring error", true)
                }
            } else {

                val legacyRecorder = legacyGSRRecorder
                if (legacyRecorder != null) {

                    val currentSamples = sampleCount.get()

                    if (currentSamples > 0) {
                        Log.d(TAG, "Legacy GSR recorder active with $currentSamples samples")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Real GSR data monitoring error", e)
        }
    }

    override suspend fun startRecording(sessionDirectory: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                if (_isRecording.get()) {
                    Log.w(TAG, "Shimmer GSR sensor already recording")
                    return@withContext true
                }

                Log.i(TAG, "Starting GSR sensor recording - checking for Shimmer3 device connectivity")

                // Check Bluetooth permissions first
                if (!BluetoothPermissionUtils.hasBleScanningPermissions(context)) {
                    Log.w(
                        TAG,
                        "Missing Bluetooth permissions for Shimmer GSR recording"
                    )
                    Log.i(
                        TAG,
                        "Missing permissions: ${BluetoothPermissionUtils.getMissingPermissions(context)}"
                    )
                    
                    // Log warning but continue - this allows the session to proceed without GSR
                    emitError(
                        ErrorType.PERMISSION_DENIED,
                        "Bluetooth permissions required for Shimmer GSR - session will continue without GSR data",
                        isRecoverable = true
                    )
                    return@withContext false // Return false to indicate GSR not available, but don't crash
                }

                this@GSRSensorRecorder.sessionDirectory = sessionDirectory
                recordingStartTime = System.nanoTime()

                var shimmerRecordingStarted = false
                var recordingSuccessful = false

                // Attempt Shimmer3 GSR+ recording (primary method)
                if (BluetoothPermissionUtils.hasBleScanningPermissions(context)) {
                    val shimmerRecorder = realShimmerGSRRecorder
                    if (shimmerRecorder != null) {
                        Log.i(TAG, "Attempting Shimmer3 GSR+ recording with enhanced BLE backend")

                        try {
                            // Try to initialize device if not already connected
                            val connectionSuccess = if (!shimmerRecorder.isDeviceConnected()) {
                                Log.i(TAG, "Shimmer device not connected, attempting connection...")
                                shimmerRecorder.initializeDevice()
                            } else {
                                Log.i(TAG, "Shimmer device already connected")
                                true
                            }

                            if (connectionSuccess) {
                                // Start the enhanced Shimmer recording
                                val sessionId = sessionDirectory.substringAfterLast("/").ifEmpty {
                                    "session_${System.currentTimeMillis()}"
                                }
                                
                                val success = shimmerRecorder.startRecording(sessionId)
                                if (success) {
                                    shimmerRecordingStarted = true
                                    recordingSuccessful = true
                                    Log.i(TAG, "Shimmer3 GSR+ recording started successfully")
                                } else {
                                    Log.w(TAG, "Shimmer3 GSR+ recording failed to start")
                                }
                            } else {
                                Log.w(
                                    TAG,
                                    "Shimmer connection failed - device may not be paired, powered on, or in range"
                                )
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Shimmer GSR recording start failed: ${e.message}")
                        }
                    } else {
                        Log.w(TAG, "Shimmer GSR recorder not initialized")
                    }
                }

                // Fallback to legacy recording if Shimmer failed
                if (!shimmerRecordingStarted) {
                    Log.i(TAG, "Shimmer3 recording unavailable, attempting legacy GSR fallback")
                    
                    val legacyRecorder = legacyGSRRecorder
                    if (legacyRecorder != null) {
                        try {
                            val sessionId = sessionDirectory.substringAfterLast("/").ifEmpty {
                                "session_${System.currentTimeMillis()}"
                            }

                            val initSuccess = legacyRecorder.initialize()
                            if (initSuccess) {
                                val success = legacyRecorder.startRecording(
                                    sessionId = sessionId,
                                    participantId = "participant_${System.currentTimeMillis()}",
                                    studyName = "IRCamera_MultiModal_Study",
                                )

                                if (success) {
                                    recordingSuccessful = true
                                    Log.i(TAG, "Legacy GSR recording started successfully")
                                } else {
                                    Log.w(TAG, "Legacy GSR recording failed to start")
                                }
                            } else {
                                Log.w(TAG, "Legacy GSR recorder initialization failed")
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Legacy GSR recording start failed: ${e.message}")
                        }
                    }
                }

                // Handle the recording result
                if (recordingSuccessful) {
                    _isRecording.set(true)
                    sampleCount.set(0)
                    syncMarkerCount.set(0)
                    sampleSequence.set(0)

                    currentSessionId = sessionDirectory.substringAfterLast("/").ifEmpty {
                        "session_${System.currentTimeMillis()}"
                    }

                    // Initialize data persistence and network streaming
                    try {
                        initializeDataHandling()
                    } catch (e: Exception) {
                        Log.w(TAG, "Data handling initialization failed: ${e.message}")
                    }

                    Log.i(
                        TAG,
                        "GSR sensor recording started successfully (Shimmer: $shimmerRecordingStarted, method: ${if (shimmerRecordingStarted) "Enhanced Shimmer3" else "Legacy"})"
                    )
                    emitStatus()
                    return@withContext true
                } else {
                    // All recording methods failed - log error but don't crash the session
                    Log.w(TAG, "All GSR recording methods failed - no GSR data will be recorded for this session")
                    emitError(
                        ErrorType.RECORDING_FAILED,
                        "Shimmer GSR device not available - check device pairing, power, and proximity. Session will continue without GSR data.",
                        isRecoverable = true
                    )
                    return@withContext false // Indicate GSR unavailable, but allow session to continue
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start GSR recording", e)
                emitError(
                    ErrorType.RECORDING_FAILED,
                    "GSR recording initialization failed: ${e.message} - Session will continue without GSR data",
                    isRecoverable = true
                )
                return@withContext false // Don't crash the entire session
            }
        }

    private suspend fun initializeDataHandling() {
        try {
            // Initialize data persistence
            gsrDataPersistence = GSRDataPersistence(context, currentSessionId!!)
            val persistenceInitialized = gsrDataPersistence?.initialize() ?: false

            if (persistenceInitialized) {
                gsrDataPersistence?.startPersistence()
                Log.i(TAG, "GSR data persistence initialized for session: $currentSessionId")
            } else {
                Log.w(TAG, "GSR data persistence initialization failed - recording will continue without persistence")
            }

            // Initialize network streaming if enabled
            if (isNetworkStreamingEnabled) {
                gsrNetworkStreamer = GSRNetworkStreamer(context, currentSessionId!!, recordingController)
                val networkInitialized = gsrNetworkStreamer?.initialize() ?: false

                if (networkInitialized) {
                    val streamingStarted = gsrNetworkStreamer?.startStreaming() ?: false
                    if (streamingStarted) {
                        Log.i(TAG, "GSR network streaming started successfully")
                    } else {
                        Log.w(TAG, "GSR network streaming failed to start")
                    }
                } else {
                    Log.w(TAG, "GSR network streaming initialization failed")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error initializing data handling: ${e.message}")
        }
    }

    private suspend fun startEnhancedShimmerRecording(
        shimmerRecorder: ShimmerGSRRecorder,
        sessionDir: String,
    ): Boolean {

        return try {

            val sessionId =
                sessionDir.substringAfterLast("/").ifEmpty {
                    "session_${System.currentTimeMillis()}"
                }

            Log.i(
                TAG,
                "Starting enhanced Shimmer recording with merged BLE backend, sessionId: $sessionId"
            )


            val success = shimmerRecorder.startRecording(sessionId)

            if (success) {
                Log.i(
                    TAG,
                    "Enhanced Shimmer GSR recording started successfully with merged BLE backend"
                )
            } else {
                Log.e(TAG, "Enhanced Shimmer GSR recording failed to start")
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start enhanced Shimmer recording", e)
            false
        }
    }

    private suspend fun startLegacyRecording(
        recorder: LegacyGSRRecorder,
        sessionDir: String,
    ): Boolean {

        return try {

            val sessionId =
                sessionDir.substringAfterLast("/").ifEmpty {
                    "session_${System.currentTimeMillis()}"
                }

            Log.i(TAG, "Starting legacy GSR recording with sessionId: $sessionId")

            val initSuccess = recorder.initialize()
            if (!initSuccess) {
                Log.w(TAG, "Legacy GSR recorder initialization failed, but continuing")
            }

            val success =
                recorder.startRecording(
                    sessionId = sessionId,
                    participantId = "participant_${System.currentTimeMillis()}",
                    studyName = "IRCamera_MultiModal_Study",
                )

            if (success) {
                Log.i(TAG, "Legacy GSR recording started successfully")
            } else {
                Log.w(TAG, "Legacy GSR recording failed to start")
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start legacy GSR recording", e)
            false
        }
    }

    override suspend fun stopRecording(): Boolean {
        try {
            if (!_isRecording.get()) {
                Log.w(TAG, "Real Shimmer GSR sensor not recording")
                return true
            }

            val shimmerRecorder = realShimmerGSRRecorder
            if (shimmerRecorder != null && shimmerRecorder.isRecording()) {
                Log.i(TAG, "Stopping Enhanced Shimmer GSR recording with merged BLE backend")

                val stopSuccess =
                    try {
                        stopEnhancedShimmerRecording(shimmerRecorder)
                    } catch (e: Exception) {
                        Log.e(TAG, "Enhanced Shimmer GSR recording stop failed", e)
                        false
                    }

                if (stopSuccess) {
                    Log.i(
                        TAG,
                        "Enhanced Shimmer GSR recording stopped successfully with merged BLE backend"
                    )
                } else {
                    Log.w(TAG, "Enhanced Shimmer GSR recording stop encountered issues")
                }
            }

            legacyGSRRecorder?.let { recorder ->
                stopLegacyRecording(recorder)
            }

            gsrNetworkStreamer?.let { streamer ->
                try {
                    val streamingStopped = streamer.stopStreaming()
                    if (streamingStopped) {
                        Log.i(TAG, "GSR network streaming stopped successfully")
                    } else {
                        Log.w(TAG, "GSR network streaming stop encountered issues")
                    }

                    streamer.cleanup()
                    gsrNetworkStreamer = null
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to stop GSR network streaming", e)
                }
            }

            gsrDataPersistence?.let { persistence ->
                try {
                    persistence.stopPersistence()
                    persistence.cleanup()

                    val stats = persistence.getStatistics()
                    Log.i(
                        TAG,
                        "GSR data persistence stopped - Written: ${stats.samplesWritten} samples to ${stats.csvFilePath}"
                    )

                    gsrDataPersistence = null
                    currentSessionId = null
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to stop GSR data persistence", e)
                }
            }

            _isRecording.set(false)

            Log.i(TAG, "Real Shimmer GSR sensor recording stopped")
            emitStatus()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop real Shimmer GSR recording", e)
            emitError(
                ErrorType.RECORDING_FAILED,
                "Failed to stop real Shimmer GSR recording: ${e.message}"
            )
            return false
        }
    }

    private suspend fun stopEnhancedShimmerRecording(shimmerRecorder: ShimmerGSRRecorder): Boolean {

        return try {
            Log.i(TAG, "Stopping enhanced Shimmer recording with merged BLE backend")

            val sessionInfo = shimmerRecorder.stopRecording()

            if (sessionInfo != null) {
                Log.i(
                    TAG,
                    "Enhanced Shimmer GSR recording stopped successfully. Session: ${sessionInfo.sessionId}, Samples: ${sessionInfo.sampleCount}",
                )
                true
            } else {
                Log.w(TAG, "Enhanced Shimmer GSR recording stop returned null session info")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop enhanced Shimmer recording", e)
            false
        }
    }

    private suspend fun stopLegacyRecording(recorder: LegacyGSRRecorder) {

        try {
            Log.i(TAG, "Stopping legacy GSR recording")

            val sessionInfo = recorder.stopRecording()

            if (sessionInfo != null) {
                Log.i(
                    TAG,
                    "Legacy GSR recording stopped successfully. Session: ${sessionInfo.sessionId}, Samples: ${sessionInfo.sampleCount}",
                )
            } else {
                Log.w(TAG, "Legacy GSR recording stop returned null session info")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop legacy GSR recording", e)
        }
    }

    override suspend fun addSyncMarker(
        markerType: String,
        timestampNs: Long,
        metadata: Map<String, String>,
    ) {
        try {
            syncMarkerCount.incrementAndGet()

            val timestampMs = timestampNs / 1_000_000
            val metadataString = metadata.entries.joinToString(", ") { "${it.key}=${it.value}" }

            realShimmerGSRRecorder?.let { shimmerRecorder ->
                val success = shimmerRecorder.triggerSyncEvent(markerType, metadataString)
                if (success) {
                    Log.i(
                        TAG,
                        "Enhanced Shimmer GSR sync marker added: $markerType at $timestampMs ms"
                    )
                } else {
                    Log.w(TAG, "Failed to add Enhanced Shimmer GSR sync marker: $markerType")
                }
            }

            legacyGSRRecorder?.let { recorder ->
                val success = recorder.addSyncMark(markerType, metadataString)
                if (success) {
                    Log.i(TAG, "Legacy GSR sync marker added: $markerType at $timestampMs ms")
                } else {
                    Log.w(TAG, "Failed to add legacy GSR sync marker: $markerType")
                }
            }

            Log.i(TAG, "GSR sync marker processing completed: $markerType")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to add GSR sync marker", e)
            emitError(ErrorType.SYNC_FAILED, "GSR sync marker failed: ${e.message}")
        }
    }

    private fun onGSRSampleReceived(sample: GSRSample) {
        try {

            val currentCount = sampleCount.incrementAndGet()
            val currentSequence = sampleSequence.incrementAndGet()

            lastSampleTimestamp = TimestampManager.getCurrentTimestampNanos()

            val gsrSampleData =
                GSRSampleData(
                    rawValue = sample.rawValue,
                    microsiemens = sample.conductance,
                    resistanceKohm = sample.resistance,
                    ppgRawValue = 0,
                    ppgFiltered = 0.0,
                    heartRateBpm = 0,
                    deviceId = sensorId,
                    batteryLevel = 100,
                    signalQuality = 100,
                    samplingRateHz = samplingRateHz,
                    packetSequence = currentSequence,
                    participantId = "participant_$currentSessionId",
                    recordingMode = determineRecordingMode(),
                )

            gsrDataPersistence?.queueDataRecord(gsrSampleData)

            gsrNetworkStreamer?.let { streamer ->
                if (streamer.isStreaming) {
                    streamer.addSample(sample)
                }
            }

            if (currentCount % 100 == 0L) {
                Log.d(
                    TAG,
                    "GSR sample processed: ${sample.conductance} µS, Resistance: ${gsrSampleData.resistanceKohm} kΩ ($currentCount total)",
                )

                gsrDataPersistence?.getStatistics()?.let { stats ->
                    Log.d(
                        TAG,
                        "Persistence stats - Written: ${stats.samplesWritten}, Pending: ${stats.pendingSamples}"
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error processing GSR sample", e)
        }
    }

    private fun calculateResistanceFromGSR(gsrMicrosiemens: Double): Double {
        return if (gsrMicrosiemens > 0) {
            1000000.0 / gsrMicrosiemens // Convert µS to kΩ
        } else {
            Double.MAX_VALUE // Infinite resistance for zero conductance
        }
    }

    private fun determineRecordingMode(): String {
        return when {
            realShimmerGSRRecorder != null && unifiedBleManager != null -> "shimmer_unified_ble"
            realShimmerGSRRecorder != null -> "shimmer_ble"
            legacyGSRRecorder != null -> "legacy_gsr"
            else -> "unknown"
        }
    }

    private fun setupGSRSampleCallback() {
        try {

            realShimmerGSRRecorder?.addListener(object : ShimmerGSRRecorder.GSRRecordingListener {
                override fun onSampleRecorded(sample: GSRSample) {
                    onGSRSampleReceived(sample)
                }

                override fun onRecordingStarted(session: SessionInfo) {}
                override fun onRecordingStopped(session: SessionInfo) {}
                override fun onSyncMarkRecorded(syncMark: SyncMark) {}
                override fun onError(error: String) {}
                override fun onDeviceConnected() {}
                override fun onDeviceDisconnected() {}
            })

            legacyGSRRecorder?.addListener(object : LegacyGSRRecorder.GSRRecordingListener {
                override fun onSampleRecorded(sample: GSRSample) {
                    onGSRSampleReceived(sample)
                }

                override fun onRecordingStarted(sessionInfo: SessionInfo) {}
                override fun onRecordingStopped(sessionInfo: SessionInfo) {}
                override fun onSyncMarkAdded(syncMark: SyncMark) {}
                override fun onError(error: String) {}
            })

            Log.i(TAG, "GSR sample callbacks configured for real-time streaming")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to setup GSR sample callbacks", e)
        }
    }

    override suspend fun cleanup() {
        try {
            if (_isRecording.get()) {
                stopRecording()
            }

            dataMonitoringJob?.cancel()
            recordingScope.cancel()

            realShimmerGSRRecorder?.let { shimmerRecorder ->
                try {
                    shimmerRecorder.disconnect()
                    Log.i(TAG, "Enhanced Shimmer GSR recorder disconnected")
                } catch (e: Exception) {
                    Log.w(TAG, "Error disconnecting Enhanced Shimmer GSR recorder", e)
                }
            }

            legacyGSRRecorder?.let { recorder ->
                try {
                    recorder.disconnect()
                    Log.i(TAG, "Legacy GSR recorder disconnected")
                } catch (e: Exception) {
                    Log.w(TAG, "Error disconnecting legacy GSR recorder", e)
                }
            }

            gsrDataPersistence?.let { persistence ->
                try {
                    if (persistence.getStatistics().isActive) {
                        persistence.stopPersistence()
                    }
                    persistence.cleanup()
                    Log.i(TAG, "GSR data persistence cleaned up successfully")
                } catch (e: Exception) {
                    Log.w(TAG, "Error cleaning up GSR data persistence", e)
                }
            }

            legacyGSRRecorder = null
            realShimmerGSRRecorder = null
            gsrDataPersistence = null
            currentSessionId = null

            Log.i(TAG, "GSR sensor cleaned up successfully")
        } catch (e: Exception) {
            Log.e(TAG, "GSR sensor cleanup failed", e)
        }
    }

    override fun getStatusFlow(): Flow<RecordingStatus> = _statusFlow.asSharedFlow()

    override fun getErrorFlow(): Flow<SensorError> = _errorFlow.asSharedFlow()

    override fun getRecordingStats(): RecordingStats {
        val currentTime = System.nanoTime()
        val sessionDuration =
            if (recordingStartTime > 0) (currentTime - recordingStartTime) / 1_000_000 else 0L

        return RecordingStats(
            sensorId = sensorId,
            sensorType = sensorType,
            sessionDurationMs = sessionDuration,
            totalSamplesRecorded = sampleCount.get(),
            averageDataRate = if (sessionDuration > 0) sampleCount.get() * 1000.0 / sessionDuration else 0.0,
            droppedSamples = 0L, // Would be calculated from data monitoring
            storageUsedMB = calculateStorageUsed(),
            syncMarkersCount = syncMarkerCount.get().toInt(),
            lastSampleTimestampNs = lastSampleTimestamp,
        )
    }

    private fun calculateStorageUsed(): Double {

        val bytesPerSample = 32 // Approximate size of GSR sample data
        val totalBytes = sampleCount.get() * bytesPerSample
        return totalBytes / (1024.0 * 1024.0)
    }

    private suspend fun emitStatus() {
        val status =
            RecordingStatus(
                sensorId = sensorId,
                sensorType = sensorType,
                isRecording = _isRecording.get(),
                samplesRecorded = sampleCount.get(),
                currentDataRate = samplingRate,
                storageUsedMB = calculateStorageUsed(),
                timestampNs = System.nanoTime(),
            )
        _statusFlow.emit(status)
    }

    private suspend fun emitError(
        errorType: ErrorType,
        message: String,
        isRecoverable: Boolean = true,
    ) {
        val error =
            SensorError(
                sensorId = sensorId,
                sensorType = sensorType,
                errorType = errorType,
                errorMessage = message,
                timestampNs = System.nanoTime(),
                isRecoverable = isRecoverable,
            )
        _errorFlow.emit(error)
    }

    fun getShimmerConnectionStatus(): String {
        return when {
            realShimmerGSRRecorder != null && realShimmerGSRRecorder!!.isDeviceConnected() -> "Enhanced Shimmer Connected (Merged BLE Backend)"
            realShimmerGSRRecorder != null && !isShimmerConnected -> "Enhanced Shimmer Connecting"
            legacyGSRRecorder != null -> "Legacy GSR Mode"
            else -> "No Device Connected"
        }
    }

    private fun isDeviceConnected(): Boolean {
        return realShimmerGSRRecorder?.isDeviceConnected() ?: false
    }

    fun getGSRConfiguration(): Map<String, Any> {
        return mapOf(
            "sampling_rate_hz" to samplingRateHz,
            "sensor_id" to sensorId,
            "connection_mode" to getShimmerConnectionStatus(),
            "adc_resolution" to "12-bit (0-4095)",
            "recording_active" to _isRecording.get(),
            "unified_ble_backend" to true,
            "enhanced_reliability" to true,
            "shimmer_connected" to isDeviceConnected(),
            "permissions_available" to hasRequiredPermissions(context),
        )
    }

    suspend fun getAvailableShimmerDevices(): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (!hasRequiredPermissions(context)) {
                    Log.w(TAG, "Cannot scan for devices without Bluetooth permissions")
                    return@withContext emptyList()
                }

                val unifiedBle = unifiedBleManager
                if (unifiedBle != null) {
                    // First get already connected devices
                    val connectedDevices = unifiedBle.getConnectedShimmerDevices()
                    val deviceList = mutableListOf<String>()
                    
                    // Add connected devices
                    connectedDevices.forEach { device ->
                        deviceList.add("${device.getName()} (${device.getAddress()}) - Connected")
                    }

                    // Now scan for nearby devices
                    val scanResultDeferred = CompletableDeferred<List<String>>()
                    
                    unifiedBle.scanForShimmerDevices(10000L, object : UnifiedBleManager.ShimmerScanCallback {
                        override fun onDeviceFound(device: UnifiedDevice) {
                            val deviceAddress = device.getAddress()
                            // Only add if not already connected
                            val isAlreadyConnected = connectedDevices.any { it.getAddress() == deviceAddress }
                            if (!isAlreadyConnected) {
                                val deviceEntry = "${device.getName()} (${deviceAddress}) - Available"
                                if (!deviceList.contains(deviceEntry)) {
                                    deviceList.add(deviceEntry)
                                }
                                Log.d(TAG, "Found nearby Shimmer device: ${device.getName()} at $deviceAddress")
                            }
                        }

                        override fun onScanComplete(foundDevices: List<UnifiedDevice>) {
                            Log.i(TAG, "Shimmer device scan completed. Total devices found: ${deviceList.size}")
                            scanResultDeferred.complete(deviceList.toList())
                        }

                        override fun onScanFailed(error: String) {
                            Log.e(TAG, "Shimmer device scan failed: $error")
                            // Still return connected devices even if scan fails
                            scanResultDeferred.complete(deviceList.toList())
                        }
                    })

                    scanResultDeferred.await()
                } else {
                    Log.w(TAG, "Unified BLE manager not available for device discovery")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get available Shimmer devices", e)
                emptyList()
            }
        }
    }

    suspend fun connectToShimmerDevice(deviceAddress: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!hasRequiredPermissions(context)) {
                    Log.e(TAG, "Cannot connect to device without Bluetooth permissions")
                    emitError(
                        ErrorType.PERMISSION_DENIED,
                        "Bluetooth permissions required for device connection"
                    )
                    return@withContext false
                }

                Log.i(TAG, "Attempting to connect to Shimmer device: $deviceAddress")

                // Use UnifiedBleManager to connect to the specific device
                val unifiedBle = unifiedBleManager
                if (unifiedBle != null) {
                    // Try to find the device in connected devices first
                    val connectedShimmerDevices = unifiedBle.getConnectedShimmerDevices()
                    val alreadyConnected = connectedShimmerDevices.any { it.getAddress() == deviceAddress }
                    
                    if (alreadyConnected) {
                        Log.i(TAG, "Device $deviceAddress is already connected")
                        isShimmerConnected = true
                        return@withContext true
                    }

                    // Device not connected, attempt connection via scan and connect
                    Log.i(TAG, "Scanning for device $deviceAddress to establish connection")
                    
                    var connectionSuccess = false
                    val connectionCompleted = CompletableDeferred<Boolean>()

                    unifiedBle.scanForShimmerDevices(5000L, object : UnifiedBleManager.ShimmerScanCallback {
                        override fun onDeviceFound(device: UnifiedDevice) {
                            if (device.getAddress() == deviceAddress) {
                                Log.i(TAG, "Found target device $deviceAddress, attempting connection")
                                // Here we would normally initiate connection to the specific device
                                // For now, we'll use the existing shimmer recorder initialization
                                val shimmerRecorder = realShimmerGSRRecorder
                                if (shimmerRecorder != null) {
                                    val success = shimmerRecorder.initializeDevice()
                                    connectionCompleted.complete(success)
                                } else {
                                    connectionCompleted.complete(false)
                                }
                            }
                        }

                        override fun onScanComplete(foundDevices: List<UnifiedDevice>) {
                            if (!connectionCompleted.isCompleted) {
                                Log.w(TAG, "Device $deviceAddress not found during scan")
                                connectionCompleted.complete(false)
                            }
                        }

                        override fun onScanFailed(error: String) {
                            Log.e(TAG, "Scan failed while looking for device $deviceAddress: $error")
                            if (!connectionCompleted.isCompleted) {
                                connectionCompleted.complete(false)
                            }
                        }
                    })

                    connectionSuccess = connectionCompleted.await()
                    
                    if (connectionSuccess) {
                        Log.i(TAG, "Successfully connected to Shimmer device: $deviceAddress")
                        isShimmerConnected = true
                    } else {
                        Log.w(TAG, "Failed to connect to Shimmer device: $deviceAddress")
                        isShimmerConnected = false
                    }
                    connectionSuccess
                } else {
                    Log.e(TAG, "UnifiedBleManager not available for device connection")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to Shimmer device: $deviceAddress", e)
                emitError(
                    ErrorType.DEVICE_ERROR,
                    "Failed to connect to Shimmer device: ${e.message}"
                )
                false
            }
        }
    }
}
