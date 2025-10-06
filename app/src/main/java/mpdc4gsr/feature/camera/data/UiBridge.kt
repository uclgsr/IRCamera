package mpdc4gsr.feature.camera.data
import android.graphics.SurfaceTexture
import android.util.Log
import mpdc4gsr.core.utils.AppLogger
import mpdc4gsr.core.utils.ErrorHandler
import android.view.Surface
import android.view.TextureView
class UiBridge(private val textureView: TextureView) {
    companion object {
        private const val TAG = "UiBridge"
    }
    private var previewSurface: Surface? = null
    private var isTextureAvailable = false
    var onError: ((String) -> Unit)? = null
    var onProgress: ((String) -> Unit)? = null
    var onModeChanged: ((String) -> Unit)? = null
    var onRecordingStateChanged: ((Boolean, String) -> Unit)? = null
    private var isRecording = false
    private var currentMode = "PREVIEW"
    init {
        setupTextureView()
    }
    fun getPreviewSurface(): Surface? = previewSurface
    fun isTextureReady(): Boolean = isTextureAvailable
    fun updateMode(mode: String) {
        currentMode = mode
        AppLogger.i(TAG, "Mode updated: $mode")
        onModeChanged?.invoke(mode)
        onRecordingStateChanged?.invoke(isRecording, mode)
    }
    fun updateRecordingState(recording: Boolean, additionalInfo: String = "") {
        isRecording = recording
        val status = if (recording) "● REC" else "○ STOPPED"
        val fullInfo =
            if (additionalInfo.isNotEmpty()) "$currentMode - $additionalInfo" else currentMode
        AppLogger.i(TAG, "Recording state: $status ($fullInfo)")
        onRecordingStateChanged?.invoke(recording, fullInfo)
        if (recording) {
            reportProgress(" Recording $currentMode mode")
        } else {
            reportProgress(" Recording stopped")
        }
    }
    fun reportError(error: String) {
        AppLogger.e(TAG, "Error: $error")
        onError?.invoke(error)
    }
    fun reportProgress(message: String) {
        AppLogger.i(TAG, "Progress: $message")
        onProgress?.invoke(message)
    }
    fun updatePreviewSize(
        width: Int,
        height: Int,
    ) {
        textureView.surfaceTexture?.setDefaultBufferSize(width, height)
        AppLogger.d(TAG, "Preview size updated: ${width}x$height")
    }
    fun release() {
        previewSurface?.release()
        previewSurface = null
        isTextureAvailable = false
        AppLogger.d(TAG, "UiBridge released")
    }
    private fun setupTextureView() {
        textureView.surfaceTextureListener =
            object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(
                    texture: SurfaceTexture,
                    width: Int,
                    height: Int,
                ) {
                    previewSurface?.release()
                    previewSurface = Surface(texture)
                    isTextureAvailable = true
                    AppLogger.i(TAG, "TextureView surface available: ${width}x$height")
                    reportProgress("Preview surface ready")
                }
                override fun onSurfaceTextureSizeChanged(
                    texture: SurfaceTexture,
                    width: Int,
                    height: Int,
                ) {
                    AppLogger.d(TAG, "TextureView size changed: ${width}x$height")
                }
                override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                    previewSurface?.release()
                    previewSurface = null
                    isTextureAvailable = false
                    AppLogger.i(TAG, "TextureView surface destroyed")
                    return true
                }
                override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {
                }
            }
    }
}
