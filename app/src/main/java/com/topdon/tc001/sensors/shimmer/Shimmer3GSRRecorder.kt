package com.topdon.tc001.sensors.shimmer

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
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
import com.shimmerresearch.driver.CallbackObject
import com.shimmerresearch.driver.ObjectCluster
import com.topdon.tc001.sensors.SensorRecorder
import com.topdon.tc001.sensors.shimmer.model.ConnectionQuality
import com.topdon.tc001.sensors.shimmer.model.GSRSample
import com.topdon.tc001.sensors.shimmer.model.ShimmerDeviceInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class Shimmer3GSRRecorder(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    override val sensorId: String = "shimmer3_gsr_plus",
    private val samplingRateHz: Int = 128
) : SensorRecorder {

    companion object {
        private const val TAG = "Shimmer3GSRRecorder"

        private val SHIMMER_MAC_PREFIXES = listOf("00:06:66", "d0:39:72", "00:80:98")
        private val SHIMMER3_GSR_DEVICE_NAMES = listOf("Shimmer3-GSR", "Shimmer_GSR", "GSRShimmer")

        private const val GSR_RANGE_AUTO = 4  // Autorange for optimal sensitivity
        private const val ADC_RESOLUTION_12BIT = 4095.0  // **12-bit ADC range (CRITICAL)**
        private const val DEFAULT_SAMPLING_RATE = 128.0  // Research-grade sampling rate
        private const val GSR_FEEDBACK_RESISTOR = 40200.0  // Ohms (Shimmer3 spec)

        private const val MIN_CONNECTION_STRENGTH = -80  // dBm
        private const val MAX_DATA_GAP_MS = 25  // Maximum acceptable gap (< 2 samples @ 128Hz)
        private const val MIN_QUALITY_SCORE = 0.85  // Research-grade quality threshold
        private const val MAX_NOISE_VARIANCE = 0.05  // GSR signal stability threshold

        fun hasRequiredPermissions(context: Context): Boolean {
            val requiredPerms = getRequiredPermissions()
            return requiredPerms.all { permission ->
                ActivityCompat.checkSelfPermission(
                    context,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            }
        }

        fun getRequiredPermissions(): Array<String> =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            } else {
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }

        fun isShimmerGSRDevice(device: BluetoothDevice): Boolean {
            val macAddress = device.address ?: return false
            val deviceName = device.name ?: ""

            val hasValidMAC = SHIMMER_MAC_PREFIXES.any { prefix ->
                macAddress.startsWith(prefix, ignoreCase = true)
            }

            val hasValidName = SHIMMER3_GSR_DEVICE_NAMES.any { name ->
                deviceName.contains(name, ignoreCase = true)
            }

            return hasValidMAC || hasValidName
        }

        fun calculateDevicePriority(device: BluetoothDevice, rssi: Int): Int {
            var priority = 0

            when {
                device.address.startsWith("00:06:66") -> priority += 100  // Primary Shimmer MAC
                device.address.startsWith("d0:39:72") -> priority += 90   // Secondary Shimmer MAC  
                device.address.startsWith("00:80:98") -> priority += 80   // Alternative MAC
            }

            device.name?.let { name ->
                when {
                    name.contains("Shimmer3-GSR", true) -> priority += 50
                    name.contains("GSR", true) -> priority += 30
                    name.contains("Shimmer", true) -> priority += 20
                }
            }

            priority += maxOf(0, (rssi + 100) / 2)  // Convert dBm to priority points

            return priority
        }
    }

    override val sensorType: String = "Shimmer3 GSR+ (Galvanic Skin Response)"
    override val samplingRate: Double = samplingRateHz.toDouble()

    private val _isRecording = AtomicBoolean(false)
    override val isRecording: Boolean get() = _isRecording.get()

    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var shimmerManager: ShimmerBluetoothManagerAndroid? = null
    private var connectedShimmer: Shimmer? = null

    private val discoveredDevices = mutableMapOf<String, ShimmerDeviceInfo>()
    private var selectedDevice: ShimmerDeviceInfo? = null
    private var deviceScanJob: Job? = null

    private val gsrDataFlow = MutableSharedFlow<GSRSample>(
        replay = 1000,  // Buffer for late subscribers
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private var recordingJob: Job? = null
    private var sessionDirectory: File? = null
    private var csvWriter: FileWriter? = null
    private val recordedSamples = AtomicLong(0)
    private var recordingStartTime: Long = 0

    private val _connectionQuality = MutableStateFlow(ConnectionQuality.UNKNOWN)
    val connectionQuality: StateFlow<ConnectionQuality> = _connectionQuality.asStateFlow()

    private val _deviceStatus = MutableStateFlow("Disconnected")
    val deviceStatus: StateFlow<String> = _deviceStatus.asStateFlow()

    private val _samplesCollected = MutableStateFlow(0L)
    val samplesCollected: StateFlow<Long> = _samplesCollected.asStateFlow()

    private val _dataQualityScore = MutableStateFlow(0.0)
    val dataQualityScore: StateFlow<Double> = _dataQualityScore.asStateFlow()

    private val recentGSRValues = mutableListOf<Double>()
    private var lastSampleTime = 0L
    private var connectionDrops = 0
    private var totalSamples = 0L

    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        Log.d(TAG, "Initializing Shimmer3 GSR+ Recorder with ${samplingRateHz}Hz sampling")
        initializeBluetooth()
    }

    private fun initializeBluetooth() {
        try {
            bluetoothManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            bluetoothAdapter = bluetoothManager?.adapter

            if (bluetoothAdapter == null) {
                Log.e(TAG, "Bluetooth not supported on this device")
                _deviceStatus.value = "Bluetooth Not Supported"
                return
            }

            if (!bluetoothAdapter!!.isEnabled) {
                Log.w(TAG, "Bluetooth is not enabled")
                _deviceStatus.value = "Bluetooth Disabled"
                return
            }

            shimmerManager = ShimmerBluetoothManagerAndroid(context, shimmerCallback)
            _deviceStatus.value = "Initialized"

            Log.d(TAG, "Bluetooth and Shimmer components initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Bluetooth components", e)
            _deviceStatus.value = "Initialization Failed"
        }
    }

    suspend fun discoverShimmerDevices(): Flow<List<ShimmerDeviceInfo>> = flow {
        if (!hasRequiredPermissions(context)) {
            Log.e(TAG, "Required permissions not granted for device discovery")
            emit(emptyList())
            return@flow
        }

        _deviceStatus.value = "Scanning for Devices"
        discoveredDevices.clear()

        try {

            withContext(Dispatchers.Main) {
                shimmerManager?.startScanBtDevices()
            }

            repeat(100) { // 10 seconds with 100ms intervals
                delay(100)

                bluetoothAdapter?.bondedDevices?.forEach { device ->
                    if (isShimmerGSRDevice(device)) {
                        val deviceInfo = ShimmerDeviceInfo(
                            macAddress = device.address,
                            name = device.name ?: "Unknown Shimmer",
                            rssi = -50, // Estimate for paired devices
                            isPaired = true,
                            priority = calculateDevicePriority(device, -50),
                            connectionState = "Available"
                        )
                        discoveredDevices[device.address] = deviceInfo
                    }
                }

                val sortedDevices = discoveredDevices.values.sortedByDescending { it.priority }
                emit(sortedDevices)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error during device discovery", e)
            emit(emptyList())
        } finally {
            withContext(Dispatchers.Main) {
                shimmerManager?.stopScanBtDevices()
            }
            _deviceStatus.value = "Scan Complete"
        }
    }

    suspend fun connectToDevice(deviceInfo: ShimmerDeviceInfo): Boolean =
        withContext(Dispatchers.IO) {
            if (!hasRequiredPermissions(context)) {
                Log.e(TAG, "Required permissions not granted for device connection")
                return@withContext false
            }

            try {
                _deviceStatus.value = "Connecting to ${deviceInfo.name}"
                selectedDevice = deviceInfo

                val success = withContext(Dispatchers.Main) {
                    shimmerManager?.connectShimmerThroughBTAddress(deviceInfo.macAddress) ?: false
                }

                if (success) {
                    Log.d(TAG, "Successfully initiated connection to ${deviceInfo.macAddress}")
                    _deviceStatus.value = "Connected"
                    return@withContext true
                } else {
                    Log.e(TAG, "Failed to connect to ${deviceInfo.macAddress}")
                    _deviceStatus.value = "Connection Failed"
                    return@withContext false
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to device", e)
                _deviceStatus.value = "Connection Error"
                return@withContext false
            }
        }

    private suspend fun configureGSRSensor(shimmer: Shimmer) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Configuring Shimmer device for GSR recording")

            shimmer.samplingRate = DEFAULT_SAMPLING_RATE

            shimmer.setEnabledSensors(Shimmer.SENSOR_GSR, true)

            shimmer.setGSRRange(GSR_RANGE_AUTO)

            shimmer.setLSM303DLHCAccelRange(0) // Minimize interference
            shimmer.setMPU9150GyroRange(0)     // Minimize power consumption

            shimmer.writeEnabledSensors()

            Log.d(TAG, "Shimmer GSR configuration completed successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure GSR sensor", e)
            throw e
        }
    }

    override suspend fun startRecording(outputDirectory: File): Boolean =
        withContext(Dispatchers.IO) {
            if (_isRecording.get()) {
                Log.w(TAG, "Recording already in progress")
                return@withContext true
            }

            if (connectedShimmer == null) {
                Log.e(TAG, "No Shimmer device connected")
                return@withContext false
            }

            try {

                sessionDirectory =
                    File(outputDirectory, "gsr_session_${System.currentTimeMillis()}")
                sessionDirectory?.mkdirs()

                val csvFile = File(sessionDirectory, "gsr_data.csv")
                csvWriter = FileWriter(csvFile)

                csvWriter?.apply {
                    write("# Shimmer3 GSR+ Recording Session\n")
                    write("# Device: ${selectedDevice?.name} (${selectedDevice?.macAddress})\n")
                    write("# Sampling Rate: ${samplingRateHz} Hz\n")
                    write("# ADC Resolution: 12-bit (0-4095)\n")
                    write("# Feedback Resistor: ${GSR_FEEDBACK_RESISTOR} Ohms\n")
                    write(
                        "# Session Start: ${
                            SimpleDateFormat(
                                "yyyy-MM-dd HH:mm:ss.SSS",
                                Locale.getDefault()
                            ).format(Date())
                        }\n"
                    )
                    write("# \n")
                    write("timestamp_ns,gsr_microsiemens,raw_adc_12bit,resistance_ohms,quality_score,connection_rssi\n")
                    flush()
                }

                configureGSRSensor(connectedShimmer!!)

                recentGSRValues.clear()
                lastSampleTime = System.nanoTime()
                connectionDrops = 0
                totalSamples = 0
                recordedSamples.set(0)
                recordingStartTime = System.currentTimeMillis()

                connectedShimmer?.startStreaming()

                _isRecording.set(true)
                _deviceStatus.value = "Recording"

                Log.d(TAG, "GSR recording started successfully")
                return@withContext true

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start GSR recording", e)
                _isRecording.set(false)
                _deviceStatus.value = "Recording Failed"
                return@withContext false
            }
        }

    private fun processGSRSample(objectCluster: ObjectCluster) {
        try {
            val timestamp = System.nanoTime()

            val rawADC = objectCluster.getFormatClusterValue("GSR", "RAW")?.toInt() ?: 0

            if (rawADC < 0 || rawADC > ADC_RESOLUTION_12BIT.toInt()) {
                Log.w(TAG, "Invalid 12-bit ADC value: $rawADC (expected 0-4095)")
                return
            }

            val voltage = (rawADC / ADC_RESOLUTION_12BIT) * 3.0  // 3V reference
            val resistance = if (voltage > 0) {
                (GSR_FEEDBACK_RESISTOR * (3.0 - voltage)) / voltage
            } else {
                Double.MAX_VALUE
            }

            val gsrMicrosiemens = if (resistance > 0 && resistance != Double.MAX_VALUE) {
                1_000_000.0 / resistance  // Convert to µS
            } else {
                0.0
            }

            val qualityScore = calculateDataQuality(gsrMicrosiemens, timestamp)
            val connectionRSSI = selectedDevice?.rssi ?: -999

            val gsrSample = GSRSample(
                timestampNanos = timestamp,
                gsrMicrosiemens = gsrMicrosiemens,
                rawADC12Bit = rawADC,
                resistanceOhms = resistance,
                qualityScore = qualityScore,
                connectionRSSI = connectionRSSI,
                sessionId = recordingStartTime.toString()
            )

            csvWriter?.apply {
                write("${timestamp},${gsrMicrosiemens},${rawADC},${resistance},${qualityScore},${connectionRSSI}\n")
                flush()
            }

            lifecycleOwner.lifecycleScope.launch {
                gsrDataFlow.emit(gsrSample)
                _samplesCollected.value = recordedSamples.incrementAndGet()
                _dataQualityScore.value = qualityScore
            }

            totalSamples++
            lastSampleTime = timestamp

        } catch (e: Exception) {
            Log.e(TAG, "Error processing GSR sample", e)
        }
    }

    private fun calculateDataQuality(gsrValue: Double, timestamp: Long): Double {
        var qualityScore = 1.0

        try {

            if (lastSampleTime > 0) {
                val timeDelta = (timestamp - lastSampleTime) / 1_000_000.0 // Convert to ms
                val expectedDelta = 1000.0 / samplingRateHz
                val deltaDeviation = kotlin.math.abs(timeDelta - expectedDelta) / expectedDelta

                if (deltaDeviation > 0.1) { // >10% deviation
                    qualityScore *= 0.8
                }

                if (timeDelta > MAX_DATA_GAP_MS) { // Gap too large
                    qualityScore *= 0.5
                    connectionDrops++
                }
            }

            recentGSRValues.add(gsrValue)
            if (recentGSRValues.size > 10) {
                recentGSRValues.removeAt(0) // Keep sliding window

                val mean = recentGSRValues.average()
                val variance = recentGSRValues.map { (it - mean) * (it - mean) }.average()
                val normalizedVariance = if (mean > 0) variance / (mean * mean) else 1.0

                if (normalizedVariance > MAX_NOISE_VARIANCE) {
                    qualityScore *= 0.9 // High noise penalty
                }
            }

            if (totalSamples > 100) {
                val dropRate = connectionDrops.toDouble() / totalSamples
                qualityScore *= (1.0 - dropRate * 2) // Penalty for drops
            }

            val connectionQual = when {
                qualityScore >= 0.95 -> ConnectionQuality.EXCELLENT
                qualityScore >= 0.85 -> ConnectionQuality.GOOD
                qualityScore >= 0.70 -> ConnectionQuality.FAIR
                qualityScore >= 0.50 -> ConnectionQuality.POOR
                else -> ConnectionQuality.CRITICAL
            }

            _connectionQuality.value = connectionQual

            return qualityScore.coerceIn(0.0, 1.0)

        } catch (e: Exception) {
            Log.e(TAG, "Error calculating data quality", e)
            return 0.5 // Default moderate quality on error
        }
    }

    override suspend fun stopRecording(): Boolean = withContext(Dispatchers.IO) {
        if (!_isRecording.get()) {
            Log.w(TAG, "No recording in progress")
            return@withContext true
        }

        try {

            connectedShimmer?.stopStreaming()

            csvWriter?.apply {
                write("# \n")
                write(
                    "# Session End: ${
                        SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss.SSS",
                            Locale.getDefault()
                        ).format(Date())
                    }\n"
                )
                write("# Total Samples: ${recordedSamples.get()}\n")
                write("# Recording Duration: ${(System.currentTimeMillis() - recordingStartTime) / 1000.0}s\n")
                write("# Average Quality Score: ${_dataQualityScore.value}\n")
                write("# Connection Drops: ${connectionDrops}\n")
                close()
            }

            _isRecording.set(false)
            _deviceStatus.value = "Recording Stopped"

            Log.d(TAG, "GSR recording stopped. ${recordedSamples.get()} samples recorded")
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping GSR recording", e)
            return@withContext false
        }
    }

    suspend fun disconnect(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (_isRecording.get()) {
                stopRecording()
            }

            connectedShimmer?.let { shimmer ->
                shimmer.stop()
                shimmer.disconnect()
            }

            connectedShimmer = null
            selectedDevice = null
            _deviceStatus.value = "Disconnected"
            _connectionQuality.value = ConnectionQuality.UNKNOWN

            Log.d(TAG, "Disconnected from Shimmer device")
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from device", e)
            return@withContext false
        }
    }

    fun getGSRDataFlow(): SharedFlow<GSRSample> = gsrDataFlow.asSharedFlow()

    private val shimmerCallback =
        object : ShimmerBluetoothManagerAndroid.ShimmerBluetoothManagerCallback {

            override fun onDeviceConnected(shimmer: Shimmer) {
                connectedShimmer = shimmer
                _deviceStatus.value = "Connected: ${shimmer.deviceName}"
                Log.d(TAG, "Device connected: ${shimmer.macId}")
            }

            override fun onDeviceDisconnected(shimmer: Shimmer) {
                if (shimmer == connectedShimmer) {
                    connectedShimmer = null
                    _deviceStatus.value = "Disconnected"
                    _connectionQuality.value = ConnectionQuality.UNKNOWN
                    Log.d(TAG, "Device disconnected: ${shimmer.macId}")
                }
            }

            override fun onNewObjectCluster(callBackObject: CallbackObject) {
                if (_isRecording.get()) {
                    processGSRSample(callBackObject.objectCluster)
                }
            }

            override fun onDeviceFound(
                bluetoothDevice: BluetoothDevice,
                rssi: Int,
                scanRecord: ByteArray?
            ) {
                if (isShimmerGSRDevice(bluetoothDevice)) {
                    val deviceInfo = ShimmerDeviceInfo(
                        macAddress = bluetoothDevice.address,
                        name = bluetoothDevice.name ?: "Unknown Shimmer",
                        rssi = rssi,
                        isPaired = false,
                        priority = calculateDevicePriority(bluetoothDevice, rssi),
                        connectionState = "Discovered"
                    )
                    discoveredDevices[bluetoothDevice.address] = deviceInfo
                    Log.d(
                        TAG,
                        "Shimmer GSR device found: ${deviceInfo.name} (${deviceInfo.macAddress}) RSSI: ${rssi}dBm"
                    )
                }
            }

            override fun onScanFinished() {
                Log.d(TAG, "Device scan finished. Found ${discoveredDevices.size} Shimmer devices")
            }
        }

    fun cleanup() {
        lifecycleOwner.lifecycleScope.launch {
            if (_isRecording.get()) {
                stopRecording()
            }
            disconnect()
        }

        deviceScanJob?.cancel()
        recordingJob?.cancel()

        csvWriter?.close()
        shimmerManager?.stopScanBtDevices()

        Log.d(TAG, "Shimmer3GSRRecorder cleanup completed")
    }
}
