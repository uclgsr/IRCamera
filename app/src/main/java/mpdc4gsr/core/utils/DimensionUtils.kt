package mpdc4gsr.core.utils

import android.content.res.Resources

/**
 * Modern AndroidX replacement for utilcode SizeUtils
 * 
 * Provides type-safe dimension conversions without external dependencies.
 * This eliminates the need for the utilcode library's SizeUtils, which has
 * hidden API warnings and is no longer maintained.
 */

val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

val Int.px: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()

val Int.sp: Int
    get() = (this * Resources.getSystem().displayMetrics.scaledDensity).toInt()

val Float.dp: Float
    get() = this * Resources.getSystem().displayMetrics.density

val Float.px: Float
    get() = this / Resources.getSystem().displayMetrics.density

val Float.sp: Float
    get() = this * Resources.getSystem().displayMetrics.scaledDensity

object ScreenDimensions {
    val screenWidthPx: Int
        get() = Resources.getSystem().displayMetrics.widthPixels

    val screenHeightPx: Int
        get() = Resources.getSystem().displayMetrics.heightPixels

    val screenDensity: Float
        get() = Resources.getSystem().displayMetrics.density

    val screenDensityDpi: Int
        get() = Resources.getSystem().displayMetrics.densityDpi

    val screenWidthDp: Int
        get() = screenWidthPx.px

    val screenHeightDp: Int
        get() = screenHeightPx.px
}
