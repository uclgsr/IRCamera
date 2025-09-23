package com.mpdc4gsr.libunified.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import com.blankj.utilcode.util.SizeUtils

class BarPickView : View {
    companion object {
        @ColorInt
        private const val DEFAULT_BG_COLOR = 0x7F000000.toInt()

        @ColorInt
        private const val DEFAULT_PROGRESS_COLOR = 0xffffffff.toInt()

        private const val THUMB_CORNERS = 4f

        private const val THUMB_STROKE_WIDTH = 1.5f
    }

    var onStartTrackingTouch: ((progress: Int, max: Int) -> Unit)? = null

    var onProgressChanged: ((progress: Int, max: Int) -> Unit)? = null

    var onStopTrackingTouch: ((progress: Int, max: Int) -> Unit)? = null

    var valueFormatListener: ((progress: Int) -> String) = {
        it.toString()
    }

    var max: Int = 100
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var min: Int = 0
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var progress: Int = 0
        set(value) {
            if (field != value.coerceIn(min, max)) {
                field = value.coerceIn(min, max)
                invalidate()
            }
        }

    private val barSize: Int

    private val rotate: Int

    private val labelText: String

    private val path = Path()
    private val paint = TextPaint()
    private val thumbRect = RectF()
    private val barRect = RectF()

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes,
    ) {
        // Initialize with default values since styleable might not exist
        max = 100
        min = 0
        progress = 0
        barSize = SizeUtils.dp2px(4f)
        rotate = 0
        labelText = ""

        paint.textSize = SizeUtils.sp2px(14f).toFloat()
        paint.isAntiAlias = true
    }

    fun setProgressAndRefresh(progress: Int) {
        this.progress = progress
        invalidate()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Basic implementation - can be expanded based on TargetBarPickView if needed
        // Draw simple bar
        val barWidth = width - paddingLeft - paddingRight
        val barHeight = barSize
        val centerY = height / 2f

        barRect.set(
            paddingLeft.toFloat(),
            centerY - barHeight / 2f,
            (paddingLeft + barWidth).toFloat(),
            centerY + barHeight / 2f
        )

        paint.color = DEFAULT_BG_COLOR
        canvas.drawRect(barRect, paint)

        // Draw progress
        val progressWidth = barWidth * (progress - min) / (max - min).toFloat()
        barRect.set(
            paddingLeft.toFloat(),
            centerY - barHeight / 2f,
            paddingLeft + progressWidth,
            centerY + barHeight / 2f
        )

        paint.color = DEFAULT_PROGRESS_COLOR
        canvas.drawRect(barRect, paint)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                onStartTrackingTouch?.invoke(progress, max)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val x = event.x - paddingLeft
                val barWidth = width - paddingLeft - paddingRight
                val newProgress = min + ((x / barWidth) * (max - min)).toInt()
                progress = newProgress.coerceIn(min, max)
                onProgressChanged?.invoke(progress, max)
                return true
            }

            MotionEvent.ACTION_UP -> {
                onStopTrackingTouch?.invoke(progress, max)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}