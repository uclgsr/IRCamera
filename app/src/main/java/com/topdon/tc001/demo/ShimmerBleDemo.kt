package com.topdon.tc001.demo

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Minimal demonstration of the Shimmer BLE scanning functionality
 * This shows that the core BLE requirements from issue #14 are implemented and working
 */
class ShimmerBleDemo : AppCompatActivity() {

    companion object {
        private const val TAG = "ShimmerBleDemo"
    }

    private lateinit var statusText: TextView
    private lateinit var scanButton: Button
    private lateinit var connectButton: Button

    private var shimmerBluetoothManager: ShimmerBluetoothManagerAndroid? = null
    private val discoveredDevices = mutableListOf<BluetoothDevice>()
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            updateStatus("Permissions granted! Ready to scan for Shimmer devices.")
        } else {
            updateStatus("Permissions denied. Cannot scan for Shimmer devices.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shimmer_ble_demo)

        statusText = findViewById(R.id.text_status)
        scanButton = findViewById(R.id.button_scan)
        connectButton = findViewById(R.id.button_connect)

        scanButton.setOnClickListener { startShimmerScan() }
        connectButton.setOnClickListener { connectToFirstDevice() }
        
        connectButton.isEnabled = false
        
        initializeShimmer()
        updateStatus("Shimmer BLE Demo initialized. Check permissions and scan for devices.")
    }

    private fun initializeShimmer() {
        lifecycleScope.launch {
            try {
                shimmerBluetoothManager = ShimmerBluetoothManagerAndroid(
                    this@ShimmerBleDemo, 
                    android.os.Handler()
                )
                updateStatus("Shimmer manager initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Shimmer manager", e)
                updateStatus("Failed to initialize Shimmer manager: ${e.message}")
            }
        }
    }

    private fun startShimmerScan() {
        lifecycleScope.launch {
            if (!hasRequiredPermissions()) {
                requestMissingPermissions()
                return@launch
            }

            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) {
                updateStatus("Bluetooth not supported on this device")
                return@launch
            }

            if (!bluetoothAdapter.isEnabled) {
                updateStatus("Please enable Bluetooth and try again")
                return@launch
            }

            updateStatus("Scanning for Shimmer devices...")
            scanButton.isEnabled = false
            discoveredDevices.clear()
            
            try {
                val devices = performBluetoothLeScanning()
                if (devices.isNotEmpty()) {
                    updateStatus("Found ${devices.size} Shimmer device(s)! Ready to connect.")
                    connectButton.isEnabled = true
                } else {
                    updateStatus("No Shimmer devices found. Make sure your device is nearby and advertising.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Scan failed", e)
                updateStatus("Scan failed: ${e.message}")
            } finally {
                scanButton.isEnabled = true
            }
        }
    }

    private suspend fun performBluetoothLeScanning(): List<BluetoothDevice> = withContext(Dispatchers.IO) {
        val foundDevices = mutableListOf<BluetoothDevice>()
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

        if (bluetoothLeScanner == null) {
            Log.w(TAG, "BLE Scanner not available")
            return@withContext foundDevices
        }

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                if (isShimmerDevice(device) && !foundDevices.contains(device)) {
                    Log.d(TAG, "Found Shimmer device: ${device.name} (${device.address})")
                    foundDevices.add(device)
                    discoveredDevices.add(device)
                    
                    // Update UI on main thread
                    runOnUiThread {
                        updateStatus("Found: ${device.name ?: "Unknown Shimmer"} - ${device.address}")
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "BLE scan failed with error code: $errorCode")
                runOnUiThread {
                    updateStatus("BLE scan failed with error code: $errorCode")
                }
            }
        }

        try {
            // Start BLE scanning
            bluetoothLeScanner.startScan(scanCallback)
            Log.i(TAG, "Started BLE scan for Shimmer devices...")

            // Scan for 10 seconds
            delay(10000)

            bluetoothLeScanner.stopScan(scanCallback)
            Log.i(TAG, "BLE scan completed. Found ${foundDevices.size} devices")

        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception during BLE scan", e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during BLE scan", e)
        }

        return@withContext foundDevices
    }

    private fun connectToFirstDevice() {
        if (discoveredDevices.isEmpty()) {
            updateStatus("No devices to connect to")
            return
        }

        val device = discoveredDevices.first()
        lifecycleScope.launch {
            try {
                updateStatus("Connecting to ${device.name ?: "Shimmer Device"}...")
                
                // Use ShimmerBluetoothManagerAndroid to connect
                shimmerBluetoothManager?.connectShimmerThroughBTAddress(device.address)
                
                updateStatus("Connection initiated to ${device.address}. Check logs for status.")
                
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed", e)
                updateStatus("Connection failed: ${e.message}")
            }
        }
    }

    private fun isShimmerDevice(device: BluetoothDevice): Boolean {
        return try {
            val deviceName = device.name?.lowercase() ?: ""
            deviceName.contains("shimmer") || 
            deviceName.contains("gsr") || 
            deviceName.startsWith("rn4")
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied for device name access", e)
            false
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val permissions = getRequiredPermissions()
        return permissions.all { permission ->
            ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun getRequiredPermissions(): Array<String> {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    private fun requestMissingPermissions() {
        permissionLauncher.launch(getRequiredPermissions())
    }

    private fun updateStatus(message: String) {
        statusText.text = message
        Log.i(TAG, "Status: $message")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}