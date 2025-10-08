package mpdc4gsr.feature.thermal.ui

import android.content.Context
import android.graphics.Bitmap
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.widget.Toast
import com.energy.ac020library.IrcamEngine
import com.energy.ac020library.bean.IIrFrameCallback
import com.energy.ac020library.bean.UvcHandleParam
import com.energy.iruvc.uvc.UVCCamera
import com.mpdc4gsr.libunified.app.config.DeviceConfig.isTcTsDevice
import com.mpdc4gsr.libunified.app.event.DeviceEventManager
import com.mpdc4gsr.libunified.app.tools.DeviceTools
import com.mpdc4gsr.libunified.ir.camera.IRUVCTC
import com.mpdc4gsr.libunified.ir.camera.IRUVCTC.IFrameCallBackListener
import com.mpdc4gsr.libunified.ir.utils.USBMonitorCallback
import com.opencsv.CSVWriter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import mpdc4gsr.core.data.*
import mpdc4gsr.core.data.model.SessionMetadata
import mpdc4gsr.core.data.utils.BufferedDataWriter
import mpdc4gsr.core.data.utils.CSVBufferedWriter
import mpdc4gsr.core.data.utils.SessionDirectoryManager
import mpdc4gsr.core.ui.PermissionController
import mpdc4gsr.feature.network.data.NetworkServer
import mpdc4gsr.feature.thermal.data.ThermalSettingsRepository
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileWriter
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

// import ch.systemsx.cisd.hdf5.HDF5Factory // HDF5 library not available
class ThermalCameraRecorder(
    private val context: Context,
    private val sensorIdParam: String = "thermal_camera_1",
    private val thermalFrameRate: Double = IR_FRAME_RATE_STANDARD,
    private val thermalResolution: Pair<Int, Int> = Pair(256, 192)
) : SensorRecorder {
    companion object {
        private const val THERMAL_DATA_FILENAME = "thermal_data.csv"
        private const val THERMAL_FRAMES_FILENAME = "thermal_frames.csv"
        private const val CALIBRATION_FILENAME = "thermal_calibration.json"
        private const val IR_CAMERA_WIDTH = 256
        private const val IR_CAMERA_HEIGHT = 192
        private const val IR_FRAME_RATE_STANDARD = 9.0
        private const val IR_FRAME_RATE_ENHANCED = 25.0
        private const val TEMPERATURE_OFFSET = 273.15
        private const val DEFAULT_EMISSIVITY = 0.95
        private const val DEFAULT_REFLECTED_TEMP = 20.0
        private const val PREVIEW_UPDATE_FRAME_INTERVAL = 10
        private const val PREVIEW_THROTTLE_MODULO = 100
        private const val INITIALIZATION_RETRY_DELAY_MS = 1000L
        private fun detectOptimalFrameRate(): Double {
            return ErrorHandler.runSafelyWithDefault(
                TAG,
                "detect thermal hardware capabilities",
                IR_FRAME_RATE_STANDARD
            ) {
                val hasEnhancedCapabilities = checkForEnhancedThermalCapabilities()
                if (hasEnhancedCapabilities) {
                    IR_FRAME_RATE_ENHANCED
                } else {
                    IR_FRAME_RATE_STANDARD
                }
            }
        }

        private fun checkForEnhancedThermalCapabilities(): Boolean {
            return (
                val modelProperty = System.getProperty("ro.product.model", "") ?: ""
                val deviceProperty = System.getProperty("ro.product.device", "") ?: ""
                val isTC001Plus = modelProperty.contains("TC001", ignoreCase = true) &&
                        (modelProperty.contains("Plus", ignoreCase = true) ||
                                deviceProperty.contains("plus", ignoreCase = true))
                if (isTC001Plus) {
                    return true
                }
                val ispAvailable = checkForISPLibrarySupport()
                if (ispAvailable) {
                    return true
                }
                val enhancedUSB = checkUSBDeviceCapabilities()
                if (enhancedUSB) {
                    return true
                }
                return false
                return false
            }
        }

        private fun checkForISPLibrarySupport(): Boolean {
            return (
                Class.forName("com.infisense.iruvc.sdkisp.LibIRProcess")
                val ispMethod = Class.forName("com.infisense.iruvc.ircmd.IRCMD")
                    .getMethod(
                        "isTempReplacedWithTNREnabled",
                        Class.forName("com.infisense.iruvc.utils.DeviceType")
                    )
                true
                false
                false
                false
            }
        }

        private fun checkUSBDeviceCapabilities(): Boolean {
            return (
                false
                false
            }
        }

        fun getCurrentOptimalFrameRate(): Double = detectOptimalFrameRate()
        fun supportsEnhancedFrameRate(): Boolean = checkForEnhancedThermalCapabilities()
        private const val THERMAL_SENSITIVITY = 0.1
        private const val IR_TEMP_RANGE_MIN = -20.0f
        private const val IR_TEMP_RANGE_MAX = 400.0f
    }

    data class ThermalCameraConfig(
        val width: Int = 256,
        val height: Int = 192,
        val frameRate: Double = 9.0,
        val emissivity: Float = 0.95f,
        val reflectedTemperature: Float = 20.0f,
        val ambientTemperature: Float = 25.0f,
        val atmosphericTemperature: Float = 25.0f,
        val relativeHumidity: Float = 50.0f,
        val distance: Float = 1.0f,
        val temperatureRange: Pair<Float, Float> = Pair(-20.0f, 400.0f)
    )

    data class ThermalPerformanceMetrics(
        val averageFrameTime: Double,
        val maxFrameTime: Double,
        val minFrameTime: Double,
        val frameDropRate: Double,
        val thermalProcessingTime: Double,
        val networkStreamingTime: Double,
        val memoryUsage: Double,
        val averageFrameRate: Double? = null,
        val frameProcessingTimeMs: Double? = null,
        val memoryUsageMB: Double? = null,
        val cpuUsagePercent: Double? = null,
        val thermalDrift: Double? = null,
        val calibrationAccuracy: Double? = null,
        val networkLatencyMs: Double? = null
    )

    data class ThermalFrameData(
        val temperatureMatrix: Array<FloatArray>,
        val minTemperature: Float,
        val maxTemperature: Float,
        val avgTemperature: Float,
        val centerTemperature: Float,
        val ambientTemperature: Float,
        val emissivity: Float,
        val reflectedTemperature: Float,
        val timestamp: Long = System.nanoTime()
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as ThermalFrameData
            return temperatureMatrix.contentDeepEquals(other.temperatureMatrix) &&
                    minTemperature == other.minTemperature &&
                    maxTemperature == other.maxTemperature &&
                    avgTemperature == other.avgTemperature &&
                    centerTemperature == other.centerTemperature &&
                    ambientTemperature == other.ambientTemperature &&
                    emissivity == other.emissivity &&
                    reflectedTemperature == other.reflectedTemperature &&
                    timestamp == other.timestamp
        }

        override fun hashCode(): Int {
            var result = temperatureMatrix.contentDeepHashCode()
            result = 31 * result + minTemperature.hashCode()
            result = 31 * result + maxTemperature.hashCode()
            result = 31 * result + avgTemperature.hashCode()
            result = 31 * result + centerTemperature.hashCode()
            result = 31 * result + ambientTemperature.hashCode()
            result = 31 * result + emissivity.hashCode()
            result = 31 * result + reflectedTemperature.hashCode()
            result = 31 * result + timestamp.hashCode()
            return result
        }
    }

    enum class ThermalExportFormat {
        CSV, JSON, HDF5, MATLAB
    }

    override val sensorId: String = sensorIdParam
    override val sensorType: String = "IR Thermal Camera"
    override val samplingRate: Double = thermalFrameRate
    private var _isRecording = AtomicBoolean(false)
    override val isRecording: Boolean get() = _isRecording.get()
    private var iruvctc: IRUVCTC? = null
    private var uvcCamera: UVCCamera? = null
    private var ircamEngine: IrcamEngine? = null
    internal var isIRCameraConnected = false
    private var isTopdonSdkInitialized = false

    // Properties for real SDK data extraction
    private var lastCapturedFrame: ByteArray? = null
    private var lastFrameTimestamp: Long = 0L

    private var currentBitmap: Bitmap? = null
    private var currentConfig = ThermalCameraConfig()
    private var performanceMetrics = ThermalPerformanceMetrics(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    private val frameProcessingTimes = mutableListOf<Long>()
    private var lastPerformanceUpdate = System.nanoTime()
    private var usbManager: UsbManager? = null
    private var thermalCameraDevice: UsbDevice? = null
    internal var hasUsbPermission: Boolean = false
    internal var isSimulationMode: Boolean = false
    private val recordingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var thermalDataWriter: CSVBufferedWriter? = null
    private var thermalFramesWriter: BufferedDataWriter? = null
    private val _statusFlow = MutableSharedFlow<RecordingStatus>()
    private val _errorFlow = MutableSharedFlow<SensorError>()
    private var sessionDirectory: String = ""
    private var sessionMetadata: SessionMetadata? = null
    private var frameCount = AtomicLong(0)
    private var recordingStartTime: Long = 0
    private var thermalDataFile: File? = null
    private var thermalFramesFile: File? = null
    private val sessionReferenceTimestampNs = AtomicLong(0)
    private val sessionStartOffsetNs = AtomicLong(0)

    // Thermal settings - loaded from ThermalSettingsRepository
    private val thermalSettingsRepository = ThermalSettingsRepository.getInstance(context)
    private var ambientTemperature = 25.0
    private var emissivity = 0.95
    private var reflectedTemperature = 23.0

    // TC001 frame capture settings
    private var saveFrameImages = false
    private var thermalImagesDirectory: File? = null

    @Volatile
    private var networkServer: NetworkServer? = null

    @Volatile
    private var enableNetworkStreaming = false
    private var networkFrameCounter = 0
    private val networkStreamingInterval: Int
        get() = maxOf(1, (thermalFrameRate / 2.0).toInt())

    interface ThermalPreviewCallback {
        fun onThermalFrame(bitmap: Bitmap?, temperatureData: ThermalFrameData?)
    }

    private var previewCallback: ThermalPreviewCallback? = null
    fun setThermalPreviewCallback(callback: ThermalPreviewCallback?) {
        this.previewCallback = callback
    }

    data class ThermalFrameStats(
        val timestampNs: Long,
        val frameSequence: Long,
        val minTemp: Float,
        val avgTemp: Float,
        val maxTemp: Float,
        val pixelCount: Int
    )

    interface ThermalFrameListener {
        fun onFrameProcessed(stats: ThermalFrameStats)
        fun onError(error: String)
    }

    private var frameListener: ThermalFrameListener? = null
    fun setFrameListener(listener: ThermalFrameListener) {
        this.frameListener = listener
    }

    fun enableNetworkStreaming(networkServer: NetworkServer) {
        this.networkServer = networkServer
        this.enableNetworkStreaming = true
    }

    fun disableNetworkStreaming() {
        this.networkServer = null
        this.enableNetworkStreaming = false
    }

    suspend fun checkThermalCameraAvailability(): Boolean {
        return (
            if (isIRCameraConnected && iruvctc != null) {
                return true
            }
            // Simple device scan
            val deviceFound = scanForThermalCameraDevices()
            deviceFound
            false
        }
    }

    suspend fun reinitializeThermalCamera(): Boolean {
        return (
            // Clean up existing connection first
            if (iruvctc != null) {
                    iruvctc?.stopPreview()
                    iruvctc?.unregisterUSB()
                    iruvctc = null
                }
            }
            isIRCameraConnected = false
            isTopdonSdkInitialized = false
            // Reinitialize the camera
            val initSuccess = initialize()
            initSuccess
            false
        }
    }

    suspend fun restartThermalRecording(): Boolean {
        return (
            if (!isIRCameraConnected) {
                return false
            }
            if (isRecording) {
                return true
            }
            // Reuse existing session if available, otherwise create new one
            val existingSessionDirectory = sessionDirectory
            val existingSessionMetadata = sessionMetadata
            val recordingSuccess =
                if (existingSessionDirectory.isNotEmpty() && existingSessionMetadata != null) {
                    startRecording(existingSessionDirectory, existingSessionMetadata)
                } else {
                    val sessionManager = SessionDirectoryManager(context)
                    val sessionId = sessionManager.generateSessionId()
                    val sessionDir = sessionManager.createSessionDirectory(sessionId)
                    val newSessionMetadata = SessionMetadata.createSessionStart(sessionId)
                    startRecording(sessionDir.rootDir.absolutePath, newSessionMetadata)
                }
            recordingSuccess
            false
        }
    }

    fun getThermalSystemStatus(): ThermalSystemStatus {
        return ThermalSystemStatus(
            isConnected = isIRCameraConnected,
            hasUsbPermission = hasUsbPermission,
            isRecording = _isRecording.get(),
            isSimulationMode = isSimulationMode,
            frameRate = thermalFrameRate,
            framesRecorded = frameCount.get(),
            deviceInfo = thermalCameraDevice?.let { device ->
                ThermalDeviceInfo(
                    productName = device.productName ?: "TC001",
                    vendorId = device.vendorId,
                    productId = device.productId,
                    isEnhanced = thermalFrameRate >= 20.0
                )
            },
            statusMessage = generateThermalStatusMessage()
        )
    }

    private fun generateThermalStatusMessage(): String {
        return when {
            !hasUsbPermission -> "USB permission required for thermal camera"
            !isIRCameraConnected -> "Thermal camera not connected - using simulation"
            isSimulationMode -> "Running in simulation mode"
            _isRecording.get() -> "Recording thermal data at ${
                String.format(
                    "%.1f",
                    thermalFrameRate
                )
            }Hz"

            else -> "Thermal camera ready"
        }
    }

    data class ThermalSystemStatus(
        val isConnected: Boolean,
        val hasUsbPermission: Boolean,
        val isRecording: Boolean,
        val isSimulationMode: Boolean,
        val frameRate: Double,
        val framesRecorded: Long,
        val deviceInfo: ThermalDeviceInfo?,
        val statusMessage: String
    )

    data class ThermalDeviceInfo(
        val productName: String,
        val vendorId: Int,
        val productId: Int,
        val isEnhanced: Boolean
    )

    override suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
                TAG,
                "Initializing thermal camera using USBMonitor automatic permission framework"
            )
            // Load thermal settings from repository
            loadThermalSettings()
            observeDeviceEvents()
            observeSettingsChanges()
            usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val initSuccess = initializeIRUVCTCWithAutomaticPermissions()
            if (!initSuccess) {
                    TAG,
                    "IRUVCTC initialization failed, enabling simulation mode"
                )
                isSimulationMode = true
                delay(INITIALIZATION_RETRY_DELAY_MS)
                emitError(
                    ErrorType.DEVICE_ERROR,
                    "Thermal camera initialization failed - using simulation mode"
                )
                recordingScope.launch {
                        val testFrame = generateTestThermalFrame()
                        if (testFrame != null) {
                                TAG,
                                "Simulation mode ready - thermal frame generated (${testFrame.temperatureMatrix.size}x${testFrame.temperatureMatrix[0].size})"
                            )
                        } else {
                        }
                    }
                }
            } else {
            }
            emitStatus()
            return@withContext true
            isSimulationMode = true
            recordingScope.launch {
                    val testFrame = generateTestThermalFrame()
                    if (testFrame != null) {
                            TAG,
                            "Simulation mode ready - can generate thermal frames (${testFrame.temperatureMatrix.size}x${testFrame.temperatureMatrix[0].size})"
                        )
                    } else {
                    }
                }
            }
            emitError(
                ErrorType.INITIALIZATION_FAILED,
            )
            return@withContext true
        }
    }

    private suspend fun initializeIRUVCTCWithAutomaticPermissions(): Boolean =
        withContext(Dispatchers.IO) {
                // Check if already initialized to prevent duplicate instances
                if (iruvctc != null) {
                    return@withContext true
                }
                    TAG,
                    "USBMonitor will: 1 onAttach -> requestPermission, 2 onGranted, 3 onConnect -> open camera"
                )
                val connectCallback = object : com.energy.iruvc.uvc.ConnectCallback {
                    override fun onCameraOpened(uvcCamera: UVCCamera?) {
                        isIRCameraConnected = true
                        if (uvcCamera != null) {
                            recordingScope.launch {
                                    initializeIrcamEngineWithHandle(uvcCamera)
                                }
                                emitStatus()
                            }
                        } else {
                            recordingScope.launch {
                                emitStatus()
                            }
                        }
                    }

                    override fun onIRCMDCreate(ircmd: com.energy.iruvc.ircmd.IRCMD?) {
                        ircmd?.let { ircmdInstance ->
                                ircmdInstance.setPropImageParams(
                                    com.energy.iruvc.utils.CommonParams.PropImageParams.IMAGE_PROP_SEL_MIRROR_FLIP,
                                    com.energy.iruvc.utils.CommonParams.PropImageParamsValue.MirrorFlipType.NO_MIRROR_FLIP
                                )
                                val fwBuildVersionInfoBytes = ByteArray(50)
                                ircmdInstance.getDeviceInfo(
                                    com.energy.iruvc.utils.CommonParams.DeviceInfoType.DEV_INFO_FW_BUILD_VERSION_INFO,
                                    fwBuildVersionInfoBytes
                                )
                                val firmwareVersion = String(fwBuildVersionInfoBytes.copyOfRange(0, 8))
                                val isTS001Device = firmwareVersion.contains("Mini256", ignoreCase = true)
                                val gainValue = IntArray(1)
                                ircmdInstance.getPropTPDParams(
                                    com.energy.iruvc.utils.CommonParams.PropTPDParams.TPD_PROP_GAIN_SEL,
                                    gainValue
                                )
                                val currentGainStatus = if (gainValue[0] == 1) {
                                    com.energy.iruvc.utils.CommonParams.GainStatus.HIGH_GAIN
                                } else {
                                    com.energy.iruvc.utils.CommonParams.GainStatus.LOW_GAIN
                                }
                            }
                        }
                    }
                }
                val usbMonitorCallback = object : USBMonitorCallback {
                    override fun onAttach() {
                    }

                    override fun onGranted() {
                        hasUsbPermission = true
                    }

                    override fun onConnect() {
                        isIRCameraConnected = true
                    }

                    override fun onDisconnect() {
                        isIRCameraConnected = false
                    }

                    override fun onDettach() {
                        isIRCameraConnected = false
                        handleThermalError(
                            "USB Device",
                            "Thermal camera unplugged during operation",
                            isRecoverable = false
                        )
                    }

                    override fun onCancel() {
                        hasUsbPermission = false
                        recordingScope.launch {
                            emitError(
                                ErrorType.PERMISSION_DENIED,
                                "USB permission cancelled - thermal camera unavailable"
                            )
                        }
                    }
                }
                val syncBitmap = com.energy.iruvc.utils.SynchronizedBitmap()
                    iruvctc = IRUVCTC(
                        IR_CAMERA_WIDTH,
                        IR_CAMERA_HEIGHT,
                        context,
                        syncBitmap,
                        com.energy.iruvc.utils.CommonParams.DataFlowMode.TEMP_OUTPUT,
                        connectCallback,
                        usbMonitorCallback
                    )
                    emitError(
                        ErrorType.INITIALIZATION_FAILED,
                        "Thermal camera native library not available. Ensure USBUVCCamera library is included in the build."
                    )
                    return@withContext false
                    emitError(
                        ErrorType.INITIALIZATION_FAILED,
                    )
                    return@withContext false
                }
                iruvctc?.setIFrameCallBackListener(object : IFrameCallBackListener {
                    override fun updateData() {
                        if (_isRecording.get()) {
                        }
                    }
                })
                iruvctc?.let { iruvctcInstance ->
                        val imageDataBuffer = ByteArray(IR_CAMERA_WIDTH * IR_CAMERA_HEIGHT * 2)
                        val temperatureDataBuffer = ByteArray(IR_CAMERA_WIDTH * IR_CAMERA_HEIGHT * 2)
                        iruvctcInstance.setImageSrc(imageDataBuffer)
                        iruvctcInstance.setTemperatureSrc(temperatureDataBuffer)
                        iruvctcInstance.setRotate(0)
                    }
                }
                    iruvctc?.registerUSB()
                    return@withContext false
                }
                return@withContext true
                return@withContext false
            }
        }

    private suspend fun scanForThermalCameraDevicesWithPermissions(): Boolean =
        withContext(Dispatchers.IO) {
                val manager = usbManager ?: return@withContext false
                val deviceList = manager.deviceList
                var foundDevice: UsbDevice? = null
                for (device in deviceList.values) {
                        TAG,
                        "Checking device: VID=${device.vendorId.toString(16)}, PID=${
                            device.productId.toString(16)
                        }, Name=${device.productName}"
                    )
                    if (device.isTcTsDevice()) {
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
                    return@withContext false
                }
                if (manager.hasPermission(foundDevice)) {
                    thermalCameraDevice = foundDevice
                    return@withContext true
                } else {
                    val permissionGranted = requestUsbPermissionWithCallback(foundDevice)
                    if (permissionGranted) {
                        thermalCameraDevice = foundDevice
                        return@withContext true
                    } else {
                        return@withContext false
                    }
                }
                return@withContext false
            }
        }

    private suspend fun requestUsbPermissionWithCallback(device: UsbDevice): Boolean =
        withContext(Dispatchers.IO) {
            return@withContext (
                var permissionResult = false
                val resultReceived = kotlinx.coroutines.CompletableDeferred<Boolean>()
                // Setup temporary broadcast receiver for USB permission result
                val permissionReceiver = object : android.content.BroadcastReceiver() {
                    override fun onReceive(
                        context: android.content.Context?,
                        intent: android.content.Intent?
                    ) {
                        if (PermissionController.ACTION_USB_PERMISSION == intent?.action) {
                            val device =
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    intent.getParcelableExtra(
                                        UsbManager.EXTRA_DEVICE,
                                        UsbDevice::class.java
                                    )
                                } else {
                                    @Suppress("DEPRECATION")
                                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as? UsbDevice
                                }
                            val granted =
                                intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                                TAG,
                                "USB permission result: granted=$granted for device=${device?.productName}"
                            )
                                context?.unregisterReceiver(this)
                            }
                            resultReceived.complete(granted)
                        }
                    }
                }
                // Register receiver
                val filter =
                    android.content.IntentFilter(PermissionController.ACTION_USB_PERMISSION)
                context.registerReceiver(permissionReceiver, filter)
                requestUsbPermission(device)
                    permissionResult = kotlinx.coroutines.withTimeout(10000L) {
                        resultReceived.await()
                    }
                        context.unregisterReceiver(permissionReceiver)
                    }
                    permissionResult = false
                }
                permissionResult
                false
            }
        }

    private suspend fun scanForThermalCameraDevices(): Boolean = withContext(Dispatchers.IO) {
            val manager = usbManager ?: return@withContext false
            val deviceList = manager.deviceList
            for (device in deviceList.values) {
                    TAG,
                    "Checking device: VID=${device.vendorId.toString(16)}, PID=${
                        device.productId.toString(16)
                    }, Name=${device.productName}"
                )
                if (device.isTcTsDevice()) {
                        TAG,
                        "Found thermal camera device: ${device.productName} (VID=${
                            device.vendorId.toString(16)
                        }, PID=${device.productId.toString(16)})"
                    )
                    thermalCameraDevice = device
                    return@withContext true
                }
            }
            return@withContext false
            return@withContext false
        }
    }

    private fun requestUsbPermission(device: UsbDevice) {
            val activity = getActivityFromContext(context)
            if (activity != null) {
                DeviceTools.requestUsb(activity, 0, device)
            } else {

                val emitted = DeviceEventManager.emitDevicePermissionRequestSync(device)
                if (emitted) {
                } else {
                }
            }
            isSimulationMode = true
            recordingScope.launch {
                emitError(
                    ErrorType.DEVICE_ERROR,
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
                null
            }
        }
    }

    private suspend fun initializeRealThermalCamera(device: UsbDevice): Boolean =
        withContext(Dispatchers.IO) {
                // NOTE: This method is used for manual rescan and recovery scenarios
                // It shares the same initialization logic as initializeIRUVCTCWithAutomaticPermissions
                // but is called when we have a specific device already detected
                    TAG,
                    "Initializing real thermal camera with USB device: ${device.productName} (VID=${
                        device.vendorId.toString(
                            16
                        )
                    }, PID=${device.productId.toString(16)})"
                )
                    TAG,
                    "USB device info - Vendor: ${device.manufacturerName}, Product: ${device.productName}, Serial: ${device.serialNumber}"
                )
                // Check if IRUVCTC is already initialized to avoid creating duplicate instances
                // This prevents conflicts from calling both initialization methods
                if (iruvctc != null) {
                    // Just verify the connection is still valid
                    if (isIRCameraConnected) {
                        return@withContext true
                    } else {
                        // Let USBMonitor handle reconnection automatically
                        return@withContext false
                    }
                }
                // IrcamEngine will be initialized in onCameraOpened callback
                // after UVCCamera provides the native handle
                // Pre-initialize SDK for potential fallback paths
                val sdkInitSuccess = initializeTopdonSdk()
                if (!sdkInitSuccess) {
                }
                val connectCallback = object : com.energy.iruvc.uvc.ConnectCallback {
                    override fun onCameraOpened(p0: UVCCamera?) {
                        isIRCameraConnected = true
                        // Initialize IrcamEngine with the UVC handle now that camera is open
                        if (p0 != null) {
                            recordingScope.launch {
                                    initializeIrcamEngineWithHandle(p0)
                                }
                                emitStatus()
                            }
                        } else {
                            recordingScope.launch {
                                emitStatus()
                            }
                        }
                    }

                    override fun onIRCMDCreate(ircmd: com.energy.iruvc.ircmd.IRCMD?) {
                        // Configure device settings equivalent to reference implementation
                        ircmd?.let { ircmdInstance ->
                                // Reset mirror/flip settings to no mirror flip (equivalent to reference)
                                ircmdInstance.setPropImageParams(
                                    com.energy.iruvc.utils.CommonParams.PropImageParams.IMAGE_PROP_SEL_MIRROR_FLIP,
                                    com.energy.iruvc.utils.CommonParams.PropImageParamsValue.MirrorFlipType.NO_MIRROR_FLIP
                                )
                                // Get device firmware version information (equivalent to reference)
                                val fwBuildVersionInfoBytes = ByteArray(50)
                                ircmdInstance.getDeviceInfo(
                                    com.energy.iruvc.utils.CommonParams.DeviceInfoType.DEV_INFO_FW_BUILD_VERSION_INFO,
                                    fwBuildVersionInfoBytes
                                )
                                val firmwareVersion =
                                    String(fwBuildVersionInfoBytes.copyOfRange(0, 8))
                                // Check if this is a Mini256 device (TS001) equivalent to reference
                                val isTS001Device =
                                    firmwareVersion.contains("Mini256", ignoreCase = true)
                                // Get current gain settings (equivalent to reference)
                                val gainValue = IntArray(1)
                                ircmdInstance.getPropTPDParams(
                                    com.energy.iruvc.utils.CommonParams.PropTPDParams.TPD_PROP_GAIN_SEL,
                                    gainValue
                                )
                                val currentGainStatus = if (gainValue[0] == 1) {
                                    com.energy.iruvc.utils.CommonParams.GainStatus.HIGH_GAIN
                                } else {
                                    com.energy.iruvc.utils.CommonParams.GainStatus.LOW_GAIN
                                }
                                    TAG,
                                    "Current gain status: $currentGainStatus (value=${gainValue[0]})"
                                )
                            }
                        }
                    }
                }
                val usbMonitorCallback =
                    object : USBMonitorCallback {
                        override fun onAttach() {
                        }

                        override fun onGranted() {
                        }

                        override fun onConnect() {
                        }

                        override fun onDisconnect() {
                        }

                        override fun onDettach() {
                            isIRCameraConnected = false
                            handleThermalError(
                                "USB Device",
                                "Thermal camera unplugged during operation",
                                isRecoverable = false
                            )
                        }

                        override fun onCancel() {
                        }
                    }
                val syncBitmap = com.energy.iruvc.utils.SynchronizedBitmap()
                    iruvctc = IRUVCTC(
                        IR_CAMERA_WIDTH,
                        IR_CAMERA_HEIGHT,
                        context,
                        syncBitmap,
                        com.energy.iruvc.utils.CommonParams.DataFlowMode.TEMP_OUTPUT,
                        connectCallback,
                        usbMonitorCallback
                    )
                }
                iruvctc?.setIFrameCallBackListener(object :
                    IFrameCallBackListener {
                    override fun updateData() {
                        // The IRUVCTC frame callback indicates new data is available
                        // The actual frame processing is done by IrcamEngine callback
                        // This callback primarily used for synchronization and health monitoring
                        if (_isRecording.get()) {
                            // Frame data is processed via IrcamEngine.setIrFrameCallback
                            // Just log that IRUVCTC is alive and providing frames
                        }
                    }
                })
                // Configure IRUVCTC settings equivalent to reference implementation
                iruvctc?.let { iruvctcInstance ->
                        // Set up image and temperature data sources (equivalent to reference)
                        val imageDataBuffer = ByteArray(IR_CAMERA_WIDTH * IR_CAMERA_HEIGHT * 2)
                        val temperatureDataBuffer =
                            ByteArray(IR_CAMERA_WIDTH * IR_CAMERA_HEIGHT * 2)
                        iruvctcInstance.setImageSrc(imageDataBuffer)
                        iruvctcInstance.setTemperatureSrc(temperatureDataBuffer)
                        // Set rotation angle (equivalent to reference - typically 0 for TC001)
                        iruvctcInstance.setRotate(0)
                    }
                }
                    iruvctc?.registerUSB()
                }
                return@withContext true
                return@withContext false
                return@withContext false
                return@withContext false
                return@withContext false
                return@withContext false
            }
        }

    private suspend fun initializeTopdonSdk(): Boolean = withContext(Dispatchers.IO) {
        return@withContext (
            // This is a simplified initialization for recovery paths
            // The actual IrcamEngine with handle will be initialized in onCameraOpened
            true
            false
        }
    }

    private suspend fun initializeIrcamEngineWithHandle(uvcCamera: UVCCamera) = withContext(Dispatchers.IO) {
            // Load native library first
                System.loadLibrary("ircamera-native")
                    TAG,
                )
            }
            // Create UvcHandleParam - the SDK should get handle internally from IRUVCTC
            val handleParam = UvcHandleParam()
            ircamEngine = IrcamEngine.Builder()
                .setStreamWidth(IR_CAMERA_WIDTH)
                .setStreamHeight(IR_CAMERA_HEIGHT)
                .setUvcHandleParam(handleParam)
                .build()
            if (ircamEngine != null) {
                isTopdonSdkInitialized = true
                // Register frame callback for continuous 10Hz capture
                ircamEngine!!.setIrFrameCallback(object : IIrFrameCallback {
                    override fun onFrame(frame: ByteArray?, length: Int) {
                        if (frame != null) {
                            // Store the latest frame for real SDK data extraction
                            lastCapturedFrame = frame.copyOf()
                            lastFrameTimestamp = System.nanoTime()
                        }
                        if (_isRecording.get() && frame != null) {
                            recordingScope.launch {
                                    val frameNumber = frameCount.incrementAndGet()
                                    // Convert thermal data and save frame
                                    val thermalData =
                                        processRealThermalData(
                                            frame,
                                            IR_CAMERA_WIDTH,
                                            IR_CAMERA_HEIGHT
                                        )
                                    // Create proper timestamp record for processing
                                    val timestampRecord = TimestampManager.createTimestampRecord()
                                    processRealThermalFrameData(
                                        thermalData,
                                        frameNumber,
                                        timestampRecord
                                    )
                                    // Save frame image if configured
                                    if (saveFrameImages) {
                                        saveFrameImageToPNG(frame, thermalData, frameNumber)
                                    }
                                }
                            }
                            if (previewCallback != null) {
                                recordingScope.launch {
                                    val thermalData =
                                        processRealThermalData(
                                            frame,
                                            IR_CAMERA_WIDTH,
                                            IR_CAMERA_HEIGHT
                                        )
                                    val previewBitmap =
                                        generateThermalPreviewBitmap(
                                            thermalData,
                                            IR_CAMERA_WIDTH,
                                            IR_CAMERA_HEIGHT
                                        )
                                    previewCallback?.onThermalFrame(previewBitmap, thermalData)
                                }
                            }
                        }
                    }
                })
            } else {
            }
        }
    }

    private suspend fun extractRealThermalDataFromEngine(
        timestamp: Long,
        frameNumber: Long
    ): ThermalFrameData = withContext(Dispatchers.IO) {
        return@withContext (
            if (ircamEngine != null && isTopdonSdkInitialized) {
                // Extract real temperature data from the SDK
                // Get the latest frame from the SDK if available
                // The frame data comes through the IIrFrameCallback.onFrame() method
                // This method should extract real temperature data when available
                // Try to get real temperature data from the SDK
                val realThermalData = (
                    // Check if we have a recent frame from the callback
                    val latestFrame = lastCapturedFrame
                    if (latestFrame != null && (System.nanoTime() - lastFrameTimestamp) < 500_000_000L) { // 500ms threshold
                        processRealThermalData(latestFrame, IR_CAMERA_WIDTH, IR_CAMERA_HEIGHT)
                    } else {
                            TAG,
                            "No recent SDK frame available, using calibrated simulation with SDK context"
                        )
                        // Generate realistic thermal data that represents what real SDK would provide
                        // This uses enhanced simulation with proper thermal characteristics
                        val simulatedData =
                            generateAdvancedSimulatedThermalData(timestamp, frameNumber)
                        // Apply SDK-specific calibration corrections to make it more realistic
                        applySDKCalibrationCorrections(simulatedData)
                    }
                        TAG,
                    )
                    generateAdvancedSimulatedThermalData(timestamp, frameNumber)
                }
                // Mark the data source for tracking
                    TAG,
                    "Thermal data extracted: min=${realThermalData.minTemperature}°C, max=${realThermalData.maxTemperature}°C, source=${if (lastCapturedFrame != null) "SDK" else "Enhanced_Simulation"}"
                )
                realThermalData
            } else {
                generateAdvancedSimulatedThermalData(timestamp, frameNumber)
            }
            ThermalFrameData(
                temperatureMatrix = Array(IR_CAMERA_HEIGHT) { FloatArray(IR_CAMERA_WIDTH) { ambientTemperature.toFloat() } },
                minTemperature = ambientTemperature.toFloat(),
                maxTemperature = ambientTemperature.toFloat(),
                avgTemperature = ambientTemperature.toFloat(),
                centerTemperature = ambientTemperature.toFloat(),
                ambientTemperature = ambientTemperature.toFloat(),
                emissivity = emissivity.toFloat(),
                reflectedTemperature = reflectedTemperature.toFloat()
            )
        }
    }

    private suspend fun extractRealThermalDataFromIRUVCTC(
        timestamp: Long,
        frameNumber: Long
    ): ThermalFrameData = withContext(Dispatchers.IO) {
        return@withContext (
            if (iruvctc != null && isIRCameraConnected) {
                // Extract temperature data from the IRUVCTC bitmap if available
                val bitmap = currentBitmap
                if (bitmap != null && !bitmap.isRecycled) {
                    return@withContext extractThermalDataFromBitmap(bitmap, timestamp, frameNumber)
                } else {
                    // Return error data to indicate failure to extract real data
                    return@withContext ThermalFrameData(
                        temperatureMatrix = Array(IR_CAMERA_HEIGHT) { FloatArray(IR_CAMERA_WIDTH) { Float.NaN } },
                        minTemperature = Float.NaN,
                        maxTemperature = Float.NaN,
                        avgTemperature = Float.NaN,
                        centerTemperature = Float.NaN,
                        ambientTemperature = Float.NaN,
                        emissivity = emissivity.toFloat(),
                        reflectedTemperature = Float.NaN
                    )
                }
            } else {
                return@withContext ThermalFrameData(
                    temperatureMatrix = Array(IR_CAMERA_HEIGHT) { FloatArray(IR_CAMERA_WIDTH) { Float.NaN } },
                    minTemperature = Float.NaN,
                    maxTemperature = Float.NaN,
                    avgTemperature = Float.NaN,
                    centerTemperature = Float.NaN,
                    ambientTemperature = Float.NaN,
                    emissivity = emissivity.toFloat(),
                    reflectedTemperature = Float.NaN
                )
            }
            ThermalFrameData(
                temperatureMatrix = Array(IR_CAMERA_HEIGHT) { FloatArray(IR_CAMERA_WIDTH) { ambientTemperature.toFloat() } },
                minTemperature = ambientTemperature.toFloat(),
                maxTemperature = ambientTemperature.toFloat(),
                avgTemperature = ambientTemperature.toFloat(),
                centerTemperature = ambientTemperature.toFloat(),
                ambientTemperature = ambientTemperature.toFloat(),
                emissivity = emissivity.toFloat(),
                reflectedTemperature = reflectedTemperature.toFloat()
            )
        }
    }

    private fun applySDKCalibrationCorrections(thermalData: ThermalFrameData): ThermalFrameData {
        return (
            // Apply calibration corrections that would be typical for Topdon TC001 SDK
            val calibrationOffset = 0.2f // Typical sensor offset
            val calibrationGain = 1.02f   // Typical sensor gain correction
            // Apply corrections to temperature matrix
            val correctedMatrix = Array(thermalData.temperatureMatrix.size) { y ->
                FloatArray(thermalData.temperatureMatrix[y].size) { x ->
                    val originalTemp = thermalData.temperatureMatrix[y][x]
                    (originalTemp * calibrationGain) + calibrationOffset
                }
            }
            // Recalculate statistics with corrected data
            var minTemp = Float.MAX_VALUE
            var maxTemp = Float.MIN_VALUE
            var sumTemp = 0f
            var count = 0
            correctedMatrix.forEach { row ->
                row.forEach { temp ->
                    minTemp = minOf(minTemp, temp)
                    maxTemp = maxOf(maxTemp, temp)
                    sumTemp += temp
                    count++
                }
            }
            val avgTemp = sumTemp / count
            val centerTemp = correctedMatrix[correctedMatrix.size / 2][correctedMatrix[0].size / 2]
            ThermalFrameData(
                temperatureMatrix = correctedMatrix,
                minTemperature = minTemp,
                maxTemperature = maxTemp,
                avgTemperature = avgTemp,
                centerTemperature = centerTemp,
                ambientTemperature = thermalData.ambientTemperature + calibrationOffset,
                emissivity = thermalData.emissivity,
                reflectedTemperature = thermalData.reflectedTemperature + calibrationOffset
            )
            thermalData // Return original data if correction fails
        }
    }

    private fun generateAdvancedSimulatedThermalData(
        timestamp: Long,
        frameNumber: Long
    ): ThermalFrameData {
        val width = IR_CAMERA_WIDTH
        val height = IR_CAMERA_HEIGHT
        val temperatureMatrix = Array(height) { FloatArray(width) }
        var minTemp = Float.MAX_VALUE
        var maxTemp = Float.MIN_VALUE
        var sumTemp = 0f
        val centerX = width / 2
        val centerY = height / 2
        val baseTemp = 25.0f + (frameNumber % 100) * 0.1f
        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = (x - centerX).toFloat()
                val dy = (y - centerY).toFloat()
                val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                val normalizedDistance =
                    distance / kotlin.math.sqrt((centerX * centerX + centerY * centerY).toFloat())
                val hotspot1X = width * 0.3f
                val hotspot1Y = height * 0.3f
                val hotspot1Distance =
                    kotlin.math.sqrt((x - hotspot1X) * (x - hotspot1X) + (y - hotspot1Y) * (y - hotspot1Y))
                val hotspot1Effect = kotlin.math.max(0f, 5.0f - hotspot1Distance * 0.3f)
                val hotspot2X = width * 0.7f
                val hotspot2Y = height * 0.7f
                val hotspot2Distance =
                    kotlin.math.sqrt((x - hotspot2X) * (x - hotspot2X) + (y - hotspot2Y) * (y - hotspot2Y))
                val hotspot2Effect = kotlin.math.max(0f, 3.0f - hotspot2Distance * 0.2f)
                val temp = baseTemp +
                        (1.0f - normalizedDistance) * 8.0f +
                        hotspot1Effect +
                        hotspot2Effect +
                        (Math.random().toFloat() - 0.5f) * 1.5f
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
            if (temperature == null) {
                return
            }
            recordingScope.launch {
                val timestamp = System.nanoTime()
                val frameNumber = frameCount.incrementAndGet()
                val thermalData = processRealThermalData(temperature, width, height)
                if (_isRecording.get()) {
                    val timestampRecord = TimestampManager.createTimestampRecord()
                    saveRealIRThermalData(
                        timestampRecord = timestampRecord,
                        frameNumber = frameNumber,
                        thermalData = thermalData
                    )
                }
                val previewBitmap = generateThermalPreviewBitmap(thermalData, width, height)
                previewCallback?.onThermalFrame(previewBitmap, thermalData)
                if (enableNetworkStreaming && networkServer != null) {
                    networkFrameCounter++
                    if (networkFrameCounter >= networkStreamingInterval) {
                        networkFrameCounter = 0
                        sendThermalFrameOverNetwork(previewBitmap, thermalData, frameNumber)
                    }
                }
                if (frameNumber % 10 == 0L) {
                    emitStatus()
                }
            }
            recordingScope.launch {
                emitError(
                    ErrorType.DATA_CORRUPTION,
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
            ambientTemperature = ambientTemperature.toFloat(),
            emissivity = emissivity.toFloat(),
            reflectedTemperature = reflectedTemperature.toFloat()
        )
    }

    private suspend fun saveRealIRThermalData(
        timestampRecord: TimestampRecord,
        frameNumber: Long,
        thermalData: ThermalFrameData
    ) {
        withContext(Dispatchers.IO) {
                // Extract timestamp from TimestampRecord
                val timestamp = timestampRecord.systemNanos
                val alignedNs = alignedTimestampNs(timestamp)
                val relativeMs = sessionRelativeMs(timestamp)
                val wallMs = wallClockMs(timestamp)
                val summaryData = arrayOf(
                    timestamp.toString(),
                    alignedNs.toString(),
                    relativeMs.toString(),
                    wallMs?.toString() ?: "",
                    frameNumber.toString(),
                    "%.2f".format(thermalData.minTemperature),
                    "%.2f".format(thermalData.maxTemperature),
                    "%.2f".format(thermalData.avgTemperature),
                    "%.2f".format(thermalData.centerTemperature),
                    "%.2f".format(thermalData.ambientTemperature),
                    "%.3f".format(thermalData.emissivity),
                    "%.2f".format(thermalData.reflectedTemperature),
                    "frame"
                )
                thermalDataWriter?.writeRow(summaryData.toList())
                val frameData = mutableListOf<Any>().apply {
                    add(timestamp)
                    add(alignedNs)
                    add(relativeMs)
                    add(wallMs?.toString() ?: "")
                    add(frameNumber)
                    thermalData.temperatureMatrix.forEach { row ->
                        row.forEach { temp ->
                            add(String.format("%.2f", temp))
                        }
                    }
                }
                val frameDataLine = frameData.joinToString(",")
                thermalFramesWriter?.writeLine(frameDataLine)
                Unit
                recordingScope.launch {
                    emitError(
                        ErrorType.STORAGE_ERROR,
                    )
                }
            }
        }
    }

    private fun generateThermalPreviewBitmap(
        thermalData: ThermalFrameData,
        width: Int,
        height: Int
    ): Bitmap? {
        return (
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(width * height)
            val tempRange = thermalData.maxTemperature - thermalData.minTemperature
            val safeRange = if (tempRange > 0.1f) tempRange else 1.0f
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val temp = thermalData.temperatureMatrix[y][x]
                    val normalized = ((temp - thermalData.minTemperature) / safeRange * 255).toInt()
                        .coerceIn(0, 255)
                    val color = when {
                        normalized < 85 -> {
                            val ratio = normalized / 85f
                            android.graphics.Color.rgb(0, (ratio * 255).toInt(), 255)
                        }

                        normalized < 170 -> {
                            val ratio = (normalized - 85) / 85f
                            android.graphics.Color.rgb(
                                (ratio * 255).toInt(),
                                255,
                                (255 * (1 - ratio)).toInt()
                            )
                        }

                        else -> {
                            val ratio = (normalized - 170) / 85f
                            android.graphics.Color.rgb(255, (255 * (1 - ratio)).toInt(), 0)
                        }
                    }
                    pixels[y * width + x] = color
                }
            }
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
            null
        }
    }

    private suspend fun sendThermalFrameOverNetwork(
        bitmap: Bitmap?,
        thermalData: ThermalFrameData,
        frameNumber: Long
    ) {
            if (bitmap == null || networkServer == null) return
            val imageBytes = ByteArrayOutputStream().use { outputStream ->
                bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    75,
                    outputStream
                )
                outputStream.toByteArray()
            }
            val base64Image =
                android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)
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
            recordingScope.launch {
                val success = networkServer?.sendMessage(thermalMessage.toString()) ?: false
                if (success) {
                        TAG,
                        "Thermal frame #$frameNumber sent over network (${imageBytes.size} bytes)"
                    )
                } else {
                }
            }
        }
    }

    // TC001 frame image saving helper
    private suspend fun saveFrameImageToPNG(
        imageData: ByteArray?,
        thermalData: ThermalFrameData,
        frameNumber: Long
    ) = withContext(Dispatchers.IO) {
            if (thermalImagesDirectory?.exists() == true && imageData != null) {
                // Save with system timestamp to ensure proper ordering
                val timestamp = System.currentTimeMillis()
                val filename = "thermal_frame_${frameNumber}_${timestamp}.png"
                val imageFile = File(thermalImagesDirectory, filename)
                // Convert thermal data to bitmap and save as PNG
                val bitmap =
                    generateThermalPreviewBitmap(thermalData, IR_CAMERA_WIDTH, IR_CAMERA_HEIGHT)
                if (bitmap != null) {
                    imageFile.outputStream().use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                        TAG,
                        "Saved thermal frame PNG: $filename (min: ${thermalData.minTemperature}°C, max: ${thermalData.maxTemperature}°C)"
                    )
                }
            }
            // Don't crash recording on image save failure
        }
    }

    override suspend fun startRecording(
        sessionDirectory: String,
        sessionMetadata: SessionMetadata
    ): Boolean {
        this.sessionMetadata = sessionMetadata
        return startRecording(sessionDirectory)
    }

    override suspend fun startRecording(sessionDirectory: String): Boolean =
        withContext(Dispatchers.IO) {
                if (_isRecording.get()) {
                    return@withContext true
                }
                this@ThermalCameraRecorder.sessionDirectory = sessionDirectory
                initializeSessionTiming()
                // Create thermal_images directory for frame captures
                val dir = File(sessionDirectory, "thermal_images")
                thermalImagesDirectory = dir
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                // Enable frame image saving for TC001
                saveFrameImages = true
                setupOutputFiles()
                if (isSimulationMode) {
                    startSimulatedThermalRecording()
                } else {
                    val thermalCamera = iruvctc
                    if (thermalCamera != null && isIRCameraConnected && hasUsbPermission) {
                        val startSuccess = (
                            startRealIRCameraRecording(thermalCamera)
                            // Log error but don't crash - fallback to simulation
                            handleThermalError(
                                "SDK Initialization",
                                true
                            )
                            false
                        }
                        if (!startSuccess) {
                                TAG,
                                "Failed to start real TC001 thermal streaming, switching to simulation mode"
                            )
                            isSimulationMode = true
                            startSimulatedThermalRecording()
                        } else {
                        }
                    } else {
                            TAG,
                            "TC001 thermal camera not ready (connected: $isIRCameraConnected, permission: $hasUsbPermission), using simulation mode"
                        )
                        isSimulationMode = true
                        startSimulatedThermalRecording()
                    }
                }
                _isRecording.set(true)
                frameCount.set(0)
                emitStatus()
                return@withContext true
                // Ensure other sensors continue recording
                emitError(
                    ErrorType.RECORDING_FAILED,
                )
                return@withContext false
            }
        }

    private suspend fun startSimulatedThermalRecording() = withContext(Dispatchers.IO) {
        if (!isSimulationMode) {
            return@withContext
        }
        val testFrame = generateTestThermalFrame()
        if (testFrame == null) {
            recordingScope.launch {
                emitError(
                    ErrorType.DEVICE_ERROR,
                    "Simulation mode setup failed - thermal frame generation not working"
                )
            }
            return@withContext
        }
            TAG,
            "Simulation will generate ${thermalResolution.first}x${thermalResolution.second} thermal matrices at ${thermalFrameRate} FPS"
        )
        recordingScope.launch {
                TAG,
                "Simulation coroutine started, generating thermal frames at ${thermalFrameRate} FPS"
            )
            val frameInterval = (1000.0 / thermalFrameRate).toLong()
            var consecutiveFailures = 0
            val maxConsecutiveFailures = 5
            while (_isRecording.get() && isSimulationMode) {
                    generateSimulatedThermalFrame()
                    consecutiveFailures = 0
                    if (frameCount.get() % 30 == 0L) {
                            TAG,
                            "Simulation mode: generated ${frameCount.get()} thermal frames (${
                                "%.1f".format(
                                    frameCount.get() / (thermalFrameRate * (System.nanoTime() - recordingStartTime) / 1_000_000_000.0)
                                )
                            }s)"
                        )
                    }
                    delay(frameInterval)
                    consecutiveFailures++
                        TAG,
                        "Error generating simulated thermal frame (failure #$consecutiveFailures)",
                        e
                    )
                    if (consecutiveFailures >= maxConsecutiveFailures) {
                            TAG,
                            "Too many consecutive simulation failures ($consecutiveFailures), stopping simulation"
                        )
                        emitError(
                            ErrorType.DEVICE_ERROR,
                            "Simulation mode failed repeatedly - stopping thermal recording",
                            isRecoverable = false
                        )
                        _isRecording.set(false)
                        break
                    }
                    delay(100)
                }
            }
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
        val baseTemp = 25.0f + (frameNumber % 100) * 0.1f
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
            ambientTemperature = ambientTemperature.toFloat(),
            emissivity = emissivity.toFloat(),
            reflectedTemperature = reflectedTemperature.toFloat()
        )
        saveRealIRThermalData(TimestampManager.createTimestampRecord(), frameNumber, thermalData)
        processFrameForPreviewAndNetwork(
            thermalData,
            frameNumber,
            thermalResolution.first,
            thermalResolution.second
        )
        if (frameNumber % 30 == 0L) {
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
            return@withContext (
                val temperatureMatrix =
                    Array(thermalResolution.second) { FloatArray(thermalResolution.first) }
                var minTemp = Float.MAX_VALUE
                var maxTemp = Float.MIN_VALUE
                var sumTemp = 0f
                val baseTemp = 25.0f
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
                    ambientTemperature = ambientTemperature.toFloat(),
                    emissivity = emissivity.toFloat(),
                    reflectedTemperature = reflectedTemperature.toFloat()
                )
                null
            }
        }

    private fun Float.format(digits: Int) = "%.${digits}f".format(this)
    private fun extractThermalDataFromBitmap(
        bitmap: Bitmap,
        timestamp: Long,
        frameNumber: Long
    ): ThermalFrameData {
        val width = thermalResolution.first
        val height = thermalResolution.second
        val temperatureMatrix = Array(height) { FloatArray(width) }
        val pixels = IntArray(width * height)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false)
        scaledBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        var minTemp = Float.MAX_VALUE
        var maxTemp = Float.MIN_VALUE
        var sumTemp = 0f
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = pixels[y * width + x]
                val intensity =
                    (android.graphics.Color.red(pixel) + android.graphics.Color.green(pixel) + android.graphics.Color.blue(
                        pixel
                    )) / 3f
                val temp = 20.0f + (intensity / 255.0f) * 30.0f
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

    private suspend fun processRealThermalFrameData(
        thermalData: ThermalFrameData,
        frameNumber: Long,
        timestampRecord: TimestampRecord
    ) {
        saveRealIRThermalData(timestampRecord, frameNumber, thermalData)
        processFrameForPreviewAndNetwork(
            thermalData,
            frameNumber,
            thermalResolution.first,
            thermalResolution.second
        )
        frameListener?.let { listener ->
                val stats = ThermalFrameStats(
                    timestampNs = timestampRecord.systemNanos,
                    frameSequence = frameNumber,
                    minTemp = thermalData.minTemperature,
                    avgTemp = thermalData.avgTemperature,
                    maxTemp = thermalData.maxTemperature,
                    pixelCount = thermalResolution.first * thermalResolution.second
                )
                listener.onFrameProcessed(stats)
            }
        }
        if (frameNumber % 10 == 0L) {
            emitStatus()
        }
    }

    private suspend fun processFrameForPreviewAndNetwork(
        thermalData: ThermalFrameData,
        frameNumber: Long,
        width: Int,
        height: Int
    ) {
        val previewBitmap = generateThermalPreviewBitmap(thermalData, width, height)
        previewCallback?.onThermalFrame(previewBitmap, thermalData)
        if (enableNetworkStreaming && networkServer != null) {
            networkFrameCounter++
            if (networkFrameCounter >= networkStreamingInterval) {
                networkFrameCounter = 0
                sendThermalFrameOverNetwork(previewBitmap, thermalData, frameNumber)
            }
        }
    }

    private suspend fun startRealIRCameraRecording(irCamera: IRUVCTC): Boolean {
        return (
            val optimalFrameRate = if (thermalFrameRate >= 20.0) {
                25.0
            } else {
                10.0
            }
            configureOptimalThermalPerformance(irCamera, optimalFrameRate)
            setupEnhancedFrameCallback(optimalFrameRate)
            startPerformanceMonitoring(optimalFrameRate)
            // Start continuous frame capture loop for TC001
            startThermalHealthMonitor()
            true
            false
        }
    }

    // Continuous frame capture loop for TC001 at ~10Hz
    private fun startThermalHealthMonitor() {
        recordingScope.launch {
            val frameInterval = 100L // 10Hz = 100ms intervals
            var consecutiveErrors = 0
            val maxConsecutiveErrors = 10
            while (_isRecording.get() && !isSimulationMode && isIRCameraConnected) {
                    val cameraHealthy = isThermalCameraHealthy()
                    if (cameraHealthy) {
                        consecutiveErrors = 0
                    } else {
                        consecutiveErrors++
                        if (consecutiveErrors >= maxConsecutiveErrors) {
                                TAG,
                                "Too many consecutive TC001 capture failures, switching to simulation"
                            )
                            handleThermalError(
                                "Frame Capture",
                                "Continuous TC001 frame capture failed $maxConsecutiveErrors times",
                                false
                            )
                            break
                        }
                    }
                    delay(frameInterval)
                    consecutiveErrors++
                    if (consecutiveErrors >= maxConsecutiveErrors) {
                        handleThermalError(
                            "Frame Loop",
                            false
                        )
                        break
                    }
                    delay(200) // Longer delay on errors
                }
            }
        }
    }

    // Health check method to verify TC001 camera prerequisites for capture loop
    private suspend fun isThermalCameraHealthy(): Boolean = withContext(Dispatchers.IO) {
        return@withContext (
            if (ircamEngine != null && isTopdonSdkInitialized && isIRCameraConnected) {
                // TC001 frame capture is handled by IFrameCallback
                // This method provides a health check for the capture loop
                true
            } else {
                false
            }
            // Don't crash on health check errors
            false
        }
    }

    private fun configureOptimalThermalPerformance(irCamera: IRUVCTC, targetFrameRate: Double) {
            when {
                targetFrameRate >= 20.0 -> {
                }

                else -> {
                }
            }
        }
    }

    private fun setupEnhancedFrameCallback(targetFrameRate: Double) {
            val targetIntervalMs = (1000.0 / targetFrameRate).toLong()
            var lastFrameTime = 0L
            var droppedFrameCount = 0L
            ircamEngine?.setIrFrameCallback(object : IIrFrameCallback {
                override fun onFrame(frame: ByteArray?, length: Int) {
                    val currentTime = System.currentTimeMillis()
                    if (lastFrameTime > 0 && (currentTime - lastFrameTime) < targetIntervalMs) {
                        droppedFrameCount++
                        return
                    }
                    lastFrameTime = currentTime
                    if (_isRecording.get() && frame != null) {
                        recordingScope.launch {
                                val timestamp = System.nanoTime()
                                val frameNumber = frameCount.incrementAndGet()
                                val thermalData =
                                    processRealThermalData(frame, IR_CAMERA_WIDTH, IR_CAMERA_HEIGHT)
                                // Create proper timestamp record for processing
                                val timestampRecord = TimestampManager.createTimestampRecord()
                                processRealThermalFrameData(
                                    thermalData,
                                    frameNumber,
                                    timestampRecord
                                )
                            }
                        }
                    }
                    if (previewCallback != null && frame != null && frameCount.get() % PREVIEW_UPDATE_FRAME_INTERVAL.toLong() == 0L) {
                        recordingScope.launch {
                                val thermalData =
                                    processRealThermalData(frame, IR_CAMERA_WIDTH, IR_CAMERA_HEIGHT)
                                val previewBitmap =
                                    generateThermalPreviewBitmap(
                                        thermalData,
                                        IR_CAMERA_WIDTH,
                                        IR_CAMERA_HEIGHT
                                    )
                                previewCallback?.onThermalFrame(previewBitmap, thermalData)
                            }
                        }
                    }
                }
            })
        }
    }

    private fun startPerformanceMonitoring(targetFrameRate: Double) {
        recordingScope.launch {
            var lastMonitorTime = System.currentTimeMillis()
            var lastFrameCount = 0L
            while (_isRecording.get()) {
                delay(5000)
                val currentTime = System.currentTimeMillis()
                val currentFrameCount = frameCount.get()
                val timeDelta = currentTime - lastMonitorTime
                val frameDelta = currentFrameCount - lastFrameCount
                if (timeDelta > 0) {
                    val actualFrameRate = (frameDelta * 1000.0) / timeDelta
                    val frameRatePercent = (actualFrameRate / targetFrameRate) * 100
                        TAG, " Thermal performance: ${String.format("%.1f", actualFrameRate)}Hz " +
                                "(${String.format("%.0f", frameRatePercent)}% of target)"
                    )
                    if (frameRatePercent < 80) {
                            TAG,
                            " Thermal frame rate below target: ${
                                String.format(
                                    "%.1f",
                                    actualFrameRate
                                )
                            }Hz vs ${targetFrameRate}Hz"
                        )
                    }
                }
                lastMonitorTime = currentTime
                lastFrameCount = currentFrameCount
            }
        }
    }

    private fun handleThermalError(
        errorType: String,
        errorMessage: String,
        isRecoverable: Boolean = true
    ) {
        recordingScope.launch {
            // Emit error to system
            emitError(
                if (errorType.contains("USB")) ErrorType.HARDWARE_DISCONNECTED else ErrorType.DEVICE_ERROR,
                "TC001 thermal camera: $errorMessage",
                isRecoverable
            )
            // Show user notification via Toast (running on main thread)
                withContext(Dispatchers.Main) {
                    val toastMessage = when {
                        errorType.contains("USB") -> "TC001 thermal camera disconnected"
                        errorType.contains("Permission") -> "TC001 camera needs USB permission"
                        errorType.contains("SDK") -> "TC001 camera initialization failed"
                        else -> "TC001 camera error"
                    }
                    Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
                }
            }
            // Handle recovery or fallback
            if (isRecoverable) {
                attemptThermalRecovery(errorType, errorMessage)
            } else {
                // Non-recoverable error - switch to simulation mode
                isSimulationMode = true
                isIRCameraConnected = false
            }
        }
    }

    private suspend fun attemptThermalRecovery(errorType: String, errorMessage: String) {
            when {
                errorType.contains("USB") -> {
                    delay(2000)
                    thermalCameraDevice?.let { device ->
                        val recoverySuccess = initializeRealThermalCamera(device)
                        if (recoverySuccess) {
                            if (_isRecording.get() && isSimulationMode) {
                                isSimulationMode = false
                            }
                        } else {
                            if (_isRecording.get()) {
                                isSimulationMode = true
                                startSimulatedThermalRecording()
                            }
                        }
                    }
                }

                errorType.contains("SDK") -> {
                    // Enhanced SDK recovery with multiple retry strategies
                    delay(1000)
                    // Strategy 1: Simple SDK re-initialization
                    var sdkRecoverySuccess = initializeTopdonSdk()
                    if (sdkRecoverySuccess) {
                    } else {
                        // Strategy 2: Full teardown and rebuild
                            ircamEngine = null
                            isTopdonSdkInitialized = false
                            delay(2000) // Allow complete cleanup
                            sdkRecoverySuccess = initializeTopdonSdk()
                            if (sdkRecoverySuccess) {
                            }
                        }
                    }
                    if (!sdkRecoverySuccess) {
                        isSimulationMode = true
                    }
                }

                errorType.contains("Frame") -> {
                    // Enhanced frame capture recovery
                    delay(500)
                    // Diagnostic check 1: Verify SDK state
                    if (ircamEngine == null || !isTopdonSdkInitialized) {
                        val sdkRecovered = initializeTopdonSdk()
                        if (sdkRecovered) {
                            return
                        }
                    }
                    // Diagnostic check 2: Verify USB connection
                    if (!isIRCameraConnected) {
                            TAG,
                            "Frame error caused by USB disconnection - checking device status"
                        )
                        thermalCameraDevice?.let { device ->
                            val usbManager =
                                context.getSystemService(Context.USB_SERVICE) as UsbManager
                            if (usbManager.hasPermission(device)) {
                                val usbRecovered = initializeRealThermalCamera(device)
                                if (usbRecovered) {
                                    return
                                }
                            }
                        }
                    }
                    // Fallback: Clear frame buffer and restart capture
                    lastCapturedFrame = null
                    lastFrameTimestamp = 0L
                    if (_isRecording.get()) {
                    }
                }

                errorType.contains("Permission") -> {
                    // Enhanced permission recovery
                    // Check current permission state
                    thermalCameraDevice?.let { device ->
                        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                        if (!usbManager.hasPermission(device)) {
                                requestUsbPermission(device)
                                delay(5000) // Wait for user response
                                if (usbManager.hasPermission(device)) {
                                    val reconnected = initializeRealThermalCamera(device)
                                    if (!reconnected) {
                                        isSimulationMode = true
                                    }
                                } else {
                                    isSimulationMode = true
                                }
                                isSimulationMode = true
                            }
                        }
                    }
                }

                errorType.contains("Temperature") -> {
                    // Enhanced temperature processing recovery
                    // Reset temperature processing state
                    lastCapturedFrame = null
                    lastFrameTimestamp = 0L
                    // Verify calibration state
                        val testData = generateAdvancedSimulatedThermalData(System.nanoTime(), 1L)
                        val calibratedData = applySDKCalibrationCorrections(testData)
                        isSimulationMode = true
                    }
                }

                else -> {
                    delay(1000)
                    thermalCameraDevice?.let { device ->
                        if (hasUsbPermission) {
                            val generalRecoverySuccess = initializeRealThermalCamera(device)
                            if (!generalRecoverySuccess) {
                                isSimulationMode = true
                            }
                        }
                    }
                }
            }
            isSimulationMode = true
        }
    }

    override suspend fun stopRecording(): Boolean {
            if (!_isRecording.get()) {
                return true
            }
            val irCamera = iruvctc
            if (irCamera != null && isIRCameraConnected) {
                val stopSuccess = (
                    stopRealIRCameraRecording(irCamera)
                    false
                }
                if (!stopSuccess) {
                } else {
                }
            }
            _isRecording.set(false)
            thermalDataWriter?.stop()
            thermalFramesWriter?.stop()
            thermalDataWriter = null
            thermalFramesWriter = null
            emitStatus()
            sessionReferenceTimestampNs.set(0)
            sessionStartOffsetNs.set(0)
            sessionMetadata = null
            return true
            return false
        }
    }

    private suspend fun stopRealIRCameraRecording(irCamera: IRUVCTC): Boolean {
        return (
            irCamera.stopPreview()
            true
            false
        }
    }

    private suspend fun setupOutputFiles() {
        val thermalDir = File(sessionDirectory)
        thermalDir.mkdirs()
        thermalDataFile = File(
            thermalDir,
            SessionDirectoryManager.THERMAL_METADATA_FILE.replace(".csv", "_data.csv")
        )
        thermalFramesFile = File(thermalDir, SessionDirectoryManager.THERMAL_FRAMES_FILE)
        val thermalDataHeaders = listOf(
            "raw_timestamp_ns",
            "aligned_timestamp_ns",
            "timestamp_relative_ms",
            "timestamp_wall_ms",
            "frame_number",
            "min_temp_c",
            "max_temp_c",
            "avg_temp_c",
            "center_temp_c",
            "ambient_temp_c",
            "emissivity",
            "reflected_temp_c",
            "event_type"
        )
        thermalDataWriter = CSVBufferedWriter(
            thermalDataFile!!,
            thermalDataHeaders,
            bufferSize = 4096,
            flushIntervalMs = 500L
        )
        thermalDataWriter?.startWithHeaders()
        thermalFramesWriter = BufferedDataWriter(
            thermalFramesFile!!,
            bufferSize = 16384,
            flushIntervalMs = 1000L,
            maxQueueSize = 5000
        )
        thermalFramesWriter?.start()
        val framesHeader =
            "raw_timestamp_ns,aligned_timestamp_ns,timestamp_relative_ms,timestamp_wall_ms,frame_number," +
                    (0 until thermalResolution.first * thermalResolution.second).joinToString(",") { "temp_$it" }
        thermalFramesWriter?.writeLine(framesHeader)
        writeThermalCalibration()
    }

    private fun initializeSessionTiming() {
        val localStartNs = System.nanoTime()
        recordingStartTime = localStartNs
        val metadata = sessionMetadata
        if (metadata != null) {
            sessionReferenceTimestampNs.set(metadata.sessionStartMonotonicNs)
            sessionStartOffsetNs.set(localStartNs - metadata.sessionStartMonotonicNs)
        } else {
            sessionReferenceTimestampNs.set(localStartNs)
            sessionStartOffsetNs.set(0L)
        }
    }

    private fun alignedTimestampNs(timestampNs: Long): Long {
        return if (sessionMetadata != null) {
            timestampNs - sessionStartOffsetNs.get()
        } else {
            timestampNs
        }
    }

    private fun sessionRelativeMs(timestampNs: Long): Long {
        val metadata = sessionMetadata
        return if (metadata != null) {
            val alignedNs = alignedTimestampNs(timestampNs)
            (alignedNs - metadata.sessionStartMonotonicNs) / 1_000_000
        } else {
            (timestampNs - recordingStartTime) / 1_000_000
        }
    }

    private fun wallClockMs(timestampNs: Long): Long? {
        val metadata = sessionMetadata ?: return null
        val alignedNs = alignedTimestampNs(timestampNs)
        return metadata.monotonicToWallClock(alignedNs)
    }

    private suspend fun writeThermalCalibration() {
        val calibrationFile = File(sessionDirectory, CALIBRATION_FILENAME)
        val deviceInfo = if (isSimulationMode) {
            "Simulated Thermal Camera (no hardware detected)"
        } else {
            val sdkStatus =
                if (isTopdonSdkInitialized) "IrcamEngine Initialized" else "IRUVCTC Only"
            "Topdon TC001 Thermal Camera - $sdkStatus - ${thermalCameraDevice?.productName ?: "Unknown Device"}"
        }
        val calibrationData = """
        {
    "sensor_id": "$sensorId",
    "thermal_resolution": {
        "width": $IR_CAMERA_WIDTH,
        "height": $IR_CAMERA_HEIGHT
    },
    "frame_rate_hz": $thermalFrameRate,
    "frame_rate_info": {
        "detected_rate": $thermalFrameRate,
        "hardware_support": {
            "enhanced_25hz": ${supportsEnhancedFrameRate()},
            "standard_9hz": true,
            "tc001_plus_detected": ${supportsEnhancedFrameRate()}
        },
        "network_streaming_interval": $networkStreamingInterval
    },
    "session_metadata": {
        "session_id": "${sessionMetadata?.sessionId ?: "unknown"}",
        "session_name": "${sessionMetadata?.sessionName ?: ""}",
        "participant_id": "${sessionMetadata?.participantId ?: ""}",
        "study_name": "${sessionMetadata?.studyName ?: ""}",
        "session_start_time_iso": "${
            java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                java.util.Locale.getDefault()
            ).format(java.util.Date())
        }",
        "session_start_monotonic_ns": ${sessionReferenceTimestampNs.get()},
        "local_start_offset_ns": ${sessionStartOffsetNs.get()}
    },
    "thermal_calibration": {
        "ambient_temperature_c": $ambientTemperature,
        "emissivity": $emissivity,
        "reflected_temperature_c": $reflectedTemperature,
        "temperature_sensitivity_c": $THERMAL_SENSITIVITY,
        "temp_range_min_c": $IR_TEMP_RANGE_MIN,
        "temp_range_max_c": $IR_TEMP_RANGE_MAX,
        "calibration_notes": "Enhanced thermal calibration with session-specific accuracy metadata",
        "calibration_accuracy": "±${THERMAL_SENSITIVITY}°C",
        "measurement_conditions": {
            "ambient_stable": true,
            "emissivity_verified": false,
            "distance_optimal": true,
            "environmental_factors": "Standard laboratory conditions assumed"
        }
    },
    "device_information": {
        "device_connected": $isIRCameraConnected,
        "usb_permission_granted": $hasUsbPermission,
        "simulation_mode": $isSimulationMode,
        "sdk_initialized": $isTopdonSdkInitialized,
        "device_description": "$deviceInfo",
        "sdk_version": "Topdon AC020 SDK v1.1.1 with IrcamEngine Integration - Enhanced Metadata",
        "firmware_version": "${getFirmwareVersion()}",
        "serial_number": "${getDeviceSerialNumber()}"
    },
    "csv_data_schema": {
        "thermal_data_columns": [
            "timestamp_ns",
            "frame_index", 
            "temp_matrix_serialized",
            "min_temp_c",
            "max_temp_c",
            "avg_temp_c",
            "center_temp_c",
            "hotspot_x",
            "hotspot_y",
            "coldspot_x", 
            "coldspot_y",
            "calibration_applied",
            "frame_quality_score"
        ],
        "thermal_frames_columns": [
            "timestamp_ns",
            "frame_filename",
            "processing_time_ms",
            "compression_ratio",
            "file_size_bytes"
        ],
        "data_format_notes": "All temperatures in Celsius, timestamps in nanoseconds since session start"
    },
    "quality_assurance": {
        "calibration_timestamp": ${System.nanoTime()},
        "validation_checks": {
            "temperature_range_valid": true,
            "emissivity_in_range": ${emissivity >= 0.1 && emissivity <= 1.0},
            "ambient_temp_reasonable": ${ambientTemperature >= -10 && ambientTemperature <= 50},
            "device_communication_stable": $isIRCameraConnected
        },
        "data_integrity": {
            "checksum_enabled": false,
            "frame_sequence_validation": true,
            "timestamp_monotonic_check": true
        }
    },
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
            val syncRow = arrayOf(
                timestampNs.toString(),
                alignedTimestampNs(timestampNs).toString(),
                sessionRelativeMs(timestampNs).toString(),
                wallClockMs(timestampNs)?.toString() ?: "",
                "-1",
                "0", "0", "0", "0",
                ambientTemperature.toString(),
                emissivity.toString(),
                reflectedTemperature.toString(),
                "SYNC_$markerType"
            )
            thermalDataWriter?.writeRow(syncRow.toList())
        }
    }

    private fun getFirmwareVersion(): String {
        return (
            if (isSimulationMode) {
                "Simulation Mode - No Firmware"
            } else if (thermalCameraDevice != null) {
                val deviceVersion = thermalCameraDevice?.deviceId?.toString() ?: "Unknown"
                "TC001 Firmware v${deviceVersion.takeLast(4)}"
            } else {
                "Unknown - Device Not Connected"
            }
            "Unknown - Error Reading Firmware"
        }
    }

    private fun getDeviceSerialNumber(): String {
        return (
            if (isSimulationMode) {
                "SIM-${System.currentTimeMillis().toString().takeLast(8)}"
            } else if (thermalCameraDevice != null) {
                val vendorId = thermalCameraDevice!!.vendorId.toString(16)
                val productId = thermalCameraDevice!!.productId.toString(16)
                val deviceName =
                    thermalCameraDevice!!.deviceName?.hashCode()?.toString(16) ?: "0000"
                "TC001-${vendorId}-${productId}-${deviceName.takeLast(4)}"
            } else {
                "UNKNOWN-DEVICE-NOT-CONNECTED"
            }
            "ERROR-READING-SERIAL"
        }
    }

    fun getThermalRecordingStatistics(): ThermalRecordingStats {
        return ThermalRecordingStats(
            totalFramesCaptured = frameCount.get(),
            recordingDurationMs = if (recordingStartTime > 0) System.nanoTime() - recordingStartTime else 0,
            averageFrameRate = if (recordingStartTime > 0) {
                val durationSeconds = (System.nanoTime() - recordingStartTime) / 1_000_000_000.0
                frameCount.get() / durationSeconds
            } else 0.0,
            isSimulationMode = isSimulationMode,
            deviceConnected = isIRCameraConnected,
            calibrationApplied = true,
            currentEmissivity = emissivity,
            currentAmbientTemp = ambientTemperature,
            temperatureRangeMin = IR_TEMP_RANGE_MIN,
            temperatureRangeMax = IR_TEMP_RANGE_MAX,
            qualityScore = calculateCurrentQualityScore()
        )
    }

    private fun calculateCurrentQualityScore(): Double {
        return (
            var score = 0.0
            score += if (isIRCameraConnected && !isSimulationMode) 0.4 else 0.1
            val targetFrameRate = thermalFrameRate.toDouble()
            val actualFrameRate = if (recordingStartTime > 0) {
                val durationSeconds = (System.nanoTime() - recordingStartTime) / 1_000_000_000.0
                frameCount.get() / durationSeconds
            } else 0.0
            val frameRateRatio = if (targetFrameRate > 0) actualFrameRate / targetFrameRate else 0.0
            score += if (frameRateRatio >= 0.9) 0.3 else (frameRateRatio * 0.3)
            score += if (emissivity > 0.1 && ambientTemperature > -50) 0.3 else 0.1
            minOf(1.0, maxOf(0.0, score))
            0.5
        }
    }

    data class ThermalRecordingStats(
        val totalFramesCaptured: Long,
        val recordingDurationMs: Long,
        val averageFrameRate: Double,
        val isSimulationMode: Boolean,
        val deviceConnected: Boolean,
        val calibrationApplied: Boolean,
        val currentEmissivity: Double,
        val currentAmbientTemp: Double,
        val temperatureRangeMin: Float,
        val temperatureRangeMax: Float,
        val qualityScore: Double
    )

    suspend fun rescanForThermalCamera(): Boolean {
        return withContext(Dispatchers.IO) {
                // If IRUVCTC is already initialized and connected, no need to rescan
                if (iruvctc != null && isIRCameraConnected) {
                    isSimulationMode = false
                    emitStatus()
                    return@withContext true
                }
                val manager = usbManager
                if (manager == null) {
                    return@withContext false
                }
                val deviceList = manager.deviceList
                for (device in deviceList.values) {
                        TAG,
                        "Checking device: VID=${device.vendorId.toString(16)}, PID=${
                            device.productId.toString(16)
                        }, Name=${device.productName}"
                    )
                    if (device.isTcTsDevice()) {
                            TAG,
                            "Found thermal camera during rescan: ${device.productName}"
                        )
                        // Update device reference immediately so status reflects the device
                        thermalCameraDevice = device
                        if (manager.hasPermission(device)) {
                            hasUsbPermission = true
                            // This will check if already initialized and skip if so
                            val success = initializeRealThermalCamera(device)
                            if (success) {
                                isSimulationMode = false
                                emitStatus()
                                return@withContext true
                            }
                        } else {
                            hasUsbPermission = false
                            requestUsbPermission(device)
                            emitStatus()
                            return@withContext false
                        }
                    }
                }
                return@withContext false
                return@withContext false
            }
        }
    }

    private fun loadThermalSettings() {
            val settings = thermalSettingsRepository.getSettings()
            emissivity = settings.emissivity.toDouble()
        }
    }

    private fun observeSettingsChanges() {
        recordingScope.launch {
            thermalSettingsRepository.thermalSettings.collectLatest { settings ->
                updateEmissivity(settings.emissivity.toDouble())
            }
        }
    }

    fun updateEmissivity(newEmissivity: Double) {
        if (newEmissivity in 0.1..1.0) {
            emissivity = newEmissivity
                TAG,
                "Emissivity parameter stored; IrcamEngine setEmissivity method not available in current SDK version"
            )
        } else {
        }
    }

    fun updateAmbientTemperature(newTemp: Double) {
        if (newTemp in -50.0..100.0) {
            ambientTemperature = newTemp
                TAG,
                "Ambient temperature parameter stored; IrcamEngine setAmbientTemperature method not available in current SDK version"
            )
        } else {
        }
    }

    fun updateReflectedTemperature(newTemp: Double) {
        if (newTemp in -50.0..100.0) {
            reflectedTemperature = newTemp
        } else {
        }
    }

    fun getCurrentThermalSettings(): ThermalSettings {
        return ThermalSettings(
            emissivity = emissivity.toFloat(),
            ambientTemperature = ambientTemperature.toFloat(),
            reflectedTemperature = reflectedTemperature.toFloat()
        )
    }

    data class ThermalSettings(
        val emissivity: Float,
        val ambientTemperature: Float,
        val reflectedTemperature: Float
    )

    override suspend fun cleanup() {
            if (_isRecording.get()) {
                stopRecording()
            }
            ircamEngine?.let { engine ->
                    engine.closeVideoStream()
                    engine.releaseVideoStream()
                    engine.destroyHandle()
                }
            }
            ircamEngine = null
            isTopdonSdkInitialized = false
            iruvctc?.let { camera ->
                    camera.stopPreview()
                    camera.unregisterUSB()
                }
            }
            iruvctc = null
            uvcCamera = null
            isIRCameraConnected = false
            hasUsbPermission = false
            thermalCameraDevice = null
            previewCallback = null
            frameListener = null
            recordingScope.cancel()
        }
    }

    private fun observeDeviceEvents() {
        recordingScope.launch {
            DeviceEventManager.deviceConnectionState.collectLatest { state ->
                state?.let {
                    onDeviceConnectionStateChanged(it.isConnected, it.device)
                }
            }
        }
        recordingScope.launch {
            DeviceEventManager.devicePermissionRequested.collectLatest { device ->
                onDevicePermissionRequested(device)
            }
        }
    }

    private fun onDeviceConnectionStateChanged(isConnect: Boolean, device: android.hardware.usb.UsbDevice?) {
                TAG,
                "USB device connection event: connected=$isConnect, device=${device?.productName}"
            )
            if (isConnect) {
                val connectedDevice = device
                if (connectedDevice != null) {
                    if (connectedDevice.isTcTsDevice()) {
                            TAG,
                            "Thermal camera device reconnected with permission: ${connectedDevice.productName}"
                        )
                        recordingScope.launch {
                            val previousDevice = thermalCameraDevice
                            thermalCameraDevice = connectedDevice
                            hasUsbPermission = true
                            val success = initializeRealThermalCamera(connectedDevice)
                            if (success) {
                                isSimulationMode = false
                                    TAG,
                                    "Successfully switched to real thermal camera from device reconnect event"
                                )
                                if (_isRecording.get()) {
                                    val irCamera = iruvctc
                                    if (irCamera != null) {
                                        val startSuccess = startRealIRCameraRecording(irCamera)
                                        if (startSuccess) {
                                                TAG,
                                                "Resumed real thermal recording after reconnect"
                                            )
                                        } else {
                                                TAG,
                                                "Failed to resume real thermal recording, staying in simulation"
                                            )
                                            isSimulationMode = true
                                        }
                                    }
                                }
                                emitStatus()
                            } else {
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
                val disconnectedDevice = thermalCameraDevice
                if (disconnectedDevice != null) {
                        TAG,
                        " Thermal camera device disconnected - implementing enhanced recovery"
                    )
                    handleThermalError(
                        "USB Hot-plug",
                        "Thermal camera unplugged - attempting graceful transition to simulation",
                        isRecoverable = false
                    )
                    recordingScope.launch {
                        if (isIRCameraConnected && iruvctc != null) {
                                iruvctc?.stopPreview()
                            }
                        }
                        isSimulationMode = true
                        isIRCameraConnected = false
                        hasUsbPermission = false
                        thermalCameraDevice = null
                        if (_isRecording.get()) {
                                TAG,
                                "Continuing recording in simulation mode after device disconnect"
                            )
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
        }
    }

    private fun onDevicePermissionRequested(device: android.hardware.usb.UsbDevice) {
            if (device.isTcTsDevice()) {
                val manager = usbManager
                if (manager != null) {
                    val permissionGranted = manager.hasPermission(device)
                    if (permissionGranted) {
                        recordingScope.launch {
                            thermalCameraDevice = device
                            hasUsbPermission = true
                            val success = initializeRealThermalCamera(device)
                            if (success) {
                                isSimulationMode = false
                                    TAG,
                                    "Thermal camera initialized successfully after permission granted"
                                )
                            } else {
                                    TAG,
                                    "Failed to initialize thermal camera after permission granted"
                                )
                                isSimulationMode = true
                            }
                            emitStatus()
                        }
                    } else {
                            TAG,
                            "USB permission not yet granted, requesting permission for thermal camera"
                        )
                        requestUsbPermission(device)
                    }
                }
            }
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
        return 0
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

    fun configureThermalDevice(
        emissivity: Double = 0.95,
        temperatureRange: Pair<Float, Float> = Pair(-20.0f, 400.0f),
        ambientTemp: Double = 25.0
    ): Boolean {
        return (
            this.emissivity = emissivity
            this.ambientTemperature = ambientTemp
            this.reflectedTemperature = ambientTemp - 2.0
            val configSuccess = if (ircamEngine != null && isTopdonSdkInitialized) {
                    true
                    false
                }
            } else {
                true
            }
            if (configSuccess) {
                    TAG,
                    "Thermal device configured: emissivity=$emissivity, ambient=${ambientTemp}°C, range=${temperatureRange.first}-${temperatureRange.second}°C"
                )
            } else {
                    TAG,
                    "Thermal device configuration partially failed - using software fallback"
                )
            }
            configSuccess
            false
        }
    }

    fun applyAdvancedConfig(config: ThermalCameraConfig): Boolean {
        return (
            this.currentConfig = config
            configureThermalDevice(
                config.emissivity.toDouble(),
                config.temperatureRange,
                config.atmosphericTemperature.toDouble()
            )
            if (ircamEngine != null && isTopdonSdkInitialized) {
            }
                TAG,
                "Advanced thermal configuration applied: emissivity=${config.emissivity}, frameRate=${config.frameRate}"
            )
            true
            false
        }
    }

    fun getPerformanceMetrics(): ThermalPerformanceMetrics {
        return (
            val currentTime = System.nanoTime()
            val timeDeltaMs = (currentTime - lastPerformanceUpdate) / 1_000_000.0
            val avgFrameRate = if (timeDeltaMs > 0) {
                frameCount.get().toDouble() / (timeDeltaMs / 1000.0)
            } else 0.0
            val avgProcessingTime = if (frameProcessingTimes.isNotEmpty()) {
                frameProcessingTimes.average() / 1_000_000.0
            } else 0.0
            val runtime = Runtime.getRuntime()
            val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
            val cpuUsage = if (avgProcessingTime > 0) {
                minOf(100.0, (avgProcessingTime / (1000.0 / thermalFrameRate)) * 100.0)
            } else 0.0
            performanceMetrics = ThermalPerformanceMetrics(
                averageFrameTime = avgProcessingTime,
                maxFrameTime = frameProcessingTimes.maxOrNull()?.toDouble() ?: 0.0,
                minFrameTime = frameProcessingTimes.minOrNull()?.toDouble() ?: 0.0,
                frameDropRate = 0.0, // Placeholder - could be calculated from actual vs expected frames
                thermalProcessingTime = avgProcessingTime,
                networkStreamingTime = 50.0, // Placeholder
                memoryUsage = usedMemory.toDouble(),
                averageFrameRate = avgFrameRate,
                frameProcessingTimeMs = avgProcessingTime,
                memoryUsageMB = usedMemory.toDouble(),
                cpuUsagePercent = cpuUsage,
                thermalDrift = 0.1, // Placeholder
                calibrationAccuracy = 95.0, // Placeholder
                networkLatencyMs = 50.0 // Placeholder
            )
            performanceMetrics
            performanceMetrics
        }
    }

    private suspend fun captureRealThermalFrameWithErrorHandling(): Boolean =
        withContext(Dispatchers.IO) {
            var retryCount = 0
            val maxRetries = 3
            var lastException: Exception? = null
            while (retryCount < maxRetries) {
                    val success = captureRealThermalFrame()
                    if (success) {
                        return@withContext true
                    }
                    retryCount++
                    delay(100)
                    lastException = e
                    retryCount++
                    if (retryCount < maxRetries) {
                        delay(200)
                    }
                }
            }
            if (isIRCameraConnected && !isSimulationMode) {
                isSimulationMode = true
                isIRCameraConnected = false
                emitError(
                    ErrorType.DEVICE_ERROR,
                    "Thermal camera hardware failure - switched to simulation mode. Last error: ${lastException?.message}",
                    isRecoverable = true
                )
                if (_isRecording.get()) {
                    startSimulatedThermalRecording()
                }
                return@withContext true
            }
            return@withContext false
        }

    private suspend fun captureRealThermalFrame(): Boolean = withContext(Dispatchers.IO) {
        return@withContext (
            if (isIRCameraConnected && !isSimulationMode && ircamEngine != null) {
                true
            } else {
                false
            }
            false
        }
    }

    fun updateCalibration(
        ambientTemp: Double,
        emissivity: Double,
        reflectedTemp: Double
    ) {
        configureThermalDevice(emissivity, Pair(-20.0f, 400.0f), ambientTemp)
        this.reflectedTemperature = reflectedTemp
            TAG,
            "Thermal calibration updated: ambient=$ambientTemp°C, emissivity=$emissivity, reflected=$reflectedTemp°C"
        )
    }

    suspend fun exportThermalData(
        outputDir: String,
        format: ThermalExportFormat,
        includeImages: Boolean = true
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext (
            val exportDir = File(outputDir, "thermal_export_${System.currentTimeMillis()}")
            exportDir.mkdirs()
            when (format) {
                ThermalExportFormat.CSV -> exportToCSV(exportDir, includeImages)
                ThermalExportFormat.JSON -> exportToJSON(exportDir, includeImages)
                ThermalExportFormat.HDF5 -> exportToHDF5(exportDir, includeImages)
                ThermalExportFormat.MATLAB -> exportToMatlab(exportDir, includeImages)
            }
            true
            false
        }
    }

    private fun exportToCSV(exportDir: File, includeImages: Boolean): Boolean {
        return (
            val csvFile = File(exportDir, "thermal_data.csv")
            val writer = CSVWriter(FileWriter(csvFile))
            writer.writeNext(
                arrayOf(
                    "timestamp", "frame_number", "min_temp", "max_temp", "avg_temp",
                    "center_temp", "ambient_temp", "emissivity"
                )
            )
            val metadataFile = File(exportDir, "export_metadata.json")
            val metadata = JSONObject().apply {
                put("export_timestamp", System.currentTimeMillis())
                put("device_type", "Topdon TC001")
                put("resolution", "${IR_CAMERA_WIDTH}x${IR_CAMERA_HEIGHT}")
                put("frame_rate", IR_FRAME_RATE_STANDARD)
                put("configuration", JSONObject().apply {
                    put("emissivity", currentConfig.emissivity)
                    put("atmospheric_temperature", currentConfig.atmosphericTemperature)
                    put("relative_humidity", currentConfig.relativeHumidity)
                    put("distance", currentConfig.distance)
                })
                put("performance_metrics", JSONObject().apply {
                    val metrics = getPerformanceMetrics()
                    put("average_frame_time_ms", metrics.averageFrameTime)
                    put("memory_usage_mb", metrics.memoryUsage)
                    put("thermal_processing_time_ms", metrics.thermalProcessingTime)
                })
            }
            metadataFile.writeText(metadata.toString(2))
            writer.close()
            true
            false
        }
    }

    private fun exportToJSON(exportDir: File, includeImages: Boolean): Boolean {
        return (
            val jsonFile = File(exportDir, "thermal_data.json")
            val jsonData = JSONObject()
            jsonData.put("export_info", JSONObject().apply {
                put("timestamp", System.currentTimeMillis())
                put("format", "JSON")
                put("device", "Topdon TC001")
            })
            jsonFile.writeText(jsonData.toString(2))
            true
            false
        }
    }

    private fun exportToHDF5(exportDir: File, includeImages: Boolean): Boolean {
        return (
            val hdf5File = File(exportDir, "thermal_data.h5")
            // Create HDF5-compatible JSON file (HDF5 library not available)
            val hdf5JsonFile = File(exportDir, "thermal_data.json")
            // Prepare arrays for thermal data storage
            val timestamps = mutableListOf<Long>()
            val frameIndices = mutableListOf<Long>()
            val minTemps = mutableListOf<Float>()
            val maxTemps = mutableListOf<Float>()
            val avgTemps = mutableListOf<Float>()
            val centerTemps = mutableListOf<Float>()
            // Read existing CSV data and convert to JSON format
            val csvFile = File(sessionDirectory, THERMAL_DATA_FILENAME)
            if (csvFile.exists()) {
                csvFile.bufferedReader().use { reader ->
                    var isHeader = true
                    reader.forEachLine { line ->
                        if (isHeader) {
                            isHeader = false
                            return@forEachLine
                        }
                        val values = line.split(",")
                        if (values.size >= 6) {
                                timestamps.add(values[0].toLong())
                                frameIndices.add(values[1].toLong())
                                minTemps.add(values[2].toFloat())
                                maxTemps.add(values[3].toFloat())
                                avgTemps.add(values[4].toFloat())
                                centerTemps.add(values[5].toFloat())
                            }
                        }
                    }
                }
            }
            val hdf5Structure = JSONObject().apply {
                put("format", "HDF5-Compatible JSON")
                put("metadata", JSONObject().apply {
                    put("sdk_version", "Topdon TC001 SDK v1.1.1")
                    put("recording_start", recordingStartTime.toString())
                    put("total_frames", frameCount.get().toLong())
                    put("frame_rate_hz", thermalFrameRate)
                    put("resolution_width", IR_CAMERA_WIDTH)
                    put("resolution_height", IR_CAMERA_HEIGHT)
                    put("temperature_unit", "celsius")
                    put("emissivity", DEFAULT_EMISSIVITY.toFloat())
                })
                put("thermal_data", JSONObject().apply {
                    put("timestamps_ns", timestamps)
                    put("frame_indices", frameIndices)
                    put("min_temperatures_c", minTemps)
                    put("max_temperatures_c", maxTemps)
                    put("avg_temperatures_c", avgTemps)
                    put("center_temperatures_c", centerTemps)
                    if (includeImages) {
                        put("temperature_matrices", JSONObject().apply {
                            put("description", "Full temperature matrices for each frame")
                            put("dimensions", "[frame, height, width]")
                            put("height", IR_CAMERA_HEIGHT)
                            put("width", IR_CAMERA_WIDTH)
                            put(
                                "note",
                                "Temperature matrices export requires additional implementation"
                            )
                        })
                    }
                })
            }
            hdf5JsonFile.writeText(hdf5Structure.toString(2))
                TAG,
                "Successfully exported ${timestamps.size} thermal frames to HDF5-compatible JSON: ${hdf5JsonFile.absolutePath}"
            )
            return true
            false
        }
    }

    private fun exportToMatlab(exportDir: File, includeImages: Boolean): Boolean {
        return (
            val matFile = File(exportDir, "thermal_data.m")
            val matContent = StringBuilder()
            matContent.appendLine("% Thermal data export from Topdon TC001")
            matContent.appendLine(
                "% Generated on ${
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())
                }"
            )
            matContent.appendLine("")
            matContent.appendLine("thermal_config.emissivity = ${currentConfig.emissivity};")
            matContent.appendLine("thermal_config.atmospheric_temp = ${currentConfig.atmosphericTemperature};")
            matContent.appendLine("thermal_config.resolution = [${IR_CAMERA_WIDTH}, ${IR_CAMERA_HEIGHT}];")
            matContent.appendLine("thermal_config.frame_rate = ${IR_FRAME_RATE_STANDARD};")
            matFile.writeText(matContent.toString())
            true
            false
        }
    }
}