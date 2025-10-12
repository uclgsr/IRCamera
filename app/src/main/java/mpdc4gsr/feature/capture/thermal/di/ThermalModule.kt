package mpdc4gsr.feature.capture.thermal.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mpdc4gsr.feature.capture.thermal.data.repository.ThermalRepositoryImpl
import mpdc4gsr.feature.capture.thermal.data.source.FailoverThermalDataSource
import mpdc4gsr.feature.capture.thermal.data.source.ThermalHardwareDataSource
import mpdc4gsr.feature.capture.thermal.data.source.ThermalHardwareDataSourceImpl
import mpdc4gsr.feature.capture.thermal.data.source.ThermalSimulationDataSource
import mpdc4gsr.feature.capture.thermal.domain.repository.ThermalRepository
import mpdc4gsr.feature.capture.thermal.domain.usecase.CaptureThermalSnapshotUseCase
import mpdc4gsr.feature.capture.thermal.domain.usecase.CheckCameraConnectionUseCase
import mpdc4gsr.feature.capture.thermal.domain.usecase.ConfigureAccuracyUseCase
import mpdc4gsr.feature.capture.thermal.domain.usecase.ConfigureImageEnhancementUseCase
import mpdc4gsr.feature.capture.thermal.domain.usecase.ConnectThermalCameraUseCase
import mpdc4gsr.feature.capture.thermal.domain.usecase.DisconnectThermalCameraUseCase
import mpdc4gsr.feature.capture.thermal.domain.usecase.GetBatteryStatusUseCase
import mpdc4gsr.feature.capture.thermal.domain.usecase.GetDeviceInfoUseCase
import mpdc4gsr.feature.capture.thermal.domain.usecase.MeasureAreaUseCase
import mpdc4gsr.feature.capture.thermal.domain.usecase.PerformCalibrationUseCase
import mpdc4gsr.feature.capture.thermal.domain.usecase.SetAgcModeUseCase
import mpdc4gsr.feature.capture.thermal.domain.usecase.SetColorPaletteUseCase
import mpdc4gsr.feature.capture.thermal.domain.usecase.SetTemperatureRangeUseCase
import mpdc4gsr.feature.capture.thermal.domain.usecase.StartThermalRecordingUseCase
import mpdc4gsr.feature.capture.thermal.domain.usecase.StartThermalStreamingUseCase
import mpdc4gsr.feature.capture.thermal.domain.usecase.StopThermalRecordingUseCase
import mpdc4gsr.feature.capture.thermal.domain.usecase.StopThermalStreamingUseCase
import mpdc4gsr.feature.capture.thermal.domain.usecase.ThermalCoreUseCases
import mpdc4gsr.feature.capture.thermal.domain.usecase.ThermalHardwareUseCases
import mpdc4gsr.feature.capture.thermal.domain.usecase.IsThermalSimulationModeUseCase
import mpdc4gsr.feature.connectivity.data.DataManagementService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ThermalModule {
    @Provides
    @Singleton
    fun provideThermalHardwareDataSource(
        @ApplicationContext context: Context,
    ): ThermalHardwareDataSource {
        val hardware = ThermalHardwareDataSourceImpl(context)
        val simulation = ThermalSimulationDataSource(context)
        return FailoverThermalDataSource(hardware, simulation)
    }

    @Provides
    @Singleton
    fun provideThermalRepository(dataSource: ThermalHardwareDataSource): ThermalRepository =
        ThermalRepositoryImpl(dataSource)

    @Provides
    @Singleton
    fun provideThermalCoreUseCases(repository: ThermalRepository): ThermalCoreUseCases =
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

    @Provides
    @Singleton
    fun provideThermalHardwareUseCases(repository: ThermalRepository): ThermalHardwareUseCases =
        ThermalHardwareUseCases(
            setColorPalette = SetColorPaletteUseCase(repository),
            setAgcMode = SetAgcModeUseCase(repository),
            configureAccuracy = ConfigureAccuracyUseCase(repository),
            measureArea = MeasureAreaUseCase(repository),
            performCalibration = PerformCalibrationUseCase(repository),
            configureImageEnhancement = ConfigureImageEnhancementUseCase(repository),
            getDeviceInfo = GetDeviceInfoUseCase(repository),
            getBatteryStatus = GetBatteryStatusUseCase(repository),
        )

    @Provides
    @Singleton
    fun provideDataManagementService(
        @ApplicationContext context: Context,
    ): DataManagementService =
        DataManagementService(context).apply {
            initialize()
        }
}
