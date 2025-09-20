package com.mpdc4gsr.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.text.TextUtils
import android.util.Log
import android.util.SparseArray
import androidx.annotation.CallSuper
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.mpdc4gsr.ble.callback.ScanListener
import com.mpdc4gsr.ble.util.Logger
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

internal abstract class AbstractScanner(easyBle: EasyBLE, val bluetoothAdapter: BluetoothAdapter) : Scanner {
    val configuration: ScanConfiguration
    val logger: Logger
    private val mainHandler: Handler
    private val scanListeners: MutableList<ScanListener> = CopyOnWriteArrayList<ScanListener>()
    private val proxyBluetoothProfiles = SparseArray<BluetoothProfile?>()
    private val deviceCreator: DeviceCreator
    private var isScanning = false

    override fun addScanListener(listener: ScanListener?) {
        if (!scanListeners.contains(listener)) {
            scanListeners.add(listener!!)
        }
    }

    override fun removeScanListener(listener: ScanListener?) {
        scanListeners.remove(listener)
    }

    private fun isLocationEnabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
            return locationManager != null && locationManager.isLocationEnabled()
        } else {
            try {
                val locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE)
                return locationMode != Settings.Secure.LOCATION_MODE_OFF
            } catch (e: SettingNotFoundException) {
                return false
            }
        }
    }

    private fun noLocationPermission(context: Context): Boolean {
        val sdkVersion = context.getApplicationInfo().targetSdkVersion
        if (sdkVersion >= 29) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        } else {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
        }
    }

    private fun noBluetoothPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
        } else {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_ADMIN
                    ) != PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasBluetoothConnectPermission(context: Context?): Boolean {
        if (context == null) return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun handleScanCallback(
        start: Boolean, device: Device?, isConnectedBySys: Boolean,
        errorCode: Int, errorMsg: String?
    ) {
        mainHandler.post(Runnable {
            for (listener in scanListeners) {
                if (device != null) {
                    listener.onScanResult(device, isConnectedBySys)
                } else if (start) {
                    listener.onScanStart()
                } else if (errorCode >= 0) {
                    listener.onScanError(errorCode, errorMsg)
                } else {
                    listener.onScanStop()
                }
            }
        })
    }

    private fun getSystemConnectedDevices(context: Context?) {
        try {
            val method = bluetoothAdapter.javaClass.getDeclaredMethod("getConnectionState")
            method.setAccessible(true)
            val state = method.invoke(bluetoothAdapter) as Int
            if (state == BluetoothAdapter.STATE_CONNECTED) {
                val devices = bluetoothAdapter.getBondedDevices()
                for (device in devices) {
                    val isConnectedMethod = device.javaClass.getDeclaredMethod("isConnected")
                    isConnectedMethod.setAccessible(true)
                    val isConnected = isConnectedMethod.invoke(device) as Boolean
                    if (isConnected) {
                        parseScanResult(device, true)
                    }
                }
            }
        } catch (ignore: Exception) {
        }

        for (i in 1..21) {
            try {
                getSystemConnectedDevices(context, i)
            } catch (ignore: Exception) {
            }
        }
    }

    private fun getSystemConnectedDevices(context: Context?, profile: Int) {
        bluetoothAdapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                if (proxy == null) return
                proxyBluetoothProfiles.put(profile, proxy)
                synchronized(this@AbstractScanner) {
                    if (!isScanning) return
                }
                try {
                    val devices = proxy.getConnectedDevices()
                    for (device in devices) {
                        parseScanResult(device, true)
                    }
                } catch (ignore: Exception) {
                }
            }

            override fun onServiceDisconnected(profile: Int) {
            }
        }, profile)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun parseScanResult(device: BluetoothDevice, result: ScanResult?) {
        if (result == null) {
            parseScanResult(device, false)
        } else {
            val record = result.getScanRecord()
            parseScanResult(device, false, result, result.getRssi(), if (record == null) null else record.getBytes())
        }
    }

    private fun parseScanResult(device: BluetoothDevice, isConnectedBySys: Boolean) {
        parseScanResult(device, isConnectedBySys, null, -120, null)
    }

    fun parseScanResult(
        device: BluetoothDevice,
        isConnectedBySys: Boolean,
        result: ScanResult?,
        rssi: Int,
        scanRecord: ByteArray?
    ) {
        val context: Context? = EasyBLE.Companion.getInstance().getContext()
        if (context != null && noBluetoothPermission(context)) {
            logger.log(
                Log.WARN,
                Logger.Companion.TYPE_SCAN_STATE,
                "Missing Bluetooth permissions, skipping device access"
            )
            return
        }

        var deviceType = BluetoothDevice.DEVICE_TYPE_UNKNOWN
        var deviceAddress = ""
        var deviceName: String? = ""

        try {
            if (hasBluetoothConnectPermission(context)) {
                deviceType = device.getType()
                deviceAddress = device.getAddress()
                deviceName = device.getName()
            }
        } catch (e: SecurityException) {
            logger.log(
                Log.WARN,
                Logger.Companion.TYPE_SCAN_STATE,
                "SecurityException accessing device properties: " + e.message
            )
            return
        }

        if ((configuration.onlyAcceptBleDevice && deviceType != BluetoothDevice.DEVICE_TYPE_LE) ||
            !deviceAddress.matches("^[0-9A-F]{2}(:[0-9A-F]{2}){5}$".toRegex())
        ) {
            return
        }
        val name = if (deviceName == null) "" else deviceName
        if (configuration.rssiLowLimit <= rssi) {
            val dev = deviceCreator.create(device, result)
            if (dev != null) {
                dev.name = if (TextUtils.isEmpty(dev.getName())) name else dev.getName()
                dev.rssi = rssi
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    dev.scanResult = result
                }
                dev.scanRecord = scanRecord
                handleScanCallback(false, dev, isConnectedBySys, -1, "")
            }
        }
        val msg = String.format(
            Locale.US,
            "found device! [name: %s, addr: %s]",
            if (TextUtils.isEmpty(name)) "N/A" else name,
            device.getAddress()
        )
        logger.log(Log.DEBUG, Logger.Companion.TYPE_SCAN_STATE, msg)
    }

    @CallSuper
    override fun startScan(context: Context) {
        synchronized(this) {
            if (!this.isBtEnabled || (getType() != ScannerType.CLASSIC && isScanning) || !this.isReady) {
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!isLocationEnabled(context)) {
                    val errorMsg =
                        "Unable to scan for Bluetooth devices, the phone's location service is not turned on."
                    handleScanCallback(
                        false,
                        null,
                        false,
                        ScanListener.Companion.ERROR_LOCATION_SERVICE_CLOSED,
                        errorMsg
                    )
                    logger.log(Log.ERROR, Logger.Companion.TYPE_SCAN_STATE, errorMsg)
                    return
                } else if (noLocationPermission(context)) {
                    val errorMsg = "Unable to scan for Bluetooth devices, lack location permission."
                    handleScanCallback(
                        false,
                        null,
                        false,
                        ScanListener.Companion.ERROR_LACK_LOCATION_PERMISSION,
                        errorMsg
                    )
                    logger.log(Log.ERROR, Logger.Companion.TYPE_SCAN_STATE, errorMsg)
                    return
                } else if (noBluetoothPermission(context)) {
                    val errorMsg = "Unable to scan for Bluetooth devices, lack Bluetooth permission."
                    handleScanCallback(
                        false,
                        null,
                        false,
                        ScanListener.Companion.ERROR_LACK_BLUETOOTH_PERMISSION,
                        errorMsg
                    )
                    logger.log(Log.ERROR, Logger.Companion.TYPE_SCAN_STATE, errorMsg)
                    return
                }
            }
            if (getType() != ScannerType.CLASSIC) {
                isScanning = true
            }
        }
        if (getType() != ScannerType.CLASSIC) {
            handleScanCallback(true, null, false, -1, "")
        }
        if (configuration.acceptSysConnectedDevice) {
            getSystemConnectedDevices(context)
        }
        performStartScan()
        if (getType() != ScannerType.CLASSIC) {
            mainHandler.postDelayed(stopScanRunnable, configuration.scanPeriodMillis.toLong())
        }
    }

    override fun isScanning(): Boolean {
        return isScanning
    }

    @CallSuper
    open fun setScanning(scanning: Boolean) {
        synchronized(this) {
            isScanning = scanning
        }
    }

    @CallSuper
    override fun stopScan(quietly: Boolean) {
        mainHandler.removeCallbacks(stopScanRunnable)
        val size = proxyBluetoothProfiles.size()
        for (i in 0..<size) {
            try {
                bluetoothAdapter.closeProfileProxy(proxyBluetoothProfiles.keyAt(i), proxyBluetoothProfiles.valueAt(i))
            } catch (ignore: Exception) {
            }
        }
        proxyBluetoothProfiles.clear()
        if (this.isBtEnabled) {
            performStopScan()
        }
        if (getType() != ScannerType.CLASSIC) {
            synchronized(this) {
                if (isScanning) {
                    isScanning = false
                    if (!quietly) {
                        handleScanCallback(false, null, false, -1, "")
                    }
                }
            }
        }
    }

    private val isBtEnabled: Boolean
        get() {
            if (bluetoothAdapter.isEnabled()) {
                try {
                    val method = bluetoothAdapter.javaClass.getDeclaredMethod("isLeEnabled")
                    method.setAccessible(true)
                    return method.invoke(bluetoothAdapter) as Boolean
                } catch (e: Exception) {
                    val state = bluetoothAdapter.getState()
                    return state == BluetoothAdapter.STATE_ON || state == 15
                }
            }
            return false
        }

    override fun onBluetoothOff() {
        synchronized(this) {
            isScanning = false
        }
        handleScanCallback(false, null, false, -1, "")
    }

    override fun release() {
        stopScan(false)
        scanListeners.clear()
    }

    protected abstract val isReady: Boolean
    private val stopScanRunnable = Runnable { stopScan(false) }

    init {
        this.configuration = easyBle.scanConfiguration
        mainHandler = Handler(Looper.getMainLooper())
        logger = easyBle.getLogger()
        deviceCreator = easyBle.getDeviceCreator()
    }

    protected abstract fun performStartScan()

    protected abstract fun performStopScan()
}
