package com.mpdc4gsr.libunified.app.compose.base

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import com.mpdc4gsr.libunified.app.tools.AppLanguageUtils
import com.mpdc4gsr.libunified.app.tools.ConstantLanguages
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme

/**
 * BaseScreenActivity - Common lifecycle patterns for screen management
 * 
 * This activity consolidates the duplicate lifecycle patterns found in 7+ files:
 * - Screen-on management (onResume/onPause)
 * - Common navigation patterns
 * - Language context attachment
 * 
 * Reduces duplication in:
 * - PdfActivity.kt
 * - IRLogMPChartActivity.kt  
 * - LogMPChartActivity.kt
 * - MonitorChartActivity.kt
 * - IRMonitorChartActivity.kt
 * - And 2+ other activities
 */
abstract class BaseScreenActivity : ComponentActivity() {

    /**
     * Override this to disable screen-on behavior for specific activities
     */
    protected open fun shouldKeepScreenOn(): Boolean = true

    /**
     * Subclasses implement their Compose content here
     */
    @Composable
    protected abstract fun ScreenContent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            LibUnifiedTheme {
                ScreenContent()
            }
        }
    }

    /**
     * Consolidated onResume pattern found in 7+ files
     */
    override fun onResume() {
        super.onResume()
        if (shouldKeepScreenOn()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    /**
     * Consolidated onPause pattern found in 7+ files
     */
    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /**
     * Consolidated language context attachment found in 4+ files
     */
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(
            AppLanguageUtils.attachBaseContext(
                newBase, 
                ConstantLanguages.ENGLISH
            )
        )
    }

    /**
     * Common navigation pattern found in 5+ files
     */
    protected fun navigateToMainActivity(pageIndex: Int = 0) {
        try {
            // Use reflection to find MainActivity to avoid direct dependency
            val mainActivityClass = Class.forName("mpdc4gsr.activities.MainActivity")
            val intent = Intent(this, mainActivityClass).apply {
                putExtra("page", pageIndex)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        } catch (e: ClassNotFoundException) {
            // Fallback - just finish this activity
            finish()
        }
    }

    /**
     * Helper method for common navigation pattern
     */
    protected fun navigateBack() {
        finish()
    }
}