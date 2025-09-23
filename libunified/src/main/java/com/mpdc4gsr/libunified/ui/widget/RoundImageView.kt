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

    var position = 0
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    private var radius = 0
    private val path = Path()
    private var density = 0f

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        density = context.resources.displayMetrics.density

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.RoundImageView, defStyleAttr, 0)
        radius = typedArray.getDimensionPixelSize(R.styleable.RoundImageView_round_radius, dp2px(DEFAULT_RADIUS))
        position = typedArray.getInt(R.styleable.RoundImageView_round_position, DEFAULT_POSITION)
        typedArray.recycle()
    }

    private fun dp2px(dpValue: Float): Int {
        return (dpValue * density + 0.5f).toInt()
    }

    override fun onDraw(canvas: Canvas) {
        if (radius > 0) {
            path.reset()
            val w = width.toFloat()
            val h = height.toFloat()
            val r = radius.toFloat()

            path.moveTo(0f, r)
            if (position and LEFT_TOP == LEFT_TOP) {
                path.quadTo(0f, 0f, r, 0f)
            } else {
                path.lineTo(0f, 0f)
                path.lineTo(r, 0f)
            }

            path.lineTo(w - r, 0f)
            if (position and RIGHT_TOP == RIGHT_TOP) {
                path.quadTo(w, 0f, w, r)
            } else {
                path.lineTo(w, 0f)
                path.lineTo(w, r)
            }

            path.lineTo(w, h - r)
            if (position and RIGHT_BOTTOM == RIGHT_BOTTOM) {
                path.quadTo(w, h, w - r, h)
            } else {
                path.lineTo(w, h)
                path.lineTo(w - r, h)
            }

            path.lineTo(r, h)
            if (position and LEFT_BOTTOM == LEFT_BOTTOM) {
                path.quadTo(0f, h, 0f, h - r)
            } else {
                path.lineTo(0f, h)
                path.lineTo(0f, h - r)
            }

            path.close()
            canvas.clipPath(path)
        }
        super.onDraw(canvas)
    }
}