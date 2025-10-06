package com.mpdc4gsr.module.thermalunified.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.mpdc4gsr.module.thermalunified.compat.dpToPx
import com.mpdc4gsr.libunified.app.tools.UnitTools
import com.mpdc4gsr.libunified.ui.charts.LineChart
import com.mpdc4gsr.libunified.ui.components.Legend
import com.mpdc4gsr.libunified.ui.components.XAxis
import com.mpdc4gsr.libunified.ui.data.Entry
import com.mpdc4gsr.libunified.ui.data.LineData
import com.mpdc4gsr.libunified.ui.data.LineDataSet
import com.mpdc4gsr.libunified.ui.formatter.ValueFormatter
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.libunified.R as LibR
import com.mpdc4gsr.module.thermalunified.R as ThermalR

class ChartTrendView : LineChart {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        val textColor: Int = ContextCompat.getColor(context, LibR.color.chart_text)
        val axisChartColors: Int = ContextCompat.getColor(context, LibR.color.chart_axis)
        this.isDragEnabled = false
        this.isScaleYEnabled = false
        this.isScaleXEnabled = false
        this.isDoubleTapToZoomEnabled = false
        this.setScaleEnabled(false)
        this.setPinchZoom(false)
        this.setTouchEnabled(true)
        this.setDrawGridBackground(false)
        this.description = null
        this.axisRight.isEnabled = false
        this.setExtraOffsets(
            0f,
            0f,
            8.dpToPx(context).toFloat(),
            4.dpToPx(context).toFloat(),
        )
        setNoDataText(context.getString(ThermalR.string.lms_http_code998))
        setNoDataTextColor(ContextCompat.getColor(context, LibR.color.chart_text))
        val mv = MyMarkerView(context, R.layout.marker_lay)
        mv.chartView = this
        marker = mv
        legend.form = Legend.LegendForm.CIRCLE
        legend.textColor = textColor
        legend.isEnabled = false
        val xAxis = this.xAxis
        xAxis.textColor = textColor
        xAxis.setDrawGridLines(false)
        xAxis.axisLineColor = 0x00000000
        xAxis.setAvoidFirstLastClipping(true)
        xAxis.isEnabled = true
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.isGranularityEnabled = true
        xAxis.textSize = 11f
        xAxis.isJumpFirstLabel = false
        xAxis.axisMinimum = 0f
        xAxis.axisMaximum = 10f
        xAxis.setLabelCount(3, true)
        xAxis.valueFormatter =
            object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    if (value < 5) {
                        return "A"
                    }
                    if (value > 5) {
                        return "B"
                    }
                    return ""
                }
            }
        val leftAxis = this.axisLeft
        leftAxis.textColor = textColor
        leftAxis.axisLineColor = 0x00000000
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = axisChartColors
        leftAxis.gridLineWidth = 1.5f
        leftAxis.setLabelCount(6, true)
        leftAxis.valueFormatter =
            object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String = ""
            }
        leftAxis.textSize = 11f
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = 50f
        data = LineData()
    }

    fun setToEmpty() {
        axisLeft.valueFormatter =
            object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String = ""
            }
        data = LineData()
        invalidate()
    }

    fun refresh(tempList: List<Float>) {
        if (tempList.isEmpty()) {
            setToEmpty()
            return
        }
        xAxis.axisMinimum = 0f
        xAxis.axisMaximum = (tempList.size - 1).toFloat()
        xAxis.setLabelCount(3, true)
        xAxis.valueFormatter =
            object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    if (value < tempList.size / 3) {
                        return "A"
                    }
                    if (value > tempList.size * 2 / 3) {
                        return "B"
                    }
                    return ""
                }
            }
        var max = tempList.first()
        var min = tempList.first()
        val entryList: ArrayList<Entry> = ArrayList(tempList.size)
        for (i in tempList.indices) {
            val tempValue = tempList[i]
            max = max.coerceAtLeast(tempValue)
            min = min.coerceAtMost(tempValue)
            entryList.add(Entry(i.toFloat(), UnitTools.showUnitValue(tempValue)))
        }
        val maxUnit = UnitTools.showUnitValue(max)
        val minUnit = UnitTools.showUnitValue(min)
        axisLeft.axisMaximum = (maxUnit + (maxUnit - minUnit) / 3).coerceAtLeast(maxUnit + 0.3f)
        axisLeft.axisMinimum = (minUnit - (maxUnit - minUnit) / 3).coerceAtMost(minUnit - 0.3f)
        axisLeft.valueFormatter =
            object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String =
                    "${String.format("%.1f", value)}${UnitTools.showUnit()}"
            }
        val lineDataSet = LineDataSet(entryList, "point temp")
        lineDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        lineDataSet.color = 0xffffffff.toInt()
        lineDataSet.circleHoleColor = 0xffffffff.toInt()
        lineDataSet.setCircleColor(0xffffffff.toInt())
        lineDataSet.valueTextColor = Color.WHITE
        lineDataSet.lineWidth = 2f
        lineDataSet.circleRadius = 1f
        lineDataSet.fillAlpha = 200
        lineDataSet.valueTextSize = 10f
        lineDataSet.setDrawValues(false)
        data = LineData(lineDataSet)
        invalidate()
    }
}
