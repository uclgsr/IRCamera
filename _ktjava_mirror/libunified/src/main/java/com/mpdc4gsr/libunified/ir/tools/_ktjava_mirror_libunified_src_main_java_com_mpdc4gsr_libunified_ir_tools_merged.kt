// Merged ALL .kt and .java files from the '_ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\ir\tools' directory and its subdirectories.
// Total files: 2 | Generated on: 2025-10-08 01:42:45


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\ir\tools\bean\libunified_src_main_java_com_mpdc4gsr_libunified_ir_tools_bean_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\ir\tools\bean' subtree
// Files: 1; Generated 2025-10-07 23:07:50


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\ir\tools\bean\SelectIndexBean.kt =====

package com.mpdc4gsr.libunified.ir.tools.bean

data class SelectIndexBean(var maxIndex: IntArray, var minIndex: IntArray)


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\ir\tools\libunified_src_main_java_com_mpdc4gsr_libunified_ir_tools_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\ir\tools' subtree
// Files: 4; Generated 2025-10-07 23:07:50


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