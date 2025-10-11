package mpdc4gsr.feature.capture.thermal.data.source

import mpdc4gsr.core.common.AppLogger
import mpdc4gsr.feature.capture.thermal.data.BatteryStatus
import mpdc4gsr.feature.capture.thermal.data.DeviceInfo
import mpdc4gsr.feature.capture.thermal.data.MeasurementArea
import mpdc4gsr.feature.capture.thermal.data.MeasurementResult
import mpdc4gsr.feature.capture.thermal.data.ThermalCalibrationData
import com.mpdc4gsr.component.shared.ir.extension.AgcMode
import com.mpdc4gsr.component.shared.ir.extension.ColorPalette
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Delegating implementation that prefers the real hardware data source, but will
 * transparently fall back to the simulation source when hardware is unavailable.
 */
class FailoverThermalDataSource(
    private val hardwareDataSource: ThermalHardwareDataSource,
    private val simulationDataSource: ThermalHardwareDataSource,
) : ThermalHardwareDataSource {
    private val delegate: AtomicReference<ThermalHardwareDataSource> = AtomicReference(hardwareDataSource)
    private val simulationForced = AtomicBoolean(checkSimulationOverride())

    override suspend fun connectDevice(): Result<Unit> {
        return if (simulationForced.get()) {
            switchToSimulation()
        } else {
            val hardwareResult =
                runCatching { hardwareDataSource.connectDevice() }
                    .getOrElse { throwable -> Result.failure(throwable) }
            if (hardwareResult.isSuccess) {
                delegate.set(hardwareDataSource)
                hardwareResult
            } else {
                AppLogger.w(
                    TAG,
                    "Hardware thermal camera unavailable, falling back to simulation: ${hardwareResult.exceptionOrNull()?.message}",
                )
                switchToSimulation()
            }
        }
    }

    override suspend fun disconnectDevice() {
        runCatching { delegate.get().disconnectDevice() }
        delegate.set(hardwareDataSource)
    }

    override suspend fun startStreaming(): Flow<ThermalFrameData> = delegate.get().startStreaming()

    override suspend fun stopStreaming() {
        delegate.get().stopStreaming()
    }

    override suspend fun captureSnapshot(): Result<ThermalSnapshot> = delegate.get().captureSnapshot()

    override suspend fun startRecording(): Result<Unit> = delegate.get().startRecording()

    override suspend fun stopRecording(): Result<String> = delegate.get().stopRecording()

    override fun isConnected(): Boolean = delegate.get().isConnected()

    override fun isSimulationMode(): Boolean = delegate.get().isSimulationMode()

    override fun getLastRecordingPath(): String? = delegate.get().getLastRecordingPath()

    override suspend fun setTemperatureRange(
        min: Float,
        max: Float,
    ): Result<Unit> = delegate.get().setTemperatureRange(min, max)

    override suspend fun setColorPalette(palette: ColorPalette): Result<Unit> = delegate.get().setColorPalette(palette)

    override suspend fun setAgcMode(mode: AgcMode): Result<Unit> = delegate.get().setAgcMode(mode)

    override suspend fun setEmissivity(value: Float): Result<Unit> = delegate.get().setEmissivity(value)

    override suspend fun setMeasurementDistance(meters: Float): Result<Unit> = delegate.get().setMeasurementDistance(meters)

    override suspend fun setReflectedTemperature(tempCelsius: Float): Result<Unit> =
        delegate.get().setReflectedTemperature(tempCelsius)

    override suspend fun getMeasurementForArea(area: MeasurementArea): Result<MeasurementResult> =
        delegate.get().getMeasurementForArea(area)

    override suspend fun applyCalibration(calibrationData: ThermalCalibrationData): Result<Unit> =
        delegate.get().applyCalibration(calibrationData)

    override suspend fun performFFC(): Result<Unit> = delegate.get().performFFC()

    override suspend fun performNUC(): Result<Unit> = delegate.get().performNUC()

    override suspend fun enableISP(enabled: Boolean): Result<Unit> = delegate.get().enableISP(enabled)

    override suspend fun setTNRLevel(level: Int): Result<Unit> = delegate.get().setTNRLevel(level)

    override suspend fun setBrightness(level: Int): Result<Unit> = delegate.get().setBrightness(level)

    override suspend fun setContrast(level: Int): Result<Unit> = delegate.get().setContrast(level)

    override suspend fun setSharpness(level: Int): Result<Unit> = delegate.get().setSharpness(level)

    override suspend fun getDeviceInfo(): Result<DeviceInfo> = delegate.get().getDeviceInfo()

    override suspend fun getBatteryStatus(): Result<BatteryStatus> = delegate.get().getBatteryStatus()

    fun forceSimulationMode(force: Boolean) {
        simulationForced.set(force)
        if (force) {
            delegate.set(simulationDataSource)
        }
    }

    private suspend fun switchToSimulation(): Result<Unit> {
        delegate.set(simulationDataSource)
        simulationForced.set(true)
        return simulationDataSource.connectDevice()
    }

    private fun checkSimulationOverride(): Boolean {
        val env = System.getenv("MPDC4GSR_FORCE_THERMAL_SIMULATION")?.toBooleanStrictOrNull() ?: false
        val prop = runCatching { System.getProperty("mpdc4gsr.forceThermalSimulation")?.toBoolean() ?: false }.getOrDefault(false)
        return env || prop
    }

    companion object {
        private const val TAG = "FailoverThermalDataSource"
    }
}


