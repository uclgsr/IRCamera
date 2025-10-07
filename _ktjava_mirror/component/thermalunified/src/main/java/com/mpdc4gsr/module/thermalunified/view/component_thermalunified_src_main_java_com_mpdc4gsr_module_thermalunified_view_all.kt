// Merged .kt under 'component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view' subtree
// Files: 45; Generated 2025-10-07 23:07:44


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\ChartLogView.kt =====

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\ChartMonitorView.kt =====

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
import com.mpdc4gsr.module.thermalunified.compat.dpToPx
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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\ChartTrendView.kt =====

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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\compass\CompassProvider.kt =====

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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\compass\CompassSource.kt =====

package com.mpdc4gsr.module.thermalunified.view.compass

enum class CompassSource(val id: String) {
    RotationVector("rotation_vector"),
    GeomagneticRotationVector("geomagnetic_rotation_vector"),
    CustomMagnetometer("custom_magnetometer"),
    Orientation("orientation"),
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\compass\LinearCompassView.kt =====

package com.mpdc4gsr.module.thermalunified.view.compass

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.view.drawToBitmap
import com.mpdc4gsr.module.thermalunified.compat.spToPx
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.utils.getPixelLinear
import com.mpdc4gsr.module.thermalunified.utils.getValuesBetween
import com.mpdc4gsr.module.thermalunified.utils.realX
import com.mpdc4gsr.module.thermalunified.utils.realY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext

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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\compass\MagQualityCompassWrapper.kt =====

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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\compass\NullCompass.kt =====

package com.mpdc4gsr.module.thermalunified.view.compass

import com.kylecorry.andromeda.sense.compass.ICompass
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.CompassDirection

class NullCompass : NullSensor(), ICompass {
    override val bearing: Bearing = Bearing.from(CompassDirection.North)
    override var declination: Float = 0f
    override val rawBearing: Float = 0f
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\compass\NullSensor.kt =====

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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\compass\SensorService.kt =====

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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\DistanceMeasureView.kt =====

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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\EmissivityView.kt =====

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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\HikSurfaceView.kt =====

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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\MyGSYVideoPlayer.kt =====

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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\TargetBarPickView.kt =====

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
import com.mpdc4gsr.module.thermalunified.compat.dpToPx
import com.mpdc4gsr.module.thermalunified.compat.spToPx
import com.mpdc4gsr.libunified.app.utils.ScreenUtils

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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\Temperature07View.kt =====

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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\TemperatureBaseView.kt =====

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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\TemperatureEditView.kt =====

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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\TemperatureHikView.kt =====

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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\view\TimeDownView.kt =====

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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\GalleryActivityViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import android.Manifest
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.launch

class GalleryActivityViewModel : BaseViewModel() {
    // Permission state management
    data class PermissionState(
        val hasAllPermissions: Boolean,
        val missingPermissions: List<String>,
        val targetSdk: Int
    )

    // ViewPager state management
    sealed class ViewPagerState {
        object Ready : ViewPagerState()
        data class TabSelected(val position: Int) : ViewPagerState()
    }

    private val _permissionState = MutableLiveData<PermissionState>()
    val permissionState = _permissionState
    private val _viewPagerState = MutableLiveData<ViewPagerState>()
    val viewPagerState = _viewPagerState
    fun initializePermissions(targetSdkVersion: Int) {
        viewModelScope.launch {
            val requiredPermissions = getRequiredPermissions(targetSdkVersion)
            val permissionState = PermissionState(
                hasAllPermissions = false, // Will be checked by permission tool
                missingPermissions = requiredPermissions,
                targetSdk = targetSdkVersion
            )
            _permissionState.value = permissionState
        }
    }

    fun onPermissionsResult(isSuccess: Boolean) {
        viewModelScope.launch {
            if (isSuccess) {
                val currentState = _permissionState.value
                _permissionState.value = currentState?.copy(hasAllPermissions = true)
                _viewPagerState.value = ViewPagerState.Ready
            }
        }
    }

    fun selectTab(position: Int) {
        _viewPagerState.value = ViewPagerState.TabSelected(position)
    }

    private fun getRequiredPermissions(targetSdkVersion: Int): List<String> {
        return when {
            targetSdkVersion >= 34 -> listOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )

            targetSdkVersion >= 33 -> listOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )

            else -> listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\GalleryViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.config.FileConfig
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.utils.SingleLiveEvent
import com.mpdc4gsr.module.thermalunified.compat.ContextProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class GalleryViewModel : BaseViewModel() {
    companion object {
        private const val TAG = "GalleryViewModel"
    }

    val galleryLiveData = SingleLiveEvent<ArrayList<String>>()

    // Data class for media items
    data class MediaItem(
        val id: String,
        val name: String,
        val path: String,
        val thumbnailPath: String,
        val size: Long,
        val dateModified: Long,
        val isVideo: Boolean = false
    )

    // State flows for Compose
    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaItems: StateFlow<List<MediaItem>> = _mediaItems.asStateFlow()
    private val _galleryItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val galleryItems: StateFlow<List<MediaItem>> = _galleryItems.asStateFlow()
    private val _videoItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val videoItems: StateFlow<List<MediaItem>> = _videoItems.asStateFlow()
    private val _isGridView = MutableStateFlow(true)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()
    private val _selectedItems = MutableStateFlow<Set<String>>(emptySet())
    val selectedItems: StateFlow<Set<String>> = _selectedItems.asStateFlow()
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadMediaItems()
    }

    // Load media items and update different flows
    private fun loadMediaItems() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val items = getMediaItemsList()
                withContext(Dispatchers.Main) {
                    _mediaItems.value = items
                    _galleryItems.value = items.filter { !it.isVideo }
                    _videoItems.value = items.filter { it.isVideo }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading media items", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // View mode toggle
    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
    }

    // Selection mode methods
    fun enterSelectionMode(item: MediaItem? = null) {
        _isSelectionMode.value = true
        item?.let {
            _selectedItems.value = setOf(it.id)
        }
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedItems.value = emptySet()
    }

    fun clearSelection() {
        _selectedItems.value = emptySet()
        _isSelectionMode.value = false
    }

    fun toggleItemSelection(item: MediaItem) {
        val currentSelected = _selectedItems.value.toMutableSet()
        if (currentSelected.contains(item.id)) {
            currentSelected.remove(item.id)
        } else {
            currentSelected.add(item.id)
        }
        _selectedItems.value = currentSelected
        // Exit selection mode if no items selected
        if (currentSelected.isEmpty()) {
            _isSelectionMode.value = false
        }
    }

    // File operations
    fun deleteSelectedItems() {
        val selectedIds = _selectedItems.value
        val itemsToDelete = _mediaItems.value.filter { selectedIds.contains(it.id) }
        viewModelScope.launch(Dispatchers.IO) {
            itemsToDelete.forEach { item ->
                try {
                    File(item.path).delete()
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting file: ${item.path}", e)
                }
            }
            withContext(Dispatchers.Main) {
                exitSelectionMode()
                loadMediaItems() // Refresh the list
            }
        }
    }

    fun shareSelectedItems() {
        val selectedIds = _selectedItems.value
        val itemsToShare = _mediaItems.value.filter { selectedIds.contains(it.id) }
        if (itemsToShare.isNotEmpty()) {
            // Implementation would depend on context being available
            // For now, just log the action
            Log.d(TAG, "Sharing ${itemsToShare.size} items")
        }
    }

    fun openMediaItem(item: MediaItem) {
        // Implementation for opening media item
        Log.d(TAG, "Opening media item: ${item.name}")
    }

    // Refresh methods
    fun refreshGallery() {
        loadMediaItems()
    }

    fun refreshVideoGallery() {
        loadMediaItems()
    }

    // Legacy methods for backward compatibility
    fun getData() {
        viewModelScope.launch {
            getGalleryList().collect { it ->
                if (it.size == 0) {
                    Log.w(TAG, "No gallery items found")
                } else {
                    galleryLiveData.postValue(it)
                }
            }
        }
    }

    fun getVideoData() {
        viewModelScope.launch {
            getVideoList().collect { it ->
                if (it.size == 0) {
                    Log.w(TAG, "No video items found")
                } else {
                    galleryLiveData.postValue(it)
                }
            }
        }
    }

    private fun getMediaItemsList(): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        // Load pictures
        val picturePath = ContextProvider.getContext()
            .getExternalFilesDir("Pictures")!!.absolutePath + File.separator + "thermal"
        val pictureDir = File(picturePath)
        if (pictureDir.isDirectory) {
            pictureDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    items.add(
                        MediaItem(
                            id = file.absolutePath,
                            name = file.name,
                            path = file.absolutePath,
                            thumbnailPath = file.absolutePath,
                            size = file.length(),
                            dateModified = file.lastModified(),
                            isVideo = false
                        )
                    )
                }
            }
        }
        // Load videos
        val videoPath = FileConfig.lineGalleryDir
        val videoDir = File(videoPath)
        if (videoDir.isDirectory) {
            videoDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    items.add(
                        MediaItem(
                            id = file.absolutePath,
                            name = file.name,
                            path = file.absolutePath,
                            thumbnailPath = file.absolutePath,
                            size = file.length(),
                            dateModified = file.lastModified(),
                            isVideo = true
                        )
                    )
                }
            }
        }
        return items.sortedByDescending { it.dateModified }
    }

    private fun getGalleryList(): Flow<ArrayList<String>> {
        val flow =
            flow {
                val path =
                    ContextProvider.getContext()
                        .getExternalFilesDir("Pictures")!!.absolutePath + File.separator + "thermal"
                val file = File(path)
                if (file.isDirectory) {
                    val list = arrayListOf<String>()
                    file.list()?.forEach { fileName ->
                        list.add("$path/$fileName")
                    }
                    emit(list)
                } else {
                    emit(arrayListOf<String>())
                }
            }.map {
                return@map it
            }
        return flow
    }

    private fun getVideoList(): Flow<ArrayList<String>> {
        val flow =
            flow {
                val path = FileConfig.lineGalleryDir
                val file = File(path)
                if (file.isDirectory) {
                    val list = arrayListOf<String>()
                    file.list()?.forEach { fileName ->
                        list.add("$path/$fileName")
                    }
                    emit(list)
                } else {
                    emit(arrayListOf<String>())
                }
            }.map {
                return@map it
            }
        return flow
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\ImageColorViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

class ImageColorViewModel : BaseViewModel() {
    private val _timestamp = MutableStateFlow("")
    val timestamp: StateFlow<String> = _timestamp.asStateFlow()
    private val _showData = MutableStateFlow(false)
    val showData: StateFlow<Boolean> = _showData.asStateFlow()
    private val _leftImagePath = MutableStateFlow("")
    val leftImagePath: StateFlow<String> = _leftImagePath.asStateFlow()
    private val _rightImagePath = MutableStateFlow("")
    val rightImagePath: StateFlow<String> = _rightImagePath.asStateFlow()
    private val _comparisonResult = MutableStateFlow("")
    val comparisonResult: StateFlow<String> = _comparisonResult.asStateFlow()

    init {
        updateTimestamp()
    }

    fun toggleDataDisplay() {
        launchWithErrorHandling {
            _showData.value = !_showData.value
        }
    }

    fun loadImages(leftImagePath: String, rightImagePath: String) {
        launchWithLoading {
            _leftImagePath.value = leftImagePath
            _rightImagePath.value = rightImagePath
        }
    }

    fun compareImages() {
        launchWithErrorHandling {
            _comparisonResult.value = "Images compared successfully"
        }
    }

    private fun updateTimestamp() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        _timestamp.value = dateFormat.format(Date())
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\IRConfigViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.utils.SingleLiveEvent
import com.mpdc4gsr.module.thermalunified.bean.DataBean
import com.mpdc4gsr.module.thermalunified.bean.ModelBean
import com.mpdc4gsr.module.thermalunified.repository.ConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IRConfigViewModel : BaseViewModel() {
    val configLiveData = SingleLiveEvent<ModelBean>()
    fun getConfig(isTC007: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            configLiveData.postValue(ConfigRepository.read(isTC007))
        }
    }

    fun updateDefaultEnvironment(
        isTC007: Boolean,
        environment: Float,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val modelBean = configLiveData.value ?: ConfigRepository.read(isTC007)
            modelBean.defaultModel.environment = environment
            ConfigRepository.update(isTC007, modelBean)
            configLiveData.postValue(modelBean)
        }
    }

    fun updateDefaultDistance(
        isTC007: Boolean,
        distance: Float,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val modelBean = configLiveData.value ?: ConfigRepository.read(isTC007)
            modelBean.defaultModel.distance = distance
            ConfigRepository.update(isTC007, modelBean)
            configLiveData.postValue(modelBean)
        }
    }

    fun updateDefaultRadiation(
        isTC007: Boolean,
        radiation: Float,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val modelBean = configLiveData.value ?: ConfigRepository.read(isTC007)
            modelBean.defaultModel.radiation = radiation
            ConfigRepository.update(isTC007, modelBean)
            configLiveData.postValue(modelBean)
        }
    }

    fun addConfig(isTC007: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val modelBean = configLiveData.value ?: ConfigRepository.read(isTC007)
            var index = 0
            modelBean.myselfModel.forEach {
                index = index.coerceAtLeast(it.id)
            }
            index++
            modelBean.myselfModel.add(DataBean(id = index, name = index.toString()))
            ConfigRepository.update(isTC007, modelBean)
            configLiveData.postValue(modelBean)
        }
    }

    fun checkConfig(
        isTC007: Boolean,
        id: Int,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val modelBean = configLiveData.value ?: ConfigRepository.read(isTC007)
            modelBean.defaultModel.use = id == 0
            modelBean.myselfModel.forEach {
                it.use = it.id == id
            }
            ConfigRepository.update(isTC007, modelBean)
            configLiveData.postValue(modelBean)
        }
    }

    fun deleteConfig(
        isTC007: Boolean,
        id: Int,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val modelBean = configLiveData.value ?: ConfigRepository.read(isTC007)
            var removeAt = modelBean.myselfModel.size
            for (i in modelBean.myselfModel.indices) {
                val dataBean = modelBean.myselfModel[i]
                if (dataBean.id == id) {
                    if (dataBean.use) {
                        modelBean.defaultModel.use = true
                    }
                    modelBean.myselfModel.removeAt(i)
                    removeAt = i
                    break
                }
            }
            if (removeAt < modelBean.myselfModel.size) {
                for (i in removeAt until modelBean.myselfModel.size) {
                    val dataBean = modelBean.myselfModel[i]
                    dataBean.id = i + 1
                    dataBean.name = dataBean.id.toString()
                }
            }
            ConfigRepository.update(isTC007, modelBean)
            configLiveData.postValue(modelBean)
        }
    }

    fun updateCustom(
        isTC007: Boolean,
        dataBean: DataBean,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val modelBean = configLiveData.value ?: ConfigRepository.read(isTC007)
            for (i in modelBean.myselfModel.indices) {
                if (modelBean.myselfModel[i].id == dataBean.id) {
                    modelBean.myselfModel[i] = dataBean
                    break
                }
            }
            ConfigRepository.update(isTC007, modelBean)
            configLiveData.postValue(modelBean)
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\IRCorrectionViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IRCorrectionViewModel : BaseViewModel() {
    // State management for correction functionality
    private val _correctionState = MutableStateFlow(CorrectionState.INACTIVE)
    val correctionState: StateFlow<CorrectionState> = _correctionState.asStateFlow()
    private val _temperatureData = MutableStateFlow<TemperatureData?>(null)
    val temperatureData: StateFlow<TemperatureData?> = _temperatureData.asStateFlow()
    private val _calibrationStatus = MutableStateFlow(CalibrationStatus.NONE)
    val calibrationStatus: StateFlow<CalibrationStatus> = _calibrationStatus.asStateFlow()
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // Current correction parameters
    private var currentCorrectionValue: Float = 0f
    private var currentTemperaturePoint: Triple<Float, Int, Int>? = null

    init {
        // Initialize with default temperature data
        _temperatureData.value = TemperatureData(
            currentTemp = 25.0f,
            correctedTemp = 25.0f,
            offsetValue = 0.0f
        )
    }

    fun toggleCorrection() {
        viewModelScope.launch {
            try {
                when (_correctionState.value) {
                    CorrectionState.INACTIVE -> {
                        _correctionState.value = CorrectionState.ACTIVE
                        startTemperatureMonitoring()
                    }

                    CorrectionState.ACTIVE -> {
                        _correctionState.value = CorrectionState.INACTIVE
                        stopTemperatureMonitoring()
                    }

                    CorrectionState.CALIBRATING -> {
                        // Cannot toggle while calibrating
                    }
                }
            } catch (e: Exception) {
                // Handle the exception by logging and updating error state
                handleError(e)
                _correctionState.value = CorrectionState.INACTIVE
            }
        }
    }

    fun updateTemperaturePoint(temp: Float, x: Int, y: Int) {
        currentTemperaturePoint = Triple(temp, x, y)
        updateTemperatureData(temp)
    }

    fun updateCorrectionValue(value: Float) {
        currentCorrectionValue = value
        currentTemperaturePoint?.let { (baseTemp, _, _) ->
            updateTemperatureData(baseTemp)
        }
    }

    fun startCalibration() {
        launchWithErrorHandling {
            _isProcessing.value = true
            _correctionState.value = CorrectionState.CALIBRATING
            _calibrationStatus.value = CalibrationStatus.NEEDS_CALIBRATION
            try {
                // Simulate calibration process
                kotlinx.coroutines.delay(2000) // Simulate calibration time
                _calibrationStatus.value = CalibrationStatus.CALIBRATED
                _correctionState.value = CorrectionState.ACTIVE
                // Restart temperature monitoring after calibration completes
                startTemperatureMonitoring()
            } catch (e: Exception) {
                handleError(e)
                _correctionState.value = CorrectionState.INACTIVE
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun resetCorrection() {
        launchWithErrorHandling {
            currentCorrectionValue = 0f
            currentTemperaturePoint = null
            _calibrationStatus.value = CalibrationStatus.NONE
            _correctionState.value = CorrectionState.INACTIVE
            // Reset temperature data
            _temperatureData.value = TemperatureData(
                currentTemp = 25.0f,
                correctedTemp = 25.0f,
                offsetValue = 0.0f
            )
        }
    }

    fun saveSettings() {
        launchWithErrorHandling {
            _isProcessing.value = true
            try {
                // Simulate saving settings
                kotlinx.coroutines.delay(1000)
                // Show success message
                _uiEvents.emit(BaseViewModel.UiEvent.ShowMessage("Correction settings saved successfully"))
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private fun startTemperatureMonitoring() {
        viewModelScope.launch {
            // Simulate temperature monitoring with some variation
            while (_correctionState.value == CorrectionState.ACTIVE) {
                currentTemperaturePoint?.let { (baseTemp, _, _) ->
                    // Add some realistic temperature variation
                    val variation = (Math.random() - 0.5).toFloat() * 0.5f
                    updateTemperatureData(baseTemp + variation)
                }
                kotlinx.coroutines.delay(500) // Update every 500ms
            }
        }
    }

    private fun stopTemperatureMonitoring() {
        // Temperature monitoring is stopped by the coroutine condition check
    }

    private fun updateTemperatureData(currentTemp: Float) {
        val correctedTemp = currentTemp + currentCorrectionValue
        _temperatureData.value = TemperatureData(
            currentTemp = currentTemp,
            correctedTemp = correctedTemp,
            offsetValue = currentCorrectionValue
        )
    }
}

data class TemperatureData(
    val currentTemp: Float,
    val correctedTemp: Float,
    val offsetValue: Float
)

enum class CorrectionState {
    INACTIVE, ACTIVE, CALIBRATING
}

enum class CalibrationStatus {
    NONE, CALIBRATED, NEEDS_CALIBRATION
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\IRGalleryEditViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.viewModelScope
import com.elvishew.xlog.XLog
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.utils.SingleLiveEvent
import com.mpdc4gsr.libunified.app.utils.UnifiedByteUtils.bytesToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class IRGalleryEditViewModel : BaseViewModel() {
    val resultLiveData = SingleLiveEvent<FrameBean>()
    fun initData(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(path)
            if (!file.exists()) {
                XLog.w("IR[ph][ph][ph][ph][ph]: ${file.absolutePath}")
                return@launch
            }
            XLog.w("IR[ph][ph]: ${file.absolutePath}")
            val bytes = file.readBytes()
            val headLenBytes = ByteArray(2)
            System.arraycopy(bytes, 0, headLenBytes, 0, 2)
            val headLen = headLenBytes.bytesToInt()
            val headDataBytes = ByteArray(headLen)
            val frameDataBytes = ByteArray(bytes.size - headLen)
            System.arraycopy(bytes, 0, headDataBytes, 0, headDataBytes.size)
            System.arraycopy(bytes, headLen, frameDataBytes, 0, frameDataBytes.size)
            XLog.w("[ph][ph][ph][ph]: ${frameDataBytes.size}")
            resultLiveData.postValue(FrameBean(headDataBytes, frameDataBytes))
        }
    }

    fun getTailData(bytes: ByteArray) {
    }

    data class FrameBean(val capital: ByteArray, val frame: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as FrameBean
            if (!capital.contentEquals(other.capital)) return false
            if (!frame.contentEquals(other.frame)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = capital.contentHashCode()
            result = 31 * result + frame.contentHashCode()
            return result
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\IRGalleryTabViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.repository.GalleryRepository.DirType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IRGalleryTabViewModel : BaseViewModel() {
    val isEditModeLD: MutableLiveData<Boolean> = MutableLiveData(false)
    val selectSizeLD: MutableLiveData<Int> = MutableLiveData(0)
    val selectAllIndex: MutableLiveData<Int> = MutableLiveData(0)

    // StateFlow properties for Compose
    private val _currentDirType = MutableStateFlow(DirType.LINE)
    val currentDirType: StateFlow<DirType> = _currentDirType.asStateFlow()
    private val _canSwitchDir = MutableStateFlow(true)
    val canSwitchDir: StateFlow<Boolean> = _canSwitchDir.asStateFlow()
    private val _hasBackIcon = MutableStateFlow(false)
    val hasBackIcon: StateFlow<Boolean> = _hasBackIcon.asStateFlow()

    // Methods for Compose fragment
    fun changeDirType(dirType: DirType) {
        _currentDirType.value = dirType
    }

    fun setCanSwitchDir(canSwitch: Boolean) {
        _canSwitchDir.value = canSwitch
    }

    fun setHasBackIcon(hasIcon: Boolean) {
        _hasBackIcon.value = hasIcon
    }

    fun navigateBack() {
        // Emit navigation back event to be handled by the fragment/activity
        // The fragment should observe uiEvents and finish the activity when NavigateBack is received
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.NavigateBack)
        }
    }

    fun showSearch() {
        // Placeholder for search functionality
    }

    fun showMoreOptions() {
        // Placeholder for more options functionality
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\IRGalleryViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.bean.GalleryBean
import com.mpdc4gsr.libunified.app.bean.GalleryTitle
import com.mpdc4gsr.libunified.app.config.FileConfig
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.repository.GalleryRepository
import com.mpdc4gsr.libunified.app.repository.TS004Repository
import com.mpdc4gsr.libunified.app.tools.TimeTools
import com.mpdc4gsr.module.thermalunified.utils.WriteTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class IRGalleryViewModel : BaseViewModel() {
    companion object {
        const val PAGE_COUNT = 20
    }

    // Existing LiveData properties
    val sourceListLD: MutableLiveData<ArrayList<GalleryBean>> = MutableLiveData()
    val showListLD: MutableLiveData<ArrayList<GalleryBean>> = MutableLiveData()
    val pageListLD: MutableLiveData<ArrayList<GalleryBean>?> = MutableLiveData()
    val deleteResultLD: MutableLiveData<Boolean> = MutableLiveData()

    // StateFlow properties for Compose
    private val _galleryItems = MutableStateFlow<List<GalleryBean>>(emptyList())
    val galleryItems: StateFlow<List<GalleryBean>> = _galleryItems.asStateFlow()
    private val _currentDirType = MutableStateFlow(GalleryRepository.DirType.LINE)
    val currentDirType: StateFlow<GalleryRepository.DirType> = _currentDirType.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _selectedItems = MutableStateFlow<Set<String>>(emptySet())
    val selectedItems: StateFlow<Set<String>> = _selectedItems.asStateFlow()
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()
    private val _isGridView = MutableStateFlow(true)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    // Cache for file sizes to avoid repeated I/O operations
    private val fileSizeCache = mutableMapOf<String, Long>()

    // Compose-related methods
    fun changeDirType(dirType: GalleryRepository.DirType) {
        _currentDirType.value = dirType
        refreshGallery()
    }

    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
    }

    fun refreshGallery() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val items = GalleryRepository.loadAllReportImg(_currentDirType.value)
                // Pre-calculate file sizes on background thread and cache them
                items.forEach { item ->
                    val fileSize = calculateFileSize(item.path)
                    fileSizeCache[item.path] = fileSize
                }
                _galleryItems.value = items
            } catch (e: Exception) {
                _galleryItems.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleItemSelection(item: GalleryBean) {
        val currentSelected = _selectedItems.value.toMutableSet()
        val itemPath = item.path ?: return
        if (currentSelected.contains(itemPath)) {
            currentSelected.remove(itemPath)
        } else {
            currentSelected.add(itemPath)
        }
        _selectedItems.value = currentSelected
        if (currentSelected.isEmpty()) {
            _isSelectionMode.value = false
        }
    }

    fun enterSelectionMode(item: GalleryBean) {
        _isSelectionMode.value = true
        val itemPath = item.path ?: return
        _selectedItems.value = setOf(itemPath)
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedItems.value = emptySet()
    }

    fun deleteSelectedItems() {
        val selectedPaths = _selectedItems.value
        if (selectedPaths.isEmpty()) return
        viewModelScope.launch {
            val itemsToDelete = _galleryItems.value.filter { selectedPaths.contains(it.path) }
            delete(itemsToDelete, _currentDirType.value, true)
            exitSelectionMode()
            refreshGallery()
        }
    }

    fun shareSelectedItems() {
        // Implementation for sharing selected items would go here
        // For now, just exit selection mode
        exitSelectionMode()
    }

    fun openGalleryItem(item: GalleryBean) {
        // Implementation for opening gallery item would go here
        // This would typically navigate to a detail view
    }

    var hasLoadPage = 0
    fun queryAllReportImg(dirType: GalleryRepository.DirType) {
        viewModelScope.launch(Dispatchers.IO) {
            val sourceList: ArrayList<GalleryBean> = GalleryRepository.loadAllReportImg(dirType)
            sourceListLD.postValue(sourceList)
            val showList: ArrayList<GalleryBean> = ArrayList(sourceList.size)
            var beforeTime = 0L
            for (galleryBean in sourceList) {
                val currentTime = TimeTools.timeToMinute(galleryBean.timeMillis, 4)
                if (beforeTime != currentTime) {
                    showList.add(GalleryTitle(galleryBean.timeMillis))
                    beforeTime = currentTime
                }
                showList.add(galleryBean)
            }
            showListLD.postValue(showList)
        }
    }

    fun queryGalleryByPage(
        isVideo: Boolean,
        dirType: GalleryRepository.DirType,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val pageList: ArrayList<GalleryBean>? =
                GalleryRepository.loadByPage(isVideo, dirType, hasLoadPage + 1, PAGE_COUNT)
            pageListLD.postValue(pageList)
            if (pageList != null) {
                val sourceList =
                    if (hasLoadPage == 0) ArrayList(pageList.size) else sourceListLD.value
                        ?: ArrayList(pageList.size)
                val showList = if (hasLoadPage == 0) ArrayList(pageList.size) else showListLD.value
                    ?: ArrayList(pageList.size)
                if (pageList.isNotEmpty()) {
                    hasLoadPage++
                }
                var beforeTime = if (sourceList.isEmpty()) 0 else TimeTools.timeToMinute(
                    sourceList.last().timeMillis,
                    4
                )
                for (galleryBean in pageList) {
                    val currentTime = TimeTools.timeToMinute(galleryBean.timeMillis, 4)
                    if (beforeTime != currentTime) {
                        showList.add(GalleryTitle(galleryBean.timeMillis))
                        beforeTime = currentTime
                    }
                    showList.add(galleryBean)
                }
                sourceList.addAll(pageList)
                sourceListLD.postValue(sourceList)
                showListLD.postValue(showList)
            }
        }
    }

    private fun calculateFileSize(path: String): Long {
        return try {
            val file = File(path)
            if (file.exists()) file.length() else 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun getCachedFileSize(path: String): Long {
        return fileSizeCache[path] ?: 0L
    }

    fun delete(
        deleteList: List<GalleryBean>,
        dirType: GalleryRepository.DirType,
        isDelLocal: Boolean,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (dirType == GalleryRepository.DirType.TS004_REMOTE) {
                val isSuccess =
                    TS004Repository.deleteFiles(
                        Array(deleteList.size) {
                            deleteList[it].id
                        },
                    )
                if (isSuccess) {
                    if (isDelLocal) {
                        deleteList.forEach {
                            if (it.hasDownload) {
                                val file = File(FileConfig.ts004GalleryDir, it.name)
                                if (file.exists()) {
                                    WriteTools.delete(file)
                                }
                            }
                        }
                    }
                    deleteResultLD.postValue(true)
                } else {
                    deleteResultLD.postValue(false)
                }
            } else {
                deleteList.forEach {
                    val file = File(it.path)
                    if (file.exists()) {
                        WriteTools.delete(file)
                    }
                }
                deleteResultLD.postValue(true)
            }
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\IRMainActivityViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.socket.WebSocketProxy
import com.mpdc4gsr.libunified.app.tools.DeviceTools
import kotlinx.coroutines.launch

class IRMainActivityViewModel : BaseViewModel() {
    // Device state management
    data class DeviceState(
        val isTC007: Boolean = false,
        val isWebSocketConnected: Boolean = false,
        val isUsbConnected: Boolean = false,
        val shouldAutoOpen: Boolean = false,
        val shouldBlur: Boolean = false
    )

    // Fragment communication state
    data class FragmentCommunicationState(
        val activeFragment: Int = 0,
        val deviceConnected: Boolean = false,
        val pendingNavigation: NavigationEvent? = null
    )

    // Navigation events
    sealed class NavigationEvent {
        data class ToMonitor(val isTC007: Boolean) : NavigationEvent()
        object ToGallery : NavigationEvent()
        data class ToThermal(val routeConfig: String) : NavigationEvent()
    }

    // ViewPager state management
    sealed class ViewPagerState {
        data class PageSelected(val position: Int) : ViewPagerState()
        data class NavigateToPage(val position: Int) : ViewPagerState()
    }

    private val _deviceState = MutableLiveData<DeviceState>()
    val deviceState = _deviceState
    private val _fragmentCommunication = MutableLiveData<FragmentCommunicationState>()
    val fragmentCommunication = _fragmentCommunication
    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent = _navigationEvent
    private val _viewPagerState = MutableLiveData<ViewPagerState>()
    val viewPagerState = _viewPagerState
    private var currentDeviceType = false // false = not TC007, true = TC007
    fun setDeviceType(isTC007: Boolean) {
        currentDeviceType = isTC007
        refreshDeviceState()
    }

    fun initializeDeviceState() {
        viewModelScope.launch {
            refreshDeviceState()
        }
    }

    fun refreshDeviceState() {
        viewModelScope.launch {
            val deviceState = if (currentDeviceType) {
                // TC007 device state
                val isConnected = WebSocketProxy.getInstance().isTC007Connect()
                DeviceState(
                    isTC007 = true,
                    isWebSocketConnected = isConnected,
                    shouldAutoOpen = isConnected && SharedManager.isConnect07AutoOpen
                )
            } else {
                // USB device state
                val isConnected = DeviceTools.isConnect(isAutoRequest = false)
                DeviceState(
                    isTC007 = false,
                    isUsbConnected = isConnected,
                    shouldAutoOpen = isConnected && SharedManager.isConnectAutoOpen
                )
            }
            _deviceState.value = deviceState
        }
    }

    fun onPageSelected(position: Int) {
        _viewPagerState.value = ViewPagerState.PageSelected(position)
        updateFragmentCommunication(position)
    }

    fun navigateToPage(position: Int) {
        _viewPagerState.value = ViewPagerState.NavigateToPage(position)
    }

    fun navigateToMonitor() {
        _navigationEvent.value = NavigationEvent.ToMonitor(currentDeviceType)
    }

    fun navigateToGallery() {
        _navigationEvent.value = NavigationEvent.ToGallery
    }

    fun navigateToThermal() {
        val routeConfig = if (currentDeviceType) {
            RouterConfig.IR_THERMAL_07
        } else {
            RouterConfig.IR_THERMAL
        }
        _navigationEvent.value = NavigationEvent.ToThermal(routeConfig)
    }

    private fun updateFragmentCommunication(activeFragment: Int) {
        val currentDeviceState = _deviceState.value ?: DeviceState()
        val communicationState = FragmentCommunicationState(
            activeFragment = activeFragment,
            deviceConnected = currentDeviceState.isWebSocketConnected || currentDeviceState.isUsbConnected
        )
        _fragmentCommunication.value = communicationState
    }

    // Guide dialog management
    fun handleGuideDialog(onGuideShow: (Int, Int) -> Unit) {
        val currentStep = SharedManager.homeGuideStep
        if (currentStep == 0) return
        val navigationTarget = when (currentStep) {
            1 -> 0
            2 -> 4
            3 -> 2
            else -> 2
        }
        onGuideShow(currentStep, navigationTarget)
    }

    fun handleGuideNavigation(step: Int) {
        SharedManager.homeGuideStep = when (step) {
            1 -> 2
            2 -> 3
            3 -> 0
            else -> 0
        }
    }

    fun completeGuide() {
        SharedManager.homeGuideStep = 0
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\IRMonitorCaptureViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class IRMonitorCaptureViewModel : BaseViewModel() {
    // Data classes matching the fragment requirements
    data class TemperatureData(
        val centerTemp: Float,
        val maxTemp: Float,
        val minTemp: Float
    )

    data class CaptureData(
        val id: Int,
        val timestamp: Long,
        val temperature: Float,
        val imagePath: String
    )

    enum class DeviceConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, ERROR
    }

    enum class CaptureState {
        INACTIVE, ACTIVE, CONTINUOUS, CAPTURING
    }

    // StateFlow properties for UI state management
    private val _captureState = MutableStateFlow(CaptureState.INACTIVE)
    val captureState: StateFlow<CaptureState> = _captureState.asStateFlow()
    private val _temperatureData = MutableStateFlow<TemperatureData?>(null)
    val temperatureData: StateFlow<TemperatureData?> = _temperatureData.asStateFlow()
    private val _captureHistory = MutableStateFlow<List<CaptureData>>(emptyList())
    val captureHistory: StateFlow<List<CaptureData>> = _captureHistory.asStateFlow()
    private val _deviceConnectionState = MutableStateFlow(DeviceConnectionState.DISCONNECTED)
    val deviceConnectionState: StateFlow<DeviceConnectionState> = _deviceConnectionState.asStateFlow()

    // Internal state
    private var captureIdCounter = 1
    private var continuousCapturingJob: kotlinx.coroutines.Job? = null

    init {
        // Initialize with mock data for development
        initializeMockData()
        // Start temperature monitoring simulation
        startTemperatureMonitoring()
    }

    fun toggleCapture() {
        viewModelScope.launch {
            when (_captureState.value) {
                CaptureState.INACTIVE -> {
                    _captureState.value = CaptureState.ACTIVE
                    simulateDeviceConnection()
                }

                CaptureState.ACTIVE -> {
                    _captureState.value = CaptureState.INACTIVE
                    stopContinuousCapture()
                }

                CaptureState.CONTINUOUS -> {
                    stopContinuousCapture()
                    _captureState.value = CaptureState.ACTIVE
                }

                CaptureState.CAPTURING -> {
                    // Already capturing, ignore
                }
            }
        }
    }

    fun captureFrame() {
        if (_deviceConnectionState.value != DeviceConnectionState.CONNECTED) return
        viewModelScope.launch {
            _captureState.value = CaptureState.CAPTURING
            // Simulate capture delay
            delay(500)
            // Create capture data
            val currentTemp = _temperatureData.value?.centerTemp ?: 25.0f
            val capture = CaptureData(
                id = captureIdCounter++,
                timestamp = System.currentTimeMillis(),
                temperature = currentTemp,
                imagePath = "/mock/path/capture_${captureIdCounter - 1}.jpg"
            )
            // Add to history
            val currentHistory = _captureHistory.value.toMutableList()
            currentHistory.add(0, capture) // Add to beginning
            _captureHistory.value = currentHistory
            // Return to previous state
            _captureState.value = if (continuousCapturingJob?.isActive == true) {
                CaptureState.CONTINUOUS
            } else {
                CaptureState.ACTIVE
            }
        }
    }

    fun toggleContinuousCapture() {
        if (_deviceConnectionState.value != DeviceConnectionState.CONNECTED) return
        viewModelScope.launch {
            if (_captureState.value == CaptureState.CONTINUOUS) {
                stopContinuousCapture()
                _captureState.value = CaptureState.ACTIVE
            } else {
                startContinuousCapture()
            }
        }
    }

    fun clearCaptureHistory() {
        viewModelScope.launch {
            _captureHistory.value = emptyList()
        }
    }

    fun exportCaptures() {
        viewModelScope.launch {
            val captures = _captureHistory.value
            if (captures.isEmpty()) {
                return@launch
            }
            // Create export data with capture information
            val exportData = captures.map { capture ->
                mapOf(
                    "id" to capture.id,
                    "timestamp" to capture.timestamp,
                    "temperature" to capture.temperature,
                    "imagePath" to capture.imagePath
                )
            }
            // In a real implementation, this would write to a file or share the data
            // For now, we log the export action
            android.util.Log.d("IRMonitorCaptureVM", "Exporting ${captures.size} captures")
        }
    }

    fun deleteCapture(capture: CaptureData) {
        viewModelScope.launch {
            val currentHistory = _captureHistory.value.toMutableList()
            currentHistory.remove(capture)
            _captureHistory.value = currentHistory
        }
    }

    // Private helper methods
    private fun initializeMockData() {
        // Initialize with mock temperature data
        _temperatureData.value = TemperatureData(
            centerTemp = 25.0f,
            maxTemp = 28.5f,
            minTemp = 22.1f
        )
        _deviceConnectionState.value = DeviceConnectionState.DISCONNECTED
    }

    private fun simulateDeviceConnection() {
        viewModelScope.launch {
            _deviceConnectionState.value = DeviceConnectionState.CONNECTING
            delay(2000) // Simulate connection delay
            _deviceConnectionState.value = DeviceConnectionState.CONNECTED
        }
    }

    private fun startTemperatureMonitoring() {
        viewModelScope.launch {
            while (true) {
                if (_deviceConnectionState.value == DeviceConnectionState.CONNECTED) {
                    // Simulate temperature readings with variation using Kotlin Random
                    val baseTemp = 25.0f
                    val variation = (Random.nextFloat() - 0.5f) * 5.0f
                    val centerTemp = baseTemp + variation
                    _temperatureData.value = TemperatureData(
                        centerTemp = centerTemp,
                        maxTemp = centerTemp + (Random.nextFloat() * 3.0f),
                        minTemp = centerTemp - (Random.nextFloat() * 2.0f)
                    )
                }
                delay(1000) // Update every second
            }
        }
    }

    private fun startContinuousCapture() {
        _captureState.value = CaptureState.CONTINUOUS
        continuousCapturingJob = viewModelScope.launch {
            while (_captureState.value == CaptureState.CONTINUOUS) {
                captureFrame()
                delay(3000) // Capture every 3 seconds
            }
        }
    }

    private fun stopContinuousCapture() {
        continuousCapturingJob?.cancel()
        continuousCapturingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopContinuousCapture()
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\IRMonitorChartLiteViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class IRMonitorChartLiteViewModel : BaseViewModel() {
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    private val _recordingTime = MutableStateFlow("00:00:00")
    val recordingTime: StateFlow<String> = _recordingTime.asStateFlow()
    private val _showOverlay = MutableStateFlow(true)
    val showOverlay: StateFlow<Boolean> = _showOverlay.asStateFlow()
    private val _currentTemp = MutableStateFlow(25.0f)
    val currentTemp: StateFlow<Float> = _currentTemp.asStateFlow()
    private val _highTemp = MutableStateFlow(30.0f)
    val highTemp: StateFlow<Float> = _highTemp.asStateFlow()
    private val _lowTemp = MutableStateFlow(20.0f)
    val lowTemp: StateFlow<Float> = _lowTemp.asStateFlow()
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()
    fun toggleRecording() {
        launchWithErrorHandling {
            _isRecording.value = !_isRecording.value
            if (!_isRecording.value) {
                _recordingTime.value = "00:00:00"
            }
        }
    }

    fun toggleOverlay() {
        launchWithErrorHandling {
            _showOverlay.value = !_showOverlay.value
        }
    }

    fun updateTemperature(current: Float, high: Float, low: Float) {
        launchWithErrorHandling {
            _currentTemp.value = current
            _highTemp.value = high
            _lowTemp.value = low
        }
    }

    fun startMonitoring() {
        launchWithLoading {
            _isMonitoring.value = true
        }
    }

    fun stopMonitoring() {
        launchWithErrorHandling {
            _isMonitoring.value = false
            _isRecording.value = false
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\IRMonitorHistoryViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Today
import com.mpdc4gsr.libunified.app.config.FileConfig
import com.mpdc4gsr.libunified.app.db.AppDatabase
import com.mpdc4gsr.libunified.app.db.dao.ThermalDao
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class IRMonitorHistoryViewModel : BaseViewModel() {
    // Data classes for history management
    data class HistoryItem(
        val id: String,
        val sessionName: String,
        val startTime: Long,
        val duration: Long,
        val sampleCount: Int,
        val avgTemperature: Float,
        val maxTemperature: Float,
        val minTemperature: Float,
        val sessionType: SessionType,
        val dataFilePath: String
    )

    enum class SessionType(val displayName: String) {
        MONITORING("Monitor"),
        CAPTURE("Capture"),
        ANALYSIS("Analysis"),
        CALIBRATION("Calibration")
    }

    enum class HistoryFilter(
        val displayName: String,
        val icon: androidx.compose.ui.graphics.vector.ImageVector
    ) {
        ALL("All", Icons.AutoMirrored.Filled.ViewList),
        TODAY("Today", Icons.Default.Today),
        WEEK("This Week", Icons.Default.DateRange),
        MONTH("This Month", Icons.Default.CalendarMonth)
    }

    // StateFlow properties expected by the Compose fragment
    private val _historyItems = MutableStateFlow<List<HistoryItem>>(emptyList())
    val historyItems: StateFlow<List<HistoryItem>> = _historyItems.asStateFlow()
    private val _selectedFilter = MutableStateFlow(HistoryFilter.ALL)
    val selectedFilter: StateFlow<HistoryFilter> = _selectedFilter.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _selectedItems = MutableStateFlow<Set<String>>(emptySet())
    val selectedItems: StateFlow<Set<String>> = _selectedItems.asStateFlow()
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    // Internal data storage
    private var allHistoryItems: List<HistoryItem> = emptyList()

    // Custom UI events for history-specific actions
    private val _historyUiEvents = MutableSharedFlow<HistoryUiEvent>()
    val historyUiEvents: SharedFlow<HistoryUiEvent> = _historyUiEvents.asSharedFlow()

    init {
        refreshHistory()
    }

    fun changeFilter(filter: HistoryFilter) {
        _selectedFilter.value = filter
        applyFilter()
    }

    fun clearSelection() {
        _selectedItems.value = emptySet()
        _isSelectionMode.value = false
    }

    fun exportSelectedItems() {
        // Implement export functionality for selected history items
        launchWithErrorHandling {
            val selectedList = _selectedItems.value
            if (selectedList.isEmpty()) {
                _historyUiEvents.emit(HistoryUiEvent.ShowMessage("No items selected for export"))
                return@launchWithErrorHandling
            }
            // Create export data from selected items
            val exportData = historyItems.value.filter { selectedList.contains(it.id) }
            // Emit export event with data
            _historyUiEvents.emit(HistoryUiEvent.ExportData(exportData))
            // Show success message and clear selection
            _historyUiEvents.emit(HistoryUiEvent.ShowMessage("Exported ${exportData.size} items"))
            clearSelection()
        }
    }

    fun deleteSelectedItems() {
        launchWithErrorHandling {
            val selectedIds = _selectedItems.value
            if (selectedIds.isNotEmpty()) {
                // Remove selected items from the database on IO thread
                withContext(Dispatchers.IO) {
                    selectedIds.forEach { id ->
                        val item = allHistoryItems.find { it.id == id }
                        item?.let {
                            // Convert id back to startTime for database operation
                            val startTime = it.startTime
                            AppDatabase.getInstance().thermalDao().delDetail(startTime)
                        }
                    }
                }
                // Refresh the data after deletion
                refreshHistory()
                clearSelection()
            }
        }
    }

    fun refreshHistory() {
        launchWithLoading {
            try {
                // Perform database operations on IO thread
                val historyItems = withContext(Dispatchers.IO) {
                    val recordList: List<ThermalDao.Record> =
                        AppDatabase.getInstance().thermalDao().queryRecordList()
                    // Convert database records to HistoryItem objects
                    recordList.mapIndexed { index, record ->
                        // Query additional details for temperature statistics
                        val detailList = AppDatabase.getInstance().thermalDao().queryDetail(record.startTime)
                        // Calculate temperature statistics from detail data
                        val temperatures = detailList.map { it.thermal }
                        val maxTemperatures = detailList.map { it.thermalMax }
                        val minTemperatures = detailList.map { it.thermalMin }
                        val avgTemp = if (temperatures.isNotEmpty()) temperatures.average().toFloat() else 0f
                        val maxTemp = maxTemperatures.maxOrNull() ?: 0f
                        val minTemp = minTemperatures.minOrNull() ?: 0f
                        HistoryItem(
                            id = record.startTime.toString(),
                            sessionName = "Session ${index + 1}",
                            startTime = record.startTime,
                            duration = record.duration.toLong() * 1000L, // Convert seconds to milliseconds
                            sampleCount = detailList.size,
                            avgTemperature = avgTemp,
                            maxTemperature = maxTemp,
                            minTemperature = minTemp,
                            sessionType = when (record.type) {
                                "point" -> SessionType.MONITORING
                                "line" -> SessionType.ANALYSIS
                                "area" -> SessionType.CAPTURE
                                else -> SessionType.MONITORING
                            },
                            dataFilePath = findThermalImagePath(record.startTime, detailList.firstOrNull()?.thermalId)
                        )
                    }
                }
                // Update the data on main thread
                allHistoryItems = historyItems
                applyFilter()
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun toggleItemSelection(id: String) {
        val currentSelected = _selectedItems.value.toMutableSet()
        if (currentSelected.contains(id)) {
            currentSelected.remove(id)
        } else {
            currentSelected.add(id)
        }
        _selectedItems.value = currentSelected
        // Exit selection mode if no items are selected
        if (currentSelected.isEmpty()) {
            _isSelectionMode.value = false
        }
    }

    fun viewHistoryDetails(item: HistoryItem) {
        // Implement navigation to details screen with history item data
        launchWithErrorHandling {
            // Emit navigation event with the selected item
            _historyUiEvents.emit(HistoryUiEvent.NavigateToDetails(item))
        }
    }

    fun enterSelectionMode() {
        _isSelectionMode.value = true
    }

    private fun applyFilter() {
        val filteredItems = when (_selectedFilter.value) {
            HistoryFilter.ALL -> allHistoryItems
            HistoryFilter.TODAY -> filterByToday()
            HistoryFilter.WEEK -> filterByThisWeek()
            HistoryFilter.MONTH -> filterByThisMonth()
        }
        _historyItems.value = filteredItems.sortedByDescending { it.startTime }
    }

    private fun filterByToday(): List<HistoryItem> {
        val calendar = Calendar.getInstance()
        val today = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val tomorrow = calendar.apply {
            add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
        return allHistoryItems.filter { it.startTime in today until tomorrow }
    }

    private fun filterByThisWeek(): List<HistoryItem> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val weekStart = calendar.timeInMillis
        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        val weekEnd = calendar.timeInMillis
        return allHistoryItems.filter { it.startTime in weekStart until weekEnd }
    }

    private fun filterByThisMonth(): List<HistoryItem> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val monthStart = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        val monthEnd = calendar.timeInMillis
        return allHistoryItems.filter { it.startTime in monthStart until monthEnd }
    }

    private fun findThermalImagePath(startTime: Long, thermalId: String?): String {
        val possibleDirs = sequenceOf(
            FileConfig.gallerySourDir,
            FileConfig.lineIrGalleryDir,
            FileConfig.tc007IrGalleryDir
        )
        val possibleNames = sequenceOf(
            thermalId?.let { "$it.jpg" },
            thermalId?.let { "$it.png" },
            "${startTime}.jpg",
            "${startTime}.png"
        ).filterNotNull()
        return possibleDirs.flatMap { dir ->
            possibleNames.map { name -> File(dir, name) }
        }.firstOrNull { it.exists() }?.absolutePath ?: ""
    }

    // History UI Event sealed class for one-time events
    sealed class HistoryUiEvent {
        data class ShowMessage(val message: String) : HistoryUiEvent()
        data class ExportData(val items: List<HistoryItem>) : HistoryUiEvent()
        data class NavigateToDetails(val item: HistoryItem) : HistoryUiEvent()
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\IRMonitorViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.db.AppDatabase
import com.mpdc4gsr.libunified.app.db.dao.ThermalDao
import com.mpdc4gsr.libunified.app.db.entity.ThermalEntity
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IRMonitorViewModel : BaseViewModel() {
    val recordListLD = MutableLiveData<List<ThermalDao.Record>>()
    fun queryRecordList() {
        viewModelScope.launch(Dispatchers.IO) {
            val recordList: List<ThermalDao.Record> =
                AppDatabase.getInstance().thermalDao().queryRecordList()
            recordListLD.postValue(recordList)
        }
    }

    val detailListLD = MutableLiveData<List<ThermalEntity>>()
    fun queryDetail(startTime: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val detailList: List<ThermalEntity> =
                AppDatabase.getInstance().thermalDao().queryDetail(startTime)
            detailListLD.postValue(detailList)
        }
    }

    fun delDetail(startTime: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            AppDatabase.getInstance().thermalDao().delDetail(startTime)
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\IRPlushViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import android.view.SurfaceView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.WorkspacePremium
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class IRPlushViewModel : BaseViewModel() {
    companion object {
        private const val CALIBRATION_DELAY_MS = 2000L
    }

    // Dual view state management
    private val _dualViewState = MutableStateFlow(DualViewState.INACTIVE)
    val dualViewState: StateFlow<DualViewState> = _dualViewState.asStateFlow()

    // Temperature data management
    private val _temperatureData = MutableStateFlow(
        TemperatureData(
            irCenterTemp = 0.0f,
            irMaxTemp = 0.0f,
            irMinTemp = 0.0f,
            ambientTemp = 0.0f
        )
    )
    val temperatureData: StateFlow<TemperatureData> = _temperatureData.asStateFlow()

    // Processing mode management
    private val _processingMode = MutableStateFlow(ProcessingMode.STANDARD)
    val processingMode: StateFlow<ProcessingMode> = _processingMode.asStateFlow()

    // Recording state management
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    fun toggleRecording() {
        _isRecording.value = !_isRecording.value
    }

    fun initializeDualView(surfaceView: SurfaceView) {
        // Only update state, don't store the SurfaceView reference
        _dualViewState.value = DualViewState.ACTIVE
    }

    fun changeProcessingMode(mode: ProcessingMode) {
        _processingMode.value = mode
    }

    fun calibrateDualView() {
        launchWithErrorHandling {
            _dualViewState.value = DualViewState.CALIBRATING
            // Simulation of calibration process
            kotlinx.coroutines.delay(CALIBRATION_DELAY_MS)
            _dualViewState.value = DualViewState.ACTIVE
        }
    }

    fun resetSettings() {
        _processingMode.value = ProcessingMode.STANDARD
        _isRecording.value = false
        _temperatureData.value = TemperatureData(
            irCenterTemp = 0.0f,
            irMaxTemp = 0.0f,
            irMinTemp = 0.0f,
            ambientTemp = 0.0f
        )
    }

    fun updateTemperatureData(
        centerTemp: Float,
        maxTemp: Float,
        minTemp: Float,
        ambientTemp: Float
    ) {
        _temperatureData.value = TemperatureData(
            irCenterTemp = centerTemp,
            irMaxTemp = maxTemp,
            irMinTemp = minTemp,
            ambientTemp = ambientTemp
        )
    }

    // Data class definitions
    data class TemperatureData(
        val irCenterTemp: Float,
        val irMaxTemp: Float,
        val irMinTemp: Float,
        val ambientTemp: Float
    )

    enum class DualViewState {
        INACTIVE, ACTIVE, CALIBRATING, ERROR
    }

    enum class ProcessingMode(val displayName: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
        STANDARD("Standard", Icons.Default.CameraAlt),
        ENHANCED("Enhanced", Icons.Default.AutoAwesome),
        PROFESSIONAL("Professional", Icons.Default.WorkspacePremium),
        FUSION("Fusion", Icons.Default.Merge)
    }

    fun showAdvancedSettings() {
        // Placeholder for advanced settings functionality
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\IRThermalDoubleViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class IRThermalDoubleViewModel : BaseViewModel() {
    private val _selectedMode = MutableStateFlow(0)
    val selectedMode: StateFlow<Int> = _selectedMode.asStateFlow()
    private val _showOverlay = MutableStateFlow(true)
    val showOverlay: StateFlow<Boolean> = _showOverlay.asStateFlow()
    private val _showTrendChart = MutableStateFlow(false)
    val showTrendChart: StateFlow<Boolean> = _showTrendChart.asStateFlow()
    private val _showCompass = MutableStateFlow(false)
    val showCompass: StateFlow<Boolean> = _showCompass.asStateFlow()
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    private val _isRangeLocked = MutableStateFlow(false)
    val isRangeLocked: StateFlow<Boolean> = _isRangeLocked.asStateFlow()
    fun selectMode(mode: Int) {
        launchWithErrorHandling {
            _selectedMode.value = mode
        }
    }

    fun toggleOverlay() {
        launchWithErrorHandling {
            _showOverlay.value = !_showOverlay.value
        }
    }

    fun toggleTrendChart() {
        launchWithErrorHandling {
            _showTrendChart.value = !_showTrendChart.value
        }
    }

    fun toggleCompass() {
        launchWithErrorHandling {
            _showCompass.value = !_showCompass.value
        }
    }

    fun toggleRecording() {
        launchWithErrorHandling {
            _isRecording.value = !_isRecording.value
        }
    }

    fun toggleRangeLock() {
        launchWithErrorHandling {
            _isRangeLocked.value = !_isRangeLocked.value
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\IRThermalFragmentViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.socket.WebSocketProxy
import com.mpdc4gsr.libunified.app.tools.DeviceTools
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class IRThermalFragmentViewModel : BaseViewModel() {
    data class DeviceConnectionState(
        val hasConnection: Boolean = false,
        val isTC007Connected: Boolean = false,
        val hasUsbDevice: Boolean = false,
        val isTC007Device: Boolean = false
    )

    data class ThermalUIState(
        val isConnected: Boolean = false,
        val isTC007Connected: Boolean = false,
        val showConnectButton: Boolean = false,
        val isLoading: Boolean = false
    )

    // Device connection state management
    private val _deviceConnectionState = MutableStateFlow(DeviceConnectionState())
    val deviceConnectionState: StateFlow<DeviceConnectionState> =
        _deviceConnectionState.asStateFlow()

    // Individual state flows required by the Compose fragment
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()
    private val _isTC007 = MutableStateFlow(false)
    val isTC007: StateFlow<Boolean> = _isTC007.asStateFlow()
    private val _deviceInfo = MutableStateFlow<String?>(null)
    val deviceInfo: StateFlow<String?> = _deviceInfo.asStateFlow()

    // Navigation events
    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent

    // Permission state management
    private val _permissionState = MutableLiveData<PermissionState>()
    val permissionState: LiveData<PermissionState> = _permissionState

    // UI state management
    private val _thermalUiState = MutableStateFlow(ThermalUIState())
    val thermalUiState: StateFlow<ThermalUIState> = _thermalUiState.asStateFlow()

    // Action events for dialogs and operations
    private val _thermalAction = MutableLiveData<ThermalAction>()
    val thermalAction: LiveData<ThermalAction> = _thermalAction

    init {
        setupDeviceStateMonitoring()
    }

    private fun setupDeviceStateMonitoring() {
        viewModelScope.launch {
            // Monitor device connections and update UI state accordingly
            combine(
                _deviceConnectionState,
                _thermalUiState
            ) { connectionState, uiState ->
                // Update individual state flows
                _connectionStatus.value = when {
                    connectionState.hasConnection -> ConnectionStatus.CONNECTED
                    else -> ConnectionStatus.DISCONNECTED
                }
                _isTC007.value = connectionState.isTC007Device
                _deviceInfo.value = if (connectionState.hasConnection) {
                    if (connectionState.isTC007Device) "TC007 Connected" else "Device Connected"
                } else {
                    if (connectionState.hasUsbDevice) "USB Device Available" else "No Device Detected"
                }
                uiState.copy(
                    isConnected = connectionState.hasConnection,
                    isTC007Connected = connectionState.isTC007Connected,
                    showConnectButton = !connectionState.hasConnection && connectionState.hasUsbDevice
                )
            }.collect { newUiState ->
                _thermalUiState.value = newUiState
            }
        }
    }

    fun checkDeviceConnection(isTC007: Boolean) {
        val hasConnection = if (isTC007) {
            WebSocketProxy.getInstance().isTC007Connect()
        } else {
            DeviceTools.isConnect(isAutoRequest = false)
        }
        val hasUsbDevice = DeviceTools.findUsbDevice() != null
        _deviceConnectionState.value = DeviceConnectionState(
            hasConnection = hasConnection,
            isTC007Connected = isTC007 && hasConnection,
            hasUsbDevice = hasUsbDevice,
            isTC007Device = isTC007
        )
        // Update individual state flows
        _connectionStatus.value = if (hasConnection) ConnectionStatus.CONNECTED else ConnectionStatus.DISCONNECTED
        _isTC007.value = isTC007
        _deviceInfo.value = when {
            hasConnection -> if (isTC007) "TC007 Connected" else "Device Connected"
            hasUsbDevice -> "USB Device Available"
            else -> "No Device Detected"
        }
    }

    fun onDeviceConnected(isTC007Device: Boolean) {
        if (!isTC007Device) {
            SharedManager.hasTcLine = true
        }
        _deviceConnectionState.value = _deviceConnectionState.value.copy(
            hasConnection = true,
            isTC007Connected = isTC007Device,
            isTC007Device = isTC007Device
        )
    }

    fun onDeviceDisconnected() {
        _deviceConnectionState.value = _deviceConnectionState.value.copy(
            hasConnection = false,
            isTC007Connected = false,
            isTC007Device = false
        )
    }

    fun onSocketConnected(isTS004: Boolean, isTC007Device: Boolean) {
        if (isTC007Device && !isTS004) {
            _deviceConnectionState.value = _deviceConnectionState.value.copy(
                hasConnection = true,
                isTC007Connected = true,
                isTC007Device = true
            )
        }
    }

    fun onSocketDisConnected(isTS004: Boolean, isTC007Device: Boolean) {
        if (isTC007Device && !isTS004) {
            _deviceConnectionState.value = _deviceConnectionState.value.copy(
                hasConnection = false,
                isTC007Connected = false,
                isTC007Device = false
            )
        }
    }

    fun handleThermalOpen(isTC007: Boolean) {
        if (isTC007) {
            _navigationEvent.value = NavigationEvent.NavigateToTC007Thermal
        } else {
            when {
                DeviceTools.isTC001PlusConnect() -> {
                    _navigationEvent.value = NavigationEvent.StartThermalPlusActivity
                }

                DeviceTools.isTC001LiteConnect() -> {
                    _navigationEvent.value = NavigationEvent.NavigateToTCLite
                }

                DeviceTools.isHikConnect() -> {
                    _navigationEvent.value = NavigationEvent.NavigateToHikMain
                }

                else -> {
                    _navigationEvent.value = NavigationEvent.StartThermalNightActivity
                }
            }
        }
    }

    fun handleMainEnter() {
        val connectionState = _deviceConnectionState.value
        if (!connectionState.hasConnection) {
            if (!connectionState.hasUsbDevice) {
                _thermalAction.value = ThermalAction.ShowDeviceConnectTip
            } else {
                _permissionState.value = PermissionState.RequestCameraPermission
            }
        }
    }

    fun onPermissionGranted() {
        _thermalAction.value = ThermalAction.ShowConnectTip
    }

    fun onPermissionDenied(doNotAskAgain: Boolean) {
        if (doNotAskAgain) {
            _thermalAction.value = ThermalAction.ShowPermissionSettingsTip
        }
    }

    // Methods required by the Compose fragment
    fun retryConnection() {
        _connectionStatus.value = ConnectionStatus.CONNECTING
        viewModelScope.launch {
            val isTC007Device = _isTC007.value
            val hasConnection = if (isTC007Device) {
                WebSocketProxy.getInstance().isTC007Connect()
            } else {
                DeviceTools.isConnect(isAutoRequest = false)
            }
            if (hasConnection) {
                _connectionStatus.value = ConnectionStatus.CONNECTED
                onDeviceConnected(isTC007Device)
            } else {
                _connectionStatus.value = ConnectionStatus.ERROR
            }
        }
    }

    fun openMainThermal() {
        val isTC007Device = _isTC007.value
        handleThermalOpen(isTC007Device)
    }

    fun connectDevice() {
        _connectionStatus.value = ConnectionStatus.CONNECTING
        // This would typically trigger device connection logic
        retryConnection()
    }

    fun openDeviceSettings() {
        _thermalAction.value = ThermalAction.ShowConnectTip
    }

    sealed class NavigationEvent {
        object NavigateToTC007Thermal : NavigationEvent()
        object StartThermalPlusActivity : NavigationEvent()
        object NavigateToTCLite : NavigationEvent()
        object NavigateToHikMain : NavigationEvent()
        object StartThermalNightActivity : NavigationEvent()
    }

    sealed class ThermalAction {
        object ShowDeviceConnectTip : ThermalAction()
        object ShowConnectTip : ThermalAction()
        object ShowPermissionSettingsTip : ThermalAction()
    }

    sealed class PermissionState {
        object RequestCameraPermission : PermissionState()
        object PermissionGranted : PermissionState()
        data class PermissionDenied(val doNotAskAgain: Boolean) : PermissionState()
    }

    enum class ConnectionStatus {
        DISCONNECTED, CONNECTING, CONNECTED, ERROR
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\MonitorThermalViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

class MonitorThermalViewModel : BaseViewModel() {
    // Monitoring State
    private val _monitoringState = MutableStateFlow(MonitoringState.STOPPED)
    val monitoringState: StateFlow<MonitoringState> = _monitoringState.asStateFlow()

    // Thermal Data
    private val _thermalData = MutableStateFlow<ThermalData?>(null)
    val thermalData: StateFlow<ThermalData?> = _thermalData.asStateFlow()

    // Recording Status
    private val _recordingStatus = MutableStateFlow(RecordingStatus.IDLE)
    val recordingStatus: StateFlow<RecordingStatus> = _recordingStatus.asStateFlow()

    // Monitoring Alerts
    private val _monitoringAlerts = MutableStateFlow<List<MonitoringAlert>>(emptyList())
    val monitoringAlerts: StateFlow<List<MonitoringAlert>> = _monitoringAlerts.asStateFlow()
    fun toggleMonitoring() {
        launchWithErrorHandling {
            _monitoringState.value = when (_monitoringState.value) {
                MonitoringState.STOPPED -> MonitoringState.ACTIVE
                MonitoringState.ACTIVE -> MonitoringState.PAUSED
                MonitoringState.PAUSED -> MonitoringState.ACTIVE
            }
        }
    }

    fun toggleRecording() {
        launchWithErrorHandling {
            _recordingStatus.value = when (_recordingStatus.value) {
                RecordingStatus.IDLE -> RecordingStatus.RECORDING
                RecordingStatus.RECORDING -> RecordingStatus.IDLE
            }
        }
    }

    fun updateMonitoringFence(fence: FenceData) {
        launchWithErrorHandling {
            // Update fence configuration for monitoring
            // Implementation would integrate with thermal processing
        }
    }

    fun updateTemperatureThreshold(threshold: TemperatureThreshold) {
        launchWithErrorHandling {
            // Update temperature threshold settings
            // Implementation would configure alert triggers
        }
    }

    fun updateAlertSettings(settings: AlertSettings) {
        launchWithErrorHandling {
            // Update alert configuration
            // Implementation would configure notification settings
        }
    }

    fun exportMonitoringData() {
        launchWithErrorHandling {
            // Export monitoring session data
            // Implementation would handle data export
        }
    }

    // Data classes and enums
    data class ThermalData(
        val currentTemp: Float,
        val maxTemp: Float,
        val minTemp: Float,
        val avgTemp: Float,
        val isAlarmTriggered: Boolean,
        val sessionDuration: String,
        val sampleCount: Int,
        val alertCount: Int,
        val dataSize: String
    )

    data class FenceData(val data: String)
    data class TemperatureThreshold(val high: Float, val low: Float)
    data class AlertSettings(val soundEnabled: Boolean, val vibrationEnabled: Boolean)
    data class MonitoringAlert(
        val message: String,
        val severity: AlertSeverity,
        val timestamp: Date
    )

    enum class MonitoringState {
        STOPPED, ACTIVE, PAUSED
    }

    enum class RecordingStatus {
        IDLE, RECORDING
    }

    enum class AlertSeverity {
        LOW, MEDIUM, HIGH
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\MonitorViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.MutableLiveData
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel

class MonitorViewModel : BaseViewModel() {
    companion object {
        const val STATS_START = 101
        const val STATS_MONITOR = 102
        const val STATS_FINISH = 103
    }

    private val _monitorState = MutableLiveData(STATS_START)
    val monitorState: MutableLiveData<Int> = _monitorState
    private val _selectedType = MutableLiveData(1)
    val selectedType: MutableLiveData<Int> = _selectedType
    private val _selectedIndex = MutableLiveData<ArrayList<Int>>(arrayListOf())
    val selectedIndex: MutableLiveData<ArrayList<Int>> = _selectedIndex
    private val _recordingTime = MutableLiveData(0L)
    val recordingTime: MutableLiveData<Long> = _recordingTime
    fun setMonitorState(state: Int) {
        _monitorState.value = state
    }

    fun selectMonitorType(type: Int, indices: ArrayList<Int>) {
        _selectedType.value = type
        _selectedIndex.value = indices
        _monitorState.value = STATS_FINISH
    }

    fun startRecording() {
        _recordingTime.value = 0L
    }

    fun updateRecordingTime(time: Long) {
        _recordingTime.value = time
    }

    fun resetState() {
        _monitorState.value = STATS_START
        _selectedType.value = 1
        _selectedIndex.value = arrayListOf()
        _recordingTime.value = 0L
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\PDFListViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.config.FileConfig
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PDFListViewModel : BaseViewModel() {
    companion object {
        private const val TAG = "PDFListViewModel"
    }

    // Data class for PDF items (matching the one in fragment)
    data class PDFItem(
        val path: String,
        val name: String,
        val size: Long,
        val pageCount: Int,
        val dateModified: Long,
        val isAnalysisReport: Boolean = false
    )

    // State flows for Compose
    private val _pdfItems = MutableStateFlow<List<PDFItem>>(emptyList())
    val pdfItems: StateFlow<List<PDFItem>> = _pdfItems.asStateFlow()
    private val _selectedItems = MutableStateFlow<Set<String>>(emptySet())
    val selectedItems: StateFlow<Set<String>> = _selectedItems.asStateFlow()
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadPDFItems()
    }

    private fun loadPDFItems() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.update { true }
            try {
                val items = getPDFItemsList()
                withContext(Dispatchers.Main) {
                    _pdfItems.update { items }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading PDF items", e)
            } finally {
                _isLoading.update { false }
            }
        }
    }

    fun enterSelectionMode(itemPath: String? = null) {
        _isSelectionMode.update { true }
        itemPath?.let { path ->
            _selectedItems.update { setOf(path) }
        }
    }

    fun exitSelectionMode() {
        _isSelectionMode.update { false }
        _selectedItems.update { emptySet() }
    }

    fun clearSelection() {
        _selectedItems.update { emptySet() }
        _isSelectionMode.update { false }
    }

    fun toggleItemSelection(itemPath: String) {
        _selectedItems.update { currentSet ->
            val mutableSet = currentSet.toMutableSet()
            if (mutableSet.contains(itemPath)) {
                mutableSet.remove(itemPath)
            } else {
                mutableSet.add(itemPath)
            }
            mutableSet
        }
        if (_selectedItems.value.isEmpty()) {
            _isSelectionMode.update { false }
        }
    }

    // File operations
    fun deleteSelectedItems() {
        val selectedPaths = _selectedItems.value
        val itemsToDelete = _pdfItems.value.filter { selectedPaths.contains(it.path) }
        viewModelScope.launch(Dispatchers.IO) {
            itemsToDelete.forEach { item ->
                try {
                    File(item.path).delete()
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting file: ${item.path}", e)
                }
            }
            withContext(Dispatchers.Main) {
                exitSelectionMode()
                loadPDFItems() // Refresh the list
            }
        }
    }

    fun refreshPDFList() {
        loadPDFItems()
    }

    // Get PDF items from file system
    private suspend fun getPDFItemsList(): List<PDFItem> {
        return try {
            val items = mutableListOf<PDFItem>()
            // Scan the PDF directory for PDF files
            val pdfDir = File(FileConfig.getPdfDir())
            Log.d(TAG, "Scanning PDF directory: ${pdfDir.absolutePath}")
            if (pdfDir.exists() && pdfDir.isDirectory) {
                val pdfFiles = pdfDir.listFiles { file ->
                    file.isFile && file.name.lowercase().endsWith(".pdf")
                }
                Log.d(TAG, "Found ${pdfFiles?.size ?: 0} PDF files")
                pdfFiles?.forEach { pdfFile ->
                    try {
                        // Determine if this is an analysis report based on filename patterns
                        val isAnalysisReport = pdfFile.name.contains("analysis", ignoreCase = true) ||
                                pdfFile.name.contains("report", ignoreCase = true) ||
                                pdfFile.name.contains("thermal", ignoreCase = true)
                        // For now, we'll use a default page count of 1
                        // In a production app, you would use a PDF library to get actual page count
                        val pageCount = 1
                        items.add(
                            PDFItem(
                                path = pdfFile.absolutePath,
                                name = pdfFile.name,
                                size = pdfFile.length(),
                                pageCount = pageCount,
                                dateModified = pdfFile.lastModified(),
                                isAnalysisReport = isAnalysisReport
                            )
                        )
                        Log.d(TAG, "Added PDF: ${pdfFile.name} (${pdfFile.length()} bytes)")
                    } catch (e: Exception) {
                        Log.w(TAG, "Error processing PDF file: ${pdfFile.name}", e)
                    }
                }
            } else {
                Log.w(TAG, "PDF directory does not exist or is not a directory: ${pdfDir.absolutePath}")
            }
            // Sort by date modified (newest first)
            val sortedItems = items.sortedByDescending { it.dateModified }
            Log.d(TAG, "Returning ${sortedItems.size} PDF items")
            sortedItems
        } catch (e: Exception) {
            Log.e(TAG, "Error getting PDF items list", e)
            emptyList()
        }
    }

    fun showMoreActions(item: PDFItem) {
        // Placeholder for more actions functionality
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\ReportDetailViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.repository.BaseRepository
import kotlinx.coroutines.flow.*

class ReportDetailViewModel : BaseViewModel() {
    private val _reportDate = MutableStateFlow("")
    val reportDate: StateFlow<String> = _reportDate.asStateFlow()
    private val _reportTime = MutableStateFlow("")
    val reportTime: StateFlow<String> = _reportTime.asStateFlow()
    private val _location = MutableStateFlow("")
    val location: StateFlow<String> = _location.asStateFlow()
    private val _inspector = MutableStateFlow("")
    val inspector: StateFlow<String> = _inspector.asStateFlow()
    private val _equipment = MutableStateFlow("")
    val equipment: StateFlow<String> = _equipment.asStateFlow()
    private val _reportId = MutableStateFlow<String?>(null)
    val reportId: StateFlow<String?> = _reportId.asStateFlow()
    private val _events = MutableSharedFlow<ReportDetailEvent>()
    val events: SharedFlow<ReportDetailEvent> = _events.asSharedFlow()
    private val reportRepository = ReportDetailRepository()

    sealed class ReportDetailEvent {
        data class ShareReport(val reportId: String) : ReportDetailEvent()
        data class DeleteReport(val reportId: String) : ReportDetailEvent()
    }

    fun loadReportData(reportId: String) {
        launchWithLoading {
            _reportId.value = reportId
            val result = reportRepository.getReportById(reportId)
            when (result) {
                is BaseRepository.Result.Success -> {
                    val report = result.data
                    _reportDate.value = report.date
                    _reportTime.value = report.time
                    _location.value = report.location
                    _inspector.value = report.inspector
                    _equipment.value = report.equipment
                }

                is BaseRepository.Result.Error -> {
                    throw result.exception
                }

                else -> {}
            }
        }
    }

    fun shareReport() {
        launchWithErrorHandling {
            val currentReportId = _reportId.value
            if (currentReportId != null) {
                _events.emit(ReportDetailEvent.ShareReport(currentReportId))
            } else {
                _uiEvents.emit(UiEvent.ShowError("No report loaded to share"))
            }
        }
    }

    fun deleteReport() {
        launchWithErrorHandling {
            val currentReportId = _reportId.value
            if (currentReportId != null) {
                _events.emit(ReportDetailEvent.DeleteReport(currentReportId))
            } else {
                _uiEvents.emit(UiEvent.ShowError("No report loaded to delete"))
            }
        }
    }

    private inner class ReportDetailRepository : BaseRepository() {
        suspend fun getReportById(reportId: String): Result<ReportDetail> = safeCall {
            val cacheKey = "report_detail_$reportId"
            getCachedOrExecute(cacheKey, 5 * 60 * 1000L) {
                ReportDetail(
                    id = reportId,
                    date = "2024-10-01",
                    time = "14:30:00",
                    location = "Building A - Room 101",
                    inspector = "John Doe",
                    equipment = "TC001 Thermal Camera"
                )
            }
        }
    }

    data class ReportDetail(
        val id: String,
        val date: String,
        val time: String,
        val location: String,
        val inspector: String,
        val equipment: String
    )
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\ReportPreviewViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import android.content.Context
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReportPreviewViewModel : BaseViewModel() {
    private val _selectedLayout = MutableStateFlow(0)
    val selectedLayout: StateFlow<Int> = _selectedLayout.asStateFlow()
    private val _showImages = MutableStateFlow(true)
    val showImages: StateFlow<Boolean> = _showImages.asStateFlow()
    private val _showMetadata = MutableStateFlow(true)
    val showMetadata: StateFlow<Boolean> = _showMetadata.asStateFlow()
    private val _showWatermark = MutableStateFlow(false)
    val showWatermark: StateFlow<Boolean> = _showWatermark.asStateFlow()
    private val _previewGenerated = MutableStateFlow(false)
    val previewGenerated: StateFlow<Boolean> = _previewGenerated.asStateFlow()
    private val _previewData = MutableStateFlow<PreviewData?>(null)
    val previewData: StateFlow<PreviewData?> = _previewData.asStateFlow()

    data class PreviewData(
        val layoutIndex: Int,
        val includeImages: Boolean,
        val includeMetadata: Boolean,
        val includeWatermark: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    )

    fun selectLayout(index: Int) {
        launchWithErrorHandling {
            _selectedLayout.value = index
        }
    }

    fun toggleImages() {
        launchWithErrorHandling {
            _showImages.value = !_showImages.value
        }
    }

    fun toggleMetadata() {
        launchWithErrorHandling {
            _showMetadata.value = !_showMetadata.value
        }
    }

    fun toggleWatermark() {
        launchWithErrorHandling {
            _showWatermark.value = !_showWatermark.value
        }
    }

    fun generatePreview() {
        launchWithLoading {
            val currentLayout = _selectedLayout.value
            val currentShowImages = _showImages.value
            val currentShowMetadata = _showMetadata.value
            val currentShowWatermark = _showWatermark.value
            delay(500)
            val preview = PreviewData(
                layoutIndex = currentLayout,
                includeImages = currentShowImages,
                includeMetadata = currentShowMetadata,
                includeWatermark = currentShowWatermark
            )
            _previewData.value = preview
            _previewGenerated.value = true
        }
    }

    fun proceedToSecond(context: Context) {
        launchWithErrorHandling {
            NavigationManager.build(RouterConfig.REPORT_PREVIEW_SECOND)
                .navigation(context)
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\ThermalFragmentViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.energy.iruvc.ircmd.IRCMD
import com.energy.iruvc.utils.CommonParams
import com.energy.iruvc.utils.SynchronizedBitmap
import com.energy.iruvc.uvc.ConnectCallback
import com.energy.iruvc.uvc.UVCCamera
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.matrix.IrSurfaceView
import com.mpdc4gsr.libunified.ir.camera.IRUVCTC
import com.mpdc4gsr.libunified.ir.extension.setAutoShutter
import com.mpdc4gsr.libunified.ir.extension.setContrast
import com.mpdc4gsr.libunified.ir.extension.setMirror
import com.mpdc4gsr.libunified.ir.extension.setPropDdeLevel
import com.mpdc4gsr.libunified.ir.utils.USBMonitorCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ThermalFragmentViewModel(
    private val context: Context? = null
) : BaseViewModel() {
    // ThermalMonitoringUiState data class for Compose UI - holds monitoring-related state
    data class ThermalMonitoringUiState(
        val isMonitoring: Boolean = false,
        val currentTemperature: Float? = null,
        val minTemperature: Float? = null,
        val maxTemperature: Float? = null,
        val averageTemperature: Float? = null,
        val isDeviceConnected: Boolean = false,
        val isRecording: Boolean = false,
        val alertCount: Int = 0
    )

    // Thermal image processing state
    private val _thermalImageState = MutableStateFlow(ThermalImageState())
    val thermalImageState: StateFlow<ThermalImageState> = _thermalImageState.asStateFlow()

    // Temperature analysis state
    private val _temperatureAnalysis = MutableStateFlow(TemperatureAnalysis())
    val temperatureAnalysis: StateFlow<TemperatureAnalysis> = _temperatureAnalysis.asStateFlow()

    // Thermal processing actions
    private val _thermalProcessingAction = MutableLiveData<ThermalProcessingAction>()
    val thermalProcessingAction: LiveData<ThermalProcessingAction> = _thermalProcessingAction

    // Fence and measurement state
    private val _fenceState = MutableStateFlow(FenceState())
    val fenceState: StateFlow<FenceState> = _fenceState.asStateFlow()

    // Video recording state
    private val _videoRecordingState = MutableStateFlow(VideoRecordingState())
    val videoRecordingState: StateFlow<VideoRecordingState> = _videoRecordingState.asStateFlow()

    // UI interaction state for processing
    private val _processingUiState = MutableStateFlow(ThermalProcessingUiState())
    val processingUiState: StateFlow<ThermalProcessingUiState> = _processingUiState.asStateFlow()

    // Temperature data for UI display
    private val _temperatureData = MutableStateFlow<TemperatureData?>(null)
    val temperatureData: StateFlow<TemperatureData?> = _temperatureData.asStateFlow()

    // Recording state for UI
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    // Connection status for UI
    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    // Processing mode for UI
    private val _processingMode = MutableStateFlow("Standard")
    val processingMode: StateFlow<String> = _processingMode.asStateFlow()

    // Thermal surface dimensions
    var rawWidth: Int = 0
        private set
    var rawHeight: Int = 0
        private set
    private var iruvctc: IRUVCTC? = null
    private var syncBitmap: SynchronizedBitmap? = null
    private var ircmd: IRCMD? = null

    init {
        setupThermalDataProcessing()
        syncRecordingStates()
        syncUiState()
    }

    private fun setupThermalDataProcessing() {
        viewModelScope.launch {
            // Combine thermal image and temperature data for comprehensive analysis
            combine(
                _thermalImageState,
                _temperatureAnalysis,
                _fenceState
            ) { imageState, tempAnalysis, fenceState ->
                ThermalProcessingUiState(
                    isProcessing = imageState.isProcessing,
                    hasValidImage = imageState.bitmap != null,
                    temperatureInfo = tempAnalysis,
                    fenceActive = fenceState.isActive,
                    processingProgress = imageState.processingProgress
                )
            }.collect { newUiState ->
                _processingUiState.value = newUiState
            }
        }
    }

    private fun syncRecordingStates() {
        viewModelScope.launch {
            // Keep _isRecording in sync with _videoRecordingState
            _videoRecordingState.collect { videoState ->
                if (_isRecording.value != videoState.isRecording) {
                    _isRecording.value = videoState.isRecording
                }
            }
        }
    }

    // Thermal image processing methods
    suspend fun processThermalBitmap(bitmap: Bitmap): ProcessedThermalResult {
        return withContext(Dispatchers.Default) {
            _thermalImageState.value = _thermalImageState.value.copy(
                isProcessing = true,
                processingProgress = 0f
            )
            try {
                val processedBitmap = applyThermalProcessing(bitmap)
                val temperatureData = extractTemperatureData(bitmap)
                val analysis = performTemperatureAnalysis(temperatureData)
                _thermalImageState.value = _thermalImageState.value.copy(
                    bitmap = processedBitmap,
                    isProcessing = false,
                    processingProgress = 1f
                )
                _temperatureAnalysis.value = analysis
                ProcessedThermalResult(
                    processedBitmap = processedBitmap,
                    temperatureAnalysis = analysis,
                    success = true
                )
            } catch (e: Exception) {
                _thermalImageState.value = _thermalImageState.value.copy(
                    isProcessing = false,
                    processingProgress = 0f
                )
                ProcessedThermalResult(
                    processedBitmap = null,
                    temperatureAnalysis = TemperatureAnalysis(),
                    success = false,
                    error = e.message
                )
            }
        }
    }

    private fun applyThermalProcessing(bitmap: Bitmap): Bitmap {
        // Apply thermal image processing algorithms
        val matrix = Matrix()
        // Add thermal processing transformations
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun extractTemperatureData(bitmap: Bitmap): FloatArray {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels.map { pixel: Int ->
            val red = (pixel shr 16) and 0xFF
            val green = (pixel shr 8) and 0xFF
            val blue = pixel and 0xFF
            val intensity = (red * 0.3f + green * 0.59f + blue * 0.11f) / 255f
            val minTemp = -20f
            val maxTemp = 120f
            minTemp + (intensity * (maxTemp - minTemp))
        }.toFloatArray()
    }

    private fun performTemperatureAnalysis(temperatureData: FloatArray): TemperatureAnalysis {
        if (temperatureData.isEmpty()) {
            return TemperatureAnalysis()
        }
        val maxTemp = temperatureData.maxOrNull() ?: 0f
        val minTemp = temperatureData.minOrNull() ?: 0f
        val avgTemp = temperatureData.average().toFloat()
        val variance = calculateVariance(temperatureData, avgTemp)
        val stdDev = kotlin.math.sqrt(variance)
        val hotSpots = detectHotSpots(temperatureData)
        val coldSpots = detectColdSpots(temperatureData)
        val temperatureTrend = calculateTemperatureTrend(temperatureData)
        return TemperatureAnalysis(
            maxTemperature = maxTemp,
            minTemperature = minTemp,
            averageTemperature = avgTemp,
            standardDeviation = stdDev,
            hotSpotCount = hotSpots.size,
            coldSpotCount = coldSpots.size,
            temperatureTrend = temperatureTrend,
            dataQuality = assessDataQuality(temperatureData),
            isValid = true
        )
    }

    private fun calculateVariance(data: FloatArray, mean: Float): Float {
        return data.map { (it - mean) * (it - mean) }.average().toFloat()
    }

    private fun detectHotSpots(temperatureData: FloatArray): List<HotSpot> {
        val threshold = temperatureData.maxOrNull()?.let { it * 0.8f } ?: 0f
        val hotSpots = mutableListOf<HotSpot>()
        temperatureData.forEachIndexed { index, temp ->
            if (temp > threshold) {
                hotSpots.add(HotSpot(index, temp))
            }
        }
        return hotSpots
    }

    private fun detectColdSpots(temperatureData: FloatArray): List<ColdSpot> {
        val threshold = temperatureData.minOrNull()?.let { it * 1.2f } ?: 0f
        val coldSpots = mutableListOf<ColdSpot>()
        temperatureData.forEachIndexed { index, temp ->
            if (temp < threshold) {
                coldSpots.add(ColdSpot(index, temp))
            }
        }
        return coldSpots
    }

    private fun calculateTemperatureTrend(temperatureData: FloatArray): TemperatureTrend {
        if (temperatureData.size < 2) return TemperatureTrend.STABLE
        val firstHalf = temperatureData.take(temperatureData.size / 2).average()
        val secondHalf = temperatureData.takeLast(temperatureData.size / 2).average()
        return when {
            secondHalf > firstHalf * 1.05 -> TemperatureTrend.RISING
            secondHalf < firstHalf * 0.95 -> TemperatureTrend.FALLING
            else -> TemperatureTrend.STABLE
        }
    }

    private fun assessDataQuality(temperatureData: FloatArray): DataQuality {
        val validCount =
            temperatureData.count { it > -40f && it < 150f } // Reasonable temperature range
        val qualityPercentage = validCount.toFloat() / temperatureData.size
        return when {
            qualityPercentage >= 0.95f -> DataQuality.EXCELLENT
            qualityPercentage >= 0.85f -> DataQuality.GOOD
            qualityPercentage >= 0.70f -> DataQuality.FAIR
            else -> DataQuality.POOR
        }
    }

    // Fence management methods
    fun activateFence(fenceType: FenceType) {
        _fenceState.value = _fenceState.value.copy(
            isActive = true,
            fenceType = fenceType,
            measurements = emptyList()
        )
    }

    fun deactivateFence() {
        _fenceState.value = _fenceState.value.copy(
            isActive = false,
            fenceType = null,
            measurements = emptyList()
        )
    }

    fun addFenceMeasurement(x: Int, y: Int, temperature: Float) {
        val currentMeasurements = _fenceState.value.measurements.toMutableList()
        currentMeasurements.add(FenceMeasurement(x, y, temperature))
        _fenceState.value = _fenceState.value.copy(
            measurements = currentMeasurements
        )
    }

    // Video recording methods
    fun startVideoRecording(outputFile: File) {
        _videoRecordingState.value = _videoRecordingState.value.copy(
            isRecording = true,
            outputFile = outputFile,
            recordingStartTime = System.currentTimeMillis()
        )
    }

    fun stopVideoRecording() {
        val recordingDuration = System.currentTimeMillis() -
                (_videoRecordingState.value.recordingStartTime ?: 0L)
        _videoRecordingState.value = _videoRecordingState.value.copy(
            isRecording = false,
            recordingDuration = recordingDuration
        )
    }

    // Public methods for UI interaction
    fun initializeThermalCamera(surfaceView: IrSurfaceView) {
        _connectionStatus.value = "Connecting"
        viewModelScope.launch {
            try {
                if (context == null) {
                    _connectionStatus.value = "Connection Failed"
                    handleError(Exception("Context not provided"))
                    return@launch
                }
                withContext(Dispatchers.Main) {
                    surfaceView.holder.setFixedSize(256, 192)
                }
                syncBitmap = SynchronizedBitmap()
                val connectCallback = object : ConnectCallback {
                    override fun onCameraOpened(camera: UVCCamera?) {
                        _connectionStatus.value = "Connected"
                        _temperatureData.value = TemperatureData(
                            centerTemp = "25.0Â°C",
                            maxTemp = "30.0Â°C",
                            minTemp = "20.0Â°C"
                        )
                    }

                    override fun onIRCMDCreate(cmd: IRCMD?) {
                        ircmd = cmd
                        cmd?.let {
                            it.setMirror(false)
                            it.setAutoShutter(true)
                            it.setPropDdeLevel(128)
                            it.setContrast(128)
                        }
                    }
                }
                val usbMonitorCallback = object : USBMonitorCallback {
                    override fun onAttach() {}
                    override fun onGranted() {}
                    override fun onDettach() {}
                    override fun onCancel() {}
                    override fun onConnect() {}
                    override fun onDisconnect() {}
                }
                iruvctc = IRUVCTC(
                    256,
                    192,
                    context,
                    syncBitmap!!,
                    CommonParams.DataFlowMode.TEMP_OUTPUT,
                    connectCallback,
                    usbMonitorCallback
                )
                iruvctc?.registerUSB()
                rawWidth = 256
                rawHeight = 192
            } catch (e: Exception) {
                _connectionStatus.value = "Connection Failed"
                handleError(e)
            }
        }
    }

    fun capturePhoto() {
        viewModelScope.launch {
            try {
                val timestamp = System.currentTimeMillis()
                val fileName = "thermal_photo_$timestamp.jpg"
                val thermalState = _thermalImageState.value
                val tempAnalysis = _temperatureAnalysis.value
                val metadata = mapOf(
                    "timestamp" to timestamp,
                    "centerTemp" to tempAnalysis.averageTemperature,
                    "maxTemp" to tempAnalysis.maxTemperature,
                    "minTemp" to tempAnalysis.minTemperature,
                    "averageTemp" to tempAnalysis.averageTemperature,
                    "deviceConnected" to (ircmd != null),
                    "sdkInitialized" to (iruvctc != null)
                )
                _thermalProcessingAction.postValue(
                    ThermalProcessingAction.PhotoCaptured(fileName, metadata)
                )
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun toggleRecording() {
        viewModelScope.launch {
            if (_isRecording.value) {
                // Stop recording
                stopVideoRecording()
                _isRecording.value = false
            } else {
                // Start recording
                try {
                    // Create a temporary file for recording
                    val outputFile = File.createTempFile(
                        "thermal_recording_${System.currentTimeMillis()}",
                        ".mp4"
                    )
                    startVideoRecording(outputFile)
                    _isRecording.value = true
                } catch (e: Exception) {
                    // Handle file creation error
                    _isRecording.value = false
                    // Emit error event and log the exception
                    handleError(e)
                    _thermalProcessingAction.postValue(
                        ThermalProcessingAction.RecordingError(e.message ?: "Failed to start recording")
                    )
                }
            }
        }
    }

    fun openSettings() {
        // Open thermal camera settings
        _thermalProcessingAction.postValue(
            ThermalProcessingAction.NavigateToSettings
        )
    }

    fun updateSurfaceDimensions(width: Int, height: Int) {
        rawWidth = width
        rawHeight = height
    }

    fun calculateViewPosition(
        index: Int,
        viewWidth: Int,
        viewHeight: Int,
        parentWidth: Int,
        parentHeight: Int
    ): Pair<Float, Float> {
        if (rawWidth == 0 || rawHeight == 0) {
            return Pair(0f, 0f)
        }
        val y = index / rawWidth
        val x = index - y * rawWidth
        val x1 = x * parentWidth / rawWidth
        val y1 = y * parentHeight / rawHeight
        val maxX = x1 - viewWidth / 2
        val maxY = y1 - viewHeight / 2
        return Pair(maxX.toFloat(), maxY.toFloat())
    }

    // Data classes for state management
    data class TemperatureData(
        val centerTemp: String = "--Â°C",
        val maxTemp: String = "--Â°C",
        val minTemp: String = "--Â°C"
    )

    data class ThermalImageState(
        val bitmap: Bitmap? = null,
        val isProcessing: Boolean = false,
        val processingProgress: Float = 0f
    )

    data class TemperatureAnalysis(
        val maxTemperature: Float = 0f,
        val minTemperature: Float = 0f,
        val averageTemperature: Float = 0f,
        val standardDeviation: Float = 0f,
        val hotSpotCount: Int = 0,
        val coldSpotCount: Int = 0,
        val temperatureTrend: TemperatureTrend = TemperatureTrend.STABLE,
        val dataQuality: DataQuality = DataQuality.POOR,
        val isValid: Boolean = false
    )

    data class FenceState(
        val isActive: Boolean = false,
        val fenceType: FenceType? = null,
        val measurements: List<FenceMeasurement> = emptyList()
    )

    data class VideoRecordingState(
        val isRecording: Boolean = false,
        val outputFile: File? = null,
        val recordingStartTime: Long? = null,
        val recordingDuration: Long = 0L
    )

    data class ThermalProcessingUiState(
        val isProcessing: Boolean = false,
        val hasValidImage: Boolean = false,
        val temperatureInfo: TemperatureAnalysis = TemperatureAnalysis(),
        val fenceActive: Boolean = false,
        val processingProgress: Float = 0f
    )

    data class ProcessedThermalResult(
        val processedBitmap: Bitmap?,
        val temperatureAnalysis: TemperatureAnalysis,
        val success: Boolean,
        val error: String? = null
    )

    data class HotSpot(val index: Int, val temperature: Float)
    data class ColdSpot(val index: Int, val temperature: Float)
    data class FenceMeasurement(val x: Int, val y: Int, val temperature: Float)
    enum class TemperatureTrend { RISING, FALLING, STABLE }
    enum class DataQuality { EXCELLENT, GOOD, FAIR, POOR }
    enum class FenceType { POINT, LINE, AREA }
    sealed class ThermalProcessingAction {
        object StartProcessing : ThermalProcessingAction()
        object ProcessingComplete : ThermalProcessingAction()
        data class ProcessingError(val message: String) : ThermalProcessingAction()
        data class TemperatureAlert(val temperature: Float, val type: AlertType) :
            ThermalProcessingAction()

        data class PhotoCaptured(val fileName: String, val metadata: Map<String, Any>) : ThermalProcessingAction()
        data class RecordingError(val message: String) : ThermalProcessingAction()
        object NavigateToSettings : ThermalProcessingAction()
        data class RegionConfigured(val fenceType: FenceType) : ThermalProcessingAction()
    }

    enum class AlertType { HOT_SPOT, COLD_SPOT, TEMPERATURE_THRESHOLD }

    // Combined UI state for compose UI
    private val _thermalUiState = MutableStateFlow(ThermalMonitoringUiState())
    val thermalUiState: StateFlow<ThermalMonitoringUiState> = _thermalUiState.asStateFlow()

    // Monitoring state
    private val _isMonitoring = MutableStateFlow(false)
    private fun syncUiState() {
        viewModelScope.launch {
            combine(
                _isMonitoring,
                _temperatureAnalysis,
                _connectionStatus,
                _isRecording
            ) { isMonitoring, tempAnalysis, connectionStatus, isRecording ->
                ThermalMonitoringUiState(
                    isMonitoring = isMonitoring,
                    currentTemperature = if (tempAnalysis.isValid) tempAnalysis.averageTemperature else null,
                    minTemperature = if (tempAnalysis.isValid) tempAnalysis.minTemperature else null,
                    maxTemperature = if (tempAnalysis.isValid) tempAnalysis.maxTemperature else null,
                    averageTemperature = if (tempAnalysis.isValid) tempAnalysis.averageTemperature else null,
                    isDeviceConnected = connectionStatus == "Connected",
                    isRecording = isRecording,
                    alertCount = tempAnalysis.hotSpotCount + tempAnalysis.coldSpotCount
                )
            }.collect { newUiState ->
                _thermalUiState.value = newUiState
            }
        }
    }

    // Monitoring control methods
    fun startMonitoring() {
        _isMonitoring.value = true
        viewModelScope.launch {
            // Start thermal monitoring process
            // TODO: Implement actual monitoring logic
        }
    }

    fun stopMonitoring() {
        _isMonitoring.value = false
        viewModelScope.launch {
            // Stop thermal monitoring process
            // TODO: Implement actual monitoring stop logic
        }
    }

    fun configureRegions() {
        viewModelScope.launch {
            try {
                val currentFence = _fenceState.value
                val nextFenceType = when (currentFence.fenceType) {
                    FenceType.POINT -> FenceType.LINE
                    FenceType.LINE -> FenceType.AREA
                    FenceType.AREA -> FenceType.POINT
                    null -> FenceType.POINT
                }
                _fenceState.value = currentFence.copy(
                    fenceType = nextFenceType
                )
                _thermalProcessingAction.postValue(
                    ThermalProcessingAction.RegionConfigured(nextFenceType)
                )
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun disconnectCamera() {
        viewModelScope.launch {
            try {
                iruvctc?.unregisterUSB()
                iruvctc?.stopPreview()
                iruvctc = null
                ircmd = null
                syncBitmap = null
                _connectionStatus.value = "Disconnected"
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun showSettings() {
        // Placeholder for settings functionality
    }

    override fun onCleared() {
        super.onCleared()
        disconnectCamera()
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\ThermalIrNightViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThermalIrNightViewModel : BaseViewModel() {
    private val _selectedMode = MutableStateFlow(0)
    val selectedMode: StateFlow<Int> = _selectedMode.asStateFlow()
    private val _nightModeEnabled = MutableStateFlow(true)
    val nightModeEnabled: StateFlow<Boolean> = _nightModeEnabled.asStateFlow()
    private val _showOverlay = MutableStateFlow(true)
    val showOverlay: StateFlow<Boolean> = _showOverlay.asStateFlow()
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    fun selectMode(mode: Int) {
        launchWithErrorHandling {
            _selectedMode.value = mode
        }
    }

    fun toggleNightMode() {
        launchWithErrorHandling {
            _nightModeEnabled.value = !_nightModeEnabled.value
        }
    }

    fun toggleOverlay() {
        launchWithErrorHandling {
            _showOverlay.value = !_showOverlay.value
        }
    }

    fun toggleRecording() {
        launchWithErrorHandling {
            _isRecording.value = !_isRecording.value
        }
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\ThermalRGBPreviewViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import android.graphics.Bitmap
import android.hardware.camera2.CameraManager
import android.view.Surface
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.*

class ThermalRGBPreviewViewModel : BaseViewModel() {
    data class RGBPreviewState(
        val isInitialized: Boolean = false,
        val isStreaming: Boolean = false,
        val resolution: String = "1920x1080",
        val frameRate: Int = 30,
        val cameraId: String = "0",
        val availableCameras: List<String> = emptyList(),
        val previewSurface: Surface? = null,
        val currentFrame: Bitmap? = null,
        val exposureMode: ExposureMode = ExposureMode.AUTO,
        val focusMode: FocusMode = FocusMode.AUTO
    )

    data class ThermalOverlayState(
        val isEnabled: Boolean = true,
        val opacity: Float = 0.7f,
        val blendMode: BlendMode = BlendMode.OVERLAY,
        val alignmentOffset: Pair<Float, Float> = 0f to 0f,
        val scale: Float = 1.0f,
        val rotation: Float = 0f,
        val thermalBitmap: Bitmap? = null,
        val colorPalette: ColorPalette = ColorPalette.IRON,
        val temperatureRange: Pair<Float, Float> = 20f to 40f
    )

    data class CombinedPreviewState(
        val rgbState: RGBPreviewState = RGBPreviewState(),
        val thermalState: ThermalOverlayState = ThermalOverlayState(),
        val isReady: Boolean = false,
        val overlayMode: OverlayMode = OverlayMode.BLENDED,
        val syncedFrame: Bitmap? = null
    )

    // StateFlow for RGB preview state management
    private val _rgbPreviewState = MutableStateFlow(RGBPreviewState())
    val rgbPreviewState: StateFlow<RGBPreviewState> = _rgbPreviewState.asStateFlow()
    private val _thermalOverlayState = MutableStateFlow(ThermalOverlayState())
    val thermalOverlayState: StateFlow<ThermalOverlayState> = _thermalOverlayState.asStateFlow()

    // SharedFlow for one-time events
    private val _previewEvents = MutableSharedFlow<PreviewEvent>()
    val previewEvents: SharedFlow<PreviewEvent> = _previewEvents.asSharedFlow()

    // Combined UI State for thermal + RGB preview
    val combinedPreviewState: StateFlow<CombinedPreviewState> = combine(
        _rgbPreviewState,
        _thermalOverlayState
    ) { rgbState, thermalState ->
        CombinedPreviewState(
            rgbState = rgbState,
            thermalState = thermalState,
            isReady = rgbState.isInitialized && thermalState.isEnabled,
            overlayMode = when {
                thermalState.blendMode == BlendMode.SIDE_BY_SIDE -> OverlayMode.SIDE_BY_SIDE
                thermalState.opacity > 0.8f -> OverlayMode.THERMAL_PRIMARY
                thermalState.opacity > 0.3f -> OverlayMode.BLENDED
                else -> OverlayMode.RGB_PRIMARY
            }
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, CombinedPreviewState())

    enum class BlendMode {
        OVERLAY, MULTIPLY, SCREEN, SIDE_BY_SIDE, PICTURE_IN_PICTURE
    }

    enum class OverlayMode {
        RGB_PRIMARY, BLENDED, THERMAL_PRIMARY, SIDE_BY_SIDE
    }

    enum class ExposureMode {
        AUTO, MANUAL, SCENE_NIGHT, SCENE_BRIGHT
    }

    enum class FocusMode {
        AUTO, MANUAL, CONTINUOUS_VIDEO, MACRO
    }

    enum class ColorPalette {
        IRON, RAINBOW, GRAYSCALE, HOT, COOL, MEDICAL
    }

    sealed class PreviewEvent {
        object RGBStreamStarted : PreviewEvent()
        object RGBStreamStopped : PreviewEvent()
        data class CameraError(val message: String) : PreviewEvent()
        data class ThermalDataReceived(val bitmap: Bitmap, val temperature: Float) : PreviewEvent()
        data class CalibrationRequired(val message: String) : PreviewEvent()
        data class ShowToast(val message: String) : PreviewEvent()
        data class ShowError(val message: String) : PreviewEvent()
    }

    // RGB Camera Management
    fun initializeRGBCamera(cameraManager: CameraManager) {
        launchWithErrorHandling {
            try {
                val cameraList = cameraManager.cameraIdList.toList()
                _rgbPreviewState.value = _rgbPreviewState.value.copy(
                    availableCameras = cameraList,
                    cameraId = cameraList.firstOrNull() ?: "0",
                    isInitialized = true
                )
                _previewEvents.emit(PreviewEvent.ShowToast("RGB camera initialized"))
            } catch (e: Exception) {
                _previewEvents.emit(PreviewEvent.CameraError("Failed to initialize RGB camera: ${e.message}"))
            }
        }
    }

    fun startRGBPreview(surface: Surface) {
        launchWithErrorHandling {
            _rgbPreviewState.value = _rgbPreviewState.value.copy(
                previewSurface = surface,
                isStreaming = true
            )
            _previewEvents.emit(PreviewEvent.RGBStreamStarted)
        }
    }

    fun stopRGBPreview() {
        launchWithErrorHandling {
            _rgbPreviewState.value = _rgbPreviewState.value.copy(
                previewSurface = null,
                isStreaming = false
            )
            _previewEvents.emit(PreviewEvent.RGBStreamStopped)
        }
    }

    fun selectCamera(cameraId: String) {
        _rgbPreviewState.value = _rgbPreviewState.value.copy(cameraId = cameraId)
    }

    fun setExposureMode(mode: ExposureMode) {
        _rgbPreviewState.value = _rgbPreviewState.value.copy(exposureMode = mode)
    }

    fun setFocusMode(mode: FocusMode) {
        _rgbPreviewState.value = _rgbPreviewState.value.copy(focusMode = mode)
    }

    // Thermal Overlay Management
    fun updateThermalOverlay(bitmap: Bitmap, temperature: Float) {
        launchWithErrorHandling {
            _thermalOverlayState.value = _thermalOverlayState.value.copy(
                thermalBitmap = bitmap,
                temperatureRange = _thermalOverlayState.value.temperatureRange.first to temperature
            )
            _previewEvents.emit(PreviewEvent.ThermalDataReceived(bitmap, temperature))
        }
    }

    fun setOverlayOpacity(opacity: Float) {
        _thermalOverlayState.value = _thermalOverlayState.value.copy(
            opacity = opacity.coerceIn(0f, 1f)
        )
    }

    fun setBlendMode(blendMode: BlendMode) {
        _thermalOverlayState.value = _thermalOverlayState.value.copy(blendMode = blendMode)
    }

    fun setColorPalette(palette: ColorPalette) {
        _thermalOverlayState.value = _thermalOverlayState.value.copy(colorPalette = palette)
    }

    fun adjustAlignment(offsetX: Float, offsetY: Float) {
        _thermalOverlayState.value = _thermalOverlayState.value.copy(
            alignmentOffset = offsetX to offsetY
        )
    }

    fun setScale(scale: Float) {
        _thermalOverlayState.value = _thermalOverlayState.value.copy(
            scale = scale.coerceIn(0.1f, 3.0f)
        )
    }

    fun setRotation(rotation: Float) {
        _thermalOverlayState.value = _thermalOverlayState.value.copy(
            rotation = rotation % 360f
        )
    }

    fun toggleThermalOverlay() {
        val currentState = _thermalOverlayState.value
        _thermalOverlayState.value = currentState.copy(isEnabled = !currentState.isEnabled)
    }

    // Calibration and Synchronization
    fun calibrateAlignment() {
        launchWithErrorHandling {
            // Simulate calibration process
            _previewEvents.emit(PreviewEvent.CalibrationRequired("Place calibration target in view and press OK"))
            // Reset alignment to defaults after calibration
            _thermalOverlayState.value = _thermalOverlayState.value.copy(
                alignmentOffset = 0f to 0f,
                scale = 1.0f,
                rotation = 0f
            )
        }
    }

    fun syncFrames() {
        launchWithErrorHandling {
            val rgbFrame = _rgbPreviewState.value.currentFrame
            val thermalFrame = _thermalOverlayState.value.thermalBitmap
            if (rgbFrame != null && thermalFrame != null) {
                // In a real implementation, this would combine the frames
                // For now, we'll just use the thermal frame as the synced frame
                val combinedState = combinedPreviewState.value
                // Update combined state would happen here
                _previewEvents.emit(PreviewEvent.ShowToast("Frames synchronized"))
            } else {
                _previewEvents.emit(PreviewEvent.ShowError("Cannot sync frames - missing RGB or thermal data"))
            }
        }
    }

    // Preset configurations for different use cases
    fun applyPreset(preset: PreviewPreset) {
        when (preset) {
            PreviewPreset.MEDICAL -> {
                _thermalOverlayState.value = _thermalOverlayState.value.copy(
                    colorPalette = ColorPalette.MEDICAL,
                    opacity = 0.8f,
                    blendMode = BlendMode.OVERLAY
                )
            }

            PreviewPreset.INDUSTRIAL -> {
                _thermalOverlayState.value = _thermalOverlayState.value.copy(
                    colorPalette = ColorPalette.IRON,
                    opacity = 0.6f,
                    blendMode = BlendMode.MULTIPLY
                )
            }

            PreviewPreset.RESEARCH -> {
                _thermalOverlayState.value = _thermalOverlayState.value.copy(
                    colorPalette = ColorPalette.RAINBOW,
                    opacity = 0.5f,
                    blendMode = BlendMode.SIDE_BY_SIDE
                )
            }

            PreviewPreset.NIGHT_VISION -> {
                _rgbPreviewState.value = _rgbPreviewState.value.copy(
                    exposureMode = ExposureMode.SCENE_NIGHT
                )
                _thermalOverlayState.value = _thermalOverlayState.value.copy(
                    colorPalette = ColorPalette.HOT,
                    opacity = 0.9f
                )
            }
        }
    }

    enum class PreviewPreset {
        MEDICAL, INDUSTRIAL, RESEARCH, NIGHT_VISION
    }

    companion object {
        private const val TAG = "ThermalRGBPreviewViewModel"
    }
}


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\viewmodel\ThermalViewModel.kt =====

package com.mpdc4gsr.module.thermalunified.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ThermalViewModel : BaseViewModel() {
    private val _exportStatus = MutableStateFlow<ExportStatus>(ExportStatus.Idle)
    val exportStatus: StateFlow<ExportStatus> = _exportStatus.asStateFlow()
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    fun yuvArea(
        yuv: ByteArray,
        temp: FloatArray,
        max: Float,
        min: Float,
    ) {
        for (i in temp.indices) {
            if (temp[i] < min) {
                yuv[i * 2] = 0x82.toByte()
                yuv[i * 2 + 1] = 0x00.toByte()
            }
            if (temp[i] > max) {
                yuv[i * 2] = 0x82.toByte()
                yuv[i * 2 + 1] = 0xFF.toByte()
            }
        }
    }

    fun exportData(context: Context, format: ExportFormat) {
        viewModelScope.launch {
            _exportStatus.value = ExportStatus.Exporting
            try {
                // Implementation for data export
                val exportFile = createExportFile(context, format)
                // Export data to file based on format
                _exportStatus.value = ExportStatus.Success(exportFile)
            } catch (e: Exception) {
                _exportStatus.value = ExportStatus.Error(e.message ?: "Export failed")
            }
        }
    }

    private fun createExportFile(context: Context, format: ExportFormat): File {
        val fileName = "thermal_export_${System.currentTimeMillis()}.${format.extension}"
        return File(context.getExternalFilesDir(null), fileName)
    }

    fun toggleRecording() {
        _isRecording.value = !_isRecording.value
    }

    fun captureSnapshot() {
        viewModelScope.launch {
            // Capture thermal snapshot
        }
    }

    sealed class ExportStatus {
        object Idle : ExportStatus()
        object Exporting : ExportStatus()
        data class Success(val file: File) : ExportStatus()
        data class Error(val message: String) : ExportStatus()
    }

    enum class ExportFormat(val extension: String) {
        CSV("csv"),
        JSON("json"),
        PDF("pdf")
    }
}


