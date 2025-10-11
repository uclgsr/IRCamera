package com.mpdc4gsr.component.shared.app.utils

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import com.energy.iruvc.utils.CommonParams

object SharedScreenUtils {
    fun getScreenWidth(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }

    fun getScreenHeight(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

    fun getDisplayMetrics(context: Context): DisplayMetrics {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics
    }

    fun getScreenSize(context: Context): Point {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        @Suppress("DEPRECATION")
        val display = wm.defaultDisplay
        val size = Point()
        @Suppress("DEPRECATION")
        display.getSize(size)
        return size
    }

    fun getScreenDensity(context: Context): Float = getDisplayMetrics(context).density

    fun getScreenDensityDpi(context: Context): Int = getDisplayMetrics(context).densityDpi

    fun dpToPx(
        context: Context,
        dp: Float,
    ): Int {
        val density = getScreenDensity(context)
        return (dp * density + 0.5f).toInt()
    }

    fun pxToDp(
        context: Context,
        px: Float,
    ): Int {
        val density = getScreenDensity(context)
        return (px / density + 0.5f).toInt()
    }

    fun getStatusBarHeight(context: Context): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    fun getNavigationBarHeight(context: Context): Int {
        var result = 0
        val resourceId =
            context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    fun hasNavigationBar(context: Context): Boolean {
        val resourceId =
            context.resources.getIdentifier("config_showNavigationBar", "bool", "android")
        return if (resourceId > 0) {
            context.resources.getBoolean(resourceId)
        } else {
            false
        }
    }

    fun getViewLocationOnScreen(view: View): IntArray {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return location
    }

    fun getViewBoundsOnScreen(view: View): Rect {
        val location = getViewLocationOnScreen(view)
        return Rect(location[0], location[1], location[0] + view.width, location[1] + view.height)
    }

    fun isPointInsideView(
        x: Float,
        y: Float,
        view: View,
    ): Boolean {
        val bounds = getViewBoundsOnScreen(view)
        return x >= bounds.left && x <= bounds.right && y >= bounds.top && y <= bounds.bottom
    }

    @JvmStatic
    fun getPreviewFPSByDataFlowMode(dataFlowMode: CommonParams.DataFlowMode): Int =
        when (dataFlowMode) {
            CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT -> 30
            CommonParams.DataFlowMode.TNR_OUTPUT -> 15
            else -> 25
        }

    @JvmStatic
    fun correct(
        value: Float,
        maxValue: Int,
    ): Int = kotlin.math.max(0, kotlin.math.min(value.toInt(), maxValue - 1))

    @JvmStatic
    fun correctPoint(
        value: Float,
        maxValue: Int,
    ): Int = kotlin.math.max(0, kotlin.math.min(value.toInt(), maxValue - 1))

    @JvmStatic
    fun getRect(
        width: Int,
        height: Int,
    ): android.graphics.Rect = android.graphics.Rect(0, 0, width, height)
}



