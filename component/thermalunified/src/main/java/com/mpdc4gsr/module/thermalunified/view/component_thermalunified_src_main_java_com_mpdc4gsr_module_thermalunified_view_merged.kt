// Merged ALL .kt and .java files from the 'component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view' directory and its subdirectories.
// Total files: 23 | Generated on: 2025-10-08 01:42:36


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\ChartLogView.kt =====

package com.mpdc4gsr.module.thermalunified.view

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.elvishew.xlog.XLog
import com.mpdc4gsr.libunified.app.db.entity.ThermalEntity
import com.mpdc4gsr.libunified.ui.charts.LineChart
import com.mpdc4gsr.libunified.ui.components.Legend
import com.mpdc4gsr.libunified.ui.components.XAxis
import com.mpdc4gsr.libunified.ui.components.YAxis
import com.mpdc4gsr.libunified.ui.data.Entry
import com.mpdc4gsr.libunified.ui.data.LineData
import com.mpdc4gsr.libunified.ui.data.LineDataSet
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.chart.IRMyValueFormatter
import com.mpdc4gsr.module.thermalunified.chart.YValueFormatter
import com.mpdc4gsr.module.thermalunified.compat.dpToPx
import com.mpdc4gsr.module.thermalunified.utils.ChartTools
import kotlinx.coroutines.*
import com.mpdc4gsr.libunified.R as LibR
import com.mpdc4gsr.libunified.R as LibcoreR
import com.mpdc4gsr.module.thermalunified.R as ThermalR

class ChartLogView : LineChart {
    private val mHandler by lazy { Handler(Looper.getMainLooper()) }
    private var viewScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle,
    ) {
        initChart()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Recreate the scope if it was cancelled
        if (!viewScope.isActive) {
            viewScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mHandler.removeCallbacksAndMessages(null)
        viewScope.cancel()
    }

    private val textColor by lazy { ContextCompat.getColor(context, LibcoreR.color.chart_text) }
    private val axisChartColors by lazy {
        ContextCompat.getColor(
            context,
            LibcoreR.color.chart_axis
        )
    }
    private val axisLine by lazy { ContextCompat.getColor(context, LibcoreR.color.circle_white) }
    private fun initChart() {
        synchronized(this) {
            this.setTouchEnabled(true)
            this.isDragEnabled = true
            this.setDrawGridBackground(false)
            this.description = null
            this.setBackgroundResource(ThermalR.color.chart_bg)
            this.setScaleEnabled(false)
            this.setPinchZoom(false)
            this.isDoubleTapToZoomEnabled = false
            this.isScaleYEnabled = false
            this.isScaleXEnabled = true
            this.setExtraOffsets(
                0f,
                0f,
                8f.dpToPx(context),
                4f.dpToPx(context),
            )
            setNoDataText(context.getString(ThermalR.string.lms_http_code998))
            setNoDataTextColor(ContextCompat.getColor(context, LibcoreR.color.chart_text))
            val mv = MyMarkerView(context, R.layout.marker_lay)
            mv.chartView = this
            marker = mv
            val data = LineData()
            data.setValueTextColor(textColor)
            this.data = data
            val l = this.legend
            l.form = Legend.LegendForm.CIRCLE
            l.textColor = textColor
            l.isEnabled = false
            val xAxis = this.xAxis
            xAxis.textColor = textColor
            xAxis.setDrawGridLines(false)
            xAxis.gridColor = axisChartColors
            xAxis.axisLineColor = 0x00000000
            xAxis.setAvoidFirstLastClipping(true)
            xAxis.isEnabled = true
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.isGranularityEnabled = true
            xAxis.textSize = 8f
            val leftAxis = this.axisLeft
            leftAxis.textColor = textColor
            leftAxis.axisLineColor = 0x00000000
            leftAxis.setDrawGridLines(true)
            leftAxis.gridColor = axisChartColors
            leftAxis.gridLineWidth = 1.5f
            leftAxis.setLabelCount(6, true)
            leftAxis.valueFormatter = YValueFormatter()
            leftAxis.textSize = 8f
            this.axisRight.isEnabled = false
        }
    }

    fun initEntry(
        data: ArrayList<ThermalEntity>,
        type: Int = 1,
    ) {
        synchronized(this) {
            val lifecycleOwner = findViewTreeLifecycleOwner()
            if (lifecycleOwner == null) {
                Log.e("ChartLogView", "No lifecycle owner found, cannot initialize chart")
                return
            }
            lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    clearEntity(data.size == 0)
                } catch (e: Exception) {
                    Log.e("chart", "clearEntity error: ${e.message}")
                }
                Log.w("chart", "clearEntity finish")
                if (data.size == 0) {
                    return@launch
                }
                Log.w("chart", "update chart start")
                val lineData: LineData = this@ChartLogView.data
                val startTime = data[0].createTime / 1000 * 1000
                xAxis.valueFormatter = IRMyValueFormatter(startTime = startTime, type = type)
                XLog.w("chart init startTime:$startTime")
                when (data[0].type) {
                    "point" -> {
                        var set = lineData.getDataSetByIndex(0)
                        if (set == null) {
                            set = createSet(0, "point temp")
                            lineData.addDataSet(set)
                        }
                        Log.w("123", "")
                        data.forEach {
                            val x =
                                ChartTools.getChartX(
                                    x = it.createTime,
                                    startTime = startTime,
                                    type = type,
                                ).toFloat()
                            val entity = Entry(x, it.thermal)
                            entity.data = it
                            set.addEntry(entity)
                        }
                        XLog.w("DataSet:${set.entryCount}")
                    }

                    "line" -> {
                        var maxDataSet = lineData.getDataSetByIndex(0)
                        if (maxDataSet == null) {
                            maxDataSet = createSet(0, "line max temp")
                        }
                        var minDataSet = lineData.getDataSetByIndex(1)
                        if (minDataSet == null) {
                            minDataSet = createSet(1, "line min temp")
                        }
                        Log.w("123", "")
                        data.forEach {
                            val x =
                                ChartTools.getChartX(
                                    x = it.createTime,
                                    startTime = startTime,
                                    type = type,
                                ).toFloat()
                            val entity = Entry(x, it.thermalMax)
                            entity.data = it
                            maxDataSet.addEntry(entity)
                            val entityMin = Entry(x, it.thermalMin)
                            entityMin.data = it
                            minDataSet.addEntry(entityMin)
                        }
                        lineData.addDataSet(maxDataSet)
                        lineData.addDataSet(minDataSet)
                        XLog.w("DataSet:${maxDataSet.entryCount}")
                    }

                    else -> {
                        var maxTempDataSet = lineData.getDataSetByIndex(0)
                        if (maxTempDataSet == null) {
                            maxTempDataSet = createSet(0, "fence max temp")
                            lineData.addDataSet(maxTempDataSet)
                        }
                        var centerTempDataSet = lineData.getDataSetByIndex(1)
                        if (centerTempDataSet == null) {
                            centerTempDataSet = createSet(1, "fence min temp")
                            lineData.addDataSet(centerTempDataSet)
                        }
                        Log.w("123", "")
                        data.forEach {
                            val x =
                                ChartTools.getChartX(
                                    x = it.createTime,
                                    startTime = startTime,
                                    type = type,
                                ).toFloat()
                            val entityMax = Entry(x, it.thermalMax)
                            entityMax.data = it
                            maxTempDataSet.addEntry(entityMax)
                            val entity = Entry(x, it.thermalMin)
                            entity.data = it
                            centerTempDataSet.addEntry(entity)
                        }
                        XLog.w("DataSet:${centerTempDataSet.entryCount}")
                    }
                }
                lineData.notifyDataChanged()
                notifyDataSetChanged()
                moveViewToX(xChartMin)
                setVisibleXRangeMinimum(ChartTools.getMinimum(type = type) / 2)
                setVisibleXRangeMaximum(ChartTools.getMaximum(type = type))
                zoom(1f, 1f, xChartMin, 0f)
                ChartTools.setX(this@ChartLogView, type)
                Log.w("chart", "update chart finish")
            }
        }
    }

    private val bgChartColors =
        intArrayOf(
            R.drawable.bg_chart_fill,
            R.drawable.bg_chart_fill2,
            R.drawable.bg_chart_fill3,
        )
    private val lineChartColors =
        intArrayOf(
            LibcoreR.color.chart_line_max,
            LibcoreR.color.chart_line_min,
            LibcoreR.color.chart_line_center,
        )
    private val linePointColors =
        intArrayOf(
            LibR.color.chart_point_max,
            LibR.color.chart_point_min,
            LibR.color.chart_point_center,
        )

    private fun createSet(
        index: Int,
        label: String,
    ): LineDataSet {
        val set = LineDataSet(null, label)
        set.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        set.setDrawFilled(false)
        set.fillDrawable = ContextCompat.getDrawable(context, bgChartColors[index])
        set.axisDependency = YAxis.AxisDependency.LEFT
        set.color = ContextCompat.getColor(context, lineChartColors[index])
        set.circleHoleColor = ContextCompat.getColor(context, linePointColors[index])
        set.setCircleColor(ContextCompat.getColor(context, lineChartColors[index]))
        set.valueTextColor = Color.WHITE
        set.lineWidth = 2f
        set.circleRadius = 1f
        set.fillAlpha = 200
        set.valueTextSize = 10f
        set.setDrawValues(false)
        return set
    }

    private fun clearEntity(isEmpty: Boolean) {
        initChart()
        if (isEmpty) {
            clear()
        } else {
            clearValues()
        }
    }
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\ChartMonitorView.kt =====

package com.mpdc4gsr.module.thermalunified.view

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import com.mpdc4gsr.libunified.app.db.entity.ThermalEntity
import com.mpdc4gsr.libunified.ui.charts.LineChart
import com.mpdc4gsr.libunified.ui.components.Legend
import com.mpdc4gsr.libunified.ui.components.XAxis
import com.mpdc4gsr.libunified.ui.components.YAxis
import com.mpdc4gsr.libunified.ui.data.Entry
import com.mpdc4gsr.libunified.ui.data.LineData
import com.mpdc4gsr.libunified.ui.data.LineDataSet
import com.mpdc4gsr.libunified.ui.listener.ChartTouchListener
import com.mpdc4gsr.libunified.ui.listener.OnChartGestureListener
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.chart.IRMyValueFormatter
import com.mpdc4gsr.module.thermalunified.chart.YValueFormatter
import com.mpdc4gsr.module.thermalunified.compat.dpToPx
import com.mpdc4gsr.module.thermalunified.utils.ChartTools
import com.mpdc4gsr.libunified.R as LibR
import com.mpdc4gsr.module.thermalunified.R as ThermalR

class ChartMonitorView : LineChart, OnChartGestureListener {
    private val mHandler by lazy { Handler(Looper.getMainLooper()) }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle,
    ) {
        initChart()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mHandler.removeCallbacksAndMessages(null)
    }

    private val textColor by lazy { ContextCompat.getColor(context, LibR.color.chart_text) }
    private val axisChartColors by lazy { ContextCompat.getColor(context, LibR.color.chart_axis) }
    private val axisLine by lazy { ContextCompat.getColor(context, LibR.color.circle_white) }
    private fun initChart() {
        synchronized(this) {
            this.setTouchEnabled(true)
            this.onChartGestureListener = this
            this.isDragEnabled = true
            this.setDrawGridBackground(false)
            this.description = null
            this.setBackgroundResource(ThermalR.color.chart_bg)
            this.setScaleEnabled(true)
            this.setPinchZoom(false)
            this.isDoubleTapToZoomEnabled = false
            this.isScaleYEnabled = false
            this.isScaleXEnabled = true
            this.setExtraOffsets(
                0f,
                0f,
                8f.dpToPx(context).toFloat(),
                4f.dpToPx(context).toFloat(),
            )
            setNoDataText(context.getString(ThermalR.string.lms_http_code998))
            setNoDataTextColor(ContextCompat.getColor(context, LibR.color.chart_text))
            val mv = MyMarkerView(context, R.layout.marker_lay)
            mv.chartView = this
            marker = mv
            val data = LineData()
            data.setValueTextColor(textColor)
            this.data = data
            val l = this.legend
            l.form = Legend.LegendForm.CIRCLE
            l.textColor = textColor
            l.isEnabled = false
            val xAxis = this.xAxis
            xAxis.textColor = textColor
            xAxis.setDrawGridLines(false)
            xAxis.gridColor = axisChartColors
            xAxis.axisLineColor = 0x00000000
            xAxis.setAvoidFirstLastClipping(true)
            xAxis.isEnabled = true
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.isGranularityEnabled = true
            xAxis.textSize = 8f
            val leftAxis = this.axisLeft
            leftAxis.textColor = textColor
            leftAxis.axisLineColor = 0x00000000
            leftAxis.setDrawGridLines(true)
            leftAxis.gridColor = axisChartColors
            leftAxis.gridLineWidth = 1.5f
            leftAxis.setLabelCount(6, true)
            leftAxis.valueFormatter = YValueFormatter()
            leftAxis.textSize = 8f
            this.axisRight.isEnabled = false
        }
    }

    private var startTime = 0L
    fun addPointToChart(
        bean: ThermalEntity,
        timeType: Int = 1,
        selectType: Int = 1,
    ) {
        synchronized(this) {
            try {
                if (bean.createTime == 0L) {
                    Log.w("123", "createTime = 0L, bean:$bean")
                    return
                }
                val lineData: LineData = this.data
                var volDataSet = lineData.getDataSetByIndex(0)
                if (volDataSet == null) {
                    startTime = bean.createTime
                    xAxis.valueFormatter =
                        IRMyValueFormatter(startTime = startTime, type = timeType)
                }
                val x =
                    ChartTools.getChartX(
                        x = bean.createTime,
                        startTime = startTime,
                        type = timeType,
                    ).toFloat()
                when (selectType) {
                    1 -> {
                        if (volDataSet == null) {
                            volDataSet = createSet(0, "point temp")
                            lineData.addDataSet(volDataSet)
                            Log.w("123", "volDataSet.entryCount:${volDataSet.entryCount}")
                        }
                        val entity = Entry(x, bean.thermal)
                        entity.data = bean
                        volDataSet.addEntry(entity)
                        Log.w("123", ":$entity")
                    }

                    2 -> {
                        if (volDataSet == null) {
                            volDataSet = createSet(0, "line max temp")
                            lineData.addDataSet(volDataSet)
                            Log.w("123", "volDataSet.entryCount:${volDataSet.entryCount}")
                        }
                        val entity = Entry(x, bean.thermalMax)
                        entity.data = bean
                        volDataSet.addEntry(entity)
                        var secondDataSet = lineData.getDataSetByIndex(1)
                        if (secondDataSet == null) {
                            secondDataSet = createSet(1, "line min temp")
                            lineData.addDataSet(secondDataSet)
                        }
                        val secondEntity = Entry(x, bean.thermalMin)
                        secondEntity.data = bean
                        secondDataSet.addEntry(secondEntity)
                    }

                    else -> {
                        if (volDataSet == null) {
                            volDataSet = createSet(0, "fence max temp")
                            lineData.addDataSet(volDataSet)
                        }
                        val entity = Entry(x, bean.thermalMax)
                        entity.data = bean
                        volDataSet.addEntry(entity)
                        var secondDataSet = lineData.getDataSetByIndex(1)
                        if (secondDataSet == null) {
                            secondDataSet = createSet(1, "fence min temp")
                            lineData.addDataSet(secondDataSet)
                        }
                        val secondEntity = Entry(x, bean.thermalMin)
                        secondEntity.data = bean
                        secondDataSet.addEntry(secondEntity)
                    }
                }
                lineData.notifyDataChanged()
                notifyDataSetChanged()
                setVisibleXRangeMinimum(ChartTools.getMinimum(type = timeType) / 2)
                setVisibleXRangeMaximum(ChartTools.getMaximum(type = timeType))
                ChartTools.setX(this, timeType)
                if ((highestVisibleX + ChartTools.getMinimum(timeType) / 2f) > xChartMax) {
                    moveViewToX(xChartMax)
                }
                if (volDataSet.entryCount == 10) {
                    zoom(100f, 1f, xChartMax, 0f)
                }
                return@synchronized
            } catch (e: Exception) {
                Log.e("123", ":${e.message}")
                return@synchronized
            }
        }
    }

    private val bgChartColors =
        intArrayOf(
            R.drawable.bg_chart_fill,
            R.drawable.bg_chart_fill2,
            R.drawable.bg_chart_fill3,
        )
    private val lineChartColors =
        intArrayOf(
            LibR.color.chart_line_max,
            LibR.color.chart_line_min,
            LibR.color.chart_line_center,
        )
    private val linePointColors =
        intArrayOf(
            LibR.color.chart_point_max,
            LibR.color.chart_point_min,
            LibR.color.chart_point_center,
        )

    private fun createSet(
        index: Int,
        label: String,
    ): LineDataSet {
        val set = LineDataSet(null, label)
        set.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        set.setDrawFilled(false)
        set.fillDrawable = ContextCompat.getDrawable(context, bgChartColors[index])
        set.axisDependency = YAxis.AxisDependency.LEFT
        set.color = ContextCompat.getColor(context, lineChartColors[index])
        set.circleHoleColor = ContextCompat.getColor(context, linePointColors[index])
        set.setCircleColor(ContextCompat.getColor(context, lineChartColors[index]))
        set.valueTextColor = Color.WHITE
        set.lineWidth = 2f
        set.circleRadius = 1f
        set.fillAlpha = 200
        set.valueTextSize = 10f
        set.setDrawValues(false)
        return set
    }

    override fun onChartGestureStart(
        me: MotionEvent?,
        lastPerformedGesture: ChartTouchListener.ChartGesture?,
    ) {
    }

    override fun onChartGestureEnd(
        me: MotionEvent?,
        lastPerformedGesture: ChartTouchListener.ChartGesture?,
    ) {
    }

    override fun onChartLongPressed(me: MotionEvent?) {
    }

    override fun onChartDoubleTapped(me: MotionEvent?) {
    }

    override fun onChartSingleTapped(me: MotionEvent?) {
    }

    override fun onChartFling(
        me1: MotionEvent?,
        me2: MotionEvent?,
        velocityX: Float,
        velocityY: Float,
    ) {
    }

    override fun onChartScale(
        me: MotionEvent?,
        scaleX: Float,
        scaleY: Float,
    ) {
        highlightValue(null)
    }

    override fun onChartTranslate(
        me: MotionEvent?,
        dX: Float,
        dY: Float,
    ) {
    }
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\ChartTrendView.kt =====

package com.mpdc4gsr.module.thermalunified.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.mpdc4gsr.libunified.app.tools.UnitTools
import com.mpdc4gsr.libunified.ui.charts.LineChart
import com.mpdc4gsr.libunified.ui.components.Legend
import com.mpdc4gsr.libunified.ui.components.XAxis
import com.mpdc4gsr.libunified.ui.data.Entry
import com.mpdc4gsr.libunified.ui.data.LineData
import com.mpdc4gsr.libunified.ui.data.LineDataSet
import com.mpdc4gsr.libunified.ui.formatter.ValueFormatter
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.compat.dpToPx
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


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\compass\CompassProvider.kt =====

package com.mpdc4gsr.module.thermalunified.view.compass

import android.content.Context
import android.hardware.Sensor
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.andromeda.sense.compass.ICompass
import com.kylecorry.andromeda.sense.compass.LegacyCompass
import com.kylecorry.andromeda.sense.orientation.GeomagneticRotationSensor
import com.kylecorry.andromeda.sense.orientation.RotationSensor

class CompassProvider(private val context: Context) {
    fun get(): ICompass {
        val smoothing = 1
        val useTrueNorth = true
        var source = CompassSource.RotationVector
        val allSources = getAvailableSources(context)
        if (allSources.isEmpty()) {
            return NullCompass()
        }
        if (!allSources.contains(source)) {
            source = allSources.firstOrNull() ?: CompassSource.CustomMagnetometer
        }
        val compass =
            when (source) {
                CompassSource.RotationVector -> {
                    RotationSensor(context, SensorService.MOTION_SENSOR_DELAY)
                }

                CompassSource.GeomagneticRotationVector -> {
                    GeomagneticRotationSensor(context, SensorService.MOTION_SENSOR_DELAY)
                }

                CompassSource.CustomMagnetometer -> {
                    RotationSensor(context, SensorService.MOTION_SENSOR_DELAY)
                }

                CompassSource.Orientation -> {
                    LegacyCompass(context, useTrueNorth, SensorService.MOTION_SENSOR_DELAY)
                }
            }
        return compass as ICompass
    }

    companion object {
        fun getAvailableSources(context: Context): List<CompassSource> {
            val sources = mutableListOf<CompassSource>()
            if (Sensors.hasSensor(context, Sensor.TYPE_ROTATION_VECTOR)) {
                sources.add(CompassSource.RotationVector)
            }
            if (Sensors.hasSensor(context, Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)) {
                sources.add(CompassSource.GeomagneticRotationVector)
            }
            if (Sensors.hasSensor(context, Sensor.TYPE_MAGNETIC_FIELD)) {
                sources.add(CompassSource.CustomMagnetometer)
            }
            @Suppress("DEPRECATION")
            if (Sensors.hasSensor(context, Sensor.TYPE_ORIENTATION)) {
                sources.add(CompassSource.Orientation)
            }
            return sources
        }
    }
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\compass\CompassSource.kt =====

package com.mpdc4gsr.module.thermalunified.view.compass

enum class CompassSource(val id: String) {
    RotationVector("rotation_vector"),
    GeomagneticRotationVector("geomagnetic_rotation_vector"),
    CustomMagnetometer("custom_magnetometer"),
    Orientation("orientation"),
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\compass\LinearCompassView.kt =====

package com.mpdc4gsr.module.thermalunified.view.compass

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.view.drawToBitmap
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.compat.spToPx
import com.mpdc4gsr.module.thermalunified.utils.getPixelLinear
import com.mpdc4gsr.module.thermalunified.utils.getValuesBetween
import com.mpdc4gsr.module.thermalunified.utils.realX
import com.mpdc4gsr.module.thermalunified.utils.realY
import kotlinx.coroutines.*

class LinearCompassView : View {
    private val paint = Paint()
    private val textPaint = Paint()
    private val markerPaint = Paint()
    private val shortLinePaint = Paint()
    private val longLinePaint = Paint()
    private val positionPaint = Paint()
    private lateinit var canvas: Canvas
    private var lineColor: Int = Color.WHITE
    private var textColor: Int = Color.WHITE
    private var shortLineColor: Int = Color.WHITE
    private var longLineColor: Int = Color.WHITE
    private var positionColor: Int = Color.WHITE
    private var centerAzimuthColor = Color.WHITE
    private lateinit var context: Context
    private var textSize: Float = 0f
    private var shortLineSize = 0f
    private var longLineSize = 0f
    private var positionSize = 0f
    private var markerSize = 0f
    private var backgroundColor = Color.BLACK
    private var lastDrawTime = 0L
    private var step = 1000 / 10
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    var curBitmap: Bitmap? = null

    constructor(context: Context) : this(context, null) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0) {
        initView()
    }

    constructor(ctx: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        ctx,
        attrs,
        defStyleAttr,
    ) {
        this.context = ctx
        textSize = 13f.spToPx(context).toFloat()
        shortLineSize = 0.5f.spToPx(context).toFloat()
        longLineSize = 0.5f.spToPx(context).toFloat()
        positionSize = 11f.spToPx(context).toFloat()
        markerSize = 2f.spToPx(context).toFloat()
        val attributes =
            context.obtainStyledAttributes(attrs, R.styleable.LinearCompassView, 0, 0)
        lineColor = attributes.getColor(R.styleable.LinearCompassView_lineColor, Color.WHITE)
        textColor = attributes.getColor(R.styleable.LinearCompassView_textColor, Color.WHITE)
        backgroundColor =
            attributes.getColor(R.styleable.LinearCompassView_backgroundColor, Color.BLACK)
        shortLineColor =
            attributes.getColor(R.styleable.LinearCompassView_shortLineColor, Color.WHITE)
        longLineColor =
            attributes.getColor(R.styleable.LinearCompassView_longLineColor, Color.WHITE)
        positionColor =
            attributes.getColor(R.styleable.LinearCompassView_positionColor, Color.WHITE)
        centerAzimuthColor =
            attributes.getColor(R.styleable.LinearCompassView_compassMarkerColor, Color.WHITE)
        shortLineSize =
            attributes.getDimension(
                R.styleable.LinearCompassView_shortLineSize,
                shortLineSize,
            )
        longLineSize =
            attributes.getDimension(
                R.styleable.LinearCompassView_longLineSize,
                longLineSize,
            )
        positionSize =
            attributes.getDimension(
                R.styleable.LinearCompassView_positionSize,
                positionSize,
            )
        markerSize =
            attributes.getDimension(
                R.styleable.LinearCompassView_markerSize,
                markerSize,
            )
        attributes.recycle()
        initView()
    }

    private fun initView() {
        paint.color = backgroundColor
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.strokeWidth = 1f
        paint.isAntiAlias = true
        textPaint.color = textColor
        textPaint.textSize = textSize
        textPaint.style = Paint.Style.FILL_AND_STROKE
        textPaint.isAntiAlias = true
        textPaint.strokeWidth = 1f
        markerPaint.color = centerAzimuthColor
        markerPaint.strokeWidth = markerSize
        markerPaint.style = Paint.Style.FILL_AND_STROKE
        markerPaint.isAntiAlias = true
        shortLinePaint.color = shortLineColor
        shortLinePaint.strokeWidth = shortLineSize
        shortLinePaint.style = Paint.Style.STROKE
        shortLinePaint.isAntiAlias = true
        longLinePaint.color = longLineColor
        longLinePaint.strokeWidth = longLineSize
        longLinePaint.style = Paint.Style.STROKE
        longLinePaint.isAntiAlias = true
        positionPaint.color = positionColor
        positionPaint.textSize = positionSize
        positionPaint.style = Paint.Style.FILL_AND_STROKE
        positionPaint.isAntiAlias = true
        positionPaint.strokeWidth = 1f
    }

    private var showAzimuthArrow = true
    private var azimuth = 0f
    private var range = 180f
    private var text: String = ""
    private fun getRawMinimum() = azimuth - range / 2
    private fun getRawMaximum() = azimuth + range / 2
    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        this.canvas = canvas
        drawAzimuthArrow()
        drawCompassLine()
    }

    private fun drawBackGround() {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    private fun drawAzimuthArrow() {
        if (!showAzimuthArrow) {
            return
        }
        val endWidth = width / 2f
        val endHeight = (3 / 10f) * height
        canvas.drawText(
            text,
            realX(text, endWidth, textPaint),
            realY(text, endHeight, textPaint),
            textPaint
        )
    }

    private fun drawCompassLine() {
        drawCompass()
        val bottomHeight = height * 7 / 10f
        canvas.drawLine(0f, (bottomHeight - 1), width.toFloat(), bottomHeight, shortLinePaint)
        canvas.drawLine(
            width / 2f + markerSize / 2,
            height * (3 / 10f),
            width / 2f + markerSize / 2,
            height * (7 / 10f),
            markerPaint,
        )
    }

    fun setCurAzimuth(azimuth: Int) {
        scope.launch(Dispatchers.IO) {
            this@LinearCompassView.azimuth = azimuth.toFloat()
            this@LinearCompassView.text = azimuth.toString()
            var curTime = System.currentTimeMillis()
            if (curTime - lastDrawTime > step) {
                lastDrawTime = curTime
                launch(Dispatchers.Main) {
                    curBitmap = this@LinearCompassView.drawToBitmap()
                    invalidate()
                }
            }
        }
    }

    private fun drawCompass() {
        getValuesBetween(getRawMinimum(), getRawMaximum(), 5f).map {
            it.toInt()
        }.toMutableList().forEach {
            val x = toPixel(it.toFloat())
            val lineHeight =
                when {
                    it % 90 == 0 -> (3 / 10f) * height
                    it % 15 == 0 -> (4 / 10f) * height
                    else -> (5 / 10f) * height
                }
            val bottomHeight = height * 7 / 10f
            when {
                it % 90 == 0 -> canvas.drawLine(x, lineHeight, x, bottomHeight, longLinePaint)
                else -> canvas.drawLine(x, lineHeight, x, bottomHeight, shortLinePaint)
            }
            if (it % 45 == 0) {
                val coord = getPositionText(it)
                canvas.drawText(
                    coord,
                    realX(coord, x, positionPaint),
                    realY(coord, height - 2f, positionPaint),
                    positionPaint
                )
            }
        }
    }

    private fun getPositionText(position: Int): String =
        when (position) {
            -90, 270 -> resources.getString(R.string.compass_west)
            -45, 315 -> resources.getString(R.string.compass_northwest)
            0, 360 -> resources.getString(R.string.compass_north)
            45, 405 -> resources.getString(R.string.compass_northeast)
            90, 450 -> resources.getString(R.string.compass_east)
            135, 495 -> resources.getString(R.string.compass_southeast)
            -180, 180 -> resources.getString(R.string.compass_south)
            -135, 225 -> resources.getString(R.string.compass_southwest)
            else -> ""
        }

    private fun toPixel(bearing: Float): Float {
        return getPixelLinear(
            bearing,
            azimuth,
            width.toFloat(),
            range,
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Recreate the scope if it was cancelled
        if (!scope.isActive) {
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.cancel()
    }
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\compass\MagQualityCompassWrapper.kt =====

package com.mpdc4gsr.module.thermalunified.view.compass

import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.ISensor
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.compass.ICompass
import com.kylecorry.sol.units.Bearing
import kotlin.math.min

class MagQualityCompassWrapper(private val compass: ICompass, private val magnetometer: ISensor) :
    AbstractSensor(), ICompass {
    override val bearing: Bearing
        get() = compass.bearing
    override var declination: Float
        get() = compass.declination
        set(value) {
            compass.declination = value
        }
    override val hasValidReading: Boolean
        get() = compass.hasValidReading
    override val rawBearing: Float
        get() = compass.rawBearing
    override val quality: Quality
        get() = Quality.values()[min(magnetometer.quality.ordinal, compass.quality.ordinal)]

    override fun startImpl() {
        compass.start(this::onReading)
        magnetometer.start(this::onReading)
    }

    override fun stopImpl() {
        compass.stop(this::onReading)
        magnetometer.stop(this::onReading)
    }

    private fun onReading(): Boolean {
        notifyListeners()
        return true
    }
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\compass\NullCompass.kt =====

package com.mpdc4gsr.module.thermalunified.view.compass

import com.kylecorry.andromeda.sense.compass.ICompass
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.CompassDirection

class NullCompass : NullSensor(), ICompass {
    override val bearing: Bearing = Bearing.from(CompassDirection.North)
    override var declination: Float = 0f
    override val rawBearing: Float = 0f
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\compass\NullSensor.kt =====

package com.mpdc4gsr.module.thermalunified.view.compass

import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.time.CoroutineTimer

abstract class NullSensor(private val interval: Long = 0) : AbstractSensor() {
    override val hasValidReading: Boolean = true
    private val timer =
        CoroutineTimer {
            notifyListeners()
        }

    override fun startImpl() {
        if (interval == 0L) {
            timer.once(0L)
        } else {
            timer.interval(interval)
        }
    }

    override fun stopImpl() {
        timer.stop()
    }
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\compass\SensorService.kt =====

package com.mpdc4gsr.module.thermalunified.view.compass

import android.content.Context
import android.hardware.SensorManager
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.andromeda.sense.compass.ICompass

class SensorService(ctx: Context) {
    private var context = ctx.applicationContext
    fun hasCompass(): Boolean {
        return Sensors.hasCompass(context)
    }

    fun getCompass(): ICompass {
        return CompassProvider(context).get()
    }

    companion object {
        const val MOTION_SENSOR_DELAY = SensorManager.SENSOR_DELAY_GAME
        const val ENVIRONMENT_SENSOR_DELAY = SensorManager.SENSOR_DELAY_NORMAL
    }
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\DetectHorizontalScrollView.java =====

package com.mpdc4gsr.module.thermalunified.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.HorizontalScrollView;

public class DetectHorizontalScrollView extends HorizontalScrollView {
    private Runnable scrollerTask;
    private int intitPosition;
    private int newCheck = 100;
    private int childWidth = 0;
    private OnScrollStopListner onScrollstopListner;

    public DetectHorizontalScrollView (Context context, AttributeSet attrs) {
        super(context, attrs);
        scrollerTask = new Runnable () {
            @Override
            public void run() {
                int newPosition = getScrollX ();
                if (intitPosition - newPosition == 0) {
                    if (onScrollstopListner == null) {
                        return;
                    }
                    onScrollstopListner.onScrollStoped();
                    Rect outRect = new Rect();
                    getDrawingRect(outRect);
                    if (getScrollX() == 0) {
                        onScrollstopListner.onScrollToLeftEdge();
                    } else if (childWidth + getPaddingLeft() + getPaddingRight() == outRect.right) {
                        onScrollstopListner.onScrollToRightEdge();
                    } else {
                        onScrollstopListner.onScrollToMiddle();
                    }
                } else {
                    intitPosition = getScrollX();
                    postDelayed(scrollerTask, newCheck);
                }
            }
        };
    }

    public void setOnScrollStopListner(OnScrollStopListner listner) {
        onScrollstopListner = listner;
    }

    public void startScrollerTask() {
        intitPosition = getScrollX();
        postDelayed(scrollerTask, newCheck);
        checkTotalWidth();
    }

    private void checkTotalWidth() {
        if (childWidth > 0) {
            return;
        }
        for (int i = 0; i < getChildCount(); i++) {
        childWidth += getChildAt(i).getWidth();
    }
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (onScrollstopListner != null) {
            onScrollstopListner.onScrollChanged(l, t, oldl, oldt);
        }
    }

    public interface OnScrollStopListner {

        void onScrollStoped();

        void onScrollToLeftEdge();

        void onScrollToRightEdge();

        void onScrollToMiddle();

        void onScrollChanged(int l, int t, int oldl, int oldt);
    }
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\DistanceMeasureView.kt =====

package com.mpdc4gsr.module.thermalunified.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class DistanceMeasureView : View {
    private var margin: Float = 0f
    private var linePaint: Paint? = null
    private var line1Y = 0f
    private var line2Y = 0f
    var distance = 0f
        private set
    var moveListener: ((distance: Float) -> Unit)? = null

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    ) {
        init()
    }

    private fun init() {
        linePaint = Paint()
        linePaint!!.color = Color.GREEN
        linePaint!!.strokeWidth = 4f
        linePaint!!.style = Paint.Style.STROKE
        val intervals = floatArrayOf(10f, 10f)
        linePaint!!.pathEffect = DashPathEffect(intervals, 0f)
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val screenHeight = measuredHeight
        val lineHeight = 50
        margin = ((screenHeight - lineHeight) / 2).toFloat()
        line1Y = margin
        line2Y = margin + lineHeight
        distance = lineHeight.toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawLine(50f, line1Y, (width - 50).toFloat(), line1Y, linePaint!!)
        canvas.drawLine(50f, line2Y, (width - 50).toFloat(), line2Y, linePaint!!)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                var newY = event.y
                if (newY < 0) {
                    newY = 0f
                } else if (newY > height) {
                    newY = height.toFloat()
                }
                if (Math.abs(newY - line1Y) < Math.abs(newY - line2Y)) {
                    val abs = line1Y - newY
                    line1Y = newY
                    line2Y += abs
                } else {
                    val abs = newY - line2Y
                    line2Y = newY
                    line1Y -= abs
                }
                distance = Math.abs(line2Y - line1Y)
                invalidate()
                moveListener?.invoke(distance)
            }
        }
        return true
    }
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\EmissivityView.kt =====

package com.mpdc4gsr.module.thermalunified.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import com.mpdc4gsr.module.thermalunified.compat.dpToPx
import com.mpdc4gsr.module.thermalunified.compat.spToPx

class EmissivityView : View {
    companion object {
        private const val DEFAULT_STROKE_WIDTH: Float = 0.5f
    }

    var isAlignTop = false
    var drawTopLine = false
    private val textList: ArrayList<CharSequence> = ArrayList(3)
    private val layoutList: ArrayList<StaticLayout> = ArrayList(3)
    private var strokeWidth: Float = 0f
    private val linePaint = Paint()
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes,
    ) {
        strokeWidth = DEFAULT_STROKE_WIDTH.dpToPx(context).coerceAtLeast(1f)
        linePaint.color = 0xff5b5961.toInt()
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeWidth = strokeWidth
    }

    fun refreshText(newList: List<String>) {
        textList.clear()
        textList.addAll(newList)
        textPaint.color = if (textList.size == 1) 0xffffffff.toInt() else 0xccffffff.toInt()
        textPaint.textSize = (if (textList.size == 1) 12f else 11f).spToPx(context)
        requestLayout()
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val widthSize: Int = MeasureSpec.getSize(widthMeasureSpec) - paddingStart - paddingEnd
        val firstWidth: Int = (widthSize * 135 / 335f).toInt()
        val elseWidth: Int = (widthSize - firstWidth) / 2
        val contentWidth: Int = firstWidth + elseWidth * 2
        layoutList.clear()
        for (i in textList.indices) {
            val textWidth: Int =
                if (textList.size == 1) {
                    contentWidth - 24.dpToPx(context)
                } else {
                    (if (i == 0) firstWidth else elseWidth) - 24.dpToPx(context)
                }
            layoutList.add(
                StaticLayout.Builder.obtain(
                    textList[i],
                    0,
                    textList[i].length,
                    textPaint,
                    textWidth
                )
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .build(),
            )
        }
        var maxHeight = 0
        for (layout in layoutList) {
            maxHeight = maxHeight.coerceAtLeast(layout.height)
        }
        if (maxHeight == 0) {
            maxHeight = textPaint.fontMetricsInt.bottom - textPaint.fontMetricsInt.top
        }
        maxHeight += 12.dpToPx(context)
        setMeasuredDimension(contentWidth + paddingStart + paddingEnd, maxHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.translate(paddingStart.toFloat(), 0f)
        val contentWidth = (width - paddingStart - paddingEnd).toFloat()
        if (drawTopLine) {
            canvas.drawLine(0f, strokeWidth / 2, contentWidth, strokeWidth / 2, linePaint)
        }
        canvas.drawLine(
            0f,
            height.toFloat() - strokeWidth / 2,
            contentWidth,
            height.toFloat() - strokeWidth / 2,
            linePaint
        )
        canvas.drawLine(strokeWidth / 2, 0f, strokeWidth / 2, height.toFloat(), linePaint)
        val padding = 12f.dpToPx(context)
        for (layout in layoutList) {
            canvas.save()
            canvas.translate(
                padding,
                if (isAlignTop) 6f.dpToPx(context) else (height - layout.height) / 2f
            )
            layout.draw(canvas)
            canvas.restore()
            val itemWidth = padding + layout.width.toFloat() + padding
            canvas.drawLine(
                itemWidth - strokeWidth / 2,
                0f,
                itemWidth - strokeWidth / 2,
                height.toFloat(),
                linePaint
            )
            canvas.translate(itemWidth, 0f)
        }
    }
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\HikSurfaceView.kt =====

package com.mpdc4gsr.module.thermalunified.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.view.SurfaceView
import com.energy.iruvc.sdkisp.LibIRProcess
import com.energy.iruvc.sdkisp.LibIRProcess.ImageRes_t
import com.energy.iruvc.utils.CommonParams.IRPROCSRCFMTType
import com.energy.iruvc.utils.CommonParams.PseudoColorType
import com.mpdc4gsr.libunified.app.bean.AlarmBean
import com.mpdc4gsr.libunified.ir.utils.IRImageHelp
import com.mpdc4gsr.libunified.ir.utils.OpencvTools
import com.mpdc4gsr.libunified.ir.utils.PseudocodeUtils
import com.mpdc4gsr.module.thermalunified.bean.DataBean
import java.nio.ByteBuffer

class HikSurfaceView : SurfaceView {
    companion object {
        private const val MULTIPLE = 2
    }

    var isOpenAmplify: Boolean = false
        set(value) {
            field = value
            val isPortrait = rotateAngle == 90 || rotateAngle == 270
            val width = (if (isPortrait) 192 else 256) * (if (value) MULTIPLE else 1)
            val height = (if (isPortrait) 256 else 192) * (if (value) MULTIPLE else 1)
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }

    @Volatile
    var rotateAngle: Int = 270
        set(value) {
            field = value
            val isPortrait = value == 90 || value == 270
            val width = (if (isPortrait) 192 else 256) * (if (isOpenAmplify) MULTIPLE else 1)
            val height = (if (isPortrait) 256 else 192) * (if (isOpenAmplify) MULTIPLE else 1)
            bitmap.reconfigure(width, height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        }
    var alarmBean = AlarmBean()
    var limitTempMin = Float.MIN_VALUE
    var limitTempMax = Float.MAX_VALUE
    private val irImageHelp = IRImageHelp()
    fun refreshCustomPseudo(it: DataBean) {
    }

    @Volatile
    private var pseudoType: PseudoColorType = PseudoColorType.PSEUDO_3
    fun setPseudoCode(code: Int) {
        pseudoType = PseudocodeUtils.changePseudocodeModeByOld(code)
    }

    private val imageRes = ImageRes_t()
    private var bitmap: Bitmap = Bitmap.createBitmap(192, 256, Bitmap.Config.ARGB_8888)
    private val sourceArgbArray = ByteArray(256 * 192 * 4)
    private val rotateArgbArray = ByteArray(256 * 192 * 4)
    private val amplifyArray = ByteArray(256 * MULTIPLE * 192 * MULTIPLE * 4)
    private val tempArray = ByteArray(256 * 192 * 2)

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes,
    ) {
        imageRes.width = 256.toChar()
        imageRes.height = 192.toChar()
    }

    fun getScaleBitmap(): Bitmap =
        synchronized(this) {
            Bitmap.createScaledBitmap(bitmap, width, height, true)
        }

    fun refresh(
        yuvArray: ByteArray,
        newTempArray: ByteArray,
    ) {
        val sourceWidth = 256
        val sourceHeight = 192
        System.arraycopy(newTempArray, 0, tempArray, 0, tempArray.size)
        val pseudo: PseudoColorType =
            if (irImageHelp.getColorList() == null) pseudoType else PseudoColorType.PSEUDO_1
        LibIRProcess.convertYuyvMapToARGBPseudocolor(
            yuvArray,
            (sourceWidth * sourceHeight).toLong(),
            pseudo,
            sourceArgbArray
        )
        irImageHelp.customPseudoColor(sourceArgbArray, tempArray, sourceWidth, sourceHeight)
        irImageHelp.setPseudoColorMaxMin(
            sourceArgbArray,
            tempArray,
            limitTempMax,
            limitTempMin,
            sourceWidth,
            sourceHeight
        )
        val newArray = irImageHelp.contourDetection(
            alarmBean,
            sourceArgbArray,
            tempArray,
            sourceWidth,
            sourceHeight
        ) ?: sourceArgbArray
        when (rotateAngle) {
            90 -> LibIRProcess.rotateLeft90(
                newArray,
                imageRes,
                IRPROCSRCFMTType.IRPROC_SRC_FMT_ARGB8888,
                rotateArgbArray
            )

            180 -> LibIRProcess.rotate180(
                newArray,
                imageRes,
                IRPROCSRCFMTType.IRPROC_SRC_FMT_ARGB8888,
                rotateArgbArray
            )

            270 -> LibIRProcess.rotateRight90(
                newArray,
                imageRes,
                IRPROCSRCFMTType.IRPROC_SRC_FMT_ARGB8888,
                rotateArgbArray
            )

            else -> System.arraycopy(newArray, 0, rotateArgbArray, 0, rotateArgbArray.size)
        }
        if (isOpenAmplify) {
            val width: Int =
                if (rotateAngle == 90 || rotateAngle == 270) sourceWidth else sourceHeight
            val height: Int =
                if (rotateAngle == 90 || rotateAngle == 270) sourceHeight else sourceWidth
            OpencvTools.supImage(rotateArgbArray, width, height, amplifyArray)
        }
        synchronized(this) {
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(if (isOpenAmplify) amplifyArray else rotateArgbArray))
        }
        val canvas: Canvas = holder.lockCanvas() ?: return
        canvas.drawBitmap(bitmap, null, Rect(0, 0, width, height), null)
        holder.unlockCanvasAndPost(canvas)
    }
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\MoveImageView.java =====

package com.mpdc4gsr.module.thermalunified.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

import androidx.annotation.Nullable;

@SuppressLint("AppCompatCustomView")
public class MoveImageView extends ImageView {

    private static final String TAG = "MoveImageView";
    private static final int MIN_CLICK_DELAY_TIME = 100;
    private static long lastClickTime;
    public OnMoveListener onMoveListener;
    private float mPreX;
    private float mPreY;

    public MoveImageView (Context context) {
        this(context, null);
    }

    public MoveImageView (Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public MoveImageView (Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public static boolean delayMoveTime () {
        boolean flag = false;
        long curClickTime = System . currentTimeMillis ();
        if ((curClickTime - lastClickTime) < MIN_CLICK_DELAY_TIME) {
            flag = false;
        } else {
            flag = true;
            lastClickTime = System.currentTimeMillis();
        }
        Log.d(TAG, "ACTION_MOVE isFastClick flag : " + flag);
        return flag;
    }

    private void init() {
        setBackgroundColor(Color.TRANSPARENT);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event . getAction ();

        switch(action) {
            case MotionEvent . ACTION_DOWN :
            Log.d(TAG, "ACTION_DOWN");
            mPreX = event.getX();
            mPreY = event.getY();
            lastClickTime = System.currentTimeMillis();
            break;

            case MotionEvent . ACTION_MOVE :
            Log.d(TAG, "ACTION_MOVE");
            float preX = mPreX;
            float preY = mPreY;
            float curX = event . getX ();
            float curY = event . getY ();

            if (onMoveListener != null && delayMoveTime()) {

                Log.d(TAG, "ACTION_MOVE isFastClick");
                onMoveListener.onMove(preX, preY, curX, curY);
                mPreX = curX;
                mPreY = curY;
            }
            break;
            case MotionEvent . ACTION_UP :
            Log.d(TAG, "ACTION_UP");
            break;
            case MotionEvent . ACTION_CANCEL :
            Log.d(TAG, "ACTION_CANCEL");
            break;

        }
        return true;
    }

    public void setOnMoveListener(OnMoveListener onMoveListener) {
        this.onMoveListener = onMoveListener;
    }

    public interface OnMoveListener {
        void onMove(float preX, float preY, float curX, float curY);
    }
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\MyGSYVideoPlayer.kt =====

package com.mpdc4gsr.module.thermalunified.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import com.mpdc4gsr.module.thermalunified.R

class MyGSYVideoPlayer : FrameLayout {
    companion object {
        const val CURRENT_STATE_PLAYING = 2
        const val CURRENT_STATE_PAUSE = 5
        const val CURRENT_STATE_IDLE = 0
    }

    private var mCurrentState = CURRENT_STATE_IDLE
    private var mStartButton: ImageView? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    private fun init(context: Context) {
    }

    fun getLayoutId(): Int = R.layout.view_my_gsy_video_player
    fun updateStartImage() {
        if (mStartButton is ImageView) {
            val imageView = mStartButton as ImageView
            if (mCurrentState == CURRENT_STATE_PLAYING) {
                imageView.setImageResource(R.drawable.svg_pause_icon)
            } else {
                imageView.setImageResource(R.drawable.svg_play_icon)
            }
        }
    }

    fun play() {
        mCurrentState = CURRENT_STATE_PLAYING
        updateStartImage()
    }

    fun pause() {
        mCurrentState = CURRENT_STATE_PAUSE
        updateStartImage()
    }

    fun stop() {
        mCurrentState = CURRENT_STATE_IDLE
        updateStartImage()
    }
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\MyMarkerView.java =====

package com.mpdc4gsr.module.thermalunified.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.TextView;

import com.mpdc4gsr.libunified.app.db.entity.ThermalEntity;
import com.mpdc4gsr.libunified.app.tools.NumberTools;
import com.mpdc4gsr.libunified.app.tools.TimeTools;
import com.mpdc4gsr.libunified.ui.components.MarkerView;
import com.mpdc4gsr.libunified.ui.data.CandleEntry;
import com.mpdc4gsr.libunified.ui.data.Entry;
import com.mpdc4gsr.libunified.ui.highlight.Highlight;
import com.mpdc4gsr.libunified.ui.utils.MPPointF;
import com.mpdc4gsr.libunified.ui.utils.Utils;
import com.mpdc4gsr.module.thermalunified.R;

@SuppressLint("ViewConstructor")
public class MyMarkerView extends MarkerView {

    private final TextView tvContent;
    private final TextView timeText;

    public MyMarkerView (Context context, int layoutResource) {
        super(context, layoutResource);
        tvContent = findViewById(R.id.tvContent);
        timeText = findViewById(R.id.time_text);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        int index = highlight . getDataIndex ();
        ThermalEntity data =(ThermalEntity) e . getData ();
        if (e instanceof CandleEntry) {
            CandleEntry ce =(CandleEntry) e;
            tvContent.setText(Utils.formatNumber(ce.getHigh(), 0, true));
        } else {
            StringBuilder str = new StringBuilder();
            String thermalStr = NumberTools . INSTANCE . to02 (data.getThermal());
            String thermalMaxStr = NumberTools . INSTANCE . to02 (data.getThermalMax());
            String thermalMinStr = NumberTools . INSTANCE . to02 (data.getThermalMin());
            if (index == 0) {
                str.append("[CHINESE_TEXT]:").append(thermalStr);
            } else if (index == 1) {
                str.append("[CHINESE_TEXT]:").append(thermalMaxStr);
                str.append(System.getProperty("line.separator")).append("[CHINESE_TEXT]:").append(thermalMinStr);
            } else {
                str.append("[CHINESE_TEXT]:").append(thermalMaxStr);
                str.append(System.getProperty("line.separator")).append("[CHINESE_TEXT]:").append(thermalMinStr);
            }
            tvContent.setText(str.toString());
            timeText.setText(TimeTools.INSTANCE.showTimeSecond(data.getCreateTime()));
        }
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF (-(getWidth() / 2f), -getHeight());
    }
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\TargetBarPickView.kt =====

package com.mpdc4gsr.module.thermalunified.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import com.mpdc4gsr.libunified.app.utils.ScreenUtils
import com.mpdc4gsr.module.thermalunified.compat.dpToPx
import com.mpdc4gsr.module.thermalunified.compat.spToPx

class TargetBarPickView : View {
    companion object {
        @ColorInt
        private const val DEFAULT_BG_COLOR = 0x7F000000.toInt()

        @ColorInt
        private const val DEFAULT_PROGRESS_COLOR = 0xffffffff.toInt()
        private const val THUMB_CORNERS = 4f
        private const val THUMB_STROKE_WIDTH = 1.5f
    }

    var onStartTrackingTouch: ((progress: Int, max: Int) -> Unit)? = null
    var onProgressChanged: ((progress: Int, max: Int) -> Unit)? = null
    var onStopTrackingTouch: ((progress: Int, max: Int) -> Unit)? = null
    var valueFormatListener: ((progress: Int) -> String) = {
        it.toString()
    }
    var max: Int = 100
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }
    var min: Int = 0
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }
    private var progress: Int = 0
        set(value) {
            if (field != value) {
                field = value.coerceAtLeast(min).coerceAtMost(max)
                invalidate()
            }
        }

    fun setProgressAndRefresh(progress: Int) {
        this.progress = progress
        onProgressChanged?.invoke(this.progress, max)
    }

    private val barSize: Int
    private val rotate: Int
    private val labelText: String
    private val path = Path()
    private val paint = TextPaint()
    private val thumbRect = RectF()
    private val barRect = RectF()

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes,
    ) {
        val typedArray =
            context.obtainStyledAttributes(
                attrs,
                com.mpdc4gsr.libunified.R.styleable.BarPickView,
                0,
                0
            )
        max = typedArray.getInt(com.mpdc4gsr.libunified.R.styleable.BarPickView_android_max, 100)
        min = typedArray.getInt(com.mpdc4gsr.libunified.R.styleable.BarPickView_barMin, 0)
        progress =
            typedArray.getInt(com.mpdc4gsr.libunified.R.styleable.BarPickView_android_progress, min)
                .coerceAtMost(max).coerceAtLeast(min)
        barSize = typedArray.getInt(
            com.mpdc4gsr.libunified.R.styleable.BarPickView_barSize,
            4.dpToPx(context)
        )
        rotate =
            typedArray.getInt(com.mpdc4gsr.libunified.R.styleable.BarPickView_barOrientation, 0)
        labelText =
            typedArray.getString(com.mpdc4gsr.libunified.R.styleable.BarPickView_barLabel) ?: ""
        val textSize = typedArray.getDimensionPixelSize(
            com.mpdc4gsr.libunified.R.styleable.BarPickView_android_textSize,
            13.spToPx(context)
        )
        typedArray.recycle()
        paint.isAntiAlias = true
        paint.textSize = textSize.toFloat()
        paint.strokeWidth = THUMB_STROKE_WIDTH.dpToPx(context).toFloat()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return false
        }
        parent.requestDisallowInterceptTouchEvent(true)
        val x: Float = event.x - barRect.left
        val y: Float = event.y - barRect.top
        val barWidth: Float = barRect.width()
        val barHeight: Float = barRect.height()
        progress =
            when (rotate) {
                0 -> (x / barWidth * (max - min) + min).toInt()
                180 -> ((barWidth - x) / barWidth * (max - min) + min).toInt()
                90 -> (y / barHeight * (max - min) + min).toInt()
                else -> ((barHeight - y) / barHeight * (max - min) + min).toInt()
            }.coerceAtLeast(min).coerceAtMost(max)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> onStartTrackingTouch?.invoke(progress, max)
            MotionEvent.ACTION_MOVE -> onProgressChanged?.invoke(progress, max)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                onStopTrackingTouch?.invoke(progress, max)
            }
        }
        return true
    }

    private fun computeThumbWidth(): Int {
        val minTextWidth = paint.measureText(valueFormatListener.invoke(min)).toInt()
        val maxTextWidth = paint.measureText(valueFormatListener.invoke(max)).toInt()
        return minTextWidth.coerceAtLeast(maxTextWidth) + 12.dpToPx(context)
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val thumbWidth = computeThumbWidth()
        val thumbHeight =
            paint.fontMetricsInt.bottom - paint.fontMetricsInt.top + 4.dpToPx(context)
        val width: Int =
            if (rotate == 0 || rotate == 180) {
                if (widthMode == MeasureSpec.UNSPECIFIED) ScreenUtils.getScreenWidth(context) else widthSize
            } else {
                val wantWidth: Int = thumbWidth + paddingStart + paddingEnd
                when (widthMode) {
                    MeasureSpec.EXACTLY -> widthSize
                    MeasureSpec.AT_MOST -> wantWidth.coerceAtMost(widthSize)
                    MeasureSpec.UNSPECIFIED -> wantWidth
                    else -> wantWidth
                }
            }
        val height: Int =
            if (rotate == 0 || rotate == 180) {
                val wantHeight: Int = thumbHeight + paddingTop + paddingBottom
                when (heightMode) {
                    MeasureSpec.EXACTLY -> heightSize
                    MeasureSpec.AT_MOST -> wantHeight.coerceAtMost(heightSize)
                    MeasureSpec.UNSPECIFIED -> wantHeight
                    else -> wantHeight
                }
            } else {
                if (heightMode == MeasureSpec.UNSPECIFIED) ScreenUtils.getScreenHeight(context) else heightSize
            }
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        computeBarRect()
        computeThumbRect()
        clipToBarRect(canvas)
        drawBgBar(canvas)
        drawProgress(canvas)
        canvas.restore()
        drawThumb(canvas)
    }

    private fun computeBarRect() {
        val textHeight = paint.fontMetricsInt.bottom - paint.fontMetricsInt.top
        val textMargin = 4.dpToPx(context)
        val thumbWidth = computeThumbWidth()
        val thumbHeight = textHeight + 4.dpToPx(context)
        if (rotate == 0 || rotate == 180) {
            val labelTextSpace = if (labelText.isEmpty()) 0 else (paint.measureText(labelText)
                .toInt() + 6.dpToPx(context))
            val leftText = valueFormatListener.invoke(if (rotate == 0) min else max)
            val rightText = valueFormatListener.invoke(if (rotate == 0) max else min)
            val leftTextWidth = paint.measureText(leftText).toInt()
            val rightTextWidth = paint.measureText(rightText).toInt()
            val left =
                paddingStart.toFloat() + leftTextWidth + textMargin + if (rotate == 0) labelTextSpace else 0
            val top = (paddingTop + thumbHeight / 2 - barSize / 2).toFloat()
            val right =
                (measuredWidth - paddingEnd - rightTextWidth - textMargin - if (rotate == 0) 0 else labelTextSpace).toFloat()
            val bottom = top + barSize
            barRect.set(left, top, right, bottom)
        } else {
            val labelTextSpace = if (labelText.isEmpty()) 0 else (textHeight + 6.dpToPx(context))
            val left = (paddingStart + thumbWidth / 2 - barSize / 2).toFloat()
            val top =
                paddingTop.toFloat() + textHeight + textMargin + if (rotate == 90) labelTextSpace else 0
            val right = left + barSize
            val bottom =
                (measuredHeight - paddingBottom - textHeight - textMargin - if (rotate == 90) 0 else labelTextSpace).toFloat()
            barRect.set(left, top, right, bottom)
        }
    }

    private fun computeThumbRect() {
        val thumbWidth = computeThumbWidth()
        val thumbHeight =
            paint.fontMetricsInt.bottom - paint.fontMetricsInt.top + 4.dpToPx(context)
        if (rotate == 0 || rotate == 180) {
            val progressWidth = (barRect.width() * (progress - min) / (max - min)).toInt()
            val left =
                (if (rotate == 0) (barRect.left + progressWidth - thumbWidth / 2) else (barRect.right - progressWidth - thumbWidth / 2))
                    .toInt()
                    .coerceAtLeast(barRect.left.toInt())
                    .coerceAtMost(barRect.right.toInt() - thumbWidth)
            val right = left + thumbWidth
            val top = paddingTop
            val bottom = measuredHeight - paddingBottom
            thumbRect.set(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
        } else {
            val progressHeight =
                (barRect.height() * (progress - min) / (max - min).toFloat()).toInt()
            val left = paddingStart
            val right = measuredWidth - paddingEnd
            val top =
                (if (rotate == 90) (barRect.top + progressHeight - thumbHeight / 2) else (barRect.bottom - progressHeight - thumbHeight / 2))
                    .toInt()
                    .coerceAtLeast(barRect.top.toInt())
                    .coerceAtMost(barRect.bottom.toInt() - thumbHeight)
            val bottom = top + thumbHeight
            thumbRect.set(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
        }
    }

    private fun clipToBarRect(canvas: Canvas) {
        canvas.save()
        val radius = (barSize / 2).toFloat()
        if (rotate == 0 || rotate == 180) {
            path.rewind()
            path.moveTo(barRect.left + radius, barRect.top)
            path.lineTo(barRect.right - radius, barRect.top)
            path.quadTo(barRect.right, barRect.top, barRect.right, barRect.top + barSize / 2)
            path.quadTo(barRect.right, barRect.bottom, barRect.right - radius, barRect.bottom)
            path.lineTo(barRect.left + radius, barRect.bottom)
            path.quadTo(barRect.left, barRect.bottom, barRect.left, barRect.bottom - barSize / 2)
            path.quadTo(barRect.left, barRect.top, barRect.left + radius, barRect.top)
            canvas.clipPath(path)
        } else {
            path.rewind()
            path.moveTo(barRect.left, barRect.bottom - radius)
            path.lineTo(barRect.left, barRect.top + radius)
            path.quadTo(barRect.left, barRect.top, barRect.left + barSize / 2, barRect.top)
            path.quadTo(barRect.right, barRect.top, barRect.right, barRect.top + radius)
            path.lineTo(barRect.right, barRect.bottom - radius)
            path.quadTo(barRect.right, barRect.bottom, barRect.right - barSize / 2, barRect.bottom)
            path.quadTo(barRect.left, barRect.bottom, barRect.left, barRect.bottom - radius)
            canvas.clipPath(path)
        }
    }

    private fun drawBgBar(canvas: Canvas) {
        paint.color = DEFAULT_BG_COLOR
        val left = barRect.left
        val top = barRect.top
        val right = barRect.right
        val bottom = barRect.bottom
        if (rotate == 0 || rotate == 180) {
            val thumbWidth = computeThumbWidth()
            val bgWidth = (barRect.width() * (max - progress) / (max - min).toFloat()).toInt()
            if (bgWidth == 0) {
                return
            }
            if (rotate == 0) {
                canvas.drawRect(
                    (right - bgWidth + thumbWidth / 2).coerceAtLeast(left + thumbWidth),
                    top,
                    right,
                    bottom,
                    paint
                )
            } else {
                canvas.drawRect(
                    left,
                    top,
                    (left + bgWidth - thumbWidth / 2).coerceAtMost(right - thumbWidth),
                    bottom,
                    paint
                )
            }
        } else {
            val thumbHeight =
                paint.fontMetricsInt.bottom - paint.fontMetricsInt.top + 4f.dpToPx(context)
            val bgHeight = (barRect.height() * (max - progress) / (max - min).toFloat()).toInt()
            if (bgHeight == 0) {
                return
            }
            if (rotate == 90) {
                canvas.drawRect(
                    left,
                    (bottom - bgHeight + thumbHeight / 2).coerceAtLeast(top + thumbHeight),
                    right,
                    bottom,
                    paint
                )
            } else {
                canvas.drawRect(
                    left,
                    top,
                    right,
                    (top + bgHeight - thumbHeight / 2).coerceAtMost(bottom - thumbHeight),
                    paint
                )
            }
        }
    }

    private fun drawProgress(canvas: Canvas) {
        paint.color = DEFAULT_PROGRESS_COLOR
        val left = barRect.left
        val top = barRect.top
        val right = barRect.right
        val bottom = barRect.bottom
        if (rotate == 0 || rotate == 180) {
            val thumbWidth = computeThumbWidth()
            val progressWidth = (barRect.width() * (progress - min) / (max - min).toFloat()).toInt()
            if (progressWidth == 0) {
                return
            }
            if (rotate == 0) {
                canvas.drawRect(
                    left,
                    top,
                    (left + progressWidth - thumbWidth / 2).coerceAtMost(right - thumbWidth),
                    bottom,
                    paint
                )
            } else {
                canvas.drawRect(
                    (right - progressWidth + thumbWidth / 2).coerceAtLeast(left + thumbWidth),
                    top,
                    right,
                    bottom,
                    paint
                )
            }
        } else {
            val thumbHeight =
                paint.fontMetricsInt.bottom - paint.fontMetricsInt.top + 4f.dpToPx(context)
            val progressHeight =
                (barRect.height() * (progress - min) / (max - min).toFloat()).toInt()
            if (progressHeight == 0) {
                return
            }
            if (rotate == 90) {
                canvas.drawRect(
                    left,
                    top,
                    right,
                    (top + progressHeight - thumbHeight / 2).coerceAtMost(bottom - thumbHeight),
                    paint
                )
            } else {
                canvas.drawRect(
                    left,
                    (bottom - progressHeight + thumbHeight / 2).coerceAtLeast(top + thumbHeight),
                    right,
                    bottom,
                    paint
                )
            }
        }
    }

    private fun drawThumb(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        val radius = THUMB_CORNERS.dpToPx(context).toFloat()
        canvas.drawRoundRect(thumbRect, radius, radius, paint)
    }
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\Temperature07View.kt =====

package com.mpdc4gsr.module.thermalunified.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent

class Temperature07View : TemperatureBaseView {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes,
    )

    override fun onDraw(canvas: Canvas) {
        if (!isTouching) {
            return
        }
        when (mode) {
            Mode.POINT -> operatePoint?.let { drawPoint(canvas, it) }
            Mode.LINE -> operateLine?.let { drawLine(canvas, it) }
            Mode.RECT -> operateRect?.let { drawRect(canvas, it) }
            Mode.TREND -> {
            }

            else -> {
            }
        }
    }

    private var isTouching = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
            return false
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> isTouching = true
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> isTouching = false
        }
        return super.onTouchEvent(event)
    }
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\TemperatureBaseView.kt =====

package com.mpdc4gsr.module.thermalunified.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.ColorInt
import com.energy.iruvc.utils.Line
import com.mpdc4gsr.libunified.app.tools.UnitTools
import com.mpdc4gsr.libunified.ir.utils.TempDrawHelper
import com.mpdc4gsr.libunified.ir.utils.TempDrawHelper.Companion.correct
import com.mpdc4gsr.libunified.ir.utils.TempDrawHelper.Companion.correctPoint
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.compat.dpToPx
import kotlin.math.*

abstract class TemperatureBaseView : View {
    companion object {
        private const val DEFAULT_MAX_COUNT = 3
    }

    private val TOUCH_TOLERANCE by lazy { 8f.dpToPx(context).toInt() }
    private val DELETE_TOLERANCE by lazy {
        2f.dpToPx(context)
    }

    enum class Mode {
        POINT,
        LINE,
        RECT,
        TREND,
        FULL,
        CLEAR,
    }

    @Volatile
    var isShowFull: Boolean = true
        set(value) {
            field = value
            if (value && mode == Mode.CLEAR) {
                mode = Mode.FULL
            }
            invalidate()
        }

    @Volatile
    open var mode = Mode.FULL
        set(value) {
            field = value
            if (value == Mode.FULL) {
                isShowFull = true
                invalidate()
            } else if (value == Mode.CLEAR) {
                isShowFull = false
                synchronized(this) {
                    pointList.clear()
                    lineList.clear()
                    rectList.clear()
                }
                trendLine = null
                invalidate()
            }
        }
    var tempTextSize: Int
        get() = helper.textSize
        set(value) {
            helper.textSize = value
            invalidate()
        }
    var textColor: Int
        @ColorInt get() = helper.textColor
        set(
            @ColorInt value
        ) {
            helper.textColor = value
            invalidate()
        }
    var onPointListener: ((pointList: List<Point>) -> Unit)? = null
    var onLineListener: ((lineList: List<Point>) -> Unit)? = null
    var onRectListener: ((rectList: List<Rect>) -> Unit)? = null
    var onTrendOperateListener: ((isAdd: Boolean) -> Unit)? = null
    protected val pointList = ArrayList<Point>()
    protected val lineList = ArrayList<Line>()
    protected val rectList = ArrayList<Rect>()

    @Volatile
    protected var trendLine: Line? = null
    protected fun getPointListSafe(): List<Point> = synchronized(this) { pointList }
    protected fun getLineListSafe(): List<Line> = synchronized(this) { lineList }
    protected fun getRectListSafe(): List<Rect> = synchronized(this) { rectList }
    private fun getSourcePointList(): List<Point> {
        val resultList = ArrayList<Point>(pointList.size)
        pointList.forEach {
            resultList.add(Point((it.x / xScale).toInt(), (it.y / yScale).toInt()))
        }
        return resultList
    }

    private fun getSourceLineList(): List<Point> {
        val resultList = ArrayList<Point>(lineList.size * 2)
        lineList.forEach {
            val startPoint = Point((it.start.x / xScale).toInt(), (it.start.y / yScale).toInt())
            val endPoint = Point((it.end.x / xScale).toInt(), (it.end.y / yScale).toInt())
            resultList.add(startPoint)
            resultList.add(endPoint)
        }
        return resultList
    }

    private fun getSourceRectList(): List<Rect> {
        val resultList = ArrayList<Rect>(rectList.size)
        rectList.forEach {
            val left = (it.left / xScale).toInt()
            val right = (it.right / xScale).toInt()
            val top = (it.top / yScale).toInt()
            val bottom = (it.bottom / yScale).toInt()
            resultList.add(Rect(left, top, right, bottom))
        }
        return resultList
    }

    private val helper = TempDrawHelper()
    protected var xScale = 0f
    protected var yScale = 0f
    protected var imageWidth = 0
    protected var imageHeight = 0

    @CallSuper
    open fun setImageSize(
        imageWidth: Int,
        imageHeight: Int,
    ) {
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        this.xScale = width.toFloat() / imageWidth.toFloat()
        this.yScale = height.toFloat() / imageHeight.toFloat()
    }

    protected val maxCount: Int

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes,
    ) {
        val typeArray = context.obtainStyledAttributes(attrs, R.styleable.TemperatureBaseView)
        maxCount = typeArray.getInt(R.styleable.TemperatureBaseView_maxCount, DEFAULT_MAX_COUNT)
        typeArray.recycle()
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        xScale = measuredWidth.toFloat() / imageWidth
        yScale = measuredHeight.toFloat() / imageHeight
    }

    protected fun drawPoint(
        canvas: Canvas,
        point: Point,
    ) {
        helper.drawPoint(canvas, point.x, point.y)
    }

    protected fun drawLine(
        canvas: Canvas,
        line: Line,
    ) {
        val startX: Int = ((line.start.x / xScale).toInt() * xScale).toInt()
        val startY: Int = ((line.start.y / yScale).toInt() * yScale).toInt()
        val stopX: Int = ((line.end.x / xScale).toInt() * xScale).toInt()
        val stopY: Int = ((line.end.y / yScale).toInt() * yScale).toInt()
        helper.drawLine(canvas, startX, startY, stopX, stopY)
    }

    protected fun drawRect(
        canvas: Canvas,
        rect: Rect,
    ) {
        val left: Int = ((rect.left / xScale).toInt() * xScale).toInt()
        val top: Int = ((rect.top / yScale).toInt() * yScale).toInt()
        val right: Int = ((rect.right / xScale).toInt() * xScale).toInt()
        val bottom: Int = ((rect.bottom / yScale).toInt() * yScale).toInt()
        helper.drawRect(canvas, left, top, right, bottom)
    }

    protected fun drawCircle(
        canvas: Canvas,
        x: Int,
        y: Int,
        isMax: Boolean,
    ) {
        helper.drawCircle(canvas, x, y, isMax)
    }

    protected fun drawTempText(
        canvas: Canvas,
        x: Int,
        y: Int,
        temp: Float,
    ) {
        helper.drawTempText(canvas, UnitTools.showC(temp), width, x, y)
    }

    protected fun drawTrendText(
        canvas: Canvas,
        line: Line,
    ) {
        helper.drawTrendText(
            canvas,
            width,
            height,
            line.start.x,
            line.start.y,
            line.end.x,
            line.end.y
        )
    }

    protected fun drawPointName(
        canvas: Canvas,
        name: String,
        point: Point,
    ) {
        val x = ((point.x / xScale).toInt() * xScale).toInt()
        val y = ((point.y / yScale).toInt() * yScale).toInt()
        helper.drawPointName(canvas, name, width, height, x, y)
    }

    protected fun drawLineName(
        canvas: Canvas,
        name: String,
        line: Line,
    ) {
        val startX = ((line.start.x / xScale).toInt() * xScale).toInt()
        val startY = ((line.start.y / yScale).toInt() * yScale).toInt()
        val stopX = ((line.end.x / xScale).toInt() * xScale).toInt()
        val stopY = ((line.end.y / yScale).toInt() * yScale).toInt()
        helper.drawPointRectName(canvas, name, width, height, startX, startY, stopX, stopY)
    }

    protected fun drawRectName(
        canvas: Canvas,
        name: String,
        rect: Rect,
    ) {
        val left: Int = ((rect.left / xScale).toInt() * xScale).toInt()
        val top: Int = ((rect.top / yScale).toInt() * yScale).toInt()
        val right: Int = ((rect.right / xScale).toInt() * xScale).toInt()
        val bottom: Int = ((rect.bottom / yScale).toInt() * yScale).toInt()
        helper.drawPointRectName(canvas, name, width, height, left, top, right, bottom)
    }

    private var downX = 0
    private var downY = 0
    private var isAddAction = true

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
            return false
        }
        return when (mode) {
            Mode.POINT -> touchPoint(event)
            Mode.LINE -> touchLine(event, false)
            Mode.RECT -> touchRect(event)
            Mode.TREND -> touchLine(event, true)
            else -> super.onTouchEvent(event)
        }
    }

    protected var operatePoint: Point? = null
    private fun touchPoint(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x.correctPoint(width)
                downY = event.y.correctPoint(height)
                val point: Point? = pollPoint(downX, downY)
                isAddAction = point == null
                operatePoint = point ?: Point(downX, downY)
                if (point == null && pointList.size == maxCount) {
                    synchronized(this) {
                        pointList.removeAt(0)
                    }
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                operatePoint?.x = event.x.correctPoint(width)
                operatePoint?.y = event.y.correctPoint(height)
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val x: Int = event.x.correctPoint(width)
                val y: Int = event.y.correctPoint(height)
                operatePoint?.x = x
                operatePoint?.y = y
                if (isAddAction || abs(x - downX) > DELETE_TOLERANCE || abs(y - downY) > DELETE_TOLERANCE) {
                    synchronized(this) {
                        pointList.add(operatePoint ?: Point())
                    }
                }
                operatePoint = null
                invalidate()
                onPointListener?.invoke(getSourcePointList())
                return true
            }

            else -> return false
        }
    }

    private fun pollPoint(
        x: Int,
        y: Int,
    ): Point? {
        for (i in pointList.size - 1 downTo 0) {
            val point: Point = pointList[i]
            if (point.x in x - TOUCH_TOLERANCE..x + TOUCH_TOLERANCE && point.y in y - TOUCH_TOLERANCE..y + TOUCH_TOLERANCE) {
                return synchronized(this) { pointList.removeAt(i) }
            }
        }
        return null
    }

    protected var operateLine: Line? = null
    protected var operateTrend: Line? = null

    private enum class LineMoveType { ALL, START, END, }

    private var lineMoveType = LineMoveType.ALL
    private val downLine: Line = Line(Point(0, 0), Point(0, 0))
    private fun touchLine(
        event: MotionEvent,
        isTrend: Boolean,
    ): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x.correct(width)
                downY = event.y.correct(height)
                val line: Line? = pollLine(downX, downY, isTrend)
                if (line == null) {
                    isAddAction = true
                    if (isTrend) {
                        operateTrend = Line(Point(downX, downY), Point(downX, downY))
                        trendLine = null
                        onTrendOperateListener?.invoke(false)
                    } else {
                        operateLine = Line(Point(downX, downY), Point(downX, downY))
                        if (lineList.size == maxCount) {
                            synchronized(this) {
                                lineList.removeAt(0)
                            }
                        }
                    }
                } else {
                    isAddAction = false
                    if (isTrend) {
                        operateTrend = line
                        onTrendOperateListener?.invoke(false)
                    } else {
                        operateLine = line
                    }
                    downLine.start.set(line.start.x, line.start.y)
                    downLine.end.set(line.end.x, line.end.y)
                    lineMoveType =
                        if (downX > line.start.x - TOUCH_TOLERANCE && downX < line.start.x + TOUCH_TOLERANCE &&
                            downY > line.start.y - TOUCH_TOLERANCE && downY < line.start.y + TOUCH_TOLERANCE
                        ) {
                            LineMoveType.START
                        } else if (downX > line.end.x - TOUCH_TOLERANCE && downX < line.end.x + TOUCH_TOLERANCE &&
                            downY > line.end.y - TOUCH_TOLERANCE && downY < line.end.y + TOUCH_TOLERANCE
                        ) {
                            LineMoveType.END
                        } else {
                            LineMoveType.ALL
                        }
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val x: Int = event.x.correct(width)
                val y: Int = event.y.correct(height)
                if (isAddAction) {
                    (if (isTrend) operateTrend else operateLine)?.end?.x = x
                    (if (isTrend) operateTrend else operateLine)?.end?.y = y
                } else {
                    when (lineMoveType) {
                        LineMoveType.ALL -> {
                            val rect: Rect = TempDrawHelper.getRect(width, height)
                            val minX: Int = min(downLine.start.x, downLine.end.x)
                            val maxX: Int = max(downLine.start.x, downLine.end.x)
                            val minY: Int = min(downLine.start.y, downLine.end.y)
                            val maxY: Int = max(downLine.start.y, downLine.end.y)
                            val biasX: Int =
                                if (x < downX) max(x - downX, rect.left - minX) else min(
                                    x - downX,
                                    rect.right - maxX
                                )
                            val biasY: Int =
                                if (y < downY) max(y - downY, rect.top - minY) else min(
                                    y - downY,
                                    rect.bottom - maxY
                                )
                            (if (isTrend) operateTrend else operateLine)?.start?.set(
                                downLine.start.x + biasX,
                                downLine.start.y + biasY
                            )
                            (if (isTrend) operateTrend else operateLine)?.end?.set(
                                downLine.end.x + biasX,
                                downLine.end.y + biasY
                            )
                        }

                        LineMoveType.START -> {
                            (if (isTrend) operateTrend else operateLine)?.start?.x = x
                            (if (isTrend) operateTrend else operateLine)?.start?.y = y
                        }

                        LineMoveType.END -> {
                            (if (isTrend) operateTrend else operateLine)?.end?.x = x
                            (if (isTrend) operateTrend else operateLine)?.end?.y = y
                        }
                    }
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val x: Int = event.x.correct(width)
                val y: Int = event.y.correct(height)
                val line: Line =
                    (if (isTrend) operateTrend else operateLine) ?: Line(Point(), Point())
                if ((line.start.x / xScale).toInt() != (line.end.x / xScale).toInt() || (line.start.y / yScale).toInt() != (line.end.y / yScale).toInt()) {
                    if (isAddAction || abs(x - downX) > DELETE_TOLERANCE || abs(y - downY) > DELETE_TOLERANCE) {
                        if (isTrend) {
                            trendLine = line
                            onTrendOperateListener?.invoke(true)
                        } else {
                            synchronized(this) {
                                lineList.add(line)
                            }
                        }
                    }
                }
                operateTrend = null
                operateLine = null
                invalidate()
                if (!isTrend) {
                    onLineListener?.invoke(getSourceLineList())
                }
                return true
            }

            else -> return false
        }
    }

    private fun pollLine(
        x: Int,
        y: Int,
        isTrend: Boolean,
    ): Line? {
        if (isTrend) {
            val resultLine = trendLine
            if (isLineConcat(resultLine, x, y)) {
                trendLine = null
                return resultLine
            }
        } else {
            for (i in lineList.size - 1 downTo 0) {
                val line: Line = lineList[i]
                if (isLineConcat(line, x, y)) {
                    return synchronized(this) { lineList.removeAt(i) }
                }
            }
        }
        return null
    }

    private fun isLineConcat(
        line: Line?,
        x: Int,
        y: Int,
    ): Boolean {
        if (line == null) {
            return false
        }
        var tempDistance =
            (line.end.y - line.start.y) * x - (line.end.x - line.start.x) * y + line.end.x * line.start.y - line.start.x * line.end.y
        tempDistance = (tempDistance / sqrt(
            (line.end.y - line.start.y).toDouble().pow(2.0) + (line.end.x - line.start.x).toDouble()
                .pow(2.0)
        )).toInt()
        return abs(tempDistance) < TOUCH_TOLERANCE && x > min(
            line.start.x,
            line.end.x
        ) - TOUCH_TOLERANCE && x < max(line.start.x, line.end.x) + TOUCH_TOLERANCE
    }

    protected var operateRect: Rect? = null

    private enum class RectMoveType { ALL, EDGE, CORNER, }

    private var rectMoveType = RectMoveType.ALL

    private enum class RectMoveEdge { LEFT, TOP, RIGHT, BOTTOM }

    private var rectMoveEdge = RectMoveEdge.LEFT

    private enum class RectMoveCorner { LT, RT, RB, LB }

    private var rectMoveCorner = RectMoveCorner.LT
    private val downRect = Rect()
    private fun touchRect(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x.correct(width)
                downY = event.y.correct(height)
                val rect: Rect? = pollRect(downX, downY)
                if (rect == null) {
                    isAddAction = true
                    operateRect = Rect(downX, downY, downX, downY)
                    if (rectList.size == maxCount) {
                        synchronized(this) {
                            rectList.removeAt(0)
                        }
                    }
                } else {
                    isAddAction = false
                    operateRect = rect
                    downRect.set(rect)
                    when (downX) {
                        in rect.left - TOUCH_TOLERANCE..rect.left + TOUCH_TOLERANCE -> {
                            when (downY) {
                                in rect.top - TOUCH_TOLERANCE..rect.top + TOUCH_TOLERANCE -> {
                                    rectMoveType = RectMoveType.CORNER
                                    rectMoveCorner = RectMoveCorner.LT
                                }

                                in rect.bottom - TOUCH_TOLERANCE..rect.bottom + TOUCH_TOLERANCE -> {
                                    rectMoveType = RectMoveType.CORNER
                                    rectMoveCorner = RectMoveCorner.LB
                                }

                                else -> {
                                    rectMoveType = RectMoveType.EDGE
                                    rectMoveEdge = RectMoveEdge.LEFT
                                }
                            }
                        }

                        in rect.right - TOUCH_TOLERANCE..rect.right + TOUCH_TOLERANCE -> {
                            when (downY) {
                                in rect.top - TOUCH_TOLERANCE..rect.top + TOUCH_TOLERANCE -> {
                                    rectMoveType = RectMoveType.CORNER
                                    rectMoveCorner = RectMoveCorner.RT
                                }

                                in rect.bottom - TOUCH_TOLERANCE..rect.bottom + TOUCH_TOLERANCE -> {
                                    rectMoveType = RectMoveType.CORNER
                                    rectMoveCorner = RectMoveCorner.RB
                                }

                                else -> {
                                    rectMoveType = RectMoveType.EDGE
                                    rectMoveEdge = RectMoveEdge.RIGHT
                                }
                            }
                        }

                        else -> {
                            when (downY) {
                                in rect.top - TOUCH_TOLERANCE..rect.top + TOUCH_TOLERANCE -> {
                                    rectMoveType = RectMoveType.EDGE
                                    rectMoveEdge = RectMoveEdge.TOP
                                }

                                in rect.bottom - TOUCH_TOLERANCE..rect.bottom + TOUCH_TOLERANCE -> {
                                    rectMoveType = RectMoveType.EDGE
                                    rectMoveEdge = RectMoveEdge.BOTTOM
                                }

                                else -> {
                                    rectMoveType = RectMoveType.ALL
                                }
                            }
                        }
                    }
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val x: Int = event.x.correct(width)
                val y: Int = event.y.correct(height)
                if (isAddAction) {
                    operateRect?.set(min(downX, x), min(downY, y), max(downX, x), max(downY, y))
                } else {
                    when (rectMoveType) {
                        RectMoveType.ALL -> {
                            val rect: Rect = TempDrawHelper.getRect(width, height)
                            val biasX: Int =
                                if (x < downX) {
                                    max(
                                        x - downX,
                                        rect.left - downRect.left,
                                    )
                                } else {
                                    min(x - downX, rect.right - downRect.right)
                                }
                            val biasY: Int =
                                if (y < downY) {
                                    max(
                                        y - downY,
                                        rect.top - downRect.top,
                                    )
                                } else {
                                    min(y - downY, rect.bottom - downRect.bottom)
                                }
                            operateRect?.set(
                                downRect.left + biasX,
                                downRect.top + biasY,
                                downRect.right + biasX,
                                downRect.bottom + biasY
                            )
                        }

                        RectMoveType.EDGE ->
                            when (rectMoveEdge) {
                                RectMoveEdge.LEFT -> {
                                    operateRect?.left = min(x, downRect.right)
                                    operateRect?.right = max(x, downRect.right)
                                }

                                RectMoveEdge.TOP -> {
                                    operateRect?.top = min(y, downRect.bottom)
                                    operateRect?.bottom = max(y, downRect.bottom)
                                }

                                RectMoveEdge.RIGHT -> {
                                    operateRect?.right = max(x, downRect.left)
                                    operateRect?.left = min(x, downRect.left)
                                }

                                RectMoveEdge.BOTTOM -> {
                                    operateRect?.bottom = max(y, downRect.top)
                                    operateRect?.top = min(y, downRect.top)
                                }
                            }

                        RectMoveType.CORNER ->
                            when (rectMoveCorner) {
                                RectMoveCorner.LT -> {
                                    operateRect?.left = min(x, downRect.right)
                                    operateRect?.right = max(x, downRect.right)
                                    operateRect?.top = min(y, downRect.bottom)
                                    operateRect?.bottom = max(y, downRect.bottom)
                                }

                                RectMoveCorner.RT -> {
                                    operateRect?.right = max(x, downRect.left)
                                    operateRect?.left = min(x, downRect.left)
                                    operateRect?.top = min(y, downRect.bottom)
                                    operateRect?.bottom = max(y, downRect.bottom)
                                }

                                RectMoveCorner.RB -> {
                                    operateRect?.right = max(x, downRect.left)
                                    operateRect?.left = min(x, downRect.left)
                                    operateRect?.bottom = max(y, downRect.top)
                                    operateRect?.top = min(y, downRect.top)
                                }

                                RectMoveCorner.LB -> {
                                    operateRect?.left = min(x, downRect.right)
                                    operateRect?.right = max(x, downRect.right)
                                    operateRect?.bottom = max(y, downRect.top)
                                    operateRect?.top = min(y, downRect.top)
                                }
                            }
                    }
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val x: Int = event.x.correct(width)
                val y: Int = event.y.correct(height)
                val rect: Rect = operateRect ?: Rect()
                if ((rect.left / xScale).toInt() != (rect.right / xScale).toInt() &&
                    (rect.top / yScale).toInt() != (rect.bottom / yScale).toInt()
                ) {
                    if (isAddAction || abs(x - downX) > DELETE_TOLERANCE || abs(y - downY) > DELETE_TOLERANCE) {
                        synchronized(this) {
                            rectList.add(rect)
                        }
                    }
                }
                operateRect = null
                invalidate()
                onRectListener?.invoke(getSourceRectList())
                return true
            }

            else -> return false
        }
    }

    private fun pollRect(
        x: Int,
        y: Int,
    ): Rect? {
        for (i in rectList.size - 1 downTo 0) {
            val rect: Rect = rectList[i]
            if (rect.left - TOUCH_TOLERANCE < x && rect.right + TOUCH_TOLERANCE > x &&
                rect.top - TOUCH_TOLERANCE < y && rect.bottom + TOUCH_TOLERANCE > y
            ) {
                return synchronized(this) { rectList.removeAt(i) }
            }
        }
        return null
    }
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\TemperatureEditView.kt =====

package com.mpdc4gsr.module.thermalunified.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import android.util.AttributeSet
import com.energy.iruvc.sdkisp.LibIRTemp
import com.energy.iruvc.utils.Line
import com.mpdc4gsr.libunified.ir.utils.TempDrawHelper.Companion.correct
import com.mpdc4gsr.libunified.ir.view.ITsTempListener
import java.lang.ref.WeakReference

class TemperatureEditView : TemperatureBaseView {
    override var mode: Mode
        get() = super.mode
        set(value) {
            super.mode = value
            if (mode == Mode.CLEAR) {
                tempListData.pointTemps.clear()
                tempListData.lineTemps.clear()
                tempListData.rectangleTemps.clear()
                for (i in 0 until 3) {
                    val tmp = irtemp.TemperatureSampleResult()
                    tmp.type = -99
                    tempListData.pointTemps.add(tmp)
                    tempListData.lineTemps.add(tmp)
                    tempListData.rectangleTemps.add(tmp)
                }
            }
        }

    class TemperatureList {
        var pointTemps = arrayListOf<LibIRTemp.TemperatureSampleResult>()
        var lineTemps = arrayListOf<LibIRTemp.TemperatureSampleResult>()
        var rectangleTemps = arrayListOf<LibIRTemp.TemperatureSampleResult>()
    }

    var tempListData = TemperatureList()
    private var irtemp: LibIRTemp = LibIRTemp()
    private var irTempData: ByteArray = byteArrayOf()
    var fullInfo: LibIRTemp.TemperatureSampleResult? = null
    var isShowName = false
        set(value) {
            field = value
            invalidate()
        }
    private var iTsTempListenerWeakReference: WeakReference<ITsTempListener>? = null
    fun setITsTempListener(listener: ITsTempListener) {
        iTsTempListenerWeakReference = WeakReference(listener)
    }

    private fun getTSTemp(temp: Float): Float =
        iTsTempListenerWeakReference?.get()?.tempCorrectByTs(temp) ?: temp

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes,
    ) {
        tempListData.pointTemps.clear()
        tempListData.lineTemps.clear()
        tempListData.rectangleTemps.clear()
        for (i in 0 until 3) {
            val tmp = irtemp.TemperatureSampleResult()
            tmp.type = -99
            tempListData.pointTemps.add(tmp)
            tempListData.lineTemps.add(tmp)
            tempListData.rectangleTemps.add(tmp)
        }
    }

    override fun setImageSize(
        imageWidth: Int,
        imageHeight: Int,
    ) {
        super.setImageSize(imageWidth, imageHeight)
        irtemp = LibIRTemp(imageWidth, imageHeight)
    }

    fun setData(bytes: ByteArray) {
        irTempData = bytes
        irtemp.setTempData(irTempData)
        fullInfo = irtemp.getTemperatureOfRect(Rect(0, 0, imageWidth, imageHeight))
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        for (i in pointList.indices) {
            val result = drawOnePoint(canvas, pointList[i], i) ?: continue
            tempListData.pointTemps[i] = result
        }
        operatePoint?.let { drawOnePoint(canvas, it, pointList.size + 1) }
        for (i in lineList.indices) {
            val result = drawOneLine(canvas, lineList[i], i) ?: continue
            tempListData.lineTemps[i] = result
        }
        operateLine?.let { drawOneLine(canvas, it, lineList.size + 1) }
        for (i in rectList.indices) {
            val result = drawOneRect(canvas, rectList[i], i) ?: continue
            tempListData.rectangleTemps[i] = result
        }
        operateRect?.let { drawOneRect(canvas, it, rectList.size + 1) }
        if (isShowFull) {
            fullInfo?.let {
                val maxX: Int = (it.maxTemperaturePixel.x * xScale).correct(width)
                val maxY: Int = (it.maxTemperaturePixel.y * yScale).correct(height)
                drawCircle(canvas, maxX, maxY, true)
                drawTempText(canvas, maxX, maxY, getTSTemp(it.maxTemperature))
                val minX: Int = (it.minTemperaturePixel.x * xScale).correct(width)
                val minY: Int = (it.minTemperaturePixel.y * yScale).correct(height)
                drawCircle(canvas, minX, minY, false)
                drawTempText(canvas, minX, minY, getTSTemp(it.minTemperature))
            }
            val centerX = width / 2
            val centerY = height / 2
            val centerResult = irtemp.getTemperatureOfPoint(Point(imageWidth / 2, imageHeight / 2))
            drawPoint(canvas, Point(centerX, centerY))
            drawTempText(canvas, centerX, centerY, getTSTemp(centerResult.maxTemperature))
        }
    }

    private fun drawOnePoint(
        canvas: Canvas,
        point: Point,
        index: Int,
    ): LibIRTemp.TemperatureSampleResult? {
        val result =
            try {
                irtemp.getTemperatureOfPoint(
                    Point(
                        (point.x / xScale).toInt(),
                        (point.y / yScale).toInt()
                    )
                )
            } catch (_: IllegalArgumentException) {
                return null
            }
        drawPoint(canvas, point)
        drawCircle(canvas, point.x, point.y, true)
        drawTempText(canvas, point.x, point.y, getTSTemp(result.maxTemperature))
        if (isShowName) {
            drawPointName(canvas, "P${index + 1}", point)
        }
        return result
    }

    private fun drawOneLine(
        canvas: Canvas,
        line: Line,
        index: Int,
    ): LibIRTemp.TemperatureSampleResult? {
        drawLine(canvas, line)
        val tempStartX: Int = (line.start.x / xScale).toInt()
        val tempStartY: Int = (line.start.y / yScale).toInt()
        val tempStopX: Int = (line.end.x / xScale).toInt()
        val tempStopY: Int = (line.end.y / yScale).toInt()
        if (tempStartX == tempStopX && tempStartY == tempStopY) {
            return null
        }
        val result =
            try {
                irtemp.getTemperatureOfLine(
                    Line(
                        Point(tempStartX, tempStartY),
                        Point(tempStopX, tempStopY)
                    )
                )
            } catch (_: IllegalArgumentException) {
                return null
            }
        val maxX: Int = (result.maxTemperaturePixel.x * xScale).correct(width)
        val maxY: Int = (result.maxTemperaturePixel.y * yScale).correct(height)
        val minX: Int = (result.minTemperaturePixel.x * xScale).correct(width)
        val minY: Int = (result.minTemperaturePixel.y * yScale).correct(height)
        drawCircle(canvas, maxX, maxY, true)
        drawCircle(canvas, minX, minY, false)
        drawTempText(canvas, maxX, maxY, getTSTemp(result.maxTemperature))
        drawTempText(canvas, minX, minY, getTSTemp(result.minTemperature))
        if (isShowName) {
            drawLineName(canvas, "L${index + 1}", line)
        }
        return result
    }

    private fun drawOneRect(
        canvas: Canvas,
        rect: Rect,
        index: Int,
    ): LibIRTemp.TemperatureSampleResult? {
        drawRect(canvas, rect)
        val left = (rect.left / xScale).toInt()
        val top = (rect.top / yScale).toInt()
        val right = (rect.right / xScale).toInt()
        val bottom = (rect.bottom / yScale).toInt()
        if (left == right || top == bottom) {
            return null
        }
        val result =
            try {
                irtemp.getTemperatureOfRect(Rect(left, top, right, bottom))
            } catch (_: IllegalArgumentException) {
                return null
            }
        val maxX: Int = (result.maxTemperaturePixel.x * xScale).correct(width)
        val maxY: Int = (result.maxTemperaturePixel.y * yScale).correct(height)
        val minX: Int = (result.minTemperaturePixel.x * xScale).correct(width)
        val minY: Int = (result.minTemperaturePixel.y * yScale).correct(height)
        drawCircle(canvas, maxX, maxY, true)
        drawCircle(canvas, minX, minY, false)
        drawTempText(canvas, maxX, maxY, getTSTemp(result.maxTemperature))
        drawTempText(canvas, minX, minY, getTSTemp(result.minTemperature))
        if (isShowName) {
            drawRectName(canvas, "R${index + 1}", rect)
        }
        return result
    }
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\TemperatureHikView.kt =====

package com.mpdc4gsr.module.thermalunified.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.energy.iruvc.sdkisp.LibIRProcess
import com.energy.iruvc.sdkisp.LibIRTemp
import com.energy.iruvc.sdkisp.LibIRTemp.TemperatureSampleResult
import com.energy.iruvc.utils.CommonParams.IRPROCSRCFMTType
import com.energy.iruvc.utils.Line
import com.mpdc4gsr.libunified.ir.utils.TempDrawHelper.Companion.correct
import com.mpdc4gsr.libunified.ir.utils.TempUtils

class TemperatureHikView : TemperatureBaseView {
    @Volatile
    private var tempInfo = TempInfo()
    private var libIRTemp = LibIRTemp()
    private var calculateThread: CalculateThread? = null

    @Volatile
    var rotateAngle: Int = 270
        set(value) {
            field = value
            val isPortrait = value == 90 || value == 270
            setImageSize(if (isPortrait) 192 else 256, if (isPortrait) 256 else 192)
        }

    @Volatile
    var onTempChangeListener: ((min: Float, max: Float) -> Unit)? = null
    var onTrendChangeListener: ((tempList: List<Float>) -> Unit)? = null
    var onTempResultListener: ((tempInfo: TempInfo) -> Unit)? = null
    private var wantAddPoint: Point? = null
    private var wantAddLine: Line? = null
    private var wantAddRect: Rect? = null
    fun addSourcePoint(point: Point) {
        if (xScale > 0 && yScale > 0) {
            synchronized(this) {
                if (pointList.size == maxCount) {
                    pointList.removeAt(0)
                }
                pointList.add(Point((point.x * xScale).toInt(), (point.y * yScale).toInt()))
            }
            invalidate()
        } else {
            wantAddPoint = point
        }
    }

    fun addSourceLine(line: Line) {
        if (xScale > 0 && yScale > 0) {
            val start = Point((line.start.x * xScale).toInt(), (line.start.y * yScale).toInt())
            val end = Point((line.end.x * xScale).toInt(), (line.end.y * yScale).toInt())
            synchronized(this) {
                if (lineList.size == maxCount) {
                    lineList.removeAt(0)
                }
                lineList.add(Line(start, end))
            }
            invalidate()
        } else {
            wantAddLine = line
        }
    }

    fun addSourceRect(rect: Rect) {
        if (xScale > 0 && yScale > 0) {
            val left = (rect.left * xScale).toInt()
            val right = (rect.right * xScale).toInt()
            val top = (rect.top * yScale).toInt()
            val bottom = (rect.bottom * yScale).toInt()
            synchronized(this) {
                if (rectList.size == maxCount) {
                    rectList.removeAt(0)
                }
                rectList.add(Rect(left, top, right, bottom))
            }
            invalidate()
        } else {
            wantAddRect = rect
        }
    }

    private val imageRes = LibIRProcess.ImageRes_t()
    private var beforeTime: Long = 0
    private val sourceTempArray = ByteArray(256 * 192 * 2)
    private val rotateTempArray = ByteArray(256 * 192 * 2)
    fun refreshTemp(newData: ByteArray) {
        val currentTime: Long = System.currentTimeMillis()
        if (currentTime - beforeTime > 1000) {
            beforeTime = currentTime
            System.arraycopy(newData, 0, sourceTempArray, 0, sourceTempArray.size)
            when (rotateAngle) {
                90 -> LibIRProcess.rotateLeft90(
                    sourceTempArray,
                    imageRes,
                    IRPROCSRCFMTType.IRPROC_SRC_FMT_Y14,
                    rotateTempArray
                )

                180 -> LibIRProcess.rotate180(
                    sourceTempArray,
                    imageRes,
                    IRPROCSRCFMTType.IRPROC_SRC_FMT_Y14,
                    rotateTempArray
                )

                270 -> LibIRProcess.rotateRight90(
                    sourceTempArray,
                    imageRes,
                    IRPROCSRCFMTType.IRPROC_SRC_FMT_Y14,
                    rotateTempArray
                )

                else -> System.arraycopy(
                    sourceTempArray,
                    0,
                    rotateTempArray,
                    0,
                    rotateTempArray.size
                )
            }
            libIRTemp.setTempData(rotateTempArray)
            if (mode != Mode.CLEAR) {
                calculateThread?.calculateTemp()
            }
        }
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes,
    ) {
        imageRes.width = 256.toChar()
        imageRes.height = 192.toChar()
        setImageSize(192, 256)
        if (context is ComponentActivity) {
            context.lifecycle.addObserver(MyLifecycleObserver())
        }
    }

    override fun setImageSize(
        imageWidth: Int,
        imageHeight: Int,
    ) {
        super.setImageSize(imageWidth, imageHeight)
        libIRTemp = LibIRTemp(imageWidth, imageHeight)
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        wantAddPoint?.let {
            addSourcePoint(it)
            wantAddPoint = null
        }
        wantAddLine?.let {
            addSourceLine(it)
            wantAddLine = null
        }
        wantAddRect?.let {
            addSourceRect(it)
            wantAddRect = null
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        if (isShowFull || pointList.isNotEmpty() || lineList.isNotEmpty() || rectList.isNotEmpty()) {
            drawPoint(canvas, Point(width / 2, height / 2))
            tempInfo.center?.let {
                drawTempText(canvas, width / 2, height / 2, it.maxTemperature)
            }
        }
        if (isShowFull) {
            tempInfo.full?.let {
                val minX: Int = (it.minTemperaturePixel.x * xScale).toInt()
                val minY: Int = (it.minTemperaturePixel.y * yScale).toInt()
                drawCircle(canvas, minX, minY, false)
                drawTempText(canvas, minX, minY, it.minTemperature)
                val maxX: Int = (it.maxTemperaturePixel.x * xScale).toInt()
                val maxY: Int = (it.maxTemperaturePixel.y * yScale).toInt()
                drawCircle(canvas, maxX, maxY, true)
                drawTempText(canvas, maxX, maxY, it.maxTemperature)
            }
        }
        for (i in pointList.indices) {
            val point: Point = pointList[i]
            drawPoint(canvas, point)
            if (i < tempInfo.pointResults.size) {
                drawCircle(canvas, point.x, point.y, true)
                drawTempText(canvas, point.x, point.y, tempInfo.pointResults[i].maxTemperature)
            }
        }
        operatePoint?.let { drawPoint(canvas, it) }
        for (i in lineList.indices) {
            drawLine(canvas, lineList[i])
            if (i < tempInfo.lineResults.size) {
                val result: TemperatureSampleResult = tempInfo.lineResults[i]
                val maxX: Int = (result.maxTemperaturePixel.x * xScale).correct(width)
                val maxY: Int = (result.maxTemperaturePixel.y * yScale).correct(height)
                val minX: Int = (result.minTemperaturePixel.x * xScale).correct(width)
                val minY: Int = (result.minTemperaturePixel.y * yScale).correct(height)
                drawCircle(canvas, maxX, maxY, true)
                drawCircle(canvas, minX, minY, false)
                drawTempText(canvas, maxX, maxY, result.maxTemperature)
                drawTempText(canvas, minX, minY, result.minTemperature)
            }
        }
        operateLine?.let { drawLine(canvas, it) }
        for (i in rectList.indices) {
            drawRect(canvas, rectList[i])
            if (i < tempInfo.rectResults.size) {
                val result: TemperatureSampleResult = tempInfo.rectResults[i]
                val maxX: Int = (result.maxTemperaturePixel.x * xScale).correct(width)
                val maxY: Int = (result.maxTemperaturePixel.y * yScale).correct(height)
                val minX: Int = (result.minTemperaturePixel.x * xScale).correct(width)
                val minY: Int = (result.minTemperaturePixel.y * yScale).correct(height)
                drawCircle(canvas, maxX, maxY, true)
                drawCircle(canvas, minX, minY, false)
                drawTempText(canvas, maxX, maxY, result.maxTemperature)
                drawTempText(canvas, minX, minY, result.minTemperature)
            }
        }
        operateRect?.let { drawRect(canvas, it) }
        trendLine?.let {
            drawLine(canvas, it)
            drawTrendText(canvas, it)
            val result: TemperatureSampleResult = tempInfo.trend ?: return@let
            val maxX: Int = (result.maxTemperaturePixel.x * xScale).correct(width)
            val maxY: Int = (result.maxTemperaturePixel.y * yScale).correct(height)
            val minX: Int = (result.minTemperaturePixel.x * xScale).correct(width)
            val minY: Int = (result.minTemperaturePixel.y * yScale).correct(height)
            drawCircle(canvas, maxX, maxY, true)
            drawCircle(canvas, minX, minY, false)
            drawTempText(canvas, maxX, maxY, result.maxTemperature)
            drawTempText(canvas, minX, minY, result.minTemperature)
        }
        operateTrend?.let {
            drawLine(canvas, it)
            drawTrendText(canvas, it)
        }
    }

    private inner class CalculateThread : HandlerThread("Calculate Thread") {
        private val mainHandler = Handler(Looper.getMainLooper())
        private var currentHandler: Handler? = null
        override fun start() {
            super.start()
            val looper: Looper = getLooper() ?: return
            currentHandler = MyHandler(looper)
        }

        override fun quit(): Boolean {
            mainHandler.removeCallbacksAndMessages(null)
            return super.quit()
        }

        fun calculateTemp() {
            currentHandler?.sendEmptyMessage(0)
        }

        private inner class MyHandler(looper: Looper) : Handler(looper) {
            override fun handleMessage(msg: Message) {
                val fullResult = libIRTemp.getTemperatureOfRect(Rect(0, 0, imageWidth, imageHeight))
                mainHandler.post {
                    onTempChangeListener?.invoke(
                        fullResult.minTemperature,
                        fullResult.maxTemperature
                    )
                }
                if (mode == Mode.CLEAR) {
                    return
                }
                val centerResult = if (isShowFull) libIRTemp.getTemperatureOfPoint(
                    Point(
                        imageWidth / 2,
                        imageHeight / 2
                    )
                ) else null
                var trendResult: TemperatureSampleResult? = null
                trendLine?.let {
                    val startPoint =
                        Point((it.start.x / xScale).toInt(), (it.start.y / yScale).toInt())
                    val endPoint = Point((it.end.x / xScale).toInt(), (it.end.y / yScale).toInt())
                    try {
                        trendResult = libIRTemp.getTemperatureOfLine(Line(startPoint, endPoint))
                    } catch (_: IllegalArgumentException) {
                    }
                    val tempList: List<Float> =
                        TempUtils.getLineTemps(startPoint, endPoint, rotateTempArray, imageWidth)
                    mainHandler.post {
                        onTrendChangeListener?.invoke(tempList)
                    }
                }
                val pointList: List<Point> = getPointListSafe()
                val pointResultList: ArrayList<TemperatureSampleResult> = ArrayList(pointList.size)
                for (point in pointList) {
                    val sourcePoint = Point((point.x / xScale).toInt(), (point.y / yScale).toInt())
                    try {
                        pointResultList.add(libIRTemp.getTemperatureOfPoint(sourcePoint))
                    } catch (_: IllegalArgumentException) {
                    }
                }
                val lineList: List<Line> = getLineListSafe()
                val lineResultList: ArrayList<TemperatureSampleResult> = ArrayList(lineList.size)
                for (line in lineList) {
                    val sourceLine =
                        Line(
                            Point((line.start.x / xScale).toInt(), (line.start.y / yScale).toInt()),
                            Point((line.end.x / xScale).toInt(), (line.end.y / yScale).toInt()),
                        )
                    try {
                        lineResultList.add(libIRTemp.getTemperatureOfLine(sourceLine))
                    } catch (_: IllegalArgumentException) {
                    }
                }
                val rectList: List<Rect> = getRectListSafe()
                val rectResultList: ArrayList<TemperatureSampleResult> = ArrayList(rectList.size)
                for (rect in rectList) {
                    val sourceRect =
                        Rect(
                            (rect.left / xScale).toInt(),
                            (rect.top / yScale).toInt(),
                            (rect.right / xScale).toInt(),
                            (rect.bottom / yScale).toInt(),
                        )
                    try {
                        rectResultList.add(libIRTemp.getTemperatureOfRect(sourceRect))
                    } catch (_: IllegalArgumentException) {
                    }
                }
                tempInfo = TempInfo(
                    centerResult,
                    if (isShowFull) fullResult else null,
                    trendResult,
                    pointResultList,
                    lineResultList,
                    rectResultList
                )
                mainHandler.post {
                    onTempResultListener?.invoke(tempInfo)
                }
                postInvalidate()
            }
        }
    }

    private inner class MyLifecycleObserver : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            calculateThread = CalculateThread()
            calculateThread?.start()
        }

        override fun onStop(owner: LifecycleOwner) {
            calculateThread?.quit()
            calculateThread = null
        }
    }

    data class TempInfo(
        val center: TemperatureSampleResult? = null,
        val full: TemperatureSampleResult? = null,
        val trend: TemperatureSampleResult? = null,
        val pointResults: List<TemperatureSampleResult> = ArrayList(0),
        val lineResults: List<TemperatureSampleResult> = ArrayList(0),
        val rectResults: List<TemperatureSampleResult> = ArrayList(0),
    )
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\TimeDownView.kt =====

package com.mpdc4gsr.module.thermalunified.view

import android.content.Context
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.view.Gravity
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import androidx.appcompat.widget.AppCompatTextView
import com.mpdc4gsr.module.thermalunified.compat.spToPx
import java.util.*

public class TimeDownView : AppCompatTextView {
    private var timer: Timer? = null
    private var downTimerTask: DownTimerTask? = null
    private var downCount = 0
    private var lastDown = 0
    private var intervalMills: Long = 0
    private var delayMills: Long = 0
    private var animationSet: AnimationSet? = null
    var isRunning = false
    private fun init() {
        if (animationSet == null) {
            animationSet = AnimationSet(true)
        }
        if (downHandler == null) {
            downHandler = DownHandler()
        }
        gravity = Gravity.CENTER
        textSize = 30f.spToPx(context)
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle,
    ) {
        init()
    }

    fun downSecond(seconds: Int) {
        downSecond(seconds, true)
    }

    fun downSecond(
        seconds: Int,
        openAnimation: Boolean,
    ) {
        if (seconds == 0) {
            isRunning = false
            visibility = GONE
            downTimeWatcher?.onLastTimeFinish(seconds)
            onFinishListener?.invoke()
        } else {
            visibility = VISIBLE
            isRunning = true
            downTime(seconds, 1, 0, 1000, openAnimation)
        }
    }

    fun downTime(
        downCount: Int,
        lastDown: Int,
        delayMills: Long,
        intervalMills: Long,
        startAnimate: Boolean,
    ) {
        timer = Timer()
        this.downCount = downCount
        this.lastDown = lastDown
        this.delayMills = delayMills
        this.intervalMills = intervalMills
        if (startAnimate) {
            initDefaultAnimate()
        }
        downTimerTask = DownTimerTask()
        timer?.schedule(downTimerTask, delayMills, intervalMills)
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        if (GONE == visibility) {
            downTimerTask = null
            timer?.cancel()
            timer = null
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (drawTextFlag == DRAW_TEXT_NO) {
            return
        }
        super.onDraw(canvas)
    }

    fun cancel() {
        animationSet?.cancel()
        downTimerTask?.cancel()
        timer?.cancel()
        drawTextFlag = DRAW_TEXT_NO
        invalidate()
        visibility = GONE
        downTimerTask = null
        timer = null
        isRunning = false
    }

    private inner class DownTimerTask : TimerTask() {
        override fun run() {
            if (downCount >= lastDown - 1) {
                val msg = Message.obtain()
                msg.what = 1
                downHandler!!.sendMessage(msg)
            }
        }
    }

    interface DownTimeWatcher {
        fun onTime(num: Int)
        fun onLastTime(num: Int)
        fun onLastTimeFinish(num: Int)
    }

    var onTimeListener: ((time: Int) -> Unit)? = null
    var onFinishListener: (() -> Unit)? = null
    var downTimeWatcher: DownTimeWatcher? = null
    fun setOnTimeDownListener(downTimeWatcher: DownTimeWatcher?) {
        this.downTimeWatcher = downTimeWatcher
    }

    private var downHandler: DownHandler? = null

    private inner class DownHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg.what == 1) {
                if (downTimeWatcher != null) {
                    downTimeWatcher!!.onTime(downCount)
                }
                onTimeListener?.invoke(downCount)
                if (downCount >= lastDown - 1) {
                    drawTextFlag = DRAW_TEXT_YES
                    if (downCount >= lastDown) {
                        text = downCount.toString() + ""
                        startDefaultAnimate()
                        if (downCount == lastDown && downTimeWatcher != null) {
                            downTimeWatcher!!.onLastTime(downCount)
                        }
                    } else if (downCount == lastDown - 1) {
                        if (afterDownDimissFlag == AFTER_LAST_TIME_DIMISS) {
                            drawTextFlag = DRAW_TEXT_NO
                        }
                        invalidate()
                        isRunning = false
                        downTimerTask == null
                        timer?.cancel()
                        timer = null
                        if (downTimeWatcher != null) {
                            downTimeWatcher!!.onLastTimeFinish(downCount)
                        }
                        onFinishListener?.invoke()
                    }
                    downCount--
                }
            }
        }
    }

    private val DRAW_TEXT_YES = 1
    private val DRAW_TEXT_NO = 0
    private var drawTextFlag = DRAW_TEXT_YES
    private val AFTER_LAST_TIME_DIMISS = 1
    private val AFTER_LAST_TIME_NODIMISS = 0
    private var afterDownDimissFlag = AFTER_LAST_TIME_DIMISS
    fun setAfterDownNoDimiss() {
        afterDownDimissFlag = AFTER_LAST_TIME_NODIMISS
    }

    fun setAferDownDimiss() {
        afterDownDimissFlag = AFTER_LAST_TIME_DIMISS
    }

    var startDefaultAnimFlag = true
    fun closeDefaultAnimate() {
        animationSet?.reset()
        startDefaultAnimFlag = false
    }

    private fun startDefaultAnimate() {
        if (startDefaultAnimFlag && isAttachedToWindow) {
            animation?.start()
        }
    }

    private fun initDefaultAnimate() {
        if (animationSet == null) {
            animationSet = AnimationSet(true)
        }
        val scaleAnimation =
            ScaleAnimation(
                1f,
                0.5f,
                1f,
                0.5f,
                ScaleAnimation.ABSOLUTE,
                measuredWidth / 2f,
                ScaleAnimation.ABSOLUTE,
                measuredHeight / 2f,
            )
        scaleAnimation.duration = intervalMills
        val alphaAnimation = AlphaAnimation(1f, 0.3f)
        alphaAnimation.duration = intervalMills
        animationSet!!.addAnimation(scaleAnimation)
        animationSet!!.addAnimation(alphaAnimation)
        animationSet!!.interpolator = AccelerateInterpolator()
        animation = animationSet
    }
}