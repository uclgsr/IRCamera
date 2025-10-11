package mpdc4gsr.feature.capture.thermal.data.source

import android.content.Context
import com.mpdc4gsr.component.shared.ir.extension.AgcMode
import com.mpdc4gsr.component.shared.ir.extension.ColorPalette
import kotlinx.coroutines.flow.Flow
import mpdc4gsr.feature.capture.thermal.data.BatteryStatus
import mpdc4gsr.feature.capture.thermal.data.DeviceInfo
import mpdc4gsr.feature.capture.thermal.data.MeasurementArea
import mpdc4gsr.feature.capture.thermal.data.MeasurementResult
import mpdc4gsr.feature.capture.thermal.data.ThermalCalibrationData

/**
 * Thin adapter over [ThermalSimulationDataSource] so the rest of the codebase can be compiled
 * without bundling the proprietary Topdon SDK. The implementation keeps the same API surface
 * as the production data source while emitting deterministic simulated data.
 */
class ThermalHardwareDataSourceImpl(
    context: Context,
) : ThermalHardwareDataSource {

    private val delegate = ThermalSimulationDataSource(context)

    override suspend fun connectDevice(): Result<Unit> = delegate.connectDevice()

    override suspend fun disconnectDevice() {
        delegate.disconnectDevice()
    }

    override suspend fun startStreaming(): Flow<ThermalFrameData> = delegate.startStreaming()

    override suspend fun stopStreaming() {
        delegate.stopStreaming()
    }

    override suspend fun captureSnapshot(): Result<ThermalSnapshot> = delegate.captureSnapshot()

    override suspend fun startRecording(): Result<Unit> = delegate.startRecording()

    override suspend fun stopRecording(): Result<String> = delegate.stopRecording()

    override fun isConnected(): Boolean = delegate.isConnected()

    override fun isSimulationMode(): Boolean = delegate.isSimulationMode()

    override fun getLastRecordingPath(): String? = delegate.getLastRecordingPath()

    override suspend fun setTemperatureRange(
        min: Float,
        max: Float,
    ): Result<Unit> = delegate.setTemperatureRange(min, max)

    override suspend fun setColorPalette(palette: ColorPalette): Result<Unit> = delegate.setColorPalette(palette)

    override suspend fun setAgcMode(mode: AgcMode): Result<Unit> = delegate.setAgcMode(mode)

    override suspend fun setEmissivity(value: Float): Result<Unit> = delegate.setEmissivity(value)

    override suspend fun setMeasurementDistance(meters: Float): Result<Unit> = delegate.setMeasurementDistance(meters)

    override suspend fun setReflectedTemperature(tempCelsius: Float): Result<Unit> =
        delegate.setReflectedTemperature(tempCelsius)

    override suspend fun getMeasurementForArea(area: MeasurementArea): Result<MeasurementResult> =
        delegate.getMeasurementForArea(area)

    override suspend fun applyCalibration(calibrationData: ThermalCalibrationData): Result<Unit> =
        delegate.applyCalibration(calibrationData)

    override suspend fun performFFC(): Result<Unit> = delegate.performFFC()

    override suspend fun performNUC(): Result<Unit> = delegate.performNUC()

    override suspend fun enableISP(enabled: Boolean): Result<Unit> = delegate.enableISP(enabled)

    override suspend fun setTNRLevel(level: Int): Result<Unit> = delegate.setTNRLevel(level)

    override suspend fun setBrightness(level: Int): Result<Unit> = delegate.setBrightness(level)

    override suspend fun setContrast(level: Int): Result<Unit> = delegate.setContrast(level)

    override suspend fun setSharpness(level: Int): Result<Unit> = delegate.setSharpness(level)

    override suspend fun getDeviceInfo(): Result<DeviceInfo> = delegate.getDeviceInfo()

    override suspend fun getBatteryStatus(): Result<BatteryStatus> = delegate.getBatteryStatus()
}
