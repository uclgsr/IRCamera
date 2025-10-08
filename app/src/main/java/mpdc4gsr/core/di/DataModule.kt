package mpdc4gsr.core.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import mpdc4gsr.core.data.ShimmerDeviceManager
import mpdc4gsr.feature.gsr.data.repository.ShimmerRepositoryImpl
import mpdc4gsr.feature.gsr.data.source.ShimmerDataSource
import mpdc4gsr.feature.gsr.data.source.ShimmerDataSourceImpl
import mpdc4gsr.feature.gsr.domain.repository.ShimmerRepository
import mpdc4gsr.feature.thermal.data.repository.ThermalRepositoryImpl
import mpdc4gsr.feature.thermal.data.source.TopdonDataSource
import mpdc4gsr.feature.thermal.data.source.TopdonDataSourceImpl
import mpdc4gsr.feature.thermal.domain.repository.ThermalRepository

@Module
@InstallIn(ViewModelComponent::class)
object DataModule {

    @Provides
    @ViewModelScoped
    fun provideShimmerDeviceManager(
        @ApplicationContext context: Context
    ): ShimmerDeviceManager {
        return ShimmerDeviceManager(context, null)
    }

    @Provides
    @ViewModelScoped
    fun provideShimmerDataSource(
        shimmerDeviceManager: ShimmerDeviceManager
    ): ShimmerDataSource {
        return ShimmerDataSourceImpl(shimmerDeviceManager)
    }

    @Provides
    @ViewModelScoped
    fun provideShimmerRepository(
        shimmerDataSource: ShimmerDataSource
    ): ShimmerRepository {
        return ShimmerRepositoryImpl(shimmerDataSource)
    }

    @Provides
    @ViewModelScoped
    fun provideTopdonDataSource(
        @ApplicationContext context: Context
    ): TopdonDataSource {
        return TopdonDataSourceImpl(context)
    }

    @Provides
    @ViewModelScoped
    fun provideThermalRepository(
        topdonDataSource: TopdonDataSource
    ): ThermalRepository {
        return ThermalRepositoryImpl(topdonDataSource)
    }
}
