// Merged ALL .kt and .java files from the 'component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\chart' directory and its subdirectories.
// Total files: 3 | Generated on: 2025-10-08 01:42:35


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\chart\IRMyValueFormatter.kt =====

package com.mpdc4gsr.module.thermalunified.chart

import android.annotation.SuppressLint
import com.mpdc4gsr.libunified.ui.formatter.IndexAxisValueFormatter
import com.mpdc4gsr.module.thermalunified.utils.ChartTools
import java.text.SimpleDateFormat
import java.util.*

class IRMyValueFormatter(private val startTime: Long, private val type: Int = 1) :
    IndexAxisValueFormatter() {
    companion object {
        const val TYPE_TIME_SECOND = 1
        const val TYPE_TIME_MINUTE = 2
        const val TYPE_TIME_HOUR = 3
        const val TYPE_TIME_DAY = 4
    }

    override fun getFormattedValue(value: Float): String {
        val time =
            if (value.toLong() % 1000 == 999L) {
                value.toLong() + 1L
            } else {
                value.toLong()
            }
        val realTime = startTime + time * ChartTools.scale(type)
        return showDateSecond(realTime)
    }

    @SuppressLint("SimpleDateFormat")
    fun showDateSecond(time: Long): String {
        val date = Date(time)
        val pattern =
            when (type) {
                TYPE_TIME_SECOND -> "HH:mm:ss"
                TYPE_TIME_MINUTE -> "HH:mm"
                TYPE_TIME_HOUR -> "HH:00"
                TYPE_TIME_DAY -> "MM-dd"
                else -> "HH:mm:ss"
            }
        val dateFormat = SimpleDateFormat(pattern)
        val timeZone =
            TimeZone.getTimeZone(TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT))
        dateFormat.timeZone = timeZone
        return dateFormat.format(date)
    }
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\chart\MyValueFormatter.kt =====

package com.mpdc4gsr.module.thermalunified.chart

import android.annotation.SuppressLint
import com.mpdc4gsr.libunified.ui.components.AxisBase
import com.mpdc4gsr.libunified.ui.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.*

class MyValueFormatter(private val startTime: Long, private val type: Int = 1) :
    IndexAxisValueFormatter() {
    companion object {
        const val TYPE_TIME_SECOND = 1
        const val TYPE_TIME_MINUTE = 2
        const val TYPE_TIME_HOUR = 3
        const val TYPE_TIME_DAY = 4
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getFormattedValue(
        value: Float,
        axis: AxisBase?,
    ): String {
        val time = startTime + value.toLong()
        return showDateSecond(time)
    }

    @SuppressLint("SimpleDateFormat")
    fun showDateSecond(time: Long): String {
        val date = Date(time)
        val pattern =
            when (type) {
                TYPE_TIME_SECOND -> "HH:mm:ss"
                TYPE_TIME_MINUTE -> "HH:mm"
                TYPE_TIME_HOUR -> "HH:00"
                TYPE_TIME_DAY -> "yy-MM-dd"
                else -> "HH:mm:ss"
            }
        val dateFormat = SimpleDateFormat(pattern)
        val timeZone = TimeZone.getDefault()
        dateFormat.timeZone = timeZone
        return dateFormat.format(date)
    }
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\chart\YValueFormatter.kt =====

package com.mpdc4gsr.module.thermalunified.chart

import com.mpdc4gsr.libunified.app.tools.UnitTools
import com.mpdc4gsr.libunified.ui.formatter.IndexAxisValueFormatter

class YValueFormatter : IndexAxisValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        return try {
            String.format("%.1f", value)
            UnitTools.showC(value)
        } catch (e: Exception) {
            UnitTools.showC(value)
        }
    }
}