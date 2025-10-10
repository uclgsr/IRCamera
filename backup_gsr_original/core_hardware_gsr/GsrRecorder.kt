package mpdc4gsr.core.hardware.gsr

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import mpdc4gsr.core.hardware.api.SensorRecorder
import mpdc4gsr.core.hardware.gsr.model.DeviceInfo
import mpdc4gsr.core.hardware.gsr.model.GSRSample

/**
 * Abstraction over a concrete GSR recorder implementation.
 *
 * This interface allows higher-level features (e.g. feature/gsr, network preview)
 * to depend on a stable contract without coupling to the rather large
 * recorder implementation. It deliberately extends [SensorRecorder] so
 * the delegate-based wrappers can keep using the existing APIs.
 */
interface GsrRecorder : SensorRecorder {
    val deviceStatus: StateFlow<String>
    val connectionQuality: StateFlow<Double>
    val dataStream: Flow<GSRSample>
    val lastConnectedDeviceAddress: String?

    suspend fun startDeviceDiscovery(): Boolean

    fun getDiscoveredDevices(): List<DeviceInfo>

    suspend fun connectToDevice(deviceInfo: DeviceInfo): Boolean

    suspend fun disconnectDevice(): Boolean

    suspend fun flushAndCloseFiles()
}
