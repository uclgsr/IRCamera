package com.mpdc4gsr.libunified.app.utils

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.DecimalFormat

object FileUtils {
    const val SIZETYPE_B = 1 // Bdouble
    const val SIZETYPE_KB = 2 // KBdouble
    const val SIZETYPE_MB = 3 // MBdouble
    const val SIZETYPE_GB = 4 // GBdouble

    fun getFileOrFilesSize(
        filePath: String,
        sizeType: Int,
    ): Double {
        val file = File(filePath)
        var blockSize: Long = 0
        try {
            blockSize =
                if (file.isDirectory) {
                    getFileSizes(file)
                } else {
                    getFileSize(file)
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return formatFileSize(blockSize, sizeType)
    }

    private fun formatFileSize(
        fileSize: Long,
        sizeType: Int,
    ): Double {
        val df = DecimalFormat("#.00")
        val fileSizeString: String =
            when (sizeType) {
                SIZETYPE_B -> df.format(fileSize.toDouble())
                SIZETYPE_KB -> df.format(fileSize.toDouble() / 1024)
                SIZETYPE_MB -> df.format(fileSize.toDouble() / 1048576)
                SIZETYPE_GB -> df.format(fileSize.toDouble() / 1073741824)
                else -> "0"
            }
        return fileSizeString.toDouble()
    }

    private fun getFileSize(file: File): Long {
        var size: Long = 0
        if (file.exists()) {
            try {
                val fis = FileInputStream(file)
                val fc = fis.channel
                size = fc.size()
                fc.close()
                fis.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
        }
        return size
    }

    private fun getFileSizes(f: File): Long {
        var size: Long = 0
        val fList = f.listFiles()
        if (fList != null) {
            for (file in fList) {
                size +=
                    if (file.isDirectory) {
                        getFileSizes(file)
                    } else {
                        getFileSize(file)
                    }
            }
        }
        return size
    }

    // Additional compatibility methods
    fun copyFile(
        source: File,
        dest: File,
    ): Boolean =
        try {
            val inputStream = FileInputStream(source)
            val outputStream = FileOutputStream(dest)
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
            inputStream.close()
            outputStream.close()
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }

    fun deleteFile(file: File): Boolean =
        if (file.exists()) {
            if (file.isDirectory) {
                deleteDirectory(file)
            } else {
                file.delete()
            }
        } else {
            false
        }

    fun deleteDirectory(dir: File): Boolean {
        if (dir.isDirectory) {
            val children = dir.list()
            if (children != null) {
                for (child in children) {
                    val success = deleteDirectory(File(dir, child))
                    if (!success) {
                        return false
                    }
                }
            }
        }
        return dir.delete()
    }

    fun createDirectory(dirPath: String): Boolean {
        val dir = File(dirPath)
        return if (!dir.exists()) {
            dir.mkdirs()
        } else {
            true
        }
    }

    fun getFileExtension(fileName: String): String =
        if (fileName.contains(".")) {
            fileName.substring(fileName.lastIndexOf(".") + 1)
        } else {
            ""
        }

    fun saveFile(
        filePath: String,
        data: ByteArray,
    ): Boolean =
        try {
            val file = File(filePath)
            val parent = file.parentFile
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            val outputStream = FileOutputStream(file)
            outputStream.write(data)
            outputStream.close()
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }

    // Extension function for saveFile to be used as lambda
    fun saveFile(
        file: File?,
        data: ByteArray,
    ) = saveFile(file?.absolutePath ?: "", data)
}
