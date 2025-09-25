package mpdc4gsr.sensors.gsr

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.mpdc4gsr.gsr.service.ShimmerDataCluster
import com.mpdc4gsr.gsr.service.ShimmerDeviceFactory
import com.mpdc4gsr.gsr.service.ShimmerDeviceInterface
import com.shimmerresearch.android.Shimmer
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
import com.shimmerresearch.driver.ObjectCluster
// Import removed - ShimmerMsg constants may not be available in this version
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mpdc4gsr.sensors.unified.ShimmerDeviceManager

/**
 * Real Shimmer device factory that uses the actual Shimmer libraries from app/libs
 * instead of mock implementations
 */
class RealShimmerDeviceFactory @JvmOverloads constructor(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner? = null
) : ShimmerDeviceFactory {
    
    companion object {
        private const val TAG = "RealShimmerDeviceFactory"
    }
    
    override fun createShimmerDevice(): ShimmerDeviceInterface {
        return RealShimmerDevice(context, lifecycleOwner)
    }
}

/**
 * Real Shimmer device implementation using the actual Shimmer SDK from app/libs
 * Simplified version that avoids problematic APIs
 */
class RealShimmerDevice(
    private val context: Context,  
    private val lifecycleOwner: LifecycleOwner? = null
) : ShimmerDeviceInterface {
    
    companion object {
        private const val TAG = "RealShimmerDevice"
    }
    
    private var shimmer: Shimmer? = null
    private var shimmerManager: ShimmerBluetoothManagerAndroid? = null
    private var dataCallback: ((ShimmerDataCluster) -> Unit)? = null
    private var connectionCallback: ((String) -> Unit)? = null
    private var isConnected = false
    
    private var shimmerHandler: Handler? = null
    
    init {
        try {
            shimmerHandler = Handler(Looper.getMainLooper())
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
                // Simple connection without handlers for now
                device.connect(address, name)
                isConnected = true
                connectionCallback?.invoke("CONNECTED")
                Log.i(TAG, "Connecting to Shimmer device: $name ($address)")
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
 * Real implementation of ShimmerDataCluster using ObjectCluster from Shimmer SDK
 * Based on the working unified implementation
 */
class RealShimmerDataCluster(private val objectCluster: ObjectCluster) : ShimmerDataCluster {
    
    companion object {
        private const val TAG = "RealShimmerDataCluster"
        
        // Shimmer sensor constants
        /**
         * Name of the GSR (Galvanic Skin Response) sensor channel as defined in the Shimmer SDK.
         * Used to retrieve raw GSR values from ObjectCluster.
         * See: https://shimmersensing.com/wp-content/uploads/2021/06/ConsensysPRO-User-Guide.pdf (Section: Data Structure)
         */
        private const val GSR_SENSOR_NAME = "GSR"
        /**
         * Name of the GSR Conductance channel as defined in the Shimmer SDK.
         * Used to retrieve calibrated GSR conductance values from ObjectCluster.
         * See: https://shimmersensing.com/wp-content/uploads/2021/06/ConsensysPRO-User-Guide.pdf (Section: Data Structure)
         */
        private const val GSR_CONDUCTANCE_NAME = "GSR Conductance"
        /**
         * Name of the PPG (Photoplethysmogram) sensor channel as defined in the Shimmer SDK.
         * Used to retrieve raw PPG values from ObjectCluster.
         * See: https://shimmersensing.com/wp-content/uploads/2021/06/ConsensysPRO-User-Guide.pdf (Section: Data Structure)
         */
        private const val PPG_SENSOR_NAME = "PPG"
        /**
         * Name of the timestamp channel as defined in the Shimmer SDK.
         * Used to retrieve the calibrated timestamp value from ObjectCluster.
         * See: https://shimmersensing.com/wp-content/uploads/2021/06/ConsensysPRO-User-Guide.pdf (Section: Data Structure)
         */
        private const val TIMESTAMP_NAME = "Timestamp"
    }
    
    override fun getGSRRawValue(): Double {
        return try {
            objectCluster.getFormatClusterValue(GSR_SENSOR_NAME, "RAW") ?: 0.0
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get GSR raw value", e)
            0.0
        }
    }
    
    override fun getGSRCalibratedValue(): Double {
        return try {
            objectCluster.getFormatClusterValue(GSR_CONDUCTANCE_NAME, "CAL") ?: 0.0
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get GSR calibrated value", e)
            0.0
        }
    }
    
    override fun getPPGValue(): Double {
        return try {
            objectCluster.getFormatClusterValue(PPG_SENSOR_NAME, "RAW") ?: 0.0
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get PPG value", e)
            0.0
        }
    }
    
    override fun getTimestamp(): Long {
        return try {
            objectCluster.getFormatClusterValue(TIMESTAMP_NAME, "CAL")?.toLong() ?: System.currentTimeMillis()
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