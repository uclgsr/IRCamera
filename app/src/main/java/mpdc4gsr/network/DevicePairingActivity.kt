package mpdc4gsr.network

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityDevicePairingBinding
import com.mpdc4gsr.gsr.model.SessionInfo
import com.mpdc4gsr.libunified.app.ktbase.BaseBindingActivity
import com.kotlinx.coroutines.launch
import com.mpdc4gsr.sensors.gsr.MultiModalRecordingActivity

class DevicePairingActivity : BaseBindingActivity<ActivityDevicePairingBinding>(),
    NetworkClient.NetworkEventListener {
    companion object {
        private const val TAG = "DevicePairingActivity"

        fun start(context: Context) {
            val intent = Intent(context, DevicePairingActivity::class.java)
            context.startActivity(intent)
        }
    }

    private lateinit var networkClient: NetworkClient
    private lateinit var controllersAdapter: ControllersAdapter

    private val discoveredControllers = mutableListOf<NetworkClient.ControllerInfo>()
    private var connectedController: NetworkClient.ControllerInfo? = null

    override fun initContentLayoutId() = R.layout.activity_device_pairing

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeViews()
        setupNetworkClient()
        setupRecyclerView()
        updateUI()
    }

    private fun initializeViews() {
        binding.scanButton.setOnClickListener { startControllerScan() }
        binding.disconnectButton.setOnClickListener { disconnectFromController() }
    }

    private fun setupNetworkClient() {
        networkClient = NetworkClient(this)
        networkClient.setEventListener(this)
    }

    private fun setupRecyclerView() {
        controllersAdapter =
            ControllersAdapter(discoveredControllers) { controller ->
                connectToController(controller)
            }

        binding.controllersRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.controllersRecyclerView.adapter = controllersAdapter
    }

    private fun startControllerScan() {
        binding.scanButton.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        binding.statusText.text = "Scanning for PC Controllers..."

        discoveredControllers.clear()
        controllersAdapter.notifyDataSetChanged()

        lifecycleScope.launch {
            try {
                val controllers = networkClient.discoverControllers()
                runOnUiThread {
                    discoveredControllers.addAll(controllers)
                    controllersAdapter.notifyDataSetChanged()

                    binding.statusText.text =
                        if (controllers.isNotEmpty()) {
                            "Found ${controllers.size} PC Controller(s)"
                        } else {
                            "No PC Controllers found. Make sure you're on the same network."
                        }

                    binding.progressBar.visibility = View.GONE
                    binding.scanButton.isEnabled = true
                }
            } catch (e: Exception) {
                runOnUiThread {
                    binding.statusText.text = "Scan failed: ${e.message}"
                    binding.progressBar.visibility = View.GONE
                    binding.scanButton.isEnabled = true
                }
            }
        }
    }

    private fun connectToController(controller: NetworkClient.ControllerInfo) {
        binding.statusText.text = "Connecting to ${controller.deviceName}..."
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val success = networkClient.connectToController(controller.ipAddress, controller.port)
            runOnUiThread {
                binding.progressBar.visibility = View.GONE
                if (!success) {
                    binding.statusText.text = "Failed to connect to ${controller.deviceName}"
                }
            }
        }
    }

    private fun disconnectFromController() {
        networkClient.disconnect()
    }

    private fun updateUI() {
        val isConnected = networkClient.isConnected()

        binding.scanButton.isEnabled = isConnected.not()
        binding.disconnectButton.visibility = if (isConnected) View.VISIBLE else View.GONE
        binding.controllersRecyclerView.visibility = if (isConnected) View.GONE else View.VISIBLE

        binding.connectionStatusText.text =
            if (isConnected) {
                "Connected to: ${connectedController?.deviceName ?: "PC Controller"}"
            } else {
                "Not connected"
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        networkClient.disconnect()
    }

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
            binding.statusText.text = "Connected to ${controller.deviceName}"
            updateUI()

            Toast.makeText(this, "Device paired successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDisconnected(reason: String) {
        runOnUiThread {
            connectedController = null
            binding.statusText.text = "Disconnected: $reason"
            updateUI()
        }
    }

    override fun onRemoteMeasurementRequest(sessionInfo: SessionInfo) {
        runOnUiThread {

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Remote Measurement Request")
                .setMessage(
                    "PC Controller is requesting to start measurement session:\n\n${sessionInfo.studyName ?: sessionInfo.sessionId}",
                )
                .setPositiveButton("Start") { _, _ ->
                    startRemoteMeasurement(sessionInfo)
                }
                .setNegativeButton("Decline") { _, _ ->

                    Toast.makeText(this, "Measurement request declined", Toast.LENGTH_SHORT).show()
                }
                .setCancelable(false)
                .show()
        }
    }

    override fun onSyncFlash(durationMs: Int) {
        runOnUiThread {

            val flashView = binding.flashOverlay
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

    override fun onTimeSynchronized(offsetNanoseconds: Long) {
        runOnUiThread {
            binding.statusText.text =
                "Time synchronized (offset: ${offsetNanoseconds / 1_000_000}ms)"
        }
    }

    override fun onDataStreamingStarted() {
        runOnUiThread {
            binding.statusText.text = "Data streaming started"
        }
    }

    override fun onDataStreamingStopped() {
        runOnUiThread {
            binding.statusText.text = "Data streaming stopped"
        }
    }

    override fun onError(
        operation: String,
        error: String,
    ) {
        runOnUiThread {
            binding.statusText.text = "Error in $operation: $error"
            Toast.makeText(this, "Network error: $error", Toast.LENGTH_LONG).show()
        }
    }

    private fun startRemoteMeasurement(sessionInfo: SessionInfo) {

        val intent =
            Intent(this, MultiModalRecordingActivity::class.java).apply {
                putExtra("session_id", sessionInfo.sessionId)
                putExtra("session_name", sessionInfo.studyName ?: sessionInfo.sessionId)
                putExtra("remote_session", true)
            }
        startActivity(intent)

        Toast.makeText(this, "Starting remote measurement session", Toast.LENGTH_SHORT).show()
    }
}

class ControllersAdapter(
    private val controllers: List<NetworkClient.ControllerInfo>,
    private val onControllerClick: (NetworkClient.ControllerInfo) -> Unit,
) : RecyclerView.Adapter<ControllersAdapter.ControllerViewHolder>() {
    class ControllerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.controller_name)
        val ipText: TextView = view.findViewById(R.id.controller_ip)
        val capabilitiesText: TextView = view.findViewById(R.id.controller_capabilities)
        val connectButton: Button = view.findViewById(R.id.connect_button)
    }

    override fun onCreateViewHolder(
        parent: android.view.ViewGroup,
        viewType: Int,
    ): ControllerViewHolder {
        val view =
            android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_controller_device, parent, false)
        return ControllerViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ControllerViewHolder,
        position: Int,
    ) {
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
