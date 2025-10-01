package mpdc4gsr.feature.settings.presentation

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mpdc4gsr.core.data.ShimmerDeviceManager
import mpdc4gsr.core.ui.BaseViewModel

/**
 * Network Settings ViewModel - MVVM Integration
 * Manages WiFi, Bluetooth, and device pairing with existing ShimmerDeviceManager
 */
class NetworkSettingsViewModel : BaseViewModel() {

    private lateinit var prefs: SharedPreferences
    private lateinit var context: Context
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var wifiManager: WifiManager? = null
    private var shimmerDeviceManager: ShimmerDeviceManager? = null

    private val _networkSettings = MutableStateFlow(NetworkSettings())
    val networkSettings: StateFlow<NetworkSettings> = _networkSettings.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val pairedDevices: StateFlow<List<DeviceInfo>> = _pairedDevices.asStateFlow()

    private val _networkInfo = MutableStateFlow(NetworkInfo())
    val networkInfo: StateFlow<NetworkInfo> = _networkInfo.asStateFlow()

    data class NetworkSettings(
        val wifiEnabled: Boolean = true,
        val bluetoothEnabled: Boolean = true,
        val autoConnect: Boolean = true
    )

    data class DeviceInfo(
        val name: String,
        val address: String,
        val type: DeviceType,
        val isConnected: Boolean
    )

    enum class DeviceType {
        SHIMMER_GSR, TOPDON_THERMAL, UNKNOWN
    }

    data class NetworkInfo(
        val wifiNetwork: String = "Not Connected",
        val ipAddress: String = "N/A",
        val bluetoothStatus: String = "Unknown"
    )

    companion object {
        private const val KEY_WIFI_ENABLED = "network_wifi_enabled"
        private const val KEY_BLUETOOTH_ENABLED = "network_bluetooth_enabled"
        private const val KEY_AUTO_CONNECT = "network_auto_connect"
    }

    fun initialize(ctx: Context) {
        context = ctx
        prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
        
        val bluetoothManager = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter
        wifiManager = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        
// TODO: Instantiate ShimmerDeviceManager in the Activity/Fragment, as it requires a
// LifecycleOwner. The instance should then be passed to this ViewModel, for example
// via the initialize() method.
        
        loadSettings()
        updateNetworkInfo()
        loadPairedDevices()
    }

    private fun loadSettings() {
        _networkSettings.value = NetworkSettings(
            wifiEnabled = wifiManager?.isWifiEnabled ?: true,
            bluetoothEnabled = bluetoothAdapter?.isEnabled ?: true,
            autoConnect = prefs.getBoolean(KEY_AUTO_CONNECT, true)
        )
    }

    private fun updateNetworkInfo() {
        viewModelScope.launch {
            val wifiInfo = wifiManager?.connectionInfo
            val ipAddress = wifiInfo?.ipAddress?.let {
                String.format(
                    "%d.%d.%d.%d",
                    (it and 0xff),
                    (it shr 8 and 0xff),
                    (it shr 16 and 0xff),
                    (it shr 24 and 0xff)
                )
            } ?: "N/A"

            _networkInfo.value = NetworkInfo(
                wifiNetwork = wifiInfo?.ssid?.replace("\"", "") ?: "Not Connected",
                ipAddress = ipAddress,
                bluetoothStatus = if (bluetoothAdapter?.isEnabled == true) "Enabled" else "Disabled"
            )
        }
    }

    private fun loadPairedDevices() {
        viewModelScope.launch {
            val devices = mutableListOf<DeviceInfo>()
            
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) 
                == PackageManager.PERMISSION_GRANTED) {
                bluetoothAdapter?.bondedDevices?.forEach { device ->
                    // Identify device type by checking for known device name patterns
                    // Note: This is a heuristic approach. For more robust detection,
                    // consider scanning for specific Bluetooth service UUIDs if available
                    val deviceType = when {
                        device.name?.contains("Shimmer", ignoreCase = true) == true -> DeviceType.SHIMMER_GSR
                        device.name?.contains("Topdon", ignoreCase = true) == true || 
                        device.name?.contains("TC001", ignoreCase = true) == true -> DeviceType.TOPDON_THERMAL
                        else -> DeviceType.UNKNOWN
                    }
                    devices.add(
                        DeviceInfo(
                            name = device.name ?: "Unknown Device",
                            address = device.address,
                            type = deviceType,
                            isConnected = false
                        )
                    )
                }
            }
            
            _pairedDevices.value = devices
        }
    }

    fun updateAutoConnect(enabled: Boolean) {
        viewModelScope.launch {
            prefs.edit().putBoolean(KEY_AUTO_CONNECT, enabled).apply()
            _networkSettings.value = _networkSettings.value.copy(autoConnect = enabled)
        }
    }

    fun scanForDevices() {
        viewModelScope.launch {
            loadPairedDevices()
            updateNetworkInfo()
        }
    }

    /**
     * Refreshes WiFi network information.
     * Note: Direct WiFi control (enable/disable) requires system-level permissions
     * in Android 10+ and is not available to regular applications.
     */
    fun refreshWifiInfo() {
        updateNetworkInfo()
    }

    /**
     * Refreshes Bluetooth status information.
     * Note: Direct Bluetooth control (enable/disable) requires BLUETOOTH_ADMIN permission
     * and user interaction via system dialogs in modern Android versions.
     */
    fun refreshBluetoothInfo() {
        updateNetworkInfo()
    }
}
