package mpdc4gsr.permissions

import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.csl.irCamera.R

/**
 * Demo activity showing how to use the enhanced PermissionController
 * for comprehensive permission handling in the multi-sensor recording app.
 */
class PermissionDemoActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "PermissionDemo"

        fun start(context: Context) {
            val intent = Intent(context, PermissionDemoActivity::class.java)
            context.startActivity(intent)
        }
    }

    private lateinit var permissionController: PermissionController
    private lateinit var statusText: TextView
    private lateinit var checkPermissionsBtn: Button
    private lateinit var requestAllBtn: Button
    private lateinit var requestCameraBtn: Button
    private lateinit var requestBluetoothBtn: Button
    private lateinit var requestBatteryBtn: Button
    private lateinit var requestUsbBtn: Button
    private lateinit var startRecordingBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create simple layout programmatically for demo
        setContentView(createDemoLayout())

        // Initialize permission controller
        permissionController = PermissionController(this)
        permissionController.initialize()

        setupClickListeners()
        updatePermissionStatus()
    }

    private fun createDemoLayout(): android.view.View {
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        statusText = TextView(this).apply {
            text = "Permission Status: Checking..."
            textSize = 16f
            setPadding(0, 0, 0, 24)
        }
        layout.addView(statusText)

        checkPermissionsBtn = Button(this).apply { text = "Check All Permissions" }
        requestAllBtn = Button(this).apply { text = "Request All Permissions" }
        requestCameraBtn = Button(this).apply { text = "Request Camera Only" }
        requestBluetoothBtn = Button(this).apply { text = "Request Bluetooth Only" }
        requestBatteryBtn = Button(this).apply { text = "Request Battery Exemption" }
        requestUsbBtn = Button(this).apply { text = "Request USB Permission" }
        startRecordingBtn = Button(this).apply {
            text = "Start Recording (Check Permissions)"
            isEnabled = false
        }

        layout.addView(checkPermissionsBtn)
        layout.addView(requestAllBtn)
        layout.addView(requestCameraBtn)
        layout.addView(requestBluetoothBtn)
        layout.addView(requestBatteryBtn)
        layout.addView(requestUsbBtn)
        layout.addView(startRecordingBtn)

        return layout
    }

    private fun setupClickListeners() {
        checkPermissionsBtn.setOnClickListener {
            updatePermissionStatus()
        }

        requestAllBtn.setOnClickListener {
            requestAllPermissions()
        }

        requestCameraBtn.setOnClickListener {
            // Demo of checking specific permission before requesting
            if (!permissionController.hasCameraPermission()) {
                showToast("Requesting camera permission...")
                permissionController.ensureAll { granted, denied ->
                    if (granted) {
                        showToast("All permissions granted!")
                    } else {
                        showToast("Some permissions denied: ${denied.joinToString(", ")}")
                    }
                    updatePermissionStatus()
                }
            } else {
                showToast("Camera permission already granted")
            }
        }

        requestBluetoothBtn.setOnClickListener {
            if (!permissionController.canConnectToShimmer()) {
                showToast("Requesting Bluetooth and Location permissions...")
                permissionController.ensureAll { granted, denied ->
                    updatePermissionStatus()
                    if (permissionController.canConnectToShimmer()) {
                        showToast("Shimmer GSR connection now available")
                    } else {
                        showToast("Shimmer GSR still unavailable - missing permissions")
                    }
                }
            } else {
                showToast("Bluetooth permissions already granted - Shimmer ready")
            }
        }

        requestBatteryBtn.setOnClickListener {
            if (permissionController.isBatteryOptimizationDisabled()) {
                showToast("Battery optimization already disabled")
            } else {
                showToast("Requesting battery optimization exemption...")
                permissionController.requestBatteryOptimizationExemption { granted ->
                    if (granted) {
                        showToast("Battery optimization exemption granted")
                    } else {
                        showToast("Battery optimization exemption denied")
                    }
                    updatePermissionStatus()
                }
            }
        }

        requestUsbBtn.setOnClickListener {
            requestUsbPermissionDemo()
        }

        startRecordingBtn.setOnClickListener {
            attemptToStartRecording()
        }
    }

    private fun requestAllPermissions() {
        Log.i(TAG, "Requesting all permissions using ensureAll()")

        permissionController.ensureAll { allGranted, deniedPermissions ->
            if (allGranted) {
                showToast("All permissions granted! Multi-sensor recording ready.")
                Log.i(TAG, "All permissions successfully granted")
            } else {
                val permissionNames = permissionController.getPermissionNames(deniedPermissions)
                showToast("Some permissions denied: ${permissionNames.joinToString(", ")}")
                Log.w(TAG, "Denied permissions: ${deniedPermissions.joinToString(", ")}")
            }
            updatePermissionStatus()
        }
    }

    private fun requestUsbPermissionDemo() {
        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val devices = usbManager.deviceList.values

        if (devices.isEmpty()) {
            showToast("No USB devices connected")
            return
        }

        // Use the first available USB device for demo
        val device = devices.first()

        showToast("Requesting USB permission for: ${device.productName}")
        permissionController.requestUsbPermission(device) { granted, grantedDevice ->
            if (granted && grantedDevice != null) {
                showToast("USB permission granted for: ${grantedDevice.productName}")
                Log.i(TAG, "USB permission granted for device: ${grantedDevice.productName}")
            } else {
                showToast("USB permission denied")
                Log.w(TAG, "USB permission denied for device: ${device.productName}")
            }
        }
    }

    private fun attemptToStartRecording() {
        when {
            !permissionController.canStartRecording() -> {
                showToast("Cannot start recording - missing camera or storage permissions")
                requestAllPermissions()
            }

            !permissionController.canShowNotifications() -> {
                showToast("Recording can start but notifications may not work")
                // Continue anyway
                showToast("Starting recording with limited notification support...")
            }

            else -> {
                showToast("All permissions ready - starting multi-sensor recording!")
                // Here you would actually start the recording service
                Log.i(TAG, "Recording would start now with all permissions granted")
            }
        }
    }

    private fun updatePermissionStatus() {
        val statusMessage = permissionController.getPermissionStatusMessage()
        statusText.text = "Permission Status:\n$statusMessage"

        // Update recording button state
        val canRecord = permissionController.canStartRecording()
        startRecordingBtn.isEnabled = canRecord
        startRecordingBtn.text = if (canRecord) {
            "Start Recording (Ready)"
        } else {
            "Start Recording (Permissions Needed)"
        }

        Log.i(TAG, "Permission status updated: $statusMessage")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Delegate to permission controller
        permissionController.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Update UI after permission result
        updatePermissionStatus()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Delegate to permission controller for battery optimization results
        permissionController.onActivityResult(requestCode, resultCode)

        // Update UI after activity result
        updatePermissionStatus()
    }

    override fun onResume() {
        super.onResume()
        // Update permission status when returning from settings
        updatePermissionStatus()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        Log.i(TAG, "Toast: $message")
    }
}
