package com.mpdc4gsr.ble.shimmer

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.mpdc4gsr.ble.core.util.BluetoothPermissionUtils
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

class ShimmerDevice(
    private val bluetoothDevice: BluetoothDevice,
    private val config: ShimmerDeviceConfig,
    private var connectionListener: UnifiedBleManager.UnifiedConnectionListener?
) : UnifiedDevice {
    private val context: Context?
    private val mainHandler: Handler
    private val isConnected = AtomicBoolean(false)
    private val isStreaming = AtomicBoolean(false)
    private var bluetoothGatt: BluetoothGatt? = null
    private var dataCharacteristic: BluetoothGattCharacteristic? = null
    private var commandCharacteristic: BluetoothGattCharacteristic? = null
    private var connectionState = UnifiedDevice.ConnectionState.DISCONNECTED
    private var lastRssi = -50

    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server for device: " + bluetoothDevice.getAddress())
                connectionState = UnifiedDevice.ConnectionState.CONNECTED
                isConnected.set(true)

                if (connectionListener != null) {
                    mainHandler.post(Runnable { connectionListener!!.onDeviceConnected(this@ShimmerDevice) })
                }


                if (bluetoothGatt != null) {
                    try {
                        bluetoothGatt!!.discoverServices()
                    } catch (e: SecurityException) {
                        Log.e(TAG, "Permission denied for service discovery", e)
                    }
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server for device: " + bluetoothDevice.getAddress())
                connectionState = UnifiedDevice.ConnectionState.DISCONNECTED
                isConnected.set(false)
                isStreaming.set(false)

                if (connectionListener != null) {
                    mainHandler.post(Runnable { connectionListener!!.onDeviceDisconnected(this@ShimmerDevice, 0) })
                }

                cleanup()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Services discovered for device: " + bluetoothDevice.getAddress())
                initializeCharacteristics()


                if (connectionListener != null) {
                    mainHandler.post(Runnable { connectionListener!!.onDeviceReady(this@ShimmerDevice) })
                }
            } else {
                Log.w(TAG, "Service discovery failed with status: " + status)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic) {
            if (SHIMMER_DATA_CHAR_UUID == characteristic.getUuid()) {
                val data = characteristic.getValue()
                if (data != null && connectionListener != null) {
                    mainHandler.post(Runnable { connectionListener!!.onDataReceived(this@ShimmerDevice, data) })
                }
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                lastRssi = rssi
            }
        }
    }

    init {
        var tmpContext: Context? = null
        try {
            if (EasyBLE.Companion.getInstance() != null) {
                tmpContext = EasyBLE.Companion.getInstance().getContext()
            }
        } catch (e: Exception) {
            tmpContext = null
        }
        this.context = tmpContext
        this.mainHandler = Handler(Looper.getMainLooper())
    }

    override fun getBluetoothDevice(): BluetoothDevice {
        return bluetoothDevice
    }

    override fun getDeviceType(): UnifiedBleManager.DeviceType {
        return config.getDeviceType()
    }

    override fun getAddress(): String {
        return bluetoothDevice.getAddress()
    }

    override fun getName(): String? {
        if (context != null) {
            return BluetoothPermissionUtils.getDeviceName(context, bluetoothDevice)
        }
        try {
            return bluetoothDevice.getName()
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied for device name access", e)
            return "Shimmer Device"
        }
    }

    override fun isConnected(): Boolean {
        return isConnected.get()
    }

    override fun connect() {
        if (isConnected.get()) {
            Log.w(TAG, "Device already connected: " + getAddress())
            return
        }

        try {
            connectionState = UnifiedDevice.ConnectionState.CONNECTING

            if (connectionListener != null) {
                mainHandler.post(Runnable { connectionListener!!.onDeviceConnected(this) })
            }


            bluetoothGatt = bluetoothDevice.connectGatt(context, false, gattCallback)

            Log.i(TAG, "Initiated GATT connection to device: " + getAddress())
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for GATT connection", e)
            connectionState = UnifiedDevice.ConnectionState.ERROR
            if (connectionListener != null) {
                mainHandler.post(Runnable { connectionListener!!.onConnectionError(this, -1, "Permission denied") })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to GATT server", e)
            connectionState = UnifiedDevice.ConnectionState.ERROR
            if (connectionListener != null) {
                mainHandler.post(Runnable { connectionListener!!.onConnectionError(this, -1, e.message) })
            }
        }
    }

    override fun disconnect() {
        try {
            connectionState = UnifiedDevice.ConnectionState.DISCONNECTING

            if (isStreaming.get()) {
                stopDataStreaming()
            }

            if (bluetoothGatt != null) {
                try {
                    bluetoothGatt!!.disconnect()
                } catch (e: SecurityException) {
                    Log.w(TAG, "Permission denied during disconnect", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
        } finally {
            cleanup()
        }
    }

    override fun startDataStreaming(): Boolean {
        if (!isConnected.get()) {
            Log.w(TAG, "Device not connected, cannot start streaming")
            return false
        }

        if (isStreaming.get()) {
            Log.w(TAG, "Already streaming data")
            return true
        }

        try {
            if (dataCharacteristic != null) {
                val notificationEnabled = enableNotifications(dataCharacteristic!!)
                if (!notificationEnabled) {
                    Log.e(TAG, "Failed to enable notifications")
                    return false
                }
            }


            if (commandCharacteristic != null) {
                val startCommand = byteArrayOf(SHIMMER_START_STREAMING)
                val commandSent = sendCommand(startCommand)
                if (commandSent) {
                    isStreaming.set(true)
                    Log.i(TAG, "Started data streaming for device: " + getAddress())
                    return true
                } else {
                    Log.e(TAG, "Failed to send start streaming command")
                    return false
                }
            }

            Log.e(TAG, "Command characteristic not available")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error starting data streaming", e)
            return false
        }
    }

    override fun stopDataStreaming(): Boolean {
        if (!isStreaming.get()) {
            Log.w(TAG, "Not currently streaming")
            return true
        }

        try {
            if (commandCharacteristic != null) {
                val stopCommand = byteArrayOf(SHIMMER_STOP_STREAMING)
                val commandSent = sendCommand(stopCommand)
                if (commandSent) {
                    isStreaming.set(false)
                    Log.i(TAG, "Stopped data streaming for device: " + getAddress())
                    return true
                } else {
                    Log.e(TAG, "Failed to send stop streaming command")
                    return false
                }
            }

            Log.e(TAG, "Command characteristic not available")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping data streaming", e)
            return false
        }
    }

    override fun sendCommand(command: ByteArray): Boolean {
        if (!isConnected.get() || commandCharacteristic == null) {
            Log.w(TAG, "Cannot send command - device not ready")
            return false
        }

        try {
            commandCharacteristic!!.setValue(command)
            return bluetoothGatt!!.writeCharacteristic(commandCharacteristic)
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for characteristic write", e)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error sending command", e)
            return false
        }
    }

    override fun getConnectionState(): UnifiedDevice.ConnectionState {
        return connectionState
    }

    override fun getRssi(): Int {
        return lastRssi
    }

    override fun getDeviceInfo(): UnifiedDevice.DeviceInfo {
        return UnifiedDevice.DeviceInfo(
            getDeviceName(),
            getAddress(),
            getDeviceType(),
            "Unknown",
            "Unknown",
            "Unknown"
        )
    }

    override fun setConnectionListener(listener: UnifiedBleManager.UnifiedConnectionListener?) {
        this.connectionListener = listener
    }


    fun setGSRRange(range: Int): Boolean {
        if (!isConnected.get() || commandCharacteristic == null) {
            Log.w(TAG, "Cannot set GSR range - device not ready")
            return false
        }

        if (range < 0 || range > 4) {
            Log.e(TAG, "Invalid GSR range: " + range + ". Must be 0-4")
            return false
        }

        try {
            val command = byteArrayOf(SHIMMER_SET_GSR_RANGE, range.toByte())
            val success = sendCommand(command)
            if (success) {
                Log.i(TAG, "Set GSR range to " + range + " for device: " + getAddress())
            }
            return success
        } catch (e: Exception) {
            Log.e(TAG, "Error setting GSR range", e)
            return false
        }
    }


    fun requestGSRRange(): Boolean {
        if (!isConnected.get() || commandCharacteristic == null) {
            Log.w(TAG, "Cannot get GSR range - device not ready")
            return false
        }

        try {
            val command = byteArrayOf(SHIMMER_GET_GSR_RANGE)
            val success = sendCommand(command)
            if (success) {
                Log.d(TAG, "Requested GSR range from device: " + getAddress())
            }
            return success
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting GSR range", e)
            return false
        }
    }


    fun setSamplingRate(samplingRate: Int): Boolean {
        if (!isConnected.get() || commandCharacteristic == null) {
            Log.w(TAG, "Cannot set sampling rate - device not ready")
            return false
        }

        try {
            val rateValue = min(samplingRate, 512).toByte()
            val command = byteArrayOf(SHIMMER_SET_SAMPLING_RATE, rateValue)
            val success = sendCommand(command)
            if (success) {
                Log.i(TAG, "Set sampling rate to " + samplingRate + "Hz for device: " + getAddress())
            }
            return success
        } catch (e: Exception) {
            Log.e(TAG, "Error setting sampling rate", e)
            return false
        }
    }


    fun requestSamplingRate(): Boolean {
        if (!isConnected.get() || commandCharacteristic == null) {
            Log.w(TAG, "Cannot get sampling rate - device not ready")
            return false
        }

        try {
            val command = byteArrayOf(SHIMMER_GET_SAMPLING_RATE)
            val success = sendCommand(command)
            if (success) {
                Log.d(TAG, "Requested sampling rate from device: " + getAddress())
            }
            return success
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting sampling rate", e)
            return false
        }
    }


    fun requestDeviceStatus(): Boolean {
        if (!isConnected.get() || commandCharacteristic == null) {
            Log.w(TAG, "Cannot get device status - device not ready")
            return false
        }

        try {
            val command = byteArrayOf(SHIMMER_GET_STATUS)
            val success = sendCommand(command)
            if (success) {
                Log.d(TAG, "Requested device status from device: " + getAddress())
            }
            return success
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting device status", e)
            return false
        }
    }


    fun requestFirmwareVersion(): Boolean {
        if (!isConnected.get() || commandCharacteristic == null) {
            Log.w(TAG, "Cannot get firmware version - device not ready")
            return false
        }

        try {
            val command = byteArrayOf(SHIMMER_GET_FW_VERSION)
            val success = sendCommand(command)
            if (success) {
                Log.d(TAG, "Requested firmware version from device: " + getAddress())
            }
            return success
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting firmware version", e)
            return false
        }
    }


    fun initializeForGSRRecording(gsrRange: Int, samplingRate: Int): Boolean {
        if (!isConnected.get()) {
            Log.w(TAG, "Cannot initialize - device not connected")
            return false
        }

        Log.i(TAG, "Initializing device for GSR recording: range=" + gsrRange + ", rate=" + samplingRate + "Hz")

        try {
            if (isStreaming.get()) {
                stopDataStreaming()
                Thread.sleep(500)
            }


            val gsrRangeSet = setGSRRange(gsrRange)
            if (!gsrRangeSet) {
                Log.w(TAG, "Failed to set GSR range, continuing with default")
            }
            Thread.sleep(200)


            val samplingRateSet = setSamplingRate(samplingRate)
            if (!samplingRateSet) {
                Log.w(TAG, "Failed to set sampling rate, continuing with default")
            }
            Thread.sleep(200)


            requestGSRRange()
            Thread.sleep(100)
            requestSamplingRate()

            Log.i(TAG, "Device initialization completed for: " + getAddress())
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error during device initialization", e)
            return false
        }
    }

    private fun initializeCharacteristics() {
        try {
            val shimmerService = bluetoothGatt!!.getService(SHIMMER_SERVICE_UUID)
            if (shimmerService == null) {
                Log.e(TAG, "Shimmer service not found")
                return
            }

            dataCharacteristic = shimmerService.getCharacteristic(SHIMMER_DATA_CHAR_UUID)
            commandCharacteristic = shimmerService.getCharacteristic(SHIMMER_CMD_CHAR_UUID)

            if (dataCharacteristic == null) {
                Log.e(TAG, "Data characteristic not found")
            }

            if (commandCharacteristic == null) {
                Log.e(TAG, "Command characteristic not found")
            }

            Log.i(TAG, "Characteristics initialized for device: " + getAddress())
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing characteristics", e)
        }
    }

    private fun enableNotifications(characteristic: BluetoothGattCharacteristic): Boolean {
        try {
            var result = bluetoothGatt!!.setCharacteristicNotification(characteristic, true)

            val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
            if (descriptor != null) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                result = result and bluetoothGatt!!.writeDescriptor(descriptor)
            }

            return result
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for enabling notifications", e)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling notifications", e)
            return false
        }
    }

    private fun cleanup() {
        try {
            if (bluetoothGatt != null) {
                if (BluetoothPermissionUtils.hasBluetoothConnectPermission(context)) {
                    bluetoothGatt!!.close()
                } else {
                    Log.w(TAG, "Missing BLUETOOTH_CONNECT permission for cleanup operation")
                }
                bluetoothGatt = null
            }

            dataCharacteristic = null
            commandCharacteristic = null
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied during cleanup", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }

    companion object {
        private const val TAG = "ShimmerDevice"


        private val SHIMMER_SERVICE_UUID: UUID = UUID.fromString("49535343-FE7D-4AE5-8FA9-9FAFD205E455")
        private val SHIMMER_DATA_CHAR_UUID: UUID = UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616")
        private val SHIMMER_CMD_CHAR_UUID: UUID = UUID.fromString("49535343-8841-43F4-A8D4-ECBE34729BB3")
        private val CLIENT_CHARACTERISTIC_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")


        private const val SHIMMER_START_STREAMING: Byte = 0x07
        private const val SHIMMER_STOP_STREAMING: Byte = 0x20
        private const val SHIMMER_GET_SAMPLING_RATE: Byte = 0x03
        private const val SHIMMER_SET_SAMPLING_RATE: Byte = 0x05
        private const val SHIMMER_GET_STATUS: Byte = 0x25
        private const val SHIMMER_GET_FW_VERSION: Byte = 0x2E
        private const val SHIMMER_SET_GSR_RANGE: Byte = 0x60
        private const val SHIMMER_GET_GSR_RANGE: Byte = 0x61
    }
}