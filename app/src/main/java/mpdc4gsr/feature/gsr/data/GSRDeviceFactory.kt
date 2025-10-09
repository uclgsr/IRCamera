package mpdc4gsr.feature.gsr.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.lifecycle.LifecycleOwner
import com.mpdc4gsr.gsr.service.GSRDeviceDataCluster
import com.mpdc4gsr.gsr.service.GSRDeviceFactory as GSRDeviceFactoryInterface
import com.mpdc4gsr.gsr.service.GSRDeviceInterface
import com.shimmerresearch.android.Shimmer
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
import com.shimmerresearch.bluetooth.ShimmerBluetooth.BT_STATE
import com.shimmerresearch.driver.Configuration
import com.shimmerresearch.driver.ObjectCluster
import com.shimmerresearch.driver.ShimmerMsg

class GSRDeviceFactory @JvmOverloads constructor(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner? = null
) : GSRDeviceFactoryInterface {
    companion object {
        private const val TAG = "GSRDeviceFactory"
    }

    override fun createGSRDevice(): GSRDeviceInterface {
        return GSRDevice(context, lifecycleOwner)
    }
}

class GSRDevice(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner? = null
) : GSRDeviceInterface {
    companion object {
        private const val TAG = "GSRDevice"
    }

    private var shimmer: Shimmer? = null
    private var shimmerManager: ShimmerBluetoothManagerAndroid? = null
    private var dataCallback: ((GSRDeviceDataCluster) -> Unit)? = null
    private var connectionCallback: ((String) -> Unit)? = null
    private var isConnected = false
    private var shimmerHandler: Handler? = null

    init {
    }

    override fun connect(address: String, name: String): Boolean {
        return try {
            if (shimmerHandler == null) {
                shimmerHandler = object : Handler(Looper.getMainLooper()) {
                    override fun handleMessage(msg: Message) {
                        when (msg.what) {
                            ShimmerMsg.MESSAGE_STATE_CHANGE -> handleStateChange(msg)
                            ShimmerMsg.MESSAGE_READ -> handleDataPacket(msg)
                            ShimmerMsg.MESSAGE_ACK_RECEIVED -> {}
                            ShimmerMsg.MESSAGE_DEVICE_NAME -> {}
                            ShimmerMsg.MESSAGE_TOAST -> {}
                            ShimmerMsg.MESSAGE_SAMPLING_RATE_RECEIVED -> {}
                            ShimmerMsg.MESSAGE_LOG_AND_STREAM_STATUS_CHANGED -> {}
                            else -> {}
                        }
                    }
                }
                shimmerManager = ShimmerBluetoothManagerAndroid(context, shimmerHandler)
            }

            shimmer = Shimmer(shimmerHandler, context)
            shimmer?.let { device ->
                device.enableGSROnTheFlyGSRRange(Configuration.Shimmer3.SENSOR_GSR, 4)
                device.setSamplingRateShimmer(128.0)
                device.setLowPowerMag(true)
                device.setLowPowerAccel(true)
                device.setLowPowerGyro(true)

                device.connect(address, name)
                true
            } ?: false
        } catch (e: Exception) {
            isConnected = false
            connectionCallback?.invoke("CONNECTION_FAILED")
            false
        }
    }

    override fun startStreaming(): Boolean {
        return try {
            shimmer?.let { device ->
                device.startStreaming()
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    override fun stopStreaming(): Boolean {
        return try {
            shimmer?.let { device ->
                device.stopStreaming()
                true
            } ?: false
        } catch (e: Exception) {
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
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    override fun isConnected(): Boolean {
        return isConnected
    }

    override fun setDataCallback(callback: (GSRDeviceDataCluster) -> Unit) {
        this.dataCallback = callback
    }

    override fun setConnectionCallback(callback: (String) -> Unit) {
        this.connectionCallback = callback
    }

    private fun handleStateChange(msg: Message) {
        try {
            val state = msg.arg1
            when (state) {
                BT_STATE.CONNECTED.ordinal -> {
                    isConnected = true
                    connectionCallback?.invoke("CONNECTED")
                }

                BT_STATE.CONNECTING.ordinal -> {
                    connectionCallback?.invoke("CONNECTING")
                }

                BT_STATE.DISCONNECTED.ordinal -> {
                    isConnected = false
                    connectionCallback?.invoke("DISCONNECTED")
                }

                BT_STATE.STREAMING.ordinal -> {
                    connectionCallback?.invoke("STREAMING")
                }

                else -> {}
            }
        } catch (e: Exception) {
        }
    }

    private fun handleDataPacket(msg: Message) {
        try {
            val shimmerMsg = msg.obj as? ShimmerMsg
            val objectCluster = shimmerMsg?.mB as? ObjectCluster
            if (objectCluster != null) {
                handleShimmerData(objectCluster)
            }
        } catch (e: Exception) {
        }
    }

    private fun handleShimmerData(objectCluster: ObjectCluster) {
        try {
            val gsrDeviceDataCluster = GSRDeviceDataCluster(objectCluster)
            dataCallback?.invoke(gsrDeviceDataCluster)
        } catch (e: Exception) {
        }
    }

}

class GSRDeviceDataCluster(private val objectCluster: ObjectCluster) : GSRDeviceDataCluster {
    companion object {
        private const val TAG = "GSRDeviceDataCluster"
        private const val GSR_CHANNEL_NAME = "GSR"
        private const val GSR_CONDUCTANCE_NAME = "GSR Conductance"
        private const val PPG_CHANNEL_NAME = "PPG_A13"
        private const val TIMESTAMP_CHANNEL = "Timestamp"
        private const val GSR_RAW_FORMAT = "RAW"
        private const val GSR_CAL_FORMAT = "CAL"
        private const val MAX_VALID_ADC_VALUE = 4096.0
        private const val MIN_VALID_ADC_VALUE = 0.0
    }

    override fun getGSRRawValue(): Double {
        return try {
            objectCluster.getFormatClusterValue(GSR_CHANNEL_NAME, GSR_RAW_FORMAT) ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    override fun getGSRCalibratedValue(): Double {
        return try {
            objectCluster.getFormatClusterValue(GSR_CONDUCTANCE_NAME, GSR_CAL_FORMAT)
                ?: objectCluster.getFormatClusterValue(GSR_CHANNEL_NAME, GSR_CAL_FORMAT)
                ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    override fun getPPGValue(): Double {
        return try {
            objectCluster.getFormatClusterValue(PPG_CHANNEL_NAME, GSR_CAL_FORMAT) ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    override fun getTimestamp(): Long {
        return try {
            objectCluster.getFormatClusterValue(TIMESTAMP_CHANNEL, GSR_CAL_FORMAT)?.toLong()
                ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    override fun hasValidGSRData(): Boolean {
        return try {
            val gsrValue = getGSRRawValue()
            gsrValue > MIN_VALID_ADC_VALUE && gsrValue < MAX_VALID_ADC_VALUE
        } catch (e: Exception) {
            false
        }
    }
}