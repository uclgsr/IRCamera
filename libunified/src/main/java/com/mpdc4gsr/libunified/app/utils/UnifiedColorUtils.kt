package com.mpdc4gsr.libunified.app.utils

import android.content.res.Resources
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import com.energy.iruvc.utils.CommonParams
import java.util.Locale

/**
 * Consolidated color utilities replacing multiple ColorUtils classes
 * All duplicate ColorUtils files have been consolidated into this single utility.
 */
object UnifiedColorUtils {

    /**
     * Extract red component from color int
     */
    fun getRed(@ColorInt color: Int): Int {
        return color shr 16 and 0xFF
    }

    /**
     * Extract green component from color int
     */
    fun getGreen(@ColorInt color: Int): Int {
        return color shr 8 and 0xFF
    }

    /**
     * Extract blue component from color int
     */
    fun getBlue(@ColorInt color: Int): Int {
        return color and 0xFF
    }

    /**
     * Extract alpha component from color int
     */
    fun getAlpha(@ColorInt color: Int): Int {
        return color shr 24 and 0xFF
    }

    /**
     * Set alpha value for a color
     */
    fun setColorAlpha(@ColorInt color: Int, alpha: Float): Int {
        val alphaInt = (alpha * 255).toInt().coerceIn(0, 255)
        return color and 0x00ffffff or (alphaInt shl 24)
    }

    /**
     * Convert color to hex string representation
     */
    fun toHexColorString(@ColorInt color: Int): String {
        return "#%08X".format(color)
    }

    /**
     * Convert color to hex string without alpha
     */
    fun toHexColorStringNoAlpha(@ColorInt color: Int): String {
        return "#%06X".format(0xFFFFFF and color)
    }

    /**
     * Format float to one decimal place
     */
    fun formatFloat(value: Float): String {
        return String.format(Locale.ENGLISH, "%.1f", value)
    }

    /**
     * Convert DP to pixels
     */
    fun dpToPx(@Dimension(unit = Dimension.DP) dp: Int): Int {
        val resources = Resources.getSystem()
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    /**
     * Convert DP to pixels (float version)
     */
    fun dpToPx(@Dimension(unit = Dimension.DP) dp: Float): Float {
        val resources = Resources.getSystem()
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }

    /**
     * Convert pixels to DP
     */
    fun pxToDp(px: Int): Int {
        val resources = Resources.getSystem()
        return (px / resources.displayMetrics.density).toInt()
    }

    /**
     * Create color from RGB components
     */
    fun createColor(red: Int, green: Int, blue: Int): Int {
        return createColor(255, red, green, blue)
    }

    /**
     * Create color from ARGB components
     */
    fun createColor(alpha: Int, red: Int, green: Int, blue: Int): Int {
        return (alpha.coerceIn(0, 255) shl 24) or
                (red.coerceIn(0, 255) shl 16) or
                (green.coerceIn(0, 255) shl 8) or
                blue.coerceIn(0, 255)
    }

    /**
     * Change pseudocode mode by old value for IR camera processing
     */
    @JvmStatic
    fun changePseudocodeModeByOld(oldMode: Int): CommonParams.PseudoColorType {
        // For now, just return PSEUDO_1 as it's the only available option
        // TODO: Add more pseudo color types when they become available
        return CommonParams.PseudoColorType.PSEUDO_1
    }
}