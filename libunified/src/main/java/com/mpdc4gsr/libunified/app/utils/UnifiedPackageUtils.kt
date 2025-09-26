package com.mpdc4gsr.libunified.app.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.util.*

/**
 * Unified Package Utilities
 *
 * Consolidates package management utilities from across the repository:
 * - Package information retrieval and validation
 * - Version comparison and management
 * - Application metadata handling
 * - System compatibility checks
 *
 * This utility replaces scattered package utilities from:
 * - BleModule package utilities
 * - libapp package management
 * - app module version utilities
 */
object UnifiedPackageUtils {

    /**
     * Get detailed package information for the current application
     */
    fun getPackageInfo(context: Context): PackageInfo? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * Get application version name
     */
    fun getVersionName(context: Context): String {
        return getPackageInfo(context)?.versionName ?: "Unknown"
    }

    /**
     * Get application version code
     */
    fun getVersionCode(context: Context): Long {
        val packageInfo = getPackageInfo(context) ?: return 0L
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    }

    /**
     * Compare two version strings
     * Returns: -1 if version1 < version2, 0 if equal, 1 if version1 > version2
     */
    fun compareVersions(version1: String, version2: String): Int {
        val v1Parts = version1.split(".").map { it.toIntOrNull() ?: 0 }
        val v2Parts = version2.split(".").map { it.toIntOrNull() ?: 0 }

        val maxLength = maxOf(v1Parts.size, v2Parts.size)

        for (i in 0 until maxLength) {
            val v1Part = v1Parts.getOrNull(i) ?: 0
            val v2Part = v2Parts.getOrNull(i) ?: 0

            when {
                v1Part < v2Part -> return -1
                v1Part > v2Part -> return 1
            }
        }

        return 0
    }

    /**
     * Check if a version meets minimum requirements
     */
    fun isVersionAtLeast(currentVersion: String, minimumVersion: String): Boolean {
        return compareVersions(currentVersion, minimumVersion) >= 0
    }

    /**
     * Get application label/name
     */
    fun getApplicationLabel(context: Context): String {
        return try {
            val applicationInfo = context.applicationInfo
            context.packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            "Unknown App"
        }
    }

    /**
     * Check if application is debuggable
     */
    fun isDebuggable(context: Context): Boolean {
        return try {
            val applicationInfo = context.applicationInfo
            (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get build configuration information
     */
    data class BuildInfo(
        val versionName: String,
        val versionCode: Long,
        val packageName: String,
        val isDebuggable: Boolean,
        val targetSdk: Int,
        val minSdk: Int,
        val buildTime: Long = System.currentTimeMillis()
    )

    /**
     * Get comprehensive build information
     */
    fun getBuildInfo(context: Context): BuildInfo {
        val packageInfo = getPackageInfo(context)
        return BuildInfo(
            versionName = getVersionName(context),
            versionCode = getVersionCode(context),
            packageName = context.packageName,
            isDebuggable = isDebuggable(context),
            targetSdk = packageInfo?.applicationInfo?.targetSdkVersion ?: 0,
            minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                packageInfo?.applicationInfo?.minSdkVersion ?: 0
            } else {
                0
            }
        )
    }

    /**
     * Format version information for display
     */
    fun formatVersionInfo(context: Context): String {
        val buildInfo = getBuildInfo(context)
        return buildString {
            append("${buildInfo.versionName} (${buildInfo.versionCode})")
            if (buildInfo.isDebuggable) {
                append(" [DEBUG]")
            }
            append("\nTarget SDK: ${buildInfo.targetSdk}")
            if (buildInfo.minSdk > 0) {
                append(", Min SDK: ${buildInfo.minSdk}")
            }
        }
    }

    /**
     * Validate package name format
     */
    fun isValidPackageName(packageName: String): Boolean {
        if (packageName.isEmpty()) return false

        val parts = packageName.split(".")
        if (parts.size < 2) return false

        return parts.all { part ->
            part.isNotEmpty() &&
                    part.first().isLetter() &&
                    part.all { it.isLetterOrDigit() || it == '_' }
        }
    }

    /**
     * Get installed package names
     */
    fun getInstalledPackages(context: Context): List<String> {
        return try {
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getInstalledPackages(
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstalledPackages(0)
            }
            packages.map { it.packageName }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Check if a specific package is installed
     */
    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}