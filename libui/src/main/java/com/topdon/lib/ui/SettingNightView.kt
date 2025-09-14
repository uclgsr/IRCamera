package com.topdon.lib.ui

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.topdon.lib.ui.databinding.UiSettingViewNightBinding
import com.topdon.lib.ui.R as UiR


class SettingNightView : LinearLayout {
    private lateinit var binding: UiSettingViewNightBinding

    var isRightArrowVisible: Boolean
        get() = binding.itemSettingEndImage.isVisible
        set(value) {
            binding.itemSettingEndImage.isVisible = value
        }

    fun setRightTextId(
        @StringRes resId: Int,
    ) {
        binding.tvEnd.isVisible = resId != 0
        if (resId != 0) {
            binding.tvEnd.setText(resId)
        }
    }

    private var iconRes: Int = 0
    private var contentStr: String = ""
    private var moreShow: Boolean = true
    private var lineShow: Boolean = false
    private var iconShow: Boolean = false

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        binding = UiSettingViewNightBinding.inflate(LayoutInflater.from(context), this, true)

        val ta: TypedArray = context.obtainStyledAttributes(attrs, UiR.styleable.SettingNightView)
        for (i in 0 until ta.indexCount) {
            when (ta.getIndex(i)) {
                UiR.styleable.SettingNightView_setting_icon_night ->
                    iconRes =
                        ta.getResourceId(
                            UiR.styleable.SettingNightView_setting_icon_night,
                            UiR.drawable.ic_setting_default_svg,
                        )

                UiR.styleable.SettingNightView_setting_text_night ->
                    contentStr =
                        ta.getString(UiR.styleable.SettingNightView_setting_text_night).toString()

                UiR.styleable.SettingNightView_setting_more_night ->
                    moreShow =
                        ta.getBoolean(UiR.styleable.SettingNightView_setting_more_night, true)

                UiR.styleable.SettingNightView_setting_line_night ->
                    lineShow =
                        ta.getBoolean(UiR.styleable.SettingNightView_setting_line_night, false)

                UiR.styleable.SettingNightView_setting_icon_show_night ->
                    iconShow =
                        ta.getBoolean(UiR.styleable.SettingNightView_setting_icon_show_night, false)
            }
        }
        ta.recycle()
        initView()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    )

    private fun initView() {

        binding.itemSettingImage.setImageResource(iconRes)
        if (iconShow) {
            binding.itemSettingImage.visibility = View.VISIBLE
        } else {
            binding.itemSettingImage.visibility = View.GONE
        }
        binding.itemSettingText.text = contentStr
        if (moreShow) {
            binding.itemSettingEndImage.visibility = View.VISIBLE
        } else {
            binding.itemSettingEndImage.visibility = View.GONE
        }
        binding.itemSettingLine.visibility = if (lineShow) View.VISIBLE else View.GONE
    }
}
