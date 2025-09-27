package mpdc4gsr.sensors.gsr

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.csl.irCamera.R
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModelActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mpdc4gsr.sensors.unified.model.DeviceInfo


class ShimmerConfigActivity : BaseViewModelActivity<ShimmerConfigViewModel>() {

    companion object {
        private const val TAG = "ShimmerConfigActivity"
    }

    private lateinit var deviceAdapter: ShimmerDeviceAdapter

    override fun providerVMClass(): Class<ShimmerConfigViewModel> = ShimmerConfigViewModel::class.java

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.onPermissionResult(permissions)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shimmer_config)

        setupUI()
        setupObservers()
        viewModel.checkPermissions()
    }

    private fun setupUI() {
        setupToolbar()
        setupRecyclerView()
        setupButtonListeners()
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Shimmer GSR Configuration"
    }

    private fun setupRecyclerView() {
        deviceAdapter = ShimmerDeviceAdapter { device ->
            onDeviceSelected(device)
        }

        findViewById<RecyclerView>(R.id.recyclerViewDevices)?.apply {
            layoutManager = LinearLayoutManager(this@ShimmerConfigActivity)
            adapter = deviceAdapter
        }
    }

    private fun setupButtonListeners() {
        findViewById<android.widget.Button>(R.id.buttonScan)?.setOnClickListener {
            try {
                viewModel.toggleScanning()
            } catch (e: Exception) {
                Log.e(TAG, "Error handling scan button click", e)
                Toast.makeText(this, "Scan operation failed: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        findViewById<android.widget.Button>(R.id.buttonTestConnection)?.setOnClickListener {
            try {
                viewModel.testConnection()
            } catch (e: Exception) {
                Log.e(TAG, "Error handling test connection button click", e)
                Toast.makeText(this, "Connection test failed: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        findViewById<android.widget.Button>(R.id.buttonDisconnect)?.setOnClickListener {
            try {
                viewModel.disconnectDevice()
            } catch (e: Exception) {
                Log.e(TAG, "Error handling disconnect button click", e)
                Toast.makeText(
                    this,
                    "Disconnect operation failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupObservers() {
        // UI State observer
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { uiState ->
                updateUI(uiState.statusMessage)
                updateScanButton(uiState.isScanning)
                
                if (uiState.isLoading) {
                    findViewById<android.widget.ProgressBar>(R.id.progressBar)?.visibility =
                        View.VISIBLE
                } else {
                    findViewById<android.widget.ProgressBar>(R.id.progressBar)?.visibility =
                        View.GONE
                }
            }
        }

        // Discovered devices observer
        lifecycleScope.launch {
            viewModel.discoveredDevices.collectLatest { devices ->
                Log.d(TAG, "Received ${devices.size} discovered Shimmer devices")
                deviceAdapter.updateDevices(devices)
            }
        }

        // Connection state observer
        lifecycleScope.launch {
            viewModel.connectionState.collectLatest { connectionState ->
                when (connectionState) {
                    is ShimmerConfigViewModel.ConnectionState.Connected -> {
                        updateConnectionStatus(connectionState.device)
                    }
                    is ShimmerConfigViewModel.ConnectionState.Disconnected -> {
                        updateConnectionStatus(null)
                    }
                    else -> {
                        // Other states handled by UI state updates
                    }
                }
            }
        }

        // Permission state observer
        viewModel.permissionState.observe(this) { permissionState ->
            if (!permissionState.hasAllPermissions) {
                permissionLauncher.launch(permissionState.missingPermissions.toTypedArray())
            }
        }

        // Action events observer
        viewModel.configAction.observe(this) { action ->
            handleConfigAction(action)
        }
    }

    private fun handleConfigAction(action: ShimmerConfigViewModel.ConfigAction) {
        when (action.type) {
            ShimmerConfigViewModel.ActionType.SHOW_TOAST -> {
                action.message?.let { message ->
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
            ShimmerConfigViewModel.ActionType.SHOW_PERMISSION_ERROR -> {
                showPermissionError()
            }
            ShimmerConfigViewModel.ActionType.UPDATE_SCAN_BUTTON -> {
                // Already handled by UI state observer
            }
            ShimmerConfigViewModel.ActionType.UPDATE_CONNECTION_STATUS -> {
                action.device?.let { device ->
                    updateConnectionStatus(device)
                }
            }
            ShimmerConfigViewModel.ActionType.SHOW_PROGRESS_BAR -> {
                findViewById<android.widget.ProgressBar>(R.id.progressBar)?.visibility =
                    View.VISIBLE
            }
            ShimmerConfigViewModel.ActionType.HIDE_PROGRESS_BAR -> {
                findViewById<android.widget.ProgressBar>(R.id.progressBar)?.visibility =
                    View.GONE
            }
        }
    }

    private fun onDeviceSelected(device: DeviceInfo) {
        Log.i(TAG, "Device selected: ${device.name} (${device.address})")
        viewModel.connectToDevice(device)
    }

    // UI Helper Methods (Pure presentation logic)
    private fun updateUI(message: String) {
        findViewById<android.widget.TextView>(R.id.textViewStatus)?.text = message
    }

    private fun updateScanButton(isScanning: Boolean) {
        findViewById<android.widget.Button>(R.id.buttonScan)?.apply {
            text = if (isScanning) "Stop Scan" else "Start Scan"
            isEnabled = true
        }
    }

    private fun updateConnectionStatus(device: DeviceInfo?) {
        findViewById<android.widget.TextView>(R.id.textViewConnectionStatus)?.apply {
            text = if (device != null) {
                "Connected to: ${device.name} (${device.address})"
            } else {
                "No device connected"
            }
        }

        findViewById<android.widget.Button>(R.id.buttonTestConnection)?.isEnabled = (device != null)
        findViewById<android.widget.Button>(R.id.buttonDisconnect)?.isEnabled = (device != null)
    }

    private fun showPermissionError() {
        Toast.makeText(
            this,
            "Bluetooth permissions are required for device scanning",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        // ViewModel handles cleanup automatically
    }
}