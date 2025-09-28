package mpdc4gsr.network

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityMultiModalConsolidatedBinding
import com.mpdc4gsr.gsr.model.SessionInfo
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModelActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mpdc4gsr.sensors.gsr.MultiModalRecordingActivity

/**
 * Modern Device Pairing Activity - MVVM with Lifecycle-aware StateFlow observers
 * Uses repeatOnLifecycle for proper lifecycle management and StateFlow collection
 */
class DevicePairingActivity : BaseViewModelActivity<DevicePairingViewModel>(),
    NetworkClient.NetworkEventListener {

    companion object {
        private const val TAG = "DevicePairingActivity"

        fun start(context: Context) {
            val intent = Intent(context, DevicePairingActivity::class.java)
            context.startActivity(intent)
        }
    }

    private lateinit var binding: ActivityMultiModalConsolidatedBinding
    private lateinit var controllersAdapter: ControllersAdapter

    override fun providerVMClass(): Class<DevicePairingViewModel> =
        DevicePairingViewModel::class.java

    override fun initContentView() = R.layout.activity_multi_modal_consolidated

    override fun initView() {
        binding = ActivityMultiModalConsolidatedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeViews()
        setupRecyclerView()
        setupModernObservers()

        viewModel.initialize(this)
    }

    private fun initializeViews() {
        binding.scanButton.setOnClickListener {
            viewModel.startControllerScan()
        }
        binding.disconnectButton.setOnClickListener {
            viewModel.disconnectFromController()
        }
    }

    private fun setupRecyclerView() {
        controllersAdapter = ControllersAdapter(emptyList()) { controller ->
            viewModel.connectToController(controller)
        }

        binding.controllersRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.controllersRecyclerView.adapter = controllersAdapter
    }

    override fun initData() {
        // Initialize any data needed for the activity
        // This method is called by BaseActivity after initView()
    }

    /**
     * Modern StateFlow observers using repeatOnLifecycle for proper lifecycle management
     * This replaces traditional LiveData observe() calls
     */
    private fun setupModernObservers() {
        // Collect UI state changes
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pairingScreenState.collectLatest { state ->
                    handleUiState(state)
                }
            }
        }

        // Collect available controllers
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.discoveredControllers.collectLatest { controllers ->
                    controllersAdapter.updateControllers(controllers)
                    binding.controllersRecyclerView.isVisible = controllers.isNotEmpty()
                }
            }
        }

        // Collect connection state
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.connectionState.collectLatest { state ->
                    handleConnectionState(state)
                }
            }
        }

        // Collect one-time events
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collectLatest { event ->
                    handlePairingEvent(event)
                }
            }
        }
    }

    private fun handleUiState(state: DevicePairingViewModel.PairingScreenState) {
        binding.apply {
            scanButton.text = if (state.showProgress) "Stop Scan" else "Start Scan"
            scanButton.isEnabled = state.canScan

            progressBar.isVisible = state.showProgress
            statusText.text = "Discovered ${state.discoveredCount} device(s)"

            // Update scan results count if the view exists
            try {
                val resourceId = resources.getIdentifier("scan_results_count", "id", packageName)
                if (resourceId != 0) {
                    val scanResultsCount = binding.root.findViewById<android.widget.TextView>(resourceId)
                    scanResultsCount?.text = "${state.discoveredCount} device(s) found"
                }
            } catch (e: Exception) {
                // View may not exist in layout, continue gracefully
            }
        }
    }

    private fun handleConnectionState(state: DevicePairingViewModel.ConnectionState) {
        binding.progressBar.isVisible = state == DevicePairingViewModel.ConnectionState.CONNECTING

        // Update connection status text if it exists
        try {
            val resourceId = resources.getIdentifier("connection_status", "id", packageName)
            if (resourceId != 0) {
                val connectionStatus = binding.root.findViewById<android.widget.TextView>(resourceId)
                connectionStatus?.let { textView ->
                    when (state) {
                        is DevicePairingViewModel.ConnectionState.Connected -> {
                            textView.text = "Connected to ${state.controller.name}"
                            textView.setTextColor(getColor(android.R.color.holo_green_dark))
                        }

                        is DevicePairingViewModel.ConnectionState.Connecting -> {
                            textView.text = "Connecting..."
                            textView.setTextColor(getColor(android.R.color.holo_orange_dark))
                        }

                        is DevicePairingViewModel.ConnectionState.Disconnected -> {
                            textView.text = "Disconnected"
                            textView.setTextColor(getColor(android.R.color.darker_gray))
                        }

                        is DevicePairingViewModel.ConnectionState.Failed -> {
                            textView.text = "Connection failed: ${state.message}"
                            textView.setTextColor(getColor(android.R.color.holo_red_dark))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // View may not exist in layout, continue gracefully
        }
    }

    private fun handlePairingEvent(event: DevicePairingViewModel.PairingEvent) {
        when (event) {
            is DevicePairingViewModel.PairingEvent.ShowError -> {
                Toast.makeText(this, event.message, Toast.LENGTH_LONG).show()
            }

            is DevicePairingViewModel.PairingEvent.ShowSuccess -> {
                Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
            }

            is DevicePairingViewModel.PairingEvent.NavigateToSession -> {
                MultiModalRecordingActivity.startRecording(this, event.sessionInfo)
            }

            is DevicePairingViewModel.PairingEvent.ShowConnectionDialog -> {
                Toast.makeText(this, "Connecting to ${event.controller.name}...", Toast.LENGTH_SHORT).show()
                binding.disconnectButton.isVisible = true
            }

            is DevicePairingViewModel.PairingEvent.NavigateBack -> {
                finish()
            }
        }
    }


    // NetworkClient.NetworkEventListener implementation
    override fun onControllerDiscovered(controller: NetworkClient.ControllerInfo) {
        // ViewModel handles this through its own NetworkEventListener implementation
    }

    override fun onConnected(controller: NetworkClient.ControllerInfo) {
        // ViewModel handles this through its own NetworkEventListener implementation
    }

    override fun onDisconnected(reason: String) {
        // ViewModel handles this through its own NetworkEventListener implementation
    }

    override fun onRemoteMeasurementRequest(sessionInfo: SessionInfo) {
        // ViewModel handles this through its own NetworkEventListener implementation
    }

    override fun onSyncFlash(durationMs: Int) {
        runOnUiThread {
            val flashView = binding.flashOverlay
            flashView.visibility = android.view.View.VISIBLE
            flashView.alpha = 1.0f

            flashView.animate()
                .alpha(0.0f)
                .setDuration(durationMs.toLong())
                .withEndAction {
                    flashView.visibility = android.view.View.GONE
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
    private var controllers: List<NetworkClient.ControllerInfo>,
    private val onControllerClick: (NetworkClient.ControllerInfo) -> Unit,
) : androidx.recyclerview.widget.RecyclerView.Adapter<ControllersAdapter.ControllerViewHolder>() {

    class ControllerViewHolder(itemView: android.view.View) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        val deviceNameText: android.widget.TextView = itemView.findViewById(android.R.id.text1)
        val deviceInfoText: android.widget.TextView = itemView.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(
        parent: android.view.ViewGroup,
        viewType: Int
    ): ControllerViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ControllerViewHolder(view)
    }

    override fun onBindViewHolder(holder: ControllerViewHolder, position: Int) {
        val controller = controllers[position]
        holder.deviceNameText.text = controller.deviceName
        holder.deviceInfoText.text = "${controller.ipAddress}:${controller.port}"

        holder.itemView.setOnClickListener {
            onControllerClick(controller)
        }
    }

    override fun getItemCount(): Int = controllers.size

    fun updateControllers(newControllers: List<NetworkClient.ControllerInfo>) {
        controllers = newControllers
        notifyDataSetChanged()
    }
}
