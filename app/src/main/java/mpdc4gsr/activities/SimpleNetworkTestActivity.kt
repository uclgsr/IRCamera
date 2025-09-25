package mpdc4gsr.activities

import android.os.Bundle
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
import mpdc4gsr.network.CommandConnection
import mpdc4gsr.network.MockRecordingController
import mpdc4gsr.network.NetworkManager
import mpdc4gsr.network.SimpleCommandHandler
import mpdc4gsr.network.TcpClient

/**
 * Simple test activity demonstrating PC Remote Control and Bidirectional Telemetry.
 * Uses simplified interfaces to avoid complex dependencies while showing the core networking functionality.
 */
class SimpleNetworkTestActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SimpleNetworkTestActivity"
        private const val DEFAULT_PC_IP = "192.168.1.100"
        private const val DEFAULT_PC_PORT = 8080
    }

    // Simplified components for testing
    private val mockController = MockRecordingController()
    private var tcpClient: TcpClient? = null
    private var commandHandler: SimpleCommandHandler? = null

    // UI Components
    private lateinit var connectionStatusIndicator: ImageView
    private lateinit var connectionStatusText: TextView
    private lateinit var ipAddressInput: EditText
    private lateinit var portInput: EditText
    private lateinit var connectButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var testCommandsButton: Button
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network_client_test)

        initializeViews()
        setupClickListeners()
        updateUI()
    }

    private fun initializeViews() {
        connectionStatusIndicator = findViewById(R.id.connection_status_indicator)
        connectionStatusText = findViewById(R.id.connection_status_text)
        ipAddressInput = findViewById(R.id.ip_address_input)
        portInput = findViewById(R.id.port_input)
        connectButton = findViewById(R.id.connect_wifi_button)
        disconnectButton = findViewById(R.id.disconnect_button)
        testCommandsButton = findViewById(R.id.test_ping_button)
        statusText = findViewById(R.id.connection_info_text)

        // Set default values
        ipAddressInput.setText(DEFAULT_PC_IP)
        portInput.setText(DEFAULT_PC_PORT.toString())
    }

    private fun setupClickListeners() {
        connectButton.setOnClickListener {
            val ip = ipAddressInput.text.toString().trim()
            val portStr = portInput.text.toString().trim()

            if (ip.isEmpty() || portStr.isEmpty()) {
                Toast.makeText(this, "Please enter IP address and port", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val port = portStr.toInt()
                connectToPC(ip, port)
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Invalid port number", Toast.LENGTH_SHORT).show()
            }
        }

        disconnectButton.setOnClickListener {
            disconnectFromPC()
        }

        testCommandsButton.setOnClickListener {
            testCommands()
        }
    }

    private fun connectToPC(ip: String, port: Int) {
        lifecycleScope.launch {
            try {
                Log.i(TAG, "Connecting to PC at $ip:$port")
                statusText.text = "Connecting to $ip:$port..."

                tcpClient = TcpClient(ip, port)

                // Set up message callback
                tcpClient?.setMessageCallback { message ->
                    runOnUiThread {
                        Log.d(TAG, "Received from PC: $message")
                        val currentText = statusText.text.toString()
                        statusText.text = "$currentText\nPC->Phone: $message"
                    }
                }

                // Set up connection state callback
                tcpClient?.setConnectionCallback { state ->
                    runOnUiThread {
                        updateConnectionStatus(state)
                    }
                }

                val connected = tcpClient?.connect() ?: false

                if (connected) {
                    Log.i(TAG, "Successfully connected to PC")
                    Toast.makeText(this@SimpleNetworkTestActivity, "Connected to PC", Toast.LENGTH_SHORT).show()

                    // Send initial handshake
                    tcpClient?.sendMessage("HELLO device=SimpleNetworkTest sensors=[Mock]")

                    // Setup command handling (simplified)
                    tcpClient?.setMessageCallback { message ->
                        runOnUiThread {
                            Log.d(TAG, "Received from PC: $message")
                            handlePCMessage(message)
                        }
                    }

                } else {
                    Log.e(TAG, "Failed to connect to PC")
                    Toast.makeText(this@SimpleNetworkTestActivity, "Failed to connect", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to PC", e)
                Toast.makeText(this@SimpleNetworkTestActivity, "Connection error: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }

            updateUI()
        }
    }

    private fun disconnectFromPC() {
        lifecycleScope.launch {
            try {
                Log.i(TAG, "Disconnecting from PC")
                tcpClient?.disconnect()
                tcpClient?.cleanup()
                tcpClient = null

                Toast.makeText(this@SimpleNetworkTestActivity, "Disconnected", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting", e)
            }

            updateUI()
        }
    }

    private fun testCommands() {
        lifecycleScope.launch {
            try {
                val client = tcpClient
                if (client == null || !client.isConnected()) {
                    Toast.makeText(this@SimpleNetworkTestActivity, "Not connected to PC", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Test PING
                client.sendMessage("PING")
                appendStatus("Phone->PC: PING")

                kotlinx.coroutines.delay(1000)

                // Test GET_STATUS
                client.sendMessage("GET_STATUS")
                appendStatus("Phone->PC: GET_STATUS")

                kotlinx.coroutines.delay(1000)

                // Test START if not recording
                if (!mockController.isRecording) {
                    client.sendMessage("START")
                    appendStatus("Phone->PC: START")
                } else {
                    client.sendMessage("STOP")
                    appendStatus("Phone->PC: STOP")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error testing commands", e)
                Toast.makeText(this@SimpleNetworkTestActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handlePCMessage(message: String) {
        appendStatus("PC->Phone: $message")

        lifecycleScope.launch {
            try {
                val response = when {
                    message.startsWith("START") -> {
                        val success = mockController.startRecording()
                        if (success) "START-ACK session_id=demo_${System.currentTimeMillis()}"
                        else "ERROR cmd=START code=START_FAILED"
                    }

                    message.startsWith("STOP") -> {
                        val success = mockController.stopRecording()
                        if (success) "STOP-ACK msg=\"Recording stopped\""
                        else "ERROR cmd=STOP code=NOT_RECORDING"
                    }

                    message.startsWith("PING") -> "PONG"

                    message.startsWith("GET_STATUS") -> {
                        val status = mockController.getStatus()
                        "STATUS ${org.json.JSONObject(status)}"
                    }

                    message.startsWith("SYNC") -> {
                        "SYNC-RESP t_ph=${System.currentTimeMillis()}"
                    }

                    else -> null
                }

                response?.let {
                    tcpClient?.sendMessage(it)
                    appendStatus("Phone->PC: $it")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error handling PC message: $message", e)
            }
        }
    }

    private fun appendStatus(text: String) {
        runOnUiThread {
            val currentText = statusText.text.toString()
            val newText = if (currentText.isEmpty()) text else "$currentText\n$text"
            statusText.text = newText

            // Keep only last 20 lines
            val lines = newText.split("\n")
            if (lines.size > 20) {
                statusText.text = lines.takeLast(20).joinToString("\n")
            }
        }
    }

    private fun updateConnectionStatus(state: CommandConnection.ConnectionState) {
        when (state) {
            CommandConnection.ConnectionState.CONNECTED -> {
                connectionStatusIndicator.setImageResource(android.R.drawable.presence_online)
                connectionStatusText.text = "Connected"
            }

            CommandConnection.ConnectionState.CONNECTING -> {
                connectionStatusIndicator.setImageResource(android.R.drawable.presence_away)
                connectionStatusText.text = "Connecting..."
            }

            CommandConnection.ConnectionState.DISCONNECTED -> {
                connectionStatusIndicator.setImageResource(android.R.drawable.presence_offline)
                connectionStatusText.text = "Disconnected"
            }

            CommandConnection.ConnectionState.ERROR -> {
                connectionStatusIndicator.setImageResource(android.R.drawable.stat_notify_error)
                connectionStatusText.text = "Error"
            }
        }
    }

    private fun updateUI() {
        val isConnected = tcpClient?.isConnected() ?: false

        connectButton.isEnabled = !isConnected
        disconnectButton.isEnabled = isConnected
        testCommandsButton.isEnabled = isConnected

        ipAddressInput.isEnabled = !isConnected
        portInput.isEnabled = !isConnected
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.launch {
            tcpClient?.disconnect()
            tcpClient?.cleanup()
        }
    }
}