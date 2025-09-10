package com.topdon.tc001.sensors

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.topdon.tc001.controller.RecordingController
import com.topdon.tc001.controller.RecordingState
import com.topdon.tc001.network.EnhancedNetworkClient
import com.topdon.tc001.service.RecordingService
import com.topdon.tc001.utils.TimeManager
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityHubSpokeIntegrationBinding
import com.topdon.lib.core.ktbase.BaseBindingActivity

// Enhanced BLE Module integration for systematic harmonization
import com.topdon.ble.EasyBLE
import com.topdon.ble.Device
import com.topdon.ble.ConnectionState
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Hub-Spoke Integration Activity demonstrating the complete Multi-Modal Physiological Sensing Platform.
 * 
 * This activity provides a comprehensive interface for:
 * - PC Controller discovery and connection
 * - Multi-modal sensor recording coordination
 * - Real-time status monitoring and error handling
 * - Time synchronization visualization
 * - Background service management
 * 
 * Features:
 * - Complete Hub-Spoke system demonstration
 * - Real-time sensor status display
 * - Network connectivity and sync quality monitoring
 * - Coordinated recording session management
 * - Error recovery and status reporting
 * 
 * @author IRCamera Android Sensor Node (Spoke)
 */
class HubSpokeIntegrationActivity : BaseBindingActivity<ActivityHubSpokeIntegrationBinding>() {

    companion object {
        private const val TAG = "HubSpokeIntegration"
        private const val DEFAULT_PC_CONTROLLER_PORT = 8080
    }

    override fun initContentLayoutId(): Int = R.layout.activity_hub_spoke_integration

    // Core components
    private lateinit var recordingController: RecordingController
    private lateinit var networkClient: EnhancedNetworkClient
    private lateinit var timeManager: TimeManager
    
    // Enhanced BLE Module for systematic harmonization
    private lateinit var enhancedBLE: EasyBLE
    private lateinit var unifiedBleManager: com.topdon.ble.UnifiedBleManager
    private var connectedBLEDevices = mutableListOf<Device>()
    
    // Service connection
    private var recordingService: RecordingService? = null
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RecordingService.RecordingServiceBinder
            recordingService = binder.getService()
            recordingController = binder.getService().getRecordingController()
            isServiceBound = true
            
            Log.i(TAG, "Connected to RecordingService")
            setupRecordingMonitoring()
            updateUI()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            recordingService = null
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
                if (::networkClient.isInitialized) {
                    networkClient.cleanup()
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
        // Set default session directory using binding
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val defaultSessionDir = "${getExternalFilesDir(null)}/hub_spoke_sessions/session_$timestamp"
        binding.sessionDirectoryEditText.setText(defaultSessionDir)
    }

    private fun initializeComponents() {
        timeManager = TimeManager.getInstance(this)
        
        // Initialize Enhanced BLE Module with Nordic backend for systematic harmonization
        enhancedBLE = EasyBLE.getBuilder()
            .setUseNordicBleBackend(true) // Enable Nordic BLE for enhanced reliability
            .build()
        
        Log.i(TAG, "Enhanced BLE Module initialized with Nordic BLE backend")
        
        // Initialize Enhanced BLE Manager for advanced multi-device coordination
        initializeAdvancedBleCoordination()
        
        // Initialize network client (will be connected to service later)
        recordingController = RecordingController(this, this)
        networkClient = EnhancedNetworkClient(this, recordingController)
    }
    
    /**
     * Initialize advanced BLE coordination for systematic multi-device management
     */
    private fun initializeAdvancedBleCoordination() {
        lifecycleScope.launch {
            try {
                // Initialize Unified BLE Manager with multi-device coordination
                unifiedBleManager = com.topdon.ble.UnifiedBleManager.getInstance(this@HubSpokeIntegrationActivity)
                unifiedBleManager.initialize(this@HubSpokeIntegrationActivity, true)
                unifiedBleManager.enableMultiDeviceMode(true)
                
                Log.i(TAG, "Advanced BLE coordination initialized for hub-spoke system")
                
                // Setup BLE device monitoring for real-time status updates
                setupBleDeviceMonitoring()
                
                // Auto-discover and setup GSR sensors for physiological sensing
                discoverGsrSensorsForHubSpoke()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing advanced BLE coordination", e)
            }
        }
    }
    
    /**
     * Setup BLE device monitoring with system-wide status tracking
     */
    private fun setupBleDeviceMonitoring() {
        lifecycleScope.launch {
            try {
                // Monitor system BLE status and update UI
                launch {
                    while (isServiceBound || !isDestroyed) {
                        try {
                            val systemStatus = unifiedBleManager.getSystemStatus()
                            updateBleStatusUI(systemStatus)
                            
                            // Log system status for debugging
                            Log.d(TAG, "BLE System Status: $systemStatus")
                            
                            kotlinx.coroutines.delay(2000) // Update every 2 seconds
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
    
    /**
     * Discover and setup GSR sensors for hub-spoke physiological sensing
     */
    private fun discoverGsrSensorsForHubSpoke() {
        lifecycleScope.launch {
            try {
                // Start BLE device discovery to find available GSR sensors
                enhancedBLE.addScanListener(object : com.topdon.ble.callback.ScanListener {
                    override fun onScanStart() {
                        Log.d(TAG, "Hub-spoke GSR sensor discovery started")
                        runOnUiThread {
                            binding.statusTextView.text = "Scanning for GSR sensors..."
                        }
                    }
                    
                    override fun onScanStop() {
                        Log.d(TAG, "Hub-spoke GSR sensor discovery stopped")
                    }
                    
                    override fun onScanResult(device: Device, isConnectedBySys: Boolean) {
                        // Check if device is a GSR sensor (Shimmer3 GSR+)
                        val deviceName = device.name?.uppercase() ?: ""
                        if (deviceName.contains("SHIMMER") || deviceName.contains("GSR")) {
                            Log.i(TAG, "GSR sensor detected for hub-spoke: ${device.name} (${device.address})")
                            
                            // Mark as GSR sensor for enhanced handling
                            unifiedBleManager.markAsGsrSensor(device.address)
                            
                            runOnUiThread {
                                binding.statusTextView.text = "GSR sensor found: ${device.name}"
                                updateDiscoveredDevicesUI(device, device.getRssi())
                            }
                        }
                    }
                    
                    override fun onScanError(errorCode: Int, errorMsg: String?) {
                        Log.e(TAG, "Hub-spoke GSR sensor discovery failed: $errorCode, message: $errorMsg")
                        runOnUiThread {
                            binding.statusTextView.text = "GSR sensor discovery failed"
                        }
                    }
                })
                
                // Start scanning for a limited time
                enhancedBLE.startScan()
                
                // Stop scanning after 30 seconds
                kotlinx.coroutines.delay(30000)
                enhancedBLE.stopScan()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error discovering GSR sensors", e)
            }
        }
    }
    
    /**
     * Update BLE status in the UI with enhanced system information
     */
    private fun updateBleStatusUI(systemStatus: com.topdon.ble.UnifiedBleManager.SystemBleStatus?) {
        runOnUiThread {
            try {
                if (systemStatus != null) {
                    val statusText = "BLE: ${systemStatus.activeConnections} active, " +
                            "${systemStatus.totalDevicesConnected} total devices, " +
                            "Multi-device: ${if (systemStatus.multiDeviceMode) "ON" else "OFF"}"
                    
                    // Update BLE status display (assuming there's a BLE status TextView)
                    // binding.bleStatusTextView.text = statusText
                    
                    // Update connection indicator based on active connections
                    val hasActiveDevices = systemStatus.activeConnections > 0
                    binding.connectButton.isEnabled = !hasActiveDevices || !networkClient.isConnected()
                    
                    Log.d(TAG, "BLE Status UI updated: $statusText")
                } else {
                    Log.w(TAG, "BLE system status is null")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating BLE status UI", e)
            }
        }
    }
    
    /**
     * Update discovered devices UI with real-time device information
     */
    private fun updateDiscoveredDevicesUI(device: Device, rssi: Int) {
        try {
            // Add device to the connected devices list
            if (!connectedBLEDevices.any { it.address == device.address }) {
                connectedBLEDevices.add(device)
                Log.i(TAG, "Added discovered BLE device: ${device.name} (${device.address})")
            }
            
            // Update device count display
            val deviceCountText = "Discovered BLE devices: ${connectedBLEDevices.size}"
            // binding.deviceCountTextView.text = deviceCountText
            
            Log.d(TAG, deviceCountText)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating discovered devices UI", e)
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
        val pcAddress = binding.pcAddressEditText.text.toString().trim()
        if (pcAddress.isEmpty()) {
            android.widget.Toast.makeText(this, "Please enter PC Controller IP address", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.statusTextView.text = "Connecting to PC Controller..."
                
                val connected = networkClient.connectToController(pcAddress, DEFAULT_PC_CONTROLLER_PORT)
                
                if (connected) {
                    binding.statusTextView.text = "Connected to PC Controller successfully"
                    android.widget.Toast.makeText(this@HubSpokeIntegrationActivity, "Connected successfully", android.widget.Toast.LENGTH_SHORT).show()
                    setupNetworkMonitoring()
                } else {
                    binding.statusTextView.text = "Failed to connect to PC Controller"
                    android.widget.Toast.makeText(this@HubSpokeIntegrationActivity, "Connection failed", android.widget.Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Connection error", e)
                binding.statusTextView.text = "Connection error: ${e.message}"
                android.widget.Toast.makeText(this@HubSpokeIntegrationActivity, "Connection error", android.widget.Toast.LENGTH_SHORT).show()
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
                networkClient.disconnect()
                binding.statusTextView.text = "Disconnected from PC Controller"
                android.widget.Toast.makeText(this@HubSpokeIntegrationActivity, "Disconnected", android.widget.Toast.LENGTH_SHORT).show()
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
            android.widget.Toast.makeText(this, "Please enter session directory", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.statusTextView.text = "Starting coordinated recording session..."
                
                // Create session directory
                val sessionDir = File(sessionDirectory)
                if (!sessionDir.exists()) {
                    sessionDir.mkdirs()
                }
                
                val success = if (networkClient.isConnected()) {
                    // Start coordinated session with PC Controller
                    networkClient.startCoordinatedSession(sessionDirectory)
                } else {
                    // Start local recording only
                    recordingController.startRecording(sessionDirectory)
                }
                
                if (success) {
                    binding.statusTextView.text = "Coordinated recording session started"
                    android.widget.Toast.makeText(this@HubSpokeIntegrationActivity, "Recording started", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    binding.statusTextView.text = "Failed to start recording session"
                    android.widget.Toast.makeText(this@HubSpokeIntegrationActivity, "Recording failed to start", android.widget.Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Recording start error", e)
                binding.statusTextView.text = "Recording start error: ${e.message}"
                android.widget.Toast.makeText(this@HubSpokeIntegrationActivity, "Recording error", android.widget.Toast.LENGTH_SHORT).show()
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
                
                val success = if (networkClient.isConnected()) {
                    // Stop coordinated session
                    networkClient.stopCoordinatedSession()
                } else {
                    // Stop local recording only
                    recordingController.stopRecording()
                }
                
                if (success) {
                    binding.statusTextView.text = "Coordinated recording session stopped"
                    android.widget.Toast.makeText(this@HubSpokeIntegrationActivity, "Recording stopped", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    binding.statusTextView.text = "Failed to stop recording session"
                    android.widget.Toast.makeText(this@HubSpokeIntegrationActivity, "Recording stop failed", android.widget.Toast.LENGTH_SHORT).show()
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
                
                if (networkClient.isConnected()) {
                    // Distribute sync marker through PC Controller
                    networkClient.distributeSyncMarker(markerType, metadata)
                } else {
                    // Add local sync marker only
                    val timestampNs = timeManager.getCurrentTimestampNs()
                    recordingController.addSyncMarker(markerType, timestampNs, metadata)
                }
                
                android.widget.Toast.makeText(this@HubSpokeIntegrationActivity, "Sync marker added", android.widget.Toast.LENGTH_SHORT).show()
                Log.i(TAG, "Sync marker added: $markerType")
                
            } catch (e: Exception) {
                Log.e(TAG, "Sync marker error", e)
                android.widget.Toast.makeText(this@HubSpokeIntegrationActivity, "Sync marker failed", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecordingMonitoring() {
        if (!::recordingController.isInitialized) return
        
        // Monitor recording state
        recordingController.recordingStateFlow
            .onEach { state ->
                runOnUiThread {
                    when (state) {
                        RecordingState.STARTING -> binding.statusTextView.text = "Starting sensors..."
                        RecordingState.RECORDING -> binding.statusTextView.text = "Recording in progress"
                        RecordingState.STOPPING -> binding.statusTextView.text = "Stopping sensors..."
                        RecordingState.STOPPED -> binding.statusTextView.text = "Recording stopped"
                        RecordingState.ERROR -> binding.statusTextView.text = "Recording error"
                    }
                    updateUI()
                }
            }
            .launchIn(lifecycleScope)
        
        // Monitor sensor status
        recordingController.sensorStatusFlow
            .onEach { statusList ->
                runOnUiThread {
                    val statusText = buildString {
                        statusList.forEach { status ->
                            append("${status.sensorType}: ")
                            append(if (status.isRecording) "Recording" else "Stopped")
                            append(" (${status.samplesRecorded} samples, ")
                            append("${String.format("%.1f", status.storageUsedMB)}MB)\n")
                        }
                    }
                    binding.sensorStatusTextView.text = statusText.trim()
                }
            }
            .launchIn(lifecycleScope)
    }

    private fun setupNetworkMonitoring() {
        // Monitor connection state
        networkClient.connectionStateFlow
            .onEach { state ->
                runOnUiThread {
                    binding.connectionStatusTextView.text = "Connection: $state"
                    updateUI()
                }
            }
            .launchIn(lifecycleScope)
        
        // Monitor time sync quality
        lifecycleScope.launch {
            while (networkClient.isConnected()) {
                val syncQuality = timeManager.getSyncQuality()
                runOnUiThread {
                    binding.syncQualityTextView.text = buildString {
                        append("Sync: ${syncQuality.level}")
                        syncQuality.qualityMs?.let { append(" (${it}ms)") }
                        syncQuality.timeSinceSyncMs?.let { append(" - ${it / 1000}s ago") }
                    }
                }
                kotlinx.coroutines.delay(2000) // Update every 2 seconds
            }
        }
    }

    private fun updateUI() {
        val isConnected = ::networkClient.isInitialized && networkClient.isConnected()
        val isRecording = ::recordingController.isInitialized && recordingController.isRecording
        
        binding.connectButton.isEnabled = !isConnected
        binding.disconnectButton.isEnabled = isConnected
        binding.startRecordingButton.isEnabled = !isRecording
        binding.stopRecordingButton.isEnabled = isRecording
        binding.addSyncMarkerButton.isEnabled = isRecording
        
        binding.pcAddressEditText.isEnabled = !isConnected
        binding.sessionDirectoryEditText.isEnabled = !isRecording
        
        if (!isConnected) {
            binding.connectionStatusTextView.text = "Connection: Disconnected"
            binding.syncQualityTextView.text = "Sync: Not Available"
        }
        
        if (!isRecording) {
            binding.sensorStatusTextView.text = "Sensors: Idle"
        }
        
        // Update BLE device status
        updateBLEDeviceStatus()
    }
    
    /**
     * Update BLE device connection status in the UI
     * Part of systematic harmonization for enhanced BLE monitoring
     */
    private fun updateBLEDeviceStatus() {
        if (::enhancedBLE.isInitialized) {
            val bleDeviceCount = connectedBLEDevices.size
            val statusText = if (bleDeviceCount > 0) {
                "BLE Devices: $bleDeviceCount connected (Enhanced Nordic Backend)"
            } else {
                "BLE Devices: Scanning for devices..."
            }
            
            // Update the sensor status to include BLE device information
            if (binding.sensorStatusTextView.text.toString().startsWith("Sensors: Idle")) {
                binding.sensorStatusTextView.text = "Sensors: Idle | $statusText"
            }
        }
    }
}