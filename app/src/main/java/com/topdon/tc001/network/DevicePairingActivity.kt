package com.topdon.tc001.network

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.csl.irCamera.R
import com.topdon.gsr.model.SessionInfo
import com.topdon.lib.core.config.RouterConfig
import com.topdon.tc001.gsr.MultiModalRecordingActivity
import kotlinx.coroutines.launch

/**
 * Device Pairing Activity for connecting to PC Controllers
 * Allows discovery, pairing, and remote measurement initiation
 */
class DevicePairingActivity : AppCompatActivity(), NetworkClient.NetworkEventListener {
    
    companion object {
        private const val TAG = "DevicePairingActivity"
        
        fun start(context: Context) {
            val intent = Intent(context, DevicePairingActivity::class.java)
            context.startActivity(intent)
        }
    }
    
    private lateinit var networkClient: NetworkClient
    private lateinit var controllersAdapter: ControllersAdapter
    
    // UI Components
    private lateinit var scanButton: Button
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var controllersRecyclerView: RecyclerView
    private lateinit var connectionStatusText: TextView
    private lateinit var disconnectButton: Button
    
    private val discoveredControllers = mutableListOf<NetworkClient.ControllerInfo>()
    private var connectedController: NetworkClient.ControllerInfo? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_pairing)
        
        initializeViews()
        setupNetworkClient()
        setupRecyclerView()
        updateUI()
    }
    
    private fun initializeViews() {
        scanButton = findViewById(R.id.scan_button)
        statusText = findViewById(R.id.status_text)
        progressBar = findViewById(R.id.progress_bar)
        controllersRecyclerView = findViewById(R.id.controllers_recycler_view)
        connectionStatusText = findViewById(R.id.connection_status_text)
        disconnectButton = findViewById(R.id.disconnect_button)
        
        scanButton.setOnClickListener { startControllerScan() }
        disconnectButton.setOnClickListener { disconnectFromController() }
    }
    
    private fun setupNetworkClient() {
        networkClient = NetworkClient(this)
        networkClient.setEventListener(this)
    }
    
    private fun setupRecyclerView() {
        controllersAdapter = ControllersAdapter(discoveredControllers) { controller ->
            connectToController(controller)
        }
        
        controllersRecyclerView.layoutManager = LinearLayoutManager(this)
        controllersRecyclerView.adapter = controllersAdapter
    }
    
    private fun startControllerScan() {
        scanButton.isEnabled = false
        progressBar.visibility = View.VISIBLE
        statusText.text = "Scanning for PC Controllers..."
        
        discoveredControllers.clear()
        controllersAdapter.notifyDataSetChanged()
        
        lifecycleScope.launch {
            try {
                val controllers = networkClient.discoverControllers()
                runOnUiThread {
                    discoveredControllers.addAll(controllers)
                    controllersAdapter.notifyDataSetChanged()
                    
                    statusText.text = if (controllers.isNotEmpty()) {
                        "Found ${controllers.size} PC Controller(s)"
                    } else {
                        "No PC Controllers found. Make sure you're on the same network."
                    }
                    
                    progressBar.visibility = View.GONE
                    scanButton.isEnabled = true
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "Scan failed: ${e.message}"
                    progressBar.visibility = View.GONE
                    scanButton.isEnabled = true
                }
            }
        }
    }
    
    private fun connectToController(controller: NetworkClient.ControllerInfo) {
        statusText.text = "Connecting to ${controller.deviceName}..."
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            val success = networkClient.connectToController(controller.ipAddress, controller.port)
            runOnUiThread {
                progressBar.visibility = View.GONE
                if (!success) {
                    statusText.text = "Failed to connect to ${controller.deviceName}"
                }
            }
        }
    }
    
    private fun disconnectFromController() {
        networkClient.disconnect()
    }
    
    private fun updateUI() {
        val isConnected = networkClient.isConnected()
        
        scanButton.isEnabled = !isConnected
        disconnectButton.visibility = if (isConnected) View.VISIBLE else View.GONE
        controllersRecyclerView.visibility = if (isConnected) View.GONE else View.VISIBLE
        
        connectionStatusText.text = if (isConnected) {
            "Connected to: ${connectedController?.deviceName ?: "PC Controller"}"
        } else {
            "Not connected"
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        networkClient.disconnect()
    }
    
    // NetworkEventListener implementation
    override fun onControllerDiscovered(controller: NetworkClient.ControllerInfo) {
        runOnUiThread {
            if (!discoveredControllers.any { it.ipAddress == controller.ipAddress }) {
                discoveredControllers.add(controller)
                controllersAdapter.notifyItemInserted(discoveredControllers.size - 1)
            }
        }
    }
    
    override fun onConnected(controller: NetworkClient.ControllerInfo) {
        runOnUiThread {
            connectedController = controller
            statusText.text = "Connected to ${controller.deviceName}"
            updateUI()
            
            Toast.makeText(this, "Device paired successfully!", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDisconnected(reason: String) {
        runOnUiThread {
            connectedController = null
            statusText.text = "Disconnected: $reason"
            updateUI()
        }
    }
    
    override fun onRemoteMeasurementRequest(sessionInfo: SessionInfo) {
        runOnUiThread {
            // Show dialog for remote measurement request
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Remote Measurement Request")
                .setMessage("PC Controller is requesting to start measurement session:\n\n${sessionInfo.sessionName}")
                .setPositiveButton("Start") { _, _ ->
                    startRemoteMeasurement(sessionInfo)
                }
                .setNegativeButton("Decline") { _, _ ->
                    // Send decline response
                    Toast.makeText(this, "Measurement request declined", Toast.LENGTH_SHORT).show()
                }
                .setCancelable(false)
                .show()
        }
    }
    
    override fun onSyncFlash(durationMs: Int) {
        runOnUiThread {
            // Flash the screen for synchronization
            val flashView = findViewById<View>(R.id.flash_overlay)
            flashView.visibility = View.VISIBLE
            flashView.alpha = 1.0f
            
            flashView.animate()
                .alpha(0.0f)
                .setDuration(durationMs.toLong())
                .withEndAction {
                    flashView.visibility = View.GONE
                }
                .start()
        }
    }
    
    override fun onError(operation: String, error: String) {
        runOnUiThread {
            statusText.text = "Error in $operation: $error"
            Toast.makeText(this, "Network error: $error", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun startRemoteMeasurement(sessionInfo: SessionInfo) {
        // Launch MultiModalRecordingActivity with remote session info
        val intent = Intent(this, MultiModalRecordingActivity::class.java).apply {
            putExtra("session_id", sessionInfo.sessionId)
            putExtra("session_name", sessionInfo.sessionName)
            putExtra("remote_session", true)
        }
        startActivity(intent)
        
        Toast.makeText(this, "Starting remote measurement session", Toast.LENGTH_SHORT).show()
    }
}

/**
 * RecyclerView adapter for displaying discovered PC Controllers
 */
class ControllersAdapter(
    private val controllers: List<NetworkClient.ControllerInfo>,
    private val onControllerClick: (NetworkClient.ControllerInfo) -> Unit
) : RecyclerView.Adapter<ControllersAdapter.ControllerViewHolder>() {
    
    class ControllerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.controller_name)
        val ipText: TextView = view.findViewById(R.id.controller_ip)
        val capabilitiesText: TextView = view.findViewById(R.id.controller_capabilities)
        val connectButton: Button = view.findViewById(R.id.connect_button)
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ControllerViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_controller_device, parent, false)
        return ControllerViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ControllerViewHolder, position: Int) {
        val controller = controllers[position]
        
        holder.nameText.text = controller.deviceName
        holder.ipText.text = "${controller.ipAddress}:${controller.port}"
        holder.capabilitiesText.text = "Capabilities: ${controller.capabilities.joinToString(", ")}"
        
        holder.connectButton.setOnClickListener {
            onControllerClick(controller)
        }
    }
    
    override fun getItemCount(): Int = controllers.size
}