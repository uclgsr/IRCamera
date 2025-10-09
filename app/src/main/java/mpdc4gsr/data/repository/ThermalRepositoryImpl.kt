package mpdc4gsr.data.repository

import com.mpdc4gsr.libunified.ir.extension.AgcMode
import com.mpdc4gsr.libunified.ir.extension.ColorPalette
import kotlinx.coroutines.flow.Flow
import mpdc4gsr.data.BatteryStatus
import mpdc4gsr.data.DeviceInfo
import mpdc4gsr.data.MeasurementArea
import mpdc4gsr.data.MeasurementResult
import mpdc4gsr.data.ThermalCalibrationData
import mpdc4gsr.data.source.ThermalFrameData
import mpdc4gsr.data.source.ThermalSnapshot
import mpdc4gsr.data.source.ThermalHardwareDataSource
import mpdc4gsr.domain.repository.ThermalRepository

class ThermalRepositoryImpl(
    private val hardwareDataSource: ThermalHardwareDataSource
) : ThermalRepository {
    override suspend fun connectCamera(): Result<Unit> {
        return hardwareDataSource.connectDevice()
    }

    override suspend fun disconnectCamera() {
        hardwareDataSource.disconnectDevice()
    }

    override suspend fun getThermalStream(): Flow<ThermalFrameData> {
        return hardwareDataSource.startStreaming()
    }

    override suspend fun stopStream() {
        hardwareDataSource.stopStreaming()
    }

    override suspend fun captureSnapshot(): Result<ThermalSnapshot> {
        return hardwareDataSource.captureSnapshot()
    }

    override suspend fun startRecording(): Result<Unit> {
        return hardwareDataSource.startRecording()
    }

    override suspend fun stopRecording(): Result<String> {
        return hardwareDataSource.stopRecording()
    }

    override fun isCameraConnected(): Boolean {
        return hardwareDataSource.isConnected()
    }

    override suspend fun setTemperatureRange(minTemp: Float, maxTemp: Float): Result<Unit> {
        return hardwareDataSource.setTemperatureRange(minTemp, maxTemp)
    }

    override suspend fun setColorPalette(palette: ColorPalette): Result<Unit> {
        return hardwareDataSource.setColorPalette(palette)
    }

    override suspend fun setAgcMode(mode: AgcMode): Result<Unit> {
        return hardwareDataSource.setAgcMode(mode)
    }

    override suspend fun getMeasurementForArea(area: MeasurementArea): Result<MeasurementResult> {
        return hardwareDataSource.getMeasurementForArea(area)
    }

    override suspend fun applyCalibration(calibrationData: ThermalCalibrationData): Result<Unit> {
        return hardwareDataSource.applyCalibration(calibrationData)
    }

    override suspend fun performFFC(): Result<Unit> {
        return hardwareDataSource.performFFC()
    }

    override suspend fun performNUC(): Result<Unit> {
        return hardwareDataSource.performNUC()
    }

    override suspend fun enableISP(enabled: Boolean): Result<Unit> {
        return hardwareDataSource.enableISP(enabled)
    }

    override suspend fun setTNRLevel(level: Int): Result<Unit> {
        return hardwareDataSource.setTNRLevel(level)
    }

    override suspend fun setBrightness(level: Int): Result<Unit> {
        return hardwareDataSource.setBrightness(level)
    }

    override suspend fun setContrast(level: Int): Result<Unit> {
        return hardwareDataSource.setContrast(level)
    }

    override suspend fun setSharpness(level: Int): Result<Unit> {
        return hardwareDataSource.setSharpness(level)
    }

    override suspend fun getDeviceInfo(): Result<DeviceInfo> {
        return hardwareDataSource.getDeviceInfo()
    }

    override suspend fun getBatteryStatus(): Result<BatteryStatus> {
        return hardwareDataSource.getBatteryStatus()
    }
}
