package com.topdon.tc001.sensors.gsr

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.csl.irCamera.R
import com.topdon.ble.util.BluetoothPermissionUtils
import com.topdon.tc001.sensors.gsr.GSRSensorRecorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GSRDeviceManagementActivity : AppCompatActivity(), View.OnClickListener {
    companion object {
        private const val TAG = "GSRDeviceManagement"

        fun startActivity(context: Context) {
            context.startActivity(Intent(context, GSRDeviceManagementActivity::class.java))
        }
    }

    private lateinit var prefs: SharedPreferences
    private var gsrSensorRecorder: GSRSensorRecorder? = null
    private lateinit var deviceAdapter: GSRDeviceAdapter
    private val discoveredDevices = mutableListOf<GSRDeviceInfo>()

    // Bluetooth components
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothManager: BluetoothManager? = null

    // Permission handling
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var pendingOperation: (() -> Unit)? = null

    // Device scanning state
    private var isScanning = false
    private var isConnecting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gsr_device_management)

        prefs = getSharedPreferences("gsr_device_prefs", Context.MODE_PRIVATE)

        // Initialize Bluetooth components
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        bluetoothAdapter = bluetoothManager?.adapter

        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not supported on this device")
            showErrorMessage("Bluetooth not supported on this device")
            finish()
            return
        }

        if (!bluetoothAdapter!!.isEnabled) {
            Log.w(TAG, "Bluetooth is disabled. User should enable it.")
            showBluetoothEnableDialog()
        }

        initializeUI()
        setupPermissionHandling()
        initializeGSRComponents()
        setupDeviceListRecycler()
        loadSavedDevices()
    }

    private fun initializeUI() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "GSR Device Management"

        // Setup click listeners
        findViewById<View>(R.id.scanDevicesButton)?.setOnClickListener(this)
        findViewById<View>(R.id.stopScanButton)?.setOnClickListener(this)
        findViewById<View>(R.id.refreshButton)?.setOnClickListener(this)
        findViewById<View>(R.id.settingsButton)?.setOnClickListener(this)

        // Setup device connection status indicators
        updateConnectionStatus("Not Connected")

        // Setup scanning indicator
        findViewById<View>(R.id.scanningIndicator)?.visibility = View.GONE

        // Setup device list empty state
        updateDeviceListState()
    }

    private fun setupPermissionHandling() {
        permissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) { permissions ->
                val allGranted = permissions.values.all { it }

                if (allGranted) {
                    Log.i(TAG, "All required permissions granted")
                    enableDeviceOperations(true)
                    pendingOperation?.invoke()
                    pendingOperation = null
                } else {
                    Log.w(TAG, "Some permissions were denied")
                    showPermissionRequiredDialog()
                    enableDeviceOperations(false)
                }
            }
    }

    private fun initializeGSRComponents() {
        lifecycleScope.launch {
            try {
                gsrSensorRecorder = GSRSensorRecorder(
                    this@GSRDeviceManagementActivity,
                    "gsr_management_1",
                    128,
                    com.topdon.tc001.controller.RecordingController(
                        this@GSRDeviceManagementActivity,
                        this@GSRDeviceManagementActivity
                    )
                )
                val initialized = gsrSensorRecorder?.initialize() ?: false

                if (initialized) {
                    Log.i(TAG, "GSR sensor recorder initialized successfully")
                    enableDeviceOperations(BluetoothPermissionUtils.hasBluetoothPermissions(this@GSRDeviceManagementActivity))
                } else {
                    Log.w(TAG, "GSR sensor recorder initialization failed")
                    showErrorMessage("Failed to initialize GSR system")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing GSR components", e)
                showErrorMessage("Error initializing GSR system: ${e.message}")
            }
        }
    }

    private fun setupDeviceListRecycler() {
        deviceAdapter =
            GSRDeviceAdapter(discoveredDevices) { device ->
                // Device item click handler
                connectToDevice(device)
            }

        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.deviceRecyclerView)?.apply {
            layoutManager = LinearLayoutManager(this@GSRDeviceManagementActivity)
            adapter = deviceAdapter
        }
    }

    private fun loadSavedDevices() {
        try {
            val savedDevicesJson = prefs.getString("saved_devices", "[]")
            // Parse and load saved devices (simplified implementation)
            Log.i(TAG, "Loaded saved devices configuration")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load saved devices", e)
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.scanDevicesButton -> startDeviceScan()
            R.id.stopScanButton -> stopDeviceScan()
            R.id.refreshButton -> refreshDeviceList()
            R.id.settingsButton -> openGSRSettings()
        }
    }

    private fun startDeviceScan() {
        if (!BluetoothPermissionUtils.hasBleScanningPermissions(this)) {
            requestRequiredPermissions {
                startDeviceScan()
            }
            return
        }

        if (isScanning) {
            Log.w(TAG, "Device scan already in progress")
            return
        }

        lifecycleScope.launch {
            try {
                isScanning = true
                updateScanningState(true)

                Log.i(TAG, "Starting GSR device scan")

                // Clear previous results
                discoveredDevices.clear()
                deviceAdapter.notifyDataSetChanged()
                updateDeviceListState()

                // Perform device discovery
                val devices = gsrSensorRecorder?.getAvailableShimmerDevices() ?: emptyList()

                // Simulate progressive discovery (like IR camera scanning)
                devices.forEach { deviceName ->
                    delay(500) // Simulate discovery time

                    val deviceInfo =
                        GSRDeviceInfo(
                            name = deviceName,
                            address = extractMacAddress(deviceName),
                            rssi = -50, // Simulated signal strength
                            isConnected = false,
                            batteryLevel = 85, // Simulated battery
                            firmwareVersion = "1.0.0",
                        )

                    discoveredDevices.add(deviceInfo)
                    deviceAdapter.notifyItemInserted(discoveredDevices.size - 1)
                    updateDeviceListState()

                    Log.d(TAG, "Discovered GSR device: $deviceName")
                }

                Log.i(TAG, "Device scan completed. Found ${devices.size} devices")
                showToast("Found ${devices.size} GSR devices")
            } catch (e: Exception) {
                Log.e(TAG, "Device scan failed", e)
                showErrorMessage("Device scan failed: ${e.message}")
            } finally {
                isScanning = false
                updateScanningState(false)
            }
        }
    }

    private fun stopDeviceScan() {
        if (!isScanning) return

        isScanning = false
        updateScanningState(false)
        Log.i(TAG, "Device scan stopped by user")
        showToast("Device scan stopped")
    }

    private fun refreshDeviceList() {
        lifecycleScope.launch {
            try {
                // Update connection status for known devices
                discoveredDevices.forEach { device ->
                    // Check if device is still available and connected
                    device.isConnected = checkDeviceConnection(device.address)
                }

                deviceAdapter.notifyDataSetChanged()

                // Update current connection status
                val connectedDevice = discoveredDevices.find { it.isConnected }
                if (connectedDevice != null) {
                    updateConnectionStatus("Connected to ${connectedDevice.name}")
                } else {
                    updateConnectionStatus("Not Connected")
                }

                Log.i(TAG, "Device list refreshed")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to refresh device list", e)
            }
        }
    }

    private fun connectToDevice(device: GSRDeviceInfo) {
        if (isConnecting) {
            Log.w(TAG, "Connection already in progress")
            return
        }

        if (!BluetoothPermissionUtils.hasBluetoothPermissions(this)) {
            requestRequiredPermissions {
                connectToDevice(device)
            }
            return
        }

        // Check if device needs pairing first
        checkDevicePairingStatus(device)
    }

    private fun performActualConnection(device: GSRDeviceInfo) {
        lifecycleScope.launch {
            try {
                isConnecting = true
                updateConnectionStatus("Connecting to ${device.name}...")

                Log.i(TAG, "Attempting to connect to GSR device: ${device.name}")

                val success = gsrSensorRecorder?.connectToShimmerDevice(device.address) ?: false

                if (success) {
                    device.isConnected = true
                    updateConnectionStatus("Connected to ${device.name}")
                    saveDeviceConnection(device)
                    showToast("Successfully connected to ${device.name}")

                    Log.i(TAG, "Successfully connected to GSR device: ${device.name}")
                } else {
                    device.isConnected = false
                    updateConnectionStatus("Connection failed")
                    showErrorMessage("Failed to connect to ${device.name}")

                    Log.w(TAG, "Failed to connect to GSR device: ${device.name}")
                }

                deviceAdapter.notifyDataSetChanged()
            } catch (e: Exception) {
                Log.e(TAG, "Connection attempt failed", e)
                device.isConnected = false
                updateConnectionStatus("Connection error")
                showErrorMessage("Connection error: ${e.message}")
                deviceAdapter.notifyDataSetChanged()
            } finally {
                isConnecting = false
            }
        }
    }

    private fun showBluetoothEnableDialog() {
        AlertDialog.Builder(this)
            .setTitle("Bluetooth Required")
            .setMessage("Bluetooth must be enabled to discover and connect to Shimmer GSR devices. Please enable Bluetooth in Settings.")
            .setPositiveButton("Enable") { _, _ ->
                try {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivity(enableBtIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start Bluetooth enable intent", e)
                    showErrorMessage("Could not open Bluetooth settings")
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                showErrorMessage("Bluetooth is required for GSR device functionality")
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun checkDevicePairingStatus(device: GSRDeviceInfo) {
        try {
            if (!BluetoothPermissionUtils.hasBluetoothPermissions(this)) {
                Log.w(TAG, "Cannot check pairing status without Bluetooth permissions")
                return
            }

            val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)
            if (bluetoothDevice == null) {
                Log.w(TAG, "Could not get Bluetooth device for address: ${device.address}")
                return
            }

            val isPaired = bluetoothDevice.bondState == BluetoothDevice.BOND_BONDED
            Log.d(
                TAG,
                "Device ${device.name} pairing status: ${if (isPaired) "Paired" else "Not paired"}"
            )

            if (!isPaired) {
                showPairingDialog(device, bluetoothDevice)
            } else {
                // Device is already paired, proceed with connection
                proceedWithConnection(device)
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception checking device pairing status", e)
            showErrorMessage("Bluetooth permissions required to check device pairing")
        } catch (e: Exception) {
            Log.e(TAG, "Error checking device pairing status", e)
            showErrorMessage("Error checking device pairing status")
        }
    }

    private fun showPairingDialog(deviceInfo: GSRDeviceInfo, bluetoothDevice: BluetoothDevice) {
        AlertDialog.Builder(this)
            .setTitle("Device Pairing Required")
            .setMessage("The Shimmer device '${deviceInfo.name}' needs to be paired before connection. Would you like to pair it now?")
            .setPositiveButton("Pair") { _, _ ->
                initiateDevicePairing(deviceInfo, bluetoothDevice)
            }
            .setNegativeButton("Cancel") { _, _ ->
                showErrorMessage("Device pairing is required for connection")
            }
            .show()
    }

    private fun initiateDevicePairing(deviceInfo: GSRDeviceInfo, bluetoothDevice: BluetoothDevice) {
        try {
            if (!BluetoothPermissionUtils.hasBluetoothPermissions(this)) {
                requestRequiredPermissions {
                    initiateDevicePairing(deviceInfo, bluetoothDevice)
                }
                return
            }

            Log.i(TAG, "Initiating pairing for device: ${deviceInfo.name}")

            val pairingResult = bluetoothDevice.createBond()
            if (pairingResult) {
                showToast("Pairing request sent. Please confirm on the device.")
                Log.i(TAG, "Pairing request sent for ${deviceInfo.name}")

                // Monitor pairing result (simplified - in real app you'd register a BroadcastReceiver)
                lifecycleScope.launch {
                    delay(5000) // Wait 5 seconds for pairing to complete
                    if (bluetoothDevice.bondState == BluetoothDevice.BOND_BONDED) {
                        showToast("Device paired successfully!")
                        proceedWithConnection(deviceInfo)
                    } else {
                        showErrorMessage("Device pairing failed. Please try again.")
                    }
                }
            } else {
                showErrorMessage("Failed to initiate device pairing")
                Log.w(TAG, "Failed to create bond for device: ${deviceInfo.name}")
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception during device pairing", e)
            showErrorMessage("Bluetooth permissions required for device pairing")
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating device pairing", e)
            showErrorMessage("Error during device pairing: ${e.message}")
        }
    }

    private fun proceedWithConnection(device: GSRDeviceInfo) {
        // Device is paired, proceed with actual connection logic
        performActualConnection(device)
    }

    private fun openGSRSettings() {
        GSRSettingsActivity.startActivity(this)
    }

    private fun requestRequiredPermissions(onGranted: (() -> Unit)? = null) {
        val missingPermissions = BluetoothPermissionUtils.getMissingPermissions(this)

        if (missingPermissions.isEmpty()) {
            enableDeviceOperations(true)
            onGranted?.invoke()
            return
        }

        pendingOperation = onGranted
        permissionLauncher.launch(missingPermissions.toTypedArray())
    }

    private fun updateScanningState(scanning: Boolean) {
        findViewById<View>(R.id.scanningIndicator)?.visibility =
            if (scanning) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.scanDevicesButton)?.isEnabled = !scanning
        findViewById<View>(R.id.stopScanButton)?.visibility =
            if (scanning) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.scanProgressText)?.text =
            if (scanning) "Scanning for devices..." else ""
    }

    private fun updateConnectionStatus(status: String) {
        findViewById<TextView>(R.id.connectionStatusText)?.text = status

        val color =
            when {
                status.contains(
                    "Connected",
                    ignoreCase = true
                ) -> getColor(android.R.color.holo_green_dark)

                status.contains(
                    "Connecting",
                    ignoreCase = true
                ) -> getColor(android.R.color.holo_orange_dark)

                else -> getColor(android.R.color.holo_red_dark)
            }
        findViewById<TextView>(R.id.connectionStatusText)?.setTextColor(color)
    }

    private fun updateDeviceListState() {
        if (discoveredDevices.isEmpty()) {
            findViewById<TextView>(R.id.emptyStateText)?.visibility = View.VISIBLE
            findViewById<TextView>(R.id.emptyStateText)?.text =
                "No devices found. Tap 'Scan Devices' to discover GSR sensors."
        } else {
            findViewById<TextView>(R.id.emptyStateText)?.visibility = View.GONE
        }

        findViewById<TextView>(R.id.deviceCountText)?.text =
            "${discoveredDevices.size} device(s) found"
    }

    private fun enableDeviceOperations(enabled: Boolean) {
        findViewById<Button>(R.id.scanDevicesButton)?.isEnabled = enabled && !isScanning
        findViewById<Button>(R.id.refreshButton)?.isEnabled = enabled
    }

    private fun showPermissionRequiredDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("Bluetooth permissions are required to discover and connect to GSR devices. Please grant the required permissions.")
            .setPositiveButton("Grant Permissions") { _, _ ->
                requestRequiredPermissions()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun extractMacAddress(deviceName: String): String {
        // Extract MAC address from device name (format: "DeviceName (XX:XX:XX:XX:XX:XX)")
        return if (deviceName.contains("(") && deviceName.contains(")")) {
            deviceName.substringAfter("(").substringBefore(")")
        } else {
            "00:00:00:00:00:00" // Default/unknown MAC
        }
    }

    private suspend fun checkDeviceConnection(address: String): Boolean {
        // Check if device is currently connected
        return gsrSensorRecorder?.getShimmerConnectionStatus()?.contains("Connected") == true
    }

    private fun saveDeviceConnection(device: GSRDeviceInfo) {
        // Save successful connection for future reference
        prefs.edit().apply {
            putString("last_connected_device", device.address)
            putString("last_connected_name", device.name)
            apply()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showErrorMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Cleanup GSR components
        lifecycleScope.launch {
            try {
                gsrSensorRecorder?.cleanup()
            } catch (e: Exception) {
                Log.w(TAG, "Error during GSR cleanup", e)
            }
        }
    }
}

data class GSRDeviceInfo(
    val name: String,
    val address: String,
    val rssi: Int,
    var isConnected: Boolean,
    val batteryLevel: Int,
    val firmwareVersion: String,
)
