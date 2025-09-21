package com.mpdc4gsr.ble.core.callback

interface BleConnectionCallback {
    fun onConnectionStateChanged(connected: Boolean)
    fun onServicesDiscovered()
}

interface BleCharacteristicCallback {
    fun onCharacteristicRead(data: ByteArray?)
    fun onCharacteristicWrite()
    fun onCharacteristicChanged(data: ByteArray?)
}