package com.mpdc4gsr.module.thermalunified.report.view

import android.content.Context
import android.graphics.Canvas
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import com.mpdc4gsr.module.thermalunified.compat.dpToPx
import com.mpdc4gsr.module.thermalunified.compat.spToPx

class WatermarkView : View {
    var watermarkText: String? = null
        set(value) {
            field = value
            invalidate()
        }
    private var marginTop: Float = 0f
    private val textPaint: TextPaint = TextPaint()

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    ) {
        marginTop = 220f.dpToPx(context)
        textPaint.isFakeBoldText = true
        textPaint.isAntiAlias = true
        textPaint.color = 0x082b79d8
        textPaint.textSize = 80f.spToPx(context)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        watermarkText?.let {
            var hasAddCount = 0
            var hasUseHeight = 0f
            while (hasUseHeight < height + marginTop) {
                canvas?.save()
                canvas?.rotate(15f)
                val translateX =
                    (width - textPaint.measureText(it)).coerceAtLeast(0f) / 2f + if (hasAddCount % 2 == 0) 100f else 0f
                canvas?.translate(translateX, 0f)
                canvas?.drawText(it, 0f, 0f, textPaint)
                canvas?.restore()
                canvas?.translate(0f, marginTop)
                hasUseHeight += marginTop
                hasAddCount++
            }
        }
    }
}
