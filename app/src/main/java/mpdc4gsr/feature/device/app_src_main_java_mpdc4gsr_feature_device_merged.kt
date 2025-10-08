// Merged ALL .kt and .java files from the 'app\src\main\java\mpdc4gsr\feature\device' directory and its subdirectories.
// Total files: 2 | Generated on: 2025-10-08 01:42:32


// ===== FROM: app\src\main\java\mpdc4gsr\feature\device\presentation\DiagnosticsViewModel.kt =====

package mpdc4gsr.feature.device.presentation

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DiagnosticsViewModel(context: Context) : BaseViewModel() {
    private val context: Context = context.applicationContext
    private val _systemStatus = MutableStateFlow(SystemStatus())
    val systemStatus: StateFlow<SystemStatus> = _systemStatus.asStateFlow()
    private val _sensorStatus = MutableStateFlow(SensorStatus())
    val sensorStatus: StateFlow<SensorStatus> = _sensorStatus.asStateFlow()

    data class SystemStatus(
        val systemHealth: String = "Checking...",
        val battery: String = "Checking...",
        val temperature: String = "Checking...",
        val memoryUsage: String = "Checking..."
    )

    data class SensorStatus(
        val gsrSensor: String = "Checking...",
        val thermalCamera: String = "Checking...",
        val rgbCamera: String = "Checking..."
    )

    companion object {
        private const val TC001_VENDOR_ID = 0x0BDA
        private const val TC001_PRODUCT_ID = 0x5830
        private const val TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss"
    }

    init {
        updateSystemStatus()
        updateSensorStatus()
    }

    fun initialize() {
        updateSystemStatus()
        updateSensorStatus()
    }

    private fun updateSystemStatus() {
        viewModelScope.launch {
            try {
                val batteryStatus = getBatteryLevel()
                val memoryInfo = getMemoryInfo()
                val temperature = getDeviceTemperature()
                _systemStatus.value = SystemStatus(
                    systemHealth = "Good",
                    battery = batteryStatus,
                    temperature = temperature,
                    memoryUsage = memoryInfo
                )
            } catch (e: Exception) {
                _systemStatus.value = SystemStatus(
                    systemHealth = "Error",
                    battery = "Error",
                    temperature = "Error",
                    memoryUsage = "Error"
                )
            }
        }
    }

    private fun updateSensorStatus() {
        viewModelScope.launch {
            val gsrStatus = checkGSRSensorStatus()
            val thermalStatus = checkThermalCameraStatus()
            val rgbStatus = checkRGBCameraStatus()
            _sensorStatus.value = SensorStatus(
                gsrSensor = gsrStatus,
                thermalCamera = thermalStatus,
                rgbCamera = rgbStatus
            )
        }
    }

    private suspend fun checkGSRSensorStatus(): String {
        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val bluetoothAdapter = bluetoothManager?.adapter
            if (bluetoothAdapter == null) {
                "Not Available - No Bluetooth"
            } else if (!bluetoothAdapter.isEnabled) {
                "Bluetooth Disabled"
            } else {
                "Ready - Bluetooth Enabled"
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private suspend fun checkThermalCameraStatus(): String {
        return try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as? android.hardware.usb.UsbManager
            if (usbManager == null) {
                "Not Available - No USB Support"
            } else {
                val deviceList = usbManager.deviceList
                val hasTC001 = deviceList.values.any { device ->
                    device.vendorId == TC001_VENDOR_ID && device.productId == TC001_PRODUCT_ID
                }
                if (hasTC001) {
                    "Connected - TC001 Detected"
                } else {
                    "Not Connected - No TC001 Device"
                }
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private suspend fun checkRGBCameraStatus(): String {
        return try {
            val cameraManager =
                context.getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
            if (cameraManager == null) {
                "Not Available - No Camera Service"
            } else {
                val cameraIdList = cameraManager.cameraIdList
                if (cameraIdList.isNotEmpty()) {
                    "Available - ${cameraIdList.size} Camera(s) Found"
                } else {
                    "Not Available - No Cameras"
                }
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private fun getBatteryLevel(): String {
        return try {
            val batteryStatus = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) {
                val batteryPct = level * 100 / scale
                "$batteryPct%"
            } else {
                "Unknown"
            }
        } catch (e: Exception) {
            "Error"
        }
    }

    private fun getMemoryInfo(): String {
        return try {
            val runtime = Runtime.getRuntime()
            val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
            val totalMemory = runtime.maxMemory() / (1024 * 1024)
            "$usedMemory MB / $totalMemory MB"
        } catch (e: Exception) {
            "Error"
        }
    }

    private fun getDeviceTemperature(): String {
        return try {
            val tempFile = File("/sys/class/thermal/thermal_zone0/temp")
            if (tempFile.exists()) {
                val temp = tempFile.readText().trim().toIntOrNull()
                if (temp != null) {
                    "${temp / 1000}Â°C"
                } else {
                    "N/A"
                }
            } else {
                "N/A"
            }
        } catch (e: Exception) {
            "N/A"
        }
    }

    fun runFullDiagnostics() {
        viewModelScope.launch {
            updateSystemStatus()
            updateSensorStatus()
        }
    }

    fun testAllSensors() {
        viewModelScope.launch {
            updateSensorStatus()
        }
    }

    fun exportDiagnosticLogs() {
        viewModelScope.launch {
            try {
                val logFile = File(context.cacheDir, "diagnostics_${System.currentTimeMillis()}.log")
                logFile.writeText(buildString {
                    appendLine("=== System Diagnostics Report ===")
                    appendLine("Generated: ${getCurrentTimestamp()}")
                    appendLine()
                    appendLine("System Status:")
                    appendLine("  Health: ${_systemStatus.value.systemHealth}")
                    appendLine("  Battery: ${_systemStatus.value.battery}")
                    appendLine("  Temperature: ${_systemStatus.value.temperature}")
                    appendLine("  Memory: ${_systemStatus.value.memoryUsage}")
                    appendLine()
                    appendLine("Sensor Status:")
                    appendLine("  GSR Sensor: ${_sensorStatus.value.gsrSensor}")
                    appendLine("  Thermal Camera: ${_sensorStatus.value.thermalCamera}")
                    appendLine("  RGB Camera: ${_sensorStatus.value.rgbCamera}")
                    appendLine()
                    appendLine("Device Info:")
                    appendLine("  Model: ${Build.MODEL}")
                    appendLine("  Android Version: ${Build.VERSION.RELEASE}")
                    appendLine("  SDK: ${Build.VERSION.SDK_INT}")
                })
                android.util.Log.i("DiagnosticsViewModel", "Diagnostic log exported to: ${logFile.absolutePath}")
            } catch (e: Exception) {
                android.util.Log.e("DiagnosticsViewModel", "Error exporting diagnostic logs", e)
            }
        }
    }

    private fun getCurrentTimestamp(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            java.time.format.DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT, Locale.US)
                .format(java.time.LocalDateTime.now())
        } else {
            SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US).format(Date())
        }
    }
}


// ===== FROM: app\src\main\java\mpdc4gsr\feature\device\presentation\DiagnosticsViewModelFactory.kt =====

package mpdc4gsr.feature.device.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class DiagnosticsViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiagnosticsViewModel::class.java)) {
            return DiagnosticsViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}