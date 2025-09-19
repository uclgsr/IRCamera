package com.mpdc4gsr.lib.core.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.topdon.lib.core.R
import com.topdon.lib.core.databinding.DialogLongTextBinding
import com.topdon.lib.core.utils.ScreenUtil


class LongTextDialog(context: Context, val title: String?, val content: String?) :
    Dialog(context, R.style.InfoDialog) {
    private val binding: DialogLongTextBinding =
        DialogLongTextBinding.inflate(LayoutInflater.from(context))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(true)
        setCanceledOnTouchOutside(true)

        binding.tvTitle.text = title
        binding.tvText.text = content
        setContentView(binding.root)
        binding.tvIKnow.setOnClickListener {
            dismiss()
        }

        window?.let {
            val layoutParams = it.attributes
            layoutParams.width = (ScreenUtil.getScreenWidth(context) * 0.74f).toInt()
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            it.attributes = layoutParams
        }
    }
}
