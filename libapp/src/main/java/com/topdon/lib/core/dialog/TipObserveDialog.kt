package com.topdon.lib.core.dialog

import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.*
import com.topdon.lib.core.R
import com.topdon.lib.core.databinding.DialogTipObserveBinding
import com.topdon.lib.core.utils.ScreenUtil

/**
 * 观测-弹框封装
 */
/**
 * TipObserveDialog displays modal dialog interface for user interaction.
 *
 * @author IRCamera Development Team
 * @since 1.0
 */
class TipObserveDialog : Dialog {
    constructor(context: Context) : super(context)

    constructor(context: Context, themeResId: Int) : super(context, themeResId)

/**
 * Builder manages camera operations and image capture functionality.
 *
 * @author IRCamera Development Team
 * @since 1.0
 */
    class Builder {
        var dialog: TipObserveDialog? = null
        private var context: Context? = null
        private var title: String? = null
        private var message: String? = null
        private var closeEvent: ((check: Boolean) -> Unit)? = null
        private var canceled = false
        private var hasCheck = false

        private lateinit var titleText: TextView
        private lateinit var messageText: TextView
        private lateinit var checkBox: CheckBox
        private lateinit var imgClose: ImageView

        constructor(context: Context) {
            this.context = context
        }

        fun setMessage(message: Int): Builder {
            this.message = context!!.getString(message)
            return this
        }

        fun setTitle(title: Int): Builder {
            this.title = context!!.getString(title)
            return this
        }

        fun setCancelListener(event: ((check: Boolean) -> Unit)? = null): Builder {
            this.closeEvent = event
            return this
        }

        fun setCanceled(canceled: Boolean): Builder {
            this.canceled = canceled
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
        fun create(): TipObserveDialog {
            if (dialog == null) {
                dialog = TipObserveDialog(context!!, R.style.InfoDialog)
            }
            val inflater =
                context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val binding = DialogTipObserveBinding.inflate(LayoutInflater.from(context!!))

            binding.tvIKnow.setOnClickListener {
                dismiss()
                closeEvent?.invoke(hasCheck)
            }

            titleText = binding.tvTitle
            messageText = binding.dialogTipMsgText
            checkBox = binding.dialogTipCheck
            imgClose = binding.imgClose
            dialog!!.addContentView(
                binding.root,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
            )
            val lp = dialog!!.window!!.attributes
            val wRatio =
                if (context!!.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    
                    0.75
                } else {
                    
                    0.35
                }
            lp.width = (ScreenUtil.getScreenWidth(context!!) * wRatio).toInt() 
            dialog!!.window!!.attributes = lp

            dialog!!.setCanceledOnTouchOutside(canceled)
            checkBox.isChecked = false
            hasCheck = false
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                hasCheck = isChecked
            }
            imgClose.setOnClickListener {
                dismiss()
                closeEvent?.invoke(hasCheck)
            }
            
            if (title != null) {
                titleText.setText(title, TextView.BufferType.NORMAL)
            }
            
            if (message != null) {
                messageText.visibility = View.VISIBLE
                messageText.setText(message, TextView.BufferType.NORMAL)
            } else {
                messageText.visibility = View.GONE
            }
            dialog!!.setContentView(binding.root)
            return dialog as TipObserveDialog
        }
    }
}
