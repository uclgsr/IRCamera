package com.mpdc4gsr.module.thermalunified.lite.util

import android.content.Context
import java.io.IOException

object CommonUtils {
    @Throws(IOException::class)
    fun getAssetData(context: Context, assetPath: String): ByteArray {
        return context.assets.open(assetPath).use { inputStream ->
            inputStream.readBytes()
        }
    }
}
