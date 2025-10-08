package mpdc4gsr.feature.camera.domain.repository

import kotlinx.coroutines.flow.Flow
import mpdc4gsr.feature.camera.domain.model.CameraState

interface CameraRepository {
    fun getCameraState(): Flow<CameraState>
    suspend fun startRecording(outputPath: String): Boolean
    suspend fun stopRecording()
    suspend fun startPreview(): Boolean
    suspend fun stopPreview()
    suspend fun setFocusMode(mode: mpdc4gsr.feature.camera.domain.model.FocusMode)
    suspend fun setWhiteBalance(balance: mpdc4gsr.feature.camera.domain.model.WhiteBalance)
    suspend fun setExposureCompensation(value: Int)
    suspend fun setZoomLevel(level: Float)
}
