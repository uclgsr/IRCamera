package mpdc4gsr.activities

import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import mpdc4gsr.sensors.gsr.GSRSettingsActivity
import mpdc4gsr.sensors.gsr.GSRSettingsComposeActivity
import mpdc4gsr.sensors.gsr.SessionDetailActivity
import mpdc4gsr.sensors.gsr.SessionDetailComposeActivity

/**
 * GSRModernizationDemoActivity - Demonstrates Compose Migration Progress
 * 
 * This activity showcases the ongoing modernization effort by providing
 * side-by-side comparison of traditional vs Compose implementations.
 * It demonstrates the benefits of the architectural improvements made.
 */
class GSRModernizationDemoActivity : BaseComposeActivity<GSRModernizationViewModel>() {

    override fun createViewModel(): GSRModernizationViewModel {
        return viewModels<GSRModernizationViewModel>().value
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(viewModel: GSRModernizationViewModel) {
        LibUnifiedTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { 
                            Text(
                                "GSR Modernization Demo",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                ModernizationDemoContent(
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun ModernizationDemoContent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Introduction Card
        IntroductionCard()
        
        // GSR Settings Comparison
        ComparisonCard(
            title = "GSR Settings",
            description = "Configuration interface for GSR sensor parameters",
            onLaunchTraditional = {
                GSRSettingsActivity.startActivity(context)
            },
            onLaunchCompose = {
                GSRSettingsComposeActivity.startActivity(context)
            }
        )
        
        // Session Detail Comparison
        ComparisonCard(
            title = "Session Details",
            description = "Detailed view of GSR recording session data",
            onLaunchTraditional = {
                SessionDetailActivity.startActivity(context, "demo_session_123")
            },
            onLaunchCompose = {
                SessionDetailComposeActivity.startActivity(context, "demo_session_123")
            }
        )
        
        // Progress Summary Card
        ProgressSummaryCard()
    }
}

@Composable
private fun IntroductionCard() {
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
                    Icons.Default.NewReleases,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Compose Modernization Progress",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                "This demo showcases the ongoing modernization of GSR sensor activities " +
                "from traditional Android Views to modern Jetpack Compose. Compare the " +
                "user experience, visual design, and functionality improvements.",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Chip(
                    onClick = { },
                    label = { Text("Phase 2 Complete") }
                )
                Chip(
                    onClick = { },
                    label = { Text("Modern UI") }
                )
                Chip(
                    onClick = { },
                    label = { Text("Enhanced UX") }
                )
            }
        }
    }
}

@Composable
private fun ComparisonCard(
    title: String,
    description: String,
    onLaunchTraditional: () -> Unit,
    onLaunchCompose: () -> Unit
) {
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
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Divider()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onLaunchTraditional,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Traditional")
                        Text(
                            "Fragment/View",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                Button(
                    onClick = onLaunchCompose,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Modern")
                        Text(
                            "Compose",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressSummaryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Modernization Progress",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Divider()
            
            ProgressIndicatorWithLabel("Infrastructure Setup", 1.0f)
            ProgressIndicatorWithLabel("MainActivity Consolidation", 1.0f)
            ProgressIndicatorWithLabel("GSR Activities Migration", 0.3f)
            ProgressIndicatorWithLabel("Camera Activities Migration", 0.1f)
            ProgressIndicatorWithLabel("Navigation Unification", 0.2f)
            
            Text(
                "🎯 Next Steps: Continue migrating remaining sensor activities, " +
                "implement unified navigation, and conduct comprehensive testing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProgressIndicatorWithLabel(
    label: String,
    progress: Float
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

class GSRModernizationViewModel : BaseViewModel() {
    // Future implementation could include:
    // - Progress tracking
    // - Feature comparison metrics
    // - User feedback collection
}