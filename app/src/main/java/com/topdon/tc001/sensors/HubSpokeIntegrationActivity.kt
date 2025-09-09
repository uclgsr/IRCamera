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

    override fun getViewBinding(): ActivityHubSpokeIntegrationBinding = 
        ActivityHubSpokeIntegrationBinding.inflate(layoutInflater)

    // Core components
    private lateinit var recordingController: RecordingController
    private lateinit var networkClient: EnhancedNetworkClient
    private lateinit var timeManager: TimeManager
    
    // Enhanced BLE Module for systematic harmonization
    private lateinit var enhancedBLE: EasyBLE
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
        
        // Initialize network client (will be connected to service later)
        recordingController = RecordingController(this, this)
        networkClient = EnhancedNetworkClient(this, recordingController)
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