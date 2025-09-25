package mpdc4gsr.test

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import mpdc4gsr.permissions.PermissionController
import mpdc4gsr.sensors.unified.ShimmerDeviceManager
import mpdc4gsr.sensors.unified.UnifiedGSRRecorder
import mpdc4gsr.sensors.unified.model.DeviceInfo


class BLEIntegrationTestActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "BLEIntegrationTest"
    }

    private lateinit var statusText: TextView
    private lateinit var logText: TextView
    private lateinit var testButton: Button
    private lateinit var clearLogsButton: Button

    private lateinit var permissionController: PermissionController
    private var gsrRecorder: UnifiedGSRRecorder? = null
    private var deviceManager: ShimmerDeviceManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        statusText = TextView(this).apply {
            text = "BLE Integration Test Ready"
            textSize = 16f
            setPadding(0, 0, 0, 16)
        }

        testButton = Button(this).apply {
            text = "Start Enhanced BLE Test"
            setOnClickListener { startBLEIntegrationTest() }
        }

        clearLogsButton = Button(this).apply {
            text = "Clear Logs"
            setOnClickListener { clearLogs() }
        }

        logText = TextView(this).apply {
            text = "Test logs will appear here...\n"
            textSize = 12f
            setPadding(0, 16, 0, 0)
            setTextIsSelectable(true)
        }

        val scrollView = ScrollView(this).apply {
            addView(logText)
        }

        layout.addView(statusText)
        layout.addView(testButton)
        layout.addView(clearLogsButton)
        layout.addView(scrollView)

        setContentView(layout)

        permissionController = PermissionController(this)

        addLog("BLE Integration Test Activity started")
        addLog("Enhanced Shimmer BLE scanning validation ready")
    }

    private fun startBLEIntegrationTest() {
        addLog("\n=== Starting Enhanced BLE Integration Test ===")
        statusText.text = "Running BLE Integration Test..."
        testButton.isEnabled = false

        lifecycleScope.launch {
            try {

                addLog("Test 1: Initializing UnifiedGSRRecorder with enhanced BLE scanning...")

                gsrRecorder = UnifiedGSRRecorder(this@BLEIntegrationTestActivity, this@BLEIntegrationTestActivity)
                val initSuccess = gsrRecorder?.initialize() ?: false

                if (initSuccess) {
                    addLog("✅ UnifiedGSRRecorder initialized successfully")
                } else {
                    addLog("❌ UnifiedGSRRecorder initialization failed")
                    return@launch
                }


                addLog("Test 2: Testing enhanced BLE device discovery...")

                val discoverySuccess = gsrRecorder?.startDeviceDiscovery() ?: false

                if (discoverySuccess) {
                    val discoveredDevices = gsrRecorder?.getDiscoveredDevices() ?: emptyList()
                    addLog("✅ Device discovery completed: found ${discoveredDevices.size} devices")

                    discoveredDevices.forEach { device ->
                        addLog("  - ${device.name} (${device.address}) RSSI: ${device.rssi}dBm")
                    }
                } else {
                    addLog("❌ Device discovery failed")
                }


                addLog("Test 3: Testing ShimmerDeviceManager directly...")

                deviceManager = ShimmerDeviceManager(this@BLEIntegrationTestActivity, this@BLEIntegrationTestActivity)
                val dmInitSuccess = deviceManager?.initialize() ?: false

                if (dmInitSuccess) {
                    addLog("✅ ShimmerDeviceManager initialized successfully")

                    val scanSuccess = deviceManager?.startDeviceScanning() ?: false
                    if (scanSuccess) {
                        addLog("✅ Enhanced BLE scanning started successfully")


                        kotlinx.coroutines.delay(5000)

                        // Get the latest scan results safely
                        val scanResults = try {
                            deviceManager?.scanResults?.first() ?: emptyList()
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to collect scan results: ${e.message}")
                            emptyList<DeviceInfo>()
                        }
                        
                        addLog("✅ BLE scan results: ${scanResults.size} devices found")

                        deviceManager?.stopDeviceScanning()
                        addLog("✅ BLE scanning stopped")
                    } else {
                        addLog("❌ Enhanced BLE scanning failed to start")
                    }
                } else {
                    addLog("❌ ShimmerDeviceManager initialization failed")
                }


                addLog("Test 4: Validating BLE permissions...")

                val hasPermissions = UnifiedGSRRecorder.hasRequiredPermissions(this@BLEIntegrationTestActivity)
                if (hasPermissions) {
                    addLog("✅ All required BLE permissions are granted")
                } else {
                    addLog("⚠️ Some BLE permissions are missing")
                    val requiredPerms = UnifiedGSRRecorder.getRequiredPermissions()
                    addLog("Required permissions: ${requiredPerms.joinToString(", ")}")
                }

                addLog("\n=== BLE Integration Test Complete ===")
                statusText.text = "BLE Integration Test Complete - Check logs"

            } catch (e: Exception) {
                addLog("❌ Test failed with exception: ${e.message}")
                Log.e(TAG, "BLE integration test failed", e)
                statusText.text = "Test Failed - Check logs"
            } finally {
                testButton.isEnabled = true


                try {
                    gsrRecorder?.cleanup()
                    deviceManager?.release()
                } catch (e: Exception) {
                    addLog("Warning: Cleanup failed: ${e.message}")
                }
            }
        }
    }

    private fun addLog(message: String) {
        runOnUiThread {
            val timestamp =
                java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            logText.append("[$timestamp] $message\n")


            (logText.parent as? ScrollView)?.post {
                (logText.parent as ScrollView).fullScroll(ScrollView.FOCUS_DOWN)
            }
        }


        Log.i(TAG, message)
    }

    private fun clearLogs() {
        logText.text = "Test logs cleared...\n"
        statusText.text = "BLE Integration Test Ready"
    }

    override fun onDestroy() {
        super.onDestroy()


        lifecycleScope.launch {
            try {
                gsrRecorder?.cleanup()
                deviceManager?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error during activity cleanup", e)
            }
        }
    }
}