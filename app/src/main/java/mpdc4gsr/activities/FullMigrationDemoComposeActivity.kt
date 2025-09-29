package mpdc4gsr.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import mpdc4gsr.compose.base.BaseComposeActivity
import mpdc4gsr.compose.theme.IRCameraTheme

/**
 * Modern Compose implementation of Full Migration Demo
 * Showcases the complete migration journey from traditional to Compose
 */
class FullMigrationDemoComposeActivity : BaseComposeActivity<FullMigrationDemoViewModel>() {
    
    override fun createViewModel(): FullMigrationDemoViewModel = 
        viewModels<FullMigrationDemoViewModel>().value
    
    @Composable
    override fun Content(viewModel: FullMigrationDemoViewModel) {
        IRCameraTheme {
            FullMigrationDemoScreen(
                viewModel = viewModel,
                onNavigateBack = { finish() }
            )
        }
    }
}

@Composable
fun FullMigrationDemoScreen(
    viewModel: FullMigrationDemoViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Material 3 App Bar
        CenterAlignedTopAppBar(
            title = { Text("Complete Migration Demo") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Navigate back"
                    )
                }
            },
            actions = {
                IconButton(onClick = { viewModel.startFullDemo() }) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start full demo",
                        tint = if (uiState.isDemoRunning) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(onClick = { viewModel.refreshMigrationStatus() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh status"
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
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
            // Migration Overview Card
            item {
                MigrationOverviewCard(
                    migrationStats = uiState.migrationStats,
                    onViewDetails = { viewModel.showMigrationDetails() }
                )
            }
            
            // Demo Progress Card
            if (uiState.isDemoRunning) {
                item {
                    DemoProgressCard(
                        currentStep = uiState.currentDemoStep,
                        totalSteps = uiState.totalDemoSteps,
                        progress = uiState.demoProgress,
                        onStopDemo = { viewModel.stopDemo() }
                    )
                }
            }
            
            // Migration Categories
            item {
                Text(
                    text = "Migration Categories",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            items(uiState.migrationCategories) { category ->
                MigrationCategoryCard(
                    category = category,
                    onViewCategory = { viewModel.viewCategory(category) },
                    onDemoCategory = { viewModel.demoCategory(category) }
                )
            }
            
            // Before/After Comparisons
            if (uiState.comparisons.isNotEmpty()) {
                item {
                    Text(
                        text = "Before/After Comparisons",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                items(uiState.comparisons) { comparison ->
                    BeforeAfterComparisonCard(
                        comparison = comparison,
                        onViewComparison = { viewModel.viewComparison(comparison) }
                    )
                }
            }
            
            // Migration Benefits
            item {
                MigrationBenefitsCard(
                    benefits = uiState.migrationBenefits
                )
            }
            
            // Error Display
            uiState.error?.let { error ->
                item {
                    ErrorCard(
                        error = error,
                        onDismiss = { viewModel.clearError() }
                    )
                }
            }
        }
    }
}

@Composable
private fun MigrationOverviewCard(
    migrationStats: MigrationStats,
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
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
                        text = "Migration Progress",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Text(
                        text = "${migrationStats.completedActivities}/${migrationStats.totalActivities} Activities",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                CircularProgressIndicator(
                    progress = migrationStats.completionPercentage / 100f,
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = migrationStats.completionPercentage / 100f,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${migrationStats.completionPercentage}% Complete",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MigrationMetric(
                    label = "Performance",
                    value = "+${migrationStats.performanceImprovement}%",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                MigrationMetric(
                    label = "Code Quality",
                    value = "+${migrationStats.codeQualityImprovement}%",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                MigrationMetric(
                    label = "Maintainability",
                    value = "+${migrationStats.maintainabilityImprovement}%",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onViewDetails,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Detailed Analysis")
            }
        }
    }
}

@Composable
private fun MigrationMetric(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

@Composable
private fun DemoProgressCard(
    currentStep: String,
    totalSteps: Int,
    progress: Float,
    onStopDemo: () -> Unit
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
                        text = "Demo in Progress",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    
                    Text(
                        text = currentStep,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                
                Button(
                    onClick = onStopDemo,
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
                progress = progress,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Step ${(progress * totalSteps).toInt() + 1} of $totalSteps",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MigrationCategoryCard(
    category: MigrationCategory,
    onViewCategory: () -> Unit,
    onDemoCategory: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (category.status) {
                CategoryStatus.COMPLETE -> MaterialTheme.colorScheme.secondaryContainer
                CategoryStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiaryContainer
                CategoryStatus.PLANNED -> MaterialTheme.colorScheme.surfaceVariant
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
                        imageVector = when (category.status) {
                            CategoryStatus.COMPLETE -> Icons.Default.CheckCircle
                            CategoryStatus.IN_PROGRESS -> Icons.Default.Schedule
                            CategoryStatus.PLANNED -> Icons.Default.RadioButtonUnchecked
                        },
                        contentDescription = "Status",
                        modifier = Modifier.size(24.dp),
                        tint = when (category.status) {
                            CategoryStatus.COMPLETE -> Color.Green
                            CategoryStatus.IN_PROGRESS -> Color.Yellow
                            CategoryStatus.PLANNED -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = when (category.status) {
                                CategoryStatus.COMPLETE -> MaterialTheme.colorScheme.onSecondaryContainer
                                CategoryStatus.IN_PROGRESS -> MaterialTheme.colorScheme.onTertiaryContainer
                                CategoryStatus.PLANNED -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        
                        Text(
                            text = "${category.completedCount}/${category.totalCount} Activities",
                            style = MaterialTheme.typography.bodySmall,
                            color = when (category.status) {
                                CategoryStatus.COMPLETE -> MaterialTheme.colorScheme.onSecondaryContainer
                                CategoryStatus.IN_PROGRESS -> MaterialTheme.colorScheme.onTertiaryContainer
                                CategoryStatus.PLANNED -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                
                Text(
                    text = "${(category.completedCount.toFloat() / category.totalCount * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = when (category.status) {
                        CategoryStatus.COMPLETE -> MaterialTheme.colorScheme.onSecondaryContainer
                        CategoryStatus.IN_PROGRESS -> MaterialTheme.colorScheme.onTertiaryContainer
                        CategoryStatus.PLANNED -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = category.description,
                style = MaterialTheme.typography.bodyMedium,
                color = when (category.status) {
                    CategoryStatus.COMPLETE -> MaterialTheme.colorScheme.onSecondaryContainer
                    CategoryStatus.IN_PROGRESS -> MaterialTheme.colorScheme.onTertiaryContainer
                    CategoryStatus.PLANNED -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewCategory,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View")
                }
                
                Button(
                    onClick = onDemoCategory,
                    modifier = Modifier.weight(1f),
                    enabled = category.status == CategoryStatus.COMPLETE
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Demo")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BeforeAfterComparisonCard(
    comparison: BeforeAfterComparison,
    onViewComparison: () -> Unit
) {
    Card(
        onClick = onViewComparison,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = comparison.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Before
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = "Before",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = comparison.beforeDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                
                // After
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = "After",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = comparison.afterDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Improvement: ${comparison.improvementMetric}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun MigrationBenefitsCard(
    benefits: List<MigrationBenefit>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Migration Benefits",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            benefits.forEach { benefit ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Benefit",
                        modifier = Modifier.size(20.dp),
                        tint = Color.Green
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = benefit.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Text(
                            text = benefit.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
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

// Data classes
data class MigrationStats(
    val totalActivities: Int,
    val completedActivities: Int,
    val completionPercentage: Int,
    val performanceImprovement: Int,
    val codeQualityImprovement: Int,
    val maintainabilityImprovement: Int
)

data class MigrationCategory(
    val id: String,
    val name: String,
    val description: String,
    val totalCount: Int,
    val completedCount: Int,
    val status: CategoryStatus
)

data class BeforeAfterComparison(
    val title: String,
    val beforeDescription: String,
    val afterDescription: String,
    val improvementMetric: String
)

data class MigrationBenefit(
    val title: String,
    val description: String
)

enum class CategoryStatus {
    COMPLETE, IN_PROGRESS, PLANNED
}