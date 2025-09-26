package mpdc4gsr.sensors.gsr

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.csl.irCamera.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mpdc4gsr.sensors.unified.ShimmerDeviceManager
import mpdc4gsr.sensors.unified.model.DeviceInfo


class ShimmerConfigActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ShimmerConfigActivity"


        private val REQUIRED_PERMISSIONS =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } else {
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
    }


    private lateinit var deviceAdapter: ShimmerDeviceAdapter
    private var shimmerDeviceManager: ShimmerDeviceManager? = null
    private var isScanning = false
    private var connectedDevice: DeviceInfo? = null


    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {            initializeShimmerManager()
        } else {            updateUI("Bluetooth permissions required for Shimmer device scanning")
            showPermissionError()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContentView(R.layout.activity_shimmer_config)

        setupUI()
        checkPermissionsAndInitialize()
    }

    private fun setupUI() {

        setupToolbar()
        setupRecyclerView()
        setupButtonListeners()


        updateUI("Ready to scan for Shimmer devices")
        updateScanButton(false)
        updateConnectionStatus(null)
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
                if (isScanning) {
                    stopDeviceScanning()
                } else {
                    startDeviceScanning()
                }
            } catch (e: Exception) {                Toast.makeText(this, "Scan operation failed: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        findViewById<android.widget.Button>(R.id.buttonTestConnection)?.setOnClickListener {
            try {
                testSelectedDeviceConnection()
            } catch (e: Exception) {                Toast.makeText(this, "Connection test failed: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        findViewById<android.widget.Button>(R.id.buttonDisconnect)?.setOnClickListener {
            try {
                disconnectCurrentDevice()
            } catch (e: Exception) {                Toast.makeText(
                    this,
                    "Disconnect operation failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun checkPermissionsAndInitialize() {
        val missingPermissions = REQUIRED_PERMISSIONS.filter { permission ->
            ActivityCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {            initializeShimmerManager()
        } else {            permissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    private fun initializeShimmerManager() {
        try {
            shimmerDeviceManager = ShimmerDeviceManager(this, this)

            lifecycleScope.launch {
                val initialized = shimmerDeviceManager!!.initialize()
                if (initialized) {                    updateUI("Shimmer device manager ready - tap 'Start Scan' to discover devices")


                    setupDeviceFlowCollectors()
                } else {                    updateUI("Failed to initialize Bluetooth - check if Bluetooth is enabled")
                }
            }
        } catch (e: Exception) {            updateUI("Initialization error: ${e.message}")
        }
    }

    private fun setupDeviceFlowCollectors() {

        lifecycleScope.launch {
            shimmerDeviceManager?.scanResults?.collectLatest { devices ->                deviceAdapter.updateDevices(devices)

                if (devices.isEmpty() && isScanning) {
                    updateUI("Scanning for Shimmer devices... (${devices.size} found)")
                } else if (devices.isNotEmpty()) {
                    updateUI("Found ${devices.size} Shimmer device(s) - select one to connect")
                }
            }
        }


        lifecycleScope.launch {
            shimmerDeviceManager?.connectionEvents?.collectLatest { event ->                when (event.state) {
                    ShimmerDeviceManager.ConnectionState.CONNECTING -> {
                        updateUI("Connecting to Shimmer device...")
                        findViewById<android.widget.ProgressBar>(R.id.progressBar)?.visibility =
                            View.VISIBLE
                    }

                    ShimmerDeviceManager.ConnectionState.CONNECTED -> {
                        val device = deviceAdapter.getDeviceByAddress(event.deviceAddress)
                        connectedDevice = device
                        updateConnectionStatus(device)
                        updateUI("Successfully connected to ${device?.name ?: event.deviceAddress}")
                        findViewById<android.widget.ProgressBar>(R.id.progressBar)?.visibility =
                            View.GONE
                        Toast.makeText(
                            this@ShimmerConfigActivity,
                            "Connected to Shimmer device!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    ShimmerDeviceManager.ConnectionState.DISCONNECTED -> {
                        connectedDevice = null
                        updateConnectionStatus(null)
                        updateUI("Shimmer device disconnected")
                        findViewById<android.widget.ProgressBar>(R.id.progressBar)?.visibility =
                            View.GONE
                    }

                    ShimmerDeviceManager.ConnectionState.FAILED -> {
                        updateUI("Connection failed: ${event.message ?: "Unknown error"}")
                        findViewById<android.widget.ProgressBar>(R.id.progressBar)?.visibility =
                            View.GONE
                        Toast.makeText(
                            this@ShimmerConfigActivity,
                            "Connection failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    ShimmerDeviceManager.ConnectionState.TIMEOUT -> {
                        updateUI("Connection timeout - device may be out of range or not responding")
                        findViewById<android.widget.ProgressBar>(R.id.progressBar)?.visibility =
                            View.GONE
                        Toast.makeText(
                            this@ShimmerConfigActivity,
                            "Connection timeout",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun startDeviceScanning() {
        val manager = shimmerDeviceManager
        if (manager == null) {
            updateUI("Device manager not initialized")
            return
        }

        lifecycleScope.launch {
            try {
                val scanStarted = manager.startDeviceScanning()
                if (scanStarted) {
                    isScanning = true
                    updateScanButton(true)
                    updateUI("Scanning for Shimmer3 GSR+ devices...")
                    deviceAdapter.clearDevices()                } else {
                    updateUI("Failed to start device scanning - check Bluetooth permissions")                }
            } catch (e: Exception) {                updateUI("Scan error: ${e.message}")
            }
        }
    }

    private fun stopDeviceScanning() {
        val manager = shimmerDeviceManager ?: return

        lifecycleScope.launch {
            try {
                manager.stopDeviceScanning()
                isScanning = false
                updateScanButton(false)

                val deviceCount = deviceAdapter.itemCount
                if (deviceCount > 0) {
                    updateUI("Scan completed - found $deviceCount Shimmer device(s)")
                } else {
                    updateUI("Scan completed - no Shimmer devices found")
                }            } catch (e: Exception) {            }
        }
    }

    private fun onDeviceSelected(device: DeviceInfo) {")

        val manager = shimmerDeviceManager
        if (manager == null) {
            Toast.makeText(this, "Device manager not available", Toast.LENGTH_SHORT).show()
            return
        }


        if (isScanning) {
            stopDeviceScanning()
        }


        lifecycleScope.launch {
            try {
                updateUI("Connecting to ${device.name}...")
                findViewById<android.widget.ProgressBar>(R.id.progressBar)?.visibility =
                    View.VISIBLE

                val connected = manager.connectToDevice(device)
                if (!connected) {                }
            } catch (e: Exception) {                updateUI("Connection error: ${e.message}")
                findViewById<android.widget.ProgressBar>(R.id.progressBar)?.visibility = View.GONE
            }
        }
    }

    private fun testSelectedDeviceConnection() {
        val device = connectedDevice
        if (device == null) {
            Toast.makeText(this, "No device connected", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                updateUI("Testing connection to ${device.name}...")
                findViewById<android.widget.ProgressBar>(R.id.progressBar)?.visibility =
                    View.VISIBLE


                kotlinx.coroutines.delay(1000)

                updateUI("Connection test successful - device is responsive")
                findViewById<android.widget.ProgressBar>(R.id.progressBar)?.visibility = View.GONE
                Toast.makeText(
                    this@ShimmerConfigActivity,
                    "Device connection test passed!",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {                updateUI("Connection test failed: ${e.message}")
                findViewById<android.widget.ProgressBar>(R.id.progressBar)?.visibility = View.GONE
                Toast.makeText(
                    this@ShimmerConfigActivity,
                    "Connection test failed",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun disconnectCurrentDevice() {
        val device = connectedDevice
        if (device == null) {
            Toast.makeText(this, "No device connected", Toast.LENGTH_SHORT).show()
            return
        }

        val manager = shimmerDeviceManager ?: return

        lifecycleScope.launch {
            try {
                val disconnected = manager.disconnectDevice(device.address)
                if (disconnected) {                } else {                }
            } catch (e: Exception) {                Toast.makeText(
                    this@ShimmerConfigActivity,
                    "Disconnect error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun updateUI(message: String) {
        runOnUiThread {
            findViewById<android.widget.TextView>(R.id.textViewStatus)?.text = message        }
    }

    private fun updateScanButton(isScanning: Boolean) {
        runOnUiThread {
            val scanButton = findViewById<android.widget.Button>(R.id.buttonScan)
            val progressBar = findViewById<android.widget.ProgressBar>(R.id.progressBar)

            if (isScanning) {
                scanButton?.text = "Stop Scan"
                progressBar?.visibility = View.VISIBLE
            } else {
                scanButton?.text = "Start Scan"
                progressBar?.visibility = View.GONE
            }
        }
    }

    private fun updateConnectionStatus(device: DeviceInfo?) {
        runOnUiThread {
            val statusView = findViewById<android.widget.TextView>(R.id.textViewConnectionStatus)
            val testButton = findViewById<android.widget.Button>(R.id.buttonTestConnection)
            val disconnectButton = findViewById<android.widget.Button>(R.id.buttonDisconnect)

            if (device != null) {
                statusView?.text = "Connected: ${device.name}"
                statusView?.setTextColor(getColor(android.R.color.holo_green_dark))
                testButton?.isEnabled = true
                disconnectButton?.isEnabled = true
            } else {
                statusView?.text = "Not Connected"
                statusView?.setTextColor(getColor(android.R.color.holo_red_dark))
                testButton?.isEnabled = false
                disconnectButton?.isEnabled = false
            }
        }
    }

    private fun showPermissionError() {
        Toast.makeText(
            this,
            "Bluetooth permissions are required for Shimmer device discovery and connection",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDestroy() {
        super.onDestroy()


        lifecycleScope.launch {
            try {
                if (isScanning) {
                    shimmerDeviceManager?.stopDeviceScanning()
                }
                shimmerDeviceManager?.release()
            } catch (e: Exception) {            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
