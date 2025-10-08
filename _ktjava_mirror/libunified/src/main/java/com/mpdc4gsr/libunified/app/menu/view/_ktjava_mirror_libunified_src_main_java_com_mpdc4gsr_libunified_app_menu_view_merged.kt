// Merged ALL .kt and .java files from the '_ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\menu\view' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:45


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\menu\view\libunified_src_main_java_com_mpdc4gsr_libunified_app_menu_view_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\menu\view' subtree
// Files: 1; Generated 2025-10-07 23:07:50


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\menu\view\ColorView.kt =====

package com.mpdc4gsr.libunified.app.menu.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.mpdc4gsr.libunified.compat.dpToPx
import com.mpdc4gsr.libunified.compat.spToPx
import com.mpdc4gsr.libunified.R

class ColorView : View {
    var colors: IntArray = intArrayOf(0xfffbda00.toInt(), 0xffea0e0e.toInt(), 0xff6907af.toInt())
    var positions: FloatArray = floatArrayOf(0f, 0.5f, 1f)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var shaderSelectYes =
        LinearGradient(0f, 0f, 0f, 0f, colors, positions, Shader.TileMode.CLAMP)
    private var shaderSelectNot =
        LinearGradient(0f, 0f, 0f, 0f, colors, positions, Shader.TileMode.CLAMP)
    private val triangleDrawable: Drawable

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
        paint.color = 0xffffffff.toInt()
        triangleDrawable = ContextCompat.getDrawable(context, R.drawable.ic_color_select_svg)!!
    }

    @SuppressLint("DrawAllocation")
    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val width: Int = if (widthMode == MeasureSpec.UNSPECIFIED) 100 else widthSize
        val barHeight: Int =
            (width * 73f / 62).toInt() // 62 and 73 from UI design - selected state with border color block aspect ratio 62:73
        val triangleSize: Int =
            (width * 12f / 62).toInt() // 62 and 12 from UI design - triangle width 12, total width 62
        val margin: Int = 4f.dpToPx(context).toInt() // 4dp spacing between color block and triangle
        val wantHeight: Int = barHeight + margin + triangleSize
        val height =
            when (heightMode) {
                MeasureSpec.EXACTLY -> heightSize
                MeasureSpec.AT_MOST -> wantHeight.coerceAtMost(heightSize)
                else -> wantHeight
            }
        setMeasuredDimension(width, height)
        refreshShader()
        triangleDrawable.setBounds(
            (width - triangleSize) / 2,
            barHeight + margin,
            (width - triangleSize) / 2 + triangleSize,
            height
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius: Float = 10f.dpToPx(context).toFloat()
        val barHeight: Int =
            (width * 73f / 62).toInt() // 62 and 73 from UI design - selected state with border color block aspect ratio 62:73
        if (isSelected) {
            val strokeSize: Float = 2f.dpToPx(context).toFloat() // Border width 2dp
            val selectBarHeight: Int = (barHeight - strokeSize * 2).toInt()
            paint.shader = null
            canvas.drawRoundRect(
                0f,
                0f,
                width.toFloat(),
                barHeight.toFloat(),
                radius,
                radius,
                paint
            )
            paint.shader = shaderSelectYes
            canvas.drawRoundRect(
                strokeSize,
                strokeSize,
                width - strokeSize,
                strokeSize + selectBarHeight,
                radius,
                radius,
                paint
            )
            triangleDrawable.draw(canvas)
        } else {
            val normalBarWidth: Int =
                (width * 50f / 62).toInt() // Unselected width 50, total width 62
            val normalBarHeight: Int = (normalBarWidth * 60f / 50).toInt() // Aspect ratio 50:60
            val top: Float = ((barHeight - normalBarHeight) / 2).toFloat()
            val left: Float = ((width - normalBarWidth) / 2).toFloat()
            paint.shader = shaderSelectNot
            canvas.drawRoundRect(
                left,
                top,
                width - left,
                top + normalBarHeight,
                radius,
                radius,
                paint
            )
        }
    }

    fun refreshColor(
        colors: IntArray,
        positions: FloatArray,
    ) {
        this.colors = colors
        this.positions = positions
        refreshShader()
        invalidate()
    }

    private fun refreshShader() {
        val strokeSize: Float = 2f.dpToPx(context).toFloat() // Border width 2dp
        val barHeight: Int =
            (measuredWidth * 73f / 62).toInt() // 62 and 73 from UI design - selected state with border color block aspect ratio 62:73
        val selectBarHeight: Int = (barHeight - strokeSize * 2).toInt()
        shaderSelectYes = LinearGradient(
            0f,
            strokeSize,
            0f,
            strokeSize + selectBarHeight,
            colors,
            positions,
            Shader.TileMode.CLAMP
        )
        val normalBarWidth: Int =
            (measuredWidth * 50f / 62).toInt() // Unselected width 50, total width 62
        val normalBarHeight: Int = (normalBarWidth * 60f / 50).toInt() // Aspect ratio 50:60
        val top: Float = ((barHeight - normalBarHeight) / 2).toFloat()
        shaderSelectNot = LinearGradient(
            0f,
            top,
            0f,
            top + normalBarHeight,
            colors,
            positions,
            Shader.TileMode.CLAMP
        )
    }
}