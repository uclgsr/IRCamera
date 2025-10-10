package com.mpdc4gsr.module.thermalunified.tools

import android.graphics.Bitmap
import android.os.Handler
import com.infisense.usbir.view.CameraView
import com.mpdc4gsr.libunified.ui.camera.CameraPreviewManager as LibUnifiedCameraPreviewManager

class CameraPreviewManager private constructor() {
    private val delegate = LibUnifiedCameraPreviewManager.getInstance()

    companion object {
        private const val TAG = "ThermalCameraPreviewManager"

        @Volatile
        private var INSTANCE: CameraPreviewManager? = null

        fun getInstance(): CameraPreviewManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: CameraPreviewManager().also { INSTANCE = it }
            }
    }

    fun scaledBitmap(cached: Boolean = false): Bitmap? = delegate.scaledBitmap(cached)

    fun getCameraBitmap(): Bitmap? = delegate.getCameraBitmap()

    fun updateCameraBitmap(bitmap: Bitmap?) {
        delegate.updateCameraBitmap(bitmap)
    }

    fun startPreview() {
        delegate.startPreview()
    }

    fun stopPreview() {
        delegate.stopPreview()
    }

    fun pausePreview() {
        delegate.pausePreview()
    }

    fun resumePreview() {
        delegate.resumePreview()
    }

    fun release() {
        delegate.release()
    }

    fun setImageRotation(rotation: Int) {
        delegate.setImageRotation(rotation)
    }

    fun getImageRotation(): Int = delegate.getImageRotation()

    fun setTemperatureLimits(
        min: Float,
        max: Float,
    ) {
        delegate.setTemperatureLimits(min, max)
    }

    fun getMinTemperature(): Float = delegate.getMinTemperature()

    fun getMaxTemperature(): Float = delegate.getMaxTemperature()

    fun getPseudocolorMode(): Int = delegate.getPseudocolorMode()

    fun applyPseudocolor(bitmap: Bitmap): Bitmap = delegate.applyPseudocolor(bitmap)

    fun init(
        cameraView: CameraView,
        handler: Handler,
    ) {
        delegate.init(cameraView, handler)
    }
}
