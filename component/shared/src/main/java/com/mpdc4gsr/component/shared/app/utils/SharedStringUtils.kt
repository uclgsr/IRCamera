package com.mpdc4gsr.component.shared.app.utils

import android.content.Context
import java.util.*

object SharedStringUtils {
    fun randomUuid(): String = UUID.randomUUID().toString().replace("-", "")

    fun fillZero(
        src: String?,
        targetLen: Int,
        head: Boolean,
    ): String? {
        if (src == null) return null
        val sb = StringBuilder(src)
        while (sb.length < targetLen) {
            if (head) {
                sb.insert(0, "0")
            } else {
                sb.append("0")
            }
        }
        return sb.toString()
    }

    fun getResString(
        context: Context,
        resId: Int,
    ): String =
        try {
            context.getString(resId)
        } catch (e: Exception) {
            ""
        }

    fun isEmpty(str: String?): Boolean = str == null || str.trim().isEmpty()

    fun isNotEmpty(str: String?): Boolean = !isEmpty(str)

    fun isBlank(str: String?): Boolean = str == null || str.trim().isEmpty()

    fun createFileName(timeStr: String): String = "_$timeStr"

    fun dateString(date: String): String {
        if (date.length < 8) return date
        val year = date.substring(0, 4)
        val month = date.substring(4, 6)
        val day = date.substring(6, 8)
        return "$year-$month-$day"
    }

    fun equals(
        a: CharSequence?,
        b: CharSequence?,
    ): Boolean {
        if (a === b) return true
        if (a != null && b != null && a.length == b.length) {
            if (a is String && b is String) {
                return a == b
            }
            for (i in a.indices) {
                if (a[i] != b[i]) return false
            }
            return true
        }
        return false
    }
}



