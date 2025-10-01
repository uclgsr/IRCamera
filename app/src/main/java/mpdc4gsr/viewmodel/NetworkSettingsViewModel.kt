package mpdc4gsr.viewmodel

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
import mpdc4gsr.sensors.gsr.ShimmerDeviceManager

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
        
        try {
            shimmerDeviceManager = ShimmerDeviceManager.getInstance(ctx)
        } catch (e: Exception) {
            // ShimmerDeviceManager may not be available
        }
        
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

    fun toggleWifi(enabled: Boolean) {
        // Note: Direct WiFi control requires system permissions in Android 10+
        updateNetworkInfo()
    }

    fun toggleBluetooth(enabled: Boolean) {
        // Note: Direct Bluetooth control requires permissions
        updateNetworkInfo()
    }
}
