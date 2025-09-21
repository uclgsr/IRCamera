package com.mpdc4gsr.commons.util

import com.mpdc4gsr.lib.core.sdk.utils.LanguageUtil

object TimeGMTUtils {
    private fun isDaylight(zone: java.util.TimeZone, time: kotlin.String): kotlin.Boolean {
        try {
            @android.annotation.SuppressLint("SimpleDateFormat") val sf =
                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val d1 = sf.parse(time)
            return zone.useDaylightTime() && zone.inDaylightTime(d1)
        } catch (e: java.text.ParseException) {
            e.printStackTrace()
        }
        return false
    }

    fun getGMTConvertTime(time: kotlin.String?, format: kotlin.String?): kotlin.String {
        try {
            if (android.text.TextUtils.isEmpty(time)) {
                return ""
            }
            val longTime = TimeGMTUtils.getStringToDate(time!!, "GMT+00:00", "yyyy-MM-dd HH:mm:ss")
            val curLocale: java.util.Locale? = LanguageUtil.getSystemLocal()
            val gmt = java.util.TimeZone.getDefault().getDisplayName(
                TimeGMTUtils.isDaylight(java.util.TimeZone.getDefault(), time),
                java.util.TimeZone.SHORT,
                curLocale
            )

            return TimeGMTUtils.getDateToString(longTime, gmt, format)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return ""
    }

    fun getDateToString(milSecond: kotlin.Long, gmt: kotlin.String?, pattern: kotlin.String?): kotlin.String {
        val date = java.util.Date(milSecond)
        val format = java.text.SimpleDateFormat(pattern)
        val timeZone = java.util.TimeZone.getTimeZone(gmt)
        format.setTimeZone(timeZone)
        return format.format(date)
    }

    fun getStringToDate(dateString: kotlin.String, gmt: kotlin.String?, pattern: kotlin.String?): kotlin.Long {
        val dateFormat = java.text.SimpleDateFormat(pattern)
        var date: java.util.Date? = java.util.Date()
        try {
            val timeZone = java.util.TimeZone.getTimeZone(gmt)
            dateFormat.setTimeZone(timeZone)
            date = dateFormat.parse(dateString)
        } catch (e: java.text.ParseException) {
            e.printStackTrace()
        }
        return date!!.getTime()
    }
}
