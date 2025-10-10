package mpdc4gsr.feature.capture.thermal.data.repository

import com.mpdc4gsr.libunified.ir.extension.AgcMode
import com.mpdc4gsr.libunified.ir.extension.ColorPalette
import kotlinx.coroutines.flow.Flow
import mpdc4gsr.feature.capture.thermal.data.BatteryStatus
import mpdc4gsr.feature.capture.thermal.data.DeviceInfo
import mpdc4gsr.feature.capture.thermal.data.MeasurementArea
import mpdc4gsr.feature.capture.thermal.data.MeasurementResult
import mpdc4gsr.feature.capture.thermal.data.ThermalCalibrationData
import mpdc4gsr.feature.capture.thermal.data.source.ThermalFrameData
import mpdc4gsr.feature.capture.thermal.data.source.ThermalHardwareDataSource
import mpdc4gsr.feature.capture.thermal.data.source.ThermalSnapshot
import mpdc4gsr.feature.capture.thermal.domain.repository.ThermalRepository

class ThermalRepositoryImpl(
    private val hardwareDataSource: ThermalHardwareDataSource,
) : ThermalRepository {
    override suspend fun connectCamera(): Result<Unit> = hardwareDataSource.connectDevice()

    override suspend fun disconnectCamera() {
        hardwareDataSource.disconnectDevice()
    }

    override suspend fun getThermalStream(): Flow<ThermalFrameData> = hardwareDataSource.startStreaming()

    override suspend fun stopStream() {
        hardwareDataSource.stopStreaming()
    }

    override suspend fun captureSnapshot(): Result<ThermalSnapshot> = hardwareDataSource.captureSnapshot()

    override suspend fun startRecording(): Result<Unit> = hardwareDataSource.startRecording()

    override suspend fun stopRecording(): Result<String> = hardwareDataSource.stopRecording()

    override fun isCameraConnected(): Boolean = hardwareDataSource.isConnected()

    override suspend fun setTemperatureRange(
        minTemp: Float,
        maxTemp: Float,
    ): Result<Unit> = hardwareDataSource.setTemperatureRange(minTemp, maxTemp)

    override suspend fun setColorPalette(palette: ColorPalette): Result<Unit> = hardwareDataSource.setColorPalette(palette)

    override suspend fun setAgcMode(mode: AgcMode): Result<Unit> = hardwareDataSource.setAgcMode(mode)

    override suspend fun getMeasurementForArea(area: MeasurementArea): Result<MeasurementResult> =
        hardwareDataSource.getMeasurementForArea(area)

    override suspend fun applyCalibration(calibrationData: ThermalCalibrationData): Result<Unit> =
        hardwareDataSource.applyCalibration(calibrationData)

    override suspend fun performFFC(): Result<Unit> = hardwareDataSource.performFFC()

    override suspend fun performNUC(): Result<Unit> = hardwareDataSource.performNUC()

    override suspend fun enableISP(enabled: Boolean): Result<Unit> = hardwareDataSource.enableISP(enabled)

    override suspend fun setTNRLevel(level: Int): Result<Unit> = hardwareDataSource.setTNRLevel(level)

    override suspend fun setBrightness(level: Int): Result<Unit> = hardwareDataSource.setBrightness(level)

    override suspend fun setContrast(level: Int): Result<Unit> = hardwareDataSource.setContrast(level)

    override suspend fun setSharpness(level: Int): Result<Unit> = hardwareDataSource.setSharpness(level)

    override suspend fun getDeviceInfo(): Result<DeviceInfo> = hardwareDataSource.getDeviceInfo()

    override suspend fun getBatteryStatus(): Result<BatteryStatus> = hardwareDataSource.getBatteryStatus()
}
