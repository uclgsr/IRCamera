package mpdc4gsr.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mpdc4gsr.feature.network.data.NetworkClient
import mpdc4gsr.feature.network.data.datasource.NetworkDataSource
import mpdc4gsr.feature.network.data.datasource.NetworkDataSourceImpl
import mpdc4gsr.feature.network.data.repository.NetworkRepositoryImpl
import mpdc4gsr.feature.network.domain.repository.NetworkRepository
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
