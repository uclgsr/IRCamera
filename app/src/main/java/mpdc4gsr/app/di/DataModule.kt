package mpdc4gsr.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import mpdc4gsr.core.hardware.gsr.GsrDeviceManager
import mpdc4gsr.core.hardware.gsr.data.GsrDataRepository
import mpdc4gsr.feature.capture.thermal.data.repository.ThermalRepositoryImpl
import mpdc4gsr.feature.capture.thermal.data.source.TopdonDataSource
import mpdc4gsr.feature.capture.thermal.data.source.TopdonDataSourceImpl
import mpdc4gsr.feature.capture.thermal.domain.repository.ThermalRepository

@Module
@InstallIn(ViewModelComponent::class)
object DataModule {
    @Provides
    @ViewModelScoped
    fun provideGsrDeviceManager(
        @ApplicationContext context: Context,
    ): GsrDeviceManager = GsrDeviceManager(context, null)

    @Provides
    @ViewModelScoped
    fun provideGsrDataRepository(): GsrDataRepository = GsrDataRepository()

    @Provides
    @ViewModelScoped
    fun provideTopdonDataSource(
        @ApplicationContext context: Context,
    ): TopdonDataSource = TopdonDataSourceImpl(context)

    @Provides
    @ViewModelScoped
    fun provideThermalRepository(topdonDataSource: TopdonDataSource): ThermalRepository =
        ThermalRepositoryImpl(topdonDataSource)
}

