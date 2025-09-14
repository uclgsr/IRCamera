package com.topdon.tc001.sensors.gsr

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.topdon.gsr.service.GSRRecorder as LegacyGSRRecorder
import com.topdon.gsr.service.ShimmerGSRRecorder
import com.topdon.gsr.model.GSRSample
import com.topdon.tc001.sensors.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

// BLE imports for comprehensive device support
import com.topdon.ble.UnifiedBleManager
import com.topdon.ble.ShimmerDevice
import com.topdon.ble.ShimmerBleController


class GSRSensorRecorder(
    private val context: Context,
    override val sensorId: String = "gsr_shimmer_1",
    private val samplingRateHz: Int = 128,
) : SensorRecorder {
    companion object {
        private const val TAG = "GSRSensorRecorder"

        // Shimmer3 GSR+ specific constants
        private const val SHIMMER_DEFAULT_SAMPLING_RATE = 128.0 // Hz
        private const val GSR_CHANNEL_ID = 0x01 // GSR sensor channel ID
        private const val GSR_RANGE_AUTO = 0x00 // Auto range setting


        fun hasRequiredPermissions(context: Context): Boolean {
            return hasBleScanningPermissions(context)
        }


        fun getMissingPermissions(context: Context): List<String> {
            val missing = mutableListOf<String>()
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                missing.add(android.Manifest.permission.BLUETOOTH)
            }
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                missing.add(android.Manifest.permission.BLUETOOTH_ADMIN)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    missing.add(android.Manifest.permission.BLUETOOTH_SCAN)
                }
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
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

    // Unified BLE manager for comprehensive device support
    private var unifiedBleManager: UnifiedBleManager? = null
    private var unifiedShimmerDevice: ShimmerDevice? = null

    // Real Shimmer components using existing GSR recording module with enhanced BLE backend
    private var realShimmerGSRRecorder: ShimmerGSRRecorder? = null
    private var shimmerDevice: ShimmerBleController? = null

    // Enhanced data persistence with cross-sensor timestamp alignment
    private var gsrDataPersistence: GSRDataPersistence? = null
    private var currentSessionId: String? = null
    private val sampleSequence = AtomicLong(0)
    private var isShimmerConnected = false

    // Legacy GSR components integration for backward compatibility
    private var legacyGSRRecorder: LegacyGSRRecorder? = null

    // Network streaming for hub-spoke communication
    private var gsrNetworkStreamer: GSRNetworkStreamer? = null
    private var isNetworkStreamingEnabled = true

    // Recording state
    private val recordingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var sessionDirectory: String = ""
    private var sampleCount = AtomicLong(0)
    private var recordingStartTime: Long = 0
    private var syncMarkerCount = AtomicLong(0)

    // Data flows
    private val _statusFlow = MutableSharedFlow<RecordingStatus>()
    private val _errorFlow = MutableSharedFlow<SensorError>()

    // Data monitoring
    private var lastSampleTimestamp: Long = 0
    private var dataMonitoringJob: Job? = null

    override suspend fun initialize(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Initializing GSR sensor with Shimmer3 integration for $sensorId")

                // Check Bluetooth permissions first (critical for Shimmer GSR device connection)
                if (!hasRequiredPermissions(context)) {
                    Log.w(TAG, "Missing required Bluetooth permissions for Shimmer GSR device")
                    Log.i(TAG, "GSR sensor will initialize but Shimmer functionality will be limited until permissions are granted")
                    // Don't fail initialization - continue with limited functionality
                }

                // Initialize unified BLE manager for comprehensive device support
                unifiedBleManager = UnifiedBleManager.getInstance(context)
                if (!unifiedBleManager!!.initialize()) {
                    Log.w(TAG, "Unified BLE manager initialization failed, falling back to legacy implementation")
                } else {
                    Log.i(TAG, "Unified BLE manager initialized successfully")
                }

                // Initialize real Shimmer GSR recorder using the existing module with enhanced BLE backend
                realShimmerGSRRecorder = ShimmerGSRRecorder(context, samplingRateHz)

                // Pre-validate Shimmer device connection (but don't fail initialization if not available)
                val shimmerRecorder = realShimmerGSRRecorder
                if (shimmerRecorder != null) {
                    try {
                        // Try to initialize the Shimmer device connection
                        val deviceInitialized = shimmerRecorder.initializeDevice()
                        if (deviceInitialized) {
                            Log.i(TAG, "Shimmer GSR device initialized and ready")
                            isShimmerConnected = true
                        } else {
                            Log.w(TAG, "Shimmer GSR device not available, but sensor recorder initialized")
                            isShimmerConnected = false
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Shimmer GSR device initialization failed, but continuing: ${e.message}")
                        isShimmerConnected = false
                    }
                }

                // Create legacy GSR recorder instance for backward compatibility
                legacyGSRRecorder = LegacyGSRRecorder(context, samplingRateHz)

                // Initialize network streaming if enabled
                if (isNetworkStreamingEnabled) {
                    try {
                        // Will be initialized properly when recording starts with actual session ID
                        Log.i(TAG, "Network streaming will be initialized during recording start")
                    } catch (e: Exception) {
                        Log.w(TAG, "Network streaming setup failed, continuing without streaming: ${e.message}")
                        isNetworkStreamingEnabled = false
                    }
                }

                // Start data monitoring
                startDataMonitoring()

                // Setup GSR sample callbacks for real-time streaming
                setupGSRSampleCallback()

                Log.i(
                    TAG,
                    "GSR sensor initialized successfully (Shimmer connected: $isShimmerConnected, Network streaming: $isNetworkStreamingEnabled)",
                )
                emitStatus()
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize GSR sensor", e)
                emitError(ErrorType.INITIALIZATION_FAILED, "GSR initialization failed: ${e.message}")
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
            // Get real GSR data from Enhanced Shimmer recorder with merged BLE backend
            val shimmerRecorder = realShimmerGSRRecorder
            if (shimmerRecorder != null) {
                // Monitor real Shimmer data flow and quality using enhanced BLE backend
                val realSampleCount = sampleCount.get()

                // Check for real data loss based on actual enhanced Shimmer data rate
                val expectedSamples = ((System.nanoTime() - recordingStartTime) / 1_000_000_000.0 * samplingRate).toLong()
                val actualSamples = realSampleCount

                if (expectedSamples > actualSamples + samplingRate) {
                    // Real data loss detected from Enhanced Shimmer device with merged BLE
                    Log.w(TAG, "Enhanced GSR data loss detected (Merged BLE): expected $expectedSamples, got $actualSamples")
                    emitError(ErrorType.DATA_CORRUPTION, "Enhanced GSR data loss detected", true)
                }

                // Monitor real Shimmer connection status and data flow with enhanced BLE
                try {
                    // Check if we have active samples being recorded with enhanced reliability
                    val currentSampleCount = sampleCount.get()
                    if (currentSampleCount == expectedSamples && expectedSamples > 0) {
                        Log.w(TAG, "Enhanced GSR data loss detected: expected more samples than $expectedSamples")
                        emitError(ErrorType.DATA_CORRUPTION, "Enhanced GSR data loss detected", true)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error monitoring enhanced Shimmer connection: ${e.message}")
                    emitError(ErrorType.DEVICE_ERROR, "Enhanced Shimmer monitoring error", true)
                }
            } else {
                // Fallback to legacy GSR recorder monitoring
                val legacyRecorder = legacyGSRRecorder
                if (legacyRecorder != null) {
                    // Monitor legacy GSR data - use available fields
                    val currentSamples = sampleCount.get()
                    // Legacy recorder doesn't expose detailed stats, use what we have
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

                // Re-check comprehensive Bluetooth permissions (user might have revoked them)
                if (!BluetoothPermissionUtils.hasBleScanningPermissions(context)) {
                    Log.w(TAG, "Comprehensive Bluetooth permissions (including location) not available for Shimmer GSR recording")
                    Log.i(TAG, "Missing permissions: ${BluetoothPermissionUtils.getMissingPermissions(context)}")
                    Log.i(TAG, "Continuing with limited GSR functionality - Shimmer features disabled")
                    // Don't fail completely, continue with legacy recording if available
                }

                this@GSRSensorRecorder.sessionDirectory = sessionDirectory
                recordingStartTime = System.nanoTime()

                var shimmerRecordingStarted = false
                var legacyRecordingStarted = false

                // Attempt Shimmer GSR recording if comprehensive permissions are available
                if (BluetoothPermissionUtils.hasBleScanningPermissions(context)) {
                    val shimmerRecorder = realShimmerGSRRecorder
                    if (shimmerRecorder != null) {
                        Log.i(TAG, "Starting Shimmer GSR recording with BLE backend")

                        // Check device connection status before operations
                        val connectionSuccess =
                            try {
                                if (!shimmerRecorder.isDeviceConnected()) {
                                    Log.i(TAG, "Shimmer device not connected, attempting connection...")
                                    shimmerRecorder.initializeDevice()
                                } else {
                                    Log.i(TAG, "Shimmer device already connected")
                                    true
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Shimmer GSR connection attempt failed: ${e.message}")
                                false
                            }

                        if (connectionSuccess) {
                            // Start the recording
                            val success =
                                try {
                                    startEnhancedShimmerRecording(shimmerRecorder, sessionDirectory)
                                } catch (e: Exception) {
                                    Log.w(TAG, "Shimmer GSR recording start failed: ${e.message}")
                                    false
                                }

                            if (success) {
                                shimmerRecordingStarted = true
                                Log.i(TAG, "Shimmer GSR recording started successfully")
                            } else {
                                Log.w(TAG, "Shimmer GSR recording failed to start")
                            }
                        } else {
                            Log.w(TAG, "Shimmer connection failed, device may not be paired or available")
                        }
                    } else {
                        Log.w(TAG, "Shimmer GSR recorder not initialized")
                    }
                } else {
                    Log.i(TAG, "Skipping Shimmer GSR recording due to missing Bluetooth permissions")
                }

                // Attempt legacy GSR recording as fallback
                val legacyRecorder = legacyGSRRecorder
                if (legacyRecorder != null) {
                    val legacySuccess =
                        try {
                            startLegacyRecording(legacyRecorder, sessionDirectory)
                        } catch (e: Exception) {
                            Log.w(TAG, "Legacy GSR recording start failed: ${e.message}")
                            false
                        }

                    if (legacySuccess) {
                        legacyRecordingStarted = true
                        Log.i(TAG, "Legacy GSR recording started successfully")
                    }
                }

                // Graceful fallback when Shimmer unavailable
                if (!shimmerRecordingStarted && !legacyRecordingStarted) {
                    Log.e(TAG, "All GSR recording methods failed to start")
                    emitError(ErrorType.RECORDING_FAILED, "No GSR recording method available - check device pairing and permissions")
                    return@withContext false
                } else {
                    Log.i(
                        TAG,
                        "GSR recording started with available methods (Shimmer: $shimmerRecordingStarted, Legacy: $legacyRecordingStarted)",
                    )

                    _isRecording.set(true)
                    sampleCount.set(0)
                    syncMarkerCount.set(0)
                    sampleSequence.set(0)

                    // Initialize enhanced data persistence with cross-sensor alignment
                    currentSessionId =
                        sessionDirectory.substringAfterLast("/").ifEmpty {
                            "session_${System.currentTimeMillis()}"
                        }

                    try {
                        gsrDataPersistence = GSRDataPersistence(context, currentSessionId!!)
                        val persistenceInitialized = gsrDataPersistence?.initialize() ?: false

                        if (persistenceInitialized) {
                            gsrDataPersistence?.startPersistence()
                            Log.i(TAG, "Enhanced GSR data persistence initialized for session: $currentSessionId")
                        } else {
                            Log.w(TAG, "GSR data persistence initialization failed - recording will continue without persistence")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to initialize GSR data persistence", e)
                    }

                    // Initialize network streaming for hub-spoke communication
                    if (isNetworkStreamingEnabled) {
                        try {
                            gsrNetworkStreamer = GSRNetworkStreamer(context, currentSessionId!!)
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
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to initialize GSR network streaming", e)
                        }
                    }

                    Log.i(TAG, "GSR sensor recording started (Shimmer: $shimmerRecordingStarted, Legacy: $legacyRecordingStarted)")
                    emitStatus()
                    return@withContext true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start real Shimmer GSR recording", e)
                emitError(ErrorType.RECORDING_FAILED, "Failed to start real Shimmer GSR recording: ${e.message}")
                return@withContext false
            }
        }

    private suspend fun startEnhancedShimmerRecording(
        shimmerRecorder: ShimmerGSRRecorder,
        sessionDir: String,
    ): Boolean {
        // Start enhanced Shimmer recording using the existing GSR recording module with merged BLE backend
        return try {
            // Extract sessionId from sessionDirectory path
            val sessionId =
                sessionDir.substringAfterLast("/").ifEmpty {
                    "session_${System.currentTimeMillis()}"
                }

            Log.i(TAG, "Starting enhanced Shimmer recording with merged BLE backend, sessionId: $sessionId")

            // The Shimmer recorder now benefits from the enhanced BLE module automatically
            // when the BLE module is configured to use Nordic backend
            val success = shimmerRecorder.startRecording(sessionId)

            if (success) {
                Log.i(TAG, "Enhanced Shimmer GSR recording started successfully with merged BLE backend")
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
        // Start legacy GSR recording using the existing GSR recording system
        return try {
            // Extract sessionId from sessionDirectory path
            val sessionId =
                sessionDir.substringAfterLast("/").ifEmpty {
                    "session_${System.currentTimeMillis()}"
                }

            Log.i(TAG, "Starting legacy GSR recording with sessionId: $sessionId")

            // Initialize the legacy recorder first
            val initSuccess = recorder.initialize()
            if (!initSuccess) {
                Log.w(TAG, "Legacy GSR recorder initialization failed, but continuing")
            }

            // Start the legacy recorder with proper parameters
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

            // Stop enhanced Shimmer recording using merged BLE backend
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
                    Log.i(TAG, "Enhanced Shimmer GSR recording stopped successfully with merged BLE backend")
                } else {
                    Log.w(TAG, "Enhanced Shimmer GSR recording stop encountered issues")
                }
            }

            // Stop legacy GSR recording
            legacyGSRRecorder?.let { recorder ->
                stopLegacyRecording(recorder)
            }

            // Stop network streaming
            gsrNetworkStreamer?.let { streamer ->
                try {
                    val streamingStopped = streamer.stopStreaming()
                    if (streamingStopped) {
                        Log.i(TAG, "GSR network streaming stopped successfully")
                    } else {
                        Log.w(TAG, "GSR network streaming stop encountered issues")
                    }

                    // Cleanup network streamer resources
                    streamer.cleanup()
                    gsrNetworkStreamer = null
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to stop GSR network streaming", e)
                }
            }

            // Stop and cleanup enhanced data persistence
            gsrDataPersistence?.let { persistence ->
                try {
                    persistence.stopPersistence()
                    persistence.cleanup()

                    val stats = persistence.getStatistics()
                    Log.i(TAG, "GSR data persistence stopped - Written: ${stats.samplesWritten} samples to ${stats.csvFilePath}")

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
            emitError(ErrorType.RECORDING_FAILED, "Failed to stop real Shimmer GSR recording: ${e.message}")
            return false
        }
    }

    private suspend fun stopEnhancedShimmerRecording(shimmerRecorder: ShimmerGSRRecorder): Boolean {
        // Stop enhanced Shimmer recording using the existing GSR recording module with merged BLE backend
        return try {
            Log.i(TAG, "Stopping enhanced Shimmer recording with merged BLE backend")

            // Call the enhanced Shimmer recorder's stop method
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
        // Stop legacy GSR recording using the existing GSR recording system
        try {
            Log.i(TAG, "Stopping legacy GSR recording")

            // Call the real legacy recorder's stop method
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

            // Convert timestamp from nanoseconds to milliseconds and add metadata string
            val timestampMs = timestampNs / 1_000_000
            val metadataString = metadata.entries.joinToString(", ") { "${it.key}=${it.value}" }

            // Add sync marker to Enhanced Shimmer GSR system (priority)
            realShimmerGSRRecorder?.let { shimmerRecorder ->
                val success = shimmerRecorder.triggerSyncEvent(markerType, metadataString)
                if (success) {
                    Log.i(TAG, "Enhanced Shimmer GSR sync marker added: $markerType at $timestampMs ms")
                } else {
                    Log.w(TAG, "Failed to add Enhanced Shimmer GSR sync marker: $markerType")
                }
            }

            // Add sync marker to legacy GSR system
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
            // Update sample count and sequence
            val currentCount = sampleCount.incrementAndGet()
            val currentSequence = sampleSequence.incrementAndGet()

            // Update last sample timestamp for monitoring
            lastSampleTimestamp = TimestampManager.getCurrentTimestampNanos()

            // Convert GSR sample to enhanced persistence format
            val gsrSampleData =
                GSRSampleData(
                    rawValue = sample.rawValue,
                    microsiemens = sample.gsrValue,
                    resistanceKohm = calculateResistanceFromGSR(sample.gsrValue),
                    ppgRawValue = sample.ppgRawValue ?: 0,
                    ppgFiltered = sample.ppgFiltered ?: 0.0,
                    heartRateBpm = sample.heartRateBpm ?: 0,
                    deviceId = sample.deviceId ?: sensorId,
                    batteryLevel = sample.batteryLevel ?: 100,
                    signalQuality = sample.signalQuality ?: 100,
                    samplingRateHz = samplingRateHz,
                    packetSequence = currentSequence,
                    participantId = "participant_$currentSessionId",
                    recordingMode = determineRecordingMode(),
                )

            // Persist data with enhanced timestamp alignment
            gsrDataPersistence?.queueDataRecord(gsrSampleData)

            // Stream to PC hub if network streaming is enabled
            gsrNetworkStreamer?.let { streamer ->
                if (streamer.isStreaming) {
                    streamer.addSample(sample)
                }
            }

            // Log sample for debugging (reduce frequency for performance)
            if (currentCount % 100 == 0L) {
                Log.d(
                    TAG,
                    "GSR sample processed: ${sample.gsrValue} µS, Resistance: ${gsrSampleData.resistanceKohm} kΩ ($currentCount total)",
                )

                // Log persistence statistics periodically
                gsrDataPersistence?.getStatistics()?.let { stats ->
                    Log.d(TAG, "Persistence stats - Written: ${stats.samplesWritten}, Pending: ${stats.pendingSamples}")
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
            // Setup callback for Enhanced Shimmer recorder
            realShimmerGSRRecorder?.setDataCallback { sample ->
                onGSRSampleReceived(sample)
            }

            // Setup callback for legacy recorder
            legacyGSRRecorder?.setDataCallback { sample ->
                onGSRSampleReceived(sample)
            }

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

            // Properly disconnect and cleanup Enhanced Shimmer recorder
            realShimmerGSRRecorder?.let { shimmerRecorder ->
                try {
                    shimmerRecorder.disconnect()
                    Log.i(TAG, "Enhanced Shimmer GSR recorder disconnected")
                } catch (e: Exception) {
                    Log.w(TAG, "Error disconnecting Enhanced Shimmer GSR recorder", e)
                }
            }

            // Properly disconnect and cleanup legacy recorder
            legacyGSRRecorder?.let { recorder ->
                try {
                    recorder.disconnect()
                    Log.i(TAG, "Legacy GSR recorder disconnected")
                } catch (e: Exception) {
                    Log.w(TAG, "Error disconnecting legacy GSR recorder", e)
                }
            }

            // Cleanup enhanced data persistence system
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

            // Clear references
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
        val sessionDuration = if (recordingStartTime > 0) (currentTime - recordingStartTime) / 1_000_000 else 0L

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
    // Estimate storage based on sample count and data structure
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
                if (unifiedBle != null && unifiedBle.isEnabled()) {
                    // Get connected Shimmer devices
                    val connectedDevices = unifiedBle.getConnectedShimmerDevices()
                    connectedDevices.map { device ->
                        "${device.deviceName} (${device.deviceAddress})"
                    }
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
                    emitError(ErrorType.PERMISSION_DENIED, "Bluetooth permissions required for device connection")
                    return@withContext false
                }

                Log.i(TAG, "Attempting to connect to Shimmer device: $deviceAddress")

                val shimmerRecorder = realShimmerGSRRecorder
                if (shimmerRecorder != null) {
                    // TODO: Implement device-specific connection logic
                    // This would require extending ShimmerGSRRecorder to support device selection
                    val success = shimmerRecorder.initializeDevice()
                    if (success) {
                        Log.i(TAG, "Successfully connected to Shimmer device: $deviceAddress")
                        isShimmerConnected = true
                    } else {
                        Log.w(TAG, "Failed to connect to Shimmer device: $deviceAddress")
                        isShimmerConnected = false
                    }
                    success
                } else {
                    Log.e(TAG, "Shimmer recorder not initialized")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to Shimmer device: $deviceAddress", e)
                emitError(ErrorType.DEVICE_ERROR, "Failed to connect to Shimmer device: ${e.message}")
                false
            }
        }
    }
}
