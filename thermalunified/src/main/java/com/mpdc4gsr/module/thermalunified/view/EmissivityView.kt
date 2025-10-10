package com.mpdc4gsr.module.thermalunified.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import com.mpdc4gsr.module.thermalunified.compat.dpToPx
import com.mpdc4gsr.module.thermalunified.compat.spToPx

class EmissivityView : View {
    companion object {
        private const val DEFAULT_STROKE_WIDTH: Float = 0.5f
    }

    var isAlignTop = false
    var drawTopLine = false
    private val textList: ArrayList<CharSequence> = ArrayList(3)
    private val layoutList: ArrayList<StaticLayout> = ArrayList(3)
    private var strokeWidth: Float = 0f
    private val linePaint = Paint()
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        0,
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int,
    ) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes,
    ) {
        strokeWidth = DEFAULT_STROKE_WIDTH.dpToPx(context).coerceAtLeast(1f)
        linePaint.color = 0xff5b5961.toInt()
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeWidth = strokeWidth
    }

    fun refreshText(newList: List<String>) {
        textList.clear()
        textList.addAll(newList)
        textPaint.color = if (textList.size == 1) 0xffffffff.toInt() else 0xccffffff.toInt()
        textPaint.textSize = (if (textList.size == 1) 12f else 11f).spToPx(context)
        requestLayout()
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val widthSize: Int = MeasureSpec.getSize(widthMeasureSpec) - paddingStart - paddingEnd
        val firstWidth: Int = (widthSize * 135 / 335f).toInt()
        val elseWidth: Int = (widthSize - firstWidth) / 2
        val contentWidth: Int = firstWidth + elseWidth * 2
        layoutList.clear()
        for (i in textList.indices) {
            val textWidth: Int =
                if (textList.size == 1) {
                    contentWidth - 24.dpToPx(context)
                } else {
                    (if (i == 0) firstWidth else elseWidth) - 24.dpToPx(context)
                }
            layoutList.add(
                StaticLayout.Builder
                    .obtain(
                        textList[i],
                        0,
                        textList[i].length,
                        textPaint,
                        textWidth,
                    ).setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .build(),
            )
        }
        var maxHeight = 0
        for (layout in layoutList) {
            maxHeight = maxHeight.coerceAtLeast(layout.height)
        }
        if (maxHeight == 0) {
            maxHeight = textPaint.fontMetricsInt.bottom - textPaint.fontMetricsInt.top
        }
        maxHeight += 12.dpToPx(context)
        setMeasuredDimension(contentWidth + paddingStart + paddingEnd, maxHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.translate(paddingStart.toFloat(), 0f)
        val contentWidth = (width - paddingStart - paddingEnd).toFloat()
        if (drawTopLine) {
            canvas.drawLine(0f, strokeWidth / 2, contentWidth, strokeWidth / 2, linePaint)
        }
        canvas.drawLine(
            0f,
            height.toFloat() - strokeWidth / 2,
            contentWidth,
            height.toFloat() - strokeWidth / 2,
            linePaint,
        )
        canvas.drawLine(strokeWidth / 2, 0f, strokeWidth / 2, height.toFloat(), linePaint)
        val padding = 12f.dpToPx(context)
        for (layout in layoutList) {
            canvas.save()
            canvas.translate(
                padding,
                if (isAlignTop) 6f.dpToPx(context) else (height - layout.height) / 2f,
            )
            layout.draw(canvas)
            canvas.restore()
            val itemWidth = padding + layout.width.toFloat() + padding
            canvas.drawLine(
                itemWidth - strokeWidth / 2,
                0f,
                itemWidth - strokeWidth / 2,
                height.toFloat(),
                linePaint,
            )
            canvas.translate(itemWidth, 0f)
        }
    }
}
