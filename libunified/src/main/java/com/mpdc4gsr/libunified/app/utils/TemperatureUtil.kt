package com.mpdc4gsr.libunified.app.utils

import com.mpdc4gsr.libunified.app.common.SharedManager

object TemperatureUtil {
    fun getTempStr(
        min: Int,
        max: Int,
    ): String =
        if (SharedManager.getTemperature() == 1) {
            "$min°C~$max°C"
        } else {
            "${(min * 1.8 + 32).toInt()}°F~${(max * 1.8 + 32).toInt()}°F"
        }
}
