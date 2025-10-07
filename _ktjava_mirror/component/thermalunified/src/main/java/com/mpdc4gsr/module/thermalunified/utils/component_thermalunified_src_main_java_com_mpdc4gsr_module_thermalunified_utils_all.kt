// Merged .kt under 'component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\utils' subtree
// Files: 11; Generated 2025-10-07 23:07:44


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\utils\ArrayUtils.kt =====

package com.mpdc4gsr.module.thermalunified.utils

object ArrayUtils {
    fun getMaxIndex(
        data: FloatArray,
        rotateType: Int = 0,
        selectIndexList: ArrayList<Int> = arrayListOf(),
    ): Int {
        val index =
            when (rotateType) {
                1, 2, 3 -> getRotateMaxIndex(data, rotateType, selectIndexList)
                else -> getMaxIndex(data, selectIndexList)
            }
        return index
    }

    fun getMinIndex(
        data: FloatArray,
        rotateType: Int = 0,
        selectIndexList: ArrayList<Int> = arrayListOf(),
    ): Int {
        val index =
            when (rotateType) {
                1, 2, 3 -> getRotateMinIndex(data, rotateType, selectIndexList)
                else -> getMinIndex(data, selectIndexList)
            }
        return index
    }

    fun matrixRotate(
        srcData: FloatArray,
        rotateType: Int = 0,
    ): FloatArray {
        return when (rotateType) {
            1 -> matrixRotate90(srcData)
            2 -> matrixRotate180(srcData)
            3 -> matrixRotate270(srcData)
            else -> srcData
        }
    }

    private fun getMaxIndex(
        data: FloatArray,
        selectIndexList: ArrayList<Int> = arrayListOf(),
    ): Int {
        if (selectIndexList.size == 0) {
            var maxIndex = 0
            for (i in 1 until data.size - 1) {
                if (data[i] > data[maxIndex]) {
                    maxIndex = i
                }
            }
            return maxIndex
        } else {
            val selectPoint = FloatArray(selectIndexList.size)
            for (i in 0 until selectIndexList.size) {
                selectPoint[i] = data[selectIndexList[i]]
            }
            var maxIndex = 0
            for (i in 1 until selectPoint.size - 1) {
                if (selectPoint[i] > selectPoint[maxIndex]) {
                    maxIndex = i
                }
            }
            return selectIndexList[maxIndex]
        }
    }

    private fun getMinIndex(
        data: FloatArray,
        selectIndexList: ArrayList<Int> = arrayListOf(),
    ): Int {
        if (selectIndexList.size == 0) {
            var minIndex = 0
            for (i in 1 until data.size - 1) {
                if (data[i] == 0f) {
                    continue
                }
                if (data[i] < data[minIndex]) {
                    minIndex = i
                }
            }
            return minIndex
        } else {
            val selectPoint = FloatArray(selectIndexList.size)
            for (i in 0 until selectIndexList.size) {
                selectPoint[i] = data[selectIndexList[i]]
            }
            var minIndex = 0
            for (i in 1 until selectPoint.size - 1) {
                if (selectPoint[i] == 0f) {
                    continue
                }
                if (selectPoint[i] < selectPoint[minIndex]) {
                    minIndex = i
                }
            }
            return selectIndexList[minIndex]
        }
    }

    private fun getRotateMaxIndex(
        data: FloatArray,
        rotateType: Int = 0,
        selectIndexList: ArrayList<Int> = arrayListOf(),
    ): Int {
        if (selectIndexList.size == 0) {
            val destData = matrixRotate(data, rotateType)
            var maxIndex = 0
            for (i in 1 until destData.size - 1) {
                if (destData[i] > destData[maxIndex]) {
                    maxIndex = i
                }
            }
            return maxIndex
        } else {
            val destData = matrixRotate(data, rotateType)
            val selectPoint = FloatArray(selectIndexList.size)
            for (i in 0 until selectIndexList.size) {
                selectPoint[i] = destData[selectIndexList[i]]
            }
            var maxIndex = 0
            for (i in 1 until selectPoint.size - 1) {
                if (selectPoint[i] > selectPoint[maxIndex]) {
                    maxIndex = i
                }
            }
            return selectIndexList[maxIndex]
        }
    }

    private fun getRotateMinIndex(
        data: FloatArray,
        rotateType: Int = 0,
        selectIndexList: ArrayList<Int> = arrayListOf(),
    ): Int {
        if (selectIndexList.size == 0) {
            val destData = matrixRotate(data, rotateType)
            var minIndex = 0
            for (i in 1 until destData.size - 1) {
                if (destData[i] == 0f) {
                    continue
                }
                if (destData[i] < destData[minIndex]) {
                    minIndex = i
                }
            }
            return minIndex
        } else {
            val destData = matrixRotate(data, rotateType)
            val selectPoint = FloatArray(selectIndexList.size)
            for (i in 0 until selectIndexList.size) {
                selectPoint[i] = destData[selectIndexList[i]]
            }
            var minIndex = 0
            for (i in 1 until selectPoint.size - 1) {
                if (selectPoint[i] == 0f) {
                    continue
                }
                if (selectPoint[i] < selectPoint[minIndex]) {
                    minIndex = i
                }
            }
            return selectIndexList[minIndex]
        }
    }

    private fun matrixRotate90(srcData: FloatArray): FloatArray {
        val row = 192
        val column = 256
        val srcMatrix = Array(row) { FloatArray(column) }
        for (i in 0 until row) {
            for (j in 0 until column) {
                srcMatrix[i][j] = srcData[i * column + j]
            }
        }
        val destMatrix = Array(column) { FloatArray(row) }
        for (x in 0 until column) {
            for (y in 0 until row) {
                destMatrix[x][y] = srcMatrix[row - 1 - y][x]
            }
        }
        val data = FloatArray(srcData.size)
        for (i in destMatrix.indices) {
            for (j in destMatrix[i].indices) {
                data[destMatrix[0].size * i + j] = destMatrix[i][j]
            }
        }
        return data
    }

    private fun matrixRotate180(srcData: FloatArray): FloatArray {
        val row = 192
        val column = 256
        val srcMatrix = Array(row) { FloatArray(column) }
        for (i in 0 until row) {
            for (j in 0 until column) {
                srcMatrix[i][j] = srcData[i * column + j]
            }
        }
        val destMatrix = Array(row) { FloatArray(column) }
        for (x in 0 until row) {
            for (y in 0 until column) {
                destMatrix[x][y] = srcMatrix[row - 1 - x][column - 1 - y]
            }
        }
        val data = FloatArray(srcData.size)
        for (i in destMatrix.indices) {
            for (j in destMatrix[i].indices) {
                data[destMatrix[0].size * i + j] = destMatrix[i][j]
            }
        }
        return data
    }

    private fun matrixRotate270(srcData: FloatArray): FloatArray {
        val row = 192
        val column = 256
        val srcMatrix = Array(row) { FloatArray(column) }
        for (i in 0 until row) {
            for (j in 0 until column) {
                srcMatrix[i][j] = srcData[i * column + j]
            }
        }
        val destMatrix = Array(column) { FloatArray(row) }
        for (x in 0 until column) {
            for (y in 0 until row) {
                destMatrix[x][y] = srcMatrix[y][column - 1 - x]
            }
        }
        val data = FloatArray(srcData.size)
        for (i in destMatrix.indices) {
            for (j in destMatrix[i].indices) {
                data[destMatrix[0].size * i + j] = destMatrix[i][j]
            }
        }
        return data
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\utils\CalibrationTools.kt =====

package com.mpdc4gsr.module.thermalunified.utils

import android.util.Log
import com.elvishew.xlog.XLog
import com.energy.iruvc.ircmd.IRCMD
import com.energy.iruvc.utils.CommonParams
import com.energy.iruvc.utils.SynchronizedBitmap

object CalibrationTools {
    fun sign(
        irCmd: IRCMD,
        singlePointTemp: Int,
    ): Boolean {
        var success = false
        if (irCmd.restoreDefaultConfig(CommonParams.DefaultConfigType.DEF_CFG_TPD) == 0) {
            irCmd.restoreDefaultConfig(CommonParams.DefaultConfigType.DEF_CFG_TPD)
            val result = irCmd.setTPDKtBtRecalPoint(
                CommonParams.TPDKtBtRecalPointType.RECAL_1_POINT,
                singlePointTemp
            )
            if (result == 0) {
                success = true
            } else {
                XLog.w("Single point calibration failed")
            }
        } else {
            XLog.w("Single point calibration failed")
        }
        return success
    }

    fun pointFirst(
        irCmd: IRCMD,
        pointTemp: Int,
    ): Boolean {
        var success = false
        if (irCmd.restoreDefaultConfig(CommonParams.DefaultConfigType.DEF_CFG_TPD) == 0) {
            val result = irCmd.setTPDKtBtRecalPoint(
                CommonParams.TPDKtBtRecalPointType.RECAL_2_POINT_FIRST,
                pointTemp + 273
            )
            if (result == 0) {
                success = true
            } else {
                XLog.w("[ph][ph][ph][ph][ph][ph]")
            }
        } else {
            XLog.w("[ph][ph][ph][ph][ph][ph]")
        }
        return success
    }

    fun pointEnd(
        irCmd: IRCMD,
        pointTemp: Int,
    ): Boolean {
        var success = false
        if (irCmd.restoreDefaultConfig(CommonParams.DefaultConfigType.DEF_CFG_TPD) == 0) {
            val result = irCmd.setTPDKtBtRecalPoint(
                CommonParams.TPDKtBtRecalPointType.RECAL_2_POINT_END,
                pointTemp + 273
            )
            if (result == 0) {
                success = true
            } else {
                Log.w("123", "[ph][ph]")
            }
        } else {
            Log.w("123", "[ph][ph]")
        }
        return success
    }

    fun potReady(irCmd: IRCMD): Boolean {
        return irCmd.rmCoverStsSwitch(CommonParams.RMCoverStsSwitchStatus.RMCOVER_DIS) == 0
    }

    fun potStart(
        irCmd: IRCMD,
        type: Int,
    ) {
        val gainType =
            when (type) {
                1 -> CommonParams.RMCoverAutoCalcType.GAIN_1
                2 -> CommonParams.RMCoverAutoCalcType.GAIN_2
                4 -> CommonParams.RMCoverAutoCalcType.GAIN_4
                else -> CommonParams.RMCoverAutoCalcType.GAIN_1
            }
        irCmd.rmCoverAutoCalc(gainType)
        irCmd.rmCoverStsSwitch(CommonParams.RMCoverStsSwitchStatus.RMCOVER_EN)
    }

    fun cancelCalibration(irCmd: IRCMD) {
        irCmd.restoreDefaultConfig(CommonParams.DefaultConfigType.DEF_CFG_TPD)
    }

    fun reset(irCmd: IRCMD) {
        irCmd.restoreDefaultConfig(CommonParams.DefaultConfigType.DEF_CFG_ALL)
    }

    fun queryGain(irCmd: IRCMD): Boolean {
        val value = IntArray(1)
        irCmd.getPropTPDParams(CommonParams.PropTPDParams.TPD_PROP_GAIN_SEL, value)
        return value[0] == 1
    }

    fun setGain(
        irCmd: IRCMD,
        type: Int,
    ) {
        if (type == 1) {
            irCmd.setPropTPDParams(
                CommonParams.PropTPDParams.TPD_PROP_GAIN_SEL,
                CommonParams.PropTPDParamsValue.GAINSELStatus.GAIN_SEL_HIGH,
            )
        } else {
            irCmd.setPropTPDParams(
                CommonParams.PropTPDParams.TPD_PROP_GAIN_SEL,
                CommonParams.PropTPDParamsValue.GAINSELStatus.GAIN_SEL_LOW
            )
        }
    }

    fun queryTpd(
        irCmd: IRCMD,
        params: CommonParams.PropTPDParams,
    ): Int {
        val value = IntArray(1)
        irCmd.getPropTPDParams(params, value)
        return value[0]
    }

    fun shutter(
        irCmd: IRCMD?,
        syncImage: SynchronizedBitmap,
    ) {
        if (syncImage.type == 1) {
            irCmd?.tc1bShutterManual()
        } else {
            irCmd?.updateOOCOrB(CommonParams.UpdateOOCOrBType.B_UPDATE)
        }
    }

    fun stsSwitch(
        irCmd: IRCMD?,
        flag: Boolean,
    ) {
        if (flag) {
            irCmd?.rmCoverStsSwitch(CommonParams.RMCoverStsSwitchStatus.RMCOVER_EN)
        } else {
            irCmd?.rmCoverStsSwitch(CommonParams.RMCoverStsSwitchStatus.RMCOVER_DIS)
        }
    }

    fun pot(
        irCmd: IRCMD,
        type: Int,
    ) {
        val gainType =
            when (type) {
                1 -> CommonParams.RMCoverAutoCalcType.GAIN_1
                2 -> CommonParams.RMCoverAutoCalcType.GAIN_2
                4 -> CommonParams.RMCoverAutoCalcType.GAIN_4
                else -> CommonParams.RMCoverAutoCalcType.GAIN_1
            }
        irCmd.rmCoverAutoCalc(gainType)
    }

    fun autoShutter(
        irCmd: IRCMD?,
        flag: Boolean,
    ) {
        val data =
            if (flag) CommonParams.PropAutoShutterParameterValue.StatusSwith.ON else CommonParams.PropAutoShutterParameterValue.StatusSwith.OFF
        irCmd?.setPropAutoShutterParameter(
            CommonParams.PropAutoShutterParameter.SHUTTER_PROP_SWITCH,
            data
        )
    }

    fun setTpdDis(
        irCmd: IRCMD?,
        value: Int,
    ) {
        val data = CommonParams.PropTPDParamsValue.NumberType(value.toString())
        setTpdParams(
            irCmd = irCmd,
            params = CommonParams.PropTPDParams.TPD_PROP_DISTANCE,
            value = data
        )
    }

    fun setTpdEms(
        irCmd: IRCMD?,
        value: Int,
    ) {
        val data = CommonParams.PropTPDParamsValue.NumberType(value.toString())
        setTpdParams(irCmd = irCmd, params = CommonParams.PropTPDParams.TPD_PROP_EMS, value = data)
    }

    private fun setTpdParams(
        irCmd: IRCMD?,
        params: CommonParams.PropTPDParams,
        value: CommonParams.PropTPDParamsValue,
    ): Int {
        return try {
            irCmd?.setPropTPDParams(params, value) ?: 0
        } catch (e: Exception) {
            XLog.w("[ph][ph][ph][ph][ph][ph][${params.name}]: ${e.message}")
            0
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\utils\ChartTools.kt =====

package com.mpdc4gsr.module.thermalunified.utils

import android.graphics.Point
import android.util.Log
import com.mpdc4gsr.libunified.ui.charts.LineChart
import kotlin.math.abs

object ChartTools {
    fun getLineTemps(
        point1: Point,
        point2: Point,
        tempArray: ByteArray,
        rotate: Int,
    ): List<Float> {
        val tempList: ArrayList<Float> = ArrayList()
        if (point1 == point2) {
            return tempList
        }
        val pointList: ArrayList<Point> = ArrayList()
        if (point1.x == point2.x) {
            val startY = point1.y.coerceAtMost(point2.y)
            val endY = point1.y.coerceAtLeast(point2.y)
            for (i in startY..endY) {
                pointList.add(Point(point1.x, i))
            }
        } else {
            val k = (point1.y - point2.y).toFloat() / (point1.x - point2.x).toFloat()
            val b = point1.y - k * point1.x
            if (abs(k) <= 1) {
                val startX = point1.x.coerceAtMost(point2.x)
                val endX = point1.x.coerceAtLeast(point2.x)
                for (i in startX..endX) {
                    pointList.add(Point(i, (k * i + b).toInt()))
                }
            } else {
                if (k >= 0) {
                    val startY = point1.y.coerceAtMost(point2.y)
                    val endY = point1.y.coerceAtLeast(point2.y)
                    for (y in startY..endY) {
                        pointList.add(Point(((y - b) / k).toInt(), y))
                    }
                } else {
                    val startY = point1.y.coerceAtLeast(point2.y)
                    val endY = point1.y.coerceAtMost(point2.y)
                    for (y in startY downTo endY) {
                        pointList.add(Point(((y - b) / k).toInt(), y))
                    }
                }
            }
        }
        val width = if (rotate == 90 || rotate == 270) 192 else 256
        pointList.forEach {
            val index = (it.y * width + it.x) * 2
            val tempInt =
                (tempArray[index + 1].toInt() shl 8 and 0xff00) or (tempArray[index].toInt() and 0xff)
            val tempValue = tempInt / 64f - 273.15f
            tempList.add(tempValue)
        }
        return tempList
    }

    fun scale(type: Int): Long {
        return when (type) {
            1 -> 1 * 1000
            2 -> 60 * 1000
            3 -> 60 * 60 * 1000
            4 -> 24 * 60 * 60 * 1000
            else -> 1
        }
    }

    fun getMinimum(type: Int): Float {
        val min =
            when (type) {
                1 -> 10f
                2 -> 10f
                3 -> 10f
                4 -> 10f
                else -> 1 * 10f
            }
        return min
    }

    fun getMaximum(type: Int): Float {
        return getMinimum(type) * 50f
    }

    fun setY(chart: LineChart) {
        var maxVol = 0f
        var minVol = 0f
        when (chart.data.dataSetCount) {
            1 -> {
                val dataSet = chart.data.getDataSetByIndex(0) ?: return
                maxVol = dataSet.yMax
                minVol = dataSet.yMin
            }

            2 -> {
                val dataSet1 = chart.data.getDataSetByIndex(0)
                val dataSet2 = chart.data.getDataSetByIndex(1)
                maxVol = if (dataSet1.yMax > dataSet2.yMax) dataSet1.yMax else dataSet2.yMax
                minVol = if (dataSet1.yMin < dataSet2.yMin) dataSet1.yMin else dataSet2.yMin
            }

            3 -> {
                val dataSet1 = chart.data.getDataSetByIndex(0)
                val dataSet2 = chart.data.getDataSetByIndex(1)
                val dataSet3 = chart.data.getDataSetByIndex(2)
                maxVol = if (dataSet1.yMax > dataSet2.yMax) dataSet1.yMax else dataSet2.yMax
                minVol = if (dataSet1.yMin < dataSet2.yMin) dataSet1.yMin else dataSet2.yMin
                maxVol = if (dataSet3.yMax > maxVol) dataSet3.yMax else maxVol
                minVol = if (dataSet3.yMin < minVol) dataSet3.yMin else minVol
            }

            else -> {
                return
            }
        }
        if (maxVol == minVol) {
            chart.axisLeft.axisMaximum = 50f
            chart.axisLeft.axisMinimum = 0f
        } else {
            if (maxVol - minVol < 0.5f) {
                chart.axisLeft.axisMaximum = (maxVol + minVol) / 2f + 0.3f
                chart.axisLeft.axisMinimum = (maxVol + minVol) / 2f - 0.3f
            } else {
                chart.axisLeft.axisMaximum = maxVol + (maxVol - minVol) * 0.15f
                chart.axisLeft.axisMinimum = minVol - (maxVol - minVol) * 0.15f
            }
        }
        Log.w("chart", "yAxis max:${chart.axisLeft.axisMaximum}, min:${chart.axisLeft.axisMinimum}")
    }

    fun setX(
        chart: LineChart,
        type: Int,
    ) {
        val xLen = chart.xChartMax - chart.xChartMin
        chart.xAxis.setLabelCount(getLabCount(xLen.toInt()), xLen <= 3)
    }

    private fun getLabCount(count: Int): Int {
        return when {
            count <= 2 -> 1
            count in 3..4 -> 2
            count in 5..7 -> 3
            count >= 8 -> 4
            else -> count
        }
    }

    fun getChartX(
        x: Long,
        startTime: Long,
        type: Int,
    ): Long {
        return (x - startTime) / scale(type)
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\utils\CommonUtils.kt =====

package com.mpdc4gsr.module.thermalunified.utils

import android.content.Context
import java.io.IOException

object CommonUtils {
    @Throws(IOException::class)
    fun getAssetData(context: Context, assetPath: String): ByteArray {
        return context.assets.open(assetPath).use { inputStream ->
            inputStream.readBytes()
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\utils\CompassCalUtils.kt =====

package com.mpdc4gsr.module.thermalunified.utils

import android.graphics.Paint
import android.graphics.Rect
import kotlin.math.roundToLong

fun realX(
    str: String,
    x: Float,
    paint: Paint,
) = x - textWidth(str, paint) / 2f

fun realY(
    str: String,
    y: Float,
    paint: Paint,
) = y - textHeight(str, paint) / 4f

fun textWidth(
    text: String,
    paint: Paint,
): Float {
    return textDimensions(text, paint).first
}

fun textHeight(
    text: String,
    paint: Paint,
): Float {
    return textDimensions(text, paint).second
}

val measurementRect = Rect()
fun textDimensions(
    text: String,
    paint: Paint,
): Pair<Float, Float> {
    paint.getTextBounds(text, 0, text.length, measurementRect)
    return measurementRect.width().toFloat() to measurementRect.height().toFloat()
}

fun getValuesBetween(
    min: Float,
    max: Float,
    divisor: Float,
): List<Float> {
    val values = mutableListOf<Float>()
    val start = min.roundNearest(divisor)
    var i = start
    while (i <= max) {
        if (i >= min) {
            values.add(i)
        }
        i += divisor
    }
    return values
}

fun Float.roundNearest(nearest: Float): Float {
    return (this / nearest).roundToLong() * nearest
}

fun getPixelLinear(
    bearing: Float,
    azimuth: Float,
    viewWidth: Float,
    fovWidth: Float,
): Float {
    val newBearing = deltaAngle(azimuth, bearing)
    val wPixelsPerDegree = viewWidth / fovWidth
    return viewWidth / 2f + newBearing * wPixelsPerDegree
}

fun deltaAngle(
    angle1: Float,
    angle2: Float,
): Float {
    val a = normalizeAngle(angle1 - angle2)
    val b = normalizeAngle(angle2 - angle1)
    return if (a < b) {
        -a
    } else {
        b
    }
}

fun normalizeAngle(angle: Float): Float {
    return wrap(angle, 0f, 360f) % 360
}

fun wrap(
    value: Float,
    min: Float,
    max: Float,
): Float {
    return wrap(value.toDouble(), min.toDouble(), max.toDouble()).toFloat()
}

fun wrap(
    value: Double,
    min: Double,
    max: Double,
): Double {
    val range = max - min
    if (value < min) {
        return max - (min - value) % range
    }
    if (value > max) {
        return min + (value - min) % range
    }
    return value
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\utils\DualParamsUtils.kt =====

package com.mpdc4gsr.module.thermalunified.utils

import com.energy.iruvc.utils.DualCameraParams
import com.mpdc4gsr.libunified.app.common.SaveSettingUtils

object DualParamsUtils {
    fun wifiFusionTypeToParams(fusionType: Int): Int {
        return when (fusionType) {
            SaveSettingUtils.FusionTypeVLOnly -> 3
            SaveSettingUtils.FusionTypeIROnlyNoFusion -> 1
            SaveSettingUtils.FusionTypeMeanFusion -> 4
            SaveSettingUtils.FusionTypeIROnly -> 0
            else -> 3
        }
    }

    fun fusionTypeToParams(fusionType: Int): DualCameraParams.FusionType {
        return when (fusionType) {
            SaveSettingUtils.FusionTypeVLOnly -> DualCameraParams.FusionType.VLOnly
            SaveSettingUtils.FusionTypeIROnlyNoFusion -> DualCameraParams.FusionType.IROnlyNoFusion
            SaveSettingUtils.FusionTypeScreenFusion -> DualCameraParams.FusionType.ScreenFusion
            SaveSettingUtils.FusionTypeHSLFusion -> DualCameraParams.FusionType.HSLFusion
            SaveSettingUtils.FusionTypeMeanFusion -> DualCameraParams.FusionType.MeanFusion
            SaveSettingUtils.FusionTypeLPYFusion -> DualCameraParams.FusionType.LPYFusion
            SaveSettingUtils.FusionTypeIROnly -> DualCameraParams.FusionType.IROnly
            else -> DualCameraParams.FusionType.LPYFusion
        }
    }

    fun paramsToFusionType(fusionTypeP: DualCameraParams.FusionType): Int {
        return when (fusionTypeP) {
            DualCameraParams.FusionType.VLOnly -> SaveSettingUtils.FusionTypeVLOnly
            DualCameraParams.FusionType.IROnlyNoFusion -> SaveSettingUtils.FusionTypeIROnlyNoFusion
            DualCameraParams.FusionType.ScreenFusion -> SaveSettingUtils.FusionTypeScreenFusion
            DualCameraParams.FusionType.HSLFusion -> SaveSettingUtils.FusionTypeHSLFusion
            DualCameraParams.FusionType.MeanFusion -> SaveSettingUtils.FusionTypeMeanFusion
            DualCameraParams.FusionType.LPYFusion -> SaveSettingUtils.FusionTypeLPYFusion
            DualCameraParams.FusionType.IROnly -> SaveSettingUtils.FusionTypeIROnly
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\utils\IRCmdTools.kt =====

package com.mpdc4gsr.module.thermalunified.utils

import android.util.Log
import com.elvishew.xlog.XLog
import com.energy.iruvc.dual.DualUVCCamera
import com.energy.iruvc.ircmd.IRCMD
import com.energy.iruvc.utils.CommonParams
import com.energy.iruvc.utils.DualCameraParams
import com.energy.iruvc.utils.SynchronizedBitmap
import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.ir.usbdual.camera.BaseDualView
import com.mpdc4gsr.libunified.ir.utils.HexDump
import com.mpdc4gsr.module.thermalunified.compat.ContextProvider
import java.io.IOException
import java.io.InputStream
import kotlin.math.ceil
import kotlin.math.floor

object IRCmdTools {
    val TAG = "IRCmdTool"
    var dispNumber = 30
    fun getDualBytes(irCmd: IRCMD?): ByteArray {
        val calibrationDataSize = 192
        val INIT_ALIGN_DATA = floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f)
        val oemInfo = ByteArray(512)
        val snData = ByteArray(256)
        val dispData = ByteArray(5)
        irCmd?.oemRead(CommonParams.ProductType.P2, oemInfo)
        XLog.w("[ph][ph][ph][ph][ph][ph][ph][ph]", "[ph][ph][ph][ph][ph][ph]:")
        val calibrationData = ByteArray(calibrationDataSize)
        val productTypeData = ByteArray(2)
        System.arraycopy(oemInfo, 0, calibrationData, 0, calibrationData.size)
        System.arraycopy(oemInfo, calibrationDataSize, productTypeData, 0, productTypeData.size)
        System.arraycopy(
            oemInfo,
            calibrationDataSize + productTypeData.size,
            dispData,
            0,
            dispData.size,
        )
        System.arraycopy(oemInfo, 256, snData, 0, snData.size)
        try {
            var str = String(dispData)
            str = str.replace(Regex("[^-\\d]"), "")
            dispNumber = str.toInt()
            if (dispNumber > 60) {
                dispNumber = dispNumber / 10
            }
            if (dispNumber < -20) {
                dispNumber = -20
            }
            XLog.w("[ph][ph][ph][ph]:", "" + dispNumber)
        } catch (e: Exception) {
            XLog.w("[ph][ph][ph][ph][ph][ph]")
        }
        val snList = String(snData).split(";")
        val snStr =
            if (snList.isNotEmpty() && snList[0].contains("sn", true)) {
                snList[0].replace("SN:", "")
            } else {
                ""
            }
        val parameters = ByteArray(calibrationDataSize + 1 + 24)
        if (String(productTypeData) == "TD") {
            System.arraycopy(calibrationData, 0, parameters, 0, calibrationData.size)
            parameters[calibrationDataSize] = 1
            val alignByte = SharedManager.getManualData(snStr)
            System.arraycopy(alignByte, 0, parameters, calibrationDataSize + 1, alignByte.size)
        } else {
            val am = ContextProvider.getContext().assets
            var `is`: InputStream? = null
            val length: Int
            try {
                `is` = am.open("dual_calibration_parameters2.bin")
                length = `is`.available()
                if (`is`.read(parameters) != length) {
                    Log.e(TAG, "read file fail ")
                }
                parameters[length] = 1
                val alignByte = SharedManager.getManualData(snStr)
                System.arraycopy(alignByte, 0, parameters, calibrationDataSize + 1, alignByte.size)
                XLog.w("[ph][ph][ph][ph][ph][ph][ph][ph][ph]ï¼Œ[ph][ph][ph][ph][ph][ph][ph]")
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                try {
                    `is`?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return parameters
    }

    fun getSNStr(irCmd: IRCMD?): String {
        val oemInfo = ByteArray(512)
        irCmd?.oemRead(CommonParams.ProductType.P2, oemInfo)
        val snData = ByteArray(256)
        System.arraycopy(oemInfo, 256, snData, 0, snData.size)
        val snList = String(snData).split(";")
        return if (snList.isNotEmpty() && snList[0].contains("sn", true)) {
            snList[0].replace("SN:", "")
        } else {
            ""
        }
    }

    fun setTpdEms(
        irCmd: IRCMD?,
        value: Int,
    ) {
        val data = CommonParams.PropTPDParamsValue.NumberType(value.toString())
        setTpdParams(irCmd = irCmd, params = CommonParams.PropTPDParams.TPD_PROP_EMS, value = data)
    }

    fun setTpdDis(
        irCmd: IRCMD?,
        value: Int,
    ) {
        val data = CommonParams.PropTPDParamsValue.NumberType(value.toString())
        setTpdParams(
            irCmd = irCmd,
            params = CommonParams.PropTPDParams.TPD_PROP_DISTANCE,
            value = data
        )
    }

    fun setLevelContrast(
        irCmd: IRCMD?,
        value: Int,
    ) {
        val data = CommonParams.PropImageParamsValue.NumberType(value.toString())
        setImageParams(
            irCmd = irCmd,
            params = CommonParams.PropImageParams.IMAGE_PROP_LEVEL_CONTRAST,
            value = data
        )
    }

    fun setLevelDdd(
        irCmd: IRCMD?,
        value: Int,
    ) {
        val data =
            when (value) {
                0 -> CommonParams.PropImageParamsValue.DDEType.DDE_0
                1 -> CommonParams.PropImageParamsValue.DDEType.DDE_1
                2 -> CommonParams.PropImageParamsValue.DDEType.DDE_2
                3 -> CommonParams.PropImageParamsValue.DDEType.DDE_3
                4 -> CommonParams.PropImageParamsValue.DDEType.DDE_4
                else -> CommonParams.PropImageParamsValue.DDEType.DDE_0
            }
        setImageParams(
            irCmd = irCmd,
            params = CommonParams.PropImageParams.IMAGE_PROP_LEVEL_DDE,
            value = data
        )
    }

    fun setLevelAgc(
        irCmd: IRCMD?,
        value: Boolean,
    ) {
        val data =
            if (value) {
                CommonParams.PropImageParamsValue.StatusSwith.ON
            } else {
                CommonParams.PropImageParamsValue.StatusSwith.OFF
            }
        setImageParams(
            irCmd = irCmd,
            params = CommonParams.PropImageParams.IMAGE_PROP_ONOFF_AGC,
            value = data
        )
    }

    fun getTpdGainSel(irCmd: IRCMD?): Int {
        val result =
            queryTpdParam(irCmd = irCmd, params = CommonParams.PropTPDParams.TPD_PROP_GAIN_SEL)
        return if (result == CommonParams.PropTPDParamsValue.GAINSELStatus.GAIN_SEL_HIGH.value) {
            1
        } else {
            0
        }
    }

    fun setTpdGainSel(
        irCmd: IRCMD?,
        value: Int,
    ): Int {
        val data =
            if (value == 1) {
                CommonParams.PropTPDParamsValue.GAINSELStatus.GAIN_SEL_HIGH
            } else {
                CommonParams.PropTPDParamsValue.GAINSELStatus.GAIN_SEL_LOW
            }
        return setTpdParams(
            irCmd = irCmd,
            params = CommonParams.PropTPDParams.TPD_PROP_GAIN_SEL,
            value = data
        )
    }

    fun queryTpdParam(
        irCmd: IRCMD?,
        params: CommonParams.PropTPDParams,
    ): Int {
        val value = IntArray(1)
        irCmd?.getPropTPDParams(params, value)
        return value[0]
    }

    fun queryImageParam(
        irCmd: IRCMD?,
        params: CommonParams.PropImageParams,
    ): Int {
        val value = IntArray(1)
        irCmd?.getPropImageParams(params, value)
        return value[0]
    }

    private fun setTpdParams(
        irCmd: IRCMD?,
        params: CommonParams.PropTPDParams,
        value: CommonParams.PropTPDParamsValue,
    ): Int {
        return try {
            irCmd?.setPropTPDParams(params, value) ?: 0
        } catch (e: Exception) {
            XLog.w("[ph][ph][ph][ph][ph][ph][${params.name}]: ${e.message}")
            0
        }
    }

    private fun setImageParams(
        irCmd: IRCMD?,
        params: CommonParams.PropImageParams,
        value: CommonParams.PropImageParamsValue,
    ): Int {
        return try {
            irCmd?.setPropImageParams(params, value) ?: 0
        } catch (e: Exception) {
            XLog.w("[ph][ph][ph][ph][ph][ph][${params.name}]: ${e.message}")
            0
        }
    }

    fun setDisp(
        dualView: BaseDualView?,
        value: Int,
    ): Int {
        return try {
            if (dualView != null) {
                dualView?.dualUVCCamera!!.setDisp(value)
                0
            } else {
                -1
            }
        } catch (e: Exception) {
            XLog.w("[ph][ph][ph][ph][ph][ph][$value]: ${e.message}")
            0
        }
    }

    fun setAlignTranslate(
        dualView: BaseDualView?,
        moveX: Int,
        moveY: Int,
    ) {
        val newSrc = ByteArray(8)
        val xSrc = ByteArray(4)
        HexDump.float2byte(moveX.toFloat(), xSrc)
        System.arraycopy(xSrc, 0, newSrc, 0, 4)
        val ySrc = ByteArray(4)
        HexDump.float2byte(moveY.toFloat(), ySrc)
        System.arraycopy(ySrc, 0, newSrc, 4, 4)
        dualView?.dualUVCCamera?.setAlignTranslateParameter(newSrc)
    }

    fun shutter(
        irCmd: IRCMD?,
        syncImage: SynchronizedBitmap,
    ) {
        if (syncImage.type == 1) {
            irCmd?.tc1bShutterManual()
        } else {
            irCmd?.updateOOCOrB(CommonParams.UpdateOOCOrBType.B_UPDATE)
        }
    }

    fun autoShutter(
        irCmd: IRCMD?,
        flag: Boolean,
    ) {
        val data =
            if (flag) CommonParams.PropAutoShutterParameterValue.StatusSwith.ON else CommonParams.PropAutoShutterParameterValue.StatusSwith.OFF
        irCmd?.setPropAutoShutterParameter(
            CommonParams.PropAutoShutterParameter.SHUTTER_PROP_SWITCH,
            data
        )
    }

    fun setIsoColorOpen(
        dualUVCCamera: DualUVCCamera?,
        highC: Float,
        lowC: Float,
    ) {
        dualUVCCamera?.setIsothermal(DualCameraParams.IsothermalState.ON)
        val normalHighTemp = (highC + 273).toDouble()
        val normalLowTemp = (lowC + 273).toDouble()
        val highTemp = ceil(normalHighTemp * 16 * 4).toInt()
        val lowTemp = floor(normalLowTemp * 16 * 4).toInt()
        val highData = ByteArray(2)
        highData[0] = highTemp.toByte()
        highData[1] = (highTemp shr 8).toByte()
        val lowData = ByteArray(2)
        lowData[0] = lowTemp.toByte()
        lowData[1] = (lowTemp shr 8).toByte()
        val tempHFin = (highData[0].toInt() and 0x00ff) + (highData[1].toInt() and 0x00ff shl 8)
        val tempLFin = (lowData[0].toInt() and 0x00ff) + (lowData[1].toInt() and 0x00ff shl 8)
        dualUVCCamera?.setTempL(tempLFin)
        dualUVCCamera?.setTempH(tempHFin)
    }

    fun setIsoColorClose(dualUVCCamera: DualUVCCamera?) {
        dualUVCCamera?.setIsothermal(DualCameraParams.IsothermalState.OFF)
    }

    fun setZoomUp(irCmd: IRCMD?) {
        irCmd?.zoomCenterUp(
            CommonParams.PreviewPathChannel.PREVIEW_PATH0,
            CommonParams.ZoomScaleStep.ZOOM_STEP2
        )
    }

    fun setZoomDown(irCmd: IRCMD?) {
        irCmd?.zoomCenterDown(
            CommonParams.PreviewPathChannel.PREVIEW_PATH0,
            CommonParams.ZoomScaleStep.ZOOM_STEP2
        )
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\utils\IRConfigData.kt =====

package com.mpdc4gsr.module.thermalunified.utils

import android.content.Context
import com.mpdc4gsr.libunified.R as LibcoreR

data class IRConfigData(val name: String, val value: String) {
    companion object {
        fun irConfigData(context: Context): ArrayList<IRConfigData> =
            arrayListOf(
                IRConfigData(
                    name = context.resources.getString(LibcoreR.string.reference_item1),
                    value = "0.95"
                ),
                IRConfigData(
                    name = context.resources.getString(LibcoreR.string.reference_item2),
                    value = "0.94"
                ),
                IRConfigData(
                    name = context.resources.getString(LibcoreR.string.reference_item3),
                    value = "0.75"
                ),
                IRConfigData(
                    name = context.resources.getString(LibcoreR.string.reference_item4),
                    value = "0.98"
                ),
                IRConfigData(
                    name = context.resources.getString(LibcoreR.string.reference_item5),
                    value = "0.95"
                ),
                IRConfigData(
                    name = context.resources.getString(LibcoreR.string.reference_item6),
                    value = "0.95"
                ),
                IRConfigData(
                    name = context.resources.getString(LibcoreR.string.reference_item7),
                    value = "0.95"
                ),
                IRConfigData(
                    name = context.resources.getString(LibcoreR.string.reference_item8),
                    value = "0.90"
                ),
                IRConfigData(
                    name = context.resources.getString(LibcoreR.string.reference_item9),
                    value = "0.85"
                ),
            )

        fun getTextByEmissivity(
            context: Context,
            emissivity: Float,
        ): String {
            val stringBuilder = StringBuilder()
            for (data in irConfigData(context)) {
                if (emissivity.toString() == data.value) {
                    if (stringBuilder.isEmpty()) {
                        stringBuilder.append(context.getString(LibcoreR.string.tc_temp_test_materials))
                            .append(" : ")
                    }
                    stringBuilder.append(data.name).append("/")
                }
            }
            if (stringBuilder.isNotEmpty()) {
                stringBuilder.deleteCharAt(stringBuilder.length - 1)
            }
            return stringBuilder.toString()
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\utils\IrConst.kt =====

package com.mpdc4gsr.module.thermalunified.utils

object IrConst {
    const val TAU_HIGH_GAIN_ASSET_PATH = "lite/highF.bin"
    const val TAU_LOW_GAIN_ASSET_PATH = "lite/lowF.bin"
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\utils\IRImageUtils.kt =====

package com.mpdc4gsr.module.thermalunified.utils

import android.graphics.Bitmap
import android.util.Log
import android.widget.ImageView
import androidx.annotation.FloatRange
import org.bytedeco.opencv.global.opencv_core.BORDER_DEFAULT
import org.bytedeco.opencv.global.opencv_core.CV_16S
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.pow
import com.mpdc4gsr.libunified.R as UiR

object IRImageUtils {
    fun showContrast(
        imageView: ImageView,
        @FloatRange(from = 0.0, to = 2.0) contrast: Double,
        @FloatRange(from = -255.0, to = 255.0) brightness: Double,
    ) {
        try {
            val lookUpTable = Mat(1, 256, CvType.CV_8U)
            val lookUpTableData = ByteArray((lookUpTable.total() * lookUpTable.channels()).toInt())
            Log.w("123", "lookUpTableData: ${lookUpTableData.size}")
            Log.w("123", "contrast: $contrast")
            for (i in 0 until lookUpTable.cols()) {
                if (i % 10 == 0) {
                    Log.i(
                        "123",
                        "$i, lutGamma x: ${i / 255.0}, ${
                            lutGamma(
                                x = i / 255.0,
                                gamma = contrast
                            ) * 255.0
                        }"
                    )
                }
                lookUpTableData[i] =
                    (lutGamma(x = i / 255.0, gamma = contrast) * 255.0).toInt().toByte()
            }
            Log.w("123", "lookUpTableData: ${lookUpTableData[1].toUByte()}")
            lookUpTable.put(0, 0, lookUpTableData)
            val srcMat = Utils.loadResource(
                com.mpdc4gsr.module.thermalunified.compat.ContextProvider.getContext(),
                UiR.drawable.ic_main_menu_battery
            )
            val dstMat = Mat()
            Core.LUT(srcMat, lookUpTable, dstMat)
            Core.add(dstMat, Scalar(brightness, brightness, brightness), dstMat)
            val resultMat = Mat()
            Imgproc.cvtColor(dstMat, resultMat, Imgproc.COLOR_BGR2RGBA)
            val bitmap = Bitmap.createBitmap(
                resultMat.size().width.toInt(),
                resultMat.size().height.toInt(),
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(resultMat, bitmap)
            imageView.setImageBitmap(bitmap)
            srcMat.release()
            dstMat.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun lutGamma(
        @FloatRange(from = 0.0, to = 1.0) x: Double,
        a: Double = 0.5,
        gamma: Double,
    ): Double {
        val y =
            if (x <= a) {
                a - a * ((1 - x / a).pow(gamma))
            } else {
                a + (1 - a) * (((x - a) / (1 - a)).pow(gamma))
            }
        return y
    }

    private fun showSharpen(
        imageView: ImageView,
        @FloatRange(from = 0.0, to = 2.55) sharpen: Double,
    ) {
        Log.i("123", "show sharpen: $sharpen")
        val scale = 1.0
        val delta = 0.0
        val kernelSize = 3
        val srcMat = Utils.loadResource(
            com.mpdc4gsr.module.thermalunified.compat.ContextProvider.getContext(),
            UiR.drawable.ic_main_menu_battery
        )
        val dstMat = Mat(srcMat.rows(), srcMat.cols(), srcMat.type())
        val preGray = Mat()
        val absDst = Mat()
        Log.i("123", "start kernel_size: $kernelSize")
        Imgproc.GaussianBlur(srcMat, srcMat, Size(3.0, 3.0), 0.0, 0.0, BORDER_DEFAULT)
        Imgproc.cvtColor(srcMat, preGray, Imgproc.COLOR_BGR2GRAY)
        Log.w("123", "cvtColor preGray: $preGray")
        Imgproc.Laplacian(srcMat, dstMat, CV_16S, kernelSize, scale, delta, BORDER_DEFAULT)
        Log.w("123", "Laplacian dstMat: $dstMat")
        Core.convertScaleAbs(dstMat, absDst)
        Log.w("123", "convertScaleAbs absDst: $absDst")
        val preMat = Mat()
        Core.addWeighted(srcMat, 1.0, absDst, sharpen, 0.0, preMat)
        Imgproc.cvtColor(preMat, dstMat, Imgproc.COLOR_BGR2RGBA)
        val bitmap = Bitmap.createBitmap(
            dstMat.size().width.toInt(),
            dstMat.size().height.toInt(),
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(dstMat, bitmap)
        imageView.setImageBitmap(bitmap)
        srcMat.release()
        dstMat.release()
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\utils\WriteTools.kt =====

package com.mpdc4gsr.module.thermalunified.utils

import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.elvishew.xlog.XLog
import com.mpdc4gsr.libunified.app.tools.FileTools
import com.mpdc4gsr.module.thermalunified.compat.ContextProvider
import java.io.File

object WriteTools {
    fun delete(file: File): Int {
        val uri: Uri = FileTools.getUri(file)
        XLog.w("[ph][ph][ph][ph] uri:$uri")
        val mediaId = queryId(uri)
        val resolver = ContextProvider.getContext().contentResolver
        val selection = "${MediaStore.Images.Media._ID} = ?"
        val selectionArgs = arrayOf(mediaId.toString())
        val result = resolver.delete(uri, selection, selectionArgs)
        XLog.w("[ph][ph][ph][ph][ph][ph]: $result")
        return result
    }

    private fun queryId(uri: Uri): Long {
        val fileName = uri.path!!.substring(uri.path!!.lastIndexOf("/") + 1)
        var result = 0L
        var cursor: Cursor? = null
        try {
            val resolver = ContextProvider.getContext().contentResolver
            cursor =
                resolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    null,
                    "${MediaStore.Images.Media.DISPLAY_NAME}=?",
                    arrayOf(fileName),
                    null,
                )
            cursor?.let {
                if (it.moveToFirst()) {
                    result = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                    XLog.w("MediaStore.Images.Media._ID: $result")
                }
            }
        } catch (e: Exception) {
            XLog.e("[ph][ph][ph][ph]: ${e.message}")
        } finally {
            cursor?.close()
        }
        return result
    }
}


