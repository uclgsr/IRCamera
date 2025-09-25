package mpdc4gsr.sensors.unified

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.shimmerresearch.android.Shimmer
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
import com.shimmerresearch.driver.ObjectCluster
import com.shimmerresearch.driver.ShimmerMsg
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
 */
class RealShimmerDevice(
    private val context: Context
) : ShimmerDeviceInterface {
    
    companion object {
        private const val TAG = "RealShimmerDevice"
        
        // Message identifiers for Shimmer communication
        // Based on typical Shimmer SDK patterns
        private const val MSG_IDENTIFIER_STATE_CHANGE = 1
        private const val MSG_IDENTIFIER_DATA_PACKET = 2
        
        // Shimmer state constants - typical Bluetooth connection states
        private const val STATE_NONE = 0
        private const val STATE_CONNECTING = 1
        private const val STATE_CONNECTED = 2
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
                MSG_IDENTIFIER_STATE_CHANGE -> {
                    handleStateChange(msg)
                }
                MSG_IDENTIFIER_DATA_PACKET -> {
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
            Log.i(TAG, "ShimmerBluetoothManagerAndroid initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ShimmerBluetoothManagerAndroid", e)
        }
    }
    
    override fun connect(address: String, name: String): Boolean {
        return try {
            Log.i(TAG, "Connecting to Shimmer device: $address")
            
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
            
            Log.i(TAG, "Starting Shimmer device streaming")
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
            
            Log.i(TAG, "Stopping Shimmer device streaming")
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
            Log.i(TAG, "Disconnecting Shimmer device")
            
            connectedDevice?.let { device ->
                if (isStreaming) {
                    device.stopStreaming()
                    isStreaming = false
                }
                device.stop()
            }
            
            deviceAddress?.let { address ->
                shimmerBluetoothManager?.disconnectShimmer(address)
            }
            
            isConnected = false
            connectedDevice = null
            deviceAddress = null
            Log.i(TAG, "Disconnected from Shimmer device")
            true
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
        Log.d(TAG, "Data callback set on Shimmer device")
    }
    
    override fun setConnectionCallback(callback: (String) -> Unit) {
        this.connectionCallback = callback
        Log.d(TAG, "Connection callback set on Shimmer device")
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
                STATE_CONNECTED -> {
                    Log.i(TAG, "Shimmer device connected: $macAddress")
                    isConnected = true
                    
                    // Get the connected device reference - note: getShimmer returns ShimmerDevice, not Shimmer
                    deviceAddress?.let { address ->
                        val shimmerDevice = shimmerBluetoothManager?.getShimmer(address)
                        // Cast or adapt as needed for compatibility
                        connectedDevice = shimmerDevice as? Shimmer
                    }
                    
                    connectionCallback?.invoke("CONNECTED")
                }
                STATE_CONNECTING -> {
                    Log.i(TAG, "Shimmer device connecting: $macAddress")
                    connectionCallback?.invoke("CONNECTING")
                }
                STATE_NONE -> {
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
        
        // Shimmer sensor constants matching the working implementation
        private const val GSR_SENSOR_NAME = "GSR"
        private const val GSR_CONDUCTANCE_NAME = "GSR Conductance"
        private const val PPG_SENSOR_NAME = "PPG A12"
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