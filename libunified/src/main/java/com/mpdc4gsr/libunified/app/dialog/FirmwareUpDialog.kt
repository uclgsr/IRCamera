package com.mpdc4gsr.libunified.app.dialog

/*
 * SIMPLIFIED: This file has been simplified to work without dataBinding.
 * For new code, prefer Jetpack Compose alternatives. See COMPOSE_MIGRATION.md.
 */

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.app.utils.ScreenUtils
import com.mpdc4gsr.libunified.databinding.DialogFirmwareUpBinding

class FirmwareUpDialog(context: Context) : Dialog(context, R.style.InfoDialog),
    View.OnClickListener {
    
    private lateinit var tvTitle: TextView
    private lateinit var tvSize: TextView
    private lateinit var tvContent: TextView
    private lateinit var tvRestartTips: TextView
    private lateinit var tvCancel: TextView
    private lateinit var tvConfirm: TextView

    var titleStr: CharSequence?
        get() = tvTitle.text
        set(value) {
            tvTitle.text = value
        }

    var sizeStr: CharSequence?
        get() = tvSize.text
        set(value) {
            tvSize.text = value
        }

    var contentStr: CharSequence?
        get() = tvContent.text
        set(value) {
            tvContent.text = value
        }

    var isShowRestartTips: Boolean
        get() = tvRestartTips.isVisible
        set(value) {
            tvRestartTips.isVisible = value
        }

    var isShowCancel: Boolean
        get() = tvCancel.isVisible
        set(value) {
            tvCancel.isVisible = value
        }

    var onCancelClickListener: (() -> Unit)? = null

    var onConfirmClickListener: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_firmware_up, null)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        setContentView(view)
        
        tvTitle = view.findViewById(R.id.tv_title)
        tvSize = view.findViewById(R.id.tv_size)
        tvContent = view.findViewById(R.id.tv_content)
        tvRestartTips = view.findViewById(R.id.tv_restart_tips)
        tvCancel = view.findViewById(R.id.tv_cancel)
        tvConfirm = view.findViewById(R.id.tv_confirm)

        window?.let {
            val layoutParams = it.attributes
            layoutParams.width = (ScreenUtils.getScreenWidth(context) * 0.72).toInt()
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            it.attributes = layoutParams
        }

        tvCancel.setOnClickListener(this)
        tvConfirm.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.tv_cancel -> {
                dismiss()
                onCancelClickListener?.invoke()
            }

            R.id.tv_confirm -> {
                dismiss()
                onConfirmClickListener?.invoke()
            }
        }
    }
}
