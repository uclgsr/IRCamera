package com.mpdc4gsr.libunified.app.tools

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

object NumberTools {
    fun to01(float: Float): String = String.format(Locale.ENGLISH, "%.1f", float)

    fun to01f(float: Float): Float = to01(float).toFloat()

    fun to02(float: Float): String = String.format(Locale.ENGLISH, "%.2f", float)

    fun to02f(float: Float): Float = to02(float).toFloat()

    fun scale(
        value: Float,
        newScale: Int,
    ): Float = BigDecimal(value.toDouble()).setScale(newScale, RoundingMode.HALF_UP).toFloat()
}
