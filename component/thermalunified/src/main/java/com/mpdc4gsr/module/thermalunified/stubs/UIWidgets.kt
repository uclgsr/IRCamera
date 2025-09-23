package com.mpdc4gsr.module.thermalunified.stubs

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/**
 * Functional implementations for missing UI widgets
 * These provide basic working functionality for an MVP
 */

class SeekBarIndicator {
    var indicatorBackgroundColor: Int = Color.LTGRAY
}

interface OnRangeChangedListener {
    fun onRangeChanged(
        view: RangeSeekBar?, 
        leftValue: Float, 
        rightValue: Float, 
        isFromUser: Boolean, 
        tempMode: Int
    )
    fun onStartTrackingTouch(view: RangeSeekBar?, isLeft: Boolean)
    fun onStopTrackingTouch(view: RangeSeekBar?, isLeft: Boolean)
}

class RangeSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    var onRangeChangedListener: OnRangeChangedListener? = null
    var currentRange = floatArrayOf(0f, 100f)
    var tempMode: Int = 0
    var leftSeekBar: SeekBarIndicator = SeekBarIndicator()
    var rightSeekBar: SeekBarIndicator = SeekBarIndicator()
    var indicatorBackgroundColor: Int = Color.LTGRAY
    
    private var minValue = 0f
    private var maxValue = 100f
    private var leftValue = 20f
    private var rightValue = 80f
    private var isDragging = false
    private var isDraggingLeft = false
    
    private val trackPaint = Paint().apply {
        color = Color.GRAY
        isAntiAlias = true
    }
    
    private val thumbPaint = Paint().apply {
        color = Color.BLUE
        isAntiAlias = true
    }

    // FIX: Added separate paint object for the right thumb to prevent colour bug
    private val rightThumbPaint = Paint().apply {
        color = Color.RED
        isAntiAlias = true
    }
    
    companion object {
        const val TEMP_MODE_MIN = -20
        const val TEMP_MODE_MAX = 150
        const val TEMP_MODE_INTERVAL = 1
        const val TEMP_MODE_CLOSE = 0
    }
    
    fun setRange(min: Float, max: Float) {
        minValue = min
        maxValue = max
        currentRange[0] = min
        currentRange[1] = max
        invalidate()
    }
    
    fun setOnRangeChangedListener(listener: OnRangeChangedListener) {
        onRangeChangedListener = listener
    }
    
    fun setRangeAndPro(minTemp: Float, maxTemp: Float, interval: Float, mode: Float) {
        setRange(minTemp, maxTemp)
        tempMode = mode.toInt()
    }
    
    fun setRangeAndPro(minTemp: Float, maxTemp: Float, interval: Float, mode: Int) {
        setRange(minTemp, maxTemp)
        tempMode = mode
    }
    
    fun setRangeAndPro(range: String) {
        // Parse range string if needed - basic implementation
    }
    
    fun setColorList(colors: IntArray?) {
        colors?.let { 
            if (it.isNotEmpty()) {
                trackPaint.color = it[0]
            }
        }
        invalidate()
    }
    
    fun setPlaces(places: FloatArray?) {
        // Store gradient positions if needed
    }
    
    fun setPlaces(places: Array<Float>?) {
        // Store gradient positions if needed
    }
    
    fun setPseudocode(code: Int) {
        tempMode = code
    }
    
    fun setIndicatorTextDecimalFormat(format: String) {
        // Store format for text display
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val trackHeight = 8f
        val thumbRadius = 20f
        val y = height / 2f
        
        // Draw track
        canvas.drawRoundRect(
            paddingStart.toFloat(), 
            y - trackHeight / 2,
            (width - paddingEnd).toFloat(), 
            y + trackHeight / 2,
            trackHeight / 2,
            trackHeight / 2,
            trackPaint
        )
        
        // Calculate thumb positions
        val trackWidth = width - paddingStart - paddingEnd
        val leftThumbX = paddingStart + (leftValue - minValue) / (maxValue - minValue) * trackWidth
        val rightThumbX = paddingStart + (rightValue - minValue) / (maxValue - minValue) * trackWidth
        
        // Draw thumbs
        canvas.drawCircle(leftThumbX, y, thumbRadius, thumbPaint)
        // FIX: Use the dedicated paint object for the right thumb
        canvas.drawCircle(rightThumbX, y, thumbRadius, rightThumbPaint)
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val trackWidth = width - paddingStart - paddingEnd
        val leftThumbX = paddingStart + (leftValue - minValue) / (maxValue - minValue) * trackWidth
        val rightThumbX = paddingStart + (rightValue - minValue) / (maxValue - minValue) * trackWidth
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val leftDistance = abs(event.x - leftThumbX)
                val rightDistance = abs(event.x - rightThumbX)
                
                isDraggingLeft = leftDistance < rightDistance
                isDragging = true
                onRangeChangedListener?.onStartTrackingTouch(this, isDraggingLeft)
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val ratio = (event.x - paddingStart) / trackWidth
                    val newValue = minValue + ratio * (maxValue - minValue)
                    
                    if (isDraggingLeft) {
                        leftValue = newValue.coerceIn(minValue, rightValue)
                    } else {
                        rightValue = newValue.coerceIn(leftValue, maxValue)
                    }
                    
                    onRangeChangedListener?.onRangeChanged(this, leftValue, rightValue, true, tempMode)
                    invalidate()
                    return true
                }
            }
            
            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    onRangeChangedListener?.onStopTrackingTouch(this, isDraggingLeft)
                    isDragging = false
                    return true
                }
            }
        }
        
        return super.onTouchEvent(event)
    }
}

class BitmapConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.constraintlayout.widget.ConstraintLayout(context, attrs, defStyleAttr)

class CameraPreView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    var cameraPreViewCloseListener: (() -> Unit)? = null
    private var isOpen = false
    private var alpha = 1.0f
    
    fun getBitmap(): android.graphics.Bitmap? {
        return try {
            android.graphics.Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), android.graphics.Bitmap.Config.ARGB_8888)
        } catch (e: Exception) {
            null
        }
    }
    
    fun closeCamera() {
        isOpen = false
        cameraPreViewCloseListener?.invoke()
        invalidate()
    }
    
    fun openCamera() {
        isOpen = true
        invalidate()
    }
    
    fun setRotation(enabled: Boolean) {
        rotation = if (enabled) 90f else 0f
    }
    
    fun setCameraAlpha(alpha: Float) {
        this.alpha = alpha.coerceIn(0f, 1f)
        setAlpha(this.alpha)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (isOpen) {
            val paint = Paint().apply {
                color = Color.DKGRAY
                alpha = (255 * this@CameraPreView.alpha).toInt()
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }
    }
}

// Stub for CameraView (from infisense package)
class CameraView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    var bitmap: android.graphics.Bitmap? = null
    var isOpenAmplify: Boolean = false
    private var showCross = false
    private var isStarted = false
    
    fun setShowCross(show: Boolean) {
        showCross = show
        invalidate()
    }
    
    fun setSyncimage(bitmap: android.graphics.Bitmap?) {
        this.bitmap = bitmap
        invalidate()
    }
    
    fun setSyncimage(syncBitmap: com.energy.iruvc.utils.SynchronizedBitmap?) {
        // Handle synchronized bitmap
        invalidate()
    }
    
    fun setTemperature(temp: Any?) {
        // Store temperature data
    }
    
    fun setImageSize(width: Int, height: Int) {
        layoutParams = layoutParams?.apply {
            this.width = width
            this.height = height
        }
    }
    
    fun setImageSize(width: Int, height: Int, context: Any?) {
        setImageSize(width, height)
    }
    
    fun updateSelectBitmap() {
        invalidate()
    }
    
    fun updateTargetBitmap() {
        invalidate()
    }
    
    fun setCameraAlpha(alpha: Float) {
        setAlpha(alpha.coerceIn(0f, 1f))
    }
    
    fun getScaledBitmap(): android.graphics.Bitmap {
        return bitmap ?: android.graphics.Bitmap.createBitmap(
            width.coerceAtLeast(1), 
            height.coerceAtLeast(1), 
            android.graphics.Bitmap.Config.ARGB_8888
        )
    }
    
    fun closeCamera() {
        isStarted = false
    }
    
    fun openCamera() {
        isStarted = true
    }
    
    fun clear() {
        bitmap = null
        invalidate()
    }
    
    fun start() {
        isStarted = true
    }
    
    fun stop() {
        isStarted = false
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        bitmap?.let { bmp ->
            canvas.drawBitmap(bmp, 0f, 0f, null)
        }
        
        if (showCross) {
            val paint = Paint().apply {
                color = Color.RED
                strokeWidth = 4f
            }
            val centerX = width / 2f
            val centerY = height / 2f
            canvas.drawLine(centerX - 20, centerY, centerX + 20, centerY, paint)
            canvas.drawLine(centerX, centerY - 20, centerX, centerY + 20, paint)
        }
    }
}

// Stub for TemperatureView (from infisense package)
class TemperatureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    var temperatureRegionMode: Int = 0
    var isEnabled: Boolean = true
    var regionAndValueBitmap: android.graphics.Bitmap? = null
    var isShowFull: Boolean = false
    
    var isUserHighTemp: Boolean = false
    var isUserLowTemp: Boolean = false
    
    private var textSize = 12
    private var lineColor = Color.WHITE
    
    fun setIndicatorTextDecimalFormat(format: String) {
        // Store format
    }
    
    fun setTextSize(size: Int) {
        textSize = size
    }
    
    fun setLinePaintColor(color: Int) {
        lineColor = color
    }
    
    fun setRangeAndPro(minTemp: Float, maxTemp: Float, interval: Float, mode: Float) {
        // Set temperature range
    }
    
    fun setRangeAndPro(minTemp: Float, maxTemp: Float, interval: Float, mode: Int) {
        // Set temperature range
    }
    
    fun setRangeAndPro(range: String) {
        // Parse range
    }
    
    fun setOnTrendChangeListener(listener: Any?) {
        // Store listener
    }
    
    fun setOnTrendAddListener(listener: Any?) {
        // Store listener
    }
    
    fun setOnTrendRemoveListener(listener: Any?) {
        // Store listener
    }
    
    fun setPseudocode(code: Int) {
        // Set pseudo color
    }
    
    fun setUserHighTemp(temp: Float) {
        // Set high temperature
    }
    
    fun setUserHighTemp(enabled: Boolean) {
        isUserHighTemp = enabled
    }
    
    fun setUserLowTemp(temp: Float) {
        // Set low temperature
    }
    
    fun setUserLowTemp(enabled: Boolean) {
        isUserLowTemp = enabled
    }
    
    fun setImageSize(width: Int, height: Int) {
        layoutParams = layoutParams?.apply {
            this.width = width
            this.height = height
        }
    }
    
    fun setImageSize(width: Int, height: Int, context: Any?) {
        setImageSize(width, height)
    }
    
    fun updateMagnifier() {
        invalidate()
    }
    
    fun start() {
        // Start temperature monitoring
    }
    
    fun stop() {
        // Stop temperature monitoring
    }
    
    // FIX: Changed to super.post() to prevent StackOverflowError
    fun post(action: Runnable) {
        super.post(action)
    }
    
    fun clear() {
        regionAndValueBitmap = null
        invalidate()
    }
    
    fun setSyncimage(bitmap: android.graphics.Bitmap?) {
        regionAndValueBitmap = bitmap
        invalidate()
    }
    
    fun setSyncimage(syncBitmap: com.energy.iruvc.utils.SynchronizedBitmap?) {
        // Handle synchronized bitmap
        invalidate()
    }
    
    fun setTemperature(temp: Any?) {
        // Set temperature data
    }
    
    var listener: Any? = null
}