package mpdc4gsr.core.di

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import mpdc4gsr.core.data.ShimmerDeviceManager
import mpdc4gsr.feature.gsr.data.repository.ShimmerRepositoryImpl
import mpdc4gsr.feature.gsr.data.source.ShimmerDataSource
import mpdc4gsr.feature.gsr.data.source.ShimmerDataSourceImpl
import mpdc4gsr.feature.gsr.domain.repository.ShimmerRepository
import mpdc4gsr.feature.gsr.domain.usecase.ConnectShimmerDeviceUseCase
import mpdc4gsr.feature.gsr.domain.usecase.ScanShimmerDevicesUseCase
import mpdc4gsr.feature.gsr.domain.usecase.StartGSRStreamingUseCase
import mpdc4gsr.feature.gsr.domain.usecase.StopGSRStreamingUseCase
import mpdc4gsr.feature.thermal.data.repository.ThermalRepositoryImpl
import mpdc4gsr.feature.thermal.data.source.ThermalHardwareDataSource
import mpdc4gsr.feature.thermal.domain.repository.ThermalRepository
import mpdc4gsr.feature.thermal.domain.usecase.*

class AppContainerExt(private val context: Context) {
    private fun provideShimmerDeviceManager(lifecycleOwner: LifecycleOwner): ShimmerDeviceManager {
        return ShimmerDeviceManager(context, lifecycleOwner)
    }

    private fun provideShimmerDataSource(lifecycleOwner: LifecycleOwner): ShimmerDataSource {
        return ShimmerDataSourceImpl(provideShimmerDeviceManager(lifecycleOwner))
    }

    fun provideShimmerRepository(lifecycleOwner: LifecycleOwner): ShimmerRepository {
        return ShimmerRepositoryImpl(provideShimmerDataSource(lifecycleOwner))
    }

    fun provideScanShimmerDevicesUseCase(lifecycleOwner: LifecycleOwner): ScanShimmerDevicesUseCase {
        return ScanShimmerDevicesUseCase(provideShimmerRepository(lifecycleOwner))
    }

    fun provideConnectShimmerDeviceUseCase(lifecycleOwner: LifecycleOwner): ConnectShimmerDeviceUseCase {
        return ConnectShimmerDeviceUseCase(provideShimmerRepository(lifecycleOwner))
    }

    fun provideStartGSRStreamingUseCase(lifecycleOwner: LifecycleOwner): StartGSRStreamingUseCase {
        return StartGSRStreamingUseCase(provideShimmerRepository(lifecycleOwner))
    }

    fun provideStopGSRStreamingUseCase(lifecycleOwner: LifecycleOwner): StopGSRStreamingUseCase {
        return StopGSRStreamingUseCase(provideShimmerRepository(lifecycleOwner))
    }

    private fun provideThermalHardwareDataSource(): ThermalHardwareDataSource {
        return mpdc4gsr.feature.thermal.data.source.ThermalHardwareDataSourceImpl(context)
    }

    fun provideThermalRepository(): ThermalRepository {
        return ThermalRepositoryImpl(provideThermalHardwareDataSource())
    }

    fun provideConnectThermalCameraUseCase(): ConnectThermalCameraUseCase {
        return ConnectThermalCameraUseCase(provideThermalRepository())
    }

    fun provideStartThermalStreamingUseCase(): StartThermalStreamingUseCase {
        return StartThermalStreamingUseCase(provideThermalRepository())
    }

    fun provideCaptureThermalSnapshotUseCase(): CaptureThermalSnapshotUseCase {
        return CaptureThermalSnapshotUseCase(provideThermalRepository())
    }

    fun provideStartThermalRecordingUseCase(): StartThermalRecordingUseCase {
        return StartThermalRecordingUseCase(provideThermalRepository())
    }

    fun provideStopThermalRecordingUseCase(): StopThermalRecordingUseCase {
        return StopThermalRecordingUseCase(provideThermalRepository())
    }
}

fun Context.getAppContainerExt(): AppContainerExt {
    return AppContainerExt(applicationContext)
}
