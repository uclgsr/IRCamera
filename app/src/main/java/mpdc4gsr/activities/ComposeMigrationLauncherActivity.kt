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
                        startActivity(Intent(this@ComposeMigrationLauncherActivity, MainActivityComposeEnhanced::class.java))
                    }
                )

                LauncherCard(
                    title = "Enhanced Sensor Dashboard", 
                    subtitle = "Multi-modal sensor integration with consolidated patterns",
                    icon = Icons.Default.Analytics,
                    onClick = { 
                        startActivity(Intent(this@ComposeMigrationLauncherActivity, SensorDashboardComposeEnhanced::class.java))
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
                        startActivity(Intent(this@ComposeMigrationLauncherActivity, MainActivityCompose::class.java))
                    }
                )

                LauncherCard(
                    title = "Task B: Thermal Camera",
                    subtitle = "Enhanced thermal UI with preserved functionality",
                    icon = Icons.Default.Camera,
                    onClick = { 
                        startActivity(Intent(
                            this@ComposeMigrationLauncherActivity, 
                            com.mpdc4gsr.module.thermalunified.activity.ThermalCameraComposeActivity::class.java
                        ))
                    }
                )

                LauncherCard(
                    title = "Task C: Sensor Dashboard",
                    subtitle = "Real-time GSR visualization and monitoring",
                    icon = Icons.Default.Analytics,
                    onClick = { 
                        startActivity(Intent(this@ComposeMigrationLauncherActivity, SensorDashboardComposeActivity::class.java))
                    }
                )

                LauncherCard(
                    title = "Task D: Settings Migration",
                    subtitle = "Complete Compose-based settings screens",
                    icon = Icons.Default.Settings,
                    onClick = { 
                        startActivity(Intent(this@ComposeMigrationLauncherActivity, SettingsComposeActivity::class.java))
                    }
                )

                LauncherCard(
                    title = "Task E: Navigation Demo",
                    subtitle = "Unified navigation system showcase",
                    icon = Icons.Default.Navigation,
                    onClick = { 
                        startActivity(Intent(this@ComposeMigrationLauncherActivity, FullMigrationDemoActivity::class.java))
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
                                    startActivity(Intent(this@ComposeMigrationLauncherActivity, MainActivity::class.java))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Original UI")
                            }
                            
                            OutlinedButton(
                                onClick = { 
                                    startActivity(Intent(this@ComposeMigrationLauncherActivity, ComposeDemoActivity::class.java))
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