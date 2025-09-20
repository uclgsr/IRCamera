package com.mpdc4gsr.ble

interface BondController {
    fun accept(device: Device?): Boolean
}
