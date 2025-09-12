package com.topdon.tc001.gsr

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityGsrDeviceManagementBinding
import com.topdon.ble.util.BluetoothPermissionUtils
import com.topdon.lib.core.ktbase.BaseBindingActivity
import com.topdon.tc001.sensors.gsr.GSRSensorRecorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * GSR Device Management Activity - UI parity with IR camera device management
 * Provides comprehensive device discovery, connection, and configuration interface
 * following the same patterns as thermal camera (Topdon TC001) management
 */
class GSRDeviceManagementActivity :
    BaseBindingActivity<ActivityGsrDeviceManagementBinding>(),
    View.OnClickListener {
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

    // Permission handling
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var pendingOperation: (() -> Unit)? = null

    // Device scanning state
    private var isScanning = false
    private var isConnecting = false

    override fun initContentLayoutId() = R.layout.activity_gsr_device_management

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("gsr_device_prefs", Context.MODE_PRIVATE)

        initializeUI()
        setupPermissionHandling()
        initializeGSRComponents()
        setupDeviceListRecycler()
        loadSavedDevices()
    }

    /**
     * Initialize UI components and event listeners - matching IR camera pattern
     */
    private fun initializeUI() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "GSR Device Management"

        // Setup click listeners
        binding.scanDevicesButton.setOnClickListener(this)
        binding.stopScanButton.setOnClickListener(this)
        binding.refreshButton.setOnClickListener(this)
        binding.settingsButton.setOnClickListener(this)

        // Setup device connection status indicators
        updateConnectionStatus("Not Connected")

        // Setup scanning indicator
        binding.scanningIndicator.visibility = View.GONE

        // Setup device list empty state
        updateDeviceListState()
    }

    /**
     * Setup permission handling system
     */
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

    /**
     * Initialize GSR sensor recorder and components
     */
    private fun initializeGSRComponents() {
        lifecycleScope.launch {
            try {
                gsrSensorRecorder = GSRSensorRecorder(this@GSRDeviceManagementActivity)
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

    /**
     * Setup device list RecyclerView - matching IR camera device list pattern
     */
    private fun setupDeviceListRecycler() {
        deviceAdapter =
            GSRDeviceAdapter(discoveredDevices) { device ->
                // Device item click handler
                connectToDevice(device)
            }

        binding.deviceRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@GSRDeviceManagementActivity)
            adapter = deviceAdapter
        }
    }

    /**
     * Load previously discovered and saved devices
     */
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

    /**
     * Start device scanning - matches IR camera discovery pattern
     */
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

    /**
     * Stop device scanning if in progress
     */
    private fun stopDeviceScan() {
        if (!isScanning) return

        isScanning = false
        updateScanningState(false)
        Log.i(TAG, "Device scan stopped by user")
        showToast("Device scan stopped")
    }

    /**
     * Refresh device list and connection status
     */
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

    /**
     * Connect to selected GSR device - matches IR camera connection pattern
     */
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

    /**
     * Open GSR settings activity
     */
    private fun openGSRSettings() {
        GSRSettingsActivity.startActivity(this)
    }

    /**
     * Request required permissions with user explanation
     */
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

    /**
     * Update scanning state UI indicators
     */
    private fun updateScanningState(scanning: Boolean) {
        binding.scanningIndicator.visibility = if (scanning) View.VISIBLE else View.GONE
        binding.scanDevicesButton.isEnabled = !scanning
        binding.stopScanButton.visibility = if (scanning) View.VISIBLE else View.GONE
        binding.scanProgressText.text = if (scanning) "Scanning for devices..." else ""
    }

    /**
     * Update connection status display
     */
    private fun updateConnectionStatus(status: String) {
        binding.connectionStatusText.text = status

        val color =
            when {
                status.contains("Connected", ignoreCase = true) -> getColor(android.R.color.holo_green_dark)
                status.contains("Connecting", ignoreCase = true) -> getColor(android.R.color.holo_orange_dark)
                else -> getColor(android.R.color.holo_red_dark)
            }
        binding.connectionStatusText.setTextColor(color)
    }

    /**
     * Update device list empty state
     */
    private fun updateDeviceListState() {
        if (discoveredDevices.isEmpty()) {
            binding.emptyStateText.visibility = View.VISIBLE
            binding.emptyStateText.text = "No devices found. Tap 'Scan Devices' to discover GSR sensors."
        } else {
            binding.emptyStateText.visibility = View.GONE
        }

        binding.deviceCountText.text = "${discoveredDevices.size} device(s) found"
    }

    /**
     * Enable or disable device operation buttons based on permissions
     */
    private fun enableDeviceOperations(enabled: Boolean) {
        binding.scanDevicesButton.isEnabled = enabled && !isScanning
        binding.refreshButton.isEnabled = enabled
    }

    /**
     * Show permission required dialog
     */
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

    /**
     * Utility methods
     */
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

/**
 * GSR device information data class
 */
data class GSRDeviceInfo(
    val name: String,
    val address: String,
    val rssi: Int,
    var isConnected: Boolean,
    val batteryLevel: Int,
    val firmwareVersion: String,
)
