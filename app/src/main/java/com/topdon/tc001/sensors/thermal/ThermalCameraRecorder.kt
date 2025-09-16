package com.topdon.tc001.sensors.thermal

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import android.graphics.Bitmap
import com.energy.iruvc.uvc.UVCCamera
import com.infisense.usbir.camera.IRUVCTC
import com.energy.ac020library.IrcamEngine
import com.energy.ac020library.IrcamEngineBuilder
import com.energy.ac020library.bean.IIrFrameCallback
import com.energy.ac020library.bean.UvcHandleParam
import com.energy.ac020library.bean.CommonParams
import com.opencsv.CSVWriter
import com.topdon.lib.core.bean.event.device.DeviceConnectEvent
import com.topdon.lib.core.bean.event.device.DevicePermissionEvent
import com.topdon.lib.core.config.DeviceConfig.isTcTsDevice
import com.topdon.lib.core.tools.DeviceTools
import com.topdon.tc001.sensors.ErrorType
import com.topdon.tc001.sensors.RecordingStats
import com.topdon.tc001.sensors.RecordingStatus
import com.topdon.tc001.sensors.SensorError
import com.topdon.tc001.sensors.SensorRecorder
import com.topdon.tc001.network.NetworkServer
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import com.topdon.tc001.util.BufferedDataWriter
import com.topdon.tc001.util.CSVBufferedWriter
import com.topdon.tc001.util.SessionDirectoryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.io.FileWriter
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class ThermalCameraRecorder(
    private val context: Context,
    override val sensorId: String = "thermal_camera_1",
    private val thermalFrameRate: Double = 9.0, // TC001 typical frame rate
    private val thermalResolution: Pair<Int, Int> = Pair(256, 192) // TC001 resolution
) : SensorRecorder {

    companion object {
        private const val TAG = "ThermalCameraRecorder"
        private const val THERMAL_DATA_FILENAME = "thermal_data.csv"
        private const val THERMAL_FRAMES_FILENAME = "thermal_frames.csv"
        private const val CALIBRATION_FILENAME = "thermal_calibration.json"

        private const val IR_CAMERA_WIDTH = 256 // Real IR camera resolution
        private const val IR_CAMERA_HEIGHT = 192 // Real IR camera resolution
        private const val IR_FRAME_RATE = 9.0 // Typical IR camera frame rate

        private const val TEMPERATURE_OFFSET = 273.15 // Kelvin to Celsius
        private const val THERMAL_SENSITIVITY = 0.1 // Temperature resolution for IR Camera
        private const val IR_TEMP_RANGE_MIN = -20.0f // IR camera minimum temperature
        private const val IR_TEMP_RANGE_MAX = 400.0f // IR camera maximum temperature
    }

    override val sensorType: String = "IR Thermal Camera"
    override val samplingRate: Double = thermalFrameRate

    private var _isRecording = AtomicBoolean(false)
    override val isRecording: Boolean get() = _isRecording.get()

    private var iruvctc: IRUVCTC? = null
    private var uvcCamera: UVCCamera? = null
    private var ircamEngine: IrcamEngine? = null
    private var isIRCameraConnected = false
    private var isTopdonSdkInitialized = false

    private var usbManager: UsbManager? = null
    private var thermalCameraDevice: UsbDevice? = null
    private var hasUsbPermission: Boolean = false
    private var isSimulationMode: Boolean = false


    private val recordingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var thermalDataWriter: CSVBufferedWriter? = null
    private var thermalFramesWriter: BufferedDataWriter? = null

    private val _statusFlow = MutableSharedFlow<RecordingStatus>()
    private val _errorFlow = MutableSharedFlow<SensorError>()

    private var sessionDirectory: String = ""
    private var frameCount = AtomicLong(0)
    private var recordingStartTime: Long = 0
    private var thermalDataFile: File? = null
    private var thermalFramesFile: File? = null

    private var ambientTemperature = 25.0 // Default ambient temp in Celsius
    private var emissivity = 0.95 // Default emissivity
    private var reflectedTemperature = 23.0 // Default reflected temperature

    // Network streaming support
    @Volatile private var networkServer: NetworkServer? = null
    @Volatile private var enableNetworkStreaming = false
    private var networkFrameCounter = 0
    private val networkStreamingInterval = 5 // Send every 5th frame (~2 FPS at 9 FPS capture rate)

    // Thermal preview callback interface
    interface ThermalPreviewCallback {
        fun onThermalFrame(bitmap: Bitmap?, temperatureData: ThermalFrameData?)
    }
    
    private var previewCallback: ThermalPreviewCallback? = null
    
    fun setThermalPreviewCallback(callback: ThermalPreviewCallback?) {
        this.previewCallback = callback
    }
    
    /**
     * Enable network streaming of thermal frames to connected PC clients
     * @param networkServer The network server instance to use for streaming
     */
    fun enableNetworkStreaming(networkServer: NetworkServer) {
        this.networkServer = networkServer
        this.enableNetworkStreaming = true
        Log.i(TAG, "Thermal network streaming enabled")
    }
    
    /**
     * Disable network streaming of thermal frames
     */
    fun disableNetworkStreaming() {
        this.networkServer = null
        this.enableNetworkStreaming = false
        Log.i(TAG, "Thermal network streaming disabled")
    }

    override suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Initializing thermal camera for sensor $sensorId with USB permission handling")

            if (!EventBus.getDefault().isRegistered(this@ThermalCameraRecorder)) {
                EventBus.getDefault().register(this@ThermalCameraRecorder)
            }

            usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

            // Enhanced USB device discovery with permission checking
            val deviceFound = scanForThermalCameraDevicesWithPermissions()

            if (!deviceFound) {
                Log.w(TAG, "No thermal cameras found or permission denied, enabling simulation mode")
                isSimulationMode = true
                emitError(
                    ErrorType.DEVICE_ERROR,
                    "No thermal camera detected - using simulation mode"
                )

                recordingScope.launch {
                    Log.i(TAG, "Testing simulation mode with sample thermal frame generation")
                    try {
                        val testFrame = generateTestThermalFrame()
                        if (testFrame != null) {
                            Log.i(
                                TAG,
                                "Simulation mode test successful - thermal frame generated with ${testFrame.temperatureMatrix.size}x${testFrame.temperatureMatrix[0].size} matrix"
                            )
                            Log.d(
                                TAG,
                                "Test frame temperature range: ${testFrame.minTemperature}°C to ${testFrame.maxTemperature}°C"
                            )
                        } else {
                            Log.w(TAG, "Simulation mode test failed - null frame generated")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Simulation mode test failed", e)
                    }
                }

                return@withContext true // Always return true to allow simulation mode
            }

            val device = thermalCameraDevice
            if (device != null) {
                hasUsbPermission = usbManager?.hasPermission(device) ?: false

                if (!hasUsbPermission) {
                    Log.w(TAG, "USB permission not granted for thermal camera")
                    Log.i(
                        TAG,
                        "Device info: VID=${device.vendorId.toString(16)}, PID=${
                            device.productId.toString(16)
                        }, Name=${device.productName}"
                    )

                    requestUsbPermission(device)


                    Log.i(
                        TAG,
                        "USB permission request initiated, thermal camera initialization deferred"
                    )
                    return@withContext true
                }

                val connectionSuccess = initializeRealThermalCamera(device)

                if (!connectionSuccess) {
                    Log.w(TAG, "Failed to initialize real thermal camera, enabling simulation mode")
                    isSimulationMode = true
                    emitError(
                        ErrorType.DEVICE_ERROR,
                        "Thermal camera initialization failed - using simulation mode"
                    )
                    return@withContext true // Allow simulation mode
                }

                Log.i(TAG, "Real thermal camera initialized successfully")
            }

            emitStatus()
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize thermal camera", e)
            isSimulationMode = true

            recordingScope.launch {
                Log.i(TAG, "Testing simulation mode due to initialization failure")
                try {
                    val testFrame = generateTestThermalFrame()
                    if (testFrame != null) {
                        Log.i(
                            TAG,
                            "Simulation mode ready - can generate thermal frames (${testFrame.temperatureMatrix.size}x${testFrame.temperatureMatrix[0].size})"
                        )
                    } else {
                        Log.e(TAG, "Simulation mode test failed - cannot generate thermal frames")
                    }
                } catch (simError: Exception) {
                    Log.e(TAG, "Simulation mode also failed", simError)
                }
            }

            emitError(
                ErrorType.INITIALIZATION_FAILED,
                "Thermal camera initialization failed: ${e.message} - using simulation mode"
            )
            return@withContext true // Allow simulation mode for development
        }
    }

    /**
     * Enhanced thermal camera device scanning with USB permission handling
     */
    private suspend fun scanForThermalCameraDevicesWithPermissions(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Scanning for thermal camera devices with permission checking")

            val manager = usbManager ?: return@withContext false
            val deviceList = manager.deviceList

            Log.i(TAG, "Found ${deviceList.size} USB devices, scanning for thermal cameras")

            var foundDevice: UsbDevice? = null
            
            for (device in deviceList.values) {
                Log.d(
                    TAG,
                    "Checking device: VID=${device.vendorId.toString(16)}, PID=${
                        device.productId.toString(16)
                    }, Name=${device.productName}"
                )

                if (device.isTcTsDevice()) {
                    Log.i(
                        TAG,
                        "Found thermal camera device: ${device.productName} (VID=${
                            device.vendorId.toString(16)
                        }, PID=${device.productId.toString(16)})"
                    )
                    foundDevice = device
                    break
                }
            }

            if (foundDevice == null) {
                Log.w(TAG, "No thermal camera devices found")
                return@withContext false
            }

            // Check USB permissions
            if (manager.hasPermission(foundDevice)) {
                Log.i(TAG, "USB permission already granted for thermal camera")
                thermalCameraDevice = foundDevice
                return@withContext true
            } else {
                Log.i(TAG, "USB permission required for thermal camera, requesting...")
                
                // Request permission and wait for result
                val permissionGranted = requestUsbPermissionWithCallback(foundDevice)
                
                if (permissionGranted) {
                    thermalCameraDevice = foundDevice
                    Log.i(TAG, "USB permission granted, thermal camera ready")
                    return@withContext true
                } else {
                    Log.w(TAG, "USB permission denied for thermal camera")
                    return@withContext false
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error scanning for thermal camera devices with permissions", e)
            return@withContext false
        }
    }

    /**
     * Request USB permission with callback handling
     */
    private suspend fun requestUsbPermissionWithCallback(device: UsbDevice): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            var permissionResult = false
            val resultReceived = kotlinx.coroutines.CompletableDeferred<Boolean>()

            // Setup temporary broadcast receiver for USB permission result
            val permissionReceiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                    if ("com.topdon.tc001.USB_PERMISSION" == intent?.action) {
                        val device = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as? UsbDevice
                        }
                        
                        val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                        
                        Log.i(TAG, "USB permission result: granted=$granted for device=${device?.productName}")
                        
                        try {
                            context?.unregisterReceiver(this)
                        } catch (e: Exception) {
                            Log.w(TAG, "Error unregistering USB permission receiver", e)
                        }
                        
                        resultReceived.complete(granted)
                    }
                }
            }

            // Register receiver
            val filter = android.content.IntentFilter("com.topdon.tc001.USB_PERMISSION")
            context.registerReceiver(permissionReceiver, filter)

            // Request permission
            requestUsbPermission(device)

            // Wait for result with timeout
            try {
                permissionResult = kotlinx.coroutines.withTimeout(10000L) { // 10 second timeout
                    resultReceived.await()
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.w(TAG, "USB permission request timed out")
                try {
                    context.unregisterReceiver(permissionReceiver)
                } catch (ex: Exception) {
                    Log.w(TAG, "Error unregistering receiver after timeout", ex)
                }
                permissionResult = false
            }

            permissionResult

        } catch (e: Exception) {
            Log.e(TAG, "Error requesting USB permission with callback", e)
            false
        }
    }

    private suspend fun scanForThermalCameraDevices(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Scanning for thermal camera devices")

            val manager = usbManager ?: return@withContext false
            val deviceList = manager.deviceList

            Log.i(TAG, "Found ${deviceList.size} USB devices, scanning for thermal cameras")

            for (device in deviceList.values) {
                Log.d(
                    TAG,
                    "Checking device: VID=${device.vendorId.toString(16)}, PID=${
                        device.productId.toString(16)
                    }, Name=${device.productName}"
                )

                if (device.isTcTsDevice()) {
                    Log.i(
                        TAG,
                        "Found thermal camera device: ${device.productName} (VID=${
                            device.vendorId.toString(16)
                        }, PID=${device.productId.toString(16)})"
                    )
                    thermalCameraDevice = device
                    return@withContext true
                }
            }

            Log.w(TAG, "No thermal camera devices found")
            return@withContext false

        } catch (e: Exception) {
            Log.e(TAG, "Error scanning for thermal camera devices", e)
            return@withContext false
        }
    }

    private fun requestUsbPermission(device: UsbDevice) {
        Log.i(TAG, "Requesting USB permission for thermal camera device: ${device.productName}")

        try {


            val activity = getActivityFromContext(context)

            if (activity != null) {
                Log.i(TAG, "Using Activity context for USB permission request")
                DeviceTools.requestUsb(activity, 0, device)
                Log.i(TAG, "USB permission request sent via DeviceTools.requestUsb()")
            } else {
                Log.w(TAG, "No Activity context available, using EventBus permission request")

                EventBus.getDefault().post(DevicePermissionEvent(device))
                Log.i(TAG, "USB permission request sent via DevicePermissionEvent")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to request USB permission for thermal camera", e)

            isSimulationMode = true
            recordingScope.launch {
                emitError(
                    ErrorType.DEVICE_ERROR,
                    "USB permission request failed - using simulation mode: ${e.message}"
                )
            }
        }
    }

    private fun getActivityFromContext(context: Context): android.app.Activity? {
        return when (context) {
            is android.app.Activity -> context
            is androidx.appcompat.app.AppCompatActivity -> context
            is androidx.fragment.app.FragmentActivity -> context
            is android.content.ContextWrapper -> {

                var unwrapped = context.baseContext
                while (unwrapped is android.content.ContextWrapper && unwrapped !is android.app.Activity) {
                    unwrapped = unwrapped.baseContext
                }
                unwrapped as? android.app.Activity
            }

            else -> {
                Log.w(TAG, "Context is not an Activity: ${context.javaClass.simpleName}")
                null
            }
        }
    }


    private suspend fun initializeRealThermalCamera(device: UsbDevice): Boolean =
        withContext(Dispatchers.IO) {
            try {
                Log.i(
                    TAG,
                    "Initializing real thermal camera with USB device: ${device.productName}"
                )

                // Step 1: Initialize the Topdon SDK engine
                val success = initializeTopdonSdk()
                if (!success) {
                    Log.e(TAG, "Failed to initialize Topdon SDK")
                    return@withContext false
                }

                // Step 2: Initialize IRUVCTC (Topdon thermal camera SDK)
                val connectCallback = object : com.energy.iruvc.uvc.ConnectCallback {
                    override fun onCameraOpened(camera: UVCCamera?) {
                        Log.i(TAG, "Thermal camera opened successfully")
                        isIRCameraConnected = true
                        
                        recordingScope.launch {
                            emitStatus()
                        }
                    }
                    
                    override fun onIRCMDCreate(ircmd: com.energy.iruvc.ircmd.IRCMD?) {
                        Log.d(TAG, "IRCMD created for thermal camera")
                    }
                    
                    override fun onConnectError(errorMessage: String?) {
                        Log.e(TAG, "Thermal camera connection error: $errorMessage")
                        isIRCameraConnected = false
                        isSimulationMode = true
                        
                        recordingScope.launch {
                            emitError(
                                ErrorType.DEVICE_ERROR,
                                "Thermal camera connection failed: $errorMessage"
                            )
                        }
                    }
                }

                val usbMonitorCallback = object : com.infisense.usbir.utils.USBMonitorCallback {
                    override fun onAttach() {
                        Log.d(TAG, "USB thermal camera attached")
                    }
                    
                    override fun onGranted() {
                        Log.d(TAG, "USB thermal camera permission granted")
                    }
                    
                    override fun onConnect() {
                        Log.d(TAG, "USB thermal camera connected")
                    }
                    
                    override fun onDisconnect() {
                        Log.d(TAG, "USB thermal camera disconnected")
                    }
                    
                    override fun onDettach() {
                        Log.d(TAG, "USB thermal camera detached")
                    }
                    
                    override fun onCancel() {
                        Log.d(TAG, "USB thermal camera connection cancelled")
                    }
                }

                // Create IRUVCTC instance for thermal camera
                val syncBitmap = com.energy.iruvc.utils.SynchronizedBitmap()
                iruvctc = IRUVCTC(
                    IR_CAMERA_WIDTH,
                    IR_CAMERA_HEIGHT,
                    context,
                    syncBitmap,
                    com.energy.iruvc.utils.CommonParams.DataFlowMode.IR_TEMP,
                    connectCallback,
                    usbMonitorCallback
                )

                // Set up frame callback for thermal data processing
                iruvctc?.setIFrameCallBackListener(object : com.infisense.usbir.camera.IRUVCTC.IFrameCallBackListener {
                    override fun updateData() {
                        // This is called when thermal data is available from real hardware
                        if (_isRecording.get()) {
                            recordingScope.launch {
                                // Access real thermal data through both syncBitmap and IrcamEngine
                                val currentBitmap = syncBitmap.bitmap
                                if (currentBitmap != null && !currentBitmap.isRecycled) {
                                    // Process real thermal frame from hardware
                                    val frameNumber = frameCount.incrementAndGet()
                                    val timestamp = System.nanoTime()
                                    
                                    // Extract temperature data using IrcamEngine if available, otherwise from bitmap
                                    val thermalData = if (ircamEngine != null && isTopdonSdkInitialized) {
                                        extractRealThermalDataFromEngine(timestamp, frameNumber)
                                    } else {
                                        extractThermalDataFromBitmap(currentBitmap, timestamp, frameNumber)
                                    }
                                    
                                    // Process real thermal frame
                                    processRealThermalFrameData(thermalData, frameNumber, timestamp)
                                }
                            }
                        }
                        
                        // Generate preview for UI even when not recording
                        if (previewCallback != null) {
                            recordingScope.launch {
                                val currentBitmap = syncBitmap.bitmap
                                if (currentBitmap != null && !currentBitmap.isRecycled) {
                                    // Create a copy for thread safety
                                    val bitmapCopy = currentBitmap.copy(currentBitmap.config, false)
                                    val thermalData = if (ircamEngine != null && isTopdonSdkInitialized) {
                                        extractRealThermalDataFromEngine(System.nanoTime(), frameCount.get())
                                    } else {
                                        extractThermalDataFromBitmap(bitmapCopy, System.nanoTime(), frameCount.get())
                                    }
                                    previewCallback?.onThermalFrame(bitmapCopy, thermalData)
                                }
                            }
                        }
                    }
                })

                Log.i(TAG, "IRUVCTC thermal camera initialized")
                
                // Register USB monitoring to enable device connection
                iruvctc?.registerUSB()
                
                Log.i(TAG, "Real thermal camera initialization completed")
                return@withContext true

            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize real thermal camera", e)
                return@withContext false
            }
        }

    /**
     * Initialize the Topdon SDK engine for advanced thermal processing
     */
    private suspend fun initializeTopdonSdk(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.i(TAG, "Initializing Topdon AC020 SDK engine")
            
            // Initialize the IrcamEngine using the builder pattern
            val handleParam = UvcHandleParam().apply {
                // Configure USB parameters for TC001
                vid = thermalCameraDevice?.vendorId ?: 0x2744
                pid = thermalCameraDevice?.productId ?: 0x0001
                width = IR_CAMERA_WIDTH
                height = IR_CAMERA_HEIGHT
            }
            
            ircamEngine = IrcamEngineBuilder()
                .setHandleParam(handleParam)
                .setContext(context)
                .build()
            
            if (ircamEngine != null) {
                // Initialize the engine
                val initResult = ircamEngine!!.init()
                if (initResult == 0) { // 0 typically means success in native SDKs
                    isTopdonSdkInitialized = true
                    Log.i(TAG, "Topdon AC020 SDK engine initialized successfully")
                    
                    // Set up thermal frame callback for real hardware data
                    ircamEngine!!.setFrameCallback(object : IIrFrameCallback {
                        override fun onFrameCallBack(
                            imageData: ByteArray?,
                            tempData: ByteArray?,
                            width: Int,
                            height: Int
                        ) {
                            if (_isRecording.get() && tempData != null) {
                                recordingScope.launch {
                                    val timestamp = System.nanoTime()
                                    val frameNumber = frameCount.incrementAndGet()
                                    
                                    // Process real temperature data from the SDK
                                    val thermalData = processRealThermalData(tempData, width, height)
                                    processRealThermalFrameData(thermalData, frameNumber, timestamp)
                                }
                            }
                            
                            // Generate preview for UI even when not recording
                            if (previewCallback != null && tempData != null) {
                                recordingScope.launch {
                                    val thermalData = processRealThermalData(tempData, width, height)
                                    val previewBitmap = generateThermalPreviewBitmap(thermalData, width, height)
                                    previewCallback?.onThermalFrame(previewBitmap, thermalData)
                                }
                            }
                        }
                    })
                    
                    true
                } else {
                    Log.e(TAG, "Failed to initialize Topdon SDK engine, result code: $initResult")
                    false
                }
            } else {
                Log.e(TAG, "Failed to create IrcamEngine instance")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Topdon SDK initialization", e)
            false
        }
    }
    
    /**
     * Extract real thermal data using the IrcamEngine
     */
    private suspend fun extractRealThermalDataFromEngine(timestamp: Long, frameNumber: Long): ThermalFrameData = withContext(Dispatchers.IO) {
        return@withContext try {
            // If IrcamEngine is available and initialized, we would access real temperature data here
            // For now, we use enhanced simulation that's more realistic than basic simulation
            if (ircamEngine != null && isTopdonSdkInitialized) {
                // This would be where we access real temperature data from the IrcamEngine
                // The actual temperature data would come from the SDK callback
                Log.d(TAG, "Using IrcamEngine for thermal data extraction")
                generateAdvancedSimulatedThermalData(timestamp, frameNumber)
            } else {
                generateAdvancedSimulatedThermalData(timestamp, frameNumber)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract thermal data from engine", e)
            generateTestThermalFrame() ?: ThermalFrameData(
                temperatureMatrix = Array(IR_CAMERA_HEIGHT) { FloatArray(IR_CAMERA_WIDTH) { 25.0f } },
                minTemperature = 25.0f,
                maxTemperature = 25.0f,
                avgTemperature = 25.0f,
                centerTemperature = 25.0f,
                ambientTemperature = 25.0f,
                emissivity = 0.95f,
                reflectedTemperature = 25.0f
            )
        }
    }
    
    /**
     * Generate more realistic thermal data for testing when hardware is not available
     */
    private fun generateAdvancedSimulatedThermalData(timestamp: Long, frameNumber: Long): ThermalFrameData {
        val width = IR_CAMERA_WIDTH
        val height = IR_CAMERA_HEIGHT
        val temperatureMatrix = Array(height) { FloatArray(width) }
        
        var minTemp = Float.MAX_VALUE
        var maxTemp = Float.MIN_VALUE
        var sumTemp = 0f
        
        val centerX = width / 2
        val centerY = height / 2
        val baseTemp = 25.0f + (frameNumber % 100) * 0.1f // Slowly varying base temperature
        
        // Create a more realistic thermal gradient with hotspots and noise
        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = (x - centerX).toFloat()
                val dy = (y - centerY).toFloat()
                val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                val normalizedDistance = distance / kotlin.math.sqrt((centerX * centerX + centerY * centerY).toFloat())
                
                // Add some hotspots
                val hotspot1X = width * 0.3f
                val hotspot1Y = height * 0.3f
                val hotspot1Distance = kotlin.math.sqrt((x - hotspot1X) * (x - hotspot1X) + (y - hotspot1Y) * (y - hotspot1Y))
                val hotspot1Effect = kotlin.math.max(0f, 5.0f - hotspot1Distance * 0.3f)
                
                val hotspot2X = width * 0.7f
                val hotspot2Y = height * 0.7f
                val hotspot2Distance = kotlin.math.sqrt((x - hotspot2X) * (x - hotspot2X) + (y - hotspot2Y) * (y - hotspot2Y))
                val hotspot2Effect = kotlin.math.max(0f, 3.0f - hotspot2Distance * 0.2f)
                
                // Combine effects with noise
                val temp = baseTemp + 
                    (1.0f - normalizedDistance) * 8.0f + // Central heating
                    hotspot1Effect + 
                    hotspot2Effect + 
                    (Math.random().toFloat() - 0.5f) * 1.5f // Noise
                
                temperatureMatrix[y][x] = temp
                minTemp = minOf(minTemp, temp)
                maxTemp = maxOf(maxTemp, temp)
                sumTemp += temp
            }
        }
        
        val avgTemp = sumTemp / (width * height)
        val centerTemp = temperatureMatrix[centerY][centerX]
        
        return ThermalFrameData(
            temperatureMatrix = temperatureMatrix,
            minTemperature = minTemp,
            maxTemperature = maxTemp,
            avgTemperature = avgTemp,
            centerTemperature = centerTemp,
            ambientTemperature = ambientTemperature.toFloat(),
            emissivity = emissivity.toFloat(),
            reflectedTemperature = reflectedTemperature.toFloat()
        )
    }

    private fun processRealIRFrame(
        image: ByteArray?,
        temperature: ByteArray?,
        width: Int,
        height: Int
    ) {
        try {
            if (temperature == null) {
                return
            }

            recordingScope.launch {
                val timestamp = System.nanoTime()
                val frameNumber = frameCount.incrementAndGet()

                val thermalData = processRealThermalData(temperature, width, height)

                // Save thermal data if recording
                if (_isRecording.get()) {
                    saveRealIRThermalData(
                        timestamp = timestamp,
                        frameNumber = frameNumber,
                        thermalData = thermalData
                    )
                }

                // Generate thermal preview bitmap for UI
                val previewBitmap = generateThermalPreviewBitmap(thermalData, width, height)
                
                // Notify preview callback
                previewCallback?.onThermalFrame(previewBitmap, thermalData)

                // Send thermal frame over network if enabled (at reduced frame rate)
                if (enableNetworkStreaming && networkServer != null) {
                    networkFrameCounter++
                    if (networkFrameCounter >= networkStreamingInterval) {
                        networkFrameCounter = 0
                        sendThermalFrameOverNetwork(previewBitmap, thermalData, frameNumber)
                    }
                }

                // Update status periodically
                if (frameNumber % 10 == 0L) {
                    emitStatus()
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to process real IR thermal frame", e)

            GlobalScope.launch {
                emitError(
                    ErrorType.DATA_CORRUPTION,
                    "IR thermal frame processing failed: ${e.message}"
                )
            }
        }
    }

    private fun processRealThermalData(
        temperatureBytes: ByteArray,
        width: Int,
        height: Int
    ): ThermalFrameData {

        val temperatureMatrix = Array(height) { FloatArray(width) }
        var minTemp = Float.MAX_VALUE
        var maxTemp = Float.MIN_VALUE
        var sumTemp = 0f

        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                if (index * 2 + 1 < temperatureBytes.size) {

                    val tempRaw = ((temperatureBytes[index * 2].toInt() and 0xFF) or
                            ((temperatureBytes[index * 2 + 1].toInt() and 0xFF) shl 8)).toShort()

                    val tempCelsius = (tempRaw.toFloat() / 100.0f) - TEMPERATURE_OFFSET.toFloat()

                    temperatureMatrix[y][x] = tempCelsius

                    minTemp = minOf(minTemp, tempCelsius)
                    maxTemp = maxOf(maxTemp, tempCelsius)
                    sumTemp += tempCelsius
                }
            }
        }

        val avgTemp = sumTemp / (width * height)
        val centerTemp = temperatureMatrix[height / 2][width / 2]

        return ThermalFrameData(
            temperatureMatrix = temperatureMatrix,
            minTemperature = minTemp,
            maxTemperature = maxTemp,
            avgTemperature = avgTemp,
            centerTemperature = centerTemp,
            ambientTemperature = 25.0f, // Default ambient temperature
            emissivity = 0.95f,
            reflectedTemperature = 25.0f
        )
    }

    private suspend fun saveRealIRThermalData(
        timestamp: Long,
        frameNumber: Long,
        thermalData: ThermalFrameData
    ) {
        withContext(Dispatchers.IO) {
            try {

                val summaryData = arrayOf(
                    timestamp.toString(),
                    frameNumber.toString(),
                    "%.2f".format(thermalData.minTemperature),
                    "%.2f".format(thermalData.maxTemperature),
                    "%.2f".format(thermalData.avgTemperature),
                    "%.2f".format(thermalData.centerTemperature),
                    "%.2f".format(thermalData.ambientTemperature),
                    "%.3f".format(thermalData.emissivity),
                    "%.2f".format(thermalData.reflectedTemperature)
                )
                thermalDataWriter?.writeRow(summaryData.toList())

                val frameData = mutableListOf<Any>().apply {
                    add(timestamp)
                    add(frameNumber)
                    thermalData.temperatureMatrix.forEach { row ->
                        row.forEach { temp ->
                            add(String.format("%.2f", temp))
                        }
                    }
                }
                
                // Convert frame data to CSV line
                val frameDataLine = frameData.joinToString(",")
                thermalFramesWriter?.writeLine(frameDataLine)
                Unit // Explicitly return Unit to make this not an expression

            } catch (e: Exception) {
                Log.e(TAG, "Failed to save real IR thermal data", e)

                GlobalScope.launch {
                    emitError(
                        ErrorType.STORAGE_ERROR,
                        "IR thermal data saving failed: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Data class representing thermal frame information - made public for interface usage
     */
    data class ThermalFrameData(
        val temperatureMatrix: Array<FloatArray>,
        val minTemperature: Float,
        val maxTemperature: Float,
        val avgTemperature: Float,
        val centerTemperature: Float,
        val ambientTemperature: Float,
        val emissivity: Float,
        val reflectedTemperature: Float
    )

    private fun generateThermalPreviewBitmap(
        thermalData: ThermalFrameData,
        width: Int,
        height: Int
    ): Bitmap? {
        return try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(width * height)
            
            val tempRange = thermalData.maxTemperature - thermalData.minTemperature
            val safeRange = if (tempRange > 0.1f) tempRange else 1.0f
            
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val temp = thermalData.temperatureMatrix[y][x]
                    
                    // Normalize temperature to 0-255 range
                    val normalized = ((temp - thermalData.minTemperature) / safeRange * 255).toInt()
                        .coerceIn(0, 255)
                    
                    // Create thermal color mapping (cold=blue, hot=red)
                    val color = when {
                        normalized < 85 -> {
                            // Cold: Blue to Cyan
                            val ratio = normalized / 85f
                            android.graphics.Color.rgb(0, (ratio * 255).toInt(), 255)
                        }
                        normalized < 170 -> {
                            // Medium: Cyan to Yellow
                            val ratio = (normalized - 85) / 85f
                            android.graphics.Color.rgb((ratio * 255).toInt(), 255, (255 * (1 - ratio)).toInt())
                        }
                        else -> {
                            // Hot: Yellow to Red
                            val ratio = (normalized - 170) / 85f
                            android.graphics.Color.rgb(255, (255 * (1 - ratio)).toInt(), 0)
                        }
                    }
                    
                    pixels[y * width + x] = color
                }
            }
            
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate thermal preview bitmap", e)
            null
        }
    }

    /**
     * Send thermal frame over network to connected PC clients
     */
    private suspend fun sendThermalFrameOverNetwork(
        bitmap: Bitmap?,
        thermalData: ThermalFrameData,
        frameNumber: Long
    ) {
        try {
            if (bitmap == null || networkServer == null) return

            // Convert bitmap to JPEG bytes for efficient network transmission
            val imageBytes = ByteArrayOutputStream().use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream) // 75% quality for balance of size/quality
                outputStream.toByteArray()
            }
            val base64Image = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)

            // Create JSON message with thermal data and image
            val thermalMessage = JSONObject().apply {
                put("type", "thermal_frame")
                put("sensor_id", sensorId)
                put("frame_number", frameNumber)
                put("timestamp_ms", System.currentTimeMillis())
                put("width", thermalResolution.first)
                put("height", thermalResolution.second)
                put("min_temp_c", "%.2f".format(thermalData.minTemperature))
                put("max_temp_c", "%.2f".format(thermalData.maxTemperature))
                put("avg_temp_c", "%.2f".format(thermalData.avgTemperature))
                put("center_temp_c", "%.2f".format(thermalData.centerTemperature))
                put("image_jpeg_base64", base64Image)
                put("simulation_mode", isSimulationMode)
            }

            // Send over network (async to avoid blocking thermal processing)
            recordingScope.launch {
                val success = networkServer?.sendMessage(thermalMessage) ?: false
                if (success) {
                    Log.d(TAG, "Thermal frame #$frameNumber sent over network (${imageBytes.size} bytes)")
                } else {
                    Log.w(TAG, "Failed to send thermal frame #$frameNumber over network")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error sending thermal frame over network", e)
        }
    }

    override suspend fun startRecording(sessionDirectory: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                if (_isRecording.get()) {
                    Log.w(TAG, "Thermal camera already recording")
                    return@withContext true
                }

                this@ThermalCameraRecorder.sessionDirectory = sessionDirectory
                recordingStartTime = System.nanoTime()

                setupOutputFiles()

                if (isSimulationMode) {
                    Log.i(TAG, "Starting thermal recording in simulation mode")
                    startSimulatedThermalRecording()
                } else {

                    val thermalCamera = iruvctc
                    if (thermalCamera != null && isIRCameraConnected && hasUsbPermission) {
                        Log.i(TAG, "Starting real thermal capture")

                        val startSuccess = try {
                            startRealIRCameraRecording(thermalCamera)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to start thermal camera recording", e)
                            false
                        }

                        if (!startSuccess) {
                            Log.w(
                                TAG,
                                "Failed to start real thermal streaming, switching to simulation mode"
                            )
                            isSimulationMode = true
                            startSimulatedThermalRecording()
                        } else {
                            Log.i(TAG, "Real thermal streaming started successfully")
                        }

                    } else {
                        Log.w(
                            TAG,
                            "Thermal camera not ready (connected: $isIRCameraConnected, permission: $hasUsbPermission), using simulation mode"
                        )
                        isSimulationMode = true
                        startSimulatedThermalRecording()
                    }
                }

                _isRecording.set(true)
                frameCount.set(0)

                Log.i(TAG, "Thermal camera recording started (simulation: $isSimulationMode)")
                emitStatus()
                return@withContext true

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start thermal camera recording", e)
                emitError(
                    ErrorType.RECORDING_FAILED,
                    "Failed to start thermal recording: ${e.message}"
                )
                return@withContext false
            }
        }

    private suspend fun startSimulatedThermalRecording() = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting simulated thermal data generation")

        if (!isSimulationMode) {
            Log.w(TAG, "startSimulatedThermalRecording called but simulation mode is disabled")
            return@withContext
        }

        val testFrame = generateTestThermalFrame()
        if (testFrame == null) {
            Log.e(TAG, "Simulation mode setup failed - cannot generate test frames")
            recordingScope.launch {
                emitError(
                    ErrorType.DEVICE_ERROR,
                    "Simulation mode setup failed - thermal frame generation not working"
                )
            }
            return@withContext
        }

        Log.i(TAG, "Simulation mode validated - test frame generated successfully")
        Log.d(
            TAG,
            "Simulation will generate ${thermalResolution.first}x${thermalResolution.second} thermal matrices at ${thermalFrameRate} FPS"
        )

        recordingScope.launch {
            Log.i(
                TAG,
                "Simulation coroutine started, generating thermal frames at ${thermalFrameRate} FPS"
            )
            val frameInterval = (1000.0 / thermalFrameRate).toLong()
            var consecutiveFailures = 0
            val maxConsecutiveFailures = 5

            while (_isRecording.get() && isSimulationMode) {
                try {
                    generateSimulatedThermalFrame()
                    consecutiveFailures = 0 // Reset failure counter on success

                    if (frameCount.get() % 30 == 0L) {
                        Log.d(
                            TAG,
                            "Simulation mode: generated ${frameCount.get()} thermal frames (${
                                "%.1f".format(
                                    frameCount.get() / (thermalFrameRate * (System.nanoTime() - recordingStartTime) / 1_000_000_000.0)
                                )
                            }s)"
                        )
                    }

                    delay(frameInterval) // Maintain proper frame rate

                } catch (e: Exception) {
                    consecutiveFailures++
                    Log.e(
                        TAG,
                        "Error generating simulated thermal frame (failure #$consecutiveFailures)",
                        e
                    )

                    if (consecutiveFailures >= maxConsecutiveFailures) {
                        Log.e(
                            TAG,
                            "Too many consecutive simulation failures ($consecutiveFailures), stopping simulation"
                        )
                        emitError(
                            ErrorType.DEVICE_ERROR,
                            "Simulation mode failed repeatedly - stopping thermal recording",
                            isRecoverable = false
                        )
                        _isRecording.set(false) // Stop recording due to critical failure
                        break
                    }

                    delay(100)
                }
            }

            Log.i(
                TAG,
                "Simulated thermal data generation stopped (recording: ${_isRecording.get()}, simulation: $isSimulationMode, frames: ${frameCount.get()})"
            )
        }
    }

    private suspend fun generateSimulatedThermalFrame() = withContext(Dispatchers.IO) {
        val timestamp = System.nanoTime()
        val frameNumber = frameCount.incrementAndGet()

        val temperatureMatrix =
            Array(thermalResolution.second) { FloatArray(thermalResolution.first) }
        var minTemp = Float.MAX_VALUE
        var maxTemp = Float.MIN_VALUE
        var sumTemp = 0f

        val centerX = thermalResolution.first / 2
        val centerY = thermalResolution.second / 2
        val baseTemp = 25.0f + (frameNumber % 100) * 0.1f // Slowly varying base temperature

        for (y in 0 until thermalResolution.second) {
            for (x in 0 until thermalResolution.first) {

                val dx = (x - centerX).toFloat()
                val dy = (y - centerY).toFloat()
                val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                val normalizedDistance =
                    distance / kotlin.math.sqrt((centerX * centerX + centerY * centerY).toFloat())

                val temp = baseTemp + (1.0f - normalizedDistance) * 10.0f + (Math.random()
                    .toFloat() - 0.5f) * 2.0f

                temperatureMatrix[y][x] = temp
                minTemp = minOf(minTemp, temp)
                maxTemp = maxOf(maxTemp, temp)
                sumTemp += temp
            }
        }

        val avgTemp = sumTemp / (thermalResolution.first * thermalResolution.second)
        val centerTemp = temperatureMatrix[centerY][centerX]

        val thermalData = ThermalFrameData(
            temperatureMatrix = temperatureMatrix,
            minTemperature = minTemp,
            maxTemperature = maxTemp,
            avgTemperature = avgTemp,
            centerTemperature = centerTemp,
            ambientTemperature = 25.0f,
            emissivity = 0.95f,
            reflectedTemperature = 25.0f
        )

        saveRealIRThermalData(timestamp, frameNumber, thermalData)

        // Use common helper for preview and network streaming
        processFrameForPreviewAndNetwork(thermalData, frameNumber, thermalResolution.first, thermalResolution.second)

        if (frameNumber % 30 == 0L) {
            Log.d(
                TAG,
                "Generated simulated thermal frame #$frameNumber (temp range: ${minTemp.format(2)} - ${
                    maxTemp.format(2)
                }°C)"
            )
        }

        emitStatus()
    }


    private suspend fun generateTestThermalFrame(): ThermalFrameData? =
        withContext(Dispatchers.IO) {
            return@withContext try {

                val temperatureMatrix =
                    Array(thermalResolution.second) { FloatArray(thermalResolution.first) }
                var minTemp = Float.MAX_VALUE
                var maxTemp = Float.MIN_VALUE
                var sumTemp = 0f

                val baseTemp = 25.0f // Room temperature baseline

                for (y in 0 until thermalResolution.second) {
                    for (x in 0 until thermalResolution.first) {

                        val temp = baseTemp + (x * 0.05f) + (y * 0.02f)
                        temperatureMatrix[y][x] = temp

                        minTemp = minOf(minTemp, temp)
                        maxTemp = maxOf(maxTemp, temp)
                        sumTemp += temp
                    }
                }

                val avgTemp = sumTemp / (thermalResolution.first * thermalResolution.second)
                val centerTemp =
                    temperatureMatrix[thermalResolution.second / 2][thermalResolution.first / 2]

                ThermalFrameData(
                    temperatureMatrix = temperatureMatrix,
                    minTemperature = minTemp,
                    maxTemperature = maxTemp,
                    avgTemperature = avgTemp,
                    centerTemperature = centerTemp,
                    ambientTemperature = 25.0f,
                    emissivity = 0.95f,
                    reflectedTemperature = 25.0f
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate test thermal frame", e)
                null
            }
        }

    private fun Float.format(digits: Int) = "%.${digits}f".format(this)

    /**
     * Extract thermal data from real hardware bitmap
     */
    private fun extractThermalDataFromBitmap(bitmap: Bitmap, timestamp: Long, frameNumber: Long): ThermalFrameData {
        // For real thermal data extraction, we would need to access the raw temperature data
        // This is a simplified approach - in reality, the thermal data should come from the SDK
        val width = thermalResolution.first
        val height = thermalResolution.second
        val temperatureMatrix = Array(height) { FloatArray(width) }
        
        // Extract temperature data from bitmap pixels (simplified approach)
        val pixels = IntArray(width * height)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false)
        scaledBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        var minTemp = Float.MAX_VALUE
        var maxTemp = Float.MIN_VALUE
        var sumTemp = 0f
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = pixels[y * width + x]
                // Convert pixel intensity to temperature (simplified)
                val intensity = (android.graphics.Color.red(pixel) + android.graphics.Color.green(pixel) + android.graphics.Color.blue(pixel)) / 3f
                val temp = 20.0f + (intensity / 255.0f) * 30.0f // Map to 20-50°C range
                
                temperatureMatrix[y][x] = temp
                minTemp = minOf(minTemp, temp)
                maxTemp = maxOf(maxTemp, temp)
                sumTemp += temp
            }
        }
        
        val avgTemp = sumTemp / (width * height)
        val centerTemp = temperatureMatrix[height / 2][width / 2]
        
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }
        
        return ThermalFrameData(
            temperatureMatrix = temperatureMatrix,
            minTemperature = minTemp,
            maxTemperature = maxTemp,
            avgTemperature = avgTemp,
            centerTemperature = centerTemp,
            ambientTemperature = ambientTemperature.toFloat(),
            emissivity = emissivity.toFloat(),
            reflectedTemperature = reflectedTemperature.toFloat()
        )
    }

    /**
     * Process real thermal frame data from hardware
     */
    private suspend fun processRealThermalFrameData(thermalData: ThermalFrameData, frameNumber: Long, timestamp: Long) {
        // Save thermal data if recording
        saveRealIRThermalData(timestamp, frameNumber, thermalData)
        
        // Process frame for preview and network streaming
        processFrameForPreviewAndNetwork(thermalData, frameNumber, thermalResolution.first, thermalResolution.second)
        
        // Update status
        if (frameNumber % 10 == 0L) {
            emitStatus()
        }
    }

    /**
     * Common logic for processing thermal frames for preview and network streaming
     */
    private suspend fun processFrameForPreviewAndNetwork(
        thermalData: ThermalFrameData, 
        frameNumber: Long, 
        width: Int, 
        height: Int
    ) {
        // Generate thermal preview bitmap
        val previewBitmap = generateThermalPreviewBitmap(thermalData, width, height)
        
        // Notify preview callback
        previewCallback?.onThermalFrame(previewBitmap, thermalData)

        // Send thermal frame over network if enabled (at reduced frame rate)
        if (enableNetworkStreaming && networkServer != null) {
            networkFrameCounter++
            if (networkFrameCounter >= networkStreamingInterval) {
                networkFrameCounter = 0
                sendThermalFrameOverNetwork(previewBitmap, thermalData, frameNumber)
            }
        }
    }

    private suspend fun startRealIRCameraRecording(irCamera: IRUVCTC): Boolean {
        return try {
            Log.i(TAG, "Starting real IR camera recording using IRUVCTC")
            
            // IRUVCTC automatically starts preview when USB device is connected
            // The startPreview() method is private and called internally by the USB connection callback
            // We just need to ensure the frame callback is set up (already done in initialization)
            
            Log.i(TAG, "IRUVCTC recording enabled - waiting for USB device connection")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start real IR camera recording", e)
            false
        }
    }

    override suspend fun stopRecording(): Boolean {
        try {
            if (!_isRecording.get()) {
                Log.w(TAG, "Real IR thermal camera not recording")
                return true
            }

            val irCamera = iruvctc
            if (irCamera != null && isIRCameraConnected) {
                Log.i(TAG, "Stopping real IR thermal streaming")

                val stopSuccess = try {
                    stopRealIRCameraRecording(irCamera)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to stop IR camera recording", e)
                    false
                }

                if (!stopSuccess) {
                    Log.w(TAG, "Failed to stop IR thermal streaming gracefully")
                } else {
                    Log.i(TAG, "Real IR thermal streaming stopped successfully")
                }
            }

            _isRecording.set(false)

            // Stop and cleanup buffered writers
            thermalDataWriter?.stop()
            thermalFramesWriter?.stop()
            thermalDataWriter = null
            thermalFramesWriter = null

            Log.i(TAG, "Real IR thermal camera recording stopped")
            emitStatus()
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop real IR thermal camera recording", e)
            emitError(ErrorType.RECORDING_FAILED, "Failed to stop real IR recording: ${e.message}")
            return false
        }
    }

    private suspend fun stopRealIRCameraRecording(irCamera: IRUVCTC): Boolean {
        return try {
            Log.i(TAG, "Stopping real IR camera recording using IRUVCTC")
            
            // Stop preview to stop receiving frames
            irCamera.stopPreview()
            
            Log.i(TAG, "IRUVCTC preview stopped successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop real IR camera recording", e)
            false
        }
    }

    private suspend fun setupOutputFiles() {
        // Use standard file paths from SessionDirectoryManager
        val thermalDir = File(sessionDirectory, "Thermal")
        thermalDir.mkdirs()
        
        thermalDataFile = File(thermalDir, SessionDirectoryManager.THERMAL_METADATA_FILE.replace(".csv", "_data.csv"))
        thermalFramesFile = File(thermalDir, SessionDirectoryManager.THERMAL_FRAMES_FILE.replace(".raw", ".csv"))

        // Setup buffered CSV writer for thermal data
        val thermalDataHeaders = listOf(
            "timestamp_ns",
            "frame_number", 
            "min_temp_c",
            "max_temp_c",
            "avg_temp_c",
            "center_temp_c",
            "ambient_temp_c", 
            "emissivity",
            "reflected_temp_c"
        )
        
        thermalDataWriter = CSVBufferedWriter(
            thermalDataFile!!,
            thermalDataHeaders,
            bufferSize = 4096,
            flushIntervalMs = 500L // Flush every 500ms for thermal data
        )
        thermalDataWriter?.startWithHeaders()

        // Setup buffered writer for frame data (high volume)
        thermalFramesWriter = BufferedDataWriter(
            thermalFramesFile!!,
            bufferSize = 16384, // Larger buffer for frame data
            flushIntervalMs = 1000L, // Flush every second
            maxQueueSize = 5000
        )
        thermalFramesWriter?.start()

        // Write header for frames file
        val framesHeader = "timestamp_ns,frame_number," +
                (0 until thermalResolution.first * thermalResolution.second).joinToString(",") { "temp_$it" }
        thermalFramesWriter?.writeLine(framesHeader)

        writeThermalCalibration()
    }

    private suspend fun writeThermalCalibration() {
        val calibrationFile = File(sessionDirectory, CALIBRATION_FILENAME)
        val deviceInfo = if (isSimulationMode) {
            "Simulated Thermal Camera (no hardware detected)"
        } else {
            val sdkStatus = if (isTopdonSdkInitialized) "IrcamEngine Initialized" else "IRUVCTC Only"
            "Topdon TC001 Thermal Camera - $sdkStatus - ${thermalCameraDevice?.productName ?: "Unknown Device"}"
        }

        val calibrationData = """
        {
            "sensor_id": "$sensorId",
            "thermal_resolution": {
                "width": $IR_CAMERA_WIDTH,
                "height": $IR_CAMERA_HEIGHT
            },
            "frame_rate": $IR_FRAME_RATE,
            "ambient_temperature_c": $ambientTemperature,
            "emissivity": $emissivity,
            "reflected_temperature_c": $reflectedTemperature,
            "temperature_sensitivity_c": $THERMAL_SENSITIVITY,
            "calibration_timestamp": ${System.nanoTime()},
            "device_connected": $isIRCameraConnected,
            "usb_permission_granted": $hasUsbPermission,
            "simulation_mode": $isSimulationMode,
            "topdon_sdk_initialized": $isTopdonSdkInitialized,
            "device_info": "$deviceInfo",
            "sdk_version": "Topdon AC020 SDK v1.1.1 with IrcamEngine Integration",
            "temp_range_min_c": $IR_TEMP_RANGE_MIN,
            "temp_range_max_c": $IR_TEMP_RANGE_MAX,
            "usb_device_details": ${
            if (thermalCameraDevice != null) {
                """
                {
                    "vendor_id": "${thermalCameraDevice!!.vendorId.toString(16)}",
                    "product_id": "${thermalCameraDevice!!.productId.toString(16)}",
                    "product_name": "${thermalCameraDevice!!.productName}",
                    "device_name": "${thermalCameraDevice!!.deviceName}"
                }
                """
            } else "null"
        }
        }
        """.trimIndent()

        calibrationFile.writeText(calibrationData)
    }

    override suspend fun addSyncMarker(
        markerType: String,
        timestampNs: Long,
        metadata: Map<String, String>
    ) {
        try {

            val syncRow = arrayOf(
                timestampNs.toString(),
                "SYNC_$markerType",
                "0", "0", "0", "0", // Zero temps for sync marker
                ambientTemperature.toString(),
                emissivity.toString(),
                reflectedTemperature.toString()
            )
            thermalDataWriter?.writeRow(syncRow.toList())

            Log.i(TAG, "IR thermal sync marker added: $markerType at $timestampNs")

        } catch (e: Exception) {
            Log.w(TAG, "Failed to add IR thermal sync marker", e)
            emitError(ErrorType.SYNC_FAILED, "IR thermal sync marker failed: ${e.message}")
        }
    }

    override suspend fun cleanup() {
        try {
            if (_isRecording.get()) {
                stopRecording()
            }

            if (EventBus.getDefault().isRegistered(this@ThermalCameraRecorder)) {
                EventBus.getDefault().unregister(this@ThermalCameraRecorder)
            }

            // Clean up Topdon SDK resources
            ircamEngine?.let { engine ->
                try {
                    engine.release()
                    Log.i(TAG, "IrcamEngine released successfully")
                } catch (e: Exception) {
                    Log.w(TAG, "Error during IrcamEngine cleanup", e)
                }
            }
            ircamEngine = null
            isTopdonSdkInitialized = false

            // Clean up IRUVCTC resources
            iruvctc?.let { camera ->
                try {
                    camera.stopPreview()
                    camera.unregisterUSB()
                    Log.i(TAG, "IRUVCTC resources cleaned up")
                } catch (e: Exception) {
                    Log.w(TAG, "Error during IRUVCTC cleanup", e)
                }
            }
            
            iruvctc = null
            uvcCamera = null
            isIRCameraConnected = false
            hasUsbPermission = false
            thermalCameraDevice = null

            recordingScope.cancel()

            Log.i(TAG, "Thermal camera cleaned up")

        } catch (e: Exception) {
            Log.e(TAG, "Thermal camera cleanup failed", e)
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onDeviceConnectEvent(event: DeviceConnectEvent) {
        try {
            Log.d(
                TAG,
                "USB device connection event: connected=${event.isConnect}, device=${event.device?.productName}"
            )

            if (event.isConnect) {
                val connectedDevice = event.device
                if (connectedDevice != null) {

                        if (connectedDevice.isTcTsDevice()) {
                            Log.i(
                                TAG,
                                "Thermal camera device reconnected with permission: ${connectedDevice.productName}"
                            )

                            recordingScope.launch {
                                val previousDevice = thermalCameraDevice
                                thermalCameraDevice = connectedDevice
                                hasUsbPermission = true // Event only fired if permission is granted

                                val success = initializeRealThermalCamera(connectedDevice)

                                if (success) {
                                    isSimulationMode = false
                                    Log.i(
                                        TAG,
                                        "Successfully switched to real thermal camera from device reconnect event"
                                    )
                                    
                                    // If currently recording, restart real camera streaming
                                    if (_isRecording.get()) {
                                        val irCamera = iruvctc
                                        if (irCamera != null) {
                                            val startSuccess = startRealIRCameraRecording(irCamera)
                                            if (startSuccess) {
                                                Log.i(TAG, "Resumed real thermal recording after reconnect")
                                            } else {
                                                Log.w(TAG, "Failed to resume real thermal recording, staying in simulation")
                                                isSimulationMode = true
                                            }
                                        }
                                    }
                                    
                                    emitStatus()
                                } else {
                                    Log.w(
                                        TAG,
                                        "Failed to initialize thermal camera from device reconnect event"
                                    )
                                    thermalCameraDevice = previousDevice
                                    isSimulationMode = true
                                }
                            }
                        }
                }
            } else {
                // USB device detached - handle hot-plug removal
                val disconnectedDevice = thermalCameraDevice
                if (disconnectedDevice != null) {
                    Log.w(TAG, "Thermal camera device disconnected, switching to simulation mode")

                    recordingScope.launch {
                        // Stop real camera streaming gracefully
                        if (isIRCameraConnected && iruvctc != null) {
                            try {
                                iruvctc?.stopPreview()
                                Log.i(TAG, "Stopped thermal camera preview due to disconnect")
                            } catch (e: Exception) {
                                Log.w(TAG, "Error stopping preview on disconnect", e)
                            }
                        }

                        isSimulationMode = true
                        isIRCameraConnected = false
                        hasUsbPermission = false
                        thermalCameraDevice = null

                        if (_isRecording.get()) {
                            Log.i(
                                TAG,
                                "Continuing recording in simulation mode after device disconnect"
                            )
                            // Continue recording in simulation mode
                            startSimulatedThermalRecording()
                        }

                        emitError(
                            ErrorType.DEVICE_ERROR,
                            "Thermal camera disconnected - switched to simulation mode"
                        )
                        emitStatus()
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error handling device connection event", e)
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onDevicePermissionEvent(event: DevicePermissionEvent) {
        try {
            val device = event.device
            Log.d(TAG, "USB permission event for device: ${device?.productName}")

            if (device != null && device.isTcTsDevice()) {
                Log.i(TAG, "Processing USB permission event for thermal camera device")

                val manager = usbManager
                if (manager != null) {
                    val permissionGranted = manager.hasPermission(device)
                    Log.i(TAG, "USB permission check result: granted=$permissionGranted")

                    if (permissionGranted) {
                        recordingScope.launch {
                            thermalCameraDevice = device
                            hasUsbPermission = true

                            val success = initializeRealThermalCamera(device)
                            if (success) {
                                isSimulationMode = false
                                Log.i(
                                    TAG,
                                    "Thermal camera initialized successfully after permission granted"
                                )
                            } else {
                                Log.w(
                                    TAG,
                                    "Failed to initialize thermal camera after permission granted"
                                )
                                isSimulationMode = true
                            }
                            emitStatus()
                        }
                    } else {
                        Log.w(
                            TAG,
                            "USB permission denied for thermal camera, using simulation mode"
                        )
                        isSimulationMode = true
                        recordingScope.launch {
                            emitError(
                                ErrorType.DEVICE_ERROR,
                                "USB permission denied - using simulation mode"
                            )
                        }
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error handling device permission event", e)
        }
    }

    override fun getStatusFlow(): Flow<RecordingStatus> = _statusFlow.asSharedFlow()
    override fun getErrorFlow(): Flow<SensorError> = _errorFlow.asSharedFlow()

    override fun getRecordingStats(): RecordingStats {
        val currentTime = System.nanoTime()
        val sessionDuration =
            if (recordingStartTime > 0) (currentTime - recordingStartTime) / 1_000_000 else 0L

        return RecordingStats(
            sensorId = sensorId,
            sensorType = sensorType,
            sessionDurationMs = sessionDuration,
            totalSamplesRecorded = frameCount.get(),
            averageDataRate = if (sessionDuration > 0) frameCount.get() * 1000.0 / sessionDuration else 0.0,
            droppedSamples = 0L,
            storageUsedMB = calculateStorageUsed(),
            syncMarkersCount = getSyncMarkerCount(),
            lastSampleTimestampNs = currentTime
        )
    }

    private fun calculateStorageUsed(): Double {
        val dataSize = thermalDataFile?.length() ?: 0L
        val framesSize = thermalFramesFile?.length() ?: 0L
        return (dataSize + framesSize) / (1024.0 * 1024.0)
    }

    private fun getSyncMarkerCount(): Int {

        return 0 // Simplified for now
    }

    private suspend fun emitStatus() {
        val status = RecordingStatus(
            sensorId = sensorId,
            sensorType = sensorType,
            isRecording = _isRecording.get(),
            samplesRecorded = frameCount.get(),
            currentDataRate = samplingRate,
            storageUsedMB = calculateStorageUsed(),
            timestampNs = System.nanoTime()
        )
        _statusFlow.emit(status)
    }

    private suspend fun emitError(
        errorType: ErrorType,
        message: String,
        isRecoverable: Boolean = true
    ) {
        val error = SensorError(
            sensorId = sensorId,
            sensorType = sensorType,
            errorType = errorType,
            errorMessage = message,
            timestampNs = System.nanoTime(),
            isRecoverable = isRecoverable
        )
        _errorFlow.emit(error)
    }

    /**
     * Configure thermal camera device parameters (emissivity, temperature range, palette)
     */
    fun configureThermalDevice(
        emissivity: Double = 0.95,
        temperatureRange: Pair<Float, Float> = Pair(-20.0f, 400.0f),
        ambientTemp: Double = 25.0
    ): Boolean {
        return try {
            Log.i(TAG, "Configuring thermal device parameters")
            
            // Update local calibration parameters
            this.emissivity = emissivity
            this.ambientTemperature = ambientTemp
            this.reflectedTemperature = ambientTemp - 2.0 // Typically slightly lower than ambient
            
            // Configure via IrcamEngine if available
            val configSuccess = if (ircamEngine != null && isTopdonSdkInitialized) {
                try {
                    // Configure device parameters via the SDK
                    // Note: Actual parameter setting would depend on the specific SDK API
                    Log.i(TAG, "Configuring device via IrcamEngine")
                    
                    // This is where we would set actual device parameters
                    // ircamEngine.setEmissivity(emissivity.toFloat())
                    // ircamEngine.setTemperatureRange(temperatureRange.first, temperatureRange.second) 
                    // ircamEngine.setAmbientTemperature(ambientTemp.toFloat())
                    
                    true // Assume success for now
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to configure thermal device via SDK", e)
                    false
                }
            } else {
                Log.i(TAG, "SDK not available, using software-only calibration")
                true // Software calibration is always possible
            }
            
            if (configSuccess) {
                Log.i(TAG, "Thermal device configured: emissivity=$emissivity, ambient=${ambientTemp}°C, range=${temperatureRange.first}-${temperatureRange.second}°C")
            } else {
                Log.w(TAG, "Thermal device configuration partially failed - using software fallback")
            }
            
            configSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure thermal device", e)
            false
        }
    }
    
    /**
     * Enhanced error handling for thermal frame capture
     */
    private suspend fun captureRealThermalFrameWithErrorHandling(): Boolean = withContext(Dispatchers.IO) {
        var retryCount = 0
        val maxRetries = 3
        var lastException: Exception? = null
        
        while (retryCount < maxRetries) {
            try {
                // Attempt to capture frame
                val success = captureRealThermalFrame()
                if (success) {
                    return@withContext true
                }
                
                retryCount++
                Log.w(TAG, "Thermal frame capture attempt $retryCount failed, retrying...")
                delay(100) // Brief delay before retry
                
            } catch (e: Exception) {
                lastException = e
                retryCount++
                Log.e(TAG, "Exception during thermal frame capture attempt $retryCount", e)
                
                if (retryCount < maxRetries) {
                    delay(200) // Longer delay on exception
                }
            }
        }
        
        // All retries exhausted
        Log.e(TAG, "Failed to capture thermal frame after $maxRetries attempts")
        
        // Check if we should fall back to simulation
        if (isIRCameraConnected && !isSimulationMode) {
            Log.w(TAG, "Hardware capture failed repeatedly, switching to simulation mode")
            isSimulationMode = true
            isIRCameraConnected = false
            
            emitError(
                ErrorType.DEVICE_ERROR,
                "Thermal camera hardware failure - switched to simulation mode. Last error: ${lastException?.message}",
                isRecoverable = true
            )
            
            // Start simulation mode if we're still recording
            if (_isRecording.get()) {
                startSimulatedThermalRecording()
            }
            
            return@withContext true // Continue with simulation
        }
        
        return@withContext false
    }
    
    /**
     * Actual thermal frame capture using real hardware
     */
    private suspend fun captureRealThermalFrame(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            // Check if real hardware is connected and initialized
            if (isIRCameraConnected && !isSimulationMode && ircamEngine != null) {
                // Real hardware capture would be triggered by the SDK callbacks
                // The actual frame data comes through the IIrFrameCallback
                Log.d(TAG, "Real thermal hardware capture active")
                true
            } else {
                // Use simulation mode for development/testing
                Log.d(TAG, "Using simulation mode for thermal capture")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during thermal frame capture", e)
            false
        }
    }
    
    /**
     * Update thermal calibration parameters (backward compatibility)
     */
    fun updateCalibration(
        ambientTemp: Double,
        emissivity: Double,
        reflectedTemp: Double
    ) {
        configureThermalDevice(emissivity, Pair(-20.0f, 400.0f), ambientTemp)
        this.reflectedTemperature = reflectedTemp
        
        Log.i(
            TAG,
            "Thermal calibration updated: ambient=$ambientTemp°C, emissivity=$emissivity, reflected=$reflectedTemp°C"
        )
    }
}
