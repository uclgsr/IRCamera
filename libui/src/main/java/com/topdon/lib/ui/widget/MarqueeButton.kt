package com.topdon.lib.ui.widget

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton

/**
 * Marquee button utility class for thermal imaging operations.
 * Provides helper functions and common functionality.
 */
/**
 * MarqueeButton manages camera operations and image capture functionality.
 *
 * @author IRCamera Development Team
 * @since 1.0
 */
class MarqueeButton : AppCompatButton {
    constructor (context: Context) : super(context)

    constructor (context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor (context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle,
    )

    override fun isFocused(): Boolean {
        return true
    }
}
