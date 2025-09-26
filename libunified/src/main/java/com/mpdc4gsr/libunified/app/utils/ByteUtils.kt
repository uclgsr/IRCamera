package com.mpdc4gsr.libunified.app.utils

import java.util.*

object ByteUtils {

    /**
     * byte[] => string
     * [0x01, 0x02] => 01 02
     */
    fun ByteArray.toHexString(separator: String = " ") = asUByteArray().joinToString(separator) {
        it.toString(16).padStart(2, '0').uppercase(Locale.getDefault())
    }

    /**
     * byte[] => string
     * [0x01, 0x02] => 01:02
     */
    fun ByteArray.toHexMd5String() = asUByteArray().joinToString(":") {
        it.toString(16).padStart(2, '0').uppercase(Locale.getDefault())
    }

    /**
     * string => byte[]
     * 0102 => [0x01, 0x02]
     */
    fun String.hexStringToByteArray() =
        ByteArray(this.length / 2) { this.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

    /**
     * UUID => ff01
     */
    fun UUID.getTag() = toString().substring(4, 8)

    /**
     * byte[] => int
     */
    fun ByteArray.bytesToInt() = run {
        var total = 0
        val size = this.size
        for (i in 0 until size) {
            total += this[i].toUByte().toInt().shl((size - i - 1) * 8)
        }
        total
    }

    /**
     * byte[] => long
     */
    fun ByteArray.bytesToLong() = run {
        if (this.isEmpty() || this.size > 8) {
            throw IllegalArgumentException("Invalid byte array size for long. Expected size: 1-8, got: ${this.size}")
        }
        var total = 0L
        val size = this.size
        for (i in 0 until size) {
            total += this[i].toUByte().toLong().shl((size - i - 1) * 8)
        }
        total
    }

    fun byteToInt(bytes: ByteArray): Int {
        var count = 0
        var b: Int
        for (i in bytes.size - 1 downTo 0) {
            b = bytes[i].toInt() and 0xff
            count += b shl (8 * (bytes.size - i - 1))
        }
        return count
    }

    /**
     * Convert bytes to int (big endian)
     */
    fun bigBytesToInt(b1: Byte, b2: Byte, b3: Byte, b4: Byte): Int {
        return (b1.toInt() and 0xFF shl 24) or
                (b2.toInt() and 0xFF shl 16) or
                (b3.toInt() and 0xFF shl 8) or
                (b4.toInt() and 0xFF)
    }

    /**
     * Convert Int to ByteArray with specified size
     */
    fun Int.toBytes(size: Int = 4): ByteArray {
        val result = ByteArray(size)
        for (i in 0 until size) {
            result[size - 1 - i] = (this shr (i * 8)).toByte()
        }
        return result
    }
}

// Extension functions to match expected interface
val ByteArray.descBytes: ByteArray get() = this
fun ByteArray.toBytes(): ByteArray = this

// Add the bigBytesToInt function
fun bigBytesToInt(b1: Byte, b2: Byte, b3: Byte, b4: Byte): Int = ByteUtils.bigBytesToInt(b1, b2, b3, b4)