package mpdc4gsr.feature.thermal.data.repository

import com.mpdc4gsr.libunified.ir.extension.AgcMode
import com.mpdc4gsr.libunified.ir.extension.ColorPalette
import kotlinx.coroutines.flow.Flow
import mpdc4gsr.feature.thermal.data.BatteryStatus
import mpdc4gsr.feature.thermal.data.DeviceInfo
import mpdc4gsr.feature.thermal.data.MeasurementArea
import mpdc4gsr.feature.thermal.data.MeasurementResult
import mpdc4gsr.feature.thermal.data.ThermalCalibrationData
import mpdc4gsr.feature.thermal.data.source.ThermalFrameData
import mpdc4gsr.feature.thermal.data.source.ThermalSnapshot
import mpdc4gsr.feature.thermal.data.source.TopdonDataSource
import mpdc4gsr.feature.thermal.domain.repository.ThermalRepository

class ThermalRepositoryImpl(
    private val topdonDataSource: TopdonDataSource
) : ThermalRepository {
    override suspend fun connectCamera(): Result<Unit> {
        return topdonDataSource.connectDevice()
    }

    override suspend fun disconnectCamera() {
        topdonDataSource.disconnectDevice()
    }

    override suspend fun getThermalStream(): Flow<ThermalFrameData> {
        return topdonDataSource.startStreaming()
    }

    override suspend fun stopStream() {
        topdonDataSource.stopStreaming()
    }

    override suspend fun captureSnapshot(): Result<ThermalSnapshot> {
        return topdonDataSource.captureSnapshot()
    }

    override suspend fun startRecording(): Result<Unit> {
        return topdonDataSource.startRecording()
    }

    override suspend fun stopRecording(): Result<String> {
        return topdonDataSource.stopRecording()
    }

    override fun isCameraConnected(): Boolean {
        return topdonDataSource.isConnected()
    }

    override suspend fun setTemperatureRange(minTemp: Float, maxTemp: Float): Result<Unit> {
        return topdonDataSource.setTemperatureRange(minTemp, maxTemp)
    }

    override suspend fun setColorPalette(palette: ColorPalette): Result<Unit> {
        return topdonDataSource.setColorPalette(palette)
    }

    override suspend fun setAgcMode(mode: AgcMode): Result<Unit> {
        return topdonDataSource.setAgcMode(mode)
    }

    override suspend fun getMeasurementForArea(area: MeasurementArea): Result<MeasurementResult> {
        return topdonDataSource.getMeasurementForArea(area)
    }

    override suspend fun applyCalibration(calibrationData: ThermalCalibrationData): Result<Unit> {
        return topdonDataSource.applyCalibration(calibrationData)
    }

    override suspend fun performFFC(): Result<Unit> {
        return topdonDataSource.performFFC()
    }

    override suspend fun performNUC(): Result<Unit> {
        return topdonDataSource.performNUC()
    }

    override suspend fun enableISP(enabled: Boolean): Result<Unit> {
        return topdonDataSource.enableISP(enabled)
    }

    override suspend fun setTNRLevel(level: Int): Result<Unit> {
        return topdonDataSource.setTNRLevel(level)
    }

    override suspend fun setBrightness(level: Int): Result<Unit> {
        return topdonDataSource.setBrightness(level)
    }

    override suspend fun setContrast(level: Int): Result<Unit> {
        return topdonDataSource.setContrast(level)
    }

    override suspend fun setSharpness(level: Int): Result<Unit> {
        return topdonDataSource.setSharpness(level)
    }

    override suspend fun getDeviceInfo(): Result<DeviceInfo> {
        return topdonDataSource.getDeviceInfo()
    }

    override suspend fun getBatteryStatus(): Result<BatteryStatus> {
        return topdonDataSource.getBatteryStatus()
    }
}
