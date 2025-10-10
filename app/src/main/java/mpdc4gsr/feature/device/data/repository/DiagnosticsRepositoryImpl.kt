package mpdc4gsr.feature.device.data.repository

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import mpdc4gsr.feature.device.domain.repository.DiagnosticsRepository
import mpdc4gsr.feature.device.domain.repository.SensorStatus
import mpdc4gsr.feature.device.domain.repository.SystemStatus
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnosticsRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : DiagnosticsRepository {
        private val _systemStatus = MutableStateFlow(SystemStatus())
        private val _sensorStatus = MutableStateFlow(SensorStatus())

        companion object {
            private const val TC001_VENDOR_ID = 0x0BDA
            private const val TC001_PRODUCT_ID = 0x5830
            private const val TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss"
        }

        init {
            updateSystemStatus()
            updateSensorStatus()
        }

        override fun getSystemStatus(): Flow<SystemStatus> = _systemStatus

        override fun getSensorStatus(): Flow<SensorStatus> = _sensorStatus

        override suspend fun runFullDiagnostics() {
            updateSystemStatus()
            updateSensorStatus()
        }

        override suspend fun testAllSensors() {
            updateSensorStatus()
        }

        override suspend fun exportDiagnosticLogs(): String {
            val logFile = File(context.cacheDir, "diagnostics_${System.currentTimeMillis()}.log")
            logFile.writeText(
                buildString {
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
                },
            )
            return logFile.absolutePath
        }

        private fun updateSystemStatus() {
            val batteryStatus = getBatteryLevel()
            val memoryInfo = getMemoryInfo()
            val temperature = getDeviceTemperature()
            _systemStatus.value =
                SystemStatus(
                    systemHealth = "Good",
                    battery = batteryStatus,
                    temperature = temperature,
                    memoryUsage = memoryInfo,
                )
        }

        private fun updateSensorStatus() {
            val gsrStatus = checkGSRSensorStatus()
            val thermalStatus = checkThermalCameraStatus()
            val rgbStatus = checkRGBCameraStatus()
            _sensorStatus.value =
                SensorStatus(
                    gsrSensor = gsrStatus,
                    thermalCamera = thermalStatus,
                    rgbCamera = rgbStatus,
                )
        }

        private fun checkGSRSensorStatus(): String {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val bluetoothAdapter = bluetoothManager?.adapter
            return if (bluetoothAdapter == null) {
                "Not Available - No Bluetooth"
            } else if (!bluetoothAdapter.isEnabled) {
                "Bluetooth Disabled"
            } else {
                "Ready - Bluetooth Enabled"
            }
        }

        private fun checkThermalCameraStatus(): String {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as? android.hardware.usb.UsbManager
            return if (usbManager == null) {
                "Not Available - No USB Support"
            } else {
                val deviceList = usbManager.deviceList
                val hasTC001 =
                    deviceList.values.any { device ->
                        device.vendorId == TC001_VENDOR_ID && device.productId == TC001_PRODUCT_ID
                    }
                if (hasTC001) {
                    "Connected - TC001 Detected"
                } else {
                    "Not Connected - No TC001 Device"
                }
            }
        }

        private fun checkRGBCameraStatus(): String {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
            return if (cameraManager == null) {
                "Not Available - No Camera Service"
            } else {
                val cameraIdList = cameraManager.cameraIdList
                if (cameraIdList.isNotEmpty()) {
                    "Available - ${cameraIdList.size} Camera(s) Found"
                } else {
                    "Not Available - No Cameras"
                }
            }
        }

        private fun getBatteryLevel(): String {
            val batteryStatus =
                context.registerReceiver(
                    null,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                )
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            return if (level >= 0 && scale > 0) {
                val batteryPct = level * 100 / scale
                "$batteryPct%"
            } else {
                "Unknown"
            }
        }

        private fun getMemoryInfo(): String {
            val runtime = Runtime.getRuntime()
            val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
            val totalMemory = runtime.maxMemory() / (1024 * 1024)
            return "$usedMemory MB / $totalMemory MB"
        }

        private fun getDeviceTemperature(): String {
            val tempFile = File("/sys/class/thermal/thermal_zone0/temp")
            return if (tempFile.exists()) {
                val temp = tempFile.readText().trim().toIntOrNull()
                if (temp != null) {
                    "${temp / 1000}°C"
                } else {
                    "N/A"
                }
            } else {
                "N/A"
            }
        }

        private fun getCurrentTimestamp(): String =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                java.time.format.DateTimeFormatter
                    .ofPattern(TIMESTAMP_FORMAT, Locale.US)
                    .format(java.time.LocalDateTime.now())
            } else {
                SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US).format(Date())
            }
    }
