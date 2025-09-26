package com.mpdc4gsr.libunified.app.utils

import java.util.Arrays
import java.util.Locale

/**
 * ByteUtils class providing byte manipulation utilities
 * This is an alias/wrapper for UnifiedByteUtils to maintain compatibility
 */
object ByteUtils {
    
    fun ByteArray.descBytes(): ByteArray = this.reversedArray()
    
    fun ByteArray.toBytes(): ByteArray = this
    
    fun bytesToInt(bytes: ByteArray): Int {
        var count = 0
        var b: Int
        for (i in bytes.size - 1 downTo 0) {
            b = bytes[i].toInt() and 0xff
            count += b shl (8 * (bytes.size - i - 1))
        }
        return count
    }
    
    fun joinPackage(vararg src: ByteArray): ByteArray {
        var bytes = ByteArray(0)
        for (bs in src) {
            bytes = Arrays.copyOf(bytes, bytes.length + bs.size)
            System.arraycopy(bs, 0, bytes, bytes.size - bs.size, bs.size)
        }
        return bytes
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
}