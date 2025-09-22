package com.mpdc4gsr.libunified.app.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.ColorInt
import com.blankj.utilcode.util.SizeUtils
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.databinding.DialogColorSelectBinding
import com.mpdc4gsr.libunified.app.utils.ScreenUtil


class ColorSelectDialog(
    context: Context,
    @ColorInt private var color: Int,
) : Dialog(context, R.style.InfoDialog) {

    var onPickListener: ((color: Int) -> Unit)? = null

    private lateinit var binding: DialogColorSelectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(true)
        setCanceledOnTouchOutside(true)

        binding = DialogColorSelectBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        binding.colorSelectView.selectColor(color)
        binding.colorSelectView.onSelectListener = {
            color = it
        }
        binding.tvSave.setOnClickListener {
            dismiss()
            onPickListener?.invoke(color)
        }

        window?.let {
            val layoutParams = it.attributes
            layoutParams.width = ScreenUtil.getScreenWidth(context) - SizeUtils.dp2px(36f)
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            it.attributes = layoutParams
        }
    }
}
