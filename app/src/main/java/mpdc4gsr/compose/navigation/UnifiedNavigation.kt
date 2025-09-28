package mpdc4gsr.compose.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mpdc4gsr.activities.*
import mpdc4gsr.sensors.gsr.GSRSettingsComposeActivity
import mpdc4gsr.sensors.gsr.SessionDetailComposeActivity
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
    object GSRModernizationDemo : UnifiedRoute("gsr_modernization_demo")
    object GSRPlot : UnifiedRoute("gsr_plot/{sessionId}") {
        fun createRoute(sessionId: String) = "gsr_plot/$sessionId"
    }
    object GSRDataView : UnifiedRoute("gsr_data_view/{filePath}") {
        fun createRoute(filePath: String) = "gsr_data_view/$filePath"
    }
    object SessionDetail : UnifiedRoute("session_detail/{sessionId}") {
        fun createRoute(sessionId: String) = "session_detail/$sessionId"
    }
    
    // Camera Integration Routes
    object CameraDashboard : UnifiedRoute("camera_dashboard")
    object DualModeCamera : UnifiedRoute("dual_mode_camera")
    object CameraSettings : UnifiedRoute("camera_settings")
    
    // Thermal Camera Routes
    object ThermalCamera : UnifiedRoute("thermal_camera")
    object ThermalSettings : UnifiedRoute("thermal_settings")
    object ThermalGallery : UnifiedRoute("thermal_gallery")
    
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
        
        composable(UnifiedRoute.GSRModernizationDemo.route) {
            GSRModernizationDemoScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(UnifiedRoute.GSRSettings.route) },
                onNavigateToSessionDetail = { sessionId ->
                    navController.navigate(UnifiedRoute.SessionDetail.createRoute(sessionId))
                }
            )
        }
        
        composable(UnifiedRoute.SessionDetail.route) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: "unknown"
            SessionDetailScreen(
                sessionId = sessionId,
                onBackClick = { navController.popBackStack() },
                onNavigateToGSRPlot = { navController.navigate(UnifiedRoute.GSRPlot.createRoute(sessionId)) }
            )
        }
        
        composable(UnifiedRoute.GSRPlot.route) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: "unknown"
            GSRPlotScreen(
                sessionId = sessionId,
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable(UnifiedRoute.GSRDataView.route) { backStackEntry ->
            val filePath = backStackEntry.arguments?.getString("filePath") ?: ""
            GSRDataViewScreen(
                filePath = filePath,
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
            DualModeCameraScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(UnifiedRoute.CameraSettings.route) }
            )
        }
        
        // Thermal Camera Routes
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
                onNavigateToGSRDemo = { navController.navigate(UnifiedRoute.GSRModernizationDemo.route) },
                onNavigateToCameraDemo = { navController.navigate(UnifiedRoute.CameraDashboard.route) },
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
        navController.navigate(UnifiedRoute.SessionDetail.createRoute(sessionId))
    }
    
    fun navigateToCamera(navController: NavHostController) {
        navController.navigate(UnifiedRoute.CameraDashboard.route)
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