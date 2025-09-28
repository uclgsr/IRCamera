package mpdc4gsr.compose.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * Navigation setup for Compose screens
 * Can coexist with existing Fragment-based navigation
 */
sealed class IRCameraScreen(val route: String) {
    object Main : IRCameraScreen("main")
    object ThermalCamera : IRCameraScreen("thermal_camera")
    object SensorDashboard : IRCameraScreen("sensor_dashboard")
    object Settings : IRCameraScreen("settings")
    object Recording : IRCameraScreen("recording")
}

@Composable
fun IRCameraNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = IRCameraScreen.Main.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(IRCameraScreen.Main.route) {
            // Main screen content - can be a hybrid with existing MainActivity
            MainScreenContent(
                onNavigateToThermal = {
                    navController.navigate(IRCameraScreen.ThermalCamera.route)
                },
                onNavigateToSensors = {
                    navController.navigate(IRCameraScreen.SensorDashboard.route)
                }
            )
        }
        
        composable(IRCameraScreen.ThermalCamera.route) {
            // Thermal camera screen - could embed existing ThermalFragment
            ThermalCameraScreenContent(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(IRCameraScreen.SensorDashboard.route) {
            // Sensor dashboard screen - could embed existing SensorDashboardFragment
            SensorDashboardScreenContent(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(IRCameraScreen.Settings.route) {
            // Settings screen
            SettingsScreenContent(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(IRCameraScreen.Recording.route) {
            // Recording control screen
            RecordingScreenContent(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

// Placeholder composables - to be implemented as screens are migrated
@Composable
private fun MainScreenContent(
    onNavigateToThermal: () -> Unit,
    onNavigateToSensors: () -> Unit
) {
    // Implementation can start as a simple wrapper around existing MainActivity content
    // Or gradually replace with pure Compose
}

@Composable
private fun ThermalCameraScreenContent(
    onNavigateBack: () -> Unit
) {
    // Can start by embedding existing ThermalFragment using FragmentContainer
}

@Composable
private fun SensorDashboardScreenContent(
    onNavigateBack: () -> Unit
) {
    // Can start by embedding existing SensorDashboardFragment
}

@Composable
private fun SettingsScreenContent(
    onNavigateBack: () -> Unit
) {
    // New Compose-first settings screen
}

@Composable
private fun RecordingScreenContent(
    onNavigateBack: () -> Unit
) {
    // New Compose-first recording controls
}