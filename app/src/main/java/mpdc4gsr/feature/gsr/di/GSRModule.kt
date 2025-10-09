package mpdc4gsr.feature.gsr.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mpdc4gsr.core.data.utils.SessionDirectoryManager
import mpdc4gsr.feature.gsr.data.GSRSettingsRepository
import mpdc4gsr.core.session.SessionManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GSRModule {

    @Provides
    @Singleton
    fun provideGSRSettingsRepository(
        @ApplicationContext context: Context
    ): GSRSettingsRepository {
        return GSRSettingsRepository(context)
    }

    @Provides
    @Singleton
    fun provideSessionManager(
        @ApplicationContext context: Context
    ): SessionManager {
        return SessionManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideSessionDirectoryManager(
        @ApplicationContext context: Context
    ): SessionDirectoryManager {
        return SessionDirectoryManager(context)
    }
}
