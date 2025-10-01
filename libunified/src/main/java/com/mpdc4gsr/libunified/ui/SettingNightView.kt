package com.mpdc4gsr.libunified.ui

/*
 * DEPRECATED: This file has been deprecated as part of migration to Jetpack Compose.
 * This code is commented out to avoid compilation errors when dataBinding is disabled.
 * See COMPOSE_MIGRATION.md for alternatives.
 */

/*

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.databinding.UiSettingViewNightBinding

class SettingNightView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var binding: UiSettingViewNightBinding

    var isRightArrowVisible: Boolean = true
        set(value) {
            field = value
            binding.itemSettingEndImage.isVisible = value
        }

    init {
        binding = UiSettingViewNightBinding.inflate(LayoutInflater.from(context), this, true)

        // Handle custom attributes
        context.obtainStyledAttributes(attrs, R.styleable.SettingNightView, defStyleAttr, 0).apply {
            try {
                // Set text
                getString(R.styleable.SettingNightView_setting_text_night)?.let {
                    binding.itemSettingText.text = it
                }

                // Set icon
                val iconRes = getResourceId(R.styleable.SettingNightView_setting_icon_night, 0)
                if (iconRes != 0) {
                    binding.itemSettingImage.setImageResource(iconRes)
                }

                // Set show icon visibility
                val showIcon =
                    getBoolean(R.styleable.SettingNightView_setting_icon_show_night, true)
                binding.itemSettingImage.isVisible = showIcon

                // Set more arrow visibility
                val showMore = getBoolean(R.styleable.SettingNightView_setting_more_night, true)
                binding.itemSettingEndImage.isVisible = showMore
                isRightArrowVisible = showMore

                // Set line visibility
                val showLine = getBoolean(R.styleable.SettingNightView_setting_line_night, false)
                binding.itemSettingLine.isVisible = showLine

            } finally {
                recycle()
            }
        }
    }

    fun setRightTextId(textResId: Int) {
        if (textResId == 0) {
            binding.tvEnd.visibility = GONE
            binding.itemSettingEndImage.visibility = if (isRightArrowVisible) VISIBLE else GONE
        } else {
            binding.tvEnd.setText(textResId)
            binding.tvEnd.visibility = VISIBLE
            binding.itemSettingEndImage.visibility = GONE
        }
    }

    override fun setOnClickListener(listener: OnClickListener?) {
        super.setOnClickListener(listener)
    }
}*/
