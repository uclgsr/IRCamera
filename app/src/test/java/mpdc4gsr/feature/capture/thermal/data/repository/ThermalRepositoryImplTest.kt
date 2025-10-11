package mpdc4gsr.feature.capture.thermal.data.repository

import com.mpdc4gsr.component.shared.ir.extension.AgcMode
import com.mpdc4gsr.component.shared.ir.extension.ColorPalette
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mpdc4gsr.feature.capture.thermal.data.BatteryStatus
import mpdc4gsr.feature.capture.thermal.data.DeviceInfo
import mpdc4gsr.feature.capture.thermal.data.MeasurementArea
import mpdc4gsr.feature.capture.thermal.data.MeasurementResult
import mpdc4gsr.feature.capture.thermal.data.ThermalCalibrationData
import mpdc4gsr.feature.capture.thermal.data.source.ThermalFrameData
import mpdc4gsr.feature.capture.thermal.data.source.ThermalHardwareDataSource
import mpdc4gsr.feature.capture.thermal.data.source.ThermalSnapshot
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ThermalRepositoryImplTest {

    private lateinit var dataSource: ThermalHardwareDataSource
    private lateinit var repository: ThermalRepositoryImpl

    @Before
    fun setUp() {
        dataSource = mock()
        runBlocking {
            whenever(dataSource.connectDevice()).thenReturn(Result.success(Unit))
            whenever(dataSource.captureSnapshot()).thenReturn(Result.success(fakeSnapshot()))
            whenever(dataSource.startRecording()).thenReturn(Result.success(Unit))
            whenever(dataSource.stopRecording()).thenReturn(Result.success("path"))
            whenever(dataSource.startStreaming()).thenReturn(flowOf(fakeFrame()))
            whenever(dataSource.getMeasurementForArea(any())).thenReturn(Result.success(fakeMeasurement()))
            whenever(dataSource.applyCalibration(any())).thenReturn(Result.success(Unit))
            whenever(dataSource.performFFC()).thenReturn(Result.success(Unit))
            whenever(dataSource.performNUC()).thenReturn(Result.success(Unit))
            whenever(dataSource.enableISP(any())).thenReturn(Result.success(Unit))
            whenever(dataSource.setTNRLevel(any())).thenReturn(Result.success(Unit))
            whenever(dataSource.setBrightness(any())).thenReturn(Result.success(Unit))
            whenever(dataSource.setContrast(any())).thenReturn(Result.success(Unit))
            whenever(dataSource.setSharpness(any())).thenReturn(Result.success(Unit))
            whenever(dataSource.setTemperatureRange(any(), any())).thenReturn(Result.success(Unit))
            whenever(dataSource.setColorPalette(any())).thenReturn(Result.success(Unit))
            whenever(dataSource.setAgcMode(any())).thenReturn(Result.success(Unit))
            whenever(dataSource.getDeviceInfo()).thenReturn(Result.success(fakeDeviceInfo()))
            whenever(dataSource.getBatteryStatus()).thenReturn(Result.success(fakeBatteryStatus()))
        }
        whenever(dataSource.isConnected()).thenReturn(true)
        whenever(dataSource.isSimulationMode()).thenReturn(false)
        whenever(dataSource.getLastRecordingPath()).thenReturn("path")

        repository = ThermalRepositoryImpl(dataSource)
    }

    @Test
    fun `delegates to hardware data source`() = runTest {
        repository.connectCamera()
        repository.getThermalStream()
        repository.captureSnapshot()
        repository.startRecording()
        repository.stopRecording()
        repository.setTemperatureRange(10f, 30f)
        repository.setColorPalette(ColorPalette.IRONBOW)
        repository.setAgcMode(AgcMode.AUTO)
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

        verifyBlocking(dataSource) { connectDevice() }
        verifyBlocking(dataSource) { startStreaming() }
        verifyBlocking(dataSource) { captureSnapshot() }
        verifyBlocking(dataSource) { startRecording() }
        verifyBlocking(dataSource) { stopRecording() }
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
