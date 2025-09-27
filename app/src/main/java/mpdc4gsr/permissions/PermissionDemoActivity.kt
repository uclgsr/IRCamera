package mpdc4gsr.permissions

import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PermissionDemoActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "PermissionDemo"
        fun start(context: Context) {
            context.startActivity(Intent(context, PermissionDemoActivity::class.java))
        }
    }

    private lateinit var permissionController: PermissionController
    private lateinit var statusText: TextView
    private lateinit var startRecordingBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createDemoLayout())
        permissionController = PermissionController(this)
        setupClickListeners()
        updatePermissionStatus()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun setupClickListeners() {
        findViewById<Button>(ID_CHECK_PERMISSIONS).setOnClickListener { updatePermissionStatus() }
        findViewById<Button>(ID_REQUEST_ALL).setOnClickListener { requestAllPermissions() }
        findViewById<Button>(ID_REQUEST_BATTERY).setOnClickListener { requestBatteryExemption() }
        findViewById<Button>(ID_REQUEST_USB).setOnClickListener { requestUsbPermissionDemo() }
        startRecordingBtn.setOnClickListener { attemptToStartRecording() }
    }

    private fun requestAllPermissions() {
        Log.i(TAG, "Requesting all permissions using ensureAll()")
        permissionController.ensureAll { allGranted, deniedPermissions ->
            if (allGranted) {
                showToast("All permissions granted! Multi-sensor recording ready.")
            } else {
                val permissionNames = permissionController.getPermissionNames(deniedPermissions)
                showToast("Some permissions denied: ${permissionNames.joinToString(", ")}")
            }
            updatePermissionStatus()
        }
    }

    private fun requestBatteryExemption() {
        if (permissionController.isBatteryOptimizationDisabled()) {
            showToast("Battery optimization already disabled.")
        } else {
            showToast("Requesting battery optimization exemption...")
            permissionController.requestBatteryOptimizationExemption()
        }
    }

    private fun requestUsbPermissionDemo() {
        val usbManager = getSystemService(USB_SERVICE) as UsbManager
        val device = usbManager.deviceList.values.firstOrNull()
        if (device == null) {
            showToast("No USB devices connected")
            return
        }
        showToast("Requesting USB permission for: ${device.productName}")
        permissionController.requestUsbPermission(device) { granted, grantedDevice ->
            if (granted) {
                showToast("USB permission granted for: ${grantedDevice?.productName}")
            } else {
                showToast("USB permission denied")
            }
        }
    }

    private fun attemptToStartRecording() {
        if (permissionController.canStartRecording()) {
            showToast("All permissions ready - starting multi-sensor recording!")
            Log.i(TAG, "Recording would start now.")
        } else {
            showToast("Cannot start recording - missing camera or storage permissions.")
            requestAllPermissions()
        }
    }

    private fun updatePermissionStatus() {
        val statusMessage = permissionController.getPermissionStatusMessage()
        statusText.text = "Permission Status:\n$statusMessage"

        val canRecord = permissionController.canStartRecording()
        startRecordingBtn.isEnabled = canRecord
        startRecordingBtn.text =
            if (canRecord) "Start Recording (Ready)" else "Start Recording (Permissions Needed)"

        Log.i(TAG, "Permission status updated.")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        Log.i(TAG, "Toast: $message")
    }

    // --- Programmatic UI Setup ---
    private val ID_CHECK_PERMISSIONS = View.generateViewId()
    private val ID_REQUEST_ALL = View.generateViewId()
    private val ID_REQUEST_BATTERY = View.generateViewId()
    private val ID_REQUEST_USB = View.generateViewId()

    private fun createDemoLayout(): View {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        statusText = TextView(this).apply {
            text = "Permission Status: Checking..."; textSize = 16f; setPadding(0, 0, 0, 24)
        }
        startRecordingBtn = Button(this).apply { text = "Start Recording"; isEnabled = false }

        layout.addView(statusText)
        layout.addView(Button(this).apply {
            text = "Check All Permissions"; id = ID_CHECK_PERMISSIONS
        })
        layout.addView(Button(this).apply { text = "Request All Permissions"; id = ID_REQUEST_ALL })
        layout.addView(Button(this).apply {
            text = "Request Battery Exemption"; id = ID_REQUEST_BATTERY
        })
        layout.addView(Button(this).apply { text = "Request USB Permission"; id = ID_REQUEST_USB })
        layout.addView(startRecordingBtn)
        return layout
    }
}