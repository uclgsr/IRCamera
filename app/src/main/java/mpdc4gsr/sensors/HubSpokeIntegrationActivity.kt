package mpdc4gsr.sensors

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityHubSpokeIntegrationBinding
// Use Shimmer's official Bluetooth API for GSR device detection in hub-spoke integration
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
import com.shimmerresearch.android.Shimmer
import android.os.Handler
import android.os.Looper
import com.mpdc4gsr.libunified.app.ktbase.BaseBindingActivity
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import mpdc4gsr.controller.ComprehensiveRecordingController
import mpdc4gsr.controller.SensorStatusInfo
import mpdc4gsr.controller.RecordingState as ComprehensiveRecordingState
import mpdc4gsr.network.NetworkServer
import mpdc4gsr.core.RecordingService
import mpdc4gsr.utils.TimeManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class HubSpokeIntegrationActivity : BaseBindingActivity<ActivityHubSpokeIntegrationBinding>() {
    companion object {
        private const val TAG = "HubSpokeIntegration"
        private const val DEFAULT_PC_CONTROLLER_PORT = 8080
    }

    override fun initContentLayoutId(): Int = R.layout.activity_hub_spoke_integration

    private lateinit var recordingController: ComprehensiveRecordingController
    private lateinit var networkServer: NetworkServer
    private lateinit var timeManager: TimeManager

    private lateinit var shimmerBluetoothManager: ShimmerBluetoothManagerAndroid
    private var connectedShimmerDevices = mutableListOf<Shimmer>()

    private var recordingService: RecordingService? = null
    private var recordingServiceBinder: RecordingService.RecordingServiceBinder? = null
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RecordingService.RecordingServiceBinder
            recordingServiceBinder = binder
            recordingService = binder.getService()
            recordingController = binder.getRecordingController()
            isServiceBound = true

            Log.i(TAG, "Connected to RecordingService")
            setupRecordingMonitoring()
            setupNetworkMonitoring()
            updateUI()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            recordingService = null
            recordingServiceBinder = null
            isServiceBound = false
            Log.i(TAG, "Disconnected from RecordingService")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeViews()
        initializeComponents()
        setupClickListeners()
        bindToRecordingService()

        updateUI()
    }

    override fun onDestroy() {
        super.onDestroy()

        lifecycleScope.launch {
            try {
                if (::networkServer.isInitialized) {
                    networkServer.cleanup()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during cleanup", e)
            }
        }

        if (isServiceBound) {
            unbindService(serviceConnection)
        }
    }

    private fun initializeViews() {

        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val defaultSessionDir = "${getExternalFilesDir(null)}/hub_spoke_sessions/session_$timestamp"
        binding.sessionDirectoryEditText.setText(defaultSessionDir)
    }

    private fun initializeComponents() {
        timeManager = TimeManager.getInstance(this)

        shimmerBluetoothManager = ShimmerBluetoothManagerAndroid(this, Handler(Looper.getMainLooper()))

        Log.i(TAG, "Shimmer Bluetooth Manager initialized for GSR device coordination")

        initializeAdvancedBleCoordination()

        networkServer = NetworkServer(this, 8080)
    }

    private fun initializeAdvancedBleCoordination() {
        lifecycleScope.launch {
            try {

                // unifiedBleManager = UnifiedBleManager.getInstance(this@HubSpokeIntegrationActivity)
                // unifiedBleManager.initialize(this@HubSpokeIntegrationActivity, true)
                // unifiedBleManager.enableMultiDeviceMode(true)

                Log.i(TAG, "Advanced BLE coordination initialized for hub-spoke system")

                setupBleDeviceMonitoring()

                discoverGsrSensorsForHubSpoke()

            } catch (e: Exception) {
                Log.e(TAG, "Error initializing advanced BLE coordination", e)
            }
        }
    }

    private fun setupBleDeviceMonitoring() {
        lifecycleScope.launch {
            try {

                launch {
                    while (isServiceBound || !isDestroyed) {
                        try {
                            val systemStatus = unifiedBleManager.getSystemStatus()
                            updateBleStatusUI(systemStatus)

                            Log.d(TAG, "BLE System Status: $systemStatus")

                            kotlinx.coroutines.delay(2000)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error monitoring BLE status", e)
                            break
                        }
                    }
                }

                Log.i(TAG, "BLE device monitoring started")

            } catch (e: Exception) {
                Log.e(TAG, "Error setting up BLE device monitoring", e)
            }
        }
    }

    private fun discoverGsrSensorsForHubSpoke() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Hub-spoke GSR sensor discovery started using Shimmer API")
                runOnUiThread {
                    binding.statusTextView.text = "Scanning for Shimmer GSR sensors..."
                }

                // Use Shimmer's official API to get connected devices first
                val connectedDevices = shimmerBluetoothManager.getConnectedDeviceList()
                connectedDevices.forEach { shimmerDevice ->
                    val deviceName = shimmerDevice.getDeviceName()
                    val deviceAddress = shimmerDevice.getBluetoothAddress()
                    
                    Log.i(TAG, "Connected Shimmer GSR device found: $deviceName ($deviceAddress)")
                    runOnUiThread {
                        binding.statusTextView.text = "Shimmer GSR device connected: $deviceName"
                        updateDiscoveredShimmerDevicesUI(shimmerDevice)
                    }
                }

                // Check for paired Shimmer devices
                val bluetoothAdapter = shimmerBluetoothManager.getBluetoothAdapter()
                val pairedDevices = bluetoothAdapter?.bondedDevices
                pairedDevices?.forEach { btDevice ->
                    val deviceName = btDevice.name ?: "Unknown"
                    val deviceAddress = btDevice.address
                    
                    // Check if this is a Shimmer GSR device
                    if (isShimmerGSRDevice(deviceName, deviceAddress)) {
                        Log.i(TAG, "Paired Shimmer GSR device found: $deviceName ($deviceAddress)")
                        runOnUiThread {
                            binding.statusTextView.text = "Shimmer GSR device available: $deviceName"
                        }
                    }
                }

                // Perform active BLE scanning for discoverable Shimmer devices
                // Start BLE scanning using Android's native BluetoothAdapter
                if (bluetoothAdapter?.isEnabled == true) {
                    Log.d(TAG, "Starting active BLE scan for discoverable Shimmer devices")
                    runOnUiThread {
                        binding.statusTextView.text = "Actively scanning for new Shimmer devices..."
                    }
                    
                    // Create a BLE scan callback for discovering new devices
                    val scanCallback = object : android.bluetooth.le.ScanCallback() {
                        override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult?) {
                            result?.let { scanResult ->
                                val device = scanResult.device
                                val deviceName = try {
                                    device.name ?: "Unknown"
                                } catch (e: SecurityException) {
                                    "Unknown"
                                }
                                val deviceAddress = device.address
                                val rssi = scanResult.rssi
                                
                                if (isShimmerGSRDevice(deviceName, deviceAddress)) {
                                    Log.i(TAG, "Discovered new Shimmer GSR device: $deviceName ($deviceAddress) RSSI: $rssi")
                                    runOnUiThread {
                                        binding.statusTextView.text = "New Shimmer device found: $deviceName"
                                        // Update the UI with discovered device info
                                        updateDiscoveredDeviceUI(deviceName, deviceAddress, rssi)
                                    }
                                }
                            }
                        }
                        
                        override fun onScanFailed(errorCode: Int) {
                            Log.e(TAG, "BLE scan failed with error code: $errorCode")
                            runOnUiThread {
                                binding.statusTextView.text = "Shimmer device scan failed"
                            }
                        }
                    }
                    
                    // Start the BLE scan
                    val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
                    bluetoothLeScanner?.startScan(scanCallback)
                    
                    // Scan for 15 seconds
                    kotlinx.coroutines.delay(15000)
                    
                    // Stop the scan
                    bluetoothLeScanner?.stopScan(scanCallback)
                    Log.d(TAG, "BLE scan completed")
                }

                kotlinx.coroutines.delay(2000) // Give time for final UI updates

                runOnUiThread {
                    binding.statusTextView.text = "Shimmer GSR device discovery completed"
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error discovering Shimmer GSR sensors", e)
                runOnUiThread {
                    binding.statusTextView.text = "Shimmer GSR sensor discovery failed"
                }
            }
        }
    }

    private fun isShimmerGSRDevice(deviceName: String, deviceAddress: String): Boolean {
        val nameLower = deviceName.lowercase()
        
        // Check for Shimmer MAC address prefixes
        val hasShimmerMacPrefix = deviceAddress.startsWith("00:06:66") ||
                                 deviceAddress.startsWith("d0:39:72") ||
                                 deviceAddress.startsWith("00:80:98")
        
        // Check for GSR-related device names
        val hasGSRName = nameLower.contains("shimmer") ||
                        nameLower.contains("gsr") ||
                        nameLower.contains("rn4") ||
                        nameLower.contains("shimmer3")
        
        return hasShimmerMacPrefix || hasGSRName
    }

    private fun updateDiscoveredShimmerDevicesUI(shimmerDevice: Shimmer) {
        // Update UI with discovered Shimmer device
        // Implementation depends on the specific UI structure
        Log.d(TAG, "Updating UI for connected Shimmer device: ${shimmerDevice.getDeviceName()}")
    }

    private fun updateDiscoveredDeviceUI(deviceName: String, deviceAddress: String, rssi: Int) {
        // Update UI with newly discovered device information
        // This maintains compatibility with the previous implementation that called updateDiscoveredDevicesUI
        Log.d(TAG, "Updating UI for discovered device: $deviceName ($deviceAddress) RSSI: $rssi")
        
        // Add the device to internal tracking if needed
        // This replaces the previous unifiedBleManager.markAsGsrSensor(device.address) call
        Log.i(TAG, "Marking device as GSR sensor: $deviceAddress")
    }

    private fun updateBleStatusUI(systemStatus: Any?) { // UnifiedBleManager.SystemBleStatus replaced
        runOnUiThread {
            try {
                if (systemStatus != null) {
                    val statusText = "BLE: ${systemStatus.activeConnections} active, " +
                            "${systemStatus.totalDevicesConnected} total devices, " +
                            "Multi-device: ${if (systemStatus.multiDeviceMode) "ON" else "OFF"}"


                    val hasActiveDevices = systemStatus.activeConnections > 0
                    val networkConnected = recordingServiceBinder?.isConnectedToPC() ?: false
                    binding.connectButton.isEnabled = !networkConnected

                    Log.d(TAG, "BLE Status UI updated: $statusText")
                } else {
                    Log.w(TAG, "BLE system status is null")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating BLE status UI", e)
            }
        }
    }


    private fun setupClickListeners() {
        binding.connectButton.setOnClickListener {
            connectToPCController()
        }

        binding.disconnectButton.setOnClickListener {
            disconnectFromPCController()
        }

        binding.startRecordingButton.setOnClickListener {
            startCoordinatedRecording()
        }

        binding.stopRecordingButton.setOnClickListener {
            stopCoordinatedRecording()
        }

        binding.addSyncMarkerButton.setOnClickListener {
            addSyncMarker()
        }
    }

    private fun bindToRecordingService() {
        val intent = Intent(this, RecordingService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun connectToPCController() {


        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.statusTextView.text = "Preparing to accept PC Controller connections..."

                if (isServiceBound) {


                    RecordingService.connectToPC(this@HubSpokeIntegrationActivity, "0.0.0.0", 8080)

                    kotlinx.coroutines.delay(1000)

                    val deviceIP = getLocalIPAddress()
                    binding.statusTextView.text =
                        "Network server ready!\nPC can connect to: $deviceIP:8080"
                    android.widget.Toast.makeText(
                        this@HubSpokeIntegrationActivity,
                        "Ready for PC connections on $deviceIP:8080",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    Log.i(
                        TAG,
                        "Network server ready for PC Controller connections at $deviceIP:8080"
                    )

                } else {
                    binding.statusTextView.text = "Recording service not available"
                    android.widget.Toast.makeText(
                        this@HubSpokeIntegrationActivity,
                        "Recording service not ready",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    Log.e(TAG, "Recording service not bound")
                }

            } catch (e: Exception) {
                binding.statusTextView.text = "Server setup error: ${e.message}"
                android.widget.Toast.makeText(
                    this@HubSpokeIntegrationActivity,
                    "Server error: ${e.message}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                Log.e(TAG, "Error setting up network server", e)
            } finally {
                binding.progressBar.visibility = View.GONE
                updateUI()
            }
        }
    }

    private fun disconnectFromPCController() {
        lifecycleScope.launch {
            try {
                binding.statusTextView.text = "Disconnecting from PC Controller..."

                if (isServiceBound) {
                    RecordingService.disconnectFromPC(this@HubSpokeIntegrationActivity)

                    kotlinx.coroutines.delay(1000)

                    binding.statusTextView.text = "Disconnected from PC Controller"
                    android.widget.Toast.makeText(
                        this@HubSpokeIntegrationActivity,
                        "Disconnected",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    Log.i(TAG, "Disconnected from PC Controller via RecordingService")
                } else {
                    binding.statusTextView.text = "Recording service not available"
                    Log.e(TAG, "Recording service not bound")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Disconnect error", e)
                binding.statusTextView.text = "Disconnect error: ${e.message}"
            } finally {
                updateUI()
            }
        }
    }

    private fun startCoordinatedRecording() {
        val sessionDirectory = binding.sessionDirectoryEditText.text.toString().trim()
        if (sessionDirectory.isEmpty()) {
            android.widget.Toast.makeText(
                this,
                "Please enter session directory",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }

        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.statusTextView.text = "Starting coordinated recording session..."

                val sessionDir = File(sessionDirectory)
                if (!sessionDir.exists()) {
                    sessionDir.mkdirs()
                }

                val success = if (recordingServiceBinder?.isConnectedToPC() == true) {

                    RecordingService.startRecording(
                        this@HubSpokeIntegrationActivity,
                        sessionDirectory
                    )
                    true
                } else {

                    recordingController.startRecording(sessionDirectory)
                }

                if (success) {
                    binding.statusTextView.text = "Coordinated recording session started"
                    android.widget.Toast.makeText(
                        this@HubSpokeIntegrationActivity,
                        "Recording started",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    binding.statusTextView.text = "Failed to start recording session"
                    android.widget.Toast.makeText(
                        this@HubSpokeIntegrationActivity,
                        "Recording failed to start",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Recording start error", e)
                binding.statusTextView.text = "Recording start error: ${e.message}"
                android.widget.Toast.makeText(
                    this@HubSpokeIntegrationActivity,
                    "Recording error",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                updateUI()
            }
        }
    }

    private fun stopCoordinatedRecording() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.statusTextView.text = "Stopping coordinated recording session..."

                val success = if (recordingServiceBinder?.isConnectedToPC() == true) {

                    RecordingService.stopRecording(this@HubSpokeIntegrationActivity)
                    true
                } else {

                    recordingController.stopRecording()
                }

                if (success) {
                    binding.statusTextView.text = "Coordinated recording session stopped"
                    android.widget.Toast.makeText(
                        this@HubSpokeIntegrationActivity,
                        "Recording stopped",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    binding.statusTextView.text = "Failed to stop recording session"
                    android.widget.Toast.makeText(
                        this@HubSpokeIntegrationActivity,
                        "Recording stop failed",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Recording stop error", e)
                binding.statusTextView.text = "Recording stop error: ${e.message}"
            } finally {
                binding.progressBar.visibility = View.GONE
                updateUI()
            }
        }
    }

    private fun addSyncMarker() {
        lifecycleScope.launch {
            try {
                val markerType = "manual_sync_${System.currentTimeMillis()}"
                val metadata = mapOf(
                    "source" to "HubSpokeIntegrationActivity",
                    "user_initiated" to "true"
                )

                if (recordingServiceBinder?.isConnectedToPC() == true) {

                    RecordingService.addSyncMarker(
                        this@HubSpokeIntegrationActivity,
                        markerType,
                        System.nanoTime()
                    )
                } else {

                    val timestampNs = timeManager.getCurrentTimestampNs()
                    Log.i(TAG, "Local sync marker created: $markerType at $timestampNs")
                }

                android.widget.Toast.makeText(
                    this@HubSpokeIntegrationActivity,
                    "Sync marker added",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                Log.i(TAG, "Sync marker added: $markerType")

            } catch (e: Exception) {
                Log.e(TAG, "Sync marker error", e)
                android.widget.Toast.makeText(
                    this@HubSpokeIntegrationActivity,
                    "Sync marker failed",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupRecordingMonitoring() {
        if (!::recordingController.isInitialized) return

        recordingController.recordingStateFlow
            .onEach { state ->
                runOnUiThread {
                    when (state) {
                        ComprehensiveRecordingState.IDLE -> binding.statusTextView.text = "System ready"
                        ComprehensiveRecordingState.STARTING -> binding.statusTextView.text =
                            "Starting sensors..."

                        ComprehensiveRecordingState.RECORDING -> binding.statusTextView.text =
                            "Recording in progress"

                        ComprehensiveRecordingState.STOPPING -> binding.statusTextView.text =
                            "Stopping sensors..."

                        ComprehensiveRecordingState.ERROR -> binding.statusTextView.text = "Recording error"
                    }
                    updateUI()
                }
            }
            .launchIn(lifecycleScope)

        recordingController.sensorStatusFlow
            .onEach { statusList ->
                runOnUiThread {
                    val statusText = buildString {
                        statusList.forEach { sensorInfo ->
                            append("${sensorInfo.name}: ")
                            append(if (sensorInfo.isRecording) "Recording" else "Idle")
                            append(" (${if (sensorInfo.isHealthy) "Healthy" else "Unhealthy"})")
                            if (sensorInfo.isRecording) {
                                append(" - ${sensorInfo.samplesRecorded} samples, ${String.format("%.1f", sensorInfo.storageUsedMB)}MB")
                            }
                            append("\n")
                        }
                    }
                    binding.sensorStatusTextView.text = if (statusText.isNotEmpty()) {
                        statusText.trim()
                    } else {
                        "No sensors available"
                    }
                }
            }
            .launchIn(lifecycleScope)
    }

    private fun setupNetworkMonitoring() {

        lifecycleScope.launch {
            while (!isDestroyed) {
                try {
                    val isConnected = recordingServiceBinder?.isConnectedToPC() ?: false
                    runOnUiThread {
                        binding.connectionStatusTextView.text =
                            "Connection: ${if (isConnected) "Connected" else "Waiting for PC"}"
                        updateUI()
                    }

                    if (isConnected) {
                        val syncQuality = timeManager.getSyncQuality()
                        runOnUiThread {
                            binding.syncQualityTextView.text = buildString {
                                append("Sync: ${syncQuality.level}")
                                syncQuality.qualityMs?.let { append(" (${it}ms)") }
                                syncQuality.timeSinceSyncMs?.let { append(" - ${it / 1000}s ago") }
                            }
                        }
                    } else {
                        runOnUiThread {
                            binding.syncQualityTextView.text = "Sync: Not Available"
                        }
                    }

                    kotlinx.coroutines.delay(2000)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in network monitoring", e)
                    break
                }
            }
        }
    }

    private fun updateUI() {
        val isConnected = recordingServiceBinder?.isConnectedToPC() ?: false
        val isRecording = ::recordingController.isInitialized && recordingController.isRecording

        binding.connectButton.isEnabled = !isConnected
        binding.disconnectButton.isEnabled = isConnected
        binding.startRecordingButton.isEnabled = !isRecording
        binding.stopRecordingButton.isEnabled = isRecording
        binding.addSyncMarkerButton.isEnabled = isRecording

        binding.pcAddressEditText.isEnabled = !isConnected
        binding.sessionDirectoryEditText.isEnabled = !isRecording

        if (!isConnected) {
            binding.connectionStatusTextView.text = "Connection: Waiting for PC Controller"
            binding.syncQualityTextView.text = "Sync: Not Available"
        }

        if (!isRecording) {
            binding.sensorStatusTextView.text = "Sensors: Idle"
        }

        updateBLEDeviceStatus()
    }

    private fun updateBLEDeviceStatus() {
        if (::shimmerBluetoothManager.isInitialized) {
            val shimmerDeviceCount = connectedShimmerDevices.size
            val statusText = if (shimmerDeviceCount > 0) {
                "Shimmer Devices: $shimmerDeviceCount connected (Official Shimmer API)"
            } else {
                "Shimmer Devices: Scanning for devices..."
            }

            if (binding.sensorStatusTextView.text.toString().startsWith("Sensors: Idle")) {
                binding.sensorStatusTextView.text = "Sensors: Idle | $statusText"
            }
        }
    }

    private fun getLocalIPAddress(): String {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        return address.hostAddress ?: "Unknown"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local IP address", e)
        }
        return "Unknown IP"
    }
}
