package com.mpdc4gsr.commons.util

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.nio.channels.FileChannel
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

object FileSizeUtil {
    const val SIZETYPE_B: Int = 1
    const val SIZETYPE_KB: Int = 2
    const val SIZETYPE_MB: Int = 3
    const val SIZETYPE_GB: Int = 4


    fun getFileOrFilesSize(filePath: String, sizeType: Int): Double {
        val file = File(filePath)
        var blockSize: Long = 0
        try {
            if (file.isDirectory()) {
                blockSize = getFileSizes(file)
            } else {
                blockSize = getFileSize(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("bcf[CHINESE_TEXT]FileSize", "getFileOrFilesSize-1-[CHINESE_TEXT]Failed!")
        }
        return FormetFileSize(blockSize, sizeType)
    }

    fun getUnit(sizeType: Int): String {
        val memoryUnit: String
        if (sizeType == SIZETYPE_B) {
            memoryUnit = "B"
        } else if (sizeType == SIZETYPE_KB) {
            memoryUnit = "KB"
        } else if (sizeType == SIZETYPE_MB) {
            memoryUnit = "MB"
        } else {
            memoryUnit = "GB"
        }
        return memoryUnit
    }

    fun getFilesSize(filePath: String): Long {
        val file = File(filePath)
        var blockSize: Long = 0
        try {
            if (file.isDirectory()) {
                blockSize = getFileSizes(file)
            } else {
                blockSize = getFileSize(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("bcf[CHINESE_TEXT]FileSize--getFilesSize-2-[CHINESE_TEXT]Failed!")
        }
        return blockSize
    }

    fun getAutoFileOrFilesSize(filePath: String, sizeType: Int): String {
        val file = File(filePath)
        var blockSize: Long = 0
        try {
            if (file.isDirectory()) {
                blockSize = getFileSizes(file)
            } else {
                blockSize = getFileSize(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("bcf[CHINESE_TEXT]FileSize", "getAutoFileOrFilesSize-3-[CHINESE_TEXT]Failed!")
        }
        return FormetFileSize(blockSize, sizeType).toString() + getUnit(sizeType)
    }

    fun getAutoFileOrFilesSize(filePath: String): String {
        val file = File(filePath)
        var blockSize: Long = 0
        try {
            if (file.isDirectory()) {
                blockSize = getFileSizes(file)
            } else {
                blockSize = getFileSize(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("bcf[CHINESE_TEXT]FileSize", "getAutoFileOrFilesSize-4-[CHINESE_TEXT]Failed!")
        }
        return FormetFileSize(blockSize)
    }

    @Throws(Exception::class)
    private fun getFileSize(file: File): Long {
        var fc: FileChannel? = null
        try {
            if (file.exists() && file.isFile()) {
                val fis = FileInputStream(file)
                fc = fis.getChannel()
                if (fc.isOpen()) {
                    return fc.size()
                }
            }
        } catch (e: Exception) {
            println("bcf[CHINESE_TEXT]FileSize--getFilesSize-5-[CHINESE_TEXT]Failed!")

            e.printStackTrace()
        } finally {
            if (fc != null) {
                fc.close()
            }
        }
        return 0
    }

    @Throws(Exception::class)
    private fun getFileSizes(f: File): Long {
        var size: Long = 0
        val flist = f.listFiles()
        for (i in flist!!.indices) {
            if (flist[i]!!.isDirectory()) {
                size = size + FileSizeUtil.getFileSizes(flist[i]!!)
            } else {
                size = size + FileSizeUtil.getFileSize(flist[i]!!)
            }
        }
        return size
    }

    fun FormetFileSize(fileS: Long): String {
        val df = DecimalFormat("#.00")
        var fileSizeString = ""
        val wrongSize = "0B"
        if (fileS == 0L) {
            return wrongSize
        }
        if (fileS < 1024) {
            fileSizeString = df.format(fileS.toDouble()) + "B"
        } else if (fileS < 1048576) {
            fileSizeString = df.format(fileS.toDouble() / 1024) + "KB"
        } else if (fileS < 1073741824) {
            fileSizeString = df.format(fileS.toDouble() / 1048576) + "MB"
        } else {
            fileSizeString = df.format(fileS.toDouble() / 1073741824) + "GB"
        }
        return fileSizeString
    }

    fun FormetFileSize(fileS: Long, sizeType: Int): Double {
        val enlocale = Locale("en", "US")
        val df = NumberFormat.getNumberInstance(enlocale) as DecimalFormat
        df.applyPattern("#.00")
        var fileSizeLong = 0.0
        when (sizeType) {
            SIZETYPE_B -> fileSizeLong = df.format(fileS.toDouble()).toDouble()
            SIZETYPE_KB -> fileSizeLong = df.format(fileS.toDouble() / 1024).toDouble()
            SIZETYPE_MB -> fileSizeLong = df.format(fileS.toDouble() / 1048576).toDouble()
            SIZETYPE_GB -> fileSizeLong = df.format(fileS.toDouble() / 1073741824).toDouble()
            else -> {}
        }
        return fileSizeLong
    }

    fun getFileSizeByWriteLog(filename: String): Long {
        try {
            val file = File(filename)
            if (!file.exists() || !file.isFile()) {
                println("bcf--getFileSizeFileSize[CHINESE_TEXT]")
                return -1
            }
            return file.length()
        } catch (e: Exception) {
            e.printStackTrace()
            println("bcf--getFileSize[CHINESE_TEXT]FileSize--getFilesSize-5-[CHINESE_TEXT]Failed!")
        }
        return 0
    }
}
