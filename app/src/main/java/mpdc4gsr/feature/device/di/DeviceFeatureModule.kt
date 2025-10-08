package mpdc4gsr.feature.device.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mpdc4gsr.feature.device.data.repository.DiagnosticsRepositoryImpl
import mpdc4gsr.feature.device.domain.repository.DiagnosticsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DeviceFeatureModule {
    
    @Binds
    @Singleton
    abstract fun bindDiagnosticsRepository(
        diagnosticsRepositoryImpl: DiagnosticsRepositoryImpl
    ): DiagnosticsRepository
}
