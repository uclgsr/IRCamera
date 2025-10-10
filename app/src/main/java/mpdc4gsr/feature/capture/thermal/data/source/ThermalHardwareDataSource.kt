package mpdc4gsr.feature.capture.thermal.data.source

import android.graphics.Bitmap
import com.mpdc4gsr.libunified.ir.extension.AgcMode
import com.mpdc4gsr.libunified.ir.extension.ColorPalette
import kotlinx.coroutines.flow.Flow
import mpdc4gsr.feature.capture.thermal.data.BatteryStatus
import mpdc4gsr.feature.capture.thermal.data.DeviceInfo
import mpdc4gsr.feature.capture.thermal.data.MeasurementArea
import mpdc4gsr.feature.capture.thermal.data.MeasurementResult
import mpdc4gsr.feature.capture.thermal.data.ThermalCalibrationData

interface ThermalHardwareDataSource {

    suspend fun connectDevice(): Result<Unit>

    suspend fun disconnectDevice()

    suspend fun startStreaming(): Flow<ThermalFrameData>

    suspend fun stopStreaming()

    suspend fun captureSnapshot(): Result<ThermalSnapshot>

    suspend fun startRecording(): Result<Unit>

    suspend fun stopRecording(): Result<String>

    fun isConnected(): Boolean

    suspend fun setTemperatureRange(min: Float, max: Float): Result<Unit>

    suspend fun setColorPalette(palette: ColorPalette): Result<Unit>

    suspend fun setAgcMode(mode: AgcMode): Result<Unit>

    suspend fun setEmissivity(value: Float): Result<Unit>

    suspend fun setMeasurementDistance(meters: Float): Result<Unit>

    suspend fun setReflectedTemperature(tempCelsius: Float): Result<Unit>

    suspend fun getMeasurementForArea(area: MeasurementArea): Result<MeasurementResult>

    suspend fun applyCalibration(calibrationData: ThermalCalibrationData): Result<Unit>

    suspend fun performFFC(): Result<Unit>

    suspend fun performNUC(): Result<Unit>

    suspend fun enableISP(enabled: Boolean): Result<Unit>

    suspend fun setTNRLevel(level: Int): Result<Unit>

    suspend fun setBrightness(level: Int): Result<Unit>

    suspend fun setContrast(level: Int): Result<Unit>

    suspend fun setSharpness(level: Int): Result<Unit>

    suspend fun getDeviceInfo(): Result<DeviceInfo>

    suspend fun getBatteryStatus(): Result<BatteryStatus>
}

data class ThermalFrameData(
    val timestamp: Long,
    val bitmap: Bitmap,
    val temperatureMatrix: Array<FloatArray>,
    val minTemp: Float,
    val maxTemp: Float,
    val centerTemp: Float
)

data class ThermalSnapshot(
    val bitmap: Bitmap,
    val temperatureMatrix: Array<FloatArray>,
    val minTemp: Float,
    val maxTemp: Float,
    val timestamp: Long,
    val location: String? = null
)

