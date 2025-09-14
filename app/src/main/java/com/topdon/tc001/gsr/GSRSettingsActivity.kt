package com.topdon.tc001.gsr

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityGsrSettingsBinding
import com.topdon.ble.util.BluetoothPermissionUtils
import com.topdon.lib.core.ktbase.BaseBindingActivity
import com.topdon.tc001.sensors.gsr.GSRSensorRecorder
import kotlinx.coroutines.launch

class GSRSettingsActivity : BaseBindingActivity<ActivityGsrSettingsBinding>() {
    companion object {
        private const val TAG = "GSRSettingsActivity"

        fun startActivity(context: Context) {
            context.startActivity(Intent(context, GSRSettingsActivity::class.java))
        }
    }

    private lateinit var prefs: SharedPreferences
    private var gsrSensorRecorder: GSRSensorRecorder? = null
    private val availableDevices = mutableListOf<String>()
    private lateinit var deviceAdapter: ArrayAdapter<String>

    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var pendingOperation: (() -> Unit)? = null

    override fun initContentLayoutId() = R.layout.activity_gsr_settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        setupPermissionHandling()

        gsrSensorRecorder = GSRSensorRecorder(this)

        setupUI()
        loadCurrentSettings()
        setupListeners()
        setupDeviceManagement()

        onBackPressedDispatcher.addCallback(
            this,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    saveCurrentSettings()
                    finish()
                }
            },
        )
    }

    private fun setupPermissionHandling() {
        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                val allGranted = permissions.values.all { it }
                val deniedPermissions = permissions.filter { !it.value }.keys

                if (allGranted) {
                    Log.i(TAG, "All Bluetooth permissions granted")
                    updatePermissionStatus(
                        "All Required Permissions Granted",
                        android.graphics.Color.parseColor("#4caf50")
                    )

                    pendingOperation?.invoke()
                    pendingOperation = null

                    enableDeviceManagement(true)
                } else {
                    Log.w(TAG, "Some Bluetooth permissions denied: $deniedPermissions")

                    val permanentlyDenied =
                        deniedPermissions.any { permission ->
                            !ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
                        }

                    if (permanentlyDenied) {
                        showPermissionPermanentlyDeniedDialog(deniedPermissions.toList())
                    } else {
                        showPermissionDeniedDialog(deniedPermissions.toList())
                    }

                    updatePermissionStatus(
                        "Missing Required Permissions",
                        android.graphics.Color.parseColor("#f44336")
                    )
                    enableDeviceManagement(false)
                }
            }
    }

    private fun showPermissionDialog(missingPermissions: List<String>) {
        val permissionDescriptions =
            missingPermissions.map { permission ->
                "• ${BluetoothPermissionUtils.getPermissionRationale(permission)}"
            }.joinToString("\n")

        AlertDialog.Builder(this)
            .setTitle("Permissions Required for Shimmer GSR")
            .setMessage(
                "The following permissions are required for Shimmer GSR device functionality:\n\n$permissionDescriptions\n\nGrant these permissions to enable device scanning and connection.",
            )
            .setPositiveButton("Grant Permissions") { _, _ ->
                requestMissingPermissions()
            }
            .setNegativeButton("Skip") { _, _ ->
                updatePermissionStatus(
                    "Permissions Denied - Limited Functionality",
                    android.graphics.Color.parseColor("#ff9800")
                )
                enableDeviceManagement(false)
            }
            .setCancelable(false)
            .show()
    }

    private fun showPermissionDeniedDialog(deniedPermissions: List<String>) {
        val permissionDescriptions =
            deniedPermissions.map { permission ->
                "• ${BluetoothPermissionUtils.getPermissionRationale(permission)}"
            }.joinToString("\n")

        AlertDialog.Builder(this)
            .setTitle("Permissions Denied")
            .setMessage(
                "The following permissions were denied:\n\n$permissionDescriptions\n\nWithout these permissions, Shimmer GSR device functionality will be limited.",
            )
            .setPositiveButton("Try Again") { _, _ ->
                requestMissingPermissions()
            }
            .setNegativeButton("Continue Without") { _, _ ->
                updatePermissionStatus(
                    "Limited Functionality - Permissions Denied",
                    android.graphics.Color.parseColor("#ff9800")
                )
                enableDeviceManagement(false)
            }
            .show()
    }

    private fun showPermissionPermanentlyDeniedDialog(deniedPermissions: List<String>) {
        val permissionDescriptions =
            deniedPermissions.map { permission ->
                "• ${BluetoothPermissionUtils.getPermissionRationale(permission)}"
            }.joinToString("\n")

        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage(
                "The following permissions are required but were denied:\n\n$permissionDescriptions\n\nTo enable Shimmer GSR functionality, please grant these permissions in the app settings.",
            )
            .setPositiveButton("Open Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Continue Without") { _, _ ->
                updatePermissionStatus(
                    "GSR Disabled - Check App Settings",
                    android.graphics.Color.parseColor("#f44336")
                )
                enableDeviceManagement(false)
            }
            .show()
    }

    private fun openAppSettings() {
        try {
            val intent =
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app settings", e)
            Toast.makeText(
                this,
                "Please grant permissions in app settings manually",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun requestMissingPermissions() {
        val missingPermissions = BluetoothPermissionUtils.getMissingPermissions(this)

        if (missingPermissions.isEmpty()) {
            Log.i(TAG, "All permissions already granted")
            updatePermissionStatus(
                "All Permissions Granted",
                android.graphics.Color.parseColor("#4caf50")
            )
            enableDeviceManagement(true)
            return
        }

        Log.i(TAG, "Requesting missing permissions: $missingPermissions")
        permissionLauncher.launch(missingPermissions.toTypedArray())
    }

    private fun checkAndRequestPermissions(onPermissionsGranted: (() -> Unit)? = null) {
        val missingPermissions = BluetoothPermissionUtils.getMissingPermissions(this)

        if (missingPermissions.isEmpty()) {
            Log.i(TAG, "All required permissions already granted")
            updatePermissionStatus(
                "All Permissions Granted",
                android.graphics.Color.parseColor("#4caf50")
            )
            enableDeviceManagement(true)
            onPermissionsGranted?.invoke()
            return
        }

        pendingOperation = onPermissionsGranted

        showPermissionDialog(missingPermissions)
    }

    private fun enableDeviceManagement(enabled: Boolean) {
        binding.scanDevicesButton.isEnabled = enabled
        binding.connectDeviceButton.isEnabled = enabled
        binding.shimmerDeviceSpinner.isEnabled = enabled

        if (!enabled) {
            binding.deviceInfoText.text = "Bluetooth permissions required for device management"
        }
    }

    private fun updatePermissionStatus(
        status: String,
        color: Int,
    ) {

        if (binding.deviceInfoText.text.toString().contains("permission", ignoreCase = true) ||
            binding.deviceInfoText.text.toString().isEmpty()
        ) {
            binding.deviceInfoText.text = status
            binding.deviceInfoText.setTextColor(color)
        }
    }

    private fun setupUI() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "GSR Recording Settings"



        setupSpinners()
        setupDeviceSpinner()
    }

    private fun setupDeviceSpinner() {

        availableDevices.clear()
        availableDevices.add("No devices found")

        deviceAdapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, availableDevices).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        binding.shimmerDeviceSpinner.adapter = deviceAdapter

        updateDeviceStatus()
    }

    private fun setupSpinners() {

        val samplingRates = arrayOf("32 Hz", "64 Hz", "128 Hz", "256 Hz", "512 Hz")
        binding.gsrSamplingRateSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, samplingRates).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        val gsrRanges = arrayOf("Auto Range", "40 kΩ - 4 MΩ", "10 kΩ - 1 MΩ", "4 kΩ - 400 kΩ")
        binding.gsrRangeSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, gsrRanges).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        val videoResolutions =
            arrayOf("4K UHD (3840×2160)", "Full HD (1920×1080)", "HD (1280×720)", "SD (720×480)")
        binding.videoResolutionSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, videoResolutions).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        val videoFrameRates = arrayOf("30 fps", "60 fps", "24 fps", "15 fps")
        binding.videoFrameRateSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, videoFrameRates).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        val rawFrameRates = arrayOf("30 fps", "15 fps", "10 fps", "5 fps", "1 fps")
        binding.rawFrameRateSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, rawFrameRates).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        val rawQualities = arrayOf("Maximum (Level 3)", "High (Level 2)", "Standard (Level 1)")
        binding.rawQualitySpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, rawQualities).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        val dataRetentions = arrayOf("Keep Forever", "30 Days", "7 Days", "24 Hours")
        binding.dataRetentionSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, dataRetentions).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        val syncTolerances = arrayOf("1 ms", "5 ms", "10 ms", "50 ms", "100 ms")
        binding.syncToleranceSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, syncTolerances).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
    }

    private fun loadCurrentSettings() {

        binding.gsrSamplingRateSpinner.setSelection(
            prefs.getInt(
                "gsr_sampling_rate",
                2
            )
        ) // Default: 128 Hz
        binding.gsrRangeSpinner.setSelection(prefs.getInt("gsr_range", 0)) // Default: Auto Range
        binding.gsrCalibrationSwitch.isChecked = prefs.getBoolean("gsr_calibration", true)

        binding.videoResolutionSpinner.setSelection(
            prefs.getInt(
                "video_resolution",
                0
            )
        ) // Default: 4K UHD
        binding.videoFrameRateSpinner.setSelection(
            prefs.getInt(
                "video_frame_rate",
                0
            )
        ) // Default: 30 fps
        binding.enableVideoSwitch.isChecked = prefs.getBoolean("enable_video", true)
        binding.enableStabilizationSwitch.isChecked = prefs.getBoolean("enable_stabilization", true)

        binding.enableRawCaptureSwitch.isChecked = prefs.getBoolean("enable_raw_capture", false)
        binding.rawFrameRateSpinner.setSelection(
            prefs.getInt(
                "raw_frame_rate",
                0
            )
        ) // Default: 30 fps
        binding.rawQualitySpinner.setSelection(prefs.getInt("raw_quality", 0)) // Default: Maximum

        binding.autoExportSwitch.isChecked = prefs.getBoolean("auto_export", false)
        binding.dataRetentionSpinner.setSelection(
            prefs.getInt(
                "data_retention",
                0
            )
        ) // Default: Keep Forever
        binding.sessionPrefixEdit.setText(prefs.getString("session_prefix", "GSR_Session"))

        binding.enableTimeSyncSwitch.isChecked = prefs.getBoolean("enable_time_sync", true)
        binding.syncToleranceSpinner.setSelection(
            prefs.getInt(
                "sync_tolerance",
                1
            )
        ) // Default: 5 ms
    }

    private fun setupListeners() {

        val saveListener = { saveCurrentSettings() }

        binding.gsrSamplingRateSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long,
                ) {
                    saveListener()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        binding.enableVideoSwitch.setOnCheckedChangeListener { _, _ -> saveListener() }
        binding.enableRawCaptureSwitch.setOnCheckedChangeListener { _, _ -> saveListener() }
        binding.autoExportSwitch.setOnCheckedChangeListener { _, _ -> saveListener() }
        binding.enableTimeSyncSwitch.setOnCheckedChangeListener { _, _ -> saveListener() }
    }

    private fun setupDeviceManagement() {

        checkAndRequestPermissions {

            initializeGSRSensorRecorder()
        }

        binding.scanDevicesButton.setOnClickListener {
            if (BluetoothPermissionUtils.hasBleScanningPermissions(this)) {
                scanForDevices()
            } else {
                checkAndRequestPermissions {
                    scanForDevices()
                }
            }
        }

        binding.connectDeviceButton.setOnClickListener {
            if (BluetoothPermissionUtils.hasBluetoothPermissions(this)) {
                connectToSelectedDevice()
            } else {
                checkAndRequestPermissions {
                    connectToSelectedDevice()
                }
            }
        }

        binding.checkPermissionsButton?.setOnClickListener {
            checkAndRequestPermissions()
        }
    }

    private fun initializeGSRSensorRecorder() {
        lifecycleScope.launch {
            try {
                gsrSensorRecorder?.initialize()
                updateDeviceStatus()
                Log.i(TAG, "GSR sensor recorder initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize GSR sensor recorder", e)
                updateDeviceStatus("Initialization Failed")
            }
        }
    }

    private fun scanForDevices() {

        if (!BluetoothPermissionUtils.hasBleScanningPermissions(this)) {
            Log.w(TAG, "Missing BLE scanning permissions")
            binding.deviceInfoText.text = "Bluetooth permissions required for device scanning"
            checkAndRequestPermissions {
                scanForDevices()
            }
            return
        }

        lifecycleScope.launch {
            try {
                binding.scanDevicesButton.isEnabled = false
                binding.scanDevicesButton.text = "Scanning..."
                binding.deviceInfoText.text = "Scanning for Shimmer devices..."

                Log.i(TAG, "Starting device scan with full permissions")
                val devices = gsrSensorRecorder?.getAvailableShimmerDevices() ?: emptyList()

                availableDevices.clear()
                if (devices.isNotEmpty()) {
                    availableDevices.addAll(devices)
                    binding.deviceInfoText.text =
                        "Found ${devices.size} device(s). Select one and tap Connect."
                    Log.i(TAG, "Found ${devices.size} Shimmer devices: $devices")
                } else {
                    availableDevices.add("No devices found")
                    binding.deviceInfoText.text =
                        "No devices found. Ensure Shimmer device is powered on and Bluetooth is enabled."
                    Log.w(TAG, "No Shimmer devices found during scan")
                }

                deviceAdapter.notifyDataSetChanged()
            } catch (e: SecurityException) {
                Log.e(
                    TAG,
                    "SecurityException during device scan - permissions may have been revoked",
                    e
                )
                binding.deviceInfoText.text =
                    "Permission error during scan. Please check app permissions."
                availableDevices.clear()
                availableDevices.add("Permission error")
                deviceAdapter.notifyDataSetChanged()

                checkAndRequestPermissions()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to scan for devices", e)
                binding.deviceInfoText.text = "Scan failed: ${e.message}"
                availableDevices.clear()
                availableDevices.add("Scan failed")
                deviceAdapter.notifyDataSetChanged()
            } finally {
                binding.scanDevicesButton.isEnabled = true
                binding.scanDevicesButton.text = "Scan Devices"
            }
        }
    }

    private fun connectToSelectedDevice() {
        val selectedDevice = binding.shimmerDeviceSpinner.selectedItem?.toString()
        if (selectedDevice.isNullOrEmpty() || selectedDevice == "No devices found" || selectedDevice == "Scan failed" || selectedDevice == "Permission error") {
            binding.deviceInfoText.text = "Please scan for devices first and select a valid device."
            return
        }

        if (!BluetoothPermissionUtils.hasBluetoothPermissions(this)) {
            Log.w(TAG, "Missing Bluetooth permissions for device connection")
            binding.deviceInfoText.text = "Bluetooth permissions required for device connection"
            checkAndRequestPermissions {
                connectToSelectedDevice()
            }
            return
        }

        lifecycleScope.launch {
            try {
                binding.connectDeviceButton.isEnabled = false
                binding.connectDeviceButton.text = "Connecting..."
                updateDeviceStatus("Connecting...")

                val deviceAddress =
                    if (selectedDevice.contains("(") && selectedDevice.contains(")")) {
                        selectedDevice.substringAfter("(").substringBefore(")")
                    } else {
                        selectedDevice // Use the full string as address if no parentheses
                    }

                Log.i(
                    TAG,
                    "Attempting to connect to device: $selectedDevice with address: $deviceAddress"
                )
                val success = gsrSensorRecorder?.connectToShimmerDevice(deviceAddress) ?: false

                if (success) {
                    updateDeviceStatus("Connected")
                    binding.deviceInfoText.text = "Successfully connected to: $selectedDevice"
                    Log.i(TAG, "Successfully connected to Shimmer device: $selectedDevice")
                } else {
                    updateDeviceStatus("Connection Failed")
                    binding.deviceInfoText.text =
                        "Failed to connect to: $selectedDevice. Check device pairing and permissions."
                    Log.w(TAG, "Failed to connect to Shimmer device: $selectedDevice")
                }
            } catch (e: SecurityException) {
                Log.e(
                    TAG,
                    "SecurityException during device connection - permissions may have been revoked",
                    e
                )
                updateDeviceStatus("Permission Error")
                binding.deviceInfoText.text =
                    "Permission error during connection. Please check app permissions."

                checkAndRequestPermissions()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to device", e)
                updateDeviceStatus("Connection Error")
                binding.deviceInfoText.text = "Connection error: ${e.message}"
            } finally {
                binding.connectDeviceButton.isEnabled = true
                binding.connectDeviceButton.text = "Connect"
            }
        }
    }

    private fun updateDeviceStatus(statusOverride: String? = null) {
        val status = statusOverride ?: gsrSensorRecorder?.getShimmerConnectionStatus() ?: "Unknown"

        binding.deviceStatusText.text = status

        val color =
            when {
                status.contains(
                    "Connected",
                    ignoreCase = true
                ) -> android.graphics.Color.parseColor("#4caf50") // Green
                status.contains(
                    "Connecting",
                    ignoreCase = true
                ) -> android.graphics.Color.parseColor("#ff9800") // Orange
                status.contains("Failed", ignoreCase = true) || status.contains(
                    "Error",
                    ignoreCase = true
                ) ->
                    android.graphics.Color.parseColor(
                        "#f44336",
                    ) // Red
                else -> android.graphics.Color.parseColor("#ffcc00") // Yellow
            }
        binding.deviceStatusText.setTextColor(color)
    }

    private fun saveCurrentSettings() {
        prefs.edit().apply {

            putInt("gsr_sampling_rate", binding.gsrSamplingRateSpinner.selectedItemPosition)
            putInt("gsr_range", binding.gsrRangeSpinner.selectedItemPosition)
            putBoolean("gsr_calibration", binding.gsrCalibrationSwitch.isChecked)

            putInt("video_resolution", binding.videoResolutionSpinner.selectedItemPosition)
            putInt("video_frame_rate", binding.videoFrameRateSpinner.selectedItemPosition)
            putBoolean("enable_video", binding.enableVideoSwitch.isChecked)
            putBoolean("enable_stabilization", binding.enableStabilizationSwitch.isChecked)

            putBoolean("enable_raw_capture", binding.enableRawCaptureSwitch.isChecked)
            putInt("raw_frame_rate", binding.rawFrameRateSpinner.selectedItemPosition)
            putInt("raw_quality", binding.rawQualitySpinner.selectedItemPosition)

            putBoolean("auto_export", binding.autoExportSwitch.isChecked)
            putInt("data_retention", binding.dataRetentionSpinner.selectedItemPosition)
            putString("session_prefix", binding.sessionPrefixEdit.text.toString())

            putBoolean("enable_time_sync", binding.enableTimeSyncSwitch.isChecked)
            putInt("sync_tolerance", binding.syncToleranceSpinner.selectedItemPosition)

            apply()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        saveCurrentSettings()
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Handle via OnBackPressedCallback instead
        onBackPressedDispatcher.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Cleanup GSR sensor recorder
        lifecycleScope.launch {
            try {
                gsrSensorRecorder?.cleanup()
            } catch (e: Exception) {
                Log.w(TAG, "Error during GSR sensor recorder cleanup", e)
            }
        }
    }
}
