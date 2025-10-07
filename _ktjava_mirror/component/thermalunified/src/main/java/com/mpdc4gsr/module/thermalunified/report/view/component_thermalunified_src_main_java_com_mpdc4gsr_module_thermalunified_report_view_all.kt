// Merged .kt under 'component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\view' subtree
// Files: 7; Generated 2025-10-07 23:07:44


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\view\ReportInfoView.kt =====

package com.mpdc4gsr.module.thermalunified.report.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.report.bean.ReportConditionBean
import com.mpdc4gsr.module.thermalunified.report.bean.ReportInfoBean

class ReportInfoView : LinearLayout {
    private lateinit var tvReportName: android.widget.TextView
    private lateinit var tvReportAuthor: android.widget.TextView
    private lateinit var groupReportPlace: androidx.constraintlayout.widget.Group
    private lateinit var tvReportPlace: android.widget.TextView
    private lateinit var tvReportDate: android.widget.TextView
    private lateinit var clReportCondition: androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var groupAmbientTemperature: androidx.constraintlayout.widget.Group
    private lateinit var tvAmbientTemperature: android.widget.TextView
    private lateinit var viewLine1: android.view.View
    private lateinit var groupAmbientHumidity: androidx.constraintlayout.widget.Group
    private lateinit var tvAmbientHumidity: android.widget.TextView
    private lateinit var viewLine2: android.view.View
    private lateinit var groupTestDistance: androidx.constraintlayout.widget.Group
    private lateinit var tvTestDistance: android.widget.TextView
    private lateinit var viewLine3: android.view.View
    private lateinit var groupEmissivity: androidx.constraintlayout.widget.Group
    private lateinit var tvEmissivity: android.widget.TextView
    private lateinit var clTop: androidx.constraintlayout.widget.ConstraintLayout

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        LayoutInflater.from(context).inflate(R.layout.view_report_info, this, true)
        initViews()
    }

    private fun initViews() {
        tvReportName = findViewById(R.id.tv_report_name)
        tvReportAuthor = findViewById(R.id.tv_report_author)
        groupReportPlace = findViewById(R.id.group_report_place)
        tvReportPlace = findViewById(R.id.tv_report_place)
        tvReportDate = findViewById(R.id.tv_report_date)
        clReportCondition = findViewById(R.id.cl_report_condition)
        groupAmbientTemperature = findViewById(R.id.group_ambient_temperature)
        tvAmbientTemperature = findViewById(R.id.tv_ambient_temperature)
        viewLine1 = findViewById(R.id.view_line_1)
        groupAmbientHumidity = findViewById(R.id.group_ambient_humidity)
        tvAmbientHumidity = findViewById(R.id.tv_ambient_humidity)
        viewLine2 = findViewById(R.id.view_line_2)
        groupTestDistance = findViewById(R.id.group_test_distance)
        tvTestDistance = findViewById(R.id.tv_test_distance)
        viewLine3 = findViewById(R.id.view_line_3)
        groupEmissivity = findViewById(R.id.group_emissivity)
        tvEmissivity = findViewById(R.id.tv_emissivity)
        clTop = findViewById(R.id.cl_top)
    }

    fun refreshInfo(reportInfoBean: ReportInfoBean?) {
        tvReportName.text = reportInfoBean?.report_name
        tvReportAuthor.isVisible = reportInfoBean?.is_report_author == 1
        tvReportAuthor.text = reportInfoBean?.report_author
        groupReportPlace.isVisible = reportInfoBean?.is_report_place == 1
        tvReportPlace.text = reportInfoBean?.report_place
        tvReportDate.isVisible = reportInfoBean?.is_report_date == 1
        tvReportDate.text = reportInfoBean?.report_date
    }

    fun refreshCondition(conditionBean: ReportConditionBean?) {
        clReportCondition.isVisible = conditionBean?.is_ambient_humidity == 1 ||
                conditionBean?.is_ambient_temperature == 1 ||
                conditionBean?.is_test_distance == 1 ||
                conditionBean?.is_emissivity == 1
        groupAmbientTemperature.isVisible = conditionBean?.is_ambient_temperature == 1
        tvAmbientTemperature.text = conditionBean?.ambient_temperature
        viewLine1.isVisible = conditionBean?.is_ambient_temperature == 1 &&
                (conditionBean.is_ambient_humidity == 1 || conditionBean.is_test_distance == 1 || conditionBean.is_emissivity == 1)
        groupAmbientHumidity.isVisible = conditionBean?.is_ambient_humidity == 1
        tvAmbientHumidity.text = conditionBean?.ambient_humidity
        viewLine2.isVisible =
            conditionBean?.is_ambient_humidity == 1 && (conditionBean.is_test_distance == 1 || conditionBean.is_emissivity == 1)
        groupTestDistance.isVisible = conditionBean?.is_test_distance == 1
        tvTestDistance.text = conditionBean?.test_distance
        viewLine3.isVisible =
            conditionBean?.is_test_distance == 1 && conditionBean.is_emissivity == 1
        groupEmissivity.isVisible = conditionBean?.is_emissivity == 1
        tvEmissivity.text = conditionBean?.emissivity
    }

    fun getPrintViewList(): ArrayList<View> {
        val result = ArrayList<View>()
        result.add(clTop)
        result.add(clReportCondition)
        return result
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\view\ReportIRInputView.kt =====

package com.mpdc4gsr.module.thermalunified.report.view

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
import com.mpdc4gsr.libunified.app.tools.UnitTools
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.report.bean.ImageTempBean
import com.mpdc4gsr.libunified.R as LibR

class ReportIRInputView : LinearLayout {
    companion object {
        private const val TYPE_FULL = 0
        private const val TYPE_POINT = 1
        private const val TYPE_LINE = 2
        private const val TYPE_RECT = 3
    }

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
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
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

    private fun setupTypeSpecificViews(
        type: Int,
        index: Int,
    ) {
        val tvMaxName = clMax.findViewById<TextView>(R.id.tv_item_name)
        val tvMinName = clMin.findViewById<TextView>(R.id.tv_item_name)
        val tvAverageName = clAverage.findViewById<TextView>(R.id.tv_item_name)
        val tvExplainName = clExplain.findViewById<TextView>(R.id.tv_item_name)
        when (type) {
            TYPE_FULL -> {
                tvTitle.setText(LibR.string.thermal_full_rect)
                clMin.isVisible = true
                clAverage.isVisible = false
                tvMaxName.text =
                    context.getString(LibR.string.chart_temperature_high) + " (${UnitTools.showUnit()})"
                tvMinName.text =
                    context.getString(LibR.string.chart_temperature_low) + " (${UnitTools.showUnit()})"
                tvExplainName.text = context.getString(LibR.string.album_report_comment)
            }

            TYPE_POINT -> {
                tvTitle.text = context.getString(LibR.string.thermal_point) + "(P)"
                clMin.isVisible = false
                clAverage.isVisible = false
                tvMaxName.text =
                    "P${index + 1} " + context.getString(LibR.string.chart_temperature) + " (${UnitTools.showUnit()})"
                tvExplainName.text =
                    "P${index + 1} " + context.getString(LibR.string.album_report_comment)
            }

            TYPE_LINE -> {
                tvTitle.text = context.getString(LibR.string.thermal_line) + "(L)"
                clMin.isVisible = true
                clAverage.isVisible = true
                tvMaxName.text =
                    "L${index + 1} " + context.getString(LibR.string.chart_temperature_high) + " (${UnitTools.showUnit()})"
                tvMinName.text =
                    "L${index + 1} " + context.getString(LibR.string.chart_temperature_low) + " (${UnitTools.showUnit()})"
                tvAverageName.text =
                    "L${index + 1} " + context.getString(LibR.string.album_report_mean_temperature) + " (${UnitTools.showUnit()})"
                tvExplainName.text =
                    "L${index + 1} " + context.getString(LibR.string.album_report_comment)
            }

            TYPE_RECT -> {
                tvTitle.text = context.getString(LibR.string.thermal_rect) + "(R)"
                clMin.isVisible = true
                clAverage.isVisible = true
                tvMaxName.text =
                    "R${index + 1} " + context.getString(LibR.string.chart_temperature_high) + " (${UnitTools.showUnit()})"
                tvMinName.text =
                    "R${index + 1} " + context.getString(LibR.string.chart_temperature_low) + " (${UnitTools.showUnit()})"
                tvAverageName.text =
                    "R${index + 1} " + context.getString(LibR.string.album_report_mean_temperature) + " (${UnitTools.showUnit()})"
                tvExplainName.text =
                    "R${index + 1} " + context.getString(LibR.string.album_report_comment)
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

    private fun setSwitchListener(
        switchCompat: SwitchCompat,
        editText: EditText,
    ) {
        switchCompat.setOnCheckedChangeListener { _, isChecked ->
            editText.isVisible = isChecked
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\view\ReportIRShowView.kt =====

package com.mpdc4gsr.module.thermalunified.report.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.mpdc4gsr.libunified.app.utils.ScreenUtils
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.compat.dpToPx
import com.mpdc4gsr.module.thermalunified.report.bean.ReportIRBean
import com.mpdc4gsr.module.thermalunified.report.bean.ReportTempBean
import com.mpdc4gsr.libunified.R as LibR

class ReportIRShowView : LinearLayout {
    companion object {
        private const val TYPE_FULL = 0
        private const val TYPE_POINT = 1
        private const val TYPE_LINE = 2
        private const val TYPE_RECT = 3
    }

    private lateinit var clImage: View
    private lateinit var clFull: View
    private lateinit var clPoint1: View
    private lateinit var clPoint2: View
    private lateinit var clPoint3: View
    private lateinit var clPoint4: View
    private lateinit var clPoint5: View
    private lateinit var clLine1: View
    private lateinit var clLine2: View
    private lateinit var clLine3: View
    private lateinit var clLine4: View
    private lateinit var clLine5: View
    private lateinit var clRect1: View
    private lateinit var clRect2: View
    private lateinit var clRect3: View
    private lateinit var clRect4: View
    private lateinit var clRect5: View

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        inflate(context, R.layout.view_report_ir_show, this)
        initViews()
        initTitleTexts()
    }

    private fun initViews() {
        clImage = findViewById(R.id.cl_image)
        clFull = findViewById(R.id.cl_full)
        clPoint1 = findViewById(R.id.cl_point1)
        clPoint2 = findViewById(R.id.cl_point2)
        clPoint3 = findViewById(R.id.cl_point3)
        clPoint4 = findViewById(R.id.cl_point4)
        clPoint5 = findViewById(R.id.cl_point5)
        clLine1 = findViewById(R.id.cl_line1)
        clLine2 = findViewById(R.id.cl_line2)
        clLine3 = findViewById(R.id.cl_line3)
        clLine4 = findViewById(R.id.cl_line4)
        clLine5 = findViewById(R.id.cl_line5)
        clRect1 = findViewById(R.id.cl_rect1)
        clRect2 = findViewById(R.id.cl_rect2)
        clRect3 = findViewById(R.id.cl_rect3)
        clRect4 = findViewById(R.id.cl_rect4)
        clRect5 = findViewById(R.id.cl_rect5)
    }

    private fun initTitleTexts() {
        initTitleText(clFull, TYPE_FULL, 0)
        initTitleText(clPoint1, TYPE_POINT, 0)
        initTitleText(clPoint2, TYPE_POINT, 1)
        initTitleText(clPoint3, TYPE_POINT, 2)
        initTitleText(clPoint4, TYPE_POINT, 3)
        initTitleText(clPoint5, TYPE_POINT, 4)
        initTitleText(clLine1, TYPE_LINE, 0)
        initTitleText(clLine2, TYPE_LINE, 1)
        initTitleText(clLine3, TYPE_LINE, 2)
        initTitleText(clLine4, TYPE_LINE, 3)
        initTitleText(clLine5, TYPE_LINE, 4)
        initTitleText(clRect1, TYPE_RECT, 0)
        initTitleText(clRect2, TYPE_RECT, 1)
        initTitleText(clRect3, TYPE_RECT, 2)
        initTitleText(clRect4, TYPE_RECT, 3)
        initTitleText(clRect5, TYPE_RECT, 4)
    }

    private fun initTitleText(
        itemRoot: View,
        type: Int,
        index: Int,
    ) {
        val tvTitle = itemRoot.findViewById<TextView>(R.id.tv_title)
        val tvAverageTitle = itemRoot.findViewById<TextView>(R.id.tv_average_title)
        val tvExplainTitle = itemRoot.findViewById<TextView>(R.id.tv_explain_title)
        tvTitle.isVisible = index == 0
        tvTitle.text =
            when (type) {
                TYPE_FULL -> context.getString(LibR.string.thermal_full_rect)
                TYPE_POINT -> context.getString(LibR.string.thermal_point) + "(P)"
                TYPE_LINE -> context.getString(LibR.string.thermal_line) + "(L)"
                else -> context.getString(LibR.string.thermal_rect) + "(R)"
            }
        tvAverageTitle.text =
            when (type) {
                TYPE_FULL, TYPE_POINT -> ""
                TYPE_LINE -> "L${index + 1} " + context.getString(LibR.string.album_report_mean_temperature)
                else -> "R${index + 1} " + context.getString(LibR.string.album_report_mean_temperature)
            }
        tvExplainTitle.text =
            when (type) {
                TYPE_FULL -> context.getString(LibR.string.album_report_comment)
                TYPE_POINT -> "P${index + 1} " + context.getString(LibR.string.album_report_comment)
                TYPE_LINE -> "L${index + 1} " + context.getString(LibR.string.album_report_comment)
                else -> "R${index + 1} " + context.getString(LibR.string.album_report_comment)
            }
    }

    fun getPrintViewList(): ArrayList<View> {
        val result = ArrayList<View>()
        result.add(clImage)
        getItemChild(clFull, result)
        getItemChild(clPoint1, result)
        getItemChild(clPoint2, result)
        getItemChild(clPoint3, result)
        getItemChild(clPoint4, result)
        getItemChild(clPoint5, result)
        getItemChild(clLine1, result)
        getItemChild(clLine2, result)
        getItemChild(clLine3, result)
        getItemChild(clLine4, result)
        getItemChild(clLine5, result)
        getItemChild(clRect1, result)
        getItemChild(clRect2, result)
        getItemChild(clRect3, result)
        getItemChild(clRect4, result)
        getItemChild(clRect5, result)
        return result
    }

    private fun getItemChild(
        itemRoot: View,
        resultList: ArrayList<View>,
    ) {
        if (itemRoot.isVisible) {
            val clRange = itemRoot.findViewById<View>(R.id.cl_range)
            val clAverage = itemRoot.findViewById<View>(R.id.cl_average)
            val clExplain = itemRoot.findViewById<View>(R.id.cl_explain)
            if (clRange.isVisible) {
                resultList.add(clRange)
            }
            if (clAverage.isVisible) {
                resultList.add(clAverage)
            }
            if (clExplain.isVisible) {
                resultList.add(clExplain)
            }
        }
    }

    fun setImageDrawable(drawable: Drawable?) {
        val ivImage = findViewById<View>(R.id.iv_image)
        val isLand = (drawable?.intrinsicWidth ?: 0) > (drawable?.intrinsicHeight ?: 0)
        val width = (ScreenUtils.getScreenWidth(context) * (if (isLand) 234 else 175) / 375f).toInt()
        val height = (width * (drawable?.intrinsicHeight ?: 0).toFloat() / (drawable?.intrinsicWidth
            ?: 1)).toInt()
        val layoutParams = ivImage.layoutParams
        layoutParams.width = width
        layoutParams.height = height
        ivImage.layoutParams = layoutParams
        (ivImage as? android.widget.ImageView)?.setImageDrawable(drawable)
    }

    fun refreshData(
        isFirst: Boolean,
        isLast: Boolean,
        reportIRBean: ReportIRBean,
    ) {
        val tvHead = findViewById<TextView>(R.id.tv_head)
        val viewNotHead = findViewById<View>(R.id.view_not_head)
        val viewImageBg = findViewById<View>(R.id.view_image_bg)
        tvHead.isVisible = isFirst
        viewNotHead.isVisible = !isFirst
        viewImageBg.setBackgroundResource(if (isFirst) R.drawable.layer_report_ir_show_top_bg else R.drawable.layer_report_ir_show_item_bg)
        clImage.setPadding(0, if (isFirst) 20f.dpToPx(context).toInt() else 0, 0, 0)
        refreshItem(clFull, reportIRBean.full_graph_data, TYPE_FULL, 0)
        val pointList = reportIRBean.point_data
        for (i in pointList.indices) {
            when (i) {
                0 -> refreshItem(clPoint1, pointList[i], TYPE_POINT, i)
                1 -> refreshItem(clPoint2, pointList[i], TYPE_POINT, i)
                2 -> refreshItem(clPoint3, pointList[i], TYPE_POINT, i)
                3 -> refreshItem(clPoint4, pointList[i], TYPE_POINT, i)
                4 -> refreshItem(clPoint5, pointList[i], TYPE_POINT, i)
            }
        }
        val tvTitlePoint2 = clPoint2.findViewById<TextView>(R.id.tv_title)
        val tvTitlePoint3 = clPoint3.findViewById<TextView>(R.id.tv_title)
        val tvTitlePoint4 = clPoint4.findViewById<TextView>(R.id.tv_title)
        val tvTitlePoint5 = clPoint5.findViewById<TextView>(R.id.tv_title)
        tvTitlePoint2.isVisible = !clPoint1.isVisible
        tvTitlePoint3.isVisible = !clPoint1.isVisible && !clPoint2.isVisible
        tvTitlePoint4.isVisible = !clPoint1.isVisible && !clPoint2.isVisible && !clPoint3.isVisible
        tvTitlePoint5.isVisible =
            !clPoint1.isVisible && !clPoint2.isVisible && !clPoint3.isVisible && !clPoint4.isVisible
        val lineList = reportIRBean.line_data
        for (i in lineList.indices) {
            when (i) {
                0 -> refreshItem(clLine1, lineList[i], TYPE_LINE, i)
                1 -> refreshItem(clLine2, lineList[i], TYPE_LINE, i)
                2 -> refreshItem(clLine3, lineList[i], TYPE_LINE, i)
                3 -> refreshItem(clLine4, lineList[i], TYPE_LINE, i)
                4 -> refreshItem(clLine5, lineList[i], TYPE_LINE, i)
            }
        }
        val tvTitleLine2 = clLine2.findViewById<TextView>(R.id.tv_title)
        val tvTitleLine3 = clLine3.findViewById<TextView>(R.id.tv_title)
        val tvTitleLine4 = clLine4.findViewById<TextView>(R.id.tv_title)
        val tvTitleLine5 = clLine5.findViewById<TextView>(R.id.tv_title)
        tvTitleLine2.isVisible = !clLine1.isVisible
        tvTitleLine3.isVisible = !clLine1.isVisible && !clLine2.isVisible
        tvTitleLine4.isVisible = !clLine1.isVisible && !clLine2.isVisible && !clLine3.isVisible
        tvTitleLine5.isVisible =
            !clLine1.isVisible && !clLine2.isVisible && !clLine3.isVisible && !clLine4.isVisible
        val rectList = reportIRBean.surface_data
        for (i in rectList.indices) {
            when (i) {
                0 -> refreshItem(clRect1, rectList[i], TYPE_RECT, i)
                1 -> refreshItem(clRect2, rectList[i], TYPE_RECT, i)
                2 -> refreshItem(clRect3, rectList[i], TYPE_RECT, i)
                3 -> refreshItem(clRect4, rectList[i], TYPE_RECT, i)
                4 -> refreshItem(clRect5, rectList[i], TYPE_RECT, i)
            }
        }
        val tvTitleRect2 = clRect2.findViewById<TextView>(R.id.tv_title)
        val tvTitleRect3 = clRect3.findViewById<TextView>(R.id.tv_title)
        val tvTitleRect4 = clRect4.findViewById<TextView>(R.id.tv_title)
        val tvTitleRect5 = clRect5.findViewById<TextView>(R.id.tv_title)
        tvTitleRect2.isVisible = !clRect1.isVisible
        tvTitleRect3.isVisible = !clRect1.isVisible && !clRect2.isVisible
        tvTitleRect4.isVisible = !clRect1.isVisible && !clRect2.isVisible && !clRect3.isVisible
        tvTitleRect5.isVisible =
            !clRect1.isVisible && !clRect2.isVisible && !clRect3.isVisible && !clRect4.isVisible
        if (rectList.isNotEmpty()) {
            when (rectList.size) {
                1 -> hideLastLine(isLast, clRect1, rectList[0], TYPE_RECT)
                2 -> hideLastLine(isLast, clRect2, rectList[1], TYPE_RECT)
                3 -> hideLastLine(isLast, clRect3, rectList[2], TYPE_RECT)
                4 -> hideLastLine(isLast, clRect4, rectList[3], TYPE_RECT)
                5 -> hideLastLine(isLast, clRect5, rectList[4], TYPE_RECT)
            }
            return
        }
        if (lineList.isNotEmpty()) {
            when (lineList.size) {
                1 -> hideLastLine(isLast, clLine1, lineList[0], TYPE_LINE)
                2 -> hideLastLine(isLast, clLine2, lineList[1], TYPE_LINE)
                3 -> hideLastLine(isLast, clLine3, lineList[2], TYPE_LINE)
                4 -> hideLastLine(isLast, clLine4, lineList[3], TYPE_LINE)
                5 -> hideLastLine(isLast, clLine5, lineList[4], TYPE_LINE)
            }
            return
        }
        if (pointList.isNotEmpty()) {
            when (pointList.size) {
                1 -> hideLastLine(isLast, clPoint1, pointList[0], TYPE_POINT)
                2 -> hideLastLine(isLast, clPoint2, pointList[1], TYPE_POINT)
                3 -> hideLastLine(isLast, clPoint3, pointList[2], TYPE_POINT)
                4 -> hideLastLine(isLast, clPoint4, pointList[3], TYPE_POINT)
                5 -> hideLastLine(isLast, clPoint5, pointList[4], TYPE_POINT)
            }
            return
        }
        hideLastLine(isLast, clFull, reportIRBean.full_graph_data, TYPE_FULL)
    }

    private fun hideLastLine(
        isLast: Boolean,
        itemRoot: View,
        tempBean: ReportTempBean?,
        type: Int,
    ) {
        if (tempBean == null) {
            return
        }
        val viewLineExplain = itemRoot.findViewById<View>(R.id.view_line_explain)
        val viewLineAverage = itemRoot.findViewById<View>(R.id.view_line_average)
        val viewLineRange = itemRoot.findViewById<View>(R.id.view_line_range)
        if (tempBean.isExplainOpen()) {
            viewLineExplain.isVisible = !isLast
        } else if ((type == TYPE_LINE || type == TYPE_RECT) && tempBean.isAverageOpen()) {
            viewLineAverage.isVisible = !isLast
        } else {
            viewLineRange.isVisible = !isLast
        }
    }

    private fun refreshItem(
        itemRoot: View,
        tempBean: ReportTempBean?,
        type: Int,
        index: Int,
    ) {
        if (tempBean == null) {
            itemRoot.isVisible = false
            return
        }
        itemRoot.isVisible =
            when (type) {
                TYPE_FULL -> tempBean.isMaxOpen() || tempBean.isMinOpen() || tempBean.isExplainOpen()
                TYPE_POINT -> tempBean.isTempOpen() || tempBean.isExplainOpen()
                else -> tempBean.isMaxOpen() || tempBean.isMinOpen() || tempBean.isAverageOpen() || tempBean.isExplainOpen()
            }
        if (!itemRoot.isVisible) {
            return
        }
        val rangeTitle =
            if (type == TYPE_POINT) {
                "P${index + 1} " + context.getString(LibR.string.chart_temperature)
            } else {
                val prefix =
                    when (type) {
                        TYPE_LINE -> "L${index + 1} "
                        TYPE_RECT -> "R${index + 1} "
                        else -> ""
                    }
                prefix +
                        if (tempBean.isMinOpen() && tempBean.isMaxOpen()) {
                            context.getString(LibR.string.chart_temperature_low) + "-" + context.getString(
                                LibR.string.chart_temperature_high
                            )
                        } else if (tempBean.isMinOpen()) {
                            context.getString(LibR.string.chart_temperature_low)
                        } else {
                            context.getString(LibR.string.chart_temperature_high)
                        }
            }
        val rangeValue =
            if (type == TYPE_POINT) {
                tempBean.temperature
            } else {
                if (tempBean.isMinOpen() && tempBean.isMaxOpen()) {
                    tempBean.min_temperature + "~" + tempBean.max_temperature
                } else if (tempBean.isMinOpen()) {
                    tempBean.min_temperature
                } else {
                    tempBean.max_temperature
                }
            }
        val tvRangeTitle = itemRoot.findViewById<TextView>(R.id.tv_range_title)
        val tvRangeValue = itemRoot.findViewById<TextView>(R.id.tv_range_value)
        val viewLineRange = itemRoot.findViewById<View>(R.id.view_line_range)
        val clAverage = itemRoot.findViewById<View>(R.id.cl_average)
        val clExplain = itemRoot.findViewById<View>(R.id.cl_explain)
        val tvAverageValue = itemRoot.findViewById<TextView>(R.id.tv_average_value)
        val tvExplainValue = itemRoot.findViewById<TextView>(R.id.tv_explain_value)
        tvRangeTitle.isVisible =
            if (type == TYPE_POINT) tempBean.isTempOpen() else tempBean.isMinOpen() || tempBean.isMaxOpen()
        tvRangeValue.isVisible =
            if (type == TYPE_POINT) tempBean.isTempOpen() else tempBean.isMinOpen() || tempBean.isMaxOpen()
        viewLineRange.isVisible =
            if (type == TYPE_POINT) tempBean.isTempOpen() else tempBean.isMinOpen() || tempBean.isMaxOpen()
        clAverage.isVisible = (type == TYPE_LINE || type == TYPE_RECT) && tempBean.isAverageOpen()
        clExplain.isVisible = tempBean.isExplainOpen()
        tvRangeTitle.text = rangeTitle
        tvRangeValue.text = rangeValue
        tvAverageValue.text = tempBean.mean_temperature
        tvExplainValue.text = tempBean.comment
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\view\WatermarkView.kt =====

package com.mpdc4gsr.module.thermalunified.report.view

import android.content.Context
import android.graphics.Canvas
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import com.mpdc4gsr.module.thermalunified.compat.dpToPx
import com.mpdc4gsr.module.thermalunified.compat.spToPx

class WatermarkView : View {
    var watermarkText: String? = null
        set(value) {
            field = value
            invalidate()
        }
    private var marginTop: Float = 0f
    private val textPaint: TextPaint = TextPaint()

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        marginTop = 220f.dpToPx(context)
        textPaint.isFakeBoldText = true
        textPaint.isAntiAlias = true
        textPaint.color = 0x082b79d8
        textPaint.textSize = 80f.spToPx(context)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        watermarkText?.let {
            var hasAddCount = 0
            var hasUseHeight = 0f
            while (hasUseHeight < height + marginTop) {
                canvas?.save()
                canvas?.rotate(15f)
                val translateX =
                    (width - textPaint.measureText(it)).coerceAtLeast(0f) / 2f + if (hasAddCount % 2 == 0) 100f else 0f
                canvas?.translate(translateX, 0f)
                canvas?.drawText(it, 0f, 0f, textPaint)
                canvas?.restore()
                canvas?.translate(0f, marginTop)
                hasUseHeight += marginTop
                hasAddCount++
            }
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\viewmodel\ModernPdfViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.report.viewmodel

import android.text.TextUtils
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.lms.LMS
import com.mpdc4gsr.libunified.app.lms.network.IResponseCallback
import com.mpdc4gsr.libunified.app.lms.utils.NetworkUtils
import com.mpdc4gsr.libunified.app.lms.utils.StringUtils
import com.mpdc4gsr.libunified.app.lms.utils.TLog
import com.mpdc4gsr.libunified.app.repository.BaseRepository
import com.mpdc4gsr.libunified.app.utils.HttpHelp
import com.mpdc4gsr.module.thermalunified.compat.ContextProvider
import com.mpdc4gsr.module.thermalunified.report.bean.ReportData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CountDownLatch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ModernPdfViewModel : BaseViewModel() {
    // Modern StateFlow-based state management
    private val _reportDataState = MutableStateFlow<ReportDataState>(ReportDataState.Idle)
    val reportDataState: StateFlow<ReportDataState> = _reportDataState.asStateFlow()
    private val _paginationState = MutableStateFlow(PaginationState())
    val paginationState: StateFlow<PaginationState> = _paginationState.asStateFlow()

    // One-time events using SharedFlow
    private val _events = MutableSharedFlow<PdfEvent>()
    val events: SharedFlow<PdfEvent> = _events.asSharedFlow()

    // Data classes for type-safe state management
    sealed class ReportDataState {
        object Idle : ReportDataState()
        object Loading : ReportDataState()
        data class Success(val data: ReportData, val isLoadMore: Boolean = false) :
            ReportDataState()

        data class Error(val message: String, val code: Int = -1) : ReportDataState()
        object NoNetwork : ReportDataState()
    }

    data class PaginationState(
        val currentPage: Int = 1,
        val hasMorePages: Boolean = true,
        val totalPages: Int = 0,
        val isLoadingMore: Boolean = false
    )

    sealed class PdfEvent {
        data class ShowToast(val message: String) : PdfEvent()
        data class ShowError(val message: String) : PdfEvent()
        data class NavigateToReport(val reportId: String) : PdfEvent()
        object RefreshCompleted : PdfEvent()
        data class ShareReport(val reportData: ReportData) : PdfEvent()
    }

    // Repository instance
    private val reportRepository = ReportRepository()
    fun getReportData(
        isTC007: Boolean,
        page: Int = 1,
        forceRefresh: Boolean = false
    ) {
        launchWithLoading {
            try {
                // Check network connectivity first
                if (!NetworkUtils.isConnected(ContextProvider.getContext())) {
                    _reportDataState.value = ReportDataState.NoNetwork
                    _events.emit(PdfEvent.ShowError("No network connection available"))
                    return@launchWithLoading
                }
                // Update loading states
                if (page == 1) {
                    _reportDataState.value = ReportDataState.Loading
                } else {
                    _paginationState.value = _paginationState.value.copy(isLoadingMore = true)
                }
                // Fetch data through repository
                val result = reportRepository.getReportData(isTC007, page, forceRefresh)
                when (result) {
                    is BaseRepository.Result.Success -> {
                        val reportData = result.data
                        _reportDataState.value = ReportDataState.Success(
                            data = reportData,
                            isLoadMore = page > 1
                        )
                        // Update pagination state
                        _paginationState.value = _paginationState.value.copy(
                            currentPage = page,
                            hasMorePages = reportData.hasMoreData(),
                            isLoadingMore = false
                        )
                        if (page == 1) {
                            _events.emit(PdfEvent.RefreshCompleted)
                        }
                    }

                    is BaseRepository.Result.Error -> {
                        val errorMessage = result.exception.message ?: "Unknown error occurred"
                        _reportDataState.value = ReportDataState.Error(errorMessage)
                        _paginationState.value = _paginationState.value.copy(isLoadingMore = false)
                        _events.emit(PdfEvent.ShowError(errorMessage))
                    }

                    is BaseRepository.Result.Loading -> {
                        // Already handled above
                    }
                }
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Failed to load report data"
                _reportDataState.value = ReportDataState.Error(errorMessage)
                _paginationState.value = _paginationState.value.copy(isLoadingMore = false)
                _events.emit(PdfEvent.ShowError(errorMessage))
            }
        }
    }

    fun loadNextPage(isTC007: Boolean) {
        val currentState = _paginationState.value
        if (currentState.hasMorePages && !currentState.isLoadingMore) {
            getReportData(isTC007, currentState.currentPage + 1)
        }
    }

    fun refreshData(isTC007: Boolean) {
        getReportData(isTC007, 1, forceRefresh = true)
    }

    fun navigateToReport(reportId: String) {
        launchWithErrorHandling {
            _events.emit(PdfEvent.NavigateToReport(reportId))
        }
    }

    fun shareReport(reportData: ReportData) {
        launchWithErrorHandling {
            _events.emit(PdfEvent.ShareReport(reportData))
        }
    }

    fun clearErrorState() {
        super.clearError()
        if (_reportDataState.value is ReportDataState.Error) {
            _reportDataState.value = ReportDataState.Idle
        }
    }

    fun resetStates() {
        _reportDataState.value = ReportDataState.Idle
        _paginationState.value = PaginationState()
    }

    private inner class ReportRepository : BaseRepository() {
        private val cacheKey = "report_data"
        suspend fun getReportData(
            isTC007: Boolean,
            page: Int,
            forceRefresh: Boolean = false
        ): BaseRepository.Result<ReportData> = safeCall {
            val key = "${cacheKey}_${isTC007}_$page"
            if (!forceRefresh) {
                // Try cached data first (5 minute cache)
                getCachedOrExecute(key, 5 * 60 * 1000L) {
                    fetchReportDataFromNetwork(isTC007, page)
                }
            } else {
                // Force refresh - clear cache and fetch
                clearCache(key)
                fetchReportDataFromNetwork(isTC007, page)
            }
        }

        private suspend fun fetchReportDataFromNetwork(
            isTC007: Boolean,
            page: Int
        ): ReportData = suspendCancellableCoroutine { continuation ->
            val downLatch = CountDownLatch(1)
            var result: ReportData? = null
            var error: Exception? = null
            HttpHelp.getFirstReportData(
                isTC007,
                page,
                object : IResponseCallback {
                    override fun onResponse(response: String?) {
                        try {
                            result = if (!response.isNullOrEmpty()) {
                                Gson().fromJson(response, ReportData::class.java)
                            } else {
                                ReportData().apply {
                                    code = -1
                                    msg = "Empty response from server"
                                }
                            }
                        } catch (e: Exception) {
                            error = Exception("JSON parsing error: ${e.message}")
                        } finally {
                            downLatch.countDown()
                        }
                    }

                    override fun onFail(exception: Exception?) {
                        error = exception ?: Exception("Network request failed")
                        result = ReportData().apply {
                            msg = exception?.message ?: "Network error"
                            code = -1
                        }
                        downLatch.countDown()
                        TLog.e("ModernPdfViewModel", "Network error: ${exception?.message}")
                    }

                    override fun onFail(failMsg: String?, errorCode: String) {
                        super.onFail(failMsg, errorCode)
                        // Handle localized error messages
                        try {
                            val localizedMessage = StringUtils.getResString(
                                LMS.mContext,
                                if (TextUtils.isEmpty(errorCode)) -500 else errorCode.toInt()
                            )
                            // Emit toast event on main thread
                            viewModelScope.launch {
                                _events.emit(PdfEvent.ShowToast(localizedMessage))
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        error = Exception(failMsg ?: "Server error")
                        result = ReportData().apply {
                            msg = failMsg
                            code = if (TextUtils.isEmpty(errorCode)) -500 else errorCode.toInt()
                        }
                        downLatch.countDown()
                    }
                }
            )
            continuation.invokeOnCancellation {
                downLatch.countDown()
            }
            // Wait for network response in IO dispatcher
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    downLatch.await()
                    when {
                        error != null -> continuation.resumeWithException(error!!)
                        result != null -> continuation.resume(result!!)
                        else -> continuation.resumeWithException(Exception("Unknown error occurred"))
                    }
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
        }
    }

    // Extension functions for ReportData
    private fun ReportData.hasMoreData(): Boolean {
        return code == 200 && data?.records?.isNotEmpty() == true && data!!.records!!.size >= 20
    }

    companion object {
        private const val TAG = "ModernPdfViewModel"
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\viewmodel\PdfViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.report.viewmodel

import android.text.TextUtils
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.lms.LMS
import com.mpdc4gsr.libunified.app.lms.network.IResponseCallback
import com.mpdc4gsr.libunified.app.lms.utils.NetworkUtils
import com.mpdc4gsr.libunified.app.lms.utils.StringUtils
import com.mpdc4gsr.libunified.app.lms.utils.TLog
import com.mpdc4gsr.libunified.app.lms.weiget.TToast
import com.mpdc4gsr.libunified.app.utils.HttpHelp
import com.mpdc4gsr.module.thermalunified.compat.ContextProvider
import com.mpdc4gsr.module.thermalunified.report.bean.ReportData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CountDownLatch
import com.mpdc4gsr.libunified.R as LibR

class PdfViewModel : BaseViewModel() {
    val listData = MutableLiveData<ReportData?>()
    fun getReportData(
        isTC007: Boolean,
        page: Int,
    ) {
        if (!NetworkUtils.isConnected(ContextProvider.getContext())) {
            TToast.shortToast(ContextProvider.getContext(), LibR.string.http_code_z5004)
            listData.postValue(null)
            return
        }
        viewModelScope.launch {
            val data = getReportDataRepository(isTC007, page)
            listData.postValue(data)
        }
    }

    private suspend fun getReportDataRepository(
        isTC007: Boolean,
        page: Int,
    ): ReportData? {
        var result: ReportData? = null
        val downLatch = CountDownLatch(1)
        HttpHelp.getFirstReportData(
            isTC007,
            page,
            object : IResponseCallback {
                override fun onResponse(p0: String?) {
                    result = Gson().fromJson(p0, ReportData::class.java)
                    downLatch.countDown()
                }

                override fun onFail(p0: Exception?) {
                    result = ReportData()
                    result?.msg = p0?.message
                    result?.code = -1
                    downLatch.countDown()
                    TLog.e("bcf", "ï¼š" + p0?.message)
                }

                override fun onFail(
                    failMsg: String?,
                    errorCode: String,
                ) {
                    super.onFail(failMsg, errorCode)
                    try {
                        StringUtils.getResString(
                            LMS.mContext,
                            if (TextUtils.isEmpty(errorCode)) -500 else errorCode.toInt(),
                        ).let {
                            TToast.shortToast(LMS.mContext, it)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            },
        )
        withContext(Dispatchers.IO) {
            downLatch.await()
        }
        return result
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\report\viewmodel\UpReportViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.report.viewmodel

import androidx.lifecycle.viewModelScope
import com.elvishew.xlog.XLog
import com.google.gson.Gson
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.lms.LMS
import com.mpdc4gsr.libunified.app.lms.UrlConstants
import com.mpdc4gsr.libunified.app.lms.bean.CommonBean
import com.mpdc4gsr.libunified.app.lms.network.HttpProxy
import com.mpdc4gsr.libunified.app.lms.network.IResponseCallback
import com.mpdc4gsr.libunified.app.lms.network.ResponseBean
import com.mpdc4gsr.libunified.app.lms.xutils.http.RequestParams
import com.mpdc4gsr.libunified.app.utils.SingleLiveEvent
import com.mpdc4gsr.module.thermalunified.report.bean.ReportBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CountDownLatch

class UpReportViewModel : BaseViewModel() {
    val commonBeanLD = SingleLiveEvent<CommonBean>()
    val exceptionLD = SingleLiveEvent<Exception?>()
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    fun upload(
        isTC007: Boolean,
        reportBean: ReportBean?,
    ) {
        viewModelScope.launch {
            uploadImages(reportBean)
            uploadJSON(isTC007, reportBean)
        }
    }

    private suspend fun uploadImages(reportBean: ReportBean?) {
        withContext(Dispatchers.IO) {
            val irList = reportBean?.infrared_data
            if (irList != null) {
                val downLatch = CountDownLatch(irList.size)
                for (reportIrBean in irList) {
                    if (reportIrBean.picture_id.isNotEmpty()) {
                        downLatch.countDown()
                        continue
                    }
                    val file = File(reportIrBean.picture_url)
                    LMS.getInstance().uploadFile(file, 0, 13, 20) { response ->
                        try {
                            if (response != null) {
                                val jsonObject = JSONObject(response)
                                val code = jsonObject.optString("code", "")
                                if (code == LMS.SUCCESS) {
                                    file.delete()
                                    val dataObject = jsonObject.optJSONObject("data")
                                    if (dataObject != null) {
                                        reportIrBean.picture_id =
                                            dataObject.optString("fileSecret", "")
                                        reportIrBean.picture_url = dataObject.optString("url", "")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            XLog.e("Error parsing upload response", e)
                        }
                        XLog.i("Upload")
                        downLatch.countDown()
                    }
                }
                downLatch.await()
                XLog.i("${irList.size} Upload")
            }
        }
    }

    private suspend fun uploadJSON(
        isTC007: Boolean,
        reportBean: ReportBean?,
    ) {
        withContext(Dispatchers.IO) {
            val url = UrlConstants.BASE_URL + "api/v1/outProduce/testReport/addTestReport"
            val params = RequestParams()
            params.addBodyParameter("reportType", 2)
            params.addBodyParameter(
                "modelId",
                if (isTC007) 1783 else 950
            )
            params.addBodyParameter("testTime", dateFormat.format(Date()))
            params.addBodyParameter("testInfo", gson.toJson(reportBean))
            params.addBodyParameter("sn", "")
            HttpProxy.getInstant().post(
                url,
                params,
                object : IResponseCallback {
                    override fun onResponse(response: String?) {
                        commonBeanLD.postValue(ResponseBean.convertCommonBean(response, null))
                    }

                    override fun onFail(exception: Exception?) {
                        exceptionLD.postValue(exception)
                    }
                },
            )
        }
    }
}


