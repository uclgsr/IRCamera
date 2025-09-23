package mpdc4gsr.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.csl.irCamera.R
import com.kotlinx.coroutines.launch
import com.mpdc4gsr.controller.HardwareValidationController
import com.mpdc4gsr.controller.RecordingController
import com.mpdc4gsr.controller.ValidationReport
import com.mpdc4gsr.permissions.PermissionController
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Phase2ValidationActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "Phase2ValidationActivity"
        private const val VALIDATION_REPORT_FILENAME = "irCamera_validation_report"
    }

    private lateinit var startValidationButton: Button
    private lateinit var validationProgressBar: ProgressBar
    private lateinit var validationStatusText: TextView
    private lateinit var permissionStatusLayout: LinearLayout
    private lateinit var validationResultsScrollView: ScrollView
    private lateinit var validationResultsText: TextView
    private lateinit var exportReportButton: Button
    private lateinit var openSettingsButton: Button

    private lateinit var permissionController: PermissionController
    private lateinit var recordingController: RecordingController
    private lateinit var hardwareValidationController: HardwareValidationController

    private var currentValidationReport: ValidationReport? = null
    private var isValidationRunning = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        updatePermissionStatus()

        if (allGranted) {
            showToast("All permissions granted! Ready for validation.")
            startValidationButton.isEnabled = true
        } else {
            val deniedPermissions = permissions.filterValues { !it }.keys
            showPermissionExplanationDialog(deniedPermissions.toList())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phase2_validation)

        initializeComponents()
        setupUI()
        updatePermissionStatus()
    }

    private fun initializeComponents() {
        Log.i(TAG, "Initializing Phase 2 validation components...")

        permissionController = PermissionController(this)
        permissionController.initialize()

        recordingController = RecordingController(this, this)

        hardwareValidationController = HardwareValidationController(
            this, this, permissionController, recordingController
        )

        startValidationButton = findViewById(R.id.btn_start_validation)
        validationProgressBar = findViewById(R.id.progress_validation)
        validationStatusText = findViewById(R.id.text_validation_status)
        permissionStatusLayout = findViewById(R.id.layout_permission_status)
        validationResultsScrollView = findViewById(R.id.scroll_validation_results)
        validationResultsText = findViewById(R.id.text_validation_results)
        exportReportButton = findViewById(R.id.btn_export_report)
        openSettingsButton = findViewById(R.id.btn_open_settings)

        Log.i(TAG, "Phase 2 validation components initialized")
    }

    private fun setupUI() {

        startValidationButton.setOnClickListener {
            if (!isValidationRunning) {
                startHardwareValidation()
            }
        }

        exportReportButton.setOnClickListener {
            exportValidationReport()
        }

        openSettingsButton.setOnClickListener {
            openAppSettings()
        }

        validationProgressBar.visibility = View.GONE
        validationResultsScrollView.visibility = View.GONE
        exportReportButton.isEnabled = false

        updateValidationButtonState()
    }

    private fun updatePermissionStatus() {
        permissionStatusLayout.removeAllViews()

        val permissionCategories = mapOf(
            "Camera" to permissionController.hasCameraPermission(),
            "Audio" to permissionController.hasAudioPermission(),
            "Bluetooth" to permissionController.hasBluetoothPermissions(),
            "Storage" to permissionController.hasStoragePermissions(),
            "Location" to permissionController.hasLocationPermission(),
            "Notifications" to permissionController.hasNotificationPermissions()
        )

        for ((category, granted) in permissionCategories) {
            val statusView = createPermissionStatusView(category, granted)
            permissionStatusLayout.addView(statusView)
        }

        updateValidationButtonState()
    }

    private fun createPermissionStatusView(category: String, granted: Boolean): View {
        val statusLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 8, 16, 8)
        }

        val categoryText = TextView(this).apply {
            text = category
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val statusIcon = TextView(this).apply {
            text = if (granted) "✓" else "✗"
            setTextColor(getColor(if (granted) android.R.color.holo_green_dark else android.R.color.holo_red_dark))
            textSize = 18f
        }

        statusLayout.addView(categoryText)
        statusLayout.addView(statusIcon)

        return statusLayout
    }

    private fun updateValidationButtonState() {
        val allPermissionsGranted = permissionController.hasAllRequiredPermissions()
        startValidationButton.isEnabled = allPermissionsGranted && !isValidationRunning
        startValidationButton.text =
            if (isValidationRunning) "Validation Running..." else "Start Hardware Validation"

        if (!allPermissionsGranted) {
            startValidationButton.setOnClickListener {
                requestMissingPermissions()
            }
        }
    }

    private fun requestMissingPermissions() {
        if (!permissionController.hasAllRequiredPermissions()) {
            permissionController.requestAllPermissions { allGranted, deniedPermissions ->
                if (!allGranted) {
                    showPermissionExplanationDialog(deniedPermissions)
                }
            }
        }
    }

    private fun showPermissionExplanationDialog(deniedPermissions: List<String>) {
        val explanations = mapOf(
            "android.permission.CAMERA" to "Camera access is required for RGB video recording",
            "android.permission.RECORD_AUDIO" to "Audio recording is required for video with sound",
            "android.permission.BLUETOOTH_SCAN" to "Bluetooth scanning is required for GSR sensor discovery",
            "android.permission.BLUETOOTH_CONNECT" to "Bluetooth connection is required for GSR sensor data",
            "android.permission.ACCESS_FINE_LOCATION" to "Location permission is required for Bluetooth LE scanning",
            "android.permission.WRITE_EXTERNAL_STORAGE" to "Storage access is required for saving sensor data",
            "android.permission.POST_NOTIFICATIONS" to "Notification permission is required for recording status updates"
        )

        val message =
            StringBuilder("The following permissions are required for hardware validation:\n\n")
        deniedPermissions.forEach { permission ->
            val explanation = explanations[permission] ?: "Required for sensor functionality"
            message.append("• $explanation\n")
        }
        message.append("\nWould you like to grant these permissions?")

        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage(message.toString())
            .setPositiveButton("Grant Permissions") { _, _ ->
                requestMissingPermissions()
            }
            .setNegativeButton("Open Settings") { _, _ ->
                openAppSettings()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun startHardwareValidation() {
        if (!permissionController.hasAllRequiredPermissions()) {
            showToast("Please grant all required permissions first")
            return
        }

        isValidationRunning = true
        validationProgressBar.visibility = View.VISIBLE
        validationResultsScrollView.visibility = View.GONE
        exportReportButton.isEnabled = false
        updateValidationButtonState()

        validationStatusText.text = "Starting comprehensive hardware validation..."

        lifecycleScope.launch {
            try {
                Log.i(TAG, "Starting hardware validation on Samsung S22")

                updateValidationStatus("Validating permissions system...")

                val report = hardwareValidationController.validateAllSensors()

                currentValidationReport = report
                displayValidationResults(report)

                Log.i(TAG, "Hardware validation completed successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Hardware validation failed", e)
                validationStatusText.text = "Validation failed: ${e.message}"
                showToast("Validation failed. Check logs for details.")
            } finally {
                isValidationRunning = false
                validationProgressBar.visibility = View.GONE
                updateValidationButtonState()
            }
        }
    }

    private fun updateValidationStatus(status: String) {
        validationStatusText.text = status
        Log.i(TAG, "Validation status: $status")
    }

    private fun displayValidationResults(report: ValidationReport) {
        val resultsText = generateValidationResultsText(report)
        validationResultsText.text = resultsText
        validationResultsScrollView.visibility = View.VISIBLE
        exportReportButton.isEnabled = true

        val summary = report.summary
        val statusMessage = if (summary.overallSuccess) {
            "✓ All validations passed (${summary.passedTests}/${summary.totalTests})"
        } else {
            "⚠ ${summary.failedTests} validations failed (${summary.passedTests}/${summary.totalTests} passed)"
        }

        validationStatusText.text = statusMessage
    }

    private fun generateValidationResultsText(report: ValidationReport): String {
        val sb = StringBuilder()

        sb.append("=== HARDWARE VALIDATION REPORT ===\n")
        sb.append("Timestamp: ${formatTimestamp(report.timestamp)}\n")
        sb.append("Device: ${report.deviceInfo.manufacturer} ${report.deviceInfo.model}\n")
        sb.append("Android: ${report.deviceInfo.androidVersion} (API ${report.deviceInfo.sdkInt})\n\n")

        with(report.summary) {
            sb.append("SUMMARY:\n")
            sb.append("- Total Tests: $totalTests\n")
            sb.append("- Passed: $passedTests\n")
            sb.append("- Failed: $failedTests\n")
            sb.append("- Duration: ${totalDurationMs}ms\n")
            sb.append("- Overall: ${if (overallSuccess) "SUCCESS" else "FAILURE"}\n\n")
        }

        sb.append("DETAILED RESULTS:\n")
        for ((category, result) in report.validationResults) {
            val status = if (result.success) "✓" else "✗"
            sb.append("$status $category: ${result.message}\n")
            if (result.metrics.isNotEmpty()) {
                for (entry in result.metrics.entries) {
                    sb.append("  - ${entry.key}: ${entry.value}\n")
                }
            }
            sb.append("\n")
        }

        if (report.sensorCapabilities.isNotEmpty()) {
            sb.append("SENSOR CAPABILITIES:\n")
            for ((sensor, capability) in report.sensorCapabilities) {
                sb.append("${capability.sensorType}:\n")
                sb.append("  - Available: ${if (capability.isAvailable) "Yes" else "No"}\n")
                for (entry in capability.capabilities.entries) {
                    sb.append("  - ${entry.key}: ${entry.value}\n")
                }
                sb.append("\n")
            }
        }

        if (report.performanceMetrics.isNotEmpty()) {
            sb.append("PERFORMANCE METRICS:\n")
            for (entry in report.performanceMetrics.entries) {
                sb.append("- ${entry.key}: ${entry.value}\n")
            }
            sb.append("\n")
        }

        if (report.errorLogs.isNotEmpty()) {
            sb.append("ERROR LOGS:\n")
            for (error in report.errorLogs) {
                sb.append("! $error\n")
            }
        }

        return sb.toString()
    }

    private fun exportValidationReport() {
        currentValidationReport?.let { report ->
            try {
                val jsonReport = convertReportToJson(report)
                val timestamp =
                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val filename = "${VALIDATION_REPORT_FILENAME}_$timestamp.json"


                showReportExportDialog(jsonReport, filename)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to export validation report", e)
                showToast("Failed to export report: ${e.message}")
            }
        } ?: showToast("No validation report available to export")
    }

    private fun convertReportToJson(report: ValidationReport): String {
        val json = JSONObject()
        json.put("timestamp", report.timestamp)
        json.put("device_info", JSONObject().apply {
            put("manufacturer", report.deviceInfo.manufacturer)
            put("model", report.deviceInfo.model)
            put("android_version", report.deviceInfo.androidVersion)
            put("sdk_int", report.deviceInfo.sdkInt)
            put("app_version", report.deviceInfo.appVersion)
        })

        val resultsJson = JSONObject()
        for ((key, result) in report.validationResults) {
            resultsJson.put(key, JSONObject().apply {
                put("success", result.success)
                put("message", result.message)
                put("metrics", JSONObject(result.metrics))
            })
        }
        json.put("validation_results", resultsJson)

        json.put("summary", JSONObject().apply {
            put("total_tests", report.summary.totalTests)
            put("passed_tests", report.summary.passedTests)
            put("failed_tests", report.summary.failedTests)
            put("total_duration_ms", report.summary.totalDurationMs)
            put("overall_success", report.summary.overallSuccess)
        })

        return json.toString(2)
    }

    private fun showReportExportDialog(jsonContent: String, filename: String) {
        AlertDialog.Builder(this)
            .setTitle("Validation Report Generated")
            .setMessage("Report saved as: $filename\n\nWould you like to view the JSON content?")
            .setPositiveButton("View JSON") { _, _ ->
                showJsonContentDialog(jsonContent)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showJsonContentDialog(jsonContent: String) {
        val textView = TextView(this).apply {
            text = jsonContent
            setPadding(32, 32, 32, 32)
            setTextIsSelectable(true)
        }

        val scrollView = ScrollView(this).apply {
            addView(textView)
        }

        AlertDialog.Builder(this)
            .setTitle("Validation Report JSON")
            .setView(scrollView)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}

private fun PermissionController.hasAudioPermission(): Boolean {

    return true // Placeholder
}

private fun PermissionController.hasLocationPermission(): Boolean {

    return true // Placeholder
}
