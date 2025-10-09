package mpdc4gsr.feature.main.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import mpdc4gsr.core.data.UnifiedGSRRecorder
import mpdc4gsr.feature.main.domain.repository.GsrConnectionState
import mpdc4gsr.feature.main.domain.repository.GsrRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GsrRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : GsrRepository {

    private val _connectionState = MutableStateFlow(GsrConnectionState.DISCONNECTED)
    private val _batteryLevel = MutableStateFlow<Int?>(null)
    private val _deviceStatus = MutableStateFlow("")
    private val _connectionQuality = MutableStateFlow(0f)

    private val unifiedGSRRecorder: UnifiedGSRRecorder by lazy {
        UnifiedGSRRecorder(context)
    }

    override suspend fun initialize(): Boolean {
        _connectionState.value = GsrConnectionState.CONNECTING
        return unifiedGSRRecorder.initialize()
    }

    override suspend fun startDeviceDiscovery(): Boolean {
        _connectionState.value = GsrConnectionState.DISCOVERING
        return unifiedGSRRecorder.startDeviceDiscovery()
    }

    override suspend fun connectToDevice(deviceAddress: String): Boolean {
        val devices = unifiedGSRRecorder.getDiscoveredDevices()
        val device = devices.find { it.address == deviceAddress } ?: return false

        val connected = unifiedGSRRecorder.connectToDevice(device)
        if (connected) {
            _connectionState.value = GsrConnectionState.CONNECTED
        } else {
            _connectionState.value = GsrConnectionState.ERROR
        }
        return connected
    }

    override suspend fun disconnect() {
        unifiedGSRRecorder.disconnect()
        _connectionState.value = GsrConnectionState.DISCONNECTED
    }

    override fun getConnectionState(): Flow<GsrConnectionState> = _connectionState

    override fun getBatteryLevel(): Flow<Int?> = _batteryLevel

    override fun getDeviceStatus(): Flow<String> = _deviceStatus

    override fun getConnectionQuality(): Flow<Float> = _connectionQuality
}
