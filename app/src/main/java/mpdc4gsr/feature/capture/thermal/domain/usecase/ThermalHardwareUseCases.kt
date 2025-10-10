package mpdc4gsr.feature.capture.thermal.domain.usecase

import com.mpdc4gsr.libunified.ir.extension.AgcMode
import com.mpdc4gsr.libunified.ir.extension.ColorPalette
import mpdc4gsr.feature.capture.thermal.data.BatteryStatus
import mpdc4gsr.feature.capture.thermal.data.DeviceInfo
import mpdc4gsr.feature.capture.thermal.data.MeasurementArea
import mpdc4gsr.feature.capture.thermal.data.MeasurementResult
import mpdc4gsr.feature.capture.thermal.data.ThermalCalibrationData
import mpdc4gsr.feature.capture.thermal.domain.repository.ThermalRepository

class SetColorPaletteUseCase(
    private val repository: ThermalRepository,
) {
    suspend operator fun invoke(palette: ColorPalette): Result<Unit> = repository.setColorPalette(palette)
}

class SetAgcModeUseCase(
    private val repository: ThermalRepository,
) {
    suspend operator fun invoke(mode: AgcMode): Result<Unit> = repository.setAgcMode(mode)
}

class ConfigureAccuracyUseCase(
    private val repository: ThermalRepository,
) {
    suspend operator fun invoke(
        emissivity: Float,
        distance: Float,
        reflectedTemp: Float,
    ): Result<Unit> {
        val calibrationData =
            ThermalCalibrationData(
                emissivity = emissivity,
                distance = distance,
                reflectedTemperature = reflectedTemp,
            )
        return repository.applyCalibration(calibrationData)
    }
}

class MeasureAreaUseCase(
    private val repository: ThermalRepository,
) {
    suspend operator fun invoke(area: MeasurementArea): Result<MeasurementResult> = repository.getMeasurementForArea(area)
}

class PerformCalibrationUseCase(
    private val repository: ThermalRepository,
) {
    suspend fun performFFC(): Result<Unit> = repository.performFFC()

    suspend fun performNUC(): Result<Unit> = repository.performNUC()
}

class ConfigureImageEnhancementUseCase(
    private val repository: ThermalRepository,
) {
    suspend fun enableISP(enabled: Boolean): Result<Unit> = repository.enableISP(enabled)

    suspend fun setTNRLevel(level: Int): Result<Unit> = repository.setTNRLevel(level)

    suspend fun setBrightness(level: Int): Result<Unit> = repository.setBrightness(level)

    suspend fun setContrast(level: Int): Result<Unit> = repository.setContrast(level)

    suspend fun setSharpness(level: Int): Result<Unit> = repository.setSharpness(level)
}

class GetDeviceInfoUseCase(
    private val repository: ThermalRepository,
) {
    suspend operator fun invoke(): Result<DeviceInfo> = repository.getDeviceInfo()
}

class GetBatteryStatusUseCase(
    private val repository: ThermalRepository,
) {
    suspend operator fun invoke(): Result<BatteryStatus> = repository.getBatteryStatus()
}

data class ThermalHardwareUseCases(
    val setColorPalette: SetColorPaletteUseCase,
    val setAgcMode: SetAgcModeUseCase,
    val configureAccuracy: ConfigureAccuracyUseCase,
    val measureArea: MeasureAreaUseCase,
    val performCalibration: PerformCalibrationUseCase,
    val configureImageEnhancement: ConfigureImageEnhancementUseCase,
    val getDeviceInfo: GetDeviceInfoUseCase,
    val getBatteryStatus: GetBatteryStatusUseCase,
)
