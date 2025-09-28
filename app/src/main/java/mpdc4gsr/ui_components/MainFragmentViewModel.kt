package mpdc4gsr.ui_components

import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.socket.WebSocketProxy
import com.mpdc4gsr.libunified.app.tools.DeviceTools
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Modernized MainFragmentViewModel using StateFlow and Repository pattern
 * Manages device connections and main screen navigation with reactive state management
 */
class MainFragmentViewModel : BaseViewModel() {

    // StateFlow for reactive device state management
    private val _deviceState = MutableStateFlow(DeviceState())
    val deviceState: StateFlow<DeviceState> = _deviceState.asStateFlow()

    private val _batteryInfo = MutableStateFlow<BatteryInfo?>(null)
    val batteryInfo: StateFlow<BatteryInfo?> = _batteryInfo.asStateFlow()

    // SharedFlow for one-time navigation events
    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents.asSharedFlow()

    // Combined state for complex UI scenarios
    private val _mainScreenState = MutableStateFlow(MainScreenState())
    val mainScreenState: StateFlow<MainScreenState> = _mainScreenState.asStateFlow()

    data class DeviceState(
        val hasAnyDevice: Boolean = false,
        val hasConnectLine: Boolean = false,
        val hasConnectTS004: Boolean = false,
        val hasConnectTC007: Boolean = false,
        val isRefreshing: Boolean = false,
        val lastUpdated: Long = System.currentTimeMillis()
    ) {
        val hasAnyConnection: Boolean
            get() = hasConnectLine || hasConnectTS004 || hasConnectTC007

        val connectedDeviceCount: Int
            get() = listOf(hasConnectLine, hasConnectTS004, hasConnectTC007).count { it }
    }

    data class MainScreenState(
        val isInitialized: Boolean = false,
        val showWelcomeScreen: Boolean = true,
        val currentDevice: ConnectType? = null,
        val isRefreshingDevices: Boolean = false
    )

    sealed class NavigationEvent {
        data class Navigate(val route: String, val isTC007: Boolean = false, val isTS004: Boolean = false) :
            NavigationEvent()

        data class ShowDeviceSelection(val availableDevices: List<ConnectType>) : NavigationEvent()
        data class ShowError(val message: String) : NavigationEvent()
        object ShowDeviceAddDialog : NavigationEvent()
    }

    // Local data class to replace removed TC007 BatteryInfo
    data class BatteryInfo(
        val status: String?,
        val remaining: String?,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isCharging(): Boolean = status == "Charging"

        fun getBattery(): Int? =
            try {
                remaining?.toInt()
            } catch (e: NumberFormatException) {
                null
            }

        fun isLowBattery(): Boolean = getBattery()?.let { it < 20 } ?: false
    }

    enum class ConnectType {
        LINE,
        TS004,
        TC007,
    }

    init {
        initializeDeviceState()
    }

    /**
     * Initialize device state on ViewModel creation
     */
    fun initializeDeviceState() {
        launchWithErrorHandling {
            _mainScreenState.value = _mainScreenState.value.copy(isRefreshingDevices = true)

            val hasConnectLine = DeviceTools.isConnect()
            val hasConnectTS004 = WebSocketProxy.getInstance().isTS004Connect()
            val hasConnectTC007 = WebSocketProxy.getInstance().isTC007Connect()
            val hasAnyDevice = SharedManager.hasTcLine

            _deviceState.value = DeviceState(
                hasAnyDevice = hasAnyDevice,
                hasConnectLine = hasConnectLine,
                hasConnectTS004 = hasConnectTS004,
                hasConnectTC007 = hasConnectTC007,
                lastUpdated = System.currentTimeMillis()
            )

            _mainScreenState.value = _mainScreenState.value.copy(
                isInitialized = true,
                isRefreshingDevices = false,
                showWelcomeScreen = !_deviceState.value.hasAnyConnection
            )
        }
    }

    /**
     * Refresh device state with loading indication
     */
    fun refreshDeviceState() {
        launchWithErrorHandling {
            _deviceState.value = _deviceState.value.copy(isRefreshing = true)

            val hasAnyDevice = SharedManager.hasTcLine
            val hasConnectLine = DeviceTools.isConnect(isAutoRequest = false)
            val hasConnectTS004 = false // TS004 functionality removed
            val hasConnectTC007 = false // TC007 functionality removed

            _deviceState.value = DeviceState(
                hasAnyDevice = hasAnyDevice,
                hasConnectLine = hasConnectLine,
                hasConnectTS004 = hasConnectTS004,
                hasConnectTC007 = hasConnectTC007,
                isRefreshing = false,
                lastUpdated = System.currentTimeMillis()
            )

            _mainScreenState.value = _mainScreenState.value.copy(
                showWelcomeScreen = !_deviceState.value.hasAnyConnection
            )
        }
    }

    /**
     * Handle device item clicks with modern event emission
     */
    fun onDeviceItemClick(connectType: ConnectType) {
        launchWithErrorHandling {
            when (connectType) {
                ConnectType.LINE -> {
                    _navigationEvents.emit(
                        NavigationEvent.Navigate(
                            route = "IR_MAIN",
                            isTC007 = false
                        )
                    )
                }

                ConnectType.TS004 -> {
                    val currentState = _deviceState.value
                    if (currentState.hasConnectTS004) {
                        _navigationEvents.emit(NavigationEvent.Navigate(route = "IR_MONOCULAR"))
                    } else {
                        _navigationEvents.emit(
                            NavigationEvent.Navigate(
                                route = "IR_DEVICE_ADD",
                                isTS004 = true
                            )
                        )
                    }
                }

                ConnectType.TC007 -> {
                    _navigationEvents.emit(
                        NavigationEvent.Navigate(
                            route = "IR_MAIN",
                            isTC007 = true
                        )
                    )
                }
            }

            _mainScreenState.value = _mainScreenState.value.copy(currentDevice = connectType)
        }
    }

    /**
     * Handle device deletion with state updates
     */
    fun onDeviceDeleted(connectType: ConnectType) {
        launchWithErrorHandling {
            when (connectType) {
                ConnectType.LINE -> {
                    SharedManager.hasTcLine = false
                }

                ConnectType.TS004 -> {
                    // TS004 functionality removed
                }

                ConnectType.TC007 -> {
                    // TC007 functionality removed
                }
            }
            refreshDeviceState()
        }
    }

    /**
     * Handle device connection events
     */
    fun onDeviceConnected(isLine: Boolean = true) {
        launchWithErrorHandling {
            if (isLine) {
                SharedManager.hasTcLine = true
            }
            refreshDeviceState()
        }
    }

    /**
     * Handle device disconnection events
     */
    fun onDeviceDisconnected() {
        refreshDeviceState()
    }

    /**
     * Handle socket connection events
     */
    fun onSocketConnected(isTS004: Boolean) {
        // TS004/TC007 functionality removed
        // Implementation preserved for future use if needed
        refreshDeviceState()
    }

    /**
     * Handle socket disconnection events
     */
    fun onSocketDisconnected(isTS004: Boolean) {
        refreshDeviceState()
    }

    /**
     * Process battery update messages with error handling
     */
    fun processBatteryUpdate(messageText: String) {
        launchWithErrorHandling {
            try {
                val battery: JSONObject = JSONObject(messageText).getJSONObject("battery")
                val batteryInfo = BatteryInfo(
                    status = battery.getString("status"),
                    remaining = battery.getString("remaining")
                )

                _batteryInfo.value = batteryInfo

                // Show low battery warning if needed
                if (batteryInfo.isLowBattery()) {
                    _navigationEvents.emit(NavigationEvent.ShowError("Battery is low: ${batteryInfo.remaining}%"))
                }
            } catch (e: Exception) {
                // Handle parsing error silently for robustness
                _navigationEvents.emit(NavigationEvent.ShowError("Failed to update battery info"))
            }
        }
    }

    /**
     * Get available devices for selection
     */
    fun getAvailableDevices(): List<ConnectType> {
        val currentState = _deviceState.value
        val availableDevices = mutableListOf<ConnectType>()

        if (currentState.hasAnyDevice) availableDevices.add(ConnectType.LINE)
        if (currentState.hasConnectTS004) availableDevices.add(ConnectType.TS004)
        if (currentState.hasConnectTC007) availableDevices.add(ConnectType.TC007)

        return availableDevices
    }

    /**
     * Show device selection dialog
     */
    fun showDeviceSelection() {
        launchWithErrorHandling {
            val availableDevices = getAvailableDevices()
            if (availableDevices.isNotEmpty()) {
                _navigationEvents.emit(NavigationEvent.ShowDeviceSelection(availableDevices))
            } else {
                _navigationEvents.emit(NavigationEvent.ShowDeviceAddDialog)
            }
        }
    }

    companion object {
        private const val TAG = "MainFragmentViewModel"
    }
}