package mpdc4gsr.feature.capture.gsr.data

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GSRSettingsRepository(
    private val context: Context,
) {
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    // StateFlow for reactive settings updates
    private val _gsrSettings = MutableStateFlow(loadGSRSettings())
    val gsrSettings: StateFlow<GSRSettings> = _gsrSettings
    private val _deviceSettings = MutableStateFlow(loadDeviceSettings())
    val deviceSettings: StateFlow<DeviceSettings> = _deviceSettings

    data class GSRSettings(
        val isEnabled: Boolean = true,
        val samplingRate: Int = 128,
        val autoStartRecording: Boolean = false,
        val enableRealTimeMonitoring: Boolean = true,
        val dataFormat: DataFormat = DataFormat.CSV,
        val bufferSize: Int = 1024,
        val enableFiltering: Boolean = true,
        val notificationEnabled: Boolean = true,
    )

    data class DeviceSettings(
        val selectedDeviceId: String? = null,
        val deviceName: String? = null,
        val connectionTimeout: Int = 30,
        val autoReconnect: Boolean = true,
        val reconnectionAttempts: Int = 3,
        val reconnectionBaseDelayMs: Long = 2000L,
        val keepDeviceConnected: Boolean = false,
        val deviceCalibrationEnabled: Boolean = true,
    )

    enum class DataFormat {
        CSV,
        JSON,
        BINARY,
    }

    companion object {
        // SharedPreferences keys
        private const val KEY_GSR_ENABLED = "gsr_enabled"
        private const val KEY_SAMPLING_RATE = "gsr_sampling_rate"
        private const val KEY_AUTO_START_RECORDING = "gsr_auto_start_recording"
        private const val KEY_REAL_TIME_MONITORING = "gsr_real_time_monitoring"
        private const val KEY_DATA_FORMAT = "gsr_data_format"
        private const val KEY_BUFFER_SIZE = "gsr_buffer_size"
        private const val KEY_ENABLE_FILTERING = "gsr_enable_filtering"
        private const val KEY_NOTIFICATION_ENABLED = "gsr_notification_enabled"
        private const val KEY_SELECTED_DEVICE_ID = "gsr_selected_device_id"
        private const val KEY_DEVICE_NAME = "gsr_device_name"
        private const val KEY_CONNECTION_TIMEOUT = "gsr_connection_timeout"
        private const val KEY_AUTO_RECONNECT = "gsr_auto_reconnect"
        private const val KEY_RECONNECTION_ATTEMPTS = "gsr_reconnection_attempts"
        private const val KEY_RECONNECTION_BASE_DELAY = "gsr_reconnection_base_delay"
        private const val KEY_KEEP_DEVICE_CONNECTED = "gsr_keep_device_connected"
        private const val KEY_DEVICE_CALIBRATION = "gsr_device_calibration"

        // Default values
        private const val DEFAULT_SAMPLING_RATE = 128
        private const val DEFAULT_CONNECTION_TIMEOUT = 30
        private const val DEFAULT_BUFFER_SIZE = 1024
    }

    private fun loadGSRSettings(): GSRSettings =
        GSRSettings(
            isEnabled = prefs.getBoolean(KEY_GSR_ENABLED, true),
            samplingRate = prefs.getInt(KEY_SAMPLING_RATE, DEFAULT_SAMPLING_RATE),
            autoStartRecording = prefs.getBoolean(KEY_AUTO_START_RECORDING, false),
            enableRealTimeMonitoring = prefs.getBoolean(KEY_REAL_TIME_MONITORING, true),
            dataFormat =
                DataFormat.valueOf(
                    prefs.getString(KEY_DATA_FORMAT, DataFormat.CSV.name) ?: DataFormat.CSV.name,
                ),
            bufferSize = prefs.getInt(KEY_BUFFER_SIZE, DEFAULT_BUFFER_SIZE),
            enableFiltering = prefs.getBoolean(KEY_ENABLE_FILTERING, true),
            notificationEnabled = prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true),
        )

    private fun loadDeviceSettings(): DeviceSettings =
        DeviceSettings(
            selectedDeviceId = prefs.getString(KEY_SELECTED_DEVICE_ID, null),
            deviceName = prefs.getString(KEY_DEVICE_NAME, null),
            connectionTimeout = prefs.getInt(KEY_CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT),
            autoReconnect = prefs.getBoolean(KEY_AUTO_RECONNECT, true),
            reconnectionAttempts = prefs.getInt(KEY_RECONNECTION_ATTEMPTS, 3),
            reconnectionBaseDelayMs = prefs.getLong(KEY_RECONNECTION_BASE_DELAY, 2000L),
            keepDeviceConnected = prefs.getBoolean(KEY_KEEP_DEVICE_CONNECTED, false),
            deviceCalibrationEnabled = prefs.getBoolean(KEY_DEVICE_CALIBRATION, true),
        )

    suspend fun updateGSRSettings(settings: GSRSettings) {
        prefs.edit().apply {
            putBoolean(KEY_GSR_ENABLED, settings.isEnabled)
            putInt(KEY_SAMPLING_RATE, settings.samplingRate)
            putBoolean(KEY_AUTO_START_RECORDING, settings.autoStartRecording)
            putBoolean(KEY_REAL_TIME_MONITORING, settings.enableRealTimeMonitoring)
            putString(KEY_DATA_FORMAT, settings.dataFormat.name)
            putInt(KEY_BUFFER_SIZE, settings.bufferSize)
            putBoolean(KEY_ENABLE_FILTERING, settings.enableFiltering)
            putBoolean(KEY_NOTIFICATION_ENABLED, settings.notificationEnabled)
            apply()
        }
        _gsrSettings.value = settings
    }

    suspend fun updateDeviceSettings(settings: DeviceSettings) {
        prefs.edit().apply {
            putString(KEY_SELECTED_DEVICE_ID, settings.selectedDeviceId)
            putString(KEY_DEVICE_NAME, settings.deviceName)
            putInt(KEY_CONNECTION_TIMEOUT, settings.connectionTimeout)
            putBoolean(KEY_AUTO_RECONNECT, settings.autoReconnect)
            putInt(KEY_RECONNECTION_ATTEMPTS, settings.reconnectionAttempts)
            putLong(KEY_RECONNECTION_BASE_DELAY, settings.reconnectionBaseDelayMs)
            putBoolean(KEY_KEEP_DEVICE_CONNECTED, settings.keepDeviceConnected)
            putBoolean(KEY_DEVICE_CALIBRATION, settings.deviceCalibrationEnabled)
            apply()
        }
        _deviceSettings.value = settings
    }

    suspend fun resetToDefaults() {
        val defaultGSRSettings = GSRSettings()
        val defaultDeviceSettings = DeviceSettings()
        updateGSRSettings(defaultGSRSettings)
        updateDeviceSettings(defaultDeviceSettings)
    }

    fun getSamplingRateOptions(): List<Int> = listOf(32, 64, 128, 256, 512, 1024)

    fun getDataFormatOptions(): List<DataFormat> = DataFormat.values().toList()

    fun getConnectionTimeoutOptions(): List<Int> = listOf(10, 15, 30, 45, 60)

    fun getBufferSizeOptions(): List<Int> = listOf(256, 512, 1024, 2048, 4096)

    fun isValidSamplingRate(rate: Int): Boolean = rate in getSamplingRateOptions()

    fun isValidConnectionTimeout(timeout: Int): Boolean = timeout in getConnectionTimeoutOptions()

    fun isValidBufferSize(size: Int): Boolean = size in getBufferSizeOptions()

    // Export current settings for backup/sharing
    fun exportSettings(): Map<String, Any> {
        val currentGSR = _gsrSettings.value
        val currentDevice = _deviceSettings.value
        return mapOf(
            "gsr_settings" to
                    mapOf(
                        "enabled" to currentGSR.isEnabled,
                        "sampling_rate" to currentGSR.samplingRate,
                        "auto_start_recording" to currentGSR.autoStartRecording,
                        "real_time_monitoring" to currentGSR.enableRealTimeMonitoring,
                        "data_format" to currentGSR.dataFormat.name,
                        "buffer_size" to currentGSR.bufferSize,
                        "enable_filtering" to currentGSR.enableFiltering,
                        "notification_enabled" to currentGSR.notificationEnabled,
                    ),
            "device_settings" to
                    mapOf(
                        "selected_device_id" to currentDevice.selectedDeviceId,
                        "device_name" to currentDevice.deviceName,
                        "connection_timeout" to currentDevice.connectionTimeout,
                        "auto_reconnect" to currentDevice.autoReconnect,
                        "keep_device_connected" to currentDevice.keepDeviceConnected,
                        "device_calibration_enabled" to currentDevice.deviceCalibrationEnabled,
                    ),
        )
    }

    // Import settings from backup
    suspend fun importSettings(settingsMap: Map<String, Any>): Boolean {
        return try {
            @Suppress("UNCHECKED_CAST")
            val gsrMap = settingsMap["gsr_settings"] as? Map<String, Any> ?: return false

            @Suppress("UNCHECKED_CAST")
            val deviceMap = settingsMap["device_settings"] as? Map<String, Any> ?: return false
            val gsrSettings =
                GSRSettings(
                    isEnabled = gsrMap["enabled"] as? Boolean ?: true,
                    samplingRate = gsrMap["sampling_rate"] as? Int ?: DEFAULT_SAMPLING_RATE,
                    autoStartRecording = gsrMap["auto_start_recording"] as? Boolean ?: false,
                    enableRealTimeMonitoring = gsrMap["real_time_monitoring"] as? Boolean ?: true,
                    dataFormat =
                        try {
                            DataFormat.valueOf(gsrMap["data_format"] as? String ?: DataFormat.CSV.name)
                        } catch (e: Exception) {
                            DataFormat.CSV
                        },
                    bufferSize = gsrMap["buffer_size"] as? Int ?: DEFAULT_BUFFER_SIZE,
                    enableFiltering = gsrMap["enable_filtering"] as? Boolean ?: true,
                    notificationEnabled = gsrMap["notification_enabled"] as? Boolean ?: true,
                )
            val deviceSettings =
                DeviceSettings(
                    selectedDeviceId = deviceMap["selected_device_id"] as? String,
                    deviceName = deviceMap["device_name"] as? String,
                    connectionTimeout =
                        deviceMap["connection_timeout"] as? Int
                            ?: DEFAULT_CONNECTION_TIMEOUT,
                    autoReconnect = deviceMap["auto_reconnect"] as? Boolean ?: true,
                    keepDeviceConnected = deviceMap["keep_device_connected"] as? Boolean ?: false,
                    deviceCalibrationEnabled =
                        deviceMap["device_calibration_enabled"] as? Boolean
                            ?: true,
                )
            updateGSRSettings(gsrSettings)
            updateDeviceSettings(deviceSettings)
            true
        } catch (e: Exception) {
            false
        }
    }
}

