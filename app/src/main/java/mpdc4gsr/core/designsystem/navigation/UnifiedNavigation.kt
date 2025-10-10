package mpdc4gsr.core.designsystem.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mpdc4gsr.core.designsystem.model.SensorType
import mpdc4gsr.feature.capture.camera.ui.CameraDashboardScreen
import mpdc4gsr.feature.capture.camera.ui.CameraSettingsScreen
import mpdc4gsr.feature.capture.camera.ui.RGBCameraScreen
import mpdc4gsr.feature.capture.camera.ui.TimeLapseCameraScreen
import mpdc4gsr.feature.capture.gsr.ui.GSRDataViewScreen
import mpdc4gsr.feature.capture.gsr.ui.GSRPlotScreen
import mpdc4gsr.feature.capture.gsr.ui.GSRSettingsScreen
import mpdc4gsr.feature.capture.gsr.ui.ResearchTemplateScreen
import mpdc4gsr.feature.capture.gsr.ui.SessionDetailScreen
import mpdc4gsr.feature.capture.thermal.ui.ThermalCameraScreen
import mpdc4gsr.feature.capture.thermal.ui.ThermalLoadingScreen
import mpdc4gsr.feature.capture.thermal.ui.ThermalSettingsScreen
import mpdc4gsr.feature.connectivity.ui.DevicePairingScreen
import mpdc4gsr.feature.control.settings.ui.AboutScreen
import mpdc4gsr.feature.control.settings.ui.ProfileEditScreen
import mpdc4gsr.feature.control.settings.ui.SettingsScreen
import mpdc4gsr.feature.dashboard.ui.ComponentShowcaseScreen
import mpdc4gsr.feature.dashboard.ui.MainScreen
import mpdc4gsr.feature.dashboard.ui.UnifiedSensorDashboard
import mpdc4gsr.feature.testing.ui.TestResultsScreen

sealed class UnifiedRoute(
    val route: String,
    val displayName: String = "",
) {
    // Main Application Routes
    object Home : UnifiedRoute("home", "Home")

    object Dashboard : UnifiedRoute("dashboard", "Sensor Overview")

    // GSR Sensor Routes
    object GSRSettings : UnifiedRoute("gsr_settings", "GSR Settings")

    object GSRPlot : UnifiedRoute("gsr_plot/{sessionId}", "GSR Session") {
        fun createRoute(sessionId: String) = "gsr_plot/$sessionId"
    }

    object GSRDataView : UnifiedRoute("gsr_data_view", "Export GSR Data") {
        fun createRoute() = "gsr_data_view"
    }

    object GSRSessionDetail : UnifiedRoute("gsr_session_detail/{sessionId}", "Session Details") {
        fun createRoute(sessionId: String) = "gsr_session_detail/$sessionId"
    }

    object ResearchTemplates : UnifiedRoute("research_templates", "Research Templates")

    // Camera Integration Routes
    object CameraDashboard : UnifiedRoute("camera_dashboard", "Camera Hub")

    object RGBCamera : UnifiedRoute("rgb_camera", "RGB Camera")

    object CameraSettings : UnifiedRoute("camera_settings", "Camera Settings")

    object TimeLapseCamera : UnifiedRoute("timelapse_camera", "Time-Lapse Camera")

    // Network Routes
    object DevicePairing : UnifiedRoute("device_pairing", "Device Pairing")

    object PermissionRequest : UnifiedRoute("permission_request", "Permissions")

    // Thermal Camera Routes - Consolidated (removed ThermalMain duplicate)
    object ThermalGallery : UnifiedRoute("thermal_gallery", "Gallery")

    object ThermalReport : UnifiedRoute("thermal_report", "Reports")

    object ThermalCamera : UnifiedRoute("thermal_camera", "Thermal Imaging")

    object ThermalSettings : UnifiedRoute("thermal_settings", "Thermal Settings")

    // System Routes
    object Settings : UnifiedRoute("settings", "Settings")

    object About : UnifiedRoute("about", "About")

    object Profile : UnifiedRoute("profile", "Profile")

    object ProfileEdit : UnifiedRoute("profile_edit", "Edit Profile")

    object NetworkConfig : UnifiedRoute("network_config", "Network")

    // Settings Sub-Routes
    object RecordingSettings : UnifiedRoute("recording_settings", "Recording Settings")

    object StorageSettings : UnifiedRoute("storage_settings", "Storage Settings")

    object SyncSettings : UnifiedRoute("sync_settings", "Sync Settings")

    object Calibration : UnifiedRoute("calibration", "Calibration")

    object NetworkSettings : UnifiedRoute("network_settings", "Network Settings")

    object Diagnostics : UnifiedRoute("diagnostics", "Diagnostics")

    object AppInfo : UnifiedRoute("app_info", "App Info")

    object PrivacyPolicy : UnifiedRoute("privacy_policy", "Privacy Policy")

    object Help : UnifiedRoute("help", "Help")

    // Development and Demo Routes
    object ComponentShowcase : UnifiedRoute("component_showcase", "Feature Demos")

    object TestingSuite : UnifiedRoute("testing_suite", "Diagnostics")
}

@Composable
fun UnifiedNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = UnifiedRoute.Home.route,
) {
    val context = LocalContext.current
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { with(NavigationAnimations) { slideInFromRight() } },
        exitTransition = { with(NavigationAnimations) { slideOutToLeft() } },
        popEnterTransition = { with(NavigationAnimations) { slideInFromLeft() } },
        popExitTransition = { with(NavigationAnimations) { slideOutToRight() } },
    ) {
        // Home and Dashboard
        composable(UnifiedRoute.Home.route) {
            NavigationPerformanceHelper.TrackNavigation(UnifiedRoute.Home.displayName)
            MainScreen(
                onNavigateToSensors = { navController.navigate(UnifiedRoute.Dashboard.route) },
                onNavigateToGallery = { navController.navigate(UnifiedRoute.ThermalGallery.route) },
                onNavigateToSettings = { navController.navigate(UnifiedRoute.Settings.route) },
                onNavigateToProfile = { navController.navigate(UnifiedRoute.Profile.route) },
            )
        }
        composable(UnifiedRoute.Dashboard.route) {
            NavigationPerformanceHelper.TrackNavigation("Dashboard")
            UnifiedSensorDashboard(
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { navController.navigate(UnifiedRoute.GSRSettings.route) },
                onSensorClick = { sensorType ->
                    when (sensorType) {
                        SensorType.GSR -> navController.navigate(UnifiedRoute.GSRSettings.route)
                        SensorType.ThermalIR -> navController.navigate(UnifiedRoute.ThermalCamera.route)
                        SensorType.RGBCamera -> navController.navigate(UnifiedRoute.CameraDashboard.route)
                    }
                },
                onCameraSettingsClick = { navController.navigate(UnifiedRoute.CameraSettings.route) },
                onGSRSettingsClick = { navController.navigate(UnifiedRoute.GSRSettings.route) },
                onThermalSettingsClick = { navController.navigate(UnifiedRoute.ThermalSettings.route) },
            )
        }
        // GSR Sensor Routes
        composable(UnifiedRoute.GSRSettings.route) {
            NavigationPerformanceHelper.TrackNavigation("GSRSettings")
            GSRSettingsScreen(
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(UnifiedRoute.GSRSessionDetail.route) { backStackEntry ->
            NavigationPerformanceHelper.TrackNavigation("GSRSessionDetail")
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: "unknown"
            SessionDetailScreen(
                sessionId = sessionId,
                onBackClick = { navController.popBackStack() },
                onNavigateToGSRPlot = {
                    navController.navigate(
                        UnifiedRoute.GSRPlot.createRoute(
                            sessionId,
                        ),
                    )
                },
            )
        }
        composable(UnifiedRoute.GSRPlot.route) { backStackEntry ->
            NavigationPerformanceHelper.TrackNavigation("GSRPlot")
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: "unknown"
            GSRPlotScreen(
                sessionId = sessionId,
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(UnifiedRoute.GSRDataView.route) {
            NavigationPerformanceHelper.TrackNavigation("GSRDataView")
            GSRDataViewScreen(
                filePath = "",
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(UnifiedRoute.ResearchTemplates.route) {
            NavigationPerformanceHelper.TrackNavigation("ResearchTemplates")
            ResearchTemplateScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        // Camera Integration Routes
        composable(UnifiedRoute.CameraDashboard.route) {
            CameraDashboardScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(UnifiedRoute.CameraSettings.route) },
                onNavigateToSingleCamera = { navController.navigate(UnifiedRoute.RGBCamera.route) },
                onNavigateToGallery = { navController.navigate(UnifiedRoute.ThermalGallery.route) },
                onNavigateToTimeLapse = { navController.navigate(UnifiedRoute.TimeLapseCamera.route) },
            )
        }
        composable(UnifiedRoute.RGBCamera.route) {
            RGBCameraScreen(
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { navController.navigate(UnifiedRoute.CameraSettings.route) },
                onCapturePhoto = { },
            )
        }
        composable(UnifiedRoute.TimeLapseCamera.route) {
            NavigationPerformanceHelper.TrackNavigation("TimeLapseCamera")
            TimeLapseCameraScreen(
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(UnifiedRoute.CameraSettings.route) {
            CameraSettingsScreen(
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(UnifiedRoute.DevicePairing.route) {
            LaunchedEffect(Unit) {
                try {
                    // Try to launch permission request activity if it exists
                    try {
                        mpdc4gsr.core.designsystem.PermissionRequestComposeActivity
                            .startActivity(context)
                    } catch (e: Exception) {
                        mpdc4gsr.core.common.AppLogger
                            .e("UnifiedNavigation", "Unexpected Exception in UnifiedNavigation catch block", e)
                    }
                } catch (e: Exception) {
                    // Final fallback - just show loading screen
                }
            }
            ThermalLoadingScreen("Loading Device Pairing...")
        }
        // Thermal Camera Routes - ThermalMain removed, use ThermalCamera instead
        composable(UnifiedRoute.ThermalGallery.route) {
            LaunchedEffect(Unit) {
                try {
                    // Use class reference instead of hard-coded string
                    val activityClass =
                        try {
                            com.mpdc4gsr.module.thermalunified.activity.ThermalGalleryComposeActivity::class.java
                        } catch (e: NoClassDefFoundError) {
                            null
                        }
                    if (activityClass != null) {
                        context.startActivity(Intent(context, activityClass))
                    }
                } catch (e: Exception) {
                    // Fallback to placeholder
                }
            }
            ThermalLoadingScreen("Loading Thermal Gallery...")
        }
        composable(UnifiedRoute.ThermalReport.route) {
            LaunchedEffect(Unit) {
                try {
                    // Use class reference instead of hard-coded string
                    val activityClass =
                        try {
                            com.mpdc4gsr.module.thermalunified.activity.ThermalReportComposeActivity::class.java
                        } catch (e: NoClassDefFoundError) {
                            null
                        }
                    if (activityClass != null) {
                        context.startActivity(Intent(context, activityClass))
                    }
                } catch (e: Exception) {
                    // Fallback to placeholder
                }
            }
            ThermalLoadingScreen("Loading Thermal Reports...")
        }
        composable(UnifiedRoute.ThermalCamera.route) {
            NavigationPerformanceHelper.TrackNavigation("ThermalCamera")
            ThermalCameraScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(UnifiedRoute.ThermalSettings.route) },
                onNavigateToGallery = { navController.navigate(UnifiedRoute.ThermalGallery.route) },
            )
        }
        composable(UnifiedRoute.ThermalSettings.route) {
            ThermalSettingsScreen(
                onBackClick = { navController.popBackStack() },
            )
        }
        // Settings and System Routes
        composable(UnifiedRoute.Settings.route) {
            NavigationPerformanceHelper.TrackNavigation("Settings")
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToGSRSettings = { navController.navigate(UnifiedRoute.GSRSettings.route) },
                onNavigateToThermalSettings = { navController.navigate(UnifiedRoute.ThermalSettings.route) },
                onNavigateToCameraSettings = { navController.navigate(UnifiedRoute.CameraSettings.route) },
                onNavigateToRecordingSettings = { navController.navigate(UnifiedRoute.RecordingSettings.route) },
                onNavigateToStorageSettings = { navController.navigate(UnifiedRoute.StorageSettings.route) },
                onNavigateToSyncSettings = { navController.navigate(UnifiedRoute.SyncSettings.route) },
                onNavigateToCalibration = { navController.navigate(UnifiedRoute.Calibration.route) },
                onNavigateToNetworkSettings = { navController.navigate(UnifiedRoute.NetworkConfig.route) },
                onNavigateToDiagnostics = { navController.navigate(UnifiedRoute.Diagnostics.route) },
                onNavigateToAppInfo = { navController.navigate(UnifiedRoute.AppInfo.route) },
                onNavigateToPrivacyPolicy = { navController.navigate(UnifiedRoute.PrivacyPolicy.route) },
                onNavigateToHelp = { navController.navigate(UnifiedRoute.Help.route) },
            )
        }
        composable(UnifiedRoute.About.route) {
            NavigationPerformanceHelper.TrackNavigation("About")
            AboutScreen(
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(UnifiedRoute.Profile.route) {
            NavigationPerformanceHelper.TrackNavigation("Profile")
            mpdc4gsr.feature.control.settings.ui.ProfileScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToResearchTemplates = { navController.navigate(UnifiedRoute.ResearchTemplates.route) },
                onNavigateToPreferences = { navController.navigate(UnifiedRoute.Settings.route) },
                onExportData = { navController.navigate(UnifiedRoute.GSRDataView.route) },
                onNavigateToEditProfile = { navController.navigate(UnifiedRoute.ProfileEdit.route) },
            )
        }
        composable(UnifiedRoute.ProfileEdit.route) {
            NavigationPerformanceHelper.TrackNavigation("ProfileEdit")
            ProfileEditScreen(
                onBackClick = { navController.popBackStack() },
                onSave = { profileData ->
                    // Profile data saved - navigate back
                    navController.popBackStack()
                },
            )
        }
        composable(UnifiedRoute.ComponentShowcase.route) {
            NavigationPerformanceHelper.TrackNavigation("ComponentShowcase")
            ComponentShowcaseScreen(
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(UnifiedRoute.TestingSuite.route) {
            NavigationPerformanceHelper.TrackNavigation("TestingSuite")
            TestResultsScreen(
                onBackClick = { navController.popBackStack() },
            )
        }
        // Network & Device Management Routes
        composable(UnifiedRoute.PermissionRequest.route) {
            LaunchedEffect(Unit) {
                try {
                    // Use class reference instead of hard-coded string
                    val activityClass =
                        try {
                            mpdc4gsr.core.designsystem.PermissionRequestComposeActivity::class.java
                        } catch (e: NoClassDefFoundError) {
                            null
                        }
                    if (activityClass != null) {
                        context.startActivity(Intent(context, activityClass))
                    }
                } catch (e: Exception) {
                    // Fallback - just show loading screen
                }
            }
            ThermalLoadingScreen("Loading Permission Manager...")
        }
        composable("device_pairing_screen") {
            DevicePairingScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        // Additional Settings Routes
        composable(UnifiedRoute.RecordingSettings.route) {
            mpdc4gsr.feature.control.settings.ui.RecordingSettingsScreen(
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(UnifiedRoute.StorageSettings.route) {
            mpdc4gsr.feature.control.settings.ui.StorageSettingsScreen(
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(UnifiedRoute.SyncSettings.route) {
            mpdc4gsr.feature.control.settings.ui.SyncSettingsScreen(
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(UnifiedRoute.Calibration.route) {
            mpdc4gsr.feature.capture.thermal.ui.ThermalCalibrationScreen(
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(UnifiedRoute.NetworkConfig.route) {
            mpdc4gsr.feature.connectivity.ui.NetworkSettingsScreen(
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(UnifiedRoute.Diagnostics.route) {
            mpdc4gsr.feature.dashboard.ui.DiagnosticsScreen(
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(UnifiedRoute.AppInfo.route) {
            mpdc4gsr.feature.control.settings.ui.AppInfoScreen(
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(UnifiedRoute.PrivacyPolicy.route) {
            mpdc4gsr.feature.control.settings.ui.PrivacyPolicyScreen(
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(UnifiedRoute.Help.route) {
            mpdc4gsr.feature.control.settings.ui.HelpScreen(
                onBackClick = { navController.popBackStack() },
            )
        }
    }
}

object NavigationHelper {
    fun navigateToGSRSettings(navController: NavHostController) {
        navController.navigate(UnifiedRoute.GSRSettings.route)
    }

    fun navigateToSessionDetail(
        navController: NavHostController,
        sessionId: String,
    ) {
        navController.navigate(UnifiedRoute.GSRSessionDetail.createRoute(sessionId))
    }

    fun navigateToCamera(navController: NavHostController) {
        navController.navigate(UnifiedRoute.CameraDashboard.route)
    }

    fun navigateToDevicePairing(navController: NavHostController) {
        navController.navigate(UnifiedRoute.DevicePairing.route)
    }

    fun navigateToPermissionRequest(navController: NavHostController) {
        navController.navigate(UnifiedRoute.PermissionRequest.route)
    }

    // Quick Action Navigation - User-centric direct access
    fun captureThermalImage(navController: NavHostController) {
        navController.navigate(UnifiedRoute.ThermalCamera.route)
    }

    fun startGSRSession(navController: NavHostController) {
        navController.navigate(UnifiedRoute.GSRSettings.route)
    }

    fun viewGallery(navController: NavHostController) {
        navController.navigate(UnifiedRoute.ThermalGallery.route)
    }

    fun viewRecentSessions(navController: NavHostController) {
        navController.navigate(UnifiedRoute.GSRDataView.route)
    }

    fun navigateWithPopUp(
        navController: NavHostController,
        destination: String,
        popUpTo: String,
        inclusive: Boolean = false,
    ) {
        navController.navigate(destination) {
            popUpTo(popUpTo) {
                this.inclusive = inclusive
            }
        }
    }
}
