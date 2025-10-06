package com.mpdc4gsr.libunified.app.utils
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
object UnifiedPreferencesUtils {
    private const val TAG = "UnifiedPreferences"
    private const val DEFAULT_PREFS_NAME = "ir_camera_prefs"
    // Preference keys organized by feature
    object Keys {
        // General app settings
        const val FIRST_LAUNCH = "first_launch"
        const val APP_VERSION = "app_version"
        const val LAST_UPDATE_CHECK = "last_update_check"
        // Camera settings
        const val CAMERA_RESOLUTION = "camera_resolution"
        const val CAMERA_FRAME_RATE = "camera_frame_rate"
        const val CAMERA_AUTO_FOCUS = "camera_auto_focus"
        const val CAMERA_FLASH_MODE = "camera_flash_mode"
        // Thermal settings
        const val THERMAL_UNIT = "thermal_unit"
        const val THERMAL_PALETTE = "thermal_palette"
        const val THERMAL_TEMPERATURE_RANGE = "thermal_temp_range"
        const val THERMAL_EMISSIVITY = "thermal_emissivity"
        // GSR settings
        const val GSR_SAMPLING_RATE = "gsr_sampling_rate"
        const val GSR_DEVICE_ADDRESS = "gsr_device_address"
        const val GSR_AUTO_CONNECT = "gsr_auto_connect"
        // Network settings
        const val NETWORK_SERVER_IP = "network_server_ip"
        const val NETWORK_SERVER_PORT = "network_server_port"
        const val NETWORK_AUTO_CONNECT = "network_auto_connect"
        const val NETWORK_TIMEOUT = "network_timeout"
        // Recording settings
        const val RECORDING_QUALITY = "recording_quality"
        const val RECORDING_AUTO_SAVE = "recording_auto_save"
        const val RECORDING_MAX_DURATION = "recording_max_duration"
        // UI settings
        const val UI_THEME = "ui_theme"
        const val UI_LANGUAGE = "ui_language"
        const val UI_SHOW_GUIDELINES = "ui_show_guidelines"
    }
    private fun getPreferences(
        context: Context,
        prefsName: String = DEFAULT_PREFS_NAME
    ): SharedPreferences {
        return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }
    fun putString(
        context: Context,
        key: String,
        value: String,
        prefsName: String = DEFAULT_PREFS_NAME
    ) {
        try {
            getPreferences(context, prefsName).edit().putString(key, value).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving string preference: $key", e)
        }
    }
    fun getString(
        context: Context,
        key: String,
        defaultValue: String = "",
        prefsName: String = DEFAULT_PREFS_NAME
    ): String {
        return try {
            getPreferences(context, prefsName).getString(key, defaultValue) ?: defaultValue
        } catch (e: Exception) {
            Log.e(TAG, "Error reading string preference: $key", e)
            defaultValue
        }
    }
    fun putInt(context: Context, key: String, value: Int, prefsName: String = DEFAULT_PREFS_NAME) {
        try {
            getPreferences(context, prefsName).edit().putInt(key, value).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving int preference: $key", e)
        }
    }
    fun getInt(
        context: Context,
        key: String,
        defaultValue: Int = 0,
        prefsName: String = DEFAULT_PREFS_NAME
    ): Int {
        return try {
            getPreferences(context, prefsName).getInt(key, defaultValue)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading int preference: $key", e)
            defaultValue
        }
    }
    fun putBoolean(
        context: Context,
        key: String,
        value: Boolean,
        prefsName: String = DEFAULT_PREFS_NAME
    ) {
        try {
            getPreferences(context, prefsName).edit().putBoolean(key, value).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving boolean preference: $key", e)
        }
    }
    fun getBoolean(
        context: Context,
        key: String,
        defaultValue: Boolean = false,
        prefsName: String = DEFAULT_PREFS_NAME
    ): Boolean {
        return try {
            getPreferences(context, prefsName).getBoolean(key, defaultValue)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading boolean preference: $key", e)
            defaultValue
        }
    }
    fun putFloat(
        context: Context,
        key: String,
        value: Float,
        prefsName: String = DEFAULT_PREFS_NAME
    ) {
        try {
            getPreferences(context, prefsName).edit().putFloat(key, value).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving float preference: $key", e)
        }
    }
    fun getFloat(
        context: Context,
        key: String,
        defaultValue: Float = 0f,
        prefsName: String = DEFAULT_PREFS_NAME
    ): Float {
        return try {
            getPreferences(context, prefsName).getFloat(key, defaultValue)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading float preference: $key", e)
            defaultValue
        }
    }
    fun putLong(
        context: Context,
        key: String,
        value: Long,
        prefsName: String = DEFAULT_PREFS_NAME
    ) {
        try {
            getPreferences(context, prefsName).edit().putLong(key, value).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving long preference: $key", e)
        }
    }
    fun getLong(
        context: Context,
        key: String,
        defaultValue: Long = 0L,
        prefsName: String = DEFAULT_PREFS_NAME
    ): Long {
        return try {
            getPreferences(context, prefsName).getLong(key, defaultValue)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading long preference: $key", e)
            defaultValue
        }
    }
    fun putStringSet(
        context: Context,
        key: String,
        value: Set<String>,
        prefsName: String = DEFAULT_PREFS_NAME
    ) {
        try {
            getPreferences(context, prefsName).edit().putStringSet(key, value).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving string set preference: $key", e)
        }
    }
    fun getStringSet(
        context: Context,
        key: String,
        defaultValue: Set<String> = emptySet(),
        prefsName: String = DEFAULT_PREFS_NAME
    ): Set<String> {
        return try {
            getPreferences(context, prefsName).getStringSet(key, defaultValue) ?: defaultValue
        } catch (e: Exception) {
            Log.e(TAG, "Error reading string set preference: $key", e)
            defaultValue
        }
    }
    fun remove(context: Context, key: String, prefsName: String = DEFAULT_PREFS_NAME) {
        try {
            getPreferences(context, prefsName).edit().remove(key).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error removing preference: $key", e)
        }
    }
    fun clear(context: Context, prefsName: String = DEFAULT_PREFS_NAME) {
        try {
            getPreferences(context, prefsName).edit().clear().apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing preferences", e)
        }
    }
    fun contains(context: Context, key: String, prefsName: String = DEFAULT_PREFS_NAME): Boolean {
        return try {
            getPreferences(context, prefsName).contains(key)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking preference exists: $key", e)
            false
        }
    }
    fun getAllKeys(context: Context, prefsName: String = DEFAULT_PREFS_NAME): Set<String> {
        return try {
            getPreferences(context, prefsName).all.keys
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all preference keys", e)
            emptySet()
        }
    }
    fun registerOnSharedPreferenceChangeListener(
        context: Context,
        listener: SharedPreferences.OnSharedPreferenceChangeListener,
        prefsName: String = DEFAULT_PREFS_NAME
    ) {
        try {
            getPreferences(context, prefsName).registerOnSharedPreferenceChangeListener(listener)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering preference change listener", e)
        }
    }
    fun unregisterOnSharedPreferenceChangeListener(
        context: Context,
        listener: SharedPreferences.OnSharedPreferenceChangeListener,
        prefsName: String = DEFAULT_PREFS_NAME
    ) {
        try {
            getPreferences(context, prefsName).unregisterOnSharedPreferenceChangeListener(listener)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering preference change listener", e)
        }
    }
    fun exportPreferences(context: Context, prefsName: String = DEFAULT_PREFS_NAME): String {
        return try {
            val prefs = getPreferences(context, prefsName).all
            val json = org.json.JSONObject()
            prefs.forEach { (key, value) ->
                json.put(key, value)
            }
            json.toString(2)
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting preferences", e)
            "{}"
        }
    }
    fun getDefaultPreferences(): Map<String, Any> {
        return mapOf(
            Keys.FIRST_LAUNCH to true,
            Keys.CAMERA_AUTO_FOCUS to true,
            Keys.THERMAL_UNIT to "celsius",
            Keys.THERMAL_PALETTE to "iron",
            Keys.THERMAL_EMISSIVITY to 0.95f,
            Keys.GSR_SAMPLING_RATE to 256,
            Keys.GSR_AUTO_CONNECT to false,
            Keys.NETWORK_AUTO_CONNECT to false,
            Keys.NETWORK_TIMEOUT to 5000,
            Keys.RECORDING_AUTO_SAVE to true
        )
    }
    fun initializePreferences(context: Context, defaults: Map<String, Any>) {
        val prefs = getSharedPreferences(context)
        val editor = prefs.edit()
        defaults.forEach { (key, value) ->
            if (!prefs.contains(key)) {
                when (value) {
                    is Boolean -> editor.putBoolean(key, value)
                    is String -> editor.putString(key, value)
                    is Int -> editor.putInt(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Long -> editor.putLong(key, value)
                }
            }
        }
        editor.apply()
    }
    fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(DEFAULT_PREFS_NAME, Context.MODE_PRIVATE)
    }
}