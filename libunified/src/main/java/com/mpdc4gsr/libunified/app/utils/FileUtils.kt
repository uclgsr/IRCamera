package com.mpdc4gsr.libunified.app.utils

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * File utilities for common file operations
 */
object FileUtils {

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

    fun getFileSize(file: File): Long {
        return if (file.exists() && file.isFile) {
            file.length()
        } else {
            0L
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
}