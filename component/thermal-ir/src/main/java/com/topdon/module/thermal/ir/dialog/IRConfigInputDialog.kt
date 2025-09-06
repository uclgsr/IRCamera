package com.topdon.module.thermal.ir.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.isVisible
import com.topdon.lib.core.tools.UnitTools
import com.topdon.lib.core.utils.ScreenUtil
import com.topdon.lms.sdk.weiget.TToast
import com.topdon.module.thermal.ir.R
import java.lang.NumberFormatException

/**
 * 温度修正 环境温度、测温距离、发射率 修改值时输入弹框.
 *
 * Created by LCG on 2024/10/24.
 */
class IRConfigInputDialog(context: Context, val type: Type, val isTC007: Boolean) : Dialog(context, R.style.TextInputDialog) {

    private var value: Float? = null
    private var onConfirmListener: ((value: Float) -> Unit)? = null

    /**
     * 设置输入框默认值
     */
    fun setInput(value: Float?): IRConfigInputDialog {
        this.value = value
        return this
    }
    /**
     * 设置确认点击事件监听.
     */
    fun setConfirmListener(l: (value: Float) -> Unit): IRConfigInputDialog {
        this.onConfirmListener = l
        return this
    }


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(true)
        setCanceledOnTouchOutside(true)

        setContentView(R.layout.dialog_ir_config_input)

        // Initialize views with findViewById
        val tvTitle: TextView = findViewById(R.id.tv_title)
        val tvUnit: TextView = findViewById(R.id.tv_unit)
        val etInput: EditText = findViewById(R.id.et_input)
        val tvCancel: TextView = findViewById(R.id.tv_cancel)
        val tvConfirm: TextView = findViewById(R.id.tv_confirm)

        when (type) {
            Type.TEMP -> {
                tvTitle.text = "${context.getString(R.string.thermal_config_environment)} ${UnitTools.showConfigC(-10, if (isTC007) 50 else 55)}"
                tvUnit.text = UnitTools.showUnit()
                tvUnit.isVisible = true
            }
            Type.DIS -> {
                tvTitle.text = "${context.getString(R.string.thermal_config_distance)} (0.2~${if (isTC007) 4 else 5}m)"
                tvUnit.text = "m"
                tvUnit.isVisible = true
            }
            Type.EM -> {
                tvTitle.text = "${context.getString(R.string.thermal_config_radiation)} (${if (isTC007) "0.1" else "0.01"}~1.00)"
                tvUnit.text = ""
                tvUnit.isVisible = false
            }
        }
        etInput.setText(if (value == null) "" else value.toString())
        etInput.setSelection(0, etInput.length())
        etInput.requestFocus()

        tvCancel.setOnClickListener { dismiss() }
        tvConfirm.setOnClickListener {
            try {
                val input: Float = etInput.text.toString().toFloat()
                val isRight = when (type) {
                    Type.TEMP -> input in UnitTools.showUnitValue(-10f) .. UnitTools.showUnitValue(if (isTC007) 50f else 55f)
                    Type.DIS -> input in 0.2f .. if (isTC007) 4f else 5f
                    Type.EM -> input in (if (isTC007) 0.1f else 0.01f) .. 1f
                }
                if (isRight) {
                    dismiss()
                    onConfirmListener?.invoke(input)
                } else {
                    TToast.shortToast(context, R.string.tip_input_format)
                }
            } catch (e: NumberFormatException) {
                TToast.shortToast(context, R.string.tip_input_format)
            }
        }

        window?.let {
            val isPortrait = context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            val layoutParams = it.attributes
            layoutParams.width = (ScreenUtil.getScreenWidth(context) * if (isPortrait) 0.73f else 0.48f).toInt()
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            it.attributes = layoutParams
        }
    }

    enum class Type {
        /**
         * 环境温度
         */
        TEMP,

        /**
         * 测温距离
         */
        DIS,

        /**
         * 发射率
         */
        EM,
    }
}