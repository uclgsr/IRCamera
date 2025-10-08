package mpdc4gsr.feature.gsr.di

import android.app.Application
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import mpdc4gsr.core.data.ShimmerDeviceManager
import mpdc4gsr.feature.gsr.data.repository.GSRDeviceRepositoryImpl
import mpdc4gsr.feature.gsr.data.source.ShimmerDataSourceImpl
import mpdc4gsr.feature.gsr.domain.repository.GSRDeviceRepository
import mpdc4gsr.feature.gsr.domain.usecase.*

@Module
@InstallIn(ViewModelComponent::class)
abstract class GSRDeviceModule {

    @Binds
    @ViewModelScoped
    abstract fun bindGSRDeviceRepository(
        gsrDeviceRepositoryImpl: GSRDeviceRepositoryImpl
    ): GSRDeviceRepository

    companion object {
        @Provides
        @ViewModelScoped
        fun provideShimmerDeviceManager(
            application: Application
        ): ShimmerDeviceManager {
            return ShimmerDeviceManager(application, null)
        }

        @Provides
        @ViewModelScoped
        fun provideShimmerDataSource(
            shimmerDeviceManager: ShimmerDeviceManager
        ): ShimmerDataSourceImpl {
            return ShimmerDataSourceImpl(shimmerDeviceManager)
        }
    }

        @Provides
        @ViewModelScoped
        fun provideScanDevicesUseCase(
            gsrDeviceRepository: GSRDeviceRepository
        ): ScanGSRDevicesUseCase {
            return ScanGSRDevicesUseCase(gsrDeviceRepository)
        }

        @Provides
        @ViewModelScoped
        fun provideConnectDeviceUseCase(
            gsrDeviceRepository: GSRDeviceRepository
        ): ConnectGSRDeviceUseCase {
            return ConnectGSRDeviceUseCase(gsrDeviceRepository)
        }

        @Provides
        @ViewModelScoped
        fun provideDisconnectDeviceUseCase(
            gsrDeviceRepository: GSRDeviceRepository
        ): DisconnectGSRDeviceUseCase {
            return DisconnectGSRDeviceUseCase(gsrDeviceRepository)
        }

        @Provides
        @ViewModelScoped
        fun provideGetBatteryLevelUseCase(
            gsrDeviceRepository: GSRDeviceRepository
        ): GetGSRDeviceBatteryUseCase {
            return GetGSRDeviceBatteryUseCase(gsrDeviceRepository)
        }

        @Provides
        @ViewModelScoped
        fun provideCheckConnectionUseCase(
            gsrDeviceRepository: GSRDeviceRepository
        ): CheckGSRDeviceConnectionUseCase {
            return CheckGSRDeviceConnectionUseCase(gsrDeviceRepository)
        }
    }
}
