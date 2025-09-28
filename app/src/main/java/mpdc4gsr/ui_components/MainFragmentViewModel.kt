package mpdc4gsr.ui_components

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.socket.WebSocketProxy
import com.mpdc4gsr.libunified.app.tools.DeviceTools
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainFragmentViewModel : BaseViewModel() {

    private val _deviceState = MutableLiveData<DeviceState>()
    val deviceState: LiveData<DeviceState> = _deviceState

    private val _batteryInfo = MutableLiveData<BatteryInfo?>()
    val batteryInfo: LiveData<BatteryInfo?> = _batteryInfo

    private val _navigationEvent = MutableLiveData<NavigationEvent?>()
    val navigationEvent: LiveData<NavigationEvent?> = _navigationEvent

    data class DeviceState(
        val hasAnyDevice: Boolean,
        val hasConnectLine: Boolean,
        val hasConnectTS004: Boolean,
        val hasConnectTC007: Boolean
    )

    data class NavigationEvent(
        val route: String,
        val isTC007: Boolean = false,
        val isTS004: Boolean = false
    )

    // Local data class to replace removed TC007 BatteryInfo
    data class BatteryInfo(
        val status: String?,
        val remaining: String?
    ) {
        fun isCharging(): Boolean = status == "Charging"

        fun getBattery(): Int? =
            try {
                remaining?.toInt()
            } catch (e: NumberFormatException) {
                null
            }
    }

    fun refreshDeviceState() {
        viewModelScope.launch {
            val hasAnyDevice = SharedManager.hasTcLine
            val hasConnectLine = DeviceTools.isConnect(isAutoRequest = false)
            val hasConnectTS004 = false // TS004 functionality removed
            val hasConnectTC007 = false // TC007 functionality removed

            _deviceState.value = DeviceState(
                hasAnyDevice = hasAnyDevice,
                hasConnectLine = hasConnectLine,
                hasConnectTS004 = hasConnectTS004,
                hasConnectTC007 = hasConnectTC007
            )
        }
    }

    fun initializeDeviceState() {
        viewModelScope.launch {
            val hasConnectLine = DeviceTools.isConnect()
            val hasConnectTS004 = WebSocketProxy.getInstance().isTS004Connect()
            val hasConnectTC007 = WebSocketProxy.getInstance().isTC007Connect()
            val hasAnyDevice = SharedManager.hasTcLine

            _deviceState.value = DeviceState(
                hasAnyDevice = hasAnyDevice,
                hasConnectLine = hasConnectLine,
                hasConnectTS004 = hasConnectTS004,
                hasConnectTC007 = hasConnectTC007
            )
        }
    }

    fun onDeviceItemClick(connectType: ConnectType) {
        when (connectType) {
            ConnectType.LINE -> {
                _navigationEvent.value = NavigationEvent(
                    route = "IR_MAIN",
                    isTC007 = false
                )
            }

            ConnectType.TS004 -> {
                val currentState = _deviceState.value
                if (currentState?.hasConnectTS004 == true) {
                    _navigationEvent.value = NavigationEvent(route = "IR_MONOCULAR")
                } else {
                    _navigationEvent.value = NavigationEvent(
                        route = "IR_DEVICE_ADD",
                        isTS004 = true
                    )
                }
            }

            ConnectType.TC007 -> {
                _navigationEvent.value = NavigationEvent(
                    route = "IR_MAIN",
                    isTC007 = true
                )
            }
        }
    }

    fun onDeviceDeleted(connectType: ConnectType) {
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

    fun onDeviceConnected(isLine: Boolean = true) {
        if (isLine) {
            SharedManager.hasTcLine = true
        }
        refreshDeviceState()
    }

    fun onDeviceDisconnected() {
        refreshDeviceState()
    }

    fun onSocketConnected(isTS004: Boolean) {
        // TS004/TC007 functionality removed
        // Implementation preserved for future use if needed
        refreshDeviceState()
    }

    fun onSocketDisconnected(isTS004: Boolean) {
        refreshDeviceState()
    }

    fun processBatteryUpdate(messageText: String) {
        try {
            val battery: JSONObject = JSONObject(messageText).getJSONObject("battery")
            _batteryInfo.value = BatteryInfo(
                status = battery.getString("status"),
                remaining = battery.getString("remaining")
            )
        } catch (e: Exception) {
            // Handle parsing error silently
        }
    }

    fun clearNavigationEvent() {
        _navigationEvent.value = null
    }

    enum class ConnectType {
        LINE,
        TS004,
        TC007,
    }
}