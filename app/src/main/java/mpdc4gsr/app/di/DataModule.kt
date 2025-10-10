package mpdc4gsr.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import mpdc4gsr.feature.capture.thermal.domain.repository.ThermalRepository

@Module
@InstallIn(ViewModelComponent::class)
object DataModule {
    @Provides
    @ViewModelScoped
    fun provideThermalRepository(
        repository: ThermalRepository,
    ): ThermalRepository = repository
}
