package com.guide.zm04c.matrix.utils

/**
 * ByteUtils with minimal implementation for hex string conversion
 * Contains only the methods referenced by GuideUsbManager
 */
object ByteUtils {
    @JvmStatic
    fun toHexString(bytes: ByteArray): String {
        val hexChars = "0123456789ABCDEF".toCharArray()
        val result = StringBuilder(bytes.size * 2)
        for (byte in bytes) {
            val i = byte.toInt() and 0xFF
            result.append(hexChars[i ushr 4])
            result.append(hexChars[i and 0x0F])
        }
        return result.toString()
    }
    
    @JvmStatic
    fun toHexString(byte: Byte): String {
        return toHexString(byteArrayOf(byte))
    }
}