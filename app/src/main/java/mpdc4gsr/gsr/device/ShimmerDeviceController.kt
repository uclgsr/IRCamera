package mpdc4gsr.gsr.device

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.ParcelUuid
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.shimmerresearch.android.Shimmer
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
import com.shimmerresearch.bluetooth.ShimmerBluetooth
import com.shimmerresearch.driver.CallbackObject
import com.shimmerresearch.driver.ObjectCluster
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mpdc4gsr.gsr.model.ConnectionState
import mpdc4gsr.gsr.model.DeviceDescriptor
import mpdc4gsr.gsr.model.DeviceType
import mpdc4gsr.gsr.model.GsrSample
import mpdc4gsr.gsr.model.TelemetryState
import mpdc4gsr.gsr.session.TimelineClock
import mpdc4gsr.gsr.capture.SimulationSource

/**
 * High-level controller that wraps the Shimmer Android Bluetooth manager to provide a multi-device
 * interface for the rewritten GSR module. It supports simultaneous connections, automatic
 * reconnection, and optional simulation streams when no hardware is available (FR1, FR8).
 */
class ShimmerDeviceController(
    private val context: Context,
    lifecycleOwner: LifecycleOwner = androidx.lifecycle.ProcessLifecycleOwner.get(),
    private val clock: TimelineClock,
    private val dispatcher: CoroutineDispatcher,
    private val simulationSource: SimulationSource,
) : LifecycleEventObserver, AutoCloseable {

    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val shimmerManager = ShimmerBluetoothManagerAndroid(context, ShimmerMsgHandler(Looper.getMainLooper()))
    private val devicesMutex = Mutex()
    private val connectedDevices = mutableMapOf<String, Shimmer>()
    private val sequenceCounter = AtomicLong()

    private val _devices = MutableStateFlow<Map<String, DeviceDescriptor>>(emptyMap())
    val devices: StateFlow<Map<String, DeviceDescriptor>> = _devices

    private val _samples = MutableSharedFlow<GsrSample>(extraBufferCapacity = 1024, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val samples: SharedFlow<GsrSample> = _samples.asSharedFlow()

    private val _telemetry = MutableStateFlow<Map<String, TelemetryState>>(emptyMap())
    val telemetry: StateFlow<Map<String, TelemetryState>> = _telemetry

    @Volatile
    private var scanning = false

    private val scanCallback =
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                scope.launch {
                    val deviceId = result.device.address.uppercase()
                    updateDevice(
                        deviceId,
                        DeviceDescriptor(
                            id = deviceId,
                            displayName = result.device.name ?: "Shimmer $deviceId",
                            type = DeviceType.SHIMMER_GSR_SENSOR,
                            connectionState = ConnectionState.DISCOVERED,
                            batteryPercent = null,
                            supportsThermal = false,
                            supportsRgb = false,
                            supportsAudio = false,
                            shimmerMacAddress = deviceId,
                            lastHeartbeat = null,
                            timeOffsetMillis = null,
                        ),
                    )
                }
            }
        }

    private val bluetoothReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_ACL_DISCONNECTED,
                    BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED -> {
                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        device?.address?.let { onDeviceDisconnected(it.uppercase()) }
                    }
                }
            }
        }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
        registerBluetoothReceiver()
    }

    fun startScanning() {
        if (!hasPermissions()) {
            Log.w(TAG, "Missing Bluetooth permissions for scanning")
            return
        }
        if (scanning) return
        scanning = true
        val filters =
            listOf(
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(UUID_GSR_SERVICE))
                    .build(),
            )
        val settings =
            ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
        bluetoothAdapter.bluetoothLeScanner.startScan(filters, settings, scanCallback)
    }

    fun stopScanning() {
        if (!scanning) return
        scanning = false
        bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
    }



    suspend fun connect(macAddress: String): Boolean =
        devicesMutex.withLock {
            updateConnectionState(macAddress, ConnectionState.CONNECTING)
            val device =
                runCatching { bluetoothAdapter.getRemoteDevice(macAddress) }
                    .getOrNull()
                    ?: return@withLock false
            shimmerManager.connectBluetoothDevice(device)
            true
        }

    suspend fun disconnect(macAddress: String): Boolean =
        devicesMutex.withLock {
            val shimmer = getShimmer(macAddress) ?: return@withLock false
            shimmer.stopStreaming()
            shimmer.disconnect()
            connectedDevices.remove(macAddress)
            updateConnectionState(macAddress, ConnectionState.DISCONNECTED)
            true
        }

    fun startStreaming(macAddress: String) {
        scope.launch {
            devicesMutex.withLock {
                val shimmer = getShimmer(macAddress) ?: return@withLock
                shimmer.startStreaming()
                updateConnectionState(macAddress, ConnectionState.RECORDING)
            }
        }
    }

    fun stopStreaming(macAddress: String) {
        scope.launch {
            devicesMutex.withLock {
                val shimmer = getShimmer(macAddress) ?: return@withLock
                shimmer.stopStreaming()
                updateConnectionState(macAddress, ConnectionState.READY)
            }
        }
    }

    fun enableSimulation(sessionId: String) {
        simulationSource.start(sessionId) { sample ->
            scope.launch { _samples.emit(sample) }
        }
    }

    fun disableSimulation() {
        simulationSource.stop()
    }

    private fun registerBluetoothReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
        }
        context.registerReceiver(bluetoothReceiver, filter)
    }

    private fun unregisterBluetoothReceiver() {
        runCatching { context.unregisterReceiver(bluetoothReceiver) }
    }

    private fun hasPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_DESTROY) {
            close()
        }
    }

    override fun close() {
        stopScanning()
        unregisterBluetoothReceiver()
        scope.launch {
            devicesMutex.withLock {
                connectedDevices.values.forEach {
                    runCatching { it.stopStreaming() }
                    runCatching { it.disconnect() }
                }
                connectedDevices.clear()
            }
        }
        scope.coroutineContext.cancel()
    }

    private fun updateDevice(deviceId: String, descriptor: DeviceDescriptor) {
        _devices.value = _devices.value.toMutableMap().apply { put(deviceId, descriptor) }
    }

    private fun updateConnectionState(deviceId: String, state: ConnectionState) {
        val descriptor = _devices.value[deviceId]
        if (descriptor != null) {
            updateDevice(deviceId, descriptor.copy(connectionState = state))
        } else {
            updateDevice(
                deviceId,
                DeviceDescriptor(
                    id = deviceId,
                    displayName = "Shimmer $deviceId",
                    type = DeviceType.SHIMMER_GSR_SENSOR,
                    connectionState = state,
                    batteryPercent = null,
                    supportsThermal = false,
                    supportsRgb = false,
                    supportsAudio = false,
                    shimmerMacAddress = deviceId,
                    lastHeartbeat = null,
                    timeOffsetMillis = null,
                ),
            )
        }
    }

    private fun getShimmer(macAddress: String): Shimmer? {
        connectedDevices[macAddress]?.let { return it }
        val shimmer =
            runCatching {
                shimmerManager.getShimmerDeviceBtConnectedFromMac(macAddress)
            }.getOrNull() as? Shimmer
        if (shimmer != null) {
            connectedDevices[macAddress] = shimmer
        }
        return shimmer
    }

    private fun onDeviceConnected(mac: String) {
        updateConnectionState(mac, ConnectionState.READY)
    }

    private fun onDeviceDisconnected(mac: String) {
        updateConnectionState(mac, ConnectionState.DISCONNECTED)
    }

    private inner class ShimmerMsgHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                ShimmerBluetooth.MSG_IDENTIFIER_STATE_CHANGE -> handleStateChange(msg.obj)
                ShimmerBluetooth.MSG_IDENTIFIER_DATA_PACKET -> handleData(msg.obj as? ObjectCluster)
                ShimmerBluetooth.MSG_IDENTIFIER_NOTIFICATION_MESSAGE -> handleNotification(msg)
            }
        }

        private fun handleStateChange(payload: Any?) {
            val (stateName, mac) =
                when (payload) {
                    is ObjectCluster -> payload.mState?.toString() to payload.macAddress
                    is CallbackObject -> payload.mState?.toString() to payload.mBluetoothAddress
                    else -> return
                }
            val address = mac?.takeIf { it.isNotBlank() } ?: return
            when (stateName?.uppercase()) {
                "CONNECTED" -> {
                    getShimmer(address)
                    onDeviceConnected(address)
                }
                "CONNECTING" -> updateConnectionState(address, ConnectionState.CONNECTING)
                "DISCONNECTED", "CONNECTION_LOST", "NONE" -> {
                    connectedDevices.remove(address)
                    onDeviceDisconnected(address)
                }
                "FAILED_TO_CONNECT" -> {
                    connectedDevices.remove(address)
                    updateConnectionState(address, ConnectionState.ERROR)
                }
                "CONNECTION_TIMEOUT" -> updateConnectionState(address, ConnectionState.ERROR)
                else -> Unit
            }
        }

        private fun handleData(cluster: ObjectCluster?) {
            cluster ?: return
            val mac = cluster.macAddress ?: return
            val sample = mapCluster(mac, cluster)
            if (sample != null) {
                scope.launch { _samples.emit(sample) }
                _telemetry.value =
                    _telemetry.value.toMutableMap().apply {
                        this[mac] =
                            TelemetryState(
                                gsrMicrosiemens = sample.gsrMicrosiemens.toFloat(),
                                skinTemperatureCelsius = sample.skinTemperatureCelsius?.toFloat(),
                                thermalSpotCelsius = null,
                                audioLevelDb = null,
                                frameRate = null,
                                droppedFrames = 0,
                                batteryPercent = _devices.value[mac]?.batteryPercent,
                                rssi = sample.connectionRssi,
                            )
                    }
            }
        }

        private fun handleNotification(msg: Message) {
            val payload = msg.obj as? CallbackObject ?: return
            val mac = payload.mBluetoothAddress ?: return
            when (msg.arg1) {
                ShimmerBluetooth.NOTIFICATION_SHIMMER_FULLY_INITIALIZED -> onDeviceConnected(mac)
                ShimmerBluetooth.NOTIFICATION_SHIMMER_STOP_STREAMING -> updateConnectionState(mac, ConnectionState.READY)
                ShimmerBluetooth.NOTIFICATION_SHIMMER_START_STREAMING -> updateConnectionState(mac, ConnectionState.RECORDING)
            }
        }
    }

    private fun mapCluster(deviceId: String, cluster: ObjectCluster): GsrSample? {
        val sensorNames = cluster.mSensorNames ?: return null
        val calibrated = cluster.mCalData ?: return null
        val uncalibrated = cluster.mUncalData

        val gsrIndex = sensorNames.indexOfFirst { it.contains("GSR", ignoreCase = true) }
        if (gsrIndex == -1 || gsrIndex !in calibrated.indices) return null

        val gsrValue = calibrated[gsrIndex]
        val rawValue =
            if (uncalibrated != null && gsrIndex in uncalibrated.indices) uncalibrated[gsrIndex].toInt()
            else null
        val tempIndex = sensorNames.indexOfFirst { it.contains("TEMP", ignoreCase = true) }
        val skinTemperature =
            if (tempIndex != -1 && tempIndex in calibrated.indices) calibrated[tempIndex] else null

        val timestamp = clock.nowInstant().toEpochMilli()
        val resistance = if (gsrValue > 0.0) 1_000_000.0 / gsrValue else null

        return GsrSample(
            deviceId = deviceId,
            timestampMillis = timestamp,
            gsrMicrosiemens = gsrValue,
            gsrRaw = rawValue,
            qualityScore = 0.9,
            connectionRssi = null,
            resistanceOhms = resistance,
            skinTemperatureCelsius = skinTemperature,
            sequenceNumber = sequenceCounter.incrementAndGet(),
        )
    }

    companion object {
        private const val TAG = "ShimmerDeviceController"
        private val UUID_GSR_SERVICE: UUID = UUID.fromString("0000ffe5-0000-1000-8000-00805f9b34fb")
    }
}

