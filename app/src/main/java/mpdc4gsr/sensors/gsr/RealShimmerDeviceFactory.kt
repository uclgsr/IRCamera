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
import com.shimmerresearch.driver.ShimmerMsg
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
    private var isConnected = false
    private var isStreaming = false
    private var deviceAddress: String? = null
    
    // Handler to process Shimmer messages from the SDK
    private val shimmerMessageHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                ShimmerMsg.MSG_IDENTIFIER_STATE_CHANGE -> {
                    handleStateChange(msg)
                }
                ShimmerMsg.MSG_IDENTIFIER_DATA_PACKET -> {
                    handleDataPacket(msg)
                }
                else -> {
                    Log.d(TAG, "Received unknown Shimmer message: ${msg.what}")
                }
            }
        }
    }
    
    init {
        try {
            shimmerBluetoothManager = ShimmerBluetoothManagerAndroid(context, shimmerMessageHandler)
            Log.i(TAG, "Real Shimmer device initialized with official SDK and message handler")
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
            
            deviceAddress = address
            
            // Use official Shimmer API to connect
            btManager.connectShimmerThroughBTAddress(address)
            
            // Connection state will be updated via message handler
            Log.i(TAG, "Connection request sent to Shimmer device: $address")
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
            deviceAddress = null
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
        Log.d(TAG, "Data callback set on real Shimmer device")
    }
    
    override fun setConnectionCallback(callback: (String) -> Unit) {
        this.connectionCallback = callback
        Log.d(TAG, "Connection callback set on real Shimmer device")
    }
    
    /**
     * Handle Shimmer state change messages from the official SDK
     */
    private fun handleStateChange(msg: Message) {
        try {
            val shimmerMsg = msg.obj as? ShimmerMsg ?: return
            val state = shimmerMsg.mB as? Int ?: return
            val macAddress = shimmerMsg.mA as? String
            
            Log.d(TAG, "Shimmer state change: state=$state, address=$macAddress")
            
            when (state) {
                Shimmer.STATE_CONNECTED -> {
                    Log.i(TAG, "Shimmer device connected: $macAddress")
                    isConnected = true
                    
                    // Get the connected device reference
                    deviceAddress?.let { address ->
                        connectedDevice = shimmerBluetoothManager?.getShimmer(address)
                    }
                    
                    connectionCallback?.invoke("CONNECTED")
                }
                Shimmer.STATE_CONNECTING -> {
                    Log.i(TAG, "Shimmer device connecting: $macAddress")
                    connectionCallback?.invoke("CONNECTING")
                }
                Shimmer.STATE_NONE -> {
                    Log.i(TAG, "Shimmer device disconnected: $macAddress")
                    isConnected = false
                    isStreaming = false
                    connectedDevice = null
                    connectionCallback?.invoke("DISCONNECTED")
                }
                else -> {
                    Log.d(TAG, "Unknown Shimmer state: $state for device: $macAddress")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling Shimmer state change", e)
        }
    }
    
    /**
     * Handle Shimmer data packet messages from the official SDK
     */
    private fun handleDataPacket(msg: Message) {
        try {
            val shimmerMsg = msg.obj as? ShimmerMsg ?: return
            val objectCluster = shimmerMsg.mB as? ObjectCluster ?: return
            
            // Forward data to callback if set
            dataCallback?.let { callback ->
                val dataCluster = RealShimmerDataCluster(objectCluster)
                callback(dataCluster)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling Shimmer data packet", e)
        }
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