package com.mpdc4gsr.libunified.app.utils

import android.content.res.Resources
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import kotlin.math.floor
import kotlin.math.roundToInt

object ColorUtils {
    fun setColorAlpha(@ColorInt color: Int, alpha: Float): Int {
        val maxAlpha = 0xff
        return color and 0x00ffffff or ((alpha * maxAlpha).toInt() shl 24)
    }

    fun toHexColorString(@ColorInt color: Int): String {
        return "#%06X".format(0xFFFFFF and color)
    }

    fun dpToPx(@Dimension(unit = Dimension.DP) dp: Int): Int {
        val r = Resources.getSystem()
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            r.displayMetrics
        ).roundToInt()
    }

    fun dpToPxF(@Dimension(unit = Dimension.DP) dp: Float): Float {
        val r = Resources.getSystem()
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.displayMetrics)
    }

    fun formatVideoTime(milliseconds: Long): String {
        val totalSeconds = floor(milliseconds.toDouble() / 1000)
        val secondsLeft = totalSeconds % 3600
        val minutes = floor(secondsLeft / 60).toInt()
        val seconds = (secondsLeft % 60).toInt()
        val m = if (minutes < 10) {
            "0$minutes"
        } else {
            minutes.toString()
        }
        val s = if (seconds < 10) {
            "0$seconds";
        } else {
            seconds.toString()
        }
        return "$m:$s"
    }

    // Compatibility methods for existing usage
    fun parseColor(colorString: String): Int {
        return try {
            android.graphics.Color.parseColor(colorString)
        } catch (e: IllegalArgumentException) {
            android.graphics.Color.WHITE
        }
    }

    fun colorToHex(color: Int): String = toHexColorString(color)
    fun adjustColorBrightness(color: Int, factor: Float): Int {
        val a = android.graphics.Color.alpha(color)
        val r = Math.round(android.graphics.Color.red(color) * factor)
        val g = Math.round(android.graphics.Color.green(color) * factor)
        val b = Math.round(android.graphics.Color.blue(color) * factor)
        return android.graphics.Color.argb(
            a,
            Math.min(r, 255),
            Math.min(g, 255),
            Math.min(b, 255)
        )
    }

    fun blendColors(color1: Int, color2: Int, ratio: Float): Int {
        val inverseRatio = 1f - ratio
        val a =
            (android.graphics.Color.alpha(color1) * ratio + android.graphics.Color.alpha(color2) * inverseRatio).toInt()
        val r =
            (android.graphics.Color.red(color1) * ratio + android.graphics.Color.red(color2) * inverseRatio).toInt()
        val g =
            (android.graphics.Color.green(color1) * ratio + android.graphics.Color.green(color2) * inverseRatio).toInt()
        val b =
            (android.graphics.Color.blue(color1) * ratio + android.graphics.Color.blue(color2) * inverseRatio).toInt()
        return android.graphics.Color.argb(a, r, g, b)
    }

    fun isColorLight(color: Int): Boolean {
        val darkness =
            1 - (0.299 * android.graphics.Color.red(color) + 0.587 * android.graphics.Color.green(
                color
            ) + 0.114 * android.graphics.Color.blue(color)) / 255
        return darkness < 0.5
    }

    fun getContrastColor(color: Int): Int {
        return if (isColorLight(color)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
    }
}