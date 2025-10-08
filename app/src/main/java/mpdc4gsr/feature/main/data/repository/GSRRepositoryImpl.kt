package mpdc4gsr.feature.main.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import mpdc4gsr.core.data.UnifiedGSRRecorder
import mpdc4gsr.feature.main.domain.repository.GSRConnectionState
import mpdc4gsr.feature.main.domain.repository.GSRRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GSRRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : GSRRepository {
    
    private val _connectionState = MutableStateFlow(GSRConnectionState.DISCONNECTED)
    private val _batteryLevel = MutableStateFlow<Int?>(null)
    private val _deviceStatus = MutableStateFlow("")
    private val _connectionQuality = MutableStateFlow(0f)
    
    private var unifiedGSRRecorder: UnifiedGSRRecorder? = null
    
    override suspend fun initialize(): Boolean {
        _connectionState.value = GSRConnectionState.CONNECTING
        return unifiedGSRRecorder?.initialize() ?: false
    }
    
    override suspend fun startDeviceDiscovery(): Boolean {
        _connectionState.value = GSRConnectionState.DISCOVERING
        return unifiedGSRRecorder?.startDeviceDiscovery() ?: false
    }
    
    override suspend fun connectToDevice(deviceAddress: String): Boolean {
        val devices = unifiedGSRRecorder?.getDiscoveredDevices() ?: return false
        val device = devices.find { it.address == deviceAddress } ?: return false
        
        val connected = unifiedGSRRecorder?.connectToDevice(device) ?: false
        if (connected) {
            _connectionState.value = GSRConnectionState.CONNECTED
        } else {
            _connectionState.value = GSRConnectionState.ERROR
        }
        return connected
    }
    
    override suspend fun disconnect() {
        unifiedGSRRecorder?.disconnect()
        _connectionState.value = GSRConnectionState.DISCONNECTED
    }
    
    override fun getConnectionState(): Flow<GSRConnectionState> = _connectionState
    
    override fun getBatteryLevel(): Flow<Int?> = _batteryLevel
    
    override fun getDeviceStatus(): Flow<String> = _deviceStatus
    
    override fun getConnectionQuality(): Flow<Float> = _connectionQuality
}
