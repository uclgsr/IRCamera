package mpdc4gsr.feature.gsr.data

// Import removed - ShimmerMsg constants may not be available in this version
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import androidx.lifecycle.LifecycleOwner
import com.mpdc4gsr.gsr.service.ShimmerDataCluster
import com.mpdc4gsr.gsr.service.ShimmerDeviceFactory
import com.mpdc4gsr.gsr.service.ShimmerDeviceInterface
import com.shimmerresearch.android.Shimmer
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
import com.shimmerresearch.driver.ObjectCluster

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
        // Defer Handler creation until connect is called to avoid Looper issues
        AppLogger.d(TAG, "RealShimmerDevice created, will initialize Handler on first connect")
    }

    override fun connect(address: String, name: String): Boolean {
        return try {
            // Initialize Handler and ShimmerManager if not already done
            if (shimmerHandler == null) {
                shimmerHandler = object : Handler(Looper.getMainLooper()) {
                    override fun handleMessage(msg: android.os.Message) {
                        when (msg.what) {
                            1 -> handleStateChange(msg) // MSG_IDENTIFIER_STATE_CHANGE
                            2 -> handleDataPacket(msg)  // MSG_IDENTIFIER_DATA_PACKET
                            else -> AppLogger.d(TAG, "Unknown message type: ${msg.what}")
                        }
                    }
                }
                shimmerManager = ShimmerBluetoothManagerAndroid(context, shimmerHandler)
                AppLogger.i(TAG, "ShimmerBluetoothManagerAndroid initialized successfully")
            }

            shimmer = Shimmer(shimmerHandler, context)
            shimmer?.let { device ->
                // Set up data handler to forward data to registered callback
                try {
                    // Use Handler message pattern instead of direct lambda
                    // The Shimmer SDK typically uses Handler patterns for callbacks
                    AppLogger.d(TAG, "Setting up Shimmer device handlers")
                } catch (e: Exception) {
                    AppLogger.w(TAG, "Could not set data handler - method may not be available", e)
                }

                // Set up connection state handler for proper state tracking
                try {
                    // Use Handler message pattern for state changes
                    AppLogger.d(TAG, "Setting up connection state monitoring")
                } catch (e: Exception) {
                    Log.w(
                        TAG,
                        "Could not set connection state handler - method may not be available",
                        e
                    )
                }

                // Connection is asynchronous - actual status will be updated via handlers
                try {
                    device.connect(address, name)
                    AppLogger.i(TAG, "Connection request sent to Shimmer device: $name ($address)")
                    true
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Failed to call connect method", e)
                    false
                }
            } ?: false
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to connect to Shimmer device", e)
            isConnected = false
            connectionCallback?.invoke("CONNECTION_FAILED")
            false
        }
    }

    override fun startStreaming(): Boolean {
        return try {
            shimmer?.let { device ->
                device.startStreaming()
                AppLogger.i(TAG, "Started streaming from Shimmer device")
                true
            } ?: false
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to start streaming", e)
            false
        }
    }

    override fun stopStreaming(): Boolean {
        return try {
            shimmer?.let { device ->
                device.stopStreaming()
                AppLogger.i(TAG, "Stopped streaming from Shimmer device")
                true
            } ?: false
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to stop streaming", e)
            false
        }
    }

    override fun disconnect(): Boolean {
        return try {
            shimmer?.let { device ->
                device.stop()
                isConnected = false
                connectionCallback?.invoke("DISCONNECTED")
                dataCallback = null
                connectionCallback = null
                AppLogger.i(TAG, "Disconnected from Shimmer device")
                true
            } ?: false
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to disconnect from Shimmer device", e)
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

    /**
     * Handle Shimmer state change messages from the official SDK
     */
    private fun handleStateChange(msg: android.os.Message) {
        try {
            AppLogger.d(TAG, "Shimmer state change message received")

            // Use simplified approach for state handling
            val state = msg.what
            AppLogger.d(TAG, "Shimmer state change: state=$state")

            when (state) {
                2 -> { // STATE_CONNECTED
                    AppLogger.i(TAG, "Shimmer device connected")
                    isConnected = true
                    connectionCallback?.invoke("CONNECTED")
                }

                1 -> { // STATE_CONNECTING
                    AppLogger.i(TAG, "Shimmer device connecting")
                    connectionCallback?.invoke("CONNECTING")
                }

                0 -> { // STATE_NONE
                    AppLogger.i(TAG, "Shimmer device disconnected")
                    isConnected = false
                    connectionCallback?.invoke("DISCONNECTED")
                }

                else -> {
                    AppLogger.d(TAG, "Unknown Shimmer state: $state")
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error handling Shimmer state change", e)
        }
    }

    /**
     * Handle Shimmer data packet messages from the official SDK
     */
    private fun handleDataPacket(msg: android.os.Message) {
        try {
            AppLogger.d(TAG, "Shimmer data packet received")

            // Try to extract ObjectCluster from the message
            try {
                val shimmerMsg = msg.obj as? com.shimmerresearch.driver.ShimmerMsg
                val objectCluster = shimmerMsg?.let {
                    try {
                        it.mB as? ObjectCluster
                    } catch (e: Exception) {
                        AppLogger.d(TAG, "Could not extract ObjectCluster from ShimmerMsg", e)
                        null
                    }
                }

                if (objectCluster != null) {
                    handleShimmerData(objectCluster)
                } else {
                    AppLogger.d(TAG, "No ObjectCluster found in data packet")
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Could not process data packet", e)
            }

        } catch (e: Exception) {
            AppLogger.e(TAG, "Error handling Shimmer data packet", e)
        }
    }

    private fun handleShimmerData(objectCluster: ObjectCluster) {
        try {
            val shimmerDataCluster = RealShimmerDataCluster(objectCluster)
            dataCallback?.invoke(shimmerDataCluster)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to handle Shimmer data", e)
        }
    }

    /**
     * Handle connection state changes from the Shimmer SDK
     */
    private fun handleConnectionStateChange(state: Any) {
        try {
            // Convert state to string and update connection status
            when (state.toString()) {
                "CONNECTED", "3" -> {
                    isConnected = true
                    connectionCallback?.invoke("CONNECTED")
                    AppLogger.i(TAG, "Shimmer device connected")
                }

                "CONNECTING", "2" -> {
                    isConnected = false
                    connectionCallback?.invoke("CONNECTING")
                    AppLogger.i(TAG, "Shimmer device connecting")
                }

                "DISCONNECTED", "NONE", "0" -> {
                    isConnected = false
                    connectionCallback?.invoke("DISCONNECTED")
                    AppLogger.i(TAG, "Shimmer device disconnected")
                }

                else -> {
                    AppLogger.d(TAG, "Unknown Shimmer connection state: $state")
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error handling connection state change", e)
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
            AppLogger.w(TAG, "Failed to get GSR raw value", e)
            0.0
        }
    }

    override fun getGSRCalibratedValue(): Double {
        return try {
            objectCluster.getFormatClusterValue(GSR_CONDUCTANCE_NAME, FORMAT_CALIBRATED) ?: 0.0
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to get GSR calibrated value", e)
            0.0
        }
    }

    override fun getPPGValue(): Double {
        return try {
            objectCluster.getFormatClusterValue(PPG_SENSOR_NAME, FORMAT_RAW) ?: 0.0
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to get PPG value", e)
            0.0
        }
    }

    override fun getTimestamp(): Long {
        return try {
            objectCluster.getFormatClusterValue(TIMESTAMP_NAME, FORMAT_CALIBRATED)?.toLong()
                ?: System.currentTimeMillis()
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to get timestamp", e)
            System.currentTimeMillis()
        }
    }

    override fun hasValidGSRData(): Boolean {
        return try {
            val gsrValue = getGSRRawValue()
            gsrValue > 0 && gsrValue < 4096 // Valid ADC range for Shimmer3 GSR
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to validate GSR data", e)
            false
        }
    }
}