package mpdc4gsr.core.di

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import mpdc4gsr.feature.gsr.data.repository.ShimmerRepositoryImpl
import mpdc4gsr.feature.gsr.data.source.ShimmerDataSource
import mpdc4gsr.feature.gsr.data.source.ShimmerDataSourceImpl
import mpdc4gsr.feature.gsr.domain.repository.ShimmerRepository
import mpdc4gsr.feature.gsr.domain.usecase.*
import mpdc4gsr.feature.thermal.data.repository.ThermalRepositoryImpl
import mpdc4gsr.feature.thermal.data.source.TopdonDataSource
import mpdc4gsr.feature.thermal.domain.repository.ThermalRepository
import mpdc4gsr.feature.thermal.domain.usecase.*
import mpdc4gsr.core.data.ShimmerDeviceManager

/**
 * Extended Dependency Injection Container
 * 
 * Provides instances of repositories, data sources, and use cases
 * following Clean Architecture principles.
 * 
 * This is a manual DI implementation that will be replaced with Hilt
 * in a future migration.
 */
class AppContainerExt(private val context: Context) {
    
    // Shimmer SDK Integration
    private fun provideShimmerDeviceManager(lifecycleOwner: LifecycleOwner): ShimmerDeviceManager {
        return ShimmerDeviceManager(context, lifecycleOwner)
    }
    
    private fun provideShimmerDataSource(lifecycleOwner: LifecycleOwner): ShimmerDataSource {
        return ShimmerDataSourceImpl(provideShimmerDeviceManager(lifecycleOwner))
    }
    
    fun provideShimmerRepository(lifecycleOwner: LifecycleOwner): ShimmerRepository {
        return ShimmerRepositoryImpl(provideShimmerDataSource(lifecycleOwner))
    }
    
    // Shimmer Use Cases
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
    
    // Thermal SDK Integration
    private fun provideTopdonDataSource(): TopdonDataSource {
        // TODO: Implement TopdonDataSourceImpl
        throw NotImplementedError("TopdonDataSource implementation pending")
    }
    
    fun provideThermalRepository(): ThermalRepository {
        return ThermalRepositoryImpl(provideTopdonDataSource())
    }
    
    // Thermal Use Cases
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

/**
 * Extension function to get AppContainerExt from Context
 */
fun Context.getAppContainerExt(): AppContainerExt {
    return AppContainerExt(applicationContext)
}
