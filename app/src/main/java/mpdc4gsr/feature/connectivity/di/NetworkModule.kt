package mpdc4gsr.feature.connectivity.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mpdc4gsr.feature.connectivity.data.NetworkClient
import mpdc4gsr.feature.connectivity.data.datasource.NetworkDataSource
import mpdc4gsr.feature.connectivity.data.datasource.NetworkDataSourceImpl
import mpdc4gsr.feature.connectivity.data.repository.NetworkRepositoryImpl
import mpdc4gsr.feature.connectivity.domain.repository.NetworkRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideNetworkClient(
        @ApplicationContext context: Context
    ): NetworkClient {
        return NetworkClient(context).apply {
            initialize()
        }
    }

    @Provides
    @Singleton
    fun provideNetworkDataSource(
        networkClient: NetworkClient
    ): NetworkDataSource {
        return NetworkDataSourceImpl(networkClient)
    }

    @Provides
    @Singleton
    fun provideNetworkRepository(
        dataSource: NetworkDataSource
    ): NetworkRepository {
        return NetworkRepositoryImpl(dataSource)
    }
}

