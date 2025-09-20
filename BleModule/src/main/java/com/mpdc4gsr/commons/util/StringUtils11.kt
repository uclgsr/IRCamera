package com.mpdc4gsr.commons.util

import android.text.TextUtils
import java.lang.Long
import java.util.Locale
import java.util.UUID
import kotlin.Boolean
import kotlin.ByteArray
import kotlin.Int
import kotlin.String

object StringUtils {
    fun randomUuid(): String {
        return UUID.randomUUID().toString().replace("-", "")
    }

    fun fillZero(src: String?, targetLen: Int, head: Boolean): String? {
        if (src == null) return null
        val sb = StringBuilder(src)
        while (sb.length % targetLen != 0) {
            if (head) {
                sb.insert(0, "0")
            } else {
                sb.append("0")
            }
        }
        return sb.toString()
    }

    fun toHex(num: Int): String? {
        return fillZero(Integer.toHexString(num), 2, true)
    }

    fun toHex(num: Long): String? {
        return fillZero(Long.toHexString(num), 2, true)
    }

    fun toBinary(num: Int): String? {
        return fillZero(Integer.toBinaryString(num), 8, true)
    }

    fun toBinary(num: kotlin.Long): String? {
        return fillZero(Long.toBinaryString(num), 8, true)
    }

    fun toHex(bytes: ByteArray?): String? {
        return toHex(bytes, " ")
    }

    fun toHex(bytes: ByteArray?, separator: String?): String? {
        if (bytes == null) {
            return null
        } else if (bytes.size == 0) {
            return ""
        }
        val sb = StringBuilder()
        for (aSrc in bytes) {
            val v = aSrc.toInt() and 0xFF
            val hv = Integer.toHexString(v)
            if (hv.length < 2) {
                sb.append(0)
            }
            sb.append(hv)
            if (!TextUtils.isEmpty(separator)) {
                sb.append(separator)
            }
        }
        var s = sb.toString().uppercase()
        if (!TextUtils.isEmpty(separator)) {
            s = s.substring(0, s.length - separator!!.length)
        }
        return s
    }

    fun toBinary(bytes: ByteArray?): String? {
        return toBinary(bytes, " ")
    }

    fun toBinary(bytes: ByteArray?, separator: String?): String? {
        if (bytes == null) {
            return null
        } else if (bytes.size == 0) {
            return ""
        }
        val sb = StringBuilder()
        for (aSrc in bytes) {
            val v = aSrc.toInt() and 0xFF
            val hv = Integer.toBinaryString(v)
            val loop = 8 - hv.length
            for (i in 0..<loop) {
                sb.append(0)
            }
            sb.append(hv)
            if (!TextUtils.isEmpty(separator)) {
                sb.append(separator)
            }
        }
        var s = sb.toString()
        if (!TextUtils.isEmpty(separator)) {
            s = s.substring(0, s.length - separator!!.length)
        }
        return s
    }

    fun subZeroAndDot(number: String?): String? {
        var number = number
        if (TextUtils.isEmpty(number)) return number
        if (number!!.indexOf(".") > 0) {
            number = number.replace("0+?$", "")
            number = number.replace("[.]$", "")
        }
        return number
    }

    @JvmOverloads
    fun toDuration(duration: Int, format: String? = null): String {
        if (format != null) {
            return String.format(Locale.ENGLISH, format, duration / 3600, duration % 3600 / 60, duration % 60)
        } else {
            return String.format(Locale.ENGLISH, "%02d:%02d:%02d", duration / 3600, duration % 3600 / 60, duration % 60)
        }
    }

    fun toByteArray(hexStr: String, separator: String): ByteArray {
        var s = hexStr.replace(separator.toRegex(), "")
        if (s.length % 2 != 0) {
            s = "0" + s
        }
        val bytes = ByteArray(s.length / 2)
        for (i in bytes.indices) {
            bytes[i] = s.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return bytes
    }
}
