package com.mpdc4gsr.component.shared.compat

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue

fun Int.dpToPx(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()

fun Int.pxToDp(context: Context): Int = (this / context.resources.displayMetrics.density).toInt()

fun Int.spToPx(context: Context): Int =
    TypedValue
        .applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            this.toFloat(),
            context.resources.displayMetrics,
        ).toInt()

fun Float.dpToPx(context: Context): Float = this * context.resources.displayMetrics.density

fun Float.pxToDp(context: Context): Float = this / context.resources.displayMetrics.density

fun Float.spToPx(context: Context): Float =
    TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        this,
        context.resources.displayMetrics,
    )

@Deprecated(
    message = "Use dpToPx(context) for context-aware conversion",
    replaceWith = ReplaceWith("this.dpToPx(context)"),
)
val Int.dpLegacy: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

@Deprecated(
    message = "Use pxToDp(context) for context-aware conversion",
    replaceWith = ReplaceWith("this.pxToDp(context)"),
)
val Int.pxLegacy: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()

@Deprecated(
    message = "Use spToPx(context) for context-aware conversion",
    replaceWith = ReplaceWith("this.spToPx(context)"),
)
val Int.spLegacy: Int
    get() =
        TypedValue
            .applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                this.toFloat(),
                Resources.getSystem().displayMetrics,
            ).toInt()

class ScreenDimensions(
    private val context: Context,
) {
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


