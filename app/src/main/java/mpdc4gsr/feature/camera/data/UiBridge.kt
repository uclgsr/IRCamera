package mpdc4gsr.feature.camera.data

import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView

class UiBridge(private val textureView: TextureView) {
    companion object {
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
        onModeChanged?.invoke(mode)
        onRecordingStateChanged?.invoke(isRecording, mode)
    }

    fun updateRecordingState(recording: Boolean, additionalInfo: String = "") {
        isRecording = recording
        val status = if (recording) "● REC" else "○ STOPPED"
        val fullInfo =
            if (additionalInfo.isNotEmpty()) "$currentMode - $additionalInfo" else currentMode
        onRecordingStateChanged?.invoke(recording, fullInfo)
        if (recording) {
            reportProgress(" Recording $currentMode mode")
        } else {
            reportProgress(" Recording stopped")
        }
    }

    fun reportError(error: String) {
        onError?.invoke(error)
    }

    fun reportProgress(message: String) {
        onProgress?.invoke(message)
    }

    fun updatePreviewSize(
        width: Int,
        height: Int,
    ) {
        textureView.surfaceTexture?.setDefaultBufferSize(width, height)
    }

    fun release() {
        previewSurface?.release()
        previewSurface = null
        isTextureAvailable = false
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
                    reportProgress("Preview surface ready")
                }

                override fun onSurfaceTextureSizeChanged(
                    texture: SurfaceTexture,
                    width: Int,
                    height: Int,
                ) {
                }

                override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                    previewSurface?.release()
                    previewSurface = null
                    isTextureAvailable = false
                    return true
                }

                override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {
                }
            }
    }
}
