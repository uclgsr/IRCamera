package mpdc4gsr.feature.network.data

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NetworkSettings(private val context: Context) {
    companion object {
        private const val TAG = "NetworkSettings"
        private const val PREFS_NAME = "network_settings"

        // Wi-Fi TCP Settings
        private const val KEY_WIFI_ENABLED = "wifi_enabled"
        private const val KEY_PC_IP_ADDRESS = "pc_ip_address"
        private const val KEY_PC_PORT = "pc_port"
        private const val KEY_AUTO_CONNECT_WIFI = "auto_connect_wifi"

        // Bluetooth Settings
        private const val KEY_BLUETOOTH_ENABLED = "bluetooth_enabled"
        private const val KEY_BLUETOOTH_DEVICE_ADDRESS = "bluetooth_device_address"
        private const val KEY_BLUETOOTH_DEVICE_NAME = "bluetooth_device_name"
        private const val KEY_AUTO_CONNECT_BLUETOOTH = "auto_connect_bluetooth"

        // Connection Settings
        private const val KEY_PREFERRED_CONNECTION_TYPE = "preferred_connection_type"
        private const val KEY_AUTO_RECONNECT = "auto_reconnect"
        private const val KEY_RECONNECT_ATTEMPTS = "reconnect_attempts"
        private const val KEY_CONNECTION_TIMEOUT = "connection_timeout"

        // Default Values
        const val DEFAULT_PC_IP = "192.168.1.100"
        const val DEFAULT_PC_PORT = 8080
        const val DEFAULT_RECONNECT_ATTEMPTS = 3
        const val DEFAULT_CONNECTION_TIMEOUT = 10000L
    }

    enum class ConnectionType {
        WIFI_TCP, BLUETOOTH_RFCOMM
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Thread-safe property accessors with background thread operations for complex tasks
    // Wi-Fi TCP Settings
    var isWifiEnabled: Boolean
        get() = prefs.getBoolean(KEY_WIFI_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_WIFI_ENABLED, value).apply()
    var pcIpAddress: String
        get() = prefs.getString(KEY_PC_IP_ADDRESS, DEFAULT_PC_IP) ?: DEFAULT_PC_IP
        set(value) = prefs.edit().putString(KEY_PC_IP_ADDRESS, value).apply()
    var pcPort: Int
        get() = prefs.getInt(KEY_PC_PORT, DEFAULT_PC_PORT)
        set(value) = prefs.edit().putInt(KEY_PC_PORT, value).apply()
    var autoConnectWifi: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CONNECT_WIFI, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CONNECT_WIFI, value).apply()

    // Bluetooth Settings
    var isBluetoothEnabled: Boolean
        get() = prefs.getBoolean(KEY_BLUETOOTH_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_BLUETOOTH_ENABLED, value).apply()
    var bluetoothDeviceAddress: String?
        get() = prefs.getString(KEY_BLUETOOTH_DEVICE_ADDRESS, null)
        set(value) = prefs.edit().putString(KEY_BLUETOOTH_DEVICE_ADDRESS, value).apply()
    var bluetoothDeviceName: String?
        get() = prefs.getString(KEY_BLUETOOTH_DEVICE_NAME, null)
        set(value) = prefs.edit().putString(KEY_BLUETOOTH_DEVICE_NAME, value).apply()
    var autoConnectBluetooth: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CONNECT_BLUETOOTH, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CONNECT_BLUETOOTH, value).apply()

    // Connection Settings
    var preferredConnectionType: ConnectionType
        get() {
            val ordinal =
                prefs.getInt(KEY_PREFERRED_CONNECTION_TYPE, ConnectionType.WIFI_TCP.ordinal)
            return ConnectionType.values().getOrNull(ordinal) ?: ConnectionType.WIFI_TCP
        }
        set(value) = prefs.edit().putInt(KEY_PREFERRED_CONNECTION_TYPE, value.ordinal).apply()
    var autoReconnect: Boolean
        get() = prefs.getBoolean(KEY_AUTO_RECONNECT, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_RECONNECT, value).apply()
    var reconnectAttempts: Int
        get() = prefs.getInt(KEY_RECONNECT_ATTEMPTS, DEFAULT_RECONNECT_ATTEMPTS)
        set(value) = prefs.edit().putInt(KEY_RECONNECT_ATTEMPTS, value).apply()
    var connectionTimeout: Long
        get() = prefs.getLong(KEY_CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT)
        set(value) = prefs.edit().putLong(KEY_CONNECTION_TIMEOUT, value).apply()

    // Keep-alive interval in milliseconds
    var keepAliveInterval: Long
        get() = prefs.getLong("keep_alive_interval", 30000L)
        set(value) = prefs.edit().putLong("keep_alive_interval", value).apply()

    // Message timeout in milliseconds
    var messageTimeout: Long
        get() = prefs.getLong("message_timeout", 10000L)
        set(value) = prefs.edit().putLong("message_timeout", value).apply()

    // Bandwidth monitoring enabled
    var bandwidthMonitoringEnabled: Boolean
        get() = prefs.getBoolean("bandwidth_monitoring_enabled", true)
        set(value) = prefs.edit().putBoolean("bandwidth_monitoring_enabled", value).apply()

    suspend fun saveBluetoothDevice(device: BluetoothDevice) = withContext(Dispatchers.IO) {
        try {
            val editor = prefs.edit()
            editor.putString(KEY_BLUETOOTH_DEVICE_ADDRESS, device.address)
            try {
                val deviceName = device.name
                editor.putString(KEY_BLUETOOTH_DEVICE_NAME, deviceName)
            } catch (e: SecurityException) {
                AppLogger.w(TAG, "Security exception accessing device name", e)
                // Save address only
            }
            editor.apply()
            AppLogger.i(TAG, "Saved Bluetooth device: ${device.address}")
        } catch (e: SecurityException) {
            AppLogger.e(TAG, "Security exception saving Bluetooth device", e)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error saving Bluetooth device", e)
        }
    }

    suspend fun getSavedBluetoothDeviceInfo(): Pair<String?, String?> =
        withContext(Dispatchers.IO) {
            try {
                val address = prefs.getString(KEY_BLUETOOTH_DEVICE_ADDRESS, null)
                val name = prefs.getString(KEY_BLUETOOTH_DEVICE_NAME, null)
                Pair(address, name)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error getting Bluetooth device info", e)
                Pair(null, null)
            }
        }

    suspend fun clearSettings() = withContext(Dispatchers.IO) {
        try {
            prefs.edit().clear().apply()
            AppLogger.i(TAG, "Network settings cleared")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error clearing settings", e)
        }
    }

    fun getConnectionSummary(): String {
        return when (preferredConnectionType) {
            ConnectionType.WIFI_TCP -> "Wi-Fi: $pcIpAddress:$pcPort"
            ConnectionType.BLUETOOTH_RFCOMM -> {
                val deviceName = bluetoothDeviceName ?: "Unknown Device"
                "Bluetooth: $deviceName"
            }
        }
    }

    fun isConfigured(): Boolean {
        return when (preferredConnectionType) {
            ConnectionType.WIFI_TCP -> pcIpAddress.isNotEmpty() && pcPort > 0
            ConnectionType.BLUETOOTH_RFCOMM -> !bluetoothDeviceAddress.isNullOrEmpty()
        }
    }
}