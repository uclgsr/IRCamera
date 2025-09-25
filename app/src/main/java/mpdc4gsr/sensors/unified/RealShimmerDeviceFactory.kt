package mpdc4gsr.sensors.unified

import android.content.Context
import android.util.Log
import com.shimmerresearch.android.Shimmer
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
import com.shimmerresearch.driver.ObjectCluster
import com.mpdc4gsr.gsr.service.ShimmerDataCluster
import com.mpdc4gsr.gsr.service.ShimmerDeviceFactory
import com.mpdc4gsr.gsr.service.ShimmerDeviceInterface

/**
 * Real Shimmer device factory using the actual shimmer libraries from app/libs
 * Uses shimmerandroidinstrumentdriver-3.2.4_beta.aar and related shimmer libraries
 */
class RealShimmerDeviceFactory(
    private val context: Context
) : ShimmerDeviceFactory {
    
    companion object {
        private const val TAG = "RealShimmerDeviceFactory"
    }
    
    override fun createShimmerDevice(): ShimmerDeviceInterface {
        return RealShimmerDevice(context)
    }
}

/**
 * Real Shimmer device implementation using actual shimmer libraries
 * Simplified version that avoids problematic APIs
 */
class RealShimmerDevice(
    private val context: Context
) : ShimmerDeviceInterface {
    
    companion object {
        private const val TAG = "RealShimmerDevice"
    }
    
    private var shimmer: Shimmer? = null
    private var shimmerManager: ShimmerBluetoothManagerAndroid? = null
    private var dataCallback: ((ShimmerDataCluster) -> Unit)? = null
    private var connectionCallback: ((String) -> Unit)? = null
    private var isConnected = false
    
    private var shimmerHandler: android.os.Handler? = null
    
    init {
        try {
            shimmerHandler = android.os.Handler(android.os.Looper.getMainLooper())
            shimmerManager = ShimmerBluetoothManagerAndroid(context, shimmerHandler)
            Log.i(TAG, "ShimmerBluetoothManagerAndroid initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ShimmerBluetoothManagerAndroid", e)
        }
    }
    
    override fun connect(address: String, name: String): Boolean {
        return try {
            shimmer = Shimmer(shimmerHandler, address)
            shimmer?.let { device ->
                // Connection is asynchronous - don't set isConnected immediately
                device.connect(address, name)
                Log.i(TAG, "Connection request sent to Shimmer device: $name ($address)")
                // Connection status will be updated via proper connection callbacks when available
                true
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to Shimmer device", e)
            isConnected = false
            connectionCallback?.invoke("CONNECTION_FAILED")
            false
        }
    }
    
    override fun startStreaming(): Boolean {
        return try {
            shimmer?.let { device ->
                device.startStreaming()
                Log.i(TAG, "Started streaming from Shimmer device")
                true
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start streaming", e)
            false
        }
    }
    
    override fun stopStreaming(): Boolean {
        return try {
            shimmer?.let { device ->
                device.stopStreaming()
                Log.i(TAG, "Stopped streaming from Shimmer device")
                true
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop streaming", e)
            false
        }
    }
    
    override fun disconnect(): Boolean {
        return try {
            shimmer?.let { device ->
                device.stop()
                isConnected = false
                connectionCallback?.invoke("DISCONNECTED")
                Log.i(TAG, "Disconnected from Shimmer device")
                true
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect from Shimmer device", e)
            false
        }
    }
    
    override fun isConnected(): Boolean {
        return isConnected
    }
    
    override fun setDataCallback(callback: (ShimmerDataCluster) -> Unit) {
        this.dataCallback = callback
    }
    
    override fun setConnectionCallback(callback: (String) -> Unit) {
        this.connectionCallback = callback
    }
    
    private fun handleShimmerData(objectCluster: ObjectCluster) {
        try {
            val shimmerDataCluster = RealShimmerDataCluster(objectCluster)
            dataCallback?.invoke(shimmerDataCluster)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle Shimmer data", e)
        }
    }
}

/**
 * Real Shimmer data cluster implementation using actual ObjectCluster data
 */
class RealShimmerDataCluster(
    private val objectCluster: ObjectCluster
) : ShimmerDataCluster {
    
    companion object {
        private const val TAG = "RealShimmerDataCluster"
        
        // Shimmer sensor constants
        private const val GSR_SENSOR_NAME = "GSR"
        private const val GSR_CONDUCTANCE_NAME = "GSR Conductance"
        private const val PPG_SENSOR_NAME = "PPG"
        private const val TIMESTAMP_NAME = "Timestamp"
        
        // Format constants for ObjectCluster data retrieval
        /**
         * Raw data format string used with getFormatClusterValue() to retrieve unprocessed sensor values.
         * This format provides the direct ADC readings from the sensor hardware.
         */
        private const val FORMAT_RAW = "RAW"
        
        /**
         * Calibrated data format string used with getFormatClusterValue() to retrieve processed sensor values.
         * This format provides sensor readings converted to physical units (e.g., microsiemens for GSR).
         */
        private const val FORMAT_CALIBRATED = "CAL"
    }
    
    override fun getGSRRawValue(): Double {
        return try {
            objectCluster.getFormatClusterValue(GSR_SENSOR_NAME, FORMAT_RAW) ?: 0.0
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get GSR raw value", e)
            0.0
        }
    }
    
    override fun getGSRCalibratedValue(): Double {
        return try {
            objectCluster.getFormatClusterValue(GSR_CONDUCTANCE_NAME, FORMAT_CALIBRATED) ?: 0.0
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get GSR calibrated value", e)
            0.0
        }
    }
    
    override fun getPPGValue(): Double {
        return try {
            objectCluster.getFormatClusterValue(PPG_SENSOR_NAME, FORMAT_RAW) ?: 0.0
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get PPG value", e)
            0.0
        }
    }
    
    override fun getTimestamp(): Long {
        return try {
            objectCluster.getFormatClusterValue(TIMESTAMP_NAME, FORMAT_CALIBRATED)?.toLong() ?: System.currentTimeMillis()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get timestamp", e)
            System.currentTimeMillis()
        }
    }
    
    override fun hasValidGSRData(): Boolean {
        return try {
            val gsrValue = getGSRRawValue()
            gsrValue > 0 && gsrValue < 4096 // Valid ADC range for Shimmer3 GSR
        } catch (e: Exception) {
            Log.w(TAG, "Failed to validate GSR data", e)
            false
        }
    }
}