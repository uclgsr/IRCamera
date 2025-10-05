package mpdc4gsr.feature.testing.ui

import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.core.data.ShimmerDeviceManager
import mpdc4gsr.core.data.UnifiedGSRRecorder
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import mpdc4gsr.core.ui.PermissionController
import mpdc4gsr.feature.testing.presentation.BLEIntegrationTestViewModel
import kotlin.io.path.createTempDirectory

/**
 * Compose version of BLE Integration Test Activity
 * Tests BLE functionality in a modern Compose UI
 * Migrated to BaseComposeActivity for consistency
 */
class BLEIntegrationTestComposeActivity : BaseComposeActivity<BLEIntegrationTestViewModel>() {


    companion object {
        private const val TAG = "BLEIntegrationTestCompose"
    }

    private lateinit var permissionController: PermissionController
    private var gsrRecorder: UnifiedGSRRecorder? = null
    private var deviceManager: ShimmerDeviceManager? = null

    override fun createViewModel(): BLEIntegrationTestViewModel {
        return viewModels<BLEIntegrationTestViewModel>().value
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize components
        permissionController = PermissionController(this)
        initializeRecorder()
    }

    @Composable
    override fun Content(viewModel: BLEIntegrationTestViewModel) {
        var logMessages by remember { mutableStateOf(listOf<String>()) }
        
        LibUnifiedTheme {
            BLEIntegrationTestScreen(
                onRunTest = { testType -> runTest(testType) },
                onClearLogs = { 
                    // Clear log messages and provide feedback
                    logMessages = emptyList()
                },
                logMessages = logMessages,
                onLogAdded = { msg -> logMessages = logMessages + msg }
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun BLEIntegrationTestScreen(
        onRunTest: (String) -> Unit,
        onClearLogs: () -> Unit,
        logMessages: List<String> = emptyList(),
        onLogAdded: (String) -> Unit = {}
    ) {
        var testResults by remember { mutableStateOf(listOf<TestCase>()) }
        var isTestRunning by remember { mutableStateOf(false) }

        // Initialize test cases
        LaunchedEffect(Unit) {
            testResults = listOf(
                TestCase(
                    id = "permissions",
                    name = "BLE Permissions",
                    description = "Test BLE and location permissions"
                ),
                TestCase(
                    id = "discovery",
                    name = "Device Discovery",
                    description = "Test Shimmer device discovery"
                ),
                TestCase(
                    id = "connection",
                    name = "Device Connection",
                    description = "Test connection to Shimmer GSR device"
                ),
                TestCase(
                    id = "streaming",
                    name = "Data Streaming",
                    description = "Test real-time GSR data streaming"
                ),
                TestCase(
                    id = "reconnection",
                    name = "Reconnection Test",
                    description = "Test automatic reconnection handling"
                )
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "BLE Integration Test",
                            fontWeight = FontWeight.Medium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = onClearLogs) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Logs")
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
                // Test Progress Overview
                TestProgressIndicator(
                    totalTests = testResults.size,
                    completedTests = testResults.count { it.status != TestStatus.PENDING },
                    passedTests = testResults.count { it.status == TestStatus.PASSED },
                    failedTests = testResults.count { it.status == TestStatus.FAILED }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Run All Tests Button
                Button(
                    onClick = {
                        if (!isTestRunning) {
                            lifecycleScope.launch { runAllTests() }
                        }
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
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Run All Tests")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Individual Test Cases
                testResults.forEach { testCase ->
                    TestResultCard(
                        testCase = testCase,
                        onRunTest = { onRunTest(testCase.id) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                if (logMessages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Test Logs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            logMessages.forEach { message ->
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun initializeRecorder() {
        try {
            // Initialize GSR recorder and device manager
            gsrRecorder = UnifiedGSRRecorder(this, this)
            deviceManager = ShimmerDeviceManager(this, this)
            AppLogger.d(TAG, "BLE components initialized successfully")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to initialize BLE components: ${e.message}")
        }
    }

    private suspend fun runAllTests() {
        AppLogger.i(TAG, "Starting comprehensive BLE integration tests")

        // Run each test sequentially
        runPermissionsTest()
        delay(1000)
        runDiscoveryTest()
        delay(1000)
        runConnectionTest()
        delay(1000)
        runStreamingTest()
        delay(1000)
        runReconnectionTest()

        AppLogger.i(TAG, "BLE integration tests completed")
    }

    private suspend fun runPermissionsTest() {
        AppLogger.d(TAG, "Running BLE permissions test")
        // Test BLE and location permissions
        try {
            val hasPermissions = permissionController.hasBluetoothPermissions()
            // Update test result based on permissions check
            AppLogger.d(TAG, "BLE permissions check: $hasPermissions")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Permissions test failed: ${e.message}")
        }
    }

    private suspend fun runDiscoveryTest() {
        AppLogger.d(TAG, "Running device discovery test")
        try {
            deviceManager?.let { manager ->
                // Start device scanning
                val success = manager.startDeviceScanning()
                AppLogger.d(TAG, "Device scanning started: $success")
                delay(5000) // Let it scan for 5 seconds
                manager.stopDeviceScanning()
                AppLogger.d(TAG, "Device discovery test completed")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Discovery test failed: ${e.message}")
        }
    }

    private suspend fun runConnectionTest() {
        AppLogger.d(TAG, "Running connection test")
        try {
            gsrRecorder?.let { recorder ->
                // Test connection by initializing the recorder
                val result = recorder.initialize()
                AppLogger.d(TAG, "Connection test result: $result")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Connection test failed: ${e.message}")
        }
    }

    private suspend fun runStreamingTest() {
        AppLogger.d(TAG, "Running data streaming test")
        try {
            gsrRecorder?.let { recorder ->
                // Test data streaming by starting a temporary recording
                val tempDir = createTempDirectory("test_streaming").toFile()
                val streamingResult = recorder.startRecording(tempDir.absolutePath)
                delay(5000) // Record for 5 seconds
                recorder.stopRecording()
                tempDir.deleteRecursively()
                AppLogger.d(TAG, "Streaming test completed: $streamingResult")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Streaming test failed: ${e.message}")
        }
    }

    private suspend fun runReconnectionTest() {
        AppLogger.d(TAG, "Running reconnection test")
        try {
            gsrRecorder?.let { recorder ->
                // Test reconnection by stopping and restarting recording
                val tempDir = createTempDirectory("test_reconnection").toFile()
                val initialResult = recorder.startRecording(tempDir.absolutePath)
                delay(2000)
                recorder.stopRecording()
                delay(1000)
                val reconnectResult = recorder.startRecording(tempDir.absolutePath)
                delay(2000)
                recorder.stopRecording()
                tempDir.deleteRecursively()
                Log.d(
                    TAG,
                    "Reconnection test result: initial=$initialResult, reconnect=$reconnectResult"
                )
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Reconnection test failed: ${e.message}")
        }
    }

    private fun runTest(testType: String) {
        lifecycleScope.launch {
            when (testType) {
                "permissions" -> runPermissionsTest()
                "discovery" -> runDiscoveryTest()
                "connection" -> runConnectionTest()
                "streaming" -> runStreamingTest()
                "reconnection" -> runReconnectionTest()
            }
        }
    }
}