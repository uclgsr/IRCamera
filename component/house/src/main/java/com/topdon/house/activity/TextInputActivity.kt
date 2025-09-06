package com.topdon.house.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import com.topdon.house.R
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.ktbase.BaseActivity

/**
 * 房屋检测 - 问题描述文字输入界面.
 *
 * 需要传递：
 * - [ExtraKeyConfig.ITEM_NAME] - String 类型 item 名称.
 * - [ExtraKeyConfig.RESULT_INPUT_TEXT] - String 类型 当前输入内容.
 *
 * 返回：[ExtraKeyConfig.RESULT_INPUT_TEXT] - String 类型 输入内容.
 *
 * Created by LCG on 2024/8/27.
 */
@SuppressLint("SetTextI18n")
class TextInputActivity : BaseActivity(), View.OnClickListener {
    private lateinit var ivExit: ImageView
    private lateinit var ivSave: ImageView
    private lateinit var tvQuickInput1: TextView
    private lateinit var tvQuickInput2: TextView  
    private lateinit var tvQuickInput3: TextView
    private lateinit var tvQuickInput4: TextView
    private lateinit var etInput: EditText
    private lateinit var tvInputCount: TextView
    private lateinit var tvTitle: TextView
    
    override fun initContentView(): Int = R.layout.activity_text_input

    override fun initView() {
        ivExit = findViewById(R.id.iv_exit)
        ivSave = findViewById(R.id.iv_save)
        tvQuickInput1 = findViewById(R.id.tv_quick_input1)
        tvQuickInput2 = findViewById(R.id.tv_quick_input2)
        tvQuickInput3 = findViewById(R.id.tv_quick_input3)
        tvQuickInput4 = findViewById(R.id.tv_quick_input4)
        etInput = findViewById(R.id.et_input)
        tvInputCount = findViewById(R.id.tv_input_count)
        tvTitle = findViewById(R.id.tv_title)
        
        ivExit.setOnClickListener(this)
        ivSave.setOnClickListener(this)
        tvQuickInput1.setOnClickListener(this)
        tvQuickInput2.setOnClickListener(this)
        tvQuickInput3.setOnClickListener(this)
        tvQuickInput4.setOnClickListener(this)

        etInput.addTextChangedListener {
            tvInputCount.text = "${it?.length ?: 0}/200"
        }
        tvTitle.text = intent.getStringExtra(ExtraKeyConfig.ITEM_NAME)
        etInput.setText(intent.getStringExtra(ExtraKeyConfig.RESULT_INPUT_TEXT))
    }

    override fun initData() {
    }

    override fun onClick(v: View?) {
        when (v) {
            ivExit -> finish()
            ivSave -> {//保存
                val intent = Intent()
                intent.putExtra(ExtraKeyConfig.RESULT_INPUT_TEXT, etInput.text.toString())
                setResult(RESULT_OK, intent)
                finish()
            }
            tvQuickInput1 -> {
                etInput.setText(etInput.text.toString() + tvQuickInput1.text)
                etInput.setSelection(etInput.text.length)
            }
            tvQuickInput2 -> {
                etInput.setText(etInput.text.toString() + tvQuickInput2.text)
                etInput.setSelection(etInput.text.length)
            }
            tvQuickInput3 -> {
                etInput.setText(etInput.text.toString() + tvQuickInput3.text)
                etInput.setSelection(etInput.text.length)
            }
            tvQuickInput4 -> {
                etInput.setText(etInput.text.toString() + tvQuickInput4.text)
                etInput.setSelection(etInput.text.length)
            }
        }
    }
}