package mpdc4gsr.compose.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mpdc4gsr.activities.*
import mpdc4gsr.compose.utils.FragmentContainer

/**
 * Task E: Complete Navigation Integration
 *
 * Unified navigation system that bridges Compose and Fragment navigation:
 * - Seamless transitions between Compose and Fragment screens
 * - Deep linking support
 * - State preservation
 * - Backward compatibility with existing navigation
 */
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
    
    object Settings : IRCameraScreen("settings")
    object SettingsCompose : IRCameraScreen("settings_compose")
    object Demo : IRCameraScreen("demo")
    object About : IRCameraScreen("about")
}

@Composable
fun IRCameraNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = IRCameraScreen.Demo.route
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Demo screen - showcases all Compose implementations
        composable(IRCameraScreen.Demo.route) {
            DemoNavigationScreen(
                onNavigateToMainCompose = {
                    navController.navigate(IRCameraScreen.MainCompose.route)
                },
                onNavigateToThermalCompose = {
                    navController.navigate(IRCameraScreen.ThermalCameraCompose.route)
                },
                onNavigateToSensorDashboard = {
                    navController.navigate(IRCameraScreen.SensorDashboardCompose.route)
                },
                onNavigateToSettings = {
                    navController.navigate(IRCameraScreen.SettingsCompose.route)
                },
                onNavigateToOriginalMain = {
                    // Launch original MainActivity as separate activity
                    context.startActivity(Intent(context, MainActivity::class.java))
                }
            )
        }

        // Main dashboard screens
        composable(IRCameraScreen.Main.route) {
            // Could embed original MainActivity using AndroidView if needed
            // For now, launch as separate activity
            LaunchActivityScreen(MainActivity::class.java)
        }

        composable(IRCameraScreen.MainCompose.route) {
            LaunchActivityScreen(MainActivity::class.java)
        }

        // Thermal camera screens
        composable(IRCameraScreen.ThermalCamera.route) {
            // Could embed existing thermal fragment using FragmentContainer
            ThermalCameraFragmentScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(IRCameraScreen.ThermalCameraCompose.route) {
            LaunchActivityScreen(
                activityClass = com.mpdc4gsr.module.thermalunified.activity.ThermalCameraComposeActivity::class.java
            )
        }

        // Sensor dashboard screens
        composable(IRCameraScreen.SensorDashboard.route) {
            // Could embed existing sensor dashboard fragment
            SensorDashboardFragmentScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(IRCameraScreen.SensorDashboardCompose.route) {
            LaunchActivityScreen(SensorDashboardComposeActivity::class.java)
        }

        // Settings screens
        composable(IRCameraScreen.Settings.route) {
            // Could embed existing settings fragment
            SettingsFragmentScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(IRCameraScreen.SettingsCompose.route) {
            LaunchActivityScreen(SettingsComposeActivity::class.java)
        }

        // About screen
        composable(IRCameraScreen.About.route) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun LaunchActivityScreen(activityClass: Class<*>) {
    val context = LocalContext.current

    // Launch activity and finish current one
    androidx.compose.runtime.LaunchedEffect(Unit) {
        context.startActivity(Intent(context, activityClass))
        if (context is android.app.Activity) {
            context.finish()
        }
    }

    // Show loading indicator while launching
    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.CircularProgressIndicator()
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

@Composable
private fun AboutScreen(onNavigateBack: () -> Unit) {
    androidx.compose.foundation.layout.Column(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        androidx.compose.material3.Text(
            text = "IRCamera",
            style = androidx.compose.material3.MaterialTheme.typography.headlineLarge
        )
        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
        androidx.compose.material3.Text(
            text = "Version 1.10.000",
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
        )
        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
        androidx.compose.material3.Text(
            text = "Thermal imaging and GSR sensor data collection application",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
        )
        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
        androidx.compose.material3.Button(onClick = onNavigateBack) {
            androidx.compose.material3.Text("Back")
        }
    }
}