package com.mpdc4gsr.libunified.compat

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue

/**
 * Modern AndroidX replacement for utilcode SizeUtils
 * Provides context-aware, type-safe dimension conversions without external dependencies
 *
 * IMPORTANT: These utilities use context-aware patterns to ensure correct UI rendering
 * across different device configurations and themes.
 */

/**
 * Context-based dimension conversions
 * These extensions require a Context parameter to ensure correct configuration
 */
fun Int.dpToPx(context: Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}

fun Int.pxToDp(context: Context): Int {
    return (this / context.resources.displayMetrics.density).toInt()
}

fun Int.spToPx(context: Context): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        this.toFloat(),
        context.resources.displayMetrics
    ).toInt()
}

fun Float.dpToPx(context: Context): Float {
    return this * context.resources.displayMetrics.density
}

fun Float.pxToDp(context: Context): Float {
    return this / context.resources.displayMetrics.density
}

fun Float.spToPx(context: Context): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        this,
        context.resources.displayMetrics
    )
}

/**
 * Legacy fallback using Resources.getSystem() - USE SPARINGLY
 * These are provided for backward compatibility but should be migrated to context-aware versions
 * @deprecated Use dpToPx(context) for context-aware conversion
 */
@Deprecated(
    message = "Use dpToPx(context) for context-aware conversion",
    replaceWith = ReplaceWith("this.dpToPx(context)")
)
val Int.dpLegacy: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

@Deprecated(
    message = "Use pxToDp(context) for context-aware conversion",
    replaceWith = ReplaceWith("this.pxToDp(context)")
)
val Int.pxLegacy: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()

@Deprecated(
    message = "Use spToPx(context) for context-aware conversion",
    replaceWith = ReplaceWith("this.spToPx(context)")
)
val Int.spLegacy: Int
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    ).toInt()

/**
 * Context-aware screen dimensions helper
 */
class ScreenDimensions(private val context: Context) {
    val screenWidthPx: Int
        get() = context.resources.displayMetrics.widthPixels

    val screenHeightPx: Int
        get() = context.resources.displayMetrics.heightPixels

    val screenDensity: Float
        get() = context.resources.displayMetrics.density

    val screenDensityDpi: Int
        get() = context.resources.displayMetrics.densityDpi

    val screenWidthDp: Int
        get() = screenWidthPx.pxToDp(context)

    val screenHeightDp: Int
        get() = screenHeightPx.pxToDp(context)
}
