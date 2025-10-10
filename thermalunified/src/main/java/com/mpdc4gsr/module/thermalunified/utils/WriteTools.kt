package com.mpdc4gsr.module.thermalunified.utils

import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.mpdc4gsr.libunified.app.tools.FileTools
import com.mpdc4gsr.module.thermalunified.compat.ContextProvider
import java.io.File

object WriteTools {
    fun delete(file: File): Int {
        val uri: Uri = FileTools.getUri(file)
        val mediaId = queryId(uri)
        val resolver = ContextProvider.getContext().contentResolver
        val selection = "${MediaStore.Images.Media._ID} = ?"
        val selectionArgs = arrayOf(mediaId.toString())
        val result = resolver.delete(uri, selection, selectionArgs)
        return result
    }

    private fun queryId(uri: Uri): Long {
        val fileName = uri.path!!.substring(uri.path!!.lastIndexOf("/") + 1)
        var result = 0L
        var cursor: Cursor? = null
        try {
            val resolver = ContextProvider.getContext().contentResolver
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
                }
            }
        } catch (e: Exception) {
        } finally {
            cursor?.close()
        }
        return result
    }
}
