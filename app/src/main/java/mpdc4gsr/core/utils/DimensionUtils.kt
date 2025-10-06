package mpdc4gsr.core.utils

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp


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

val Int.dp: Dp
    @Composable
    get() = with(LocalDensity.current) { this@dp.toDp() }
val Float.dp: Dp
    @Composable
    get() = with(LocalDensity.current) { this@dp.toDp() }

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

@Composable
fun rememberScreenDimensions(): ScreenDimensions {
    val context = LocalContext.current
    return ScreenDimensions(context)
}
