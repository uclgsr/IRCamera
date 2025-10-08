package mpdc4gsr.feature.gsr.data

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mpdc4gsr.gsr.model.GSRSample
import com.mpdc4gsr.gsr.model.SessionInfo
import com.mpdc4gsr.gsr.model.SyncMark
import com.mpdc4gsr.gsr.service.ShimmerGSRRecorder
import com.shimmerresearch.android.Shimmer
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
import com.shimmerresearch.bluetooth.ShimmerBluetooth.BT_STATE
import com.shimmerresearch.driver.ObjectCluster
import com.shimmerresearch.driver.ShimmerDevice
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import mpdc4gsr.core.data.*
import mpdc4gsr.feature.network.data.RecordingController
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import com.mpdc4gsr.gsr.service.GSRRecorder as LegacyGSRRecorder
import mpdc4gsr.feature.gsr.data.GSRDeviceFactory

class GSRSensorRecorder(
    private val context: Context,
    override val sensorId: String = "gsr_shimmer_1",
    private val samplingRateHz: Int = 128,
    private val recordingController: RecordingController
) : SensorRecorder {
    companion object {
        private const val TAG = "GSRSensorRecorder"
        private const val TIMING_HEALTH_POOR_MS = 2000L
        private const val HEALTH_SCORE_WEIGHT_HISTORICAL = 0.8
        private const val HEALTH_SCORE_WEIGHT_SAMPLE = 0.15
        private const val HEALTH_SCORE_WEIGHT_TIMING = 0.05
        private const val POOR_CONNECTION_THRESHOLD = 50.0
        private const val SHIMMER_DEFAULT_SAMPLING_RATE = 128.0
        private const val GSR_CHANNEL_ID = 0x01
        private const val GSR_RANGE_AUTO = 0x00

        // Monitoring and connection delays
        private const val STATUS_MONITORING_INTERVAL_MS = 1000L
        private const val CONNECTION_VERIFICATION_DELAY_MS = 2000L
        private const val CONNECTION_STATE_MONITOR_INTERVAL_MS = 1000L
        private const val CONNECTION_STATE_ERROR_DELAY_MS = 5000L
        private const val RECONNECTION_VERIFY_DELAY_MS = 1000L
        private const val SHIMMER_MIN_SAMPLING_RATE = 1.0
        private const val SHIMMER_MAX_SAMPLING_RATE = 512.0

        // Enhanced batch writing configuration for improved performance
        private const val CSV_BATCH_SIZE = 50 // Write every 50 samples for better performance
        private const val CSV_FLUSH_INTERVAL_MS = 5000L // Flush every 5 seconds
        private const val CONNECTION_HEALTH_CHECK_INTERVAL = 10 // Check connection every 10 samples
        fun hasRequiredPermissions(context: Context): Boolean {
            return hasBleScanningPermissions(context)
        }

        fun hasBleScanningPermissions(context: Context): Boolean {
            return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            }
        }

        fun hasBluetoothConnectPermission(context: Context): Boolean {
            return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
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
    }

    override val sensorType: String = "GSR Shimmer3"
    private var _samplingRate: Double = samplingRateHz.toDouble()
    override val samplingRate: Double get() = _samplingRate
    private var _isRecording = AtomicBoolean(false)
    override val isRecording: Boolean get() = _isRecording.get()
    private var unifiedShimmerDevice: ShimmerDevice? = null
    private var realShimmerGSRRecorder: ShimmerGSRRecorder? = null
    private var gsrDataPersistence: GSRDataPersistence? = null
    private var currentSessionId: String? = null
    private val sampleSequence = AtomicLong(0)
    private var isShimmerConnected = false
    private var legacyGSRRecorder: LegacyGSRRecorder? = null
    private var gsrSettingsRepository: GSRSettingsRepository? = null
    private var effectiveSamplingRate: Double = samplingRateHz.toDouble()
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
            try {                if (!hasRequiredPermissions(context)) {                }                if (initializeShimmerBluetoothManager()) {                    startConnectionStateMonitoring()
                }
                gsrSettingsRepository = GSRSettingsRepository(context)
                val gsrSettings = gsrSettingsRepository?.gsrSettings?.value
                effectiveSamplingRate = gsrSettings?.samplingRate?.toDouble() ?: samplingRateHz.toDouble()
                effectiveSamplingRate =
                    effectiveSamplingRate.coerceIn(SHIMMER_MIN_SAMPLING_RATE, SHIMMER_MAX_SAMPLING_RATE)
                // Observe settings changes for real-time updates
                observeGSRSettingsChanges()
                realShimmerGSRRecorder =
                    ShimmerGSRRecorder(
                        context,
                        GSRDeviceFactory(context),
                        effectiveSamplingRate.toInt()
                    )
                val shimmerRecorder = realShimmerGSRRecorder
                if (shimmerRecorder != null) {
                    try {
                        val deviceInitialized = shimmerRecorder.initializeDevice()
                        if (deviceInitialized) {                            isShimmerConnected = true
                        } else {                            isShimmerConnected = false
                        }
                    } catch (e: Exception) {                        isShimmerConnected = false
                    }
                }
                legacyGSRRecorder =
                    LegacyGSRRecorder(context, GSRDeviceFactory(context), effectiveSamplingRate.toInt())
                if (isNetworkStreamingEnabled) {
                    try {                    } catch (e: Exception) {                        isNetworkStreamingEnabled = false
                    }
                }
                startDataMonitoring()
                setupGSRSampleCallback()
                emitStatus()
                return@withContext true
            } catch (e: Exception) {                emitError(
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
                    delay(STATUS_MONITORING_INTERVAL_MS)
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
                    emitError(ErrorType.DATA_CORRUPTION, "Enhanced GSR data loss detected", true)
                }
                try {
                    val currentSampleCount = sampleCount.get()
                    if (currentSampleCount == expectedSamples && expectedSamples > 0) {                        emitError(
                            ErrorType.DATA_CORRUPTION,
                            "Enhanced GSR data loss detected",
                            true
                        )
                    }
                } catch (e: Exception) {                    emitError(ErrorType.DEVICE_ERROR, "Enhanced Shimmer monitoring error", true)
                }
            } else {
                val legacyRecorder = legacyGSRRecorder
                if (legacyRecorder != null) {
                    val currentSamples = sampleCount.get()
                    if (currentSamples > 0) {                    }
                }
            }
        } catch (e: Exception) {        }
    }

    override suspend fun startRecording(sessionDirectory: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                if (_isRecording.get()) {                    return@withContext true
                        "Missing permissions: ${getMissingPermissions(context)}"
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
                if (GSRSensorRecorder.hasBleScanningPermissions(context)) {
                    val shimmerRecorder = realShimmerGSRRecorder
                    if (shimmerRecorder != null) {                        try {
                            val connectionSuccess = if (!shimmerRecorder.isDeviceConnected()) {                                shimmerRecorder.initializeDevice()
                            } else {                                true
                            }
                            if (connectionSuccess) {
                                val sessionId = sessionDirectory.substringAfterLast("/").ifEmpty {
                                    "session_${System.currentTimeMillis()}"
                                }
                                val success = shimmerRecorder.startRecording(sessionId)
                                if (success) {
                                    shimmerRecordingStarted = true
                                    recordingSuccessful = true                                } else {                                }
                            } else {                            }
                        } catch (e: Exception) {                        }
                    } else {                    }
                }
                if (!shimmerRecordingStarted) {                    val legacyRecorder = legacyGSRRecorder
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
                                    recordingSuccessful = true                                } else {                                }
                            } else {                            }
                        } catch (e: Exception) {                        }
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
                    } catch (e: Exception) {                    }
                    emitStatus()
                    return@withContext true
                } else {                    emitError(
                        ErrorType.RECORDING_FAILED,
                        "Shimmer GSR device not available - check device pairing, power, and proximity. Session will continue without GSR data.",
                        isRecoverable = true
                    )
                    return@withContext false
                }
            } catch (e: Exception) {                emitError(
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
                gsrDataPersistence?.startPersistence()            } else {            }
            if (isNetworkStreamingEnabled) {
                gsrNetworkStreamer =
                    GSRNetworkStreamer(context, currentSessionId!!, recordingController)
                val networkInitialized = gsrNetworkStreamer?.initialize() ?: false
                if (networkInitialized) {
                    val streamingStarted = gsrNetworkStreamer?.startStreaming() ?: false
                    if (streamingStarted) {                    } else {                    }
                } else {                }
            }
        } catch (e: Exception) {        }
    }

    private suspend fun startEnhancedShimmerRecording(
        shimmerRecorder: ShimmerGSRRecorder,
        sessionDir: String,
    ): Boolean {
        return try {
            val sessionId =
                sessionDir.substringAfterLast("/").ifEmpty {
                    "session_${System.currentTimeMillis()}"
                }            val success = shimmerRecorder.startRecording(sessionId)
            if (success) {            } else {            }
            success
        } catch (e: Exception) {            false
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
                }            val initSuccess = recorder.initialize()
            if (!initSuccess) {            }
            val success =
                recorder.startRecording(
                    sessionId = sessionId,
                    participantId = "participant_${System.currentTimeMillis()}",
                    studyName = "IRCamera_MultiModal_Study",
                )
            if (success) {            } else {            }
            success
        } catch (e: Exception) {            false
        }
    }

    override suspend fun stopRecording(): Boolean {
        try {
            if (!_isRecording.get()) {                return true
            }
            // Flush any remaining batch samples before stopping
            flushBatchSamples()            val shimmerRecorder = realShimmerGSRRecorder
            if (shimmerRecorder != null && shimmerRecorder.isRecording()) {                val stopSuccess =
                    try {
                        stopEnhancedShimmerRecording(shimmerRecorder)
                    } catch (e: Exception) {                        false
                    }
                if (stopSuccess) {                } else {                }
            }
            legacyGSRRecorder?.let { recorder ->
                stopLegacyRecording(recorder)
            }
            gsrNetworkStreamer?.let { streamer ->
                try {
                    val streamingStopped = streamer.stopStreaming()
                    if (streamingStopped) {                    } else {                    }
                    streamer.cleanup()
                    gsrNetworkStreamer = null
                } catch (e: Exception) {                }
            }
            gsrDataPersistence?.let { persistence ->
                try {
                    persistence.stopPersistence()
                    persistence.cleanup()
                    val stats = persistence.getStatistics()                    gsrDataPersistence = null
                    currentSessionId = null
                } catch (e: Exception) {                }
            }
            _isRecording.set(false)
            // Close CSV file and ensure all data is written as per plan requirements
            closeCsvFile()            emitStatus()
            return true
        } catch (e: Exception) {            emitError(
                ErrorType.RECORDING_FAILED,
                "Failed to stop real Shimmer GSR recording: ${e.message}"
            )
            return false
        }
    }

    private suspend fun stopEnhancedShimmerRecording(shimmerRecorder: ShimmerGSRRecorder): Boolean {
        return try {            val sessionInfo = shimmerRecorder.stopRecording()
            if (sessionInfo != null) {                true
            } else {                false
            }
        } catch (e: Exception) {            false
        }
    }

    private suspend fun stopLegacyRecording(recorder: LegacyGSRRecorder) {
        try {            val sessionInfo = recorder.stopRecording()
            if (sessionInfo != null) {            } else {            }
        } catch (e: Exception) {        }
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
                if (success) {                } else {                }
            }
            legacyGSRRecorder?.let { recorder ->
                val success = recorder.addSyncMark(markerType, metadataString)
                if (success) {                } else {                }
            }        } catch (e: Exception) {            emitError(ErrorType.SYNC_FAILED, "GSR sync marker failed: ${e.message}")
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
                gsrDataPersistence?.getStatistics()?.let { stats ->                }
            }
        } catch (e: Exception) {        }
    }

    private fun flushBatchSamples() {
        if (batchSampleBuffer.isNotEmpty()) {
            try {
                // Flush batch to persistence layer
                batchSampleBuffer.forEach { sampleData ->
                    gsrDataPersistence?.queueDataRecord(sampleData)
                }                batchSampleBuffer.clear()
                lastFlushTime = System.currentTimeMillis()
            } catch (e: Exception) {            }
        }
    }

    private fun updateConnectionHealth(sample: GSRSample) {
        try {
            val now = System.currentTimeMillis()
            val timeSinceLastCheck = now - lastConnectionCheck
            // Calculate health based on sample quality and timing using constants
            val sampleQuality = when {
                sample.rawValue == 0 -> 0.0
                sample.rawValue !in GSRConstants.GSR_RAW_LOWER_BOUND..GSRConstants.GSR_RAW_UPPER_BOUND -> 30.0
                sample.conductance !in GSRConstants.GSR_MICROSIEMENS_LOWER_BOUND..GSRConstants.GSR_MICROSIEMENS_UPPER_BOUND -> 40.0
                sample.conductance > GSRConstants.GSR_HIGH_THRESHOLD -> 60.0
                sample.conductance < GSRConstants.GSR_LOW_THRESHOLD -> 70.0
                else -> 95.0
            }
            val timingHealth = when {
                timeSinceLastCheck > TIMING_HEALTH_POOR_MS -> 20.0
                timeSinceLastCheck > GSRConstants.TIMING_HEALTH_ACCEPTABLE_MS -> 70.0
                else -> 100.0
            }
            // Update connection health score with weighted average using constants
            connectionHealthScore = (connectionHealthScore * HEALTH_SCORE_WEIGHT_HISTORICAL) +
                    (sampleQuality * HEALTH_SCORE_WEIGHT_SAMPLE) +
                    (timingHealth * HEALTH_SCORE_WEIGHT_TIMING)
            connectionHealthScore = connectionHealthScore.coerceIn(0.0, 100.0)
            lastConnectionCheck = now
            if (connectionHealthScore < POOR_CONNECTION_THRESHOLD) {
                    ErrorType.CONNECTION_LOST,
                    "Poor connection quality detected - check sensor contact",
                    isRecoverable = true
                )
            }
        } catch (e: Exception) {        }
    }

    private fun calculateResistanceFromGSR(gsrMicrosiemens: Double): Double {
        // Use centralized resistance calculation utility
        return GSRCalculationUtils.calculateResistanceFromGSR(gsrMicrosiemens)
    }

    private fun determineRecordingMode(): String {
        return when {
            realShimmerGSRRecorder != null && shimmerBluetoothManager != null -> "shimmer_official"
            realShimmerGSRRecorder != null -> "shimmer_ble"
            legacyGSRRecorder != null -> "legacy_gsr"
            else -> "unknown"
        }
    }

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
                GSRCalculationUtils.calculateGSRMicrosiemens(gsrRawValue)
            }
            // Calculate signal quality score based on data integrity
            val qualityScore = calculateSignalQuality(gsrMicrosiemens, gsrRawValue)
            GSRSample(
                timestamp = unifiedTimestamp,
                utcTimestamp = unifiedTimestamp,
                conductance = gsrMicrosiemens,
                resistance = if (gsrMicrosiemens > 0) 1000.0 / gsrMicrosiemens else Double.MAX_VALUE,
                rawValue = gsrRawValue,
                sampleIndex = sampleSequence.incrementAndGet(),
                sessionId = currentSessionId ?: ""
            )
        } catch (e: Exception) {            null
        }
    }

    private fun extractCalibratedGSRValue(objectCluster: ObjectCluster): Double {
        return try {
            val gsrCalibratedData = objectCluster.getFormatClusterValue("GSR Conductance", "CAL")
            gsrCalibratedData?.toDouble() ?: 0.0
        } catch (e: Exception) {            0.0
        }
    }

    private fun extractRawGSRValue(objectCluster: ObjectCluster): Int {
        return try {
            val gsrRawData = objectCluster.getFormatClusterValue("GSR", "RAW")
            gsrRawData?.toInt() ?: 0
        } catch (e: Exception) {            0
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
        // Use centralized GSR calculation utility
        return GSRCalculationUtils.calculateGSRMicrosiemens(rawValue)
    }

    private fun calculateSignalQuality(gsrMicrosiemens: Double, rawValue: Int): Double {
        // Use centralized signal quality calculation
        return GSRCalculationUtils.calculateSignalQuality(gsrMicrosiemens, rawValue)
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
            })        } catch (e: Exception) {        }
    }

    private fun setupObjectClusterDataHandler() {
        try {            val shimmerManager = shimmerBluetoothManager
            if (shimmerManager != null) {
                // MVP implementation: Basic data handling setup
                // Enhanced ObjectCluster handler integration can be added when API is available            } else {            }
        } catch (e: Exception) {        }
    }

    private fun observeGSRSettingsChanges() {
        recordingScope.launch {
            gsrSettingsRepository?.gsrSettings?.collectLatest { settings ->                val newSamplingRate =
                    settings.samplingRate.toDouble().coerceIn(SHIMMER_MIN_SAMPLING_RATE, SHIMMER_MAX_SAMPLING_RATE)
                if (newSamplingRate != effectiveSamplingRate) {
                    effectiveSamplingRate = newSamplingRate
                    _samplingRate = effectiveSamplingRate                    // Note: Shimmer device needs to be reconfigured for sampling rate changes
                    // Log a warning if recording is active as changes won't apply until restart
                    if (_isRecording.get()) {                    } else if (isShimmerConnected) {                    }
                }
            }
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
                    shimmerRecorder.disconnect()                } catch (e: Exception) {                }
            }
            legacyGSRRecorder?.let { recorder ->
                try {
                    recorder.disconnect()                } catch (e: Exception) {                }
            }
            gsrDataPersistence?.let { persistence ->
                try {
                    if (persistence.getStatistics().isActive) {
                        persistence.stopPersistence()
                    }
                    persistence.cleanup()                } catch (e: Exception) {                }
            }
            legacyGSRRecorder = null
            realShimmerGSRRecorder = null
            gsrDataPersistence = null
            currentSessionId = null        } catch (e: Exception) {        }
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

    private fun emitError(
        errorType: ErrorType,
        message: String,
        isRecoverable: Boolean = true,
    ) {
        recordingScope.launch {
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
                if (!hasRequiredPermissions(context)) {                    return@withContext emptyList()
                }
                val shimmerManager = shimmerBluetoothManager
                if (shimmerManager != null) {
                    val deviceList = mutableListOf<String>()
                    // Get connected Shimmer devices - using current device if available
                    val connectedDevices = try {                        // Check if we have a current device that's connected
                        currentConnectedDevice?.let { device ->
                            if (device.bluetoothRadioState == BT_STATE.CONNECTED ||
                                device.bluetoothRadioState == BT_STATE.STREAMING
                            ) {
                                listOf(device)
                            } else {
                                emptyList()
                            }
                        } ?: emptyList()
                    } catch (e: Exception) {                        // Fall back to checking current device if available
                        currentConnectedDevice?.let { device ->
                            try {
                                if (device.bluetoothRadioState == BT_STATE.CONNECTED ||
                                    device.bluetoothRadioState == BT_STATE.STREAMING
                                ) {
                                    listOf(device)
                                } else {
                                    emptyList()
                                }
                            } catch (deviceError: Exception) {                                emptyList()
                            }
                        } ?: emptyList()
                    }
                    connectedDevices.forEach { shimmer ->
                        try {
                            val deviceName =
                                shimmer.getShimmerUserAssignedName() ?: "Unknown Shimmer"
                            val deviceAddress = shimmer.getMacId() ?: "Unknown Address"
                            deviceList.add("$deviceName ($deviceAddress) - Connected")
                        } catch (e: Exception) {                        }
                    }
                    // Scan for paired Shimmer devices
                    val scanResultDeferred = CompletableDeferred<List<String>>()
                    // Use Shimmer's paired device detection
                    try {
                        val bluetoothManager =
                            context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                        val bluetoothAdapter = bluetoothManager?.adapter
                        if (bluetoothAdapter?.isEnabled == true) {
                            if (hasBluetoothConnectPermission(context)) {
                                val pairedDevices = bluetoothAdapter.bondedDevices
                                pairedDevices?.forEach { btDevice ->
                                    val deviceName = btDevice.name ?: "Unknown"
                                    val deviceAddress = btDevice.address
                                    // Check if this is a Shimmer GSR device
                                    if (isShimmerGSRDevice(deviceName, deviceAddress)) {
                                        val isAlreadyConnected = connectedDevices.any { shimmer ->
                                            try {
                                                shimmer.getMacId() == deviceAddress
                                            } catch (e: Exception) {
                                                false
                                            }
                                        }
                                        if (!isAlreadyConnected) {
                                            val deviceEntry = "$deviceName ($deviceAddress) - Available"
                                            if (!deviceList.contains(deviceEntry)) {
                                                deviceList.add(deviceEntry)
                                            }                                        }
                                    }
                                }
                            } else {                            }
                        }
                    } catch (e: Exception) {                    }
                    scanResultDeferred.complete(deviceList.toList())
                    scanResultDeferred.await()
                } else {                    emptyList()
                }
            } catch (e: Exception) {                emptyList()
            }
        }
    }

    suspend fun connectToShimmerDevice(deviceAddress: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!hasRequiredPermissions(context)) {                    emitError(
                        ErrorType.PERMISSION_DENIED,
                        "Bluetooth permissions required for device connection"
                    )
                    return@withContext false
                }                val shimmerManager = shimmerBluetoothManager
                if (shimmerManager != null) {
                    // Check if device is already connected by checking current device
                    val alreadyConnected = try {                        currentConnectedDevice?.let { device ->
                            device.getMacId() == deviceAddress &&
                                    (device.bluetoothRadioState == BT_STATE.CONNECTED ||
                                            device.bluetoothRadioState == BT_STATE.STREAMING)
                        } ?: false
                    } catch (e: Exception) {                        false
                    }
                    if (alreadyConnected) {                        isShimmerConnected = true
                        return@withContext true
                    }                    var connectionSuccess = false
                    try {
                        // Use Shimmer's official connection API                        // Use actual Shimmer SDK connection methods
                        shimmerManager.connectShimmerThroughBTAddress(deviceAddress)
                        // Connection is asynchronous, success will be verified after delay
                        connectionSuccess = true
                        if (connectionSuccess) {
                            // Wait for connection to establish
                            delay(CONNECTION_VERIFICATION_DELAY_MS)
                            // Verify connection by checking if we can get a device from the manager
                            val connectedDevice = try {                                // Try to verify the current connected device matches the requested address
                                currentConnectedDevice?.let { device ->
                                    try {
                                        if (device.getMacId() == deviceAddress) device else null
                                    } catch (e: Exception) {                                        null
                                    }
                                }
                            } catch (e: Exception) {                                null
                            }
                            if (connectedDevice != null) {
                                // Verify the device is actually connected by checking its state
                                val actuallyConnected = try {
                                    val state = connectedDevice.bluetoothRadioState
                                    state == BT_STATE.CONNECTED || state == BT_STATE.STREAMING
                                } catch (e: Exception) {                                    false
                                }
                                if (actuallyConnected) {
                                    currentConnectedDevice = connectedDevice                                    isShimmerConnected = true
                                } else {                                    connectionSuccess = false
                                }
                            } else {                                connectionSuccess = false
                            }
                        }
                    } catch (e: Exception) {                        connectionSuccess = false
                    }
                    if (connectionSuccess) {
                        return@withContext connectionSuccess
                    } else {                        isShimmerConnected = false
                        return@withContext false
                    }
                } else {                    return@withContext false
                }
            } catch (e: Exception) {                emitError(
                    ErrorType.DEVICE_ERROR,
                    "Failed to connect to Shimmer device: ${e.message}"
                )
                false
            }
        }
    }

    private suspend fun initializeShimmerBluetoothManager(): Boolean {
        return try {
            // Switch to Main dispatcher to ensure proper Looper context for Handler creation
            withContext(Dispatchers.Main) {
                shimmerBluetoothManager =
                    ShimmerBluetoothManagerAndroid(
                        context,
                        android.os.Handler(android.os.Looper.getMainLooper())
                    )                true
            }
        } catch (e: Exception) {            false
        }
    }

    private fun startConnectionStateMonitoring() {
        connectionStateMonitoringJob = recordingScope.launch {
            while (isActive) {
                try {
                    monitorConnectionState()
                    delay(CONNECTION_STATE_MONITOR_INTERVAL_MS)
                } catch (e: Exception) {                    delay(CONNECTION_STATE_ERROR_DELAY_MS)
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
                        if (!isShimmerConnected) {                            isShimmerConnected = true
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
                        if (isShimmerConnected) {                            isShimmerConnected = false
                            handleDisconnection(device)
                        }
                    }

                    else -> {                    }
                }
            } catch (e: Exception) {            }
        }
    }

    private suspend fun handleDisconnection(device: Shimmer) {
        if (reconnectionAttempts < maxReconnectionAttempts) {
            reconnectionAttempts++            // Emit user-friendly error message as per plan requirements
            emitError(
                ErrorType.CONNECTION_LOST,
                "GSR sensor disconnected - attempting to reconnect ($reconnectionAttempts/$maxReconnectionAttempts)",
                isRecoverable = true
            )
            // Use progressive delay between attempts (1s, 2s, 3s)
            val progressiveDelay = reconnectionAttempts * 1000L
            delay(progressiveDelay)
            try {                device.connect()
                // Wait briefly to confirm connection
                delay(RECONNECTION_VERIFY_DELAY_MS)
                val connectionState = device.getBluetoothRadioState()
                if (connectionState == BT_STATE.CONNECTED || connectionState == BT_STATE.STREAMING) {                    reconnectionAttempts = 0 // Reset counter on successful reconnection
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
                } else {                }
            } catch (e: Exception) {                if (reconnectionAttempts >= maxReconnectionAttempts) {                    // Graceful fallback as per plan requirements
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
        } else {        }
    }

    private suspend fun startShimmerStreaming(device: Shimmer): Boolean {
        return try {
            // Configure GSR sensor with settings from repository
            try {
                val gsrSettings = gsrSettingsRepository?.gsrSettings?.value
                // Apply GSR-specific configurations if available in SDK                // Note: Some methods may not be available in all Shimmer SDK versions
                // device.setGSRRange(GSR_RANGE_AUTO)
                // device.enableGSRSensor(true)
            } catch (e: Exception) {            }
            // Apply sampling rate from settings (already validated in initialize)
            device.setSamplingRateShimmer(effectiveSamplingRate)            // Start streaming
            device.startStreaming()            true
        } catch (e: Exception) {            emitError(
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
            val gsrValue = (objectCluster.getFormatClusterValue("GSR Conductance", "CAL") as? Number)?.toDouble() ?: 0.0
            val ppgValue = (objectCluster.getFormatClusterValue("PPG_A13", "CAL") as? Number)?.toDouble() ?: 0.0
            val gsrSample = GSRSample(
                timestamp = timestampRecord.systemNanos,
                utcTimestamp = timestampRecord.systemTimeMs,
                conductance = gsrValue,
                resistance = if (gsrValue > 0) 1000.0 / gsrValue else Double.MAX_VALUE,
                rawValue = (gsrValue * 4095 / 100).toInt(),
                sampleIndex = sampleCount.incrementAndGet(),
                sessionId = currentSessionId ?: ""
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
            }        } catch (e: Exception) {            emitError(
                ErrorType.DATA_PROCESSING_ERROR,
                "Failed to process GSR data: ${e.message}"
            )
        }
    }

    // Enhanced CSV buffering for better I/O performance as per plan
    private val csvBuffer = mutableListOf<String>()
    private var csvBufferCount = 0
    private var csvFile: java.io.File? = null
    private var csvWriter: java.io.FileWriter? = null
    private var lastCsvFlush = System.currentTimeMillis()
    private fun logGSRSampleToCSV(
        sample: GSRSample,
        timestampRecord: TimestampRecord,
        deviceTimestamp: Long
    ) {
        try {
            // Create CSV entry with all required data as per plan requirements
            val csvEntry = buildString {
                append("${timestampRecord.systemNanos},")           // Primary timestamp (phone-based)
                append("${timestampRecord.systemTimeMs},")          // Wall clock time
                append("${timestampRecord.sessionRelativeMs},")     // Session relative time
                append("${deviceTimestamp},")                       // Device timestamp for drift analysis
                append("${sample.conductance},")                    // GSR conductance 
                append("${sample.rawValue},")                       // Raw ADC value
                append("0")                                         // PPG placeholder (not available in current GSRSample)
            }
            // Add to buffer for batch writing (50 samples as per plan)
            synchronized(csvBuffer) {
                csvBuffer.add(csvEntry)
                csvBufferCount++
                // Write batch when buffer reaches target size or time threshold exceeded
                val currentTime = System.currentTimeMillis()
                if (csvBufferCount >= CSV_BATCH_SIZE ||
                    (currentTime - lastCsvFlush) > CSV_FLUSH_INTERVAL_MS
                ) {
                    flushCsvBuffer()
                }
            }        } catch (e: Exception) {            emitError(
                ErrorType.DATA_PROCESSING_ERROR,
                "Failed to buffer GSR sample: ${e.message}"
            )
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
            writer.flush()            csvBuffer.clear()
            csvBufferCount = 0
            lastCsvFlush = System.currentTimeMillis()
        } catch (e: Exception) {            emitError(
                ErrorType.STORAGE_ERROR,
                "Failed to write GSR data to file: ${e.message}"
            )
        }
    }

    private fun initializeCsvFile(sessionDirectory: String) {
        try {
            csvFile = java.io.File(sessionDirectory, "gsr.csv")
            csvWriter = java.io.FileWriter(csvFile!!, false) // false = overwrite existing file
            // Write header as per plan requirements
            csvWriter!!.write("${getGSRCsvHeader()}\n")
            csvWriter!!.flush()        } catch (e: Exception) {            emitError(
                ErrorType.STORAGE_ERROR,
                "Failed to create GSR data file: ${e.message}"
            )
        }
    }

    private fun closeCsvFile() {
        try {
            // Flush any remaining buffered data
            flushCsvBuffer()
            csvWriter?.close()
            csvWriter = null
            csvFile = null        } catch (e: Exception) {        }
    }

    private fun getGSRCsvHeader(): String {
        return "system_nanos,system_time_ms,session_relative_ms,device_timestamp,conductance_microsiemens,raw_adc,ppg_value"
    }

    suspend fun promptDevicePairing(deviceAddress: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {                val shimmerManager = shimmerBluetoothManager
                if (shimmerManager != null) {
                    // Use standard Android Bluetooth pairing
                    val bluetoothManager =
                        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                    val bluetoothAdapter = bluetoothManager?.adapter
                    if (!hasBluetoothConnectPermission(context)) {                        return@withContext false
                    }
                    val bondedDevices = bluetoothAdapter?.bondedDevices
                    val isAlreadyBonded =
                        bondedDevices?.any { it.address == deviceAddress } ?: false
                    if (isAlreadyBonded) {                        return@withContext true
                    } else {                        emitError(
                            ErrorType.PAIRING_REQUIRED,
                            "Please pair with device $deviceAddress in Android Bluetooth settings",
                            isRecoverable = true
                        )
                        return@withContext false
                    }
                } else {                    return@withContext false
                }
            } catch (e: Exception) {                return@withContext false
            }
        }
    }

    suspend fun scanAndPairDevices(): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (!hasRequiredPermissions(context)) {                    emitError(
                        ErrorType.PERMISSION_DENIED,
                        "Bluetooth permissions required for device discovery"
                    )
                    return@withContext emptyList()
                }
                val deviceList = mutableListOf<String>()
                val bluetoothManager =
                    context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val bluetoothAdapter = bluetoothManager?.adapter
                if (!hasBluetoothConnectPermission(context)) {                    return@withContext deviceList
                }
                val bondedDevices = bluetoothAdapter?.bondedDevices
                bondedDevices?.forEach { device ->
                    if (isShimmerDevice(device)) {
                        deviceList.add("${device.name} (${device.address}) - Bonded")                    }
                }
                val nearbyDevices = getAvailableShimmerDevices()
                nearbyDevices.forEach { deviceEntry ->
                    if (!deviceList.any { it.contains(deviceEntry.substringBefore(" - ")) }) {
                        deviceList.add(deviceEntry)
                    }
                }                return@withContext deviceList
            } catch (e: Exception) {                emitError(
                    ErrorType.DEVICE_ERROR,
                    "Device discovery failed: ${e.message}"
                )
                return@withContext emptyList()
            }
        }
    }

    private fun isShimmerGSRDevice(deviceName: String, deviceAddress: String): Boolean {
        val nameLower = deviceName.lowercase()
        // Check for Shimmer MAC address prefixes
        val hasShimmerMacPrefix = deviceAddress.startsWith("00:06:66") ||
                deviceAddress.startsWith("d0:39:72") ||
                deviceAddress.startsWith("00:80:98")
        // Check for GSR-related device names
        val hasGSRName = nameLower.contains("shimmer") ||
                nameLower.contains("gsr") ||
                nameLower.contains("rn4") ||
                nameLower.contains("shimmer3")
        return hasShimmerMacPrefix || hasGSRName
    }

    private fun isShimmerDevice(device: android.bluetooth.BluetoothDevice): Boolean {
        return try {
            val deviceName = if (ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                device.name
            } else {
                null
            }
            deviceName?.lowercase()?.contains("shimmer") == true ||
                    device.address.startsWith("00:06:66") ||
                    device.address.startsWith("d0:39:72") ||
                    device.address.startsWith("00:80:98")
        } catch (e: Exception) {            false
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
            } catch (e: Exception) {            }
        }
        currentConnectedDevice = null
        isShimmerConnected = false
    }
}
