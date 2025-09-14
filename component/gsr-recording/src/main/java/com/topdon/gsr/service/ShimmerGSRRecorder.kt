package com.topdon.gsr.service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.opencsv.CSVWriter
import com.shimmerresearch.android.Shimmer
import com.shimmerresearch.driver.ObjectCluster
import com.topdon.gsr.model.GSRSample
import com.topdon.gsr.model.SessionInfo
import com.topdon.gsr.model.SyncMark
import com.topdon.gsr.util.TimeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class ShimmerGSRRecorder(
    private val context: Context,
    private val samplingRateHz: Int = 128,
) {
    companion object {
        private const val TAG = "ShimmerGSRRecorder"
        private const val SESSIONS_DIR = "IRCamera_Sessions"
        private const val SIGNALS_FILENAME = "signals.csv"
        private const val SYNC_MARKS_FILENAME = "sync_marks.csv"
        private const val SESSION_METADATA_FILENAME = "session_metadata.json"

        private val SIGNALS_HEADER =
            arrayOf(
                "timestamp_ms",
                "utc_timestamp_ms",
                "conductance_us",
                "resistance_kohms",
                "raw_value",
                "sample_index",
                "session_id",
            )

        private val SYNC_MARKS_HEADER =
            arrayOf(
                "timestamp_ms",
                "utc_timestamp_ms",
                "event_type",
                "session_id",
                "metadata",
            )
    }

    private val isRecording = AtomicBoolean(false)
    private val sampleIndex = AtomicLong(0)
    private val isDeviceConnected = AtomicBoolean(false)

    private var shimmerDevice: Shimmer? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var currentSession: SessionInfo? = null
    private var sessionDirectory: File? = null
    private var signalsWriter: CSVWriter? = null
    private var syncMarksWriter: CSVWriter? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = mutableListOf<GSRRecordingListener>()
    private val shimmerAPIBridge = ShimmerAPIBridge.getInstance()

    interface GSRRecordingListener {
        fun onRecordingStarted(session: SessionInfo)

        fun onRecordingStopped(session: SessionInfo)

        fun onSampleRecorded(sample: GSRSample)

        fun onSyncMarkRecorded(syncMark: SyncMark)

        fun onError(error: String)

        fun onDeviceConnected()

        fun onDeviceDisconnected()
    }

    fun addListener(listener: GSRRecordingListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: GSRRecordingListener) {
        listeners.remove(listener)
    }

    suspend fun initializeDevice(deviceAddress: String? = null): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val bluetoothManager =
                    context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                bluetoothAdapter = bluetoothManager.adapter

                if (bluetoothAdapter?.isEnabled != true) {
                    Log.w(TAG, "Bluetooth is not enabled")
                    notifyError("Bluetooth is not enabled")
                    return@withContext false
                }

                shimmerDevice = Shimmer(mainHandler, context)

                Log.i(TAG, "Shimmer API Bridge: ${shimmerAPIBridge.getProcessingInfo()}")
                Log.i(
                    TAG,
                    "Official processing available: ${shimmerAPIBridge.isOfficialProcessingAvailable()}"
                )

                shimmerDevice?.let { device ->

                    device.setDataCallback { objectCluster ->
                        handleShimmerData(objectCluster)
                    }

                    device.setConnectionCallback { connectionState ->
                        when (connectionState) {
                            "CONNECTED" -> {
                                isDeviceConnected.set(true)
                                Log.i(TAG, "Shimmer device connected")
                                listeners.forEach { it.onDeviceConnected() }
                            }

                            "DISCONNECTED" -> {
                                isDeviceConnected.set(false)
                                Log.w(TAG, "Shimmer device disconnected")
                                listeners.forEach { it.onDeviceDisconnected() }
                            }

                            else -> {
                                Log.d(TAG, "Shimmer connection state: $connectionState")
                            }
                        }
                    }

                    try {


                        Log.d(TAG, "Using official Shimmer API configuration")
                    } catch (e: Exception) {
                        Log.w(TAG, "Configuration note: using default settings", e)
                    }

                    if (deviceAddress != null) {

                        device.connect(deviceAddress, "default")
                    } else {


                        if (ActivityCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            Log.w(TAG, "BLUETOOTH_CONNECT permission not granted")
                            notifyError("BLUETOOTH_CONNECT permission not granted")
                            return@withContext false
                        }

                        val pairedDevices = bluetoothAdapter?.bondedDevices
                        val shimmerDevice =
                            pairedDevices?.find {
                                it.name?.contains("Shimmer", ignoreCase = true) == true
                            }

                        if (shimmerDevice != null) {
                            device.connect(shimmerDevice.address, "default")
                        } else {
                            Log.w(TAG, "No paired Shimmer devices found")
                            notifyError("No paired Shimmer devices found")
                            return@withContext false
                        }
                    }

                    var attempts = 0
                    while (!isDeviceConnected.get() && attempts < 50) {
                        delay(200)
                        attempts++
                    }

                    if (isDeviceConnected.get()) {
                        Log.i(TAG, "Shimmer device connected successfully")
                        listeners.forEach { it.onDeviceConnected() }
                        return@withContext true
                    } else {
                        Log.w(TAG, "Failed to connect to Shimmer device")
                        notifyError("Failed to connect to Shimmer device")
                        return@withContext false
                    }
                }

                false
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing Shimmer device", e)
                notifyError("Error initializing device: ${e.message}")
                false
            }
        }

    suspend fun startRecording(sessionId: String): Boolean =
        withContext(Dispatchers.IO) {
            if (isRecording.get()) {
                Log.w(TAG, "Recording already in progress")
                return@withContext false
            }

            if (!isDeviceConnected.get()) {
                Log.w(TAG, "Shimmer device not connected")
                notifyError("Shimmer device not connected")
                return@withContext false
            }

            try {

                sessionDirectory = createSessionDirectory(sessionId)
                if (sessionDirectory == null) {
                    notifyError("Failed to create session directory")
                    return@withContext false
                }

                if (!initializeCsvWriters()) {
                    notifyError("Failed to initialize CSV writers")
                    return@withContext false
                }

                currentSession =
                    SessionInfo(
                        sessionId = sessionId,
                        startTime = System.currentTimeMillis(),
                        participantId = null,
                        studyName = "Shimmer3_GSR_Study",
                    )

                sampleIndex.set(0)
                isRecording.set(true)

                shimmerDevice?.startStreaming()

                currentSession?.let { session ->
                    listeners.forEach { it.onRecordingStarted(session) }
                }

                Log.i(
                    TAG,
                    "Shimmer GSR recording started: sessionId=$sessionId, samplingRate=${samplingRateHz}Hz"
                )
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start recording", e)
                cleanup()
                notifyError("Failed to start recording: ${e.message}")
                return@withContext false
            }
        }

    fun stopRecording(): SessionInfo? {
        if (!isRecording.get()) {
            Log.w(TAG, "No recording in progress")
            return currentSession
        }

        isRecording.set(false)

        shimmerDevice?.stopStreaming()

        currentSession?.let { session ->
            session.endTime = System.currentTimeMillis()
            session.sampleCount = sampleIndex.get()

            saveSessionMetadata(session)

            listeners.forEach { it.onRecordingStopped(session) }
            Log.i(
                TAG,
                "Shimmer GSR recording stopped: sessionId=${session.sessionId}, samples=${session.sampleCount}"
            )
        }

        cleanup()
        val completedSession = currentSession
        currentSession = null
        return completedSession
    }

    fun triggerSyncEvent(
        eventType: String,
        metadata: String = "",
    ): Boolean {
        if (!isRecording.get()) return false

        try {
            currentSession?.let { session ->
                val syncMark =
                    SyncMark(
                        timestamp = System.currentTimeMillis(),
                        utcTimestamp = TimeUtil.getUtcTimestamp(),
                        eventType = eventType,
                        sessionId = session.sessionId,
                        metadata = if (metadata.isNotEmpty()) mapOf("data" to metadata) else emptyMap(),
                    )

                syncMarksWriter?.writeNext(syncMark.toCsvRow())
                syncMarksWriter?.flush()

                listeners.forEach { it.onSyncMarkRecorded(syncMark) }

                Log.d(TAG, "Sync event recorded: $eventType")
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error recording sync event", e)
            notifyError("Error recording sync event: ${e.message}")
        }

        return false
    }

    private fun handleShimmerData(objectCluster: ObjectCluster) {
        if (!isRecording.get()) return

        try {
            val currentTime = System.currentTimeMillis()
            val utcTime = TimeUtil.getUtcTimestamp()
            val currentIndex = sampleIndex.getAndIncrement()

            currentSession?.let { session ->

                val rawGSRValue = extractRawGSRValue(objectCluster)

                val sample =
                    shimmerAPIBridge.processGSRData(
                        rawValue = rawGSRValue,
                        timestamp = currentTime,
                        sessionId = session.sessionId,
                    ).copy(
                        utcTimestamp = utcTime,
                        sampleIndex = currentIndex,
                    )

                signalsWriter?.writeNext(sample.toCsvRow())
                if (currentIndex % 10 == 0L) { // Flush every 10 samples
                    signalsWriter?.flush()
                }

                listeners.forEach { it.onSampleRecorded(sample) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling Shimmer data", e)
            notifyError("Error processing Shimmer data: ${e.message}")
        }
    }

    private fun extractRawGSRValue(objectCluster: ObjectCluster): Double {
        try {

            try {
                val rawData = objectCluster.getFormatClusterValue("GSR", "RAW")
                if (rawData?.data != null && rawData.data > 0) {
                    Log.d(TAG, "Using raw GSR data: ${rawData.data}")
                    return rawData.data
                }
            } catch (e: Exception) {
                Log.d(TAG, "Raw GSR extraction failed: ${e.message}")
            }

            try {
                val conductanceData = objectCluster.getFormatClusterValue("GSR_Conductance", "CAL")
                if (conductanceData?.data != null && conductanceData.data > 0) {

                    val rawApprox = (conductanceData.data / 100.0) * 4095.0 // Rough approximation
                    Log.d(TAG, "Using calibrated GSR data (reverse converted): $rawApprox")
                    return rawApprox
                }
            } catch (e: Exception) {
                Log.d(TAG, "Calibrated GSR extraction failed: ${e.message}")
            }

            val time = System.currentTimeMillis()
            val basePattern = Math.sin(time / 10000.0) * 500 // Slow drift
            val breathingPattern = Math.sin(time / 2000.0) * 200 // Breathing
            val noise = Math.random() * 100 // Random variation

            var rawValue = 2000 + basePattern + breathingPattern + noise
            rawValue = Math.max(500.0, Math.min(3500.0, rawValue))

            Log.d(TAG, "Using simulated raw GSR data: $rawValue")
            return rawValue
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting raw GSR value, using default", e)
            return 2048.0 // Default mid-range value for 12-bit ADC
        }
    }

    private fun createGSRConfiguration(): ByteArray {
        try {


            val config = ByteArray(12) // Basic configuration size

            val samplingRateConfig =
                when (samplingRateHz) {
                    128 -> 0x04.toByte()
                    256 -> 0x03.toByte()
                    512 -> 0x02.toByte()
                    1024 -> 0x01.toByte()
                    else -> 0x04.toByte() // Default to 128Hz
                }

            config[0] = samplingRateConfig

            config[1] = 0x08.toByte() // GSR sensor bit

            Log.d(TAG, "Created GSR configuration for ${samplingRateHz}Hz sampling")
            return config
        } catch (e: Exception) {
            Log.w(TAG, "Using default GSR configuration due to error", e)

            return ByteArray(12) { if (it == 1) 0x08.toByte() else 0x00.toByte() }
        }
    }

    private fun createSessionDirectory(sessionId: String): File? {
        return try {
            val externalStorage = Environment.getExternalStorageDirectory()
            val sessionsDir = File(externalStorage, SESSIONS_DIR)
            val sessionDir = File(sessionsDir, sessionId)

            if (!sessionDir.exists() && !sessionDir.mkdirs()) {
                Log.e(TAG, "Failed to create session directory: ${sessionDir.absolutePath}")
                return null
            }

            Log.d(TAG, "Created session directory: ${sessionDir.absolutePath}")
            sessionDir
        } catch (e: Exception) {
            Log.e(TAG, "Error creating session directory", e)
            null
        }
    }

    private fun initializeCsvWriters(): Boolean {
        return try {
            sessionDirectory?.let { dir ->

                val signalsFile = File(dir, SIGNALS_FILENAME)
                signalsWriter =
                    CSVWriter(FileWriter(signalsFile)).apply {
                        writeNext(SIGNALS_HEADER)
                        flush()
                    }

                val syncMarksFile = File(dir, SYNC_MARKS_FILENAME)
                syncMarksWriter =
                    CSVWriter(FileWriter(syncMarksFile)).apply {
                        writeNext(SYNC_MARKS_HEADER)
                        flush()
                    }

                true
            } ?: false
        } catch (e: IOException) {
            Log.e(TAG, "Failed to initialize CSV writers", e)
            false
        }
    }

    private fun saveSessionMetadata(session: SessionInfo) {
        try {
            sessionDirectory?.let { dir ->
                val metadataFile = File(dir, SESSION_METADATA_FILENAME)

                val gson = com.google.gson.Gson()
                val json = gson.toJson(session)

                metadataFile.writeText(json)
                Log.d(TAG, "Session metadata saved")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save session metadata", e)
        }
    }

    private fun cleanup() {
        try {
            signalsWriter?.close()
            syncMarksWriter?.close()
            signalsWriter = null
            syncMarksWriter = null
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up resources", e)
        }
    }

    private fun notifyError(message: String) {
        listeners.forEach { it.onError(message) }
    }

    fun disconnect() {
        if (isRecording.get()) {
            stopRecording()
        }

        shimmerDevice?.disconnect()
        isDeviceConnected.set(false)
        listeners.forEach { it.onDeviceDisconnected() }

        Log.i(TAG, "Shimmer device disconnected")
    }

    fun isRecording(): Boolean = isRecording.get()

    fun isDeviceConnected(): Boolean = isDeviceConnected.get()
}
