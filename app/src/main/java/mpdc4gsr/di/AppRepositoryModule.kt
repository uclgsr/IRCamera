package mpdc4gsr.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mpdc4gsr.feature.device.data.repository.DiagnosticsRepositoryImpl
import mpdc4gsr.feature.device.domain.repository.DiagnosticsRepository
import mpdc4gsr.feature.main.data.repository.GsrRepositoryImpl
import mpdc4gsr.feature.main.data.repository.NetworkRepositoryImpl
import mpdc4gsr.feature.main.data.repository.SessionRepositoryImpl
import mpdc4gsr.feature.main.domain.repository.GsrRepository
import mpdc4gsr.feature.main.domain.repository.NetworkRepository
import mpdc4gsr.feature.main.domain.repository.SessionRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppRepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindSessionRepository(
        sessionRepositoryImpl: SessionRepositoryImpl
    ): SessionRepository
    
    @Binds
    @Singleton
    abstract fun bindNetworkRepository(
        networkRepositoryImpl: NetworkRepositoryImpl
    ): NetworkRepository
    
    @Binds
    @Singleton
    abstract fun bindGsrRepository(
        gsrRepositoryImpl: GsrRepositoryImpl
    ): GsrRepository
    
    @Binds
    @Singleton
    abstract fun bindDiagnosticsRepository(
        diagnosticsRepositoryImpl: DiagnosticsRepositoryImpl
    ): DiagnosticsRepository
}
