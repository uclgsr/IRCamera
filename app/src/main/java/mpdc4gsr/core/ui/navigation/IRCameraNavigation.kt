package mpdc4gsr.core.ui.navigation

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mpdc4gsr.feature.gsr.ui.SensorDashboardComposeActivity
import mpdc4gsr.feature.main.ui.MainComposeActivity
import mpdc4gsr.feature.settings.ui.AboutScreen
import mpdc4gsr.feature.settings.ui.SettingsComposeActivity

sealed class IRCameraScreen(
    val route: String,
) {
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
    startDestination: String = IRCameraScreen.Main.route,
) {
    val context = LocalContext.current
    NavHost(
        navController = navController,
        startDestination = startDestination,
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
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(IRCameraScreen.ThermalCameraCompose.route) {
            // Try to launch thermal activity or show placeholder
            LaunchedEffect(Unit) {
                try {
                    val intent =
                        Intent().apply {
                            setClassName(
                                context,
                                "com.mpdc4gsr.module.thermalunified.activity.ThermalCameraComposeActivity",
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
                onNavigateBack = { navController.popBackStack() },
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
                onNavigateBack = { navController.popBackStack() },
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
                onBackClick = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ThermalCameraFragmentScreen(onNavigateBack: () -> Unit) {
    // This would embed the existing ThermalFragment using FragmentContainer
    // For now, show a placeholder that explains the integration
    androidx.compose.foundation.layout.Column(
        modifier =
            androidx.compose.ui.Modifier
                .fillMaxSize()
                .padding(16.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        androidx.compose.material3.Text(
            text = "Thermal Camera Fragment Integration",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
        )
        androidx.compose.foundation.layout
            .Spacer(
                modifier =
                    androidx.compose.ui.Modifier
                        .height(16.dp),
            )
        androidx.compose.material3.Text(
            text = "This screen would embed the existing ThermalFragment using FragmentContainer",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
        )
        androidx.compose.foundation.layout
            .Spacer(
                modifier =
                    androidx.compose.ui.Modifier
                        .height(16.dp),
            )
        androidx.compose.material3.Button(onClick = onNavigateBack) {
            androidx.compose.material3.Text("Back")
        }
    }
}

@Composable
private fun SensorDashboardFragmentScreen(onNavigateBack: () -> Unit) {
    // This would embed the existing SensorDashboardFragment using FragmentContainer
    androidx.compose.foundation.layout.Column(
        modifier =
            androidx.compose.ui.Modifier
                .fillMaxSize()
                .padding(16.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        androidx.compose.material3.Text(
            text = "Sensor Dashboard Fragment Integration",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
        )
        androidx.compose.foundation.layout
            .Spacer(
                modifier =
                    androidx.compose.ui.Modifier
                        .height(16.dp),
            )
        androidx.compose.material3.Text(
            text = "This screen would embed the existing SensorDashboardFragment",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
        )
        androidx.compose.foundation.layout
            .Spacer(
                modifier =
                    androidx.compose.ui.Modifier
                        .height(16.dp),
            )
        androidx.compose.material3.Button(onClick = onNavigateBack) {
            androidx.compose.material3.Text("Back")
        }
    }
}

@Composable
private fun SettingsFragmentScreen(onNavigateBack: () -> Unit) {
    // This would embed existing settings fragments
    androidx.compose.foundation.layout.Column(
        modifier =
            androidx.compose.ui.Modifier
                .fillMaxSize()
                .padding(16.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        androidx.compose.material3.Text(
            text = "Settings Fragment Integration",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
        )
        androidx.compose.foundation.layout
            .Spacer(
                modifier =
                    androidx.compose.ui.Modifier
                        .height(16.dp),
            )
        androidx.compose.material3.Text(
            text = "This screen would embed existing settings fragments",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
        )
        androidx.compose.foundation.layout
            .Spacer(
                modifier =
                    androidx.compose.ui.Modifier
                        .height(16.dp),
            )
        androidx.compose.material3.Button(onClick = onNavigateBack) {
            androidx.compose.material3.Text("Back")
        }
    }
}
