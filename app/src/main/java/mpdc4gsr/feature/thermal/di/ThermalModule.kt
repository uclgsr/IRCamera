package mpdc4gsr.feature.thermal.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import mpdc4gsr.core.di.IoDispatcher
import mpdc4gsr.feature.thermal.data.source.TopdonDataSourceImpl
import mpdc4gsr.feature.thermal.domain.repository.ThermalRepository
import mpdc4gsr.feature.thermal.domain.usecase.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ThermalModule {

    @Provides
    @Singleton
    fun provideThermalRepository(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): ThermalRepository {
        return TopdonDataSourceImpl(context, ioDispatcher)
    }

    @Provides
    fun provideConnectThermalCameraUseCase(
        repository: ThermalRepository
    ): ConnectThermalCameraUseCase {
        return ConnectThermalCameraUseCase(repository)
    }

    @Provides
    fun provideDisconnectThermalCameraUseCase(
        repository: ThermalRepository
    ): DisconnectThermalCameraUseCase {
        return DisconnectThermalCameraUseCase(repository)
    }

    @Provides
    fun provideStartThermalStreamingUseCase(
        repository: ThermalRepository
    ): StartThermalStreamingUseCase {
        return StartThermalStreamingUseCase(repository)
    }

    @Provides
    fun provideStopThermalStreamingUseCase(
        repository: ThermalRepository
    ): StopThermalStreamingUseCase {
        return StopThermalStreamingUseCase(repository)
    }

    @Provides
    fun provideCaptureThermalSnapshotUseCase(
        repository: ThermalRepository
    ): CaptureThermalSnapshotUseCase {
        return CaptureThermalSnapshotUseCase(repository)
    }

    @Provides
    fun provideStartThermalRecordingUseCase(
        repository: ThermalRepository
    ): StartThermalRecordingUseCase {
        return StartThermalRecordingUseCase(repository)
    }

    @Provides
    fun provideStopThermalRecordingUseCase(
        repository: ThermalRepository
    ): StopThermalRecordingUseCase {
        return StopThermalRecordingUseCase(repository)
    }

    @Provides
    fun provideSetTemperatureRangeUseCase(
        repository: ThermalRepository
    ): SetTemperatureRangeUseCase {
        return SetTemperatureRangeUseCase(repository)
    }

    @Provides
    fun provideCheckCameraConnectionUseCase(
        repository: ThermalRepository
    ): CheckCameraConnectionUseCase {
        return CheckCameraConnectionUseCase(repository)
    }
}
