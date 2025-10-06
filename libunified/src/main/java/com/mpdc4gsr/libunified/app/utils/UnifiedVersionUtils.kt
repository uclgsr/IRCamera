package com.mpdc4gsr.libunified.app.utils
import android.content.Context
import android.text.TextUtils
object UnifiedVersionUtils {
    fun getVersionName(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    fun getVersionCode(context: Context): Long {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.longVersionCode
        } catch (e: Exception) {
            0L
        }
    }
    fun compareVersions(serverVersion: String, currentVersion: String): Boolean {
        if (TextUtils.isEmpty(serverVersion) || TextUtils.isEmpty(currentVersion)) {
            return false
        }
        val v1Parts = serverVersion.split(".").map { it.toIntOrNull() ?: 0 }
        val v2Parts = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLength = maxOf(v1Parts.size, v2Parts.size)
        for (i in 0 until maxLength) {
            val v1Part = v1Parts.getOrElse(i) { 0 }
            val v2Part = v2Parts.getOrElse(i) { 0 }
            when {
                v1Part > v2Part -> return true
                v1Part < v2Part -> return false
                // Continue if equal
            }
        }
        return false // Versions are equal
    }
    fun isUpdateNeeded(context: Context, serverVersion: String): Boolean {
        val currentVersion = getVersionName(context)
        return compareVersions(serverVersion, currentVersion)
    }
}