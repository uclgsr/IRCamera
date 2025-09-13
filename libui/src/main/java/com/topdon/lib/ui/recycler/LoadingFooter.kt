package com.topdon.lib.ui.recycler

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.topdon.lib.ui.databinding.UiFooterViewBinding

/**
 * 自定义FooterView - Modernized with view binding
 */
/**
 * Custom Loading footer view for thermal imaging display.
 * Provides specialized rendering and interaction capabilities.
 */
/**
 * LoadingFooter manages camera operations and image capture functionality.
 *
 * @author IRCamera Development Team
 * @since 1.0
 */
class LoadingFooter : LinearLayout {
    private val binding: UiFooterViewBinding

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs, 0) {
        binding = UiFooterViewBinding.inflate(LayoutInflater.from(context), this, true)
    }

    fun setNoMoreData(noMoreData: Boolean): Boolean {
        binding.llLoading.isVisible = !noMoreData
        binding.clLoadEnd.isVisible = noMoreData
        return true
    }

    fun getCustomView(): View = this
}
