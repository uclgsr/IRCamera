package mpdc4gsr.feature.main.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import mpdc4gsr.core.ui.navigation.UnifiedNavHost

/**
 * Primary MainActivity implementation using Clean Architecture + MVVM + Compose
 *
 * This is the main entry point for the IRCamera application using the unified
 * navigation system and Compose UI framework.
 *
 * Architecture:
 * - UI Layer: Compose-based declarative UI
 * - Presentation Layer: ViewModels handle state and business logic
 * - Domain Layer: Use cases encapsulate business rules
 * - Data Layer: Repositories manage data sources
 *
 * This consolidates previous MainActivity variants (MainActivity, MainActivityAlternative,
 * MainActivityLegacy) into a single Clean Architecture implementation.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LibUnifiedTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UnifiedNavHost()
                }
            }
        }
    }
}