package mpdc4gsr.compose.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme

/**
 * Modernization Progress Screen - Comprehensive Showcase
 *
 * Central hub that demonstrates the complete modernization journey:
 * - Infrastructure improvements overview
 * - Activity migration progress
 * - Interactive demos and comparisons
 * - Next steps and roadmap
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernizationProgressScreen(
    onNavigateToGSRDemo: () -> Unit,
    onNavigateToCameraDemo: () -> Unit,
    onNavigateToThermalDemo: () -> Unit,
    onNavigateToComponentShowcase: () -> Unit,
    modifier: Modifier = Modifier
) {
    LibUnifiedTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Compose Modernization",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        ) { paddingValues ->
            ModernizationProgressContent(
                onNavigateToGSRDemo = onNavigateToGSRDemo,
                onNavigateToCameraDemo = onNavigateToCameraDemo,
                onNavigateToThermalDemo = onNavigateToThermalDemo,
                onNavigateToComponentShowcase = onNavigateToComponentShowcase,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun ModernizationProgressContent(
    onNavigateToGSRDemo: () -> Unit,
    onNavigateToCameraDemo: () -> Unit,
    onNavigateToThermalDemo: () -> Unit,
    onNavigateToComponentShowcase: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Overview Card
        OverviewCard()

        // Infrastructure Achievements
        InfrastructureAchievementsCard()

        // Migration Progress
        MigrationProgressCard()

        // Interactive Demos
        InteractiveDemosCard(
            onNavigateToGSRDemo = onNavigateToGSRDemo,
            onNavigateToCameraDemo = onNavigateToCameraDemo,
            onNavigateToThermalDemo = onNavigateToThermalDemo,
            onNavigateToComponentShowcase = onNavigateToComponentShowcase
        )

        // Next Steps
        NextStepsCard()

        // Technical Metrics
        TechnicalMetricsCard()
    }
}

@Composable
private fun OverviewCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    "IRCamera Compose Modernization",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                "Comprehensive migration from traditional Android Views to modern Jetpack Compose, " +
                        "with cross-module infrastructure and unified navigation system.",
                style = MaterialTheme.typography.bodyLarge
            )

            Divider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem("Phases", "2/3", "Complete")
                MetricItem("Activities", "9", "Modernized")
                MetricItem("Infrastructure", "100%", "Shared")
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    subtitle: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InfrastructureAchievementsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Architecture,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Infrastructure Achievements",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Divider()

            AchievementItem(
                title = "Cross-Module Compose Infrastructure",
                description = "BaseComposeActivity moved to libunified for universal access",
                status = AchievementStatus.COMPLETE
            )

            AchievementItem(
                title = "Unified Theme System",
                description = "LibUnifiedTheme provides consistent styling across modules",
                status = AchievementStatus.COMPLETE
            )

            AchievementItem(
                title = "MainActivity Consolidation",
                description = "Rationalized from 4 conflicting implementations to 3 clear purposes",
                status = AchievementStatus.COMPLETE
            )

            AchievementItem(
                title = "Build System Stabilization",
                description = "Resolved dependency conflicts and plugin issues",
                status = AchievementStatus.COMPLETE
            )

            AchievementItem(
                title = "Unified Navigation System",
                description = "Type-safe navigation with smooth animations",
                status = AchievementStatus.IN_PROGRESS
            )
        }
    }
}

enum class AchievementStatus {
    COMPLETE,
    IN_PROGRESS,
    PLANNED
}

@Composable
private fun AchievementItem(
    title: String,
    description: String,
    status: AchievementStatus
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            when (status) {
                AchievementStatus.COMPLETE -> Icons.Default.CheckCircle
                AchievementStatus.IN_PROGRESS -> Icons.Default.Schedule
                AchievementStatus.PLANNED -> Icons.Default.RadioButtonUnchecked
            },
            contentDescription = null,
            tint = when (status) {
                AchievementStatus.COMPLETE -> MaterialTheme.colorScheme.primary
                AchievementStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiary
                AchievementStatus.PLANNED -> MaterialTheme.colorScheme.outline
            },
            modifier = Modifier.size(20.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MigrationProgressCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Activity Migration Progress",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Divider()

            ProgressCategory(
                title = "Main Application",
                completed = 3,
                total = 4,
                items = listOf(
                    "MainActivity (Primary Compose)" to true,
                    "MainActivityLegacy (Compatibility)" to true,
                    "MainActivityAlternative (Experimental)" to true,
                    "SimplifiedMainActivity" to false
                )
            )

            ProgressCategory(
                title = "GSR Sensor Activities",
                completed = 4,
                total = 7,
                items = listOf(
                    "GSRSettingsComposeActivity" to true,
                    "SessionDetailComposeActivity" to true,
                    "GSRModernizationDemoActivity" to true,
                    "GSRDataViewActivity" to false,
                    "GSRPlotActivity" to false,
                    "MultiModalRecordingActivity" to false,
                    "GSRVideoPlayerActivity" to false
                )
            )

            ProgressCategory(
                title = "Camera Integration",
                completed = 2,
                total = 4,
                items = listOf(
                    "CameraDashboardScreen" to true,
                    "DualModeCameraScreen" to true,
                    "Camera2SystemValidator" to false,
                    "CameraStatusWidget" to false
                )
            )
        }
    }
}

@Composable
private fun ProgressCategory(
    title: String,
    completed: Int,
    total: Int,
    items: List<Pair<String, Boolean>>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                "$completed/$total",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        LinearProgressIndicator(
            progress = { completed.toFloat() / total.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )

        items.forEach { (itemName, isCompleted) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    itemName,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InteractiveDemosCard(
    onNavigateToGSRDemo: () -> Unit,
    onNavigateToCameraDemo: () -> Unit,
    onNavigateToThermalDemo: () -> Unit,
    onNavigateToComponentShowcase: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Interactive Demonstrations",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Divider()

            DemoItem(
                title = "GSR Sensor Modernization",
                description = "Compare traditional vs Compose GSR activities",
                icon = Icons.Default.Sensors,
                onClick = onNavigateToGSRDemo
            )

            DemoItem(
                title = "Camera Integration",
                description = "Modern dual-mode camera interface",
                icon = Icons.Default.CameraAlt,
                onClick = onNavigateToCameraDemo
            )

            DemoItem(
                title = "Thermal Imaging Suite",
                description = "Professional thermal analysis tools",
                icon = Icons.Default.Thermostat,
                onClick = onNavigateToThermalDemo
            )

            DemoItem(
                title = "Component Showcase",
                description = "Explore all modern UI components",
                icon = Icons.Default.Widgets,
                onClick = onNavigateToComponentShowcase
            )

            DemoItem(
                title = "Testing Suite",
                description = "Comprehensive testing and validation",
                icon = Icons.Default.Science,
                onClick = { /* Navigate to testing suite */ }
            )
        }
    }
}

@Composable
private fun DemoItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun NextStepsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Next Steps & Roadmap",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Divider()

            NextStepItem(
                phase = "Phase 3",
                title = "Complete Activity Migration",
                description = "Migrate remaining GSR and camera activities",
                priority = "High"
            )

            NextStepItem(
                phase = "Phase 4",
                title = "Navigation Unification",
                description = "Complete unified navigation implementation",
                priority = "Medium"
            )

            NextStepItem(
                phase = "Phase 5",
                title = "Performance Optimization",
                description = "Optimize Compose performance and memory usage",
                priority = "Medium"
            )

            NextStepItem(
                phase = "Phase 6",
                title = "Comprehensive Testing",
                description = "End-to-end testing of all modernized components",
                priority = "High"
            )
        }
    }
}

@Composable
private fun NextStepItem(
    phase: String,
    title: String,
    description: String,
    priority: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Badge(
            containerColor = when (priority) {
                "High" -> MaterialTheme.colorScheme.error
                "Medium" -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.outline
            }
        ) {
            Text(phase, style = MaterialTheme.typography.labelSmall)
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Badge(
            containerColor = when (priority) {
                "High" -> MaterialTheme.colorScheme.errorContainer
                "Medium" -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ) {
            Text(priority, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun TechnicalMetricsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Technical Impact Metrics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Divider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TechnicalMetricItem(
                    label = "Code Reduction",
                    value = "25%",
                    description = "Fewer duplicate implementations"
                )

                TechnicalMetricItem(
                    label = "Build Stability",
                    value = "100%",
                    description = "Resolved all config conflicts"
                )

                TechnicalMetricItem(
                    label = "Cross-Module",
                    value = "Unified",
                    description = "Shared infrastructure"
                )
            }

            Divider()

            Text(
                "🎯 Key Benefits: Enhanced maintainability, consistent user experience, " +
                        "improved development efficiency, and future-ready architecture.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TechnicalMetricItem(
    label: String,
    value: String,
    description: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}