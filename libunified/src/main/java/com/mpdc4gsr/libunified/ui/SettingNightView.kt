package com.mpdc4gsr.libunified.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.mpdc4gsr.libunified.R
class SettingNightView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private val itemSettingImage: ImageView
    private val itemSettingEndImage: ImageView
    private val tvEnd: TextView
    private val itemSettingText: TextView
    private val itemSettingLine: View
    var isRightArrowVisible: Boolean = true
        set(value) {
            field = value
            itemSettingEndImage.isVisible = value
        }
    init {
        LayoutInflater.from(context).inflate(R.layout.ui_setting_view_night, this, true)
        itemSettingImage = findViewById(R.id.item_setting_image)
        itemSettingEndImage = findViewById(R.id.item_setting_end_image)
        tvEnd = findViewById(R.id.tv_end)
        itemSettingText = findViewById(R.id.item_setting_text)
        itemSettingLine = findViewById(R.id.item_setting_line)
        // Handle custom attributes
        context.obtainStyledAttributes(attrs, R.styleable.SettingNightView, defStyleAttr, 0).apply {
            try {
                // Set text
                getString(R.styleable.SettingNightView_setting_text_night)?.let {
                    itemSettingText.text = it
                }
                // Set icon
                val iconRes = getResourceId(R.styleable.SettingNightView_setting_icon_night, 0)
                if (iconRes != 0) {
                    itemSettingImage.setImageResource(iconRes)
                }
                // Set show icon visibility
                val showIcon =
                    getBoolean(R.styleable.SettingNightView_setting_icon_show_night, true)
                itemSettingImage.isVisible = showIcon
                // Set more arrow visibility
                val showMore = getBoolean(R.styleable.SettingNightView_setting_more_night, true)
                itemSettingEndImage.isVisible = showMore
                isRightArrowVisible = showMore
                // Set line visibility
                val showLine = getBoolean(R.styleable.SettingNightView_setting_line_night, false)
                itemSettingLine.isVisible = showLine
            } finally {
                recycle()
            }
        }
    }
    fun setRightTextId(textResId: Int) {
        if (textResId == 0) {
            tvEnd.visibility = GONE
            itemSettingEndImage.visibility = if (isRightArrowVisible) VISIBLE else GONE
        } else {
            tvEnd.setText(textResId)
            tvEnd.visibility = VISIBLE
            itemSettingEndImage.visibility = GONE
        }
    }
    override fun setOnClickListener(listener: OnClickListener?) {
        super.setOnClickListener(listener)
    }
}
