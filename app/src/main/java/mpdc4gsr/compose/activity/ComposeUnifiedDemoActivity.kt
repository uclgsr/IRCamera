package mpdc4gsr.compose.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mpdc4gsr.compose.screens.*
import mpdc4gsr.compose.theme.IRCameraTheme

/**
 * Demo activity to showcase the unified IR Camera UI with Compose
 * Demonstrates navigation between all refactored screens
 */
class ComposeUnifiedDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IRCameraTheme {
                ComposeUnifiedDemo()
            }
        }
    }
}

@Composable
fun ComposeUnifiedDemo() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "demo_home"
    ) {
        composable("demo_home") {
            DemoHomeScreen(
                onNavigateToConnect = { navController.navigate("connect") },
                onNavigateToMonitor = { navController.navigate("monitor") },
                onNavigateToAnnotate = { navController.navigate("annotate") }
            )
        }
        
        composable("connect") {
            ConnectScreen(
                onDeviceSelected = { device ->
                    // Navigate to monitor when device is selected
                    navController.navigate("monitor")
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable("monitor") {
            ThermalMonitorScreen(
                onBackClick = { navController.popBackStack() },
                onSettingsClick = {
                    // Settings functionality
                },
                onRecordClick = {
                    // Recording functionality
                }
            )
        }
        
        composable("annotate") {
            AnnotateScreen(
                onBackClick = { navController.popBackStack() },
                onSave = {
                    // Save functionality
                },
                onShare = {
                    // Share functionality
                }
            )
        }
    }
}

@Composable
private fun DemoHomeScreen(
    onNavigateToConnect: () -> Unit,
    onNavigateToMonitor: () -> Unit,
    onNavigateToAnnotate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "IR Camera Compose UI Demo",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Text(
            text = "Unified UI Implementation",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Demo navigation buttons
        Button(
            onClick = onNavigateToConnect,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Connect Screen")
        }
        
        Button(
            onClick = onNavigateToMonitor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Thermal Monitor Screen")
        }
        
        Button(
            onClick = onNavigateToAnnotate,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Annotation Screen")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
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
                    text = "Features Implemented:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val features = listOf(
                    "• TitleBar component replacing TitleView",
                    "• ConnectScreen with device list (LazyColumn)",
                    "• ThermalMonitorScreen with camera preview",
                    "• Temperature overlays and measurement displays",
                    "• AnnotateScreen with report functionality",
                    "• Consistent dark theme matching reference",
                    "• Navigation between all screens"
                )
                
                features.forEach { feature ->
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ComposeUnifiedDemoPreview() {
    IRCameraTheme {
        ComposeUnifiedDemo()
    }
}