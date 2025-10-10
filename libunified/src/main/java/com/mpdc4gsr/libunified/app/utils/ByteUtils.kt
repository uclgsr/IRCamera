package com.mpdc4gsr.libunified.app.utils

import java.util.*

object ByteUtils {
    fun byteMerger(
        byte1: ByteArray,
        byte2: Int,
        byte3: Int,
        byte4: Int,
    ): ByteArray =
        byteMerger(
            byte1,
            intToByteArray(byte2),
            intToByteArray(byte3),
            intToByteArray2(byte4),
        )

    fun byteMerger(
        byte1: ByteArray,
        byte2: String,
        byte3: String,
    ): ByteArray = byteMerger(byte1, byte2.toByteArray(), byte3.toByteArray())

    fun byteMerger(
        byte1: String,
        byte2: Int,
    ): ByteArray = byteMerger(byte1.toByteArray(), intToByteArray(byte2))

    fun byteMerger(
        byte1: ByteArray,
        byte2: Int,
    ): ByteArray = byteMerger(byte1, intToByteArray(byte2))

    fun byteMerger(
        byte1: String,
        byte2: String,
    ): ByteArray = byteMerger(byte1.toByteArray(), byte2.toByteArray())

    fun byteMerger(vararg bytes: ByteArray): ByteArray {
        var resultByteArray = ByteArray(0)
        for (b in bytes) {
            resultByteArray = Arrays.copyOf(resultByteArray, resultByteArray.size + b.size)
            System.arraycopy(b, 0, resultByteArray, resultByteArray.size - b.size, b.size)
        }
        return resultByteArray
    }

    fun bytesToFloat(bytes: ByteArray): Float {
        val value = Integer.valueOf(HexUtil.bytesToHexString(bytes), 16)
        return value.toFloat()
    }

    fun byteToFloat(vararg bytes: Byte): Float {
        val resultByte = ByteArray(bytes.size)
        for (i in bytes.indices) {
            resultByte[i] = bytes[i]
        }
        val value = Integer.valueOf(HexUtil.bytesToHexString(resultByte), 16)
        return value.toFloat()
    }

    fun byteToInt(b: Byte): Int = b.toInt() and 0xFF

    fun intToByteArray(value: Int): ByteArray =
        byteArrayOf(
            (value shr 24 and 0xFF).toByte(),
            (value shr 16 and 0xFF).toByte(),
            (value shr 8 and 0xFF).toByte(),
            (value and 0xFF).toByte(),
        )

    fun intToByteArray2(value: Int): ByteArray =
        byteArrayOf(
            (value and 0xFF).toByte(),
            (value shr 8 and 0xFF).toByte(),
        )

    // Compatibility methods for existing code
    fun ByteArray.descBytes(): ByteArray = this.reversedArray()

    fun ByteArray.toBytes(): ByteArray = this

    fun String.toBytes(length: Int): ByteArray {
        val bytes = this.toByteArray()
        val result = ByteArray(length)
        val copyLength = minOf(bytes.size, length)
        System.arraycopy(bytes, 0, result, 0, copyLength)
        return result
    }

    fun Short.toLittleBytes(): ByteArray =
        byteArrayOf(
            (this.toInt() and 0xFF).toByte(),
            (this.toInt() shr 8 and 0xFF).toByte(),
        )

    fun Int.toLittleBytes(): ByteArray =
        byteArrayOf(
            (this and 0xFF).toByte(),
            (this shr 8 and 0xFF).toByte(),
            (this shr 16 and 0xFF).toByte(),
            (this shr 24 and 0xFF).toByte(),
        )

    fun Float.toLittleBytes(): ByteArray {
        val bits = this.toBits()
        return bits.toLittleBytes()
    }

    fun Int.getIndex(index: Int): Int =
        if (index < 32) {
            (this shr index) and 1
        } else {
            0
        }

    fun bigBytesToInt(
        b1: Byte,
        b2: Byte,
        b3: Byte,
        b4: Byte,
    ): Int =
        (b1.toInt() and 0xFF shl 24) or
                (b2.toInt() and 0xFF shl 16) or
                (b3.toInt() and 0xFF shl 8) or
                (b4.toInt() and 0xFF)

    fun joinPackage(vararg src: ByteArray): ByteArray = byteMerger(*src)

    fun numberToBytes(
        bigEndian: Boolean,
        value: Long,
        len: Int,
    ): ByteArray {
        val bytes = ByteArray(8)
        for (i in 0..7) {
            val j = if (bigEndian) 7 - i else i
            bytes[i] = (value shr 8 * j and 0xff).toByte()
        }
        return if (len > 8) {
            bytes
        } else {
            Arrays.copyOfRange(bytes, if (bigEndian) 8 - len else 0, if (bigEndian) 8 else len)
        }
    }

    fun splitPackage(
        src: ByteArray,
        size: Int,
    ): List<ByteArray> {
        val list = mutableListOf<ByteArray>()
        val loop = src.size / size + if (src.size % size == 0) 0 else 1
        for (i in 0 until loop) {
            val from = i * size
            val to = minOf(src.size, from + size)
            list.add(Arrays.copyOfRange(src, from, to))
        }
        return list
    }

    // Additional utility methods
    fun toHexString(bytes: ByteArray): String = HexUtil.bytesToHexString(bytes)

    fun bytesToInt(bytes: ByteArray): Int {
        var count = 0
        for (i in bytes.indices.reversed()) {
            val b = bytes[i].toInt() and 0xff
            count += b shl (8 * (bytes.size - i - 1))
        }
        return count
    }
}

object HexUtil {
    fun bytesToHexString(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            val hex = Integer.toHexString(b.toInt() and 0xFF)
            if (hex.length == 1) {
                sb.append('0')
            }
            sb.append(hex)
        }
        return sb.toString().uppercase(Locale.getDefault())
    }
}
