package mpdc4gsr.feature.testing.ui
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import mpdc4gsr.core.ui.AppBaseViewModel
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import mpdc4gsr.core.ui.components.SensorStatus
import mpdc4gsr.core.ui.components.TitleBar
import mpdc4gsr.core.ui.theme.IRCameraTheme
enum class TestSensorType(
    val displayName: String,
    val icon: ImageVector,
    val key: String
) {
    THERMAL_CAMERA("Thermal Camera", Icons.Default.Thermostat, "thermal_camera"),
    RGB_CAMERA("RGB Camera", Icons.Default.Camera, "rgb_camera"),
    GSR_SENSOR("GSR Sensor", Icons.Default.Sensors, "shimmer_gsr"),
    BLUETOOTH("Bluetooth", Icons.Default.Bluetooth, "bluetooth_device"),
    NETWORK("Network", Icons.Default.NetworkCheck, "network_device"),
    STORAGE("Storage", Icons.Default.Storage, "storage_device")
}
data class SensorTestStatus(
    val sensorType: TestSensorType,
    val status: SensorStatus,
    val message: String,
    val lastUpdate: String = "Never",
    val dataRate: String = "0 KB/s"
)
class SensorDashboardTestViewModel : AppBaseViewModel() {
    private val _sensorStatuses = mutableStateOf(
        TestSensorType.values().map { type ->
            SensorTestStatus(
                sensorType = type,
                status = SensorStatus.DISCONNECTED,
                message = "Not connected"
            )
        }
    )
    val sensorStatuses: State<List<SensorTestStatus>> = _sensorStatuses
    private val _isRunningTest = mutableStateOf(false)
    val isRunningTest: State<Boolean> = _isRunningTest
    private val _testProgress = mutableStateOf(0f)
    val testProgress: State<Float> = _testProgress
    private val _testMessage = mutableStateOf("Ready to start sensor testing")
    val testMessage: State<String> = _testMessage
    fun runCompleteTest() {
        launchWithErrorHandling {
            _isRunningTest.value = true
            _testProgress.value = 0f
            val sensors = TestSensorType.values()
            sensors.forEachIndexed { index, sensorType ->
                _testMessage.value = "Testing ${sensorType.displayName}..."
                // Update to connecting
                updateSensorStatus(
                    sensorType,
                    SensorStatus.CONNECTING,
                    "Connecting...",
                    "Just now"
                )
                delay(1500)
                // Simulate different test outcomes
                val testResult = when (index % 4) {
                    0 -> {
                        // Connected successfully
                        Triple(
                            SensorStatus.CONNECTED,
                            "Connected successfully",
                            when (sensorType) {
                                TestSensorType.THERMAL_CAMERA -> "125 KB/s"
                                TestSensorType.RGB_CAMERA -> "1.2 MB/s"
                                TestSensorType.GSR_SENSOR -> "2 KB/s"
                                TestSensorType.BLUETOOTH -> "64 KB/s"
                                TestSensorType.NETWORK -> "10 MB/s"
                                TestSensorType.STORAGE -> "50 MB/s"
                            }
                        )
                    }
                    1 -> {
                        // Warning state
                        Triple(
                            SensorStatus.ERROR,
                            "Connected with issues",
                            "Reduced rate"
                        )
                    }
                    2 -> {
                        // Error state
                        Triple(
                            SensorStatus.ERROR,
                            "Connection failed",
                            "0 KB/s"
                        )
                    }
                    else -> {
                        // Disconnected
                        Triple(
                            SensorStatus.DISCONNECTED,
                            "Device not available",
                            "0 KB/s"
                        )
                    }
                }
                updateSensorStatus(
                    sensorType,
                    testResult.first,
                    testResult.second,
                    "Just tested",
                    testResult.third
                )
                _testProgress.value = (index + 1).toFloat() / sensors.size
                delay(1000)
            }
            _testMessage.value = "Testing complete. Results displayed above."
            _isRunningTest.value = false
        }
    }
    fun testIndividualSensor(sensorType: TestSensorType) {
        launchWithErrorHandling {
            _testMessage.value = "Testing ${sensorType.displayName}..."
            updateSensorStatus(
                sensorType,
                SensorStatus.CONNECTING,
                "Testing connection...",
                "Testing now"
            )
            delay(2000)
            // Simulate random test result
            val success = kotlin.random.Random.nextFloat() > 0.3f
            if (success) {
                updateSensorStatus(
                    sensorType,
                    SensorStatus.CONNECTED,
                    "Test passed - sensor responding",
                    "Just tested",
                    when (sensorType) {
                        TestSensorType.THERMAL_CAMERA -> "125 KB/s"
                        TestSensorType.RGB_CAMERA -> "1.2 MB/s"
                        TestSensorType.GSR_SENSOR -> "2 KB/s"
                        TestSensorType.BLUETOOTH -> "64 KB/s"
                        TestSensorType.NETWORK -> "10 MB/s"
                        TestSensorType.STORAGE -> "50 MB/s"
                    }
                )
                _testMessage.value = "${sensorType.displayName} test passed"
            } else {
                updateSensorStatus(
                    sensorType,
                    SensorStatus.ERROR,
                    "Test failed - sensor not responding",
                    "Just tested",
                    "0 KB/s"
                )
                _testMessage.value = "${sensorType.displayName} test failed"
            }
        }
    }
    private fun updateSensorStatus(
        sensorType: TestSensorType,
        status: SensorStatus,
        message: String,
        lastUpdate: String,
        dataRate: String = "0 KB/s"
    ) {
        _sensorStatuses.value = _sensorStatuses.value.map { sensorStatus ->
            if (sensorStatus.sensorType == sensorType) {
                sensorStatus.copy(
                    status = status,
                    message = message,
                    lastUpdate = lastUpdate,
                    dataRate = dataRate
                )
            } else sensorStatus
        }
    }
    fun resetAllSensors() {
        _sensorStatuses.value = TestSensorType.values().map { type ->
            SensorTestStatus(
                sensorType = type,
                status = SensorStatus.DISCONNECTED,
                message = "Reset to initial state"
            )
        }
        _testMessage.value = "All sensors reset to initial state"
    }
}
class SensorDashboardTestComposeActivity : BaseComposeActivity<SensorDashboardTestViewModel>() {
    override fun createViewModel(): SensorDashboardTestViewModel =
        viewModels<SensorDashboardTestViewModel>().value
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: SensorDashboardTestViewModel) {
        IRCameraTheme {
            val context = LocalContext.current
            val sensorStatuses by viewModel.sensorStatuses
            val isRunningTest by viewModel.isRunningTest
            val testProgress by viewModel.testProgress
            val testMessage by viewModel.testMessage
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16131E))
            ) {
                TitleBar(
                    title = "Sensor Dashboard Test",
                    onBackClick = { finish() },
                    actions = {
                        IconButton(onClick = { viewModel.resetAllSensors() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset",
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
                    // Test status card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isRunningTest)
                                MaterialTheme.colorScheme.surfaceVariant
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isRunningTest) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Science,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isRunningTest) "Running Tests..." else "Test Controls",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = testMessage,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (isRunningTest) {
                                Spacer(modifier = Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    progress = { testProgress },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${(testProgress * 100).toInt()}% Complete",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                    // Control buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.runCompleteTest() },
                            modifier = Modifier.weight(1f),
                            enabled = !isRunningTest
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Run All Tests")
                        }
                        OutlinedButton(
                            onClick = { viewModel.resetAllSensors() },
                            modifier = Modifier.weight(1f),
                            enabled = !isRunningTest
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset All")
                        }
                    }
                    // Sensor status cards
                    Text(
                        text = "Sensor Test Results",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    sensorStatuses.forEach { sensorStatus ->
                        SensorTestCard(
                            sensorStatus = sensorStatus,
                            onTest = { viewModel.testIndividualSensor(sensorStatus.sensorType) },
                            isTestingEnabled = !isRunningTest,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
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
                                    text = "Sensor Dashboard Testing",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "This test validates the sensor dashboard functionality by simulating different sensor connection states and data flows. Use this to verify UI responsiveness and error handling.",
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
private fun SensorTestCard(
    sensorStatus: SensorTestStatus,
    onTest: () -> Unit,
    isTestingEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (sensorStatus.status) {
                SensorStatus.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
                SensorStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
                SensorStatus.CONNECTING -> MaterialTheme.colorScheme.surfaceVariant
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
            if (sensorStatus.status == SensorStatus.CONNECTING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = sensorStatus.sensorType.icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = when (sensorStatus.status) {
                        SensorStatus.CONNECTED -> MaterialTheme.colorScheme.primary
                        SensorStatus.ERROR -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sensorStatus.sensorType.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = sensorStatus.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Rate: ${sensorStatus.dataRate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Updated: ${sensorStatus.lastUpdate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    color = when (sensorStatus.status) {
                        SensorStatus.CONNECTED -> MaterialTheme.colorScheme.primary
                        SensorStatus.ERROR -> MaterialTheme.colorScheme.error
                        SensorStatus.CONNECTING -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = sensorStatus.status.name.lowercase()
                            .replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = when (sensorStatus.status) {
                            SensorStatus.CONNECTED -> MaterialTheme.colorScheme.onPrimary
                            SensorStatus.ERROR -> MaterialTheme.colorScheme.onError
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onTest,
                    enabled = isTestingEnabled,
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "Test",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}