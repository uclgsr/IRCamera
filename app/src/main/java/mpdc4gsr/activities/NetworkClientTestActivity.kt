package mpdc4gsr.activities

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import kotlinx.coroutines.launch
import mpdc4gsr.core.RecordingService
import mpdc4gsr.network.CommandConnection
import mpdc4gsr.network.NetworkManager

/**
 * Test activity for demonstrating bidirectional command/control networking functionality.
 * Shows how Android app can connect as client to PC server via Wi-Fi or Bluetooth.
 */
class NetworkClientTestActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "NetworkClientTestActivity"
        private const val DEFAULT_PC_IP = "192.168.1.100"
        private const val DEFAULT_PC_PORT = 8080
    }
    
    private var recordingService: RecordingService? = null
    private var networkManager: NetworkManager? = null
    private var isBound = false
    
    // UI Components
    private lateinit var connectionStatusIndicator: ImageView
    private lateinit var connectionStatusText: TextView
    private lateinit var ipAddressInput: EditText
    private lateinit var portInput: EditText
    private lateinit var connectWifiButton: Button
    private lateinit var testPingButton: Button
    private lateinit var connectBluetoothButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var connectionInfoText: TextView
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.i(TAG, "Service connected")
            val binder = service as RecordingService.RecordingServiceBinder
            recordingService = binder.getService()
            networkManager = binder.getNetworkManager()
            isBound = true
            
            observeConnectionState()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.i(TAG, "Service disconnected")
            recordingService = null
            networkManager = null
            isBound = false
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network_client_test)
        
        initializeUI()
        
        // Bind to RecordingService
        val serviceIntent = Intent(this, RecordingService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        
        setupUI()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }
    
    private fun initializeUI() {
        connectionStatusIndicator = findViewById(R.id.connection_status_indicator)
        connectionStatusText = findViewById(R.id.connection_status_text)
        ipAddressInput = findViewById(R.id.ip_address_input)
        portInput = findViewById(R.id.port_input)
        connectWifiButton = findViewById(R.id.connect_wifi_button)
        testPingButton = findViewById(R.id.test_ping_button)
        connectBluetoothButton = findViewById(R.id.connect_bluetooth_button)
        disconnectButton = findViewById(R.id.disconnect_button)
        connectionInfoText = findViewById(R.id.connection_info_text)
        
        setupButtonListeners()
    }
    
    private fun setupButtonListeners() {
        connectWifiButton.setOnClickListener {
            val ip = ipAddressInput.text.toString().trim()
            val port = portInput.text.toString().trim().toIntOrNull() ?: DEFAULT_PC_PORT
            
            if (ip.isNotEmpty()) {
                if (port in 1..65535) {
                    testWifiConnection(ip, port)
                } else {
                    Toast.makeText(this, "Please enter a valid port (1-65535)", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter a valid IP address", Toast.LENGTH_SHORT).show()
            }
        }
        
        testPingButton.setOnClickListener {
            testSendMessage()
        }
        
        connectBluetoothButton.setOnClickListener {
            testBluetoothConnection()
        }
        
        disconnectButton.setOnClickListener {
            lifecycleScope.launch {
                networkManager?.disconnect()
                updateConnectionStatus(CommandConnection.ConnectionState.DISCONNECTED)
            }
        }
    }
    
    private fun updateConnectionStatus(state: CommandConnection.ConnectionState) {
        if (!::connectionStatusText.isInitialized ||
            !::connectionStatusIndicator.isInitialized ||
            !::testPingButton.isInitialized ||
            !::disconnectButton.isInitialized ||
            !::connectionInfoText.isInitialized) {
            Log.w(TAG, "UI not fully initialized, skipping status update")
            return
        }
        
        runOnUiThread {
            val (statusText, statusIcon, buttonsEnabled) = when (state) {
                CommandConnection.ConnectionState.CONNECTING -> {
                    Triple("Connecting...", android.R.drawable.presence_away, false)
                }
                CommandConnection.ConnectionState.CONNECTED -> {
                    Triple("Connected", android.R.drawable.presence_online, true)
                }
                CommandConnection.ConnectionState.DISCONNECTED -> {
                    Triple("Disconnected", android.R.drawable.presence_offline, false)
                }
                CommandConnection.ConnectionState.ERROR -> {
                    Triple("Connection Error", android.R.drawable.presence_busy, false)
                }
            }
            
            connectionStatusText.text = statusText
            connectionStatusIndicator.setImageResource(statusIcon)
            testPingButton.isEnabled = buttonsEnabled
            disconnectButton.isEnabled = buttonsEnabled
            
            // Update connection info
            connectionInfoText.text = getConnectionInfo()
        }
    }
    
    private fun setupUI() {
        Log.i(TAG, "Network Client Test Activity started")
        updateConnectionStatus(CommandConnection.ConnectionState.DISCONNECTED)
        
        // Auto-test Wi-Fi connection after service binding (delayed)
        lifecycleScope.launch {
            kotlinx.coroutines.delay(2000) // Wait for service binding
            // Don't auto-connect, let user manually test
        }
    }
    
    private fun observeConnectionState() {
        networkManager?.let { manager ->
            lifecycleScope.launch {
                manager.connectionState.collect { state ->
                    Log.i(TAG, "Connection state changed: $state")
                    updateConnectionStatus(state)
                    
                    val message = when (state) {
                        CommandConnection.ConnectionState.CONNECTING -> "Connecting to PC..."
                        CommandConnection.ConnectionState.CONNECTED -> "Connected to PC server!"
                        CommandConnection.ConnectionState.DISCONNECTED -> "Disconnected from PC"
                        CommandConnection.ConnectionState.ERROR -> "Connection error occurred"
                    }
                    runOnUiThread {
                        Toast.makeText(this@NetworkClientTestActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    
    private fun testWifiConnection(ip: String = DEFAULT_PC_IP, port: Int = DEFAULT_PC_PORT) {
        if (!isBound || networkManager == null) {
            Log.w(TAG, "Service not bound, cannot test connection")
            Toast.makeText(this, "Service not ready, please wait", Toast.LENGTH_SHORT).show()
            return
        }
        
        Log.i(TAG, "Testing Wi-Fi connection to PC server at $ip:$port")
        updateConnectionStatus(CommandConnection.ConnectionState.CONNECTING)
        
        lifecycleScope.launch {
            try {
                val success = networkManager!!.connectWifi(ip, port)
                if (success) {
                    Log.i(TAG, "Successfully connected to PC server via Wi-Fi")
                    
                    // Test sending a message after connection
                    kotlinx.coroutines.delay(1000)
                    testSendMessage()
                    
                } else {
                    Log.e(TAG, "Failed to connect to PC server via Wi-Fi")
                    updateConnectionStatus(CommandConnection.ConnectionState.ERROR)
                    runOnUiThread {
                        Toast.makeText(this@NetworkClientTestActivity, 
                            "Failed to connect to PC at $ip:$port", 
                            Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during Wi-Fi connection test", e)
                updateConnectionStatus(CommandConnection.ConnectionState.ERROR)
                runOnUiThread {
                    Toast.makeText(this@NetworkClientTestActivity, 
                        "Connection error: ${e.message}", 
                        Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun testSendMessage() {
        lifecycleScope.launch {
            try {
                Log.i(TAG, "Testing message sending")
                val sent = networkManager?.sendResponse("PING")
                if (sent == true) {
                    Log.i(TAG, "Successfully sent PING message")
                    runOnUiThread {
                        Toast.makeText(this@NetworkClientTestActivity, "PING message sent successfully", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.w(TAG, "Failed to send PING message")
                    runOnUiThread {
                        Toast.makeText(this@NetworkClientTestActivity, "Failed to send PING message", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during message send test", e)
                runOnUiThread {
                    Toast.makeText(this@NetworkClientTestActivity, "Message send error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun testBluetoothConnection() {
        if (!isBound || networkManager == null) {
            Log.w(TAG, "Service not bound, cannot test connection")
            Toast.makeText(this, "Service not ready, please wait", Toast.LENGTH_SHORT).show()
            return
        }
        
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth not available on this device")
            Toast.makeText(this, "Bluetooth not available on this device", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!bluetoothAdapter.isEnabled) {
            Log.w(TAG, "Bluetooth is not enabled")
            Toast.makeText(this, "Please enable Bluetooth first", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Get paired devices and try to connect to the first one (for testing)
        try {
            val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
            if (pairedDevices.isNotEmpty()) {
                val testDevice = pairedDevices.first()
                Log.i(TAG, "Testing Bluetooth connection to ${testDevice.name} (${testDevice.address})")
                
                updateConnectionStatus(CommandConnection.ConnectionState.CONNECTING)
                Toast.makeText(this, "Connecting to ${testDevice.name}...", Toast.LENGTH_SHORT).show()
                
                lifecycleScope.launch {
                    try {
                        val success = networkManager!!.connectBluetooth(testDevice)
                        if (success) {
                            Log.i(TAG, "Successfully connected via Bluetooth")
                            
                        } else {
                            Log.e(TAG, "Failed to connect via Bluetooth")
                            updateConnectionStatus(CommandConnection.ConnectionState.ERROR)
                            runOnUiThread {
                                Toast.makeText(this@NetworkClientTestActivity, 
                                    "Failed to connect to ${testDevice.name}", 
                                    Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception during Bluetooth connection test", e)
                        updateConnectionStatus(CommandConnection.ConnectionState.ERROR)
                        runOnUiThread {
                            Toast.makeText(this@NetworkClientTestActivity, 
                                "Bluetooth connection error: ${e.message}", 
                                Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                Log.w(TAG, "No paired Bluetooth devices found")
                Toast.makeText(this, "No paired Bluetooth devices found. Please pair a device first.", Toast.LENGTH_LONG).show()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Bluetooth permission denied", e)
            Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    
    fun getConnectionInfo(): String {
        return if (networkManager != null) {
            val baseInfo = networkManager?.getConnectionInfo()?.toString() ?: "No connection info"
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            val connectionState = if (::connectionStatusText.isInitialized) {
                connectionStatusText.text.toString()
            } else {
                "Unknown"
            }
            
            """
Connection Status: $connectionState
Last Updated: $timestamp
Service Bound: $isBound

Network Manager Info:
$baseInfo

Available Actions:
- Connect via Wi-Fi using IP and port
- Connect via Bluetooth to paired devices
- Send test messages when connected
- Monitor connection state changes
            """.trimIndent()
        } else {
            "NetworkManager not available - Service not bound"
        }
    }
}