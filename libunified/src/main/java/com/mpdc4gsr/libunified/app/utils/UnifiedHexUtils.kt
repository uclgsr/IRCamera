package com.mpdc4gsr.libunified.app.utils

import java.util.Locale

/**
 * Consolidated hex and conversion utilities
 * All duplicate hex utility files have been consolidated into this single utility.
 */
object UnifiedHexUtils {

    private const val HEX_CHARS = "0123456789ABCDEF"

    /**
     * Convert byte array to hex string with spaces
     */
    fun binaryToHexString(bytes: ByteArray): String {
        val result = StringBuilder()
        for (b in bytes) {
            val hex = String.format("%02X", b)
            result.append(hex).append(" ")
        }
        return result.toString().trim()
    }

    /**
     * Convert byte array to hex string without spaces
     */
    fun bytesToHex(bytes: ByteArray): String {
        val result = StringBuilder()
        for (b in bytes) {
            result.append(String.format("%02X", b))
        }
        return result.toString()
    }

    /**
     * Convert hex string to byte array
     */
    fun hexToBytes(hex: String): ByteArray {
        val cleanHex = hex.replace(" ", "").replace("-", "").replace(":", "")
        val len = cleanHex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(cleanHex[i], 16) shl 4) + Character.digit(
                cleanHex[i + 1],
                16
            )).toByte()
            i += 2
        }
        return data
    }

    /**
     * Convert single byte to hex string
     */
    fun byteToHex(byte: Byte): String {
        return String.format("%02X", byte)
    }

    /**
     * Convert int to hex string
     */
    fun intToHex(value: Int): String {
        return String.format("%08X", value)
    }

    /**
     * Convert long to hex string
     */
    fun longToHex(value: Long): String {
        return String.format("%016X", value)
    }

    /**
     * Convert hex string to int
     */
    fun hexToInt(hex: String): Int {
        return hex.toInt(16)
    }

    /**
     * Convert hex string to long
     */
    fun hexToLong(hex: String): Long {
        return hex.toLong(16)
    }

    /**
     * Check if string is valid hex
     */
    fun isValidHex(hex: String): Boolean {
        return try {
            hex.toLong(16)
            true
        } catch (e: NumberFormatException) {
            false
        }
    }

    /**
     * Convert byte array to formatted hex dump
     */
    fun hexDump(bytes: ByteArray, bytesPerLine: Int = 16): String {
        val result = StringBuilder()
        for (i in bytes.indices step bytesPerLine) {
            result.append(String.format("%04X: ", i))

            // Hex representation
            for (j in 0 until bytesPerLine) {
                if (i + j < bytes.size) {
                    result.append(String.format("%02X ", bytes[i + j]))
                } else {
                    result.append("   ")
                }
            }

            result.append(" | ")

            // ASCII representation
            for (j in 0 until bytesPerLine) {
                if (i + j < bytes.size) {
                    val c = bytes[i + j].toInt() and 0xFF
                    result.append(if (c in 32..126) c.toChar() else '.')
                } else {
                    result.append(' ')
                }
            }

            result.append("\n")
        }
        return result.toString()
    }

    /**
     * Convert string to hex representation
     */
    fun stringToHex(str: String): String {
        return bytesToHex(str.toByteArray())
    }

    /**
     * Convert hex string back to string
     */
    fun hexToString(hex: String): String {
        return String(hexToBytes(hex))
    }
}