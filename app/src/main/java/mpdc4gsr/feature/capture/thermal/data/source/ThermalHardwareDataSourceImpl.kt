package mpdc4gsr.feature.capture.thermal.data.source

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
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
import com.mpdc4gsr.component.shared.ir.extension.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull
import mpdc4gsr.core.data.utils.TimeManager
import mpdc4gsr.feature.capture.thermal.data.*
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min
import java.io.File
import java.io.FileOutputStream
import java.io.BufferedOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ThermalHardwareDataSourceImpl(
    private val context: Context,
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
        private const val FFC_CALIBRATION_DELAY_MS = 1500L
        private const val NUC_CALIBRATION_DELAY_MS = 1500L
        private const val FRAME_MAGIC = 0x54434652 // 'TCFR'
        private const val FRAME_VERSION = 1
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
    private val timeManager = TimeManager.getInstance(context)
    private var recordingFile: File? = null
    private var recordingOutputStream: BufferedOutputStream? = null
    private var recordingStartTimestampMs: Long = 0L
    private var recordedFrames: Long = 0L
    private var sessionMinRecordedTemp: Float = Float.POSITIVE_INFINITY
    private var sessionMaxRecordedTemp: Float = Float.NEGATIVE_INFINITY
    private var lastRecordingPath: String? = null
    private var frameCallback: IFrameCallback? = null
    private var connectionDeferred: kotlinx.coroutines.CompletableDeferred<Result<Unit>>? = null

    override suspend fun connectDevice(): Result<Unit> =
        try {
            connectionDeferred = CompletableDeferred()
            if (usbMonitor == null) {
                usbMonitor =
                    USBMonitor(
                        context,
                        object : USBMonitor.OnDeviceConnectListener {
                            override fun onAttach(device: UsbDevice?) {
                                device?.let {
                                    usbMonitor?.requestPermission(it)
                                }
                            }

                            override fun onGranted(
                                usbDevice: UsbDevice?,
                                granted: Boolean,
                            ) {
                                if (granted && usbDevice != null) {
                                } else {
                                    connectionDeferred?.complete(Result.failure(Exception("USB permission denied")))
                                }
                            }

                            override fun onConnect(
                                device: UsbDevice?,
                                ctrlBlock: USBMonitor.UsbControlBlock?,
                                createNew: Boolean,
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

                            override fun onDisconnect(
                                device: UsbDevice?,
                                ctrlBlock: USBMonitor.UsbControlBlock?,
                            ) {
                                isConnected = false
                            }

                            override fun onDettach(device: UsbDevice?) {
                                isConnected = false
                            }

                            override fun onCancel(device: UsbDevice?) {
                                connectionDeferred?.complete(Result.failure(Exception("USB connection cancelled")))
                            }
                        },
                    )
                usbMonitor?.register()
            }
            if (uvcCamera == null) {
                uvcCamera =
                    ConcreateUVCBuilder()
                        .setUVCType(UVCType.USB_UVC)
                        .build()
            }
            val timeoutResult =
                withTimeoutOrNull(10000) {
                    connectionDeferred?.await()
                }
            timeoutResult ?: Result.failure(Exception("Connection timeout"))
        } catch (e: Exception) {
            Result.failure(e)
        }

    private fun openCamera(ctrlBlock: USBMonitor.UsbControlBlock): Boolean =
        try {
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

    private fun initializeIRCMD() {
        try {
            uvcCamera?.let { camera ->
                ircmd =
                    ConcreteIRCMDBuilder()
                        .setIrcmdType(IRCMDType.USB_IR_256_384)
                        .setIdCamera(camera.nativePtr)
                        .build()
            }
        } catch (e: Exception) {
            mpdc4gsr.core.common.AppLogger.e(
                "ThermalHardwareDataSourceImpl",
                "Unexpected Exception in ThermalHardwareDataSourceImpl catch block",
                e,
            )
        }
    }

    private fun initializeLibIRTemp() {
        try {
            irTemp = LibIRTemp()
        } catch (e: Exception) {
            mpdc4gsr.core.common.AppLogger.e(
                "ThermalHardwareDataSourceImpl",
                "Unexpected Exception in ThermalHardwareDataSourceImpl catch block",
                e,
            )
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
            mpdc4gsr.core.common.AppLogger.e(
                "ThermalHardwareDataSourceImpl",
                "Unexpected Exception in ThermalHardwareDataSourceImpl catch block",
                e,
            )
        }
    }

    override suspend fun startStreaming(): Flow<ThermalFrameData> =
        flow {
            if (!isConnected) {
                throw IllegalStateException("Camera not connected")
            }
            val frameChannel = Channel<ThermalFrameData>(Channel.BUFFERED)
            frameCallback =
                IFrameCallback { frame ->
                    try {
                        if (frame != null && frame.size >= FRAME_BUFFER_SIZE) {
                            System.arraycopy(frame, 0, imageBuffer, 0, minOf(FRAME_BUFFER_SIZE, frame.size))
                            val processedData = processFrame(frame)
                            if (processedData != null) {
                                val thermalFrame = createThermalFrameData(processedData)
                                if (isRecording) {
                                    writeFrameToRecording(thermalFrame)
                                }
                                val sendResult = frameChannel.trySend(thermalFrame)
                                if (sendResult.isFailure) {
                                }
                            }
                        }
                    } catch (e: Exception) {
                        mpdc4gsr.core.common.AppLogger.e(
                            "ThermalHardwareDataSourceImpl",
                            "Unexpected Exception in ThermalHardwareDataSourceImpl catch block",
                            e,
                        )
                    }
                }
            uvcCamera?.setFrameCallback(frameCallback)
            isStreaming = true
            try {
                while (isStreaming) {
                    val frame =
                        withTimeoutOrNull(FRAME_RECEIVE_TIMEOUT_MS) {
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

    private fun processFrame(frame: ByteArray): ByteArray? =
        try {
            val imageRes =
                LibIRProcess.ImageRes_t().apply {
                    width = CAMERA_WIDTH.toChar()
                    height = CAMERA_HEIGHT.toChar()
                }
            LibIRProcess.convertYuyvMapToARGBPseudocolor(
                frame,
                (CAMERA_WIDTH * CAMERA_HEIGHT).toLong(),
                CommonParams.PseudoColorType.PSEUDO_1,
                rgbBuffer,
            )
            rgbBuffer.copyOf()
        } catch (e: Exception) {
            null
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
                val centerResult =
                    temp.getTemperatureOfPoint(
                        android.graphics.Point(CAMERA_WIDTH / 2, CAMERA_HEIGHT / 2),
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
            mpdc4gsr.core.common.AppLogger.e(
                "ThermalHardwareDataSourceImpl",
                "Unexpected Exception in ThermalHardwareDataSourceImpl catch block",
                e,
            )
        }
        val bitmap = createBitmapFromFrame(processedData)
        return ThermalFrameData(
            timestamp = timeManager.getCurrentTimestampMs(),
            bitmap = bitmap,
            temperatureMatrix = temperatureMatrix,
            minTemp = minTemp,
            maxTemp = maxTemp,
            centerTemp = centerTemp,
        )
    }

    private fun createBitmapFromFrame(data: ByteArray): Bitmap =
        try {
            val bitmap = Bitmap.createBitmap(CAMERA_WIDTH, CAMERA_HEIGHT, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(java.nio.ByteBuffer.wrap(data))
            bitmap
        } catch (e: Exception) {
            Bitmap.createBitmap(CAMERA_WIDTH, CAMERA_HEIGHT, Bitmap.Config.ARGB_8888)
        }

    override suspend fun stopStreaming() {
        try {
            uvcCamera?.setFrameCallback(null)
            frameCallback = null
            isStreaming = false
        } catch (e: Exception) {
            mpdc4gsr.core.common.AppLogger.e(
                "ThermalHardwareDataSourceImpl",
                "Unexpected Exception in ThermalHardwareDataSourceImpl catch block",
                e,
            )
        }
    }

    private fun writeFrameToRecording(frame: ThermalFrameData) {
        val output = recordingOutputStream ?: return
        try {
            val bitmap = frame.bitmap
            val pixelCount = bitmap.width * bitmap.height
            val bitmapBytes = ByteArray(pixelCount * 4)
            val pixelBuffer = ByteBuffer.wrap(bitmapBytes)
            bitmap.copyPixelsToBuffer(pixelBuffer)

            val temperatureMatrix = frame.temperatureMatrix
            val rowCount = temperatureMatrix.size
            val columnCount = temperatureMatrix.firstOrNull()?.size ?: 0
            val matrixSize = rowCount * columnCount
            val matrixBytes =
                if (matrixSize > 0) {
                    val buffer = ByteBuffer.allocate(matrixSize * 4).order(ByteOrder.LITTLE_ENDIAN)
                    temperatureMatrix.forEach { row ->
                        row.forEach { value ->
                            buffer.putFloat(value)
                        }
                    }
                    buffer.array()
                } else {
                    ByteArray(0)
                }

    private fun writeRecordingManifest(recordingFile: File, endTimestampMs: Long) {
        try {
            val manifest = JSONObject().apply {
                put("file", recordingFile.name)
                put("absolute_path", recordingFile.absolutePath)
                put("created_at_ms", recordingStartTimestampMs)
                put("ended_at_ms", endTimestampMs)
                put("duration_ms", (endTimestampMs - recordingStartTimestampMs).coerceAtLeast(0))
                put("frames_recorded", recordedFrames)
                put("mode", if (isSimulationMode()) "simulation" else "hardware")
                if (sessionMinRecordedTemp != Float.MAX_VALUE) {
                    put("min_temp_observed", sessionMinRecordedTemp.toDouble())
                }
                if (sessionMaxRecordedTemp != Float.MIN_VALUE) {
                    put("max_temp_observed", sessionMaxRecordedTemp.toDouble())
                }
            }
            val manifestFile = File(
                recordingFile.parentFile ?: context.filesDir,
                "${recordingFile.nameWithoutExtension}_manifest.json",
            )
            manifestFile.writeText(manifest.toString(2))
        } catch (e: Exception) {
            mpdc4gsr.core.common.AppLogger.e(
                TAG,
                "Failed to write thermal recording manifest",
                e,
            )
        }
    }

            val header =
                ByteBuffer
                    .allocate(4 + 4 + 8 + 4 * 3 + 4 * 2 + 4 + 4)
                    .order(ByteOrder.LITTLE_ENDIAN)
            header.putInt(FRAME_MAGIC)
            header.putInt(FRAME_VERSION)
            header.putLong(frame.timestamp)
            header.putFloat(frame.minTemp)
            header.putFloat(frame.maxTemp)
            header.putFloat(frame.centerTemp)
            header.putInt(bitmap.width)
            header.putInt(bitmap.height)
            header.putInt(bitmapBytes.size)
            header.putInt(matrixSize)
            output.write(header.array())
            output.write(bitmapBytes)
            if (matrixBytes.isNotEmpty()) {
                output.write(matrixBytes)
            }
            recordedFrames += 1
            if (!frame.minTemp.isNaN()) {
                sessionMinRecordedTemp = min(sessionMinRecordedTemp, frame.minTemp)
            }
            if (!frame.maxTemp.isNaN()) {
                sessionMaxRecordedTemp = max(sessionMaxRecordedTemp, frame.maxTemp)
            }
        } catch (e: Exception) {
            mpdc4gsr.core.common.AppLogger.e(
                TAG,
                "Failed to persist thermal frame",
                e,
            )
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
            val snapshot =
                ThermalSnapshot(
                    bitmap = bitmap,
                    temperatureMatrix = temperatureMatrix,
                    minTemp = minTemp,
                    maxTemp = maxTemp,
                    timestamp = timeManager.getCurrentTimestampMs(),
                    location = null,
                )
            Result.success(snapshot)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun MeasurementArea.EllipseArea.toBoundingRect(): Rect {
        val left = (centerX - radiusX).coerceAtLeast(0)
        val top = (centerY - radiusY).coerceAtLeast(0)
        val rightCandidate = (centerX + radiusX).coerceAtMost(CAMERA_WIDTH - 1)
        val bottomCandidate = (centerY + radiusY).coerceAtMost(CAMERA_HEIGHT - 1)
        val right = ensureMinSize(left, rightCandidate, CAMERA_WIDTH)
        val bottom = ensureMinSize(top, bottomCandidate, CAMERA_HEIGHT)
        return Rect(left, top, right, bottom)
    }

    private fun MeasurementArea.PolygonArea.toBoundingRect(): Rect {
        if (points.isEmpty()) {
            return Rect(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT)
        }

        val minX = points.minOf { it.x }.coerceAtLeast(0)
        val minY = points.minOf { it.y }.coerceAtLeast(0)
        val maxX = points.maxOf { it.x }.coerceAtMost(CAMERA_WIDTH - 1)
        val maxY = points.maxOf { it.y }.coerceAtMost(CAMERA_HEIGHT - 1)
        val right = ensureMinSize(minX, maxX, CAMERA_WIDTH)
        val bottom = ensureMinSize(minY, maxY, CAMERA_HEIGHT)
        return Rect(minX, minY, right, bottom)
    }

    private fun ensureMinSize(start: Int, end: Int, maxExclusive: Int): Int {
        val clampedEnd = end.coerceAtMost(maxExclusive)
        return if (clampedEnd <= start) {
            (start + 1).coerceAtMost(maxExclusive)
        } else {
            clampedEnd
        }
    }

    private fun LibIRTemp.TemperatureSampleResult.toMeasurementResult(area: MeasurementArea): MeasurementResult {
        val min = minTemperature.toFloat()
        val max = maxTemperature.toFloat()
        return MeasurementResult(
            minTemp = min,
            maxTemp = max,
            avgTemp = (min + max) / 2f,
            area = area,
        )
    }

    override suspend fun startRecording(): Result<Unit> {
        return try {
            if (!isConnected) {
                return Result.failure(IllegalStateException("Camera not connected"))
            }
            val recordingDir = File(context.filesDir, "thermal_recordings")
            recordingDir.mkdirs()
            recordingStartTimestampMs = timeManager.getCurrentTimestampMs()
            recordedFrames = 0L
            sessionMinRecordedTemp = Float.POSITIVE_INFINITY
            sessionMaxRecordedTemp = Float.NEGATIVE_INFINITY
            lastRecordingPath = null
            recordingFile = File(recordingDir, "thermal_${recordingStartTimestampMs}.tcf")
            recordingOutputStream = BufferedOutputStream(FileOutputStream(recordingFile))
            isRecording = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun stopRecording(): Result<String> =
        try {
            isRecording = false
            recordingOutputStream?.flush()
            recordingOutputStream?.close()
            recordingOutputStream = null
            val file = recordingFile
            val filePath = file?.absolutePath ?: ""
            if (file != null) {
                val endTimestamp = timeManager.getCurrentTimestampMs()
                writeRecordingManifest(file, endTimestamp)
                lastRecordingPath = file.absolutePath
            }
            recordingFile = null
            Result.success(filePath)
        } catch (e: Exception) {
            Result.failure(e)
        }

    override fun isConnected(): Boolean = isConnected

    override fun isSimulationMode(): Boolean = false

    override fun getLastRecordingPath(): String? = lastRecordingPath

    override suspend fun setTemperatureRange(
        min: Float,
        max: Float,
    ): Result<Unit> {
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
                val result =
                    when (area) {
                        is MeasurementArea.PointArea -> {
                            val tempResult: LibIRTemp.TemperatureSampleResult? =
                                temp.getTemperatureOfPoint(area.point)
                            tempResult?.toMeasurementResult(area)
                        }

                        is MeasurementArea.RectangleArea -> {
                            val tempResult: LibIRTemp.TemperatureSampleResult? =
                                temp.getTemperatureOfRect(area.rect)
                            tempResult?.toMeasurementResult(area)
                        }

                        is MeasurementArea.LineArea -> {
                            val startResult: LibIRTemp.TemperatureSampleResult? =
                                temp.getTemperatureOfPoint(area.start)
                            val endResult: LibIRTemp.TemperatureSampleResult? =
                                temp.getTemperatureOfPoint(area.end)
                            if (startResult != null && endResult != null) {
                                val minT =
                                    min(
                                        startResult.minTemperature.toFloat(),
                                        endResult.minTemperature.toFloat(),
                                    )
                                val maxT =
                                    max(
                                        startResult.maxTemperature.toFloat(),
                                        endResult.maxTemperature.toFloat(),
                                    )
                                MeasurementResult(
                                    minTemp = minT,
                                    maxTemp = maxT,
                                    avgTemp = (minT + maxT) / 2f,
                                    area = area,
                                )
                            } else {
                                null
                            }
                        }

                        is MeasurementArea.EllipseArea -> {
                            val rect = area.toBoundingRect()
                            val tempResult: LibIRTemp.TemperatureSampleResult? =
                                temp.getTemperatureOfRect(rect)
                            tempResult?.toMeasurementResult(area)
                        }

                        is MeasurementArea.PolygonArea -> {
                            val rect = area.toBoundingRect()
                            val tempResult: LibIRTemp.TemperatureSampleResult? =
                                temp.getTemperatureOfRect(rect)
                            tempResult?.toMeasurementResult(area)
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
            val deviceInfo =
                DeviceInfo(
                    model = "TC001",
                    serialNumber = "UNKNOWN", // TODO: Fetch from SDK
                    firmwareVersion = "1.0.0", // TODO: Fetch from SDK
                    sdkVersion = "1.1.1", // TODO: Fetch from SDK
                    resolution = Pair(CAMERA_WIDTH, CAMERA_HEIGHT),
                    frameRate = 9.0f,
                    temperatureRange = Pair(MIN_TEMP_RANGE, MAX_TEMP_RANGE),
                )
            Result.success(deviceInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getBatteryStatus(): Result<BatteryStatus> =
        try {
            val batteryStatus =
                BatteryStatus(
                    level = 100,
                    isCharging = false,
                    voltage = 3.7f,
                )
            Result.success(batteryStatus)
        } catch (e: Exception) {
            Result.failure(e)
        }

    fun configureCameraSettings(
        enableMirror: Boolean = false,
        enableAutoShutter: Boolean = true,
        ddeLevel: Int = 128,
        contrastLevel: Int = 128,
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






