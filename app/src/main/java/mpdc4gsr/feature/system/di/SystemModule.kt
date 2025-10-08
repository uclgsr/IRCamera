package mpdc4gsr.feature.system.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import mpdc4gsr.core.di.IoDispatcher
import mpdc4gsr.feature.system.data.repository.SystemRepositoryImpl
import mpdc4gsr.feature.system.domain.repository.SystemRepository
import mpdc4gsr.feature.system.domain.usecase.DiscoverControllersUseCase
import mpdc4gsr.feature.system.domain.usecase.StartRecordingUseCase
import mpdc4gsr.feature.system.domain.usecase.StopRecordingUseCase
import mpdc4gsr.feature.system.domain.usecase.SyncClocksUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SystemModule {

    @Provides
    @Singleton
    fun provideSystemRepository(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): SystemRepository {
        return SystemRepositoryImpl(context, ioDispatcher)
    }

    @Provides
    fun provideStartRecordingUseCase(
        repository: SystemRepository
    ): StartRecordingUseCase {
        return StartRecordingUseCase(repository)
    }

    @Provides
    fun provideStopRecordingUseCase(
        repository: SystemRepository
    ): StopRecordingUseCase {
        return StopRecordingUseCase(repository)
    }

    @Provides
    fun provideDiscoverControllersUseCase(
        repository: SystemRepository
    ): DiscoverControllersUseCase {
        return DiscoverControllersUseCase(repository)
    }

    @Provides
    fun provideSyncClocksUseCase(
        repository: SystemRepository
    ): SyncClocksUseCase {
        return SyncClocksUseCase(repository)
    }
}
