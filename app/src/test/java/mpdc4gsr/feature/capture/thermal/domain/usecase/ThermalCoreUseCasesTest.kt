package mpdc4gsr.feature.capture.thermal.domain.usecase

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import mpdc4gsr.feature.capture.thermal.data.MeasurementArea
import mpdc4gsr.feature.capture.thermal.data.MeasurementResult
import mpdc4gsr.feature.capture.thermal.data.ThermalCalibrationData
import mpdc4gsr.feature.capture.thermal.data.source.ThermalFrameData
import mpdc4gsr.feature.capture.thermal.data.source.ThermalSnapshot
import mpdc4gsr.feature.capture.thermal.domain.repository.ThermalRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.onBlocking
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class ThermalCoreUseCasesTest {

    private lateinit var repository: ThermalRepository
    private lateinit var useCases: ThermalCoreUseCases

    @Before
    fun setUp() {
        repository =
            mock {
                onBlocking { connectCamera() } doReturn Result.success(Unit)
                onBlocking { getThermalStream() } doReturn flowOf(fakeFrame())
                onBlocking { captureSnapshot() } doReturn Result.success(fakeSnapshot())
                on { isCameraConnected() } doReturn true
                on { isSimulationMode() } doReturn false
                onBlocking { getMeasurementForArea(any()) } doReturn Result.success(fakeMeasurement())
            }
        useCases =
            ThermalCoreUseCases(
                connectCamera = ConnectThermalCameraUseCase(repository),
                disconnectCamera = DisconnectThermalCameraUseCase(repository),
                startStreaming = StartThermalStreamingUseCase(repository),
                stopStreaming = StopThermalStreamingUseCase(repository),
                captureSnapshot = CaptureThermalSnapshotUseCase(repository),
                startRecording = StartThermalRecordingUseCase(repository),
                stopRecording = StopThermalRecordingUseCase(repository),
                setTemperatureRange = SetTemperatureRangeUseCase(repository),
                checkConnection = CheckCameraConnectionUseCase(repository),
                isSimulationMode = IsThermalSimulationModeUseCase(repository),
            )
    }

    @Test
    fun `connect camera delegates to repository`() = runTest {
        val result = useCases.connectCamera()

        assert(result.isSuccess)
        verify(repository).connectCamera()
    }

    @Test
    fun `streaming use case returns repository flow`() = runTest {
        val frames = useCases.startStreaming().toList(mutableListOf())

        assertEquals(1, frames.size)
        verify(repository).getThermalStream()
    }

    @Test
    fun `temperature range validation checks bounds`() = runTest {
        val failure = useCases.setTemperatureRange(30f, 10f)
        assert(failure.isFailure)
    }

    @Test
    fun `checkConnection and simulation mode mirror repository`() {
        assert(useCases.checkConnection())
        assertFalse(useCases.isSimulationMode())
    }

    private fun fakeFrame(): ThermalFrameData =
        ThermalFrameData(
            timestamp = 0L,
            bitmap = android.graphics.Bitmap.createBitmap(2, 2, android.graphics.Bitmap.Config.ARGB_8888),
            temperatureMatrix = arrayOf(floatArrayOf(1f)),
            minTemp = 1f,
            maxTemp = 2f,
            centerTemp = 1.5f,
        )

    private fun fakeSnapshot(): ThermalSnapshot =
        ThermalSnapshot(
            bitmap = android.graphics.Bitmap.createBitmap(2, 2, android.graphics.Bitmap.Config.ARGB_8888),
            temperatureMatrix = arrayOf(floatArrayOf(1f)),
            minTemp = 1f,
            maxTemp = 2f,
            timestamp = 0L,
        )

    private fun fakeMeasurement(): MeasurementResult =
        MeasurementResult(
            minTemp = 1f,
            maxTemp = 2f,
            avgTemp = 1.5f,
            area = MeasurementArea.PointArea(android.graphics.Point(0, 0)),
        )
}
