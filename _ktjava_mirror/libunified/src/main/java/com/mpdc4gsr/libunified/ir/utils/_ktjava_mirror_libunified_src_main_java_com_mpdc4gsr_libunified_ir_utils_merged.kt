// Merged ALL .kt and .java files from the '_ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\ir\utils' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:45


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\ir\utils\libunified_src_main_java_com_mpdc4gsr_libunified_ir_utils_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\ir\utils' subtree
// Files: 6; Generated 2025-10-07 23:07:50


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ir\utils\IRImageHelp.kt =====

package com.mpdc4gsr.libunified.ir.utils

import android.util.Log
import com.mpdc4gsr.libunified.app.bean.AlarmBean
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.IOException

class IRImageHelp {
    @Volatile
    private var colorList: IntArray? = null

    @Volatile
    private var places: FloatArray? = null
    private var isUseGray = true
    private var customMaxTemp = 0f
    private var customMinTemp = 0f
    private var maxRGB = IntArray(3)
    private var minRGB = IntArray(3)
    fun getColorList(): IntArray? {
        return colorList
    }

    fun setColorList(
        colorList: IntArray?,
        places: FloatArray?,
        isUseGray: Boolean,
        customMaxTemp: Float,
        customMinTemp: Float,
    ) {
        if (colorList == null) {
            this.isUseGray = true
        } else {
            this.isUseGray = isUseGray
        }
        this.colorList = colorList
        this.places = places
        if (colorList != null) {
            this.customMaxTemp = customMaxTemp
            this.customMinTemp = customMinTemp
            val maxColor = colorList[colorList.size - 1]
            val minColor = colorList[0]
            this.maxRGB[0] = maxColor shr 16 and 0xFF
            this.maxRGB[1] = maxColor shr 8 and 0xFF
            this.maxRGB[2] = maxColor and 0xFF
            this.minRGB[0] = minColor shr 16 and 0xFF
            this.minRGB[1] = minColor shr 8 and 0xFF
            this.minRGB[2] = minColor and 0xFF
        }
    }

    fun customPseudoColor(
        imageDst: ByteArray,
        temperatureSrc: ByteArray,
        imageWidth: Int,
        imageHeight: Int,
    ): ByteArray {
        try {
            if (colorList != null) {
                var j = 0
                val imageDstLength: Int = imageWidth * imageHeight * 4
                var index = 0
                while (index < imageDstLength) {
                    var temperature0: Float =
                        (
                                (temperatureSrc.get(j).toInt() and 0xff) + (
                                        temperatureSrc.get(j + 1)
                                            .toInt() and 0xff
                                        ) * 256
                                ).toFloat()
                    temperature0 = (temperature0 / 64 - 273.15).toFloat()
                    if (temperature0 >= customMinTemp && temperature0 <= customMaxTemp) {
                        val intensity =
                            ((temperature0 - customMinTemp) / (customMaxTemp - customMinTemp) * 255).toInt()
                                .coerceIn(0, 255)
                        imageDst[index] = intensity.toByte()
                        imageDst[index + 1] = intensity.toByte()
                        imageDst[index + 2] = intensity.toByte()
                    } else if (temperature0 > customMaxTemp) {
                        if (isUseGray) {
                        } else {
                            imageDst[index] = maxRGB[0].toByte()
                            imageDst[index + 1] = maxRGB[1].toByte()
                            imageDst[index + 2] = maxRGB[2].toByte()
                        }
                    } else if (temperature0 < customMinTemp) {
                        if (isUseGray) {
                        } else {
                            imageDst[index] = minRGB[0].toByte()
                            imageDst[index + 1] = minRGB[1].toByte()
                            imageDst[index + 2] = minRGB[2].toByte()
                        }
                    }
                    imageDst[index + 3] = 255.toByte()
                    index += 4
                    j += 2
                }
            }
        } catch (exception: Exception) {
            Log.e("[ph][ph][ph][ph]", exception.message!!)
        } finally {
            return imageDst
        }
    }

    fun setPseudoColorMaxMin(
        imageDst: ByteArray?,
        temperatureSrc: ByteArray?,
        max: Float,
        min: Float,
        imageWidth: Int,
        imageHeight: Int,
    ) {
        if (temperatureSrc != null && (max != Float.MAX_VALUE || min != Float.MIN_VALUE)) {
            var j = 0
            val imageDstLength: Int = imageWidth * imageHeight * 4
            val biaochiMax: Float = max
            val biaochiMin: Float = min
            val startTimeAll = System.currentTimeMillis()
            var index = 0
            while (index < imageDstLength) {
                var temperature0: Float =
                    (
                            (temperatureSrc[j].toInt() and 0xff) + (
                                    temperatureSrc[j + 1]
                                        .toInt() and 0xff
                                    ) * 256
                            ).toFloat()
                temperature0 = (temperature0 / 64 - 273.15).toFloat()
                val y0: Int = imageDst!![j].toInt() and 0xff
                if (temperature0 < biaochiMin || temperature0 > biaochiMax) {
                    val r: Int = imageDst!![index].toInt() and 0xff
                    val g: Int = imageDst!![index + 1].toInt() and 0xff
                    val b: Int = imageDst!![index + 2].toInt() and 0xff
                    val grey = (r * 0.3f + g * 0.59f + b * 0.11f).toInt()
                    imageDst!![index] = grey.toByte()
                    imageDst!![index + 1] = grey.toByte()
                    imageDst!![index + 2] = grey.toByte()
                }
                imageDst!![index + 3] = 255.toByte()
                index += 4
                j += 2
            }
        }
    }

    fun contourDetection(
        alarmBean: AlarmBean?,
        imageDst: ByteArray?,
        temperatureSrc: ByteArray?,
        imageWidth: Int,
        imageHeight: Int,
    ): ByteArray? {
        if (alarmBean != null && imageDst != null && temperatureSrc != null) {
            if (alarmBean.isMarkOpen && (
                        (alarmBean.highTemp != Float.MAX_VALUE && alarmBean.isHighOpen) ||
                                (alarmBean.isLowOpen && alarmBean.lowTemp != Float.MIN_VALUE)
                        )
            ) {
                try {
                    val resultBitmap =
                        OpencvTools.draw_edge_from_temp_reigon_bitmap_argb_psd(
                            imageDst,
                            temperatureSrc,
                            imageHeight,
                            imageWidth,
                            if (alarmBean.isHighOpen) alarmBean.highTemp else Float.MAX_VALUE,
                            if (alarmBean.isLowOpen) alarmBean.lowTemp else Float.MIN_VALUE,
                            alarmBean.highColor,
                            alarmBean.lowColor,
                            alarmBean.markType,
                        )
                    // Convert Bitmap to byte array
                    val mat = Mat(resultBitmap.height, resultBitmap.width, CvType.CV_8UC4)
                    Utils.bitmapToMat(resultBitmap, mat)
                    Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2BGR)
                    val grayData = ByteArray(mat.cols() * mat.rows() * 3)
                    mat[0, 0, grayData]
                    // Now convert to RGBA for return
                    val diffMat =
                        Mat(
                            imageHeight,
                            imageWidth,
                            CvType.CV_8UC3,
                        )
                    diffMat.put(0, 0, grayData)
                    Imgproc.cvtColor(diffMat, diffMat, Imgproc.COLOR_BGR2RGBA)
                    val finalData = ByteArray(diffMat.cols() * diffMat.rows() * 4)
                    diffMat[0, 0, finalData]
                    return finalData
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
            }
        }
        return imageDst
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ir\utils\PseudocodeUtils.kt =====

package com.mpdc4gsr.libunified.ir.utils

import com.energy.iruvc.utils.CommonParams

object PseudocodeUtils {
    fun changeDualPseudocodeModelByOld(oldPseudocodeMode: Int): CommonParams.PseudoColorUsbDualType {
        return when (oldPseudocodeMode) {
            1 -> {
                CommonParams.PseudoColorUsbDualType.WHITE_HOT_MODE
            }

            3 -> {
                CommonParams.PseudoColorUsbDualType.IRONBOW_MODE
            }

            4 -> {
                CommonParams.PseudoColorUsbDualType.RAINBOW_MODE
            }

            5 -> {
                CommonParams.PseudoColorUsbDualType.AURORA_MODE
            }

            6 -> {
                CommonParams.PseudoColorUsbDualType.MEDICAL_MODE
            }

            7 -> {
                CommonParams.PseudoColorUsbDualType.RED_HOT_MODE
            }

            8 -> {
                CommonParams.PseudoColorUsbDualType.JUNGLE_MODE
            }

            9 -> {
                CommonParams.PseudoColorUsbDualType.MEDICAL_MODE
            }

            10 -> {
                CommonParams.PseudoColorUsbDualType.NIGHT_MODE
            }

            11 -> {
                CommonParams.PseudoColorUsbDualType.BLACK_HOT_MODE
            }

            else -> {
                CommonParams.PseudoColorUsbDualType.IRONBOW_MODE
            }
        }
    }

    fun changePseudocodeModeByOld(oldPseudocodeMode: Int): CommonParams.PseudoColorType {
        return when (oldPseudocodeMode) {
            1 -> {
                CommonParams.PseudoColorType.PSEUDO_1
            }

            3 -> {
                CommonParams.PseudoColorType.PSEUDO_3
            }

            4 -> {
                CommonParams.PseudoColorType.PSEUDO_4
            }

            5 -> {
                CommonParams.PseudoColorType.PSEUDO_5
            }

            6 -> {
                CommonParams.PseudoColorType.PSEUDO_6
            }

            7 -> {
                CommonParams.PseudoColorType.PSEUDO_7
            }

            8 -> {
                CommonParams.PseudoColorType.PSEUDO_8
            }

            9 -> {
                CommonParams.PseudoColorType.PSEUDO_9
            }

            10 -> {
                CommonParams.PseudoColorType.PSEUDO_10
            }

            11 -> {
                CommonParams.PseudoColorType.PSEUDO_11
            }

            else -> {
                CommonParams.PseudoColorType.PSEUDO_1
            }
        }
    }

    fun changePseudocodeModeByNew(pseudoColorType: CommonParams.PseudoColorType): Int {
        return when (pseudoColorType) {
            CommonParams.PseudoColorType.PSEUDO_1 -> {
                1
            }

            CommonParams.PseudoColorType.PSEUDO_3 -> {
                3
            }

            CommonParams.PseudoColorType.PSEUDO_4 -> {
                4
            }

            CommonParams.PseudoColorType.PSEUDO_5 -> {
                5
            }

            CommonParams.PseudoColorType.PSEUDO_6 -> {
                6
            }

            CommonParams.PseudoColorType.PSEUDO_7 -> {
                7
            }

            CommonParams.PseudoColorType.PSEUDO_8 -> {
                8
            }

            CommonParams.PseudoColorType.PSEUDO_9 -> {
                9
            }

            CommonParams.PseudoColorType.PSEUDO_10 -> {
                10
            }

            CommonParams.PseudoColorType.PSEUDO_11 -> {
                11
            }

            else -> {
                1
            }
        }
    }
} // The file should end here.


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ir\utils\SupRUtils.kt =====

package com.mpdc4gsr.libunified.ir.utils

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

object SupRUtils {
    fun canOpenSupR(): Boolean {
        return true
    }

    fun showOpenSupRTipsDialog(activity: Activity) {
    }

    fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    fun byteArrayToBitmap(byteArray: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ir\utils\TempDrawHelper.kt =====

package com.mpdc4gsr.libunified.ir.utils

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.TypedValue
import androidx.annotation.ColorInt
import com.mpdc4gsr.libunified.compat.ContextProvider
import com.mpdc4gsr.libunified.compat.dpToPx
import kotlin.math.max
import kotlin.math.min

class TempDrawHelper {
    companion object {
        private val POINT_SIZE: Int by lazy { 16f.dpToPx(ContextProvider.getContext()).toInt() }
        private val CIRCLE_RADIUS: Int by lazy { 3f.dpToPx(ContextProvider.getContext()).toInt() }
        private val TEMP_TEXT_OFFSET: Int by lazy { 6f.dpToPx(ContextProvider.getContext()).toInt() }
        fun Float.correctPoint(max: Int): Int = this.toInt()
            .coerceAtLeast(POINT_SIZE / 2)
            .coerceAtMost(max - POINT_SIZE / 2)

        fun Float.correct(max: Int): Int = this.toInt()
            .coerceAtLeast(CIRCLE_RADIUS)
            .coerceAtMost(max - CIRCLE_RADIUS)

        fun getRect(width: Int, height: Int): Rect =
            Rect(CIRCLE_RADIUS, CIRCLE_RADIUS, width - CIRCLE_RADIUS, height - CIRCLE_RADIUS)
    }

    var textSize: Int
        get() = textPaint.textSize.toInt()
        set(value) {
            textPaint.textSize = value.toFloat()
        }
    var textColor: Int
        @ColorInt get() = textPaint.color
        set(@ColorInt value) {
            textPaint.color = value
        }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bluePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val redPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        linePaint.strokeWidth = 1f.dpToPx(ContextProvider.getContext())
        linePaint.color = Color.WHITE
        bluePaint.color = Color.BLUE
        redPaint.color = Color.RED
        val context = ContextProvider.getContext()
        textPaint.textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            14f,
            context.resources.displayMetrics
        )
        textPaint.color = Color.WHITE
    }

    fun drawPoint(canvas: Canvas, x: Int, y: Int) {
        val left: Float = x - POINT_SIZE / 2f
        val top: Float = y - POINT_SIZE / 2f
        val right: Float = x + POINT_SIZE / 2f
        val bottom: Float = y + POINT_SIZE / 2f
        canvas.drawLine(left, y.toFloat(), right, y.toFloat(), linePaint) //
        canvas.drawLine(x.toFloat(), top, x.toFloat(), bottom, linePaint) //
    }

    fun drawLine(canvas: Canvas, startX: Int, startY: Int, stopX: Int, stopY: Int) {
        canvas.drawLine(
            startX.toFloat(),
            startY.toFloat(),
            stopX.toFloat(),
            stopY.toFloat(),
            linePaint
        )
    }

    fun drawRect(canvas: Canvas, left: Int, top: Int, right: Int, bottom: Int) {
        val leftF: Float = left.toFloat()
        val topF: Float = top.toFloat()
        val rightF: Float = right.toFloat()
        val bottomF: Float = bottom.toFloat()
        val points = floatArrayOf(
            leftF,
            topF,
            rightF,
            topF,
            rightF,
            topF,
            rightF,
            bottomF,
            rightF,
            bottomF,
            leftF,
            bottomF,
            leftF,
            bottomF,
            leftF,
            topF
        )
        canvas.drawLines(points, linePaint)
    }

    fun drawCircle(canvas: Canvas, x: Int, y: Int, isMax: Boolean) {
        canvas.drawCircle(
            x.toFloat(),
            y.toFloat(),
            CIRCLE_RADIUS.toFloat(),
            if (isMax) redPaint else bluePaint
        )
    }

    fun drawTempText(canvas: Canvas, text: String, width: Int, x: Int, y: Int) {
        var textX: Float = (x + TEMP_TEXT_OFFSET).toFloat()
        var textY: Float = (y - TEMP_TEXT_OFFSET).toFloat()
        val textWidth: Float = textPaint.measureText(text)
        if (x > width - textWidth - TEMP_TEXT_OFFSET) {//ï¼Œ
            textX = x - TEMP_TEXT_OFFSET - textWidth
        }
        val textFontTop: Float = -textPaint.getFontMetrics().top
        if (y < textFontTop + TEMP_TEXT_OFFSET / 2) {//ï¼Œ
            textY = y + TEMP_TEXT_OFFSET / 2 + textFontTop
        }
        canvas.drawText(text, textX, textY, textPaint)
    }

    fun drawTrendText(
        canvas: Canvas,
        width: Int,
        height: Int,
        startX: Int,
        startY: Int,
        stopX: Int,
        stopY: Int
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

    fun drawPointName(canvas: Canvas, name: String, width: Int, height: Int, x: Int, y: Int) {
        val textWidth: Float = textPaint.measureText(name)
        val textHeight: Float = -textPaint.getFontMetrics().top
        var textX = x - textWidth / 2
        var textY = y + POINT_SIZE / 2 + textHeight
        if (textX < 0) {//x
            textX = 0f
        }
        if (textX + textWidth > width) {//x
            textX = width - textWidth
        }
        if (textY > height) {//ï¼Œ
            textY = y - POINT_SIZE / 2 - textPaint.fontMetrics.bottom
        }
        canvas.drawText(name, textX, textY, textPaint)
    }

    fun drawPointRectName(
        canvas: Canvas,
        name: String,
        width: Int,
        height: Int,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {
        val fontMetrics: Paint.FontMetrics = textPaint.getFontMetrics()
        val textWidth: Float = textPaint.measureText(name)
        val textHeight: Float = -fontMetrics.top
        val centerX: Int = left + (right - left) / 2
        val centerY: Int = top + (bottom - top) / 2
        val offset: Float = (-fontMetrics.ascent + fontMetrics.descent) / 2 - fontMetrics.descent
        var textX: Float = centerX - textWidth / 2
        var textY: Float = centerY + offset
        if (textX < 0) {//x
            textX = 0f
        }
        if (textX + textWidth > width) {//x
            textX = width - textWidth
        }
        if (textY < textHeight) {//y
            textY = textHeight
        }
        if (textY > height) {//y
            textY = height.toFloat()
        }
        canvas.drawText(name, textX, textY, textPaint)
    }

}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ir\utils\TempUtils.kt =====

package com.mpdc4gsr.libunified.ir.utils

import android.graphics.Point
import kotlin.math.abs

object TempUtils {
    fun getLineTemps(point1: Point, point2: Point, tempArray: ByteArray, width: Int): List<Float> {
        if (point1 == point2) {//ï¼Œ
            return ArrayList(0)
        }
        val pointList: ArrayList<Point> =
            ArrayList(abs(point1.x - point2.x).coerceAtLeast(abs(point1.y - point2.y)))
        if (point1.x == point2.x) {// X 
            val startY = point1.y.coerceAtMost(point2.y)
            val endY = point1.y.coerceAtLeast(point2.y)
            for (i in startY..endY) {
                pointList.add(Point(point1.x, i))
            }
        } else {
            val k = (point1.y - point2.y).toFloat() / (point1.x - point2.x).toFloat()
            val b = point1.y - k * point1.x
            if (abs(k) <= 1) {//x
                val startX = point1.x.coerceAtMost(point2.x)
                val endX = point1.x.coerceAtLeast(point2.x)
                for (i in startX..endX) {
                    pointList.add(Point(i, (k * i + b).toInt()))
                }
            } else {//y
                if (k >= 0) {//
                    val startY = point1.y.coerceAtMost(point2.y)
                    val endY = point1.y.coerceAtLeast(point2.y)
                    for (y in startY..endY) {
                        pointList.add(Point(((y - b) / k).toInt(), y))
                    }
                } else {//
                    val startY = point1.y.coerceAtLeast(point2.y)
                    val endY = point1.y.coerceAtMost(point2.y)
                    for (y in startY downTo endY) {
                        pointList.add(Point(((y - b) / k).toInt(), y))
                    }
                }
            }
        }
        val tempList: ArrayList<Float> = ArrayList(pointList.size)
        pointList.forEach {
            val index = (it.y * width + it.x) * 2
            val tempInt =
                (tempArray[index + 1].toInt() shl 8 and 0xff00) or (tempArray[index].toInt() and 0xff)
            val tempValue = tempInt / 64f - 273.15f
            tempList.add(tempValue)
        }
        return tempList
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ir\utils\ViewStubUtils.kt =====

package com.mpdc4gsr.libunified.ir.utils

import android.view.View
import android.view.ViewStub

object ViewStubUtils {
    fun showViewStub(viewStub: ViewStub?, isShow: Boolean, callback: ((view: View?) -> Unit)?) {
        if (viewStub != null) {
            if (isShow) {
                try {
                    val view = viewStub.inflate()
                    callback?.invoke(view)
                } catch (e: Exception) {
                    viewStub.visibility = View.VISIBLE
                }
            } else {
                viewStub.visibility = View.GONE
            }
        }
    }
}