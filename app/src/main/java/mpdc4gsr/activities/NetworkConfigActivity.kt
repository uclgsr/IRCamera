package mpdc4gsr.activities

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import kotlinx.coroutines.launch
import mpdc4gsr.network.NetworkSettings
import mpdc4gsr.permissions.PermissionController
import mpdc4gsr.permissions.PermissionManager

/**
 * Activity for configuring network connection settings
 */
class NetworkConfigActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "NetworkConfigActivity"
        private const val REQUEST_ENABLE_BT = 1
    }

    private lateinit var networkSettings: NetworkSettings
    private lateinit var permissionManager: PermissionManager
    private var bluetoothAdapter: BluetoothAdapter? = null

    private val bluetoothEnableResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            showBluetoothDevices()
        } else {
            Toast.makeText(this, "Bluetooth is required for device connection", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Temporary - will create proper layout

        networkSettings = NetworkSettings(this)
        permissionManager = PermissionManager(this, PermissionController(this))
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter

        setupUI()
    }

    private fun setupUI() {
        title = "Network Configuration"

        // For now, show current settings in Toast (will replace with proper UI)
        val summary = networkSettings.getConnectionSummary()
        Toast.makeText(this, "Current settings: $summary", Toast.LENGTH_LONG).show()

        Log.i(TAG, "Network configuration activity opened")

        // Test Bluetooth device discovery
        testBluetoothConfiguration()
    }

    private fun testBluetoothConfiguration() {
        lifecycleScope.launch {
            try {
                val hasPermission = permissionManager.requestBluetoothPermissions()
                if (hasPermission) {
                    Log.i(TAG, "Bluetooth permissions granted")
                    showBluetoothDevices()
                } else {
                    Log.w(TAG, "Bluetooth permissions denied")
                    Toast.makeText(
                        this@NetworkConfigActivity,
                        "Bluetooth permissions required for device connection",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error requesting Bluetooth permissions", e)
            }
        }
    }

    private fun showBluetoothDevices() {
        val adapter = bluetoothAdapter
        if (adapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
            return
        }

        if (!adapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothEnableResult.launch(enableBtIntent)
            return
        }

        try {
            val pairedDevices: Set<BluetoothDevice> = adapter.bondedDevices
            val deviceList = pairedDevices.map { "${it.name} (${it.address})" }

            if (deviceList.isNotEmpty()) {
                val message = "Available Bluetooth devices:\n${deviceList.joinToString("\n")}"
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                Log.i(TAG, "Found ${deviceList.size} paired Bluetooth devices")

                // For demo purposes, save the first device
                val firstDevice = pairedDevices.first()
                lifecycleScope.launch {
                    networkSettings.saveBluetoothDevice(firstDevice)
                    networkSettings.preferredConnectionType =
                        NetworkSettings.ConnectionType.BLUETOOTH_RFCOMM
                    Log.i(TAG, "Saved Bluetooth device: ${firstDevice.name}")
                }
            } else {
                Toast.makeText(this, "No paired Bluetooth devices found", Toast.LENGTH_SHORT).show()
                Log.w(TAG, "No paired Bluetooth devices available")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception accessing Bluetooth devices", e)
            Toast.makeText(
                this,
                "Permission denied accessing Bluetooth devices",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error accessing Bluetooth devices", e)
            Toast.makeText(this, "Error accessing Bluetooth devices", Toast.LENGTH_SHORT).show()
        }
    }

    private fun configureWifiSettings() {
        // Configure Wi-Fi settings
        networkSettings.pcIpAddress = NetworkSettings.DEFAULT_PC_IP
        networkSettings.pcPort = NetworkSettings.DEFAULT_PC_PORT
        networkSettings.preferredConnectionType = NetworkSettings.ConnectionType.WIFI_TCP

        val message = "Wi-Fi configured: ${networkSettings.pcIpAddress}:${networkSettings.pcPort}"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        Log.i(TAG, "Wi-Fi settings configured")
    }

    fun getNetworkSettings(): NetworkSettings = networkSettings
}