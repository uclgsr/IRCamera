package com.mpdc4gsr.libunified.app.utils

object UnifiedHexUtils {
    private const val HEX_CHARS = "0123456789ABCDEF"

    fun binaryToHexString(bytes: ByteArray): String {
        val result = StringBuilder()
        for (b in bytes) {
            val hex = String.format("%02X", b)
            result.append(hex).append(" ")
        }
        return result.toString().trim()
    }

    fun bytesToHex(bytes: ByteArray): String {
        val result = StringBuilder()
        for (b in bytes) {
            result.append(String.format("%02X", b))
        }
        return result.toString()
    }

    fun hexToBytes(hex: String): ByteArray {
        val cleanHex = hex.replace(" ", "").replace("-", "").replace(":", "")
        val len = cleanHex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] =
                (
                    (Character.digit(cleanHex[i], 16) shl 4) +
                        Character.digit(
                            cleanHex[i + 1],
                            16,
                        )
                ).toByte()
            i += 2
        }
        return data
    }

    fun byteToHex(byte: Byte): String = String.format("%02X", byte)

    fun intToHex(value: Int): String = String.format("%08X", value)

    fun longToHex(value: Long): String = String.format("%016X", value)

    fun hexToInt(hex: String): Int = hex.toInt(16)

    fun hexToLong(hex: String): Long = hex.toLong(16)

    fun isValidHex(hex: String): Boolean =
        try {
            hex.toLong(16)
            true
        } catch (e: NumberFormatException) {
            false
        }

    fun hexDump(
        bytes: ByteArray,
        bytesPerLine: Int = 16,
    ): String {
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

    fun stringToHex(str: String): String = bytesToHex(str.toByteArray())

    fun hexToString(hex: String): String = String(hexToBytes(hex))
}
