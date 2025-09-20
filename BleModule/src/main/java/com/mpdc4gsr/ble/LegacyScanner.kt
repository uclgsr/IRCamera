package com.mpdc4gsr.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.LeScanCallback
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import com.mpdc4gsr.ble.callback.ScanListener
import com.mpdc4gsr.ble.util.BluetoothPermissionUtils

internal class LegacyScanner(easyBle: EasyBLE, bluetoothAdapter: BluetoothAdapter?) :
    AbstractScanner(easyBle, bluetoothAdapter), LeScanCallback {
    override fun isReady(): Boolean {
        return true
    }

    override fun performStartScan() {
        val context: Context? = EasyBLE.Companion.getInstance().getContext()
        if (!BluetoothPermissionUtils.hasBluetoothScanPermission(context)) {
            Log.w(TAG, "Missing BLUETOOTH_SCAN permission for startLeScan()")
            handleScanCallback(
                false, null, false, ScanListener.Companion.ERROR_LACK_BLUETOOTH_PERMISSION,
                "Missing Bluetooth scan permission"
            )
            return
        }

        try {
            bluetoothAdapter.startLeScan(this)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException in startLeScan(): " + e.message)
            handleScanCallback(
                false, null, false, ScanListener.Companion.ERROR_LACK_BLUETOOTH_PERMISSION,
                "Bluetooth permission denied: " + e.message
            )
        }
    }

    override fun performStopScan() {
        val context: Context? = EasyBLE.Companion.getInstance().getContext()
        if (!BluetoothPermissionUtils.hasBluetoothScanPermission(context)) {
            Log.w(TAG, "Missing BLUETOOTH_SCAN permission for stopLeScan()")
            return
        }

        try {
            bluetoothAdapter.stopLeScan(this)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException in stopLeScan(): " + e.message)
        }
    }

    override fun onLeScan(device: BluetoothDevice?, rssi: Int, scanRecord: ByteArray?) {
        parseScanResult(device, false, null, rssi, scanRecord)
    }

    override fun getType(): ScannerType {
        return ScannerType.LEGACY
    }

    companion object {
        private const val TAG = "LegacyScanner"
    }
}
