package com.mpdc4gsr.ble.core

import com.mpdc4gsr.commons.observer.Observer

interface EventObserver : Observer {
    fun onConnectionStateChange(device: Device, newState: Int, status: Int)
    fun onCharacteristicChanged(device: Device, characteristic: String, data: ByteArray)
    fun onServiceDiscovered(device: Device)
    fun onMtuChanged(device: Device, mtu: Int, status: Int)
}