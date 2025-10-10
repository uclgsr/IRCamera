package mpdc4gsr.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import mpdc4gsr.feature.capture.thermal.domain.repository.ThermalRepository
import mpdc4gsr.feature.capture.thermal.domain.usecase.CaptureThermalSnapshotUseCase
import mpdc4gsr.feature.capture.thermal.domain.usecase.ConnectThermalCameraUseCase
import mpdc4gsr.feature.capture.thermal.domain.usecase.StartThermalRecordingUseCase
import mpdc4gsr.feature.capture.thermal.domain.usecase.StartThermalStreamingUseCase
import mpdc4gsr.feature.capture.thermal.domain.usecase.StopThermalRecordingUseCase

@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {
    @Provides
    @ViewModelScoped
    fun provideConnectThermalCameraUseCase(repository: ThermalRepository): ConnectThermalCameraUseCase =
        ConnectThermalCameraUseCase(repository)

    @Provides
    @ViewModelScoped
    fun provideStartThermalStreamingUseCase(repository: ThermalRepository): StartThermalStreamingUseCase =
        StartThermalStreamingUseCase(repository)

    @Provides
    @ViewModelScoped
    fun provideCaptureThermalSnapshotUseCase(repository: ThermalRepository): CaptureThermalSnapshotUseCase =
        CaptureThermalSnapshotUseCase(repository)

    @Provides
    @ViewModelScoped
    fun provideStartThermalRecordingUseCase(repository: ThermalRepository): StartThermalRecordingUseCase =
        StartThermalRecordingUseCase(repository)

    @Provides
    @ViewModelScoped
    fun provideStopThermalRecordingUseCase(repository: ThermalRepository): StopThermalRecordingUseCase =
        StopThermalRecordingUseCase(repository)
}

