package mpdc4gsr.feature.testing.ui
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.core.ui.navigation.UnifiedNavHost
import mpdc4gsr.core.ui.navigation.UnifiedRoute

class NavigationTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LibUnifiedTheme {
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
                    "Camera Settings" to UnifiedRoute.CameraSettings.route,
                    "Thermal Camera" to UnifiedRoute.ThermalCamera.route,
                    "Thermal Settings" to UnifiedRoute.ThermalSettings.route,
                    "Device Pairing" to UnifiedRoute.DevicePairing.route,
                    "Permission Request" to UnifiedRoute.PermissionRequest.route,
                    "Settings" to UnifiedRoute.Settings.route,
                    "About" to UnifiedRoute.About.route
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
                                Icons.AutoMirrored.Filled.NavigateNext,
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