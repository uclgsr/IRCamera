package mpdc4gsr.compose.navigation

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
 * Demo Navigation Screen showcasing all completed Tasks
 * 
 * This screen demonstrates the complete Compose migration with:
 * - Task A: Main Dashboard Migration ✅
 * - Task B: Thermal Camera Enhancement ✅
 * - Task C: Sensor Dashboard Modernization ✅
 * - Task D: Settings Migration ✅
 * - Task E: Navigation Integration ✅
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoNavigationScreen(
    onNavigateToMainCompose: () -> Unit,
    onNavigateToThermalCompose: () -> Unit,
    onNavigateToSensorDashboard: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToOriginalMain: () -> Unit
) {
    IRCameraTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "IRCamera Compose Migration - Complete",
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
                            text = "🎉 Full Migration Complete!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "All parallel development tasks have been completed",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
                
                // Task completion overview
                TaskCompletionOverview()
                
                // Navigation options
                Text(
                    text = "Explore Completed Tasks",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Task A: Main Dashboard
                NavigationCard(
                    title = "Task A: Main Dashboard",
                    subtitle = "Hybrid MainActivity with modern Compose UI",
                    icon = Icons.Default.Dashboard,
                    status = "✅ COMPLETE",
                    onClick = onNavigateToMainCompose
                )
                
                // Task B: Thermal Camera
                NavigationCard(
                    title = "Task B: Thermal Camera",
                    subtitle = "Enhanced thermal UI with preserved functionality",
                    icon = Icons.Default.Camera,
                    status = "✅ COMPLETE", 
                    onClick = onNavigateToThermalCompose
                )
                
                // Task C: Sensor Dashboard
                NavigationCard(
                    title = "Task C: Sensor Dashboard",
                    subtitle = "Modern GSR visualization and monitoring",
                    icon = Icons.Default.Analytics,
                    status = "✅ COMPLETE",
                    onClick = onNavigateToSensorDashboard
                )
                
                // Task D: Settings Migration
                NavigationCard(
                    title = "Task D: Settings Migration",
                    subtitle = "Complete Compose-based settings screens",
                    icon = Icons.Default.Settings,
                    status = "✅ COMPLETE",
                    onClick = onNavigateToSettings
                )
                
                // Task E: Navigation Integration (this screen demonstrates it)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Navigation,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Task E: Navigation Integration",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Unified navigation system (this screen!)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Text(
                            text = "✅ ACTIVE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
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
                            text = "Compare with Original",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "See the difference between original and migrated UI",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onNavigateToOriginalMain,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Launch Original MainActivity")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskCompletionOverview() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Migration Progress",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            val tasks = listOf(
                "✅ Infrastructure Setup" to "Base classes, theme, components",
                "✅ Task A: Main Dashboard" to "Hybrid MainActivity implementation",
                "✅ Task B: Thermal Camera" to "Enhanced thermal UI with controls",
                "✅ Task C: Sensor Dashboard" to "GSR visualization and monitoring", 
                "✅ Task D: Settings Migration" to "Complete Compose settings",
                "✅ Task E: Navigation Integration" to "Unified navigation system"
            )
            
            tasks.forEach { (title, subtitle) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    status: String,
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
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}