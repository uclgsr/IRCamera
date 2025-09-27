package mpdc4gsr.permissions

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PermissionRequestActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "PermissionRequestActivity"
        fun start(context: Context) {
            context.startActivity(Intent(context, PermissionRequestActivity::class.java))
        }
    }

    private lateinit var permissionController: PermissionController
    private lateinit var permissionManager: PermissionManager

    // UI Components
    private lateinit var statusText: TextView
    private lateinit var logText: TextView
    private lateinit var cameraStatusIcon: TextView
    private lateinit var bluetoothStatusIcon: TextView
    private lateinit var locationStatusIcon: TextView
    private lateinit var storageStatusIcon: TextView
    private lateinit var usbStatusIcon: TextView
    private lateinit var startRecordingBtn: Button
    private lateinit var logScrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()
        initializePermissionSystem()
        updatePermissionStatus()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun initializePermissionSystem() {
        permissionController = PermissionController(this)
        permissionManager = PermissionManager(this, permissionController)
        addLog("Permission System initialised.")
    }

    private fun updatePermissionStatus() {
        cameraStatusIcon.text = if (permissionController.hasCameraPermissions()) "OK" else "Needed"
        bluetoothStatusIcon.text =
            if (permissionController.hasBluetoothPermissions()) "OK" else "Needed"
        locationStatusIcon.text =
            if (permissionController.hasLocationPermission()) "OK" else "Needed"
        storageStatusIcon.text =
            if (permissionController.hasStoragePermissions()) "OK" else "Needed"
        usbStatusIcon.text = if (permissionController.hasUsbPermissions()) "Ready" else "N/A"

        val allCriticalGranted =
            permissionController.canStartRecording() && permissionController.canConnectToShimmer()
        statusText.text = if (allCriticalGranted) {
            "All critical permissions granted"
        } else {
            "Some permissions missing"
        }
        startRecordingBtn.isEnabled = allCriticalGranted
        addLog("Permission status updated.")
    }

    private fun requestCameraPermissions() {
        addLog("Requesting camera permissions...")
        lifecycleScope.launch {
            val granted = permissionManager.requestCameraPermissions()
            addLog(if (granted) "Camera permissions granted" else "Camera permissions denied")
            updatePermissionStatus()
        }
    }

    private fun requestBluetoothPermissions() {
        addLog("Requesting Bluetooth permissions...")
        lifecycleScope.launch {
            val granted = permissionManager.requestBluetoothPermissions()
            addLog(if (granted) "Bluetooth permissions granted" else "Bluetooth permissions denied")
            updatePermissionStatus()
        }
    }

    private fun requestAllPermissions() {
        addLog("Starting comprehensive permission request...")
        lifecycleScope.launch {
            val granted = permissionManager.requestAllCriticalPermissions()
            addLog(if (granted) "Critical permissions granted" else "Some permissions were denied")
            updatePermissionStatus()
        }
    }

    private fun testRecordingCapabilities() {
        addLog("Testing recording capabilities...")
        addLog("Status: ${permissionController.getPermissionStatusMessage()}")
    }

    private fun addLog(message: String) {
        runOnUiThread {
            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            logText.append("[$timestamp] $message\n")
            logScrollView.post { logScrollView.fullScroll(ScrollView.FOCUS_DOWN) }
        }
        Log.i(TAG, message)
    }

    // --- UI Setup ---
    private fun setupUI() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        val title = TextView(this).apply {
            text = "Enhanced Permission Request System"; textSize = 20f; setPadding(0, 0, 0, 16)
        }
        statusText = TextView(this).apply {
            text = "Checking permissions..."; textSize = 16f; setPadding(
            0,
            0,
            0,
            16
        )
        }
        val buttonLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val requestCameraBtn = Button(this).apply {
            text = "Camera"; setOnClickListener { requestCameraPermissions() }
        }
        val requestBluetoothBtn = Button(this).apply {
            text = "Bluetooth"; setOnClickListener { requestBluetoothPermissions() }
        }
        val requestAllBtn = Button(this).apply {
            text = "Request All"; setOnClickListener { requestAllPermissions() }
        }
        buttonLayout.addView(requestCameraBtn)
        buttonLayout.addView(requestBluetoothBtn)
        buttonLayout.addView(requestAllBtn)
        val secondaryLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        startRecordingBtn = Button(this).apply {
            text = "Test Recording"; setOnClickListener { testRecordingCapabilities() }; isEnabled =
            false
        }
        val clearLogsBtn =
            Button(this).apply { text = "Clear Logs"; setOnClickListener { logText.text = "" } }
        secondaryLayout.addView(startRecordingBtn)
        secondaryLayout.addView(clearLogsBtn)
        logText = TextView(this).apply {
            text = "Logs...\n"; textSize = 12f; setPadding(
            0,
            16,
            0,
            0
        ); setTextIsSelectable(true)
        }
        logScrollView = ScrollView(this).apply { addView(logText) }
        layout.addView(title)
        layout.addView(statusText)
        layout.addView(createPermissionStatusGrid())
        layout.addView(buttonLayout)
        layout.addView(secondaryLayout)
        layout.addView(logScrollView)
        setContentView(layout)
    }

    private fun createPermissionStatusGrid(): LinearLayout {
        val grid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(
            0,
            8,
            0,
            16
        )
        }
        cameraStatusIcon = TextView(this)
        bluetoothStatusIcon = TextView(this)
        locationStatusIcon = TextView(this)
        storageStatusIcon = TextView(this)
        usbStatusIcon = TextView(this)
        grid.addView(createStatusRow("Camera & Audio:", cameraStatusIcon))
        grid.addView(createStatusRow("Bluetooth:", bluetoothStatusIcon))
        grid.addView(createStatusRow("Location (for BLE):", locationStatusIcon))
        grid.addView(createStatusRow("Storage:", storageStatusIcon))
        grid.addView(createStatusRow("USB (Thermal Camera):", usbStatusIcon))
        return grid
    }

    private fun createStatusRow(labelText: String, iconView: TextView): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(TextView(context).apply {
                text = labelText; textSize = 14f; layoutParams =
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            iconView.apply { text = "?"; textSize = 18f }
            addView(iconView)
        }
    }
}