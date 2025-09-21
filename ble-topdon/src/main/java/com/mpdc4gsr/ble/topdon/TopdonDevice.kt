package com.mpdc4gsr.ble.topdon

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.Context
import android.util.Log
import com.mpdc4gsr.ble.core.util.BluetoothPermissionUtils
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class TopdonDevice(
    private val bluetoothDevice: BluetoothDevice,
    private val config: TopdonDeviceConfig,
    private var connectionListener: UnifiedBleManager.UnifiedConnectionListener?
) : UnifiedDevice {
    private val connectionState =
        AtomicReference<UnifiedDevice.ConnectionState>(UnifiedDevice.ConnectionState.DISCONNECTED)
    private val dataStreaming = AtomicBoolean(false)

    private var bluetoothGatt: BluetoothGatt? = null
    private var dataCharacteristic: BluetoothGattCharacteristic? = null
    private var commandCharacteristic: BluetoothGattCharacteristic? = null
    private var currentRssi = 0
    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            try {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    Log.i(TAG, "Connected to Topdon device: " + getAddress())
                    connectionState.set(UnifiedDevice.ConnectionState.CONNECTED)

                    if (BluetoothPermissionUtils.hasBluetoothConnectPermission(
                            EasyBLE.Companion.getInstance().getContext()
                        )
                    ) {
                        try {
                            gatt.discoverServices()
                        } catch (e: SecurityException) {
                            Log.e(TAG, "Permission error discovering services: " + e.message)
                            notifyConnectionError(0, "Permission error: " + e.message)
                        }
                    } else {
                        Log.e(TAG, "Missing Bluetooth permissions for service discovery")
                        notifyConnectionError(0, "Missing Bluetooth permissions")
                    }
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    Log.i(TAG, "Disconnected from Topdon device: " + getAddress())
                    connectionState.set(UnifiedDevice.ConnectionState.DISCONNECTED)

                    if (connectionListener != null) {
                        connectionListener!!.onDeviceDisconnected(this@TopdonDevice, status)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in connection state change", e)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            try {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i(TAG, "Services discovered for MPDC4GSR device: " + getAddress())

                    var topdonService = gatt.getService(MPDC4GSR_SERVICE_UUID)
                    if (topdonService == null) {
                        topdonService = gatt.getService(MPDC4GSR_THERMAL_SERVICE_UUID)
                    }

                    if (topdonService != null) {
                        dataCharacteristic = topdonService.getCharacteristic(MPDC4GSR_DATA_CHAR_UUID)
                        if (dataCharacteristic == null) {
                            dataCharacteristic = topdonService.getCharacteristic(MPDC4GSR_THERMAL_DATA_CHAR_UUID)
                        }

                        commandCharacteristic = topdonService.getCharacteristic(MPDC4GSR_CMD_CHAR_UUID)

                        if (dataCharacteristic != null && commandCharacteristic != null) {
                            if (BluetoothPermissionUtils.hasBluetoothConnectPermission(
                                    EasyBLE.Companion.getInstance().getContext()
                                )
                            ) {
                                try {
                                    gatt.setCharacteristicNotification(dataCharacteristic, true)

                                    val descriptor = dataCharacteristic!!.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
                                    if (descriptor != null) {
                                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                                        gatt.writeDescriptor(descriptor)
                                    }
                                } catch (e: SecurityException) {
                                    Log.e(TAG, "Permission error enabling notifications: " + e.message)
                                    notifyConnectionError(0, "Permission error: " + e.message)
                                    return
                                }
                            } else {
                                Log.e(TAG, "Missing Bluetooth permissions for notifications")
                                notifyConnectionError(0, "Missing Bluetooth permissions")
                                return
                            }

                            Log.i(TAG, "Topdon device ready: " + getAddress())

                            if (connectionListener != null) {
                                connectionListener!!.onDeviceConnected(this@TopdonDevice)
                                connectionListener!!.onDeviceReady(this@TopdonDevice)
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "Service discovery failed for Topdon device: " + status)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in service discovery", e)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic) {
            try {
                if (characteristic.getUuid() == MPDC4GSR_DATA_CHAR_UUID ||
                    characteristic.getUuid() == MPDC4GSR_THERMAL_DATA_CHAR_UUID
                ) {
                    val data = characteristic.getValue()

                    if (data != null && data.size > 0) {
                        Log.d(TAG, "Received Topdon data: " + data.size + " bytes")

                        if (connectionListener != null) {
                            connectionListener!!.onDataReceived(this@TopdonDevice, data)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing characteristic change", e)
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                currentRssi = rssi
                Log.d(TAG, "RSSI updated: " + rssi)
            }
        }
    }
    private val deviceInfo: UnifiedDevice.DeviceInfo

    init {
        val context: Context? = EasyBLE.Companion.getInstance().getContext()
        val deviceName = BluetoothPermissionUtils.getDeviceName(context, bluetoothDevice)
        val deviceAddress = BluetoothPermissionUtils.getDeviceAddress(context, bluetoothDevice)

        this.deviceInfo = UnifiedDevice.DeviceInfo(
            if (deviceName != null && !deviceName.isEmpty()) deviceName else "Topdon Device",
            deviceAddress,
            config.getDeviceType(),
            "1.0",
            "1.0",
            deviceAddress
        )

        Log.i(TAG, "Created TopdonDevice: " + deviceAddress)
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
        val context: Context? = EasyBLE.Companion.getInstance().getContext()
        return BluetoothPermissionUtils.getDeviceName(context, bluetoothDevice)
    }

    override fun isConnected(): Boolean {
        return connectionState.get() == UnifiedDevice.ConnectionState.CONNECTED
    }

    override fun connect() {
        if (connectionState.get() == UnifiedDevice.ConnectionState.CONNECTING ||
            connectionState.get() == UnifiedDevice.ConnectionState.CONNECTED
        ) {
            Log.w(TAG, "Already connecting or connected to " + getAddress())
            return
        }

        try {
            Log.i(TAG, "Connecting to Topdon device: " + getAddress())
            connectionState.set(UnifiedDevice.ConnectionState.CONNECTING)

            if (!BluetoothPermissionUtils.hasBluetoothConnectPermission(EasyBLE.Companion.getInstance().getContext())) {
                Log.e(TAG, "Missing Bluetooth permissions for connection")
                connectionState.set(UnifiedDevice.ConnectionState.ERROR)
                notifyConnectionError(0, "Missing Bluetooth permissions")
                return
            }

            try {
                bluetoothGatt = bluetoothDevice.connectGatt(null, config.isAutoReconnectEnabled(), gattCallback)
            } catch (e: SecurityException) {
                Log.e(TAG, "Permission error connecting to device: " + e.message)
                connectionState.set(UnifiedDevice.ConnectionState.ERROR)
                notifyConnectionError(0, "Permission error: " + e.message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to Topdon device", e)
            connectionState.set(UnifiedDevice.ConnectionState.ERROR)
            notifyConnectionError(0, e.message)
        }
    }

    override fun disconnect() {
        try {
            Log.i(TAG, "Disconnecting from Topdon device: " + getAddress())

            if (dataStreaming.get()) {
                stopDataStreaming()
            }

            connectionState.set(UnifiedDevice.ConnectionState.DISCONNECTING)

            if (bluetoothGatt != null) {
                if (BluetoothPermissionUtils.hasBluetoothConnectPermission(
                        EasyBLE.Companion.getInstance().getContext()
                    )
                ) {
                    try {
                        bluetoothGatt!!.disconnect()
                        bluetoothGatt!!.close()
                    } catch (e: SecurityException) {
                        Log.e(TAG, "Permission error during disconnect: " + e.message)
                    }
                }
                bluetoothGatt = null
            }

            connectionState.set(UnifiedDevice.ConnectionState.DISCONNECTED)
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from Topdon device", e)
        }
    }

    override fun startDataStreaming(): Boolean {
        if (!isConnected()) {
            Log.w(TAG, "Device not connected")
            return false
        }

        if (dataStreaming.get()) {
            Log.w(TAG, "Data streaming already active")
            return true
        }

        try {
            Log.i(TAG, "Starting Topdon data streaming for type: " + config.getDeviceType())

            val startCommand: ByteArray?
            when (config.getDeviceType()) {
                UnifiedBleManager.DeviceType.MPDC4GSR_THERMAL -> startCommand = byteArrayOf(MPDC4GSR_START_THERMAL)
                UnifiedBleManager.DeviceType.MPDC4GSR_ENV -> startCommand = byteArrayOf(MPDC4GSR_START_ENV)
                UnifiedBleManager.DeviceType.MPDC4GSR_MULTI -> {
                    sendCommand(byteArrayOf(MPDC4GSR_START_THERMAL))
                    startCommand = byteArrayOf(MPDC4GSR_START_ENV)
                }

                else -> startCommand = byteArrayOf(MPDC4GSR_START_THERMAL)
            }

            val success = sendCommand(startCommand)

            if (success) {
                dataStreaming.set(true)
                Log.i(TAG, "Topdon data streaming started")
            }

            return success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Topdon data streaming", e)
            return false
        }
    }

    override fun stopDataStreaming(): Boolean {
        if (!dataStreaming.get()) {
            Log.w(TAG, "Data streaming not active")
            return true
        }

        try {
            Log.i(TAG, "Stopping Topdon data streaming")

            val stopCommand: ByteArray?
            when (config.getDeviceType()) {
                UnifiedBleManager.DeviceType.MPDC4GSR_THERMAL -> stopCommand = byteArrayOf(MPDC4GSR_STOP_THERMAL)
                UnifiedBleManager.DeviceType.MPDC4GSR_ENV -> stopCommand = byteArrayOf(MPDC4GSR_STOP_ENV)
                UnifiedBleManager.DeviceType.MPDC4GSR_MULTI -> {
                    sendCommand(byteArrayOf(MPDC4GSR_STOP_THERMAL))
                    stopCommand = byteArrayOf(MPDC4GSR_STOP_ENV)
                }

                else -> stopCommand = byteArrayOf(MPDC4GSR_STOP_THERMAL)
            }

            val success = sendCommand(stopCommand)

            if (success) {
                dataStreaming.set(false)
                Log.i(TAG, "Topdon data streaming stopped")
            }

            return success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop Topdon data streaming", e)
            return false
        }
    }

    override fun sendCommand(command: ByteArray): Boolean {
        if (!isConnected() || commandCharacteristic == null) {
            Log.w(TAG, "Cannot send command - device not ready")
            return false
        }

        try {
            if (!BluetoothPermissionUtils.hasBluetoothConnectPermission(EasyBLE.Companion.getInstance().getContext())) {
                Log.e(TAG, "Missing Bluetooth permissions for command write")
                return false
            }

            commandCharacteristic!!.setValue(command)
            val success: Boolean
            try {
                success = bluetoothGatt!!.writeCharacteristic(commandCharacteristic)
            } catch (e: SecurityException) {
                Log.e(TAG, "Permission error writing command: " + e.message)
                return false
            }

            Log.d(TAG, "Sent Topdon command: " + command.contentToString() + " success: " + success)
            return success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send Topdon command", e)
            return false
        }
    }

    override fun getConnectionState(): UnifiedDevice.ConnectionState {
        return connectionState.get()
    }

    override fun getRssi(): Int {
        return currentRssi
    }

    override fun getDeviceInfo(): UnifiedDevice.DeviceInfo {
        return deviceInfo
    }

    override fun setConnectionListener(listener: UnifiedBleManager.UnifiedConnectionListener?) {
        this.connectionListener = listener
    }

    private fun notifyConnectionError(errorCode: Int, message: String?) {
        if (connectionListener != null) {
            connectionListener!!.onConnectionError(this, errorCode, message)
        }
    }

    companion object {
        private const val TAG = "MPDC4GSRDevice"

        private val MPDC4GSR_SERVICE_UUID: UUID? = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        private val MPDC4GSR_DATA_CHAR_UUID: UUID? = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
        private val MPDC4GSR_CMD_CHAR_UUID: UUID? = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
        private val CLIENT_CHARACTERISTIC_CONFIG: UUID? = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        private val MPDC4GSR_THERMAL_SERVICE_UUID: UUID? = UUID.fromString("12345678-1234-5678-9012-123456789ABC")
        private val MPDC4GSR_THERMAL_DATA_CHAR_UUID: UUID? = UUID.fromString("12345678-1234-5678-9012-123456789ABD")

        private const val MPDC4GSR_START_THERMAL: Byte = 0x01
        private const val MPDC4GSR_STOP_THERMAL: Byte = 0x02
        private const val MPDC4GSR_START_ENV: Byte = 0x03
        private const val MPDC4GSR_STOP_ENV: Byte = 0x04
    }
}
