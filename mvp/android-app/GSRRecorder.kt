package com.ircamera.mvp

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import com.shimmerresearch.android.shimmer.Shimmer
import kotlinx.coroutines.*
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * MVP GSR Recorder - Shimmer3 GSR+ Integration
 * 
 * Core functionality:
 * - BLE connection to Shimmer3 GSR+
 * - Real-time GSR data collection
 * - Timestamped CSV logging
 * - 12-bit ADC precision (0-4095 range)
 */
class GSRRecorder(
    private val context: Context,
    private val onGSRUpdate: (Double) -> Unit
) {
    
    companion object {
        private const val TAG = "GSRRecorder"
        private const val SHIMMER_DEVICE_NAME = "Shimmer3"
        private const val GSR_SAMPLING_RATE = 128.0 // 128 Hz
        
        // GSR calculation constants (12-bit ADC)
        private const val ADC_RESOLUTION = 4095.0 // 12-bit: 0-4095
        private const val REFERENCE_VOLTAGE = 3.0
        private const val GSR_RANGE = 40.0 // kΩ range
    }
    
    private var shimmerDevice: Shimmer? = null
    private var isRecording = false
    private var csvWriter: FileWriter? = null
    private var recordingJob: Job? = null
    private var currentSessionId: String? = null
    
    // Data collection
    private val gsrDataBuffer = mutableListOf<GSRDataPoint>()
    private var lastTimestamp = 0L
    
    data class GSRDataPoint(
        val timestamp: Long,
        val gsrValue: Double,
        val rawADC: Int
    )
    
    /**
     * Start GSR recording session
     */
    fun startRecording(sessionId: String) {
        if (isRecording) {
            Log.w(TAG, "Recording already in progress")
            return
        }
        
        currentSessionId = sessionId
        isRecording = true
        
        recordingJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                initializeShimmer()
                startDataCollection()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start GSR recording", e)
                isRecording = false
                throw e
            }
        }
    }
    
    /**
     * Stop GSR recording and save data
     */
    fun stopRecording() {
        if (!isRecording) return
        
        isRecording = false
        recordingJob?.cancel()
        
        try {
            shimmerDevice?.let { shimmer ->
                if (shimmer.isConnected) {
                    shimmer.stopStreaming()
                    shimmer.disconnect()
                }
            }
            
            csvWriter?.close()
            csvWriter = null
            
            Log.i(TAG, "GSR recording stopped. Collected ${gsrDataBuffer.size} samples")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping GSR recording", e)
        }
    }
    
    private suspend fun initializeShimmer() = withContext(Dispatchers.Main) {
        try {
            // Find Shimmer device
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                ?: throw Exception("Bluetooth not available")
            
            if (!bluetoothAdapter.isEnabled) {
                throw Exception("Bluetooth not enabled")
            }
            
            val pairedDevices = bluetoothAdapter.bondedDevices
            val shimmerBtDevice = pairedDevices.find { 
                it.name?.contains(SHIMMER_DEVICE_NAME, ignoreCase = true) == true 
            } ?: throw Exception("Shimmer device not found in paired devices")
            
            // Initialize Shimmer connection
            shimmerDevice = Shimmer(context).apply {
                setBluetoothDevice(shimmerBtDevice)
                setShimmerUserAssignedName(shimmerBtDevice.name)
            }
            
            // Connect to Shimmer
            connectShimmer()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Shimmer", e)
            throw e
        }
    }
    
    private suspend fun connectShimmer() = withContext(Dispatchers.IO) {
        shimmerDevice?.let { shimmer ->
            try {
                // Connect to device
                shimmer.connect()
                
                // Wait for connection
                var attempts = 0
                while (!shimmer.isConnected && attempts < 30) { // 30 second timeout
                    delay(1000)
                    attempts++
                }
                
                if (!shimmer.isConnected) {
                    throw Exception("Failed to connect to Shimmer after 30 seconds")
                }
                
                // Configure for GSR recording
                configureShimmerForGSR(shimmer)
                
                Log.i(TAG, "Shimmer connected and configured for GSR recording")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to Shimmer", e)
                throw e
            }
        }
    }
    
    private fun configureShimmerForGSR(shimmer: Shimmer) {
        try {
            // Set sampling rate
            shimmer.setSamplingRateShimmer(GSR_SAMPLING_RATE)
            
            // Enable GSR sensor
            shimmer.setEnabledSensors(Shimmer.SENSOR_GSR)
            
            // Set GSR range (if available)
            shimmer.setGSRRange(0) // Auto range
            
            Log.i(TAG, "Shimmer configured: ${GSR_SAMPLING_RATE}Hz, GSR enabled")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure Shimmer", e)
            throw e
        }
    }
    
    private suspend fun startDataCollection() = withContext(Dispatchers.IO) {
        try {
            // Create CSV file
            val csvFile = createCSVFile()
            csvWriter = FileWriter(csvFile)
            
            // Write CSV header
            csvWriter?.append("timestamp_ns,gsr_microsiemens,raw_adc,session_id\n")
            
            // Set up data callback
            shimmerDevice?.setDataCallback { _, objectCluster ->
                CoroutineScope(Dispatchers.IO).launch {
                    processGSRData(objectCluster)
                }
            }
            
            // Start streaming
            shimmerDevice?.startStreaming()
            
            Log.i(TAG, "GSR data collection started")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start data collection", e)
            throw e
        }
    }
    
    private fun processGSRData(objectCluster: Any) {
        try {
            // Extract GSR data from Shimmer ObjectCluster
            // Note: This uses Shimmer Android API structure
            val gsrRaw = extractGSRRaw(objectCluster)
            val timestamp = System.nanoTime()
            
            // Convert raw ADC to microsiemens (12-bit precision)
            val gsrMicrosiemens = convertRawToMicrosiemens(gsrRaw)
            
            // Create data point
            val dataPoint = GSRDataPoint(timestamp, gsrMicrosiemens, gsrRaw)
            
            // Add to buffer
            synchronized(gsrDataBuffer) {
                gsrDataBuffer.add(dataPoint)
            }
            
            // Write to CSV
            csvWriter?.append("$timestamp,$gsrMicrosiemens,$gsrRaw,$currentSessionId\n")
            csvWriter?.flush()
            
            // Update UI callback
            onGSRUpdate(gsrMicrosiemens)
            
            lastTimestamp = timestamp
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing GSR data", e)
        }
    }
    
    private fun extractGSRRaw(objectCluster: Any): Int {
        // Extract raw GSR value from Shimmer ObjectCluster
        // This is a simplified version - actual implementation depends on Shimmer API
        try {
            // Using reflection to get GSR data from ObjectCluster
            val clusterClass = objectCluster.javaClass
            val getDataMethod = clusterClass.getMethod("getData", String::class.java)
            val gsrData = getDataMethod.invoke(objectCluster, "GSR")
            
            return (gsrData as? Double)?.toInt() ?: 0
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract GSR raw value", e)
            return 0
        }
    }
    
    private fun convertRawToMicrosiemens(rawADC: Int): Double {
        // Convert 12-bit ADC value to microsiemens
        // Formula: GSR(μS) = 1 / (R_GSR(kΩ) * 1000)
        // Where R_GSR = (ADC * RefVolt / ADC_Resolution) * Range / RefVolt
        
        if (rawADC <= 0) return 0.0
        
        val voltage = (rawADC * REFERENCE_VOLTAGE) / ADC_RESOLUTION
        val resistance = (voltage / REFERENCE_VOLTAGE) * GSR_RANGE * 1000 // Convert to Ω
        
        return if (resistance > 0) 1_000_000.0 / resistance else 0.0 // Convert to μS
    }
    
    private fun createCSVFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "gsr_data_${currentSessionId}_$timestamp.csv"
        
        val dataDir = File(context.getExternalFilesDir(null), "gsr_data")
        if (!dataDir.exists()) {
            dataDir.mkdirs()
        }
        
        return File(dataDir, filename)
    }
    
    /**
     * Get current GSR statistics
     */
    fun getCurrentStats(): GSRStats? {
        if (!isRecording || gsrDataBuffer.isEmpty()) return null
        
        synchronized(gsrDataBuffer) {
            val recentData = gsrDataBuffer.takeLast(100) // Last 100 samples
            val gsrValues = recentData.map { it.gsrValue }
            
            return GSRStats(
                sampleCount = gsrDataBuffer.size,
                currentValue = gsrValues.lastOrNull() ?: 0.0,
                averageValue = gsrValues.average(),
                minValue = gsrValues.minOrNull() ?: 0.0,
                maxValue = gsrValues.maxOrNull() ?: 0.0,
                recordingDurationMs = if (gsrDataBuffer.isNotEmpty()) {
                    (lastTimestamp - gsrDataBuffer.first().timestamp) / 1_000_000
                } else 0L
            )
        }
    }
    
    data class GSRStats(
        val sampleCount: Int,
        val currentValue: Double,
        val averageValue: Double,
        val minValue: Double,
        val maxValue: Double,
        val recordingDurationMs: Long
    )
}