package mpdc4gsr.feature.main.data.repository

import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.core.sensors.gsr.DefaultGsrRecorder
import mpdc4gsr.feature.main.domain.repository.GsrConnectionState
import mpdc4gsr.feature.main.domain.repository.GsrRepository
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GsrRepositoryImpl
@Inject
constructor(
    @ApplicationContext private val context: Context,
) : GsrRepository {
    private val _connectionState = MutableStateFlow(GsrConnectionState.DISCONNECTED)
    private val _batteryLevel = MutableStateFlow<Int?>(null)
    private val _deviceStatus = MutableStateFlow("Disconnected")
    private val _connectionQuality = MutableStateFlow(0f)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val observationStarted = AtomicBoolean(false)

    private val gsrRecorder: DefaultGsrRecorder by lazy {
        DefaultGsrRecorder(context, ProcessLifecycleOwner.get()).also { recorder ->
            startObservingRecorder(recorder)
        }
    }

    override suspend fun initialize(): Boolean {
        _connectionState.value = GsrConnectionState.CONNECTING
        val initialized = gsrRecorder.initialize()
        if (!initialized) {
            _connectionState.value = GsrConnectionState.ERROR
            _deviceStatus.value = "Initialization failed"
        }
        return initialized
    }

    override suspend fun startDeviceDiscovery(): Boolean {
        _connectionState.value = GsrConnectionState.DISCOVERING
        val discovered = gsrRecorder.startDeviceDiscovery()
        if (!discovered) {
            _connectionState.value = GsrConnectionState.ERROR
            _deviceStatus.value = "Discovery failed"
        }
        return discovered
    }

    override suspend fun connectToDevice(deviceAddress: String): Boolean {
        val device =
            gsrRecorder
                .getDiscoveredDevices()
                .find { it.address == deviceAddress }
                ?: return false

        val connected = gsrRecorder.connectToDevice(device)
        _connectionState.value =
            if (connected) {
                _deviceStatus.value = "Connected to ${device.name}"
                GsrConnectionState.CONNECTED
            } else {
                _deviceStatus.value = "Failed to connect to ${device.name}"
                GsrConnectionState.ERROR
            }
        return connected
    }

    override suspend fun disconnect() {
        gsrRecorder.disconnectDevice()
        _connectionState.value = GsrConnectionState.DISCONNECTED
        _deviceStatus.value = "Disconnected"
    }

    override fun getConnectionState(): Flow<GsrConnectionState> = _connectionState

    override fun getBatteryLevel(): Flow<Int?> = _batteryLevel

    override fun getDeviceStatus(): Flow<String> = _deviceStatus

    override fun getConnectionQuality(): Flow<Float> = _connectionQuality

    private fun startObservingRecorder(recorder: DefaultGsrRecorder) {
        if (observationStarted.compareAndSet(false, true)) {
            scope.launch {
                recorder.deviceStatus.collect { status ->
                    _deviceStatus.value = status
                }
            }
            scope.launch {
                recorder.connectionQuality.collect { quality ->
                    _connectionQuality.value = quality.toFloat()
                }
            }
            scope.launch {
                recorder.getDataStream().collect { sample ->
                    // Estimate battery based on RSSI (placeholder until proper metric available)
                    val estimatedBattery = (sample.connectionRssi + 100).coerceIn(0, 100)
                    _batteryLevel.value = estimatedBattery
                }
            }
        }
    }
}
