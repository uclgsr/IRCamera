package mpdc4gsr.feature.capture.thermal.data.source

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import com.mpdc4gsr.libunified.ir.extension.AgcMode
import com.mpdc4gsr.libunified.ir.extension.ColorPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject
import mpdc4gsr.core.data.utils.TimeManager
import mpdc4gsr.feature.capture.thermal.data.BatteryStatus
import mpdc4gsr.feature.capture.thermal.data.DeviceInfo
import mpdc4gsr.feature.capture.thermal.data.MeasurementArea
import mpdc4gsr.feature.capture.thermal.data.MeasurementResult
import mpdc4gsr.feature.capture.thermal.data.ThermalCalibrationData
import java.io.File
import java.io.FileWriter
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Simulation fallback for the thermal hardware data source. Generates deterministic,
 * time-varying thermal frames so that the rest of the application can be exercised
 * without a physical Topdon camera attached.
 */
class ThermalSimulationDataSource(
    private val context: Context,
    private val width: Int = DEFAULT_WIDTH,
    private val height: Int = DEFAULT_HEIGHT,
    private val frameRateHz: Float = DEFAULT_FRAME_RATE_HZ,
) : ThermalHardwareDataSource {
    companion object {
        private const val DEFAULT_WIDTH = 256
        private const val DEFAULT_HEIGHT = 192
        private const val DEFAULT_FRAME_RATE_HZ = 12f
        private const val DEFAULT_MIN_TEMP = 24f
        private const val DEFAULT_MAX_TEMP = 37f
        private const val CSV_HEADER = "timestamp_ms,min_temp,max_temp,center_temp"
        private const val SIMULATION_MODEL = "TC001-SIM"
    }

    private val isConnected = AtomicBoolean(false)
    private val isStreaming = AtomicBoolean(false)
    private val isRecording = AtomicBoolean(false)
    private val timeManager = TimeManager.getInstance(context)
    private val random = Random(0x5A17BEEF)
    private val frameIntervalMs = (1_000f / frameRateHz).roundToInt().coerceAtLeast(1)
    private var currentMinTemp = DEFAULT_MIN_TEMP
    private var currentMaxTemp = DEFAULT_MAX_TEMP
    private var emissivity = 0.95f
    private var distanceMeters = 1.0f
    private var reflectedTemp = 20.0f
    private var latestFrame: ThermalFrameData? = null
    private var recordingFile: File? = null
    private var recordingWriter: FileWriter? = null
    private var recordingStartTimestampMs: Long = 0L
    private var recordedFrames: Long = 0L
    private var sessionMinTemp: Float = Float.POSITIVE_INFINITY
    private var sessionMaxTemp: Float = Float.NEGATIVE_INFINITY
    private var lastRecordingPath: String? = null

    override suspend fun connectDevice(): Result<Unit> {
        isConnected.set(true)
        return Result.success(Unit)
    }

    override suspend fun disconnectDevice() {
        stopStreaming()
        stopRecording()
        isConnected.set(false)
    }

    override suspend fun startStreaming(): Flow<ThermalFrameData> =
        flow {
            if (!isConnected.get()) {
                throw IllegalStateException("Simulation not connected")
            }
            if (!isStreaming.compareAndSet(false, true)) {
                throw IllegalStateException("Simulation stream already active")
            }
            try {
                while (isStreaming.get()) {
                    val frame = generateFrame()
                    latestFrame = frame
                    emit(frame)
                    if (isRecording.get()) {
                        writeFrameMetadata(frame)
                    }
                    delay(frameIntervalMs.toLong())
                }
            } finally {
                isStreaming.set(false)
            }
        }

    override suspend fun stopStreaming() {
        isStreaming.set(false)
    }

    override suspend fun captureSnapshot(): Result<ThermalSnapshot> {
        val frame = latestFrame ?: generateFrame().also { latestFrame = it }
        val bitmapCopy = frame.bitmap.copy(Bitmap.Config.ARGB_8888, false)
        val matrixCopy = frame.temperatureMatrix.map { row -> row.clone() }.toTypedArray()
        val snapshot =
            ThermalSnapshot(
                bitmap = bitmapCopy,
                temperatureMatrix = matrixCopy,
                minTemp = frame.minTemp,
                maxTemp = frame.maxTemp,
                timestamp = frame.timestamp,
            )
        return Result.success(snapshot)
    }

    override suspend fun startRecording(): Result<Unit> {
        if (!isConnected.get()) {
            return Result.failure(IllegalStateException("Simulation not connected"))
        }
        if (isRecording.get()) {
            return Result.success(Unit)
        }
        val directory = File(context.filesDir, "thermal_simulation")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val fileName = "thermal_sim_${timeManager.getCurrentTimestampMs()}.csv"
        val file = File(directory, fileName)
        recordingWriter = FileWriter(file).apply {
            write("$CSV_HEADER\n")
            flush()
        }
        recordingFile = file
        isRecording.set(true)
        return Result.success(Unit)
    }

    override suspend fun stopRecording(): Result<String> {
        if (!isRecording.getAndSet(false)) {
            return Result.success(recordingFile?.absolutePath ?: "")
        }
        try {
            recordingWriter?.flush()
            recordingWriter?.close()
        } catch (_: Exception) {
        } finally {
            recordingWriter = null
        }
        val path = recordingFile?.absolutePath ?: ""
        recordingFile = null
        return Result.success(path)
    }

    override fun isConnected(): Boolean = isConnected.get()

    override fun isSimulationMode(): Boolean = true

    override fun getLastRecordingPath(): String? = lastRecordingPath

    override suspend fun setTemperatureRange(
        min: Float,
        max: Float,
    ): Result<Unit> {
        currentMinTemp = min
        currentMaxTemp = if (max <= min) min + 1 else max
        return Result.success(Unit)
    }

    override suspend fun setColorPalette(palette: ColorPalette): Result<Unit> = Result.success(Unit)

    override suspend fun setAgcMode(mode: AgcMode): Result<Unit> = Result.success(Unit)

    override suspend fun setEmissivity(value: Float): Result<Unit> {
        emissivity = value
        return Result.success(Unit)
    }

    override suspend fun setMeasurementDistance(meters: Float): Result<Unit> {
        distanceMeters = meters
        return Result.success(Unit)
    }

    override suspend fun setReflectedTemperature(tempCelsius: Float): Result<Unit> {
        reflectedTemp = tempCelsius
        return Result.success(Unit)
    }

    override suspend fun getMeasurementForArea(area: MeasurementArea): Result<MeasurementResult> {
        val frame = latestFrame ?: generateFrame().also { latestFrame = it }
        val matrix = frame.temperatureMatrix
        var minTemp = Float.MAX_VALUE
        var maxTemp = Float.MIN_VALUE
        var sumTemp = 0.0
        var samples = 0

        iterateArea(area) { x, y ->
            val temp = matrix.getOrNull(y)?.getOrNull(x) ?: return@iterateArea
            samples++
            sumTemp += temp
            if (temp < minTemp) minTemp = temp
            if (temp > maxTemp) maxTemp = temp
        }

        if (samples == 0) {
            return Result.failure(IllegalArgumentException("No samples in measurement area"))
        }

        val result =
            MeasurementResult(
                minTemp = minTemp,
                maxTemp = maxTemp,
                avgTemp = (sumTemp / samples).toFloat(),
                area = area,
            )
        return Result.success(result)
    }

    override suspend fun applyCalibration(calibrationData: ThermalCalibrationData): Result<Unit> {
        emissivity = calibrationData.emissivity
        distanceMeters = calibrationData.distance
        reflectedTemp = calibrationData.reflectedTemperature
        return Result.success(Unit)
    }

    override suspend fun performFFC(): Result<Unit> = Result.success(Unit)

    override suspend fun performNUC(): Result<Unit> = Result.success(Unit)

    override suspend fun enableISP(enabled: Boolean): Result<Unit> = Result.success(Unit)

    override suspend fun setTNRLevel(level: Int): Result<Unit> = Result.success(Unit)

    override suspend fun setBrightness(level: Int): Result<Unit> = Result.success(Unit)

    override suspend fun setContrast(level: Int): Result<Unit> = Result.success(Unit)

    override suspend fun setSharpness(level: Int): Result<Unit> = Result.success(Unit)

    override suspend fun getDeviceInfo(): Result<DeviceInfo> =
        Result.success(
            DeviceInfo(
                model = SIMULATION_MODEL,
                serialNumber = "SIM-${android.os.Build.SERIAL ?: "0000"}",
                firmwareVersion = "sim-1.0",
                sdkVersion = "sim-1.0",
                resolution = width to height,
                frameRate = frameRateHz,
                temperatureRange = currentMinTemp to currentMaxTemp,
            ),
        )

    override suspend fun getBatteryStatus(): Result<BatteryStatus> =
        Result.success(
            BatteryStatus(
                level = 100,
                isCharging = true,
                voltage = 3.7f,
            ),
        )

    private fun generateFrame(): ThermalFrameData {
        val timestamp = timeManager.getCurrentTimestampMs()
        val matrix = Array(height) { FloatArray(width) }
        val pixels = IntArray(width * height)
        var minTemp = Float.MAX_VALUE
        var maxTemp = Float.MIN_VALUE

        val timeSeconds = (timestamp % 60_000L) / 1_000f
        val emissivityFactor = 1f - (1f - emissivity) * 0.1f
        val distanceFactor = (1f / (1f + distanceMeters * 0.1f)).coerceIn(0.7f, 1f)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val nx = x / width.toFloat()
                val ny = y / height.toFloat()
                val wave =
                    (sin(2 * PI * (nx + timeSeconds / 6f)) +
                        sin(2 * PI * (ny + timeSeconds / 9f)) +
                        sin(PI * (nx + ny) + timeSeconds / 4f)) / 3f
                val jitter = (random.nextFloat() - 0.5f) * 0.06f
                val normalized = ((wave + 1f) / 2f + jitter).coerceIn(0f, 1f)
                val baseTemperature =
                    currentMinTemp +
                        (currentMaxTemp - currentMinTemp) * normalized * emissivityFactor * distanceFactor
                val finalTemperature = baseTemperature + reflectedTemp * 0.01f
                matrix[y][x] = finalTemperature
                if (finalTemperature < minTemp) minTemp = finalTemperature
                if (finalTemperature > maxTemp) maxTemp = finalTemperature
                pixels[y * width + x] = mapTempToColor(finalTemperature)
            }
        }

        val centerTemp = matrix[height / 2][width / 2]
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

        return ThermalFrameData(
            timestamp = timestamp,
            bitmap = bitmap,
            temperatureMatrix = matrix,
            minTemp = minTemp,
            maxTemp = maxTemp,
            centerTemp = centerTemp,
        )
    }

    private fun writeRecordingManifest(recordingFile: File) {
        try {
            val endTimestamp = timeManager.getCurrentTimestampMs()
            val manifest = JSONObject().apply {
                put("file", recordingFile.name)
                put("absolute_path", recordingFile.absolutePath)
                put("created_at_ms", recordingStartTimestampMs)
                put("ended_at_ms", endTimestamp)
                put("duration_ms", (endTimestamp - recordingStartTimestampMs).coerceAtLeast(0))
                put("frames_recorded", recordedFrames)
                put("mode", "simulation")
                if (sessionMinTemp.isFinite()) {
                    put("min_temp_observed", sessionMinTemp.toDouble())
                }
                if (sessionMaxTemp.isFinite()) {
                    put("max_temp_observed", sessionMaxTemp.toDouble())
                }
            }
            val manifestFile = File(
                recordingFile.parentFile ?: context.filesDir,
                "${recordingFile.nameWithoutExtension}_manifest.json",
            )
            manifestFile.writeText(manifest.toString(2))
        } catch (_: Exception) {
        }
    }

    private fun mapTempToColor(temp: Float): Int {
        val ratio =
            ((temp - currentMinTemp) / (currentMaxTemp - currentMinTemp + 1e-3f))
                .coerceIn(0f, 1f)
        val hue = 240f - (240f * ratio)
        val hsv = floatArrayOf(hue, 1f, 1f)
        return Color.HSVToColor(hsv)
    }

    private fun writeFrameMetadata(frame: ThermalFrameData) {
        try {
            recordingWriter?.let { writer ->
                writer.write(
                    "${frame.timestamp}," +
                        "${"%.2f".format(frame.minTemp)}," +
                        "${"%.2f".format(frame.maxTemp)}," +
                        "${"%.2f".format(frame.centerTemp)}\n",
                )
                recordedFrames += 1
                if (!frame.minTemp.isNaN()) {
                    sessionMinTemp = min(sessionMinTemp, frame.minTemp)
                }
                if (!frame.maxTemp.isNaN()) {
                    sessionMaxTemp = max(sessionMaxTemp, frame.maxTemp)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun iterateArea(
        area: MeasurementArea,
        visitor: (x: Int, y: Int) -> Unit,
    ) {
        when (area) {
            is MeasurementArea.PointArea -> {
                val point = area.point
                visitor(point.x.coerceIn(0, width - 1), point.y.coerceIn(0, height - 1))
            }

            is MeasurementArea.RectangleArea -> {
                val rect = area.rect
                val left = rect.left.coerceIn(0, width - 1)
                val right = rect.right.coerceIn(left, width - 1)
                val top = rect.top.coerceIn(0, height - 1)
                val bottom = rect.bottom.coerceIn(top, height - 1)
                for (y in top..bottom) {
                    for (x in left..right) {
                        visitor(x, y)
                    }
                }
            }

            is MeasurementArea.LineArea -> {
                val dx = area.end.x - area.start.x
                val dy = area.end.y - area.start.y
                val steps = max(abs(dx), abs(dy)).coerceAtLeast(1)
                for (step in 0..steps) {
                    val t = step / steps.toFloat()
                    val x = (area.start.x + dx * t).roundToInt().coerceIn(0, width - 1)
                    val y = (area.start.y + dy * t).roundToInt().coerceIn(0, height - 1)
                    visitor(x, y)
                }
            }

            is MeasurementArea.EllipseArea -> {
                val rect =
                    Rect(
                        (area.centerX - area.radiusX).coerceAtLeast(0),
                        (area.centerY - area.radiusY).coerceAtLeast(0),
                        (area.centerX + area.radiusX).coerceAtMost(width - 1),
                        (area.centerY + area.radiusY).coerceAtMost(height - 1),
                    )
                val rxSq = area.radiusX.toDouble().pow(2.0)
                val rySq = area.radiusY.toDouble().pow(2.0)
                for (y in rect.top..rect.bottom) {
                    for (x in rect.left..rect.right) {
                        val nx = (x - area.centerX).toDouble().pow(2.0) / rxSq
                        val ny = (y - area.centerY).toDouble().pow(2.0) / rySq
                        if (nx + ny <= 1.0) {
                            visitor(x, y)
                        }
                    }
                }
            }

            is MeasurementArea.PolygonArea -> {
                val points = area.points
                if (points.isEmpty()) {
                    return
                }
                val xs = points.map { it.x }
                val ys = points.map { it.y }
                val left = xs.minOrNull()?.coerceAtLeast(0) ?: 0
                val right = xs.maxOrNull()?.coerceAtMost(width - 1) ?: width - 1
                val top = ys.minOrNull()?.coerceAtLeast(0) ?: 0
                val bottom = ys.maxOrNull()?.coerceAtMost(height - 1) ?: height - 1
                for (y in top..bottom) {
                    for (x in left..right) {
                        if (pointInPolygon(Point(x, y), points)) {
                            visitor(x, y)
                        }
                    }
                }
            }
        }
    }

    private fun pointInPolygon(
        point: Point,
        polygon: List<Point>,
    ): Boolean {
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val pi = polygon[i]
            val pj = polygon[j]
            val intersects =
                ((pi.y > point.y) != (pj.y > point.y)) &&
                    (point.x < (pj.x - pi.x) * (point.y - pi.y).toFloat() / (pj.y - pi.y + 1e-6f) + pi.x)
            if (intersects) {
                inside = !inside
            }
            j = i
        }
        return inside
    }
}






