package com.mpdc4gsr.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import java.util.UUID

interface Connection {
    val device: Device

    val mtu: Int

    fun reconnect()

    fun disconnect()

    fun refresh()

    fun release()

    fun releaseNoEvent()

    val connectionState: ConnectionState

    val isAutoReconnectEnabled: Boolean

    val gatt: BluetoothGatt?

    fun clearRequestQueue()

    fun clearRequestQueueByType(type: RequestType?)

    val connectionConfiguration: ConnectionConfiguration

    fun getService(service: UUID?): BluetoothGattService?

    fun getCharacteristic(service: UUID?, characteristic: UUID?): BluetoothGattCharacteristic?

    fun getDescriptor(service: UUID?, characteristic: UUID?, descriptor: UUID?): BluetoothGattDescriptor?

    fun execute(request: Request?)

    fun isNotificationOrIndicationEnabled(characteristic: BluetoothGattCharacteristic?): Boolean

    fun isNotificationOrIndicationEnabled(service: UUID?, characteristic: UUID?): Boolean

    fun setBluetoothGattCallback(callback: BluetoothGattCallback?)

    fun hasProperty(service: UUID?, characteristic: UUID?, property: Int): Boolean

    companion object {
        val clientCharacteristicConfig: UUID? = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        const val REQUEST_FAIL_TYPE_REQUEST_FAILED: Int = 0
        const val REQUEST_FAIL_TYPE_CHARACTERISTIC_NOT_EXIST: Int = 1
        const val REQUEST_FAIL_TYPE_DESCRIPTOR_NOT_EXIST: Int = 2
        const val REQUEST_FAIL_TYPE_SERVICE_NOT_EXIST: Int = 3

        const val REQUEST_FAIL_TYPE_GATT_STATUS_FAILED: Int = 4
        const val REQUEST_FAIL_TYPE_GATT_IS_NULL: Int = 5
        const val REQUEST_FAIL_TYPE_BLUETOOTH_ADAPTER_DISABLED: Int = 6
        const val REQUEST_FAIL_TYPE_REQUEST_TIMEOUT: Int = 7
        const val REQUEST_FAIL_TYPE_CONNECTION_DISCONNECTED: Int = 8
        const val REQUEST_FAIL_TYPE_CONNECTION_RELEASED: Int = 9
        const val REQUEST_FAIL_TYPE_NO_PERMISSION: Int = 10

        const val TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE: Int = 0

        const val TIMEOUT_TYPE_CANNOT_CONNECT: Int = 1

        const val TIMEOUT_TYPE_CANNOT_DISCOVER_SERVICES: Int = 2


        const val CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION: Int = 1

        const val CONNECT_FAIL_TYPE_CONNECTION_IS_UNSUPPORTED: Int = 2

        const val CONNECT_FAIL_TYPE_NO_PERMISSION: Int = 3
    }
}
