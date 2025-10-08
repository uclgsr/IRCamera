package mpdc4gsr.feature.thermal.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mpdc4gsr.feature.thermal.data.repository.ThermalRepositoryImpl
import mpdc4gsr.feature.thermal.data.source.ThermalHardwareDataSource
import mpdc4gsr.feature.thermal.data.source.ThermalHardwareDataSourceImpl
import mpdc4gsr.feature.thermal.domain.repository.ThermalRepository
import mpdc4gsr.feature.thermal.domain.usecase.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ThermalModule {

    @Provides
    @Singleton
    fun provideThermalHardwareDataSource(
        @ApplicationContext context: Context
    ): ThermalHardwareDataSource {
        return ThermalHardwareDataSourceImpl(context)
    }

    @Provides
    @Singleton
    fun provideThermalRepository(
        dataSource: ThermalHardwareDataSource
    ): ThermalRepository {
        return ThermalRepositoryImpl(dataSource)
    }

    @Provides
    @Singleton
    fun provideThermalCoreUseCases(
        repository: ThermalRepository
    ): ThermalCoreUseCases {
        return ThermalCoreUseCases(
            connectCamera = ConnectThermalCameraUseCase(repository),
            disconnectCamera = DisconnectThermalCameraUseCase(repository),
            startStreaming = StartThermalStreamingUseCase(repository),
            stopStreaming = StopThermalStreamingUseCase(repository),
            captureSnapshot = CaptureThermalSnapshotUseCase(repository),
            startRecording = StartThermalRecordingUseCase(repository),
            stopRecording = StopThermalRecordingUseCase(repository),
            setTemperatureRange = SetTemperatureRangeUseCase(repository),
            checkConnection = CheckCameraConnectionUseCase(repository)
        )
    }

    @Provides
    @Singleton
    fun provideThermalHardwareUseCases(
        repository: ThermalRepository
    ): ThermalHardwareUseCases {
        return ThermalHardwareUseCases(
            setColorPalette = SetColorPaletteUseCase(repository),
            setAgcMode = SetAgcModeUseCase(repository),
            configureAccuracy = ConfigureAccuracyUseCase(repository),
            measureArea = MeasureAreaUseCase(repository),
            performCalibration = PerformCalibrationUseCase(repository),
            configureImageEnhancement = ConfigureImageEnhancementUseCase(repository),
            getDeviceInfo = GetDeviceInfoUseCase(repository),
            getBatteryStatus = GetBatteryStatusUseCase(repository)
        )
    }
}
