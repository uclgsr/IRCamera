package mpdc4gsr.core.data

import android.content.Context
import android.content.SharedPreferences
import mpdc4gsr.core.utils.AppLogger

object FeatureFlags {
    private const val TAG = "FeatureFlags"
    private const val PREFS_NAME = "pc_to_phone_features"
    private const val KEY_COMM_USE_WSS = "COMM_USE_WSS"
    private const val KEY_TLS_ENABLE = "TLS_ENABLE"
    private const val KEY_MDNS_ENABLE = "MDNS_ENABLE"
    private const val KEY_FILE_UPLOAD_PROTOCOL = "FILE_UPLOAD_PROTOCOL"
    private const val KEY_TIME_SYNC_MODE = "TIME_SYNC_MODE"
    private const val DEFAULT_COMM_USE_WSS = true
    private const val DEFAULT_TLS_ENABLE = true
    private const val DEFAULT_MDNS_ENABLE = true
    private const val DEFAULT_FILE_UPLOAD_PROTOCOL = "tcp"
    private const val DEFAULT_TIME_SYNC_MODE = "ntp"
    private var prefs: SharedPreferences? = null
    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        AppLogger.i(TAG, "Feature flags initialized with defaults")
        logCurrentConfiguration()
    }

    val COMM_USE_WSS: Boolean
        get() = prefs?.getBoolean(KEY_COMM_USE_WSS, DEFAULT_COMM_USE_WSS) ?: DEFAULT_COMM_USE_WSS
    val TLS_ENABLE: Boolean
        get() = prefs?.getBoolean(KEY_TLS_ENABLE, DEFAULT_TLS_ENABLE) ?: DEFAULT_TLS_ENABLE
    val MDNS_ENABLE: Boolean
        get() = prefs?.getBoolean(KEY_MDNS_ENABLE, DEFAULT_MDNS_ENABLE) ?: DEFAULT_MDNS_ENABLE
    val FILE_UPLOAD_PROTOCOL: String
        get() = prefs?.getString(KEY_FILE_UPLOAD_PROTOCOL, DEFAULT_FILE_UPLOAD_PROTOCOL)
            ?: DEFAULT_FILE_UPLOAD_PROTOCOL
    val TIME_SYNC_MODE: String
        get() = prefs?.getString(KEY_TIME_SYNC_MODE, DEFAULT_TIME_SYNC_MODE)
            ?: DEFAULT_TIME_SYNC_MODE

    fun setCommUseWSS(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_COMM_USE_WSS, enabled)?.apply()
        AppLogger.i(TAG, "COMM_USE_WSS set to $enabled")
    }

    fun setTlsEnable(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_TLS_ENABLE, enabled)?.apply()
        AppLogger.i(TAG, "TLS_ENABLE set to $enabled")
    }

    fun setMdnsEnable(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_MDNS_ENABLE, enabled)?.apply()
        AppLogger.i(TAG, "MDNS_ENABLE set to $enabled")
    }

    fun setFileUploadProtocol(protocol: String) {
        prefs?.edit()?.putString(KEY_FILE_UPLOAD_PROTOCOL, protocol)?.apply()
        AppLogger.i(TAG, "FILE_UPLOAD_PROTOCOL set to $protocol")
    }

    fun setTimeSyncMode(mode: String) {
        prefs?.edit()?.putString(KEY_TIME_SYNC_MODE, mode)?.apply()
        AppLogger.i(TAG, "TIME_SYNC_MODE set to $mode")
    }

    fun getAllFlags(): Map<String, Any> {
        return mapOf(
            KEY_COMM_USE_WSS to COMM_USE_WSS,
            KEY_TLS_ENABLE to TLS_ENABLE,
            KEY_MDNS_ENABLE to MDNS_ENABLE,
            KEY_FILE_UPLOAD_PROTOCOL to FILE_UPLOAD_PROTOCOL,
            KEY_TIME_SYNC_MODE to TIME_SYNC_MODE,
        )
    }

    fun resetToDefaults() {
        prefs?.edit()?.clear()?.apply()
        AppLogger.i(TAG, "Feature flags reset to defaults")
        logCurrentConfiguration()
    }

    private fun logCurrentConfiguration() {
        val flags = getAllFlags()
        AppLogger.i(TAG, "Current feature flag configuration:")
        flags.forEach { (key, value) ->
            AppLogger.i(TAG, "  $key: $value")
        }
    }

    fun validateConfiguration(): List<String> {
        val warnings = mutableListOf<String>()
        if (COMM_USE_WSS && !TLS_ENABLE) {
            warnings.add("COMM_USE_WSS=true but TLS_ENABLE=false - WebSocket Secure requires TLS")
        }
        if (FILE_UPLOAD_PROTOCOL !in listOf("tcp", "http", "websocket")) {
            warnings.add("Invalid FILE_UPLOAD_PROTOCOL: $FILE_UPLOAD_PROTOCOL")
        }
        if (TIME_SYNC_MODE !in listOf("ntp", "manual", "disabled")) {
            warnings.add("Invalid TIME_SYNC_MODE: $TIME_SYNC_MODE")
        }
        if (warnings.isNotEmpty()) {
            warnings.forEach { warning ->
                AppLogger.w(TAG, "Configuration warning: $warning")
            }
        }
        return warnings
    }
}
