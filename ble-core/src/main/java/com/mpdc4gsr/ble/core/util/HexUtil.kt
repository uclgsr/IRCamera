package com.mpdc4gsr.ble.core.util

import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.Byte
import java.util.Locale
import kotlin.ByteArray
import kotlin.Int
import kotlin.String
import kotlin.byteArrayOf
import kotlin.text.toInt

object HexUtil {
    private var `in`: FileInputStream? = null

    fun bytesToHexString(bArray: ByteArray?): String {
        if (bArray == null || bArray.size <= 0) return "BYTE IS NULL"
        val length = bArray.size
        val sb = StringBuffer(bArray.size)
        var sTemp: String?
        for (i in 0..<length) {
            sTemp = Integer.toHexString(0xFF and bArray[i].toInt())
            if (sTemp.length < 2) sb.append(0)
            sb.append(sTemp.uppercase(Locale.getDefault()))
        }
        return sb.toString()
    }

    fun byteToHex(byte1: Byte): String {
        val sb = StringBuffer(1)
        var sTemp: String?
        for (i in 0..0) {
            sTemp = Integer.toHexString(0xFF and byte1.toInt())
            if (sTemp.length < 2) sb.append(0)
            sb.append(sTemp.uppercase(Locale.getDefault()))
        }
        return sb.toString()
    }

    fun toByteArray(hexStr: String): ByteArray {
        var s = hexStr.replace("".toRegex(), "")
        if (s.length % 2 != 0) {
            s = "0" + s
        }
        val bytes = ByteArray(s.length / 2)
        for (i in bytes.indices) {
            bytes[i] = s.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return bytes
    }

    fun toByteArray1(hexStr: String): ByteArray {
        var s = hexStr.replace("".toRegex(), "")
        if (s.length % 2 != 0) {
            s = "0" + s
        }
        var bytes = ByteArray(s.length / 2)
        for (i in bytes.indices) {
            bytes[i] = s.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        if (bytes.size != 2) {
            val v = bytes[0]
            bytes = ByteArray(2)
            bytes[0] = 0
            bytes[1] = v
        }
        return bytes
    }

    fun getString2HexBytes(src: String): ByteArray {
        val ret = ByteArray(src.length / 2)
        val tmp = src.toByteArray()
        for (i in 0..<src.length / 2) {
            ret[i] = uniteBytes(tmp[i * 2], tmp[i * 2 + 1])
        }
        return ret
    }

    fun HexString2Bytes(src: String): ByteArray {
        val len = src.length / 2
        val ret = ByteArray(len)
        val tmp = src.toByteArray()
        for (i in 0..<len) {
            ret[i] = uniteBytes(tmp[i * 2], tmp[i * 2 + 1])
        }
        return ret
    }

    fun uniteBytes(src0: Byte, src1: Byte): Byte {
        val char0 = src0.toInt().toChar()
        val char1 = src1.toInt().toChar()
        val hexChar0 = Character.getNumericValue(char0)
        val hexChar1 = Character.getNumericValue(char1)
        return ((hexChar0 shl 4) or hexChar1).toByte()
    }

    fun hexToByte(hex: String): ByteArray {
        var m = 0
        var n = 0
        val byteLen = hex.length / 2
        val ret = ByteArray(byteLen)
        for (i in 0..<byteLen) {
            m = i * 2 + 1
            n = m + 1
            val intVal = Integer.decode("0x" + hex.substring(i * 2, m) + hex.substring(m, n))
            ret[i] = intVal.toByte()
        }
        return ret
    }

    fun hexToString(bytes: String): String {
        var bytes = bytes
        bytes = bytes.uppercase(Locale.getDefault())
        val hexString = "0123456789ABCDEFabcdef"
        val baos = ByteArrayOutputStream(bytes.length / 2)

        var i = 0
        while (i < bytes.length) {
            baos.write((hexString.indexOf(bytes.get(i)) shl 4 or hexString.indexOf(bytes.get(i + 1))))
            i += 2
        }
        return String(baos.toByteArray())
    }

    fun readFileToByteArray(path: String): ByteArray? {
        val file = File(path)
        if (!file.exists()) {
            Log.d("bcf", "File doesn't exist!")
            return null
        }
        try {
            `in` = FileInputStream(file)
            val inSize = `in`!!.getChannel().size()
            if (inSize == 0L) {
                Log.d("bcf", "The FileInputStream has no content!")
                return null
            }

            val buffer = ByteArray(`in`!!.available())
            `in`!!.read(buffer)
            return buffer
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return null
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        } finally {
            try {
                `in`!!.close()
            } catch (e: IOException) {
                return null
            }
        }
    }

    fun byteSub(data: ByteArray, start: Int, length: Int): ByteArray {
        var bt = ByteArray(length)
        if (start + length > data.size) {
            bt = ByteArray(data.size - start)
        }
        var i = 0
        while (i < length && (i + start) < data.size) {
            bt[i] = data[i + start]
            i++
        }
        return bt
    }
}
