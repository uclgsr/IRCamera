package com.topdon.lib.ui.recycler

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.topdon.lib.ui.R as UiR

/**
 * 自定义HeaderView
 */
/**
 * Custom Header view for thermal imaging display.
 * Provides specialized rendering and interaction capabilities.
 */
/**
 * HeaderView implements custom user interface component functionality.
 *
 * @author IRCamera Development Team
 * @since 1.0
 */
class HeaderView : LinearLayout {
    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs, 0) {
        inflate(context, UiR.layout.ui_header_view, this)
    }
}
