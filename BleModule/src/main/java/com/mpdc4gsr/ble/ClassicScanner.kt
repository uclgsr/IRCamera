package com.mpdc4gsr.ble

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.util.Log
import com.mpdc4gsr.ble.callback.ScanListener
import com.mpdc4gsr.ble.util.BluetoothPermissionUtils

internal class ClassicScanner(easyBle: EasyBLE, bluetoothAdapter: BluetoothAdapter?) :
    AbstractScanner(easyBle, bluetoothAdapter) {
    private var stopQuietly = false

    override fun isReady(): Boolean {
        return true
    }

    override fun performStartScan() {
        val context: Context? = EasyBLE.Companion.getInstance().getContext()
        if (!BluetoothPermissionUtils.hasBluetoothScanPermission(context)) {
            Log.w(TAG, "Missing BLUETOOTH_SCAN permission for startDiscovery()")
            handleScanCallback(
                false, null, false, ScanListener.Companion.ERROR_LACK_BLUETOOTH_PERMISSION,
                "Missing Bluetooth scan permission"
            )
            return
        }

        try {
            bluetoothAdapter.startDiscovery()
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException in startDiscovery(): " + e.message)
            handleScanCallback(
                false, null, false, ScanListener.Companion.ERROR_LACK_BLUETOOTH_PERMISSION,
                "Bluetooth permission denied: " + e.message
            )
        }
    }

    override fun performStopScan() {
        val context: Context? = EasyBLE.Companion.getInstance().getContext()
        if (!BluetoothPermissionUtils.hasBluetoothScanPermission(context)) {
            Log.w(TAG, "Missing BLUETOOTH_SCAN permission for cancelDiscovery()")
            return
        }

        try {
            bluetoothAdapter.cancelDiscovery()
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException in cancelDiscovery(): " + e.message)
        }
    }

    override fun setScanning(scanning: Boolean) {
        super.setScanning(scanning)
        if (scanning) {
            handleScanCallback(true, null, false, -1, "")
        } else if (!stopQuietly) {
            handleScanCallback(false, null, false, -1, "")
        } else {
            stopQuietly = false
        }
    }

    override fun stopScan(quietly: Boolean) {
        if (isScanning()) {
            stopQuietly = quietly
        }
        super.stopScan(quietly)
    }

    override fun getType(): ScannerType {
        return ScannerType.CLASSIC
    }

    companion object {
        private const val TAG = "ClassicScanner"
    }
}
