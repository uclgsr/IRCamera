package com.mpdc4gsr.ble

interface UnifiedDevice {
    val bluetoothDevice: BluetoothDevice

    val deviceType: UnifiedBleManager.DeviceType

    val address: String

    val name: String?

    val isConnected: Boolean

    fun connect()

    fun disconnect()

    fun startDataStreaming(): Boolean

    fun stopDataStreaming(): Boolean

    fun sendCommand(command: ByteArray): Boolean

    val connectionState: ConnectionState

    val rssi: Int

    val deviceInfo: DeviceInfo

    fun setConnectionListener(listener: UnifiedBleManager.UnifiedConnectionListener?)


    val deviceId: String
        get() = this.address

    val deviceName: String
        get() {
            val name = this.name
            return if (name != null) name else "Unknown Device"
        }

    fun startRecording(timestamp: Long): Boolean {
        return startDataStreaming()
    }

    fun stopRecording(timestamp: Long): Boolean {
        return stopDataStreaming()
    }

    fun addSyncMark(timestamp: Long): Boolean {
        val syncCommand = byteArrayOf(0x00, 0x01)
        return sendCommand(syncCommand)
    }

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
        ERROR
    }

    class DeviceInfo(
        val deviceName: String?, val deviceAddress: String?,
        val deviceType: UnifiedBleManager.DeviceType?,
        val hardwareVersion: String?, val firmwareVersion: String?,
        val serialNumber: String?
    ) {
        override fun toString(): String {
            return "DeviceInfo{" +
                    "name='" + deviceName + '\'' +
                    ", address='" + deviceAddress + '\'' +
                    ", type=" + deviceType +
                    ", hw='" + hardwareVersion + '\'' +
                    ", fw='" + firmwareVersion + '\'' +
                    ", sn='" + serialNumber + '\'' +
                    '}'
        }
    }
}
