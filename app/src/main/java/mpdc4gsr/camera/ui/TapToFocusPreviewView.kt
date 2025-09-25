package mpdc4gsr.camera.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.camera.view.PreviewView

/**
 * Custom PreviewView that supports tap-to-focus functionality
 * with visual feedback for focus point
 */
class TapToFocusPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : PreviewView(context, attrs, defStyleAttr) {

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

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Calculate normalized coordinates (0.0 to 1.0)
                val normalizedX = event.x / width
                val normalizedY = event.y / height
                
                // Store touch position for visual feedback
                focusX = event.x
                focusY = event.y
                
                // Show focus indicator
                showFocusIndicator = true
                focusIndicatorAlpha = 255
                invalidate()
                
                // Trigger focus callback
                onTapToFocus?.invoke(normalizedX, normalizedY)
                
                // Hide focus indicator after delay
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
        
        // Draw focus indicator if active
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
            
            // Fade out effect
            if (focusIndicatorAlpha > 0) {
                focusIndicatorAlpha = (focusIndicatorAlpha - 8).coerceAtLeast(0)
                if (focusIndicatorAlpha > 0) {
                    postInvalidateDelayed(50)
                }
            }
        }
    }
    
    /**
     * Programmatically trigger focus at specific coordinates
     */
    fun triggerFocusAt(x: Float, y: Float) {
        focusX = x
        focusY = y
        showFocusIndicator = true
        focusIndicatorAlpha = 255
        invalidate()
        
        val normalizedX = x / width
        val normalizedY = y / height
        onTapToFocus?.invoke(normalizedX, normalizedY)
        
        postDelayed({
            showFocusIndicator = false
            invalidate()
        }, 1500)
    }
    
    /**
     * Hide focus indicator
     */
    fun hideFocusIndicator() {
        showFocusIndicator = false
        invalidate()
    }
}