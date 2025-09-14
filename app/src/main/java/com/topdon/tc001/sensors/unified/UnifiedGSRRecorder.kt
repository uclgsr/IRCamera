package com.topdon.tc001.sensors.unified

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.shimmerresearch.android.Shimmer
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
import com.shimmerresearch.driver.ObjectCluster
import com.shimmerresearch.driver.ShimmerDevice
import com.topdon.tc001.sensors.SensorRecorder
import com.topdon.tc001.sensors.unified.model.DeviceInfo
import com.topdon.tc001.sensors.unified.model.GSRSample
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class UnifiedGSRRecorder(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    override val sensorId: String = "unified_gsr_shimmer",
    private val samplingRateHz: Int = 128
) : SensorRecorder {

    companion object {
        private const val TAG = "UnifiedGSRRecorder"

        private val SHIMMER_MAC_PREFIXES = listOf("00:06:66", "d0:39:72")

        private const val GSR_RANGE_AUTO = 4  // Autorange for optimal sensitivity
        private const val ADC_RESOLUTION_12BIT = 4095.0  // 12-bit ADC range
        private const val DEFAULT_SAMPLING_RATE = 128.0  // Research-grade sampling

        private const val MIN_CONNECTION_STRENGTH = -70  // dBm
        private const val MAX_DATA_GAP_MS = 50  // Maximum acceptable gap
        private const val MIN_QUALITY_SCORE = 0.8  // Quality threshold

        fun hasRequiredPermissions(context: Context): Boolean {
            val bluetoothScan = ActivityCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED

            val bluetoothConnect = ActivityCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED

            val locationFine = ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            return bluetoothScan && bluetoothConnect && locationFine
        }

        fun getRequiredPermissions(): Array<String> = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        )
    }

    override val sensorType: String = "GSR (Galvanic Skin Response)"
    override val samplingRate: Double = samplingRateHz.toDouble()

    private val _isRecording = AtomicBoolean(false)
    override val isRecording: Boolean get() = _isRecording.get()

    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var shimmerManager: ShimmerBluetoothManagerAndroid? = null
    private var connectedShimmer: Shimmer? = null

    private val discoveredDevices = mutableListOf<DeviceInfo>()
    private var selectedDevice: DeviceInfo? = null

    private val gsrDataFlow = MutableSharedFlow<GSRSample>(
        replay = 1000,  // Buffer recent samples for late subscribers
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private var recordingJob: Job? = null
    private var sessionDirectory: File? = null
    private var csvWriter: FileWriter? = null
    private val recordedSamples = AtomicLong(0)
    private var recordingStartTime: Long = 0

    private val _connectionQuality = MutableStateFlow(0.0)
    val connectionQuality: StateFlow<Double> = _connectionQuality.asStateFlow()

    private val _deviceStatus = MutableStateFlow("Disconnected")
    val deviceStatus: StateFlow<String> = _deviceStatus.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())

    override suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Initializing Unified GSR Recorder with Shimmer3 GSR+ integration")

        try {

            if (!hasRequiredPermissions(context)) {
                Log.e(TAG, "Missing required BLE permissions for GSR recording")
                _deviceStatus.value = "Missing Permissions"
                return@withContext false
            }

            bluetoothManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothAdapter = bluetoothManager?.adapter

            if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
                Log.e(TAG, "Bluetooth not available or disabled")
                _deviceStatus.value = "Bluetooth Disabled"
                return@withContext false
            }

            shimmerManager = ShimmerBluetoothManagerAndroid(context, shimmerCallback)

            _deviceStatus.value = "Initialized"
            Log.i(TAG, "GSR Recorder initialization completed successfully")
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize GSR recorder", e)
            _deviceStatus.value = "Initialization Failed"
            return@withContext false
        }
    }

    suspend fun startDeviceDiscovery(): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting Shimmer3 GSR+ device discovery with MAC filtering")

        if (shimmerManager == null) {
            Log.e(TAG, "Shimmer manager not initialized")
            return@withContext false
        }

        try {
            _deviceStatus.value = "Discovering..."
            discoveredDevices.clear()

            shimmerManager?.startScanBle()

            delay(5000)  // 5-second discovery window

            val foundDevices = shimmerManager?.getListofConnectableDevices()
            Log.i(TAG, "Discovery found ${foundDevices?.size ?: 0} potential devices")

            foundDevices?.forEach { (address, name) ->
                if (SHIMMER_MAC_PREFIXES.any { address.startsWith(it, ignoreCase = true) }) {
                    val deviceInfo = DeviceInfo(
                        address = address,
                        name = name ?: "Shimmer Device",
                        deviceType = if (name?.contains(
                                "GSR",
                                ignoreCase = true
                            ) == true
                        ) "GSR+" else "Shimmer",
                        rssi = -50,  // Default RSSI, will be updated during connection
                        isGSRCapable = true,
                        priority = if (name?.contains("GSR", ignoreCase = true) == true) 1 else 2
                    )
                    discoveredDevices.add(deviceInfo)
                    Log.i(TAG, "Added Shimmer device: $address ($name)")
                }
            }

            discoveredDevices.sortBy { it.priority }

            shimmerManager?.stopScanBle()

            if (discoveredDevices.isNotEmpty()) {
                _deviceStatus.value = "Found ${discoveredDevices.size} devices"
                return@withContext true
            } else {
                _deviceStatus.value = "No devices found"
                return@withContext false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error during device discovery", e)
            _deviceStatus.value = "Discovery Failed"
            return@withContext false
        }
    }

    suspend fun connectToDevice(deviceInfo: DeviceInfo): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Connecting to Shimmer device: ${deviceInfo.address} (${deviceInfo.name})")

        if (shimmerManager == null) {
            Log.e(TAG, "Shimmer manager not initialized")
            return@withContext false
        }

        try {
            _deviceStatus.value = "Connecting..."
            selectedDevice = deviceInfo

            shimmerManager?.connectShimmerThroughBTAddress(deviceInfo.address)

            var attempts = 0
            while (connectedShimmer == null && attempts < 30) { // 30 second timeout
                delay(1000)
                attempts++
            }

            if (connectedShimmer != null) {

                configureGSRSensor()
                _deviceStatus.value = "Connected: ${deviceInfo.name}"
                _connectionQuality.value = 1.0
                return@withContext true
            } else {
                _deviceStatus.value = "Connection Failed"
                return@withContext false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to device", e)
            _deviceStatus.value = "Connection Error"
            return@withContext false
        }
    }

    private suspend fun configureGSRSensor() = withContext(Dispatchers.IO) {
        Log.i(TAG, "Configuring GSR sensor for research-grade recording")

        val shimmer = connectedShimmer ?: return@withContext

        try {

            shimmer.enableSensor(Shimmer.SENSOR_GSR)
            shimmer.enableSensor(Shimmer.SENSOR_EXG1_24BIT) // For PPG if available

            shimmer.setSamplingRateShimmer(DEFAULT_SAMPLING_RATE)

            shimmer.setGSRRange(GSR_RANGE_AUTO)

            shimmer.writeShimmerAndSensorConfiguration()

            Log.i(TAG, "GSR sensor configured: 128Hz sampling, autorange, 12-bit ADC")

        } catch (e: Exception) {
            Log.e(TAG, "Error configuring GSR sensor", e)
            throw e
        }
    }

    override suspend fun startRecording(sessionDirectory: String): Boolean =
        withContext(Dispatchers.IO) {
            Log.i(TAG, "Starting GSR recording session")

            val shimmer = connectedShimmer
            if (shimmer == null) {
                Log.e(TAG, "No Shimmer device connected for recording")
                return@withContext false
            }

            if (_isRecording.get()) {
                Log.w(TAG, "Recording already in progress")
                return@withContext true
            }

            try {

                this@UnifiedGSRRecorder.sessionDirectory = File(sessionDirectory)
                this@UnifiedGSRRecorder.sessionDirectory?.mkdirs()

                val csvFile = File(this@UnifiedGSRRecorder.sessionDirectory, "gsr_data.csv")
                csvWriter = FileWriter(csvFile)

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                csvWriter?.write("# GSR Recording Session\n")
                csvWriter?.write("# Device: ${selectedDevice?.name} (${selectedDevice?.address})\n")
                csvWriter?.write("# Sampling Rate: ${samplingRate}Hz\n")
                csvWriter?.write("# ADC Resolution: 12-bit (0-${ADC_RESOLUTION_12BIT.toInt()})\n")
                csvWriter?.write("# Started: ${dateFormat.format(Date())}\n")
                csvWriter?.write("# Columns: timestamp_ns,timestamp_iso,gsr_microsiemens,gsr_raw,ppg_raw,quality_score,connection_rssi\n")
                csvWriter?.flush()

                recordedSamples.set(0)
                recordingStartTime = System.nanoTime()

                shimmer.startStreaming()

                _isRecording.set(true)
                _deviceStatus.value = "Recording..."

                recordingJob = lifecycleOwner.lifecycleScope.launch {
                    processRecordingData()
                }

                Log.i(TAG, "GSR recording started successfully")
                return@withContext true

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start GSR recording", e)
                _deviceStatus.value = "Recording Failed"
                return@withContext false
            }
        }

    override suspend fun stopRecording(): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Stopping GSR recording session")

        if (!_isRecording.get()) {
            Log.w(TAG, "No recording in progress")
            return@withContext true
        }

        try {
            _isRecording.set(false)

            connectedShimmer?.stopStreaming()

            recordingJob?.cancel()
            recordingJob = null

            csvWriter?.write(
                "# Recording stopped: ${
                    SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss.SSS",
                        Locale.getDefault()
                    ).format(Date())
                }\n"
            )
            csvWriter?.write("# Total samples: ${recordedSamples.get()}\n")
            csvWriter?.write("# Duration: ${(System.nanoTime() - recordingStartTime) / 1_000_000_000.0} seconds\n")
            csvWriter?.close()
            csvWriter = null

            val sampleCount = recordedSamples.get()
            val durationSec = (System.nanoTime() - recordingStartTime) / 1_000_000_000.0

            _deviceStatus.value =
                "Stopped (${sampleCount} samples, ${String.format("%.1f", durationSec)}s)"
            Log.i(
                TAG,
                "GSR recording stopped: $sampleCount samples in ${
                    String.format(
                        "%.2f",
                        durationSec
                    )
                } seconds"
            )

            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping GSR recording", e)
            return@withContext false
        }
    }

    private suspend fun processRecordingData() = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting real-time GSR data processing")


        while (_isRecording.get()) {
            delay(100)  // Update connection quality every 100ms
            updateConnectionQuality()
        }
    }

    private fun updateConnectionQuality() {
        val shimmer = connectedShimmer ?: return

        try {

            val isStreaming = shimmer.isStreaming
            val connectionState = shimmer.getShimmerState()

            val quality = when {
                !isStreaming -> 0.0
                connectionState == ShimmerDevice.SHIMMER_STATE_STREAMING -> {

                    val baseQuality = 0.9
                    val sampleRate = recordedSamples.get() / maxOf(
                        1.0,
                        (System.nanoTime() - recordingStartTime) / 1_000_000_000.0
                    )
                    val rateQuality = minOf(1.0, sampleRate / samplingRate)
                    baseQuality * rateQuality
                }

                connectionState == ShimmerDevice.SHIMMER_STATE_CONNECTED -> 0.7
                else -> 0.3
            }

            _connectionQuality.value = quality

        } catch (e: Exception) {
            Log.w(TAG, "Error updating connection quality", e)
            _connectionQuality.value = 0.5
        }
    }

    suspend fun addSyncMarker(markerType: String, timestamp: Long): Boolean {
        return try {
            if (_isRecording.get() && csvWriter != null) {
                val iso = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(
                    Date(timestamp / 1_000_000)
                )
                csvWriter?.write("# SYNC_MARKER: $markerType at $timestamp ($iso)\n")
                csvWriter?.flush()

                Log.i(TAG, "Added sync marker: $markerType at $timestamp")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding sync marker", e)
            false
        }
    }

    fun getRecordingStatus(): Map<String, Any> {
        return mapOf(
            "sensor_type" to sensorType,
            "sensor_id" to sensorId,
            "is_recording" to isRecording,
            "device_status" to _deviceStatus.value,
            "connection_quality" to _connectionQuality.value,
            "sampling_rate" to samplingRate,
            "recorded_samples" to recordedSamples.get(),
            "selected_device" to (selectedDevice?.name ?: "None"),
            "discovered_devices" to discoveredDevices.size
        )
    }

    fun getDiscoveredDevices(): List<DeviceInfo> = discoveredDevices.toList()

    fun getDataStream(): Flow<GSRSample> = gsrDataFlow.asSharedFlow()

    suspend fun disconnectDevice(): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Disconnecting from Shimmer device")

        try {

            if (_isRecording.get()) {
                stopRecording()
            }

            connectedShimmer?.stop()
            connectedShimmer = null
            selectedDevice = null

            _deviceStatus.value = "Disconnected"
            _connectionQuality.value = 0.0

            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting device", e)
            return@withContext false
        }
    }

    suspend fun cleanup(): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Cleaning up GSR recorder resources")

        try {

            if (_isRecording.get()) {
                stopRecording()
            }

            disconnectDevice()

            shimmerManager?.stopScanBle()
            shimmerManager = null

            discoveredDevices.clear()

            Log.i(TAG, "GSR recorder cleanup completed")
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
            return@withContext false
        }
    }

    private val shimmerCallback =
        object : ShimmerBluetoothManagerAndroid.ShimmerBluetoothManagerCallback {

            override fun onConnectionStateChange(shimmer: Shimmer, state: Int) {
                Log.i(
                    TAG,
                    "Shimmer connection state changed: $state for device ${shimmer.macAddress}"
                )

                when (state) {
                    ShimmerDevice.SHIMMER_STATE_CONNECTED -> {
                        connectedShimmer = shimmer
                        mainHandler.post {
                            _deviceStatus.value = "Connected: ${shimmer.deviceName}"
                            _connectionQuality.value = 0.8
                        }
                    }

                    ShimmerDevice.SHIMMER_STATE_CONNECTING -> {
                        mainHandler.post {
                            _deviceStatus.value = "Connecting..."
                        }
                    }

                    ShimmerDevice.SHIMMER_STATE_STREAMING -> {
                        mainHandler.post {
                            _deviceStatus.value = "Streaming: ${shimmer.deviceName}"
                            _connectionQuality.value = 1.0
                        }
                    }

                    ShimmerDevice.SHIMMER_STATE_NONE -> {
                        if (connectedShimmer?.macAddress == shimmer.macAddress) {
                            connectedShimmer = null
                        }
                        mainHandler.post {
                            _deviceStatus.value = "Disconnected"
                            _connectionQuality.value = 0.0
                        }
                    }
                }
            }

            override fun onDataReceived(shimmer: Shimmer, objectCluster: ObjectCluster) {

                lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    processGSRData(shimmer, objectCluster)
                }
            }

            override fun onError(shimmer: Shimmer, error: String) {
                Log.e(TAG, "Shimmer error for device ${shimmer.macAddress}: $error")
                mainHandler.post {
                    _deviceStatus.value = "Error: $error"
                    _connectionQuality.value = 0.2
                }
            }
        }

    private suspend fun processGSRData(shimmer: Shimmer, objectCluster: ObjectCluster) {
        if (!_isRecording.get()) return

        try {
            val timestamp = System.nanoTime()
            val iso =
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())

            val gsrCalibratedData =
                objectCluster.getFormatClusterValue(Shimmer.CHANNEL_TYPE_CAL, "GSR")
            val gsrRawData = objectCluster.getFormatClusterValue(Shimmer.CHANNEL_TYPE_UNCAL, "GSR")

            val ppgRawData =
                objectCluster.getFormatClusterValue(Shimmer.CHANNEL_TYPE_UNCAL, "PPG_A13")

            val gsrMicrosiemens = gsrCalibratedData?.data ?: 0.0
            val gsrRaw = gsrRawData?.data ?: 0.0
            val ppgRaw = ppgRawData?.data ?: 0.0

            val gsrRawInt = gsrRaw.toInt()
            val qualityScore = when {
                gsrRawInt < 0 || gsrRawInt > ADC_RESOLUTION_12BIT.toInt() -> 0.0  // Out of range
                gsrMicrosiemens <= 0 -> 0.5  // Calibration issue
                else -> _connectionQuality.value
            }

            val gsrSample = GSRSample(
                timestamp = timestamp,
                timestampIso = iso,
                gsrMicrosiemens = gsrMicrosiemens,
                gsrRaw = gsrRawInt,
                ppgRaw = ppgRaw.toInt(),
                qualityScore = qualityScore,
                connectionRssi = -50  // Default RSSI
            )

            gsrDataFlow.tryEmit(gsrSample)

            csvWriter?.write("${timestamp},${iso},${gsrMicrosiemens},${gsrRawInt},${ppgRaw.toInt()},${qualityScore},-50\n")
            if (recordedSamples.incrementAndGet() % 100 == 0L) {
                csvWriter?.flush()  // Flush every 100 samples
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing GSR data", e)
        }
    }
}
