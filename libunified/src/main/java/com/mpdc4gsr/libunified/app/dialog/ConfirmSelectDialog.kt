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
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.app.utils.ScreenUtils


class ConfirmSelectDialog(context: Context) : Dialog(context, R.style.InfoDialog),
    View.OnClickListener {
    var onConfirmClickListener: ((isSelect: Boolean) -> Unit)? = null

    private lateinit var ivIcon: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var rlMessage: ConstraintLayout
    private lateinit var ivSelect: ImageView
    private lateinit var tvMessage: TextView
    private lateinit var tvCancel: TextView
    private lateinit var tvConfirm: TextView

    fun setShowIcon(isShowIcon: Boolean) {
        ivIcon.isVisible = isShowIcon
    }

    fun setTitleRes(
        @StringRes titleRes: Int,
    ) {
        tvTitle.setText(titleRes)
    }

    fun setTitleStr(titleStr: String) {
        tvTitle.text = titleStr
    }

    fun setShowMessage(isShowMessage: Boolean) {
        rlMessage.isVisible = isShowMessage
    }

    fun setMessageRes(
        @StringRes messageRes: Int,
    ) {
        tvMessage.setText(messageRes)
    }

    fun setShowCancel(isShowCancel: Boolean) {
        tvCancel.isVisible = isShowCancel
    }

    fun setCancelText(
        @StringRes cancelRes: Int,
    ) {
        tvCancel.setText(cancelRes)
    }

    fun setConfirmText(
        @StringRes confirmRes: Int,
    ) {
        tvConfirm.setText(confirmRes)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(true)
        setCanceledOnTouchOutside(true)

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_confirm_select, null)
        setContentView(view)

        ivIcon = view.findViewById(R.id.iv_icon)
        tvTitle = view.findViewById(R.id.tv_title)
        rlMessage = view.findViewById(R.id.rl_message)
        ivSelect = view.findViewById(R.id.iv_select)
        tvMessage = view.findViewById(R.id.tv_message)
        tvCancel = view.findViewById(R.id.tv_cancel)
        tvConfirm = view.findViewById(R.id.tv_confirm)

        window?.let {
            val layoutParams = it.attributes
            layoutParams.width = (ScreenUtils.getScreenWidth(context) * 0.72).toInt()
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            it.attributes = layoutParams
        }

        rlMessage.setOnClickListener(this)
        tvCancel.setOnClickListener(this)
        tvConfirm.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.rl_message -> {
                ivSelect.isSelected = !ivSelect.isSelected
            }

            R.id.tv_cancel -> {
                dismiss()
            }

            R.id.tv_confirm -> {
                dismiss()
                onConfirmClickListener?.invoke(ivSelect.isSelected)
            }
        }
    }
}
