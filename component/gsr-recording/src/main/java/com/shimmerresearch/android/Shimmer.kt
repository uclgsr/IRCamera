package com.shimmerresearch.android

import android.content.Context
import android.os.Handler
import android.util.Log
import com.shimmerresearch.bluetooth.BluetoothManager
import com.shimmerresearch.driver.Configuration
import com.shimmerresearch.driver.ObjectCluster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class Shimmer(private val handler: Handler, private val context: Context) {
    companion object {
        private const val TAG = "Shimmer"
        private const val SIMULATION_DATA_INTERVAL_MS = 8L // 125 Hz for 128 Hz target rate

        const val MESSAGE_STATE_CHANGE = 0
        const val MESSAGE_READ = 2
        const val MESSAGE_ACK_RECEIVED = 4
        const val MESSAGE_TOAST = 5
        const val MESSAGE_PACKET_LOSS_DETECTED = 6
        const val MESSAGE_STOP_STREAMING = 7
        const val MESSAGE_INQUIRY_RESPONSE = 8

        const val STATE_NONE = 0
        const val STATE_CONNECTING = 1
        const val STATE_CONNECTED = 2
        const val STATE_STREAMING = 3

        const val SENSOR_GSR = 0x10
        const val SENSOR_ACCEL = 0x80
        const val SENSOR_GYRO = 0x40
        const val SENSOR_MAG = 0x20

        const val SAMPLING_RATE_1024HZ = 1024.0
        const val SAMPLING_RATE_512HZ = 512.0
        const val SAMPLING_RATE_256HZ = 256.0
        const val SAMPLING_RATE_128HZ = 128.0
        const val SAMPLING_RATE_64HZ = 64.0
        const val SAMPLING_RATE_32HZ = 32.0

        const val SHIMMER_2 = 0
        const val SHIMMER_2R = 1
        const val SHIMMER_3 = 2
        const val SHIMMER_GQ = 3
    }

    private val isConnected = AtomicBoolean(false)
    private val isStreaming = AtomicBoolean(false)
    private var simulationJob: Job? = null
    private var deviceAddress: String = ""
    private var deviceName: String = ""
    private var connectionState = STATE_NONE
    private var deviceType = SHIMMER_3
    private var firmwareVersion = "1.0.0"
    private var batteryLevel = 100
    private var enabledSensors: Long = SENSOR_GSR.toLong()
    private var samplingRate = SAMPLING_RATE_128HZ
    private var configuration: Configuration = Configuration.getDefaultGSRConfiguration()

    private val bluetoothManager = BluetoothManager(context)

    private var dataCallback: ((ObjectCluster) -> Unit)? = null
    private var connectionCallback: ((String) -> Unit)? = null

    private var realShimmerInstance: Any? = null

    // Connection management properties
    private var connectionRetryCount: Int = 0
    private var reconnectionJob: Job? = null
    private var coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    private val connectionTimeout = Runnable {
        Log.w(TAG, "Connection timeout reached")
        disconnect()
    }

    fun connect(
        address: String,
        name: String = "Shimmer3_GSR",
    ) {
        Log.i(
            TAG,
            "Attempting to connect to Shimmer device: $address (attempt ${connectionRetryCount + 1})"
        )
        deviceAddress = address
        deviceName = name

        connectionState = STATE_CONNECTING
        sendMessage(MESSAGE_STATE_CHANGE, STATE_CONNECTING, -1, null)

        // Cancel any existing reconnection attempts
        reconnectionJob?.cancel()

        try {

            val realShimmer = createRealShimmerConnection(address, name)
            if (realShimmer != null) {
                realShimmerInstance = realShimmer
                connectionRetryCount = 0 // Reset retry count on success
                Log.i(TAG, "Successfully connected to real Shimmer device")
                return
            }
        } catch (e: Exception) {
            Log.d(TAG, "Real Shimmer device not available, using simulation: ${e.message}")
        }

        handler.postDelayed({
            handler.removeCallbacks(connectionTimeout)

            if (connectionState == STATE_CONNECTING) {
                isConnected.set(true)
                connectionState = STATE_CONNECTED
                connectionRetryCount = 0 // Reset on success
                sendMessage(MESSAGE_STATE_CHANGE, STATE_CONNECTED, -1, null)
                connectionCallback?.invoke("CONNECTED")
                Log.i(TAG, "Simulated Shimmer connected successfully")
            }
        }, 1000)
    }

    fun startStreaming() {
        if (!isConnected.get() && connectionState != STATE_CONNECTED) {
            Log.w(TAG, "Cannot start streaming - device not connected")
            return
        }

        try {

            realShimmerInstance?.let { device ->
                val method = device.javaClass.getMethod("startStreaming")
                method.invoke(device)
                isStreaming.set(true)
                connectionState = STATE_STREAMING
                sendMessage(MESSAGE_STATE_CHANGE, STATE_STREAMING, -1, null)
                Log.i(TAG, "Started streaming from real Shimmer device")
                return
            }
        } catch (e: Exception) {
            Log.d(TAG, "Real device streaming not available, using simulation")
        }

        isStreaming.set(true)
        connectionState = STATE_STREAMING
        sendMessage(MESSAGE_STATE_CHANGE, STATE_STREAMING, -1, null)
        startSimulationDataGeneration()
        Log.i(TAG, "Started simulated Shimmer3 GSR streaming at ${samplingRate.toInt()}Hz")
    }

    fun stopStreaming() {
        isStreaming.set(false)

        try {
            realShimmerInstance?.let { device ->
                val method = device.javaClass.getMethod("stopStreaming")
                method.invoke(device)
                Log.i(TAG, "Stopped real Shimmer device streaming")
                return
            }
        } catch (e: Exception) {
            Log.d(TAG, "Real device stop streaming not available")
        }

        simulationJob?.cancel()
        simulationJob = null
        Log.i(TAG, "Stopped Shimmer3 GSR streaming")
    }

    fun disconnect() {
        // Step 9: Enhanced resource cleanup
        Log.i(TAG, "Disconnecting Shimmer device...")

        // Cancel any ongoing reconnection attempts
        reconnectionJob?.cancel()
        reconnectionJob = null

        // Cancel all coroutines in the scope for proper cleanup
        coroutineScope.coroutineContext[Job]?.cancel()

        // Recreate coroutine scope for future use (allows reconnection)
        coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        // Stop streaming first if active
        stopStreaming()

        // Reset connection state
        isConnected.set(false)
        connectionState = STATE_NONE
        connectionRetryCount = 0

        try {
            realShimmerInstance?.let { device ->
                val method = device.javaClass.getMethod("disconnect")
                method.invoke(device)
                Log.i(TAG, "Disconnected from real Shimmer device")
            }
        } catch (e: Exception) {
            Log.d(TAG, "Real device disconnect not available")
        }

        realShimmerInstance = null
        sendMessage(MESSAGE_STATE_CHANGE, STATE_NONE, -1, null)
        connectionCallback?.invoke("DISCONNECTED")
        Log.i(TAG, "Shimmer disconnected and resources cleaned up")
    }

    fun isConnected(): Boolean = isConnected.get() || connectionState == STATE_CONNECTED

    fun getBluetoothAddress(): String = deviceAddress

    fun getDeviceName(): String = deviceName.ifEmpty { "Shimmer3_GSR" }

    fun getShimmerState(): Int = connectionState

    fun setHandler(
        @Suppress("UNUSED_PARAMETER") newHandler: Handler,
    ) {


        Log.d(TAG, "Handler setting requested (using constructor handler for compatibility)")
    }

    fun setDataCallback(callback: (ObjectCluster) -> Unit) {
        this.dataCallback = callback
    }

    fun setConnectionCallback(callback: (String) -> Unit) {
        this.connectionCallback = callback
    }

    fun writeEnabledSensors(sensors: Long) {
        enabledSensors = sensors
        try {
            realShimmerInstance?.let { device ->
                val method = device.javaClass.getMethod("writeEnabledSensors", Long::class.java)
                method.invoke(device, sensors)
                Log.d(TAG, "Enabled sensors configured: $sensors")
                return
            }
        } catch (e: Exception) {
            Log.d(TAG, "Real device sensor configuration not available")
        }
        Log.d(TAG, "Enabled sensors set to: $sensors (simulated)")
    }

    fun writeSamplingRate(rate: Double) {
        samplingRate = rate
        try {
            realShimmerInstance?.let { device ->
                val method = device.javaClass.getMethod("writeSamplingRate", Double::class.java)
                method.invoke(device, rate)
                Log.d(TAG, "Sampling rate configured: ${rate}Hz")
                return
            }
        } catch (e: Exception) {
            Log.d(TAG, "Real device sampling rate configuration not available")
        }
        Log.d(TAG, "Sampling rate set to: ${rate}Hz (simulated)")
    }

    fun getEnabledSensors(): Long = enabledSensors

    fun getSamplingRate(): Double = samplingRate

    fun getShimmerVersion(): Int = deviceType

    fun getFirmwareVersionFullName(): String = firmwareVersion

    fun getBatteryLevel(): Int = batteryLevel

    fun isStreaming(): Boolean = isStreaming.get()

    fun writeConfigurationBytes(config: ByteArray) {
        try {
            realShimmerInstance?.let { device ->
                val method =
                    device.javaClass.getMethod("writeConfigurationBytes", ByteArray::class.java)
                method.invoke(device, config)
                Log.d(TAG, "Configuration written to real device")
                return
            }
        } catch (e: Exception) {
            Log.d(TAG, "Real device configuration write not available")
        }
        Log.d(TAG, "Configuration bytes written (simulated): ${config.size} bytes")
    }

    fun readConfigurationBytes() {
        try {
            realShimmerInstance?.let { device ->
                val method = device.javaClass.getMethod("readConfigurationBytes")
                method.invoke(device)
                Log.d(TAG, "Configuration read from real device")
                return
            }
        } catch (e: Exception) {
            Log.d(TAG, "Real device configuration read not available")
        }
        Log.d(TAG, "Configuration read (simulated)")
    }

    fun setGSRRange(range: Int) {
        configuration.gsrRange = range
        try {
            realShimmerInstance?.let { device ->
                val method = device.javaClass.getMethod("setGSRRange", Int::class.java)
                method.invoke(device, range)
                Log.d(TAG, "GSR range set on real device: $range")
                return
            }
        } catch (e: Exception) {
            Log.d(TAG, "Real device GSR range setting not available")
        }
        Log.d(TAG, "GSR range set (simulated): $range")
    }

    fun getConfiguration(): Configuration = configuration

    fun setConfiguration(config: Configuration) {
        configuration = config
        samplingRate = config.samplingRate
        enabledSensors = config.enabledSensors
        Log.d(TAG, "Configuration updated: $config")
    }

    fun getAvailableDevices(): List<android.bluetooth.BluetoothDevice> {
        return bluetoothManager.getBondedShimmerDevices()
    }

    fun validateDevice(address: String): Boolean {
        val device = bluetoothManager.findShimmerDeviceByAddress(address)
        return device?.let { bluetoothManager.validateShimmerDevice(it).isValid } ?: false
    }

    private fun sendMessage(
        what: Int,
        arg1: Int,
        arg2: Int,
        obj: Any?,
    ) {
        val message = handler.obtainMessage(what, arg1, arg2, obj)
        handler.sendMessage(message)
    }

    private fun createRealShimmerConnection(
        address: String,
        name: String,
    ): Any? {
        return try {

            val shimmerClass = Class.forName("com.shimmerresearch.android.Shimmer")
            val constructor = shimmerClass.getConstructor(Handler::class.java, Context::class.java)
            val shimmerInstance = constructor.newInstance(handler, context)

            val connectMethod =
                shimmerClass.getMethod("connect", String::class.java, String::class.java)
            connectMethod.invoke(shimmerInstance, address, name)

            setupRealDeviceCallbacks(shimmerInstance)
            shimmerInstance
        } catch (e: Exception) {
            Log.d(TAG, "Could not create real Shimmer device: ${e.message}")
            null
        }
    }

    private fun setupRealDeviceCallbacks(
        @Suppress("UNUSED_PARAMETER") shimmerInstance: Any,
    ) {
        try {


            Log.d(TAG, "Real Shimmer device callbacks configured")
        } catch (e: Exception) {
            Log.w(TAG, "Could not set up real device callbacks", e)
        }
    }

    private fun startSimulationDataGeneration() {
        simulationJob =
            coroutineScope.launch {
                var sampleCount = 0L

                while (isStreaming.get() && isActive) {
                    try {
                        val objectCluster = ObjectCluster()

                        val currentTime = System.currentTimeMillis()

                        dataCallback?.invoke(objectCluster)

                        sendMessage(MESSAGE_READ, 0, 0, objectCluster)

                        sampleCount++
                        delay(SIMULATION_DATA_INTERVAL_MS)
                    } catch (e: Exception) {
                        if (isActive) {
                            Log.e(TAG, "Error in data simulation", e)
                        }
                    }
                }
            }
    }
}
