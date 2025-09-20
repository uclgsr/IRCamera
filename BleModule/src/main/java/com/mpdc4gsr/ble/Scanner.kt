package com.mpdc4gsr.ble

import android.content.Context
import com.mpdc4gsr.ble.callback.ScanListener

internal interface Scanner {
    fun addScanListener(listener: ScanListener?)

    fun removeScanListener(listener: ScanListener?)

    fun startScan(context: Context?)

    fun stopScan(quietly: Boolean)

    val isScanning: Boolean

    fun onBluetoothOff()

    fun release()

    val type: ScannerType?
}
