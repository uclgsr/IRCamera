package mpdc4gsr.feature.gsr.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import mpdc4gsr.core.sensors.gsr.GsrDeviceManager
import mpdc4gsr.feature.gsr.data.repository.GSRDeviceRepositoryImpl
import mpdc4gsr.feature.gsr.data.source.GSRDeviceDataSource
import mpdc4gsr.feature.gsr.data.source.GSRDeviceDataSourceImpl
import mpdc4gsr.feature.gsr.domain.repository.GSRDeviceRepository
import mpdc4gsr.feature.gsr.domain.usecase.CheckGSRDeviceConnectionUseCase
import mpdc4gsr.feature.gsr.domain.usecase.ConnectGSRDeviceUseCase
import mpdc4gsr.feature.gsr.domain.usecase.DisconnectGSRDeviceUseCase
import mpdc4gsr.feature.gsr.domain.usecase.GetGSRDeviceBatteryUseCase
import mpdc4gsr.feature.gsr.domain.usecase.ScanGSRDevicesUseCase

@Module
@InstallIn(ViewModelComponent::class)
abstract class GSRDeviceModule {
    @Binds
    @ViewModelScoped
    abstract fun bindGSRDeviceRepository(gsrDeviceRepositoryImpl: GSRDeviceRepositoryImpl): GSRDeviceRepository

    @Binds
    @ViewModelScoped
    abstract fun bindGSRDeviceDataSource(gsrDeviceDataSourceImpl: GSRDeviceDataSourceImpl): GSRDeviceDataSource

    companion object {
        @Provides
        @ViewModelScoped
        fun provideGsrDeviceManager(
            @ApplicationContext context: Context,
        ): GsrDeviceManager = GsrDeviceManager(context)

        @Provides
        @ViewModelScoped
        fun provideScanDevicesUseCase(gsrDeviceRepository: GSRDeviceRepository): ScanGSRDevicesUseCase =
            ScanGSRDevicesUseCase(gsrDeviceRepository)

        @Provides
        @ViewModelScoped
        fun provideConnectDeviceUseCase(gsrDeviceRepository: GSRDeviceRepository): ConnectGSRDeviceUseCase =
            ConnectGSRDeviceUseCase(gsrDeviceRepository)

        @Provides
        @ViewModelScoped
        fun provideDisconnectDeviceUseCase(gsrDeviceRepository: GSRDeviceRepository): DisconnectGSRDeviceUseCase =
            DisconnectGSRDeviceUseCase(gsrDeviceRepository)

        @Provides
        @ViewModelScoped
        fun provideGetBatteryLevelUseCase(gsrDeviceRepository: GSRDeviceRepository): GetGSRDeviceBatteryUseCase =
            GetGSRDeviceBatteryUseCase(gsrDeviceRepository)

        @Provides
        @ViewModelScoped
        fun provideCheckConnectionUseCase(gsrDeviceRepository: GSRDeviceRepository): CheckGSRDeviceConnectionUseCase =
            CheckGSRDeviceConnectionUseCase(gsrDeviceRepository)
    }
}
