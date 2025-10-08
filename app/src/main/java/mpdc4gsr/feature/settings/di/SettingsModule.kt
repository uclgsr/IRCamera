package mpdc4gsr.feature.settings.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mpdc4gsr.feature.settings.data.RecordingSettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {
    
    @Provides
    @Singleton
    fun provideRecordingSettingsRepository(
        @ApplicationContext context: Context
    ): RecordingSettingsRepository {
        return RecordingSettingsRepository(context)
    }
}
