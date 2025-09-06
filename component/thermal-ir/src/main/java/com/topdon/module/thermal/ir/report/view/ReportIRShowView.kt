package com.topdon.module.thermal.ir.report.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.blankj.utilcode.util.SizeUtils
import com.topdon.lib.core.utils.ScreenUtil
import com.topdon.module.thermal.ir.R
import com.topdon.lib.core.R as LibR
import com.topdon.module.thermal.ir.report.bean.ReportIRBean
import com.topdon.module.thermal.ir.report.bean.ReportTempBean

/**
 * 一项红外数据预览 View.
 *
 * 包含一张图片对应的 全图、点、线、面 预览信息.
 */
class ReportIRShowView: LinearLayout {
    companion object {
        private const val TYPE_FULL = 0 //全图
        private const val TYPE_POINT = 1//点
        private const val TYPE_LINE = 2 //线
        private const val TYPE_RECT = 3 //面
    }

    // View references - migrated from synthetic views
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

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
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

    private fun initTitleText(itemRoot: View, type: Int, index: Int) {
        val tvTitle = itemRoot.findViewById<TextView>(R.id.tv_title)
        val tvAverageTitle = itemRoot.findViewById<TextView>(R.id.tv_average_title)
        val tvExplainTitle = itemRoot.findViewById<TextView>(R.id.tv_explain_title)
        
        tvTitle.isVisible = index == 0
        tvTitle.text = when (type) {
            TYPE_FULL -> context.getString(LibR.string.thermal_full_rect)
            TYPE_POINT -> context.getString(LibR.string.thermal_point) + "(P)"
            TYPE_LINE -> context.getString(LibR.string.thermal_line) + "(L)"
            else -> context.getString(LibR.string.thermal_rect) + "(R)"
        }
        tvAverageTitle.text = when (type) {
            TYPE_FULL, TYPE_POINT -> "" //全图、点没有平均温
            TYPE_LINE -> "L${index + 1} " + context.getString(LibR.string.album_report_mean_temperature)
            else -> "R${index + 1} " + context.getString(LibR.string.album_report_mean_temperature)
        }
        tvExplainTitle.text = when (type) {
            TYPE_FULL -> context.getString(LibR.string.album_report_comment)
            TYPE_POINT -> "P${index + 1} " + context.getString(LibR.string.album_report_comment)
            TYPE_LINE -> "L${index + 1} " + context.getString(LibR.string.album_report_comment)
            else -> "R${index + 1} " + context.getString(LibR.string.album_report_comment)
        }
    }

    /**
     * 获取需要转为 PDF 的所有 View 列表.
     */
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

    private fun getItemChild(itemRoot: View, resultList: ArrayList<View>) {
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
        val width = (ScreenUtil.getScreenWidth(context) * (if (isLand) 234 else 175) / 375f).toInt()
        val height = (width * (drawable?.intrinsicHeight ?: 0).toFloat() / (drawable?.intrinsicWidth ?: 1)).toInt()
        val layoutParams = ivImage.layoutParams
        layoutParams.width = width
        layoutParams.height = height
        ivImage.layoutParams = layoutParams
        (ivImage as? android.widget.ImageView)?.setImageDrawable(drawable)
    }

    fun refreshData(isFirst: Boolean, isLast: Boolean, reportIRBean: ReportIRBean) {
        val tvHead = findViewById<TextView>(R.id.tv_head)
        val viewNotHead = findViewById<View>(R.id.view_not_head)
        val viewImageBg = findViewById<View>(R.id.view_image_bg)
        
        tvHead.isVisible = isFirst
        viewNotHead.isVisible = !isFirst
        viewImageBg.setBackgroundResource(if (isFirst) R.drawable.layer_report_ir_show_top_bg else R.drawable.layer_report_ir_show_item_bg)
        clImage.setPadding(0, if (isFirst) SizeUtils.dp2px(20f) else 0, 0, 0)

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
        
        // Update title visibility with findViewById
        val tvTitlePoint2 = clPoint2.findViewById<TextView>(R.id.tv_title)
        val tvTitlePoint3 = clPoint3.findViewById<TextView>(R.id.tv_title)
        val tvTitlePoint4 = clPoint4.findViewById<TextView>(R.id.tv_title)
        val tvTitlePoint5 = clPoint5.findViewById<TextView>(R.id.tv_title)
        
        tvTitlePoint2.isVisible = !clPoint1.isVisible
        tvTitlePoint3.isVisible = !clPoint1.isVisible && !clPoint2.isVisible
        tvTitlePoint4.isVisible = !clPoint1.isVisible && !clPoint2.isVisible && !clPoint3.isVisible
        tvTitlePoint5.isVisible = !clPoint1.isVisible && !clPoint2.isVisible && !clPoint3.isVisible && !clPoint4.isVisible

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
        
        // Update line title visibility with findViewById
        val tvTitleLine2 = clLine2.findViewById<TextView>(R.id.tv_title)
        val tvTitleLine3 = clLine3.findViewById<TextView>(R.id.tv_title)
        val tvTitleLine4 = clLine4.findViewById<TextView>(R.id.tv_title)
        val tvTitleLine5 = clLine5.findViewById<TextView>(R.id.tv_title)
        
        tvTitleLine2.isVisible = !clLine1.isVisible
        tvTitleLine3.isVisible = !clLine1.isVisible && !clLine2.isVisible
        tvTitleLine4.isVisible = !clLine1.isVisible && !clLine2.isVisible && !clLine3.isVisible
        tvTitleLine5.isVisible = !clLine1.isVisible && !clLine2.isVisible && !clLine3.isVisible && !clLine4.isVisible

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
        
        // Update rect title visibility with findViewById
        val tvTitleRect2 = clRect2.findViewById<TextView>(R.id.tv_title)
        val tvTitleRect3 = clRect3.findViewById<TextView>(R.id.tv_title)
        val tvTitleRect4 = clRect4.findViewById<TextView>(R.id.tv_title)
        val tvTitleRect5 = clRect5.findViewById<TextView>(R.id.tv_title)
        
        tvTitleRect2.isVisible = !clRect1.isVisible
        tvTitleRect3.isVisible = !clRect1.isVisible && !clRect2.isVisible
        tvTitleRect4.isVisible = !clRect1.isVisible && !clRect2.isVisible && !clRect3.isVisible
        tvTitleRect5.isVisible = !clRect1.isVisible && !clRect2.isVisible && !clRect3.isVisible && !clRect4.isVisible

        // 把最后一条分割线藏起来
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

    private fun hideLastLine(isLast: Boolean, itemRoot: View, tempBean: ReportTempBean?, type: Int) {
        if (tempBean == null) {
            return
        }
        // Find the appropriate line view to hide based on what's visible
        val viewLineExplain = itemRoot.findViewById<View>(R.id.view_line_explain)
        val viewLineAverage = itemRoot.findViewById<View>(R.id.view_line_average)
        val viewLineRange = itemRoot.findViewById<View>(R.id.view_line_range)
        
        // Hide the last visible line divider
        if (tempBean.isExplainOpen()) {
            viewLineExplain.isVisible = !isLast
        } else if ((type == TYPE_LINE || type == TYPE_RECT) && tempBean.isAverageOpen()) {
            viewLineAverage.isVisible = !isLast
        } else {
            viewLineRange.isVisible = !isLast
        }
    }

    private fun refreshItem(itemRoot: View, tempBean: ReportTempBean?, type: Int, index: Int) {
        if (tempBean == null) {
            itemRoot.isVisible = false
            return
        }

        itemRoot.isVisible = when (type) {
            TYPE_FULL -> tempBean.isMaxOpen() || tempBean.isMinOpen() || tempBean.isExplainOpen()
            TYPE_POINT -> tempBean.isTempOpen() || tempBean.isExplainOpen()
            else -> tempBean.isMaxOpen() || tempBean.isMinOpen() || tempBean.isAverageOpen() || tempBean.isExplainOpen()
        }
        if (!itemRoot.isVisible) {
            return
        }

        val rangeTitle = if (type == TYPE_POINT) {
            "P${index + 1} " + context.getString(LibR.string.chart_temperature)
        } else {
            val prefix = when (type) {
                TYPE_LINE -> "L${index + 1} "
                TYPE_RECT -> "R${index + 1} "
                else -> ""
            }
            prefix + if (tempBean.isMinOpen() && tempBean.isMaxOpen()) {
                context.getString(LibR.string.chart_temperature_low) + "-" + context.getString(LibR.string.chart_temperature_high)
            } else if (tempBean.isMinOpen()) {
                context.getString(LibR.string.chart_temperature_low)
            } else {
                context.getString(LibR.string.chart_temperature_high)
            }
        }
        val rangeValue = if (type == TYPE_POINT) {
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

        tvRangeTitle.isVisible = if (type == TYPE_POINT) tempBean.isTempOpen() else tempBean.isMinOpen() || tempBean.isMaxOpen()
        tvRangeValue.isVisible = if (type == TYPE_POINT) tempBean.isTempOpen() else tempBean.isMinOpen() || tempBean.isMaxOpen()
        viewLineRange.isVisible = if (type == TYPE_POINT) tempBean.isTempOpen() else tempBean.isMinOpen() || tempBean.isMaxOpen()
        clAverage.isVisible = (type == TYPE_LINE || type == TYPE_RECT) && tempBean.isAverageOpen()
        clExplain.isVisible = tempBean.isExplainOpen()
        tvRangeTitle.text = rangeTitle
        tvRangeValue.text = rangeValue
        tvAverageValue.text = tempBean.mean_temperature
        tvExplainValue.text = tempBean.comment
    }
}