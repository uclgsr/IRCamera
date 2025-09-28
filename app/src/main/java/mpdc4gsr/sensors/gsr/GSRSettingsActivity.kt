package mpdc4gsr.sensors.gsr

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityGsrSettingsBinding
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModelActivity

/**
 * GSRSettingsActivity - Phase 4 MVVM Implementation
 * Demonstrates Repository pattern with comprehensive settings management
 */
class GSRSettingsActivity : BaseViewModelActivity<GSRSettingsViewModel>() {

    companion object {
        private const val TAG = "GSRSettingsActivity"

        fun startActivity(context: Context) {
            context.startActivity(Intent(context, GSRSettingsActivity::class.java))
        }

        private fun getPermissionRationale(permission: String): String {
            return when (permission) {
                android.Manifest.permission.BLUETOOTH_SCAN -> "Scan for Bluetooth devices"
                android.Manifest.permission.BLUETOOTH_CONNECT -> "Connect to Bluetooth devices"
                android.Manifest.permission.ACCESS_FINE_LOCATION -> "Access device location for Bluetooth scanning"
                else -> "Required for Bluetooth functionality"
            }
        }
    }

    private lateinit var binding: ActivityGsrSettingsBinding
    private lateinit var deviceAdapter: ArrayAdapter<String>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    override fun providerVMClass(): Class<GSRSettingsViewModel> = GSRSettingsViewModel::class.java

    override fun initContentView() = R.layout.activity_gsr_settings

    override fun initView() {
        binding = ActivityGsrSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPermissionHandling()
        setupUI()
        setupObservers()

        viewModel.initialize(this)

        setupBackPressedCallback()
    }

    override fun initData() {
        // Initialize any data needed for the activity
        // This method is called by BaseActivity after initView()
    }

    private fun setupPermissionHandling() {
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.values.all { it }
            val deniedPermissions = permissions.filter { !it.value }.keys

            viewModel.onPermissionsResult(
                permissions.keys.toTypedArray(),
                permissions.values.map { if (it) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED }
                    .toIntArray()
            )
        }
    }

    private fun setupUI() {
        setupDeviceSpinner()
        setupSettingsControls()
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.clNavGallery.setOnClickListener {
            navigateToMainActivity(0) // Gallery page
        }
        
        binding.bottomNavigation.clNavMain.setOnClickListener {
            navigateToMainActivity(1) // Main page
        }
        
        binding.bottomNavigation.clNavMine.setOnClickListener {
            navigateToMainActivity(2) // Mine page
        }
        
        // Update navigation background to show main is selected
        binding.bottomNavigation.ivNavigationBg.setImageResource(R.drawable.ic_main_bg_select)
    }

    private fun navigateToMainActivity(pageIndex: Int) {
        val intent = Intent(this, mpdc4gsr.activities.MainActivity::class.java).apply {
            putExtra("page", pageIndex)
        }
        startActivity(intent)
        finish()
    }

    private fun setupDeviceSpinner() {
        deviceAdapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf<String>())
        deviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.shimmerDeviceSpinner?.adapter = deviceAdapter
        binding.shimmerDeviceSpinner?.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    // Handle device selection through ViewModel
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
    }

    private fun setupSettingsControls() {
        // Sampling Rate Spinner
        val samplingRates = arrayOf("32 Hz", "64 Hz", "128 Hz", "256 Hz", "512 Hz", "1024 Hz")
        val samplingAdapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, samplingRates)
        samplingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.gsrSamplingRateSpinner?.adapter = samplingAdapter

        // Data Format Spinner - commented out as it doesn't exist in current layout
        // val dataFormats = arrayOf("CSV", "JSON", "Binary")
        // val formatAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, dataFormats)
        // formatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // binding.dataFormatSpinner?.adapter = formatAdapter

        // Button listeners
        binding.scanDevicesButton?.setOnClickListener {
            viewModel.startDeviceScan()
        }

        binding.connectDeviceButton?.setOnClickListener {
            // Handle device connection
        }

        // binding.disconnectButton?.setOnClickListener {
        //     viewModel.disconnectDevice()
        // }

        // binding.exportSettingsButton?.setOnClickListener {
        //     viewModel.exportSettings()
        // }

        // binding.resetSettingsButton?.setOnClickListener {
        //     showResetConfirmationDialog()
        // }

        // binding.requestPermissionsButton?.setOnClickListener {
        //     viewModel.requestPermissions()
        // }
    }

    private fun setupObservers() {
        // Settings state observers
        viewModel.gsrSettings.observe(this) { settings ->
            updateGSRSettingsUI(settings)
        }

        viewModel.deviceSettings.observe(this) { settings ->
            updateDeviceSettingsUI(settings)
        }

        // Permission state observer
        viewModel.permissionState.observe(this) { permissionState ->
            updatePermissionUI(permissionState)
        }

        // Device connection state observer
        viewModel.deviceConnectionState.observe(this) { connectionState ->
            updateDeviceConnectionUI(connectionState)
        }

        // Available devices observer
        viewModel.availableDevices.observe(this) { devices ->
            updateDeviceList(devices)
        }

        // Scanning state observer
        viewModel.scanningState.observe(this) { scanningState ->
            updateScanningUI(scanningState)
        }

        // Error observer
        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        // Settings action observer
        viewModel.settingsAction.observe(this) { action ->
            action?.let {
                handleSettingsAction(it)
                viewModel.clearAction()
            }
        }

        // Combined UI state observer for optimization
        viewModel.uiState.observe(this) { uiState ->
            // Update UI based on combined state
            updateCombinedUI(uiState)
        }
    }

    private fun updateGSRSettingsUI(settings: GSRSettingsRepository.GSRSettings) {
        // binding.gsrEnabledSwitch?.isChecked = settings.isEnabled
        // binding.autoStartSwitch?.isChecked = settings.autoStartRecording
        // binding.realTimeMonitoringSwitch?.isChecked = settings.enableRealTimeMonitoring
        // binding.filteringEnabledSwitch?.isChecked = settings.enableFiltering
        // binding.notificationEnabledSwitch?.isChecked = settings.notificationEnabled

        // Update sampling rate spinner
        val samplingRateIndex = when (settings.samplingRate) {
            32 -> 0
            64 -> 1
            128 -> 2
            256 -> 3
            512 -> 4
            1024 -> 5
            else -> 2 // default to 128 Hz
        }
        binding.gsrSamplingRateSpinner?.setSelection(samplingRateIndex)

        // Update data format spinner - commented out as it doesn't exist in current layout
        // val formatIndex = when (settings.dataFormat) {
        //     GSRSettingsRepository.DataFormat.CSV -> 0
        //     GSRSettingsRepository.DataFormat.JSON -> 1
        //     GSRSettingsRepository.DataFormat.BINARY -> 2
        // }
        // binding.dataFormatSpinner?.setSelection(formatIndex)
    }

    private fun updateDeviceSettingsUI(settings: GSRSettingsRepository.DeviceSettings) {
        // binding.autoReconnectSwitch?.isChecked = settings.autoReconnect
        // binding.keepConnectedSwitch?.isChecked = settings.keepDeviceConnected
        binding.gsrCalibrationSwitch?.isChecked = settings.deviceCalibrationEnabled

        // Update selected device display - commented out as it doesn't exist in current layout
        // binding.selectedDeviceText?.text = settings.deviceName ?: "No device selected"
    }

    private fun updatePermissionUI(permissionState: GSRSettingsViewModel.PermissionState) {
        val status = if (permissionState.hasAllPermissions) {
            "All Required Permissions Granted"
        } else {
            "Missing Required Permissions (${permissionState.missingPermissions.size})"
        }

        // binding.permissionStatusText?.text = status
        // binding.permissionStatusText?.setTextColor(
        //     if (permissionState.hasAllPermissions) {
        //         android.graphics.Color.parseColor("#4caf50") // Green
        //     } else {
        //         android.graphics.Color.parseColor("#f44336") // Red
        //     }
        // )

        // binding.requestPermissionsButton?.isVisible = !permissionState.hasAllPermissions
        // binding.deviceManagementLayout?.isVisible = permissionState.hasAllPermissions
    }

    private fun updateDeviceConnectionUI(connectionState: GSRSettingsViewModel.DeviceConnectionState) {
        // binding.connectionStatusText?.text = connectionState.connectionStatus
        binding.connectDeviceButton?.isVisible = !connectionState.isConnected
        // binding.disconnectButton?.isVisible = connectionState.isConnected

        connectionState.deviceInfo?.let { device ->
            // binding.connectedDeviceText?.text = "Connected: ${device.name}"
            // binding.signalStrengthText?.text = "Signal: ${connectionState.signalStrength}%"
        }
    }

    private fun updateDeviceList(devices: List<GSRSettingsViewModel.DeviceInfo>) {
        deviceAdapter.clear()
        deviceAdapter.addAll(devices.map { "${it.name} (${it.address})" })
        deviceAdapter.notifyDataSetChanged()

        // binding.deviceCountText?.text = "Found ${devices.size} device(s)"
    }

    private fun updateScanningUI(scanningState: GSRSettingsViewModel.ScanningState) {
        binding.scanDevicesButton?.isEnabled =
            scanningState != GSRSettingsViewModel.ScanningState.SCANNING
        // binding.scanProgressBar?.isVisible = scanningState == GSRSettingsViewModel.ScanningState.SCANNING

        // binding.scanStatusText?.text = when (scanningState) {
        //     GSRSettingsViewModel.ScanningState.IDLE -> "Ready to scan"
        //     GSRSettingsViewModel.ScanningState.SCANNING -> "Scanning for devices..."
        //     GSRSettingsViewModel.ScanningState.COMPLETED -> "Scan completed"
        //     GSRSettingsViewModel.ScanningState.FAILED -> "Scan failed"
        // }
    }

    private fun updateCombinedUI(uiState: GSRSettingsViewModel.UIState) {
        // Update UI elements that depend on multiple state components
        val isFullyConfigured = uiState.gsrSettings.isEnabled &&
                uiState.deviceSettings.selectedDeviceId != null

        // binding.readyIndicator?.isVisible = isFullyConfigured
        // binding.configurationStatusText?.text = if (isFullyConfigured) {
        //     "GSR system ready for recording"
        // } else {
        //     "Configuration incomplete"
        // }
    }

    private fun handleSettingsAction(action: GSRSettingsViewModel.SettingsAction) {
        when (action.type) {
            GSRSettingsViewModel.ActionType.SHOW_PERMISSION_DIALOG -> {
                val permissions = action.data as? List<String> ?: return
                showPermissionDialog(permissions)
            }

            GSRSettingsViewModel.ActionType.SHOW_PERMISSION_DENIED_DIALOG -> {
                val permissions = action.data as? List<String> ?: return
                showPermissionDeniedDialog(permissions)
            }

            GSRSettingsViewModel.ActionType.SHOW_PERMISSION_PERMANENTLY_DENIED_DIALOG -> {
                val permissions = action.data as? List<String> ?: return
                showPermissionPermanentlyDeniedDialog(permissions)
            }

            GSRSettingsViewModel.ActionType.OPEN_APP_SETTINGS -> {
                openAppSettings()
            }

            GSRSettingsViewModel.ActionType.DEVICE_SCAN_COMPLETED -> {
                Toast.makeText(this, action.message, Toast.LENGTH_SHORT).show()
            }

            GSRSettingsViewModel.ActionType.DEVICE_CONNECTED -> {
                Toast.makeText(this, action.message, Toast.LENGTH_SHORT).show()
            }

            GSRSettingsViewModel.ActionType.DEVICE_DISCONNECTED -> {
                Toast.makeText(this, action.message, Toast.LENGTH_SHORT).show()
            }

            GSRSettingsViewModel.ActionType.SETTINGS_EXPORTED -> {
                Toast.makeText(this, action.message, Toast.LENGTH_SHORT).show()
            }

            GSRSettingsViewModel.ActionType.SETTINGS_IMPORTED -> {
                Toast.makeText(this, action.message, Toast.LENGTH_SHORT).show()
            }

            else -> {
                // Handle other actions
            }
        }
    }

    private fun showPermissionDialog(missingPermissions: List<String>) {
        val permissionDescriptions = missingPermissions.map { permission ->
            "• ${getPermissionRationale(permission)}"
        }.joinToString("\n")

        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("GSR functionality requires the following permissions:\n\n$permissionDescriptions")
            .setPositiveButton("Grant Permissions") { _, _ ->
                permissionLauncher.launch(missingPermissions.toTypedArray())
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(false)
            .show()
    }

    private fun showPermissionDeniedDialog(deniedPermissions: List<String>) {
        val permissionDescriptions = deniedPermissions.map { permission ->
            "• ${getPermissionRationale(permission)}"
        }.joinToString("\n")

        AlertDialog.Builder(this)
            .setTitle("Permissions Denied")
            .setMessage("The following permissions were denied:\n\n$permissionDescriptions\n\nWithout these permissions, GSR functionality will be limited.")
            .setPositiveButton("Try Again") { _, _ ->
                viewModel.requestPermissions()
            }
            .setNegativeButton("Continue Without", null)
            .show()
    }

    private fun showPermissionPermanentlyDeniedDialog(deniedPermissions: List<String>) {
        val permissionDescriptions = deniedPermissions.map { permission ->
            "• ${getPermissionRationale(permission)}"
        }.joinToString("\n")

        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("The following permissions are required:\n\n$permissionDescriptions\n\nTo enable GSR functionality, please grant these permissions in app settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Continue Without", null)
            .show()
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
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

    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Reset Settings")
            .setMessage("Are you sure you want to reset all GSR settings to defaults? This action cannot be undone.")
            .setPositiveButton("Reset") { _, _ ->
                viewModel.resetToDefaults()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(
            this,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Settings are automatically saved via Repository pattern
                    finish()
                }
            }
        )
    }
}
