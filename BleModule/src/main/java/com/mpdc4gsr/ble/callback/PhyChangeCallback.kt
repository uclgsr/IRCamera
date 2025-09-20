package com.mpdc4gsr.ble.callback

import com.mpdc4gsr.ble.Request

interface PhyChangeCallback : RequestFailedCallback {
    fun onPhyChange(request: Request?, txPhy: Int, rxPhy: Int)
}
