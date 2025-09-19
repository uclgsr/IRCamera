package com.mpdc4gsr.lib.core.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.mpdc4gsr.lib.core.R
import com.mpdc4gsr.lib.core.databinding.DialogConfirmSelectBinding
import com.mpdc4gsr.lib.core.utils.ScreenUtil

class ConfirmSelectDialog(context: Context) : Dialog(context, R.style.InfoDialog),
    View.OnClickListener {
    var onConfirmClickListener: ((isSelect: Boolean) -> Unit)? = null
    private lateinit var binding: DialogConfirmSelectBinding

    fun setShowIcon(isShowIcon: Boolean) {
        binding.ivIcon.isVisible = isShowIcon
    }

    fun setTitleRes(
        @StringRes titleRes: Int,
    ) {
        binding.tvTitle.setText(titleRes)
    }

    fun setTitleStr(titleStr: String) {
        binding.tvTitle.text = titleStr
    }

    fun setShowMessage(isShowMessage: Boolean) {
        binding.rlMessage.isVisible = isShowMessage
    }

    fun setMessageRes(
        @StringRes messageRes: Int,
    ) {
        binding.tvMessage.setText(messageRes)
    }

    fun setShowCancel(isShowCancel: Boolean) {
        binding.tvCancel.isVisible = isShowCancel
    }

    fun setCancelText(
        @StringRes cancelRes: Int,
    ) {
        binding.tvCancel.setText(cancelRes)
    }

    fun setConfirmText(
        @StringRes confirmRes: Int,
    ) {
        binding.tvConfirm.setText(confirmRes)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(true)
        setCanceledOnTouchOutside(true)

        binding = DialogConfirmSelectBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        window?.let {
            val layoutParams = it.attributes
            layoutParams.width = (ScreenUtil.getScreenWidth(context) * 0.72).toInt()
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            it.attributes = layoutParams
        }

        binding.rlMessage.setOnClickListener(this)
        binding.tvCancel.setOnClickListener(this)
        binding.tvConfirm.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.rlMessage -> { 
                binding.ivSelect.isSelected = !binding.ivSelect.isSelected
            }

            binding.tvCancel -> { 
                dismiss()
            }

            binding.tvConfirm -> { 
                dismiss()
                onConfirmClickListener?.invoke(binding.ivSelect.isSelected)
            }
        }
    }
}
