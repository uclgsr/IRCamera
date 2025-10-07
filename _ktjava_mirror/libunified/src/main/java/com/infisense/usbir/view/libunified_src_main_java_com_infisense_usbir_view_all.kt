// Merged .kt under 'libunified\src\main\java\com\infisense\usbir\view' subtree
// Files: 1; Generated 2025-10-07 23:07:48


// ===== libunified\src\main\java\com\infisense\usbir\view\CameraView.kt =====

package com.infisense.usbir.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import com.elvishew.xlog.XLog
import com.energy.iruvc.utils.SynchronizedBitmap

class CameraView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val TAG = "InfisenseCameraView"

    // Core properties for thermal camera display
    var bitmap: Bitmap? = null
        set(value) {
            field = value
            invalidate()
        }
    var isOpenAmplify: Boolean = false
        set(value) {
            field = value
            invalidate()
        }
    var isDrawLine: Boolean = false
        set(value) {
            field = value
            invalidate()
        }
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0
    private var showCross: Boolean = false
    private var syncImage: SynchronizedBitmap? = null
    private val paint = Paint().apply {
        isAntiAlias = true
    }

    // Topdon TC001 specific camera control methods
    fun start() {
        XLog.d(TAG, "Starting Topdon TC001 camera view")
        visibility = VISIBLE
    }

    fun stop() {
        XLog.d(TAG, "Stopping Topdon TC001 camera view")
        bitmap = null
        invalidate()
    }

    fun openCamera() {
        XLog.d(TAG, "Opening Topdon TC001 camera")
        visibility = VISIBLE
        start()
    }

    fun setImageSize(width: Int, height: Int) {
        this.imageWidth = width
        this.imageHeight = height
        requestLayout()
    }

    fun setShowCross(show: Boolean) {
        this.showCross = show
        invalidate()
    }

    fun setSyncimage(sync: SynchronizedBitmap?) {
        this.syncImage = sync
        invalidate()
    }

    fun getScaledBitmap(): Bitmap? {
        return bitmap?.let { originalBitmap ->
            if (width > 0 && height > 0) {
                try {
                    Bitmap.createScaledBitmap(originalBitmap, width, height, true)
                } catch (e: Exception) {
                    XLog.e(TAG, "Failed to create scaled bitmap for TC001", e)
                    originalBitmap
                }
            } else {
                originalBitmap
            }
        }
    }

    // TC001 thermal imaging specific methods
    fun updateSelectBitmap() {
        invalidate()
    }

    fun updateTargetBitmap() {
        invalidate()
    }

    fun updateMagnifier() {
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bitmap?.let { bmp ->
            try {
                val destRect = Rect(0, 0, width, height)
                canvas.drawBitmap(bmp, null, destRect, paint)
                // Draw crosshair for thermal targeting if enabled
                if (showCross) {
                    drawThermalCrosshair(canvas)
                }
            } catch (e: Exception) {
                XLog.e(TAG, "Failed to draw TC001 thermal bitmap", e)
            }
        }
    }

    private fun drawThermalCrosshair(canvas: Canvas) {
        val centerX = width / 2f
        val centerY = height / 2f
        val crossSize = 20f
        paint.strokeWidth = 2f
        paint.color = android.graphics.Color.RED
        // Draw horizontal line
        canvas.drawLine(centerX - crossSize, centerY, centerX + crossSize, centerY, paint)
        // Draw vertical line
        canvas.drawLine(centerX, centerY - crossSize, centerX, centerY + crossSize, paint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (imageWidth > 0 && imageHeight > 0) {
            // Maintain aspect ratio for thermal camera feed
            val aspectRatio = imageWidth.toFloat() / imageHeight.toFloat()
            val width = MeasureSpec.getSize(widthMeasureSpec)
            val height = (width / aspectRatio).toInt()
            setMeasuredDimension(width, height)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
}


