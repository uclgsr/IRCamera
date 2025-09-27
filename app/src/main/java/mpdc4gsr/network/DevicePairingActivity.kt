package mpdc4gsr.network

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityDevicePairingBinding
import com.mpdc4gsr.gsr.model.SessionInfo
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModelActivity
import mpdc4gsr.sensors.gsr.MultiModalRecordingActivity

class DevicePairingActivity : BaseViewModelActivity<DevicePairingViewModel>(),
    NetworkClient.NetworkEventListener {
    
    companion object {
        private const val TAG = "DevicePairingActivity"

        fun start(context: Context) {
            val intent = Intent(context, DevicePairingActivity::class.java)
            context.startActivity(intent)
        }
    }

    private lateinit var binding: ActivityDevicePairingBinding
    private lateinit var controllersAdapter: ControllersAdapter

    override fun providerVMClass(): Class<DevicePairingViewModel> = DevicePairingViewModel::class.java

    override fun initContentView() = R.layout.activity_device_pairing

    override fun initView() {
        binding = ActivityDevicePairingBinding.inflate(layoutInflater)
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

    private fun setupObservers() {
        viewModel.discoveredControllers.observe(this) { controllers ->
            controllersAdapter.updateControllers(controllers)
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
        if (::binding.isInitialized && binding.root.findViewById<android.widget.TextView>(R.id.connected_device_text) != null) {
            val connectedDeviceText = binding.root.findViewById<android.widget.TextView>(R.id.connected_device_text)
            connectedDeviceText.isVisible = controller != null
            connectedDeviceText.text = controller?.let { 
                "Connected to: ${it.deviceName} (${it.ipAddress}:${it.port})" 
            }
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

    // NetworkClient.NetworkEventListener implementation - delegate to ViewModel
    override fun onControllerConnected(controller: NetworkClient.ControllerInfo) {
        // ViewModel handles this through its own NetworkEventListener implementation
    }

    override fun onControllerDisconnected() {
        // ViewModel handles this through its own NetworkEventListener implementation
    }

    override fun onConnectionError(error: String) {
        // ViewModel handles this through its own NetworkEventListener implementation
    }

    override fun onRecordingSessionStarted(sessionInfo: SessionInfo) {
        // ViewModel handles this through its own NetworkEventListener implementation
    }

    override fun onRecordingSessionStopped() {
        // ViewModel handles this through its own NetworkEventListener implementation
    }

    override fun onDataReceived(data: String) {
        // ViewModel handles this through its own NetworkEventListener implementation
    }
}
