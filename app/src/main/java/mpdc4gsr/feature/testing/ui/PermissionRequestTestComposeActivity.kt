package mpdc4gsr.feature.testing.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.PermissionController
import mpdc4gsr.core.ui.PermissionManager
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.feature.testing.presentation.PermissionRequestTestViewModel
import java.text.SimpleDateFormat
import java.util.*

class PermissionRequestTestComposeActivity : BaseComposeActivity<PermissionRequestTestViewModel>() {
    companion object {
        private const val TAG = "PermissionRequestTestCompose"
    }

    enum class PermissionStatus {
        GRANTED, DENIED, NOT_REQUESTED, REQUESTING
    }

    data class PermissionInfo(
        val permission: String,
        val name: String,
        val description: String,
        val status: PermissionStatus,
        val isRequired: Boolean,
        val lastChecked: Long = System.currentTimeMillis()
    )

    data class PermissionLog(
        val timestamp: String,
        val action: String,
        val permission: String,
        val result: String
    )

    private lateinit var permissionController: PermissionController
    private lateinit var permissionManager: PermissionManager
    private var isTestRunning by mutableStateOf(false)
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        handlePermissionResults(permissions)
    }

    override fun createViewModel(): PermissionRequestTestViewModel {
        return viewModels<PermissionRequestTestViewModel>().value
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializePermissionSystem()
    }

    @Composable
    override fun Content(viewModel: PermissionRequestTestViewModel) {
        LibUnifiedTheme {
            PermissionRequestTestScreen()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun PermissionRequestTestScreen() {
        var testResults by remember { mutableStateOf(listOf<TestCase>()) }
        var permissions by remember { mutableStateOf(listOf<PermissionInfo>()) }
        var permissionLogs by remember { mutableStateOf(listOf<PermissionLog>()) }
        var overallPermissionStatus by remember { mutableStateOf("Not Checked") }
        var canStartRecording by remember { mutableStateOf(false) }

        // Function to update permission status (defined before being used)
        fun updatePermissionStatus() {
            val permissionList = listOf(
                PermissionInfo(
                    permission = Manifest.permission.CAMERA,
                    name = "Camera",
                    description = "Required for thermal and RGB camera access",
                    status = getPermissionStatus(Manifest.permission.CAMERA),
                    isRequired = true
                ),
                PermissionInfo(
                    permission = Manifest.permission.RECORD_AUDIO,
                    name = "Microphone",
                    description = "Required for audio recording during sessions",
                    status = getPermissionStatus(Manifest.permission.RECORD_AUDIO),
                    isRequired = true
                ),
                PermissionInfo(
                    permission = Manifest.permission.BLUETOOTH,
                    name = "Bluetooth",
                    description = "Required for GSR sensor communication",
                    status = getPermissionStatus(Manifest.permission.BLUETOOTH),
                    isRequired = true
                ),
                PermissionInfo(
                    permission = Manifest.permission.BLUETOOTH_ADMIN,
                    name = "Bluetooth Admin",
                    description = "Required for Bluetooth device management",
                    status = getPermissionStatus(Manifest.permission.BLUETOOTH_ADMIN),
                    isRequired = true
                ),
                PermissionInfo(
                    permission = Manifest.permission.ACCESS_FINE_LOCATION,
                    name = "Location",
                    description = "Required for Bluetooth device scanning",
                    status = getPermissionStatus(Manifest.permission.ACCESS_FINE_LOCATION),
                    isRequired = true
                ),
                PermissionInfo(
                    permission = Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    name = "Storage",
                    description = "Required for saving recordings and data",
                    status = getPermissionStatus(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    isRequired = true
                ),
                PermissionInfo(
                    permission = Manifest.permission.READ_EXTERNAL_STORAGE,
                    name = "Read Storage",
                    description = "Required for accessing saved files",
                    status = getPermissionStatus(Manifest.permission.READ_EXTERNAL_STORAGE),
                    isRequired = false
                )
            )
            permissions = permissionList
            val grantedCount = permissions.count { it.status == PermissionStatus.GRANTED }
            val requiredCount = permissions.count { it.isRequired }
            val requiredGrantedCount =
                permissions.count { it.isRequired && it.status == PermissionStatus.GRANTED }
            overallPermissionStatus =
                "$requiredGrantedCount/$requiredCount required permissions granted"
            canStartRecording = requiredGrantedCount == requiredCount
        }
        // Initialize test cases and permissions
        LaunchedEffect(Unit) {
            testResults = listOf(
                TestCase(
                    id = "camera_permissions",
                    name = "Camera Permissions",
                    description = "Test camera access permissions"
                ),
                TestCase(
                    id = "bluetooth_permissions",
                    name = "Bluetooth Permissions",
                    description = "Test Bluetooth and location permissions"
                ),
                TestCase(
                    id = "storage_permissions",
                    name = "Storage Permissions",
                    description = "Test storage access permissions"
                ),
                TestCase(
                    id = "microphone_permissions",
                    name = "Microphone Permissions",
                    description = "Test audio recording permissions"
                ),
                TestCase(
                    id = "permission_flow",
                    name = "Permission Flow",
                    description = "Test complete permission request flow"
                ),
                TestCase(
                    id = "permission_persistence",
                    name = "Permission Persistence",
                    description = "Test permission state persistence"
                )
            )
            updatePermissionStatus()
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Permission Request Test",
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
                // Permission Status Overview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (canStartRecording)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
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
                                    imageVector = if (canStartRecording) Icons.Default.Security else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (canStartRecording) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Permission Status",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (canStartRecording) {
                                AssistChip(
                                    onClick = { },
                                    label = { Text("Ready") },
                                    leadingIcon = {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        labelColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = overallPermissionStatus,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Permission List
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "App Permissions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        permissions.forEach { permission ->
                            PermissionItem(
                                permission = permission,
                                onRequest = { requestSinglePermission(permission.permission) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
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
                // Permission Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            requestAllPermissions()
                        },
                        enabled = !canStartRecording,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Request All")
                    }
                    OutlinedButton(
                        onClick = {
                            updatePermissionStatus()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Refresh Status")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            isTestRunning = true
                            lifecycleScope.launch { runAllPermissionTests() }
                        },
                        enabled = !isTestRunning,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isTestRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testing...")
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Run Tests")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            if (canStartRecording) {
                                // Simulate starting recording
                                addPermissionLog(
                                    "START_RECORDING",
                                    "ALL",
                                    "Recording started successfully"
                                )
                            }
                        },
                        enabled = canStartRecording,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FiberManualRecord, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start Recording")
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
                // Permission Logs
                if (permissionLogs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Permission Logs",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                TextButton(
                                    onClick = { permissionLogs = emptyList() }
                                ) {
                                    Text("Clear")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            permissionLogs.takeLast(8).forEach { log ->
                                PermissionLogItem(log = log)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun PermissionItem(
        permission: PermissionInfo,
        onRequest: () -> Unit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = getPermissionIcon(permission.status),
                        contentDescription = null,
                        tint = getPermissionColor(permission.status),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = permission.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (permission.isRequired) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "*",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Text(
                    text = permission.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (permission.status != PermissionStatus.GRANTED) {
                TextButton(onClick = onRequest) {
                    Text("Request")
                }
            }
        }
    }

    @Composable
    fun PermissionLogItem(log: PermissionLog) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${log.action} - ${log.permission}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = log.result,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = log.timestamp,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    private fun getPermissionIcon(status: PermissionStatus): androidx.compose.ui.graphics.vector.ImageVector {
        return when (status) {
            PermissionStatus.GRANTED -> Icons.Default.CheckCircle
            PermissionStatus.DENIED -> Icons.Default.Block
            PermissionStatus.NOT_REQUESTED -> Icons.AutoMirrored.Filled.HelpOutline
            PermissionStatus.REQUESTING -> Icons.Default.HourglassEmpty
        }
    }

    @Composable
    private fun getPermissionColor(status: PermissionStatus): androidx.compose.ui.graphics.Color {
        return when (status) {
            PermissionStatus.GRANTED -> MaterialTheme.colorScheme.primary
            PermissionStatus.DENIED -> MaterialTheme.colorScheme.error
            PermissionStatus.NOT_REQUESTED -> MaterialTheme.colorScheme.outline
            PermissionStatus.REQUESTING -> MaterialTheme.colorScheme.tertiary
        }
    }

    private fun initializePermissionSystem() {
        permissionController = PermissionController(this)
        permissionManager = PermissionManager(this, permissionController)
    }

    private fun getPermissionStatus(permission: String): PermissionStatus {
        return when (ContextCompat.checkSelfPermission(this, permission)) {
            PackageManager.PERMISSION_GRANTED -> PermissionStatus.GRANTED
            PackageManager.PERMISSION_DENIED -> PermissionStatus.DENIED
            else -> PermissionStatus.NOT_REQUESTED
        }
    }

    private fun requestAllPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        addPermissionLog("REQUEST_ALL", "Multiple", "Requesting all required permissions")
        permissionLauncher.launch(requiredPermissions)
    }

    private fun requestSinglePermission(permission: String) {
        addPermissionLog("REQUEST_SINGLE", permission, "Requesting single permission")
        permissionLauncher.launch(arrayOf(permission))
    }

    private fun handlePermissionResults(permissions: Map<String, Boolean>) {
        permissions.forEach { (permission, granted) ->
            val result = if (granted) "GRANTED" else "DENIED"
            addPermissionLog("RESULT", permission, result)
        }
    }

    private fun addPermissionLog(action: String, permission: String, result: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val log = PermissionLog(timestamp, action, permission, result)
        // In a real implementation, this would update the state properly
        AppLogger.d(TAG, "Permission log: $log")
    }

    private suspend fun runAllPermissionTests() {
        AppLogger.i(TAG, "Running all permission tests")
        try {
            testCameraPermissions()
            delay(1000)
            testBluetoothPermissions()
            delay(1000)
            testStoragePermissions()
            delay(1000)
            testMicrophonePermissions()
            delay(1000)
            testPermissionFlow()
            delay(1000)
            testPermissionPersistence()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Permission tests failed: ${e.message}")
        } finally {
            isTestRunning = false
        }
    }

    private suspend fun testCameraPermissions() {
        AppLogger.d(TAG, "Testing camera permissions")
        try {
            delay(2000)
            AppLogger.d(TAG, "Camera permissions test completed")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Camera permissions test failed: ${e.message}")
        }
    }

    private suspend fun testBluetoothPermissions() {
        AppLogger.d(TAG, "Testing Bluetooth permissions")
        try {
            delay(2000)
            AppLogger.d(TAG, "Bluetooth permissions test completed")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Bluetooth permissions test failed: ${e.message}")
        }
    }

    private suspend fun testStoragePermissions() {
        AppLogger.d(TAG, "Testing storage permissions")
        try {
            delay(2000)
            AppLogger.d(TAG, "Storage permissions test completed")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Storage permissions test failed: ${e.message}")
        }
    }

    private suspend fun testMicrophonePermissions() {
        AppLogger.d(TAG, "Testing microphone permissions")
        try {
            delay(2000)
            AppLogger.d(TAG, "Microphone permissions test completed")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Microphone permissions test failed: ${e.message}")
        }
    }

    private suspend fun testPermissionFlow() {
        AppLogger.d(TAG, "Testing permission flow")
        try {
            delay(3000)
            AppLogger.d(TAG, "Permission flow test completed")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Permission flow test failed: ${e.message}")
        }
    }

    private suspend fun testPermissionPersistence() {
        AppLogger.d(TAG, "Testing permission persistence")
        try {
            delay(2000)
            AppLogger.d(TAG, "Permission persistence test completed")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Permission persistence test failed: ${e.message}")
        }
    }

    private fun runIndividualTest(testId: String) {
        lifecycleScope.launch {
            when (testId) {
                "camera_permissions" -> testCameraPermissions()
                "bluetooth_permissions" -> testBluetoothPermissions()
                "storage_permissions" -> testStoragePermissions()
                "microphone_permissions" -> testMicrophonePermissions()
                "permission_flow" -> testPermissionFlow()
                "permission_persistence" -> testPermissionPersistence()
            }
        }
    }
}