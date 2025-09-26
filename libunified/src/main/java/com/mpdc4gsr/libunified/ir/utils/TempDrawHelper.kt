package com.mpdc4gsr.libunified.ir.utils

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import kotlin.math.max
import kotlin.math.min

/**
 * TempDrawHelper - Helper for drawing temperature-related UI elements
 * Used by TemperatureView for positioning and coordinate corrections
 */
class TempDrawHelper {
    
    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        textSize = 24f
        color = Color.WHITE
    }
    
    companion object {
        
        /**
         * Correct coordinate value to ensure it's within bounds
         */
        @JvmStatic
        fun correct(value: Float, maxValue: Int): Int {
            return max(0, min(value.toInt(), maxValue - 1))
        }
        
        /**
         * Correct point coordinate to ensure it's within bounds
         */
        @JvmStatic
        fun correctPoint(value: Float, maxValue: Int): Int {
            return max(0, min(value.toInt(), maxValue - 1))
        }
        
        /**
         * Get rectangle bounds for view
         */
        @JvmStatic
        fun getRect(width: Int, height: Int): Rect {
            return Rect(0, 0, width, height)
        }
    }
    
    /**
     * Set text size
     */
    fun setTextSize(textSize: Int) {
        paint.textSize = textSize.toFloat()
    }
    
    /**
     * Set text color
     */
    fun setTextColor(color: Int) {
        paint.color = color
    }
    
    /**
     * Draw circle for temperature point
     */
    fun drawCircle(canvas: Canvas, x: Int, y: Int, isMax: Boolean) {
        val oldColor = paint.color
        paint.color = if (isMax) Color.RED else Color.BLUE
        canvas.drawCircle(x.toFloat(), y.toFloat(), 10f, paint)
        paint.color = oldColor
    }
    
    /**
     * Draw point
     */
    fun drawPoint(canvas: Canvas, x: Int, y: Int) {
        val oldColor = paint.color
        paint.color = Color.GREEN
        canvas.drawCircle(x.toFloat(), y.toFloat(), 8f, paint)
        paint.color = oldColor
    }
    
    /**
     * Draw line
     */
    fun drawLine(canvas: Canvas, startX: Int, startY: Int, stopX: Int, stopY: Int) {
        val oldColor = paint.color
        val oldStyle = paint.style
        paint.color = Color.YELLOW
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawLine(startX.toFloat(), startY.toFloat(), stopX.toFloat(), stopY.toFloat(), paint)
        paint.color = oldColor
        paint.style = oldStyle
    }
    
    /**
     * Draw rectangle
     */
    fun drawRect(canvas: Canvas, left: Int, top: Int, right: Int, bottom: Int) {
        val oldColor = paint.color
        val oldStyle = paint.style
        paint.color = Color.CYAN
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
        paint.color = oldColor
        paint.style = oldStyle
    }
    
    /**
     * Draw trend text
     */
    fun drawTrendText(canvas: Canvas, viewWidth: Int, viewHeight: Int, startX: Int, startY: Int, stopX: Int, stopY: Int) {
        val text = "Trend"
        val centerX = (startX + stopX) / 2
        val centerY = (startY + stopY) / 2
        drawTempText(canvas, text, viewWidth, centerX, centerY)
    }
    
    /**
     * Draw temperature text
     */
    fun drawTempText(canvas: Canvas, text: String, viewWidth: Int, x: Int, y: Int) {
        val oldColor = paint.color
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        
        // Calculate text position to avoid going off screen
        val textWidth = paint.measureText(text)
        val adjustedX = if (x + textWidth > viewWidth) {
            (viewWidth - textWidth).toInt()
        } else {
            x
        }
        
        canvas.drawText(text, adjustedX.toFloat(), (y - 15).toFloat(), paint)
        paint.color = oldColor
    }
}