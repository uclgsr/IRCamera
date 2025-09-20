package com.mpdc4gsr.ble

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import com.mpdc4gsr.ble.UnifiedBleManager.UnifiedScanListener
import com.mpdc4gsr.ble.util.BluetoothPermissionUtils

class UnifiedBleExample(private val context: Context) {
    private var unifiedBleManager: UnifiedBleManager? = null

    private var shimmerGSRDevice: UnifiedDevice? = null
    private var topdonThermalDevice: UnifiedDevice? = null

    fun startComprehensiveExample() {
        Log.i(TAG, "Starting comprehensive Shimmer Nordic and Topdon BLE example")

        unifiedBleManager = UnifiedBleManager.Companion.getInstance(context)
        if (!unifiedBleManager!!.initialize()) {
            Log.e(TAG, "Failed to initialize unified BLE manager")
            return
        }

        startDeviceDiscovery()
    }

    private fun startDeviceDiscovery() {
        Log.i(TAG, "Starting unified device discovery")

        unifiedBleManager!!.startUnifiedDeviceDiscovery(object : UnifiedScanListener {
            override fun onShimmerDeviceFound(
                device: BluetoothDevice,
                type: UnifiedBleManager.DeviceType?,
                rssi: Int,
                scanRecord: ByteArray?
            ) {
                val deviceName = BluetoothPermissionUtils.getDeviceName(context, device)
                val deviceAddress = BluetoothPermissionUtils.getDeviceAddress(context, device)
                Log.i(
                    TAG,
                    "Found Shimmer device: " + deviceName + " (" + deviceAddress + ") Type: " + type + " RSSI: " + rssi
                )

                if (type == UnifiedBleManager.DeviceType.SHIMMER_GSR && shimmerGSRDevice == null) {
                    connectToShimmerGSRDevice(device)
                }
            }

            override fun onTopdonDeviceFound(
                device: BluetoothDevice,
                type: UnifiedBleManager.DeviceType?,
                rssi: Int,
                scanRecord: ByteArray?
            ) {
                val deviceName = BluetoothPermissionUtils.getDeviceName(context, device)
                val deviceAddress = BluetoothPermissionUtils.getDeviceAddress(context, device)
                Log.i(
                    TAG,
                    "Found MPDC4GSR device: " + deviceName + " (" + deviceAddress + ") Type: " + type + " RSSI: " + rssi
                )

                if (type == UnifiedBleManager.DeviceType.MPDC4GSR_THERMAL && topdonThermalDevice == null) {
                    connectToTopdonThermalDevice(device)
                }
            }

            override fun onUnknownDeviceFound(device: BluetoothDevice?, rssi: Int, scanRecord: ByteArray?) {
                val deviceName = BluetoothPermissionUtils.getDeviceName(context, device)
                val deviceAddress = BluetoothPermissionUtils.getDeviceAddress(context, device)
                Log.d(TAG, "Found unknown BLE device: " + deviceName + " (" + deviceAddress + ")")
            }

            override fun onScanError(errorCode: Int, message: String?) {
                Log.e(TAG, "Scan error: " + errorCode + " - " + message)
            }

            override fun onScanComplete() {
                Log.i(TAG, "Device discovery completed")

                checkDeviceReadiness()
            }
        })
    }

    private fun connectToShimmerGSRDevice(device: BluetoothDevice) {
        Log.i(TAG, "Connecting to Shimmer GSR device: " + device.getAddress())

        val gsrConfig: ShimmerDeviceConfig = ShimmerDeviceConfig.Companion.createDefaultGSRConfig()

        shimmerGSRDevice = unifiedBleManager!!.connectToShimmerDevice(
            device,
            gsrConfig,
            UnifiedBleExample.UnifiedConnectionListener()
        )

        if (shimmerGSRDevice != null) {
            Log.i(TAG, "Shimmer GSR device connection initiated")
        }
    }

    private fun connectToTopdonThermalDevice(device: BluetoothDevice) {
        Log.i(TAG, "Connecting to Topdon thermal device: " + device.getAddress())

        val thermalConfig: TopdonDeviceConfig = TopdonDeviceConfig.Companion.createDefaultThermalConfig()

        topdonThermalDevice = unifiedBleManager!!.connectToTopdonDevice(
            device,
            thermalConfig,
            UnifiedBleExample.UnifiedConnectionListener()
        )

        if (topdonThermalDevice != null) {
            Log.i(TAG, "Topdon thermal device connection initiated")
        }
    }

    private fun checkDeviceReadiness() {
        val connectedDevices = unifiedBleManager!!.getConnectedDevices()
        Log.i(TAG, "Connected devices: " + connectedDevices.size)

        var hasShimmerGSR = false
        var hasTopdonThermal = false

        for (device in connectedDevices) {
            if (device.isConnected()) {
                when (device.getDeviceType()) {
                    UnifiedBleManager.DeviceType.SHIMMER_GSR -> hasShimmerGSR = true
                    UnifiedBleManager.DeviceType.MPDC4GSR_THERMAL -> hasTopdonThermal = true
                }
            }
        }

        if (hasShimmerGSR && hasTopdonThermal) {
            Log.i(TAG, "All required devices connected - starting synchronized recording")
            startSynchronizedRecording()
        } else {
            Log.w(TAG, "Not all required devices connected. GSR: " + hasShimmerGSR + ", Thermal: " + hasTopdonThermal)
        }
    }

    private fun startSynchronizedRecording() {
        Log.i(TAG, "Starting synchronized multi-modal recording")

        if (shimmerGSRDevice != null && shimmerGSRDevice!!.isConnected()) {
            val gsrStarted = shimmerGSRDevice!!.startDataStreaming()
            Log.i(TAG, "GSR streaming started: " + gsrStarted)
        }

        if (topdonThermalDevice != null && topdonThermalDevice!!.isConnected()) {
            val thermalStarted = topdonThermalDevice!!.startDataStreaming()
            Log.i(TAG, "Thermal streaming started: " + thermalStarted)
        }

        Log.i(TAG, "Synchronized multi-modal recording active")
    }

    fun stopAndCleanup() {
        Log.i(TAG, "Stopping recording and cleaning up")

        if (shimmerGSRDevice != null) {
            shimmerGSRDevice!!.stopDataStreaming()
        }

        if (topdonThermalDevice != null) {
            topdonThermalDevice!!.stopDataStreaming()
        }

        if (unifiedBleManager != null) {
            unifiedBleManager!!.disconnectAllDevices()
            unifiedBleManager!!.cleanup()
        }

        Log.i(TAG, "Cleanup completed")
    }

    private fun handleGSRData(device: UnifiedDevice, data: ByteArray) {
        Log.d(TAG, "GSR data received: " + data.size + " bytes from " + device.getAddress())
    }

    private fun handleThermalData(device: UnifiedDevice, data: ByteArray) {
        Log.d(TAG, "Thermal data received: " + data.size + " bytes from " + device.getAddress())
    }

    private inner class UnifiedConnectionListener : UnifiedBleManager.UnifiedConnectionListener {
        override fun onDeviceConnected(device: UnifiedDevice) {
            Log.i(TAG, "Device connected: " + device.getName() + " (" + device.getDeviceType() + ")")
        }

        override fun onDeviceDisconnected(device: UnifiedDevice, reason: Int) {
            Log.i(TAG, "Device disconnected: " + device.getName() + " Reason: " + reason)
        }

        override fun onConnectionError(device: UnifiedDevice, errorCode: Int, message: String?) {
            Log.e(TAG, "Connection error for " + device.getName() + ": " + errorCode + " - " + message)
        }

        override fun onDataReceived(device: UnifiedDevice, data: ByteArray) {
            when (device.getDeviceType()) {
                UnifiedBleManager.DeviceType.SHIMMER_GSR -> handleGSRData(device, data)
                UnifiedBleManager.DeviceType.MPDC4GSR_THERMAL -> handleThermalData(device, data)
                else -> Log.d(TAG, "Data received from " + device.getDeviceType() + ": " + data.size + " bytes")
            }
        }

        override fun onDeviceReady(device: UnifiedDevice) {
            Log.i(TAG, "Device ready: " + device.getName() + " (" + device.getDeviceType() + ")")
            checkDeviceReadiness()
        }
    }

    companion object {
        private const val TAG = "UnifiedBleExample"
    }
}
