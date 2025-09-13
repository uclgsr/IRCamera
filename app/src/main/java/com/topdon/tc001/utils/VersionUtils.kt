package com.topdon.tc001.utils

import android.content.Context

/**
 * Utility class for version-related operations.
 *
 * This object provides methods to retrieve version information from the application context.
 */
object VersionUtils {
    /**
     * Gets the version name from the application's package information.
     *
     * @param context The application context used to access package manager
     * @return The version name string, or empty string if not available
     */
    fun getCodeStr(context: Context): String {
        val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        return versionName ?: ""
    }
}
