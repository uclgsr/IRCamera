package mpdc4gsr.feature.testing.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import kotlinx.coroutines.launch
import mpdc4gsr.core.utils.AppLogger

class TestingSuiteHubActivity : ComponentActivity() {
    data class TestingModule(
        val id: String,
        val title: String,
        val description: String,
        val icon: ImageVector,
        val composeActivity: Class<*>? = null,
        val legacyActivity: Class<*>? = null,
        val category: TestCategory,
        val priority: TestPriority = TestPriority.MEDIUM
    )

    enum class TestCategory {
        BLE_INTEGRATION, GSR_SENSORS, CAMERA_SYSTEMS,
        SYNCHRONIZATION, DATA_INTEGRITY, PERFORMANCE,
        USER_INTERFACE, NETWORK, SYSTEM
    }

    enum class TestPriority {
        HIGH, MEDIUM, LOW
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LibUnifiedTheme {
                TestingSuiteHubScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TestingSuiteHubScreen() {
        var selectedCategory by remember { mutableStateOf<TestCategory?>(null) }
        var searchQuery by remember { mutableStateOf("") }
        val testingModules = remember {
            listOf(
                // BLE Integration Tests
                TestingModule(
                    id = "ble_integration",
                    title = "BLE Integration Test",
                    description = "Test Shimmer BLE connectivity and data streaming",
                    icon = Icons.Default.Bluetooth,
                    composeActivity = BLEIntegrationTestComposeActivity::class.java,
                    legacyActivity = null,
                    category = TestCategory.BLE_INTEGRATION,
                    priority = TestPriority.HIGH
                ),
                TestingModule(
                    id = "gsr_reconnection",
                    title = "GSR Reconnection Test",
                    description = "Test GSR device reconnection handling",
                    icon = Icons.Default.Refresh,
                    composeActivity = GSRReconnectionTestComposeActivity::class.java,
                    legacyActivity = null,
                    category = TestCategory.BLE_INTEGRATION
                ),
                // GSR Sensor Tests
                TestingModule(
                    id = "gsr_bench",
                    title = "GSR Bench Test",
                    description = "Comprehensive GSR performance benchmarking",
                    icon = Icons.Default.Speed,
                    composeActivity = GSRBenchTestComposeActivity::class.java,
                    legacyActivity = null,
                    category = TestCategory.GSR_SENSORS,
                    priority = TestPriority.HIGH
                ),
                TestingModule(
                    id = "gsr_data_integrity",
                    title = "GSR Data Integrity",
                    description = "Validate GSR data quality and consistency",
                    icon = Icons.Default.VerifiedUser,
                    composeActivity = GSRDataIntegrityTestComposeActivity::class.java,
                    legacyActivity = null,
                    category = TestCategory.DATA_INTEGRITY,
                    priority = TestPriority.HIGH
                ),
                // Camera System Tests  
                TestingModule(
                    id = "rgb_camera",
                    title = "RGB Camera Test",
                    description = "Test RGB camera recording and controls",
                    icon = Icons.Default.Camera,
                    composeActivity = RgbCameraTestComposeActivity::class.java,
                    legacyActivity = null,
                    category = TestCategory.CAMERA_SYSTEMS,
                    priority = TestPriority.HIGH
                ),
                TestingModule(
                    id = "raw_capture",
                    title = "RAW Capture Test",
                    description = "Test RAW image capture functionality",
                    icon = Icons.Default.PhotoCamera,
                    composeActivity = RawCaptureTestComposeActivity::class.java,
                    legacyActivity = null,
                    category = TestCategory.CAMERA_SYSTEMS
                ),
                // Synchronization Tests
                TestingModule(
                    id = "cross_modal_sync",
                    title = "Cross-Modal Sync",
                    description = "Test synchronization between sensors",
                    icon = Icons.Default.Sync,
                    composeActivity = CrossModalSyncTestComposeActivity::class.java,
                    legacyActivity = null,
                    category = TestCategory.SYNCHRONIZATION,
                    priority = TestPriority.HIGH
                ),
                TestingModule(
                    id = "time_sync",
                    title = "Time Synchronization",
                    description = "Test timestamp synchronization accuracy",
                    icon = Icons.Default.Schedule,
                    composeActivity = TimeSynchronizationTestComposeActivity::class.java,
                    legacyActivity = null,
                    category = TestCategory.SYNCHRONIZATION
                ),
                TestingModule(
                    id = "timestamp_unification",
                    title = "Timestamp Unification",
                    description = "Test unified timestamp system",
                    icon = Icons.Default.Timeline,
                    legacyActivity = null,
                    category = TestCategory.SYNCHRONIZATION
                ),
                // Session & Performance Tests
                TestingModule(
                    id = "session_lifecycle",
                    title = "Session Lifecycle",
                    description = "Test recording session management",
                    icon = Icons.Default.Timelapse,
                    composeActivity = SessionLifecycleTestComposeActivity::class.java,
                    legacyActivity = null,
                    category = TestCategory.PERFORMANCE
                ),
                TestingModule(
                    id = "parallel_recording",
                    title = "Parallel Recording",
                    description = "Test multi-sensor parallel recording",
                    icon = Icons.Default.MultipleStop,
                    composeActivity = ParallelRecordingTestComposeActivity::class.java,
                    legacyActivity = null,
                    category = TestCategory.PERFORMANCE,
                    priority = TestPriority.HIGH
                ),
                TestingModule(
                    id = "complete_session",
                    title = "Complete Session Trial",
                    description = "End-to-end session testing",
                    icon = Icons.Default.CheckCircle,
                    composeActivity = CompleteSessionTrialComposeActivity::class.java,
                    legacyActivity = null,
                    category = TestCategory.PERFORMANCE,
                    priority = TestPriority.HIGH
                ),
                // Additional Testing Activities
                TestingModule(
                    id = "sensor_dashboard",
                    title = "Sensor Dashboard Test",
                    description = "Test sensor dashboard UI and functionality",
                    icon = Icons.Default.Dashboard,
                    composeActivity = SensorDashboardTestComposeActivity::class.java,
                    legacyActivity = null,
                    category = TestCategory.USER_INTERFACE
                ),
                TestingModule(
                    id = "simple_network",
                    title = "Simple Network Test",
                    description = "Test PC remote control and networking",
                    icon = Icons.Default.NetworkCheck,
                    composeActivity = SimpleNetworkTestComposeActivity::class.java,
                    legacyActivity = null,
                    category = TestCategory.NETWORK
                ),
                TestingModule(
                    id = "permission_request",
                    title = "Permission Request Test",
                    description = "Test app permission system validation",
                    icon = Icons.Default.Security,
                    composeActivity = PermissionRequestTestComposeActivity::class.java,
                    legacyActivity = null,
                    category = TestCategory.SYSTEM
                )
            )
        }
        val filteredModules = testingModules.filter { module ->
            (selectedCategory == null || module.category == selectedCategory) &&
                    (searchQuery.isEmpty() || module.title.contains(
                        searchQuery,
                        ignoreCase = true
                    ) ||
                            module.description.contains(searchQuery, ignoreCase = true))
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Testing Suite Hub",
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
            val keyboardController = LocalSoftwareKeyboardController.current
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // Search and Filter
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search tests") },
                    leadingIcon = {
                        IconButton(onClick = { keyboardController?.hide() }) {
                            Icon(Icons.Default.Search, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                        }
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Category Filter Chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            onClick = { selectedCategory = null },
                            label = { Text("All") },
                            selected = selectedCategory == null
                        )
                    }
                    item {
                        FilterChip(
                            onClick = { selectedCategory = TestCategory.BLE_INTEGRATION },
                            label = { Text("BLE") },
                            selected = selectedCategory == TestCategory.BLE_INTEGRATION
                        )
                    }
                    item {
                        FilterChip(
                            onClick = { selectedCategory = TestCategory.GSR_SENSORS },
                            label = { Text("GSR") },
                            selected = selectedCategory == TestCategory.GSR_SENSORS
                        )
                    }
                    item {
                        FilterChip(
                            onClick = { selectedCategory = TestCategory.CAMERA_SYSTEMS },
                            label = { Text("Camera") },
                            selected = selectedCategory == TestCategory.CAMERA_SYSTEMS
                        )
                    }
                    item {
                        FilterChip(
                            onClick = { selectedCategory = TestCategory.SYNCHRONIZATION },
                            label = { Text("Sync") },
                            selected = selectedCategory == TestCategory.SYNCHRONIZATION
                        )
                    }
                    item {
                        FilterChip(
                            onClick = { selectedCategory = TestCategory.PERFORMANCE },
                            label = { Text("Performance") },
                            selected = selectedCategory == TestCategory.PERFORMANCE
                        )
                    }
                    item {
                        FilterChip(
                            onClick = { selectedCategory = TestCategory.USER_INTERFACE },
                            label = { Text("UI") },
                            selected = selectedCategory == TestCategory.USER_INTERFACE
                        )
                    }
                    item {
                        FilterChip(
                            onClick = { selectedCategory = TestCategory.NETWORK },
                            label = { Text("Network") },
                            selected = selectedCategory == TestCategory.NETWORK
                        )
                    }
                    item {
                        FilterChip(
                            onClick = { selectedCategory = TestCategory.SYSTEM },
                            label = { Text("System") },
                            selected = selectedCategory == TestCategory.SYSTEM
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Testing Modules List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredModules) { module ->
                        TestingModuleCard(
                            module = module,
                            onComposeClick = {
                                module.composeActivity?.let {
                                    startActivity(Intent(this@TestingSuiteHubActivity, it))
                                }
                            },
                            onLegacyClick = {
                                module.legacyActivity?.let {
                                    startActivity(Intent(this@TestingSuiteHubActivity, it))
                                }
                            }
                        )
                    }
                    item {
                        // Comprehensive Testing Button
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Rocket,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Comprehensive Testing Suite",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Run all automated tests with detailed reporting",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { runComprehensiveTests() }
                                ) {
                                    Text("Run Full Test Suite")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun TestingModuleCard(
        module: TestingModule,
        onComposeClick: () -> Unit,
        onLegacyClick: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = module.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = module.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            if (module.priority == TestPriority.HIGH) {
                                Spacer(modifier = Modifier.width(8.dp))
                                AssistChip(
                                    onClick = { },
                                    label = { Text("High Priority") },
                                    colors = AssistChipDefaults.assistChipColors(
                                        labelColor = MaterialTheme.colorScheme.error
                                    )
                                )
                            }
                        }
                        Text(
                            text = module.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (module.composeActivity != null) {
                        Button(
                            onClick = onComposeClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NewReleases,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Compose")
                        }
                    }
                    if (module.legacyActivity != null) {
                        OutlinedButton(
                            onClick = onLegacyClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Legacy")
                        }
                    }
                }
            }
        }
    }

    private fun runComprehensiveTests() {
        lifecycleScope.launch {
            // Run comprehensive testing suite using activity launcher
            try {
                // Create ComposeTestingSuiteActivity to wrap the testing logic
                val intent =
                    Intent(this@TestingSuiteHubActivity, ComposeTestingSuiteActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                AppLogger.e("TestingSuiteHub", "Failed to run comprehensive tests: ${e.message}")
            }
        }
    }
}