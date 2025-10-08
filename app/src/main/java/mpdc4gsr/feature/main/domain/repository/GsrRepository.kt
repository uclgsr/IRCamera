package mpdc4gsr.feature.main.domain.repository

import kotlinx.coroutines.flow.Flow

interface GsrRepository {
    suspend fun initialize(): Boolean
    suspend fun startDeviceDiscovery(): Boolean
    suspend fun connectToDevice(deviceAddress: String): Boolean
    suspend fun disconnect()
    fun getConnectionState(): Flow<GsrConnectionState>
    fun getBatteryLevel(): Flow<Int?>
    fun getDeviceStatus(): Flow<String>
    fun getConnectionQuality(): Flow<Float>
}

enum class GsrConnectionState {
    DISCONNECTED, DISCOVERING, CONNECTING, CONNECTED, ERROR
}
