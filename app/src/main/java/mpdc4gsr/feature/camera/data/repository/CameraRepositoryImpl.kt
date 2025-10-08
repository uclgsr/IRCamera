package mpdc4gsr.feature.camera.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import mpdc4gsr.core.data.RgbCameraRecorder
import mpdc4gsr.feature.camera.domain.model.CameraState
import mpdc4gsr.feature.camera.domain.model.FocusMode
import mpdc4gsr.feature.camera.domain.model.WhiteBalance
import mpdc4gsr.feature.camera.domain.repository.CameraRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : CameraRepository {
    
    private val _cameraState = MutableStateFlow(CameraState())
    private val recorder: RgbCameraRecorder by lazy { RgbCameraRecorder(context) }
    
    override fun getCameraState(): Flow<CameraState> = _cameraState
    
    override suspend fun startRecording(outputPath: String): Boolean {
        val success = recorder.startRecording(outputPath)
        if (success) {
            _cameraState.value = _cameraState.value.copy(isRecording = true)
        }
        return success
    }
    
    override suspend fun stopRecording() {
        recorder.stopRecording()
        _cameraState.value = _cameraState.value.copy(isRecording = false)
    }
    
    override suspend fun startPreview(): Boolean {
        _cameraState.value = _cameraState.value.copy(isPreviewing = true)
        return true
    }
    
    override suspend fun stopPreview() {
        _cameraState.value = _cameraState.value.copy(isPreviewing = false)
    }
    
    override suspend fun setFocusMode(mode: FocusMode) {
        _cameraState.value = _cameraState.value.copy(focusMode = mode)
    }
    
    override suspend fun setWhiteBalance(balance: WhiteBalance) {
        _cameraState.value = _cameraState.value.copy(whiteBalance = balance)
    }
    
    override suspend fun setExposureCompensation(value: Int) {
        _cameraState.value = _cameraState.value.copy(exposureCompensation = value)
    }
    
    override suspend fun setZoomLevel(level: Float) {
        _cameraState.value = _cameraState.value.copy(zoomLevel = level)
    }
}
