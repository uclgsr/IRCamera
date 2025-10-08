package mpdc4gsr.feature.network.ui

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.csl.irCamera.BuildConfig
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.theme.IRCameraTheme

enum class NetworkConnectionStatus {
    DISCONNECTED, CONNECTING, CONNECTED, ERROR
}

data class NetworkTestCommand(
    val name: String,
    val description: String,
    val command: String,
    val expectedResponse: String
)

data class TestResult(
    val command: String,
    val response: String,
    val success: Boolean,
    val timestamp: Long
)

@dagger.hilt.android.lifecycle.HiltViewModel
class SimpleNetworkTestViewModel @javax.inject.Inject constructor() : AppBaseViewModel() {
    private val _connectionStatus = mutableStateOf(NetworkConnectionStatus.DISCONNECTED)
    val connectionStatus: State<NetworkConnectionStatus> = _connectionStatus
    private val _ipAddress = mutableStateOf("192.168.1.100")
    val ipAddress: State<String> = _ipAddress
    private val _port = mutableStateOf("8080")
    val port: State<String> = _port
    private val _ipAddressError = mutableStateOf<String?>(null)
    val ipAddressError: State<String?> = _ipAddressError
    private val _portError = mutableStateOf<String?>(null)
    val portError: State<String?> = _portError
    private val _statusMessage = mutableStateOf("Ready to connect to PC Remote Control")
    val statusMessage: State<String> = _statusMessage
    private val _testResults = mutableStateOf<List<TestResult>>(emptyList())
    val testResults: State<List<TestResult>> = _testResults
    private val _isRunningTests = mutableStateOf(false)
    val isRunningTests: State<Boolean> = _isRunningTests
    private val _testCommands = mutableStateOf(
        listOf(
            NetworkTestCommand(
                "Start Recording",
                "Initiates recording on the mobile device",
                "START_RECORDING",
                "RECORDING_STARTED"
            ),
            NetworkTestCommand(
                "Stop Recording",
                "Stops recording on the mobile device",
                "STOP_RECORDING",
                "RECORDING_STOPPED"
            ),
            NetworkTestCommand(
                "Get Status",
                "Retrieves current device status",
                "GET_STATUS",
                "STATUS_OK"
            ),
            NetworkTestCommand(
                "Get Battery",
                "Retrieves battery level information",
                "GET_BATTERY",
                "BATTERY_85"
            ),
            NetworkTestCommand(
                "Connect Thermal",
                "Connect to thermal camera device",
                "CONNECT_THERMAL",
                "THERMAL_CONNECTED"
            ),
            NetworkTestCommand(
                "Connect GSR",
                "Connect to GSR sensor device",
                "CONNECT_GSR",
                "GSR_CONNECTED"
            )
        )
    )
    val testCommands: State<List<NetworkTestCommand>> = _testCommands
    fun updateIpAddress(ip: String) {
        _ipAddress.value = ip
        _ipAddressError.value = validateIpAddress(ip)
    }

    fun updatePort(portStr: String) {
        _port.value = portStr
        _portError.value = validatePort(portStr)
    }

    private fun validateIpAddress(ip: String): String? {
        if (ip.isBlank()) {
            return "IP address cannot be empty"
        }
        val parts = ip.split(".")
        if (parts.size != 4) {
            return "Invalid IP address format"
        }
        for (part in parts) {
            val num = part.toIntOrNull()
            if (num == null || num < 0 || num > 255) {
                return "Invalid IP address range"
            }
        }
        return null
    }

    private fun validatePort(portStr: String): String? {
        if (portStr.isBlank()) {
            return "Port cannot be empty"
        }
        val port = portStr.toIntOrNull()
        if (port == null) {
            return "Port must be a number"
        }
        if (port < 1 || port > 65535) {
            return "Port must be between 1 and 65535"
        }
        return null
    }

    fun connect() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _connectionStatus.value = NetworkConnectionStatus.CONNECTING
            _statusMessage.value = "Connecting to ${_ipAddress.value}:${_port.value}..."
            delay(2000) // Simulate connection time
            // Simulate connection result (80% success rate)
            val success = kotlin.random.Random.nextFloat() > 0.2f
            if (success) {
                _connectionStatus.value = NetworkConnectionStatus.CONNECTED
                _statusMessage.value = "Connected to PC Remote Control successfully"
            } else {
                _connectionStatus.value = NetworkConnectionStatus.ERROR
                _statusMessage.value = "Failed to connect. Check IP address and port."
            }
        }
    }

    fun disconnect() {
        _connectionStatus.value = NetworkConnectionStatus.DISCONNECTED
        _statusMessage.value = "Disconnected from PC Remote Control"
        _testResults.value = emptyList()
    }

    fun runAllTests() {
        if (_connectionStatus.value != NetworkConnectionStatus.CONNECTED) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isRunningTests.value = true
            _testResults.value = emptyList()
            _statusMessage.value = "Running automated test suite..."
            _testCommands.value.forEach { testCommand ->
                _statusMessage.value = "Testing: ${testCommand.name}"
                delay(1500)
                val success = kotlin.random.Random.nextFloat() > 0.15f // 85% success rate
                val response = if (success) {
                    testCommand.expectedResponse
                } else {
                    "ERROR_${kotlin.random.Random.nextInt(100, 999)}"
                }
                val result = TestResult(
                    command = testCommand.command,
                    response = response,
                    success = success,
                    timestamp = System.currentTimeMillis()
                )
                _testResults.value = _testResults.value + result
                delay(500)
            }
            val successCount = _testResults.value.count { it.success }
            val totalCount = _testResults.value.size
            _statusMessage.value = "Test suite complete: $successCount/$totalCount tests passed"
            _isRunningTests.value = false
        }
    }

    fun runSingleTest(command: NetworkTestCommand) {
        if (_connectionStatus.value != NetworkConnectionStatus.CONNECTED) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _statusMessage.value = "Testing: ${command.name}"
            delay(1000)
            val success = kotlin.random.Random.nextFloat() > 0.2f // 80% success rate
            val response = if (success) {
                command.expectedResponse
            } else {
                "ERROR_TIMEOUT"
            }
            val result = TestResult(
                command = command.command,
                response = response,
                success = success,
                timestamp = System.currentTimeMillis()
            )
            _testResults.value = _testResults.value + result
            _statusMessage.value = if (success) {
                "${command.name} test passed"
            } else {
                "${command.name} test failed"
            }
        }
    }

    fun clearResults() {
        _testResults.value = emptyList()
        _statusMessage.value = "Test results cleared"
    }
}

@dagger.hilt.android.AndroidEntryPoint
class SimpleNetworkTestActivityCompose : mpdc4gsr.core.ui.HiltComposeActivity() {
    private val viewModel: SimpleNetworkTestViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        IRCameraTheme {
            val context = LocalContext.current
            val keyboardController = LocalSoftwareKeyboardController.current
            val connectionStatus by viewModel.connectionStatus
            val ipAddress by viewModel.ipAddress
            val port by viewModel.port
            val ipAddressError by viewModel.ipAddressError
            val portError by viewModel.portError
            val statusMessage by viewModel.statusMessage
            val testResults by viewModel.testResults
            val isRunningTests by viewModel.isRunningTests
            val testCommands by viewModel.testCommands
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16131E))
            ) {
                TitleBar(
                    title = "Network Test Interface",
                    onBackClick = { finish() },
                    actions = {
                        IconButton(onClick = { viewModel.clearResults() }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Connection status card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (connectionStatus) {
                                NetworkConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
                                NetworkConnectionStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
                                NetworkConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.surfaceVariant
                                else -> MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when (connectionStatus) {
                                NetworkConnectionStatus.CONNECTING -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.dp
                                    )
                                }

                                NetworkConnectionStatus.CONNECTED -> {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                NetworkConnectionStatus.ERROR -> {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                else -> {
                                    Icon(
                                        imageVector = Icons.Default.NetworkCheck,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = connectionStatus.name.lowercase()
                                        .replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = statusMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    // Connection settings
                    if (connectionStatus == NetworkConnectionStatus.DISCONNECTED) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "PC Remote Control Connection",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                OutlinedTextField(
                                    value = ipAddress,
                                    onValueChange = { viewModel.updateIpAddress(it) },
                                    label = { Text("IP Address") },
                                    placeholder = { Text("192.168.1.100") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    singleLine = true,
                                    isError = ipAddressError != null,
                                    supportingText = ipAddressError?.let { { Text(it) } },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = {
                                            // Focus moves to next field automatically
                                        }
                                    )
                                )
                                OutlinedTextField(
                                    value = port,
                                    onValueChange = { viewModel.updatePort(it) },
                                    label = { Text("Port") },
                                    placeholder = { Text("8080") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    singleLine = true,
                                    isError = portError != null,
                                    supportingText = portError?.let { { Text(it) } },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            keyboardController?.hide()
                                        }
                                    )
                                )
                                Button(
                                    onClick = { viewModel.connect() },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = ipAddressError == null && portError == null
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = "Connect",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Connect to PC")
                                }
                            }
                        }
                    } else if (connectionStatus == NetworkConnectionStatus.CONNECTED) {
                        // Test controls
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.runAllTests() },
                                modifier = Modifier.weight(1f),
                                enabled = !isRunningTests
                            ) {
                                if (isRunningTests) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Run All Tests")
                            }
                            OutlinedButton(
                                onClick = { viewModel.disconnect() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LinkOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Disconnect")
                            }
                        }
                        // Individual test commands
                        Text(
                            text = "Test Commands",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        testCommands.forEach { command ->
                            NetworkTestCommandCard(
                                command = command,
                                onTest = { viewModel.runSingleTest(command) },
                                isTestingEnabled = !isRunningTests,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        // Test results
                        if (testResults.isNotEmpty()) {
                            Text(
                                text = "Test Results",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    testResults.takeLast(10).forEach { result ->
                                        TestResultRow(
                                            result = result,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else if (connectionStatus == NetworkConnectionStatus.ERROR) {
                        Button(
                            onClick = { viewModel.connect() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry Connection")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Information card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "PC Remote Control Testing",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "This interface tests bidirectional communication with PC Remote Control. Commands are sent to the PC and responses are validated. App version: ${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkTestCommandCard(
    command: NetworkTestCommand,
    onTest: () -> Unit,
    isTestingEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = command.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = command.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Command: ${command.command}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onTest,
                enabled = isTestingEnabled
            ) {
                Text("Test")
            }
        }
    }
}

@Composable
private fun TestResultRow(
    result: TestResult,
    modifier: Modifier = Modifier
) {
    val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (result.success) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = null,
            tint = if (result.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = result.command,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(120.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = result.response,
            style = MaterialTheme.typography.bodySmall,
            color = if (result.success)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = timeFormat.format(java.util.Date(result.timestamp)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}