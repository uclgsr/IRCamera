package com.mpdc4gsr.commons.util

import java.util.Arrays
import kotlin.math.min
import kotlin.math.pow


object MathUtils {
    fun setDoubleAccuracy(num: Double, scale: Int): Double {
        return ((num * 10.0.pow(scale.toDouble())).toInt()) / 10.0.pow(scale.toDouble())
    }

    fun getPercents(scale: Int, vararg values: Float): FloatArray {
        var total = 0f
        val list: MutableList<Int?> = ArrayList<Int?>()
        for (i in values.indices) {
            if (values[i] != 0f) {
                list.add(i)
            }
            total += values[i]
        }

        if (total == 0f) {
            return FloatArray(values.size)
        }

        val fs = FloatArray(values.size)
        val sc = 10.0.pow((scale + 2).toDouble()).toInt()
        var sum = 0f
        for (i in list.indices) {
            val index: Int = list.get(i)!!
            if (i == list.size - 1) {
                fs[index] = 1 - sum
            } else {
                fs[index] = (values[index] / total * sc).toInt() / sc.toFloat()
                sum += fs[index]
            }
        }
        return fs
    }

    fun numberToBytes(bigEndian: Boolean, value: Long, len: Int): ByteArray {
        val bytes = ByteArray(8)
        for (i in 0..7) {
            val j = if (bigEndian) 7 - i else i
            bytes[i] = (value shr 8 * j and 0xffL).toByte()
        }
        if (len > 8) {
            return bytes
        } else {
            return Arrays.copyOfRange(bytes, if (bigEndian) 8 - len else 0, if (bigEndian) 8 else len)
        }
    }

    fun <T> bytesToNumber(bigEndian: Boolean, cls: Class<T?>?, vararg src: Byte): T? {
        val len = min(8, src.size)
        val bs = ByteArray(8)
        System.arraycopy(src, 0, bs, if (bigEndian) 8 - len else 0, len)
        var value: Long = 0

        for (i in 0..7) {
            val shift = (if (bigEndian) 7 - i else i) shl 3
            value = value or (0xffL shl shift and (bs[i].toLong() shl shift))
        }
        if (src.size == 1) {
            value = value.toByte().toLong()
        } else if (src.size == 2) {
            value = value.toShort().toLong()
        } else if (src.size <= 4) {
            value = value.toInt().toLong()
        }
        if (cls == Short::class.javaPrimitiveType || cls == Short::class.java) {
            return value.toShort() as T?
        } else if (cls == Int::class.javaPrimitiveType || cls == Int::class.java) {
            return value.toInt() as T?
        } else if (cls == Long::class.javaPrimitiveType || cls == Long::class.java) {
            return value as T?
        }
        throw IllegalArgumentException("cls must be one of short, int and long")
    }

    fun reverseBitAndByte(src: ByteArray?): ByteArray? {
        if (src == null || src.size == 0) {
            return null
        }
        val target = ByteArray(src.size)

        for (i in src.indices) {
            var value = 0
            var tmp = src[src.size - 1 - i].toInt()
            for (j in 7 downTo 0) {
                value = value or ((tmp and 0x01) shl j)
                tmp = tmp shr 1
            }
            target[i] = value.toByte()
        }
        return target
    }

    fun splitPackage(src: ByteArray, size: Int): MutableList<ByteArray?> {
        val list: MutableList<ByteArray?> = ArrayList<ByteArray?>()
        val loop = src.size / size + (if (src.size % size == 0) 0 else 1)
        for (i in 0..<loop) {
            val from = i * size
            val to = min(src.size, from + size)
            list.add(Arrays.copyOfRange(src, i * size, to))
        }
        return list
    }

    fun joinPackage(vararg src: ByteArray): ByteArray {
        var bytes = ByteArray(0)
        for (bs in src) {
            bytes = bytes.copyOf(bytes.size + bs.size)
            System.arraycopy(bs, 0, bytes, bytes.size - bs.size, bs.size)
        }
        return bytes
    }

    fun calcCrc8(bytes: ByteArray): Int {
        var crc = 0
        for (b in bytes) {
            crc = crc xor b.toInt()
            for (i in 0..7) {
                if ((crc and 0x80) != 0) {
                    crc = (crc shl 1) xor 0x07
                } else {
                    crc = crc shl 1
                }
            }
        }
        return crc and 0xff
    }

    fun calcCRC16_Modbus(data: ByteArray): Int {
        var crc = 0xffff
        for (b in data) {
            if (b < 0) {
                crc = crc xor b.toInt() + 256
            } else {
                crc = crc xor b.toInt()
            }
            for (i in 8 downTo 1) {
                if ((crc and 0x0001) != 0) {
                    crc = crc shr 1
                    crc = crc xor 0xA001
                } else crc = crc shr 1
            }
        }
        return crc and 0xffff
    }

    fun calcCRC_CCITT_XModem(bytes: ByteArray): Int {
        var crc = 0
        val polynomial = 0x1021
        for (b in bytes) {
            for (i in 0..7) {
                val bit = ((b.toInt() shr (7 - i) and 1) == 1)
                val c15 = ((crc shr 15 and 1) == 1)
                crc = crc shl 1
                if (c15 xor bit) crc = crc xor polynomial
            }
        }
        return crc and 0xffff
    }

    fun calcCRC_CCITT_XModem(bytes: ByteArray, offset: Int, len: Int): Int {
        var crc = 0
        val polynomial = 0x1021
        for (i in offset..<offset + len) {
            val b = bytes[i]
            for (j in 0..7) {
                val bit = ((b.toInt() shr (7 - j) and 1) == 1)
                val c15 = ((crc shr 15 and 1) == 1)
                crc = crc shl 1
                if (c15 xor bit) crc = crc xor polynomial
            }
        }
        return crc and 0xffff
    }

    fun calcCRC_CCITT_0xFFFF(bytes: ByteArray): Int {
        var crc = 0xffff
        val polynomial = 0x1021
        for (b in bytes) {
            for (i in 0..7) {
                val bit = ((b.toInt() shr (7 - i) and 1) == 1)
                val c15 = ((crc shr 15 and 1) == 1)
                crc = crc shl 1
                if (c15 xor bit) crc = crc xor polynomial
            }
        }
        return crc and 0xffff
    }

    fun calcCRC_CCITT_0xFFFF(bytes: ByteArray, offset: Int, len: Int): Int {
        var crc = 0xffff
        val polynomial = 0x1021
        for (i in offset..<offset + len) {
            val b = bytes[i]
            for (j in 0..7) {
                val bit = ((b.toInt() shr (7 - j) and 1) == 1)
                val c15 = ((crc shr 15 and 1) == 1)
                crc = crc shl 1
                if (c15 xor bit) crc = crc xor polynomial
            }
        }
        return crc and 0xffff
    }
}
