package com.mpdc4gsr.module.thermalunified.viewmodel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.socket.WebSocketProxy
import com.mpdc4gsr.libunified.app.tools.DeviceTools
import kotlinx.coroutines.launch
class IRMainActivityViewModel : BaseViewModel() {
    // Device state management
    data class DeviceState(
        val isTC007: Boolean = false,
        val isWebSocketConnected: Boolean = false,
        val isUsbConnected: Boolean = false,
        val shouldAutoOpen: Boolean = false,
        val shouldBlur: Boolean = false
    )
    // Fragment communication state
    data class FragmentCommunicationState(
        val activeFragment: Int = 0,
        val deviceConnected: Boolean = false,
        val pendingNavigation: NavigationEvent? = null
    )
    // Navigation events
    sealed class NavigationEvent {
        data class ToMonitor(val isTC007: Boolean) : NavigationEvent()
        object ToGallery : NavigationEvent()
        data class ToThermal(val routeConfig: String) : NavigationEvent()
    }
    // ViewPager state management
    sealed class ViewPagerState {
        data class PageSelected(val position: Int) : ViewPagerState()
        data class NavigateToPage(val position: Int) : ViewPagerState()
    }
    private val _deviceState = MutableLiveData<DeviceState>()
    val deviceState = _deviceState
    private val _fragmentCommunication = MutableLiveData<FragmentCommunicationState>()
    val fragmentCommunication = _fragmentCommunication
    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent = _navigationEvent
    private val _viewPagerState = MutableLiveData<ViewPagerState>()
    val viewPagerState = _viewPagerState
    private var currentDeviceType = false // false = not TC007, true = TC007
    fun setDeviceType(isTC007: Boolean) {
        currentDeviceType = isTC007
        refreshDeviceState()
    }
    fun initializeDeviceState() {
        viewModelScope.launch {
            refreshDeviceState()
        }
    }
    fun refreshDeviceState() {
        viewModelScope.launch {
            val deviceState = if (currentDeviceType) {
                // TC007 device state
                val isConnected = WebSocketProxy.getInstance().isTC007Connect()
                DeviceState(
                    isTC007 = true,
                    isWebSocketConnected = isConnected,
                    shouldAutoOpen = isConnected && SharedManager.isConnect07AutoOpen
                )
            } else {
                // USB device state
                val isConnected = DeviceTools.isConnect(isAutoRequest = false)
                DeviceState(
                    isTC007 = false,
                    isUsbConnected = isConnected,
                    shouldAutoOpen = isConnected && SharedManager.isConnectAutoOpen
                )
            }
            _deviceState.value = deviceState
        }
    }
    fun onPageSelected(position: Int) {
        _viewPagerState.value = ViewPagerState.PageSelected(position)
        updateFragmentCommunication(position)
    }
    fun navigateToPage(position: Int) {
        _viewPagerState.value = ViewPagerState.NavigateToPage(position)
    }
    fun navigateToMonitor() {
        _navigationEvent.value = NavigationEvent.ToMonitor(currentDeviceType)
    }
    fun navigateToGallery() {
        _navigationEvent.value = NavigationEvent.ToGallery
    }
    fun navigateToThermal() {
        val routeConfig = if (currentDeviceType) {
            RouterConfig.IR_THERMAL_07
        } else {
            RouterConfig.IR_THERMAL
        }
        _navigationEvent.value = NavigationEvent.ToThermal(routeConfig)
    }
    private fun updateFragmentCommunication(activeFragment: Int) {
        val currentDeviceState = _deviceState.value ?: DeviceState()
        val communicationState = FragmentCommunicationState(
            activeFragment = activeFragment,
            deviceConnected = currentDeviceState.isWebSocketConnected || currentDeviceState.isUsbConnected
        )
        _fragmentCommunication.value = communicationState
    }
    // Guide dialog management
    fun handleGuideDialog(onGuideShow: (Int, Int) -> Unit) {
        val currentStep = SharedManager.homeGuideStep
        if (currentStep == 0) return
        val navigationTarget = when (currentStep) {
            1 -> 0
            2 -> 4
            3 -> 2
            else -> 2
        }
        onGuideShow(currentStep, navigationTarget)
    }
    fun handleGuideNavigation(step: Int) {
        SharedManager.homeGuideStep = when (step) {
            1 -> 2
            2 -> 3
            3 -> 0
            else -> 0
        }
    }
    fun completeGuide() {
        SharedManager.homeGuideStep = 0
    }
}