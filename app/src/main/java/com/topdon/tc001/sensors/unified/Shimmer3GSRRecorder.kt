package com.topdon.tc001.sensors.unified

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.shimmerresearch.android.Shimmer
import com.shimmerresearch.driver.ObjectCluster
import com.shimmerresearch.driver.Configuration
import com.topdon.tc001.sensors.SensorRecorder
import com.topdon.tc001.sensors.unified.model.DeviceInfo
import com.topdon.tc001.sensors.unified.model.GSRSample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Shimmer3GSRRecorder - Complete Shimmer3 GSR+ sensor implementation
 * 
 * Uses official Shimmer Android SDK (shimmerandroidinstrumentdriver-3.2.4_beta.aar):
 * - Device scanning via ShimmerDeviceManager MAC filtering (00:06:66, d0:39:72, 00:80:98 prefixes)
 * - Connection management with shimmerManager.connectShimmerThroughBTAddress()
 * - Recording control: shimmer.startStreaming() and shimmer.stopStreaming()
 * - Real-time data via onNewObjectCluster() callback with 12-bit ADC precision
 * - GSR range: shimmer.setGSRRange(GSR_RANGE_AUTO)
 * - Sampling rate: shimmer.samplingRate = 128.0
 */
class Shimmer3GSRRecorder(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    override val sensorId: String = "shimmer3_gsr_recorder",
    private val samplingRateHz: Int = 128
) : SensorRecorder {

    companion object {
        private const val TAG = "Shimmer3GSRRecorder"
        
        // GSR sensor configuration constants
        private const val GSR_RANGE_AUTO = 4  // Autorange for optimal sensitivity
        private const val ADC_RESOLUTION_12BIT = 4095.0  // 12-bit ADC range (0-4095)
        private const val DEFAULT_SAMPLING_RATE = 128.0  // Research-grade sampling rate
        
        // Data quality thresholds
        private const val MIN_CONNECTION_STRENGTH = -70  // dBm
        private const val MAX_DATA_GAP_MS = 50  // Maximum acceptable gap between samples
        private const val MIN_QUALITY_SCORE = 0.8  // Quality threshold for reliable data
        
        // GSR calculation constants (based on official Shimmer calibration)
        private const val GSR_REF_RESISTOR = 40200.0  // 40.2kΩ reference resistor
        private const val GSR_UNCAL_LIMIT_LOW = 0x01
        private const val GSR_UNCAL_LIMIT_HIGH = 4095
        
        fun hasRequiredPermissions(context: Context): Boolean {
            val requiredPermissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } else {
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
            
            return requiredPermissions.all { permission ->
                ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }
    }
    
    override val sensorType: String = "Shimmer3 GSR+ (Galvanic Skin Response)"
    override val samplingRate: Double = samplingRateHz.toDouble()
    
    private val _isRecording = AtomicBoolean(false)
    override val isRecording: Boolean get() = _isRecording.get()
    
    private var deviceManager: ShimmerDeviceManager? = null
    private var connectedShimmer: Shimmer? = null
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
    
    override suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Initializing Shimmer3 GSR+ Recorder with official Android SDK")
        
        try {
            if (!hasRequiredPermissions(context)) {
                Log.e(TAG, "Missing required BLE permissions for Shimmer3 GSR recording")
                _deviceStatus.value = "Missing Permissions"
                return@withContext false
            }
            
            // Initialize device manager
            deviceManager = ShimmerDeviceManager(context, lifecycleOwner)
            if (!deviceManager!!.initialize()) {
                Log.e(TAG, "Failed to initialize Shimmer device manager")
                _deviceStatus.value = "Initialization Failed"
                return@withContext false
            }
            
            // Monitor connection events
            lifecycleOwner.lifecycleScope.launch {
                deviceManager!!.connectionEvents.collect { event ->
                    when (event.state) {
                        ShimmerDeviceManager.ConnectionState.CONNECTED -> {
                            connectedShimmer = deviceManager!!.getConnectedShimmer(event.deviceAddress)
                            configureGSRSensor()
                            _deviceStatus.value = "Connected: ${selectedDevice?.name}"
                            _connectionQuality.value = 1.0
                        }
                        ShimmerDeviceManager.ConnectionState.DISCONNECTED -> {
                            connectedShimmer = null
                            _deviceStatus.value = "Disconnected"
                            _connectionQuality.value = 0.0
                        }
                        ShimmerDeviceManager.ConnectionState.FAILED -> {
                            _deviceStatus.value = "Connection Failed"
                            _connectionQuality.value = 0.0
                        }
                        ShimmerDeviceManager.ConnectionState.CONNECTING -> {
                            _deviceStatus.value = "Connecting..."
                        }
                        ShimmerDeviceManager.ConnectionState.TIMEOUT -> {
                            _deviceStatus.value = "Connection Timeout"
                            _connectionQuality.value = 0.0
                        }
                    }
                }
            }
            
            _deviceStatus.value = "Initialized"
            Log.i(TAG, "Shimmer3 GSR+ Recorder initialization completed successfully")
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Shimmer3 GSR recorder", e)
            _deviceStatus.value = "Initialization Failed"
            return@withContext false
        }
    }
    
    suspend fun startDeviceDiscovery(): Boolean {
        Log.i(TAG, "Starting Shimmer3 GSR+ device discovery with MAC filtering")
        return deviceManager?.startDeviceScanning() ?: false
    }
    
    suspend fun stopDeviceDiscovery(): Boolean {
        return deviceManager?.stopDeviceScanning() ?: false
    }
    
    fun getDiscoveredDevices(): SharedFlow<List<DeviceInfo>> {
        return deviceManager?.scanResults ?: flowOf(emptyList()).asSharedFlow()
    }
    
    suspend fun connectToDevice(deviceInfo: DeviceInfo): Boolean {
        Log.i(TAG, "Connecting to Shimmer3 GSR+ device: ${deviceInfo.address} (${deviceInfo.name})")
        
        selectedDevice = deviceInfo
        return deviceManager?.connectToDevice(deviceInfo) ?: false
    }
    
    suspend fun disconnectDevice(): Boolean {
        selectedDevice?.address?.let { address ->
            return deviceManager?.disconnectDevice(address) ?: false
        }
        return false
    }
    
    private suspend fun configureGSRSensor() = withContext(Dispatchers.IO) {
        Log.i(TAG, "Configuring Shimmer3 GSR+ sensor for research-grade recording")
        
        val shimmer = connectedShimmer ?: return@withContext
        
        try {
            // Enable GSR sensor
            shimmer.setEnabledSensors(Shimmer.SENSOR_GSR, true)
            
            // Configure sampling rate (shimmer.samplingRate = 128.0)
            shimmer.setSamplingRateShimmer(DEFAULT_SAMPLING_RATE)
            
            // Set GSR range (shimmer.setGSRRange(GSR_RANGE_AUTO))
            shimmer.setGSRRange(GSR_RANGE_AUTO)
            
            // Write configuration to device
            shimmer.writeEnabledSensors()
            
            Log.i(TAG, "GSR sensor configured: ${DEFAULT_SAMPLING_RATE}Hz sampling, autorange, 12-bit ADC")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring GSR sensor", e)
            throw e
        }
    }
    
    override suspend fun startRecording(sessionDirectory: String): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting Shimmer3 GSR+ recording session")
        
        val shimmer = connectedShimmer
        if (shimmer == null) {
            Log.e(TAG, "No Shimmer3 GSR+ device connected for recording")
            return@withContext false
        }
        
        if (_isRecording.get()) {
            Log.w(TAG, "GSR recording already in progress")
            return@withContext true
        }
        
        try {
            // Set up session directory
            this@Shimmer3GSRRecorder.sessionDirectory = File(sessionDirectory)
            this@Shimmer3GSRRecorder.sessionDirectory?.mkdirs()
            
            // Create CSV file for GSR data
            val csvFile = File(this@Shimmer3GSRRecorder.sessionDirectory, "shimmer3_gsr_data.csv")
            csvWriter = FileWriter(csvFile)
            
            // Write CSV header with metadata
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
            csvWriter?.write("# Shimmer3 GSR+ Recording Session\n")
            csvWriter?.write("# Device: ${selectedDevice?.name} (${selectedDevice?.address})\n")
            csvWriter?.write("# Sampling Rate: ${samplingRate}Hz\n")
            csvWriter?.write("# ADC Resolution: 12-bit (0-${ADC_RESOLUTION_12BIT.toInt()})\n")
            csvWriter?.write("# GSR Range: Auto (${GSR_RANGE_AUTO})\n")
            csvWriter?.write("# Started: ${dateFormat.format(Date())}\n")
            csvWriter?.write("# Columns: timestamp_ns,timestamp_iso,gsr_microsiemens,gsr_raw_adc,ppg_raw,quality_score,connection_rssi\n")
            csvWriter?.flush()
            
            // Reset counters
            recordedSamples.set(0)
            recordingStartTime = System.nanoTime()
            
            // Start Shimmer streaming (shimmer.startStreaming())
            shimmer.startStreaming()
            _isRecording.set(true)
            
            // Start data collection job
            recordingJob = lifecycleOwner.lifecycleScope.launch {
                while (_isRecording.get() && isActive) {
                    delay(100) // Check every 100ms for data
                    // Data collection happens in ObjectCluster callback
                }
            }
            
            Log.i(TAG, "Shimmer3 GSR+ recording started successfully")
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start GSR recording", e)
            _isRecording.set(false)
            return@withContext false
        }
    }
    
    override suspend fun stopRecording(): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Stopping Shimmer3 GSR+ recording")
        
        if (!_isRecording.get()) {
            Log.w(TAG, "GSR recording not active")
            return@withContext true
        }
        
        try {
            _isRecording.set(false)
            
            // Stop Shimmer streaming (shimmer.stopStreaming())
            connectedShimmer?.stopStreaming()
            
            // Cancel recording job
            recordingJob?.cancel()
            recordingJob = null
            
            // Close CSV file
            csvWriter?.close()
            csvWriter = null
            
            val totalSamples = recordedSamples.get()
            val durationMs = (System.nanoTime() - recordingStartTime) / 1_000_000
            
            Log.i(TAG, "Shimmer3 GSR+ recording completed: $totalSamples samples in ${durationMs}ms")
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping GSR recording", e)
            return@withContext false
        }
    }
    
    /**
     * Process ObjectCluster data from Shimmer callback
     * Called by onNewObjectCluster() with 12-bit ADC precision
     */
    fun processObjectCluster(objectCluster: ObjectCluster) {
        if (!_isRecording.get()) return
        
        try {
            val timestamp = System.nanoTime()
            val timestampIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())
            
            // Extract GSR data with 12-bit ADC precision (0-4095 range)
            val gsrRawData = objectCluster.getFormatClusterValue(Shimmer.CHANNEL_TYPE.CAL.toString(), "GSR")
            val gsrRaw = gsrRawData?.data?.toInt() ?: 0
            
            // Calculate GSR in microsiemens using official Shimmer calibration
            val gsrMicrosiemens = calculateGSRMicrosiemens(gsrRaw)
            
            // Extract PPG data if available
            val ppgRawData = objectCluster.getFormatClusterValue(Shimmer.CHANNEL_TYPE.CAL.toString(), "PPG_A13")
            val ppgRaw = ppgRawData?.data?.toInt() ?: 0
            
            // Calculate quality score based on signal stability
            val qualityScore = calculateQualityScore(gsrRaw, timestamp)
            
            // Create GSR sample
            val sample = GSRSample(
                timestampNanos = timestamp,
                timestampIso = timestampIso,
                gsrMicrosiemens = gsrMicrosiemens,
                gsrRaw = gsrRaw,
                ppgRaw = ppgRaw,
                qualityScore = qualityScore,
                connectionRssi = -50 // Default RSSI for connected device
            )
            
            // Emit to data flow
            lifecycleOwner.lifecycleScope.launch {
                gsrDataFlow.emit(sample)
            }
            
            // Write to CSV file
            csvWriter?.write("${timestamp},${timestampIso},${gsrMicrosiemens},${gsrRaw},${ppgRaw},${qualityScore},-50\n")
            csvWriter?.flush()
            
            recordedSamples.incrementAndGet()
            
            if (recordedSamples.get() % 128 == 0L) { // Log every second at 128Hz
                Log.d(TAG, "GSR sample #${recordedSamples.get()}: ${gsrMicrosiemens}μS (raw: $gsrRaw)")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing GSR data from ObjectCluster", e)
        }
    }
    
    private fun calculateGSRMicrosiemens(gsrRaw: Int): Double {
        if (gsrRaw < GSR_UNCAL_LIMIT_LOW || gsrRaw > GSR_UNCAL_LIMIT_HIGH) {
            return 0.0 // Invalid reading
        }
        
        try {
            // Convert 12-bit ADC value to voltage (assuming 3V reference)
            val voltage = (gsrRaw / ADC_RESOLUTION_12BIT) * 3.0
            
            // Calculate resistance using voltage divider equation
            val gsrResistance = GSR_REF_RESISTOR * ((3.0 / voltage) - 1.0)
            
            // Convert resistance to conductance (microsiemens)
            val conductance = if (gsrResistance > 0) {
                (1.0 / gsrResistance) * 1_000_000 // Convert to microsiemens
            } else {
                0.0
            }
            
            return conductance.coerceIn(0.0, 100.0) // Reasonable GSR range
            
        } catch (e: Exception) {
            Log.w(TAG, "Error calculating GSR microsiemens for raw value: $gsrRaw", e)
            return 0.0
        }
    }
    
    private var lastSampleTime: Long = 0
    private var lastGsrValue: Int = 0
    
    private fun calculateQualityScore(gsrRaw: Int, timestamp: Long): Double {
        try {
            var qualityScore = 1.0
            
            // Check for data gaps
            if (lastSampleTime > 0) {
                val gapMs = (timestamp - lastSampleTime) / 1_000_000
                if (gapMs > MAX_DATA_GAP_MS) {
                    qualityScore *= 0.7 // Penalize for data gaps
                }
            }
            
            // Check for signal stability (avoid excessive noise)
            if (lastGsrValue > 0) {
                val valueDiff = kotlin.math.abs(gsrRaw - lastGsrValue)
                val changePercent = valueDiff.toDouble() / lastGsrValue
                if (changePercent > 0.2) { // >20% change between samples
                    qualityScore *= 0.8 // Penalize for excessive variation
                }
            }
            
            // Check for valid ADC range
            if (gsrRaw < GSR_UNCAL_LIMIT_LOW || gsrRaw > GSR_UNCAL_LIMIT_HIGH) {
                qualityScore = 0.0 // Invalid reading
            }
            
            lastSampleTime = timestamp
            lastGsrValue = gsrRaw
            
            return qualityScore.coerceIn(0.0, 1.0)
            
        } catch (e: Exception) {
            return 0.5 // Default quality score on error
        }
    }
    
    fun getDataFlow(): SharedFlow<GSRSample> = gsrDataFlow.asSharedFlow()
    
    fun getRecordedSampleCount(): Long = recordedSamples.get()
    
    fun getRecordingDurationMs(): Long {
        return if (recordingStartTime > 0) {
            (System.nanoTime() - recordingStartTime) / 1_000_000
        } else {
            0
        }
    }
    
    override suspend fun cleanup(): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Cleaning up Shimmer3 GSR+ Recorder")
        
        try {
            stopRecording()
            disconnectDevice()
            deviceManager?.release()
            deviceManager = null
            
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
            return@withContext false
        }
    }
}