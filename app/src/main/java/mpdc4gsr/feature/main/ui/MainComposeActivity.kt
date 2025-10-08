package mpdc4gsr.feature.main.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import mpdc4gsr.core.ui.model.SensorType
import mpdc4gsr.core.ui.theme.IRCameraTheme
import mpdc4gsr.feature.camera.ui.CameraSettingsScreen
import mpdc4gsr.feature.camera.ui.RGBCameraScreen
import mpdc4gsr.feature.gsr.ui.GSRSettingsScreen
import mpdc4gsr.feature.network.ui.ConnectScreen
import mpdc4gsr.feature.network.ui.NetworkSettingsScreen
import mpdc4gsr.feature.settings.ui.*
import mpdc4gsr.feature.thermal.ui.*

private object MainNavRoutes {
    const val MAIN = "main"
    const val UNIFIED_DASHBOARD = "unified_dashboard"
    const val GSR_SENSOR = "gsr_sensor"
    const val RGB_CAMERA = "rgb_camera"
    const val THERMAL_CONNECT = "thermal_connect"
    const val THERMAL_MONITOR = "thermal_monitor"
    const val THERMAL_CALIBRATE = "thermal_calibrate"
    const val THERMAL_ANNOTATE = "thermal_annotate"
    const val GALLERY = "gallery"
    const val SETTINGS = "settings"
    const val PROFILE = "profile"
    const val GSR_SETTINGS = "gsr_settings"
    const val CAMERA_SETTINGS = "camera_settings"
    const val THERMAL_SETTINGS = "thermal_settings"
    const val RECORDING_SETTINGS = "recording_settings"
    const val STORAGE_SETTINGS = "storage_settings"
    const val SYNC_SETTINGS = "sync_settings"
    const val CALIBRATION = "calibration"
    const val NETWORK_SETTINGS = "network_settings"
    const val DIAGNOSTICS = "diagnostics"
    const val APP_INFO = "app_info"
    const val PRIVACY_POLICY = "privacy_policy"
    const val HELP = "help"
}

@AndroidEntryPoint
class MainComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
        startDestination = MainNavRoutes.MAIN
    ) {
        // Main screen with bottom navigation
        composable(MainNavRoutes.MAIN) {
            MainScreen(
                onNavigateToSensors = { navController.navigate(MainNavRoutes.UNIFIED_DASHBOARD) },
                onNavigateToGallery = { navController.navigate(MainNavRoutes.GALLERY) },
                onNavigateToSettings = { navController.navigate(MainNavRoutes.SETTINGS) },
                onNavigateToProfile = { navController.navigate(MainNavRoutes.PROFILE) },
                onNavigateToSensor = { sensorType ->
                    when (sensorType) {
                        SensorType.GSR -> navController.navigate(MainNavRoutes.GSR_SENSOR)
                        SensorType.ThermalIR -> navController.navigate(MainNavRoutes.THERMAL_CONNECT)
                        SensorType.RGBCamera -> navController.navigate(MainNavRoutes.RGB_CAMERA)
                    }
                }
            )
        }
        // Unified sensor dashboard
        composable(MainNavRoutes.UNIFIED_DASHBOARD) {
            UnifiedSensorDashboard(
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { navController.navigate(MainNavRoutes.SETTINGS) },
                onSensorClick = { sensorType ->
                    when (sensorType) {
                        SensorType.GSR -> navController.navigate(MainNavRoutes.GSR_SENSOR)
                        SensorType.ThermalIR -> navController.navigate(MainNavRoutes.THERMAL_CONNECT)
                        SensorType.RGBCamera -> navController.navigate(MainNavRoutes.RGB_CAMERA)
                    }
                },
                onCameraSettingsClick = { navController.navigate(MainNavRoutes.CAMERA_SETTINGS) },
                onGSRSettingsClick = { navController.navigate(MainNavRoutes.GSR_SETTINGS) },
                onThermalSettingsClick = { navController.navigate(MainNavRoutes.THERMAL_SETTINGS) }
            )
        }
        // Individual sensor screens
        composable(MainNavRoutes.GSR_SENSOR) {
            GSRSensorScreen(
                navController = navController
            )
        }
        composable(MainNavRoutes.RGB_CAMERA) {
            RGBCameraScreen(
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { navController.navigate(MainNavRoutes.CAMERA_SETTINGS) },
                onCapturePhoto = {
                    // Capture photo functionality
                }
            )
        }
        // Thermal camera workflow
        composable(MainNavRoutes.THERMAL_CONNECT) {
            ConnectScreen(
                onDeviceSelected = { device ->
                    navController.navigate(MainNavRoutes.THERMAL_MONITOR)
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(MainNavRoutes.THERMAL_MONITOR) {
            ThermalMonitorScreen(
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { navController.navigate(MainNavRoutes.THERMAL_SETTINGS) },
                onRecordClick = {
                    // Recording functionality
                }
            )
        }
        composable(MainNavRoutes.THERMAL_CALIBRATE) {
            CalibrateScreen(
                onBackClick = { navController.popBackStack() },
                onCalibrationComplete = {
                    navController.popBackStack()
                },
                onCalibrationCancel = { navController.popBackStack() }
            )
        }
        composable(MainNavRoutes.THERMAL_ANNOTATE) {
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
        composable(MainNavRoutes.GALLERY) {
            GalleryScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(MainNavRoutes.SETTINGS) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToGSRSettings = { navController.navigate(MainNavRoutes.GSR_SETTINGS) },
                onNavigateToThermalSettings = { navController.navigate(MainNavRoutes.THERMAL_SETTINGS) },
                onNavigateToCameraSettings = { navController.navigate(MainNavRoutes.CAMERA_SETTINGS) },
                onNavigateToRecordingSettings = { navController.navigate(mpdc4gsr.core.ui.navigation.UnifiedRoute.RecordingSettings.route) },
                onNavigateToStorageSettings = { navController.navigate(mpdc4gsr.core.ui.navigation.UnifiedRoute.StorageSettings.route) },
                onNavigateToSyncSettings = { navController.navigate(mpdc4gsr.core.ui.navigation.UnifiedRoute.SyncSettings.route) },
                onNavigateToCalibration = { navController.navigate(mpdc4gsr.core.ui.navigation.UnifiedRoute.Calibration.route) },
                onNavigateToNetworkSettings = { navController.navigate(mpdc4gsr.core.ui.navigation.UnifiedRoute.NetworkSettings.route) },
                onNavigateToDiagnostics = { navController.navigate(mpdc4gsr.core.ui.navigation.UnifiedRoute.Diagnostics.route) },
                onNavigateToAppInfo = { navController.navigate(mpdc4gsr.core.ui.navigation.UnifiedRoute.AppInfo.route) },
                onNavigateToPrivacyPolicy = { navController.navigate(mpdc4gsr.core.ui.navigation.UnifiedRoute.PrivacyPolicy.route) },
                onNavigateToHelp = { navController.navigate(mpdc4gsr.core.ui.navigation.UnifiedRoute.Help.route) }
            )
        }
        composable(MainNavRoutes.PROFILE) {
            ProfileScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        // Settings screens
        composable(MainNavRoutes.GSR_SETTINGS) {
            GSRSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(MainNavRoutes.CAMERA_SETTINGS) {
            CameraSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(MainNavRoutes.THERMAL_SETTINGS) {
            ThermalSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(MainNavRoutes.RECORDING_SETTINGS) {
            RecordingSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(MainNavRoutes.STORAGE_SETTINGS) {
            StorageSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(MainNavRoutes.SYNC_SETTINGS) {
            SyncSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(MainNavRoutes.CALIBRATION) {
            CalibrationScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(MainNavRoutes.NETWORK_SETTINGS) {
            NetworkSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(MainNavRoutes.DIAGNOSTICS) {
            DiagnosticsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(MainNavRoutes.APP_INFO) {
            AppInfoScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(MainNavRoutes.PRIVACY_POLICY) {
            PrivacyPolicyScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(MainNavRoutes.HELP) {
            HelpScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}