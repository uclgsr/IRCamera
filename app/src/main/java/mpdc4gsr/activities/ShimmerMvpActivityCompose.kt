package mpdc4gsr.activities

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import com.shimmerresearch.android.Shimmer
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
import com.shimmerresearch.driver.ObjectCluster
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.ShimmerNetworkClient
import mpdc4gsr.compose.base.BaseComposeActivity
import mpdc4gsr.compose.components.TitleBar
import mpdc4gsr.compose.theme.IRCameraTheme
import mpdc4gsr.sensors.TimestampManager
import mpdc4gsr.sensors.gsr.GSRCalculationUtils
import mpdc4gsr.sensors.gsr.GSRConstants
import mpdc4gsr.sensors.unified.model.GSRSample
import mpdc4gsr.viewmodel.BaseViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class ShimmerConnectionState {
    DISCONNECTED, SCANNING, CONNECTING, CONNECTED, ERROR
}

enum class GSRQuality(val displayName: String, val color: Color) {
    EXCELLENT("Excellent", Color(0xFF4CAF50)),
    GOOD("Good", Color(0xFF8BC34A)),
    FAIR("Fair", Color(0xFFFF9800)),
    POOR("Poor", Color(0xFFFF5722)),
    UNKNOWN("Unknown", Color(0xFF9E9E9E))
}

data class ShimmerDevice(
    val name: String,
    val address: String,
    val isConnected: Boolean = false,
    val signalStrength: Int = 0
)

data class GSRData(
    val timestamp: Long,
    val rawValue: Double,
    val gsrValue: Double,
    val quality: GSRQuality
)

class ShimmerMvpViewModel : BaseViewModel() {
    private val _shimmerConnectionState = mutableStateOf(ShimmerConnectionState.DISCONNECTED)
    val shimmerConnectionState: State<ShimmerConnectionState> = _shimmerConnectionState

    private val _availableDevices = mutableStateOf<List<ShimmerDevice>>(emptyList())
    val availableDevices: State<List<ShimmerDevice>> = _availableDevices

    private val _connectedDevice = mutableStateOf<ShimmerDevice?>(null)
    val connectedDevice: State<ShimmerDevice?> = _connectedDevice

    private val _gsrData = mutableStateOf<List<GSRData>>(emptyList())
    val gsrData: State<List<GSRData>> = _gsrData

    private val _currentGSRValue = mutableStateOf(0.0)
    val currentGSRValue: State<Double> = _currentGSRValue

    private val _currentQuality = mutableStateOf(GSRQuality.UNKNOWN)
    val currentQuality: State<GSRQuality> = _currentQuality

    private val _statusMessage = mutableStateOf("Ready to scan for Shimmer devices")
    val statusMessage: State<String> = _statusMessage

    private val _samplesCollected = mutableStateOf(0)
    val samplesCollected: State<Int> = _samplesCollected

    private val _recordingDuration = mutableStateOf("00:00:00")
    val recordingDuration: State<String> = _recordingDuration

    private val _isRecording = mutableStateOf(false)
    val isRecording: State<Boolean> = _isRecording

    fun startScanning() {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            _connectionState.value = ShimmerConnectionState.SCANNING
            _statusMessage.value = "Scanning for Shimmer devices..."
            _availableDevices.value = emptyList()

            delay(2000)

            // Simulate discovering Shimmer devices
            val mockDevices = listOf(
                ShimmerDevice("Shimmer3-GSR-001", "00:06:66:6C:E9:01", signalStrength = 85),
                ShimmerDevice("Shimmer3-GSR-002", "00:06:66:6C:E9:02", signalStrength = 72),
                ShimmerDevice("ShimmerBT-GSR", "00:06:66:6C:E9:03", signalStrength = 91)
            )

            delay(3000) // Simulate scanning time
            _availableDevices.value = mockDevices
            _connectionState.value = ShimmerConnectionState.DISCONNECTED
            _statusMessage.value = "Found ${mockDevices.size} Shimmer devices"
        }
    }

    fun connectToDevice(device: ShimmerDevice) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            _connectionState.value = ShimmerConnectionState.CONNECTING
            _statusMessage.value = "Connecting to ${device.name}..."

            delay(3000) // Simulate connection time

            // Simulate successful connection (90% success rate)
            val isConnected = kotlin.random.Random.nextFloat() > 0.1f

            if (isConnected) {
                _connectedDevice.value = device.copy(isConnected = true)
                _connectionState.value = ShimmerConnectionState.CONNECTED
                _statusMessage.value = "Connected to ${device.name}"
                startDataSimulation()
            } else {
                _connectionState.value = ShimmerConnectionState.ERROR
                _statusMessage.value = "Failed to connect to ${device.name}"
            }
        }
    }

    fun disconnect() {
        _connectedDevice.value = null
        _connectionState.value = ShimmerConnectionState.DISCONNECTED
        _statusMessage.value = "Disconnected from Shimmer device"
        _isRecording.value = false
        _gsrData.value = emptyList()
        _samplesCollected.value = 0
        _recordingDuration.value = "00:00:00"
    }

    private fun startDataSimulation() {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            while (_connectionState.value == ShimmerConnectionState.CONNECTED) {
                delay(50) // 20 Hz sampling rate

                if (_isRecording.value) {
                    val timestamp = System.currentTimeMillis()
                    val rawValue = kotlin.random.Random.nextDouble(200.0, 4000.0)
                    val gsrValue = GSRCalculationUtils.calculateGSRMicrosiemens(rawValue.roundToInt())
                    val quality = determineQuality(rawValue, gsrValue)

                    val sample = GSRData(timestamp, rawValue, gsrValue, quality)

                    val currentData = _gsrData.value.toMutableList()
                    currentData.add(sample)
                    if (currentData.size > 200) { // Keep last 200 samples
                        currentData.removeAt(0)
                    }
                    _gsrData.value = currentData

                    _currentGSRValue.value = gsrValue
                    _currentQuality.value = quality
                    _samplesCollected.value = _samplesCollected.value + 1
                }
            }
        }
    }

    fun startRecording() {
        if (_connectionState.value == ShimmerConnectionState.CONNECTED) {
            _isRecording.value = true
            _statusMessage.value = "Recording GSR data..."

            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                var seconds = 0
                while (_isRecording.value) {
                    delay(1000)
                    seconds++
                    val hours = seconds / 3600
                    val minutes = (seconds % 3600) / 60
                    val secs = seconds % 60
                    _recordingDuration.value = String.format("%02d:%02d:%02d", hours, minutes, secs)
                }
            }
        }
    }

    fun stopRecording() {
        _isRecording.value = false
        _statusMessage.value = "Recording stopped. Collected ${_samplesCollected.value} samples"
    }

    private fun determineQuality(rawValue: Double, gsrValue: Double): GSRQuality {
        return when {
            rawValue in 500.0..3500.0 && gsrValue in 1.0..25.0 -> GSRQuality.EXCELLENT
            rawValue in 200.0..500.0 || rawValue in 3500.0..4000.0 -> GSRQuality.GOOD
            rawValue in 100.0..200.0 || rawValue in 4000.0..5000.0 -> GSRQuality.FAIR
            else -> GSRQuality.POOR
        }
    }
}

class ShimmerMvpActivityCompose : BaseComposeActivity<ShimmerMvpViewModel>() {

    private val bluetoothPermissions = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModels<ShimmerMvpViewModel>().value.startScanning()
        }
    }

    private val bluetoothEnableResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            checkPermissionsAndScan()
        }
    }

    override fun createViewModel(): ShimmerMvpViewModel = viewModels<ShimmerMvpViewModel>().value

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: ShimmerMvpViewModel) {
        IRCameraTheme {
            val context = LocalContext.current
            val connectionState by viewModel.shimmerConnectionState
            val availableDevices by viewModel.availableDevices
            val connectedDevice by viewModel.connectedDevice
            val gsrData by viewModel.gsrData
            val currentGSRValue by viewModel.currentGSRValue
            val currentQuality by viewModel.currentQuality
            val statusMessage by viewModel.statusMessage
            val samplesCollected by viewModel.samplesCollected
            val recordingDuration by viewModel.recordingDuration
            val isRecording by viewModel.isRecording

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16131E))
            ) {
                TitleBar(
                    title = "Shimmer GSR Testing",
                    onBackClick = { finish() },
                    actions = {
                        IconButton(onClick = { checkPermissionsAndScan() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Scan",
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
                    // Connection status
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (connectionState) {
                                ShimmerConnectionState.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
                                ShimmerConnectionState.ERROR -> MaterialTheme.colorScheme.errorContainer
                                ShimmerConnectionState.SCANNING, ShimmerConnectionState.CONNECTING -> MaterialTheme.colorScheme.surfaceVariant
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
                            when (connectionState) {
                                ShimmerConnectionState.SCANNING, ShimmerConnectionState.CONNECTING -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.dp
                                    )
                                }

                                ShimmerConnectionState.CONNECTED -> {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                ShimmerConnectionState.ERROR -> {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                else -> {
                                    Icon(
                                        imageVector = Icons.Default.Bluetooth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = connectionState.name.lowercase()
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

                            if (connectedDevice != null) {
                                OutlinedButton(
                                    onClick = { viewModel.disconnect() }
                                ) {
                                    Text("Disconnect")
                                }
                            }
                        }
                    }

                    // Current GSR reading (if connected and recording)
                    if (connectionState == ShimmerConnectionState.CONNECTED && isRecording) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = currentQuality.color.copy(alpha = 0.1f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = String.format("%.2f µS", currentGSRValue),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = currentQuality.color
                                )
                                Text(
                                    text = "Current GSR Value",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                    color = currentQuality.color
                                ) {
                                    Text(
                                        text = currentQuality.displayName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 4.dp
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = samplesCollected.toString(),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Samples",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = recordingDuration,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Duration",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Device list or recording controls
                    if (connectionState == ShimmerConnectionState.CONNECTED) {
                        // Recording controls
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (isRecording) {
                                Button(
                                    onClick = { viewModel.stopRecording() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Stop Recording")
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.startRecording() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Start Recording")
                                }
                            }
                        }

                        // Recent GSR data
                        if (gsrData.isNotEmpty()) {
                            Text(
                                text = "Recent GSR Data",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            ) {
                                LazyColumn(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(gsrData.takeLast(20).reversed()) { sample ->
                                        GSRDataRow(sample)
                                    }
                                }
                            }
                        }
                    } else if (availableDevices.isNotEmpty()) {
                        // Available devices
                        Text(
                            text = "Available Shimmer Devices",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        availableDevices.forEach { device ->
                            ShimmerDeviceCard(
                                device = device,
                                onConnect = { viewModel.connectToDevice(device) },
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    } else if (connectionState == ShimmerConnectionState.DISCONNECTED) {
                        // Scan button
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bluetooth,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Shimmer GSR Testing",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Scan for Shimmer3 GSR devices to begin testing",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { checkPermissionsAndScan() }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Scan for Devices")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkPermissionsAndScan() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothEnableResult.launch(enableBtIntent)
            return
        }

        // Check permissions
        val missingPermissions = bluetoothPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            viewModels<ShimmerMvpViewModel>().value.startScanning()
        }
    }
}

@Composable
private fun ShimmerDeviceCard(
    device: ShimmerDevice,
    onConnect: () -> Unit,
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
            Icon(
                imageVector = Icons.Default.Sensors,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Signal: ${device.signalStrength}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onConnect
            ) {
                Text("Connect")
            }
        }
    }
}

@Composable
private fun GSRDataRow(
    sample: GSRData,
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = timeFormat.format(Date(sample.timestamp)),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(80.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = String.format("%.2f µS", sample.gsrValue),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(70.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            color = sample.quality.color.copy(alpha = 0.2f)
        ) {
            Text(
                text = sample.quality.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = sample.quality.color,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}