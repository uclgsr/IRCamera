package com.topdon.tc001.view

import android.content.Context
import android.content.res.TypedArray
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ImageSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.csl.irCamera.databinding.UiMainConnectionGuideBinding
import com.topdon.lib.ui.R as UiR

class ConnectionGuideView : LinearLayout {
    private var iconRes: Int = 0
    private var contentStr: String = ""
    private var iconShow: Boolean = false
    private lateinit var binding: UiMainConnectionGuideBinding

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        val ta: TypedArray = context.obtainStyledAttributes(attrs, UiR.styleable.ConnectionGuideView)
        for (i in 0 until ta.indexCount) {
            when (ta.getIndex(i)) {
                UiR.styleable.ConnectionGuideView_guide_icon ->
                    iconRes =
                        ta.getResourceId(UiR.styleable.ConnectionGuideView_guide_icon, 0)
                UiR.styleable.ConnectionGuideView_guide_text ->
                    contentStr =
                        ta.getString(UiR.styleable.ConnectionGuideView_guide_text).toString()
                UiR.styleable.ConnectionGuideView_guide_icon_show ->
                    iconShow =
                        ta.getBoolean(UiR.styleable.ConnectionGuideView_guide_icon_show, false)
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
        binding = UiMainConnectionGuideBinding.inflate(LayoutInflater.from(context), this, true)
        binding.ivIcon.setImageResource(iconRes)
        binding.tvContent.text = contentStr
        binding.ivIcon.visibility = if (iconShow) View.VISIBLE else View.GONE
    }

    fun setText(text: CharSequence?) {
        if (TextUtils.isEmpty(text)) return
        binding.tvContent.text = text
        binding.tvContent.movementMethod = LinkMovementMethod.getInstance()
    }

    fun getText(): String {
        return binding.tvContent.text.toString()
    }

    fun setHighlightColor(color: Int) {
        binding.tvContent.highlightColor = color
    }

    fun getCompoundDrawables(content: String) {
        var mContent = "$content  " // 插入空格是为了后面替换图片
        val spannableString = SpannableString(mContent)
        val drawable = context.getDrawable(UiR.drawable.ic_connection_press_tip)
        drawable!!.setBounds(0, 0, drawable.minimumWidth, drawable.minimumHeight)
        spannableString.setSpan(ImageSpan(drawable), mContent.length - 1, mContent.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.tvContent.text = spannableString
    }
}
