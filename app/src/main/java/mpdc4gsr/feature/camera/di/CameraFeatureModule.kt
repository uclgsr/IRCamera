package mpdc4gsr.feature.camera.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mpdc4gsr.feature.camera.data.repository.CameraRepositoryImpl
import mpdc4gsr.feature.camera.domain.repository.CameraRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CameraFeatureModule {
    
    @Binds
    @Singleton
    abstract fun bindCameraRepository(
        cameraRepositoryImpl: CameraRepositoryImpl
    ): CameraRepository
}
