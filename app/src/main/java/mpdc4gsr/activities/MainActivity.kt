package mpdc4gsr.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mpdc4gsr.libunified.app.compose.theme.LibTheme
import mpdc4gsr.compose.navigation.UnifiedNavHost

/**
 * Primary MainActivity implementation using Compose
 * 
 * This is the main entry point for the IRCamera application using the unified
 * navigation system and Compose UI framework.
 */
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            LibTheme {
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