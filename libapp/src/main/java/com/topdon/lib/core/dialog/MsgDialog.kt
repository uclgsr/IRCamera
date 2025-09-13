package com.topdon.lib.core.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.topdon.lib.core.R
import com.topdon.lib.core.databinding.DialogMsgBinding
import com.topdon.lib.core.utils.ScreenUtil

/**
 * messagetip窗
 * create by fylder on 2018/6/15
 **/
/**
 * MsgDialog displays modal dialog interface for user interaction.
 *
 * @author IRCamera Development Team
 * @since 1.0
 */
class MsgDialog : Dialog {
    constructor(context: Context) : super(context)

    constructor(context: Context, themeResId: Int) : super(context, themeResId)

/**
 * Builder manages camera operations and image capture functionality.
 *
 * @author IRCamera Development Team
 * @since 1.0
 */
    class Builder {
        var dialog: MsgDialog? = null

        private var context: Context? = null

        private var imgRes: Int = 0
        private var message: String? = null
        private var positiveClickListener: OnClickListener? = null

        private var tipImg: ImageView? = null
        private var messageText: TextView? = null
        private var closeImg: ImageView? = null

        constructor(context: Context) {
            this.context = context
        }

        fun setImg(
            @DrawableRes res: Int,
        ): Builder {
            this.imgRes = res
            return this
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

        fun setCloseListener(listener: OnClickListener): Builder {
            this.positiveClickListener = listener
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
        fun create(): MsgDialog {
            if (dialog == null) {
                dialog = MsgDialog(context!!, R.style.InfoDialog)
            }
            val binding = DialogMsgBinding.inflate(LayoutInflater.from(context!!))
            tipImg = binding.dialogMsgImg
            messageText = binding.dialogMsgText
            closeImg = binding.dialogMsgClose
            dialog!!.addContentView(
                binding.root,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
            )
            val lp = dialog!!.window!!.attributes
            val wRatio =
                if (context!!.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    
                    0.9
                } else {
                    
                    0.3
                }
            lp.width = (ScreenUtil.getScreenWidth(context!!) * wRatio).toInt() 
            dialog!!.window!!.attributes = lp

            dialog!!.setCanceledOnTouchOutside(false)
            closeImg!!.setOnClickListener {
                dismiss()
                if (positiveClickListener != null) {
                    positiveClickListener!!.onClick(dialog!!)
                }
            }
            
            if (imgRes != 0) {
                tipImg?.visibility = View.VISIBLE
                tipImg?.setImageResource(imgRes)
            } else {
                tipImg?.visibility = View.GONE
            }
            
            if (message != null) {
                messageText?.visibility = View.VISIBLE
                messageText?.setText(message, TextView.BufferType.NORMAL)
            } else {
                messageText?.visibility = View.GONE
            }

            dialog!!.setContentView(binding.root)
            return dialog as MsgDialog
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
