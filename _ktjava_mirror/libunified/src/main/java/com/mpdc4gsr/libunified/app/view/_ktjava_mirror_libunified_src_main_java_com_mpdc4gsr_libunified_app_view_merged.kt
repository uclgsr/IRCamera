// Merged ALL .kt and .java files from the '_ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\view' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:45


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\view\libunified_src_main_java_com_mpdc4gsr_libunified_app_view_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\view' subtree
// Files: 7; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\view\ColorSelectView.kt =====

package com.mpdc4gsr.libunified.app.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt

class ColorSelectView : View {
    companion object {
        private const val DEFAULT_STROKE_WIDTH = 3
        private val ROW_COLOR_1 =
            intArrayOf(
                0xFFFEFFFE.toInt(),
                0xFFEBEBEB.toInt(),
                0xFFD6D6D6.toInt(),
                0xFFC2C2C2.toInt(),
                0xFFADADAD.toInt(),
                0xFF999999.toInt(),
                0xFF858585.toInt(),
                0xFF707070.toInt(),
                0xFF5C5C5C.toInt(),
                0xFF474747.toInt(),
                0xFF333333.toInt(),
                0xFF000000.toInt()
            )
        private val ROW_COLOR_2 =
            intArrayOf(
                0xFF00374A.toInt(),
                0xFF011D57.toInt(),
                0xFF11053B.toInt(),
                0xFF2E063D.toInt(),
                0xFF3C071B.toInt(),
                0xFF5C0701.toInt(),
                0xFF5A1C00.toInt(),
                0xFF583300.toInt(),
                0xFF563D00.toInt(),
                0xFF666100.toInt(),
                0xFF4F5504.toInt(),
                0xFF263E0F.toInt()
            )
        private val ROW_COLOR_3 =
            intArrayOf(
                0xFF004D65.toInt(),
                0xFF012F7B.toInt(),
                0xFF1A0A52.toInt(),
                0xFF450D59.toInt(),
                0xFF551029.toInt(),
                0xFF831100.toInt(),
                0xFF7B2900.toInt(),
                0xFF7A4A00.toInt(),
                0xFF785800.toInt(),
                0xFF8D8602.toInt(),
                0xFF6F760A.toInt(),
                0xFF38571A.toInt()
            )
        private val ROW_COLOR_4 =
            intArrayOf(
                0xFF016E8F.toInt(),
                0xFF0042A9.toInt(),
                0xFF2C0977.toInt(),
                0xFF61187C.toInt(),
                0xFF791A3D.toInt(),
                0xFFB51A00.toInt(),
                0xFFAD3E00.toInt(),
                0xFFA96800.toInt(),
                0xFFA67B01.toInt(),
                0xFFC4BC00.toInt(),
                0xFF9BA50E.toInt(),
                0xFF4E7A27.toInt()
            )
        private val ROW_COLOR_5 =
            intArrayOf(
                0xFF008CB4.toInt(),
                0xFF0056D6.toInt(),
                0xFF371A94.toInt(),
                0xFF7A219E.toInt(),
                0xFF99244F.toInt(),
                0xFFE22400.toInt(),
                0xFFDA5100.toInt(),
                0xFFD38301.toInt(),
                0xFFD19D01.toInt(),
                0xFFF5EC00.toInt(),
                0xFFC3D117.toInt(),
                0xFF669D34.toInt()
            )
        private val ROW_COLOR_6 =
            intArrayOf(
                0xFF00A1D8.toInt(),
                0xFF0061FD.toInt(),
                0xFF4D22B2.toInt(),
                0xFF982ABC.toInt(),
                0xFFB92D5D.toInt(),
                0xFFFF4015.toInt(),
                0xFFFF6A00.toInt(),
                0xFFFFAB01.toInt(),
                0xFFFCC700.toInt(),
                0xFFFEFB41.toInt(),
                0xFFD9EC37.toInt(),
                0xFF76BB40.toInt()
            )
        private val ROW_COLOR_7 =
            intArrayOf(
                0xFF01C7FC.toInt(),
                0xFF3A87FD.toInt(),
                0xFF5E30EB.toInt(),
                0xFFBE38F3.toInt(),
                0xFFE63B7A.toInt(),
                0xFFFE6250.toInt(),
                0xFFFE8648.toInt(),
                0xFFFEB43F.toInt(),
                0xFFFECB3E.toInt(),
                0xFFFFF76B.toInt(),
                0xFFE4EF65.toInt(),
                0xFF96D35F.toInt()
            )
        private val ROW_COLOR_8 =
            intArrayOf(
                0xFF52D6FC.toInt(),
                0xFF74A7FF.toInt(),
                0xFF864FFD.toInt(),
                0xFFD357FE.toInt(),
                0xFFEE719E.toInt(),
                0xFFFF8C82.toInt(),
                0xFFFEA57D.toInt(),
                0xFFFEC777.toInt(),
                0xFFFED977.toInt(),
                0xFFFFF994.toInt(),
                0xFFEAF28F.toInt(),
                0xFFB1DD8B.toInt()
            )
        private val ROW_COLOR_9 =
            intArrayOf(
                0xFF93E3FC.toInt(),
                0xFFA7C6FF.toInt(),
                0xFFB18CFE.toInt(),
                0xFFE292FE.toInt(),
                0xFFF4A4C0.toInt(),
                0xFFFFB5AF.toInt(),
                0xFFFFC5AB.toInt(),
                0xFFFED9A8.toInt(),
                0xFFFDE4A8.toInt(),
                0xFFFFFBB9.toInt(),
                0xFFF1F7B7.toInt(),
                0xFFCDE8B5.toInt()
            )
        private val ROW_COLOR_10 =
            intArrayOf(
                0xFFCBF0FF.toInt(),
                0xFFD2E2FE.toInt(),
                0xFFD8C9FE.toInt(),
                0xFFEFCAFE.toInt(),
                0xFFF9D3E0.toInt(),
                0xFFFFDAD8.toInt(),
                0xFFFFE2D6.toInt(),
                0xFFFEECD4.toInt(),
                0xFFFEF1D5.toInt(),
                0xFFFDFBDD.toInt(),
                0xFFF6FADB.toInt(),
                0xFFDEEED4.toInt()
            )
        private val COLOR =
            arrayOf(
                ROW_COLOR_1,
                ROW_COLOR_2,
                ROW_COLOR_3,
                ROW_COLOR_4,
                ROW_COLOR_5,
                ROW_COLOR_6,
                ROW_COLOR_7,
                ROW_COLOR_8,
                ROW_COLOR_9,
                ROW_COLOR_10
            )

        private fun getRowFromColor(
            @ColorInt color: Int,
        ): Int =
            when (color) {
                0xFFFEFFFE.toInt(), 0xFFEBEBEB.toInt(), 0xFFD6D6D6.toInt(), 0xFFC2C2C2.toInt(), 0xFFADADAD.toInt(), 0xFF999999.toInt(), 0xFF858585.toInt(), 0xFF707070.toInt(), 0xFF5C5C5C.toInt(), 0xFF474747.toInt(), 0xFF333333.toInt(), 0xFF000000.toInt() -> 0
                0xFF00374A.toInt(), 0xFF011D57.toInt(), 0xFF11053B.toInt(), 0xFF2E063D.toInt(), 0xFF3C071B.toInt(), 0xFF5C0701.toInt(), 0xFF5A1C00.toInt(), 0xFF583300.toInt(), 0xFF563D00.toInt(), 0xFF666100.toInt(), 0xFF4F5504.toInt(), 0xFF263E0F.toInt() -> 1
                0xFF004D65.toInt(), 0xFF012F7B.toInt(), 0xFF1A0A52.toInt(), 0xFF450D59.toInt(), 0xFF551029.toInt(), 0xFF831100.toInt(), 0xFF7B2900.toInt(), 0xFF7A4A00.toInt(), 0xFF785800.toInt(), 0xFF8D8602.toInt(), 0xFF6F760A.toInt(), 0xFF38571A.toInt() -> 2
                0xFF016E8F.toInt(), 0xFF0042A9.toInt(), 0xFF2C0977.toInt(), 0xFF61187C.toInt(), 0xFF791A3D.toInt(), 0xFFB51A00.toInt(), 0xFFAD3E00.toInt(), 0xFFA96800.toInt(), 0xFFA67B01.toInt(), 0xFFC4BC00.toInt(), 0xFF9BA50E.toInt(), 0xFF4E7A27.toInt() -> 3
                0xFF008CB4.toInt(), 0xFF0056D6.toInt(), 0xFF371A94.toInt(), 0xFF7A219E.toInt(), 0xFF99244F.toInt(), 0xFFE22400.toInt(), 0xFFDA5100.toInt(), 0xFFD38301.toInt(), 0xFFD19D01.toInt(), 0xFFF5EC00.toInt(), 0xFFC3D117.toInt(), 0xFF669D34.toInt() -> 4
                0xFF00A1D8.toInt(), 0xFF0061FD.toInt(), 0xFF4D22B2.toInt(), 0xFF982ABC.toInt(), 0xFFB92D5D.toInt(), 0xFFFF4015.toInt(), 0xFFFF6A00.toInt(), 0xFFFFAB01.toInt(), 0xFFFCC700.toInt(), 0xFFFEFB41.toInt(), 0xFFD9EC37.toInt(), 0xFF76BB40.toInt() -> 5
                0xFF01C7FC.toInt(), 0xFF3A87FD.toInt(), 0xFF5E30EB.toInt(), 0xFFBE38F3.toInt(), 0xFFE63B7A.toInt(), 0xFFFE6250.toInt(), 0xFFFE8648.toInt(), 0xFFFEB43F.toInt(), 0xFFFECB3E.toInt(), 0xFFFFF76B.toInt(), 0xFFE4EF65.toInt(), 0xFF96D35F.toInt() -> 6
                0xFF52D6FC.toInt(), 0xFF74A7FF.toInt(), 0xFF864FFD.toInt(), 0xFFD357FE.toInt(), 0xFFEE719E.toInt(), 0xFFFF8C82.toInt(), 0xFFFEA57D.toInt(), 0xFFFEC777.toInt(), 0xFFFED977.toInt(), 0xFFFFF994.toInt(), 0xFFEAF28F.toInt(), 0xFFB1DD8B.toInt() -> 7
                0xFF93E3FC.toInt(), 0xFFA7C6FF.toInt(), 0xFFB18CFE.toInt(), 0xFFE292FE.toInt(), 0xFFF4A4C0.toInt(), 0xFFFFB5AF.toInt(), 0xFFFFC5AB.toInt(), 0xFFFED9A8.toInt(), 0xFFFDE4A8.toInt(), 0xFFFFFBB9.toInt(), 0xFFF1F7B7.toInt(), 0xFFCDE8B5.toInt() -> 8
                0xFFCBF0FF.toInt(), 0xFFD2E2FE.toInt(), 0xFFD8C9FE.toInt(), 0xFFEFCAFE.toInt(), 0xFFF9D3E0.toInt(), 0xFFFFDAD8.toInt(), 0xFFFFE2D6.toInt(), 0xFFFEECD4.toInt(), 0xFFFEF1D5.toInt(), 0xFFFDFBDD.toInt(), 0xFFF6FADB.toInt(), 0xFFDEEED4.toInt() -> 9
                else -> -1
            }

        private fun getColumnFromColor(
            @ColorInt color: Int,
        ): Int =
            when (color) {
                0xFFFEFFFE.toInt(), 0xFF00374A.toInt(), 0xFF004D65.toInt(), 0xFF016E8F.toInt(), 0xFF008CB4.toInt(), 0xFF00A1D8.toInt(), 0xFF01C7FC.toInt(), 0xFF52D6FC.toInt(), 0xFF93E3FC.toInt(), 0xFFCBF0FF.toInt() -> 0
                0xFFEBEBEB.toInt(), 0xFF011D57.toInt(), 0xFF012F7B.toInt(), 0xFF0042A9.toInt(), 0xFF0056D6.toInt(), 0xFF0061FD.toInt(), 0xFF3A87FD.toInt(), 0xFF74A7FF.toInt(), 0xFFA7C6FF.toInt(), 0xFFD2E2FE.toInt() -> 1
                0xFFD6D6D6.toInt(), 0xFF11053B.toInt(), 0xFF1A0A52.toInt(), 0xFF2C0977.toInt(), 0xFF371A94.toInt(), 0xFF4D22B2.toInt(), 0xFF5E30EB.toInt(), 0xFF864FFD.toInt(), 0xFFB18CFE.toInt(), 0xFFD8C9FE.toInt() -> 2
                0xFFC2C2C2.toInt(), 0xFF2E063D.toInt(), 0xFF450D59.toInt(), 0xFF61187C.toInt(), 0xFF7A219E.toInt(), 0xFF982ABC.toInt(), 0xFFBE38F3.toInt(), 0xFFD357FE.toInt(), 0xFFE292FE.toInt(), 0xFFEFCAFE.toInt() -> 3
                0xFFADADAD.toInt(), 0xFF3C071B.toInt(), 0xFF551029.toInt(), 0xFF791A3D.toInt(), 0xFF99244F.toInt(), 0xFFB92D5D.toInt(), 0xFFE63B7A.toInt(), 0xFFEE719E.toInt(), 0xFFF4A4C0.toInt(), 0xFFF9D3E0.toInt() -> 4
                0xFF999999.toInt(), 0xFF5C0701.toInt(), 0xFF831100.toInt(), 0xFFB51A00.toInt(), 0xFFE22400.toInt(), 0xFFFF4015.toInt(), 0xFFFE6250.toInt(), 0xFFFF8C82.toInt(), 0xFFFFB5AF.toInt(), 0xFFFFDAD8.toInt() -> 5
                0xFF858585.toInt(), 0xFF5A1C00.toInt(), 0xFF7B2900.toInt(), 0xFFAD3E00.toInt(), 0xFFDA5100.toInt(), 0xFFFF6A00.toInt(), 0xFFFE8648.toInt(), 0xFFFEA57D.toInt(), 0xFFFFC5AB.toInt(), 0xFFFFE2D6.toInt() -> 6
                0xFF707070.toInt(), 0xFF583300.toInt(), 0xFF7A4A00.toInt(), 0xFFA96800.toInt(), 0xFFD38301.toInt(), 0xFFFFAB01.toInt(), 0xFFFEB43F.toInt(), 0xFFFEC777.toInt(), 0xFFFED9A8.toInt(), 0xFFFEECD4.toInt() -> 7
                0xFF5C5C5C.toInt(), 0xFF563D00.toInt(), 0xFF785800.toInt(), 0xFFA67B01.toInt(), 0xFFD19D01.toInt(), 0xFFFCC700.toInt(), 0xFFFECB3E.toInt(), 0xFFFED977.toInt(), 0xFFFDE4A8.toInt(), 0xFFFEF1D5.toInt() -> 8
                0xFF474747.toInt(), 0xFF666100.toInt(), 0xFF8D8602.toInt(), 0xFFC4BC00.toInt(), 0xFFF5EC00.toInt(), 0xFFFEFB41.toInt(), 0xFFFFF76B.toInt(), 0xFFFFF994.toInt(), 0xFFFFFBB9.toInt(), 0xFFFDFBDD.toInt() -> 9
                0xFF333333.toInt(), 0xFF4F5504.toInt(), 0xFF6F760A.toInt(), 0xFF9BA50E.toInt(), 0xFFC3D117.toInt(), 0xFFD9EC37.toInt(), 0xFFE4EF65.toInt(), 0xFFEAF28F.toInt(), 0xFFF1F7B7.toInt(), 0xFFF6FADB.toInt() -> 10
                0xFF000000.toInt(), 0xFF263E0F.toInt(), 0xFF38571A.toInt(), 0xFF4E7A27.toInt(), 0xFF669D34.toInt(), 0xFF76BB40.toInt(), 0xFF96D35F.toInt(), 0xFFB1DD8B.toInt(), 0xFFCDE8B5.toInt(), 0xFFDEEED4.toInt() -> 11
                else -> -1
            }
    }

    var isNeedStroke: Boolean = false
        set(value) {
            invalidate()
            field = value
        }
    var onSelectListener: ((color: Int) -> Unit)? = null
    fun reset() {
        currentRow = -1
        currentColumn = -1
        invalidate()
    }

    fun selectColor(
        @ColorInt color: Int,
    ) {
        currentRow = getRowFromColor(color)
        currentColumn = getColumnFromColor(color)
        invalidate()
    }

    private var currentRow: Int = -1
    private var currentColumn: Int = -1
    private val widthPixels: Int
    private val density: Float
    private val strokeWidth: Int
    private val path = Path()
    private val itemPaint = Paint()
    private val itemSelectPaint = Paint()
    private val strokePaint = Paint()

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
        widthPixels = context.resources.displayMetrics.widthPixels
        density = context.resources.displayMetrics.density
        strokeWidth = dp2px(DEFAULT_STROKE_WIDTH.toFloat())
        itemPaint.isAntiAlias = true
        itemPaint.style = Paint.Style.FILL
        itemSelectPaint.isAntiAlias = true
        itemSelectPaint.style = Paint.Style.STROKE
        itemSelectPaint.color = 0xffffffff.toInt()
        itemSelectPaint.strokeWidth = strokeWidth.toFloat()
        strokePaint.isAntiAlias = true
        strokePaint.style = Paint.Style.STROKE
        strokePaint.color = 0xff999999.toInt()
        strokePaint.strokeWidth = strokeWidth / 2f
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val itemSize =
            ((if (widthMode == MeasureSpec.UNSPECIFIED) widthPixels else widthSize - strokeWidth) / 12f).toInt()
        val width = itemSize * 12 + strokeWidth
        val wantHeight = itemSize * 10 + strokeWidth
        val height =
            when (heightMode) {
                MeasureSpec.EXACTLY -> heightSize
                MeasureSpec.AT_MOST -> wantHeight.coerceAtMost(heightSize)
                MeasureSpec.UNSPECIFIED -> wantHeight
                else -> wantHeight
            }
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val itemSize = (measuredWidth - strokeWidth) / 12f
        val connerSize = itemSize * 8f / 26f
        val margin = strokeWidth / 2f
        if (isNeedStroke) {
            path.rewind()
            path.moveTo(margin, margin + connerSize)
            path.quadTo(margin, margin, margin + connerSize, margin)
            path.lineTo(margin + itemSize * 12 - connerSize, margin)
            path.quadTo(margin + itemSize * 12, margin, margin + itemSize * 12, margin + connerSize)
            path.lineTo(margin + itemSize * 12, margin + itemSize * 10 - connerSize)
            path.quadTo(
                margin + itemSize * 12,
                margin + itemSize * 10,
                margin + itemSize * 12 - connerSize,
                margin + itemSize * 10
            )
            path.lineTo(margin + connerSize, margin + itemSize * 10)
            path.quadTo(margin, margin + itemSize * 10, margin, margin + itemSize * 10 - connerSize)
            path.close()
            canvas.drawPath(path, strokePaint)
        }
        for (row in 0 until 10) {
            for (column in 0 until 12) {
                itemPaint.color = COLOR[row][column]
                if (row == 0 && column == 0) {
                    path.rewind()
                    path.moveTo(margin, margin + connerSize)
                    path.quadTo(margin, margin, margin + connerSize, margin)
                    path.lineTo(margin + itemSize, margin)
                    path.lineTo(margin + itemSize, margin + itemSize)
                    path.lineTo(margin, margin + itemSize)
                    path.close()
                    canvas.drawPath(path, itemPaint)
                } else if (row == 0 && column == 11) {
                    path.rewind()
                    path.moveTo(margin + itemSize * 12 - connerSize, margin)
                    path.quadTo(
                        margin + itemSize * 12,
                        margin,
                        margin + itemSize * 12,
                        margin + connerSize
                    )
                    path.lineTo(margin + itemSize * 12, margin + itemSize)
                    path.lineTo(margin + itemSize * 11, margin + itemSize)
                    path.lineTo(margin + itemSize * 11, margin)
                    path.close()
                    canvas.drawPath(path, itemPaint)
                } else if (row == 9 && column == 0) {
                    path.rewind()
                    path.moveTo(margin + connerSize, margin + itemSize * 10)
                    path.quadTo(
                        margin,
                        margin + itemSize * 10,
                        margin,
                        margin + itemSize * 10 - connerSize
                    )
                    path.lineTo(margin, margin + itemSize * 9)
                    path.lineTo(margin + itemSize, margin + itemSize * 9)
                    path.lineTo(margin + itemSize, margin + itemSize * 10)
                    path.close()
                    canvas.drawPath(path, itemPaint)
                } else if (row == 9 && column == 11) {
                    path.rewind()
                    path.moveTo(margin + itemSize * 12, margin + itemSize * 10 - connerSize)
                    path.quadTo(
                        margin + itemSize * 12,
                        margin + itemSize * 10,
                        margin + itemSize * 12 - connerSize,
                        margin + itemSize * 10
                    )
                    path.lineTo(margin + itemSize * 11, margin + itemSize * 10)
                    path.lineTo(margin + itemSize * 11, margin + itemSize * 9)
                    path.lineTo(margin + itemSize * 12, margin + itemSize * 9)
                    path.close()
                    canvas.drawPath(path, itemPaint)
                } else {
                    val left = margin + itemSize * column
                    val top = margin + itemSize * row
                    canvas.drawRect(left, top, left + itemSize, top + itemSize, itemPaint)
                }
            }
        }
        if (currentRow >= 0 && currentColumn >= 0) {
            val left = margin + itemSize * currentColumn
            val top = margin + itemSize * currentRow
            val right = left + itemSize
            val bottom = top + itemSize
            path.rewind()
            if (currentRow == 0 && currentColumn == 0) {
                path.moveTo(left, top + connerSize)
                path.quadTo(left, top, left + connerSize, top)
            } else {
                path.moveTo(left, top + strokeWidth)
                path.quadTo(left, top, left + strokeWidth, top)
            }
            if (currentRow == 0 && currentColumn == 11) {
                path.lineTo(right - connerSize, top)
                path.quadTo(right, top, right, top + connerSize)
            } else {
                path.lineTo(right - strokeWidth, top)
                path.quadTo(right, top, right, top + strokeWidth)
            }
            if (currentRow == 9 && currentColumn == 11) {
                path.lineTo(right, bottom - connerSize)
                path.quadTo(right, bottom, right - connerSize, bottom)
            } else {
                path.lineTo(right, bottom - strokeWidth)
                path.quadTo(right, bottom, right - strokeWidth, bottom)
            }
            if (currentRow == 9 && currentColumn == 0) {
                path.lineTo(left + connerSize, bottom)
                path.quadTo(left, bottom, left, bottom - connerSize)
            } else {
                path.lineTo(left + strokeWidth, bottom)
                path.quadTo(left, bottom, left, bottom - strokeWidth)
            }
            path.close()
            canvas?.drawPath(path, itemSelectPaint)
        }
    }

    private var downRow = 0
    private var downColumn = 0
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return false
        }
        val x = event.x.toInt() - strokeWidth / 2
        val y = event.y.toInt() - strokeWidth / 2
        val itemSize = (measuredWidth - strokeWidth) / 12
        val column =
            (x / itemSize + (if (x % itemSize > 0) 1 else 0) - 1).coerceAtMost(11).coerceAtLeast(0)
        val row =
            (y / itemSize + (if (y % itemSize > 0) 1 else 0) - 1).coerceAtMost(9).coerceAtLeast(0)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downRow = row
                downColumn = column
            }

            MotionEvent.ACTION_UP -> {
                if (row == downRow && column == downColumn) {
                    currentRow = row
                    currentColumn = column
                    invalidate()
                    onSelectListener?.invoke(COLOR[row][column])
                }
            }
        }
        return true
    }

    private fun dp2px(dpValue: Float): Int {
        return (dpValue * density + 0.5f).toInt()
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\view\ImageEditView.kt =====

package com.mpdc4gsr.libunified.app.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class ImageEditView : View {
    companion object {
        private const val PAINT_WIDTH = 6
        private const val HALF_PAINT_WIDTH = 3
        private const val ARROW_WIDTH = 30
        private const val PAINT_COLOR = 0xffe22400.toInt()
    }

    enum class Type {
        CIRCLE,
        RECT,
        ARROW,
    }

    var type: Type = Type.CIRCLE
    var color: Int
        get() = paint.color
        set(value) {
            paint.color = value
            invalidate()
        }
    var sourceBitmap: Bitmap? = null
        set(value) {
            if (value == null) {
                return
            }
            if (width == 0 || height == 0) {
                bgBitmap = null
            } else {
                bgBitmap = Bitmap.createScaledBitmap(value, width, height, true)
                invalidate()
            }
            field = value
        }
    private var hasEditData = false
    private var bgBitmap: Bitmap? = null
    private var editBitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val path = Path()

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
        paint.color = PAINT_COLOR
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = PAINT_WIDTH.toFloat()
        paint.isDither = true
    }

    fun clear() {
        hasEditData = false
        canvas?.drawColor(0x00000000, PorterDuff.Mode.CLEAR)
        invalidate()
    }

    fun buildResultBitmap(): Bitmap? {
        val bgBitmap = this.bgBitmap ?: return null
        val editBitmap = this.editBitmap
        if (hasEditData && editBitmap != null) {
            val canvas = Canvas(bgBitmap)
            canvas.drawBitmap(editBitmap, 0f, 0f, null)
        }
        return bgBitmap
    }

    @SuppressLint("DrawAllocation")
    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) {
        super.onLayout(changed, left, top, right, bottom)
        val oldBitmap = editBitmap
        if (oldBitmap == null || oldBitmap.width != measuredWidth || oldBitmap.height != measuredHeight) {
            val newBitmap =
                if (oldBitmap == null) {
                    Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
                } else {
                    Bitmap.createScaledBitmap(oldBitmap, measuredWidth, measuredHeight, true)
                }
            canvas = Canvas(newBitmap)
            editBitmap = newBitmap
        }
        sourceBitmap?.let {
            if (bgBitmap == null) {
                bgBitmap = Bitmap.createScaledBitmap(it, measuredWidth, measuredHeight, true)
            } else {
                if (bgBitmap?.width != measuredWidth || bgBitmap?.height != measuredHeight) {
                    bgBitmap = Bitmap.createScaledBitmap(it, measuredWidth, measuredHeight, true)
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bgBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }
        editBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }
        drawEdit(canvas)
    }

    private fun drawEdit(canvas: Canvas?) {
        if (downX == 0 && downY == 0 && currentX == 0 && currentY == 0) {
            return
        }
        when (type) {
            Type.CIRCLE -> {
                paint.style = Paint.Style.STROKE
                val left = downX.coerceAtMost(currentX).toFloat()
                val top = downY.coerceAtMost(currentY).toFloat()
                val right = downX.coerceAtLeast(currentX).toFloat()
                val bottom = downY.coerceAtLeast(currentY).toFloat()
                canvas?.drawOval(left, top, right, bottom, paint)
            }

            Type.RECT -> {
                paint.style = Paint.Style.STROKE
                val left = downX.coerceAtMost(currentX).toFloat()
                val top = downY.coerceAtMost(currentY).toFloat()
                val right = downX.coerceAtLeast(currentX).toFloat()
                val bottom = downY.coerceAtLeast(currentY).toFloat()
                canvas?.drawRect(left, top, right, bottom, paint)
            }

            Type.ARROW -> {
                if (abs(downX - currentX) < ARROW_WIDTH && abs(downY - currentY) < ARROW_WIDTH) {
                    return
                }
                paint.style = Paint.Style.FILL
                path.reset()
                if (downX == currentX) {
                    val endY =
                        if (downY > currentY) currentY + PAINT_WIDTH else (currentY - PAINT_WIDTH)
                    canvas?.drawLine(
                        downX.toFloat(),
                        downY.toFloat(),
                        currentX.toFloat(),
                        endY.toFloat(),
                        paint
                    )
                    val triangleH: Float = (ARROW_WIDTH / 2) * sqrt(3f)
                    val y: Float =
                        if (downY > currentY) currentY + triangleH else (currentY - triangleH)
                    val x1: Float = downX - (ARROW_WIDTH / 2f)
                    val x2: Float = downX + (ARROW_WIDTH / 2f)
                    path.moveTo(currentX.toFloat(), currentY.toFloat())
                    path.lineTo(x1, y)
                    path.lineTo(x2, y)
                    path.close()
                    canvas?.drawPath(path, paint)
                } else if (downY == currentY) {
                    val endX =
                        if (downX > currentX) currentX + PAINT_WIDTH else (currentX - PAINT_WIDTH)
                    canvas?.drawLine(
                        downX.toFloat(),
                        downY.toFloat(),
                        endX.toFloat(),
                        currentY.toFloat(),
                        paint
                    )
                    val triangleH: Float = (ARROW_WIDTH / 2) * sqrt(3f)
                    val x: Float =
                        if (downX > currentX) currentX + triangleH else (currentX - triangleH)
                    val y1: Float = downY - (ARROW_WIDTH / 2f)
                    val y2: Float = downY + (ARROW_WIDTH / 2f)
                    path.moveTo(currentX.toFloat(), currentY.toFloat())
                    path.lineTo(x, y1)
                    path.lineTo(x, y2)
                    path.close()
                    canvas?.drawPath(path, paint)
                } else {
                    val k1: Float = (downY - currentY).toFloat() / (downX - currentX).toFloat()
                    val b1: Float = downY - k1 * downX
                    val a1: Float = -b1 / k1
                    val backWidth = PAINT_WIDTH
                    val endY: Float =
                        if (k1 > 0) {
                            val hypotenuse: Float =
                                sqrt((currentX - a1).pow(2) + currentY.toFloat().pow(2))
                            if (currentX > downX) {
                                currentY * (hypotenuse - backWidth) / hypotenuse
                            } else {
                                currentY * (hypotenuse + backWidth) / hypotenuse
                            }
                        } else {
                            val hypotenuse: Float =
                                sqrt((a1 - currentX).pow(2) + currentY.toFloat().pow(2))
                            if (currentX > downX) {
                                currentY * (hypotenuse + backWidth) / hypotenuse
                            } else {
                                currentY * (hypotenuse - backWidth) / hypotenuse
                            }
                        }
                    val endX = (endY - b1) / k1
                    canvas?.drawLine(downX.toFloat(), downY.toFloat(), endX, endY, paint)
                    val triangleH: Float = (ARROW_WIDTH / 2) * sqrt(3f)
                    val y: Float =
                        if (k1 > 0) {
                            val hypotenuse: Float =
                                sqrt((currentX - a1).pow(2) + currentY.toFloat().pow(2))
                            if (currentX > downX) {
                                currentY * (hypotenuse - triangleH) / hypotenuse
                            } else {
                                currentY * (hypotenuse + triangleH) / hypotenuse
                            }
                        } else {
                            val hypotenuse: Float =
                                sqrt((a1 - currentX).pow(2) + currentY.toFloat().pow(2))
                            if (currentX > downX) {
                                currentY * (hypotenuse + triangleH) / hypotenuse
                            } else {
                                currentY * (hypotenuse - triangleH) / hypotenuse
                            }
                        }
                    val x = (y - b1) / k1
                    val k2: Float = -1 / k1
                    val b2: Float = y - k2 * x
                    val a2: Float = -b2 / k2
                    val hypotenuse2: Float =
                        sqrt((if (k2 > 0) x - a2 else (a2 - x)).pow(2) + y.pow(2))
                    val yLeft = y * (hypotenuse2 - ARROW_WIDTH / 2) / hypotenuse2
                    val yRight = y * (hypotenuse2 + ARROW_WIDTH / 2) / hypotenuse2
                    val xLeft = (yLeft - b2) / k2
                    val xRight = (yRight - b2) / k2
                    path.moveTo(currentX.toFloat(), currentY.toFloat())
                    path.lineTo(xLeft, yLeft)
                    path.lineTo(xRight, yRight)
                    path.close()
                    canvas?.drawPath(path, paint)
                }
            }
        }
    }

    private var downX = 0
    private var downY = 0
    private var currentX = 0
    private var currentY = 0

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null || !isEnabled) {
            return false
        }
        currentX =
            event.x.toInt().coerceAtLeast(HALF_PAINT_WIDTH).coerceAtMost(width - HALF_PAINT_WIDTH)
        currentY =
            event.y.toInt().coerceAtLeast(HALF_PAINT_WIDTH).coerceAtMost(height - HALF_PAINT_WIDTH)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x.toInt().coerceAtLeast(HALF_PAINT_WIDTH)
                    .coerceAtMost(width - HALF_PAINT_WIDTH)
                downY = event.y.toInt().coerceAtLeast(HALF_PAINT_WIDTH)
                    .coerceAtMost(height - HALF_PAINT_WIDTH)
            }

            MotionEvent.ACTION_MOVE -> {
                invalidate()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                drawEdit(canvas)
                downX = 0
                downY = 0
                currentX = 0
                currentY = 0
                hasEditData = true
                invalidate()
            }
        }
        return true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        canvas = null
        sourceBitmap = null
        bgBitmap = null
        editBitmap = null
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\view\MainTitleView.kt =====

package com.mpdc4gsr.libunified.app.view

import android.content.Context
import android.util.AttributeSet

class MainTitleView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : TitleView(context, attrs) {
    override fun initView() {
        tvLeft = addTextView(context)
        tvRight1 = addTextView(context)
        tvRight2 = addTextView(context, 2f, 40f)
        tvRight3 = addTextView(context)
        tvTitle = addTextView(context)
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\view\MyTextView.kt =====

package com.mpdc4gsr.libunified.app.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatTextView
import com.mpdc4gsr.libunified.R

class MyTextView : AppCompatTextView {
    private var topHeight = 0
    private var bottomHeight = 0
    private var startHeight = 0
    private var endHeight = 0
    private var leftHeight = 0
    private var rightHeight = 0

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.MyTextView, defStyleAttr, 0)
        val drawableHeight = typedArray.getDimensionPixelSize(
            R.styleable.MyTextView_drawable_height,
            textSize.toInt()
        )
        topHeight =
            typedArray.getDimensionPixelSize(R.styleable.MyTextView_top_height, drawableHeight)
        bottomHeight =
            typedArray.getDimensionPixelSize(R.styleable.MyTextView_bottom_height, drawableHeight)
        startHeight =
            typedArray.getDimensionPixelSize(R.styleable.MyTextView_start_height, drawableHeight)
        endHeight =
            typedArray.getDimensionPixelSize(R.styleable.MyTextView_end_height, drawableHeight)
        leftHeight =
            typedArray.getDimensionPixelSize(R.styleable.MyTextView_left_height, drawableHeight)
        rightHeight =
            typedArray.getDimensionPixelSize(R.styleable.MyTextView_right_height, drawableHeight)
        typedArray.recycle()
        val drawables = compoundDrawables
        val relativeDrawables = compoundDrawablesRelative
        val left = drawables[0]
        val top = drawables[1]
        val right = drawables[2]
        val bottom = drawables[3]
        val start = relativeDrawables[0]
        val end = relativeDrawables[2]
        if (start != null || end != null) {
            setCompoundDrawablesRelative(start, top, end, bottom)
        } else {
            setCompoundDrawables(left, top, right, bottom)
        }
    }

    override fun setCompoundDrawables(
        left: Drawable?,
        top: Drawable?,
        right: Drawable?,
        bottom: Drawable?,
    ) {
        setDrawableBounds(top, topHeight)
        setDrawableBounds(bottom, bottomHeight)
        setDrawableBounds(left, leftHeight)
        setDrawableBounds(right, rightHeight)
        super.setCompoundDrawables(left, top, right, bottom)
    }

    override fun setCompoundDrawablesWithIntrinsicBounds(
        left: Drawable?,
        top: Drawable?,
        right: Drawable?,
        bottom: Drawable?,
    ) {
        setCompoundDrawables(left, top, right, bottom)
    }

    override fun setCompoundDrawablesRelative(
        start: Drawable?,
        top: Drawable?,
        end: Drawable?,
        bottom: Drawable?,
    ) {
        setDrawableBounds(top, topHeight)
        setDrawableBounds(bottom, bottomHeight)
        setDrawableBounds(start, startHeight)
        setDrawableBounds(end, endHeight)
        super.setCompoundDrawablesRelative(start, top, end, bottom)
    }

    override fun setCompoundDrawablesRelativeWithIntrinsicBounds(
        start: Drawable?,
        top: Drawable?,
        end: Drawable?,
        bottom: Drawable?,
    ) {
        setCompoundDrawablesRelative(start, top, end, bottom)
    }

    fun setDrawableHeightPx(pxHeight: Int) {
        topHeight = pxHeight
        bottomHeight = pxHeight
        startHeight = pxHeight
        endHeight = pxHeight
        leftHeight = pxHeight
        rightHeight = pxHeight
        invalidate()
    }

    fun setOnlyDrawableStart(drawable: Drawable?) {
        setCompoundDrawablesRelative(drawable, null, null, null)
    }

    fun setOnlyDrawableStart(
        @DrawableRes start: Int,
    ) {
        setCompoundDrawablesRelativeWithIntrinsicBounds(start, 0, 0, 0)
    }

    fun hasAnyDrawable(): Boolean {
        for (drawable in compoundDrawables) {
            if (drawable != null) {
                return true
            }
        }
        for (drawable in compoundDrawablesRelative) {
            if (drawable != null) {
                return true
            }
        }
        return false
    }

    private fun setDrawableBounds(
        drawable: Drawable?,
        height: Int,
    ) {
        if (drawable != null && height > 0) {
            drawable.setBounds(
                0,
                0,
                (height * 1f * drawable.intrinsicWidth / drawable.intrinsicHeight).toInt(),
                height
            )
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\view\TitleView.kt =====

package com.mpdc4gsr.libunified.app.view

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import com.mpdc4gsr.libunified.compat.dpToPx
import com.mpdc4gsr.libunified.R

open class TitleView : ViewGroup {
    companion object {
        private const val ICON_SIZE = 48f
    }

    private val isTitleCenter: Boolean
    private val actionBarSize: Int
    protected var tvLeft: MyTextView? = null
    protected var tvRight1: MyTextView? = null
    protected var tvRight2: MyTextView? = null
    protected var tvRight3: MyTextView? = null
    protected var tvTitle: MyTextView? = null

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
        val typedArray = context.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
        actionBarSize = typedArray.getDimensionPixelSize(0, 0)
        typedArray.recycle()
        initView()
        tvTitle?.setPadding(0)
        tvTitle?.isVisible = true
        tvTitle?.maxLines = 2
        tvTitle?.ellipsize = TextUtils.TruncateAt.END
        val a = context.obtainStyledAttributes(attrs, R.styleable.TitleView, defStyleAttr, 0)
        tvLeft?.text = a.getText(R.styleable.TitleView_leftText)
        tvLeft?.setOnlyDrawableStart(a.getDrawable(R.styleable.TitleView_leftDrawable))
        tvLeft?.isVisible = tvLeft?.text?.isNotEmpty() == true || tvLeft!!.hasAnyDrawable()
        val leftColor: ColorStateList? = a.getColorStateList(R.styleable.TitleView_leftTextColor)
        if (leftColor != null) {
            tvLeft?.setTextColor(leftColor)
        }
        if (a.getBoolean(R.styleable.TitleView_isInitLeft, true)) {
            tvLeft?.isVisible = true
            tvLeft?.setOnlyDrawableStart(R.drawable.ic_back_white_night_svg)
            tvLeft?.setOnClickListener {
                if (context is Activity) {
                    context.finish()
                }
            }
        }
        tvRight1?.text = a.getText(R.styleable.TitleView_rightText)
        tvRight1?.setOnlyDrawableStart(a.getDrawable(R.styleable.TitleView_rightDrawable))
        tvRight1?.isVisible = tvRight1?.text?.isNotEmpty() == true || tvRight1!!.hasAnyDrawable()
        val rightColor: ColorStateList? = a.getColorStateList(R.styleable.TitleView_rightTextColor)
        if (rightColor != null) {
            tvRight1?.setTextColor(rightColor)
        }
        tvRight2?.setOnlyDrawableStart(a.getDrawable(R.styleable.TitleView_right2Drawable))
        tvRight2?.isVisible = tvRight2!!.hasAnyDrawable()
        tvRight3?.setOnlyDrawableStart(a.getDrawable(R.styleable.TitleView_right3Drawable))
        tvRight3?.isVisible = tvRight3!!.hasAnyDrawable()
        isTitleCenter = a.getBoolean(R.styleable.TitleView_isTitleCenter, false)
        tvTitle?.text = a.getText(R.styleable.TitleView_titleText)
        tvTitle?.gravity =
            if (isTitleCenter) Gravity.CENTER else (Gravity.CENTER_VERTICAL or Gravity.START)
        a.recycle()
    }

    open fun initView() {
        tvLeft = addTextView(context)
        tvRight1 = addTextView(context)
        tvRight2 = addTextView(context)
        tvRight3 = addTextView(context)
        tvTitle = addTextView(context)
    }

    fun addTextView(
        context: Context,
        padding: Float,
        imgHeight: Float,
    ): MyTextView {
        val textView = MyTextView(context)
        textView.isVisible = false
        textView.gravity = Gravity.CENTER_VERTICAL
        textView.textSize = 16f
        textView.setTextColor(0xffffffff.toInt())
        textView.setPadding(padding.dpToPx(context).toInt())
        textView.setDrawableHeightPx(imgHeight.dpToPx(context).toInt())
        addView(textView)
        return textView
    }

    fun addTextView(context: Context): MyTextView {
        return addTextView(context, 12f, 24f)
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        var maxHeight = actionBarSize.coerceAtLeast(ICON_SIZE.dpToPx(context).toInt())
        for (i in 0 until childCount) {
            val childView: View = getChildAt(i)
            if (childView != tvTitle && childView.visibility != View.GONE) {
                measureChild(childView, widthMeasureSpec, heightMeasureSpec)
                maxHeight = maxHeight.coerceAtLeast(childView.measuredHeight)
            }
        }
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), maxHeight)
        for (i in 0 until childCount) {
            val childView: View = getChildAt(i)
            if (childView != tvTitle && childView.visibility != View.GONE) {
                val widthSpec =
                    MeasureSpec.makeMeasureSpec(childView.measuredWidth, MeasureSpec.EXACTLY)
                childView.measure(
                    widthSpec,
                    MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.EXACTLY)
                )
            }
        }
        if (isTitleCenter) {
            val leftSize: Int =
                if (tvLeft?.isVisible == true) tvLeft?.measuredWidth ?: 0 else ICON_SIZE.dpToPx(context).toInt()
            var rightSize = 0
            if (tvRight1?.isVisible == true) {
                rightSize += tvRight1?.measuredWidth ?: 0
            }
            if (tvRight2?.isVisible == true) {
                rightSize += tvRight2?.measuredWidth ?: 0
            }
            if (tvRight3?.isVisible == true) {
                rightSize += tvRight3?.measuredWidth ?: 0
            }
            if (rightSize == 0) {
                rightSize = ICON_SIZE.dpToPx(context).toInt()
            }
            val titleWidth = measuredWidth - leftSize.coerceAtLeast(rightSize) * 2
            val widthSpec =
                MeasureSpec.makeMeasureSpec(titleWidth.coerceAtLeast(0), MeasureSpec.EXACTLY)
            tvTitle?.measure(widthSpec, MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.EXACTLY))
        } else {
            var titleWidth = measuredWidth
            titleWidth -= if (tvLeft?.isVisible == true) tvLeft?.measuredWidth ?: 0 else ICON_SIZE.dpToPx(context)
                .toInt()
            titleWidth -= if (tvRight1?.isVisible == true) tvRight1?.measuredWidth ?: 0 else ICON_SIZE.dpToPx(context)
                .toInt()
            if (tvRight2?.isVisible == true) {
                titleWidth -= tvRight2?.measuredWidth ?: 0
            }
            if (tvRight3?.isVisible == true) {
                titleWidth -= tvRight3?.measuredWidth ?: 0
            }
            val widthSpec =
                MeasureSpec.makeMeasureSpec(titleWidth.coerceAtLeast(0), MeasureSpec.EXACTLY)
            tvTitle?.measure(widthSpec, MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.EXACTLY))
        }
    }

    override fun onLayout(
        changed: Boolean,
        l: Int,
        t: Int,
        r: Int,
        b: Int,
    ) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (!child.isVisible) {
                continue
            }
            val childWidth = child.measuredWidth
            when (child) {
                tvLeft -> child.layout(0, 0, childWidth, measuredHeight)
                tvRight1 -> child.layout(
                    measuredWidth - childWidth,
                    0,
                    measuredWidth,
                    measuredHeight
                )

                tvRight2 -> {
                    val right = measuredWidth - tvRight1!!.measuredWidth
                    child.layout(right - tvRight2!!.measuredWidth, 0, right, measuredHeight)
                }

                tvRight3 -> {
                    val right = measuredWidth - tvRight1!!.measuredWidth - tvRight2!!.measuredWidth
                    child.layout(right - tvRight3!!.measuredWidth, 0, right, measuredHeight)
                }

                tvTitle -> {
                    if (isTitleCenter) {
                        val margin = (measuredWidth - childWidth) / 2
                        child.layout(margin, 0, margin + childWidth, measuredHeight)
                    } else {
                        val left =
                            if (tvLeft?.isVisible == true) tvLeft?.measuredWidth ?: 0 else ICON_SIZE.dpToPx(context)
                                .toInt()
                        child.layout(left, 0, left + childWidth, measuredHeight)
                    }
                }
            }
        }
    }

    fun setTitleText(
        @StringRes resId: Int,
    ) {
        tvTitle?.setText(resId)
        tvTitle?.invalidate()
    }

    fun setTitleText(title: CharSequence?) {
        tvTitle?.text = title
        tvTitle?.invalidate()
    }

    var isLeftVisible: Boolean
        get() = tvLeft!!.isVisible
        set(value) {
            if (tvLeft?.isVisible != value) {
                tvLeft?.isVisible = value
                requestLayout()
            }
        }

    fun setLeftDrawable(
        @DrawableRes resId: Int,
    ) {
        tvLeft?.isVisible = resId != 0 || tvLeft?.text?.isNotEmpty() == true
        tvLeft?.setCompoundDrawablesWithIntrinsicBounds(resId, 0, 0, 0)
        requestLayout()
    }

    fun setLeftText(
        @StringRes resId: Int,
    ) {
        tvLeft?.setText(resId)
        tvLeft?.isVisible = true
        requestLayout()
    }

    fun setLeftText(text: CharSequence?) {
        tvLeft?.text = text
        tvLeft?.isVisible = text?.isNotEmpty() == true || tvLeft!!.hasAnyDrawable()
        requestLayout()
    }

    fun setLeftClickListener(leftClickListener: OnClickListener?) {
        tvLeft?.setOnClickListener(leftClickListener)
    }

    var isRightVisible: Boolean
        get() = tvRight1!!.isVisible
        set(value) {
            if (tvRight1?.isVisible != value) {
                tvRight1?.isVisible = value
                requestLayout()
            }
        }

    fun setRightDrawable(
        @DrawableRes resId: Int,
    ) {
        tvRight1?.isVisible = resId != 0 || tvRight1?.text?.isNotEmpty() == true
        tvRight1?.setCompoundDrawablesWithIntrinsicBounds(resId, 0, 0, 0)
        requestLayout()
    }

    fun setRightText(
        @StringRes resId: Int,
    ) {
        tvRight1?.setText(resId)
        tvRight1?.isVisible = true
        requestLayout()
    }

    fun setRightText(text: CharSequence?) {
        tvRight1?.text = text
        tvRight1?.isVisible = text?.isNotEmpty() == true || tvRight1!!.hasAnyDrawable()
        requestLayout()
    }

    fun setRightClickListener(rightClickListener: OnClickListener?) {
        tvRight1?.setOnClickListener(rightClickListener)
    }

    fun setRight2Drawable(
        @DrawableRes resId: Int,
    ) {
        tvRight2?.isVisible = resId != 0 || tvRight2?.text?.isNotEmpty() == true
        tvRight2?.setCompoundDrawablesWithIntrinsicBounds(resId, 0, 0, 0)
        requestLayout()
    }

    fun setRight2ClickListener(right2ClickListener: OnClickListener?) {
        tvRight2?.setOnClickListener(right2ClickListener)
    }

    fun setRight3Drawable(
        @DrawableRes resId: Int,
    ) {
        tvRight3?.isVisible = resId != 0 || tvRight3?.text?.isNotEmpty() == true
        tvRight3?.setCompoundDrawablesWithIntrinsicBounds(resId, 0, 0, 0)
        requestLayout()
    }

    fun setRight3ClickListener(right3ClickListener: OnClickListener?) {
        tvRight3?.setOnClickListener(right3ClickListener)
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\viewmodel\FirmwareViewModel.kt =====

package com.mpdc4gsr.libunified.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.mpdc4gsr.libunified.compat.ContextProvider
import com.elvishew.xlog.XLog
import com.google.gson.Gson
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.app.config.FileConfig
import com.mpdc4gsr.libunified.app.lms.LMS
import com.mpdc4gsr.libunified.app.lms.UrlConstants
import com.mpdc4gsr.libunified.app.lms.bean.CommonBean
import com.mpdc4gsr.libunified.app.lms.network.HttpProxy
import com.mpdc4gsr.libunified.app.lms.network.IResponseCallback
import com.mpdc4gsr.libunified.app.lms.network.ResponseBean
import com.mpdc4gsr.libunified.app.lms.utils.DateUtils
import com.mpdc4gsr.libunified.app.lms.utils.LanguageUtils
import com.mpdc4gsr.libunified.app.lms.xutils.http.RequestParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.CountDownLatch

class FirmwareViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TS004_SOFT_CODE = "TS004_FirmwareSW_Scope"
        private const val TC007_SOFT_CODE = "TC007_FirmwareSW_Wireless"
        private const val TS004_FIRMWARE_VERSION = "V1.70"
        private const val TS004_FIRMWARE_NAME = "TS004V1.70.zip"
        private const val TC007_FIRMWARE_VERSION = "V4.06"
        private const val TC007_FIRMWARE_NAME = "TC007V4.06.zip"
        private const val USE_DEBUG_SN = false
        private const val TS004_DEBUG_SN = "1D003655A10016"
        private const val TS004_DEBUG_RANDOM_NUM = "8D2N01"
        private const val TC007_DEBUG_SN = "1D004714E10002"
        private const val TC007_DEBUG_RANDOM_NUM = "EN6L6Q"
    }

    @Volatile
    private var isRequest = false
    val firmwareDataLD: MutableLiveData<FirmwareData?> = MutableLiveData()
    val failLD: MutableLiveData<Boolean> = MutableLiveData()

    data class FirmwareData(
        val version: String,
        val updateStr: String,
        val downUrl: String,
        val size: Long,
    )

    fun queryFirmware(isTS004: Boolean) {
        if (isRequest) {
            return
        }
    }

    private fun getInfoFromAssets(
        isTS004: Boolean,
        firmware: String,
    ) {
        val apkVersionStr = if (isTS004) TS004_FIRMWARE_VERSION else TC007_FIRMWARE_VERSION
        val apkFirmwareName = if (isTS004) TS004_FIRMWARE_NAME else TC007_FIRMWARE_NAME
        val newVersion: Double = getVersionFromStr(apkVersionStr)
        val currentVersion: Double = getVersionFromStr(firmware)
        XLog.d("${if (isTS004) "TS004" else "TC007"} [ph][ph][ph][ph] - current[ph][ph]ï¼š$currentVersion apk[ph][ph][ph][ph]ï¼š$newVersion")
        if (newVersion <= currentVersion) {
            firmwareDataLD.postValue(null)
            isRequest = false
            return
        }
        val firmwareFile = FileConfig.getFirmwareFile(apkFirmwareName)
        try {
            val application: Application = getApplication()
            val inputStream = application.assets.open(apkFirmwareName)
            val outputStream: OutputStream = FileOutputStream(firmwareFile)
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
            inputStream.close()
            outputStream.close()
        } catch (e: IOException) {
            XLog.e("${if (isTS004) "TS004" else "TC007"} [ph][ph][ph][ph] - [ph][ph][ph][ph][ph][ph][ph][ph][ph][ph][ph]! ${e.message}")
            firmwareFile.delete()
            firmwareDataLD.postValue(null)
            isRequest = false
            return
        }
        val tipsStr = getApplication<Application>().getString(R.string.fireware_update_tips)
        firmwareDataLD.postValue(
            FirmwareData(
                apkVersionStr,
                tipsStr,
                apkFirmwareName,
                firmwareFile.length()
            )
        )
        isRequest = false
    }

    private suspend fun getInfoFromNetwork(
        isTS004: Boolean,
        sn: String,
        randomNum: String,
        firmware: String,
    ) {
        val bindCode = bindDevice(sn, randomNum)
        if (bindCode != LMS.SUCCESS.toInt() && bindCode != 15109) {
            XLog.w("${if (isTS004) "TS004" else "TC007"} [ph][ph][ph][ph] - [ph][ph][ph][ph][ph][ph]! sn: $sn")
            failLD.postValue(bindCode == 15162)
            isRequest = false
            return
        }
        val packageData: PackageData? =
            querySoftPackage(sn, if (isTS004) TS004_SOFT_CODE else TC007_SOFT_CODE)
        if (packageData == null) {
            XLog.w("${if (isTS004) "TS004" else "TC007"} [ph][ph][ph][ph] - [ph][ph][ph][ph][ph][ph][ph][ph][ph][ph][ph]!")
            failLD.postValue(false)
            isRequest = false
            return
        }
        val record: PackageData.Record? = packageData.getFirstRecord()
        val newVersionStr: String? = record?.maxUpdateVersion
        if (record == null || newVersionStr == null) {
            XLog.d("${if (isTS004) "TS004" else "TC007"} [ph][ph][ph][ph] - [ph][ph][ph][ph][ph][ph][ph]ï¼Œ[ph]current[ph][ph][ph][ph][ph][ph]")
            firmwareDataLD.postValue(null)
            isRequest = false
            return
        }
        val newVersion: Double = getVersionFromStr(newVersionStr)
        val currentVersion: Double = getVersionFromStr(firmware)
        XLog.d("${if (isTS004) "TS004" else "TC007"} [ph][ph][ph][ph] - current[ph][ph]ï¼š$currentVersion [ph][ph][ph][ph][ph]ï¼š$newVersion")
        if (newVersion <= currentVersion) {
            firmwareDataLD.postValue(null)
            isRequest = false
            return
        }
        val downloadData = queryDownloadUrl(sn, record.maxUpdateVersionSoftId)
        if (downloadData?.responseCode == LMS.SUCCESS.toInt()) {
            firmwareDataLD.postValue(
                FirmwareData(
                    newVersionStr,
                    record.getUpdateStr(),
                    downloadData.downUrl ?: "",
                    downloadData.size ?: 0,
                ),
            )
        } else {
            XLog.w("${if (isTS004) "TS004" else "TC007"} [ph][ph][ph][ph] - [ph][ph][ph][ph][ph][ph][ph][ph][ph][ph][ph]!")
            failLD.postValue(downloadData?.responseCode == 60312)
        }
        isRequest = false
    }

    private suspend fun bindDevice(
        sn: String,
        randomNum: String,
    ): Int {
        return withContext(Dispatchers.IO) {
            var code = LMS.SUCCESS.toInt()
            val countDownLatch = CountDownLatch(1)
            LMS.getInstance().bindDevice(sn, randomNum, "", "", object : IResponseCallback {
                override fun onResponse(response: String) {
                    try {
                        val responseBean = Gson().fromJson(response, ResponseBean::class.java)
                        code = responseBean.code.toInt()
                    } catch (e: Exception) {
                        code = 9999 // Error code
                    }
                    countDownLatch.countDown()
                }
            })
            countDownLatch.await()
            return@withContext code
        }
    }

    private suspend fun querySoftPackage(
        sn: String,
        softCode: String,
    ): PackageData? =
        withContext(Dispatchers.IO) {
            var packageData: PackageData? = null
            val countDownLatch = CountDownLatch(1)
            val url = UrlConstants.BASE_URL + "api/v1/user/deviceSoftOut/page"
            val params = RequestParams()
            params.addBodyParameter("sn", sn)
            params.addBodyParameter("softCode", softCode)
            params.addBodyParameter(
                "downloadLanguageId",
                LanguageUtils.getLanguageId(ContextProvider.getContext())
            )
            params.addBodyParameter("downloadPlatformId", 2)
            params.addBodyParameter(
                "queryTime",
                DateUtils.formatDate(System.currentTimeMillis()),
            )
            HttpProxy.getInstance().post(
                url,
                params,
                object : IResponseCallback {
                    override fun onResponse(response: String?) {
                        try {
                            val commonBean: CommonBean =
                                Gson().fromJson(response, CommonBean::class.java)
                            packageData = Gson().fromJson(commonBean.data, PackageData::class.java)
                        } catch (_: Exception) {
                        }
                        countDownLatch.countDown()
                    }

                    override fun onFail(exception: Exception?) {
                        countDownLatch.countDown()
                    }
                },
            )
            countDownLatch.await()
            return@withContext packageData
        }

    private suspend fun queryDownloadUrl(
        sn: String,
        businessId: Int,
    ): DownloadData? =
        withContext(Dispatchers.IO) {
            var result: DownloadData? = null
            val countDownLatch = CountDownLatch(1)
            val url = UrlConstants.BASE_URL + "api/v1/user/deviceSoftOut/getFileUrl"
            val params = RequestParams()
            params.addBodyParameter("sn", sn)
            params.addBodyParameter("businessId", businessId)
            params.addBodyParameter("businessType", 20)
            params.addBodyParameter("productType", 20)
            params.addBodyParameter("isCheckPoint", 0)
            HttpProxy.getInstance().post(
                url,
                params,
                object : IResponseCallback {
                    override fun onResponse(response: String?) {
                        try {
                            val commonBean: CommonBean =
                                Gson().fromJson(response, CommonBean::class.java)
                            if (commonBean.code == LMS.SUCCESS) {
                                result = Gson().fromJson(commonBean.data, DownloadData::class.java)
                                result?.responseCode = commonBean.code.toInt()
                            } else {
                                result = DownloadData("", 0, commonBean.code.toInt())
                            }
                        } catch (_: Exception) {
                        }
                        countDownLatch.countDown()
                    }

                    override fun onFail(exception: Exception?) {
                        countDownLatch.countDown()
                    }
                },
            )
            countDownLatch.await()
            return@withContext result
        }

    private fun getVersionFromStr(versionStr: String): Double =
        try {
            if (versionStr[0] == 'V') {
                versionStr.substring(1, versionStr.length).toDouble()
            } else {
                versionStr.toDouble()
            }
        } catch (e: NumberFormatException) {
            0.0
        }

    private class PackageData {
        var records: List<Record>? = null
        fun getFirstRecord(): Record? = if (records?.isNotEmpty() == true) records?.get(0) else null
        data class Record(
            var maxUpdateVersion: String?,
            var maxUpdateVersionSoftId: Int,
            var maxVersionDetailResVO: MaxVersionDetailResVO?,
        ) {
            fun getUpdateStr(): String {
                val otherExplain: List<OtherExplain>? = maxVersionDetailResVO?.otherExplain
                if (otherExplain != null) {
                    for (data in otherExplain) {
                        if (data.valueType == 3) {
                            return data.textDescription ?: ""
                        }
                    }
                }
                return ""
            }
        }

        data class MaxVersionDetailResVO(
            val otherExplain: List<OtherExplain>?,
        )

        data class OtherExplain(
            val valueType: Int,
            val textDescription: String?,
        )
    }

    private data class DownloadData(
        val downUrl: String?,
        val size: Long?,
        var responseCode: Int,
    )
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\viewmodel\ModernFirmwareViewModel.kt =====

package com.mpdc4gsr.libunified.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.repository.BaseRepository
import com.mpdc4gsr.libunified.app.repository.FirmwareRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class ModernFirmwareViewModel(
    application: Application,
    private val firmwareRepository: FirmwareRepository = FirmwareRepository(application)
) : AndroidViewModel(application) {
    // State management
    private val _firmwareState = MutableStateFlow<FirmwareState>(FirmwareState.Idle)
    val firmwareState: StateFlow<FirmwareState> = _firmwareState.asStateFlow()
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    // One-time events
    private val _events = MutableSharedFlow<FirmwareEvent>()
    val events: SharedFlow<FirmwareEvent> = _events.asSharedFlow()

    // Sealed classes for type-safe state management
    sealed class FirmwareState {
        object Idle : FirmwareState()
        object Checking : FirmwareState()
        data class UpdateAvailable(val info: FirmwareRepository.FirmwareInfo) : FirmwareState()
        object UpToDate : FirmwareState()
        data class Error(val message: String, val isBindError: Boolean = false) : FirmwareState()
    }

    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(val progress: Float) : DownloadState()
        data class Completed(val file: File) : DownloadState()
        data class Error(val message: String) : DownloadState()
    }

    sealed class FirmwareEvent {
        data class ShowUpdateDialog(val info: FirmwareRepository.FirmwareInfo) : FirmwareEvent()
        data class ShowError(val message: String) : FirmwareEvent()
        data class ShowSuccess(val message: String) : FirmwareEvent()
        object UpdateCompleted : FirmwareEvent()
    }

    // Public API for checking firmware updates
    fun checkFirmwareUpdate(isTC007: Boolean, deviceInfo: FirmwareRepository.DeviceInfo) {
        viewModelScope.launch {
            _firmwareState.value = FirmwareState.Checking
            firmwareRepository.checkFirmwareUpdate(isTC007, deviceInfo).collect { result ->
                when (result) {
                    is BaseRepository.Result.Loading -> {
                        _firmwareState.value = FirmwareState.Checking
                    }

                    is BaseRepository.Result.Success -> {
                        val firmwareInfo = result.data
                        if (firmwareInfo != null && firmwareInfo.isUpdateAvailable) {
                            _firmwareState.value = FirmwareState.UpdateAvailable(firmwareInfo)
                            _events.emit(FirmwareEvent.ShowUpdateDialog(firmwareInfo))
                        } else {
                            _firmwareState.value = FirmwareState.UpToDate
                            _events.emit(FirmwareEvent.ShowSuccess("Firmware is up to date"))
                        }
                    }

                    is BaseRepository.Result.Error -> {
                        val errorMessage = result.exception.message ?: "Unknown error occurred"
                        _firmwareState.value = FirmwareState.Error(errorMessage)
                        _events.emit(FirmwareEvent.ShowError(errorMessage))
                    }
                }
            }
        }
    }

    // Download firmware update
    fun downloadFirmwareUpdate(firmwareInfo: FirmwareRepository.FirmwareInfo, outputDir: File) {
        viewModelScope.launch {
            _downloadState.value = DownloadState.Downloading(0f)
            when (val result = firmwareRepository.downloadFirmware(firmwareInfo, outputDir)) {
                is BaseRepository.Result.Success -> {
                    _downloadState.value = DownloadState.Completed(result.data)
                    _events.emit(FirmwareEvent.ShowSuccess("Firmware downloaded successfully"))
                    _events.emit(FirmwareEvent.UpdateCompleted)
                }

                is BaseRepository.Result.Error -> {
                    val errorMessage = result.exception.message ?: "Download failed"
                    _downloadState.value = DownloadState.Error(errorMessage)
                    _events.emit(FirmwareEvent.ShowError(errorMessage))
                }

                else -> {
                    // Handle loading state if needed
                }
            }
        }
    }

    // Get firmware from assets as fallback
    fun getFirmwareFromAssets(isTC007: Boolean) {
        viewModelScope.launch {
            when (val result = firmwareRepository.getFirmwareFromAssets(isTC007)) {
                is BaseRepository.Result.Success -> {
                    _firmwareState.value = FirmwareState.UpdateAvailable(result.data)
                    _events.emit(FirmwareEvent.ShowUpdateDialog(result.data))
                }

                is BaseRepository.Result.Error -> {
                    val errorMessage = result.exception.message ?: "Failed to load local firmware"
                    _firmwareState.value = FirmwareState.Error(errorMessage)
                    _events.emit(FirmwareEvent.ShowError(errorMessage))
                }

                else -> {
                    // Handle loading state if needed
                }
            }
        }
    }

    // Error handling
    fun onBindError() {
        _firmwareState.value = FirmwareState.Error("Service binding failed", true)
        viewModelScope.launch {
            _events.emit(FirmwareEvent.ShowError("Firmware service is not available"))
        }
    }

    // Clear error state
    fun clearError() {
        if (_firmwareState.value is FirmwareState.Error) {
            _firmwareState.value = FirmwareState.Idle
        }
        if (_downloadState.value is DownloadState.Error) {
            _downloadState.value = DownloadState.Idle
        }
    }

    // Reset states
    fun reset() {
        _firmwareState.value = FirmwareState.Idle
        _downloadState.value = DownloadState.Idle
    }

    // Compatibility with legacy FirmwareData
    data class FirmwareData(
        val version: String,
        val updateStr: String,
        val downUrl: String,
        val size: Long
    ) {
        companion object {
            fun fromFirmwareInfo(info: FirmwareRepository.FirmwareInfo): FirmwareData {
                return FirmwareData(
                    version = info.version,
                    updateStr = info.updateDescription,
                    downUrl = info.downloadUrl,
                    size = info.size
                )
            }
        }
    }

    // Convert to legacy format for compatibility
    fun getFirmwareDataLegacy(): FirmwareData? {
        val state = _firmwareState.value
        return if (state is FirmwareState.UpdateAvailable) {
            FirmwareData.fromFirmwareInfo(state.info)
        } else {
            null
        }
    }
}