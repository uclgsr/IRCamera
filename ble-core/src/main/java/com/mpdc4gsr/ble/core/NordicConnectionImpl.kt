package com.mpdc4gsr.ble.core

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import java.util.UUID

/**
 * Nordic BLE connection implementation
 */
class NordicConnectionImpl(
    override val device: Device,
    private val easyBLE: EasyBLE? = null,
    private val bluetoothAdapter: android.bluetooth.BluetoothAdapter? = null,
    private val configuration: Any? = null,
    private val connectDelay: Int = 0,
    private val observer: Any? = null
) : Connection {
    override val mtu: Int = 23
    override val connectionState: ConnectionState = ConnectionState.DISCONNECTED
    override val isAutoReconnectEnabled: Boolean = false
    override val gatt: BluetoothGatt? = null
    override val connectionConfiguration: ConnectionConfiguration = ConnectionConfiguration()

    override fun reconnect() {
    }

    override fun disconnect() {
    }

    override fun refresh() {
    }

    override fun release() {
    }

    override fun releaseNoEvent() {
    }

    override fun clearRequestQueue() {
    }

    override fun clearRequestQueueByType(type: RequestType?) {
    }

    override fun getService(service: UUID?): BluetoothGattService? {
        return null
    }

    override fun getCharacteristic(service: UUID?, characteristic: UUID?): BluetoothGattCharacteristic? {
        return null
    }

    override fun getDescriptor(service: UUID?, characteristic: UUID?, descriptor: UUID?): BluetoothGattDescriptor? {
        return null
    }

    override fun execute(request: Request?) {
    }

    override fun isNotificationOrIndicationEnabled(characteristic: BluetoothGattCharacteristic?): Boolean {
        return false
    }

    override fun isNotificationOrIndicationEnabled(service: UUID?, characteristic: UUID?): Boolean {
        return false
    }

    override fun setBluetoothGattCallback(callback: BluetoothGattCallback?) {
    }

    override fun hasProperty(service: UUID?, characteristic: UUID?, property: Int): Boolean {
        return false
    }

    /**
     * Get connection state
     */
    fun getConnectionState(): ConnectionState {
        return connectionState
    }
}