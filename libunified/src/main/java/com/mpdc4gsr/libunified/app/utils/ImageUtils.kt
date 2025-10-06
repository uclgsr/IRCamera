package com.mpdc4gsr.libunified.app.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.elvishew.xlog.XLog
import com.mpdc4gsr.libunified.app.config.FileConfig.lineIrGalleryDir
import com.mpdc4gsr.libunified.compat.ContextProvider
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
    fun saveToCache(context: Context, bitmap: Bitmap): String {
        val cacheFile = context.externalCacheDir ?: context.cacheDir
        val file = File(cacheFile, "Report_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
        }
        return file.absolutePath
    }

    fun save(bitmap: Bitmap, isTC007: Boolean = false): String {
        val dicName = if (isTC007) "TC007" else CommUtils.getAppName()
        val fileName = "${dicName}_${System.currentTimeMillis()}.jpg"
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$dicName")
                }
                val uri = ContextProvider.getContext().contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                )
                uri?.let {
                    ContextProvider.getContext().contentResolver.openOutputStream(it)?.use { outputStream ->
                        BufferedOutputStream(outputStream).use { bos ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos)
                            bos.flush()
                        }
                    }
                }
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val albumDir = File(picturesDir, dicName)
                if (!albumDir.exists()) {
                    albumDir.mkdirs()
                }
                val file = File(albumDir, fileName)
                FileOutputStream(file).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                    fos.flush()
                }
            }
        } catch (e: Exception) {
            XLog.e("Failed to save image: ${e.message}")
        }
        return fileName.removeSuffix(".jpg")
    }

    fun saveImageToApp(bitmap: Bitmap): String {
        val saveFile = File(ContextProvider.getContext().cacheDir, "PinP_${System.currentTimeMillis()}.jpg")
        FileOutputStream(saveFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
        }
        return saveFile.absolutePath
    }

    fun saveLiteFrame(bs: ByteArray, capital: ByteArray, nuct: ByteArray, name: String) {
        try {
            val dir = lineIrGalleryDir
            val galleryPath = File(dir)
            val fileName = "${name}.ir"
            val file = File(galleryPath, fileName)
            file.writeBytes(capital.plus(bs))
            Log.w(":", file.absolutePath)
        } catch (e: Exception) {
            XLog.e(": ${e.message}")
        }
    }

    fun saveFrame(bs: ByteArray, capital: ByteArray, name: String) {
        try {
            val dir = lineIrGalleryDir
            val galleryPath = File(dir)
            val fileName = "${name}.ir"
            val file = File(galleryPath, fileName)
            file.writeBytes(capital.plus(bs))
            Log.w(":", file.absolutePath)
        } catch (e: Exception) {
            XLog.e(": ${e.message}")
        }
    }

    fun saveOneFrameAGRB(bs: ByteArray, name: String) {
        try {
            val dir = lineIrGalleryDir
            val galleryPath = File(dir)
            val fileName = "${name}.ir"
            val file = File(galleryPath, fileName)
            file.writeBytes(bs)
        } catch (e: Exception) {
            XLog.e(": ${e.message}")
        }
    }
}
