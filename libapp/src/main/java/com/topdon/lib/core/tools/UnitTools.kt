package com.topdon.lib.core.tools

import com.topdon.lib.core.common.SharedManager
import java.util.*

object UnitTools {

    @JvmStatic
    fun showC(float: Float): String {
    val str =
    if (SharedManager.getTemperature() == 1) {
    // 温度
    "${String.format(Locale.ENGLISH, "%.1f", float)}°C"
    } else {
    // 华氏度
    "${String.format(Locale.ENGLISH, "%.1f", (float * 1.8000 + 32.00))}°F"
    }
    return str
    }


    @JvmStatic
    fun showC(
    float: Float,
    isC: Boolean,
    ): String {
    val str =
    if (isC) {
    // 温度
    "${String.format(Locale.ENGLISH, "%.1f", float)}°C"
    } else {
    // 华氏度
    "${String.format(Locale.ENGLISH, "%.1f", (float * 1.8000 + 32.00))}°F"
    }
    return str
    }


    @JvmStatic
    fun showIntervalC(
    min: Int,
    max: Int,
    ): String {
    val str =
    if (SharedManager.getTemperature() == 1) {
    // 温度
    "$min~$max°C"
    } else {
    // 华氏度
    val maxT: Int = (max * 1.8000 + 32.00).toInt()
    val minT: Int = (min * 1.8000 + 32.00).toInt()
    "$minT~$maxT°F"
    }
    return str
    }


    @JvmStatic
    fun showConfigC(
    min: Int,
    max: Int,
    ): String {
    val str =
    if (SharedManager.getTemperature() == 1) {
    // 温度
    "($min~$max°C)"
    } else {
    // 华氏度
    val maxT: Int = (max * 1.8000 + 32.00).toInt()
    val minT: Int = (min * 1.8000 + 32.00).toInt()
    "($minT~$maxT°F)"
    }
    return str
    }


    @JvmStatic
    fun showUnit(): String {
    val str =
    if (SharedManager.getTemperature() == 1) {
    // 温度
    "°C"
    } else {
    // 华氏度
    "°F"
    }
    return str
    }


    @JvmStatic
    fun showUnitValue(value: Float): Float {
    val str =
    if (SharedManager.getTemperature() == 1) {
    // 温度
    value
    } else {
    // 华氏度
    toF(value)
    }
    return str.toFloat()
    }


    @JvmStatic
    fun showUnitValue(
    value: Float,
    showC: Boolean,
    ): Float {
        if (value == Float.MAX_VALUE || value == Float.MIN_VALUE) {
            return value
        }
        val str =
            if (showC) {
                // 温度
                value
            } else {
                // 华氏度
                toF(value)
            }
        return str.toFloat()
    }


    @JvmStatic
    fun showToCValue(
    value: Float,
    isShowC: Boolean,
    ): Float {
    val str =
    if (isShowC) {
    // 温度
    value
    } else {
    // 华氏度
    toC(value)
    }
    return str.toFloat()
    }


    @JvmStatic
    fun showToCValue(value: Float): Float {
    val str =
    if (SharedManager.getTemperature() == 1) {
    // 温度
    value
    } else {
    // 华氏度
    toC(value)
    }
    return str.toFloat()
    }


    fun toF(value: Float): Float {
    return value * 1.8000f + 32.00f
    }


    fun toC(value: Float): Float {
    return (value - 32.0f) / 1.8000f
    }


    @JvmStatic
    fun showNoUnit(float: Float): String {
    val str =
    if (SharedManager.getTemperature() == 1) { // 摄氏度
    String.format(Locale.ENGLISH, "%.1f", float)
    } else {
    String.format(Locale.ENGLISH, "%.1f", (float * 1.8000 + 32.00))
    }
    return if (str.endsWith(".0")) str.substring(0, str.length - 2) else str
    }


    @JvmStatic
    fun showWithUnit(float: Float): String {
    val str =
    if (SharedManager.getTemperature() == 1) { // 摄氏度
    String.format(Locale.ENGLISH, "%.1f", float)
    } else {
    String.format(Locale.ENGLISH, "%.1f", (float * 1.8000 + 32.00))
    }
    return (if (str.endsWith(".0")) str.substring(0, str.length - 2) else str) + showUnit()
    }
}
