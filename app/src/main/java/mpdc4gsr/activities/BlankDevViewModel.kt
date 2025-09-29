package mpdc4gsr.activities

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for USB Device Handler Compose Activity
 * Manages USB device detection, connection, and permissions
 */
class BlankDevViewModel(
    private val application: Application
) : BaseViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val hasUsbPermission: Boolean = false,
        val connectedDevices: List<UsbDeviceInfo> = emptyList(),
        val availableDevices: List<UsbDeviceInfo> = emptyList(),
        val autoNavigateToMain: Boolean = true,
        val navigationCountdown: Int? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val usbManager = application.getSystemService(Context.USB_SERVICE) as UsbManager
    private var countdownJob: Job? = null

    companion object {
        private const val ACTION_USB_PERMISSION = "mpdc4gsr.USB_PERMISSION"
        private const val AUTO_NAVIGATE_DELAY = 10 // seconds
    }

    init {
        refreshUsbDevices()
        startAutoNavigationCountdown()
    }

    /**
     * Refresh USB devices list
     */
    fun refreshUsbDevices() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val deviceList = usbManager.deviceList
                val connectedDevices = mutableListOf<UsbDeviceInfo>()
                val availableDevices = mutableListOf<UsbDeviceInfo>()
                
                deviceList.values.forEach { usbDevice ->
                    val deviceInfo = UsbDeviceInfo(
                        name = usbDevice.deviceName ?: "Unknown Device",
                        productId = usbDevice.productId,
                        vendorId = usbDevice.vendorId,
                        deviceType = determineDeviceType(usbDevice),
                        hasPermission = usbManager.hasPermission(usbDevice)
                    )
                    
                    if (deviceInfo.hasPermission) {
                        connectedDevices.add(deviceInfo)
                    } else {
                        availableDevices.add(deviceInfo)
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    connectedDevices = connectedDevices,
                    availableDevices = availableDevices,
                    hasUsbPermission = connectedDevices.isNotEmpty()
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to refresh USB devices: ${e.message}"
                )
            }
        }
    }

    /**
     * Request USB permissions for all available devices
     */
    fun requestUsbPermissions() {
        viewModelScope.launch {
            try {
                val deviceList = usbManager.deviceList
                deviceList.values.forEach { usbDevice ->
                    if (!usbManager.hasPermission(usbDevice)) {
                        requestPermissionForUsbDevice(usbDevice)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to request USB permissions: ${e.message}"
                )
            }
        }
    }

    /**
     * Request permission for a specific device
     */
    fun requestPermissionForDevice(device: UsbDeviceInfo) {
        viewModelScope.launch {
            try {
                val deviceList = usbManager.deviceList
                val usbDevice = deviceList.values.find { 
                    it.productId == device.productId && it.vendorId == device.vendorId 
                }
                
                usbDevice?.let { requestPermissionForUsbDevice(it) }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to request permission for device: ${e.message}"
                )
            }
        }
    }

    /**
     * Connect to a device (request permission if needed)
     */
    fun connectDevice(device: UsbDeviceInfo) {
        requestPermissionForDevice(device)
    }

    /**
     * Disconnect a device (revoke permission)
     */
    fun disconnectDevice(device: UsbDeviceInfo) {
        viewModelScope.launch {
            try {
                // For USB devices, we can't actually "disconnect" them,
                // but we can stop using them and refresh the list
                refreshUsbDevices()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to disconnect device: ${e.message}"
                )
            }
        }
    }

    /**
     * Handle USB device attached event
     */
    fun handleUsbDeviceAttached(device: UsbDevice) {
        viewModelScope.launch {
            refreshUsbDevices()
            
            // Automatically request permission for known device types
            if (isKnownThermalDevice(device)) {
                requestPermissionForUsbDevice(device)
            }
        }
    }

    /**
     * Handle USB device detached event
     */
    fun handleUsbDeviceDetached(device: UsbDevice) {
        viewModelScope.launch {
            refreshUsbDevices()
        }
    }

    /**
     * Handle USB permission result
     */
    fun handleUsbPermissionResult(device: UsbDevice, granted: Boolean) {
        viewModelScope.launch {
            if (granted) {
                _uiState.value = _uiState.value.copy(
                    error = "USB permission granted for ${device.deviceName ?: "device"}"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    error = "USB permission denied for ${device.deviceName ?: "device"}"
                )
            }
            
            refreshUsbDevices()
        }
    }

    /**
     * Toggle auto-navigation to main
     */
    fun toggleAutoNavigation() {
        val newAutoNavigate = !_uiState.value.autoNavigateToMain
        _uiState.value = _uiState.value.copy(autoNavigateToMain = newAutoNavigate)
        
        if (newAutoNavigate) {
            startAutoNavigationCountdown()
        } else {
            stopAutoNavigationCountdown()
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Start auto-navigation countdown
     */
    private fun startAutoNavigationCountdown() {
        if (!_uiState.value.autoNavigateToMain) return
        
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (i in AUTO_NAVIGATE_DELAY downTo 1) {
                _uiState.value = _uiState.value.copy(navigationCountdown = i)
                delay(1000)
            }
            
            // Auto-navigate would happen here in a real implementation
            // For now, just clear the countdown
            _uiState.value = _uiState.value.copy(navigationCountdown = null)
        }
    }

    /**
     * Stop auto-navigation countdown
     */
    private fun stopAutoNavigationCountdown() {
        countdownJob?.cancel()
        _uiState.value = _uiState.value.copy(navigationCountdown = null)
    }

    /**
     * Request permission for a USB device
     */
    private fun requestPermissionForUsbDevice(device: UsbDevice) {
        val context = application.applicationContext
        val permissionIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        usbManager.requestPermission(device, permissionIntent)
    }

    /**
     * Determine device type based on vendor/product IDs
     */
    private fun determineDeviceType(device: UsbDevice): UsbDeviceType {
        return when {
            isKnownThermalDevice(device) -> UsbDeviceType.CAMERA
            isKnownSensorDevice(device) -> UsbDeviceType.SENSOR
            else -> UsbDeviceType.UNKNOWN
        }
    }

    /**
     * Check if device is a known thermal camera
     */
    private fun isKnownThermalDevice(device: UsbDevice): Boolean {
        // Add known thermal camera vendor/product IDs
        val knownThermalDevices = listOf(
            Pair(0x289D, 0x0010), // Example Topdon TC001
            Pair(0x289D, 0x0011), // Example Topdon variant
            // Add more known thermal camera IDs
        )
        
        return knownThermalDevices.any { (vendorId, productId) ->
            device.vendorId == vendorId && device.productId == productId
        }
    }

    /**
     * Check if device is a known sensor
     */
    private fun isKnownSensorDevice(device: UsbDevice): Boolean {
        // Add known sensor device vendor/product IDs
        val knownSensorDevices = listOf(
            // Add known sensor device IDs if any use USB
        )
        
        return knownSensorDevices.any { (vendorId, productId) ->
            device.vendorId == vendorId && device.productId == productId
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}