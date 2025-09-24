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
        setContentView(R.layout.activity_main) // Using existing layout for simplicity
        
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
    
    private fun setupUI() {
        // Create simple buttons programmatically since we're using existing layout
        title = "Network Client Test"
        
        Log.i(TAG, "Network Client Test Activity started")
        Toast.makeText(this, "Network Client Test - Check logs for functionality", Toast.LENGTH_LONG).show()
        
        // Simulate Wi-Fi connection test after a delay to ensure service is bound
        lifecycleScope.launch {
            kotlinx.coroutines.delay(2000) // Wait for service binding
            testWifiConnection()
        }
    }
    
    private fun observeConnectionState() {
        networkManager?.let { manager ->
            lifecycleScope.launch {
                manager.connectionState.collect { state ->
                    Log.i(TAG, "Connection state changed: $state")
                    runOnUiThread {
                        val message = when (state) {
                            CommandConnection.ConnectionState.CONNECTING -> "Connecting to PC..."
                            CommandConnection.ConnectionState.CONNECTED -> "Connected to PC server!"
                            CommandConnection.ConnectionState.DISCONNECTED -> "Disconnected from PC"
                            CommandConnection.ConnectionState.ERROR -> "Connection error occurred"
                        }
                        Toast.makeText(this@NetworkClientTestActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    
    private fun testWifiConnection() {
        if (!isBound || networkManager == null) {
            Log.w(TAG, "Service not bound, cannot test connection")
            return
        }
        
        Log.i(TAG, "Testing Wi-Fi connection to PC server at $DEFAULT_PC_IP:$DEFAULT_PC_PORT")
        
        lifecycleScope.launch {
            try {
                val success = networkManager!!.connectWifi(DEFAULT_PC_IP, DEFAULT_PC_PORT)
                if (success) {
                    Log.i(TAG, "Successfully connected to PC server via Wi-Fi")
                    
                    // Test sending a message
                    kotlinx.coroutines.delay(1000)
                    testSendMessage()
                    
                    // Disconnect after 10 seconds
                    kotlinx.coroutines.delay(10000)
                    networkManager!!.disconnect()
                } else {
                    Log.e(TAG, "Failed to connect to PC server via Wi-Fi")
                    Toast.makeText(this@NetworkClientTestActivity, 
                        "Failed to connect to PC at $DEFAULT_PC_IP:$DEFAULT_PC_PORT", 
                        Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during Wi-Fi connection test", e)
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
                } else {
                    Log.w(TAG, "Failed to send PING message")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during message send test", e)
            }
        }
    }
    
    private fun testBluetoothConnection() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth not available on this device")
            return
        }
        
        if (!bluetoothAdapter.isEnabled) {
            Log.w(TAG, "Bluetooth is not enabled")
            return
        }
        
        // Get paired devices and try to connect to the first one (for testing)
        try {
            val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
            if (pairedDevices.isNotEmpty()) {
                val testDevice = pairedDevices.first()
                Log.i(TAG, "Testing Bluetooth connection to ${testDevice.name} (${testDevice.address})")
                
                lifecycleScope.launch {
                    try {
                        val success = networkManager!!.connectBluetooth(testDevice)
                        if (success) {
                            Log.i(TAG, "Successfully connected via Bluetooth")
                            
                            // Disconnect after 5 seconds
                            kotlinx.coroutines.delay(5000)
                            networkManager!!.disconnect()
                        } else {
                            Log.e(TAG, "Failed to connect via Bluetooth")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception during Bluetooth connection test", e)
                    }
                }
            } else {
                Log.w(TAG, "No paired Bluetooth devices found")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Bluetooth permission denied", e)
        }
    }
    
    fun getConnectionInfo(): String {
        return networkManager?.getConnectionInfo()?.toString() ?: "NetworkManager not available"
    }
}