package mpdc4gsr.feature.dashboard.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mpdc4gsr.feature.dashboard.data.repository.GsrRepositoryImpl
import mpdc4gsr.feature.dashboard.data.repository.NetworkRepositoryImpl
import mpdc4gsr.feature.dashboard.data.repository.SessionRepositoryImpl
import mpdc4gsr.feature.dashboard.domain.repository.GsrRepository
import mpdc4gsr.feature.dashboard.domain.repository.NetworkRepository
import mpdc4gsr.feature.dashboard.domain.repository.SessionRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MainModule {
    @Binds
    @Singleton
    abstract fun bindGsrRepository(impl: GsrRepositoryImpl): GsrRepository

    @Binds
    @Singleton
    abstract fun bindNetworkRepository(impl: NetworkRepositoryImpl): NetworkRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository
}

