// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget' directory and its subdirectories.
// Total files: 25 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\BarPickView.kt =====

package com.mpdc4gsr.libunified.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.compat.dpToPx
import com.mpdc4gsr.libunified.compat.spToPx

class BarPickView : View {
    companion object {
        @ColorInt
        private const val DEFAULT_BG_COLOR = 0xff787878.toInt()

        @ColorInt
        private const val DEFAULT_PROGRESS_COLOR = 0xffffffff.toInt()
        private const val THUMB_CORNERS = 11f
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
    private var progress: Int = 0
        set(value) {
            if (field != value) {
                field = value.coerceAtLeast(min).coerceAtMost(max)
                invalidate()
            }
        }

    fun setProgressAndRefresh(progress: Int) {
        this.progress = progress
        onProgressChanged?.invoke(this.progress, max)
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
        defStyleRes
    ) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.BarPickView, 0, 0)
        max = typedArray.getInt(R.styleable.BarPickView_android_max, 100)
        min = typedArray.getInt(R.styleable.BarPickView_barMin, 0)
        progress =
            typedArray.getInt(R.styleable.BarPickView_android_progress, min).coerceAtMost(max)
                .coerceAtLeast(min)
        barSize = typedArray.getInt(R.styleable.BarPickView_barSize, 4f.dpToPx(context).toInt())
        rotate = typedArray.getInt(R.styleable.BarPickView_barOrientation, 0)
        labelText = typedArray.getString(R.styleable.BarPickView_barLabel) ?: ""
        val textSize = typedArray.getDimensionPixelSize(
            R.styleable.BarPickView_android_textSize,
            13f.spToPx(context).toInt()
        )
        typedArray.recycle()
        paint.isAntiAlias = true
        paint.textSize = textSize.toFloat()
        paint.strokeWidth = THUMB_STROKE_WIDTH.dpToPx(context)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return false
        }
        parent.requestDisallowInterceptTouchEvent(true)
        val x: Float = event.x - barRect.left
        val y: Float = event.y - barRect.top
        val barWidth: Float = barRect.width()
        val barHeight: Float = barRect.height()
        progress = when (rotate) {
            0 -> (x / barWidth * (max - min) + min).toInt()
            180 -> ((barWidth - x) / barWidth * (max - min) + min).toInt()
            90 -> (y / barHeight * (max - min) + min).toInt()
            else -> ((barHeight - y) / barHeight * (max - min) + min).toInt()
        }.coerceAtLeast(min).coerceAtMost(max)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> onStartTrackingTouch?.invoke(progress, max)
            MotionEvent.ACTION_MOVE -> onProgressChanged?.invoke(progress, max)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                onStopTrackingTouch?.invoke(progress, max)
            }
        }
        return true
    }

    private fun computeThumbWidth(): Int {
        val minTextWidth = paint.measureText(valueFormatListener.invoke(min)).toInt()
        val maxTextWidth = paint.measureText(valueFormatListener.invoke(max)).toInt()
        return minTextWidth.coerceAtLeast(maxTextWidth) + 12f.dpToPx(context).toInt()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val thumbWidth = computeThumbWidth()
        val thumbHeight: Int =
            paint.fontMetricsInt.bottom - paint.fontMetricsInt.top + 4f.dpToPx(context).toInt()
        val width: Int = if (rotate == 0 || rotate == 180) {
            if (widthMode == MeasureSpec.UNSPECIFIED) context.resources.displayMetrics.widthPixels else widthSize
        } else {
            val wantWidth: Int = thumbWidth + paddingStart + paddingEnd
            when (widthMode) {
                MeasureSpec.EXACTLY -> widthSize
                MeasureSpec.AT_MOST -> wantWidth.coerceAtMost(widthSize)
                MeasureSpec.UNSPECIFIED -> wantWidth
                else -> wantWidth
            }
        }
        val height: Int = if (rotate == 0 || rotate == 180) {
            val wantHeight: Int = thumbHeight + paddingTop + paddingBottom
            when (heightMode) {
                MeasureSpec.EXACTLY -> heightSize
                MeasureSpec.AT_MOST -> wantHeight.coerceAtMost(heightSize)
                MeasureSpec.UNSPECIFIED -> wantHeight
                else -> wantHeight
            }
        } else {
            if (heightMode == MeasureSpec.UNSPECIFIED) context.resources.displayMetrics.heightPixels else heightSize
        }
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        computeBarRect()
        computeThumbRect()
        clipToBarRect(canvas)
        drawBgBar(canvas)
        drawProgress(canvas)
        canvas.restore()
        drawThumb(canvas)
        drawText(canvas)
    }

    private fun computeBarRect() {
        val textHeight = paint.fontMetricsInt.bottom - paint.fontMetricsInt.top
        val textMargin = 4f.dpToPx(context)
        val thumbWidth = computeThumbWidth()
        val thumbHeight = textHeight + 4f.dpToPx(context).toInt()
        if (rotate == 0 || rotate == 180) {
            val labelTextSpace =
                if (labelText.isEmpty()) 0 else (paint.measureText(labelText)
                    .toInt() + 6f.dpToPx(context).toInt())
            val leftText = valueFormatListener.invoke(if (rotate == 0) min else max)
            val rightText = valueFormatListener.invoke(if (rotate == 0) max else min)
            val leftTextWidth = paint.measureText(leftText).toInt()
            val rightTextWidth = paint.measureText(rightText).toInt()
            val left =
                paddingStart.toFloat() + leftTextWidth + textMargin + if (rotate == 0) labelTextSpace else 0
            val top = (paddingTop + thumbHeight / 2 - barSize / 2).toFloat()
            val right =
                (measuredWidth - paddingEnd - rightTextWidth - textMargin - if (rotate == 0) 0 else labelTextSpace).toFloat()
            val bottom = top + barSize
            barRect.set(left, top, right, bottom)
        } else {
            val labelTextSpace = if (labelText.isEmpty()) 0 else (textHeight + 6f.dpToPx(context).toInt())
            val left = (paddingStart + thumbWidth / 2 - barSize / 2).toFloat()
            val top =
                paddingTop.toFloat() + textHeight + textMargin + if (rotate == 90) labelTextSpace else 0
            val right = left + barSize
            val bottom =
                (measuredHeight - paddingBottom - textHeight - textMargin - if (rotate == 90) 0 else labelTextSpace).toFloat()
            barRect.set(left, top, right, bottom)
        }
    }

    private fun computeThumbRect() {
        val thumbWidth = computeThumbWidth()
        val thumbHeight: Int =
            paint.fontMetricsInt.bottom - paint.fontMetricsInt.top + 4f.dpToPx(context).toInt()
        if (rotate == 0 || rotate == 180) {
            val progressWidth = (barRect.width() * (progress - min) / (max - min)).toInt()
            val left =
                (if (rotate == 0) (barRect.left + progressWidth - thumbWidth / 2) else (barRect.right - progressWidth - thumbWidth / 2))
                    .toInt()
                    .coerceAtLeast(barRect.left.toInt())
                    .coerceAtMost(barRect.right.toInt() - thumbWidth)
            val right = left + thumbWidth
            val top = paddingTop
            val bottom = measuredHeight - paddingBottom
            thumbRect.set(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
        } else {
            val progressHeight =
                (barRect.height() * (progress - min) / (max - min).toFloat()).toInt()
            val left = paddingStart
            val right = measuredWidth - paddingEnd
            val top =
                (if (rotate == 90) (barRect.top + progressHeight - thumbHeight / 2f) else (barRect.bottom - progressHeight - thumbHeight / 2f))
                    .toInt()
                    .coerceAtLeast(barRect.top.toInt())
                    .coerceAtMost(barRect.bottom.toInt() - thumbHeight)
            val bottom = top + thumbHeight
            thumbRect.set(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
        }
    }

    private fun clipToBarRect(canvas: Canvas) {
        canvas.save()
        val radius = (barSize / 2).toFloat()
        if (rotate == 0 || rotate == 180) {
            path.rewind()
            path.moveTo(barRect.left + radius, barRect.top)
            path.lineTo(barRect.right - radius, barRect.top)
            path.quadTo(barRect.right, barRect.top, barRect.right, barRect.top + barSize / 2)
            path.quadTo(barRect.right, barRect.bottom, barRect.right - radius, barRect.bottom)
            path.lineTo(barRect.left + radius, barRect.bottom)
            path.quadTo(barRect.left, barRect.bottom, barRect.left, barRect.bottom - barSize / 2)
            path.quadTo(barRect.left, barRect.top, barRect.left + radius, barRect.top)
            canvas.clipPath(path)
        } else {
            path.rewind()
            path.moveTo(barRect.left, barRect.bottom - radius)
            path.lineTo(barRect.left, barRect.top + radius)
            path.quadTo(barRect.left, barRect.top, barRect.left + barSize / 2, barRect.top)
            path.quadTo(barRect.right, barRect.top, barRect.right, barRect.top + radius)
            path.lineTo(barRect.right, barRect.bottom - radius)
            path.quadTo(barRect.right, barRect.bottom, barRect.right - barSize / 2, barRect.bottom)
            path.quadTo(barRect.left, barRect.bottom, barRect.left, barRect.bottom - radius)
            canvas.clipPath(path)
        }
    }

    private fun drawBgBar(canvas: Canvas) {
        paint.color = DEFAULT_BG_COLOR
        val left = barRect.left
        val top = barRect.top
        val right = barRect.right
        val bottom = barRect.bottom
        if (rotate == 0 || rotate == 180) {
            val thumbWidth = computeThumbWidth()
            val bgWidth = (barRect.width() * (max - progress) / (max - min).toFloat()).toInt()
            if (bgWidth == 0) {
                return
            }
            if (rotate == 0) {
                canvas.drawRect(
                    (right - bgWidth + thumbWidth / 2).coerceAtLeast(left + thumbWidth),
                    top,
                    right,
                    bottom,
                    paint
                )
            } else {
                canvas.drawRect(
                    left,
                    top,
                    (left + bgWidth - thumbWidth / 2).coerceAtMost(right - thumbWidth),
                    bottom,
                    paint
                )
            }
        } else {
            val thumbHeight =
                paint.fontMetricsInt.bottom - paint.fontMetricsInt.top + 4f.dpToPx(context).toInt()
            val bgHeight = (barRect.height() * (max - progress) / (max - min).toFloat()).toInt()
            if (bgHeight == 0) {
                return
            }
            if (rotate == 90) {
                canvas.drawRect(
                    left,
                    (bottom - bgHeight + thumbHeight / 2).coerceAtLeast(top + thumbHeight),
                    right,
                    bottom,
                    paint
                )
            } else {
                canvas.drawRect(
                    left,
                    top,
                    right,
                    (top + bgHeight - thumbHeight / 2).coerceAtMost(bottom - thumbHeight),
                    paint
                )
            }
        }
    }

    private fun drawProgress(canvas: Canvas) {
        paint.color = DEFAULT_PROGRESS_COLOR
        val left = barRect.left
        val top = barRect.top
        val right = barRect.right
        val bottom = barRect.bottom
        if (rotate == 0 || rotate == 180) {
            val thumbWidth = computeThumbWidth()
            val progressWidth = (barRect.width() * (progress - min) / (max - min).toFloat()).toInt()
            if (progressWidth == 0) {
                return
            }
            if (rotate == 0) {
                canvas.drawRect(
                    left,
                    top,
                    (left + progressWidth - thumbWidth / 2).coerceAtMost(right - thumbWidth),
                    bottom,
                    paint
                )
            } else {
                canvas.drawRect(
                    (right - progressWidth + thumbWidth / 2).coerceAtLeast(left + thumbWidth),
                    top,
                    right,
                    bottom,
                    paint
                )
            }
        } else {
            val thumbHeight =
                paint.fontMetricsInt.bottom - paint.fontMetricsInt.top + 4f.dpToPx(context).toInt()
            val progressHeight =
                (barRect.height() * (progress - min) / (max - min).toFloat()).toInt()
            if (progressHeight == 0) {
                return
            }
            if (rotate == 90) {
                canvas.drawRect(
                    left,
                    top,
                    right,
                    (top + progressHeight - thumbHeight / 2).coerceAtMost(bottom - thumbHeight),
                    paint
                )
            } else {
                canvas.drawRect(
                    left,
                    (bottom - progressHeight + thumbHeight / 2).coerceAtLeast(top + thumbHeight),
                    right,
                    bottom,
                    paint
                )
            }
        }
    }

    private fun drawThumb(canvas: Canvas) {
        paint.style = Paint.Style.STROKE
        val radius = THUMB_CORNERS.dpToPx(context)
        canvas.drawRoundRect(thumbRect, radius, radius, paint)
        paint.style = Paint.Style.FILL
        val progressText = valueFormatListener.invoke(progress)
        val textWidth = paint.measureText(progressText)
        val x = thumbRect.left + (thumbRect.width() - textWidth) / 2
        val y = thumbRect.top + 2f.dpToPx(context) - paint.fontMetricsInt.top
        canvas.drawText(progressText, x, y, paint)
    }

    private fun drawText(canvas: Canvas) {
        if (rotate == 0 || rotate == 180) {
            val y = thumbRect.top + 2f.dpToPx(context) - paint.fontMetricsInt.top
            val labelTextWidth = paint.measureText(labelText)
            val labelX =
                if (rotate == 0) paddingStart.toFloat() else (width - paddingEnd - labelTextWidth)
            canvas.drawText(labelText, labelX, y, paint)
            val leftText = valueFormatListener.invoke(if (rotate == 0) min else max)
            val leftX = barRect.left - 4f.dpToPx(context) - paint.measureText(leftText)
            canvas.drawText(leftText, leftX, y, paint)
            val rightText = valueFormatListener.invoke(if (rotate == 0) max else min)
            canvas.drawText(rightText, barRect.right + 4f.dpToPx(context), y, paint)
        } else {
            val topText = valueFormatListener.invoke(if (rotate == 90) min else max)
            val topTextWidth = paint.measureText(topText)
            val topX = thumbRect.left + (thumbRect.width() - topTextWidth) / 2
            val topY = barRect.top - 4f.dpToPx(context) - paint.fontMetricsInt.bottom
            canvas.drawText(topText, topX, topY, paint)
            val bottomText = valueFormatListener.invoke(if (rotate == 90) max else min)
            val bottomTextWidth = paint.measureText(bottomText)
            val bottomX = thumbRect.left + (thumbRect.width() - bottomTextWidth) / 2
            val bottomY = barRect.bottom + 4f.dpToPx(context) - paint.fontMetricsInt.top
            canvas.drawText(bottomText, bottomX, bottomY, paint)
            val labelTextWidth = paint.measureText(labelText)
            val labelX = thumbRect.left + (thumbRect.width() - labelTextWidth) / 2
            val labelY = if (rotate == 90) {
                (paddingTop + 6f.dpToPx(context) - paint.fontMetricsInt.top).toFloat()
            } else {
                (height - 6f.dpToPx(context) - paint.fontMetricsInt.bottom).toFloat()
            }
            canvas.drawText(labelText, labelX, labelY, paint)
        }
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\BatteryView.kt =====

package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class BatteryView : AppCompatImageView {
    var battery = -1
        set(value) {
            field = value
            invalidate()
        }
    var isCharging = false
        set(value) {
            field = value
            invalidate()
        }
    private val paint = Paint()
    private val path = Path()

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        paint.isAntiAlias = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        when (heightMode) {
            MeasureSpec.EXACTLY -> {
                val wantWidth = (heightSize * 58 / 30f).toInt()
                when (widthMode) {
                    MeasureSpec.EXACTLY -> setMeasuredDimension(widthSize, heightSize)
                    MeasureSpec.AT_MOST -> setMeasuredDimension(
                        wantWidth.coerceAtMost(widthSize),
                        heightSize
                    )

                    else -> setMeasuredDimension(wantWidth, heightSize)
                }
            }

            MeasureSpec.AT_MOST -> {
                when (widthMode) {
                    MeasureSpec.EXACTLY -> setMeasuredDimension(
                        widthSize,
                        (widthSize * 30 / 58f).toInt().coerceAtMost(heightSize)
                    )

                    MeasureSpec.AT_MOST -> {
                        if (widthSize < 58) {
                            if (heightSize < 30) {//
                                if ((widthSize * 30 / 58f).toInt() <= heightSize) {
                                    setMeasuredDimension(widthSize, (widthSize * 30 / 58f).toInt())
                                } else {
                                    setMeasuredDimension(
                                        (heightSize * 58 / 30f).toInt(),
                                        heightSize
                                    )
                                }
                            } else {//
                                setMeasuredDimension(widthSize, (widthSize * 30 / 58f).toInt())
                            }
                        } else {
                            if (heightSize < 30) {//
                                setMeasuredDimension((heightSize * 58 / 30f).toInt(), heightSize)
                            } else {//
                                setMeasuredDimension(58, 30)
                            }
                        }
                    }

                    else -> setMeasuredDimension(
                        (widthSize * 30.coerceAtMost(heightSize) / 58f).toInt(),
                        30.coerceAtMost(heightSize)
                    )
                }
            }

            else -> {
                when (widthMode) {
                    MeasureSpec.EXACTLY -> setMeasuredDimension(
                        widthSize,
                        (widthSize * 30 / 58f).toInt()
                    )

                    MeasureSpec.AT_MOST -> setMeasuredDimension(
                        58.coerceAtMost(widthSize),
                        (58.coerceAtMost(widthSize) * 30 / 58f).toInt()
                    )

                    else -> setMeasuredDimension(58, 30)
                }
            }
        }
        drawWidth =
            if ((measuredWidth * 30 / 58f).toInt() <= measuredHeight) measuredWidth else (measuredHeight * 58 / 30f).toInt()
        drawHeight =
            if ((measuredWidth * 30 / 58f).toInt() <= measuredHeight) (measuredWidth * 30 / 58f).toInt() else measuredHeight
        paint.strokeWidth = drawWidth * 2 / 58f
        val levelWidth = drawWidth * 42 / 58f
        val levelHeight = drawHeight * 20 / 30f
        val radius = drawWidth * 4 / 58f
        val left = drawWidth * 5 / 58f
        val top = drawWidth * 5 / 58f
        val right = left + levelWidth
        val bottom = top + levelHeight
        path.rewind()
        path.moveTo(left + radius, top)
        path.lineTo(right - radius, top)
        path.quadTo(right, top, right, top + radius)
        path.lineTo(right, bottom - radius)
        path.quadTo(right, bottom, right - radius, bottom)
        path.lineTo(left + radius, bottom)
        path.quadTo(left, bottom, left, bottom - radius)
        path.lineTo(left, top + radius)
        path.quadTo(left, top, left + radius, top)
    }

    private var drawWidth: Int = 0
    private var drawHeight: Int = 0
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //
        val lineSize = drawWidth * 2 / 58f
        val roundSize = drawWidth * 6 / 58f
        val batteryWidth = drawWidth * 50 / 58f
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.BUTT
        paint.color = 0xff83808c.toInt()
        canvas.drawRoundRect(
            lineSize / 2,
            lineSize / 2,
            lineSize / 2 + batteryWidth,
            drawHeight.toFloat() - lineSize / 2,
            roundSize,
            roundSize,
            paint
        )
        //
        val anodeWidth = drawWidth * 3 / 58f
        val anodeHeight = drawHeight * 8 / 30f - lineSize
        val anodeX = drawWidth - anodeWidth / 2
        val anodeStartY = (drawHeight - anodeHeight) / 2
        paint.style = Paint.Style.FILL
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = anodeWidth
        canvas.drawLine(anodeX, anodeStartY, anodeX, anodeStartY + anodeHeight, paint)
        //
        if (battery <= 0) {
            return
        }
        val progressWidth = drawWidth * 42 / 58f * battery / 100
        paint.strokeCap = Paint.Cap.BUTT
        paint.color =
            (if (isCharging) 0xff6dc80e else if (battery <= 10) 0xffeb433e else 0xffffffff).toInt()
        canvas.clipPath(path)
        canvas.drawRect(
            lineSize + anodeWidth,
            lineSize + anodeWidth,
            lineSize + anodeWidth + progressWidth,
            drawHeight - lineSize - anodeWidth,
            paint
        )
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\BitmapConstraintLayout.kt =====

package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.drawToBitmap

open class BitmapConstraintLayout : ConstraintLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    @Volatile
    var viewBitmap: Bitmap? = null
    fun updateBitmap() {
        if (!isShown) {
            return
        }
        try {
            viewBitmap = this.drawToBitmap()
        } catch (_: Exception) {
        }
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\Comm3DSeekBar.kt =====

package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatSeekBar
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.compat.dpToPx
import com.mpdc4gsr.libunified.compat.spToPx
import kotlin.math.roundToInt

class Comm3DSeekBar : AppCompatSeekBar {
    private lateinit var mPaint: TextPaint
    private val orientation: Int
    private var mMaxWidth = 48
    private var mMaxHeight = 48
    private var mMinWidth = 24
    private var mMinHeight = 24
    var level = 0;

    //
    private val mProgressTextRect: Rect = Rect()

    //
    private val mThumbWidth: Int = 50f.dpToPx(context).toInt()

    //
    private val mIndicatorWidth: Int = 50f.dpToPx(context).toInt()
    private var onSeekBarChangeListener: OnSeekBarChangeListener? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.CommSeekBar, defStyleAttr, 0)
        orientation = typedArray.getInt(R.styleable.CommSeekBar_android_orientation, 0)
        mMaxWidth =
            typedArray.getDimensionPixelSize(R.styleable.CommSeekBar_android_maxWidth, mMaxWidth)
        mMaxHeight =
            typedArray.getDimensionPixelSize(R.styleable.CommSeekBar_android_maxHeight, mMaxHeight)
        mMinWidth =
            typedArray.getDimensionPixelSize(R.styleable.CommSeekBar_android_minWidth, mMinWidth)
        mMinHeight =
            typedArray.getDimensionPixelSize(R.styleable.CommSeekBar_android_minHeight, mMinHeight)
        mPaint = TextPaint()
        mPaint.setAntiAlias(true)
        mPaint.setColor(Color.parseColor("#00574B"))
        mPaint.setTextSize(16f.spToPx(context).toFloat())
        typedArray.recycle()
    }

    override fun setOnSeekBarChangeListener(l: OnSeekBarChangeListener?) {
        if (orientation == 0) {
            super.setOnSeekBarChangeListener(l)
        } else {
            onSeekBarChangeListener = l
        }
    }

    override fun setProgress(progress: Int) {
        super.setProgress(progress)
        if (orientation != 0) {
            onSeekBarChangeListener?.onProgressChanged(this, progress, false)
        }
    }

    override fun setProgress(progress: Int, animate: Boolean) {
        super.setProgress(progress, animate)
        if (orientation != 0) {
            onSeekBarChangeListener?.onProgressChanged(this, progress, false)
        }
    }

    override fun setMax(max: Int) {
        super.setMax(max)
        if (orientation != 0) {
            onSeekBarChangeListener?.onProgressChanged(this, progress, false)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (orientation == 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        } else {
            val d = progressDrawable
            val thumbWidth = thumb?.intrinsicWidth ?: 0
            var dw = 0
            var dh = 0
            if (d != null) {
                dw = mMinWidth.coerceAtLeast(mMaxWidth.coerceAtMost(d.intrinsicWidth))
                dw = thumbWidth.coerceAtLeast(dw)
                dh = mMinHeight.coerceAtLeast(mMaxHeight.coerceAtMost(d.intrinsicHeight))
            }
            dw += paddingLeft + paddingRight
            dh += paddingTop + paddingBottom
            setMeasuredDimension(
                resolveSizeAndState(dw, widthMeasureSpec, 0),
                resolveSizeAndState(dh, heightMeasureSpec, 0)
            )
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (orientation != 0) {
            calculateDrawable(w, h)
        }
    }

    private fun calculateDrawable(w: Int, h: Int) {
        val paddingWidth: Int = w - paddingLeft - paddingRight
        val paddingHeight: Int = h - paddingTop - paddingBottom
        val trackWidth = mMaxWidth.coerceAtMost(paddingWidth)
        val thumbWidth = thumb?.intrinsicWidth ?: 0
        val thumbHeight = thumb?.intrinsicHeight ?: 0
        val trackOffset: Int
        val thumbTopOffset: Int
        if (thumbWidth > trackWidth) {
            val offsetHeight = (paddingWidth - thumbWidth) / 2
            trackOffset = offsetHeight + (thumbWidth - trackWidth) / 2
            thumbTopOffset = offsetHeight
        } else {
            val offsetHeight = (paddingWidth - trackWidth) / 2
            trackOffset = offsetHeight
            thumbTopOffset = offsetHeight + (trackWidth - thumbWidth) / 2
        }
        if (progressDrawable != null) {
            progressDrawable.setBounds(0, trackOffset, paddingHeight, trackOffset + trackWidth)
        }
        if (thumb != null) {
            val available: Int = paddingHeight - thumbHeight + thumbOffset * 2
            val left = progress / max.toFloat() * available + 0.5f
            val reviseLeft = left.coerceAtLeast(thumbHeight / 2 + 0.5f)
                .coerceAtMost(paddingHeight - thumbHeight / 2 - 0.5f).toInt()
            thumb.setBounds(
                reviseLeft,
                thumbTopOffset,
                reviseLeft + thumbHeight,
                thumbTopOffset + thumbWidth
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (orientation == 0) {
            super.onDraw(canvas)
//            val progressText = "$progress%"
//            mPaint.getTextBounds(progressText, 0, progressText.length, mProgressTextRect)
//            // 
//            val progressRatio = progress.toFloat() / max
//            // thumb
//            val thumbOffset: Float =
//                (mThumbWidth - mProgressTextRect.width()) / 2 - mThumbWidth * progressRatio
//            val thumbX = width * progressRatio + thumbOffset
//            val thumbY: Float = height / 2f + mProgressTextRect.height() / 2f
//            canvas!!.drawText(progressText, thumbX, thumbY, mPaint)
        } else {
            canvas?.let {
                it.rotate(90f)
                it.translate(-paddingStart.toFloat(), -width.toFloat() + paddingEnd)
                super.onDraw(canvas)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (orientation == 0) {
            return super.onTouchEvent(event)
        }
        if (!isEnabled) {
            return false
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                trackTouchEvent(event)
                onSeekBarChangeListener?.onStartTrackingTouch(this)
            }

            MotionEvent.ACTION_MOVE -> {
                trackTouchEvent(event)
            }

            MotionEvent.ACTION_UP -> {
                isPressed = false
                trackTouchEvent(event)
                invalidate()
                onSeekBarChangeListener?.onStopTrackingTouch(this)
            }

            MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                invalidate()
                stopTrackTouchLevel()
                onSeekBarChangeListener?.onStopTrackingTouch(this)
            }
        }
        return true
    }

    fun stopTrackTouchLevel() {
        if (level > 0) {
            val newLevel = (progress.toFloat() / 100 * 4).roundToInt()
            setProgress((newLevel.toFloat() / level * 100).toInt())
        }
    }

    private fun trackTouchEvent(event: MotionEvent) {
        val y = event.y.roundToInt()
        progress = if (y < paddingTop) {
            0
        } else if (y > height - paddingBottom) {
            max
        } else {
            val availableHeight: Int = height - paddingTop - paddingBottom
            val scale: Float = (y - paddingTop) / availableHeight.toFloat()
            (scale * max).roundToInt()
        }
        stopTrackTouchLevel()
        if (thumb != null) {
            calculateDrawable(width, height)
            invalidate()
        }
        onSeekBarChangeListener?.onProgressChanged(this, progress, true)
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\CommSeekBar.kt =====

package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatSeekBar
import com.mpdc4gsr.libunified.R
import kotlin.math.roundToInt

class CommSeekBar : AppCompatSeekBar {
    private val orientation: Int
    private var mMaxWidth = 48
    private var mMaxHeight = 48
    private var mMinWidth = 24
    private var mMinHeight = 24
    var level = 0;
    private var onSeekBarChangeListener: OnSeekBarChangeListener? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.CommSeekBar, defStyleAttr, 0)
        orientation = typedArray.getInt(R.styleable.CommSeekBar_android_orientation, 0)
        mMaxWidth =
            typedArray.getDimensionPixelSize(R.styleable.CommSeekBar_android_maxWidth, mMaxWidth)
        mMaxHeight =
            typedArray.getDimensionPixelSize(R.styleable.CommSeekBar_android_maxHeight, mMaxHeight)
        mMinWidth =
            typedArray.getDimensionPixelSize(R.styleable.CommSeekBar_android_minWidth, mMinWidth)
        mMinHeight =
            typedArray.getDimensionPixelSize(R.styleable.CommSeekBar_android_minHeight, mMinHeight)
        typedArray.recycle()
    }

    override fun setOnSeekBarChangeListener(l: OnSeekBarChangeListener?) {
        if (orientation == 0) {
            super.setOnSeekBarChangeListener(l)
        } else {
            onSeekBarChangeListener = l
        }
    }

    override fun setProgress(progress: Int) {
        super.setProgress(progress)
        if (orientation != 0) {
            onSeekBarChangeListener?.onProgressChanged(this, progress, false)
        }
    }

    override fun setProgress(progress: Int, animate: Boolean) {
        super.setProgress(progress, animate)
        if (orientation != 0) {
            onSeekBarChangeListener?.onProgressChanged(this, progress, false)
        }
    }

    override fun setMax(max: Int) {
        super.setMax(max)
        if (orientation != 0) {
            onSeekBarChangeListener?.onProgressChanged(this, progress, false)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (orientation == 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        } else {
            val d = progressDrawable
            val thumbWidth = thumb?.intrinsicWidth ?: 0
            var dw = 0
            var dh = 0
            if (d != null) {
                dw = mMinWidth.coerceAtLeast(mMaxWidth.coerceAtMost(d.intrinsicWidth))
                dw = thumbWidth.coerceAtLeast(dw)
                dh = mMinHeight.coerceAtLeast(mMaxHeight.coerceAtMost(d.intrinsicHeight))
            }
            dw += paddingLeft + paddingRight
            dh += paddingTop + paddingBottom
            setMeasuredDimension(
                resolveSizeAndState(dw, widthMeasureSpec, 0),
                resolveSizeAndState(dh, heightMeasureSpec, 0)
            )
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (orientation != 0) {
            calculateDrawable(w, h)
        }
    }

    private fun calculateDrawable(w: Int, h: Int) {
        val paddingWidth: Int = w - paddingLeft - paddingRight
        val paddingHeight: Int = h - paddingTop - paddingBottom
        val trackWidth = mMaxWidth.coerceAtMost(paddingWidth)
        val thumbWidth = thumb?.intrinsicWidth ?: 0
        val thumbHeight = thumb?.intrinsicHeight ?: 0
        val trackOffset: Int
        val thumbTopOffset: Int
        if (thumbWidth > trackWidth) {
            val offsetHeight = (paddingWidth - thumbWidth) / 2
            trackOffset = offsetHeight + (thumbWidth - trackWidth) / 2
            thumbTopOffset = offsetHeight
        } else {
            val offsetHeight = (paddingWidth - trackWidth) / 2
            trackOffset = offsetHeight
            thumbTopOffset = offsetHeight + (trackWidth - thumbWidth) / 2
        }
        if (progressDrawable != null) {
            progressDrawable.setBounds(0, trackOffset, paddingHeight, trackOffset + trackWidth)
        }
        if (thumb != null) {
            val available: Int = paddingHeight - thumbHeight + thumbOffset * 2
            val left = progress / max.toFloat() * available + 0.5f
            val reviseLeft = left.coerceAtLeast(thumbHeight / 2 + 0.5f)
                .coerceAtMost(paddingHeight - thumbHeight / 2 - 0.5f).toInt()
            thumb.setBounds(
                reviseLeft,
                thumbTopOffset,
                reviseLeft + thumbHeight,
                thumbTopOffset + thumbWidth
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (orientation == 0) {
            super.onDraw(canvas)
        } else {
            canvas?.let {
                it.rotate(90f)
                it.translate(-paddingStart.toFloat(), -width.toFloat() + paddingEnd)
                super.onDraw(canvas)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (orientation == 0) {
            return super.onTouchEvent(event)
        }
        if (!isEnabled) {
            return false
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                trackTouchEvent(event)
                onSeekBarChangeListener?.onStartTrackingTouch(this)
            }

            MotionEvent.ACTION_MOVE -> {
                trackTouchEvent(event)
            }

            MotionEvent.ACTION_UP -> {
                isPressed = false
                trackTouchEvent(event)
                invalidate()
                onSeekBarChangeListener?.onStopTrackingTouch(this)
            }

            MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                invalidate()
                stopTrackTouchLevel()
                onSeekBarChangeListener?.onStopTrackingTouch(this)
            }
        }
        return true
    }

    fun stopTrackTouchLevel() {
        if (level > 0) {
            val newLevel = (progress.toFloat() / 100 * 4).roundToInt()
            setProgress((newLevel.toFloat() / level * 100).toInt())
        }
    }

    private fun trackTouchEvent(event: MotionEvent) {
        val y = event.y.roundToInt()
        progress = if (y < paddingTop) {
            0
        } else if (y > height - paddingBottom) {
            max
        } else {
            val availableHeight: Int = height - paddingTop - paddingBottom
            val scale: Float = (y - paddingTop) / availableHeight.toFloat()
            (scale * max).roundToInt()
        }
        stopTrackTouchLevel()
        if (thumb != null) {
            calculateDrawable(width, height)
            invalidate()
        }
        onSeekBarChangeListener?.onProgressChanged(this, progress, true)
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\CountDownView.kt =====

package com.mpdc4gsr.libunified.ui.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.mpdc4gsr.libunified.R

class CountDownView : View {
    //
    private var mRingColor = 0

    //
    private var mRingWidth = 0

    //
    private var mRingProgressTextSize = 0

    //
    private var mWidth = 0

    //
    private var mHeight = 0

    //
    private var mRingText: String? = null
    private lateinit var mPaint: Paint
    private lateinit var mTextPaint: Paint

    //
    private var mRectF: RectF? = null

    //
    private var mProgressTextColor = 0
    private var mCountdownTime = 0
    private var mCurrentProgress = 0f
    private var valueAnimator: ValueAnimator? = null
    private var mListener: OnCountDownListener? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.CountDownView)
        for (i in 0 until ta.indexCount) {
            when (ta.getIndex(i)) {
                R.styleable.CountDownView_ringColor -> mRingColor =
                    ta.getColor(
                        R.styleable.CountDownView_ringColor,
                        ContextCompat.getColor(context, R.color.colorAccent)
                    )

                R.styleable.CountDownView_ringWidth -> mRingWidth =
                    ta.getDimensionPixelSize(
                        R.styleable.CountDownView_ringWidth,
                        40
                    )

                R.styleable.CountDownView_progressTextSize -> mRingProgressTextSize =
                    ta.getDimensionPixelSize(
                        R.styleable.CountDownView_progressTextSize,
                        20
                    )

                R.styleable.CountDownView_progressTextColor -> mProgressTextColor =
                    ta.getColor(
                        R.styleable.CountDownView_progressTextColor,
                        ContextCompat.getColor(context, R.color.colorAccent)
                    )

                R.styleable.CountDownView_countdownTime -> mCountdownTime =
                    ta.getInteger(
                        R.styleable.CountDownView_countdownTime,
                        60
                    )

                R.styleable.CountDownView_progressText -> mRingText =
                    ta.getString(R.styleable.CountDownView_progressText)
            }
        }
        ta.recycle()
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPaint.isAntiAlias = true
        mTextPaint = Paint()
        this.setWillNotDraw(false)
    }

    @SuppressLint("DrawAllocation")
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        mWidth = measuredWidth
        mHeight = measuredHeight
        mRectF = RectF(
            0 + mRingWidth / 2f,
            0 + mRingWidth / 2f,
            mWidth - mRingWidth / 2f,
            mHeight - mRingWidth / 2f
        )
    }

    fun setCountdownTime(mCountdownTime: Int) {
        this.mCountdownTime = mCountdownTime
        mRingText = mCountdownTime.toString()
        invalidate()
    }

    private fun getValueAnimator(countdownTime: Long): ValueAnimator? {
        val valueAnimator = ValueAnimator.ofFloat(0f, 100f)
        valueAnimator.duration = countdownTime
        valueAnimator.interpolator = LinearInterpolator()
        valueAnimator.repeatCount = 0
        return valueAnimator
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //
        mPaint.color = mRingColor
        mPaint.style = Paint.Style.FILL
        mPaint.strokeWidth = mRingWidth.toFloat()
        canvas.drawArc(mRectF!!, -90f, mCurrentProgress - 360, false, mPaint)
        val font = ResourcesCompat.getFont(context, R.font.roboto_regular)
        //
        mTextPaint.isAntiAlias = true
        mTextPaint.textAlign = Paint.Align.CENTER
        mTextPaint.typeface = font
        // (5 4 3 2 1)
        // val text: String = (mCountdownTime - (mCurrentProgress / 360f * mCountdownTime)).toInt().toString()
        mTextPaint.textSize = mRingProgressTextSize.toFloat()
        mTextPaint.color = mProgressTextColor
        //
        val fontMetrics = mTextPaint.fontMetricsInt
        val baseline =
            ((mRectF!!.bottom + mRectF!!.top - fontMetrics.bottom - fontMetrics.top) / 2).toInt()
        canvas.drawText(mRingText!!, mRectF!!.centerX(), baseline.toFloat(), mTextPaint)
    }

    fun startCountDown() {
        if (!isAttachedToWindow) {
            return
        }
        valueAnimator = getValueAnimator((mCountdownTime * 1000).toLong())
        valueAnimator!!.addUpdateListener { animation ->
            val i = animation.animatedValue.toString().toFloat()
            mCurrentProgress = (360 * (i / 100f))
            invalidate()
        }
        if (isAttachedToWindow) {
            valueAnimator!!.start()
        }
        valueAnimator!!.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                //
                if (mListener != null) {
                    mListener!!.countDownFinished()
                }
            }
        })
    }

    fun stopCountDown() {
        if (valueAnimator!!.isRunning) {
            valueAnimator!!.cancel()
        }
    }

    fun setOnCountDownListener(mListener: OnCountDownListener) {
        this.mListener = mListener
    }

    interface OnCountDownListener {
        fun countDownFinished()
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\IndicateView.java =====

package com.mpdc4gsr.libunified.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.mpdc4gsr.libunified.R;

public class IndicateView extends View {
    private int selectColor = Color.parseColor("#06AAFF");
    private int unSelectColor = Color.parseColor("#80FFFFFF");
    private Paint paint;
    private int count = 0;
    private int currentIndex = 0;
    private float radius = 6f;

    public IndicateView(Context context) {
        this(context, null);
    }

    public IndicateView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IndicateView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttributes(context, attrs);
        initPaint();
    }

    private void initAttributes(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.IndicateView);
            selectColor = typedArray.getColor(R.styleable.IndicateView_selectColor, Color.parseColor("#06AAFF"));
            typedArray.recycle();
        }
    }

    private void initPaint() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
    }

    public void setCount(int count) {
        this.count = count;
        invalidate();
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = count * (int) (radius * 2) + (count - 1) * (int) (radius * 2);
        int height = (int) (radius * 2);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (count <= 0) return;

        float centerY = getHeight() / 2f;
        float startX = radius;

        for (int i = 0; i < count; i++) {
            paint.setColor(i == currentIndex ? selectColor : unSelectColor);
            float centerX = startX + i * (radius * 4);
            canvas.drawCircle(centerX, centerY, radius, paint);
        }
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\LiteSurfaceView.kt =====

package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.SurfaceView
import java.nio.ByteBuffer

class LiteSurfaceView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : SurfaceView(context, attrs) {
    var mFinalImageWidth = 0
    var mFinalImageHeight = 0
    var tmpData: ByteArray? = null
    var mIrRotateData: ByteArray? = null
    var imageBitmap: Bitmap? = null
    fun scaleBitmap(): Bitmap {
        try {
            val irData =
                mIrRotateData ?: return Bitmap.createBitmap(
                    measuredWidth,
                    measuredHeight,
                    Bitmap.Config.ARGB_8888
                )
            if (tmpData == null) {
                tmpData = ByteArray(irData.size)
            }
            val tempData = tmpData ?: return Bitmap.createBitmap(
                measuredWidth,
                measuredHeight,
                Bitmap.Config.ARGB_8888
            )
            System.arraycopy(irData, 0, tempData, 0, irData.size)
            if (imageBitmap == null || imageBitmap!!.getWidth() != mFinalImageWidth) {
                imageBitmap =
                    Bitmap.createBitmap(
                        mFinalImageWidth,
                        mFinalImageHeight,
                        Bitmap.Config.ARGB_8888
                    )
            }
            imageBitmap?.copyPixelsFromBuffer(ByteBuffer.wrap(tempData))
            return Bitmap.createScaledBitmap(
                imageBitmap!!,
                measuredWidth, measuredHeight, true
            )
        } catch (e: Exception) {
            return Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
        }
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\MarqueeButton.kt =====

package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton

class MarqueeButton : AppCompatButton {
    constructor (context: Context) : super(context)
    constructor (context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor (context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    override fun isFocused(): Boolean {
        return true
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\MarqueeText.kt =====

package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class MarqueeText : AppCompatTextView {
    constructor (context: Context) : super(context)
    constructor (context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor (context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    override fun isFocused(): Boolean {
        return true
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\MyItemDecoration.kt =====

package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

class MyItemDecoration(context: Context) : RecyclerView.ItemDecoration() {
    var wholeLeft: Float? = null
    var wholeRight: Float? = null
    var wholeTop: Float? = null
    var wholeBottom: Float? = null
    var itemLeft: Float? = null
    var itemRight: Float? = null
    var itemTop: Float? = null
    var itemBottom: Float? = null
    private val density: Float = context.resources.displayMetrics.density
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        val itemCount = parent.adapter?.itemCount ?: return
        val position = parent.getChildAdapterPosition(view)
        val layoutManager = parent.layoutManager
        when (layoutManager) {
            is GridLayoutManager -> {
                if (layoutManager.orientation == LinearLayoutManager.VERTICAL) {
                    setVerticalMulti(outRect, position, itemCount, layoutManager.spanCount)
                } else {
                    setHorizontalMulti(outRect, position, itemCount, layoutManager.spanCount)
                }
            }

            is LinearLayoutManager -> {
                if (layoutManager.orientation == LinearLayoutManager.VERTICAL) {
                    setVerticalOne(outRect, position, itemCount)
                } else {
                    setHorizontalOne(outRect, position, itemCount)
                }
            }

            is StaggeredGridLayoutManager -> {
                val layoutParams = view.layoutParams
                val spanIndex =
                    if (layoutParams is StaggeredGridLayoutManager.LayoutParams) layoutParams.spanIndex else 0
                if (layoutManager.orientation == LinearLayoutManager.VERTICAL) {
                    setVerticalMultiStaggered(
                        outRect,
                        position,
                        itemCount,
                        layoutManager.spanCount,
                        spanIndex
                    )
                } else {
                    setHorizontalMulti(outRect, position, itemCount, layoutManager.spanCount)
                }
            }
        }
    }

    private fun setVerticalOne(outRect: Rect, position: Int, itemCount: Int) {
        val left: Int = dp2px(wholeLeft ?: ((itemLeft ?: 0f) * 2))
        val right: Int = dp2px(wholeRight ?: ((itemRight ?: 0f) * 2))
        val top: Int =
            dp2px(if (position == 0) wholeTop ?: ((itemTop ?: 0f) * 2) else (itemTop ?: 0f))
        val bottom: Int =
            dp2px(
                if (position == itemCount - 1) wholeBottom ?: ((itemBottom
                    ?: 0f) * 2) else (itemBottom ?: 0f)
            )
        outRect.set(left, top, right, bottom)
    }

    private fun setHorizontalOne(outRect: Rect, position: Int, itemCount: Int) {
        val left: Int =
            dp2px(if (position == 0) wholeLeft ?: ((itemLeft ?: 0f) * 2) else (itemLeft ?: 0f))
        val right: Int =
            dp2px(
                if (position == itemCount - 1) wholeRight ?: ((itemRight ?: 0f) * 2) else (itemRight
                    ?: 0f)
            )
        val top: Int = dp2px(wholeTop ?: ((itemTop ?: 0f) * 2))
        val bottom: Int = dp2px(wholeBottom ?: ((itemBottom ?: 0f) * 2))
        outRect.set(left, top, right, bottom)
    }

    private fun setVerticalMulti(outRect: Rect, position: Int, itemCount: Int, spanCount: Int) {
        val totalRow = itemCount / spanCount + if (itemCount % spanCount == 0) 0 else 1 //
        val rowPosition = position / spanCount    // position [0, totalRow)
        val columnPosition = position % spanCount // position [0, spanCount)
        val left: Int = dp2px(
            if (columnPosition == 0) wholeLeft ?: ((itemLeft ?: 0f) * 2) else (itemLeft ?: 0f)
        )
        val right: Int =
            dp2px(
                if (columnPosition == spanCount - 1) wholeRight ?: ((itemRight
                    ?: 0f) * 2) else (itemRight ?: 0f)
            )
        val top: Int =
            dp2px(if (rowPosition == 0) wholeTop ?: ((itemTop ?: 0f) * 2) else (itemTop ?: 0f))
        val bottom: Int =
            dp2px(
                if (rowPosition == totalRow - 1) wholeBottom ?: ((itemBottom
                    ?: 0f) * 2) else (itemBottom ?: 0f)
            )
        outRect.set(left, top, right, bottom)
    }

    private fun setVerticalMultiStaggered(
        outRect: Rect,
        position: Int,
        itemCount: Int,
        spanCount: Int,
        spanIndex: Int
    ) {
        val totalRow = itemCount / spanCount + if (itemCount % spanCount == 0) 0 else 1 //
        val rowPosition = position / spanCount //position[0, totalRow)
        val left: Int =
            dp2px(if (spanIndex == 0) wholeLeft ?: ((itemLeft ?: 0f) * 2) else (itemLeft ?: 0f))
        val right: Int =
            dp2px(
                if (spanIndex == spanCount - 1) wholeRight ?: ((itemRight
                    ?: 0f) * 2) else (itemRight ?: 0f)
            )
        val top: Int =
            dp2px(if (rowPosition == 0) wholeTop ?: ((itemTop ?: 0f) * 2) else (itemTop ?: 0f))
        val bottom: Int =
            dp2px(
                if (rowPosition == totalRow - 1) wholeBottom ?: ((itemBottom
                    ?: 0f) * 2) else (itemBottom ?: 0f)
            )
        outRect.set(left, top, right, bottom)
    }

    private fun setHorizontalMulti(outRect: Rect, position: Int, itemCount: Int, spanCount: Int) {
        // MVP implementation: Basic horizontal multi-row spacing
        // Can be enhanced when horizontal multi-row requirements are clarified
        val column = position % spanCount
        val row = position / spanCount
        val left: Int =
            dp2px(if (column == 0) wholeLeft ?: ((itemLeft ?: 0f) * 2) else (itemLeft ?: 0f))
        val right: Int =
            dp2px(
                if (column == spanCount - 1) wholeRight ?: ((itemRight ?: 0f) * 2) else (itemRight
                    ?: 0f)
            )
        val top: Int = dp2px(if (row == 0) wholeTop ?: ((itemTop ?: 0f) * 2) else (itemTop ?: 0f))
        val bottom: Int = dp2px(wholeBottom ?: ((itemBottom ?: 0f) * 2))
        outRect.set(left, top, right, bottom)
    }

    private fun dp2px(dpValue: Float): Int = (dpValue * density + 0.5f).toInt()
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\NoScrollViewPager.kt =====

package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

class NoScrollViewPager : ViewPager {
    private var isCanScroll = false

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return isCanScroll && super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        return isCanScroll && super.onTouchEvent(ev)
    }

    override fun setCurrentItem(item: Int) {
        //
        super.setCurrentItem(item, false)
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\RadioGroupPlus.kt =====

package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.annotation.IdRes

class RadioGroupPlus : LinearLayout {
    // holds the checked id; the selection is empty by default
    @get:IdRes
    var checkedRadioButtonId = -1
        private set

    // tracks children radio buttons checked state
    private var mChildOnCheckedChangeListener: CompoundButton.OnCheckedChangeListener? = null

    // when true, mOnCheckedChangeListener discards events
    private var mProtectFromCheckedChange = false
    private var mOnCheckedChangeListener: OnCheckedChangeListener? = null
    private var mPassThroughListener: PassThroughHierarchyChangeListener? = null

    constructor(context: Context?) : super(context) {
        orientation = VERTICAL
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        // MVP implementation: Basic attribute handling for RadioGroupPlus
        // Enhanced attribute processing can be added as needed
        init()
    }

    private fun init() {
        mChildOnCheckedChangeListener = CheckedStateTracker()
        mPassThroughListener = PassThroughHierarchyChangeListener()
        super.setOnHierarchyChangeListener(mPassThroughListener)
    }

    override fun setOnHierarchyChangeListener(listener: OnHierarchyChangeListener) {
        // the user listener is delegated to our pass-through listener
        mPassThroughListener!!.mOnHierarchyChangeListener = listener
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        // checks the appropriate radio button as requested in the XML file
        if (checkedRadioButtonId != -1) {
            mProtectFromCheckedChange = true
            setCheckedStateForView(checkedRadioButtonId, true)
            mProtectFromCheckedChange = false
            setCheckedId(checkedRadioButtonId)
        }
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        if (child is RadioButton) {
            val button = child
            if (button.isChecked) {
                mProtectFromCheckedChange = true
                if (checkedRadioButtonId != -1) {
                    setCheckedStateForView(checkedRadioButtonId, false)
                }
                mProtectFromCheckedChange = false
                setCheckedId(button.id)
            }
        }
        super.addView(child, index, params)
    }

    fun check(@IdRes id: Int) {
        // don't even bother
        if (id != -1 && id == checkedRadioButtonId) {
            return
        }
        if (checkedRadioButtonId != -1) {
            setCheckedStateForView(checkedRadioButtonId, false)
        }
        if (id != -1) {
            setCheckedStateForView(id, true)
        }
        setCheckedId(id)
    }

    private fun setCheckedId(@IdRes id: Int) {
        checkedRadioButtonId = id
        if (mOnCheckedChangeListener != null) {
            mOnCheckedChangeListener!!.onCheckedChanged(this, checkedRadioButtonId)
        }
    }

    private fun setCheckedStateForView(viewId: Int, checked: Boolean) {
        val checkedView = findViewById<View>(viewId)
        if (checkedView != null && checkedView is RadioButton) {
            checkedView.isChecked = checked
        }
    }

    fun clearCheck() {
        check(-1)
    }

    fun setOnCheckedChangeListener(listener: OnCheckedChangeListener) {
        mOnCheckedChangeListener = listener
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams): Boolean {
        return p is RadioGroup.LayoutParams
    }

    override fun generateDefaultLayoutParams(): LinearLayout.LayoutParams {
        return LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun getAccessibilityClassName(): CharSequence {
        return RadioGroup::class.java.name
    }

    class LayoutParams : LinearLayout.LayoutParams {
        constructor(c: Context?, attrs: AttributeSet?) : super(c, attrs) {}
        constructor(w: Int, h: Int) : super(w, h) {}
        constructor(w: Int, h: Int, initWeight: Float) : super(w, h, initWeight) {}
        constructor(p: ViewGroup.LayoutParams?) : super(p) {}
        constructor(source: MarginLayoutParams?) : super(source) {}

        override fun setBaseAttributes(
            a: TypedArray,
            widthAttr: Int, heightAttr: Int
        ) {
            width = if (a.hasValue(widthAttr)) {
                a.getLayoutDimension(widthAttr, "layout_width")
            } else {
                WRAP_CONTENT
            }
            height = if (a.hasValue(heightAttr)) {
                a.getLayoutDimension(heightAttr, "layout_height")
            } else {
                WRAP_CONTENT
            }
        }
    }

    interface OnCheckedChangeListener {
        fun onCheckedChanged(group: RadioGroupPlus, @IdRes checkedId: Int)
    }

    private inner class CheckedStateTracker : CompoundButton.OnCheckedChangeListener {
        override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
            // prevents from infinite recursion
            if (mProtectFromCheckedChange) {
                return
            }
            mProtectFromCheckedChange = true
            if (checkedRadioButtonId != -1) {
                setCheckedStateForView(checkedRadioButtonId, false)
            }
            mProtectFromCheckedChange = false
            val id = buttonView.id
            setCheckedId(id)
        }
    }

    private inner class PassThroughHierarchyChangeListener :
        OnHierarchyChangeListener {
        var mOnHierarchyChangeListener: OnHierarchyChangeListener? = null
        fun traverseTree(view: View) {
            if (view is RadioButton) {
                var id = view.getId()
                // generates an id if it's missing
                if (id == NO_ID) {
                    id = generateViewId()
                    view.setId(id)
                }
                view.setOnCheckedChangeListener(
                    mChildOnCheckedChangeListener
                )
            }
            if (view !is ViewGroup) {
                return
            }
            val viewGroup = view
            if (viewGroup.childCount == 0) {
                return
            }
            for (i in 0 until viewGroup.childCount) {
                traverseTree(viewGroup.getChildAt(i))
            }
        }

        override fun onChildViewAdded(parent: View, child: View) {
            traverseTree(child)
            if (parent === this@RadioGroupPlus && child is RadioButton) {
                var id = child.getId()
                // generates an id if it's missing
                if (id == NO_ID) {
                    id = generateViewId()
                    child.setId(id)
                }
                child.setOnCheckedChangeListener(
                    mChildOnCheckedChangeListener
                )
            }
            mOnHierarchyChangeListener?.onChildViewAdded(parent, child)
        }

        override fun onChildViewRemoved(parent: View, child: View) {
            if (parent === this@RadioGroupPlus && child is RadioButton) {
                child.setOnCheckedChangeListener(null)
            }
            mOnHierarchyChangeListener?.onChildViewRemoved(parent, child)
        }
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\RoundImageView.kt =====

package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.mpdc4gsr.libunified.R

class RoundImageView : AppCompatImageView {
    companion object {
        const val LEFT_TOP = 1
        const val RIGHT_TOP = 2
        const val LEFT_BOTTOM = 4
        const val RIGHT_BOTTOM = 8
        private const val DEFAULT_RADIUS = 10f
        private const val DEFAULT_POSITION = 15
    }

    var position = 0 //
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }
    private var radius = 0 //ï¼Œ px
    private val path = Path()//
    private var density = 0f //ï¼Œdppx

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        density = context.resources.displayMetrics.density
        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.RoundImageView, defStyleAttr, 0)
        radius = typedArray.getDimensionPixelSize(
            R.styleable.RoundImageView_round_radius,
            dp2px(DEFAULT_RADIUS)
        )
        position = typedArray.getInt(R.styleable.RoundImageView_round_position, DEFAULT_POSITION)
        typedArray.recycle()
    }

    override fun onDraw(canvas: Canvas) {
        path.rewind()
        if (position and LEFT_TOP == LEFT_TOP) {
            path.moveTo(radius.toFloat(), 0f)
        }
        if (position and RIGHT_TOP == RIGHT_TOP) {
            path.lineTo((width - radius).toFloat(), 0f)
            path.quadTo(width.toFloat(), 0f, width.toFloat(), radius.toFloat())
        } else {
            path.lineTo(width.toFloat(), 0f)
        }
        if (position and RIGHT_BOTTOM == RIGHT_BOTTOM) {
            path.lineTo(width.toFloat(), (height - radius).toFloat())
            path.quadTo(
                width.toFloat(),
                height.toFloat(),
                (width - radius).toFloat(),
                height.toFloat()
            )
        } else {
            path.lineTo(width.toFloat(), height.toFloat())
        }
        if (position and LEFT_BOTTOM == LEFT_BOTTOM) {
            path.lineTo(radius.toFloat(), height.toFloat())
            path.quadTo(0f, height.toFloat(), 0f, (height - radius).toFloat())
        } else {
            path.lineTo(0f, height.toFloat())
        }
        if (position and LEFT_TOP == LEFT_TOP) {
            path.lineTo(0f, radius.toFloat())
            path.quadTo(0f, 0f, radius.toFloat(), 0f)
        } else {
            path.lineTo(0f, 0f)
        }
        canvas.clipPath(path)
        super.onDraw(canvas)
    }

    fun setRadius(radius: Float) {
        if (this.radius != dp2px(radius)) {
            this.radius = dp2px(radius)
            invalidate()
        }
    }

    private fun dp2px(dpValue: Float): Int {
        return (dpValue * density + 0.5f).toInt()
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\seekbar\OnRangeChangedListener.java =====

package com.mpdc4gsr.libunified.ui.widget.seekbar;

import androidx.annotation.NonNull;

public interface OnRangeChangedListener {
    void onRangeChanged(@NonNull RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser, int tempMode);

    void onStartTrackingTouch(@NonNull RangeSeekBar view, boolean isLeft);

    void onStopTrackingTouch(@NonNull RangeSeekBar view, boolean isLeft);
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\seekbar\RangeSeekBar.java =====

package com.mpdc4gsr.libunified.ui.widget.seekbar;

import static com.mpdc4gsr.libunified.ui.widget.seekbar.SeekBar.INDICATOR_ALWAYS_HIDE;
import static com.mpdc4gsr.libunified.ui.widget.seekbar.SeekBar.INDICATOR_ALWAYS_SHOW;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import com.mpdc4gsr.libunified.R;
import com.mpdc4gsr.libunified.app.menu.util.PseudoColorConfig;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class RangeSeekBar extends View {

    public final static int SEEKBAR_MODE_SINGLE = 1;
    public final static int SEEKBAR_MODE_RANGE = 2;
    public final static int TEMP_MODE_CLOSE = 0;//close
    public final static int TEMP_MODE_MAX = 2;//
    public final static int TEMP_MODE_MIN = 1;//
    public final static int TEMP_MODE_INTERVAL = 3;//
    public final static int TRICK_MARK_MODE_NUMBER = 0;
    public final static int TRICK_MARK_MODE_OTHER = 1;
    public final static int TICK_MARK_GRAVITY_LEFT = 0;
    public final static int TICK_MARK_GRAVITY_CENTER = 1;
    public final static int TICK_MARK_GRAVITY_RIGHT = 2;
    private final static int MIN_INTERCEPT_DISTANCE = 100;
    float touchDownX, touchDownY;
    float reservePercent;
    boolean isScaleThumb = false;
    Paint paint = new Paint();
    RectF progressDefaultDstRect = new RectF();
    RectF progressDstRect = new RectF();
    Rect progressSrcRect = new Rect();
    RectF stepDivRect = new RectF();
    Rect tickMarkTextRect = new Rect();
    SeekBar leftSB;
    SeekBar rightSB;
    SeekBar currTouchSB;
    Bitmap progressBitmap;
    Bitmap progressDefaultBitmap;
    List<Bitmap> stepsBitmaps = new ArrayList<>();
    Long updateTime = System.currentTimeMillis();
    private int pseudocode = 3;
    private boolean noNegativeNumber = false;
    private int tempMode = TEMP_MODE_CLOSE;
    private int progressTop, progressBottom, progressLeft, progressRight;
    private int seekBarMode;
    private int tickMarkMode;
    private int tickMarkTextMargin;
    private int tickMarkTextSize;
    private int tickMarkGravity;
    private int tickMarkLayoutGravity;
    private int tickMarkTextColor;
    private int tickMarkInRangeTextColor;
    private CharSequence[] tickMarkTextArray;
    private float progressRadius;
    private int progressColor;
    private int progressDefaultColor;
    private int progressDrawableId;
    private int progressDefaultDrawableId;
    private int progressHeight;
    private int progressWidth;
    private float minInterval;
    private int gravity;
    private boolean enableThumbOverlap;
    private int stepsColor;
    private float stepsWidth;
    private float stepsHeight;
    private float stepsRadius;
    private int steps;
    private boolean stepsAutoBonding;
    private int stepsDrawableId;
    private float minProgress, maxProgress;
    private boolean isEnable = true;
    private int progressPaddingRight;
    private OnRangeChangedListener callback;
    @Nullable
    private int[] colorList;
    @Nullable
    private float[] places;

    public RangeSeekBar(Context context) {
        this(context, null);
    }

    public RangeSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(attrs);
        initPaint();
        initSeekBar(attrs);
        initStepsBitmap();
    }

    public int getTempMode() {
        return tempMode;
    }

    public void setTempMode(int tempMode) {
        this.tempMode = tempMode;
    }

    private void updateTempModeState() {
        if (tempMode == TEMP_MODE_CLOSE) {
            if (currTouchSB == leftSB) {
                tempMode = TEMP_MODE_MIN;
            } else if (currTouchSB == rightSB) {
                tempMode = TEMP_MODE_MAX;
            }
        } else if (tempMode == TEMP_MODE_MIN) {
            if (currTouchSB == rightSB) {
                tempMode = TEMP_MODE_INTERVAL;
            }
        } else if (tempMode == TEMP_MODE_MAX) {
            if (currTouchSB == leftSB) {
                tempMode = TEMP_MODE_INTERVAL;
            }
        }
    }

    private void initProgressBitmap() {
        if (progressBitmap == null) {
            progressBitmap = Utils.drawableToBitmap(getContext(), progressWidth, progressHeight, progressDrawableId);
        }
        if (progressDefaultBitmap == null) {
            progressDefaultBitmap = Utils.drawableToBitmap(getContext(), progressWidth, progressHeight, progressDefaultDrawableId);
        }
    }

    private boolean verifyStepsMode() {
        if (steps < 1 || stepsHeight <= 0 || stepsWidth <= 0) return false;
        return true;
    }

    private void initStepsBitmap() {
        if (!verifyStepsMode() || stepsDrawableId == 0) return;
        if (stepsBitmaps.isEmpty()) {
            Bitmap bitmap = Utils.drawableToBitmap(getContext(), (int) stepsWidth, (int) stepsHeight, stepsDrawableId);
            for (int i = 0; i <= steps; i++) {
                stepsBitmaps.add(bitmap);
            }
        }
    }

    private void initSeekBar(AttributeSet attrs) {
        leftSB = new SeekBar(this, attrs, true);
        rightSB = new SeekBar(this, attrs, false);
        rightSB.setVisible(seekBarMode != SEEKBAR_MODE_SINGLE);
    }

    private void initAttrs(AttributeSet attrs) {
        try {
            TypedArray t = getContext().obtainStyledAttributes(attrs, R.styleable.RangeSeekBar);
            seekBarMode = t.getInt(R.styleable.RangeSeekBar_rsb_mode, SEEKBAR_MODE_RANGE);
            minProgress = t.getFloat(R.styleable.RangeSeekBar_rsb_min, 0);
            maxProgress = t.getFloat(R.styleable.RangeSeekBar_rsb_max, 100);
            minInterval = t.getFloat(R.styleable.RangeSeekBar_rsb_min_interval, 0);
            gravity = t.getInt(R.styleable.RangeSeekBar_rsb_gravity, Gravity.TOP);
            progressColor = t.getColor(R.styleable.RangeSeekBar_rsb_progress_color, -1);
            progressRadius = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_progress_radius, -1);
            progressDefaultColor = t.getColor(R.styleable.RangeSeekBar_rsb_progress_default_color, -1);
            progressDrawableId = t.getResourceId(R.styleable.RangeSeekBar_rsb_progress_drawable, 0);
            progressDefaultDrawableId = t.getResourceId(R.styleable.RangeSeekBar_rsb_progress_drawable_default, 0);
            progressHeight = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_progress_height, Utils.dp2px(getContext(), 2));
            tickMarkMode = t.getInt(R.styleable.RangeSeekBar_rsb_tick_mark_mode, TRICK_MARK_MODE_NUMBER);
            tickMarkGravity = t.getInt(R.styleable.RangeSeekBar_rsb_tick_mark_gravity, TICK_MARK_GRAVITY_CENTER);
            tickMarkLayoutGravity = t.getInt(R.styleable.RangeSeekBar_rsb_tick_mark_layout_gravity, Gravity.TOP);
            tickMarkTextArray = t.getTextArray(R.styleable.RangeSeekBar_rsb_tick_mark_text_array);
            tickMarkTextMargin = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_tick_mark_text_margin, Utils.dp2px(getContext(), 7));
            tickMarkTextSize = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_tick_mark_text_size, Utils.dp2px(getContext(), 12));
            tickMarkTextColor = t.getColor(R.styleable.RangeSeekBar_rsb_tick_mark_text_color, progressDefaultColor);
            tickMarkInRangeTextColor = t.getColor(R.styleable.RangeSeekBar_rsb_tick_mark_in_range_text_color, progressColor);
            steps = t.getInt(R.styleable.RangeSeekBar_rsb_steps, 0);
            stepsColor = t.getColor(R.styleable.RangeSeekBar_rsb_step_color, 0xFF9d9d9d);
            stepsRadius = t.getDimension(R.styleable.RangeSeekBar_rsb_step_radius, 0);
            stepsWidth = t.getDimension(R.styleable.RangeSeekBar_rsb_step_width, 0);
            stepsHeight = t.getDimension(R.styleable.RangeSeekBar_rsb_step_height, 0);
            stepsDrawableId = t.getResourceId(R.styleable.RangeSeekBar_rsb_step_drawable, 0);
            stepsAutoBonding = t.getBoolean(R.styleable.RangeSeekBar_rsb_step_auto_bonding, true);
            t.recycle();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    protected void onMeasureProgress(int w, int h) {
        int viewHeight = h - getPaddingBottom() - getPaddingTop();
        if (h <= 0) return;

        if (gravity == Gravity.TOP) {

            float maxIndicatorHeight = 0;
            if (leftSB.getIndicatorShowMode() != INDICATOR_ALWAYS_HIDE
                    || rightSB.getIndicatorShowMode() != INDICATOR_ALWAYS_HIDE) {
                maxIndicatorHeight = Math.max(leftSB.getIndicatorRawHeight(), rightSB.getIndicatorRawHeight());
            }
            float thumbHeight = Math.max(leftSB.getThumbScaleHeight(), rightSB.getThumbScaleHeight());
            thumbHeight -= progressHeight / 2f;

            progressTop = (int) (maxIndicatorHeight + (thumbHeight - progressHeight) / 2f);
            if (tickMarkTextArray != null && tickMarkLayoutGravity == Gravity.TOP) {
                progressTop = (int) Math.max(getTickMarkRawHeight(), maxIndicatorHeight + (thumbHeight - progressHeight) / 2f);
            }
            progressBottom = progressTop + progressHeight;
        } else if (gravity == Gravity.BOTTOM) {
            if (tickMarkTextArray != null && tickMarkLayoutGravity == Gravity.BOTTOM) {
                progressBottom = viewHeight - getTickMarkRawHeight();
            } else {
                progressBottom = (int) (viewHeight - Math.max(leftSB.getThumbScaleHeight(), rightSB.getThumbScaleHeight()) / 2f
                        + progressHeight / 2f);
            }
            progressTop = progressBottom - progressHeight;
        } else {
            progressTop = (viewHeight - progressHeight) / 2;
            progressBottom = progressTop + progressHeight;
        }

        int maxThumbWidth = (int) Math.max(leftSB.getThumbScaleWidth(), rightSB.getThumbScaleWidth());
        progressLeft = maxThumbWidth / 2 + getPaddingLeft();
        progressRight = w - maxThumbWidth / 2 - getPaddingRight();
        progressWidth = progressRight - progressLeft;
        progressDefaultDstRect.set(getProgressLeft(), getProgressTop(), getProgressRight(), getProgressBottom());
        progressPaddingRight = w - progressRight;

        if (progressRadius <= 0) {
            progressRadius = (int) ((getProgressBottom() - getProgressTop()) * 0.15f);
        }
        initProgressBitmap();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        /*
         * onMeasurewidthMeasureSpecheightMeasureSpecï¼Œ
         * MeasureSpec.EXACTLY
         * MeasureSpec.AT_MOST
         * MeasureSpec.UNSPECIFIED
         */

        if (heightMode == MeasureSpec.EXACTLY) {
            heightSize = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY);
        } else if (heightMode == MeasureSpec.AT_MOST && getParent() instanceof ViewGroup
                && heightSize == ViewGroup.LayoutParams.MATCH_PARENT) {
            heightSize = MeasureSpec.makeMeasureSpec(((ViewGroup) getParent()).getMeasuredHeight(), MeasureSpec.AT_MOST);
        } else {
            int heightNeeded;
            if (gravity == Gravity.CENTER) {
                if (tickMarkTextArray != null && tickMarkLayoutGravity == Gravity.BOTTOM) {
                    heightNeeded = (int) (2 * (getRawHeight() - getTickMarkRawHeight()));
                } else {
                    heightNeeded = (int) (2 * (getRawHeight() - Math.max(leftSB.getThumbScaleHeight(), rightSB.getThumbScaleHeight()) / 2));
                }
            } else {
                heightNeeded = (int) getRawHeight();
            }
            heightSize = MeasureSpec.makeMeasureSpec(heightNeeded, MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightSize);
    }

    protected int getTickMarkRawHeight() {
        if (tickMarkTextArray != null && tickMarkTextArray.length > 0) {
            return tickMarkTextMargin + Utils.measureText(String.valueOf(tickMarkTextArray[0]), tickMarkTextSize).height() + 3;
        }
        return 0;
    }

    protected float getRawHeight() {
        float rawHeight;
        if (seekBarMode == SEEKBAR_MODE_SINGLE) {
            rawHeight = leftSB.getRawHeight();
            if (tickMarkLayoutGravity == Gravity.BOTTOM && tickMarkTextArray != null) {
                float h = Math.max((leftSB.getThumbScaleHeight() - progressHeight) / 2, getTickMarkRawHeight());
                rawHeight = rawHeight - leftSB.getThumbScaleHeight() / 2 + progressHeight / 2f + h;
            }
        } else {
            rawHeight = Math.max(leftSB.getRawHeight(), rightSB.getRawHeight());
            if (tickMarkLayoutGravity == Gravity.BOTTOM && tickMarkTextArray != null) {
                float thumbHeight = Math.max(leftSB.getThumbScaleHeight(), rightSB.getThumbScaleHeight());
                float h = Math.max((thumbHeight - progressHeight) / 2, getTickMarkRawHeight());
                rawHeight = rawHeight - thumbHeight / 2 + progressHeight / 2f + h;
            }
        }
        return rawHeight;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        onMeasureProgress(w, h);

        setRange(minProgress, maxProgress, minInterval);

        int lineCenterY = (getProgressBottom() + getProgressTop()) / 2;
        leftSB.onSizeChanged(getProgressLeft(), lineCenterY);
        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            rightSB.onSizeChanged(getProgressLeft(), lineCenterY);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        onDrawTickMark(canvas, paint); //
        onDrawProgressBar(canvas, paint); //
        onDrawSteps(canvas, paint);
        onDrawSeekBar(canvas); //
    }

    protected void onDrawTickMark(Canvas canvas, Paint paint) {
        if (tickMarkTextArray != null) {
            int trickPartWidth = progressWidth / (tickMarkTextArray.length - 1);
            for (int i = 0; i < tickMarkTextArray.length; i++) {
                final String text2Draw = tickMarkTextArray[i].toString();
                if (TextUtils.isEmpty(text2Draw)) continue;
                paint.getTextBounds(text2Draw, 0, text2Draw.length(), tickMarkTextRect);
                paint.setColor(tickMarkTextColor);

                float x;
                if (tickMarkMode == TRICK_MARK_MODE_OTHER) {
                    if (tickMarkGravity == TICK_MARK_GRAVITY_RIGHT) {
                        x = getProgressLeft() + i * trickPartWidth - tickMarkTextRect.width();
                    } else if (tickMarkGravity == TICK_MARK_GRAVITY_CENTER) {
                        x = getProgressLeft() + i * trickPartWidth - tickMarkTextRect.width() / 2f;
                    } else {
                        x = getProgressLeft() + i * trickPartWidth;
                    }
                } else {
                    float num = Utils.parseFloat(text2Draw);
                    SeekBarState[] states = getRangeSeekBarState();
                    if (Utils.compareFloat(num, states[0].value) != -1 && Utils.compareFloat(num, states[1].value) != 1 && (seekBarMode == SEEKBAR_MODE_RANGE)) {
                        paint.setColor(tickMarkInRangeTextColor);
                    }

                    x = getProgressLeft() + progressWidth * (num - minProgress) / (maxProgress - minProgress)
                            - tickMarkTextRect.width() / 2f;
                }
                float y;
                if (tickMarkLayoutGravity == Gravity.TOP) {
                    y = getProgressTop() - tickMarkTextMargin;
                } else {
                    y = getProgressBottom() + tickMarkTextMargin + tickMarkTextRect.height();
                }
                canvas.drawText(text2Draw, x, y, paint);
            }
        }
    }

    protected void onDrawProgressBar(Canvas canvas, Paint paint) {

        paint.setShader(null);
        if (Utils.verifyBitmap(progressDefaultBitmap)) {
            canvas.drawBitmap(progressDefaultBitmap, null, progressDefaultDstRect, paint);
        } else {
            if (progressDefaultColor == -1) {
                int[] colors = PseudoColorConfig.getSeekBarColors();
                float[] positions = PseudoColorConfig.getSeekBarAlpha();
                paint.setShader(new LinearGradient(progressWidth, 0f, 0f, 0f, colors, positions, Shader.TileMode.CLAMP));
            } else {
                paint.setColor(progressDefaultColor);
            }
            canvas.drawRoundRect(progressDefaultDstRect, progressRadius, progressRadius, paint);
        }

        if (seekBarMode == SEEKBAR_MODE_RANGE) {

            progressDstRect.top = getProgressTop();
            progressDstRect.left = leftSB.left + leftSB.getThumbScaleWidth() / 2f + progressWidth * leftSB.currPercent;
            progressDstRect.right = rightSB.left + rightSB.getThumbScaleWidth() / 2f + progressWidth * rightSB.currPercent;
            progressDstRect.bottom = getProgressBottom();
        } else {
            progressDstRect.top = getProgressTop();
            progressDstRect.left = leftSB.left + leftSB.getThumbScaleWidth() / 2f;
            progressDstRect.right = leftSB.left + leftSB.getThumbScaleWidth() / 2f + progressWidth * leftSB.currPercent;
            progressDstRect.bottom = getProgressBottom();
        }
        if (colorList != null) {
            paint.setShader(new LinearGradient(progressWidth, 0f, 0f, 0f, colorList, places, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(progressDstRect, progressRadius, progressRadius, paint);
        } else if (progressColor == -1) {
            int[] colors = PseudoColorConfig.getColors(pseudocode);
            float[] positions = PseudoColorConfig.getPositions(pseudocode);
            paint.setShader(new LinearGradient(progressWidth, 0f, 0f, 0f, colors, positions, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(progressDstRect, progressRadius, progressRadius, paint);
        } else {
            if (Utils.verifyBitmap(progressBitmap)) {
                progressSrcRect.top = 0;
                progressSrcRect.bottom = progressBitmap.getHeight();
                int bitmapWidth = progressBitmap.getWidth();
                if (seekBarMode == SEEKBAR_MODE_RANGE) {
                    progressSrcRect.left = (int) (bitmapWidth * leftSB.currPercent);
                    progressSrcRect.right = (int) (bitmapWidth * rightSB.currPercent);
                } else {
                    progressSrcRect.left = 0;
                    progressSrcRect.right = (int) (bitmapWidth * leftSB.currPercent);
                }
                canvas.drawBitmap(progressBitmap, progressSrcRect, progressDstRect, null);
            } else {
                paint.setColor(progressColor);
                canvas.drawRoundRect(progressDstRect, progressRadius, progressRadius, paint);
            }
        }

    }

    protected void onDrawSteps(Canvas canvas, Paint paint) {
        if (!verifyStepsMode()) return;
        int stepMarks = getProgressWidth() / (steps);
        float extHeight = (stepsHeight - getProgressHeight()) / 2f;
        for (int k = 0; k <= steps; k++) {
            float x = getProgressLeft() + k * stepMarks - stepsWidth / 2f;
            stepDivRect.set(x, getProgressTop() - extHeight, x + stepsWidth, getProgressBottom() + extHeight);
            if (stepsBitmaps.isEmpty() || stepsBitmaps.size() <= k) {
                paint.setColor(stepsColor);
                canvas.drawRoundRect(stepDivRect, stepsRadius, stepsRadius, paint);
            } else {
                canvas.drawBitmap(stepsBitmaps.get(k), null, stepDivRect, paint);
            }
        }
    }

    protected void onDrawSeekBar(Canvas canvas) {

        if (leftSB.getIndicatorShowMode() == INDICATOR_ALWAYS_SHOW) {
            leftSB.setShowIndicatorEnable(true);
        }
        leftSB.draw(canvas, true);

        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            if (rightSB.getIndicatorShowMode() == INDICATOR_ALWAYS_SHOW) {
                rightSB.setShowIndicatorEnable(true);
            }
            rightSB.draw(canvas, false);
        }
    }

    private void initPaint() {
        paint.setStyle(Paint.Style.FILL);

        if (progressDefaultColor == -1) {
            int[] colors = PseudoColorConfig.getSeekBarColors();
            float[] positions = PseudoColorConfig.getSeekBarAlpha();
            paint.setShader(new LinearGradient(progressWidth, 0f, 0f, 0f, colors, positions, Shader.TileMode.CLAMP));
        } else {
            paint.setColor(progressDefaultColor);
        }

        paint.setTextSize(tickMarkTextSize);
    }

    private void changeThumbActivateState(boolean hasActivate) {
        if (hasActivate && currTouchSB != null) {
            boolean state = currTouchSB == leftSB;
            leftSB.setActivate(state);
            if (seekBarMode == SEEKBAR_MODE_RANGE)
                rightSB.setActivate(!state);
        } else {
            leftSB.setActivate(false);
            if (seekBarMode == SEEKBAR_MODE_RANGE)
                rightSB.setActivate(false);
        }
    }

    protected float getEventX(MotionEvent event) {
        return event.getX();
    }

    protected float getEventY(MotionEvent event) {
        return event.getY();
    }

    private void scaleCurrentSeekBarThumb() {
        if (currTouchSB != null && currTouchSB.getThumbScaleRatio() > 1f && !isScaleThumb) {
            isScaleThumb = true;
            currTouchSB.scaleThumb();
        }
    }

    private void resetCurrentSeekBarThumb() {
        if (currTouchSB != null && currTouchSB.getThumbScaleRatio() > 1f && isScaleThumb) {
            isScaleThumb = false;
            currTouchSB.resetThumb();
        }
    }

    protected float calculateCurrentSeekBarPercent(float touchDownX) {
        if (currTouchSB == null) return 0;
        float percent = (touchDownX - getProgressLeft()) * 1f / (progressWidth);
        if (touchDownX < getProgressLeft()) {
            percent = 0;
        } else if (touchDownX > getProgressRight()) {
            percent = 1;
        }

        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            if (currTouchSB == leftSB) {
                if (percent > rightSB.currPercent - reservePercent) {
                    percent = rightSB.currPercent - reservePercent;
                }
            } else if (currTouchSB == rightSB) {
                if (percent < leftSB.currPercent + reservePercent) {
                    percent = leftSB.currPercent + reservePercent;
                }
            }
        }
        return percent;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnable) return false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchDownX = getEventX(event);
                touchDownY = getEventY(event);
                if (seekBarMode == SEEKBAR_MODE_RANGE) {
                    if (rightSB.currPercent >= 1 && leftSB.collide(getEventX(event), getEventY(event))) {
                        currTouchSB = leftSB;
                        scaleCurrentSeekBarThumb();
                    } else if (rightSB.collide(getEventX(event), getEventY(event))) {
                        currTouchSB = rightSB;
                        scaleCurrentSeekBarThumb();
                    } else {
                        float performClick = (touchDownX - getProgressLeft()) * 1f / (progressWidth);
                        float distanceLeft = Math.abs(leftSB.currPercent - performClick);
                        float distanceRight = Math.abs(rightSB.currPercent - performClick);
                        if (distanceLeft < distanceRight) {
                            currTouchSB = leftSB;
                        } else {
                            currTouchSB = rightSB;
                        }
                        performClick = calculateCurrentSeekBarPercent(touchDownX);
                        currTouchSB.slide(performClick);
                    }
                } else {
                    currTouchSB = leftSB;
                    scaleCurrentSeekBarThumb();
                }

                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (callback != null) {
                    callback.onStartTrackingTouch(this, currTouchSB == leftSB);
                }
                changeThumbActivateState(true);
                return true;
            case MotionEvent.ACTION_MOVE:
                float x = getEventX(event);
                if ((seekBarMode == SEEKBAR_MODE_RANGE) && leftSB.currPercent == rightSB.currPercent) {
                    currTouchSB.materialRestore();
                    if (callback != null) {
                        callback.onStopTrackingTouch(this, currTouchSB == leftSB);
                    }
                    if (x - touchDownX > 0) {

                        if (currTouchSB != rightSB) {
                            currTouchSB.setShowIndicatorEnable(false);
                            resetCurrentSeekBarThumb();
                            currTouchSB = rightSB;
                        }
                    } else {

                        if (currTouchSB != leftSB) {
                            currTouchSB.setShowIndicatorEnable(false);
                            resetCurrentSeekBarThumb();
                            currTouchSB = leftSB;
                        }
                    }
                    if (callback != null) {
                        callback.onStartTrackingTouch(this, currTouchSB == leftSB);
                    }
                }
                scaleCurrentSeekBarThumb();
                currTouchSB.material = currTouchSB.material >= 1 ? 1 : currTouchSB.material + 0.1f;
                touchDownX = x;
                currTouchSB.slide(calculateCurrentSeekBarPercent(touchDownX));
                currTouchSB.setShowIndicatorEnable(true);

                if (callback != null) {
                    SeekBarState[] states = getRangeSeekBarState();
                    updateTempModeState();
                    callback.onRangeChanged(this, states[0].value, states[1].value, true, tempMode);
                }
                invalidate();

                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                changeThumbActivateState(true);
                break;
            case MotionEvent.ACTION_CANCEL:
                if (seekBarMode == SEEKBAR_MODE_RANGE) {
                    rightSB.setShowIndicatorEnable(false);
                }
                if (currTouchSB == leftSB) {
                    resetCurrentSeekBarThumb();
                } else if (currTouchSB == rightSB) {
                    resetCurrentSeekBarThumb();
                }
                leftSB.setShowIndicatorEnable(false);
                if (callback != null) {
                    SeekBarState[] states = getRangeSeekBarState();
                    updateTempModeState();
                    callback.onRangeChanged(this, states[0].value, states[1].value, false, tempMode);
                }

                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                changeThumbActivateState(false);
                break;
            case MotionEvent.ACTION_UP:
                if (verifyStepsMode() && stepsAutoBonding) {
                    float percent = calculateCurrentSeekBarPercent(getEventX(event));
                    float stepPercent = 1.0f / steps;
                    int stepSelected = new BigDecimal(percent / stepPercent).setScale(0, RoundingMode.HALF_UP).intValue();
                    currTouchSB.slide(stepSelected * stepPercent);
                }

                if (seekBarMode == SEEKBAR_MODE_RANGE) {
                    rightSB.setShowIndicatorEnable(false);
                }
                leftSB.setShowIndicatorEnable(false);
                currTouchSB.materialRestore();
                resetCurrentSeekBarThumb();
                if (callback != null) {
                    SeekBarState[] states = getRangeSeekBarState();
                    updateTempModeState();
                    callback.onRangeChanged(this, states[0].value, states[1].value, false, tempMode);
                }

                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (callback != null) {
                    callback.onStopTrackingTouch(this, currTouchSB == leftSB);
                }
                changeThumbActivateState(false);
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.minValue = minProgress;
        ss.maxValue = maxProgress;
        ss.rangeInterval = minInterval;
        SeekBarState[] results = getRangeSeekBarState();
        ss.currSelectedMin = results[0].value;
        ss.currSelectedMax = results[1].value;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        try {
            SavedState ss = (SavedState) state;
            super.onRestoreInstanceState(ss.getSuperState());
            float min = ss.minValue;
            float max = ss.maxValue;
            float rangeInterval = ss.rangeInterval;
            setRange(min, max, rangeInterval);
            float currSelectedMin = ss.currSelectedMin;
            float currSelectedMax = ss.currSelectedMax;
            setProgress(currSelectedMin, currSelectedMax);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void setNoNegativeNumber(Boolean noNegativeNumber) {
        this.noNegativeNumber = noNegativeNumber;
        if (leftSB != null) {
            leftSB.setNoNegativeNumber(noNegativeNumber);
        }
        if (rightSB != null) {
            rightSB.setNoNegativeNumber(noNegativeNumber);
        }
    }

    public void setOnRangeChangedListener(OnRangeChangedListener listener) {
        callback = listener;
    }

    public void setProgress(float value) {
        setProgress(value, maxProgress);
    }

    public void setProgressNoCallBack(float leftValue, float rightValue) {
        leftValue = Math.min(leftValue, rightValue);
        rightValue = Math.max(leftValue, rightValue);
        if (rightValue - leftValue < minInterval) {
            if (leftValue - minProgress > maxProgress - rightValue) {
                leftValue = rightValue - minInterval;
            } else {
                rightValue = leftValue + minInterval;
            }
        }

        if (leftValue < minProgress) {
            leftValue = minProgress;
        }

        if (rightValue > maxProgress) {
            rightValue = maxProgress;
        }
        float range = maxProgress - minProgress;
        leftSB.currPercent = Math.abs(leftValue - minProgress) / range;
        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            rightSB.currPercent = Math.abs(rightValue - minProgress) / range;
        }

        postInvalidate();
    }

    @Override
    public void invalidate() {
        if (System.currentTimeMillis() - updateTime < 50) {
            return;
        }
        super.invalidate();
        updateTime = System.currentTimeMillis();
    }

    public void setProgress(float leftValue, float rightValue) {
        leftValue = Math.min(leftValue, rightValue);
        rightValue = Math.max(leftValue, rightValue);
        if (rightValue - leftValue < minInterval) {
            if (leftValue - minProgress > maxProgress - rightValue) {
                leftValue = rightValue - minInterval;
            } else {
                rightValue = leftValue + minInterval;
            }
        }

        if (leftValue < minProgress) {
            leftValue = minProgress;
        }

        if (rightValue > maxProgress) {
            rightValue = maxProgress;
        }
        float range = maxProgress - minProgress;
        leftSB.currPercent = Math.abs(leftValue - minProgress) / range;
        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            rightSB.currPercent = Math.abs(rightValue - minProgress) / range;
        }
        if (callback != null) {
            callback.onRangeChanged(this, leftValue, rightValue, false, tempMode);
        }
        invalidate();
    }

    public void setRange(float min, float max) {
        setRange(min, max, minInterval);
        setProgress(getLeftSeekBar().left, getRightSeekBar().right);
    }

    public void setRangeAndPro(float editMin, float editMax, float realLeftValue, float realRightValue) {
        if (editMin == Float.MIN_VALUE && editMax == Float.MAX_VALUE) {
            setRangeNoInvalidate(realLeftValue, realRightValue, 0.1f);
            setProgressNoCallBack(realLeftValue, realRightValue);
            return;
        }
        setRangeNoInvalidate(realLeftValue, realRightValue, 0.1f);
        if (editMax <= realRightValue && editMin >= realLeftValue) {

            setProgressNoCallBack(editMin, editMax);
        } else if (editMax > realRightValue && editMin < realLeftValue) {

            setProgressNoCallBack(realLeftValue, realRightValue);
        } else if (editMax > realRightValue && editMin > realRightValue) {

            setProgressNoCallBack(realRightValue, realRightValue);
        } else if (editMax < realLeftValue && editMin < realLeftValue) {

            setProgressNoCallBack(realLeftValue, realLeftValue);
        } else if (editMax <= realRightValue && editMin < realLeftValue) {

            setProgressNoCallBack(realLeftValue, editMax);
        } else if (editMax > realRightValue && editMin >= realLeftValue) {

            setProgressNoCallBack(editMin, realRightValue);
        }
    }

    public void setRange(float min, float max, float minInterval) {

        if (maxProgress == max && min == minProgress) {

            return;
        }
        maxProgress = max;
        minProgress = min;
        this.minInterval = minInterval;
        reservePercent = minInterval / (max - min);

        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            if (leftSB.currPercent + reservePercent <= 1 && leftSB.currPercent + reservePercent > rightSB.currPercent) {
                rightSB.currPercent = leftSB.currPercent + reservePercent;
            } else if (rightSB.currPercent - reservePercent >= 0 && rightSB.currPercent - reservePercent < leftSB.currPercent) {
                leftSB.currPercent = rightSB.currPercent - reservePercent;
            }
        }
        postInvalidate();
    }

    public void setRangeNoInvalidate(float min, float max, float minInterval) {

        if (maxProgress == max && min == minProgress) {

            return;
        }
        maxProgress = max;
        minProgress = min;
        this.minInterval = minInterval;
        reservePercent = minInterval / (max - min);

        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            if (leftSB.currPercent + reservePercent <= 1 && leftSB.currPercent + reservePercent > rightSB.currPercent) {
                rightSB.currPercent = leftSB.currPercent + reservePercent;
            } else if (rightSB.currPercent - reservePercent >= 0 && rightSB.currPercent - reservePercent < leftSB.currPercent) {
                leftSB.currPercent = rightSB.currPercent - reservePercent;
            }
        }
    }

    public SeekBarState[] getRangeSeekBarState() {
        SeekBarState leftSeekBarState = new SeekBarState();
        leftSeekBarState.value = leftSB.getProgress();

        leftSeekBarState.indicatorText = String.valueOf(leftSeekBarState.value);
        if (Utils.compareFloat(leftSeekBarState.value, minProgress) == 0) {
            leftSeekBarState.isMin = true;
        } else if (Utils.compareFloat(leftSeekBarState.value, maxProgress) == 0) {
            leftSeekBarState.isMax = true;
        }

        SeekBarState rightSeekBarState = new SeekBarState();
        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            rightSeekBarState.value = rightSB.getProgress();
            rightSeekBarState.indicatorText = String.valueOf(rightSeekBarState.value);
            if (Utils.compareFloat(rightSB.currPercent, minProgress) == 0) {
                rightSeekBarState.isMin = true;
            } else if (Utils.compareFloat(rightSB.currPercent, maxProgress) == 0) {
                rightSeekBarState.isMax = true;
            }
        }

        return new SeekBarState[]{leftSeekBarState, rightSeekBarState};
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.isEnable = enabled;
    }

    public void setIndicatorText(String progress) {
        leftSB.setIndicatorText(progress);
        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            rightSB.setIndicatorText(progress);
        }
    }

    public void setIndicatorTextDecimalFormat(String formatPattern) {
        leftSB.setIndicatorTextDecimalFormat(formatPattern);
        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            rightSB.setIndicatorTextDecimalFormat(formatPattern);
        }
    }

    public void setIndicatorTextStringFormat(String formatPattern) {
        leftSB.setIndicatorTextStringFormat(formatPattern);
        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            rightSB.setIndicatorTextStringFormat(formatPattern);
        }
    }

    public SeekBar getLeftSeekBar() {
        return leftSB;
    }

    public SeekBar getRightSeekBar() {
        return rightSB;
    }

    public int getProgressTop() {
        return progressTop;
    }

    public void setProgressTop(int progressTop) {
        this.progressTop = progressTop;
    }

    public int getProgressBottom() {
        return progressBottom;
    }

    public void setProgressBottom(int progressBottom) {
        this.progressBottom = progressBottom;
    }

    public int getProgressLeft() {
        return progressLeft;
    }

    public void setProgressLeft(int progressLeft) {
        this.progressLeft = progressLeft;
    }

    public int getProgressRight() {
        return progressRight;
    }

    public void setProgressRight(int progressRight) {
        this.progressRight = progressRight;
    }

    public int getProgressPaddingRight() {
        return progressPaddingRight;
    }

    public int getProgressHeight() {
        return progressHeight;
    }

    public void setProgressHeight(int progressHeight) {
        this.progressHeight = progressHeight;
    }

    public float getMinProgress() {
        return minProgress;
    }

    public float getMaxProgress() {
        return maxProgress;
    }

    public void setProgressColor(@ColorInt int progressDefaultColor, @ColorInt int progressColor) {
        this.progressDefaultColor = progressDefaultColor;
        this.progressColor = progressColor;
    }

    public int getTickMarkTextColor() {
        return tickMarkTextColor;
    }

    public void setTickMarkTextColor(@ColorInt int tickMarkTextColor) {
        this.tickMarkTextColor = tickMarkTextColor;
    }

    public int getTickMarkInRangeTextColor() {
        return tickMarkInRangeTextColor;
    }

    public void setTickMarkInRangeTextColor(@ColorInt int tickMarkInRangeTextColor) {
        this.tickMarkInRangeTextColor = tickMarkInRangeTextColor;
    }

    public int getSeekBarMode() {
        return seekBarMode;
    }

    public void setSeekBarMode(@SeekBarModeDef int seekBarMode) {
        this.seekBarMode = seekBarMode;
        rightSB.setVisible(seekBarMode != SEEKBAR_MODE_SINGLE);
    }

    public int getTickMarkMode() {
        return tickMarkMode;
    }

    public void setTickMarkMode(@TickMarkModeDef int tickMarkMode) {
        this.tickMarkMode = tickMarkMode;
    }

    public int getTickMarkTextMargin() {
        return tickMarkTextMargin;
    }

    public void setTickMarkTextMargin(int tickMarkTextMargin) {
        this.tickMarkTextMargin = tickMarkTextMargin;
    }

    public int getTickMarkTextSize() {
        return tickMarkTextSize;
    }

    public void setTickMarkTextSize(int tickMarkTextSize) {
        this.tickMarkTextSize = tickMarkTextSize;
    }

    public int getTickMarkGravity() {
        return tickMarkGravity;
    }

    public void setTickMarkGravity(@TickMarkGravityDef int tickMarkGravity) {
        this.tickMarkGravity = tickMarkGravity;
    }

    public CharSequence[] getTickMarkTextArray() {
        return tickMarkTextArray;
    }

    public void setTickMarkTextArray(CharSequence[] tickMarkTextArray) {
        this.tickMarkTextArray = tickMarkTextArray;
    }

    public float getMinInterval() {
        return minInterval;
    }

    public float getProgressRadius() {
        return progressRadius;
    }

    public void setProgressRadius(float progressRadius) {
        this.progressRadius = progressRadius;
    }

    public int getProgressColor() {
        return progressColor;
    }

    public void setProgressColor(@ColorInt int progressColor) {
        this.progressColor = progressColor;
    }

    public int getProgressDefaultColor() {
        return progressDefaultColor;
    }

    public void setProgressDefaultColor(@ColorInt int progressDefaultColor) {
        this.progressDefaultColor = progressDefaultColor;
    }

    public int getProgressDrawableId() {
        return progressDrawableId;
    }

    public void setProgressDrawableId(@DrawableRes int progressDrawableId) {
        this.progressDrawableId = progressDrawableId;
        progressBitmap = null;
        initProgressBitmap();
    }

    public int getProgressDefaultDrawableId() {
        return progressDefaultDrawableId;
    }

    public void setProgressDefaultDrawableId(@DrawableRes int progressDefaultDrawableId) {
        this.progressDefaultDrawableId = progressDefaultDrawableId;
        progressDefaultBitmap = null;
        initProgressBitmap();
    }

    public int getProgressWidth() {
        return progressWidth;
    }

    public void setProgressWidth(int progressWidth) {
        this.progressWidth = progressWidth;
    }

    public void setTypeface(Typeface typeFace) {
        paint.setTypeface(typeFace);
    }

    public boolean isEnableThumbOverlap() {
        return enableThumbOverlap;
    }

    public void setEnableThumbOverlap(boolean enableThumbOverlap) {
        this.enableThumbOverlap = enableThumbOverlap;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    public int getStepsColor() {
        return stepsColor;
    }

    public void setStepsColor(@ColorInt int stepsColor) {
        this.stepsColor = stepsColor;
    }

    public float getStepsWidth() {
        return stepsWidth;
    }

    public void setStepsWidth(float stepsWidth) {
        this.stepsWidth = stepsWidth;
    }

    public float getStepsHeight() {
        return stepsHeight;
    }

    public void setStepsHeight(float stepsHeight) {
        this.stepsHeight = stepsHeight;
    }

    public float getStepsRadius() {
        return stepsRadius;
    }

    public void setStepsRadius(float stepsRadius) {
        this.stepsRadius = stepsRadius;
    }

    public int getTickMarkLayoutGravity() {
        return tickMarkLayoutGravity;
    }

    public void setTickMarkLayoutGravity(@TickMarkLayoutGravityDef int tickMarkLayoutGravity) {
        this.tickMarkLayoutGravity = tickMarkLayoutGravity;
    }

    public int getGravity() {
        return gravity;
    }

    public void setGravity(@GravityDef int gravity) {
        this.gravity = gravity;
    }

    public boolean isStepsAutoBonding() {
        return stepsAutoBonding;
    }

    public void setStepsAutoBonding(boolean stepsAutoBonding) {
        this.stepsAutoBonding = stepsAutoBonding;
    }

    public int getStepsDrawableId() {
        return stepsDrawableId;
    }

    public void setStepsDrawableId(@DrawableRes int stepsDrawableId) {
        this.stepsBitmaps.clear();
        this.stepsDrawableId = stepsDrawableId;
        initStepsBitmap();
    }

    public List<Bitmap> getStepsBitmaps() {
        return stepsBitmaps;
    }

    public void setStepsBitmaps(List<Bitmap> stepsBitmaps) {

        this.stepsBitmaps.clear();
        this.stepsBitmaps.addAll(stepsBitmaps);
    }

    public void setStepsDrawable(List<Integer> stepsDrawableIds) {

        if (!verifyStepsMode()) {
            throw new IllegalArgumentException("stepsWidth must > 0, stepsHeight must > 0,steps must > 0 First!!");
        }
        List<Bitmap> stepsBitmaps = new ArrayList<>();
        for (int i = 0; i < stepsDrawableIds.size(); i++) {
            stepsBitmaps.add(Utils.drawableToBitmap(getContext(), (int) stepsWidth, (int) stepsHeight, stepsDrawableIds.get(i)));
        }
        setStepsBitmaps(stepsBitmaps);
    }

    public void setPseudocode(int pseudocode) {
        this.pseudocode = pseudocode;
        invalidate();
    }

    public void setColorList(@Nullable int[] colorList) {
        this.colorList = colorList;
        invalidate();
    }

    public void setPlaces(@Nullable float[] newPlaces) {
        if (newPlaces == null) {
            places = null;
        } else {
            if (places == null || places.length != newPlaces.length) {
                places = new float[newPlaces.length];
            }
            for (int i = 0; i < newPlaces.length; i++) {
                places[places.length - 1 - i] = 1 - newPlaces[i];
            }
        }
        invalidate();
    }

    public void drawIndPath(boolean isEnabled) {
        // Placeholder implementation
        invalidate();
    }

    @IntDef({SEEKBAR_MODE_SINGLE, SEEKBAR_MODE_RANGE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SeekBarModeDef {
    }

    @IntDef({TRICK_MARK_MODE_NUMBER, TRICK_MARK_MODE_OTHER})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TickMarkModeDef {
    }

    @IntDef({TICK_MARK_GRAVITY_LEFT, TICK_MARK_GRAVITY_CENTER, TICK_MARK_GRAVITY_RIGHT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TickMarkGravityDef {
    }

    @IntDef({Gravity.TOP, Gravity.BOTTOM})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TickMarkLayoutGravityDef {
    }

    @IntDef({Gravity.TOP, Gravity.CENTER, Gravity.BOTTOM})
    @Retention(RetentionPolicy.SOURCE)
    public @interface GravityDef {
    }

    public static class Gravity {
        public final static int TOP = 0;
        public final static int BOTTOM = 1;
        public final static int CENTER = 2;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\seekbar\SavedState.java =====

package com.mpdc4gsr.libunified.ui.widget.seekbar;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

public class SavedState extends View.BaseSavedState {
    public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
        public SavedState createFromParcel(Parcel in) {
            return new SavedState(in);
        }

        public SavedState[] newArray(int size) {
            return new SavedState[size];
        }
    };
    public float minValue;
    public float maxValue;
    public float rangeInterval;
    public int tickNumber;
    public float currSelectedMin;
    public float currSelectedMax;

    public SavedState(Parcelable superState) {
        super(superState);
    }

    private SavedState(Parcel in) {
        super(in);
        minValue = in.readFloat();
        maxValue = in.readFloat();
        rangeInterval = in.readFloat();
        tickNumber = in.readInt();
        currSelectedMin = in.readFloat();
        currSelectedMax = in.readFloat();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeFloat(minValue);
        out.writeFloat(maxValue);
        out.writeFloat(rangeInterval);
        out.writeInt(tickNumber);
        out.writeFloat(currSelectedMin);
        out.writeFloat(currSelectedMax);
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\seekbar\SeekBar.java =====

package com.mpdc4gsr.libunified.ui.widget.seekbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.*;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.mpdc4gsr.libunified.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.DecimalFormat;
import java.util.Locale;

public class SeekBar {

    public static final int INDICATOR_SHOW_WHEN_TOUCH = 0;
    public static final int INDICATOR_ALWAYS_HIDE = 1;
    public static final int INDICATOR_ALWAYS_SHOW_AFTER_TOUCH = 2;
    public static final int INDICATOR_ALWAYS_SHOW = 3;
    public static final int WRAP_CONTENT = -1;
    public static final int MATCH_PARENT = -2;
    float thumbScaleRatio;
    int left, right, top, bottom;
    float currPercent;
    float material = 0;
    boolean isLeft;
    Bitmap thumbBitmap;
    Bitmap thumbInactivatedBitmap;
    Bitmap indicatorBitmap;
    ValueAnimator anim;
    String userText2Draw;
    boolean isActivate = false;
    boolean isVisible = true;
    RangeSeekBar rangeSeekBar;
    String indicatorTextStringFormat;
    Path indicatorArrowPath = new Path();
    Rect indicatorTextRect = new Rect();
    Rect indicatorRect = new Rect();
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    DecimalFormat indicatorTextDecimalFormat;
    int scaleThumbWidth;
    int scaleThumbHeight;
    private boolean thumbShow;
    private int indicatorShowMode;
    private int indicatorHeight;
    private int indicatorWidth;
    private int indicatorMargin;
    private int indicatorDrawableId;
    private int indicatorArrowSize;
    private int indicatorTextSize;
    private int indicatorTextColor;
    private float indicatorRadius;
    private int indicatorBackgroundColor;
    private int indicatorPaddingLeft, indicatorPaddingRight, indicatorPaddingTop, indicatorPaddingBottom;
    private int thumbDrawableId;
    private int thumbInactivatedDrawableId;
    private int thumbWidth;
    private int thumbHeight;
    private boolean isShowIndicator;
    private boolean noNegativeNumber = false;

    public SeekBar(RangeSeekBar rangeSeekBar, AttributeSet attrs, boolean isLeft) {
        this.rangeSeekBar = rangeSeekBar;
        this.isLeft = isLeft;
        initAttrs(attrs);
        initBitmap();
        initVariables();
    }

    private void initAttrs(AttributeSet attrs) {
        TypedArray t = getContext().obtainStyledAttributes(attrs, R.styleable.RangeSeekBar);
        if (t == null) return;
        indicatorMargin = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_indicator_margin, 0);
        indicatorDrawableId = t.getResourceId(R.styleable.RangeSeekBar_rsb_indicator_drawable, 0);
        indicatorShowMode = t.getInt(R.styleable.RangeSeekBar_rsb_indicator_show_mode, INDICATOR_ALWAYS_HIDE);
        indicatorHeight = t.getLayoutDimension(R.styleable.RangeSeekBar_rsb_indicator_height, WRAP_CONTENT);
        indicatorWidth = t.getLayoutDimension(R.styleable.RangeSeekBar_rsb_indicator_width, WRAP_CONTENT);
        indicatorTextSize = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_indicator_text_size, Utils.dp2px(getContext(), 14));
        indicatorTextColor = t.getColor(R.styleable.RangeSeekBar_rsb_indicator_text_color, Color.WHITE);
        indicatorBackgroundColor = t.getColor(R.styleable.RangeSeekBar_rsb_indicator_background_color, ContextCompat.getColor(getContext(), R.color.colorAccent));
        indicatorPaddingLeft = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_indicator_padding_left, 0);
        indicatorPaddingRight = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_indicator_padding_right, 0);
        indicatorPaddingTop = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_indicator_padding_top, 0);
        indicatorPaddingBottom = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_indicator_padding_bottom, 0);
        indicatorArrowSize = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_indicator_arrow_size, 0);
        thumbDrawableId = t.getResourceId(R.styleable.RangeSeekBar_rsb_thumb_drawable, R.drawable.rsb_default_thumb);
        thumbShow = t.getBoolean(R.styleable.RangeSeekBar_rsb_show_thumb, false);
        thumbInactivatedDrawableId = t.getResourceId(R.styleable.RangeSeekBar_rsb_thumb_inactivated_drawable, 0);
        thumbWidth = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_thumb_width, Utils.dp2px(getContext(), 26));
        thumbHeight = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_thumb_height, Utils.dp2px(getContext(), 26));
        thumbScaleRatio = t.getFloat(R.styleable.RangeSeekBar_rsb_thumb_scale_ratio, 1f);
        indicatorRadius = t.getDimension(R.styleable.RangeSeekBar_rsb_indicator_radius, 0f);
        t.recycle();
    }

    protected void initVariables() {
        scaleThumbWidth = thumbWidth;
        scaleThumbHeight = thumbHeight;
        if (indicatorHeight == WRAP_CONTENT) {
            indicatorHeight = Utils.measureText("8", indicatorTextSize).height() + indicatorPaddingTop + indicatorPaddingBottom;
        }
        if (indicatorArrowSize <= 0) {
            indicatorArrowSize = (int) (thumbWidth / 4);
        }
    }

    public Context getContext() {
        return rangeSeekBar.getContext();
    }

    public Resources getResources() {
        if (getContext() != null) return getContext().getResources();
        return null;
    }

    private void initBitmap() {
        setIndicatorDrawableId(indicatorDrawableId);
        setThumbDrawableId(thumbDrawableId, thumbWidth, thumbHeight);
        setThumbInactivatedDrawableId(thumbInactivatedDrawableId, thumbWidth, thumbHeight);
    }

    protected void onSizeChanged(int x, int y) {
        initVariables();
        initBitmap();
        left = (int) (x - getThumbScaleWidth() / 2);
        right = (int) (x + getThumbScaleWidth() / 2);
        top = y - getThumbHeight() / 2;
        bottom = y + getThumbHeight() / 2;
    }

    public void scaleThumb() {
        scaleThumbWidth = (int) getThumbScaleWidth();
        scaleThumbHeight = (int) getThumbScaleHeight();
        int y = rangeSeekBar.getProgressBottom();
        top = y - scaleThumbHeight / 2;
        bottom = y + scaleThumbHeight / 2;
        setThumbDrawableId(thumbDrawableId, scaleThumbWidth, scaleThumbHeight);
    }

    public void resetThumb() {
        scaleThumbWidth = getThumbWidth();
        scaleThumbHeight = getThumbHeight();
        int y = rangeSeekBar.getProgressBottom();
        top = y - scaleThumbHeight / 2;
        bottom = y + scaleThumbHeight / 2;
        setThumbDrawableId(thumbDrawableId, scaleThumbWidth, scaleThumbHeight);
    }

    public float getRawHeight() {
        return getIndicatorHeight() + getIndicatorArrowSize() + getIndicatorMargin() + getThumbScaleHeight();
    }

    public void setNoNegativeNumber(Boolean noNegativeNumber) {
        this.noNegativeNumber = noNegativeNumber;
    }

    protected void draw(Canvas canvas, boolean isLeft) {
        if (!isVisible) {
            return;
        }
        int offset = (int) (rangeSeekBar.getProgressWidth() * currPercent);
        canvas.save();
        canvas.translate(offset, 0);

        canvas.translate(left, 0);
        if (isShowIndicator) {
            onDrawIndicator(canvas, paint, formatCurrentIndicatorText(userText2Draw)); //
        }

        if (thumbShow) {
            onDrawThumb(canvas);
        } else {
            onDrawThumb(canvas, isLeft); //
        }
        canvas.restore();
    }

    protected void onDrawThumb(Canvas canvas) {
        if (thumbInactivatedBitmap != null && !isActivate) {
            canvas.drawBitmap(thumbInactivatedBitmap, 0, rangeSeekBar.getProgressTop() + (rangeSeekBar.getProgressHeight() - scaleThumbHeight) / 2f, null);
        } else if (thumbBitmap != null) {

            canvas.drawBitmap(thumbBitmap, 0, rangeSeekBar.getProgressTop() + (rangeSeekBar.getProgressHeight() - scaleThumbHeight) / 2f, null);
        }
    }

    protected void onDrawThumb(Canvas canvas, Boolean isLeft) {
        if (thumbInactivatedBitmap != null && !isActivate) {

        } else if (thumbBitmap != null) {

            Matrix matrix = new Matrix();
            int offX = thumbBitmap.getWidth() / 2;
            int offY = thumbBitmap.getHeight() / 2;
            matrix.postTranslate(-offX, -offY);
            if (isLeft) {
                matrix.postRotate(90);
                offX = offX - 5;
            } else {
                matrix.postRotate(270);
                offX = offX + 5;
            }

        }
    }

    protected String formatCurrentIndicatorText(String text2Draw) {
        SeekBarState[] states = rangeSeekBar.getRangeSeekBarState();
        if (TextUtils.isEmpty(text2Draw)) {
            if (isLeft) {
                if (indicatorTextDecimalFormat != null) {
                    text2Draw = indicatorTextDecimalFormat.format(states[0].value);
                } else {
                    text2Draw = states[0].indicatorText;
                }
            } else {
                if (indicatorTextDecimalFormat != null) {
                    text2Draw = indicatorTextDecimalFormat.format(states[1].value);
                } else {
                    text2Draw = states[1].indicatorText;
                }
            }
        }
        if (indicatorTextStringFormat != null) {

            text2Draw = String.format(Locale.ENGLISH, indicatorTextStringFormat, Float.parseFloat(text2Draw));
        }
        return text2Draw;
    }

    protected void onDrawIndicator(Canvas canvas, Paint paint, String text2Draw) {
        try {
            if (text2Draw == null) return;
            paint.setTextSize(indicatorTextSize);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(indicatorBackgroundColor);
            if (noNegativeNumber) {
                text2Draw = text2Draw.replace("-", "");
            }
            paint.getTextBounds(text2Draw, 0, text2Draw.length(), indicatorTextRect);
            int realIndicatorWidth = indicatorWidth + indicatorPaddingLeft + indicatorPaddingRight;
            if (indicatorWidth > realIndicatorWidth) {
                realIndicatorWidth = indicatorWidth;
            }

            int realIndicatorHeight = indicatorTextRect.height() + indicatorPaddingTop + indicatorPaddingBottom;
            if (indicatorHeight > realIndicatorHeight) {
                realIndicatorHeight = indicatorHeight;
            }

            indicatorRect.left = (int) (scaleThumbWidth / 2f - realIndicatorWidth / 2f);
            indicatorRect.top = bottom - realIndicatorHeight - scaleThumbHeight - indicatorMargin;
            indicatorRect.right = indicatorRect.left + realIndicatorWidth;
            indicatorRect.bottom = indicatorRect.top + realIndicatorHeight;

            if (indicatorBitmap == null) {

                int ax = scaleThumbWidth / 2;
                int ay = indicatorRect.bottom;
                int bx = ax - indicatorArrowSize;
                int by = ay - indicatorArrowSize;
                int cx = ax + indicatorArrowSize;
                indicatorArrowPath.reset();
                indicatorArrowPath.moveTo(ax, ay);
                indicatorArrowPath.lineTo(bx, by);
                indicatorArrowPath.lineTo(cx, by);
                indicatorArrowPath.close();
                canvas.drawPath(indicatorArrowPath, paint);
                indicatorRect.bottom -= indicatorArrowSize;
                indicatorRect.top -= indicatorArrowSize;
                Log.w("pseudo colorrefresh", "///");
            }

            int defaultPaddingOffset = Utils.dp2px(getContext(), 1);
            int leftOffset = indicatorRect.width() / 2 - (int) (rangeSeekBar.getProgressWidth() * currPercent) - rangeSeekBar.getProgressLeft() + defaultPaddingOffset;
            int rightOffset = indicatorRect.width() / 2 - (int) (rangeSeekBar.getProgressWidth() * (1 - currPercent)) - rangeSeekBar.getProgressPaddingRight() + defaultPaddingOffset;

            if (leftOffset > 0) {
                indicatorRect.left += leftOffset;
                indicatorRect.right += leftOffset;
            } else if (rightOffset > 0) {
                indicatorRect.left -= rightOffset;
                indicatorRect.right -= rightOffset;
            }

            if (indicatorBitmap != null) {
                int offset = (int) (rangeSeekBar.getProgressWidth() * currPercent);

                Rect rect = new Rect(indicatorRect.left, indicatorRect.top, indicatorWidth, indicatorRect.bottom);
                Utils.drawBitmap(canvas, paint, indicatorBitmap, rect);
            } else if (indicatorRadius > 0f) {
                canvas.drawRoundRect(new RectF(indicatorRect), indicatorRadius, indicatorRadius, paint);
            } else {
                canvas.drawRect(indicatorRect, paint);
            }

            int tx, ty;
            if (indicatorPaddingLeft > 0) {
                tx = indicatorRect.left + indicatorPaddingLeft;
            } else if (indicatorPaddingRight > 0) {
                tx = indicatorRect.right - indicatorPaddingRight - indicatorTextRect.width();
            } else {
                tx = indicatorRect.left + (realIndicatorWidth - indicatorTextRect.width()) / 2;
            }

            if (indicatorPaddingTop > 0) {
                ty = indicatorRect.top + indicatorTextRect.height() + indicatorPaddingTop;
            } else if (indicatorPaddingBottom > 0) {
                ty = indicatorRect.bottom - indicatorTextRect.height() - indicatorPaddingBottom;
            } else {
                ty = indicatorRect.bottom - (realIndicatorHeight - indicatorTextRect.height()) / 2 + 1;
            }

            paint.setColor(indicatorTextColor);
            canvas.drawText(text2Draw, tx, ty, paint);
        } catch (Exception e) {
            Log.w("", e.getMessage() + "");
        }
    }

    protected boolean collide(float x, float y) {
        int offset = (int) (rangeSeekBar.getProgressWidth() * currPercent);
        return x > left + offset && x < right + offset && y > top && y < bottom;
    }

    protected void slide(float percent) {
        if (percent < 0) percent = 0;
        else if (percent > 1) percent = 1;
        currPercent = percent;
    }

    protected void setShowIndicatorEnable(boolean isEnable) {
        switch (indicatorShowMode) {
            case INDICATOR_SHOW_WHEN_TOUCH:
                isShowIndicator = isEnable;
                break;
            case INDICATOR_ALWAYS_SHOW:
            case INDICATOR_ALWAYS_SHOW_AFTER_TOUCH:
                isShowIndicator = true;
                break;
            case INDICATOR_ALWAYS_HIDE:
                isShowIndicator = false;
                break;
        }
    }

    public void materialRestore() {
        if (rangeSeekBar != null && !rangeSeekBar.isAttachedToWindow()) {
            return;
        }
        if (anim != null) anim.cancel();
        anim = ValueAnimator.ofFloat(material, 0);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                material = (float) animation.getAnimatedValue();
                if (rangeSeekBar != null) rangeSeekBar.invalidate();
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                material = 0;
                if (rangeSeekBar != null) rangeSeekBar.invalidate();
            }
        });
        try {
            anim.start();
        } catch (IllegalStateException e) {
            Log.w("SeekBar", "Failed to start material restore animation: " + e.getMessage());
        }
    }

    public void setIndicatorText(String text) {
        userText2Draw = text;
    }

    public DecimalFormat getIndicatorTextDecimalFormat() {
        return indicatorTextDecimalFormat;
    }

    public void setIndicatorTextDecimalFormat(String formatPattern) {
        indicatorTextDecimalFormat = new DecimalFormat(formatPattern);
    }

    public void setIndicatorTextStringFormat(String formatPattern) {
        indicatorTextStringFormat = formatPattern;
    }

    public int getIndicatorDrawableId() {
        return indicatorDrawableId;
    }

    public void setIndicatorDrawableId(@DrawableRes int indicatorDrawableId) {
        if (indicatorDrawableId != 0) {
            this.indicatorDrawableId = indicatorDrawableId;
            indicatorBitmap = BitmapFactory.decodeResource(getResources(), indicatorDrawableId);
            if (indicatorBitmap == null) {
                indicatorBitmap = Utils.drawableToBitmap(indicatorWidth, indicatorHeight,
                        ResourcesCompat.getDrawable(getResources(), indicatorDrawableId, getContext().getTheme()));
            }
        }
    }

    public int getIndicatorArrowSize() {
        return indicatorArrowSize;
    }

    public void setIndicatorArrowSize(int indicatorArrowSize) {
        this.indicatorArrowSize = indicatorArrowSize;
    }

    public int getIndicatorPaddingLeft() {
        return indicatorPaddingLeft;
    }

    public void setIndicatorPaddingLeft(int indicatorPaddingLeft) {
        this.indicatorPaddingLeft = indicatorPaddingLeft;
    }

    public int getIndicatorPaddingRight() {
        return indicatorPaddingRight;
    }

    public void setIndicatorPaddingRight(int indicatorPaddingRight) {
        this.indicatorPaddingRight = indicatorPaddingRight;
    }

    public int getIndicatorPaddingTop() {
        return indicatorPaddingTop;
    }

    public void setIndicatorPaddingTop(int indicatorPaddingTop) {
        this.indicatorPaddingTop = indicatorPaddingTop;
    }

    public int getIndicatorPaddingBottom() {
        return indicatorPaddingBottom;
    }

    public void setIndicatorPaddingBottom(int indicatorPaddingBottom) {
        this.indicatorPaddingBottom = indicatorPaddingBottom;
    }

    public int getIndicatorMargin() {
        return indicatorMargin;
    }

    public void setIndicatorMargin(int indicatorMargin) {
        this.indicatorMargin = indicatorMargin;
    }

    public int getIndicatorShowMode() {
        return indicatorShowMode;
    }

    public void setIndicatorShowMode(@IndicatorModeDef int indicatorShowMode) {
        this.indicatorShowMode = indicatorShowMode;
    }

    public void showIndicator(boolean isShown) {
        isShowIndicator = isShown;
    }

    public boolean isShowIndicator() {
        return isShowIndicator;
    }

    public int getIndicatorRawHeight() {
        if (indicatorHeight > 0) {
            if (indicatorBitmap != null) {
                return indicatorHeight + indicatorMargin;
            } else {
                return indicatorHeight + indicatorArrowSize + indicatorMargin;
            }
        } else {
            if (indicatorBitmap != null) {
                return Utils.measureText("8", indicatorTextSize).height() + indicatorPaddingTop + indicatorPaddingBottom + indicatorMargin;
            } else {
                return Utils.measureText("8", indicatorTextSize).height() + indicatorPaddingTop + indicatorPaddingBottom + indicatorMargin + indicatorArrowSize;
            }
        }
    }

    public int getIndicatorHeight() {
        return indicatorHeight;
    }

    public void setIndicatorHeight(int indicatorHeight) {
        this.indicatorHeight = indicatorHeight;
    }

    public int getIndicatorWidth() {
        return indicatorWidth;
    }

    public void setIndicatorWidth(int indicatorWidth) {
        this.indicatorWidth = indicatorWidth;
    }

    public int getIndicatorTextSize() {
        return indicatorTextSize;
    }

    public void setIndicatorTextSize(int indicatorTextSize) {
        this.indicatorTextSize = indicatorTextSize;
    }

    public int getIndicatorTextColor() {
        return indicatorTextColor;
    }

    public void setIndicatorTextColor(@ColorInt int indicatorTextColor) {
        this.indicatorTextColor = indicatorTextColor;
    }

    public int getIndicatorBackgroundColor() {
        return indicatorBackgroundColor;
    }

    public void setIndicatorBackgroundColor(@ColorInt int indicatorBackgroundColor) {
        this.indicatorBackgroundColor = indicatorBackgroundColor;
    }

    public int getThumbInactivatedDrawableId() {
        return thumbInactivatedDrawableId;
    }

    public void setThumbInactivatedDrawableId(@DrawableRes int thumbInactivatedDrawableId, int width, int height) {
        if (thumbInactivatedDrawableId != 0 && getResources() != null) {
            this.thumbInactivatedDrawableId = thumbInactivatedDrawableId;
            thumbInactivatedBitmap = Utils.drawableToBitmap(width, height,
                    ResourcesCompat.getDrawable(getResources(), thumbInactivatedDrawableId, getContext().getTheme()));
        }
    }

    public int getThumbDrawableId() {
        return thumbDrawableId;
    }

    public void setThumbDrawableId(@DrawableRes int thumbDrawableId) {
        if (thumbWidth <= 0 || thumbHeight <= 0) {
            throw new IllegalArgumentException("please set thumbWidth and thumbHeight first!");
        }
        if (thumbDrawableId != 0 && getResources() != null) {
            this.thumbDrawableId = thumbDrawableId;
            thumbBitmap = Utils.drawableToBitmap(thumbWidth, thumbHeight,
                    ResourcesCompat.getDrawable(getResources(), thumbDrawableId, getContext().getTheme()));
        }
    }

    public void setThumbDrawableId(@DrawableRes int thumbDrawableId, int width, int height) {
        if (thumbDrawableId != 0 && getResources() != null && width > 0 && height > 0) {
            this.thumbDrawableId = thumbDrawableId;
            thumbBitmap = Utils.drawableToBitmap(width, height,
                    ResourcesCompat.getDrawable(getResources(), thumbDrawableId, getContext().getTheme()));
        }
    }

    public int getThumbWidth() {
        return thumbWidth;
    }

    public void setThumbWidth(int thumbWidth) {
        this.thumbWidth = thumbWidth;
    }

    public float getThumbScaleHeight() {
        return thumbHeight * thumbScaleRatio;
    }

    public float getThumbScaleWidth() {
        return thumbWidth * thumbScaleRatio;
    }

    public int getThumbHeight() {
        return thumbHeight;
    }

    public void setThumbHeight(int thumbHeight) {
        this.thumbHeight = thumbHeight;
    }

    public float getIndicatorRadius() {
        return indicatorRadius;
    }

    public void setIndicatorRadius(float indicatorRadius) {
        this.indicatorRadius = indicatorRadius;
    }

    protected boolean getActivate() {
        return isActivate;
    }

    protected void setActivate(boolean activate) {
        isActivate = activate;
    }

    public void setTypeface(Typeface typeFace) {
        paint.setTypeface(typeFace);
    }

    public float getThumbScaleRatio() {
        return thumbScaleRatio;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    public float getProgress() {
        float range = rangeSeekBar.getMaxProgress() - rangeSeekBar.getMinProgress();
        return rangeSeekBar.getMinProgress() + range * currPercent;
    }

    @IntDef({INDICATOR_SHOW_WHEN_TOUCH, INDICATOR_ALWAYS_HIDE, INDICATOR_ALWAYS_SHOW_AFTER_TOUCH, INDICATOR_ALWAYS_SHOW})
    @Retention(RetentionPolicy.SOURCE)
    public @interface IndicatorModeDef {
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\seekbar\SeekBarState.java =====

package com.mpdc4gsr.libunified.ui.widget.seekbar;

public class SeekBarState {
    public String indicatorText;
    public float value; //now progress value
    public boolean isMin;
    public boolean isMax;

    @Override
    public String toString() {
        return "indicatorText: " + indicatorText + " ,isMin: " + isMin + " ,isMax: " + isMax;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\seekbar\Utils.java =====

package com.mpdc4gsr.libunified.ui.widget.seekbar;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

public class Utils {

    private static final String TAG = "RangeSeekBar";

    public static void print(String log) {
        Log.d(TAG, log);
    }

    public static void print(Object... logs) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Object log : logs) {
            stringBuilder.append(log);
        }
        Log.d(TAG, stringBuilder.toString());
    }

    public static Bitmap drawableToBitmap(Context context, int width, int height, int drawableId) {
        if (context == null || width <= 0 || height <= 0 || drawableId == 0) return null;
        Drawable drawable = ResourcesCompat.getDrawable(context.getResources(), drawableId, context.getTheme());
        return Utils.drawableToBitmap(width, height, drawable);
    }

    public static Bitmap drawableToBitmap(int width, int height, Drawable drawable) {
        Bitmap bitmap = null;
        try {
            if (drawable instanceof BitmapDrawable) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                bitmap = bitmapDrawable.getBitmap();
                if (bitmap != null && bitmap.getHeight() > 0) {
                    Matrix matrix = new Matrix();
                    float scaleWidth = width * 1.0f / bitmap.getWidth();
                    float scaleHeight = height * 1.0f / bitmap.getHeight();
                    matrix.postScale(scaleWidth, scaleHeight);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    return bitmap;
                }
            }
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    public static void drawNinePath(Canvas canvas, Bitmap bmp, Rect rect) {
        NinePatch.isNinePatchChunk(bmp.getNinePatchChunk());
        NinePatch patch = new NinePatch(bmp, bmp.getNinePatchChunk(), null);
        patch.draw(canvas, rect);
    }

    public static void drawBitmap(Canvas canvas, Paint paint, Bitmap bmp, Rect rect) {
        try {
            if (NinePatch.isNinePatchChunk(bmp.getNinePatchChunk())) {
                drawNinePath(canvas, bmp, rect);
                return;
            }
        } catch (Exception e) {
        }
        canvas.drawBitmap(bmp, rect.left, rect.top, paint);
    }

    public static int dp2px(Context context, float dpValue) {
        if (context == null || compareFloat(0f, dpValue) == 0) return 0;
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static int compareFloat(float a, float b) {
        int ta = Math.round(a * 1000000);
        int tb = Math.round(b * 1000000);
        if (ta > tb) {
            return 1;
        } else if (ta < tb) {
            return -1;
        } else {
            return 0;
        }
    }

    public static int compareFloat(float a, float b, int degree) {
        if (Math.abs(a - b) < Math.pow(0.1, degree)) {
            return 0;
        } else {
            if (a < b) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    public static float parseFloat(String s) {
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    public static Rect measureText(String text, float textSize) {
        Paint paint = new Paint();
        Rect textRect = new Rect();
        paint.setTextSize(textSize);
        paint.getTextBounds(text, 0, text.length(), textRect);
        paint.reset();
        return textRect;
    }

    public static boolean verifyBitmap(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled() || bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0) {
            return false;
        }
        return true;
    }

    public static int getColor(Context context, @ColorRes int colorId) {
        if (context != null) {
            return ContextCompat.getColor(context.getApplicationContext(), colorId);
        }
        return Color.WHITE;
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\seekbar\VerticalRangeSeekBar.java =====

package com.mpdc4gsr.libunified.ui.widget.seekbar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;

import androidx.annotation.IntDef;

import com.mpdc4gsr.libunified.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class VerticalRangeSeekBar extends RangeSeekBar {

    public final static int TEXT_DIRECTION_VERTICAL = 1;
    public final static int TEXT_DIRECTION_HORIZONTAL = 2;
    public final static int DIRECTION_LEFT = 1;
    public final static int DIRECTION_RIGHT = 2;
    private int orientation = DIRECTION_LEFT;
    private int tickMarkDirection = TEXT_DIRECTION_VERTICAL;
    private int maxTickMarkWidth;
    private boolean noNegativeNumber = false;

    public VerticalRangeSeekBar(Context context) {
        this(context, null);
    }

    public VerticalRangeSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(attrs);
        initSeekBar(attrs);
    }

    private void initAttrs(AttributeSet attrs) {
        try {
            TypedArray t = getContext().obtainStyledAttributes(attrs, R.styleable.VerticalRangeSeekBar);
            orientation = t.getInt(R.styleable.VerticalRangeSeekBar_rsb_orientation, DIRECTION_LEFT);
            tickMarkDirection = t.getInt(R.styleable.VerticalRangeSeekBar_rsb_tick_mark_orientation, TEXT_DIRECTION_VERTICAL);
            t.recycle();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void initSeekBar(AttributeSet attrs) {
        leftSB = new VerticalSeekBar(this, attrs, true);
        rightSB = new VerticalSeekBar(this, attrs, false);
        rightSB.setVisible(getSeekBarMode() != SEEKBAR_MODE_SINGLE);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(h, w, oldh, oldw);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        /*
         * onMeasurewidthMeasureSpecheightMeasureSpecï¼Œ
         * MeasureSpec.EXACTLY
         * MeasureSpec.AT_MOST
         * MeasureSpec.UNSPECIFIED
         */

        if (widthMode == MeasureSpec.EXACTLY) {
            widthSize = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);
        } else if (widthMode == MeasureSpec.AT_MOST && getParent() instanceof ViewGroup
                && widthSize == ViewGroup.LayoutParams.MATCH_PARENT) {
            widthSize = MeasureSpec.makeMeasureSpec(((ViewGroup) getParent()).getMeasuredHeight(), MeasureSpec.AT_MOST);
        } else {
            int heightNeeded;
            if (getGravity() == Gravity.CENTER) {
                heightNeeded = 2 * getProgressTop() + getProgressHeight();
            } else {
                heightNeeded = (int) getRawHeight();
            }
            widthSize = MeasureSpec.makeMeasureSpec(heightNeeded, MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthSize, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (orientation == DIRECTION_LEFT) {
            canvas.rotate(-90);
            canvas.translate(-getHeight(), 0);
        } else {
            canvas.rotate(90);
            canvas.translate(0, -getWidth());
        }
        super.onDraw(canvas);
    }

    @Override
    protected void onDrawTickMark(Canvas canvas, Paint paint) {
        if (getTickMarkTextArray() != null) {
            int arrayLength = getTickMarkTextArray().length;
            int trickPartWidth = getProgressWidth() / (arrayLength - 1);
            for (int i = 0; i < arrayLength; i++) {
                final String text2Draw = getTickMarkTextArray()[i].toString();
                if (TextUtils.isEmpty(text2Draw)) continue;
                paint.getTextBounds(text2Draw, 0, text2Draw.length(), tickMarkTextRect);
                paint.setColor(getTickMarkTextColor());

                float x;
                if (getTickMarkMode() == TRICK_MARK_MODE_OTHER) {
                    if (getTickMarkGravity() == TICK_MARK_GRAVITY_RIGHT) {
                        x = getProgressLeft() + i * trickPartWidth - tickMarkTextRect.width();
                    } else if (getTickMarkGravity() == TICK_MARK_GRAVITY_CENTER) {
                        x = getProgressLeft() + i * trickPartWidth - tickMarkTextRect.width() / 2f;
                    } else {
                        x = getProgressLeft() + i * trickPartWidth;
                    }
                } else {
                    float num = Utils.parseFloat(text2Draw);
                    SeekBarState[] states = getRangeSeekBarState();
                    if (Utils.compareFloat(num, states[0].value) != -1 && Utils.compareFloat(num, states[1].value) != 1 && (getSeekBarMode() == SEEKBAR_MODE_RANGE)) {
                        paint.setColor(getTickMarkInRangeTextColor());
                    }

                    x = getProgressLeft() + getProgressWidth() * (num - getMinProgress()) / (getMaxProgress() - getMinProgress())
                            - tickMarkTextRect.width() / 2f;
                }
                float y;
                if (getTickMarkLayoutGravity() == Gravity.TOP) {
                    y = getProgressTop() - getTickMarkTextMargin();
                } else {
                    y = getProgressBottom() + getTickMarkTextMargin() + tickMarkTextRect.height();
                }
                int degrees = 0;
                float rotateX = (x + tickMarkTextRect.width() / 2f);
                float rotateY = (y - tickMarkTextRect.height() / 2f);
                if (tickMarkDirection == TEXT_DIRECTION_VERTICAL) {
                    if (orientation == DIRECTION_LEFT) {
                        degrees = 90;
                    } else if (orientation == DIRECTION_RIGHT) {
                        degrees = -90;
                    }
                }
                if (degrees != 0) {
                    canvas.rotate(degrees, rotateX, rotateY);
                }
                canvas.drawText(text2Draw, x, y, paint);
                if (degrees != 0) {
                    canvas.rotate(-degrees, rotateX, rotateY);
                }
            }
        }

    }

    @Override
    protected int getTickMarkRawHeight() {
        if (maxTickMarkWidth > 0) return getTickMarkTextMargin() + maxTickMarkWidth;
        if (getTickMarkTextArray() != null && getTickMarkTextArray().length > 0) {
            int arrayLength = getTickMarkTextArray().length;
            maxTickMarkWidth = Utils.measureText(String.valueOf(getTickMarkTextArray()[0]), getTickMarkTextSize()).width();
            for (int i = 1; i < arrayLength; i++) {
                int width = Utils.measureText(String.valueOf(getTickMarkTextArray()[i]), getTickMarkTextSize()).width();
                if (maxTickMarkWidth < width) {
                    maxTickMarkWidth = width;
                }
            }
            return getTickMarkTextMargin() + maxTickMarkWidth;
        }
        return 0;
    }

    public void setNoNegativeNumber(Boolean noNegativeNumber) {
        this.noNegativeNumber = noNegativeNumber;
        if (leftSB != null) {
            leftSB.setNoNegativeNumber(noNegativeNumber);
        }
        if (rightSB != null) {
            rightSB.setNoNegativeNumber(noNegativeNumber);
        }
    }

    @Override
    public void setTickMarkTextSize(int tickMarkTextSize) {
        super.setTickMarkTextSize(tickMarkTextSize);
        maxTickMarkWidth = 0;
    }

    @Override
    public void setTickMarkTextArray(CharSequence[] tickMarkTextArray) {
        super.setTickMarkTextArray(tickMarkTextArray);
        maxTickMarkWidth = 0;
    }

    @Override
    protected float getEventX(MotionEvent event) {
        if (orientation == DIRECTION_LEFT) {
            return getHeight() - event.getY();
        } else {
            return event.getY();
        }
    }

    @Override
    protected float getEventY(MotionEvent event) {
        if (orientation == DIRECTION_LEFT) {
            return event.getX();
        } else {
            return -event.getX() + getWidth();
        }
    }

    public void drawIndPath(boolean draw) {
        if (leftSB != null && leftSB instanceof VerticalSeekBar) {
            getLeftSeekBar().setDrawIndPathBg(draw);
        }
        if (rightSB != null && rightSB instanceof VerticalSeekBar) {
            getRightSeekBar().setDrawIndPathBg(draw);
        }
    }

    public VerticalSeekBar getLeftSeekBar() {
        return (VerticalSeekBar) leftSB;
    }

    public VerticalSeekBar getRightSeekBar() {
        return (VerticalSeekBar) rightSB;
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(@DirectionDef int orientation) {
        this.orientation = orientation;
    }

    public int getTickMarkDirection() {
        return tickMarkDirection;
    }

    public void setTickMarkDirection(@TextDirectionDef int tickMarkDirection) {
        this.tickMarkDirection = tickMarkDirection;
    }

    @IntDef({TEXT_DIRECTION_VERTICAL, TEXT_DIRECTION_HORIZONTAL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TextDirectionDef {
    }

    @IntDef({DIRECTION_LEFT, DIRECTION_RIGHT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DirectionDef {
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\seekbar\VerticalSeekBar.java =====

package com.mpdc4gsr.libunified.ui.widget.seekbar;

import android.graphics.Canvas;
import android.util.AttributeSet;

public class VerticalSeekBar extends SeekBar {

    public VerticalSeekBar(VerticalRangeSeekBar parent, AttributeSet attrs, boolean isLeft) {
        super(parent, attrs, isLeft);
    }

    public void setNoNegativeNumber(boolean noNegativeNumber) {
        // Implementation for vertical specific logic
    }

    public void setDrawIndPathBg(boolean draw) {
        // Implementation for draw indicator path background
    }

    @Override
    public void draw(Canvas canvas, boolean isLeft) {
        // Save canvas state for rotation
        canvas.save();

        // Rotate canvas for vertical drawing
        canvas.rotate(-90);

        // Call parent draw method
        super.draw(canvas, isLeft);

        // Restore canvas state
        canvas.restore();
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\SteeringWheelView.kt =====

package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.mpdc4gsr.libunified.R

class SteeringWheelView : LinearLayout, OnClickListener {
    private lateinit var tvConfirm: TextView
    private lateinit var steeringWheelStartBtn: ImageView
    private lateinit var steeringWheelCenterBtn: ImageView
    private lateinit var steeringWheelEndBtn: ImageView
    var listener: ((action: Int, moveX: Int) -> Unit)? = null
    var moveX = 30
    var rotationIR = 270
        set(value) {
            field = value
            if (value == 270 || value == 90) {
                if (::tvConfirm.isInitialized) tvConfirm.rotation = 270f
                rotation = 90f
            } else {
                if (::tvConfirm.isInitialized) tvConfirm.rotation = 0f
                rotation = 0f
            }
            requestLayout()
        }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private fun initView() {
        inflate(context, R.layout.ui_steering_wheel_view, this)
        tvConfirm = findViewById(R.id.tv_confirm)
        steeringWheelStartBtn = findViewById(R.id.steering_wheel_start_btn)
        steeringWheelCenterBtn = findViewById(R.id.steering_wheel_center_btn)
        steeringWheelEndBtn = findViewById(R.id.steering_wheel_end_btn)
        steeringWheelStartBtn.setOnClickListener(this)
        steeringWheelCenterBtn.setOnClickListener(this)
        steeringWheelEndBtn.setOnClickListener(this)
        if (rotationIR == 270 || rotationIR == 90) {
            tvConfirm.rotation = 270f
            rotation = 90f
        } else {
            tvConfirm.rotation = 0f
            rotation = 0f
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            steeringWheelStartBtn -> {
                moveX += 10
                if (moveX > 60) {
                    moveX = 60
                }
                listener?.invoke(-1, moveX)
            }

            steeringWheelCenterBtn -> {
                listener?.invoke(0, moveX)
            }

            steeringWheelEndBtn -> {
                moveX -= 10
                if (moveX < -20) {
                    moveX = -20
                }
                listener?.invoke(1, moveX)
            }
        }
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\TipsSeekBar.kt =====

package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.compat.dpToPx

class TipsSeekBar : ViewGroup, SeekBar.OnSeekBarChangeListener {
    private val tipsPercent: Float
    private val seekPercent: Float
    private val seekBar: SeekBar
    private val tvTips: TextView
    private val tvMin: TextView
    private val tvMax: TextView
    var progress: Int
        get() {
            return seekBar.progress
        }
        set(value) {
            seekBar.progress = value
            if (valueFormatListener != null) {
                tvTips.text = valueFormatListener?.invoke(value)
            }
        }
    var valueText: String
        get() {
            return tvTips.text.toString()
        }
        set(value) {
            tvTips.text = value
        }
    var onProgressChangeListener: ((progress: Int, fromUser: Boolean) -> Unit)? = null
    var onStopTrackingTouch: ((progress: Int) -> Unit)? = null
    var valueFormatListener: ((progress: Int) -> CharSequence?)? = null
        set(value) {
            tvTips.text = value?.invoke(seekBar.progress)
            field = value
        }

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
        defStyleRes
    ) {
        // seekBar  maxHeight  29  xml ï¼Œ View  maxHeight, attr  seekBar
        val thumb = ContextCompat.getDrawable(context, R.drawable.ic_tips_seek_bar_thumb)
        val thumbWidth = thumb?.intrinsicWidth ?: 0
        seekBar = SeekBar(context, attrs)
        seekBar.splitTrack = false
        seekBar.thumb = thumb
        seekBar.progressDrawable =
            ContextCompat.getDrawable(context, R.drawable.ui_progress_ir_camera_setting)
        seekBar.setPadding(thumbWidth / 2, 0, thumbWidth / 2, 0)
        seekBar.setOnSeekBarChangeListener(this)
        addView(seekBar, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        tvTips = TextView(context)
        tvTips.text = seekBar.progress.toString()
        tvTips.textSize = 12f
        tvTips.gravity = Gravity.CENTER
        tvTips.paint.isFakeBoldText = true
        tvTips.setTextColor(0xff16131e.toInt())
        tvTips.setBackgroundResource(R.drawable.ic_tips_seek_bar_tips_bg)
        addView(tvTips)
        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.TipsSeekBar, defStyleAttr, 0)
        val minText = typedArray.getText(R.styleable.TipsSeekBar_minText)
        val maxText = typedArray.getText(R.styleable.TipsSeekBar_maxText)
        tipsPercent = typedArray.getFraction(R.styleable.TipsSeekBar_tipsPercent, 1, 1, 0f)
        seekPercent = typedArray.getFraction(R.styleable.TipsSeekBar_seekPercent, 1, 1, 0f)
        typedArray.recycle()
        tvMin = TextView(context)
        tvMin.text = minText
        tvMin.textSize = 14f
        tvMin.setTextColor(0xffffffff.toInt())
        addView(tvMin)
        tvMax = TextView(context)
        tvMax.text = maxText
        tvMax.textSize = 14f
        tvMax.setTextColor(0xffffffff.toInt())
        addView(tvMax)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val width =
            if (widthMode == MeasureSpec.UNSPECIFIED) context.resources.displayMetrics.widthPixels else widthSize
        for (i in 0 until childCount) {
            when (val child = getChildAt(i)) {
                seekBar -> {
                    val childWidthSpec = MeasureSpec.makeMeasureSpec(
                        (width * seekPercent).toInt(),
                        MeasureSpec.EXACTLY
                    )
                    val childHeightSpc =
                        MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.AT_MOST)
                    child.measure(
                        childWidthSpec,
                        if (heightMode == MeasureSpec.EXACTLY) childHeightSpc else heightMeasureSpec
                    )
                }

                tvTips -> {
                    val tipsWidth = (width * tipsPercent).toInt()
                    val tipsHeight = (tipsWidth * 44 / 56f).toInt()
                    val childWidthSpec = MeasureSpec.makeMeasureSpec(tipsWidth, MeasureSpec.EXACTLY)
                    val childHeightSpc =
                        MeasureSpec.makeMeasureSpec(tipsHeight, MeasureSpec.EXACTLY)
                    child.measure(childWidthSpec, childHeightSpc)
                }

                else -> {
                    measureChild(child, widthMeasureSpec, heightMeasureSpec)
                }
            }
        }
        val height =
            tvTips.measuredHeight + 5f.dpToPx(context).toInt() + (seekBar.thumb?.intrinsicHeight
                ?: seekBar.measuredHeight)
        setMeasuredDimension(width, if (heightMode == MeasureSpec.EXACTLY) heightSize else height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val childWidth = child.measuredWidth
            val childHeight = child.measuredHeight
            when (child) {
                seekBar -> {
                    val top = paddingTop + tvTips.measuredHeight + 5f.dpToPx(context).toInt()
                    val left = (measuredWidth - childWidth) / 2
                    child.layout(left, top, left + childWidth, top + childHeight)
                }

                tvTips -> {
                    val seekBarSeeWidth =
                        seekBar.measuredWidth - seekBar.paddingLeft - seekBar.paddingRight
                    val baseLeft = (measuredWidth - seekBarSeeWidth) / 2
                    val progressLeft =
                        (seekBarSeeWidth * seekBar.progress / seekBar.max.toFloat()).toInt()
                    val left = baseLeft + progressLeft - childWidth / 2
                    child.layout(left, paddingTop, left + childWidth, paddingTop + childHeight)
                }

                tvMin -> {
                    val baseTop = paddingTop + tvTips.measuredHeight + 5f.dpToPx(context).toInt()
                    val top = baseTop + (seekBar.measuredHeight - childHeight) / 2
                    child.layout(paddingStart, top, paddingStart + childWidth, top + childHeight)
                }

                tvMax -> {
                    val baseTop = paddingTop + tvTips.measuredHeight + 5f.dpToPx(context).toInt()
                    val top = baseTop + (seekBar.measuredHeight - childHeight) / 2
                    val left = measuredWidth - paddingEnd - childWidth
                    child.layout(left, top, left + childWidth, top + childHeight)
                }
            }
        }
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        tvTips.text =
            if (valueFormatListener == null) progress.toString() else valueFormatListener?.invoke(
                progress
            )
        requestLayout()
        onProgressChangeListener?.invoke(progress, fromUser)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        onStopTrackingTouch?.invoke(this.seekBar.progress)
    }

    fun getFormattedValue(): String {
        return valueFormatListener?.invoke(progress)?.toString() ?: progress.toString()
    }
}


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\widget\WifiSteeringWheelView.kt =====

package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.mpdc4gsr.libunified.R

class WifiSteeringWheelView : LinearLayout, OnClickListener {
    private lateinit var tvConfirm: TextView
    private lateinit var steeringWheelStartBtn: ImageView
    private lateinit var steeringWheelCenterBtn: ImageView
    private lateinit var steeringWheelEndBtn: ImageView
    private lateinit var steeringWheelTopBtn: ImageView
    private lateinit var steeringWheelBottomBtn: ImageView
    var listener: ((action: Int, moveX: Int, moveY: Int) -> Unit)? = null
    var moveX = 0
    var moveY = 0
    var rotationIR = 270
        set(value) {
            field = value
            if (value == 270 || value == 90) {
                if (::tvConfirm.isInitialized) tvConfirm.rotation = 270f
                rotation = 90f
            } else {
                if (::tvConfirm.isInitialized) tvConfirm.rotation = 0f
                rotation = 0f
            }
            requestLayout()
        }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private fun initView() {
        inflate(context, R.layout.ui_wifi_steering_wheel_view, this)
        tvConfirm = findViewById(R.id.tv_confirm)
        steeringWheelStartBtn = findViewById(R.id.steering_wheel_start_btn)
        steeringWheelCenterBtn = findViewById(R.id.steering_wheel_center_btn)
        steeringWheelEndBtn = findViewById(R.id.steering_wheel_end_btn)
        steeringWheelTopBtn = findViewById(R.id.steering_wheel_top_btn)
        steeringWheelBottomBtn = findViewById(R.id.steering_wheel_bottom_btn)
        steeringWheelStartBtn.setOnClickListener(this)
        steeringWheelCenterBtn.setOnClickListener(this)
        steeringWheelEndBtn.setOnClickListener(this)
        steeringWheelTopBtn.setOnClickListener(this)
        steeringWheelBottomBtn.setOnClickListener(this)
        if (rotationIR == 270 || rotationIR == 90) {
            tvConfirm.rotation = 270f
            rotation = 90f
        } else {
            tvConfirm.rotation = 0f
            rotation = 0f
        }
    }

    val moveI = 2
    override fun onClick(v: View?) {
        when (v) {
            steeringWheelStartBtn -> {
//                moveY -= moveI
                listener?.invoke(-1, moveX, moveY)
            }

            steeringWheelCenterBtn -> {
                listener?.invoke(0, moveX, moveY)
            }

            steeringWheelTopBtn -> {
//                moveX += moveI
                listener?.invoke(2, moveX, moveY)
            }

            steeringWheelBottomBtn -> {
//                moveX -= moveI
                listener?.invoke(3, moveX, moveY)
            }

            steeringWheelEndBtn -> {
//                moveY += moveI
                listener?.invoke(1, moveX, moveY)
            }
        }
    }
}