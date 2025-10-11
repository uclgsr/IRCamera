package com.mpdc4gsr.component.thermal.utils

import android.content.Context
import java.io.IOException

object CommonUtils {
    @Throws(IOException::class)
    fun getAssetData(
        context: Context,
        assetPath: String,
    ): ByteArray =
        context.assets.open(assetPath).use { inputStream ->
            inputStream.readBytes()
        }
}

