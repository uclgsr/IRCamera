package com.mpdc4gsr.libunified.app.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.databinding.DialogLongTextBinding
import com.mpdc4gsr.libunified.app.utils.ScreenUtil


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
