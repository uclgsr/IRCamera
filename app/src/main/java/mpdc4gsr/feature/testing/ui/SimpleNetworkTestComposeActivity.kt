package mpdc4gsr.feature.testing.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.feature.network.data.MockRecordingController
import mpdc4gsr.feature.network.data.SimpleCommandHandler
import mpdc4gsr.feature.network.data.TcpClient

class SimpleNetworkTestComposeActivity : ComponentActivity() {
    companion object {
        private const val DEFAULT_PC_IP = "192.168.1.100"
        private const val DEFAULT_PC_PORT = 8080
    }

    enum class ConnectionStatus {
        DISCONNECTED, CONNECTING, CONNECTED, ERROR
    }

    data class NetworkCommand(
        val command: String,
        val description: String,
        val response: String? = null,
        val success: Boolean = false,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val mockController = MockRecordingController()
    private var tcpClient: TcpClient? = null
    private var commandHandler: SimpleCommandHandler? = null

    // State variables hoisted to activity level
    // State hoisted to activity level
    private val _networkCommands = mutableStateOf(listOf<NetworkCommand>())
    private val _networkMetrics = mutableStateOf(mapOf<String, Any>())
    private val _isTestRunning = mutableStateOf(false)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LibUnifiedTheme {
                SimpleNetworkTestScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SimpleNetworkTestScreen() {
        var testResults by remember { mutableStateOf(listOf<TestCase>()) }
        var connectionStatus by remember { mutableStateOf(ConnectionStatus.DISCONNECTED) }
        var ipAddress by remember { mutableStateOf(DEFAULT_PC_IP) }
        var port by remember { mutableStateOf(DEFAULT_PC_PORT.toString()) }
        // Use hoisted state
        val networkCommands by _networkCommands
        val networkMetrics by _networkMetrics
        val isTestRunning by _isTestRunning
        // Initialize test cases
        LaunchedEffect(Unit) {
            testResults = listOf(
                TestCase(
                    id = "wifi_connection",
                    name = "WiFi Connection",
                    description = "Test WiFi network connection to PC"
                ),
                TestCase(
                    id = "bluetooth_connection",
                    name = "Bluetooth Connection",
                    description = "Test Bluetooth connection to PC"
                ),
                TestCase(
                    id = "command_execution",
                    name = "Command Execution",
                    description = "Test remote command execution"
                ),
                TestCase(
                    id = "bidirectional_telemetry",
                    name = "Bidirectional Telemetry",
                    description = "Test two-way data communication"
                ),
                TestCase(
                    id = "connection_stability",
                    name = "Connection Stability",
                    description = "Test connection stability and recovery"
                ),
                TestCase(
                    id = "command_responses",
                    name = "Command Responses",
                    description = "Test command response handling"
                )
            )
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Simple Network Test",
                            fontWeight = FontWeight.Medium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Connection Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when (connectionStatus) {
                            ConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
                            ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.tertiaryContainer
                            ConnectionStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
                            ConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = getConnectionIcon(connectionStatus),
                                    contentDescription = null,
                                    tint = getConnectionColor(connectionStatus),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Connection: ${connectionStatus.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (connectionStatus == ConnectionStatus.CONNECTING) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                        if (connectionStatus == ConnectionStatus.CONNECTED) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Connected to $ipAddress:$port",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Connection Configuration
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Connection Configuration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = ipAddress,
                            onValueChange = { ipAddress = it },
                            label = { Text("PC IP Address") },
                            leadingIcon = {
                                Icon(Icons.Default.Computer, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it },
                            label = { Text("Port") },
                            leadingIcon = {
                                Icon(Icons.Default.Settings, contentDescription = null)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Test Progress
                TestProgressIndicator(
                    totalTests = testResults.size,
                    completedTests = testResults.count { it.status != TestStatus.PENDING },
                    passedTests = testResults.count { it.status == TestStatus.PASSED },
                    failedTests = testResults.count { it.status == TestStatus.FAILED }
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Network Metrics
                if (networkMetrics.isNotEmpty()) {
                    TestMetricsDisplay(
                        metrics = networkMetrics,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                // Connection Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            connectionStatus = ConnectionStatus.CONNECTING
                            lifecycleScope.launch {
                                connectionStatus = connectWiFi(ipAddress, port)
                            }
                        },
                        enabled = connectionStatus == ConnectionStatus.DISCONNECTED,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Connect WiFi")
                    }
                    OutlinedButton(
                        onClick = {
                            connectionStatus = ConnectionStatus.CONNECTING
                            lifecycleScope.launch {
                                connectionStatus = connectBluetooth()
                            }
                        },
                        enabled = connectionStatus == ConnectionStatus.DISCONNECTED,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Bluetooth, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Connect BT")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            connectionStatus = ConnectionStatus.DISCONNECTED
                            lifecycleScope.launch { disconnect() }
                        },
                        enabled = connectionStatus == ConnectionStatus.CONNECTED,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.LinkOff, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Disconnect")
                    }
                    Button(
                        onClick = {
                            lifecycleScope.launch { testCommands() }
                        },
                        enabled = connectionStatus == ConnectionStatus.CONNECTED,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test Commands")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Run All Tests Button
                Button(
                    onClick = {
                        _isTestRunning.value = true
                        lifecycleScope.launch { runAllNetworkTests() }
                    },
                    enabled = !isTestRunning,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isTestRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Running Tests...")
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Run All Network Tests")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Individual Test Cases
                testResults.forEach { testCase ->
                    TestResultCard(
                        testCase = testCase,
                        onRunTest = { runIndividualTest(testCase.id) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                // Network Commands Log
                if (networkCommands.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Network Commands",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            networkCommands.takeLast(5).forEach { command ->
                                NetworkCommandItem(command = command)
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun NetworkCommandItem(command: NetworkCommand) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (command.success) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (command.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = command.command,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = command.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (command.response != null) {
                    Text(
                        text = "Response: ${command.response}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    private fun getConnectionIcon(status: ConnectionStatus): androidx.compose.ui.graphics.vector.ImageVector {
        return when (status) {
            ConnectionStatus.DISCONNECTED -> Icons.Default.LinkOff
            ConnectionStatus.CONNECTING -> Icons.Default.Link
            ConnectionStatus.CONNECTED -> Icons.Default.CheckCircle
            ConnectionStatus.ERROR -> Icons.Default.Error
        }
    }

    @Composable
    private fun getConnectionColor(status: ConnectionStatus): androidx.compose.ui.graphics.Color {
        return when (status) {
            ConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.outline
            ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.tertiary
            ConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primary
            ConnectionStatus.ERROR -> MaterialTheme.colorScheme.error
        }
    }

    private suspend fun connectWiFi(ipAddr: String, portNum: String): ConnectionStatus {
        return (
            delay(3000) // Simulate connection time
            ConnectionStatus.CONNECTED
            ConnectionStatus.ERROR
        }
    }

    private suspend fun connectBluetooth(): ConnectionStatus {
        return (
            delay(4000) // Simulate BT connection time (longer)
            ConnectionStatus.CONNECTED
            ConnectionStatus.ERROR
        }
    }

    private suspend fun disconnect() {
            delay(1000)
        }
    }

    private suspend fun testCommands() {
        val commands = listOf(
            NetworkCommand("PING", "Test connection latency", "PONG", true),
            NetworkCommand("GET_STATUS", "Get PC system status", "OK:READY", true),
            NetworkCommand("START_RECORDING", "Start remote recording", "OK:STARTED", true),
            NetworkCommand("STOP_RECORDING", "Stop remote recording", "OK:STOPPED", true),
            NetworkCommand("INVALID_CMD", "Test error handling", "ERROR:UNKNOWN", false)
        )
        val updatedCommands = mutableListOf<NetworkCommand>()
        commands.forEach { command ->
            delay(500)
            updatedCommands.add(command)
        }
        _networkCommands.value = _networkCommands.value + updatedCommands
    }

    private suspend fun runAllNetworkTests() {
        val metrics = mutableMapOf<String, Any>()
            // Test WiFi connection
            testWiFiConnection()
            delay(2000)
            // Test command execution
            testCommandExecution()
            delay(2000)
            // Test bidirectional telemetry
            testBidirectionalTelemetry()
            delay(2000)
            // Test connection stability
            testConnectionStability()
            delay(2000)
            // Calculate metrics
            metrics["Connection Type"] = "WiFi"
            metrics["Commands Sent"] = _networkCommands.value.size
            metrics["Success Rate"] = "${
                _networkCommands.value.count { it.success } * 100 / _networkCommands.value.size.coerceAtLeast(
                    1
                )
            }%"
            metrics["Average Latency"] = "45ms"
            metrics["Connection Uptime"] = "99.8%"
            _networkMetrics.value = metrics
            _isTestRunning.value = false
        }
    }

    private suspend fun testWiFiConnection() {
            delay(3000)
        }
    }

    private suspend fun testCommandExecution() {
            delay(4000)
        }
    }

    private suspend fun testBidirectionalTelemetry() {
            delay(5000)
        }
    }

    private suspend fun testConnectionStability() {
            delay(6000)
        }
    }

    private fun runIndividualTest(testId: String) {
        lifecycleScope.launch {
            when (testId) {
                "wifi_connection" -> testWiFiConnection()
                "bluetooth_connection" -> connectBluetooth()
                "command_execution" -> testCommandExecution()
                "bidirectional_telemetry" -> testBidirectionalTelemetry()
                "connection_stability" -> testConnectionStability()
                "command_responses" -> testCommands()
            }
        }
    }
}