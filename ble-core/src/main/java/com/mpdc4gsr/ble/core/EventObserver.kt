package com.mpdc4gsr.ble.core

interface EventObserver {
    fun onConnectionStateChange(device: Device, newState: Int, status: Int)
    fun onCharacteristicChanged(device: Device, characteristic: String, data: ByteArray)
    fun onServiceDiscovered(device: Device)
    fun onMtuChanged(device: Device, mtu: Int, status: Int)
}