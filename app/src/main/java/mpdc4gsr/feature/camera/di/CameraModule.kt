package mpdc4gsr.feature.camera.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mpdc4gsr.feature.camera.data.CameraRepository
import mpdc4gsr.feature.camera.data.CameraRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CameraModule {

    @Provides
    @Singleton
    fun provideCameraRepository(
        @ApplicationContext context: Context
    ): CameraRepository {
        return CameraRepositoryImpl(context)
    }
}
