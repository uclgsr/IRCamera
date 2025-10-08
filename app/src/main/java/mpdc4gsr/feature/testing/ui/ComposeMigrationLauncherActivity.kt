package mpdc4gsr.feature.testing.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.camera.ui.DualModeCameraComposeActivity
import mpdc4gsr.feature.main.ui.DeviceTypeComposeActivity
import mpdc4gsr.feature.main.ui.MainActivity
import mpdc4gsr.feature.network.ui.DevicePairingComposeActivity
import mpdc4gsr.feature.settings.ui.*
import mpdc4gsr.feature.thermal.ui.IRGalleryEditComposeActivity

class ComposeMigrationLauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IRCameraTheme {
                LifecycleAwareMigrationLauncherScreen()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        window.decorView.post {
            window.decorView.clearAnimation()
        }
    }

    @Composable
    private fun LifecycleAwareMigrationLauncherScreen() {
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_PAUSE -> {
                        window.decorView.post {
                            window.decorView.clearAnimation()
                        }
                    }

                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
        MigrationLauncherScreen()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MigrationLauncherScreen() {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "IRCamera Compose Migration",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = " Enhanced Migration - Dev Updated",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Updated with consolidated layout integration from dev branch",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
                // Enhanced implementations
                Text(
                    text = "Enhanced Implementations (Updated)",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                LauncherCard(
                    title = "Unified Main Activity",
                    subtitle = "Single unified MainActivity with all features",
                    icon = Icons.Default.Dashboard,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                mpdc4gsr.feature.main.ui.MainActivity::class.java
                            )
                        )
                    }
                )
                LauncherCard(
                    title = "Enhanced Sensor Dashboard",
                    subtitle = "Multi-modal sensor integration with consolidated patterns",
                    icon = Icons.Default.Analytics,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                mpdc4gsr.feature.gsr.ui.SensorDashboardComposeEnhanced::class.java
                            )
                        )
                    }
                )
                // Original implementations
                Text(
                    text = "Original Implementations",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                LauncherCard(
                    title = "Task A: Main Dashboard",
                    subtitle = "Original hybrid MainActivity with modern Compose UI",
                    icon = Icons.Default.Dashboard,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                MainActivity::class.java
                            )
                        )
                    }
                )
                LauncherCard(
                    title = "Task B: Thermal Camera",
                    subtitle = "Enhanced thermal UI with preserved functionality",
                    icon = Icons.Default.Camera,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                com.mpdc4gsr.module.thermalunified.activity.ThermalCameraComposeActivity::class.java
                            )
                        )
                    }
                )
                LauncherCard(
                    title = "Task C: Sensor Dashboard",
                    subtitle = "Real-time GSR visualization and monitoring",
                    icon = Icons.Default.Analytics,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                mpdc4gsr.feature.gsr.ui.SensorDashboardComposeActivity::class.java
                            )
                        )
                    }
                )
                LauncherCard(
                    title = "Task D: Settings Migration",
                    subtitle = "Complete Compose-based settings screens",
                    icon = Icons.Default.Settings,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                SettingsComposeActivity::class.java
                            )
                        )
                    }
                )
                LauncherCard(
                    title = "Task E: Navigation System",
                    subtitle = "Unified navigation system testing",
                    icon = Icons.Default.Navigation,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                NavigationTestActivity::class.java
                            )
                        )
                    }
                )
                LauncherCard(
                    title = "Testing Suite Hub",
                    subtitle = "Comprehensive testing dashboard with 14+ test activities",
                    icon = Icons.Default.BugReport,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                TestingSuiteHubActivity::class.java
                            )
                        )
                    }
                )
                // New Compose Activities Section
                Text(
                    text = "Additional Compose Conversions",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 16.dp)
                )
                LauncherCard(
                    title = "WebView Activity",
                    subtitle = "Modern WebView implementation with error handling",
                    icon = Icons.Default.Web,
                    onClick = {
                        val intent = Intent(
                            this@ComposeMigrationLauncherActivity,
                            WebViewComposeActivity::class.java
                        )
                        intent.putExtra("URL", "https://github.com/uclgsr/IRCamera")
                        startActivity(intent)
                    }
                )
                LauncherCard(
                    title = "Version Info",
                    subtitle = "Complete app version information with modern UI",
                    icon = Icons.Default.Info,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                VersionComposeActivity::class.java
                            )
                        )
                    }
                )
                LauncherCard(
                    title = "Policy Viewer",
                    subtitle = "Privacy policy and terms with rich content display",
                    icon = Icons.Default.Policy,
                    onClick = {
                        val intent = Intent(
                            this@ComposeMigrationLauncherActivity,
                            PolicyComposeActivity::class.java
                        )
                        intent.putExtra(PolicyComposeActivity.KEY_THEME_TYPE, 2) // Privacy Policy
                        startActivity(intent)
                    }
                )
                LauncherCard(
                    title = "Device Type Selection",
                    subtitle = "Modern device selection with enhanced UX",
                    icon = Icons.Default.DeviceHub,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                DeviceTypeComposeActivity::class.java
                            )
                        )
                    }
                )
                LauncherCard(
                    title = "Help & Support",
                    subtitle = "Interactive help guide with actionable steps",
                    icon = Icons.AutoMirrored.Filled.Help,
                    onClick = {
                        val intent = Intent(
                            this@ComposeMigrationLauncherActivity,
                            MoreHelpComposeActivity::class.java
                        )
                        intent.putExtra("SETTING_CONNECTION_TYPE", 1) // Connection help
                        startActivity(intent)
                    }
                )
                LauncherCard(
                    title = "PDF Manual Viewer",
                    subtitle = "Enhanced manual viewer with modern UI",
                    icon = Icons.Default.PictureAsPdf,
                    onClick = {
                        val intent = Intent(
                            this@ComposeMigrationLauncherActivity,
                            PdfComposeActivity::class.java
                        )
                        intent.putExtra("isTS001", true) // TC001 manual
                        startActivity(intent)
                    }
                )
                LauncherCard(
                    title = "Terms & Conditions",
                    subtitle = "Modern agreement screen with interactive elements",
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                ClauseComposeActivity::class.java
                            )
                        )
                    }
                )
                LauncherCard(
                    title = "Network Configuration",
                    subtitle = "Advanced network setup with device discovery",
                    icon = Icons.Default.NetworkCheck,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                mpdc4gsr.feature.network.ui.NetworkConfigComposeActivity::class.java
                            )
                        )
                    }
                )
                LauncherCard(
                    title = "Unified Sensor Control",
                    subtitle = "Comprehensive sensor management and monitoring",
                    icon = Icons.Default.Sensors,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                mpdc4gsr.feature.gsr.ui.UnifiedSensorComposeActivity::class.java
                            )
                        )
                    }
                )
                LauncherCard(
                    title = "Network Client Test",
                    subtitle = "Test Wi-Fi and Bluetooth network connections",
                    icon = Icons.Default.NetworkWifi,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                mpdc4gsr.feature.network.ui.NetworkClientTestComposeActivity2::class.java
                            )
                        )
                    }
                )
                // New GSR Sensor Activities Section
                Text(
                    text = "GSR Sensor Activities (High Priority)",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp)
                )
                LauncherCard(
                    title = "Session Manager",
                    subtitle = "Modern session management with search and batch operations",
                    icon = Icons.Default.FolderOpen,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                mpdc4gsr.feature.gsr.ui.SessionManagerComposeActivity::class.java
                            )
                        )
                    }
                )
                LauncherCard(
                    title = "Multi-Modal Recording",
                    subtitle = "Advanced multi-sensor recording with real-time monitoring",
                    icon = Icons.Default.RadioButtonChecked,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                mpdc4gsr.feature.gsr.ui.MultiModalRecordingComposeActivity::class.java
                            )
                        )
                    }
                )
                LauncherCard(
                    title = "Shimmer Configuration",
                    subtitle = "Device discovery and configuration with modern UI",
                    icon = Icons.Default.BluetoothConnected,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                mpdc4gsr.feature.gsr.ui.ShimmerConfigComposeActivity::class.java
                            )
                        )
                    }
                )
                LauncherCard(
                    title = "Research Templates",
                    subtitle = "Interactive template gallery with creation wizard",
                    icon = Icons.Default.Science,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                mpdc4gsr.feature.gsr.ui.ResearchTemplateComposeActivity::class.java
                            )
                        )
                    }
                )
                LauncherCard(
                    title = "GSR Video Player",
                    subtitle = "Enhanced video playback with synchronized sensor data",
                    icon = Icons.Default.PlayCircle,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                mpdc4gsr.feature.gsr.ui.GSRVideoPlayerComposeActivity::class.java
                            )
                        )
                    }
                )
                LauncherCard(
                    title = "GSR Data Plot",
                    subtitle = "Modern data visualization with interactive charts",
                    icon = Icons.Default.Timeline,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                mpdc4gsr.feature.gsr.ui.GSRPlotComposeActivity::class.java
                            )
                        )
                    }
                )
                // Network & Device Management Section
                Text(
                    text = "Network & Device Management (Priority 2)",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 16.dp)
                )
                LauncherCard(
                    title = "Device Pairing",
                    subtitle = "Advanced BLE device discovery and pairing with diagnostics",
                    icon = Icons.Default.BluetoothConnected,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                DevicePairingComposeActivity::class.java
                            )
                        )
                    }
                )
                LauncherCard(
                    title = "Permission Manager",
                    subtitle = "Interactive permission management with educational content",
                    icon = Icons.Default.Security,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                mpdc4gsr.core.ui.PermissionRequestComposeActivity::class.java
                            )
                        )
                    }
                )
                LauncherCard(
                    title = "GSR Device Management",
                    subtitle = "Comprehensive GSR device monitoring and configuration",
                    icon = Icons.Default.DeviceHub,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                mpdc4gsr.feature.gsr.ui.GSRDeviceManagementComposeActivity::class.java
                            )
                        )
                    }
                )
                // Camera Integration Section
                Text(
                    text = "Camera Integration Activities (Priority 2)",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 16.dp)
                )
                LauncherCard(
                    title = "Dual Mode Camera",
                    subtitle = "Advanced dual camera recording with thermal and RGB sync",
                    icon = Icons.Default.CameraAlt,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                DualModeCameraComposeActivity::class.java
                            )
                        )
                    }
                )
                // Thermal Camera Module Section
                Text(
                    text = "Thermal Camera Module Activities (Priority 3)",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 16.dp)
                )
                LauncherCard(
                    title = "Thermal Gallery",
                    subtitle = "Advanced thermal image gallery with filtering and analysis",
                    icon = Icons.Default.PhotoLibrary,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                com.mpdc4gsr.module.thermalunified.activity.ThermalGalleryComposeActivity::class.java
                            )
                        )
                    }
                )
                LauncherCard(
                    title = "Thermal Report Creation",
                    subtitle = "Professional thermal report generation with templates",
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                com.mpdc4gsr.module.thermalunified.report.activity.ThermalReportCreationComposeActivity::class.java
                            )
                        )
                    }
                )
                LauncherCard(
                    title = "Thermal Video Player",
                    subtitle = "Advanced thermal video playback with analysis tools",
                    icon = Icons.Default.PlayArrow,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                com.mpdc4gsr.module.thermalunified.activity.ThermalVideoComposeActivity::class.java
                            )
                        )
                    }
                )
                LauncherCard(
                    title = "IR Thermal",
                    subtitle = "Thermal camera interface with essential controls",
                    icon = Icons.Default.Thermostat,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                com.mpdc4gsr.module.thermalunified.activity.ThermalComposeActivity::class.java
                            )
                        )
                    }
                )
                LauncherCard(
                    title = "Thermal Monitoring",
                    subtitle = "Advanced thermal monitoring dashboard with alerts",
                    icon = Icons.Default.Monitor,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                com.mpdc4gsr.module.thermalunified.activity.ThermalMonitoringComposeActivity::class.java
                            )
                        )
                    }
                )
                LauncherCard(
                    title = "Thermal Camera",
                    subtitle = "Thermal camera interface with correction and calibration tools",
                    icon = Icons.Default.AutoFixHigh,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                com.mpdc4gsr.module.thermalunified.activity.ThermalCameraComposeActivity::class.java
                            )
                        )
                    }
                )
                // Fragment Migration Section
                Text(
                    text = "Fragment to Compose Migration (Priority 3)",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 16.dp)
                )
                LauncherCard(
                    title = "Sensor Dashboard Fragment",
                    subtitle = "Modern Fragment with Compose UI for sensor monitoring",
                    icon = Icons.Default.Dashboard,
                    onClick = {
                        // This would be integrated into MainActivity's fragment navigation
                        // For demo purposes, show a message
                        android.widget.Toast.makeText(
                            this@ComposeMigrationLauncherActivity,
                            "Fragment integrated into MainActivity navigation",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                )
                // Advanced Features Section
                Text(
                    text = "Advanced Features",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 16.dp)
                )
                LauncherCard(
                    title = "Fault-Tolerant Recording",
                    subtitle = "Enhanced recording with automatic error recovery",
                    icon = Icons.Default.HighQuality,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                FaultTolerantRecordingComposeActivity::class.java
                            )
                        )
                    }
                )
                LauncherCard(
                    title = "Main Interface",
                    subtitle = "Unified main interface with all features",
                    icon = Icons.Default.Tune,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                mpdc4gsr.feature.main.ui.MainActivity::class.java
                            )
                        )
                    }
                )
                LauncherCard(
                    title = "Thermal Image Editor",
                    subtitle = "Advanced thermal image editing and analysis",
                    icon = Icons.Default.PhotoFilter,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                IRGalleryEditComposeActivity::class.java
                            )
                        )
                    }
                )
                // Testing & Development Section
                Text(
                    text = "Testing & Development Tools",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 16.dp)
                )
                LauncherCard(
                    title = "Shimmer GSR Configuration",
                    subtitle = "Advanced GSR device configuration and testing with real-time data monitoring",
                    icon = Icons.Default.Sensors,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                mpdc4gsr.feature.gsr.ui.ShimmerConfigComposeActivity::class.java
                            )
                        )
                    }
                )
                LauncherCard(
                    title = "Sensor Dashboard Test",
                    subtitle = "Comprehensive sensor dashboard testing interface",
                    icon = Icons.Default.Dashboard,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                SensorDashboardTestComposeActivity::class.java
                            )
                        )
                    }
                )
                LauncherCard(
                    title = "Network Test Interface",
                    subtitle = "PC Remote Control and bidirectional telemetry testing",
                    icon = Icons.Default.NetworkCheck,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                mpdc4gsr.feature.network.ui.SimpleNetworkTestActivityCompose::class.java
                            )
                        )
                    }
                )
                // UI Components Section
                Text(
                    text = "Enhanced UI Components",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 16.dp)
                )
                LauncherCard(
                    title = "Compose Components Showcase",
                    subtitle = "Interactive showcase of modernized UI components with enhanced functionality",
                    icon = Icons.Default.AutoAwesome,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                ComposeComponentsShowcaseActivity::class.java
                            )
                        )
                    }
                )
                // GSR Sensor Suite
                Text(
                    text = "GSR Sensor Suite",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 16.dp)
                )
                LauncherCard(
                    title = "GSR Device Management",
                    subtitle = "Enhanced GSR device discovery, connection, and real-time monitoring",
                    icon = Icons.Default.DeviceHub,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                mpdc4gsr.feature.gsr.ui.GSRDeviceManagementComposeActivity::class.java
                            )
                        )
                    }
                )
                LauncherCard(
                    title = "Multi-Modal Recording",
                    subtitle = "Advanced coordinated multi-sensor recording with live statistics",
                    icon = Icons.Default.RecordVoiceOver,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                mpdc4gsr.feature.gsr.ui.MultiModalRecordingComposeActivity::class.java
                            )
                        )
                    }
                )
                LauncherCard(
                    title = "Session Manager",
                    subtitle = "Comprehensive session management with filtering and export capabilities",
                    icon = Icons.Default.Folder,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                mpdc4gsr.feature.gsr.ui.SessionManagerComposeActivity::class.java
                            )
                        )
                    }
                )
                // Comparison option
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Compare Implementations",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "See the difference between original and migrated UI",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    startActivity(
                                        Intent(
                                            this@ComposeMigrationLauncherActivity,
                                            MainActivity::class.java
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Original UI")
                            }
                            OutlinedButton(
                                onClick = {
                                    startActivity(
                                        Intent(
                                            this@ComposeMigrationLauncherActivity,
                                            ComposeComponentsShowcaseActivity::class.java
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Component Showcase")
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun LauncherCard(
        title: String,
        subtitle: String,
        icon: ImageVector,
        onClick: () -> Unit
    ) {
        Card(
            onClick = mpdc4gsr.core.ui.deferAction(onClick),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}