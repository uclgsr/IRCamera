// Merged ALL .kt and .java files from the 'app\src\main\java\mpdc4gsr\core\di' directory and its subdirectories.
// Total files: 2 | Generated on: 2025-10-08 01:42:32


// ===== FROM: app\src\main\java\mpdc4gsr\core\di\AppContainer.kt =====

package mpdc4gsr.core.di

import android.content.Context

interface AppContainer {
    val context: Context
}

class DefaultAppContainer(
    override val context: Context
) : AppContainer


// ===== FROM: app\src\main\java\mpdc4gsr\core\di\AppContainerExt.kt =====

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
import mpdc4gsr.feature.thermal.data.source.TopdonDataSource
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

    private fun provideTopdonDataSource(): TopdonDataSource {
        return mpdc4gsr.feature.thermal.data.source.TopdonDataSourceImpl(context)
    }

    fun provideThermalRepository(): ThermalRepository {
        return ThermalRepositoryImpl(provideTopdonDataSource())
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