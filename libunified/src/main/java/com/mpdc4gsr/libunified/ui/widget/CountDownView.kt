package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.mpdc4gsr.libunified.R

class CountDownView : View {
    
    private var ringColor: Int = 0
    private var progressTextSize: Float = 0f
    private var ringWidth: Float = 0f
    private var progressTextColor: Int = 0
    private var progressText: String = ""
    private var countdownTime: Int = 0
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    
    constructor(context: Context) : super(context) {
        initDefaults()
    }
    
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initAttributes(context, attrs)
    }
    
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initAttributes(context, attrs)
    }
    
    private fun initDefaults() {
        ringColor = ContextCompat.getColor(context, R.color.white)
        progressTextSize = 48f
        ringWidth = 8f
        progressTextColor = ContextCompat.getColor(context, R.color.black)
        progressText = "00:00"
        countdownTime = 60
    }
    
    private fun initAttributes(context: Context, attrs: AttributeSet?) {
        initDefaults()
        
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CountDownView)
            
            ringColor = typedArray.getColor(R.styleable.CountDownView_ringColor, ringColor)
            progressTextSize = typedArray.getDimension(R.styleable.CountDownView_progressTextSize, progressTextSize)
            ringWidth = typedArray.getDimension(R.styleable.CountDownView_ringWidth, ringWidth)
            progressTextColor = typedArray.getColor(R.styleable.CountDownView_progressTextColor, progressTextColor)
            progressText = typedArray.getString(R.styleable.CountDownView_progressText) ?: progressText
            countdownTime = typedArray.getInt(R.styleable.CountDownView_countdownTime, countdownTime)
            
            typedArray.recycle()
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (minOf(width, height) / 2f) - ringWidth
        
        // Draw ring
        paint.color = ringColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = ringWidth
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        // Draw text
        paint.color = progressTextColor
        paint.style = Paint.Style.FILL
        paint.textSize = progressTextSize
        paint.textAlign = Paint.Align.CENTER
        
        val textY = centerY - (paint.descent() + paint.ascent()) / 2
        canvas.drawText(progressText, centerX, textY, paint)
    }
    
    fun setProgressText(text: String) {
        progressText = text
        invalidate()
    }
    
    fun setCountdownTime(time: Int) {
        countdownTime = time
        invalidate()
    }
}