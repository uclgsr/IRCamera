package mpdc4gsr.utils

import android.content.Context
import com.mpdc4gsr.libunified.app.utils.UnifiedVersionUtils

/**
 * Version utilities for the app module
 * Delegates to UnifiedVersionUtils for consistency
 */
object VersionUtils {
    
    /**
     * Get version code as string 
     */
    fun getCodeStr(context: Context): String {
        return UnifiedVersionUtils.getVersionName(context)
    }
    
    /**
     * Get version name
     */
    fun getVersionName(context: Context): String {
        return UnifiedVersionUtils.getVersionName(context)
    }
    
    /**
     * Get version code
     */
    fun getVersionCode(context: Context): Long {
        return UnifiedVersionUtils.getVersionCode(context)
    }
    
    /**
     * Check if update is needed
     */
    fun isUpdateNeeded(context: Context, serverVersion: String): Boolean {
        return UnifiedVersionUtils.isUpdateNeeded(context, serverVersion)
    }
}