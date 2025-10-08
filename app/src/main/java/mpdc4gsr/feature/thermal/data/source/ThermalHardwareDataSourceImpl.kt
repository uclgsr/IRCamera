package mpdc4gsr.feature.thermal.data.source

import android.content.Context
import android.graphics.Bitmap
import android.hardware.usb.UsbDevice
import com.energy.iruvc.ircmd.ConcreteIRCMDBuilder
import com.energy.iruvc.ircmd.IRCMD
import com.energy.iruvc.ircmd.IRCMDType
import com.energy.iruvc.sdkisp.LibIRProcess
import com.energy.iruvc.sdkisp.LibIRTemp
import com.energy.iruvc.usb.USBMonitor
import com.energy.iruvc.utils.CommonParams
import com.energy.iruvc.utils.IFrameCallback
import com.energy.iruvc.utils.SynchronizedBitmap
import com.energy.iruvc.uvc.ConcreateUVCBuilder
import com.energy.iruvc.uvc.UVCCamera
import com.energy.iruvc.uvc.UVCType
import com.mpdc4gsr.libunified.ir.extension.*
import mpdc4gsr.feature.thermal.data.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileOutputStream

class ThermalHardwareDataSourceImpl(
    private val context: Context
) : ThermalHardwareDataSource {
    companion object {
        private const val TAG = "TopdonDataSourceImpl"
        private const val CAMERA_WIDTH = 256
        private const val CAMERA_HEIGHT = 192
        private const val DEFAULT_TEMP = 25.0f
        private const val MIN_TEMP_RANGE = -20.0f
        private const val MAX_TEMP_RANGE = 400.0f
        private const val FRAME_BUFFER_SIZE = 256 * 192 * 2
        private const val FRAME_RECEIVE_TIMEOUT_MS = 1000L
    }

    private var isConnected = false
    private var isStreaming = false
    private var isRecording = false
    private var usbMonitor: USBMonitor? = null
    private var uvcCamera: UVCCamera? = null
    private var ircmd: IRCMD? = null
    private var irTemp: LibIRTemp? = null
    private val syncBitmap = SynchronizedBitmap()
    private var currentMinTemp = MIN_TEMP_RANGE
    private var currentMaxTemp = MAX_TEMP_RANGE
    private val imageBuffer = ByteArray(FRAME_BUFFER_SIZE)
    private val temperatureBuffer = ByteArray(FRAME_BUFFER_SIZE)
    private val rgbBuffer = ByteArray(CAMERA_WIDTH * CAMERA_HEIGHT * 4)
    private var recordingFile: File? = null
    private var recordingOutputStream: FileOutputStream? = null
    private var frameCallback: IFrameCallback? = null
    private var connectionDeferred: kotlinx.coroutines.CompletableDeferred<Result<Unit>>? = null
    override suspend fun connectDevice(): Result<Unit> {
        return try {
            connectionDeferred = CompletableDeferred()
            if (usbMonitor == null) {
                usbMonitor = USBMonitor(context, object : USBMonitor.OnDeviceConnectListener {
                    override fun onAttach(device: UsbDevice?) {
                        device?.let {
                            usbMonitor?.requestPermission(it)
                        }
                    }

                    override fun onGranted(usbDevice: UsbDevice?, granted: Boolean) {
                        if (granted && usbDevice != null) {
                        } else {
                            connectionDeferred?.complete(Result.failure(Exception("USB permission denied")))
                        }
                    }

                    override fun onConnect(
                        device: UsbDevice?,
                        ctrlBlock: USBMonitor.UsbControlBlock?,
                        createNew: Boolean
                    ) {
                        ctrlBlock?.let {
                            val result = openCamera(it)
                            if (result) {
                                isConnected = true
                                connectionDeferred?.complete(Result.success(Unit))
                            } else {
                                connectionDeferred?.complete(Result.failure(Exception("Failed to open camera")))
                            }
                        }
                    }

                    override fun onDisconnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
                        isConnected = false
                    }

                    override fun onDettach(device: UsbDevice?) {
                        isConnected = false
                    }

                    override fun onCancel(device: UsbDevice?) {
                        connectionDeferred?.complete(Result.failure(Exception("USB connection cancelled")))
                    }
                })
                usbMonitor?.register()
            }
            if (uvcCamera == null) {
                uvcCamera = ConcreateUVCBuilder()
                    .setUVCType(UVCType.USB_UVC)
                    .build()
            }
            val timeoutResult = withTimeoutOrNull(10000) {
                connectionDeferred?.await()
            }
            timeoutResult ?: Result.failure(Exception("Connection timeout"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun openCamera(ctrlBlock: USBMonitor.UsbControlBlock): Boolean {
        return try {
            uvcCamera?.let { camera ->
                val result = camera.openUVCCamera(ctrlBlock)
                if (result == 0) {
                    initializeIRCMD()
                    initializeLibIRTemp()
                    true
                } else {
                    false
                }
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun initializeIRCMD() {
        try {
            uvcCamera?.let { camera ->
                ircmd = ConcreteIRCMDBuilder()
                    .setIrcmdType(IRCMDType.USB_IR_256_384)
                    .setIdCamera(camera.nativePtr)
                    .build()
            }
        } catch (e: Exception) {
        }
    }

    private fun initializeLibIRTemp() {
        try {
            irTemp = LibIRTemp()
        } catch (e: Exception) {
        }
    }

    override suspend fun disconnectDevice() {
        try {
            if (isRecording) {
                stopRecording()
            }
            if (isStreaming) {
                stopStreaming()
            }
            ircmd = null
            uvcCamera?.closeUVCCamera()
            uvcCamera = null
            usbMonitor?.unregister()
            usbMonitor?.destroy()
            usbMonitor = null
            irTemp = null
            isConnected = false
        } catch (e: Exception) {
        }
    }

    override suspend fun startStreaming(): Flow<ThermalFrameData> {
        return flow {
            if (!isConnected) {
                throw IllegalStateException("Camera not connected")
            }
            val frameChannel = Channel<ThermalFrameData>(Channel.BUFFERED)
            frameCallback = IFrameCallback { frame ->
                try {
                    if (frame != null && frame.size >= FRAME_BUFFER_SIZE) {
                        System.arraycopy(frame, 0, imageBuffer, 0, minOf(FRAME_BUFFER_SIZE, frame.size))
                        val processedData = processFrame(frame)
                        if (processedData != null) {
                            val thermalFrame = createThermalFrameData(processedData)
                            val sendResult = frameChannel.trySend(thermalFrame)
                            if (sendResult.isFailure) {
                            }
                        }
                    }
                } catch (e: Exception) {
                }
            }
            uvcCamera?.setFrameCallback(frameCallback)
            isStreaming = true
            try {
                while (isStreaming) {
                    val frame = withTimeoutOrNull(FRAME_RECEIVE_TIMEOUT_MS) {
                        frameChannel.receive()
                    }
                    if (frame != null) {
                        emit(frame)
                    }
                }
            } finally {
                frameChannel.close()
            }
        }
    }

    private fun processFrame(frame: ByteArray): ByteArray? {
        return try {
            val imageRes = LibIRProcess.ImageRes_t().apply {
                width = CAMERA_WIDTH.toChar()
                height = CAMERA_HEIGHT.toChar()
            }
            LibIRProcess.convertYuyvMapToARGBPseudocolor(
                frame,
                (CAMERA_WIDTH * CAMERA_HEIGHT).toLong(),
                CommonParams.PseudoColorType.PSEUDO_1,
                rgbBuffer
            )
            rgbBuffer.copyOf()
        } catch (e: Exception) {
            null
        }
    }

    private fun createThermalFrameData(processedData: ByteArray): ThermalFrameData {
        val temperatureMatrix = Array(CAMERA_HEIGHT) { FloatArray(CAMERA_WIDTH) }
        var minTemp = MIN_TEMP_RANGE
        var maxTemp = MAX_TEMP_RANGE
        var centerTemp = DEFAULT_TEMP
        try {
            irTemp?.let { temp ->
                val fullRect = android.graphics.Rect(0, 0, CAMERA_WIDTH - 1, CAMERA_HEIGHT - 1)
                val fullResult = temp.getTemperatureOfRect(fullRect)
                if (fullResult != null) {
                    minTemp = fullResult.minTemperature
                    maxTemp = fullResult.maxTemperature
                }
                val centerResult = temp.getTemperatureOfPoint(
                    android.graphics.Point(CAMERA_WIDTH / 2, CAMERA_HEIGHT / 2)
                )
                centerTemp = centerResult?.maxTemperature ?: DEFAULT_TEMP
                for (y in 0 until CAMERA_HEIGHT) {
                    for (x in 0 until CAMERA_WIDTH) {
                        val result = temp.getTemperatureOfPoint(android.graphics.Point(x, y))
                        temperatureMatrix[y][x] = result?.maxTemperature ?: DEFAULT_TEMP
                    }
                }
            }
        } catch (e: Exception) {
        }
        val bitmap = createBitmapFromFrame(processedData)
        return ThermalFrameData(
            timestamp = System.currentTimeMillis(),
            bitmap = bitmap,
            temperatureMatrix = temperatureMatrix,
            minTemp = minTemp,
            maxTemp = maxTemp,
            centerTemp = centerTemp
        )
    }

    private fun createBitmapFromFrame(data: ByteArray): Bitmap {
        return try {
            val bitmap = Bitmap.createBitmap(CAMERA_WIDTH, CAMERA_HEIGHT, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(java.nio.ByteBuffer.wrap(data))
            bitmap
        } catch (e: Exception) {
            Bitmap.createBitmap(CAMERA_WIDTH, CAMERA_HEIGHT, Bitmap.Config.ARGB_8888)
        }
    }

    override suspend fun stopStreaming() {
        try {
            uvcCamera?.setFrameCallback(null)
            frameCallback = null
            isStreaming = false
        } catch (e: Exception) {
        }
    }

    override suspend fun captureSnapshot(): Result<ThermalSnapshot> {
        return try {
            if (!isConnected) {
                return Result.failure(IllegalStateException("Camera not connected"))
            }
            val frameData = imageBuffer.copyOf()
            val processedData = processFrame(frameData)
            if (processedData == null) {
                return Result.failure(Exception("Failed to process frame"))
            }
            val temperatureMatrix = Array(CAMERA_HEIGHT) { FloatArray(CAMERA_WIDTH) }
            var minTemp = Float.MAX_VALUE
            var maxTemp = Float.MIN_VALUE
            irTemp?.let { temp ->
                for (y in 0 until CAMERA_HEIGHT) {
                    for (x in 0 until CAMERA_WIDTH) {
                        val result = temp.getTemperatureOfPoint(android.graphics.Point(x, y))
                        val temperature = result?.maxTemperature ?: DEFAULT_TEMP
                        temperatureMatrix[y][x] = temperature
                        if (temperature < minTemp) minTemp = temperature
                        if (temperature > maxTemp) maxTemp = temperature
                    }
                }
            }
            val bitmap = createBitmapFromFrame(processedData)
            val snapshot = ThermalSnapshot(
                bitmap = bitmap,
                temperatureMatrix = temperatureMatrix,
                minTemp = minTemp,
                maxTemp = maxTemp,
                timestamp = System.currentTimeMillis(),
                location = null
            )
            Result.success(snapshot)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun startRecording(): Result<Unit> {
        return try {
            if (!isConnected) {
                return Result.failure(IllegalStateException("Camera not connected"))
            }
            val recordingDir = File(context.filesDir, "thermal_recordings")
            recordingDir.mkdirs()
            recordingFile = File(recordingDir, "thermal_${System.currentTimeMillis()}.bin")
            recordingOutputStream = FileOutputStream(recordingFile)
            isRecording = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun stopRecording(): Result<String> {
        return try {
            isRecording = false
            recordingOutputStream?.flush()
            recordingOutputStream?.close()
            recordingOutputStream = null
            val filePath = recordingFile?.absolutePath ?: ""
            Result.success(filePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isConnected(): Boolean {
        return isConnected
    }

    override suspend fun setTemperatureRange(min: Float, max: Float): Result<Unit> {
        return try {
            if (!isConnected) {
                return Result.failure(IllegalStateException("Camera not connected"))
            }
            currentMinTemp = min
            currentMaxTemp = max
            ircmd?.setManualAgcMin(min)
            ircmd?.setManualAgcMax(max)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setColorPalette(palette: ColorPalette): Result<Unit> {
        return try {
            if (!isConnected) {
                return Result.failure(IllegalStateException("Camera not connected"))
            }
            ircmd?.setColorPalette(palette) ?: return Result.failure(Exception("IRCMD not initialized"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setAgcMode(mode: AgcMode): Result<Unit> {
        return try {
            if (!isConnected) {
                return Result.failure(IllegalStateException("Camera not connected"))
            }
            ircmd?.setAgcMode(mode) ?: return Result.failure(Exception("IRCMD not initialized"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setEmissivity(value: Float): Result<Unit> {
        return try {
            if (!isConnected) {
                return Result.failure(IllegalStateException("Camera not connected"))
            }
            ircmd?.setEmissivity(value) ?: return Result.failure(Exception("IRCMD not initialized"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setMeasurementDistance(meters: Float): Result<Unit> {
        return try {
            if (!isConnected) {
                return Result.failure(IllegalStateException("Camera not connected"))
            }
            ircmd?.setDistance(meters) ?: return Result.failure(Exception("IRCMD not initialized"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setReflectedTemperature(tempCelsius: Float): Result<Unit> {
        return try {
            if (!isConnected) {
                return Result.failure(IllegalStateException("Camera not connected"))
            }
            ircmd?.setReflectedTemperature(tempCelsius) ?: return Result.failure(Exception("IRCMD not initialized"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMeasurementForArea(area: MeasurementArea): Result<MeasurementResult> {
        return try {
            if (!isConnected) {
                return Result.failure(IllegalStateException("Camera not connected"))
            }
            irTemp?.let { temp ->
                val result = when (area) {
                    is MeasurementArea.PointArea -> {
                        val tempResult = temp.getTemperatureOfPoint(area.point)
                        tempResult?.let {
                            MeasurementResult(
                                minTemp = it.minTemperature,
                                maxTemp = it.maxTemperature,
                                avgTemp = (it.minTemperature + it.maxTemperature) / 2,
                                area = area
                            )
                        }
                    }
                    is MeasurementArea.RectangleArea -> {
                        val tempResult = temp.getTemperatureOfRect(area.rect)
                        tempResult?.let {
                            MeasurementResult(
                                minTemp = it.minTemperature,
                                maxTemp = it.maxTemperature,
                                avgTemp = (it.minTemperature + it.maxTemperature) / 2,
                                area = area
                            )
                        }
                    }
                    is MeasurementArea.LineArea -> {
                        val startResult = temp.getTemperatureOfPoint(area.start)
                        val endResult = temp.getTemperatureOfPoint(area.end)
                        if (startResult != null && endResult != null) {
                            val minT = minOf(startResult.minTemperature, endResult.minTemperature)
                            val maxT = maxOf(startResult.maxTemperature, endResult.maxTemperature)
                            MeasurementResult(
                                minTemp = minT,
                                maxTemp = maxT,
                                avgTemp = (minT + maxT) / 2,
                                area = area
                            )
                        } else null
                    }
                    is MeasurementArea.EllipseArea -> {
                        // TODO: Implement ellipse area measurement
                        // SDK currently does not provide native ellipse measurement
                        // Approximate using bounding rectangle for now
                        val tempResult = temp.getTemperatureOfRect(area.boundingRect)
                        tempResult?.let {
                            MeasurementResult(
                                minTemp = it.minTemperature,
                                maxTemp = it.maxTemperature,
                                avgTemp = (it.minTemperature + it.maxTemperature) / 2,
                                area = area
                            )
                        }
                    }
                    is MeasurementArea.PolygonArea -> {
                        // TODO: Implement polygon area measurement
                        // SDK currently does not provide native polygon measurement
                        // Approximate using bounding rectangle for now
                        val tempResult = temp.getTemperatureOfRect(area.boundingRect)
                        tempResult?.let {
                            MeasurementResult(
                                minTemp = it.minTemperature,
                                maxTemp = it.maxTemperature,
                                avgTemp = (it.minTemperature + it.maxTemperature) / 2,
                                area = area
                            )
                        }
                    }
                }
                result?.let { Result.success(it) } ?: Result.failure(Exception("Measurement failed"))
            } ?: Result.failure(Exception("LibIRTemp not initialized"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun applyCalibration(calibrationData: ThermalCalibrationData): Result<Unit> {
        return try {
            if (!isConnected) {
                return Result.failure(IllegalStateException("Camera not connected"))
            }
            ircmd?.let { cmd ->
                cmd.setEmissivity(calibrationData.emissivity)
                cmd.setDistance(calibrationData.distance)
                cmd.setReflectedTemperature(calibrationData.reflectedTemperature)
            } ?: return Result.failure(Exception("IRCMD not initialized"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun performFFC(): Result<Unit> {
        return try {
            if (!isConnected) {
                return Result.failure(IllegalStateException("Camera not connected"))
            }
            ircmd?.performFFC() ?: return Result.failure(Exception("IRCMD not initialized"))
            // TODO: Replace fixed delay with SDK callback/status check when available
            // Current approach waits for FFC operation to complete based on typical hardware timing
            delay(FFC_CALIBRATION_DELAY_MS)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun performNUC(): Result<Unit> {
        return try {
            if (!isConnected) {
                return Result.failure(IllegalStateException("Camera not connected"))
            }
            ircmd?.performNUC() ?: return Result.failure(Exception("IRCMD not initialized"))
            // TODO: Replace fixed delay with SDK callback/status check when available
            // Current approach waits for NUC operation to complete based on typical hardware timing
            delay(NUC_CALIBRATION_DELAY_MS)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun enableISP(enabled: Boolean): Result<Unit> {
        return try {
            if (!isConnected) {
                return Result.failure(IllegalStateException("Camera not connected"))
            }
            ircmd?.enableISP(enabled) ?: return Result.failure(Exception("IRCMD not initialized"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setTNRLevel(level: Int): Result<Unit> {
        return try {
            if (!isConnected) {
                return Result.failure(IllegalStateException("Camera not connected"))
            }
            ircmd?.setTNRLevel(level) ?: return Result.failure(Exception("IRCMD not initialized"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setBrightness(level: Int): Result<Unit> {
        return try {
            if (!isConnected) {
                return Result.failure(IllegalStateException("Camera not connected"))
            }
            ircmd?.setBrightness(level) ?: return Result.failure(Exception("IRCMD not initialized"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setContrast(level: Int): Result<Unit> {
        return try {
            if (!isConnected) {
                return Result.failure(IllegalStateException("Camera not connected"))
            }
            ircmd?.setContrast(level) ?: return Result.failure(Exception("IRCMD not initialized"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setSharpness(level: Int): Result<Unit> {
        return try {
            if (!isConnected) {
                return Result.failure(IllegalStateException("Camera not connected"))
            }
            ircmd?.setSharpness(level) ?: return Result.failure(Exception("IRCMD not initialized"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDeviceInfo(): Result<DeviceInfo> {
        return try {
            if (!isConnected) {
                return Result.failure(IllegalStateException("Camera not connected"))
            }
            // TODO: Replace hardcoded values with actual SDK calls when API becomes available
            // Current SDK does not expose device info query methods
            val deviceInfo = DeviceInfo(
                model = "TC001",
                serialNumber = "UNKNOWN",  // TODO: Fetch from SDK
                firmwareVersion = "1.0.0",  // TODO: Fetch from SDK
                sdkVersion = "1.1.1",  // TODO: Fetch from SDK
                resolution = Pair(CAMERA_WIDTH, CAMERA_HEIGHT),
                frameRate = 9.0f,
                temperatureRange = Pair(MIN_TEMP_RANGE, MAX_TEMP_RANGE)
            )
            Result.success(deviceInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getBatteryStatus(): Result<BatteryStatus> {
        return try {
            val batteryStatus = BatteryStatus(
                level = 100,
                isCharging = false,
                voltage = 3.7f
            )
            Result.success(batteryStatus)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun configureCameraSettings(
        enableMirror: Boolean = false,
        enableAutoShutter: Boolean = true,
        ddeLevel: Int = 128,
        contrastLevel: Int = 128
    ): Result<Unit> {
        return try {
            if (!isConnected) {
                return Result.failure(IllegalStateException("Camera not connected"))
            }
            ircmd?.let { cmd ->
                cmd.setMirror(enableMirror)
                cmd.setAutoShutter(enableAutoShutter)
                cmd.setPropDdeLevel(ddeLevel)
                cmd.setContrast(contrastLevel)
            } ?: return Result.failure(Exception("IRCMD not initialized"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
