package mpdc4gsr.feature.dashboard.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.feature.dashboard.domain.repository.GsrConnectionState
import mpdc4gsr.feature.dashboard.domain.repository.GsrRepository
import mpdc4gsr.gsr.device.ShimmerDeviceController
import mpdc4gsr.gsr.model.ConnectionState
import java.util.concurrent.atomic.AtomicBoolean

@Singleton
class GsrRepositoryImpl
@Inject
constructor(
    private val shimmerDeviceController: ShimmerDeviceController
) : GsrRepository {
    private val _connectionState = MutableStateFlow(GsrConnectionState.DISCONNECTED)
    private val _batteryLevel = MutableStateFlow<Int?>(null)
    private val _deviceStatus = MutableStateFlow("Disconnected")
    private val _connectionQuality = MutableStateFlow(0f)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val observationStarted = AtomicBoolean(false)

    override suspend fun initialize(): Boolean {
        ensureObservation()
        return true
    }

    override suspend fun startDeviceDiscovery(): Boolean {
        ensureObservation()
        _connectionState.value = GsrConnectionState.DISCOVERING
        shimmerDeviceController.startScanning()
        return true
    }

    override suspend fun connectToDevice(deviceAddress: String): Boolean {
        ensureObservation()
        _connectionState.value = GsrConnectionState.CONNECTING
        val result = shimmerDeviceController.connect(deviceAddress)
        if (!result) {
            _connectionState.value = GsrConnectionState.ERROR
            _deviceStatus.value = "Failed to connect to $deviceAddress"
        }
        return result
    }

    override suspend fun disconnect() {
        _connectionState.value = GsrConnectionState.DISCONNECTED
        shimmerDeviceController.devices.value.keys.forEach { shimmerDeviceController.disconnect(it) }
        _deviceStatus.value = "Disconnected"
    }

    override fun getConnectionState(): Flow<GsrConnectionState> = _connectionState

    override fun getBatteryLevel(): Flow<Int?> = _batteryLevel

    override fun getDeviceStatus(): Flow<String> = _deviceStatus

    override fun getConnectionQuality(): Flow<Float> = _connectionQuality

    private fun ensureObservation() {
        if (observationStarted.compareAndSet(false, true)) {
            scope.launch {
                shimmerDeviceController.devices.collect { devices ->
                    if (devices.isEmpty()) {
                        _connectionState.value = GsrConnectionState.DISCONNECTED
                        _deviceStatus.value = "No devices"
                    } else {
                        val active = devices.values.first()
                        when (active.connectionState) {
                            ConnectionState.CONNECTED, ConnectionState.READY, ConnectionState.RECORDING -> {
                                _connectionState.value = GsrConnectionState.CONNECTED
                                _deviceStatus.value = "Connected to ${active.displayName}"
                            }
                            ConnectionState.CONNECTING -> {
                                _connectionState.value = GsrConnectionState.CONNECTING
                                _deviceStatus.value = "Connecting to ${active.displayName}"
                            }
                            ConnectionState.ERROR -> {
                                _connectionState.value = GsrConnectionState.ERROR
                                _deviceStatus.value = "Device error"
                            }
                            ConnectionState.DISCOVERED -> {
                                _connectionState.value = GsrConnectionState.DISCOVERING
                                _deviceStatus.value = "Discovered ${active.displayName}"
                            }
                            ConnectionState.DISCONNECTED -> {
                                _connectionState.value = GsrConnectionState.DISCONNECTED
                                _deviceStatus.value = "Disconnected"
                            }
                        }
                    }
                }
            }
            scope.launch {
                shimmerDeviceController.telemetry.collect { telemetryMap ->
                    val telemetry = telemetryMap.values.firstOrNull()
                    _batteryLevel.value = telemetry?.batteryPercent
                    _connectionQuality.value = telemetry?.rssi?.let { (it + 100) / 100f } ?: _connectionQuality.value
                }
            }
            scope.launch {
                shimmerDeviceController.samples.collect { sample ->
                    _connectionQuality.value = sample.qualityScore?.toFloat() ?: 1f
                }
            }
        }
    }
}

