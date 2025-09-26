package com.mpdc4gsr.libunified.app.utils

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.FileChannel
import java.text.DecimalFormat

/**
 * FileUtils based on reference repository implementation
 * Adapted from BleModule/src/main/java/com/topdon/commons/util/FileSizeUtil.java
 * and BleModule/src/main/java/com/topdon/commons/util/FolderUtil.java
 */
object FileUtils {

    const val SIZETYPE_B = 1    // 获取文件大小单位为B的double值
    const val SIZETYPE_KB = 2   // 获取文件大小单位为KB的double值
    const val SIZETYPE_MB = 3   // 获取文件大小单位为MB的double值
    const val SIZETYPE_GB = 4   // 获取文件大小单位为GB的double值

    fun getFileOrFilesSize(filePath: String, sizeType: Int): Double {
        val file = File(filePath)
        var blockSize: Long = 0
        try {
            blockSize = if (file.isDirectory) {
                getFileSizes(file)
            } else {
                getFileSize(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("FileUtils", "获取文件大小失败!")
        }
        return formatFileSize(blockSize, sizeType)
    }

    private fun formatFileSize(fileSize: Long, sizeType: Int): Double {
        val df = DecimalFormat("#.00")
        val fileSizeString: String = when (sizeType) {
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
            Log.e("FileUtils", "文件或者文件夹不存在，请检查路径是否正确!")
        }
        return size
    }

    private fun getFileSizes(f: File): Long {
        var size: Long = 0
        val fList = f.listFiles()
        if (fList != null) {
            for (file in fList) {
                size += if (file.isDirectory) {
                    getFileSizes(file)
                } else {
                    getFileSize(file)
                }
            }
        }
        return size
    }

    // Additional compatibility methods
    fun copyFile(source: File, dest: File): Boolean {
        return try {
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
    }

    fun deleteFile(file: File): Boolean {
        return if (file.exists()) {
            if (file.isDirectory) {
                deleteDirectory(file)
            } else {
                file.delete()
            }
        } else {
            false
        }
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

    fun getFileExtension(fileName: String): String {
        return if (fileName.contains(".")) {
            fileName.substring(fileName.lastIndexOf(".") + 1)
        } else {
            ""
        }
    }

    fun saveFile(filePath: String, data: ByteArray): Boolean {
        return try {
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
    }
    
    // Extension function for saveFile to be used as lambda
    fun saveFile(file: File?, data: ByteArray) = saveFile(file?.absolutePath ?: "", data)
}