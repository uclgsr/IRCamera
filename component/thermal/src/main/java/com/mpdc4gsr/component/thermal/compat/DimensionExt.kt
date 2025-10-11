package com.mpdc4gsr.component.thermal.compat

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
val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

@Deprecated(
    message = "Use pxToDp(context) for context-aware conversion",
    replaceWith = ReplaceWith("this.pxToDp(context)"),
)
val Int.px: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()

@Deprecated(
    message = "Use spToPx(context) for context-aware conversion",
    replaceWith = ReplaceWith("this.spToPx(context)"),
)
val Int.sp: Int
    get() =
        TypedValue
            .applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                this.toFloat(),
                Resources.getSystem().displayMetrics,
            ).toInt()

@Deprecated(
    message = "Use dpToPx(context) for context-aware conversion",
    replaceWith = ReplaceWith("this.dpToPx(context)"),
)
val Float.dp: Float
    get() = this * Resources.getSystem().displayMetrics.density

@Deprecated(
    message = "Use pxToDp(context) for context-aware conversion",
    replaceWith = ReplaceWith("this.pxToDp(context)"),
)
val Float.px: Float
    get() = this / Resources.getSystem().displayMetrics.density

@Deprecated(
    message = "Use spToPx(context) for context-aware conversion",
    replaceWith = ReplaceWith("this.spToPx(context)"),
)
val Float.sp: Float
    get() =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            this,
            Resources.getSystem().displayMetrics,
        )

