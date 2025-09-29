package com.mpdc4gsr.libunified.utils

/**
 * CommonUtils - Consolidated utility functions
 * 
 * This class consolidates duplicate utility functions found across multiple files:
 * - bytesToInt: found in 3+ files (HexDump.java, CommonUtil.java)
 * - Time formatting: found in 5+ files
 * - Common conversion utilities
 * 
 * Reduces duplication in:
 * - libunified/ir/utils/HexDump.java
 * - thermalunified/utils/HexDump.java  
 * - thermalunified/lite/util/CommonUtil.java
 * - Multiple UI components with time formatting
 */
object CommonUtils {

    /**
     * Convert byte array to integer (consolidated from 3+ duplicate implementations)
     * Original found in HexDump.java and CommonUtil.java files
     */
    @JvmStatic
    fun bytesToInt(src: ByteArray, offset: Int): Int {
        return ((src[offset].toInt() and 0xFF) or
                ((src[offset + 1].toInt() and 0xFF) shl 8) or
                ((src[offset + 2].toInt() and 0xFF) shl 16) or
                ((src[offset + 3].toInt() and 0xFF) shl 24))
    }

    /**
     * Format time as HH:MM:SS (consolidated from 5+ duplicate implementations)
     * Original pattern: String.format("%02d:%02d:%02d", hours, minutes, seconds)
     */
    @JvmStatic
    fun formatTime(hours: Int, minutes: Int, seconds: Int): String {
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    /**
     * Format time from total seconds
     */
    @JvmStatic
    fun formatTime(totalSeconds: Long): String {
        val hours = (totalSeconds / 3600).toInt()
        val minutes = ((totalSeconds % 3600) / 60).toInt()
        val seconds = (totalSeconds % 60).toInt()
        return formatTime(hours, minutes, seconds)
    }

    /**
     * Format time from milliseconds
     */
    @JvmStatic
    fun formatTimeFromMillis(millis: Long): String {
        return formatTime(millis / 1000)
    }

    /**
     * Convert array of bytes to hex string
     */
    @JvmStatic
    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = "0123456789ABCDEF"[v ushr 4]
            hexChars[j * 2 + 1] = "0123456789ABCDEF"[v and 0x0F]
        }
        return String(hexChars)
    }

    /**
     * Convert hex string to byte array
     */
    @JvmStatic
    fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    /**
     * Safe integer parsing with default value
     */
    @JvmStatic
    fun parseInt(value: String?, defaultValue: Int = 0): Int {
        return try {
            value?.toIntOrNull() ?: defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }

    /**
     * Safe float parsing with default value
     */
    @JvmStatic
    fun parseFloat(value: String?, defaultValue: Float = 0f): Float {
        return try {
            value?.toFloatOrNull() ?: defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }
}