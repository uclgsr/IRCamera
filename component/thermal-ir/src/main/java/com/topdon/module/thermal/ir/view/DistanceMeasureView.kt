package com.topdon.module.thermal.ir.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View



class DistanceMeasureView : View {
    private var margin: Float = 0f
    private var linePaint: Paint? = null
    private var line1Y = 0f
    private var line2Y = 0f
    var distance = 0f
    private set

    var moveListener: ((distance: Float) -> Unit)? = null

    constructor(context: Context?) : super(context) {
    init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
    init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    ) {
    init()
    }

    private fun init() {
        linePaint = Paint()
        linePaint!!.color = Color.GREEN
        linePaint!!.strokeWidth = 4f
        linePaint!!.style = Paint.Style.STROKE
//set虚线的间隔长度和线条长度
        val intervals = floatArrayOf(10f, 10f)
        linePaint!!.pathEffect = DashPathEffect(intervals, 0f)
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
//calculation初始位置，使得两条线居中，间隔20dp
        val screenHeight = measuredHeight
        val lineHeight = 50 // 在dimens.xml中定义line_height
        margin = ((screenHeight - lineHeight) / 2).toFloat()
        line1Y = margin
        line2Y = margin + lineHeight
        distance = lineHeight.toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
//drawing两条水平线
        canvas.drawLine(50f, line1Y, (width - 50).toFloat(), line1Y, linePaint!!)
        canvas.drawLine(50f, line2Y, (width - 50).toFloat(), line2Y, linePaint!!)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
    when (event.action) {
    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
    var newY = event.y

//限制线的范围，防止线移出视图范围
                if (newY < 0) {
                    newY = 0f
                } else if (newY > height) {
                    newY = height.toFloat()
                }

//根据触摸位置update线的位置
                if (Math.abs(newY - line1Y) < Math.abs(newY - line2Y)) {
                    val abs = line1Y - newY
                    line1Y = newY
                    line2Y += abs
                } else {
                    val abs = newY - line2Y
                    line2Y = newY
                    line1Y -= abs
                }
//update距离
                distance = Math.abs(line2Y - line1Y)
                invalidate()
                moveListener?.invoke(distance)
            }
        }
        return true
    }
}
