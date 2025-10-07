// Merged .kt under 'core\ui\navigation' subtree
// Files: 4; Generated 2025-10-07 19:59:55


// ===== core\ui\navigation\IRCameraNavigation.kt =====

package mpdc4gsr.core.ui.navigation

import android.content.Intent
import android.util.Log
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mpdc4gsr.core.ui.ComposePerformanceMonitor
import mpdc4gsr.core.ui.model.SensorType
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.feature.camera.ui.*
import mpdc4gsr.feature.gsr.ui.*
import mpdc4gsr.feature.main.ui.ComponentShowcaseScreen
import mpdc4gsr.feature.main.ui.MainComposeActivity
import mpdc4gsr.feature.main.ui.MainScreen
import mpdc4gsr.feature.main.ui.UnifiedSensorDashboard
import mpdc4gsr.feature.network.ui.DevicePairingScreen
import mpdc4gsr.feature.settings.ui.AboutScreen
import mpdc4gsr.feature.settings.ui.ProfileEditScreen
import mpdc4gsr.feature.settings.ui.SettingsComposeActivity
import mpdc4gsr.feature.settings.ui.SettingsScreen
import mpdc4gsr.feature.testing.ui.TestResultsScreen
import mpdc4gsr.feature.thermal.ui.ThermalCameraScreen
import mpdc4gsr.feature.thermal.ui.ThermalLoadingScreen
import mpdc4gsr.feature.thermal.ui.ThermalSettingsScreen

sealed class IRCameraScreen(val route: String) {
    object Main : IRCameraScreen("main")
    object MainCompose : IRCameraScreen("main_compose")
    object MainFragment : IRCameraScreen("main_fragment")
    object MainFragmentCompose : IRCameraScreen("main_fragment_compose")
    object ThermalCamera : IRCameraScreen("thermal_camera")
    object ThermalCameraCompose : IRCameraScreen("thermal_camera_compose")
    object ThermalFragment : IRCameraScreen("thermal_fragment")
    object ThermalFragmentCompose : IRCameraScreen("thermal_fragment_compose")
    object SensorDashboard : IRCameraScreen("sensor_dashboard")
    object SensorDashboardCompose : IRCameraScreen("sensor_dashboard_compose")
    object SensorDashboardFragment : IRCameraScreen("sensor_dashboard_fragment")
    object SensorDashboardFragmentCompose : IRCameraScreen("sensor_dashboard_fragment_compose")
    object Gallery : IRCameraScreen("gallery")
    object GalleryFragment : IRCameraScreen("gallery_fragment")
    object GalleryFragmentCompose : IRCameraScreen("gallery_fragment_compose")

    // Priority 3: Specialized Thermal Fragments
    object IRCorrectionFragment : IRCameraScreen("ir_correction_fragment")
    object IRCorrectionFragmentCompose : IRCameraScreen("ir_correction_fragment_compose")
    object MonitorThermalFragment : IRCameraScreen("monitor_thermal_fragment")
    object MonitorThermalFragmentCompose : IRCameraScreen("monitor_thermal_fragment_compose")

    // Priority 4: Additional Gallery and Lite Fragments
    object IRGalleryTabFragmentCompose : IRCameraScreen("ir_gallery_tab_fragment_compose")
    object GalleryPictureFragment : IRCameraScreen("gallery_picture_fragment")
    object GalleryPictureFragmentCompose : IRCameraScreen("gallery_picture_fragment_compose")
    object IRPlushFragment : IRCameraScreen("ir_plush_fragment")
    object IRPlushFragmentCompose : IRCameraScreen("ir_plush_fragment_compose")
    object IRMonitorLiteFragment : IRCameraScreen("ir_monitor_lite_fragment")
    object IRMonitorLiteFragmentCompose : IRCameraScreen("ir_monitor_lite_fragment_compose")

    // Priority 5: Final Specialized Fragments
    object GalleryVideoFragment : IRCameraScreen("gallery_video_fragment")
    object GalleryVideoFragmentCompose : IRCameraScreen("gallery_video_fragment_compose")
    object PDFListFragmentCompose : IRCameraScreen("pdf_list_fragment_compose")
    object IRMonitorCaptureFragment : IRCameraScreen("ir_monitor_capture_fragment")
    object IRMonitorCaptureFragmentCompose : IRCameraScreen("ir_monitor_capture_fragment_compose")
    object IRMonitorHistoryFragment : IRCameraScreen("ir_monitor_history_fragment")
    object IRMonitorHistoryFragmentCompose : IRCameraScreen("ir_monitor_history_fragment_compose")
    object IRMonitorThermalFragment : IRCameraScreen("ir_monitor_thermal_fragment")
    object IRMonitorThermalFragmentCompose : IRCameraScreen("ir_monitor_thermal_fragment_compose")
    object Settings : IRCameraScreen("settings")
    object SettingsCompose : IRCameraScreen("settings_compose")
    object About : IRCameraScreen("about")
}

@Composable
fun IRCameraNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = IRCameraScreen.Main.route
) {
    val context = LocalContext.current
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Main dashboard screens
        composable(IRCameraScreen.Main.route) {
            // Launch main activity
            LaunchedEffect(Unit) {
                context.startActivity(Intent(context, MainComposeActivity::class.java))
            }
            LoadingScreen()
        }
        composable(IRCameraScreen.MainCompose.route) {
            // Launch MainComposeActivity
            LaunchedEffect(Unit) {
                context.startActivity(Intent(context, MainComposeActivity::class.java))
            }
            LoadingScreen()
        }
        // Thermal camera screens
        composable(IRCameraScreen.ThermalCamera.route) {
            // Could embed existing thermal fragment using FragmentContainer
            ThermalCameraFragmentScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(IRCameraScreen.ThermalCameraCompose.route) {
            // Try to launch thermal activity or show placeholder
            LaunchedEffect(Unit) {
                try {
                    val intent = Intent().apply {
                        setClassName(
                            context,
                            "com.mpdc4gsr.module.thermalunified.activity.ThermalCameraComposeActivity"
                        )
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback - stay in compose
                }
            }
            LoadingScreen()
        }
        // Sensor dashboard screens
        composable(IRCameraScreen.SensorDashboard.route) {
            // Could embed existing sensor dashboard fragment
            SensorDashboardFragmentScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(IRCameraScreen.SensorDashboardCompose.route) {
            LaunchedEffect(Unit) {
                try {
                    context.startActivity(Intent(context, SensorDashboardComposeActivity::class.java))
                } catch (e: Exception) {
                    // Stay in compose if activity doesn't exist
                }
            }
            LoadingScreen()
        }
        // Settings screens
        composable(IRCameraScreen.Settings.route) {
            // Could embed existing settings fragment
            SettingsFragmentScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(IRCameraScreen.SettingsCompose.route) {
            LaunchedEffect(Unit) {
                try {
                    context.startActivity(Intent(context, SettingsComposeActivity::class.java))
                } catch (e: Exception) {
                    // Stay in compose if activity doesn't exist
                }
            }
            LoadingScreen()
        }
        // About screen
        composable(IRCameraScreen.About.route) {
            AboutScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ThermalCameraFragmentScreen(onNavigateBack: () -> Unit) {
    // This would embed the existing ThermalFragment using FragmentContainer
    // For now, show a placeholder that explains the integration
    androidx.compose.foundation.layout.Column(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        androidx.compose.material3.Text(
            text = "Thermal Camera Fragment Integration",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
        )
        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
        androidx.compose.material3.Text(
            text = "This screen would embed the existing ThermalFragment using FragmentContainer",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
        )
        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
        androidx.compose.material3.Button(onClick = onNavigateBack) {
            androidx.compose.material3.Text("Back")
        }
    }
}

@Composable
private fun SensorDashboardFragmentScreen(onNavigateBack: () -> Unit) {
    // This would embed the existing SensorDashboardFragment using FragmentContainer
    androidx.compose.foundation.layout.Column(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        androidx.compose.material3.Text(
            text = "Sensor Dashboard Fragment Integration",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
        )
        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
        androidx.compose.material3.Text(
            text = "This screen would embed the existing SensorDashboardFragment",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
        )
        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
        androidx.compose.material3.Button(onClick = onNavigateBack) {
            androidx.compose.material3.Text("Back")
        }
    }
}

@Composable
private fun SettingsFragmentScreen(onNavigateBack: () -> Unit) {
    // This would embed existing settings fragments
    androidx.compose.foundation.layout.Column(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        androidx.compose.material3.Text(
            text = "Settings Fragment Integration",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
        )
        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
        androidx.compose.material3.Text(
            text = "This screen would embed existing settings fragments",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
        )
        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
        androidx.compose.material3.Button(onClick = onNavigateBack) {
            androidx.compose.material3.Text("Back")
        }
    }
}


// ===== core\ui\navigation\NavigationAnimations.kt =====

package mpdc4gsr.core.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween

object NavigationAnimations {
    private const val ANIMATION_DURATION_MS = 300
    private const val FAST_ANIMATION_DURATION_MS = 200
    private fun <S> AnimatedContentTransitionScope<S>.slideTransition(
        direction: AnimatedContentTransitionScope.SlideDirection,
        duration: Int,
        isEnter: Boolean
    ): Any = if (isEnter) {
        slideIntoContainer(
            towards = direction,
            animationSpec = tween(duration)
        )
    } else {
        slideOutOfContainer(
            towards = direction,
            animationSpec = tween(duration)
        )
    }

    fun <S> AnimatedContentTransitionScope<S>.slideInFromRight(): EnterTransition =
        slideTransition(
            AnimatedContentTransitionScope.SlideDirection.Left,
            ANIMATION_DURATION_MS,
            true
        ) as EnterTransition

    fun <S> AnimatedContentTransitionScope<S>.slideOutToLeft(): ExitTransition =
        slideTransition(
            AnimatedContentTransitionScope.SlideDirection.Left,
            ANIMATION_DURATION_MS,
            false
        ) as ExitTransition

    fun <S> AnimatedContentTransitionScope<S>.slideInFromLeft(): EnterTransition =
        slideTransition(
            AnimatedContentTransitionScope.SlideDirection.Right,
            ANIMATION_DURATION_MS,
            true
        ) as EnterTransition

    fun <S> AnimatedContentTransitionScope<S>.slideOutToRight(): ExitTransition =
        slideTransition(
            AnimatedContentTransitionScope.SlideDirection.Right,
            ANIMATION_DURATION_MS,
            false
        ) as ExitTransition

    fun <S> AnimatedContentTransitionScope<S>.fastSlideInFromRight(): EnterTransition =
        slideTransition(
            AnimatedContentTransitionScope.SlideDirection.Left,
            FAST_ANIMATION_DURATION_MS,
            true
        ) as EnterTransition

    fun <S> AnimatedContentTransitionScope<S>.fastSlideOutToLeft(): ExitTransition =
        slideTransition(
            AnimatedContentTransitionScope.SlideDirection.Left,
            FAST_ANIMATION_DURATION_MS,
            false
        ) as ExitTransition

    fun <S> AnimatedContentTransitionScope<S>.fastSlideInFromLeft(): EnterTransition =
        slideTransition(
            AnimatedContentTransitionScope.SlideDirection.Right,
            FAST_ANIMATION_DURATION_MS,
            true
        ) as EnterTransition

    fun <S> AnimatedContentTransitionScope<S>.fastSlideOutToRight(): ExitTransition =
        slideTransition(
            AnimatedContentTransitionScope.SlideDirection.Right,
            FAST_ANIMATION_DURATION_MS,
            false
        ) as ExitTransition
}


// ===== core\ui\navigation\NavigationPerformanceHelper.kt =====

package mpdc4gsr.core.ui.navigation

import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import mpdc4gsr.core.ui.ComposePerformanceMonitor

object NavigationPerformanceHelper {
    private const val TAG = "NavigationPerf"
    private const val WARNING_THRESHOLD_MS = 300L

    @Composable
    fun TrackNavigation(routeName: String) {
        val startTime = remember { System.currentTimeMillis() }
        LaunchedEffect(Unit) {
            val latency = System.currentTimeMillis() - startTime
            ComposePerformanceMonitor.trackNavigation(routeName, startTime)
            if (latency > WARNING_THRESHOLD_MS) {
                AppLogger.w(TAG, "Slow navigation to $routeName: ${latency}ms (threshold: ${WARNING_THRESHOLD_MS}ms)")
            }
        }
    }

    fun logPerformanceSummary() {
        val summary = ComposePerformanceMonitor.getPerformanceSummary()
        AppLogger.d(TAG, "=== Navigation Performance Summary ===")
        val navigationMetrics = summary.filter { it.key.startsWith("navigation_") }
        if (navigationMetrics.isEmpty()) {
            AppLogger.d(TAG, "No navigation metrics recorded yet")
            return
        }
        navigationMetrics.forEach { (route, metric) ->
            val routeName = route.removePrefix("navigation_")
            Log.d(
                TAG, "$routeName: avg=${String.format("%.1f", metric.average)}ms, " +
                        "max=${metric.max}ms, min=${metric.min}ms, count=${metric.count}"
            )
        }
        val slowRoutes = navigationMetrics.filter { it.value.average > WARNING_THRESHOLD_MS }
        if (slowRoutes.isNotEmpty()) {
            AppLogger.w(TAG, "=== Routes Exceeding Threshold (${WARNING_THRESHOLD_MS}ms) ===")
            slowRoutes.forEach { (route, metric) ->
                val routeName = route.removePrefix("navigation_")
                AppLogger.w(TAG, "$routeName: avg=${String.format("%.1f", metric.average)}ms")
            }
        }
        AppLogger.d(TAG, "======================================")
    }

    fun getSlowRoutes(thresholdMs: Long = WARNING_THRESHOLD_MS): List<Pair<String, Double>> {
        val summary = ComposePerformanceMonitor.getPerformanceSummary()
        val navigationMetrics = summary.filter { it.key.startsWith("navigation_") }
        return navigationMetrics
            .filter { it.value.average > thresholdMs }
            .map {
                it.key.removePrefix("navigation_") to it.value.average
            }
            .sortedByDescending { it.second }
    }

    fun getFastestRoute(): Pair<String, Double>? {
        val summary = ComposePerformanceMonitor.getPerformanceSummary()
        val navigationMetrics = summary.filter { it.key.startsWith("navigation_") }
        return navigationMetrics
            .minByOrNull { it.value.average }
            ?.let { it.key.removePrefix("navigation_") to it.value.average }
    }

    fun getSlowestRoute(): Pair<String, Double>? {
        val summary = ComposePerformanceMonitor.getPerformanceSummary()
        val navigationMetrics = summary.filter { it.key.startsWith("navigation_") }
        return navigationMetrics
            .maxByOrNull { it.value.average }
            ?.let { it.key.removePrefix("navigation_") to it.value.average }
    }
}


// ===== core\ui\navigation\UnifiedNavigation.kt =====

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
import mpdc4gsr.feature.camera.ui.TimeLapseCameraScreen
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
import mpdc4gsr.feature.settings.ui.ProfileEditScreen
import mpdc4gsr.feature.settings.ui.SettingsScreen
import mpdc4gsr.feature.testing.ui.TestResultsScreen
import mpdc4gsr.feature.thermal.ui.ThermalCameraScreen
import mpdc4gsr.feature.thermal.ui.ThermalLoadingScreen
import mpdc4gsr.feature.thermal.ui.ThermalSettingsScreen

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
                onNavigateBack = { navController.popBackStack() }
            )
        }
        // Camera Integration Routes
        composable(UnifiedRoute.CameraDashboard.route) {
            CameraDashboardScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToDualMode = { navController.navigate(UnifiedRoute.DualModeCamera.route) },
                onNavigateToSettings = { navController.navigate(UnifiedRoute.CameraSettings.route) },
                onNavigateToSingleCamera = { navController.navigate(UnifiedRoute.RGBCamera.route) },
                onNavigateToGallery = { navController.navigate(UnifiedRoute.ThermalGallery.route) },
                onNavigateToTimeLapse = { navController.navigate(UnifiedRoute.TimeLapseCamera.route) }
            )
        }
        composable(UnifiedRoute.RGBCamera.route) {
            RGBCameraScreen(
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { navController.navigate(UnifiedRoute.CameraSettings.route) },
                onCapturePhoto = { }
            )
        }
        composable(UnifiedRoute.TimeLapseCamera.route) {
            NavigationPerformanceHelper.TrackNavigation("TimeLapseCamera")
            TimeLapseCameraScreen(
                onBackClick = { navController.popBackStack() }
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
                onNavigateToRecordingSettings = { navController.navigate(UnifiedRoute.RecordingSettings.route) },
                onNavigateToStorageSettings = { navController.navigate(UnifiedRoute.StorageSettings.route) },
                onNavigateToSyncSettings = { navController.navigate(UnifiedRoute.SyncSettings.route) },
                onNavigateToCalibration = { navController.navigate(UnifiedRoute.Calibration.route) },
                onNavigateToNetworkSettings = { navController.navigate(UnifiedRoute.NetworkConfig.route) },
                onNavigateToDiagnostics = { navController.navigate(UnifiedRoute.Diagnostics.route) },
                onNavigateToAppInfo = { navController.navigate(UnifiedRoute.AppInfo.route) },
                onNavigateToPrivacyPolicy = { navController.navigate(UnifiedRoute.PrivacyPolicy.route) },
                onNavigateToHelp = { navController.navigate(UnifiedRoute.Help.route) }
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
                onExportData = { navController.navigate(UnifiedRoute.GSRDataView.route) },
                onNavigateToEditProfile = { navController.navigate(UnifiedRoute.ProfileEdit.route) }
            )
        }
        composable(UnifiedRoute.ProfileEdit.route) {
            NavigationPerformanceHelper.TrackNavigation("ProfileEdit")
            ProfileEditScreen(
                onBackClick = { navController.popBackStack() },
                onSave = { profileData ->
                    // Profile data saved - navigate back
                    navController.popBackStack()
                }
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
        composable(UnifiedRoute.RecordingSettings.route) {
            mpdc4gsr.feature.settings.ui.RecordingSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(UnifiedRoute.StorageSettings.route) {
            mpdc4gsr.feature.settings.ui.StorageSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(UnifiedRoute.SyncSettings.route) {
            mpdc4gsr.feature.settings.ui.SyncSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(UnifiedRoute.Calibration.route) {
            mpdc4gsr.feature.thermal.ui.CalibrationScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(UnifiedRoute.NetworkConfig.route) {
            mpdc4gsr.feature.network.ui.NetworkSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(UnifiedRoute.Diagnostics.route) {
            mpdc4gsr.feature.main.ui.DiagnosticsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(UnifiedRoute.AppInfo.route) {
            mpdc4gsr.feature.settings.ui.AppInfoScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(UnifiedRoute.PrivacyPolicy.route) {
            mpdc4gsr.feature.settings.ui.PrivacyPolicyScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(UnifiedRoute.Help.route) {
            mpdc4gsr.feature.settings.ui.HelpScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

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


