package com.mpdc4gsr.libunified.app.utils

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.util.DisplayMetrics
import android.view.Display
import android.view.View
import android.view.WindowManager
import com.energy.iruvc.utils.CommonParams

/**
 * Consolidated screen utilities replacing multiple ScreenUtils classes
 * Replaces:
 * - libunified/src/main/java/com/mpdc4gsr/libunified/ir/utils/ScreenUtils.java
 * - Various screen-related utility functions scattered across modules
 */
object UnifiedScreenUtils {

    /**
     * Get screen width in pixels
     */
    fun getScreenWidth(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }

    /**
     * Get screen height in pixels
     */
    fun getScreenHeight(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

    /**
     * Get display metrics
     */
    fun getDisplayMetrics(context: Context): DisplayMetrics {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics
    }

    /**
     * Get screen size as Point
     */
    fun getScreenSize(context: Context): Point {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        @Suppress("DEPRECATION")
        val display = wm.defaultDisplay
        val size = Point()
        @Suppress("DEPRECATION")
        display.getSize(size)
        return size
    }

    /**
     * Get screen density
     */
    fun getScreenDensity(context: Context): Float {
        return getDisplayMetrics(context).density
    }

    /**
     * Get screen density DPI
     */
    fun getScreenDensityDpi(context: Context): Int {
        return getDisplayMetrics(context).densityDpi
    }

    /**
     * Convert dp to px
     */
    fun dpToPx(context: Context, dp: Float): Int {
        val density = getScreenDensity(context)
        return (dp * density + 0.5f).toInt()
    }

    /**
     * Convert px to dp
     */
    fun pxToDp(context: Context, px: Float): Int {
        val density = getScreenDensity(context)
        return (px / density + 0.5f).toInt()
    }

    /**
     * Get status bar height
     */
    fun getStatusBarHeight(context: Context): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    /**
     * Get navigation bar height
     */
    fun getNavigationBarHeight(context: Context): Int {
        var result = 0
        val resourceId =
            context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    /**
     * Check if navigation bar is present
     */
    fun hasNavigationBar(context: Context): Boolean {
        val resourceId =
            context.resources.getIdentifier("config_showNavigationBar", "bool", "android")
        return if (resourceId > 0) {
            context.resources.getBoolean(resourceId)
        } else {
            false
        }
    }

    /**
     * Get view location on screen
     */
    fun getViewLocationOnScreen(view: View): IntArray {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return location
    }

    /**
     * Get view bounds on screen
     */
    fun getViewBoundsOnScreen(view: View): Rect {
        val location = getViewLocationOnScreen(view)
        return Rect(location[0], location[1], location[0] + view.width, location[1] + view.height)
    }

    /**
     * Check if point is inside view bounds
     */
    fun isPointInsideView(x: Float, y: Float, view: View): Boolean {
        val bounds = getViewBoundsOnScreen(view)
        return x >= bounds.left && x <= bounds.right && y >= bounds.top && y <= bounds.bottom
    }

    /**
     * Get preview FPS by data flow mode for IR camera
     */
    @JvmStatic
    fun getPreviewFPSByDataFlowMode(dataFlowMode: CommonParams.DataFlowMode): Int {
        return when (dataFlowMode) {
            CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT -> 30
            CommonParams.DataFlowMode.TNR_OUTPUT -> 15
            else -> 25
        }
    }

    /**
     * Correct coordinate value to ensure it's within bounds - used for temperature drawing
     */
    @JvmStatic
    fun correct(value: Float, maxValue: Int): Int {
        return kotlin.math.max(0, kotlin.math.min(value.toInt(), maxValue - 1))
    }

    /**
     * Correct point coordinate to ensure it's within bounds - used for temperature drawing
     */
    @JvmStatic
    fun correctPoint(value: Float, maxValue: Int): Int {
        return kotlin.math.max(0, kotlin.math.min(value.toInt(), maxValue - 1))
    }

    /**
     * Get rectangle bounds for view - used for temperature drawing
     */
    @JvmStatic
    fun getRect(width: Int, height: Int): android.graphics.Rect {
        return android.graphics.Rect(0, 0, width, height)
    }
}