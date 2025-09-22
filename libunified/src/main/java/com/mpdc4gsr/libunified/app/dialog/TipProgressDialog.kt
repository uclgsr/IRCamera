package com.mpdc4gsr.libunified.app.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.TextView
import androidx.annotation.StringRes
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.databinding.DialogTipProgressBinding
import com.mpdc4gsr.libunified.app.utils.ScreenUtil


class TipProgressDialog : Dialog {
    constructor(context: Context) : super(context)

    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    class Builder {
        var dialog: TipProgressDialog? = null

        private var context: Context? = null

        private var message: String? = null
        private var canceleable = true

        private var messageText: TextView? = null

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

        fun setCanceleable(cancal: Boolean): Builder {
            this.canceleable = cancal
            return this
        }

        fun dismiss() {
            this.dialog!!.dismiss()
        }

        fun create(): TipProgressDialog {
            if (dialog == null) {
                dialog = TipProgressDialog(context!!, R.style.InfoDialog)
            }
            val inflater =
                context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val binding = DialogTipProgressBinding.inflate(LayoutInflater.from(context!!))
            messageText = binding.dialogTipLoadMsg

            dialog!!.addContentView(
                binding.root,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
            )
            val lp = dialog!!.window!!.attributes
            val wRatio =
                if (context!!.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {

                    0.52
                } else {

                    0.35
                }
            lp.width = (ScreenUtil.getScreenWidth(context!!) * wRatio).toInt()
            dialog!!.window!!.attributes = lp

            dialog!!.setCanceledOnTouchOutside(canceleable)

            if (message != null) {
                messageText?.visibility = View.VISIBLE
                messageText?.setText(message, TextView.BufferType.NORMAL)
            } else {
                messageText?.visibility = View.GONE
            }

            dialog!!.setContentView(binding.root)
            return dialog as TipProgressDialog
        }
    }

    interface OnClickListener {
        fun onClick(dialog: DialogInterface)
    }
}
