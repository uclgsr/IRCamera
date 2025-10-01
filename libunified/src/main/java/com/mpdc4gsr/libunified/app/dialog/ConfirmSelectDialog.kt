package com.mpdc4gsr.libunified.app.dialog

/*
 * DEPRECATED: This file has been deprecated as part of migration to Jetpack Compose.
 * This code is commented out to avoid compilation errors when dataBinding is disabled.
 * See COMPOSE_MIGRATION.md for alternatives.
 */

/*

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.app.utils.ScreenUtil
import com.mpdc4gsr.libunified.databinding.DialogConfirmSelectBinding

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
*/
