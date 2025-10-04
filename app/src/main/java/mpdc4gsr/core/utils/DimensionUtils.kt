package mpdc4gsr.core.utils

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * Modern AndroidX replacement for utilcode SizeUtils
 *
 * Provides context-aware, type-safe dimension conversions without external dependencies.
 * This eliminates the need for the utilcode library's SizeUtils, which has
 * hidden API warnings and is no longer maintained.
 *
 * IMPORTANT: These utilities use context-aware patterns to ensure correct UI rendering
 * across different device configurations and themes.
 */

/**
 * Context-based dimension conversions for non-Composable code
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
 * Composable-safe dimension conversions using LocalDensity
 * These are the preferred methods for Jetpack Compose UI code
 */
val Int.dp: Dp
    @Composable
    get() = with(LocalDensity.current) { this@dp.toDp() }

val Float.dp: Dp
    @Composable
    get() = with(LocalDensity.current) { this@dp.toDp() }

/**
 * Legacy fallback using Resources.getSystem() - USE SPARINGLY
 * These are provided for backward compatibility but should be migrated to context-aware versions
 * @deprecated Use dpToPx(context) or Composable .dp extension instead
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

/**
 * Composable helper to get screen dimensions
 */
@Composable
fun rememberScreenDimensions(): ScreenDimensions {
    val context = LocalContext.current
    return ScreenDimensions(context)
}
