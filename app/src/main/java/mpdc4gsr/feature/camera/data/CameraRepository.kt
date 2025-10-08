package mpdc4gsr.feature.camera.data

import android.view.TextureView
import kotlinx.coroutines.flow.Flow
import mpdc4gsr.feature.camera.domain.model.CameraCapabilities
import mpdc4gsr.feature.camera.domain.model.CameraMode
import mpdc4gsr.feature.camera.domain.model.RecordingResult

interface CameraRepository {
    suspend fun initialize(cameraId: String = "0"): Result<Unit>
    suspend fun switchMode(mode: CameraMode): Result<Unit>
    suspend fun startRecording(sessionId: String): Result<Unit>
    suspend fun stopRecording(): Result<RecordingResult>
    fun release()
    fun getCurrentMode(): CameraMode
    fun getAvailableModes(): List<CameraMode>
    fun isRecording(): Boolean
    fun getDeviceCapabilities(): CameraCapabilities?
    fun configureStage3Processing(enabled: Boolean)
    fun isStage3ProcessingEnabled(): Boolean
}
