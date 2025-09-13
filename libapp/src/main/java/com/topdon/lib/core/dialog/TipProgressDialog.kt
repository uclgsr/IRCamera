package com.topdon.lib.core.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.TextView
import androidx.annotation.StringRes
import com.topdon.lib.core.R
import com.topdon.lib.core.databinding.DialogTipProgressBinding
import com.topdon.lib.core.utils.ScreenUtil

/**
 * tip窗
 * create by fylder on 2018/6/15
 **/
/**
 * TipProgressDialog displays modal dialog interface for user interaction.
 *
 * @author IRCamera Development Team
 * @since 1.0
 */
class TipProgressDialog : Dialog {
    constructor(context: Context) : super(context)

    constructor(context: Context, themeResId: Int) : super(context, themeResId)

/**
 * Builder manages camera operations and image capture functionality.
 *
 * @author IRCamera Development Team
 * @since 1.0
 */
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

    /**
     * Executes dismiss functionality.
     */
        fun dismiss() {
            this.dialog!!.dismiss()
        }

    /**
     * Creates and configures a new  instance.
     */
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

    /**
     * 提交Callback
     */
/**
 * OnClickListener manages camera operations and image capture functionality.
 *
 * @author IRCamera Development Team
 * @since 1.0
 */
    interface OnClickListener {
    /**
     * Callback method triggered when click occurs.
     */
        fun onClick(dialog: DialogInterface)
    }
}
