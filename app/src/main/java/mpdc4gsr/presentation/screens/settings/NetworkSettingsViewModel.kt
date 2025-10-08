package mpdc4gsr.presentation.screens.settings

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.core.data.ShimmerDeviceManager
import javax.inject.Inject

@HiltViewModel
class NetworkSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
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

    init {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter
        wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
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

    @Suppress("DEPRECATION")
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
                == PackageManager.PERMISSION_GRANTED
            ) {
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

    fun refreshWifiInfo() {
        updateNetworkInfo()
    }

    fun refreshBluetoothInfo() {
        updateNetworkInfo()
    }
}
