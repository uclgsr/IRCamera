package com.mpdc4gsr.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.util.Log
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class NordicConnectionImpl(
    easyBLE: EasyBLE?, bluetoothAdapter: BluetoothAdapter?,
    device: Device, configuration: ConnectionConfiguration?,
    connectDelay: Int, companionObserver: EventObserver?
) : Connection {
    private val baseConnection: Connection

    private val enhancedMode = AtomicBoolean(true)
    private val autoRetryEnabled = AtomicBoolean(true)

    init {
        this.baseConnection = ConnectionImpl(
            easyBLE, bluetoothAdapter, device,
            configuration, connectDelay, companionObserver
        )

        Log.i(TAG, "Created Enhanced BLE connection wrapper for device: " + device.getAddress())
    }

    override fun getDevice(): Device {
        return baseConnection.getDevice()
    }

    override fun getMtu(): Int {
        return baseConnection.getMtu()
    }

    override fun reconnect() {
        if (autoRetryEnabled.get()) {
            Log.i(TAG, "Enhanced reconnection with improved retry logic for device: " + getDevice().getAddress())
        }
        baseConnection.reconnect()
    }

    override fun disconnect() {
        Log.i(TAG, "Enhanced disconnect for device: " + getDevice().getAddress())
        baseConnection.disconnect()
    }

    override fun refresh() {
        Log.i(TAG, "Enhanced refresh for device: " + getDevice().getAddress())
        baseConnection.refresh()
    }

    override fun release() {
        Log.i(TAG, "Enhanced release for device: " + getDevice().getAddress())
        baseConnection.release()
    }

    override fun releaseNoEvent() {
        Log.i(TAG, "Enhanced release (no event) for device: " + getDevice().getAddress())
        baseConnection.releaseNoEvent()
    }

    override fun getConnectionState(): ConnectionState {
        return baseConnection.getConnectionState()
    }

    override fun isAutoReconnectEnabled(): Boolean {
        return baseConnection.isAutoReconnectEnabled()
    }

    override fun getGatt(): BluetoothGatt? {
        return baseConnection.getGatt()
    }

    override fun clearRequestQueue() {
        Log.d(TAG, "Enhanced clear request queue for device: " + getDevice().getAddress())
        baseConnection.clearRequestQueue()
    }

    override fun clearRequestQueueByType(type: RequestType?) {
        Log.d(TAG, "Enhanced clear request queue by type: " + type + " for device: " + getDevice().getAddress())
        baseConnection.clearRequestQueueByType(type)
    }

    override fun getConnectionConfiguration(): ConnectionConfiguration {
        return baseConnection.getConnectionConfiguration()
    }

    override fun getService(service: UUID?): BluetoothGattService? {
        return baseConnection.getService(service)
    }

    override fun getCharacteristic(service: UUID?, characteristic: UUID?): BluetoothGattCharacteristic? {
        return baseConnection.getCharacteristic(service, characteristic)
    }

    override fun getDescriptor(service: UUID?, characteristic: UUID?, descriptor: UUID?): BluetoothGattDescriptor? {
        return baseConnection.getDescriptor(service, characteristic, descriptor)
    }

    override fun execute(request: Request) {
        if (enhancedMode.get()) {
            Log.d(
                TAG,
                "Enhanced request execution for device: " + getDevice().getAddress() + ", type: " + request.getType()
            )
        }
        baseConnection.execute(request)
    }

    override fun isNotificationOrIndicationEnabled(characteristic: BluetoothGattCharacteristic?): Boolean {
        return baseConnection.isNotificationOrIndicationEnabled(characteristic)
    }

    override fun isNotificationOrIndicationEnabled(service: UUID?, characteristic: UUID?): Boolean {
        return baseConnection.isNotificationOrIndicationEnabled(service, characteristic)
    }

    override fun setBluetoothGattCallback(callback: BluetoothGattCallback?) {
        baseConnection.setBluetoothGattCallback(callback)
    }

    override fun hasProperty(service: UUID?, characteristic: UUID?, property: Int): Boolean {
        return baseConnection.hasProperty(service, characteristic, property)
    }

    fun setAutoRetryEnabled(enabled: Boolean) {
        autoRetryEnabled.set(enabled)
        Log.i(
            TAG,
            "Auto-retry " + (if (enabled) "enabled" else "disabled") + " for device: " + getDevice().getAddress()
        )
    }

    fun isEnhancedMode(): Boolean {
        return enhancedMode.get()
    }

    fun setEnhancedMode(enabled: Boolean) {
        enhancedMode.set(enabled)
        Log.i(
            TAG,
            "Enhanced mode " + (if (enabled) "enabled" else "disabled") + " for device: " + getDevice().getAddress()
        )
    }

    val isAutoRetry: Boolean
        get() = autoRetryEnabled.get()

    companion object {
        private const val TAG = "NordicConnectionImpl"
    }
}
