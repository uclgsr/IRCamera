package com.topdon.module.thermal.ir.report.view

import android.annotation.SuppressLint
import android.content.Context
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isVisible
import com.topdon.lib.core.tools.UnitTools
import com.topdon.module.thermal.ir.R
import com.topdon.lib.core.R as LibR
import com.topdon.module.thermal.ir.report.bean.ImageTempBean

class ReportIRInputView: LinearLayout {

    companion object {
        private const val TYPE_FULL = 0 //全图
        private const val TYPE_POINT = 1//点
        private const val TYPE_LINE = 2 //线
        private const val TYPE_RECT = 3 //面
    }

    // View references - migrated from synthetic views
    private lateinit var clTitle: View
    private lateinit var viewLine: View
    private lateinit var tvTitle: TextView
    private lateinit var clMax: View
    private lateinit var clMin: View
    private lateinit var clAverage: View
    private lateinit var clExplain: View

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    @SuppressLint("SetTextI18n")
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        inflate(context, R.layout.view_report_ir_input, this)
        initViews()

        val etExplain = clExplain.findViewById<EditText>(R.id.et_item)
        etExplain.inputType = InputType.TYPE_CLASS_TEXT
        etExplain.filters = arrayOf(LengthFilter(150))

        val switchMax = clMax.findViewById<SwitchCompat>(R.id.switch_item)
        val etMax = clMax.findViewById<EditText>(R.id.et_item)
        setSwitchListener(switchMax, etMax)

        val switchMin = clMin.findViewById<SwitchCompat>(R.id.switch_item)
        val etMin = clMin.findViewById<EditText>(R.id.et_item)
        setSwitchListener(switchMin, etMin)

        val switchAverage = clAverage.findViewById<SwitchCompat>(R.id.switch_item)
        val etAverage = clAverage.findViewById<EditText>(R.id.et_item)
        setSwitchListener(switchAverage, etAverage)

        val switchExplain = clExplain.findViewById<SwitchCompat>(R.id.switch_item)
        setSwitchListener(switchExplain, etExplain)

        val typeArray = context.obtainStyledAttributes(attrs, R.styleable.ReportIRInputView)
        val type = typeArray.getInt(R.styleable.ReportIRInputView_type, TYPE_FULL)
        val index = typeArray.getInt(R.styleable.ReportIRInputView_index, 0)
        typeArray.recycle()

        clTitle.isVisible = index == 0
        viewLine.isVisible = index > 0

        setupTypeSpecificViews(type, index)
    }

    private fun initViews() {
        clTitle = findViewById(R.id.cl_title)
        viewLine = findViewById(R.id.view_line)
        tvTitle = findViewById(R.id.tv_title)
        clMax = findViewById(R.id.cl_max)
        clMin = findViewById(R.id.cl_min)
        clAverage = findViewById(R.id.cl_average)
        clExplain = findViewById(R.id.cl_explain)
    }

    private fun setupTypeSpecificViews(type: Int, index: Int) {
        val tvMaxName = clMax.findViewById<TextView>(R.id.tv_item_name)
        val tvMinName = clMin.findViewById<TextView>(R.id.tv_item_name)
        val tvAverageName = clAverage.findViewById<TextView>(R.id.tv_item_name)
        val tvExplainName = clExplain.findViewById<TextView>(R.id.tv_item_name)

        when (type) {
            TYPE_FULL -> {
                tvTitle.setText(LibR.string.thermal_full_rect)
                clMin.isVisible = true
                clAverage.isVisible = false
                tvMaxName.text = context.getString(LibR.string.chart_temperature_high) + " (${UnitTools.showUnit()})"
                tvMinName.text = context.getString(LibR.string.chart_temperature_low) + " (${UnitTools.showUnit()})"
                tvExplainName.text = context.getString(LibR.string.album_report_comment)
            }
            TYPE_POINT -> {
                tvTitle.text = context.getString(LibR.string.thermal_point) + "(P)"
                clMin.isVisible = false
                clAverage.isVisible = false
                tvMaxName.text = "P${index + 1} " + context.getString(LibR.string.chart_temperature) + " (${UnitTools.showUnit()})"
                tvExplainName.text = "P${index + 1} " + context.getString(LibR.string.album_report_comment)
            }
            TYPE_LINE -> {
                tvTitle.text = context.getString(LibR.string.thermal_line) + "(L)"
                clMin.isVisible = true
                clAverage.isVisible = true
                tvMaxName.text = "L${index + 1} " + context.getString(LibR.string.chart_temperature_high) + " (${UnitTools.showUnit()})"
                tvMinName.text = "L${index + 1} " + context.getString(LibR.string.chart_temperature_low) + " (${UnitTools.showUnit()})"
                tvAverageName.text = "L${index + 1} " + context.getString(LibR.string.album_report_mean_temperature) + " (${UnitTools.showUnit()})"
                tvExplainName.text = "L${index + 1} " + context.getString(LibR.string.album_report_comment)
            }
            TYPE_RECT -> {
                tvTitle.text = context.getString(LibR.string.thermal_rect) + "(R)"
                clMin.isVisible = true
                clAverage.isVisible = true
                tvMaxName.text = "R${index + 1} " + context.getString(LibR.string.chart_temperature_high) + " (${UnitTools.showUnit()})"
                tvMinName.text = "R${index + 1} " + context.getString(LibR.string.chart_temperature_low) + " (${UnitTools.showUnit()})"
                tvAverageName.text = "R${index + 1} " + context.getString(LibR.string.album_report_mean_temperature) + " (${UnitTools.showUnit()})"
                tvExplainName.text = "R${index + 1} " + context.getString(LibR.string.album_report_comment)
            }
        }
    }

    fun isSwitchMaxCheck(): Boolean {
        val switchMax = clMax.findViewById<SwitchCompat>(R.id.switch_item)
        return switchMax.isChecked
    }
    
    fun isSwitchMinCheck(): Boolean {
        val switchMin = clMin.findViewById<SwitchCompat>(R.id.switch_item)
        return switchMin.isChecked
    }
    
    fun isSwitchAverageCheck(): Boolean {
        val switchAverage = clAverage.findViewById<SwitchCompat>(R.id.switch_item)
        return switchAverage.isChecked
    }
    
    fun isSwitchExplainCheck(): Boolean {
        val switchExplain = clExplain.findViewById<SwitchCompat>(R.id.switch_item)
        return switchExplain.isChecked
    }

    fun getMaxInput(): String {
        val etMax = clMax.findViewById<EditText>(R.id.et_item)
        return etMax.text.toString()
    }
    
    fun getMinInput(): String {
        val etMin = clMin.findViewById<EditText>(R.id.et_item)
        return etMin.text.toString()
    }
    
    fun getAverageInput(): String {
        val etAverage = clAverage.findViewById<EditText>(R.id.et_item)
        return etAverage.text.toString()
    }
    
    fun getExplainInput(): String {
        val etExplain = clExplain.findViewById<EditText>(R.id.et_item)
        return etExplain.text.toString()
    }

    fun refreshData(tempBean: ImageTempBean.TempBean?) {
        val etMax = clMax.findViewById<EditText>(R.id.et_item)
        val etMin = clMin.findViewById<EditText>(R.id.et_item)
        val etAverage = clAverage.findViewById<EditText>(R.id.et_item)
        val etExplain = clExplain.findViewById<EditText>(R.id.et_item)
        
        tempBean?.max?.let {
            etMax.setText(UnitTools.showUnitValue(it.toFloat())?.toString())
        }
        tempBean?.min?.let {
            etMin.setText(UnitTools.showUnitValue(it.toFloat())?.toString())
        }
        tempBean?.average?.let {
            etAverage.setText(UnitTools.showUnitValue(it.toFloat())?.toString())
        }
        etExplain.setText("")
    }

    private fun setSwitchListener(switchCompat: SwitchCompat, editText: EditText) {
        switchCompat.setOnCheckedChangeListener { _, isChecked ->
            editText.isVisible = isChecked
        }
    }
}