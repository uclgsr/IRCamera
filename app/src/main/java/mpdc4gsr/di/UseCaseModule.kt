package mpdc4gsr.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import mpdc4gsr.domain.repository.ShimmerRepository
import mpdc4gsr.domain.usecase.ConnectShimmerDeviceUseCase
import mpdc4gsr.domain.usecase.ScanShimmerDevicesUseCase
import mpdc4gsr.domain.usecase.StartGSRStreamingUseCase
import mpdc4gsr.domain.usecase.StopGSRStreamingUseCase
import mpdc4gsr.domain.repository.ThermalRepository
import mpdc4gsr.domain.usecase.CaptureThermalSnapshotUseCase
import mpdc4gsr.domain.usecase.ConnectThermalCameraUseCase
import mpdc4gsr.domain.usecase.StartThermalRecordingUseCase
import mpdc4gsr.domain.usecase.StartThermalStreamingUseCase
import mpdc4gsr.domain.usecase.StopThermalRecordingUseCase

@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {

    @Provides
    @ViewModelScoped
    fun provideScanShimmerDevicesUseCase(
        repository: ShimmerRepository
    ): ScanShimmerDevicesUseCase {
        return ScanShimmerDevicesUseCase(repository)
    }

    @Provides
    @ViewModelScoped
    fun provideConnectShimmerDeviceUseCase(
        repository: ShimmerRepository
    ): ConnectShimmerDeviceUseCase {
        return ConnectShimmerDeviceUseCase(repository)
    }

    @Provides
    @ViewModelScoped
    fun provideStartGSRStreamingUseCase(
        repository: ShimmerRepository
    ): StartGSRStreamingUseCase {
        return StartGSRStreamingUseCase(repository)
    }

    @Provides
    @ViewModelScoped
    fun provideStopGSRStreamingUseCase(
        repository: ShimmerRepository
    ): StopGSRStreamingUseCase {
        return StopGSRStreamingUseCase(repository)
    }

    @Provides
    @ViewModelScoped
    fun provideConnectThermalCameraUseCase(
        repository: ThermalRepository
    ): ConnectThermalCameraUseCase {
        return ConnectThermalCameraUseCase(repository)
    }

    @Provides
    @ViewModelScoped
    fun provideStartThermalStreamingUseCase(
        repository: ThermalRepository
    ): StartThermalStreamingUseCase {
        return StartThermalStreamingUseCase(repository)
    }

    @Provides
    @ViewModelScoped
    fun provideCaptureThermalSnapshotUseCase(
        repository: ThermalRepository
    ): CaptureThermalSnapshotUseCase {
        return CaptureThermalSnapshotUseCase(repository)
    }

    @Provides
    @ViewModelScoped
    fun provideStartThermalRecordingUseCase(
        repository: ThermalRepository
    ): StartThermalRecordingUseCase {
        return StartThermalRecordingUseCase(repository)
    }

    @Provides
    @ViewModelScoped
    fun provideStopThermalRecordingUseCase(
        repository: ThermalRepository
    ): StopThermalRecordingUseCase {
        return StopThermalRecordingUseCase(repository)
    }
}
