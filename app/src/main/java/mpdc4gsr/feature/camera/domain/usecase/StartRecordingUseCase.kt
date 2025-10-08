package mpdc4gsr.feature.camera.domain.usecase

import mpdc4gsr.feature.camera.domain.repository.CameraRepository
import javax.inject.Inject

class StartRecordingUseCase @Inject constructor(
    private val cameraRepository: CameraRepository
) {
    suspend operator fun invoke(outputPath: String): Boolean {
        return cameraRepository.startRecording(outputPath)
    }
}
