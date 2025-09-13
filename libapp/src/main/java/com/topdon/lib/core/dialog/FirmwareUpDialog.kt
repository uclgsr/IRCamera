package com.topdon.lib.core.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.topdon.lib.core.R
import com.topdon.lib.core.databinding.DialogFirmwareUpBinding
import com.topdon.lib.core.utils.ScreenUtil

/**
 * firmwareUpgrade有新versiontip弹框.
 * Created by LCG on 2024/3/4.
 */
/**
 * FirmwareUpDialog displays modal dialog interface for user interaction.
 *
 * @author IRCamera Development Team
 * @since 1.0
 */
class FirmwareUpDialog(context: Context) : Dialog(context, R.style.InfoDialog), View.OnClickListener {
    private var _binding: DialogFirmwareUpBinding? = null
    private val binding get() = _binding!!

    /**
     * titletext，如 "发现新version V3.50"
     */
    var titleStr: CharSequence?
        get() = binding.tvTitle.text
        set(value) {
            binding.tvTitle.text = value
        }

    /**
     * file大小text，如 "大小: 239.6MB"
     */
    var sizeStr: CharSequence?
        get() = binding.tvSize.text
        set(value) {
            binding.tvSize.text = value
        }

    /**
     * Upgrade内容，一般直接扔从interface拿到的东西
     */
    var contentStr: CharSequence?
        get() = binding.tvContent.text
        set(value) {
            binding.tvContent.text = value
        }

    /**
     * 是否Show/Display底部device重启tip，目前仅firmwareUpgrade需要Show/Display，默认Hide(Gone).
     */
    var isShowRestartTips: Boolean
        get() = binding.tvRestartTips.isVisible
        set(value) {
            binding.tvRestartTips.isVisible = value
        }

    /**
     * 是否Show/DisplayCancelbutton，默认Show/Display.
     */
    var isShowCancel: Boolean
        get() = binding.tvCancel.isVisible
        set(value) {
            binding.tvCancel.isVisible = value
        }

    /**
     * CancelclickEventListener.
     */
    var onCancelClickListener: (() -> Unit)? = null

    /**
     * updateclickEventListener.
     */
    var onConfirmClickListener: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = DialogFirmwareUpBinding.inflate(LayoutInflater.from(context))
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        setContentView(binding.root)

        window?.let {
            val layoutParams = it.attributes
            layoutParams.width = (ScreenUtil.getScreenWidth(context) * 0.72).toInt()
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            it.attributes = layoutParams
        }

        binding.tvCancel.setOnClickListener(this)
        binding.tvConfirm.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.tvCancel -> { 
                dismiss()
                onCancelClickListener?.invoke()
            }
            binding.tvConfirm -> { 
                dismiss()
                onConfirmClickListener?.invoke()
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        _binding = null
    }
}
