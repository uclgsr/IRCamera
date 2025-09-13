package com.topdon.module.thermal.ir.utils

import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.blankj.utilcode.util.*
import com.elvishew.xlog.XLog
import com.topdon.lib.core.tools.FileTools
import java.io.File

/**
 * Write tools for thermal imaging processing.
 * Contains specialized algorithms and processing functions.
 */
object WriteTools {
    fun delete(file: File): Int {
        val uri: Uri = FileTools.getUri(file)
        XLog.w("deletefile uri:$uri")
        val mediaId = queryId(uri) // MediaStore.Audio.Media._ID of item to update.
        val resolver = Utils.getApp().applicationContext.contentResolver
        val selection = "${MediaStore.Images.Media._ID} = ?"
        // By using selection + args we protect against improper escaping of // values.
        val selectionArgs = arrayOf(mediaId.toString())
        val result = resolver.delete(uri, selection, selectionArgs)
        XLog.w("delete结果file: $result")
        return result
    }

    /**
查询MediaStore.Images.Media._ID
     */
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
                    XLog.w("MediaStore.Images.Media._ID: $result")
                }
            }
        } catch (e: Exception) {
            XLog.e("查询exception: ${e.message}")
        } finally {
            cursor?.close()
        }
        return result
    }
}
