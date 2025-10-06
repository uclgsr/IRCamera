package com.mpdc4gsr.libunified.app.utils

import java.util.*

@OptIn(ExperimentalUnsignedTypes::class)
object UnifiedByteUtils {
    fun ByteArray.toHexString(separator: String = " "): String =
        asUByteArray().joinToString(separator) {
            it.toString(16).padStart(2, '0').uppercase(Locale.getDefault())
        }

    fun ByteArray.toHexMd5String(): String =
        asUByteArray().joinToString(":") {
            it.toString(16).padStart(2, '0').uppercase(Locale.getDefault())
        }

    fun String.hexStringToByteArray(): ByteArray =
        ByteArray(this.length / 2) { this.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

    fun UUID.getTag(): String = toString().substring(4, 8)
    fun ByteArray.bytesToInt(): Int {
        var total = 0
        val size = this.size
        for (i in 0 until size) {
            total += this[i].toUByte().toInt().shl((size - i - 1) * 8)
        }
        return total
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

    fun numberToBytes(bigEndian: Boolean, value: Long, len: Int): ByteArray {
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

    fun splitPackage(src: ByteArray, size: Int): List<ByteArray> {
        val list = mutableListOf<ByteArray>()
        val loop = src.size / size + if (src.size % size == 0) 0 else 1
        for (i in 0 until loop) {
            val from = i * size
            val to = minOf(src.size, from + size)
            list.add(Arrays.copyOfRange(src, from, to))
        }
        return list
    }

    fun joinPackage(vararg src: ByteArray): ByteArray {
        var bytes = ByteArray(0)
        for (bs in src) {
            bytes = Arrays.copyOf(bytes, bytes.size + bs.size)
            System.arraycopy(bs, 0, bytes, bytes.size - bs.size, bs.size)
        }
        return bytes
    }
}