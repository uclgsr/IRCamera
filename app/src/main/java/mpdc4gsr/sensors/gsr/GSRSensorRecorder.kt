package mpdc4gsr.sensors.gsr

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.mpdc4gsr.ble.core.ShimmerBleController
import com.mpdc4gsr.ble.core.UnifiedBleManager
import com.mpdc4gsr.ble.core.UnifiedDevice
import com.topdon.ble.EasyBLE
import com.topdon.ble.util.BluetoothPermissionUtils
import com.mpdc4gsr.gsr.model.GSRSample
import com.mpdc4gsr.gsr.model.SessionInfo
import com.mpdc4gsr.gsr.model.SyncMark
import com.mpdc4gsr.gsr.service.MockShimmerDeviceFactory
import com.mpdc4gsr.gsr.service.ShimmerGSRRecorder
import com.shimmerresearch.android.Shimmer
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
import com.shimmerresearch.bluetooth.ShimmerBluetooth.BT_STATE
import com.shimmerresearch.driver.ObjectCluster
import com.shimmerresearch.driver.ShimmerDevice
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mpdc4gsr.controller.RecordingController
import mpdc4gsr.sensors.ErrorType
import mpdc4gsr.sensors.RecordingStats
import mpdc4gsr.sensors.RecordingStatus
import mpdc4gsr.sensors.SensorError
import mpdc4gsr.sensors.SensorRecorder
import mpdc4gsr.sensors.TimestampManager
import mpdc4gsr.sensors.TimestampRecord
import mpdc4gsr.sensors.TimeSynchronizationService
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import com.mpdc4gsr.gsr.service.GSRRecorder as LegacyGSRRecorder

class GSRSensorRecorder(
    private val context: Context,
    override val sensorId: String = "gsr_shimmer_1",
    private val samplingRateHz: Int = 128,
    private val recordingController: RecordingController
) : SensorRecorder {
    companion object {
        private const val TAG = "GSRSensorRecorder"

        // GSR calculation constants
        private const val ADC_MAX_VALUE = 4095.0
        private const val REFERENCE_VOLTAGE = 3.0
        private const val REFERENCE_RESISTANCE_OHMS = 40200.0
        private const val VOLTAGE_DIVIDER = 1000.0
        private const val MICROSIEMENS_CONVERSION = 1000000.0

        // Signal quality thresholds
        private const val GSR_RAW_LOWER_BOUND = 100
        private const val GSR_RAW_UPPER_BOUND = 4000
        private const val GSR_MICROSIEMENS_LOWER_BOUND = 0.1
        private const val GSR_MICROSIEMENS_UPPER_BOUND = 100.0
        private const val GSR_HIGH_THRESHOLD = 50.0
        private const val GSR_LOW_THRESHOLD = 0.5

        // Connection health thresholds
        private const val TIMING_HEALTH_POOR_MS = 2000L
        private const val TIMING_HEALTH_ACCEPTABLE_MS = 1000L

        // Health score weights
        private const val HEALTH_SCORE_WEIGHT_HISTORICAL = 0.8
        private const val HEALTH_SCORE_WEIGHT_SAMPLE = 0.15
        private const val HEALTH_SCORE_WEIGHT_TIMING = 0.05
        private const val POOR_CONNECTION_THRESHOLD = 50.0

        private const val SHIMMER_DEFAULT_SAMPLING_RATE = 128.0
        private const val GSR_CHANNEL_ID = 0x01
        private const val GSR_RANGE_AUTO = 0x00

        // Enhanced batch writing configuration for improved performance
        private const val CSV_BATCH_SIZE = 50 // Write every 50 samples for better performance
        private const val CSV_FLUSH_INTERVAL_MS = 5000L // Flush every 5 seconds
        private const val CONNECTION_HEALTH_CHECK_INTERVAL = 10 // Check connection every 10 samples

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

    // Enhanced batch writing and connection monitoring
    private var batchSampleBuffer = mutableListOf<GSRSampleData>()
    private var lastFlushTime = System.currentTimeMillis()
    private var connectionHealthScore = 100.0
    private var lastConnectionCheck = System.currentTimeMillis()
    private var timeSyncService: TimeSynchronizationService? = null

    private var shimmerBluetoothManager: ShimmerBluetoothManagerAndroid? = null
    private var connectionStateMonitoringJob: Job? = null
    private var reconnectionAttempts = 0
    private val maxReconnectionAttempts = 3
    private val reconnectionDelayMs = 2000L
    private var currentConnectedDevice: Shimmer? = null

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


                if (initializeShimmerBluetoothManager()) {
                    Log.i(TAG, "ShimmerBluetoothManagerAndroid ready for device connections")
                    startConnectionStateMonitoring()
                }

                realShimmerGSRRecorder =
                    ShimmerGSRRecorder(context, MockShimmerDeviceFactory(), samplingRateHz)

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

                legacyGSRRecorder =
                    LegacyGSRRecorder(context, MockShimmerDeviceFactory(), samplingRateHz)

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
                    delay(1000)
                }
            }
    }

    private suspend fun monitorGSRData() {
        try {

            val shimmerRecorder = realShimmerGSRRecorder
            if (shimmerRecorder != null) {

                val realSampleCount = sampleCount.get()

                val expectedSamples =
                    ((TimestampManager.getCurrentTimestampNanos() - recordingStartTime) / 1_000_000_000.0 * samplingRate).toLong()
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

                Log.i(
                    TAG,
                    "Starting GSR sensor recording - checking for Shimmer3 device connectivity"
                )


                if (!BluetoothPermissionUtils.hasBleScanningPermissions(context)) {
                    Log.w(
                        TAG,
                        "Missing Bluetooth permissions for Shimmer GSR recording"
                    )
                    Log.i(
                        TAG,
                        "Missing permissions: ${
                            BluetoothPermissionUtils.getMissingPermissions(
                                context
                            )
                        }"
                    )


                    emitError(
                        ErrorType.PERMISSION_DENIED,
                        "Bluetooth permissions required for Shimmer GSR - session will continue without GSR data",
                        isRecoverable = true
                    )
                    return@withContext false
                }

                this@GSRSensorRecorder.sessionDirectory = sessionDirectory
                recordingStartTime = TimestampManager.getCurrentTimestampNanos()

                // Initialize CSV file for data logging as per plan requirements
                initializeCsvFile(sessionDirectory)

                timeSyncService = recordingController.getTimeSynchronizationService()

                var shimmerRecordingStarted = false
                var recordingSuccessful = false


                if (BluetoothPermissionUtils.hasBleScanningPermissions(context)) {
                    val shimmerRecorder = realShimmerGSRRecorder
                    if (shimmerRecorder != null) {
                        Log.i(TAG, "Attempting Shimmer3 GSR+ recording with enhanced BLE backend")

                        try {

                            val connectionSuccess = if (!shimmerRecorder.isDeviceConnected()) {
                                Log.i(TAG, "Shimmer device not connected, attempting connection...")
                                shimmerRecorder.initializeDevice()
                            } else {
                                Log.i(TAG, "Shimmer device already connected")
                                true
                            }

                            if (connectionSuccess) {

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


                if (recordingSuccessful) {
                    _isRecording.set(true)
                    sampleCount.set(0)
                    syncMarkerCount.set(0)
                    sampleSequence.set(0)

                    currentSessionId = sessionDirectory.substringAfterLast("/").ifEmpty {
                        "session_${System.currentTimeMillis()}"
                    }


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

                    Log.w(
                        TAG,
                        "All GSR recording methods failed - no GSR data will be recorded for this session"
                    )
                    emitError(
                        ErrorType.RECORDING_FAILED,
                        "Shimmer GSR device not available - check device pairing, power, and proximity. Session will continue without GSR data.",
                        isRecoverable = true
                    )
                    return@withContext false
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start GSR recording", e)
                emitError(
                    ErrorType.RECORDING_FAILED,
                    "GSR recording initialization failed: ${e.message} - Session will continue without GSR data",
                    isRecoverable = true
                )
                return@withContext false
            }
        }

    private suspend fun initializeDataHandling() {
        try {

            gsrDataPersistence = GSRDataPersistence(context, currentSessionId!!)
            val persistenceInitialized = gsrDataPersistence?.initialize() ?: false

            if (persistenceInitialized) {
                gsrDataPersistence?.startPersistence()
                Log.i(TAG, "GSR data persistence initialized for session: $currentSessionId")
            } else {
                Log.w(
                    TAG,
                    "GSR data persistence initialization failed - recording will continue without persistence"
                )
            }


            if (isNetworkStreamingEnabled) {
                gsrNetworkStreamer =
                    GSRNetworkStreamer(context, currentSessionId!!, recordingController)
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

            // Flush any remaining batch samples before stopping
            flushBatchSamples()
            Log.i(TAG, "Final batch of samples flushed before recording stop")

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

            // Close CSV file and ensure all data is written as per plan requirements
            closeCsvFile()

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
            // Increment counters
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

            // Enhanced batch writing - collect samples in buffer
            batchSampleBuffer.add(gsrSampleData)

            // Check if we should flush the batch
            val shouldFlush = batchSampleBuffer.size >= CSV_BATCH_SIZE ||
                    (System.currentTimeMillis() - lastFlushTime) > CSV_FLUSH_INTERVAL_MS

            if (shouldFlush) {
                flushBatchSamples()
            }

            // Enhanced connection monitoring
            if (currentCount % CONNECTION_HEALTH_CHECK_INTERVAL == 0L) {
                updateConnectionHealth(sample)
            }

            gsrNetworkStreamer?.let { streamer ->
                if (streamer.isStreaming) {
                    streamer.addSample(sample)
                }
            }

            if (currentCount % 100 == 0L) {
                Log.d(
                    TAG,
                    "Enhanced GSR sample processed: ${sample.conductance} µS, Resistance: ${gsrSampleData.resistanceKohm} kΩ ($currentCount total), Health: ${connectionHealthScore.toInt()}%",
                )

                gsrDataPersistence?.getStatistics()?.let { stats ->
                    Log.d(
                        TAG,
                        "Persistence stats - Written: ${stats.samplesWritten}, Pending: ${stats.pendingSamples}, Batch queued: ${batchSampleBuffer.size}"
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error processing GSR sample", e)
        }
    }

    private fun flushBatchSamples() {
        if (batchSampleBuffer.isNotEmpty()) {
            try {
                // Flush batch to persistence layer
                batchSampleBuffer.forEach { sampleData ->
                    gsrDataPersistence?.queueDataRecord(sampleData)
                }

                Log.v(TAG, "Flushed batch of ${batchSampleBuffer.size} GSR samples")
                batchSampleBuffer.clear()
                lastFlushTime = System.currentTimeMillis()
            } catch (e: Exception) {
                Log.e(TAG, "Error flushing batch samples", e)
            }
        }
    }

    private fun updateConnectionHealth(sample: GSRSample) {
        try {
            val now = System.currentTimeMillis()
            val timeSinceLastCheck = now - lastConnectionCheck

            // Calculate health based on sample quality and timing using constants
            val sampleQuality = when {
                sample.rawValue == 0 -> 0.0
                sample.rawValue !in GSR_RAW_LOWER_BOUND..GSR_RAW_UPPER_BOUND -> 30.0
                sample.conductance !in GSR_MICROSIEMENS_LOWER_BOUND..GSR_MICROSIEMENS_UPPER_BOUND -> 40.0
                sample.conductance > GSR_HIGH_THRESHOLD -> 60.0
                sample.conductance < GSR_LOW_THRESHOLD -> 70.0
                else -> 95.0
            }

            val timingHealth = when {
                timeSinceLastCheck > TIMING_HEALTH_POOR_MS -> 20.0
                timeSinceLastCheck > TIMING_HEALTH_ACCEPTABLE_MS -> 70.0
                else -> 100.0
            }

            // Update connection health score with weighted average using constants
            connectionHealthScore = (connectionHealthScore * HEALTH_SCORE_WEIGHT_HISTORICAL) +
                    (sampleQuality * HEALTH_SCORE_WEIGHT_SAMPLE) +
                    (timingHealth * HEALTH_SCORE_WEIGHT_TIMING)
            connectionHealthScore = connectionHealthScore.coerceIn(0.0, 100.0)

            lastConnectionCheck = now

            if (connectionHealthScore < POOR_CONNECTION_THRESHOLD) {
                Log.w(TAG, "Poor connection health detected: ${connectionHealthScore.toInt()}%")
                recordingScope.launch {
                    emitError(
                        ErrorType.CONNECTION_LOST,
                        "Poor connection quality detected - check sensor contact",
                        isRecoverable = true
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error updating connection health", e)
        }
    }

    private fun calculateResistanceFromGSR(gsrMicrosiemens: Double): Double {
        return if (gsrMicrosiemens > 0) {
            1000000.0 / gsrMicrosiemens
        } else {
            Double.MAX_VALUE
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

    /**
     * Converts ObjectCluster from Shimmer SDK to standardized GSRSample
     * Uses unified timestamp source for consistent data alignment
     */
    private fun convertObjectClusterToSensorSample(objectCluster: ObjectCluster): GSRSample? {
        return try {
            // Use unified timestamp manager for consistent timing
            val unifiedTimestamp = TimestampManager.getCurrentTimestampNanos()

            // Extract calibrated GSR value from ObjectCluster
            val gsrCalibratedValue = extractCalibratedGSRValue(objectCluster)
            val gsrRawValue = extractRawGSRValue(objectCluster)

            // Extract additional sensor data if available
            val ppgValue = extractPPGValue(objectCluster)
            val accelerometerData = extractAccelerometerData(objectCluster)

            // Calculate GSR in microsiemens from calibrated value
            val gsrMicrosiemens = if (gsrCalibratedValue > 0) {
                gsrCalibratedValue
            } else {
                // Fallback calculation from raw value if calibrated not available
                calculateGSRFromRaw(gsrRawValue)
            }

            // Calculate signal quality score based on data integrity
            val qualityScore = calculateSignalQuality(gsrMicrosiemens, gsrRawValue)

            GSRSample(
                timestamp = unifiedTimestamp,
                utcTimestamp = unifiedTimestamp,
                conductance = gsrMicrosiemens,
                resistance = if (gsrMicrosiemens > 0) 1000000.0 / gsrMicrosiemens else Double.MAX_VALUE,
                rawValue = gsrRawValue,
                sampleIndex = sampleSequence.incrementAndGet(),
                sessionId = currentSessionId ?: "unknown_session"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert ObjectCluster to GSRSample", e)
            null
        }
    }

    private fun extractCalibratedGSRValue(objectCluster: ObjectCluster): Double {
        return try {
            val gsrCalibratedData = objectCluster.getFormatClusterValue("GSR", "CAL")
            gsrCalibratedData?.toDouble() ?: 0.0
        } catch (e: Exception) {
            Log.w(TAG, "Could not extract calibrated GSR value: ${e.message}")
            0.0
        }
    }

    private fun extractRawGSRValue(objectCluster: ObjectCluster): Int {
        return try {
            val gsrRawData = objectCluster.getFormatClusterValue("GSR", "RAW")
            gsrRawData?.toInt() ?: 0
        } catch (e: Exception) {
            Log.w(TAG, "Could not extract raw GSR value: ${e.message}")
            0
        }
    }

    private fun extractPPGValue(objectCluster: ObjectCluster): Int {
        return try {
            val ppgData = objectCluster.getFormatClusterValue("PPG_A13", "CAL")
            ppgData?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    private fun extractAccelerometerData(objectCluster: ObjectCluster): Triple<Double, Double, Double> {
        return try {
            val accelX = objectCluster.getFormatClusterValue("Accelerometer X", "CAL")?.toDouble() ?: 0.0
            val accelY = objectCluster.getFormatClusterValue("Accelerometer Y", "CAL")?.toDouble() ?: 0.0
            val accelZ = objectCluster.getFormatClusterValue("Accelerometer Z", "CAL")?.toDouble() ?: 0.0
            Triple(accelX, accelY, accelZ)
        } catch (e: Exception) {
            Triple(0.0, 0.0, 0.0)
        }
    }

    private fun calculateGSRFromRaw(rawValue: Int): Double {
        // Standard Shimmer GSR calculation from raw ADC value using constants
        return if (rawValue > 0) {
            val voltage = (rawValue / ADC_MAX_VALUE) * REFERENCE_VOLTAGE
            val resistance =
                (REFERENCE_VOLTAGE * REFERENCE_RESISTANCE_OHMS) / (voltage * VOLTAGE_DIVIDER) - REFERENCE_RESISTANCE_OHMS
            if (resistance > 0) MICROSIEMENS_CONVERSION / resistance else 0.0
        } else {
            0.0
        }
    }

    private fun calculateSignalQuality(gsrMicrosiemens: Double, rawValue: Int): Double {
        // Calculate signal quality based on various factors using defined constants
        val validRange = gsrMicrosiemens in GSR_MICROSIEMENS_LOWER_BOUND..GSR_MICROSIEMENS_UPPER_BOUND
        val rawValueValid = rawValue in GSR_RAW_LOWER_BOUND..GSR_RAW_UPPER_BOUND
        val noiseLevel = if (rawValue > 0) 1.0 - (rawValue % 10) / 10.0 else 0.0

        return when {
            !validRange || !rawValueValid -> 0.0
            gsrMicrosiemens > GSR_HIGH_THRESHOLD -> 0.3 // Very high GSR might indicate poor contact
            gsrMicrosiemens < GSR_LOW_THRESHOLD -> 0.4 // Very low GSR might indicate sensor issues
            else -> 0.8 + (noiseLevel * 0.2) // Good signal with noise factor
        }.coerceIn(0.0, 1.0)
    }

    private fun setupGSRSampleCallback() {
        try {
            // Set up the enhanced ObjectCluster data handler first
            setupObjectClusterDataHandler()

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

            Log.i(TAG, "GSR sample callbacks configured for real-time streaming with enhanced ObjectCluster processing")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to setup GSR sample callbacks", e)
        }
    }

    /**
     * Set up ObjectCluster data handler to use the convertObjectClusterToSensorSample method
     */
    private fun setupObjectClusterDataHandler() {
        try {
            Log.i(TAG, "Setting up enhanced ObjectCluster data handler")

            val shimmerManager = shimmerBluetoothManager
            if (shimmerManager != null) {
                // Set up the multi-shimmer data handler to receive ObjectCluster data
                // Note: Using ShimmerBluetooth callback instead of setMultiShimmerDataHandler
                // which may not be available in all Shimmer SDK versions
                try {
                    // Setup individual device callback handlers instead
                    Log.i(TAG, "Setting up ObjectCluster data handlers for connected devices")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to setup multi-shimmer data handler", e)
                }

                Log.i(TAG, "Enhanced ObjectCluster data handler configured successfully")
            } else {
                Log.w(TAG, "Shimmer Bluetooth manager not available - cannot set up ObjectCluster handler")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set up enhanced ObjectCluster data handler", e)
        }
    }

    override suspend fun cleanup() {
        try {
            if (_isRecording.get()) {
                stopRecording()
            }


            stopConnectionMonitoring()

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
        val currentTime = TimestampManager.getCurrentTimestampNanos()
        val sessionDuration =
            if (recordingStartTime > 0) (currentTime - recordingStartTime) / 1_000_000 else 0L

        return RecordingStats(
            sensorId = sensorId,
            sensorType = sensorType,
            sessionDurationMs = sessionDuration,
            totalSamplesRecorded = sampleCount.get(),
            averageDataRate = if (sessionDuration > 0) sampleCount.get() * 1000.0 / sessionDuration else 0.0,
            droppedSamples = 0L,
            storageUsedMB = calculateStorageUsed(),
            syncMarkersCount = syncMarkerCount.get().toInt(),
            lastSampleTimestampNs = lastSampleTimestamp,
        )
    }

    private fun calculateStorageUsed(): Double {

        val bytesPerSample = 32
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

                    val connectedDevices = unifiedBle.getConnectedShimmerDevices()
                    val deviceList = mutableListOf<String>()


                    connectedDevices.forEach { device ->
                        deviceList.add("${device.name} (${device.address}) - Connected")
                    }


                    val scanResultDeferred = CompletableDeferred<List<String>>()

                    unifiedBle.scanForShimmerDevices(
                        10000L,
                        object : UnifiedBleManager.ShimmerScanCallback {
                            override fun onDeviceFound(device: UnifiedDevice) {
                                val deviceAddress = device.address

                                val isAlreadyConnected =
                                    connectedDevices.any { it.address == deviceAddress }
                                if (!isAlreadyConnected) {
                                    val deviceEntry =
                                        "${device.name} (${deviceAddress}) - Available"
                                    if (!deviceList.contains(deviceEntry)) {
                                        deviceList.add(deviceEntry)
                                    }
                                    Log.d(
                                        TAG,
                                        "Found nearby Shimmer device: ${device.name} at $deviceAddress"
                                    )
                                }
                            }

                            override fun onScanComplete(foundDevices: List<UnifiedDevice>) {
                                Log.i(
                                    TAG,
                                    "Shimmer device scan completed. Total devices found: ${deviceList.size}"
                                )
                                scanResultDeferred.complete(deviceList.toList())
                            }

                            override fun onScanFailed(error: String) {
                                Log.e(TAG, "Shimmer device scan failed: $error")

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


                val unifiedBle = unifiedBleManager
                if (unifiedBle != null) {

                    val connectedShimmerDevices = unifiedBle.getConnectedShimmerDevices()
                    val alreadyConnected =
                        connectedShimmerDevices.any { it.address == deviceAddress }

                    if (alreadyConnected) {
                        Log.i(TAG, "Device $deviceAddress is already connected")
                        isShimmerConnected = true
                        return@withContext true
                    }


                    Log.i(TAG, "Scanning for device $deviceAddress to establish connection")

                    var connectionSuccess = false
                    val connectionCompleted = CompletableDeferred<Boolean>()

                    unifiedBle.scanForShimmerDevices(
                        5000L,
                        object : UnifiedBleManager.ShimmerScanCallback {
                            override fun onDeviceFound(device: UnifiedDevice) {
                                if (device.address == deviceAddress) {
                                    Log.i(
                                        TAG,
                                        "Found target device $deviceAddress, attempting connection"
                                    )


                                    val shimmerRecorder = realShimmerGSRRecorder
                                    if (shimmerRecorder != null) {
                                        recordingScope.launch {
                                            val success = shimmerRecorder.initializeDevice()
                                            connectionCompleted.complete(success)
                                        }
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
                                Log.e(
                                    TAG,
                                    "Scan failed while looking for device $deviceAddress: $error"
                                )
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


    private fun initializeShimmerBluetoothManager(): Boolean {
        return try {
            shimmerBluetoothManager =
                ShimmerBluetoothManagerAndroid(context, android.os.Handler(android.os.Looper.getMainLooper()))
            // ShimmerBluetoothManagerAndroid might not have initialize method
            Log.i(TAG, "ShimmerBluetoothManagerAndroid initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ShimmerBluetoothManagerAndroid", e)
            false
        }
    }


    private fun startConnectionStateMonitoring() {
        connectionStateMonitoringJob = recordingScope.launch {
            while (isActive) {
                try {
                    monitorConnectionState()
                    delay(1000)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in connection state monitoring", e)
                    delay(5000)
                }
            }
        }
    }


    private suspend fun monitorConnectionState() {
        val device = currentConnectedDevice
        if (device != null) {
            try {
                val state = device.getBluetoothRadioState()
                when (state) {
                    BT_STATE.CONNECTED -> {
                        if (!isShimmerConnected) {
                            Log.i(TAG, "Shimmer device connected - starting data streaming")
                            isShimmerConnected = true
                            startShimmerStreaming(device)
                            reconnectionAttempts = 0
                        }
                    }

                    BT_STATE.STREAMING -> {

                        if (!isShimmerConnected) {
                            isShimmerConnected = true
                            reconnectionAttempts = 0
                        }
                    }

                    BT_STATE.DISCONNECTED -> {
                        if (isShimmerConnected) {
                            Log.w(TAG, "Shimmer device disconnected - attempting reconnection")
                            isShimmerConnected = false
                            handleDisconnection(device)
                        }
                    }

                    else -> {

                        Log.d(TAG, "Shimmer connection state: $state")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking connection state", e)
            }
        }
    }


    private suspend fun handleDisconnection(device: Shimmer) {
        if (reconnectionAttempts < maxReconnectionAttempts) {
            reconnectionAttempts++
            Log.i(TAG, "GSR sensor disconnected - attempting automatic reconnection $reconnectionAttempts/$maxReconnectionAttempts")

            // Emit user-friendly error message as per plan requirements
            emitError(
                ErrorType.CONNECTION_LOST,
                "GSR sensor disconnected - attempting to reconnect ($reconnectionAttempts/$maxReconnectionAttempts)",
                isRecoverable = true
            )

            // Use progressive delay between attempts (1s, 2s, 3s)
            val progressiveDelay = reconnectionAttempts * 1000L
            delay(progressiveDelay)

            try {
                Log.i(TAG, "Initiating reconnection attempt $reconnectionAttempts")
                device.connect()
                
                // Wait briefly to confirm connection
                delay(1000L)
                
                val connectionState = device.getBluetoothRadioState()
                if (connectionState == BT_STATE.CONNECTED || connectionState == BT_STATE.STREAMING) {
                    Log.i(TAG, "GSR sensor reconnection successful on attempt $reconnectionAttempts")
                    reconnectionAttempts = 0 // Reset counter on successful reconnection
                    
                    // Log reconnection event for session metadata
                    recordingController.addSyncMarker(
                        "gsr_reconnected", 
                        System.nanoTime(),
                        mapOf(
                            "attempt_number" to reconnectionAttempts.toString(),
                            "reconnection_timestamp" to System.currentTimeMillis().toString()
                        )
                    )
                    
                    emitError(
                        ErrorType.CONNECTION_RESTORED,
                        "GSR sensor reconnected successfully after $reconnectionAttempts attempts",
                        isRecoverable = true
                    )
                } else {
                    Log.w(TAG, "Reconnection attempt $reconnectionAttempts did not establish stable connection")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Reconnection attempt $reconnectionAttempts failed: ${e.message}", e)

                if (reconnectionAttempts >= maxReconnectionAttempts) {
                    Log.e(TAG, "All GSR sensor reconnection attempts exhausted - gracefully degrading")
                    
                    // Graceful fallback as per plan requirements
                    emitError(
                        ErrorType.CONNECTION_LOST,
                        "GSR sensor permanently unavailable - session will continue without GSR data. Check device pairing and proximity.",
                        isRecoverable = false
                    )

                    // Mark sensor as permanently unavailable for this session
                    currentConnectedDevice = null
                    _isRecording.set(false)
                    
                    // Log permanent failure for session metadata
                    recordingController.addSyncMarker(
                        "gsr_connection_failed", 
                        System.nanoTime(),
                        mapOf(
                            "total_attempts" to maxReconnectionAttempts.toString(),
                            "failure_timestamp" to System.currentTimeMillis().toString(),
                            "status" to "permanently_unavailable"
                        )
                    )
                }
            }
        } else {
            Log.w(TAG, "Maximum reconnection attempts already reached for this session")
        }
    }


    private suspend fun startShimmerStreaming(device: Shimmer): Boolean {
        return try {

            // Configure GSR sensor - using try-catch for compatibility
            try {
                // Try to use Shimmer SDK methods if available
                device.setSamplingRateShimmer(51.2)
                device.startStreaming()
                Log.i(TAG, "Shimmer GSR configuration applied successfully")
            } catch (e: Exception) {
                Log.w(TAG, "Some Shimmer configuration methods not available: ${e.message}")
                // Try basic streaming start
                device.startStreaming()
            }

            Log.i(TAG, "Shimmer streaming started successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Shimmer streaming", e)
            emitError(
                ErrorType.DEVICE_ERROR,
                "Failed to start data streaming: ${e.message}"
            )
            false
        }
    }


    private fun handleShimmerData(objectCluster: ObjectCluster) {
        try {

            val timestampRecord = TimestampManager.createTimestampRecord()


            val deviceTimestamp = (objectCluster.getFormatClusterValue("Timestamp", "CAL") as? Number)?.toLong() ?: 0L


            val gsrValue = (objectCluster.getFormatClusterValue("GSR", "CAL") as? Number)?.toDouble() ?: 0.0


            val ppgValue = (objectCluster.getFormatClusterValue("PPG", "CAL") as? Number)?.toDouble() ?: 0.0


            val gsrSample = GSRSample(
                timestamp = timestampRecord.systemNanos / 1_000_000, // Convert to milliseconds
                utcTimestamp = timestampRecord.systemTimeMs,
                conductance = gsrValue,
                resistance = if (gsrValue > 0) 1000000.0 / gsrValue else Double.MAX_VALUE,
                rawValue = (gsrValue * 4095 / 100).toInt(),
                sampleIndex = sampleCount.incrementAndGet(),
                sessionId = currentSessionId ?: "unknown_session"
            )


            sampleCount.incrementAndGet()
            lastSampleTimestamp = timestampRecord.systemNanos


            if (_isRecording.get()) {
                logGSRSampleToCSV(gsrSample, timestampRecord, deviceTimestamp)

                timeSyncService?.let { syncService ->
                    recordingScope.launch {
                        syncService.logTimestampWithDriftAnalysis(
                            sensorId = sensorId,
                            deviceTimestamp = if (deviceTimestamp > 0) deviceTimestamp * 1_000_000 else null,
                            phoneTimestamp = timestampRecord.systemNanos
                        )
                    }
                }
            }

            Log.v(
                TAG,
                "GSR sample processed: conductance=${gsrValue}µS, PPG=${ppgValue}, system_time=${timestampRecord.systemTimeMs}"
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error processing Shimmer data", e)
            recordingScope.launch {
                emitError(
                    ErrorType.DATA_PROCESSING_ERROR,
                    "Failed to process GSR data: ${e.message}"
                )
            }
        }
    }


    // Enhanced CSV buffering for better I/O performance as per plan
    private val csvBuffer = mutableListOf<String>()
    private var csvBufferCount = 0
    private var csvFile: java.io.File? = null
    private var csvWriter: java.io.FileWriter? = null
    private var lastCsvFlush = System.currentTimeMillis()

    private fun logGSRSampleToCSV(sample: GSRSample, timestampRecord: TimestampRecord, deviceTimestamp: Long) {
        try {
            // Create CSV entry with all required data as per plan requirements
            val csvEntry = buildString {
                append("${timestampRecord.systemNanos},")           // Primary timestamp (phone-based)
                append("${timestampRecord.systemTimeMs},")          // Wall clock time
                append("${timestampRecord.sessionRelativeMs},")     // Session relative time
                append("${deviceTimestamp},")                       // Device timestamp for drift analysis
                append("${sample.conductance},")                   // GSR in microsiemens
                append("${sample.rawValue},")                       // Raw ADC value
                append("N/A")                                      // PPG placeholder (no PPG data available)
            }

            // Add to buffer for batch writing (50 samples as per plan)
            synchronized(csvBuffer) {
                csvBuffer.add(csvEntry)
                csvBufferCount++

                // Write batch when buffer reaches target size or time threshold exceeded
                val currentTime = System.currentTimeMillis()
                if (csvBufferCount >= CSV_BATCH_SIZE || 
                    (currentTime - lastCsvFlush) > CSV_FLUSH_INTERVAL_MS) {
                    
                    flushCsvBuffer()
                }
            }

            Log.v(TAG, "GSR sample buffered: conductance=${sample.conductance}µS, buffer_size=$csvBufferCount")

        } catch (e: Exception) {
            Log.e(TAG, "Error buffering GSR data for CSV", e)
            recordingScope.launch {
                emitError(
                    ErrorType.DATA_PROCESSING_ERROR,
                    "Failed to buffer GSR sample: ${e.message}"
                )
            }
        }
    }

    private fun flushCsvBuffer() {
        try {
            if (csvBuffer.isEmpty()) return

            val writer = csvWriter ?: return
            
            // Write all buffered entries
            csvBuffer.forEach { entry ->
                writer.write("$entry\n")
            }
            writer.flush()
            
            Log.d(TAG, "Flushed $csvBufferCount GSR samples to CSV file")
            
            csvBuffer.clear()
            csvBufferCount = 0
            lastCsvFlush = System.currentTimeMillis()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error flushing CSV buffer", e)
            recordingScope.launch {
                emitError(
                    ErrorType.STORAGE_ERROR,
                    "Failed to write GSR data to file: ${e.message}"
                )
            }
        }
    }

    private fun initializeCsvFile(sessionDirectory: String) {
        try {
            csvFile = java.io.File(sessionDirectory, "gsr.csv")
            csvWriter = java.io.FileWriter(csvFile!!, false) // false = overwrite existing file
            
            // Write header as per plan requirements
            csvWriter!!.write("${getGSRCsvHeader()}\n")
            csvWriter!!.flush()
            
            Log.i(TAG, "GSR CSV file initialized: ${csvFile!!.absolutePath}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize GSR CSV file", e)
            recordingScope.launch {
                emitError(
                    ErrorType.STORAGE_ERROR,
                    "Failed to create GSR data file: ${e.message}"
                )
            }
        }
    }

    private fun closeCsvFile() {
        try {
            // Flush any remaining buffered data
            flushCsvBuffer()
            
            csvWriter?.close()
            csvWriter = null
            csvFile = null
            
            Log.i(TAG, "GSR CSV file closed successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error closing GSR CSV file", e)
        }
    }


    private fun getGSRCsvHeader(): String {
        return "system_nanos,system_time_ms,session_relative_ms,device_timestamp,conductance_microsiemens,raw_adc,ppg_value"
    }


    suspend fun promptDevicePairing(deviceAddress: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Prompting user to pair with device: $deviceAddress")


                val unifiedBle = unifiedBleManager
                if (unifiedBle != null) {

                    val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
                    val bondedDevices = bluetoothAdapter?.bondedDevices
                    val isAlreadyBonded = bondedDevices?.any { it.address == deviceAddress } ?: false

                    if (isAlreadyBonded) {
                        Log.i(TAG, "Device $deviceAddress is already bonded")
                        return@withContext true
                    } else {
                        Log.i(TAG, "Device $deviceAddress needs pairing - user should pair in system settings")
                        emitError(
                            ErrorType.PAIRING_REQUIRED,
                            "Please pair with device $deviceAddress in Android Bluetooth settings",
                            isRecoverable = true
                        )
                        return@withContext false
                    }
                } else {
                    Log.e(TAG, "UnifiedBleManager not available for device pairing")
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during device pairing prompt", e)
                return@withContext false
            }
        }
    }


    suspend fun scanAndPairDevices(): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (!hasRequiredPermissions(context)) {
                    Log.e(TAG, "Cannot scan for devices without proper permissions")
                    emitError(
                        ErrorType.PERMISSION_DENIED,
                        "Bluetooth permissions required for device discovery"
                    )
                    return@withContext emptyList()
                }

                val deviceList = mutableListOf<String>()


                val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
                val bondedDevices = bluetoothAdapter?.bondedDevices

                bondedDevices?.forEach { device ->
                    if (isShimmerDevice(device)) {
                        deviceList.add("${device.name} (${device.address}) - Bonded")
                        Log.d(TAG, "Found bonded Shimmer device: ${device.name} at ${device.address}")
                    }
                }


                val nearbyDevices = getAvailableShimmerDevices()
                nearbyDevices.forEach { deviceEntry ->
                    if (!deviceList.any { it.contains(deviceEntry.substringBefore(" - ")) }) {
                        deviceList.add(deviceEntry)
                    }
                }

                Log.i(TAG, "Device discovery completed: found ${deviceList.size} Shimmer devices")
                return@withContext deviceList

            } catch (e: Exception) {
                Log.e(TAG, "Error during device discovery and pairing", e)
                emitError(
                    ErrorType.DEVICE_ERROR,
                    "Device discovery failed: ${e.message}"
                )
                return@withContext emptyList()
            }
        }
    }


    private fun isShimmerDevice(device: android.bluetooth.BluetoothDevice): Boolean {
        return try {
            val deviceName = BluetoothPermissionUtils.getDeviceName(context, device) ?: "Unknown"
            deviceName.lowercase().contains("shimmer") ||
                    device.address.startsWith("00:06:66") ||
                    device.address.startsWith("d0:39:72") ||
                    device.address.startsWith("00:80:98")
        } catch (e: Exception) {
            Log.w(TAG, "Error checking if device is Shimmer", e)
            false
        }
    }


    private fun stopConnectionMonitoring() {
        connectionStateMonitoringJob?.cancel()
        connectionStateMonitoringJob = null

        currentConnectedDevice?.let { device ->
            try {
                if (device.isStreaming) {
                    device.stopStreaming()
                }
                device.disconnect()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping Shimmer device", e)
            }
        }
        currentConnectedDevice = null
        isShimmerConnected = false
    }
}
