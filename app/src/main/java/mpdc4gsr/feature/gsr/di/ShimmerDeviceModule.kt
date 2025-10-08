package mpdc4gsr.feature.gsr.di

import android.app.Application
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import mpdc4gsr.core.data.ShimmerDeviceManager
import mpdc4gsr.feature.gsr.data.repository.ShimmerRepositoryImpl
import mpdc4gsr.feature.gsr.data.source.ShimmerDataSourceImpl
import mpdc4gsr.feature.gsr.domain.repository.ShimmerRepository
import mpdc4gsr.feature.gsr.domain.usecase.*

@Module
@InstallIn(ViewModelComponent::class)
abstract class ShimmerDeviceModule {

    @Binds
    @ViewModelScoped
    abstract fun bindShimmerRepository(
        shimmerRepositoryImpl: ShimmerRepositoryImpl
    ): ShimmerRepository

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
            shimmerRepository: ShimmerRepository
        ): ScanShimmerDevicesUseCase {
            return ScanShimmerDevicesUseCase(shimmerRepository)
        }

        @Provides
        @ViewModelScoped
        fun provideConnectDeviceUseCase(
            shimmerRepository: ShimmerRepository
        ): ConnectShimmerDeviceUseCase {
            return ConnectShimmerDeviceUseCase(shimmerRepository)
        }

        @Provides
        @ViewModelScoped
        fun provideDisconnectDeviceUseCase(
            shimmerRepository: ShimmerRepository
        ): DisconnectShimmerDeviceUseCase {
            return DisconnectShimmerDeviceUseCase(shimmerRepository)
        }

        @Provides
        @ViewModelScoped
        fun provideGetBatteryLevelUseCase(
            shimmerRepository: ShimmerRepository
        ): GetDeviceBatteryUseCase {
            return GetDeviceBatteryUseCase(shimmerRepository)
        }

        @Provides
        @ViewModelScoped
        fun provideCheckConnectionUseCase(
            shimmerRepository: ShimmerRepository
        ): CheckDeviceConnectionUseCase {
            return CheckDeviceConnectionUseCase(shimmerRepository)
        }
    }
}
