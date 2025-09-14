package com.topdon.lib.core.dialog

import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.annotation.StringRes
import com.topdon.lib.core.R
import com.topdon.lib.core.databinding.DialogTipOtgBinding
import com.topdon.lib.core.utils.ScreenUtil

/**
 * 提示窗
 * create by fylder on 2018/6/15
 **/
class TipOtgDialog : Dialog {
    constructor(context: Context) : super(context)

    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    class Builder {
        var dialog: TipOtgDialog? = null
        private var context: Context? = null
        private var message: String? = null
        private var positiveStr: String? = null
        private var cancelStr: String? = null
        private var positiveEvent: ((check: Boolean) -> Unit)? = null
        private var cancelEvent: (() -> Unit)? = null
        private var canceled = false
        private var hasCheck = false

        private lateinit var messageText: TextView
        private lateinit var checkBox: CheckBox
        private lateinit var successBtn: Button
        private lateinit var cancelBtn: Button

        constructor(context: Context) {
            this.context = context
        }

        fun setMessage(message: String): Builder {
            this.message = message
            return this
        }

        fun setMessage(
            @StringRes message: Int,
        ): Builder {
            this.message = context!!.getString(message)
            return this
        }

        fun setPositiveListener(
            @StringRes strRes: Int,
            event: ((check: Boolean) -> Unit)? = null,
        ): Builder {
            return setPositiveListener(context!!.getString(strRes), event)
        }

        fun setPositiveListener(
            str: String,
            event: ((check: Boolean) -> Unit)? = null,
        ): Builder {
            this.positiveStr = str
            this.positiveEvent = event
            return this
        }

        fun setCancelListener(
            @StringRes strRes: Int,
            event: (() -> Unit)? = null,
        ): Builder {
            return setCancelListener(context!!.getString(strRes), event)
        }

        fun setCancelListener(
            str: String,
            event: (() -> Unit)? = null,
        ): Builder {
            this.cancelStr = str
            this.cancelEvent = event
            return this
        }

        fun setCanceled(canceled: Boolean): Builder {
            this.canceled = canceled
            return this
        }

        fun dismiss() {
            this.dialog!!.dismiss()
        }

        fun create(): TipOtgDialog {
            if (dialog == null) {
                dialog = TipOtgDialog(context!!, R.style.InfoDialog)
            }
            val inflater =
                context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val binding = DialogTipOtgBinding.inflate(LayoutInflater.from(context!!))
            messageText = binding.dialogTipMsgText
            checkBox = binding.dialogTipCheck
            successBtn = binding.dialogTipSuccessBtn
            cancelBtn = binding.dialogTipCancelBtn
            dialog!!.addContentView(
                binding.root,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
            )
            val lp = dialog!!.window!!.attributes
            val wRatio =
                if (context!!.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {

                    0.85
                } else {

                    0.35
                }
            lp.width = (ScreenUtil.getScreenWidth(context!!) * wRatio).toInt() // 设置宽度
            dialog!!.window!!.attributes = lp

            dialog!!.setCanceledOnTouchOutside(canceled)
            checkBox.isChecked = false
            hasCheck = false
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                hasCheck = isChecked
            }
            successBtn.setOnClickListener {
                dismiss()
                positiveEvent?.invoke(hasCheck)
            }
            cancelBtn.setOnClickListener {
                dismiss()
                cancelEvent?.invoke()
            }

            if (positiveStr != null) {
                successBtn.text = positiveStr
            }
            if (!TextUtils.isEmpty(cancelStr)) {
                cancelBtn.visibility = View.VISIBLE
                cancelBtn.text = cancelStr
            } else {
                cancelBtn.visibility = View.GONE
                cancelBtn.text = ""
            }

            if (message != null) {
                messageText.visibility = View.VISIBLE
                messageText.setText(message, TextView.BufferType.NORMAL)
            } else {
                messageText.visibility = View.GONE
            }

            dialog!!.setContentView(binding.root)
            return dialog as TipOtgDialog
        }
    }
}
