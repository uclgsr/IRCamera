package mpdc4gsr.network

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityMultiModalConsolidatedBinding
import com.mpdc4gsr.gsr.model.SessionInfo
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModelActivity
import mpdc4gsr.sensors.gsr.MultiModalRecordingActivity

/**
 * Device Pairing Activity for Network Controllers
 * Manages discovery and connection to PC Controllers for remote measurement sessions
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
        setupObservers()

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

    private fun setupObservers() {
        viewModel.discoveredControllers.observe(this) { controllers ->
            controllersAdapter.updateControllers(controllers)
            binding.controllersRecyclerView.isVisible = controllers.isNotEmpty()
        }

        viewModel.connectedController.observe(this) { controller ->
            updateConnectionUI(controller)
        }

        viewModel.connectionState.observe(this) { state ->
            updateConnectionState(state)
        }

        viewModel.scanState.observe(this) { state ->
            updateScanState(state)
        }

        viewModel.statusMessage.observe(this) { message ->
            binding.statusText.text = message
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.navigationEvent.observe(this) { event ->
            event?.let {
                handleNavigationEvent(it)
                viewModel.clearNavigationEvent()
            }
        }
    }

    private fun updateConnectionUI(controller: NetworkClient.ControllerInfo?) {
        binding.disconnectButton.isVisible = controller != null
        try {
            // Try to access the connected device text view if it exists in the layout
            val resourceId = resources.getIdentifier("connected_device_text", "id", packageName)
            if (resourceId != 0) {
                val connectedDeviceText =
                    binding.root.findViewById<android.widget.TextView>(resourceId)
                connectedDeviceText?.let { textView ->
                    textView.isVisible = controller != null
                    textView.text = controller?.let {
                        "Connected to: ${it.deviceName} (${it.ipAddress}:${it.port})"
                    } ?: ""
                }
            }
        } catch (e: Exception) {
            // Element may not exist in layout, continue gracefully
        }
    }

    private fun updateConnectionState(state: DevicePairingViewModel.ConnectionState) {
        binding.progressBar.isVisible = state == DevicePairingViewModel.ConnectionState.CONNECTING
    }

    private fun updateScanState(state: DevicePairingViewModel.ScanState) {
        binding.scanButton.isEnabled = viewModel.canStartScan()
        binding.progressBar.isVisible = state == DevicePairingViewModel.ScanState.SCANNING
    }

    private fun handleNavigationEvent(event: DevicePairingViewModel.NavigationEvent) {
        when (event.action) {
            "START_RECORDING", "RECORDING_STARTED" -> {
                event.sessionInfo?.let { sessionInfo ->
                    MultiModalRecordingActivity.startRecording(this, sessionInfo)
                }
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
