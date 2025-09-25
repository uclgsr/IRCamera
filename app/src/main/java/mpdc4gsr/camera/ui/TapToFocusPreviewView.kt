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
 * Custom FrameLayout that contains a PreviewView and supports tap-to-focus functionality
 * with visual feedback for focus point
 */
class TapToFocusPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    val previewView = PreviewView(context, attrs, defStyleAttr)

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
        // Add the PreviewView to the FrameLayout
        addView(previewView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        
        // Enable drawing for focus indicators
        setWillNotDraw(false)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val widthF = width.takeIf { it > 0 } ?: previewView.width
                val heightF = height.takeIf { it > 0 } ?: previewView.height
                if (widthF <= 0 || heightF <= 0) return super.onTouchEvent(event)

                val normalizedX = event.x / widthF
                val normalizedY = event.y / heightF

                focusX = event.x
                focusY = event.y

                showFocusIndicator = true
                focusIndicatorAlpha = 255
                invalidate()

                onTapToFocus?.invoke(normalizedX, normalizedY)

                postDelayed({
                    showFocusIndicator = false
                    invalidate()
                }, 1500)

                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawFocusIndicator(canvas)
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
                    postInvalidateOnAnimation()
                }
            }
        }
    }

    fun getPreviewView(): PreviewView = previewView

    fun triggerFocusAt(x: Float, y: Float) {
        focusX = x
        focusY = y
        showFocusIndicator = true
        focusIndicatorAlpha = 255
        invalidate()

        val widthF = width.takeIf { it > 0 } ?: previewView.width
        val heightF = height.takeIf { it > 0 } ?: previewView.height
        if (widthF > 0 && heightF > 0) {
            val normalizedX = x / widthF
            val normalizedY = y / heightF
            onTapToFocus?.invoke(normalizedX, normalizedY)
        }

        postDelayed({
            showFocusIndicator = false
            invalidate()
        }, 1500)
    }

    fun hideFocusIndicator() {
        showFocusIndicator = false
        invalidate()
    }
}