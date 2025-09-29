package com.mpdc4gsr.libunified.app.ktbase

import android.content.Context
import android.content.Intent
import android.view.WindowManager
import com.mpdc4gsr.libunified.app.tools.AppLanguageUtils
import com.mpdc4gsr.libunified.app.tools.ConstantLanguages

/**
 * BaseScreenActivity - Common lifecycle patterns for View-based activities (non-binding)
 * 
 * This activity consolidates the duplicate lifecycle patterns found in 7+ files:
 * - Screen-on management (onResume/onPause)
 * - Common navigation patterns
 * - Language context attachment
 * 
 * Reduces duplication in:
 * - IRLogMPChartActivity.kt
 * - LogMPChartActivity.kt
 * - MonitorChartActivity.kt
 * - IRMonitorChartActivity.kt
 * - And other non-binding activities
 */
abstract class BaseScreenActivity : BaseActivity() {

    /**
     * Override this to disable screen-on behavior for specific activities
     */
    protected open fun shouldKeepScreenOn(): Boolean = true

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
     * Language context attachment is already handled in BaseActivity
     * but kept here for clarity and consistency
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