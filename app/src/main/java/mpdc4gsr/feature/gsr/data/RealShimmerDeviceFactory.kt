package mpdc4gsr.feature.gsr.data
// Import removed - ShimmerMsg constants may not be available in this version
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LifecycleOwner
import com.mpdc4gsr.gsr.service.ShimmerDataCluster
import com.mpdc4gsr.gsr.service.ShimmerDeviceFactory
import com.mpdc4gsr.gsr.service.ShimmerDeviceInterface
import com.shimmerresearch.android.Shimmer
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
import com.shimmerresearch.driver.ObjectCluster

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
        // Defer Handler creation until connect is called to avoid Looper issues    }

    override fun connect(address: String, name: String): Boolean {
        return try {
            // Initialize Handler and ShimmerManager if not already done
            if (shimmerHandler == null) {
                shimmerHandler = object : Handler(Looper.getMainLooper()) {
                    override fun handleMessage(msg: android.os.Message) {
                        when (msg.what) {
                            0 -> handleStateChange(msg)
                            2 -> handleDataPacket(msg)
                            4 ->                            5 ->                            9 ->                            11 ->                            999 ->                            else ->                        }
                    }
                }
                shimmerManager = ShimmerBluetoothManagerAndroid(context, shimmerHandler)            }
            shimmer = Shimmer(shimmerHandler, context)
            shimmer?.let { device ->
                // Set up data handler to forward data to registered callback
                try {
                    // Use Handler message pattern instead of direct lambda
                    // The Shimmer SDK typically uses Handler patterns for callbacks                } catch (e: Exception) {                }
                // Set up connection state handler for proper state tracking
                try {
                    // Use Handler message pattern for state changes                } catch (e: Exception) {                }
                // Connection is asynchronous - actual status will be updated via handlers
                try {
                    device.connect(address, name)
                } catch (e: Exception) {                    false
                }
            } ?: false
        } catch (e: Exception) {            isConnected = false
            connectionCallback?.invoke("CONNECTION_FAILED")
            false
        }
    }

    override fun startStreaming(): Boolean {
        return try {
            shimmer?.let { device ->
                device.startStreaming()                true
            } ?: false
        } catch (e: Exception) {            false
        }
    }

    override fun stopStreaming(): Boolean {
        return try {
            shimmer?.let { device ->
                device.stopStreaming()                true
            } ?: false
        } catch (e: Exception) {            false
        }
    }

    override fun disconnect(): Boolean {
        return try {
            shimmer?.let { device ->
                device.stop()
                isConnected = false
                connectionCallback?.invoke("DISCONNECTED")
                dataCallback = null
                connectionCallback = null                true
            } ?: false
        } catch (e: Exception) {            false
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

    private fun handleStateChange(msg: android.os.Message) {
        try {
            val state = msg.arg1            when (state) {
                2 -> {                    isConnected = true
                    connectionCallback?.invoke("CONNECTED")
                }

                1 -> {                    connectionCallback?.invoke("CONNECTING")
                }

                0 -> {                    isConnected = false
                    connectionCallback?.invoke("DISCONNECTED")
                }

                3 -> {                    connectionCallback?.invoke("STREAMING")
                }

                else -> {                }
            }
        } catch (e: Exception) {        }
    }

    private fun handleDataPacket(msg: android.os.Message) {
        try {            // Try to extract ObjectCluster from the message
            try {
                val shimmerMsg = msg.obj as? com.shimmerresearch.driver.ShimmerMsg
                val objectCluster = shimmerMsg?.let {
                    try {
                        it.mB as? ObjectCluster
                    } catch (e: Exception) {                        null
                    }
                }
                if (objectCluster != null) {
                    handleShimmerData(objectCluster)
                } else {                }
            } catch (e: Exception) {            }
        } catch (e: Exception) {        }
    }

    private fun handleShimmerData(objectCluster: ObjectCluster) {
        try {
            val shimmerDataCluster = RealShimmerDataCluster(objectCluster)
            dataCallback?.invoke(shimmerDataCluster)
        } catch (e: Exception) {        }
    }

    private fun handleConnectionStateChange(state: Any) {
        try {
            // Convert state to string and update connection status
            when (state.toString()) {
                "CONNECTED", "3" -> {
                    isConnected = true
                    connectionCallback?.invoke("CONNECTED")                }

                "CONNECTING", "2" -> {
                    isConnected = false
                    connectionCallback?.invoke("CONNECTING")                }

                "DISCONNECTED", "NONE", "0" -> {
                    isConnected = false
                    connectionCallback?.invoke("DISCONNECTED")                }

                else -> {                }
            }
        } catch (e: Exception) {        }
    }
}

class RealShimmerDataCluster(private val objectCluster: ObjectCluster) : ShimmerDataCluster {
    companion object {
        private const val TAG = "RealShimmerDataCluster"
        // Shimmer sensor constants
    }

    override fun getGSRRawValue(): Double {
        return try {
            objectCluster.getFormatClusterValue("GSR", "RAW") ?: 0.0
        } catch (e: Exception) {            0.0
        }
    }

    override fun getGSRCalibratedValue(): Double {
        return try {
            objectCluster.getFormatClusterValue("GSR Conductance", "CAL") ?: 0.0
        } catch (e: Exception) {            0.0
        }
    }

    override fun getPPGValue(): Double {
        return try {
            objectCluster.getFormatClusterValue("PPG_A13", "CAL") ?: 0.0
        } catch (e: Exception) {            0.0
        }
    }

    override fun getTimestamp(): Long {
        return try {
            objectCluster.getFormatClusterValue("Timestamp", "CAL")?.toLong() ?: System.currentTimeMillis()
        } catch (e: Exception) {            System.currentTimeMillis()
        }
    }

    override fun hasValidGSRData(): Boolean {
        return try {
            val gsrValue = getGSRRawValue()
            gsrValue > 0 && gsrValue < 4096 // Valid ADC range for Shimmer3 GSR
        } catch (e: Exception) {            false
        }
    }
}