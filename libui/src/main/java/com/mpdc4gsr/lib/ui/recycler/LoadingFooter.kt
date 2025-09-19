package com.mpdc4gsr.lib.ui.recycler

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.mpdc4gsr.lib.ui.databinding.UiFooterViewBinding


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
