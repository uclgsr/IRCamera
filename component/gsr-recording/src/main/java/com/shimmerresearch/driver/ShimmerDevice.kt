package com.shimmerresearch.driver

import android.content.Context
import android.os.Handler

/**
 * Official Shimmer API compatible ShimmerDevice base class
 * This abstract class defines the interface for all Shimmer devices
 *
 * Based on the official Shimmer Android API structure from:
 * https://github.com/ShimmerEngineering/ShimmerAndroidAPI
 *
 * Compatible with shimmerdriver v0.11.4_beta
 */
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

    /**
     * Set callback for receiving Shimmer data - Official API method
     */
    open fun setShimmerDataCallback(callback: (ObjectCluster) -> Unit) {
        this.dataCallback = callback
    }

    /**
     * Set callback for Bluetooth connection state changes - Official API method
     */
    open fun setBluetoothConnectionCallback(callback: (String) -> Unit) {
        this.connectionCallback = callback
    }

    /**
     * Write configuration bytes to Shimmer device - Official API method
     */
    abstract fun writeConfigurationBytes(config: ByteArray)

    /**
     * Connect to Shimmer device - Official API method
     */
    abstract fun connect(
        address: String,
        name: String,
    )

    /**
     * Start data streaming from Shimmer device - Official API method
     */
    abstract fun startStreaming()

    /**
     * Stop data streaming from Shimmer device - Official API method
     */
    abstract fun stopStreaming()

    /**
     * Disconnect from Shimmer device - Official API method
     */
    abstract fun disconnect()

    /**
     * Check if device is connected - Official API method
     */
    open fun isConnected(): Boolean = deviceState == STATE_CONNECTED

    /**
     * Get device MAC address - Official API method
     */
    open fun getMacAddress(): String = ""

    /**
     * Get device name - Official API method
     */
    open fun getDeviceName(): String = "Shimmer3_GSR"

    /**
     * Get current device state - Official API method
     */
    open fun getShimmerState(): Int = deviceState

    /**
     * Enable/disable sensor - Official API method
     */
    open fun writeEnabledSensors(sensors: Long) {
        // Default implementation - override in subclasses
    }

    /**
     * Set sampling rate - Official API method
     */
    open fun writeSamplingRate(rate: Double) {
        // Default implementation - override in subclasses
    }

    /**
     * Get enabled sensors - Official API method
     */
    open fun getEnabledSensors(): Long = 0L

    /**
     * Get sampling rate - Official API method
     */
    open fun getSamplingRate(): Double = 128.0

    /**
     * Set GSR range - Official API method
     */
    open fun setGSRRange(range: Int) {
        // Default implementation - override in subclasses
    }

    /**
     * Get GSR range - Official API method
     */
    open fun getGSRRange(): Int = GSR_RANGE_AUTO

    /**
     * Inquiry device information - Official API method
     */
    open fun inquiry() {
        // Default implementation - override in subclasses
        sendMessage(MESSAGE_INQUIRY_RESPONSE, 0, 0, "Device inquiry completed (simulated)")
    }

    /**
     * Read calibration parameters - Official API method
     */
    open fun readCalibrationParameters() {
        // Default implementation - override in subclasses
    }

    /**
     * Get device firmware version - Official API method
     */
    open fun getFirmwareVersionFullName(): String = "1.0.0"

    /**
     * Get device hardware version - Official API method
     */
    open fun getHardwareVersion(): String = "3.0"

    /**
     * Get battery level - Official API method
     */
    open fun getBatteryLevel(): Double = 100.0

    /**
     * Check if streaming - Official API method
     */
    open fun isStreaming(): Boolean = deviceState == STATE_STREAMING

    /**
     * Set device name - Official API method
     */
    open fun setDeviceName(name: String) {
        // Default implementation - override in subclasses
    }

    /**
     * Read configuration from device - Official API method
     */
    open fun readConfigurationBytes() {
        // Default implementation - override in subclasses
    }

    /**
     * Get configuration bytes - Official API method
     */
    open fun getConfigurationBytes(): ByteArray {
        return ByteArray(CONFIG_SETUP_BYTES_SIZE)
    }

    /**
     * Reset device to defaults - Official API method
     */
    open fun resetToDefaultConfiguration() {
        writeSamplingRate(128.0)
        writeEnabledSensors(0x10L) // GSR sensor
        setGSRRange(GSR_RANGE_AUTO)
    }

    /**
     * Get connection timeout - Official API method
     */
    open fun getConnectionTimeout(): Long = CONNECTION_TIMEOUT_MS

    /**
     * Send message through handler - matches official API pattern
     */
    protected fun sendMessage(
        what: Int,
        arg1: Int,
        arg2: Int,
        obj: Any?,
    ) {
        val message = handler.obtainMessage(what, arg1, arg2, obj)
        handler.sendMessage(message)
    }

    /**
     * Update device state and notify
     */
    protected fun updateState(newState: Int) {
        deviceState = newState
        sendMessage(MESSAGE_STATE_CHANGE, newState, -1, null)
    }
}
