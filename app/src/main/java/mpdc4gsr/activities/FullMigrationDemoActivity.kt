package mpdc4gsr.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import mpdc4gsr.compose.navigation.IRCameraNavHost
import mpdc4gsr.compose.theme.IRCameraTheme

/**
 * Full Migration Demo Activity
 * 
 * This activity demonstrates the complete Jetpack Compose migration with all tasks completed:
 * - Task A: Main Dashboard Migration ✅
 * - Task B: Thermal Camera Enhancement ✅ 
 * - Task C: Sensor Dashboard Modernization ✅
 * - Task D: Settings Migration ✅
 * - Task E: Navigation Integration ✅
 * 
 * This is the entry point to showcase the entire migrated application.
 */
class FullMigrationDemoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            IRCameraTheme {
                CompleteMigrationApp()
            }
        }
    }

    @Composable
    private fun CompleteMigrationApp() {
        val navController = rememberNavController()
        
        IRCameraNavHost(
            navController = navController,
            startDestination = "demo" // Start with demo screen showing all completed tasks
        )
    }
}