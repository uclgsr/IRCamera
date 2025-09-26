package com.mpdc4gsr.libunified.app.utils

import android.content.Context
import android.content.pm.PackageManager
import com.mpdc4gsr.libunified.app.BaseApplication

/**
 * Common utility functions used across the application
 */
object CommUtils {
    
    /**
     * Get the application name
     */
    fun getAppName(): String {
        return try {
            val context = BaseApplication.instance
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(context.packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            "IRCamera"
        }
    }
    
    /**
     * Get app version name
     */
    fun getAppVersionName(): String {
        return try {
            val context = BaseApplication.instance
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
    
    /**
     * Get app version code
     */
    fun getAppVersionCode(): Long {
        return try {
            val context = BaseApplication.instance
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            1L
        }
    }
}