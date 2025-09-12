package com.topdon.tc001.camera.core

import android.graphics.SurfaceTexture
import android.util.Log
import android.view.Surface
import android.view.TextureView

/**
 * UiBridge - pushes preview Surface and exposes errors/progress
 * Clean interface between camera core and UI components
 */
class UiBridge(private val textureView: TextureView) {
    
    companion object {
        private const val TAG = "UiBridge"
    }
    
    private var previewSurface: Surface? = null
    private var isTextureAvailable = false
    
    // Callbacks to UI
    var onError: ((String) -> Unit)? = null
    var onProgress: ((String) -> Unit)? = null
    var onModeChanged: ((String) -> Unit)? = null
    
    init {
        setupTextureView()
    }
    
    /**
     * Get preview surface for camera session
     */
    fun getPreviewSurface(): Surface? = previewSurface
    
    /**
     * Check if texture is ready
     */
    fun isTextureReady(): Boolean = isTextureAvailable
    
    /**
     * Update UI with mode change
     */
    fun updateMode(mode: String) {
        Log.i(TAG, "Mode updated: $mode")
        onModeChanged?.invoke(mode)
    }
    
    /**
     * Report error to UI
     */
    fun reportError(error: String) {
        Log.e(TAG, "Error: $error")
        onError?.invoke(error)
    }
    
    /**
     * Report progress to UI
     */
    fun reportProgress(message: String) {
        Log.i(TAG, "Progress: $message")
        onProgress?.invoke(message)
    }
    
    /**
     * Update preview size
     */
    fun updatePreviewSize(width: Int, height: Int) {
        textureView.surfaceTexture?.setDefaultBufferSize(width, height)
        Log.d(TAG, "Preview size updated: ${width}x${height}")
    }
    
    /**
     * Release resources
     */
    fun release() {
        previewSurface?.release()
        previewSurface = null
        isTextureAvailable = false
        Log.d(TAG, "UiBridge released")
    }
    
    private fun setupTextureView() {
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                texture: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                previewSurface?.release()
                previewSurface = Surface(texture)
                isTextureAvailable = true
                
                Log.i(TAG, "TextureView surface available: ${width}x${height}")
                reportProgress("Preview surface ready")
            }
            
            override fun onSurfaceTextureSizeChanged(
                texture: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                Log.d(TAG, "TextureView size changed: ${width}x${height}")
            }
            
            override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                previewSurface?.release()
                previewSurface = null
                isTextureAvailable = false
                
                Log.i(TAG, "TextureView surface destroyed")
                return true
            }
            
            override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {
                // Frame updated - high frequency, no logging
            }
        }
    }
}