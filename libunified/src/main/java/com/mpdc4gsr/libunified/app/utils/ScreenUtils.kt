package com.mpdc4gsr.libunified.app.utils

import android.content.Context
import android.content.res.Configuration

object ScreenUtils {
    @JvmStatic
    fun getScreenWidth(context: Context): Int = context.resources.displayMetrics.widthPixels

    @JvmStatic
    fun getScreenHeight(context: Context): Int = context.resources.displayMetrics.heightPixels

    @JvmStatic
    fun isPortrait(context: Context): Boolean {
        return context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    }

    @JvmStatic
    fun isLandscape(context: Context): Boolean {
        return context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    // Additional compatibility methods
    @JvmStatic
    fun dpToPx(context: Context, dp: Float): Int =
        (dp * context.resources.displayMetrics.density + 0.5f).toInt()

    @JvmStatic
    fun pxToDp(context: Context, px: Float): Int =
        (px / context.resources.displayMetrics.density + 0.5f).toInt()

    @JvmStatic
    fun getScreenDensity(context: Context): Float = context.resources.displayMetrics.density
}