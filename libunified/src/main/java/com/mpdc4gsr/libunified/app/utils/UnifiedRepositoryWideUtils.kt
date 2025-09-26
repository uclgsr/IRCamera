package com.mpdc4gsr.libunified.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import java.util.*

/**
 * REPOSITORY-WIDE consolidated utilities covering ALL modules across the ENTIRE repository
 *
 * This is the MASTER utility consolidation that replaces utilities from:
 * - BleModule/src/main/java/com/topdon/commons/util/* (ALL utility classes)
 * - BleModule/src/main/java/com/topdon/ble/util/* (ALL utility classes)
 * - component/gsr-recording/src/main/java/com/mpdc4gsr/gsr/util/* (ALL utilities)
 * - component/thermalunified/src/main/java/com/mpdc4gsr/module/thermalunified/utils/* (ALL utilities)
 * - component/user/src/main/java/com/mpdc4gsr/module/user/util/* (ALL utilities)
 * - libapp/src/main/java/com/mpdc4gsr/lib/util/* (ALL utilities)
 * - app/src/main/java/mpdc4gsr/utils/* (ALL remaining utilities)
 * - libunified scattered utility classes across ALL subdirectories
 * - ALL other utility classes across the ENTIRE repository
*/
object UnifiedRepositoryWideUtils {

private const val TAG = "UnifiedRepositoryWide"

// ==================== STRING UTILITIES (REPOSITORY-WIDE) ====================

/**
 * Generate UUID without dashes (consolidates all UUID generation across repository)
*/
fun randomUuid(): String {
return UUID.randomUUID().toString().replace("-", "")
}

/**
 * Fill string with zeros (used across multiple modules)
*/
fun fillZero(src: String?, targetLen: Int, head: Boolean): String {
if (src == null) return ""
if (src.length >= targetLen) return src

val zeros = "0".repeat(targetLen - src.length)
return if (head) zeros + src else src + zeros
}

/**
 * Check if string is empty or null (repository-wide implementation)
*/
fun isEmpty(str: String?): Boolean {
return str.isNullOrEmpty() || str.trim().isEmpty()
}

/**
 * Check if string is not empty (repository-wide implementation)
*/
fun isNotEmpty(str: String?): Boolean = !isEmpty(str)

/**
 * Safe string conversion (repository-wide implementation)
*/
fun safeString(obj: Any?): String {
return obj?.toString() ?: ""
}

// ==================== PREFERENCES UTILITIES (REPOSITORY-WIDE) ====================

/**
 * Repository-wide preferences management
*/
object Preferences {
private const val DEFAULT_PREFS = "repository_wide_prefs"

fun putString(context: Context, key: String, value: String, prefsName: String = DEFAULT_PREFS) {
context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
.edit()
.putString(key, value)
.apply()
}

fun getString(context: Context, key: String, defaultValue: String = "", prefsName: String = DEFAULT_PREFS): String {
return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
.getString(key, defaultValue) ?: defaultValue
}

fun putInt(context: Context, key: String, value: Int, prefsName: String = DEFAULT_PREFS) {
context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
.edit()
.putInt(key, value)
.apply()
}

fun getInt(context: Context, key: String, defaultValue: Int = 0, prefsName: String = DEFAULT_PREFS): Int {
return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
.getInt(key, defaultValue)
}

fun putBoolean(context: Context, key: String, value: Boolean, prefsName: String = DEFAULT_PREFS) {
context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
.edit()
.putBoolean(key, value)
.apply()
}

fun getBoolean(context: Context, key: String, defaultValue: Boolean = false, prefsName: String = DEFAULT_PREFS): Boolean {
return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
.getBoolean(key, defaultValue)
}
}

// ==================== DEVICE UTILITIES (REPOSITORY-WIDE) ====================

/**
 * Get device model information (used across multiple modules)
*/
fun getDeviceModel(): String = Build.MODEL

/**
 * Get device manufacturer (used across multiple modules)
*/
fun getDeviceManufacturer(): String = Build.MANUFACTURER

/**
 * Get Android version (used across multiple modules)
*/
fun getAndroidVersion(): String = Build.VERSION.RELEASE

/**
 * Get device hardware information (used across multiple modules)
*/
fun getDeviceHardware(): String = Build.HARDWARE

/**
 * Get device brand (used across multiple modules)
*/
fun getDeviceBrand(): String = Build.BRAND

/**
 * Check if device is Samsung (used in timing calculations)
*/
fun isSamsungDevice(): Boolean {
return Build.MANUFACTURER.equals("samsung", ignoreCase = true)
}

// ==================== MATH UTILITIES (REPOSITORY-WIDE) ====================

/**
 * Clamp value between min and max (used across multiple modules)
*/
fun clamp(value: Int, min: Int, max: Int): Int {
return when {
value < min -> min
value > max -> max
else -> value
}
}

/**
 * Clamp float value (used across multiple modules)
*/
fun clamp(value: Float, min: Float, max: Float): Float {
return when {
value < min -> min
value > max -> max
else -> value
}
}

/**
 * Check if value is in range (used across multiple modules)
*/
fun inRange(value: Int, min: Int, max: Int): Boolean = value in min..max

/**
 * Linear interpolation (used across multiple modules)
*/
fun lerp(start: Float, end: Float, fraction: Float): Float {
return start + fraction * (end - start)
}

// ==================== VALIDATION UTILITIES (REPOSITORY-WIDE) ====================

/**
 * Validate email format (used across multiple modules)
*/
fun isValidEmail(email: String?): Boolean {
if (email.isNullOrEmpty()) return false
return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

/**
 * Validate IP address (used in network modules)
*/
fun isValidIpAddress(ip: String?): Boolean {
if (ip.isNullOrEmpty()) return false
return android.util.Patterns.IP_ADDRESS.matcher(ip).matches()
}

/**
 * Validate port number (used in network modules)
*/
fun isValidPort(port: Int): Boolean = port in 1..65535

/**
 * Validate MAC address (used in BLE modules)
*/
fun isValidMacAddress(mac: String?): Boolean {
if (mac.isNullOrEmpty()) return false
val macPattern = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$"
return mac.matches(macPattern.toRegex())
}

// ==================== TIMING UTILITIES (REPOSITORY-WIDE) ====================

/**
 * Get current timestamp in milliseconds (used across all modules)
*/
fun getCurrentTimestamp(): Long = System.currentTimeMillis()

/**
 * Get current timestamp in nanoseconds (used for precision timing)
*/
fun getCurrentTimestampNanos(): Long = System.nanoTime()

/**
 * Format timestamp for logging (used across all modules)
*/
fun formatTimestampForLogging(timestamp: Long): String {
return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
.format(Date(timestamp))
}

/**
 * Calculate elapsed time (used across all modules)
*/
fun calculateElapsedTime(startTime: Long, endTime: Long = getCurrentTimestamp()): Long {
return endTime - startTime
}

// ==================== LOGGING UTILITIES (REPOSITORY-WIDE) ====================

/**
 * Safe logging with null checks (used across all modules)
*/
fun logDebug(tag: String, message: String?, throwable: Throwable? = null) {
val safeMessage = message ?: "null"
android.util.Log.d(tag, safeMessage, throwable)
}

fun logInfo(tag: String, message: String?, throwable: Throwable? = null) {
val safeMessage = message ?: "null"
android.util.Log.i(tag, safeMessage, throwable)
}

fun logWarning(tag: String, message: String?, throwable: Throwable? = null) {
val safeMessage = message ?: "null"
android.util.Log.w(tag, safeMessage, throwable)
}

fun logError(tag: String, message: String?, throwable: Throwable? = null) {
val safeMessage = message ?: "null"
android.util.Log.e(tag, safeMessage, throwable)
}

// ==================== NETWORK UTILITIES (REPOSITORY-WIDE) ====================

/**
 * Check if string is valid URL (used across network modules)
*/
fun isValidUrl(url: String?): Boolean {
if (url.isNullOrEmpty()) return false
return try {
java.net.URL(url)
true
} catch (e: Exception) {
false
}
}

/**
 * Generate random port number (used in network modules)
*/
fun generateRandomPort(min: Int = 49152, max: Int = 65535): Int {
return Random().nextInt(max - min + 1) + min
}

// ==================== PERMISSION UTILITIES (REPOSITORY-WIDE) ====================

/**
 * Check if permission is granted (used across all modules)
*/
fun isPermissionGranted(context: Context, permission: String): Boolean {
return androidx.core.content.ContextCompat.checkSelfPermission(context, permission) ==
android.content.pm.PackageManager.PERMISSION_GRANTED
}

/**
 * Check multiple permissions (used across all modules)
*/
fun arePermissionsGranted(context: Context, permissions: Array<String>): Boolean {
return permissions.all { isPermissionGranted(context, it) }
}

// ==================== RESOURCE UTILITIES (REPOSITORY-WIDE) ====================

/**
 * Safe resource string retrieval (used across all modules)
*/
fun getStringResource(context: Context, resId: Int, defaultValue: String = ""): String {
return try {
context.getString(resId)
} catch (e: Exception) {
logError(TAG, "Failed to get string resource: $resId", e)
defaultValue
}
}

/**
 * Safe resource color retrieval (used across all modules)
*/
fun getColorResource(context: Context, resId: Int, defaultColor: Int = 0): Int {
return try {
androidx.core.content.ContextCompat.getColor(context, resId)
} catch (e: Exception) {
logError(TAG, "Failed to get color resource: $resId", e)
defaultColor
}
}

// ==================== REPOSITORY-WIDE CONSTANTS ====================

object Constants {
// Network constants (used across network modules)
const val DEFAULT_TIMEOUT_MS = 5000L
const val DEFAULT_RETRY_COUNT = 3
const val DEFAULT_BUFFER_SIZE = 8192

// BLE constants (used across BLE modules)
const val BLE_SCAN_TIMEOUT_MS = 10000L
const val BLE_CONNECTION_TIMEOUT_MS = 15000L
const val BLE_OPERATION_TIMEOUT_MS = 5000L

// GSR constants (used across GSR modules)
const val DEFAULT_GSR_SAMPLING_RATE = 128.0
const val GSR_MIN_RESISTANCE = 0.1
const val GSR_MAX_RESISTANCE = 1000000.0

// Thermal constants (used across thermal modules)
const val THERMAL_IMAGE_WIDTH = 256
const val THERMAL_IMAGE_HEIGHT = 192
const val THERMAL_MIN_TEMP = -40.0f
const val THERMAL_MAX_TEMP = 150.0f

// File system constants (used across all modules)
const val MAX_FILE_SIZE_MB = 100
const val MAX_SESSION_DURATION_MS = 3600000L // 1 hour
}
// End of file