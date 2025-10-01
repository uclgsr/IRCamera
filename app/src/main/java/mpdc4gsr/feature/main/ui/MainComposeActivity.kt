package mpdc4gsr.feature.main.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mpdc4gsr.feature.gsr.ui.GSRSensorScreen
import mpdc4gsr.feature.gsr.ui.GSRSettingsScreen
import mpdc4gsr.feature.camera.ui.RGBCameraScreen
import mpdc4gsr.feature.camera.ui.CameraSettingsScreen
import mpdc4gsr.feature.network.ui.ConnectScreen
import mpdc4gsr.feature.network.ui.NetworkSettingsScreen
import mpdc4gsr.feature.thermal.ui.ThermalMonitorScreen
import mpdc4gsr.feature.thermal.ui.ThermalSettingsScreen
import mpdc4gsr.feature.thermal.ui.GalleryScreen
import mpdc4gsr.feature.thermal.ui.CalibrateScreen
import mpdc4gsr.feature.thermal.ui.AnnotateScreen
import mpdc4gsr.feature.thermal.ui.CalibrationScreen
import mpdc4gsr.feature.settings.ui.SettingsScreen
import mpdc4gsr.feature.settings.ui.ProfileScreen
import mpdc4gsr.feature.settings.ui.AboutScreen
import mpdc4gsr.feature.settings.ui.RecordingSettingsScreen
import mpdc4gsr.feature.settings.ui.StorageSettingsScreen
import mpdc4gsr.feature.settings.ui.SyncSettingsScreen
import mpdc4gsr.feature.settings.ui.AppInfoScreen
import mpdc4gsr.feature.settings.ui.PrivacyPolicyScreen
import mpdc4gsr.feature.settings.ui.ProfileScreen
import mpdc4gsr.feature.settings.ui.SettingsScreen
import mpdc4gsr.feature.thermal.ui.GalleryScreen
import mpdc4gsr.feature.thermal.ui.ThermalMonitorScreen
import mpdc4gsr.feature.thermal.ui.ThermalSettingsScreen
import mpdc4gsr.feature.thermal.ui.CalibrationScreen
import mpdc4gsr.feature.thermal.ui.GalleryScreen
import mpdc4gsr.feature.settings.ui.SettingsScreen
import mpdc4gsr.feature.settings.ui.ProfileScreen
import mpdc4gsr.feature.settings.ui.AboutScreen
import mpdc4gsr.feature.settings.ui.HelpScreen
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.core.ui.model.SensorType


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
                navController = navController
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
                onBackClick = { navController.popBackStack() },
                onNavigateToGSRSettings = { navController.navigate("gsr_settings") },
                onNavigateToThermalSettings = { navController.navigate("thermal_settings") },
                onNavigateToCameraSettings = { navController.navigate("camera_settings") },
                onNavigateToRecordingSettings = { navController.navigate("recording_settings") },
                onNavigateToStorageSettings = { navController.navigate("storage_settings") },
                onNavigateToSyncSettings = { navController.navigate("sync_settings") },
                onNavigateToCalibration = { navController.navigate("calibration") },
                onNavigateToNetworkSettings = { navController.navigate("network_settings") },
                onNavigateToDiagnostics = { navController.navigate("diagnostics") },
                onNavigateToAppInfo = { navController.navigate("app_info") },
                onNavigateToPrivacyPolicy = { navController.navigate("privacy_policy") },
                onNavigateToHelp = { navController.navigate("help") }
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

        composable("recording_settings") {
            RecordingSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("storage_settings") {
            StorageSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("sync_settings") {
            SyncSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("calibration") {
            CalibrationScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("network_settings") {
            NetworkSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("diagnostics") {
            DiagnosticsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("app_info") {
            AppInfoScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("privacy_policy") {
            PrivacyPolicyScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("help") {
            HelpScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}