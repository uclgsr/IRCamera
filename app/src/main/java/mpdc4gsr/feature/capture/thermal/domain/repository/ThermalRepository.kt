package mpdc4gsr.feature.capture.thermal.domain.repository

import com.mpdc4gsr.libunified.ir.extension.AgcMode
import com.mpdc4gsr.libunified.ir.extension.ColorPalette
import kotlinx.coroutines.flow.Flow
import mpdc4gsr.feature.capture.thermal.data.BatteryStatus
import mpdc4gsr.feature.capture.thermal.data.DeviceInfo
import mpdc4gsr.feature.capture.thermal.data.MeasurementArea
import mpdc4gsr.feature.capture.thermal.data.MeasurementResult
import mpdc4gsr.feature.capture.thermal.data.ThermalCalibrationData
import mpdc4gsr.feature.capture.thermal.data.source.ThermalFrameData
import mpdc4gsr.feature.capture.thermal.data.source.ThermalSnapshot

interface ThermalRepository {
    suspend fun connectCamera(): Result<Unit>

    suspend fun disconnectCamera()

    suspend fun getThermalStream(): Flow<ThermalFrameData>

    suspend fun stopStream()

    suspend fun captureSnapshot(): Result<ThermalSnapshot>

    suspend fun startRecording(): Result<Unit>

    suspend fun stopRecording(): Result<String>

    fun isCameraConnected(): Boolean

    fun isSimulationMode(): Boolean

    fun getLastRecordingPath(): String?

    suspend fun setTemperatureRange(
        minTemp: Float,
        maxTemp: Float,
    ): Result<Unit>

    suspend fun setColorPalette(palette: ColorPalette): Result<Unit>

    suspend fun setAgcMode(mode: AgcMode): Result<Unit>

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

