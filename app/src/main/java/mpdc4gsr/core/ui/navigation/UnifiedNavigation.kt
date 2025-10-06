package mpdc4gsr.core.ui.navigation

import android.content.Intent
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mpdc4gsr.core.ui.model.SensorType
import mpdc4gsr.feature.camera.ui.CameraDashboardScreen
import mpdc4gsr.feature.camera.ui.CameraSettingsScreen
import mpdc4gsr.feature.camera.ui.DualModeCameraScreen
import mpdc4gsr.feature.camera.ui.RGBCameraScreen
import mpdc4gsr.feature.gsr.ui.GSRDataViewScreen
import mpdc4gsr.feature.gsr.ui.GSRPlotScreen
import mpdc4gsr.feature.gsr.ui.GSRSettingsScreen
import mpdc4gsr.feature.gsr.ui.ResearchTemplateScreen
import mpdc4gsr.feature.gsr.ui.SessionDetailScreen
import mpdc4gsr.feature.main.ui.ComponentShowcaseScreen
import mpdc4gsr.feature.main.ui.MainScreen
import mpdc4gsr.feature.main.ui.UnifiedSensorDashboard
import mpdc4gsr.feature.network.ui.DevicePairingScreen
import mpdc4gsr.feature.settings.ui.AboutScreen
import mpdc4gsr.feature.settings.ui.SettingsScreen
import mpdc4gsr.feature.testing.ui.TestResultsScreen
import mpdc4gsr.feature.thermal.ui.ThermalCameraScreen
import mpdc4gsr.feature.thermal.ui.ThermalLoadingScreen
import mpdc4gsr.feature.thermal.ui.ThermalSettingsScreen

/**
 * Unified Navigation System - Phase 2 Implementation
 *
 * This comprehensive navigation system provides:
 * - Type-safe navigation with sealed class routes
 * - Smooth animations between screens
 * - Deep linking support
 * - Integration with both Compose and traditional activities
 * - Centralized navigation logic for maintainability
 */
sealed class UnifiedRoute(val route: String, val displayName: String = "") {
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
    object DualModeCamera : UnifiedRoute("dual_mode_camera", "Thermal + RGB Camera")
    object RGBCamera : UnifiedRoute("rgb_camera", "RGB Camera")
    object CameraSettings : UnifiedRoute("camera_settings", "Camera Settings")

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
    object NetworkConfig : UnifiedRoute("network_config", "Network")

    // Development and Demo Routes
    object ComponentShowcase : UnifiedRoute("component_showcase", "Feature Demos")
    object TestingSuite : UnifiedRoute("testing_suite", "Diagnostics")
}

@Composable
fun UnifiedNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = UnifiedRoute.Home.route
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { with(NavigationAnimations) { slideInFromRight() } },
        exitTransition = { with(NavigationAnimations) { slideOutToLeft() } },
        popEnterTransition = { with(NavigationAnimations) { slideInFromLeft() } },
        popExitTransition = { with(NavigationAnimations) { slideOutToRight() } }
    ) {
        // Home and Dashboard
        composable(UnifiedRoute.Home.route) {
            NavigationPerformanceHelper.TrackNavigation(UnifiedRoute.Home.displayName)

            MainScreen(
                onNavigateToSensors = { navController.navigate(UnifiedRoute.Dashboard.route) },
                onNavigateToGallery = { navController.navigate(UnifiedRoute.ThermalGallery.route) },
                onNavigateToSettings = { navController.navigate(UnifiedRoute.Settings.route) },
                onNavigateToProfile = { navController.navigate(UnifiedRoute.Profile.route) }
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
                onThermalSettingsClick = { navController.navigate(UnifiedRoute.ThermalSettings.route) }
            )
        }

        // GSR Sensor Routes
        composable(UnifiedRoute.GSRSettings.route) {
            NavigationPerformanceHelper.TrackNavigation("GSRSettings")

            GSRSettingsScreen(
                onBackClick = { navController.popBackStack() }
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
                            sessionId
                        )
                    )
                }
            )
        }

        composable(UnifiedRoute.GSRPlot.route) { backStackEntry ->
            NavigationPerformanceHelper.TrackNavigation("GSRPlot")
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: "unknown"

            GSRPlotScreen(
                sessionId = sessionId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(UnifiedRoute.GSRDataView.route) {
            NavigationPerformanceHelper.TrackNavigation("GSRDataView")

            GSRDataViewScreen(
                filePath = "",
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(UnifiedRoute.ResearchTemplates.route) {
            NavigationPerformanceHelper.TrackNavigation("ResearchTemplates")

            ResearchTemplateScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // Camera Integration Routes
        composable(UnifiedRoute.CameraDashboard.route) {
            CameraDashboardScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToDualMode = { navController.navigate(UnifiedRoute.DualModeCamera.route) },
                onNavigateToSettings = { navController.navigate(UnifiedRoute.CameraSettings.route) },
                onNavigateToSingleCamera = { navController.navigate(UnifiedRoute.RGBCamera.route) },
                onNavigateToGallery = { navController.navigate(UnifiedRoute.ThermalGallery.route) }
            )
        }

        composable(UnifiedRoute.RGBCamera.route) {
            RGBCameraScreen(
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { navController.navigate(UnifiedRoute.CameraSettings.route) },
                onCapturePhoto = { /* Photo capture handled by screen */ }
            )
        }

        composable(UnifiedRoute.DualModeCamera.route) {
            LaunchedEffect(Unit) {
                try {
                    // Use class reference instead of hard-coded string
                    val activityClass = try {
                        mpdc4gsr.feature.camera.ui.DualModeCameraActivityCompose::class.java
                    } catch (e: NoClassDefFoundError) {
                        null
                    }

                    if (activityClass != null) {
                        context.startActivity(Intent(context, activityClass))
                    } else {
                        navController.navigate("dual_mode_camera_screen")
                    }
                } catch (e: Exception) {
                    // Fallback to screen
                    navController.navigate("dual_mode_camera_screen")
                }
            }
            ThermalLoadingScreen("Loading Dual Mode Camera...")
        }

        composable(UnifiedRoute.CameraSettings.route) {
            CameraSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(UnifiedRoute.DevicePairing.route) {
            LaunchedEffect(Unit) {
                try {
                    // Try to launch permission request activity if it exists
                    try {
                        mpdc4gsr.core.ui.PermissionRequestComposeActivity.startActivity(context)
                    } catch (e: Exception) {
                        AppLogger.e("UnifiedNavigation", "Failed to start permission request activity", e)
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
                    val activityClass = try {
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
                    val activityClass = try {
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
                onNavigateToGallery = { navController.navigate(UnifiedRoute.ThermalGallery.route) }
            )
        }

        composable(UnifiedRoute.ThermalSettings.route) {
            ThermalSettingsScreen(
                onBackClick = { navController.popBackStack() }
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
                onNavigateToRecordingSettings = { navController.navigate("recording_settings") },
                onNavigateToStorageSettings = { navController.navigate("storage_settings") },
                onNavigateToSyncSettings = { navController.navigate("sync_settings") },
                onNavigateToCalibration = { navController.navigate("calibration") },
                onNavigateToNetworkSettings = { navController.navigate(UnifiedRoute.NetworkConfig.route) },
                onNavigateToDiagnostics = { navController.navigate("diagnostics") },
                onNavigateToAppInfo = { navController.navigate("app_info") },
                onNavigateToPrivacyPolicy = { navController.navigate("privacy_policy") },
                onNavigateToHelp = { navController.navigate("help") }
            )
        }

        composable(UnifiedRoute.About.route) {
            NavigationPerformanceHelper.TrackNavigation("About")

            AboutScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(UnifiedRoute.Profile.route) {
            NavigationPerformanceHelper.TrackNavigation("Profile")

            mpdc4gsr.feature.settings.ui.ProfileScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToResearchTemplates = { navController.navigate(UnifiedRoute.ResearchTemplates.route) },
                onNavigateToPreferences = { navController.navigate(UnifiedRoute.Settings.route) },
                onExportData = { navController.navigate(UnifiedRoute.GSRDataView.route) }
            )
        }

        composable(UnifiedRoute.ComponentShowcase.route) {
            NavigationPerformanceHelper.TrackNavigation("ComponentShowcase")

            ComponentShowcaseScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(UnifiedRoute.TestingSuite.route) {
            NavigationPerformanceHelper.TrackNavigation("TestingSuite")

            TestResultsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // Network & Device Management Routes
        composable(UnifiedRoute.PermissionRequest.route) {
            LaunchedEffect(Unit) {
                try {
                    // Use class reference instead of hard-coded string
                    val activityClass = try {
                        mpdc4gsr.core.ui.PermissionRequestComposeActivity::class.java
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

        // Fallback routes for screens when activities fail to launch
        composable("dual_mode_camera_screen") {
            DualModeCameraScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(UnifiedRoute.CameraSettings.route) }
            )
        }

        composable("device_pairing_screen") {
            DevicePairingScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Additional Settings Routes
        composable("recording_settings") {
            mpdc4gsr.feature.settings.ui.RecordingSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("storage_settings") {
            mpdc4gsr.feature.settings.ui.StorageSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("sync_settings") {
            mpdc4gsr.feature.settings.ui.SyncSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("calibration") {
            mpdc4gsr.feature.thermal.ui.CalibrationScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(UnifiedRoute.NetworkConfig.route) {
            mpdc4gsr.feature.network.ui.NetworkSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("diagnostics") {
            mpdc4gsr.feature.main.ui.DiagnosticsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("app_info") {
            mpdc4gsr.feature.settings.ui.AppInfoScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("privacy_policy") {
            mpdc4gsr.feature.settings.ui.PrivacyPolicyScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("help") {
            mpdc4gsr.feature.settings.ui.HelpScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

/**
 * Navigation Helper Functions
 */
object NavigationHelper {
    fun navigateToGSRSettings(navController: NavHostController) {
        navController.navigate(UnifiedRoute.GSRSettings.route)
    }

    fun navigateToSessionDetail(navController: NavHostController, sessionId: String) {
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

    fun thermalRGBCapture(navController: NavHostController) {
        navController.navigate(UnifiedRoute.DualModeCamera.route)
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
        inclusive: Boolean = false
    ) {
        navController.navigate(destination) {
            popUpTo(popUpTo) {
                this.inclusive = inclusive
            }
        }
    }
}