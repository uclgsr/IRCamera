package com.mpdc4gsr.libunified.app.utils

import android.content.Context
import android.content.res.Configuration

/**
 * ScreenUtil alias for backward compatibility
 * Delegates to UnifiedScreenUtils for actual implementation
 */
object ScreenUtil {
    @JvmStatic
    fun getScreenWidth(context: Context): Int = UnifiedScreenUtils.getScreenWidth(context)

    @JvmStatic
    fun getScreenHeight(context: Context): Int = UnifiedScreenUtils.getScreenHeight(context)

    @JvmStatic
    fun isPortrait(context: Context): Boolean {
        return context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    }

    @JvmStatic
    fun isLandscape(context: Context): Boolean {
        return context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }
    
    @JvmStatic
    fun dpToPx(context: Context, dp: Float): Int = UnifiedScreenUtils.dpToPx(context, dp)
    
    @JvmStatic
    fun pxToDp(context: Context, px: Float): Int = UnifiedScreenUtils.pxToDp(context, px)
    
    @JvmStatic
    fun getScreenDensity(context: Context): Float = UnifiedScreenUtils.getScreenDensity(context)
}