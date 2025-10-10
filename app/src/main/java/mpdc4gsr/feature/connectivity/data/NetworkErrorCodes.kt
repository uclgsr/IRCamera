package mpdc4gsr.feature.connectivity.data

object NetworkErrorCodes {
    // Connection Error Codes (1xxx)
    const val ERROR_CONNECTION_FAILED = 1001
    const val ERROR_CONNECTION_TIMEOUT = 1002
    const val ERROR_CONNECTION_REFUSED = 1003
    const val ERROR_CONNECTION_LOST = 1004
    const val ERROR_CONNECTION_RESET = 1005
    const val ERROR_AUTHENTICATION_FAILED = 1006
    const val ERROR_SSL_HANDSHAKE_FAILED = 1007

    // Bluetooth Error Codes (2xxx)
    const val ERROR_BLUETOOTH_NOT_SUPPORTED = 2001
    const val ERROR_BLUETOOTH_DISABLED = 2002
    const val ERROR_BLUETOOTH_PERMISSION_DENIED = 2003
    const val ERROR_BLUETOOTH_DEVICE_NOT_FOUND = 2004
    const val ERROR_BLUETOOTH_PAIRING_FAILED = 2005
    const val ERROR_BLUETOOTH_SERVICE_DISCOVERY_FAILED = 2006

    // Wi-Fi Error Codes (3xxx)
    const val ERROR_WIFI_DISABLED = 3001
    const val ERROR_WIFI_NO_NETWORK = 3002
    const val ERROR_WIFI_INVALID_IP = 3003
    const val ERROR_WIFI_PORT_UNREACHABLE = 3004
    const val ERROR_WIFI_DNS_RESOLUTION_FAILED = 3005

    // Protocol Error Codes (4xxx)
    const val ERROR_PROTOCOL_VERSION_MISMATCH = 4001
    const val ERROR_INVALID_MESSAGE_FORMAT = 4002
    const val ERROR_UNSUPPORTED_COMMAND = 4003
    const val ERROR_MESSAGE_TOO_LARGE = 4004
    const val ERROR_PROTOCOL_VIOLATION = 4005

    // Recording Error Codes (5xxx)
    const val ERROR_RECORDING_ALREADY_ACTIVE = 5001
    const val ERROR_RECORDING_NOT_ACTIVE = 5002
    const val ERROR_RECORDING_PERMISSION_DENIED = 5003
    const val ERROR_RECORDING_HARDWARE_FAILURE = 5004
    const val ERROR_RECORDING_STORAGE_FULL = 5005

    // Configuration Error Codes (6xxx)
    const val ERROR_INVALID_CONFIGURATION = 6001
    const val ERROR_SETTINGS_NOT_FOUND = 6002
    const val ERROR_SETTINGS_CORRUPTED = 6003
    const val ERROR_UNSUPPORTED_SETTING = 6004

    // System Error Codes (7xxx)
    const val ERROR_INSUFFICIENT_MEMORY = 7001
    const val ERROR_INSUFFICIENT_BATTERY = 7002
    const val ERROR_DEVICE_OVERHEATING = 7003
    const val ERROR_SYSTEM_RESOURCE_UNAVAILABLE = 7004

    // Unknown/Generic Error
    const val ERROR_UNKNOWN = 9999

    fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            // Connection errors
            ERROR_CONNECTION_FAILED -> "Connection failed"
            ERROR_CONNECTION_TIMEOUT -> "Connection timed out"
            ERROR_CONNECTION_REFUSED -> "Connection refused by server"
            ERROR_CONNECTION_LOST -> "Connection lost"
            ERROR_CONNECTION_RESET -> "Connection reset by peer"
            ERROR_AUTHENTICATION_FAILED -> "Authentication failed"
            ERROR_SSL_HANDSHAKE_FAILED -> "SSL handshake failed"
            // Bluetooth errors
            ERROR_BLUETOOTH_NOT_SUPPORTED -> "Bluetooth not supported"
            ERROR_BLUETOOTH_DISABLED -> "Bluetooth is disabled"
            ERROR_BLUETOOTH_PERMISSION_DENIED -> "Bluetooth permission denied"
            ERROR_BLUETOOTH_DEVICE_NOT_FOUND -> "Bluetooth device not found"
            ERROR_BLUETOOTH_PAIRING_FAILED -> "Bluetooth pairing failed"
            ERROR_BLUETOOTH_SERVICE_DISCOVERY_FAILED -> "Bluetooth service discovery failed"
            // Wi-Fi errors
            ERROR_WIFI_DISABLED -> "Wi-Fi is disabled"
            ERROR_WIFI_NO_NETWORK -> "No Wi-Fi network available"
            ERROR_WIFI_INVALID_IP -> "Invalid IP address"
            ERROR_WIFI_PORT_UNREACHABLE -> "Port unreachable"
            ERROR_WIFI_DNS_RESOLUTION_FAILED -> "DNS resolution failed"
            // Protocol errors
            ERROR_PROTOCOL_VERSION_MISMATCH -> "Protocol version mismatch"
            ERROR_INVALID_MESSAGE_FORMAT -> "Invalid message format"
            ERROR_UNSUPPORTED_COMMAND -> "Unsupported command"
            ERROR_MESSAGE_TOO_LARGE -> "Message too large"
            ERROR_PROTOCOL_VIOLATION -> "Protocol violation"
            // Recording errors
            ERROR_RECORDING_ALREADY_ACTIVE -> "Recording already active"
            ERROR_RECORDING_NOT_ACTIVE -> "No active recording"
            ERROR_RECORDING_PERMISSION_DENIED -> "Recording permission denied"
            ERROR_RECORDING_HARDWARE_FAILURE -> "Recording hardware failure"
            ERROR_RECORDING_STORAGE_FULL -> "Storage full"
            // Configuration errors
            ERROR_INVALID_CONFIGURATION -> "Invalid configuration"
            ERROR_SETTINGS_NOT_FOUND -> "Settings not found"
            ERROR_SETTINGS_CORRUPTED -> "Settings corrupted"
            ERROR_UNSUPPORTED_SETTING -> "Unsupported setting"
            // System errors
            ERROR_INSUFFICIENT_MEMORY -> "Insufficient memory"
            ERROR_INSUFFICIENT_BATTERY -> "Insufficient battery"
            ERROR_DEVICE_OVERHEATING -> "Device overheating"
            ERROR_SYSTEM_RESOURCE_UNAVAILABLE -> "System resource unavailable"
            else -> "Unknown error"
        }
    }

    fun getErrorCategory(errorCode: Int): String {
        return when (errorCode / 1000) {
            1 -> "Connection"
            2 -> "Bluetooth"
            3 -> "Wi-Fi"
            4 -> "Protocol"
            5 -> "Recording"
            6 -> "Configuration"
            7 -> "System"
            else -> "Unknown"
        }
    }

    fun isRecoverable(errorCode: Int): Boolean {
        return when (errorCode) {
            ERROR_CONNECTION_TIMEOUT,
            ERROR_CONNECTION_LOST,
            ERROR_CONNECTION_RESET,
            ERROR_WIFI_NO_NETWORK,
            ERROR_WIFI_PORT_UNREACHABLE -> true

            else -> false
        }
    }

    data class NetworkError(
        val code: Int,
        val message: String = getErrorMessage(code),
        val category: String = getErrorCategory(code),
        val recoverable: Boolean = isRecoverable(code),
        val timestamp: Long = System.currentTimeMillis(),
        val details: String? = null
    )
}
