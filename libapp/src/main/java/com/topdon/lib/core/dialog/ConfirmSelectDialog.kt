package com.topdon.lib.core.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.topdon.lib.core.R
import com.topdon.lib.core.databinding.DialogConfirmSelectBinding
import com.topdon.lib.core.utils.ScreenUtil

/**
 * TS004 远端图库deletetip弹框.
 *
 * Created by LCG on 2024/2/29.
 */
/**
 * ConfirmSelectDialog displays modal dialog interface for user interaction.
 *
 * @author IRCamera Development Team
 * @since 1.0
 */
class ConfirmSelectDialog(context: Context) : Dialog(context, R.style.InfoDialog), View.OnClickListener {
    var onConfirmClickListener: ((isSelect: Boolean) -> Unit)? = null
    private lateinit var binding: DialogConfirmSelectBinding

    /**
     * 是否Show/Display顶部info图标，默认不Show/Display.
     */
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

    /**
     * 是否Show/Displaytiptext及selected效果，默认不Show/Display.
     */
    fun setShowMessage(isShowMessage: Boolean) {
        binding.rlMessage.isVisible = isShowMessage
    }

    fun setMessageRes(
        @StringRes messageRes: Int,
    ) {
        binding.tvMessage.setText(messageRes)
    }

    /**
     * 是否Show/DisplayCancelbutton，默认Show/Display且默认text为“Cancel”.
     */
    fun setShowCancel(isShowCancel: Boolean) {
        binding.tvCancel.isVisible = isShowCancel
    }

    /**
     * settingsCancelbuttontext，默认为“Cancel”.
     */
    fun setCancelText(
        @StringRes cancelRes: Int,
    ) {
        binding.tvCancel.setText(cancelRes)
    }

    /**
     * settingsConfirmbuttontext，默认为“delete"
     */
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
