package mpdc4gsr.feature.gsr.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import mpdc4gsr.core.data.UnifiedGSRRecorder
import mpdc4gsr.core.di.IoDispatcher
import mpdc4gsr.feature.gsr.data.GSRSettingsRepository
import mpdc4gsr.feature.gsr.data.source.ShimmerDataSourceImpl
import mpdc4gsr.feature.gsr.domain.repository.ShimmerRepository
import mpdc4gsr.feature.gsr.domain.usecase.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GSRModule {

    @Provides
    @Singleton
    fun provideGSRSettingsRepository(
        @ApplicationContext context: Context
    ): GSRSettingsRepository {
        return GSRSettingsRepository(context)
    }

    @Provides
    @Singleton
    fun provideUnifiedGSRRecorder(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): UnifiedGSRRecorder {
        return UnifiedGSRRecorder(context, samplingRateHz = 128.0)
    }

    @Provides
    @Singleton
    fun provideShimmerRepository(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): ShimmerRepository {
        return ShimmerDataSourceImpl(context, ioDispatcher)
    }

    @Provides
    fun provideScanShimmerDevicesUseCase(
        repository: ShimmerRepository
    ): ScanShimmerDevicesUseCase {
        return ScanShimmerDevicesUseCase(repository)
    }

    @Provides
    fun provideConnectShimmerDeviceUseCase(
        repository: ShimmerRepository
    ): ConnectShimmerDeviceUseCase {
        return ConnectShimmerDeviceUseCase(repository)
    }

    @Provides
    fun provideDisconnectShimmerDeviceUseCase(
        repository: ShimmerRepository
    ): DisconnectShimmerDeviceUseCase {
        return DisconnectShimmerDeviceUseCase(repository)
    }

    @Provides
    fun provideStartGSRStreamingUseCase(
        repository: ShimmerRepository
    ): StartGSRStreamingUseCase {
        return StartGSRStreamingUseCase(repository)
    }

    @Provides
    fun provideStopGSRStreamingUseCase(
        repository: ShimmerRepository
    ): StopGSRStreamingUseCase {
        return StopGSRStreamingUseCase(repository)
    }

    @Provides
    fun provideCheckDeviceConnectionUseCase(
        repository: ShimmerRepository
    ): CheckDeviceConnectionUseCase {
        return CheckDeviceConnectionUseCase(repository)
    }

    @Provides
    fun provideGetDeviceBatteryUseCase(
        repository: ShimmerRepository
    ): GetDeviceBatteryUseCase {
        return GetDeviceBatteryUseCase(repository)
    }
}
