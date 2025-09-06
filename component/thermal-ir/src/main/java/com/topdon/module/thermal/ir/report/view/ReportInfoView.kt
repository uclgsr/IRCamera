package com.topdon.module.thermal.ir.report.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.topdon.module.thermal.ir.R
import com.topdon.module.thermal.ir.report.bean.ReportConditionBean
import com.topdon.module.thermal.ir.report.bean.ReportInfoBean

/**
 * 报告信息 - 预览 View.
 */
class ReportInfoView: LinearLayout {

    // View declarations
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

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
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

    /**
     * 根据指定的报告信息刷新对应 View.
     */
    fun refreshInfo(reportInfoBean: ReportInfoBean?) {
        tvReportName.text = reportInfoBean?.report_name

        tvReportAuthor.isVisible = reportInfoBean?.is_report_author == 1
        tvReportAuthor.text = reportInfoBean?.report_author

        groupReportPlace.isVisible = reportInfoBean?.is_report_place == 1
        tvReportPlace.text = reportInfoBean?.report_place

        tvReportDate.isVisible = reportInfoBean?.is_report_date == 1
        tvReportDate.text = reportInfoBean?.report_date
    }

    /**
     * 根据指定的检测条件信息刷新对应 View.
     */
    fun refreshCondition(conditionBean: ReportConditionBean?) {
        clReportCondition.isVisible = conditionBean?.is_ambient_humidity == 1
                || conditionBean?.is_ambient_temperature == 1
                || conditionBean?.is_test_distance == 1
                || conditionBean?.is_emissivity == 1

        groupAmbientTemperature.isVisible = conditionBean?.is_ambient_temperature == 1
        tvAmbientTemperature.text = conditionBean?.ambient_temperature
        viewLine1.isVisible = conditionBean?.is_ambient_temperature == 1 &&
                (conditionBean.is_ambient_humidity == 1 || conditionBean.is_test_distance == 1 || conditionBean.is_emissivity == 1)

        groupAmbientHumidity.isVisible = conditionBean?.is_ambient_humidity == 1
        tvAmbientHumidity.text = conditionBean?.ambient_humidity
        viewLine2.isVisible = conditionBean?.is_ambient_humidity == 1 && (conditionBean.is_test_distance == 1 || conditionBean.is_emissivity == 1)

        groupTestDistance.isVisible = conditionBean?.is_test_distance == 1
        tvTestDistance.text = conditionBean?.test_distance
        viewLine3.isVisible = conditionBean?.is_test_distance == 1 && conditionBean.is_emissivity == 1

        groupEmissivity.isVisible = conditionBean?.is_emissivity == 1
        tvEmissivity.text = conditionBean?.emissivity
    }

    /**
     * 获取需要转为 PDF 的所有 View 列表.
     */
    fun getPrintViewList(): ArrayList<View> {
        val result = ArrayList<View>()
        result.add(clTop)
        result.add(clReportCondition)
        return result
    }
}