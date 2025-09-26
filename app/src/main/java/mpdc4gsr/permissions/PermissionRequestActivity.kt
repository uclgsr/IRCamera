package mpdc4gsr.permissions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


class PermissionRequestActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "PermissionRequestActivity"

        fun start(context: Context) {
            val intent = Intent(context, PermissionRequestActivity::class.java)
            context.startActivity(intent)
        }
    }

    private lateinit var permissionController: PermissionController
    private lateinit var enhancedPermissionManager: EnhancedPermissionManager


    private lateinit var statusText: TextView
    private lateinit var logText: TextView


    private lateinit var cameraStatusIcon: TextView
    private lateinit var bluetoothStatusIcon: TextView
    private lateinit var locationStatusIcon: TextView
    private lateinit var storageStatusIcon: TextView
    private lateinit var usbStatusIcon: TextView


    private lateinit var requestCameraBtn: Button
    private lateinit var requestBluetoothBtn: Button
    private lateinit var requestAllBtn: Button
    private lateinit var startRecordingBtn: Button
    private lateinit var clearLogsBtn: Button


    private val requestMultiplePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        handlePermissionResults(permissions)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupUI()
        initializePermissionSystem()
        updatePermissionStatus()
    }

    private fun setupUI() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }


        val title = TextView(this).apply {
            text = "Enhanced Permission Request System"
            textSize = 20f
            setPadding(0, 0, 0, 16)
        }
        layout.addView(title)


        statusText = TextView(this).apply {
            text = "Checking permissions..."
            textSize = 16f
            setPadding(0, 0, 0, 16)
        }
        layout.addView(statusText)


        val permissionGrid = createPermissionStatusGrid()
        layout.addView(permissionGrid)


        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        requestCameraBtn = Button(this).apply {
            text = "Camera"
            setOnClickListener { requestCameraPermissions() }
        }
        buttonLayout.addView(requestCameraBtn)

        requestBluetoothBtn = Button(this).apply {
            text = "Bluetooth"
            setOnClickListener { requestBluetoothPermissions() }
        }
        buttonLayout.addView(requestBluetoothBtn)

        requestAllBtn = Button(this).apply {
            text = "Request All"
            setOnClickListener { requestAllPermissions() }
        }
        buttonLayout.addView(requestAllBtn)

        layout.addView(buttonLayout)


        val secondaryLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        startRecordingBtn = Button(this).apply {
            text = "Test Recording"
            setOnClickListener { testRecordingCapabilities() }
            isEnabled = false
        }
        secondaryLayout.addView(startRecordingBtn)

        clearLogsBtn = Button(this).apply {
            text = "Clear Logs"
            setOnClickListener { clearLogs() }
        }
        secondaryLayout.addView(clearLogsBtn)

        layout.addView(secondaryLayout)


        logText = TextView(this).apply {
            text = "Permission request logs will appear here...\n"
            textSize = 12f
            setPadding(0, 16, 0, 0)
            setTextIsSelectable(true)
        }

        val scrollView = ScrollView(this).apply {
            addView(logText)
        }
        layout.addView(scrollView)

        setContentView(layout)
    }

    private fun createPermissionStatusGrid(): LinearLayout {
        val grid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8, 0, 16)
        }


        val cameraRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        cameraRow.addView(TextView(this).apply {
            text = "Camera & Audio:"
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        cameraStatusIcon = TextView(this).apply {
            text = "❓"
            textSize = 18f
        }
        cameraRow.addView(cameraStatusIcon)
        grid.addView(cameraRow)


        val bluetoothRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        bluetoothRow.addView(TextView(this).apply {
            text = "Bluetooth:"
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        bluetoothStatusIcon = TextView(this).apply {
            text = "❓"
            textSize = 18f
        }
        bluetoothRow.addView(bluetoothStatusIcon)
        grid.addView(bluetoothRow)


        val locationRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        locationRow.addView(TextView(this).apply {
            text = "Location (for BLE):"
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        locationStatusIcon = TextView(this).apply {
            text = "❓"
            textSize = 18f
        }
        locationRow.addView(locationStatusIcon)
        grid.addView(locationRow)


        val storageRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        storageRow.addView(TextView(this).apply {
            text = "Storage:"
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        storageStatusIcon = TextView(this).apply {
            text = "❓"
            textSize = 18f
        }
        storageRow.addView(storageStatusIcon)
        grid.addView(storageRow)


        val usbRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        usbRow.addView(TextView(this).apply {
            text = "USB (Thermal Camera):"
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        usbStatusIcon = TextView(this).apply {
            text = "❓"
            textSize = 18f
        }
        usbRow.addView(usbStatusIcon)
        grid.addView(usbRow)

        return grid
    }

    private fun initializePermissionSystem() {
        permissionController = PermissionController(this)
        permissionController.initialize()

        enhancedPermissionManager = EnhancedPermissionManager(this, permissionController)

        addLog("Enhanced Permission System initialized")
        addLog("This system addresses permission request gaps mentioned in the issue")
    }

    private fun updatePermissionStatus() {

        val hasCameraPermission = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        val hasAudioPermission = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        cameraStatusIcon.text = if (hasCameraPermission && hasAudioPermission) "✅" else "❌"


        val hasBluetoothPermissions = permissionController.hasBluetoothPermissions()
        bluetoothStatusIcon.text = if (hasBluetoothPermissions) "✅" else "❌"


        val hasLocationPermissions = permissionController.hasLocationPermission()
        locationStatusIcon.text = if (hasLocationPermissions) "✅" else "❌"


        val hasStoragePermissions = permissionController.hasStoragePermissions()
        storageStatusIcon.text = if (hasStoragePermissions) "✅" else "❌"


        val hasUsbPermissions = permissionController.hasUsbPermissions()
        usbStatusIcon.text = if (hasUsbPermissions) "✅" else "⚠️"


        val allCriticalGranted =
            (hasCameraPermission && hasAudioPermission) && hasBluetoothPermissions && hasLocationPermissions

        statusText.text = if (allCriticalGranted) {
            "✅ All critical permissions granted - Ready for recording"
        } else {
            "⚠️ Some permissions missing - Click buttons below to request"
        }

        startRecordingBtn.isEnabled = allCriticalGranted

        addLog(
            "Permission status updated: Camera=${if (hasCameraPermission && hasAudioPermission) "OK" else "Missing"}, " +
                    "Bluetooth=${if (hasBluetoothPermissions) "OK" else "Missing"}, " +
                    "Location=${if (hasLocationPermissions) "OK" else "Missing"}"
        )
    }

    private fun requestCameraPermissions() {
        addLog("🎥 Requesting camera permissions with enhanced UI guidance...")

        lifecycleScope.launch {
            try {
                val granted = enhancedPermissionManager.requestCameraPermissions()
                if (granted) {
                    addLog("✅ Camera permissions granted successfully")
                } else {
                    addLog("❌ Camera permissions denied - RGB recording disabled")
                }
                updatePermissionStatus()
            } catch (e: Exception) {
                addLog("❌ Error requesting camera permissions: ${e.message}")
                Log.e(TAG, "Error requesting camera permissions", e)
            }
        }
    }

    private fun requestBluetoothPermissions() {
        addLog("📡 Requesting Bluetooth permissions with enhanced UI guidance...")

        lifecycleScope.launch {
            try {
                val granted = enhancedPermissionManager.requestBluetoothPermissions()
                if (granted) {
                    addLog("✅ Bluetooth permissions granted successfully")
                } else {
                    addLog("❌ Bluetooth permissions denied - GSR connectivity disabled")
                }
                updatePermissionStatus()
            } catch (e: Exception) {
                addLog("❌ Error requesting Bluetooth permissions: ${e.message}")
                Log.e(TAG, "Error requesting Bluetooth permissions", e)
            }
        }
    }

    private fun requestAllPermissions() {
        addLog("🔄 Starting comprehensive permission request sequence...")

        lifecycleScope.launch {
            try {
                val granted = enhancedPermissionManager.requestAllCriticalPermissions()
                if (granted) {
                    addLog("✅ Critical permissions granted - App ready for use")
                } else {
                    addLog("⚠️ Some permissions denied - Limited functionality available")
                }
                updatePermissionStatus()
            } catch (e: Exception) {
                addLog("❌ Error in permission request sequence: ${e.message}")
                Log.e(TAG, "Error in permission request sequence", e)
            }
        }
    }

    private fun testRecordingCapabilities() {
        addLog("🧪 Testing recording capabilities with current permissions...")

        val hasCameraPermission = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        val hasBluetoothPermissions = permissionController.hasBluetoothPermissions()

        val capabilities = mutableListOf<String>()

        if (hasCameraPermission) {
            capabilities.add("✅ RGB Camera Recording (4K@60fps)")
        } else {
            capabilities.add("❌ RGB Camera Recording - Permission denied")
        }

        if (hasBluetoothPermissions) {
            capabilities.add("✅ Shimmer GSR Sensor connectivity")
        } else {
            capabilities.add("❌ GSR Sensor connectivity - Permission denied")
        }


        capabilities.add("⚠️ Thermal Camera - USB permission required when device connected")

        addLog("📊 Current Recording Capabilities:")
        capabilities.forEach { capability ->
            addLog("  $capability")
        }

        if (hasCameraPermission || hasBluetoothPermissions) {
            addLog("✅ Sufficient permissions for basic recording functionality")
        } else {
            addLog("❌ Insufficient permissions - Please grant Camera or Bluetooth permissions")
        }
    }

    private fun clearLogs() {
        logText.text = "Permission request logs cleared...\n"
    }

    private fun addLog(message: String) {
        runOnUiThread {
            val timestamp =
                java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                    .format(java.util.Date())
            logText.append("[$timestamp] $message\n")


            (logText.parent as? ScrollView)?.post {
                (logText.parent as ScrollView).fullScroll(ScrollView.FOCUS_DOWN)
            }
        }

        Log.i(TAG, message)
    }

    private fun handlePermissionResults(permissions: Map<String, Boolean>) {
        addLog("📋 Permission results received:")

        permissions.forEach { (permission, granted) ->
            val permissionName = permission.substringAfterLast(".")
            val status = if (granted) "✅ Granted" else "❌ Denied"
            addLog("  $permissionName: $status")
        }

        updatePermissionStatus()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)


        permissionController.onRequestPermissionsResult(requestCode, permissions, grantResults)


        updatePermissionStatus()
    }

    override fun onResume() {
        super.onResume()

        updatePermissionStatus()
    }
}