package com.mpdc4gsr.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import com.mpdc4gsr.ble.callback.ScanListener
import com.mpdc4gsr.ble.util.BluetoothPermissionUtils
import com.mpdc4gsr.ble.util.Logger

internal class LeScanner(easyBle: EasyBLE, bluetoothAdapter: BluetoothAdapter?) :
    AbstractScanner(easyBle, bluetoothAdapter) {
    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            parseScanResult(result.getDevice(), result)
        }

        override fun onScanFailed(errorCode: Int) {
            handleScanCallback(
                false,
                null,
                false,
                ScanListener.Companion.ERROR_SCAN_FAILED,
                "onScanFailed. errorCode = " + errorCode
            )
            logger.log(Log.ERROR, Logger.Companion.TYPE_SCAN_STATE, "onScanFailed. errorCode = " + errorCode)
            stopScan(true)
        }
    }
    private var bleScanner: BluetoothLeScanner? = null

    private val leScanner: BluetoothLeScanner?
        get() {
            if (bleScanner == null) {
                bleScanner = bluetoothAdapter.getBluetoothLeScanner()
            }
            return bleScanner
        }

    override fun isReady(): Boolean {
        return this.leScanner != null
    }

    override fun performStartScan() {
        val context: Context? = EasyBLE.Companion.getInstance().getContext()
        if (!BluetoothPermissionUtils.hasBluetoothScanPermission(context)) {
            Log.w(TAG, "Missing BLUETOOTH_SCAN permission for startScan()")
            handleScanCallback(
                false, null, false, ScanListener.Companion.ERROR_LACK_BLUETOOTH_PERMISSION,
                "Missing Bluetooth scan permission"
            )
            return
        }

        val settings: ScanSettings?
        if (configuration.scanSettings == null) {
            settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .build()
        } else {
            settings = configuration.scanSettings
        }

        try {
            bleScanner!!.startScan(configuration.filters, settings, scanCallback)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException in startScan(): " + e.message)
            handleScanCallback(
                false, null, false, ScanListener.Companion.ERROR_LACK_BLUETOOTH_PERMISSION,
                "Bluetooth permission denied: " + e.message
            )
        }
    }

    override fun performStopScan() {
        if (bleScanner != null) {
            val context: Context? = EasyBLE.Companion.getInstance().getContext()
            if (!BluetoothPermissionUtils.hasBluetoothScanPermission(context)) {
                Log.w(TAG, "Missing BLUETOOTH_SCAN permission for stopScan()")
                return
            }

            try {
                bleScanner!!.stopScan(scanCallback)
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException in stopScan(): " + e.message)
            }
        }
    }

    override fun getType(): ScannerType {
        return ScannerType.LE
    }

    companion object {
        private const val TAG = "LeScanner"
    }
}
