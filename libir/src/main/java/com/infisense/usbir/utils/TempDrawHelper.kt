package com.infisense.usbir.utils

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.annotation.ColorInt
import com.blankj.utilcode.util.SizeUtils
import kotlin.math.max
import kotlin.math.min

/**
 * TC007、2D编辑、插件的 point/line/areatemperature图层有 View、SurfaceView 两种implementation，
 * 先用这个工具class抽取相同 draw 逻辑，后续考虑Optimize。
 *
 * Created by LCG on 2024/12/6.
 */
/**
 * TempDrawHelper manages camera operations and image capture functionality.
 *
 * @author IRCamera Development Team
 * @since 1.0
 */
class TempDrawHelper {
    companion object {
        /**
         * point是一个十字架，该值为十字架的长度，单位 px.
         */
        private val POINT_SIZE: Int = SizeUtils.dp2px(16f)

        /**
         * point、line、area、全图 maximum温及minimum温center实心圆的半径，单位 px.
         */
        private val CIRCLE_RADIUS: Int = SizeUtils.dp2px(3f)

        /**
         * temperature值text，与实心圆圆心的偏移量，防止text与实心圆重叠，X轴为该值，Y轴为该值/2，单位 px.
         */
        private val TEMP_TEXT_OFFSET = SizeUtils.dp2px(6f)

        /**
         * 修正指定十字架的 View 坐标值，确保十字架不会超出 View 界外.
         */
        fun Float.correctPoint(max: Int): Int =
            this.toInt()
                .coerceAtLeast(POINT_SIZE / 2)
                .coerceAtMost(max - POINT_SIZE / 2)

        /**
         * 修正line、area、maximum温point、minimum温point的 View 坐标值，确保实心圆不会超出 View 界外。
         */
        fun Float.correct(max: Int): Int =
            this.toInt()
                .coerceAtLeast(CIRCLE_RADIUS)
                .coerceAtMost(max - CIRCLE_RADIUS)

        /**
         * Get/Retrieve可保证实心圆不会超出 View 界外的 Rect.
         */
        fun getRect(
            width: Int,
            height: Int,
        ): Rect = Rect(CIRCLE_RADIUS, CIRCLE_RADIUS, width - CIRCLE_RADIUS, height - CIRCLE_RADIUS)
    }

    /**
     * temperature值text、趋势图 AB 两个字母、point/line/areaname text大小，单位 px.
     */
    var textSize: Int
        get() = textPaint.textSize.toInt()
        set(value) {
            textPaint.textSize = value.toFloat()
        }

    /**
     * temperature值text、趋势图 AB 两个字母、point/line/areaname textcolor值.
     */
    var textColor: Int
        @ColorInt get() = textPaint.color
        set(
            @ColorInt value
        ) {
            textPaint.color = value
        }

    /**
     * 绘制 point、line、area、趋势图直line Paint，白色.
     * 描边宽度 1dp.
     */
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * point、line、area、全图 低温point实心圆 Paint，蓝色.
     */
    private val bluePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * point、line、area、全图 高温point实心圆 Paint，红色.
     */
    private val redPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * 高温temperaturetext、低温temperaturetext、趋势图 AB 两个字母、point/line/areaname Paint，
     * color默认白色，大小默认 14sp，可由textcolor、大小settings更改.
     */
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        linePaint.strokeWidth = SizeUtils.dp2px(1f).toFloat()
        linePaint.color = Color.WHITE

        bluePaint.color = Color.BLUE

        redPaint.color = Color.RED

        textPaint.textSize = SizeUtils.sp2px(14f).toFloat()
        textPaint.color = Color.WHITE
    }

    // ******************************************** Draw ********************************************

    /**
     * 在 (x,y) 画一个十字.
     *
     * 注意，不对 x、y 进行processing，传进来是哪就在哪绘制。
     */
    fun drawPoint(
        canvas: Canvas,
        x: Int,
        y: Int,
    ) {
        val left: Float = x - POINT_SIZE / 2f
        val top: Float = y - POINT_SIZE / 2f
        val right: Float = x + POINT_SIZE / 2f
        val bottom: Float = y + POINT_SIZE / 2f
        canvas.drawLine(left, y.toFloat(), right, y.toFloat(), linePaint) 
        canvas.drawLine(x.toFloat(), top, x.toFloat(), bottom, linePaint) 
    }

    /**
     * connection (startX, startY)、(stopX, stopY) 两point绘制一条line段.
     *
     * 注意，不对 坐标parameter 进行processing，传进来是哪就在哪绘制。
     */
    fun drawLine(
        canvas: Canvas,
        startX: Int,
        startY: Int,
        stopX: Int,
        stopY: Int,
    ) {
        canvas.drawLine(startX.toFloat(), startY.toFloat(), stopX.toFloat(), stopY.toFloat(), linePaint)
    }

    /**
     * 按指定range绘制一个矩形.
     *
     * 注意，不对 坐标parameter 进行processing，传进来是哪就在哪绘制。
     */
    fun drawRect(
        canvas: Canvas,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) {
        val leftF: Float = left.toFloat()
        val topF: Float = top.toFloat()
        val rightF: Float = right.toFloat()
        val bottomF: Float = bottom.toFloat()
        val points =
            floatArrayOf(leftF, topF, rightF, topF, rightF, topF, rightF, bottomF, rightF, bottomF, leftF, bottomF, leftF, bottomF, leftF, topF)
        canvas.drawLines(points, linePaint)
    }

    /**
     * 在 (x,y) 画一个实心圆。
     *
     * 注意，不对 x、y 进行processing，传进来是哪就在哪绘制。
     * @param isMax true-maximum温红色 false-minimum温蓝色
     */
    fun drawCircle(
        canvas: Canvas,
        x: Int,
        y: Int,
        isMax: Boolean,
    ) {
        canvas.drawCircle(x.toFloat(), y.toFloat(), CIRCLE_RADIUS.toFloat(), if (isMax) redPaint else bluePaint)
    }

    /**
     * 指定的 (x,y) 坐标为实心圆圆心，以该实心圆为基准绘制指定text。
     * 若空间允许则放置在实心圆圆心右上方，否则根据实际情况放置在下方、左方或左下方.
     *
     * 注意，不对 x、y 进行processing，传进来是哪就在哪绘制。
     * @param x 实心圆圆心的 View 尺寸坐标
     */
    fun drawTempText(
        canvas: Canvas,
        text: String,
        width: Int,
        x: Int,
        y: Int,
    ) {
        var textX: Float = (x + TEMP_TEXT_OFFSET).toFloat()
        var textY: Float = (y - TEMP_TEXT_OFFSET).toFloat()

        val textWidth: Float = textPaint.measureText(text)
        if (x > width - textWidth - TEMP_TEXT_OFFSET) { // 超出右边界，那就挪到左边
            textX = x - TEMP_TEXT_OFFSET - textWidth
        }

        val textFontTop: Float = -textPaint.getFontMetrics().top
        if (y < textFontTop + TEMP_TEXT_OFFSET / 2) { // 超出上边界，那就挪到下area
            textY = y + TEMP_TEXT_OFFSET / 2 + textFontTop
        }

        canvas.drawText(text, textX, textY, textPaint)
    }

    /**
     * 指定的 (startX, startY)、(stopX, stopY) 坐标为line段，
     * 以该line段为基准绘制趋势图的 "A"、"B" text。
     *
     * 注意，不对 坐标parameter 进行processing，传进来是哪就在哪绘制。
     */
    fun drawTrendText(
        canvas: Canvas,
        width: Int,
        height: Int,
        startX: Int,
        startY: Int,
        stopX: Int,
        stopY: Int,
    ) {
        val fontMetrics: Paint.FontMetrics = textPaint.getFontMetrics()
        val textWidth: Float = textPaint.measureText("A")
        val textHeight: Float = -fontMetrics.top

        val minX: Int = min(startX, stopX)
        val maxX: Int = max(startX, stopX)
        val leftX: Float = (minX - textWidth).coerceAtLeast(0f)
        val rightX: Float = maxX.toFloat().coerceAtMost(width - textWidth)

        val minY: Int = min(startY, stopY)
        val maxY: Int = max(startY, stopY)
        val topY: Float = (minY - (-fontMetrics.top + fontMetrics.ascent)).coerceAtLeast(textHeight)
        val bottomY: Float = (maxY + textHeight).coerceAtMost(height.toFloat())

        val k: Float = (startY - stopY).toFloat() / (startX - stopX)
        canvas.drawText("A", leftX, if (k >= 0) topY else bottomY, textPaint)
        canvas.drawText("B", rightX, if (k >= 0) bottomY else topY, textPaint)
    }

    /**
     * 指定的 (x,y) 坐标为实心圆圆心，以该实心圆为基准绘制指定pointnametext。
     * 若空间允许则放置在实心圆圆心正下方，否则放正上方.
     *
     * 注意，不对 x、y 进行processing，传进来是哪就在哪绘制。
     * @param x 实心圆圆心的 View 尺寸坐标
     */
    fun drawPointName(
        canvas: Canvas,
        name: String,
        width: Int,
        height: Int,
        x: Int,
        y: Int,
    ) {
        val textWidth: Float = textPaint.measureText(name)
        val textHeight: Float = -textPaint.getFontMetrics().top

        var textX = x - textWidth / 2
        var textY = y + POINT_SIZE / 2 + textHeight

        if (textX < 0) { 
            textX = 0f
        }
        if (textX + textWidth > width) { 
            textX = width - textWidth
        }
        if (textY > height) { // 若名字放point下area要超出range时，放point上area
            textY = y - POINT_SIZE / 2 - textPaint.fontMetrics.bottom
        }
        canvas.drawText(name, textX, textY, textPaint)
    }

    /**
     * 指定的 line段或矩形 坐标为range，
     * 以该range为基准绘制指定linenametext，放置于rangecenter。
     *
     * 注意，不对 x、y 进行processing，传进来是哪就在哪绘制。
     */
    fun drawPointRectName(
        canvas: Canvas,
        name: String,
        width: Int,
        height: Int,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) {
        val fontMetrics: Paint.FontMetrics = textPaint.getFontMetrics()
        val textWidth: Float = textPaint.measureText(name)
        val textHeight: Float = -fontMetrics.top
        val centerX: Int = left + (right - left) / 2
        val centerY: Int = top + (bottom - top) / 2
        val offset: Float = (-fontMetrics.ascent + fontMetrics.descent) / 2 - fontMetrics.descent

        var textX: Float = centerX - textWidth / 2
        var textY: Float = centerY + offset

        if (textX < 0) { 
            textX = 0f
        }
        if (textX + textWidth > width) { 
            textX = width - textWidth
        }
        if (textY < textHeight) { 
            textY = textHeight
        }
        if (textY > height) { 
            textY = height.toFloat()
        }
        canvas.drawText(name, textX, textY, textPaint)
    }

    // ******************************************** Touch ********************************************
}
