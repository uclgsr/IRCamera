package mpdc4gsr.compose.navigation

import android.content.Intent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mpdc4gsr.compose.screens.*
import mpdc4gsr.compose.testing.TestResultsScreen

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
sealed class UnifiedRoute(val route: String) {
    // Main Application Routes
    object Home : UnifiedRoute("home")
    object Dashboard : UnifiedRoute("dashboard")

    // GSR Sensor Routes
    object GSRSettings : UnifiedRoute("gsr_settings")
    object GSRDemo : UnifiedRoute("gsr_demo")
    object GSRPlot : UnifiedRoute("gsr_plot/{sessionId}") {
        fun createRoute(sessionId: String) = "gsr_plot/$sessionId"
    }
    object GSRDataView : UnifiedRoute("gsr_data_view") {
        // File paths should be passed via arguments, not URL parameters
        fun createRoute() = "gsr_data_view"
    }
    object GSRSessionDetail : UnifiedRoute("gsr_session_detail/{sessionId}") {
        fun createRoute(sessionId: String) = "gsr_session_detail/$sessionId"
    }

    // Camera Integration Routes
    object CameraDashboard : UnifiedRoute("camera_dashboard")
    object DualModeCamera : UnifiedRoute("dual_mode_camera")
    object CameraSettings : UnifiedRoute("camera_settings")

    // Network Routes
    object DevicePairing : UnifiedRoute("device_pairing")
    object PermissionRequest : UnifiedRoute("permission_request")

    // Thermal Camera Routes (consistent naming)
    object ThermalMain : UnifiedRoute("thermal_main")
    object ThermalGallery : UnifiedRoute("thermal_gallery") 
    object ThermalReport : UnifiedRoute("thermal_report")
    object ThermalCamera : UnifiedRoute("thermal_camera")
    object ThermalSettings : UnifiedRoute("thermal_settings")

    // System Routes
    object Settings : UnifiedRoute("settings")
    object About : UnifiedRoute("about")
    object NetworkConfig : UnifiedRoute("network_config")

    // Development and Demo Routes
    object ModernizationProgress : UnifiedRoute("modernization_progress")
    object ComponentShowcase : UnifiedRoute("component_showcase")
    object TestingSuite : UnifiedRoute("testing_suite")
}

@Composable
fun UnifiedNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = UnifiedRoute.ModernizationProgress.route
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        }
    ) {
        // Home and Dashboard
        composable(UnifiedRoute.Home.route) {
            MainScreen(
                onNavigateToSensors = { navController.navigate(UnifiedRoute.Dashboard.route) },
                onNavigateToGallery = { navController.navigate(UnifiedRoute.ThermalGallery.route) },
                onNavigateToSettings = { navController.navigate(UnifiedRoute.Settings.route) }
            )
        }

        composable(UnifiedRoute.Dashboard.route) {
            UnifiedSensorDashboard(
                onBackClick = { navController.popBackStack() },
                onNavigateToGSRSettings = { navController.navigate(UnifiedRoute.GSRSettings.route) },
                onNavigateToCamera = { navController.navigate(UnifiedRoute.CameraDashboard.route) },
                onNavigateToThermal = { navController.navigate(UnifiedRoute.ThermalCamera.route) }
            )
        }

        // GSR Sensor Routes
        composable(UnifiedRoute.GSRSettings.route) {
            GSRSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(UnifiedRoute.GSRDemo.route) {
            GSRModernizationDemoScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(UnifiedRoute.GSRSettings.route) },
                onNavigateToSessionDetail = { sessionId ->
                    navController.navigate(UnifiedRoute.GSRSessionDetail.createRoute(sessionId))
                }
            )
        }

        composable(UnifiedRoute.GSRSessionDetail.route) { backStackEntry ->
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
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: "unknown"
            GSRPlotScreen(
                sessionId = sessionId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(UnifiedRoute.GSRDataView.route) {
            // File path should be passed via savedStateHandle or ViewModel
            GSRDataViewScreen(
                filePath = "", // Get from savedStateHandle or arguments
                onBackClick = { navController.popBackStack() }
            )
        }

        // Camera Integration Routes
        composable(UnifiedRoute.CameraDashboard.route) {
            CameraDashboardScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToDualMode = { navController.navigate(UnifiedRoute.DualModeCamera.route) },
                onNavigateToSettings = { navController.navigate(UnifiedRoute.CameraSettings.route) }
            )
        }

        composable(UnifiedRoute.DualModeCamera.route) {
            LaunchedEffect(Unit) {
                try {
                    context.startActivity(
                        Intent(
                            context,
                            Class.forName("mpdc4gsr.activities.DualModeCameraActivityCompose")
                        )
                    )
                } catch (e: Exception) {
                    // Fallback to screen
                    navController.navigate("dual_mode_camera_screen")
                }
            }
            ThermalLoadingScreen("Loading Dual Mode Camera...")
        }

        composable(UnifiedRoute.DevicePairing.route) {
            LaunchedEffect(Unit) {
                try {
                    context.startActivity(
                        Intent(
                            context,
                            Class.forName("mpdc4gsr.network.DevicePairingComposeActivity")
                        )
                    )
                } catch (e: Exception) {
                    // Fallback to activities package
                    try {
                        context.startActivity(
                            Intent(
                                context,
                                Class.forName("mpdc4gsr.activities.DevicePairingActivityCompose")
                            )
                        )
                    } catch (e2: Exception) {
                        // Final fallback - just show loading screen
                    }
                }
            }
            ThermalLoadingScreen("Loading Device Pairing...")
        }

        // Thermal Camera Routes
        composable(UnifiedRoute.ThermalMain.route) {
            LaunchedEffect(Unit) {
                try {
                    context.startActivity(
                        Intent(
                            context,
                            Class.forName("com.mpdc4gsr.module.thermalunified.activity.IRMainComposeActivity")
                        )
                    )
                } catch (e: Exception) {
                    // Fallback to placeholder
                }
            }
            ThermalLoadingScreen("Loading Thermal Main...")
        }

        composable(UnifiedRoute.ThermalGallery.route) {
            LaunchedEffect(Unit) {
                try {
                    context.startActivity(
                        Intent(
                            context,
                            Class.forName("com.mpdc4gsr.module.thermalunified.activity.ThermalGalleryComposeActivity")
                        )
                    )
                } catch (e: Exception) {
                    // Fallback to placeholder
                }
            }
            ThermalLoadingScreen("Loading Thermal Gallery...")
        }

        composable(UnifiedRoute.ThermalReport.route) {
            LaunchedEffect(Unit) {
                try {
                    context.startActivity(
                        Intent(
                            context,
                            Class.forName("com.mpdc4gsr.module.thermalunified.activity.ThermalReportComposeActivity")
                        )
                    )
                } catch (e: Exception) {
                    // Fallback to placeholder
                }
            }
            ThermalLoadingScreen("Loading Thermal Reports...")
        }

        composable(UnifiedRoute.ThermalCamera.route) {
            ThermalCameraScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(UnifiedRoute.ThermalSettings.route) },
                onNavigateToGallery = { navController.navigate(UnifiedRoute.ThermalGallery.route) }
            )
        }

        // Settings and System Routes
        composable(UnifiedRoute.Settings.route) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToNetworkConfig = { navController.navigate(UnifiedRoute.NetworkConfig.route) }
            )
        }

        composable(UnifiedRoute.About.route) {
            AboutScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // Development and Demo Routes
        composable(UnifiedRoute.ModernizationProgress.route) {
            ModernizationProgressScreen(
                onNavigateToGSRDemo = { navController.navigate(UnifiedRoute.GSRDemo.route) },
                onNavigateToCameraDemo = { navController.navigate(UnifiedRoute.CameraDashboard.route) },
                onNavigateToThermalDemo = { navController.navigate(UnifiedRoute.ThermalMain.route) },
                onNavigateToComponentShowcase = { navController.navigate(UnifiedRoute.ComponentShowcase.route) }
            )
        }

        composable(UnifiedRoute.ComponentShowcase.route) {
            ComponentShowcaseScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(UnifiedRoute.TestingSuite.route) {
            TestResultsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // Network & Device Management Routes
        composable(UnifiedRoute.PermissionRequest.route) {
            LaunchedEffect(Unit) {
                try {
                    context.startActivity(
                        Intent(
                            context,
                            Class.forName("mpdc4gsr.permissions.PermissionRequestComposeActivity")
                        )
                    )
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