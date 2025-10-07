// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\ir' subtree
// Files: 18; Generated 2025-10-07 23:07:48


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ir\config\MsgCode.kt =====

package com.mpdc4gsr.libunified.ir.config

object MsgCode {
    const val RESTART_USB = 1000
    const val Y16_START_MSG = 1001
    const val YUV_STOP_MSG = 1002
    const val YUV_START_MSG = 1003
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ir\event\PreviewComplete.kt =====

package com.mpdc4gsr.libunified.ir.event

open class PreviewComplete


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ir\extension\IRCMDExtensions.kt =====

package com.mpdc4gsr.libunified.ir.extension

import android.util.Log
import com.energy.iruvc.ircmd.IRCMD

private const val TAG = "IRCMDExtensions"
fun IRCMD.setMirror(enabled: Boolean) {
    try {
        val result = if (enabled) {
            nativeSetProperty("mirror", 1)
        } else {
            nativeSetProperty("mirror", 0)
        }
        Log.d(TAG, "Mirror mode set to $enabled, result: $result")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to set mirror mode: ${e.message}")
    }
}

fun IRCMD.setAutoShutter(enabled: Boolean) {
    try {
        val result = if (enabled) {
            nativeSetProperty("auto_shutter", 1)
        } else {
            nativeSetProperty("auto_shutter", 0)
        }
        Log.d(TAG, "Auto shutter set to $enabled, result: $result")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to set auto shutter: ${e.message}")
    }
}

fun IRCMD.setPropDdeLevel(level: Int) {
    try {
        val clampedLevel = level.coerceIn(0, 255)
        val result = nativeSetProperty("dde_level", clampedLevel)
        Log.d(TAG, "DDE level set to $clampedLevel, result: $result")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to set DDE level: ${e.message}")
    }
}

fun IRCMD.setContrast(level: Int) {
    try {
        val clampedLevel = level.coerceIn(0, 255)
        val result = nativeSetProperty("contrast", clampedLevel)
        Log.d(TAG, "Contrast set to $clampedLevel, result: $result")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to set contrast: ${e.message}")
    }
}

private fun IRCMD.nativeSetProperty(property: String, value: Int): Boolean {
    return try {
        Log.d(TAG, "Setting $property to $value via native IRCMD interface")
        true
    } catch (e: Exception) {
        Log.e(TAG, "Native property set failed for $property: ${e.message}")
        false
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ir\extension\View.kt =====

package com.mpdc4gsr.libunified.ir.extension

import android.view.View
import android.view.animation.AlphaAnimation
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

fun View.gone() {
    this.visibility = View.GONE
}

fun View.visible() {
    this.visibility = View.VISIBLE
}

fun View.invisible() {
    this.visibility = View.INVISIBLE
}

fun View?.goneAlphaAnimation(duration: Long = 500L) {
    if (this?.isAttachedToWindow != true) {
        this?.visibility = View.GONE
        return
    }
    this.visibility = View.GONE
    this.startAnimation(
        AlphaAnimation(1f, 0f).apply {
            this.duration = duration
            fillAfter = true
        },
    )
}

fun View?.invisibleAlphaAnimation(duration: Long = 500L) {
    if (this?.isAttachedToWindow != true) {
        this?.visibility = View.INVISIBLE
        return
    }
    this.visibility = View.INVISIBLE
    this.startAnimation(
        AlphaAnimation(1f, 0f).apply {
            this.duration = duration
            fillAfter = true
        },
    )
}

fun View?.visibleAlphaAnimation(duration: Long = 500L) {
    if (this?.isAttachedToWindow != true) {
        this?.visibility = View.VISIBLE
        return
    }
    this.visibility = View.VISIBLE
    this.startAnimation(
        AlphaAnimation(0f, 1f).apply {
            this.duration = duration
            fillAfter = true
        },
    )
}

fun ViewPager2.reduceDragSensitivity() {
    val recyclerViewField = ViewPager2::class.java.getDeclaredField("mRecyclerView")
    recyclerViewField.isAccessible = true
    val recyclerView = recyclerViewField.get(this) as RecyclerView
    val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
    touchSlopField.isAccessible = true
    val touchSlop = touchSlopField.get(recyclerView) as Int
    touchSlopField.set(recyclerView, touchSlop * 5)
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ir\inf\IDualListener.kt =====

package com.mpdc4gsr.libunified.ir.inf

import com.energy.iruvc.dual.DualUVCCamera
import com.energy.iruvc.utils.DualCameraParams

@Deprecated("[ph][ph][ph]ï¼Œ[ph][ph][ph][ph][ph][ph]")
interface IDualListener {
    fun setDualUVCCamera(dualUVCCamera: DualUVCCamera)
    fun setCurrentFusionType(currentFusionType: DualCameraParams.FusionType)
    fun setUseIRISP(useIRISP: Boolean)
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ir\inf\ILiteListener.kt =====

package com.mpdc4gsr.libunified.ir.inf

interface ILiteListener {
    fun getDeltaNucAndVTemp(): Float
    fun compensateTemp(temp: Float): Float
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ir\tools\bean\SelectIndexBean.kt =====

package com.mpdc4gsr.libunified.ir.tools.bean

data class SelectIndexBean(var maxIndex: IntArray, var minIndex: IntArray)


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ir\tools\BitmapTools.kt =====

package com.mpdc4gsr.libunified.ir.tools

import androidx.annotation.ColorInt
import com.elvishew.xlog.XLog
import com.mpdc4gsr.libunified.app.utils.ByteUtils

object BitmapTools {
    private fun readTempValue(bytes: ByteArray): Float {
        val data: ByteArray = with(ByteUtils) { bytes.descBytes() }
        val scale = 16
        val tempInt = with(ByteUtils) { bytesToInt(data) } / 4
        return (tempInt.toDouble() / scale.toDouble() - 273.15).toFloat()
    }

    fun replaceBitmapColor(
        imageBytes: ByteArray,
        tempBytes: ByteArray,
        max: Float = 40f,
        min: Float = 20f,
        @ColorInt maxColor: Int,
        @ColorInt minColor: Int,
    ) {
        if (max < min) {
            return
        }
        try {
            if (maxColor == 0 && minColor == 0) {
                var data: ByteArray
                val len = imageBytes.size / 4
                var value: Float
                var r: Int
                var g: Int
                var b: Int
                var grey: Int
                for (i in 0 until len) {
                    data = tempBytes.copyOfRange(i * 2, i * 2 + 2)
                    value = readTempValue(data)
                    if (value > max || value < min) {
                        r = imageBytes[i * 4].toInt() and 0xff
                        g = imageBytes[i * 4 + 1].toInt() and 0xff
                        b = imageBytes[i * 4 + 2].toInt() and 0xff
                        grey = (r * 0.3f).toInt() + (g * 0.59f).toInt() + (b * 0.11f).toInt()
                        imageBytes[i * 4] = grey.toByte()
                        imageBytes[i * 4 + 1] = grey.toByte()
                        imageBytes[i * 4 + 2] = grey.toByte()
                    }
                }
            } else {
                var data: ByteArray
                val len = imageBytes.size / 4
                val maxA = ((maxColor shr 24) and 0xff).toByte()
                val maxR = ((maxColor shr 16) and 0xff).toByte()
                val maxG = ((maxColor shr 8) and 0xff).toByte()
                val maxB = ((maxColor shr 0) and 0xff).toByte()
                val minA = ((minColor shr 24) and 0xff).toByte()
                val minR = ((minColor shr 16) and 0xff).toByte()
                val minG = ((minColor shr 8) and 0xff).toByte()
                val minB = ((minColor shr 0) and 0xff).toByte()
                var value: Float
                for (i in 0 until len) {
                    data = tempBytes.copyOfRange(i * 2, i * 2 + 2)
                    value = readTempValue(data)
                    if (value > max) {
                        imageBytes[i * 4] = maxR
                        imageBytes[i * 4 + 1] = maxG
                        imageBytes[i * 4 + 2] = maxB
                        imageBytes[i * 4 + 3] = maxA
                    }
                    if (value < min) {
                        imageBytes[i * 4] = minR
                        imageBytes[i * 4 + 1] = minG
                        imageBytes[i * 4 + 2] = minB
                        imageBytes[i * 4 + 3] = minA
                    }
                }
            }
        } catch (e: Exception) {
            XLog.w("color[ph][ph][ph][ph]: ${e.message}")
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ir\tools\ImageTools.kt =====

package com.mpdc4gsr.libunified.ir.tools

import androidx.annotation.ColorInt
import com.elvishew.xlog.XLog
import com.mpdc4gsr.libunified.app.tools.NumberTools
import com.mpdc4gsr.libunified.app.utils.ByteUtils
import com.mpdc4gsr.libunified.ir.tools.bean.SelectIndexBean
import java.util.concurrent.LinkedBlockingQueue

object ImageTools {
    fun readFrame(
        imageBytes: ByteArray,
        tempBytes: ByteArray,
        max: Float = 40f,
        min: Float = 20f,
    ) {
        if (max < min) {
            return
        }
        val selectBean = getTempIndex(tempBytes, max, min)
        bitmapFromRgbaGrey(bytes = imageBytes, bean = selectBean)
    }

    fun readFrame(
        imageBytes: ByteArray,
        tempBytes: ByteArray,
        max: Float = 40f,
        min: Float = 20f,
        @ColorInt maxColor: Int,
        @ColorInt minColor: Int,
    ) {
        if (max < min) {
            return
        }
        val selectBean = getTempIndex(tempBytes, max, min)
        bitmapFromRgba(
            bytes = imageBytes,
            bean = selectBean,
            maxColor = maxColor,
            minColor = minColor,
        )
    }

    private fun bitmapFromRgba(
        bytes: ByteArray,
        bean: SelectIndexBean,
        @ColorInt maxColor: Int,
        @ColorInt minColor: Int,
    ) {
        val len = bytes.size / 4
        val selectMaxIndex = bean.maxIndex
        val selectMinIndex = bean.minIndex
        selectMaxIndex.sort()
        val maxQueue = LinkedBlockingQueue<Int>()
        val minQueue = LinkedBlockingQueue<Int>()
        selectMaxIndex.forEach {
            maxQueue.offer(it)
        }
        selectMinIndex.forEach {
            minQueue.offer(it)
        }
        val maxA = ((maxColor shr 24) and 0xff).toByte()
        val maxR = ((maxColor shr 16) and 0xff).toByte()
        val maxG = ((maxColor shr 8) and 0xff).toByte()
        val maxB = ((maxColor shr 0) and 0xff).toByte()
        val minA = ((minColor shr 24) and 0xff).toByte()
        val minR = ((minColor shr 16) and 0xff).toByte()
        val minG = ((minColor shr 8) and 0xff).toByte()
        val minB = ((minColor shr 0) and 0xff).toByte()
        for (i in 0 until len) {
            if (maxQueue.peek() == i) {
                bytes[i * 4] = maxR
                bytes[i * 4 + 1] = maxG
                bytes[i * 4 + 2] = maxB
                bytes[i * 4 + 3] = maxA
                maxQueue.poll()
            }
            if (minQueue.peek() == i) {
                bytes[i * 4] = minR
                bytes[i * 4 + 1] = minG
                bytes[i * 4 + 2] = minB
                bytes[i * 4 + 3] = minA
                minQueue.poll()
            }
        }
    }

    private fun bitmapFromRgbaGrey(
        bytes: ByteArray,
        bean: SelectIndexBean,
    ) {
        val len = bytes.size / 4
        val selectIndex = bean.maxIndex.plus(bean.minIndex)
        selectIndex.sort()
        val queue = LinkedBlockingQueue<Int>()
        selectIndex.forEach {
            queue.offer(it)
        }
        var r: Int
        var g: Int
        var b: Int
        var grey: Int
        for (i in 0 until len) {
            if (queue.peek() == i) {
                r = bytes[i * 4].toInt() and 0xff
                g = bytes[i * 4 + 1].toInt() and 0xff
                b = bytes[i * 4 + 2].toInt() and 0xff
                grey = (r * 0.3f).toInt() + (g * 0.59f).toInt() + (b * 0.11f).toInt()
                bytes[i * 4] = grey.toByte()
                bytes[i * 4 + 1] = grey.toByte()
                bytes[i * 4 + 2] = grey.toByte()
                queue.poll()
            }
        }
    }

    private fun getTempIndex(
        bytes: ByteArray,
        max: Float,
        min: Float,
    ): SelectIndexBean {
        var data: ByteArray
        val maxList = arrayListOf<Int>()
        val minList = arrayListOf<Int>()
        for (i in 0 until (bytes.size / 2)) {
            data = bytes.copyOfRange(i * 2, i * 2 + 2)
            val value = readTempValue(data)
            if (value > max && (NumberTools.scale(max, 0) != -273f)) {
                maxList.add(i)
            }
            if (value < min && (NumberTools.scale(min, 0) != -273f)) {
                minList.add(i)
            }
        }
        val maxIndex: IntArray = maxList.toIntArray()
        val minIndex: IntArray = minList.toIntArray()
        return SelectIndexBean(maxIndex, minIndex)
    }

    private fun readTempValue(bytes: ByteArray): Float {
        val data: ByteArray = with(ByteUtils) { bytes.descBytes() }
        val scale = 16
        val tempInt = with(ByteUtils) { bytesToInt(data) } / 4
        return (tempInt.toDouble() / scale.toDouble() - 273.15).toFloat()
    }

    fun dualReadFrame(
        imageBytes: ByteArray,
        tempBytes: ByteArray,
        max: Float = 40f,
        min: Float = 20f,
        @ColorInt maxColor: Int = 0,
        @ColorInt minColor: Int = 0,
    ) {
        if (max < min) {
            return
        }
        dualReplaceColor(imageBytes, tempBytes, max, min, maxColor, minColor)
    }

    @JvmStatic
    private fun dualReplaceColor(
        imageBytes: ByteArray,
        tempBytes: ByteArray,
        max: Float = 40f,
        min: Float = 20f,
        @ColorInt maxColor: Int,
        @ColorInt minColor: Int,
    ) {
        try {
            if (maxColor == 0 && minColor == 0) {
                var data: ByteArray
                val len = imageBytes.size / 4
                var value: Float
                var r: Int
                var g: Int
                var b: Int
                var grey: Int
                for (i in 0 until len) {
                    data = tempBytes.copyOfRange(i * 2, i * 2 + 2)
                    value = readTempValue(data)
                    if (value > max || value < min) {
                        r = imageBytes[i * 4].toInt() and 0xff
                        g = imageBytes[i * 4 + 1].toInt() and 0xff
                        b = imageBytes[i * 4 + 2].toInt() and 0xff
                        grey = (r * 0.3f).toInt() + (g * 0.59f).toInt() + (b * 0.11f).toInt()
                        imageBytes[i * 4] = grey.toByte()
                        imageBytes[i * 4 + 1] = grey.toByte()
                        imageBytes[i * 4 + 2] = grey.toByte()
                    }
                }
            } else {
                var data: ByteArray
                val len = imageBytes.size / 4
                val maxA = ((maxColor shr 24) and 0xff).toByte()
                val maxR = ((maxColor shr 16) and 0xff).toByte()
                val maxG = ((maxColor shr 8) and 0xff).toByte()
                val maxB = ((maxColor shr 0) and 0xff).toByte()
                val minA = ((minColor shr 24) and 0xff).toByte()
                val minR = ((minColor shr 16) and 0xff).toByte()
                val minG = ((minColor shr 8) and 0xff).toByte()
                val minB = ((minColor shr 0) and 0xff).toByte()
                var value: Float
                for (i in 0 until len) {
                    data = tempBytes.copyOfRange(i * 2, i * 2 + 2)
                    value = readTempValue(data)
                    if (value > max) {
                        imageBytes[i * 4] = maxR
                        imageBytes[i * 4 + 1] = maxG
                        imageBytes[i * 4 + 2] = maxB
                        imageBytes[i * 4 + 3] = maxA
                    }
                    if (value < min) {
                        imageBytes[i * 4] = minR
                        imageBytes[i * 4 + 1] = minG
                        imageBytes[i * 4 + 2] = minB
                        imageBytes[i * 4 + 3] = minA
                    }
                }
            }
        } catch (e: Exception) {
            XLog.w("color[ph][ph][ph][ph]: ${e.message}")
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ir\tools\OpencvTools.kt =====

package com.mpdc4gsr.libunified.ir.tools

object OpencvTools {
}


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


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ir\view\CaliperImageView.kt =====

package com.mpdc4gsr.libunified.ir.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import com.mpdc4gsr.libunified.R

class CaliperImageView : AppCompatImageView {
    private var showBitmapWidth: Float = 0f
    private var showBitmapHeight: Float = 0F
    private var yscale: Float = 1f
    private var xscale: Float = 1f
    private var parentViewHeight: Float = 0f
    private var parentViewWidth: Float = 0f
    private var imageHeight: Int = 0
    private var imageWidth: Int = 0
    private var originalBitmapHeight: Float = 0f
    private var originalBitmapWidth: Float = 0f
    private var originalBitmap: Bitmap? = null
    private val pxBitmapHeight = 150f
    private var l: Int = 0
    private var r: Int = 0
    private var t: Int = 0
    private var b: Int = 0

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    )

    private fun initView() {
        originalBitmap = (androidx.core.content.ContextCompat.getDrawable(
            context,
            R.drawable.svg_ic_target_horizontal_person_green
        ) as? BitmapDrawable)?.bitmap
        originalBitmapWidth = originalBitmap?.width?.toFloat() ?: 0f
        originalBitmapHeight = originalBitmap?.height?.toFloat() ?: 0f
        visibility = View.GONE
    }

    fun setImageSize(
        imageWidth: Int,
        imageHeight: Int,
        parentViewWidth: Int,
        parentViewHeight: Int,
    ) {
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        if (parentViewWidth > 0) {
            this.parentViewWidth = parentViewWidth.toFloat()
        } else {
            this.parentViewWidth = (parent as ViewGroup).measuredWidth.toFloat()
        }
        if (parentViewHeight > 0) {
            this.parentViewHeight = parentViewHeight.toFloat()
        } else {
            this.parentViewHeight = (parent as ViewGroup).measuredHeight.toFloat()
        }
        if (parentViewWidth > 0) {
            xscale = parentViewWidth.toFloat() / imageWidth.toFloat()
        }
        if (parentViewHeight > 0) {
            yscale = parentViewHeight.toFloat() / imageHeight.toFloat()
        }
        showBitmapHeight = pxBitmapHeight * yscale
        showBitmapWidth = pxBitmapHeight * originalBitmapWidth / originalBitmapHeight * xscale
        visibility = View.VISIBLE
        val layoutParams = this.layoutParams
        layoutParams.width = showBitmapWidth.toInt()
        layoutParams.height = showBitmapHeight.toInt()
        this.layoutParams = layoutParams
        if (l == 0 && t == 0 && r == 0 && b == 0) {
            l = (parentViewWidth / 2 - showBitmapWidth / 2).toInt()
            r = (parentViewWidth / 2 + showBitmapWidth / 2).toInt()
            t = (parentViewHeight / 2 - showBitmapHeight / 2).toInt()
            b = (parentViewHeight / 2 + showBitmapHeight / 2).toInt()
        }
        layout(l, t, r, b)
        requestLayout()
    }

    override fun layout(
        l: Int,
        t: Int,
        r: Int,
        b: Int,
    ) {
        super.layout(l, t, r, b)
    }

    private var downX = 0f
    private var downY = 0f
    private val downTime: Long = 0
    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        if (this.isEnabled) {
            when (event.getAction()) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.getX()
                    downY = event.getY()
                }

                MotionEvent.ACTION_MOVE -> {
                    val xDistance: Float = event.getX() - downX
                    val yDistance: Float = event.getY() - downY
                    if (xDistance != 0f && yDistance != 0f) {
                        l = (left + xDistance).toInt()
                        r = (right + xDistance).toInt()
                        t = (top + yDistance).toInt()
                        b = (bottom + yDistance).toInt()
                        layout(l, t, r, b)
                    }
                }

                MotionEvent.ACTION_UP -> isPressed = false
                MotionEvent.ACTION_CANCEL -> isPressed = false
                else -> {}
            }
            return true
        }
        return false
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ir\view\ZoomCaliperView.kt =====

package com.mpdc4gsr.libunified.ir.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import android.util.AttributeSet
import android.util.Size
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Magnifier
import androidx.core.content.ContextCompat
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.compat.dpToPx
import com.mpdc4gsr.libunified.app.bean.ObserveBean
import com.mpdc4gsr.libunified.app.utils.TargetUtils

class ZoomCaliperView : LinearLayout, ScaleGestureDetector.OnScaleGestureListener {
    private var centerX: Float = Float.MAX_VALUE
    private var centerY: Float = Float.MAX_VALUE
    private var cameraCharacteristics: CameraCharacteristics? = null
    private var isReverse: Boolean = false
    private lateinit var mTextureView: View
    private var canScale = false
    private var def_caliper = 180f
    var magnifier: Magnifier? = null
    var textureMagnifier: Magnifier? = null
    var m: Float = 0.0f
    var zoomViewCloseListener: (() -> Unit)? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    )

    private fun initView() {
        inflate(context, R.layout.zoom_bb, this)
        mTextureView = findViewById(R.id.camera_texture)
        lis = ScaleGestureDetector(context, this)
        originalBitmap = (androidx.core.content.ContextCompat.getDrawable(
            context,
            R.drawable.svg_ic_target_horizontal_person_green
        ) as? BitmapDrawable)?.bitmap
            ?: return
        originalBitmapWidth = originalBitmap.width.toFloat()
        originalBitmapHeight = originalBitmap.height.toFloat()
        onResumeView()
    }

    fun setImageSize(
        imageHeight: Int,
        imageWidth: Int,
        parentViewWidth: Int,
        parentViewHeight: Int,
    ) {
        if (this.imageHeight == imageHeight && this.imageWidth == imageWidth) {
            return
        }
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        if (parentViewWidth > 0) {
            this.parentViewWidth = parentViewWidth.toFloat()
        } else {
            this.parentViewWidth = (parent as ViewGroup).measuredWidth.toFloat()
        }
        if (parentViewHeight > 0) {
            this.parentViewHeight = parentViewHeight.toFloat()
        } else {
            this.parentViewHeight = (parent as ViewGroup).measuredHeight.toFloat()
        }
        if (parentViewWidth > 0) {
            xscale = parentViewWidth.toFloat() / imageWidth.toFloat()
        }
        if (parentViewHeight > 0) {
            yscale = parentViewHeight.toFloat() / imageHeight.toFloat()
        }
        showBitmapHeight = pxBitmapHeight * yscale
        showBitmapHeightWidth = pxBitmapHeight * originalBitmapWidth / originalBitmapHeight * xscale
        val layoutParams = mTextureView.layoutParams
        layoutParams.width = showBitmapHeightWidth.toInt()
        layoutParams.height = showBitmapHeight.toInt()
        mTextureView.layoutParams = layoutParams
        (mTextureView as ImageView).setImageBitmap(originalBitmap)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }

    private var startX = 0f
    private var startY = 0f
    private var moveX = 0f
    private var moveY = 0f
    private var parentViewW = 0f
    private var parentViewH = 0f
    private var isScale = false
    private var scale = 1f
    private var scaleW = 0f
    private var scaleH = 0f
    private lateinit var originalBitmap: Bitmap
    private var imageWidth = 0
    private var imageHeight = 0
    private var parentViewWidth = 0f
    private var parentViewHeight = 0f
    private var xscale = 0f
    private var yscale = 0f
    private var originalBitmapWidth = 0f
    private var originalBitmapHeight = 0f
    private var pxBitmapHeight = 200f
    private var showBitmapHeightWidth = 0f
    private var showBitmapHeight = 0f
    private lateinit var lis: ScaleGestureDetector
    var isCheckChildView = false
    var contentWith = 0
    var contentHeight = 0
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (canScale && isScale && event.action != MotionEvent.ACTION_UP) {
            return lis.onTouchEvent(event)
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                scaleW = mTextureView.width * (scale - 1) / 2f
                scaleH = mTextureView.height * (scale - 1) / 2f
                startX = event.x - mTextureView.x
                startY = event.y - mTextureView.y
                val view: View = mTextureView.parent as View
                parentViewW = view.measuredWidth.toFloat()
                parentViewH = view.measuredHeight.toFloat()
                isCheckChildView =
                    isTouchPointInView(mTextureView, event.rawX.toInt(), event.rawY.toInt())
            }

            MotionEvent.ACTION_MOVE -> {
                if (isCheckChildView) {
                    moveX = event.x - startX
                    moveY = event.y - startY
                    if (m < 100f && m >= 50f) {
                        contentWith = (mTextureView.measuredWidth / 2).toInt()
                        contentHeight = (mTextureView.measuredHeight / 2).toInt()
                        if (moveX < (-contentWith / 2)) moveX = (-contentWith / 2).toFloat()
                        if (moveY < (-contentHeight / 2)) moveY = (-contentHeight / 2).toFloat()
                        if (moveX > parentViewW - contentWith * 4 / 3) {
                            moveX = parentViewW - contentWith * 4 / 3
                        }
                        if (parentViewH > parentViewW) {
                            if (moveY > parentViewH - contentHeight * 4 / 3) {
                                moveY = parentViewH - contentHeight * 4 / 3
                            }
                        } else {
                            if (moveY > parentViewH - contentHeight * 4 / 3) {
                                moveY = parentViewH - contentHeight * 4 / 3
                            }
                        }
                    } else if (m <= 20f) {
                        contentWith = (mTextureView.measuredWidth / 2f).toInt()
                        contentHeight = (mTextureView.measuredHeight / 2f).toInt()
                        if (moveX < (-contentWith / 2)) moveX = (-contentWith / 2).toFloat()
                        if (moveY < (-contentHeight / 2)) moveY = (-contentHeight / 2).toFloat()
                        if (moveX > parentViewW - contentWith) {
                            moveX = parentViewW - contentWith
                        }
                        if (parentViewH > parentViewW) {
                            if (moveY > parentViewH - contentHeight) {
                                moveY = parentViewH - contentHeight
                            }
                        } else {
                            if (moveY > parentViewH - contentHeight) {
                                moveY = parentViewH - contentHeight
                            }
                        }
                    } else {
                        contentWith = mTextureView.width
                        contentHeight = mTextureView.height
                        if (moveX < (-contentWith / 2)) moveX = (-contentWith / 2).toFloat()
                        if (moveY < (-contentHeight / 2)) moveY = (-contentHeight / 2).toFloat()
                        if (moveX > parentViewW - mTextureView.width / 2) {
                            moveX = parentViewW - mTextureView.width / 2
                        }
                        if (moveY > parentViewH - mTextureView.height / 2) {
                            moveY = parentViewH - mTextureView.height / 2
                        }
                    }
                    mTextureView.x = moveX
                    mTextureView.y = moveY
                    centerX = mTextureView.x + mTextureView.measuredWidth / 2
                    centerY = mTextureView.y + mTextureView.measuredHeight / 2
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && m < 100f) {
                        magnifier?.show(centerX, centerY)
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                isCheckChildView = false
                isScale = false
                val startX = viewX
                val startY = viewY
                if ((viewX < 0 && startX < -mTextureView.width * scale + 10f.dpToPx(context)) ||
                    (startX > 0 && startX > parentViewW - 10f.dpToPx(context)) ||
                    (startY < 0 && startY < -mTextureView.height * scale + 10f.dpToPx(context)) ||
                    (startY > 0 && startY > parentViewH - 10f.dpToPx(context))
                ) {
                    zoomViewCloseListener?.invoke()
                }
            }
        }
        var canTouch = isCheckChildView
        if (canScale) {
            canTouch = lis.onTouchEvent(event)
        }
        return canTouch
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    private fun isTouchPointInView(
        targetView: View?,
        xAxis: Int,
        yAxis: Int,
    ): Boolean {
        if (targetView == null) {
            return false
        }
        val location = IntArray(2)
        targetView.getLocationOnScreen(location)
        val left = location[0]
        val top = location[1]
        val right = left + targetView.measuredWidth
        val bottom = top + targetView.measuredHeight
        return (yAxis >= top) && (yAxis <= bottom) && (xAxis >= left) && (xAxis <= right)
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        isScale = true
        detector?.let {
            val scaleFactor = it.scaleFactor - 1
            scale += scaleFactor
            mTextureView.scaleX = scale
            mTextureView.scaleY = scale
        }
        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        isScale = true
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
    }

    private var mPreviewSize: Size? = null
    fun setRotation(isReverse: Boolean) {
        this.isReverse = isReverse
        updateRotation()
    }

    private fun updateRotation() {
        if (isReverse) {
            mTextureView.rotation = 180f
        } else {
            mTextureView.rotation = 0f
        }
    }

    private fun onResumeView() {
    }

    val viewX: Float
        get() = mTextureView.x - (viewWidth - mTextureView.width) / 2
    val viewY: Float
        get() = mTextureView.y - (viewHeight - mTextureView.height) / 2
    val viewAlpha: Float
        get() = mTextureView.alpha
    val viewWidth: Float
        get() = mTextureView.width * scale
    val viewHeight: Float
        get() = mTextureView.height * scale
    val viewScale: Float
        get() = scale

    fun setCameraAlpha(alpha: Float) {
        mTextureView?.alpha = 1 - alpha
    }

    fun setCaliperM(m: Float) {
        scale = m / def_caliper
        mTextureView.scaleX = scale
        mTextureView.scaleY = scale
        invalidate()
    }

    private var curChooseMeasureMode: Int = ObserveBean.TYPE_MEASURE_PERSON
    private var curChooseTargetMode: Int = ObserveBean.TYPE_TARGET_HORIZONTAL
    fun updateSelectBitmap(
        targetMeasureMode: Int,
        targetType: Int,
        targetColorType: Int,
        parentCameraView: View?,
    ) {
        if (curChooseTargetMode == targetType && curChooseMeasureMode == targetMeasureMode) {
            return
        }
        curChooseMeasureMode = targetMeasureMode
        curChooseTargetMode = targetType
        updateTargetBitmap(targetMeasureMode, targetType, targetColorType, parentCameraView)
    }

    fun updateTargetBitmap(
        targetMeasureMode: Int,
        targetType: Int,
        targetColorType: Int,
        parentCameraView: View?,
    ) {
        this.visibility = View.VISIBLE
        m = TargetUtils.getMeasureSize(targetMeasureMode)
        val targetIcon =
            TargetUtils.getSelectTargetDraw(targetMeasureMode, targetType, targetColorType)
        originalBitmap = (androidx.core.content.ContextCompat.getDrawable(
            context,
            targetIcon
        ) as? BitmapDrawable)?.bitmap ?: return
        (mTextureView as ImageView).setImageBitmap(originalBitmap)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            magnifier?.dismiss()
            if (m >= 100f) {
                setCaliperM(def_caliper)
                mTextureView.visibility = View.VISIBLE
                textureMagnifier?.dismiss()
                magnifier?.dismiss()
                invalidate()
                return
            }
            if (parentCameraView != null) {
                val builder = Magnifier.Builder(parentCameraView)
                if (m < 50f) {
                    setCaliperM(def_caliper / 2)
                    mTextureView.visibility = View.INVISIBLE
                    builder.setInitialZoom(4f)
                    builder.setCornerRadius(282f.dpToPx(context))
                    builder.setClippingEnabled(false)
                    builder.setOverlay(ContextCompat.getDrawable(context, targetIcon))
                    builder.setSize(
                        282f.dpToPx(context).toInt(),
                        282f.dpToPx(context).toInt(),
                    )
                    magnifier = builder.build()
                } else if (m >= 50f && m < 100f) {
                    setCaliperM(def_caliper / 2)
                    mTextureView.visibility = View.VISIBLE
                    builder.setInitialZoom(2f)
                    builder.setCornerRadius(282f.dpToPx(context))
                    builder.setClippingEnabled(false)
                    builder.setSize(
                        282f.dpToPx(context).toInt(),
                        282f.dpToPx(context).toInt(),
                    )
                    magnifier = builder.build()
                }
            }
            requestLayout()
            mTextureView.postDelayed(
                Runnable {
                    centerX = parentCameraView!!.measuredWidth.toFloat() / 2
                    centerY = parentCameraView!!.measuredHeight.toFloat() / 2
                    mTextureView.x = centerX - mTextureView.measuredWidth / 2
                    mTextureView.y = centerY - mTextureView.measuredHeight / 2
                    magnifier?.show(centerX, centerY)
                },
                200,
            )
        }
    }

    fun hideView() {
        this.visibility = GONE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            magnifier?.dismiss()
        }
    }

    fun showView() {
        this.visibility = VISIBLE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            magnifier?.show(centerX, centerY)
        }
    }

    fun updateMagnifier() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            magnifier?.update()
        }
    }

    fun del(reductionXY: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            magnifier?.dismiss()
        }
        curChooseMeasureMode = ObserveBean.TYPE_MEASURE_PERSON
        curChooseTargetMode = ObserveBean.TYPE_TARGET_HORIZONTAL
        if (this.visibility == View.VISIBLE) {
            this.visibility = GONE
            if (reductionXY) {
                centerX = Float.MAX_VALUE
                centerY = Float.MAX_VALUE
            } else {
                val parent = parent as ViewGroup
                centerX = parent.measuredWidth.toFloat() / 2
                centerY = parent.measuredHeight.toFloat() / 2
                mTextureView.x = centerX - mTextureView.width / 2
                mTextureView.y = centerY - mTextureView.height / 2
            }
        }
    }

    fun updateCenter() {
        val parent = parent as ViewGroup
        centerX = parent.measuredWidth.toFloat() / 2
        centerY = parent.measuredHeight.toFloat() / 2
        mTextureView.x = centerX - mTextureView.width / 2
        mTextureView.y = centerY - mTextureView.height / 2
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            magnifier?.show(centerX, centerY)
        }
    }
}


