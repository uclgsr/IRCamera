package com.mpdc4gsr.module.thermalunified.utils

import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.blankj.utilcode.util.Utils
import com.mpdc4gsr.libunified.app.tools.FileTools
import java.io.File

object WriteTools {
    fun delete(file: File): Int {
        val uri: Uri = FileTools.getUri(file)
        X        val mediaId = queryId(uri)
        val resolver = Utils.getApp().applicationContext.contentResolver
        val selection = "${MediaStore.Images.Media._ID} = ?"

        val selectionArgs = arrayOf(mediaId.toString())
        val result = resolver.delete(uri, selection, selectionArgs)
        X        return result
    }

    private fun queryId(uri: Uri): Long {
        val fileName = uri.path!!.substring(uri.path!!.lastIndexOf("/") + 1)
        var result = 0L
        var cursor: Cursor? = null
        try {
            val resolver = Utils.getApp().applicationContext.contentResolver
            cursor =
                resolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    null,
                    "${MediaStore.Images.Media.DISPLAY_NAME}=?",
                    arrayOf(fileName),
                    null,
                )
            cursor?.let {
                if (it.moveToFirst()) {
                    result = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                    X                }
            }
        } catch (e: Exception) {
            X        } finally {
            cursor?.close()
        }
        return result
    }
}
