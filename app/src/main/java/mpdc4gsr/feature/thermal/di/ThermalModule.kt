package mpdc4gsr.feature.thermal.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mpdc4gsr.feature.thermal.data.repository.ThermalRepositoryImpl
import mpdc4gsr.feature.thermal.data.source.TopdonDataSource
import mpdc4gsr.feature.thermal.data.source.TopdonDataSourceImpl
import mpdc4gsr.feature.thermal.domain.repository.ThermalRepository
import mpdc4gsr.feature.thermal.domain.usecase.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ThermalModule {

    @Provides
    @Singleton
    fun provideTopdonDataSource(
        @ApplicationContext context: Context
    ): TopdonDataSource {
        return TopdonDataSourceImpl(context)
    }

    @Provides
    @Singleton
    fun provideThermalRepository(
        dataSource: TopdonDataSource
    ): ThermalRepository {
        return ThermalRepositoryImpl(dataSource)
    }

    @Provides
    @Singleton
    fun provideThermalUseCases(
        repository: ThermalRepository
    ): ThermalUseCases {
        return ThermalUseCases(
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
    fun provideThermalSDKUseCases(
        repository: ThermalRepository
    ): mpdc4gsr.feature.thermal.domain.ThermalSDKUseCases {
        return mpdc4gsr.feature.thermal.domain.ThermalSDKUseCases(
            setColorPalette = mpdc4gsr.feature.thermal.domain.SetColorPaletteUseCase(repository),
            setAgcMode = mpdc4gsr.feature.thermal.domain.SetAgcModeUseCase(repository),
            configureAccuracy = mpdc4gsr.feature.thermal.domain.ConfigureAccuracyUseCase(repository),
            measureArea = mpdc4gsr.feature.thermal.domain.MeasureAreaUseCase(repository),
            performCalibration = mpdc4gsr.feature.thermal.domain.PerformCalibrationUseCase(repository),
            configureImageEnhancement = mpdc4gsr.feature.thermal.domain.ConfigureImageEnhancementUseCase(repository),
            getDeviceInfo = mpdc4gsr.feature.thermal.domain.GetDeviceInfoUseCase(repository),
            getBatteryStatus = mpdc4gsr.feature.thermal.domain.GetBatteryStatusUseCase(repository)
        )
    }
}

data class ThermalUseCases(
    val connectCamera: ConnectThermalCameraUseCase,
    val disconnectCamera: DisconnectThermalCameraUseCase,
    val startStreaming: StartThermalStreamingUseCase,
    val stopStreaming: StopThermalStreamingUseCase,
    val captureSnapshot: CaptureThermalSnapshotUseCase,
    val startRecording: StartThermalRecordingUseCase,
    val stopRecording: StopThermalRecordingUseCase,
    val setTemperatureRange: SetTemperatureRangeUseCase,
    val checkConnection: CheckCameraConnectionUseCase
)
