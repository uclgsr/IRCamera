package com.mpdc4gsr.libunified.app.tools

import android.content.Context
import android.util.DisplayMetrics
import com.mpdc4gsr.libunified.app.utils.ScreenUtils
import com.mpdc4gsr.libunified.compat.ContextProvider
import kotlin.math.pow
import kotlin.math.sqrt

object ScreenTools {
    fun isLandPhone(): Boolean {
        val displayMetrics: DisplayMetrics = ContextProvider.getContext().resources.displayMetrics
        val width = displayMetrics.widthPixels.toFloat()
        val height = displayMetrics.heightPixels.toFloat()
        return (width / height) < 0.75f
    }

    fun isIPad(context: Context): Boolean {
        val width = ScreenUtils.getScreenWidth(context)
        val height = ScreenUtils.getScreenHeight(context)
        val densityDpi = context.resources.displayMetrics.densityDpi
        val diagonalPixels = sqrt(width.toDouble().pow(2) + height.toDouble().pow(2))
        val screenInches = diagonalPixels / densityDpi
        return screenInches >= 7f
    }
}
