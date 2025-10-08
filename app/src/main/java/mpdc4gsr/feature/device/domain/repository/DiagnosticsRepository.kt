package mpdc4gsr.feature.device.domain.repository

import kotlinx.coroutines.flow.Flow

interface DiagnosticsRepository {
    fun getSystemStatus(): Flow<SystemStatus>
    fun getSensorStatus(): Flow<SensorStatus>
    suspend fun runFullDiagnostics()
    suspend fun testAllSensors()
    suspend fun exportDiagnosticLogs(): String
}

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
