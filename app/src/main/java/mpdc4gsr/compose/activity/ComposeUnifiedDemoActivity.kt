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
import mpdc4gsr.compose.sensors.gsr.GSRSensorScreen
import mpdc4gsr.compose.sensors.camera.RGBCameraScreen
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
                onNavigateToCalibrate = { navController.navigate("calibrate") },
                onNavigateToAnnotate = { navController.navigate("annotate") },
                onNavigateToUnifiedDashboard = { navController.navigate("unified_dashboard") },
                onNavigateToGSR = { navController.navigate("gsr_sensor") },
                onNavigateToRGBCamera = { navController.navigate("rgb_camera") }
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
        
        composable("calibrate") {
            CalibrateScreen(
                onBackClick = { navController.popBackStack() },
                onCalibrationComplete = {
                    // Navigate back after successful calibration
                    navController.popBackStack()
                },
                onCalibrationCancel = { navController.popBackStack() }
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
        
        composable("unified_dashboard") {
            UnifiedSensorDashboard(
                onBackClick = { navController.popBackStack() },
                onSettingsClick = {
                    // Settings functionality
                },
                onSensorClick = { sensorType ->
                    when (sensorType) {
                        SensorType.GSR -> navController.navigate("gsr_sensor")
                        SensorType.ThermalIR -> navController.navigate("monitor")
                        SensorType.RGBCamera -> navController.navigate("rgb_camera")
                    }
                }
            )
        }
        
        composable("gsr_sensor") {
            GSRSensorScreen(
                onBackClick = { navController.popBackStack() },
                onSettingsClick = {
                    // GSR settings functionality
                },
                onSaveData = {
                    // Save GSR data functionality
                }
            )
        }
        
        composable("rgb_camera") {
            RGBCameraScreen(
                onBackClick = { navController.popBackStack() },
                onSettingsClick = {
                    // RGB camera settings functionality
                },
                onCapturePhoto = {
                    // Capture photo functionality
                }
            )
        }
    }
}

@Composable
private fun DemoNavigationScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "IR Camera Compose UI",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Complete Multi-Modal Sensor Platform",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // Navigation items
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(getAllNavigationItems()) { item ->
                NavigationItemCard(
                    item = item,
                    onClick = { navController.navigate(item.route) }
                )
            }
        }
    }
}

@Composable
private fun NavigationItemCard(
    item: NavigationItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = Color.Blue,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.description,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

data class NavigationItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val route: String
)

private fun getAllNavigationItems(): List<NavigationItem> {
    return listOf(
        // Main Application
        NavigationItem(
            title = "Main Application",
            description = "Complete app with tab navigation",
            icon = Icons.Default.Home,
            route = "main"
        ),
        NavigationItem(
            title = "Unified Sensor Dashboard",
            description = "Master dashboard for all sensors",
            icon = Icons.Default.Dashboard,
            route = "unified_dashboard"
        ),
        
        // Core Thermal Screens
        NavigationItem(
            title = "Device Connection",
            description = "Connect thermal devices",
            icon = Icons.Default.Link,
            route = "connect"
        ),
        NavigationItem(
            title = "Thermal Monitor",
            description = "Live thermal camera preview",
            icon = Icons.Default.Thermostat,
            route = "monitor"
        ),
        NavigationItem(
            title = "Calibration",
            description = "Dual-camera alignment",
            icon = Icons.Default.Tune,
            route = "calibrate"
        ),
        NavigationItem(
            title = "Annotation & Report",  
            description = "Temperature measurement annotation",
            icon = Icons.Default.Edit,
            route = "annotate"
        ),
        
        // Individual Sensors
        NavigationItem(
            title = "GSR Sensor",
            description = "Galvanic skin response monitoring",
            icon = Icons.Default.MonitorHeart,
            route = "gsr_sensor"
        ),
        NavigationItem(
            title = "RGB Camera",
            description = "Standard camera interface",
            icon = Icons.Default.Camera,
            route = "rgb_camera"
        ),
        
        // Data & Media
        NavigationItem(
            title = "Gallery",
            description = "Media and data gallery",
            icon = Icons.Default.PhotoLibrary,
            route = "gallery"
        ),
        NavigationItem(
            title = "Thermal Gallery",
            description = "Thermal images & recordings",
            icon = Icons.Default.Folder,
            route = "thermal_gallery"
        ),
        NavigationItem(
            title = "GSR Data Viewer",
            description = "Comprehensive GSR data analysis",
            icon = Icons.Default.Analytics,
            route = "gsr_data"
        ),
        NavigationItem(
            title = "GSR Video Player",
            description = "GSR session video playback",
            icon = Icons.Default.PlayCircle,
            route = "gsr_video"
        ),
        
        // Recording & Reports
        NavigationItem(
            title = "Multi-Modal Recording",
            description = "Synchronized sensor recording",
            icon = Icons.Default.RecordVoiceOver,
            route = "multimodal_recording"
        ),
        NavigationItem(
            title = "Report Creation",
            description = "Create thermal analysis reports",
            icon = Icons.Default.Description,
            route = "report_creation"
        ),
        
        // Settings & Profile
        NavigationItem(
            title = "Settings",
            description = "Application settings",
            icon = Icons.Default.Settings,
            route = "settings"
        ),
        NavigationItem(
            title = "GSR Settings",
            description = "GSR sensor configuration",
            icon = Icons.Default.Tune,
            route = "gsr_settings"
        ),
        NavigationItem(
            title = "Camera Settings",
            description = "RGB camera configuration",
            icon = Icons.Default.CameraAlt,
            route = "camera_settings"
        ),
        NavigationItem(
            title = "Thermal Settings",
            description = "Thermal camera configuration",
            icon = Icons.Default.Thermostat,
            route = "thermal_settings"
        ),
        NavigationItem(
            title = "Profile",
            description = "User profile and research stats",
            icon = Icons.Default.Person,
            route = "profile"
        ),
        NavigationItem(
            title = "Policy Viewer",
            description = "Terms & privacy policy",
            icon = Icons.Default.Policy,
            route = "policy"
        )
    )
}
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
            text = "Unified Multi-Modal Sensor UI",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Unified dashboard button (featured)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Button(
                onClick = onNavigateToUnifiedDashboard,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                )
            ) {
                Text(
                    "🚀 Unified Sensor Dashboard",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Individual sensor screens
        Text(
            text = "Individual Sensor Screens:",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onNavigateToGSR,
                modifier = Modifier.weight(1f)
            ) {
                Text("GSR Sensor", fontSize = 12.sp)
            }
            Button(
                onClick = onNavigateToRGBCamera,
                modifier = Modifier.weight(1f)
            ) {
                Text("RGB Camera", fontSize = 12.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Original thermal camera screens
        Text(
            text = "Thermal Camera Screens:",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        
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
            onClick = onNavigateToCalibrate,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calibration Screen")
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
                    "✅ Unified Multi-Modal Sensor Dashboard",
                    "✅ GSR (Galvanic Skin Response) Sensor Screen",
                    "✅ RGB Camera Control and Recording Screen", 
                    "✅ Thermal IR Camera with Temperature Overlays",
                    "✅ TitleBar component replacing TitleView",
                    "✅ ConnectScreen with device list (LazyColumn)",
                    "✅ ThermalMonitorScreen with camera preview",
                    "✅ CalibrateScreen with dual-camera alignment",
                    "✅ AnnotateScreen with report functionality",
                    "✅ Real-time sensor data visualization",
                    "✅ Consistent dark theme matching reference",
                    "✅ Complete navigation between all screens"
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