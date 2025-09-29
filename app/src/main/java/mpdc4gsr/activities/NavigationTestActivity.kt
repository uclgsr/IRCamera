package mpdc4gsr.activities

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
import androidx.navigation.compose.rememberNavController
import com.mpdc4gsr.libunified.app.compose.theme.LibTheme
import mpdc4gsr.compose.navigation.UnifiedNavHost
import mpdc4gsr.compose.navigation.UnifiedRoute

/**
 * Navigation Test Activity
 * 
 * This activity provides a test interface for all navigation routes to verify
 * that the navigation system works correctly after the updates.
 */
class NavigationTestActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            LibTheme {
                NavigationTestScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationTestScreen() {
    val navController = rememberNavController()
    var showRouteList by remember { mutableStateOf(true) }
    
    if (showRouteList) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopAppBar(
                title = { Text("Navigation Test") },
                actions = {
                    TextButton(
                        onClick = { showRouteList = false }
                    ) {
                        Text("Start Navigation")
                    }
                }
            )
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val routes = listOf(
                    "Home" to UnifiedRoute.Home.route,
                    "Dashboard" to UnifiedRoute.Dashboard.route,
                    "GSR Settings" to UnifiedRoute.GSRSettings.route,
                    "Camera Dashboard" to UnifiedRoute.CameraDashboard.route,
                    "Thermal Camera" to UnifiedRoute.ThermalCamera.route,
                    "Device Pairing" to UnifiedRoute.DevicePairing.route,
                    "Permission Request" to UnifiedRoute.PermissionRequest.route,
                    "Settings" to UnifiedRoute.Settings.route,
                    "About" to UnifiedRoute.About.route,
                    "Modernization Progress" to UnifiedRoute.ModernizationProgress.route
                )
                
                items(routes) { (name, route) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            navController.navigate(route)
                            showRouteList = false
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.NavigateNext,
                                contentDescription = "Navigate"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = route,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Show the unified navigation host
        UnifiedNavHost(navController = navController)
    }
}