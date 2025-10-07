// Merged .kt under 'component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\compat' subtree
// Files: 2; Generated 2025-10-07 23:07:44


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\compat\ContextProvider.kt =====

package com.mpdc4gsr.module.thermalunified.compat

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

@SuppressLint("StaticFieldLeak")
object ContextProvider {
    private lateinit var applicationContext: Context

    @JvmStatic
    fun init(application: Application) {
        applicationContext = application.applicationContext
    }

    @JvmStatic
    fun getContext(): Context {
        if (!::applicationContext.isInitialized) {
            throw IllegalStateException(
                "ContextProvider not initialized. Call ContextProvider.init() in Application.onCreate()"
            )
        }
        return applicationContext
    }

    @JvmStatic
    fun getApplication(): Application {
        return getContext() as Application
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\compat\DimensionExt.kt =====

package com.mpdc4gsr.module.thermalunified.compat

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue


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

@Deprecated(
    message = "Use dpToPx(context) for context-aware conversion",
    replaceWith = ReplaceWith("this.dpToPx(context)")
)
val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

@Deprecated(
    message = "Use pxToDp(context) for context-aware conversion",
    replaceWith = ReplaceWith("this.pxToDp(context)")
)
val Int.px: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()

@Deprecated(
    message = "Use spToPx(context) for context-aware conversion",
    replaceWith = ReplaceWith("this.spToPx(context)")
)
val Int.sp: Int
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    ).toInt()

@Deprecated(
    message = "Use dpToPx(context) for context-aware conversion",
    replaceWith = ReplaceWith("this.dpToPx(context)")
)
val Float.dp: Float
    get() = this * Resources.getSystem().displayMetrics.density

@Deprecated(
    message = "Use pxToDp(context) for context-aware conversion",
    replaceWith = ReplaceWith("this.pxToDp(context)")
)
val Float.px: Float
    get() = this / Resources.getSystem().displayMetrics.density

@Deprecated(
    message = "Use spToPx(context) for context-aware conversion",
    replaceWith = ReplaceWith("this.spToPx(context)")
)
val Float.sp: Float
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        this,
        Resources.getSystem().displayMetrics
    )


