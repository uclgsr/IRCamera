package mpdc4gsr.compose.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mpdc4gsr.compose.screens.*
import mpdc4gsr.compose.sensors.gsr.GSRSensorScreen
import mpdc4gsr.compose.sensors.camera.RGBCameraScreen
import mpdc4gsr.compose.theme.IRCameraTheme

/**
 * Main Compose Activity - Unified entry point for the IR Camera application
 * Replaces the traditional MainActivity with a complete Compose implementation
 */
class MainComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IRCameraTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        // Main screen with bottom navigation
        composable("main") {
            MainScreen(
                onNavigateToSensors = { navController.navigate("unified_dashboard") },
                onNavigateToGallery = { navController.navigate("gallery") },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToProfile = { navController.navigate("profile") }
            )
        }
        
        // Unified sensor dashboard
        composable("unified_dashboard") {
            UnifiedSensorDashboard(
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { navController.navigate("settings") },
                onSensorClick = { sensorType ->
                    when (sensorType) {
                        SensorType.GSR -> navController.navigate("gsr_sensor")
                        SensorType.ThermalIR -> navController.navigate("thermal_connect")
                        SensorType.RGBCamera -> navController.navigate("rgb_camera")
                    }
                }
            )
        }
        
        // Individual sensor screens
        composable("gsr_sensor") {
            GSRSensorScreen(
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { navController.navigate("gsr_settings") },
                onSaveData = {
                    // Save GSR data functionality
                }
            )
        }
        
        composable("rgb_camera") {
            RGBCameraScreen(
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { navController.navigate("camera_settings") },
                onCapturePhoto = {
                    // Capture photo functionality
                }
            )
        }
        
        // Thermal camera workflow
        composable("thermal_connect") {
            ConnectScreen(
                onDeviceSelected = { device ->
                    navController.navigate("thermal_monitor")
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable("thermal_monitor") {
            ThermalMonitorScreen(
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { navController.navigate("thermal_settings") },
                onRecordClick = {
                    // Recording functionality
                }
            )
        }
        
        composable("thermal_calibrate") {
            CalibrateScreen(
                onBackClick = { navController.popBackStack() },
                onCalibrationComplete = {
                    navController.popBackStack()
                },
                onCalibrationCancel = { navController.popBackStack() }
            )
        }
        
        composable("thermal_annotate") {
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
        
        // Additional screens
        composable("gallery") {
            GalleryScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable("settings") {
            SettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable("profile") {
            ProfileScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        
        // Settings screens
        composable("gsr_settings") {
            GSRSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable("camera_settings") {
            CameraSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable("thermal_settings") {
            ThermalSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}