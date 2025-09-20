package com.mpdc4gsr.ble

import android.bluetooth.BluetoothDevice

interface RemoveBondFilter {
    fun accept(device: BluetoothDevice?): Boolean
}
