package mpdc4gsr.core.data.model

enum class NetworkStatus(
    val displayName: String,
    val isConnected: Boolean,
    val canDiscover: Boolean,
) {
    DISCONNECTED("Disconnected", false, false),
    NO_WIFI("No Wi-Fi", false, false),
    CONNECTED_TO_WIFI("Connected to Wi-Fi", true, true),
    PERMISSION_DENIED("Permission Denied", false, false),
    DISCOVERING("Discovering Controllers", true, true),
    READY("Ready", true, false),
    NO_CONTROLLERS_FOUND("No Controllers Found", true, false),
    CONNECTING("Connecting", true, false),
    CONNECTED("Connected to PC", true, false),
    CONNECTION_FAILED("Connection Failed", true, true),
    NETWORK_LOST("Network Lost", false, false),
    ERROR("Network Error", false, false),
    ;

    val isNetworkAvailable: Boolean
        get() = this != DISCONNECTED && this != NO_WIFI && this != NETWORK_LOST && this != PERMISSION_DENIED
    val isError: Boolean
        get() = this == ERROR || this == CONNECTION_FAILED || this == PERMISSION_DENIED
    val isConnecting: Boolean
        get() = this == DISCOVERING || this == CONNECTING
    val canConnect: Boolean
        get() = this == READY || this == NO_CONTROLLERS_FOUND || this == CONNECTION_FAILED
    val statusColor: StatusColor
        get() =
            when (this) {
                CONNECTED -> StatusColor.GREEN
                CONNECTED_TO_WIFI, READY -> StatusColor.BLUE
                DISCOVERING, CONNECTING -> StatusColor.YELLOW
                CONNECTION_FAILED, NO_CONTROLLERS_FOUND -> StatusColor.ORANGE
                DISCONNECTED, NO_WIFI, NETWORK_LOST, ERROR, PERMISSION_DENIED -> StatusColor.RED
            }
    val description: String
        get() =
            when (this) {
                DISCONNECTED -> "No network connection available"
                NO_WIFI -> "Wi-Fi connection required for PC communication"
                CONNECTED_TO_WIFI -> "Connected to Wi-Fi network"
                PERMISSION_DENIED -> "Network permissions required"
                DISCOVERING -> "Scanning for PC controllers on local network"
                READY -> "Ready to connect to PC controllers"
                NO_CONTROLLERS_FOUND -> "No PC controllers found on network"
                CONNECTING -> "Establishing connection to PC controller"
                CONNECTED -> "Connected and communicating with PC controller"
                CONNECTION_FAILED -> "Unable to connect to PC controller"
                NETWORK_LOST -> "Wi-Fi connection lost"
                ERROR -> "Network error occurred"
            }
    val recommendedAction: String?
        get() =
            when (this) {
                DISCONNECTED, NO_WIFI -> "Connect to Wi-Fi network"
                PERMISSION_DENIED -> "Grant network permissions in settings"
                NO_CONTROLLERS_FOUND -> "Ensure PC controller is running and on same network"
                CONNECTION_FAILED -> "Check PC controller address and try again"
                NETWORK_LOST -> "Reconnect to Wi-Fi network"
                ERROR -> "Check network settings and try again"
                else -> null
            }

    enum class StatusColor {
        GREEN,
        BLUE,
        YELLOW,
        ORANGE,
        RED,
    }

    companion object {
        fun getConnectedStates(): List<NetworkStatus> = values().filter { it.isConnected }

        fun getErrorStates(): List<NetworkStatus> = values().filter { it.isError }

        fun getDiscoveryStates(): List<NetworkStatus> = values().filter { it.canDiscover }

        fun fromConnectionState(
            hasWifi: Boolean,
            hasInternet: Boolean,
            isDiscovering: Boolean,
            connectedControllers: Int,
        ): NetworkStatus =
            when {
                !hasWifi -> NO_WIFI
                !hasInternet -> CONNECTED_TO_WIFI
                connectedControllers > 0 -> CONNECTED
                isDiscovering -> DISCOVERING
                else -> READY
            }
    }
}
