package mpdc4gsr.feature.network.ui

import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import mpdc4gsr.core.ui.theme.IRCameraTheme

/**
 * Modern Compose implementation of Network Client Test
 * Provides comprehensive network testing with Material 3 UI
 */
class NetworkClientTestComposeActivity : BaseComposeActivity<NetworkClientTestViewModel>() {

    override fun createViewModel(): NetworkClientTestViewModel =
        viewModels<NetworkClientTestViewModel>().value

    @Composable
    override fun Content(viewModel: NetworkClientTestViewModel) {
        IRCameraTheme {
            NetworkClientTestScreen(
                viewModel = viewModel,
                onNavigateBack = { finish() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkClientTestScreen(
    viewModel: NetworkClientTestViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val localContext = androidx.compose.ui.platform.LocalContext.current
    val uiState by viewModel.networkTestUiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Material 3 App Bar
        CenterAlignedTopAppBar(
            title = { Text("Network Client Test") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back"
                    )
                }
            },
            actions = {
                IconButton(onClick = { viewModel.startComprehensiveTest() }) {
                    Icon(
                        imageVector = if (uiState.isTestRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (uiState.isTestRunning) "Stop test" else "Start test",
                        tint = if (uiState.isTestRunning) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { viewModel.refreshNetworkStatus() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh network"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        // Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Network Status Overview
            item {
                NetworkStatusOverviewCard(
                    networkStatus = NetworkTestStatus(
                        overallStatus = when (uiState.networkStatus) {
                            "Connected" -> TestStatus.PASS
                            "Connecting" -> TestStatus.PENDING
                            "Error" -> TestStatus.FAIL
                            else -> TestStatus.WARNING
                        },
                        latency = 0,
                        bandwidth = 0f,
                        packetLoss = 0f,
                        connectedDevices = if (uiState.networkStatus == "Connected") 1 else 0
                    ),
                    onRunQuickTest = { viewModel.runQuickNetworkTest() }
                )
            }

            // Test Progress (if running)
            if (uiState.isTestRunning) {
                item {
                    TestProgressCard(
                        currentTest = uiState.currentTest,
                        progress = uiState.testProgress,
                        onStopTest = { viewModel.stopTest() }
                    )
                }
            }

            // Test Categories
            item {
                Text(
                    text = "Network Test Categories",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(uiState.testCategories) { category ->
                TestCategoryCard(
                    category = category,
                    onRunCategoryTest = { viewModel.runCategoryTest(category) }
                )
            }

            // Test Results
            if (uiState.testResults.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Test Results",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                items(uiState.testResults.take(5)) { result ->
                    TestResultCard(
                        result = result,
                        onViewDetails = { viewModel.viewTestDetails(result) }
                    )
                }
            }

            // Network Configuration
            item {
                NetworkConfigurationCard(
                    configuration = uiState.networkConfiguration,
                    onUpdateConfiguration = { config ->
                        viewModel.updateNetworkConfiguration(config)
                    }
                )
            }

            // Error Display
            uiState.error?.let { errorMessage ->
                item {
                    ErrorCard(
                        error = errorMessage,
                        onDismiss = { viewModel.clearError() }
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkStatusOverviewCard(
    networkStatus: NetworkTestStatus,
    onRunQuickTest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (networkStatus.overallStatus) {
                TestStatus.PASS -> MaterialTheme.colorScheme.primaryContainer
                TestStatus.FAIL -> MaterialTheme.colorScheme.errorContainer
                TestStatus.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
                TestStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
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
                Column {
                    Text(
                        text = "Network Status",
                        style = MaterialTheme.typography.titleMedium,
                        color = when (networkStatus.overallStatus) {
                            TestStatus.PASS -> MaterialTheme.colorScheme.onPrimaryContainer
                            TestStatus.FAIL -> MaterialTheme.colorScheme.onErrorContainer
                            TestStatus.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
                            TestStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    Text(
                        text = networkStatus.overallStatus.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = when (networkStatus.overallStatus) {
                            TestStatus.PASS -> MaterialTheme.colorScheme.onPrimaryContainer
                            TestStatus.FAIL -> MaterialTheme.colorScheme.onErrorContainer
                            TestStatus.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
                            TestStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                Button(onClick = onRunQuickTest) {
                    Icon(
                        imageVector = Icons.Default.NetworkCheck,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Quick Test")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                NetworkMetric(
                    label = "Latency",
                    value = "${networkStatus.latency}ms",
                    status = if (networkStatus.latency < 100) TestStatus.PASS else TestStatus.WARNING
                )

                NetworkMetric(
                    label = "Bandwidth",
                    value = "${networkStatus.bandwidth} Mbps",
                    status = if (networkStatus.bandwidth > 10) TestStatus.PASS else TestStatus.WARNING
                )

                NetworkMetric(
                    label = "Packet Loss",
                    value = "${networkStatus.packetLoss}%",
                    status = if (networkStatus.packetLoss < 5) TestStatus.PASS else TestStatus.FAIL
                )
            }

            if (networkStatus.connectedDevices > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Connected Devices: ${networkStatus.connectedDevices}",
                    style = MaterialTheme.typography.bodySmall,
                    color = when (networkStatus.overallStatus) {
                        TestStatus.PASS -> MaterialTheme.colorScheme.onPrimaryContainer
                        TestStatus.FAIL -> MaterialTheme.colorScheme.onErrorContainer
                        TestStatus.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
                        TestStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun NetworkMetric(
    label: String,
    value: String,
    status: TestStatus
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = when (status) {
                TestStatus.PASS -> Color.Green
                TestStatus.WARNING -> Color.Yellow
                TestStatus.FAIL -> Color.Red
                TestStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TestProgressCard(
    currentTest: String,
    progress: Float,
    onStopTest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
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
                Column {
                    Text(
                        text = "Testing in Progress",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )

                    Text(
                        text = currentTest,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }

                Button(
                    onClick = onStopTest,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stop")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${(progress * 100).toInt()}% Complete",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestCategoryCard(
    category: NetworkTestCategory,
    onRunCategoryTest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (category.lastResult) {
                TestStatus.PASS -> MaterialTheme.colorScheme.secondaryContainer
                TestStatus.FAIL -> MaterialTheme.colorScheme.errorContainer
                TestStatus.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
                TestStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (category.type) {
                            NetworkTestType.CONNECTION -> Icons.Default.NetworkCheck
                            NetworkTestType.LATENCY -> Icons.Default.Speed
                            NetworkTestType.THROUGHPUT -> Icons.Default.Tune
                            NetworkTestType.RELIABILITY -> Icons.Default.Security
                        },
                        contentDescription = "Test type",
                        modifier = Modifier.size(24.dp),
                        tint = when (category.lastResult) {
                            TestStatus.PASS -> MaterialTheme.colorScheme.onSecondaryContainer
                            TestStatus.FAIL -> MaterialTheme.colorScheme.onErrorContainer
                            TestStatus.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
                            TestStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = when (category.lastResult) {
                                TestStatus.PASS -> MaterialTheme.colorScheme.onSecondaryContainer
                                TestStatus.FAIL -> MaterialTheme.colorScheme.onErrorContainer
                                TestStatus.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
                                TestStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )

                        Text(
                            text = "${category.testCount} tests",
                            style = MaterialTheme.typography.bodySmall,
                            color = when (category.lastResult) {
                                TestStatus.PASS -> MaterialTheme.colorScheme.onSecondaryContainer
                                TestStatus.FAIL -> MaterialTheme.colorScheme.onErrorContainer
                                TestStatus.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
                                TestStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Icon(
                    imageVector = when (category.lastResult) {
                        TestStatus.PASS -> Icons.Default.CheckCircle
                        TestStatus.FAIL -> Icons.Default.Error
                        TestStatus.WARNING -> Icons.Default.Warning
                        TestStatus.PENDING -> Icons.Default.Schedule
                    },
                    contentDescription = "Test result",
                    tint = when (category.lastResult) {
                        TestStatus.PASS -> Color.Green
                        TestStatus.FAIL -> Color.Red
                        TestStatus.WARNING -> Color.Yellow
                        TestStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = category.description,
                style = MaterialTheme.typography.bodyMedium,
                color = when (category.lastResult) {
                    TestStatus.PASS -> MaterialTheme.colorScheme.onSecondaryContainer
                    TestStatus.FAIL -> MaterialTheme.colorScheme.onErrorContainer
                    TestStatus.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
                    TestStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onRunCategoryTest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Run ${category.name} Tests")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestResultCard(
    result: NetworkTestResult,
    onViewDetails: () -> Unit
) {
    Card(
        onClick = onViewDetails,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (result.status) {
                    TestStatus.PASS -> Icons.Default.CheckCircle
                    TestStatus.FAIL -> Icons.Default.Error
                    TestStatus.WARNING -> Icons.Default.Warning
                    TestStatus.PENDING -> Icons.Default.Schedule
                },
                contentDescription = "Test result",
                modifier = Modifier.size(24.dp),
                tint = when (result.status) {
                    TestStatus.PASS -> Color.Green
                    TestStatus.FAIL -> Color.Red
                    TestStatus.WARNING -> Color.Yellow
                    TestStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = result.testName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "${result.timestamp} - ${result.duration}ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (result.details.isNotEmpty()) {
                    Text(
                        text = result.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NetworkConfigurationCard(
    configuration: NetworkConfiguration,
    onUpdateConfiguration: (NetworkConfiguration) -> Unit
) {
    val localContext = androidx.compose.ui.platform.LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Network Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ConfigurationItem(
                        label = "Server Address",
                        value = configuration.serverAddress
                    )

                    ConfigurationItem(
                        label = "Port",
                        value = configuration.port.toString()
                    )

                    ConfigurationItem(
                        label = "Timeout",
                        value = "${configuration.timeoutMs}ms"
                    )

                    ConfigurationItem(
                        label = "Retry Attempts",
                        value = configuration.retryAttempts.toString()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        // TODO: Implement network configuration editor dialog
                        android.widget.Toast.makeText(
                            localContext,
                            "Network configuration editor",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Configuration")
                }
            }
        }
    }
}

@Composable
private fun ConfigurationItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorCard(
    error: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

// Type aliases to use ViewModel types
typealias NetworkTestCategory = NetworkClientTestViewModel.NetworkTestCategory
typealias NetworkTestResult = NetworkClientTestViewModel.NetworkTestResult
typealias TestStatus = NetworkClientTestViewModel.TestStatus
typealias NetworkTestType = NetworkClientTestViewModel.NetworkTestType
typealias NetworkConfiguration = NetworkClientTestViewModel.NetworkConfiguration

// Data classes specific to this activity
data class NetworkTestStatus(
    val overallStatus: TestStatus,
    val latency: Int,
    val bandwidth: Float,
    val packetLoss: Float,
    val connectedDevices: Int
)