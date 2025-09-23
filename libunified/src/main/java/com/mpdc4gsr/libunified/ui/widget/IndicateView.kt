package com.mpdc4gsr.libunified.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import com.blankj.utilcode.util.SizeUtils
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.app.utils.ScreenUtil

class IndicateView : View {
    
    @ColorInt
    private var selectColor: Int = 0xffffba42.toInt()
    
    @ColorInt  
    private var unSelectColor: Int = 0x80ffffff.toInt()
    
    private var count = 0
    private var currentIndex = 0
    
    private val roundRadius: Float = SizeUtils.dp2px(2f).toFloat()

    private val defaultPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val selectPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        defaultPaint.color = 0xffffffff.toInt()
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.IndicateView)
        val color = typedArray.getColor(R.styleable.IndicateView_selectColor, 0xffffba42.toInt())
        typedArray.recycle()
        selectPaint.color = color
        selectColor = color
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val screenWidth = ScreenUtil.getScreenWidth(context)
        val itemSize = SizeUtils.dp2px(8f)
        val maxWidth = screenWidth / 2
        val minWidth = count * itemSize + (count - 1) * SizeUtils.dp2px(4f)
        val finalWidth = minOf(maxWidth, minWidth)
        val height = itemSize
        setMeasuredDimension(finalWidth, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (count <= 0) return

        val itemSize = SizeUtils.dp2px(8f)
        val itemSpacing = SizeUtils.dp2px(4f)
        val totalWidth = width
        val totalItemsWidth = count * itemSize + (count - 1) * itemSpacing
        val startX = (totalWidth - totalItemsWidth) / 2f

        for (i in 0 until count) {
            val paint = if (i == currentIndex) selectPaint else defaultPaint
            val x = startX + i * (itemSize + itemSpacing)
            val centerX = x + itemSize / 2f
            val centerY = height / 2f
            val radius = itemSize / 2f

            canvas.drawCircle(centerX, centerY, radius, paint)
        }
    }
    
    fun setCount(count: Int) {
        this.count = count
        requestLayout()
    }
    
    fun setCurrentIndex(index: Int) {
        if (index != currentIndex) {
            currentIndex = index
            invalidate()
        }
    }
    
    fun getCurrentIndex(): Int = currentIndex
    
    fun getCount(): Int = count
}