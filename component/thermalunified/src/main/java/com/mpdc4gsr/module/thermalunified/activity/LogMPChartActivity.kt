package com.mpdc4gsr.module.thermalunified.activity

import android.graphics.Color
import android.util.Log
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.blankj.utilcode.util.SizeUtils
import com.elvishew.xlog.XLog
import com.mpdc4gsr.libunified.app.db.entity.ThermalEntity
import com.mpdc4gsr.libunified.app.ktbase.BaseScreenActivity
import com.mpdc4gsr.libunified.app.tools.ToastTools
import com.mpdc4gsr.libunified.ui.charts.LineChart
import com.mpdc4gsr.libunified.ui.components.Legend
import com.mpdc4gsr.libunified.ui.components.XAxis
import com.mpdc4gsr.libunified.ui.components.YAxis
import com.mpdc4gsr.libunified.ui.data.Entry
import com.mpdc4gsr.libunified.ui.data.LineData
import com.mpdc4gsr.libunified.ui.data.LineDataSet
import com.mpdc4gsr.libunified.ui.highlight.Highlight
import com.mpdc4gsr.libunified.ui.listener.OnChartValueSelectedListener
import com.mpdc4gsr.module.thermalunified.R
import com.mpdc4gsr.module.thermalunified.adapter.SettingTimeAdapter
import com.mpdc4gsr.module.thermalunified.chart.MyValueFormatter
import com.mpdc4gsr.module.thermalunified.view.MyMarkerView
import com.mpdc4gsr.module.thermalunified.viewmodel.LogViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class LogMPChartActivity : BaseScreenActivity(), OnChartValueSelectedListener {
    private val viewModel: LogViewModel by viewModels()

    private val adapter: SettingTimeAdapter by lazy { SettingTimeAdapter(this) }

    private lateinit var chart: LineChart
    private var selectType = 1

    override fun initContentView() = R.layout.activity_log_mp_chart

    override fun initView() {

        val toolbar =
            findViewById<androidx.appcompat.widget.Toolbar>(com.mpdc4gsr.libunified.R.id.toolbar_lay)
        toolbar?.title = getString(R.string.app_record)

        chart = findViewById(R.id.log_chart_time_chart)
        val recyclerView =
            findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.log_chart_time_recycler)
        recyclerView.layoutManager = GridLayoutManager(this, 4)
        recyclerView.adapter = adapter
        adapter.listener =
            object : SettingTimeAdapter.OnItemClickListener {
                override fun onClick(
                    index: Int,
                    time: Int,
                ) {

                    chart.highlightValue(null)
                    selectType = index + 1
                    queryLog()
                }
            }
        viewModel.resultLiveData.observe(this) {
            dismissLoadingDialog()
            try {
                initEntry(it.dataList)
            } catch (e: Exception) {
                XLog.e("刷新图表异常:${e.message}")
                ToastTools.showShort("图表异常，请重新加载")
            }
        }
        clearEntity(true)
    }

    override fun initData() {
        queryLog()
    }

    // onResume and onPause methods are now handled by BaseScreenActivity
    // This eliminates the duplicate lifecycle pattern found in 7+ activities

    private fun queryLog() {
        showLoadingDialog()

        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.queryLogVolsByStartTime(
                type = 1,
                selectTimeType = selectType,
            )
        }
    }

    private fun initChart() {
        chart.clear()
        chart.setOnChartValueSelectedListener(this)
        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setDrawGridBackground(false)
        chart.description = null
        chart.setBackgroundResource(com.mpdc4gsr.libunified.R.color.chart_bg)
        chart.setScaleEnabled(true)
        chart.setPinchZoom(false)
        chart.isDoubleTapToZoomEnabled = false
        chart.isScaleYEnabled = false
        chart.setExtraOffsets(
            0f,
            0f,
            SizeUtils.dp2px(8f).toFloat(),
            SizeUtils.dp2px(4f).toFloat(),
        )
        chart.setNoDataText(getString(R.string.lms_http_code998))
        chart.setNoDataTextColor(textColor)
        val mv = MyMarkerView(this, R.layout.marker_lay)
        mv.chartView = chart
        chart.marker = mv
        val data = LineData()
        data.setValueTextColor(textColor)
        chart.data = data
        val l = chart.legend
        l.form = Legend.LegendForm.CIRCLE
        l.textColor = textColor
        l.isEnabled = false
        val xAxis = chart.xAxis
        xAxis.textColor = textColor
        xAxis.setDrawGridLines(true)
        xAxis.setAvoidFirstLastClipping(true)
        xAxis.isEnabled = true
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.axisLineColor = textColor
        xAxis.granularity = 1f
        xAxis.isGranularityEnabled = true
        xAxis.textSize = 9f
        val leftAxis = chart.axisLeft
        leftAxis.textSize = 9f
        leftAxis.textColor = textColor
        leftAxis.setDrawGridLines(true)
        leftAxis.setLabelCount(6, false)
        val rightAxis = chart.axisRight
        rightAxis.isEnabled = false

    }

    private val bgChartColors =
        intArrayOf(
            R.drawable.bg_chart_fill,
            R.drawable.bg_chart_fill2,
            R.drawable.bg_chart_fill3,
        )
    private val lineChartColors =
        intArrayOf(
            com.mpdc4gsr.libunified.R.color.chart_line_max,
            com.mpdc4gsr.libunified.R.color.chart_line_min,
            com.mpdc4gsr.libunified.R.color.chart_line_center,
        )
    private val textColor by lazy {
        ContextCompat.getColor(
            this,
            com.mpdc4gsr.libunified.R.color.chart_text
        )
    }

    private fun createSet(
        index: Int,
        label: String,
    ): LineDataSet {
        val set = LineDataSet(null, label)

        set.mode = LineDataSet.Mode.LINEAR
        set.setDrawFilled(false)
        set.fillDrawable = ContextCompat.getDrawable(this, bgChartColors[index])
        set.axisDependency = YAxis.AxisDependency.LEFT
        set.color = ContextCompat.getColor(this, lineChartColors[index])
        set.setCircleColor(ContextCompat.getColor(this, com.mpdc4gsr.libunified.R.color.white))


        set.valueTextColor = Color.WHITE
        set.lineWidth = 2f
        set.circleRadius = 1f
        set.setCircleColor(ContextCompat.getColor(this, lineChartColors[index]))

        set.fillAlpha = 200
        set.valueTextSize = 10f
        set.setDrawValues(false)
        return set
    }

    private fun initEntry(data: ArrayList<ThermalEntity>) {
        synchronized(chart) {
            lifecycleScope.launch(Dispatchers.IO) {
                clearEntity(data.size == 0)
                if (data.size == 0) {
                    return@launch
                }
                Log.i("chart", "update chart start")
                val lineData: LineData? = chart.data
                if (lineData != null) {
                    Log.w(
                        "123",
                        "时间区间:${(data.last().createTime - data.first().createTime) / 1000}",
                    )
                    val startTime = data[0].createTime
                    Log.w("123", "设置初始时间startTime:$startTime")
                    chart.xAxis.valueFormatter =
                        MyValueFormatter(startTime = startTime, type = selectType)
                    XLog.w("chart init startTime:$startTime")
                    data[0].type = "default"
                    when (data[0].type) {
                        "point" -> {
                            var set = lineData.getDataSetByIndex(0)
                            if (set == null) {
                                set = createSet(2, "temp")
                                lineData.addDataSet(set)
                            }
                            data.forEach {
                                val x = (it.createTime - startTime).toFloat()
                                val entity = Entry(x, it.thermal)
                                entity.data = it
                                set.addEntry(entity)
                            }
                            XLog.w("DataSet:${set.entryCount}")
                        }

                        "line" -> {
                            var maxDataSet = lineData.getDataSetByIndex(0)
                            if (maxDataSet == null) {
                                maxDataSet = createSet(0, "line maxTemp")
                                lineData.addDataSet(maxDataSet)
                            }

                            var minDataSet = lineData.getDataSetByIndex(1)
                            if (minDataSet == null) {
                                minDataSet = createSet(1, "line minTemp")
                                lineData.addDataSet(minDataSet)
                            }
                            Log.w("123", "两条曲线")
                            data.forEach {
                                val x = (it.createTime - startTime).toFloat()

                                val entity = Entry(x, it.thermalMax)
                                entity.data = it
                                maxDataSet.addEntry(entity)

                                val entityMin = Entry(x, it.thermalMin)
                                entityMin.data = it
                                minDataSet.addEntry(entityMin)
                            }
                            XLog.w("DataSet:${maxDataSet.entryCount}")
                        }

                        else -> {

                            var maxTempDataSet = lineData.getDataSetByIndex(0)
                            if (maxTempDataSet == null) {
                                maxTempDataSet = createSet(0, "fence maxTemp")
                                lineData.addDataSet(maxTempDataSet)
                            }

                            var centerTempDataSet = lineData.getDataSetByIndex(1)
                            if (centerTempDataSet == null) {
                                centerTempDataSet = createSet(1, "fence minTemp")
                                lineData.addDataSet(centerTempDataSet)
                            }
                            data.forEach {
                                val x = (it.createTime - startTime).toFloat()

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
                    chart.notifyDataSetChanged()
                    chart.setVisibleXRangeMinimum(getMinimum())
                    chart.setVisibleXRangeMaximum(getMaximum())
                    chart.xAxis.setLabelCount(5, false)
                    chart.moveViewToX(chart.xChartMax)
                    chart.zoom(1f, 1f, chart.xChartMax, 0f)
                }
                Log.w("chart", "update chart finish")
            }
        }
    }

    override fun onValueSelected(
        e: Entry?,
        h: Highlight?,
    ) {
    }

    override fun onNothingSelected() {
    }

    private fun getLabCount(count: Int): Int {
        return when (count) {
            in 0..2 -> 1
            in 3..4 -> 2
            in 5..6 -> 3
            in 7..9 -> 4
            else -> 5
        }
    }

    private fun getMinimum(): Float {
        val min =
            when (selectType) {
                1 -> 1 * 10 * 1000f
                2 -> 10 * 60 * 1000f
                3 -> 10 * 60 * 60 * 1000f
                4 -> 10 * 24 * 60 * 60 * 1000f
                else -> 1 * 10 * 1000f
            }
        return min
    }

    private fun getMaximum(): Float {
        return getMinimum() * 50f
    }

    private fun clearEntity(isEmpty: Boolean) {
        initChart()
        if (isEmpty) {
            chart.clear()
        } else {
            chart.clearValues()
        }
    }
}
