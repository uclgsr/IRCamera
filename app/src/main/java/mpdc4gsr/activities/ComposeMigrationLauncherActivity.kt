package mpdc4gsr.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mpdc4gsr.compose.theme.IRCameraTheme

/**
 * Launch Activity for demonstrating the complete Compose migration
 * This activity serves as an entry point to all implemented tasks
 */
class ComposeMigrationLauncherActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            IRCameraTheme {
                MigrationLauncherScreen()
            }
        }
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
                            text = "🚀 Enhanced Migration - Dev Updated",
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
                    title = "Enhanced Main Dashboard",
                    subtitle = "Updated with consolidated layout integration",
                    icon = Icons.Default.Dashboard,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                MainActivityAlternative::class.java
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
                                SensorDashboardComposeEnhanced::class.java
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
                                SensorDashboardComposeActivity::class.java
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
                    title = "Task E: Navigation Demo",
                    subtitle = "Unified navigation system showcase",
                    icon = Icons.Default.Navigation,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                FullMigrationDemoActivity::class.java
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
                                mpdc4gsr.compose.testing.TestingSuiteHubActivity::class.java
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
                    title = "WebView Activity (Compose)",
                    subtitle = "Modern WebView implementation with error handling",
                    icon = Icons.Default.Web,
                    onClick = {
                        val intent = Intent(
                            this@ComposeMigrationLauncherActivity,
                            WebViewActivityCompose::class.java
                        )
                        intent.putExtra("URL", "https://github.com/uclgsr/IRCamera")
                        startActivity(intent)
                    }
                )

                LauncherCard(
                    title = "Version Info (Compose)",
                    subtitle = "Complete app version information with modern UI",
                    icon = Icons.Default.Info,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                VersionActivityCompose::class.java
                            )
                        )
                    }
                )

                LauncherCard(
                    title = "Policy Viewer (Compose)",
                    subtitle = "Privacy policy and terms with rich content display",
                    icon = Icons.Default.Policy,
                    onClick = {
                        val intent = Intent(
                            this@ComposeMigrationLauncherActivity,
                            PolicyActivityCompose::class.java
                        )
                        intent.putExtra(PolicyActivityCompose.KEY_THEME_TYPE, 2) // Privacy Policy
                        startActivity(intent)
                    }
                )

                LauncherCard(
                    title = "Device Type Selection (Compose)",
                    subtitle = "Modern device selection with enhanced UX",
                    icon = Icons.Default.DeviceHub,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                DeviceTypeActivityCompose::class.java
                            )
                        )
                    }
                )

                LauncherCard(
                    title = "Help & Support (Compose)",
                    subtitle = "Interactive help guide with actionable steps",
                    icon = Icons.Default.Help,
                    onClick = {
                        val intent = Intent(
                            this@ComposeMigrationLauncherActivity,
                            MoreHelpActivityCompose::class.java
                        )
                        intent.putExtra("SETTING_CONNECTION_TYPE", 1) // Connection help
                        startActivity(intent)
                    }
                )

                LauncherCard(
                    title = "PDF Manual Viewer (Compose)",
                    subtitle = "Enhanced manual viewer with modern UI",
                    icon = Icons.Default.PictureAsPdf,
                    onClick = {
                        val intent = Intent(
                            this@ComposeMigrationLauncherActivity,
                            PdfActivityCompose::class.java
                        )
                        intent.putExtra("isTS001", true) // TC001 manual
                        startActivity(intent)
                    }
                )

                LauncherCard(
                    title = "Terms & Conditions (Compose)",
                    subtitle = "Modern agreement screen with interactive elements",
                    icon = Icons.Default.Assignment,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                ClauseActivityCompose::class.java
                            )
                        )
                    }
                )

                LauncherCard(
                    title = "Network Configuration (Compose)",
                    subtitle = "Advanced network setup with device discovery",
                    icon = Icons.Default.NetworkCheck,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                NetworkConfigActivityCompose::class.java
                            )
                        )
                    }
                )

                LauncherCard(
                    title = "Unified Sensor Control (Compose)",
                    subtitle = "Comprehensive sensor management and monitoring",
                    icon = Icons.Default.Sensors,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                UnifiedSensorActivityCompose::class.java
                            )
                        )
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
                    title = "Fault-Tolerant Recording (Compose)",
                    subtitle = "Enhanced recording with automatic error recovery",
                    icon = Icons.Default.HighQuality,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                FaultTolerantRecordingActivityCompose::class.java
                            )
                        )
                    }
                )

                LauncherCard(
                    title = "Simplified Interface (Compose)",
                    subtitle = "Clean and streamlined user interface",
                    icon = Icons.Default.Tune,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                SimplifiedMainActivityCompose::class.java
                            )
                        )
                    }
                )

                LauncherCard(
                    title = "Thermal Image Editor (Compose)",
                    subtitle = "Advanced thermal image editing and analysis",
                    icon = Icons.Default.PhotoFilter,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                IRGalleryEditActivityCompose::class.java
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
                    title = "Shimmer GSR Testing (Compose)",
                    subtitle = "Advanced GSR sensor testing with real-time data monitoring",
                    icon = Icons.Default.Sensors,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                ShimmerMvpActivityCompose::class.java
                            )
                        )
                    }
                )

                LauncherCard(
                    title = "Sensor Dashboard Test (Compose)",
                    subtitle = "Comprehensive sensor dashboard testing interface",
                    icon = Icons.Default.Dashboard,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                SensorDashboardTestActivityCompose::class.java
                            )
                        )
                    }
                )

                LauncherCard(
                    title = "Network Test Interface (Compose)",
                    subtitle = "PC Remote Control and bidirectional telemetry testing",
                    icon = Icons.Default.NetworkCheck,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                SimpleNetworkTestActivityCompose::class.java
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
                    title = "GSR Device Management (Compose)",
                    subtitle = "Enhanced GSR device discovery, connection, and real-time monitoring",
                    icon = Icons.Default.DeviceHub,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                GSRDeviceManagementActivityCompose::class.java
                            )
                        )
                    }
                )

                LauncherCard(
                    title = "Multi-Modal Recording (Compose)",
                    subtitle = "Advanced coordinated multi-sensor recording with live statistics",
                    icon = Icons.Default.RecordVoiceOver,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                MultiModalRecordingActivityCompose::class.java
                            )
                        )
                    }
                )

                LauncherCard(
                    title = "Session Manager (Compose)",
                    subtitle = "Comprehensive session management with filtering and export capabilities",
                    icon = Icons.Default.Folder,
                    onClick = {
                        startActivity(
                            Intent(
                                this@ComposeMigrationLauncherActivity,
                                SessionManagerActivityCompose::class.java
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
                                            ComposeDemoActivity::class.java
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Demo Showcase")
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
            onClick = onClick,
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