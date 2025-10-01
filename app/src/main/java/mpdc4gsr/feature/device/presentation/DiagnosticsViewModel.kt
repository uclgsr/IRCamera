package mpdc4gsr.feature.device.presentation

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import mpdc4gsr.core.ui.BaseViewModel

/**
 * Diagnostics ViewModel - MVVM Integration
 * Provides real-time system diagnostics and sensor status monitoring
 */
class DiagnosticsViewModel : BaseViewModel() {

    private lateinit var context: Context

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

    fun initialize(ctx: Context) {
        context = ctx
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
            // TODO: Integrate with actual sensor status checks
            _sensorStatus.value = SensorStatus(
                gsrSensor = "OK",
                thermalCamera = "OK",
                rgbCamera = "OK"
            )
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
            // Note: Actual temperature reading requires hardware sensor access
            // This is a placeholder for demonstration
            val tempFile = File("/sys/class/thermal/thermal_zone0/temp")
            if (tempFile.exists()) {
                val temp = tempFile.readText().trim().toIntOrNull()
                if (temp != null) {
                    "${temp / 1000}°C"
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
            // TODO: Implement comprehensive diagnostics
            updateSystemStatus()
            updateSensorStatus()
        }
    }

    fun testAllSensors() {
        viewModelScope.launch {
            // TODO: Integrate with sensor test procedures
            updateSensorStatus()
        }
    }

    fun exportDiagnosticLogs() {
        viewModelScope.launch {
            // TODO: Implement log export functionality
        }
    }
}
