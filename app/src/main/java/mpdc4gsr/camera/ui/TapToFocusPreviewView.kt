package mpdc4gsr.camera.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.camera.view.PreviewView

/**
 * Composite view that wraps a CameraX PreviewView and provides tap-to-focus
 * visual feedback via a custom overlay.
 */
class TapToFocusPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val internalPreviewView = PreviewView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    private val overlayView = object : android.view.View(context) {
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            drawFocusIndicator(canvas)
        }
    }.apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        isClickable = false
        isFocusable = false
    }

    private val focusCirclePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val focusInnerPaint = Paint().apply {
        color = Color.TRANSPARENT
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var focusX = -1f
    private var focusY = -1f
    private var showFocusIndicator = false
    private var focusIndicatorAlpha = 255
    private val focusCircleRadius = 60f

    // Callback for tap-to-focus events
    var onTapToFocus: ((normalizedX: Float, normalizedY: Float) -> Unit)? = null

    init {
        addView(internalPreviewView)
        addView(overlayView)
        isClickable = true
        isFocusable = true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val widthF = width.takeIf { it > 0 } ?: internalPreviewView.width
                val heightF = height.takeIf { it > 0 } ?: internalPreviewView.height
                if (widthF <= 0 || heightF <= 0) return super.onTouchEvent(event)

                val normalizedX = event.x / widthF
                val normalizedY = event.y / heightF

                focusX = event.x
                focusY = event.y

                showFocusIndicator = true
                focusIndicatorAlpha = 255
                overlayView.invalidate()

                onTapToFocus?.invoke(normalizedX, normalizedY)

                postDelayed({
                    showFocusIndicator = false
                    overlayView.invalidate()
                }, 1500)

                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun drawFocusIndicator(canvas: Canvas) {
        if (showFocusIndicator && focusX >= 0 && focusY >= 0) {
            focusCirclePaint.alpha = focusIndicatorAlpha
            focusInnerPaint.alpha = (focusIndicatorAlpha * 0.3f).toInt()

            // Draw outer circle
            canvas.drawCircle(focusX, focusY, focusCircleRadius, focusCirclePaint)

            // Draw inner circle
            canvas.drawCircle(focusX, focusY, focusCircleRadius * 0.7f, focusInnerPaint)

            // Draw crosshair
            val crosshairLength = focusCircleRadius * 0.4f
            canvas.drawLine(
                focusX - crosshairLength, focusY,
                focusX + crosshairLength, focusY,
                focusCirclePaint
            )
            canvas.drawLine(
                focusX, focusY - crosshairLength,
                focusX, focusY + crosshairLength,
                focusCirclePaint
            )

            if (focusIndicatorAlpha > 0) {
                focusIndicatorAlpha = (focusIndicatorAlpha - 8).coerceAtLeast(0)
                if (focusIndicatorAlpha > 0) {
                    overlayView.postInvalidateOnAnimation()
                }
            }
        }
    }

    fun getPreviewView(): PreviewView = internalPreviewView

    fun triggerFocusAt(x: Float, y: Float) {
        focusX = x
        focusY = y
        showFocusIndicator = true
        focusIndicatorAlpha = 255
        overlayView.invalidate()

        val widthF = width.takeIf { it > 0 } ?: internalPreviewView.width
        val heightF = height.takeIf { it > 0 } ?: internalPreviewView.height
        if (widthF > 0 && heightF > 0) {
            val normalizedX = x / widthF
            val normalizedY = y / heightF
            onTapToFocus?.invoke(normalizedX, normalizedY)
        }

        postDelayed({
            showFocusIndicator = false
            overlayView.invalidate()
        }, 1500)
    }

    fun hideFocusIndicator() {
        showFocusIndicator = false
        overlayView.invalidate()
    }
}