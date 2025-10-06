package com.mpdc4gsr.libunified.app.utils
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import com.energy.iruvc.utils.CommonParams
import java.io.File
import java.io.FileOutputStream
object UnifiedFileUtils {
    fun isFileExist(filePath: String): Boolean {
        if (UnifiedStringUtils.isBlank(filePath)) {
            return false
        }
        val file = File(filePath)
        return file.exists() && file.isFile
    }
    fun isDirectoryExist(dirPath: String): Boolean {
        if (UnifiedStringUtils.isBlank(dirPath)) {
            return false
        }
        val dir = File(dirPath)
        return dir.exists() && dir.isDirectory
    }
    fun createDirectory(dirPath: String): Boolean {
        if (UnifiedStringUtils.isBlank(dirPath)) {
            return false
        }
        val dir = File(dirPath)
        return if (!dir.exists()) {
            dir.mkdirs()
        } else {
            true
        }
    }
    fun deleteDirectory(dirPath: String): Boolean {
        return try {
            val dir = File(dirPath)
            deleteRecursively(dir)
        } catch (e: Exception) {
            false
        }
    }
    private fun deleteRecursively(file: File): Boolean {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                if (!deleteRecursively(child)) {
                    return false
                }
            }
        }
        return file.delete()
    }
    fun deleteFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            file.delete()
        } catch (e: Exception) {
            false
        }
    }
    fun getFileSize(filePath: String): Long {
        return try {
            val file = File(filePath)
            if (file.exists()) file.length() else 0L
        } catch (e: Exception) {
            0L
        }
    }
    fun readTextFile(filePath: String): String? {
        return try {
            File(filePath).readText()
        } catch (e: Exception) {
            null
        }
    }
    fun writeTextFile(filePath: String, content: String): Boolean {
        return try {
            File(filePath).writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }
    fun appendTextFile(filePath: String, content: String): Boolean {
        return try {
            File(filePath).appendText(content)
            true
        } catch (e: Exception) {
            false
        }
    }
    fun copyFile(sourcePath: String, destPath: String): Boolean {
        return try {
            val sourceFile = File(sourcePath)
            val destFile = File(destPath)
            // Create parent directories if they don't exist
            destFile.parentFile?.mkdirs()
            sourceFile.copyTo(destFile, overwrite = true)
            true
        } catch (e: Exception) {
            false
        }
    }
    fun getExternalStorageDirectory(): String {
        return Environment.getExternalStorageDirectory().absolutePath
    }
    fun getAppExternalFilesDir(context: Context, type: String? = null): String? {
        return context.getExternalFilesDir(type)?.absolutePath
    }
    fun saveBitmapToFile(
        bitmap: Bitmap,
        filePath: String,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        quality: Int = 100
    ): Boolean {
        return try {
            val file = File(filePath)
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { fos ->
                bitmap.compress(format, quality, fos)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    @JvmStatic
    fun getY16SrcTypeByDataFlowMode(dataFlowMode: CommonParams.DataFlowMode): CommonParams.Y16ModePreviewSrcType {
        return when (dataFlowMode) {
            CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT -> CommonParams.Y16ModePreviewSrcType.Y16_MODE_TEMPERATURE
            CommonParams.DataFlowMode.TNR_OUTPUT -> CommonParams.Y16ModePreviewSrcType.Y16_MODE_TNR
            else -> CommonParams.Y16ModePreviewSrcType.Y16_MODE_TEMPERATURE
        }
    }
}