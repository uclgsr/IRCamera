package com.shimmerresearch.driver

import android.content.Context
import android.os.Handler


abstract class ShimmerDevice(
    protected val context: Context,
    protected val handler: Handler,
) {
    companion object {
    // Device state constants from official API
    const val STATE_NONE = 0
    const val STATE_CONNECTING = 1
    const val STATE_CONNECTED = 2
    const val STATE_STREAMING = 3

    // Message types from official API
    const val MESSAGE_STATE_CHANGE = 0
    const val MESSAGE_READ = 2
    const val MESSAGE_ACK_RECEIVED = 4
    const val MESSAGE_TOAST = 5
    const val MESSAGE_STOP_STREAMING = 7
    const val MESSAGE_INQUIRY_RESPONSE = 8

    // Configuration constants
    const val CONFIG_SETUP_BYTES_SIZE = 12
    const val MAX_INQUIRY_RETRY = 3
    const val CONNECTION_TIMEOUT_MS = 10000L

    // GSR Range constants
    const val GSR_RANGE_10KOHM_56KOHM = 0
    const val GSR_RANGE_56KOHM_220KOHM = 1
    const val GSR_RANGE_220KOHM_680KOHM = 2
    const val GSR_RANGE_680KOHM_4_7MOHM = 3
    const val GSR_RANGE_AUTO = 4
    }

    protected var dataCallback: ((ObjectCluster) -> Unit)? = null
    protected var connectionCallback: ((String) -> Unit)? = null
    protected var deviceState: Int = STATE_NONE


    open fun setShimmerDataCallback(callback: (ObjectCluster) -> Unit) {
    this.dataCallback = callback
    }


    open fun setBluetoothConnectionCallback(callback: (String) -> Unit) {
    this.connectionCallback = callback
    }


    abstract fun writeConfigurationBytes(config: ByteArray)


    abstract fun connect(
    address: String,
    name: String,
    )


    abstract fun startStreaming()


    abstract fun stopStreaming()


    abstract fun disconnect()


    open fun isConnected(): Boolean = deviceState == STATE_CONNECTED


    open fun getMacAddress(): String = ""


    open fun getDeviceName(): String = "Shimmer3_GSR"


    open fun getShimmerState(): Int = deviceState


    open fun writeEnabledSensors(sensors: Long) {
    // Default implementation - override in subclasses
    }


    open fun writeSamplingRate(rate: Double) {
    // Default implementation - override in subclasses
    }


    open fun getEnabledSensors(): Long = 0L


    open fun getSamplingRate(): Double = 128.0


    open fun setGSRRange(range: Int) {
    // Default implementation - override in subclasses
    }


    open fun getGSRRange(): Int = GSR_RANGE_AUTO


    open fun inquiry() {
    // Default implementation - override in subclasses
    sendMessage(MESSAGE_INQUIRY_RESPONSE, 0, 0, "Device inquiry completed (simulated)")
    }


    open fun readCalibrationParameters() {
    // Default implementation - override in subclasses
    }


    open fun getFirmwareVersionFullName(): String = "1.0.0"


    open fun getHardwareVersion(): String = "3.0"


    open fun getBatteryLevel(): Double = 100.0


    open fun isStreaming(): Boolean = deviceState == STATE_STREAMING


    open fun setDeviceName(name: String) {
    // Default implementation - override in subclasses
    }


    open fun readConfigurationBytes() {
    // Default implementation - override in subclasses
    }


    open fun getConfigurationBytes(): ByteArray {
    return ByteArray(CONFIG_SETUP_BYTES_SIZE)
    }


    open fun resetToDefaultConfiguration() {
    writeSamplingRate(128.0)
    writeEnabledSensors(0x10L) // GSR sensor
    setGSRRange(GSR_RANGE_AUTO)
    }


    open fun getConnectionTimeout(): Long = CONNECTION_TIMEOUT_MS


    protected fun sendMessage(
    what: Int,
    arg1: Int,
    arg2: Int,
    obj: Any?,
    ) {
    val message = handler.obtainMessage(what, arg1, arg2, obj)
    handler.sendMessage(message)
    }


    protected fun updateState(newState: Int) {
    deviceState = newState
    sendMessage(MESSAGE_STATE_CHANGE, newState, -1, null)
    }
}
