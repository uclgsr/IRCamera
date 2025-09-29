package mpdc4gsr.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mpdc4gsr.libunified.app.compose.theme.LibTheme

/**
 * Unified Navigation Demo Activity
 * 
 * This activity serves as a comprehensive demonstration of all navigation
 * systems that have been updated and integrated in the IRCamera application.
 */
class UnifiedNavigationDemoActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            LibTheme {
                UnifiedNavigationDemoScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedNavigationDemoScreen() {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("IRCamera Navigation Demo") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val demoOptions = listOf(
                DemoOption(
                    "Primary Navigation System",
                    "Test the main unified navigation with all screens",
                    Icons.Default.Home,
                    MainActivity::class.java
                ),
                DemoOption(
                    "Navigation Route Testing",
                    "Comprehensive testing interface for all routes",
                    Icons.Default.Assessment,
                    NavigationTestActivity::class.java
                ),
                DemoOption(
                    "Legacy MainActivity",
                    "Original MainActivity for backward compatibility",
                    Icons.Default.History,
                    MainActivityLegacy::class.java
                ),
                DemoOption(
                    "Alternative MainActivity", 
                    "Experimental MainActivity with advanced features",
                    Icons.Default.Science,
                    MainActivityAlternative::class.java
                ),
                DemoOption(
                    "Compose Components Showcase",
                    "Demonstration of all available compose components",
                    Icons.Default.ViewModule,
                    ComposeComponentsShowcaseActivity::class.java
                ),
                DemoOption(
                    "Sensor Dashboard",
                    "GSR sensor monitoring and control interface",
                    Icons.Default.Sensors,
                    SensorDashboardComposeActivity::class.java
                ),
                DemoOption(
                    "Settings & Configuration",
                    "Application settings and device configuration",
                    Icons.Default.Settings,
                    SettingsComposeActivity::class.java
                )
            )
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Navigation System Status",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        StatusItem("MainActivity Implementation", true)
                        StatusItem("Unified Navigation Routes", true)
                        StatusItem("Compose Screen Integration", true)
                        StatusItem("Activity Declarations", true)
                        StatusItem("Theme Integration", true)
                    }
                }
            }
            
            items(demoOptions) { option ->
                DemoOptionCard(
                    option = option,
                    onNavigate = { activityClass ->
                        try {
                            context.startActivity(Intent(context, activityClass))
                        } catch (e: Exception) {
                            // Handle missing activity gracefully
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun StatusItem(label: String, isComplete: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            if (isComplete) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = if (isComplete) "Complete" else "Incomplete",
            tint = if (isComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun DemoOptionCard(
    option: DemoOption,
    onNavigate: (Class<*>) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onNavigate(option.activityClass) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                option.icon,
                contentDescription = option.title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

private data class DemoOption(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val activityClass: Class<*>
)