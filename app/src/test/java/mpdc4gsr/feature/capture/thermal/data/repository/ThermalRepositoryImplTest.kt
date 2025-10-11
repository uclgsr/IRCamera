package mpdc4gsr.feature.capture.thermal.data.repository

import com.mpdc4gsr.component.shared.ir.extension.AgcMode
import com.mpdc4gsr.component.shared.ir.extension.ColorPalette
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mpdc4gsr.feature.capture.thermal.data.BatteryStatus
import mpdc4gsr.feature.capture.thermal.data.DeviceInfo
import mpdc4gsr.feature.capture.thermal.data.MeasurementArea
import mpdc4gsr.feature.capture.thermal.data.MeasurementResult
import mpdc4gsr.feature.capture.thermal.data.ThermalCalibrationData
import mpdc4gsr.feature.capture.thermal.data.source.ThermalFrameData
import mpdc4gsr.feature.capture.thermal.data.source.ThermalHardwareDataSource
import mpdc4gsr.feature.capture.thermal.data.source.ThermalSnapshot
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.onBlocking
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class ThermalRepositoryImplTest {

    private val dataSource: ThermalHardwareDataSource = mock {
        onBlocking { connectDevice() } doReturn Result.success(Unit)
        onBlocking { captureSnapshot() } doReturn Result.success(fakeSnapshot())
        onBlocking { startRecording() } doReturn Result.success(Unit)
        onBlocking { stopRecording() } doReturn Result.success("path")
        on { isConnected() } doReturn true
        on { isSimulationMode() } doReturn false
        onBlocking { getThermalStream() } doReturn flowOf(fakeFrame())
        onBlocking { getMeasurementForArea(any()) } doReturn Result.success(fakeMeasurement())
        onBlocking { applyCalibration(any()) } doReturn Result.success(Unit)
        onBlocking { getDeviceInfo() } doReturn Result.success(fakeDeviceInfo())
        onBlocking { getBatteryStatus() } doReturn Result.success(fakeBatteryStatus())
        on { getLastRecordingPath() } doReturn "path"
    }
    private val repository = ThermalRepositoryImpl(dataSource)

    @Test
    fun `delegates to hardware data source`() = runTest {
        repository.connectCamera()
        repository.getThermalStream()
        repository.captureSnapshot()
        repository.startRecording()
        repository.stopRecording()
        repository.setTemperatureRange(10f, 30f)
        repository.setColorPalette(ColorPalette.IRON)
        repository.setAgcMode(AgcMode.Auto)
        repository.getMeasurementForArea(MeasurementArea.PointArea(android.graphics.Point(1, 1)))
        repository.applyCalibration(ThermalCalibrationData())
        repository.performFFC()
        repository.performNUC()
        repository.enableISP(true)
        repository.setTNRLevel(2)
        repository.setBrightness(3)
        repository.setContrast(4)
        repository.setSharpness(5)
        repository.getDeviceInfo()
        repository.getBatteryStatus()

        verify { dataSource.connectDevice() }
        verify { dataSource.getThermalStream() }
        verify { dataSource.captureSnapshot() }
        verify { dataSource.startRecording() }
        verify { dataSource.stopRecording() }
    }

    @Test
    fun `state queries mirror data source`() {
        assertEquals(true, repository.isCameraConnected())
        assertEquals(false, repository.isSimulationMode())
        assertEquals("path", repository.getLastRecordingPath())
    }

    private fun fakeSnapshot(): ThermalSnapshot =
        ThermalSnapshot(
            bitmap = android.graphics.Bitmap.createBitmap(2, 2, android.graphics.Bitmap.Config.ARGB_8888),
            temperatureMatrix = arrayOf(floatArrayOf(1f, 2f)),
            minTemp = 1f,
            maxTemp = 2f,
            timestamp = 0L,
        )

    private fun fakeFrame(): ThermalFrameData =
        ThermalFrameData(
            timestamp = 0L,
            bitmap = android.graphics.Bitmap.createBitmap(2, 2, android.graphics.Bitmap.Config.ARGB_8888),
            temperatureMatrix = arrayOf(floatArrayOf(1f)),
            minTemp = 1f,
            maxTemp = 2f,
            centerTemp = 1.5f,
        )

    private fun fakeMeasurement(): MeasurementResult =
        MeasurementResult(
            minTemp = 1f,
            maxTemp = 2f,
            avgTemp = 1.5f,
            area = MeasurementArea.PointArea(android.graphics.Point(0, 0)),
        )

    private fun fakeDeviceInfo(): DeviceInfo =
        DeviceInfo(
            model = "model",
            serialNumber = "serial",
            firmwareVersion = "1.0",
            sdkVersion = "sdk",
            resolution = 1 to 1,
            frameRate = 9f,
            temperatureRange = 10f to 20f,
        )

    private fun fakeBatteryStatus(): BatteryStatus =
        BatteryStatus(
            level = 50,
            isCharging = true,
            voltage = 3.7f,
        )
}
