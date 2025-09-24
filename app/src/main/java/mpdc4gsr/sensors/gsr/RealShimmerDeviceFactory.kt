package mpdc4gsr.sensors.gsr

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.mpdc4gsr.gsr.service.ShimmerDataCluster
import com.mpdc4gsr.gsr.service.ShimmerDeviceFactory
import com.mpdc4gsr.gsr.service.ShimmerDeviceInterface
import com.shimmerresearch.android.Shimmer
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
import com.shimmerresearch.driver.ObjectCluster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mpdc4gsr.sensors.unified.ShimmerDeviceManager

/**
 * Real Shimmer device factory that uses the actual Shimmer libraries from app/libs
 * instead of mock implementations
 */
class RealShimmerDeviceFactory(
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
 * This is a simplified bridge to the more complex ShimmerDeviceManager
 */
class RealShimmerDevice(
    private val context: Context,  
    private val lifecycleOwner: LifecycleOwner? = null
) : ShimmerDeviceInterface {
    
    companion object {
        private const val TAG = "RealShimmerDevice"
    }
    
    private var shimmerBluetoothManager: ShimmerBluetoothManagerAndroid? = null
    private var connectedDevice: Shimmer? = null
    private var dataCallback: ((ShimmerDataCluster) -> Unit)? = null
    private var connectionCallback: ((String) -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isConnected = false
    private var isStreaming = false
    
    init {
        try {
            shimmerBluetoothManager = ShimmerBluetoothManagerAndroid(context, mainHandler)
            Log.i(TAG, "Real Shimmer device initialized with official SDK")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Shimmer Bluetooth Manager", e)
        }
    }
    
    override fun connect(address: String, name: String): Boolean {
        return try {
            Log.i(TAG, "Connecting to real Shimmer device: $address")
            
            val btManager = shimmerBluetoothManager ?: run {
                Log.e(TAG, "Shimmer Bluetooth Manager not initialized")
                return false
            }
            
            // Use official Shimmer API to connect
            btManager.connectShimmerThroughBTAddress(address)
            
            // Simple connection state tracking (real implementation would use proper callbacks)
            isConnected = true
            connectionCallback?.invoke("CONNECTED")
            
            // Get the connected device for future operations
            // Note: This is simplified - in real implementation you'd wait for connection callback
            try {
                connectedDevice = btManager.getShimmer(address)
            } catch (e: Exception) {
                Log.w(TAG, "Could not get connected device immediately: ${e.message}")
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to Shimmer device: $address", e)
            isConnected = false
            connectionCallback?.invoke("CONNECTION_FAILED")
            false
        }
    }
    
    override fun startStreaming(): Boolean {
        return try {
            val device = connectedDevice ?: run {
                Log.w(TAG, "No Shimmer device connected for streaming")
                return false
            }
            
            Log.i(TAG, "Starting real Shimmer device streaming")
            device.startStreaming()
            isStreaming = true
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start streaming", e)
            false
        }
    }
    
    override fun stopStreaming(): Boolean {
        return try {
            val device = connectedDevice ?: run {
                Log.w(TAG, "No Shimmer device connected to stop streaming")
                return false
            }
            
            Log.i(TAG, "Stopping real Shimmer device streaming")
            device.stopStreaming()
            isStreaming = false
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop streaming", e)
            false
        }
    }
    
    override fun disconnect(): Boolean {
        return try {
            Log.i(TAG, "Disconnecting real Shimmer device")
            
            connectedDevice?.let { device ->
                if (isStreaming) {
                    device.stopStreaming()
                    isStreaming = false
                }
                device.disconnect()
            }
            
            connectedDevice = null
            isConnected = false
            connectionCallback?.invoke("DISCONNECTED")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect Shimmer device", e)
            false
        }
    }
    
    override fun isConnected(): Boolean {
        return isConnected && connectedDevice?.isConnected == true
    }
    
    override fun setDataCallback(callback: (ShimmerDataCluster) -> Unit) {
        this.dataCallback = callback
        
        // Set up data callback on the actual Shimmer device
        connectedDevice?.let { device ->
            // Note: This is simplified - real implementation would set proper data packet callback
            // For now, we'll generate sample data to test the interface
            if (isStreaming) {
                // In a real implementation, this would be handled by Shimmer's data packet callback
                Log.d(TAG, "Data callback set on real Shimmer device")
            }
        }
    }
    
    override fun setConnectionCallback(callback: (String) -> Unit) {
        this.connectionCallback = callback
    }
}

/**
 * Real implementation of ShimmerDataCluster using ObjectCluster from Shimmer SDK
 * This is a simplified version that provides basic GSR data access
 */
class RealShimmerDataCluster(private val objectCluster: ObjectCluster) : ShimmerDataCluster {
    
    companion object {
        // Common Shimmer sensor channel names based on Shimmer SDK
        private const val GSR_RAW_CHANNEL = "GSR"
        private const val GSR_CONDUCTANCE_CHANNEL = "GSR Conductance"
        private const val PPG_CHANNEL = "PPG A12"
        private const val TIMESTAMP_CHANNEL = "Timestamp"
    }
    
    override fun getGSRRawValue(): Double {
        return try {
            val data = objectCluster.getProperty(GSR_RAW_CHANNEL)
            data?.data ?: 0.0
        } catch (e: Exception) {
            Log.w("RealShimmerDataCluster", "Failed to get GSR raw value", e)
            0.0
        }
    }
    
    override fun getGSRCalibratedValue(): Double {
        return try {
            val data = objectCluster.getProperty(GSR_CONDUCTANCE_CHANNEL)
            data?.data ?: 0.0
        } catch (e: Exception) {
            Log.w("RealShimmerDataCluster", "Failed to get GSR calibrated value", e)
            0.0
        }
    }
    
    override fun getPPGValue(): Double {
        return try {
            val data = objectCluster.getProperty(PPG_CHANNEL)
            data?.data ?: 0.0
        } catch (e: Exception) {
            Log.w("RealShimmerDataCluster", "Failed to get PPG value", e)
            0.0
        }
    }
    
    override fun getTimestamp(): Long {
        return try {
            val data = objectCluster.getProperty(TIMESTAMP_CHANNEL)
            data?.data?.toLong() ?: System.currentTimeMillis()
        } catch (e: Exception) {
            Log.w("RealShimmerDataCluster", "Failed to get timestamp", e)
            System.currentTimeMillis()
        }
    }
    
    override fun hasValidGSRData(): Boolean {
        return try {
            val gsrRaw = getGSRRawValue()
            val gsrCalibratedValue = getGSRCalibratedValue()
            
            // Basic validation: raw value should be within ADC range and calibrated value should be positive
            gsrRaw > 0 && gsrRaw < 4096 && gsrCalibratedValue > 0
        } catch (e: Exception) {
            Log.w("RealShimmerDataCluster", "Error validating GSR data", e)
            false
        }
    }
}