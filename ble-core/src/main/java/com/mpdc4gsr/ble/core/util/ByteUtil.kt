package com.mpdc4gsr.ble.core.util

import android.util.Log

object ByteUtil {
    fun byteMerger(byte1: ByteArray, byte2: Int, byte3: Int, byte4: Int): ByteArray {
        return byteMerger(byte1, intToByteArray(byte2), intToByteArray(byte3), intToByteArray2(byte4))
    }

    fun byteMerger(byte1: ByteArray, byte2: String, byte3: String): ByteArray {
        return byteMerger(byte1, byte2.toByteArray(), byte3.toByteArray())
    }

    fun byteMerger(byte1: ByteArray, byte2: String, byte3: String, byte4: String): ByteArray {
        return byteMerger(byte1, byte2.toByteArray(), byte3.toByteArray(), byte4.toByteArray())
    }

    fun byteMerger(byte1: String, byte2: Int): ByteArray {
        return byteMerger(byte1.toByteArray(), intToByteArray(byte2))
    }

    fun byteMerger(byte1: ByteArray, byte2: Int): ByteArray {
        return byteMerger(byte1, intToByteArray(byte2))
    }

    fun byteMerger(byte1: String, byte2: String): ByteArray {
        return byteMerger(byte1.toByteArray(), byte2.toByteArray())
    }

    fun byteMerger(byte1: String, byte2: ByteArray?): ByteArray {
        return ByteUtil.byteMerger(byte1.toByteArray(), byte2!!)
    }

    fun byteMerger(byte1: ByteArray, byte2: String): ByteArray {
        return byteMerger(byte1, byte2.toByteArray())
    }


    fun byteMerger(vararg bytes: ByteArray): ByteArray {
        var length = 0
        for (tmp in bytes) {
            length += tmp.size
        }
        val result = ByteArray(length)
        var lastTypeLength = 0
        for (tmp in bytes) {
            System.arraycopy(tmp, 0, result, lastTypeLength, tmp.size)
            lastTypeLength += tmp.size
        }
        return result
    }

    fun intToByteArray(i: Int): ByteArray {
        val result = ByteArray(1)
        result[0] = (i and 0xFF).toByte()
        return result
    }

    fun intToByteArray2(i: Int): ByteArray {
        val result = ByteArray(4)
        result[0] = ((i shr 24) and 0xFF).toByte()
        result[1] = ((i shr 16) and 0xFF).toByte()
        result[2] = ((i shr 8) and 0xFF).toByte()
        result[3] = (i and 0xFF).toByte()
        return result
    }

    fun LongToBytes(values: Long): ByteArray {
        val buffer = ByteArray(4)
        for (i in 0..3) {
            val offset = (4 - i - 1) * 8
            buffer[i] = ((values shr offset) and 0xffL).toByte()
        }
        return buffer
    }

    fun bytesToFloat(bytes: ByteArray?): Float {
        val value: Float = HexUtil.bytesToHexString(bytes).toInt(16).toFloat()
        return value
    }

    fun byteToFloat(vararg bytes: Byte): Float {
        val resultByte = ByteArray(bytes.size)
        for (i in bytes.indices) {
            resultByte[i] = bytes[i]
        }
        val value: Float = HexUtil.bytesToHexString(resultByte).toInt(16).toFloat()
        Log.e("bcf", "bytesToFloat bytes: " + HexUtil.bytesToHexString(resultByte) + "   float:" + value)
        return value
    }

    fun byteToInt(b: Byte): Int {
        return b.toInt() and 0xFF
    }

    fun short2byte(s: Short): ByteArray {
        val b = ByteArray(2)
        for (i in 0..1) {
            val offset = 16 - (i + 1) * 8
            b[i] = ((s.toInt() shr offset) and 0xff).toByte()
        }
        return b
    }

    fun byteArrayToInt(bytes: ByteArray): Int {
        var value = 0
        for (i in 0..3) {
            val shift = (3 - i) * 8
            value += (bytes[i].toInt() and 0xFF) shl shift
        }
        return value
    }

    fun getCmdType(bytes: ByteArray?): String {
        val hex = HexUtil.bytesToHexString(bytes)
        var cmd = ""
        if (hex.length >= 16) {
            cmd = hex.substring(12, 16)
        }
        return cmd
    }

    fun getCmd(bytes: ByteArray?): String {
        val hex = HexUtil.bytesToHexString(bytes)
        var cmd = ""
        if (hex.length >= 16) {
            cmd = hex.substring(12, 14)
        }
        return cmd
    }
}
