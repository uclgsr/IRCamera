package mpdc4gsr.feature.network.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.core.RecordingService
import mpdc4gsr.core.ui.AppBaseViewModel
import mpdc4gsr.feature.network.data.CommandConnection
import mpdc4gsr.feature.network.data.NetworkManager

class NetworkClientTestViewModel : AppBaseViewModel() {
    private val _networkConnectionState = MutableStateFlow(CommandConnection.ConnectionState.DISCONNECTED)
    val networkConnectionState: StateFlow<CommandConnection.ConnectionState> = _networkConnectionState.asStateFlow()
    private val _ipAddress = MutableStateFlow("192.168.1.100")
    val ipAddress: StateFlow<String> = _ipAddress.asStateFlow()
    private val _port = MutableStateFlow("8080")
    val port: StateFlow<String> = _port.asStateFlow()
    private val _connectionInfo = MutableStateFlow("")
    val connectionInfo: StateFlow<String> = _connectionInfo.asStateFlow()

    // Data classes for network testing (shared with NetworkClientTestComposeActivity)
    enum class TestStatus(val displayName: String) {
        PASS("Pass"),
        FAIL("Fail"),
        WARNING("Warning"),
        PENDING("Pending")
    }

    enum class NetworkTestType { CONNECTION, LATENCY, THROUGHPUT, RELIABILITY }
    data class NetworkConfiguration(
        val serverAddress: String = "192.168.1.100",
        val port: Int = 8080,
        val timeoutMs: Long = 5000,
        val retryAttempts: Int = 3
    )

    data class NetworkTestCategory(
        val name: String,
        val description: String,
        val type: NetworkTestType,
        val testCount: Int,
        val lastResult: TestStatus
    )

    data class NetworkTestResult(
        val testName: String,
        val status: TestStatus,
        val timestamp: String,
        val duration: Long,
        val details: String
    )

    // UI State for NetworkClientTestComposeActivity
    data class NetworkTestUiState(
        val isTestRunning: Boolean = false,
        val currentTest: String = "",
        val testProgress: Float = 0f,
        val networkStatus: String = "Disconnected",
        val testCategories: List<NetworkTestCategory> = emptyList(),
        val testResults: List<NetworkTestResult> = emptyList(),
        val networkConfiguration: NetworkConfiguration = NetworkConfiguration(),
        val error: String? = null
    )

    private val _networkTestUiState = MutableStateFlow(NetworkTestUiState())
    val networkTestUiState: StateFlow<NetworkTestUiState> = _networkTestUiState.asStateFlow()
    fun updateConnectionState(state: CommandConnection.ConnectionState) {
        _networkConnectionState.value = state
    }

    fun updateIpAddress(ip: String) {
        _ipAddress.value = ip
    }

    fun updatePort(port: String) {
        _port.value = port
    }

    fun updateConnectionInfo(info: String) {
        _connectionInfo.value = info
    }

    // Methods for NetworkClientTestComposeActivity
    fun startComprehensiveTest() {
        _networkTestUiState.value = _networkTestUiState.value.copy(isTestRunning = true)
    }

    fun stopTest() {
        _networkTestUiState.value = _networkTestUiState.value.copy(isTestRunning = false)
    }

    fun refreshNetworkStatus() {
        _networkTestUiState.value = _networkTestUiState.value.copy(
            networkStatus = when (_networkConnectionState.value) {
                CommandConnection.ConnectionState.CONNECTED -> "Connected"
                CommandConnection.ConnectionState.CONNECTING -> "Connecting"
                CommandConnection.ConnectionState.ERROR -> "Error"
                else -> "Disconnected"
            }
        )
    }

    fun runQuickNetworkTest() {
        // Stub implementation
    }

    fun runCategoryTest(category: NetworkTestCategory) {
        // Stub implementation
    }

    fun viewTestDetails(result: NetworkTestResult) {
        // Stub implementation
    }

    fun updateNetworkConfiguration(config: NetworkConfiguration) {
        _networkTestUiState.value = _networkTestUiState.value.copy(networkConfiguration = config)
        // Update IP and port from configuration
        _ipAddress.value = config.serverAddress
        _port.value = config.port.toString()
    }

    override fun clearError() {
        _networkTestUiState.value = _networkTestUiState.value.copy(error = null)
    }
}

class NetworkClientTestActivityCompose : BaseComposeActivity<NetworkClientTestViewModel>() {
    companion object {
        private const val DEFAULT_PC_IP = "192.168.1.100"
        private const val DEFAULT_PC_PORT = 8080
    }

    private lateinit var testViewModel: NetworkClientTestViewModel
    private var recordingService: RecordingService? = null
    private var networkManager: NetworkManager? = null
    private var isBound = false
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RecordingService.RecordingServiceBinder
            recordingService = binder.getService()
            networkManager = binder.getNetworkManager()
            isBound = true
            observeConnectionState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            recordingService = null
            networkManager = null
            isBound = false
        }
    }

    override fun createViewModel(): NetworkClientTestViewModel {
        testViewModel = NetworkClientTestViewModel()
        return testViewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Bind to RecordingService
        val serviceIntent = Intent(this, RecordingService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    private fun observeConnectionState() {
        networkManager?.let { manager ->
            lifecycleScope.launch {
                manager.connectionState.collect { state ->
                    testViewModel.updateConnectionState(state)
                    updateConnectionInfo()
                }
            }
        }
    }

    private fun updateConnectionInfo() {
        val info = networkManager?.let { manager ->
            when (val state = testViewModel.networkConnectionState.value) {
                CommandConnection.ConnectionState.CONNECTED -> {
                    "Connected to ${testViewModel.ipAddress.value}:${testViewModel.port.value}"
                }

                CommandConnection.ConnectionState.CONNECTING -> {
                    "Connecting to ${testViewModel.ipAddress.value}:${testViewModel.port.value}..."
                }

                CommandConnection.ConnectionState.ERROR -> {
                    "Connection failed to ${testViewModel.ipAddress.value}:${testViewModel.port.value}"
                }

                else -> "Not connected"
            }
        } ?: "Service not available"
        testViewModel.updateConnectionInfo(info)
    }

    private fun testWifiConnection(ip: String, port: Int) {
        lifecycleScope.launch {
            try {
                networkManager?.connectWifi(ip, port)
            } catch (e: Exception) {
                testViewModel.updateConnectionState(CommandConnection.ConnectionState.ERROR)
            }
        }
    }

    private fun testSendMessage() {
        lifecycleScope.launch {
            try {
                networkManager?.sendResponse("ping")
            } catch (e: Exception) {
            }
        }
    }

    private fun testBluetoothConnection() {
        // Placeholder for Bluetooth connection logic
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: NetworkClientTestViewModel) {
        val connectionState by testViewModel.networkConnectionState.collectAsState()
        val ipAddress by testViewModel.ipAddress.collectAsState()
        val port by testViewModel.port.collectAsState()
        val connectionInfo by viewModel.connectionInfo.collectAsState()
        val scrollState = rememberScrollState()
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Network Client Test",
                            fontWeight = FontWeight.Bold
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
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Connection Status Card
                ConnectionStatusCard(
                    connectionState = connectionState,
                    connectionInfo = connectionInfo
                )
                // Connection Configuration Card
                ConnectionConfigCard(
                    ipAddress = ipAddress,
                    port = port,
                    onIpAddressChange = viewModel::updateIpAddress,
                    onPortChange = viewModel::updatePort
                )
                // Action Buttons Card
                ActionButtonsCard(
                    connectionState = connectionState,
                    onConnectWifi = {
                        val portInt = port.toIntOrNull() ?: DEFAULT_PC_PORT
                        if (ipAddress.isNotEmpty() && portInt in 1..65535) {
                            testWifiConnection(ipAddress, portInt)
                        }
                    },
                    onTestPing = ::testSendMessage,
                    onConnectBluetooth = ::testBluetoothConnection,
                    onDisconnect = {
                        lifecycleScope.launch {
                            networkManager?.disconnect()
                        }
                    }
                )
                // Test Information Card
                TestInfoCard()
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    connectionState: CommandConnection.ConnectionState,
    connectionInfo: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (connectionState) {
                CommandConnection.ConnectionState.CONNECTED ->
                    MaterialTheme.colorScheme.primaryContainer

                CommandConnection.ConnectionState.ERROR ->
                    MaterialTheme.colorScheme.errorContainer

                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when (connectionState) {
                        CommandConnection.ConnectionState.CONNECTED -> Icons.Default.CheckCircle
                        CommandConnection.ConnectionState.CONNECTING -> Icons.Default.Sync
                        CommandConnection.ConnectionState.ERROR -> Icons.Default.Error
                        else -> Icons.Default.Circle
                    },
                    contentDescription = "Connection Status",
                    tint = when (connectionState) {
                        CommandConnection.ConnectionState.CONNECTED -> MaterialTheme.colorScheme.tertiary
                        CommandConnection.ConnectionState.CONNECTING -> MaterialTheme.colorScheme.secondary
                        CommandConnection.ConnectionState.ERROR -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.outline
                    }
                )
                Text(
                    text = when (connectionState) {
                        CommandConnection.ConnectionState.CONNECTED -> "Connected"
                        CommandConnection.ConnectionState.CONNECTING -> "Connecting..."
                        CommandConnection.ConnectionState.ERROR -> "Connection Error"
                        else -> "Disconnected"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (connectionInfo.isNotEmpty()) {
                Text(
                    text = connectionInfo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConnectionConfigCard(
    ipAddress: String,
    port: String,
    onIpAddressChange: (String) -> Unit,
    onPortChange: (String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Connection Configuration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = ipAddress,
                onValueChange = onIpAddressChange,
                label = { Text("PC IP Address") },
                placeholder = { Text("192.168.1.100") },
                leadingIcon = { Icon(Icons.Default.Computer, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = {
                        // Focus moves to next field automatically
                    }
                )
            )
            OutlinedTextField(
                value = port,
                onValueChange = onPortChange,
                label = { Text("Port") },
                placeholder = { Text("8080") },
                leadingIcon = { Icon(Icons.Default.Router, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
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
        }
    }
}

@Composable
private fun ActionButtonsCard(
    connectionState: CommandConnection.ConnectionState,
    onConnectWifi: () -> Unit,
    onTestPing: () -> Unit,
    onConnectBluetooth: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Network Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onConnectWifi,
                    modifier = Modifier.weight(1f),
                    enabled = connectionState == CommandConnection.ConnectionState.DISCONNECTED
                ) {
                    Icon(Icons.Default.Wifi, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WiFi")
                }
                Button(
                    onClick = onConnectBluetooth,
                    modifier = Modifier.weight(1f),
                    enabled = connectionState == CommandConnection.ConnectionState.DISCONNECTED
                ) {
                    Icon(Icons.Default.Bluetooth, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bluetooth")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onTestPing,
                    modifier = Modifier.weight(1f),
                    enabled = connectionState == CommandConnection.ConnectionState.CONNECTED
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Ping")
                }
                Button(
                    onClick = onDisconnect,
                    modifier = Modifier.weight(1f),
                    enabled = connectionState == CommandConnection.ConnectionState.CONNECTED,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Disconnect")
                }
            }
        }
    }
}

@Composable
private fun TestInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Test Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "This activity tests bidirectional network communication between the Android app and PC server. " +
                        "Use WiFi for high-speed data transfer or Bluetooth for reliable short-range communication.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}