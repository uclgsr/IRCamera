package com.topdon.house.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.topdon.house.R
import com.topdon.lib.core.R as LibR
import com.topdon.lib.core.utils.ScreenUtil
import com.topdon.lms.sdk.weiget.TToast

/**
 * 输入一项文字内容 弹框.
 *
 * Created by LCG on 2024/8/29.
 * @param inputText 预输入的文字内容
 */
class InputTextDialog(context: Context, private val inputText: String, private val onConfirmListener: (text: String) -> Unit) :
    Dialog(context, LibR.style.TextInputDialog), View.OnClickListener {

    private lateinit var contentView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(true)
        setCanceledOnTouchOutside(true)

        contentView = LayoutInflater.from(context).inflate(R.layout.dialog_input_text, null)
        val etInput = contentView.findViewById<android.widget.EditText>(R.id.et_input)
        val tvCancel = contentView.findViewById<android.widget.TextView>(R.id.tv_cancel)
        val tvConfirm = contentView.findViewById<android.widget.TextView>(R.id.tv_confirm)
        
        etInput.setText(inputText)
        etInput.setSelection(0, etInput.length())
        etInput.requestFocus()
        tvCancel.setOnClickListener(this)
        tvConfirm.setOnClickListener(this)
        setContentView(contentView)

        window?.let {
            val layoutParams = it.attributes
            layoutParams.width = (ScreenUtil.getScreenWidth(context) * 0.76f).toInt()
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            it.attributes = layoutParams
        }
    }

    override fun onClick(v: View?) {
        val tvCancel = contentView.findViewById<android.widget.TextView>(R.id.tv_cancel)
        val tvConfirm = contentView.findViewById<android.widget.TextView>(R.id.tv_confirm)
        val etInput = contentView.findViewById<android.widget.EditText>(R.id.et_input)
        
        when (v) {
            tvCancel -> {
                dismiss()
            }
            tvConfirm -> {
                if (etInput.text.isEmpty()) {
                    TToast.shortToast(context, LibR.string.album_report_input_name_tips)
                    return
                }
                dismiss()
                onConfirmListener.invoke(etInput.text.toString())
            }
        }
    }
}