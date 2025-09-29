package com.mpdc4gsr.module.thermalunified.activity

import android.graphics.Color
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.blankj.utilcode.util.SizeUtils
import com.mpdc4gsr.libunified.app.bean.tools.ThermalBean
import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.db.AppDatabase
import com.mpdc4gsr.libunified.app.db.entity.ThermalEntity
import com.mpdc4gsr.libunified.app.ktbase.BaseScreenActivity
import com.mpdc4gsr.libunified.app.tools.NumberTools
import com.mpdc4gsr.libunified.app.tools.TimeTool
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
import com.mpdc4gsr.module.thermalunified.adapter.SettingCheckAdapter
import com.mpdc4gsr.module.thermalunified.adapter.SettingTimeAdapter
import com.mpdc4gsr.module.thermalunified.chart.MyValueFormatter
import com.mpdc4gsr.module.thermalunified.view.MyMarkerView
import com.mpdc4gsr.module.thermalunified.viewmodel.LogViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MonitorChartActivity : BaseScreenActivity(), View.OnClickListener, OnChartValueSelectedListener {
    private val viewModel: LogViewModel by viewModels()

    private val timeAdapter: SettingTimeAdapter by lazy { SettingTimeAdapter(this) }
    private val adapter: SettingCheckAdapter by lazy { SettingCheckAdapter(this) }

    private var selectDuration = 1
    private var selectType = 1
    private var selectIndex: ArrayList<Int> = arrayListOf()
    private val bean = ThermalBean()
    private var selectTimeType = 1
    private var latestTime = 0L
    private var startMonitor = false

    private lateinit var chart: LineChart

    override fun initContentView() = R.layout.activity_monitor_chart

    override fun initView() {

        val toolbar =
            findViewById<androidx.appcompat.widget.Toolbar>(com.mpdc4gsr.libunified.R.id.toolbar_lay)
        toolbar?.title = getString(R.string.main_thermal_motion)

        selectType = intent.getIntExtra("type", 3)
        selectIndex = intent.getIntegerArrayListExtra("select")!!
        Log.w("123", "selectType:$selectType")
        Log.w("123", "selectIndex:${selectIndex.joinToString()}")

        type =
            when (selectType) {
                1 -> "point"
                2 -> "line"
                else -> "fence"
            }
        chart = findViewById(R.id.mp_chart_view)
        initChart()
        initRecycler()
        viewModel.resultLiveData.observe(this) {

            Log.w("123", "查询到历史数据:${it.dataList.size}")
            resultVol(it)
        }
        lifecycleScope.launch {
            delay(300)
            onIrVideoStart()
        }
    }

    override fun initData() {
    }

    // onResume and onPause methods are now handled by BaseScreenActivity
    // This eliminates the duplicate lifecycle pattern found in 7+ activities

    override fun onDestroy() {
        super.onDestroy()
        onIrVideoStop()
    }

    override fun onClick(v: View?) {
        when (v) {
        }
    }

    private fun initRecycler() {
        val monitorChartTimeRecycler =
            findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.monitor_chart_time_recycler)
        val monitorChartSettingRecycler =
            findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.monitor_chart_setting_recycler)

        monitorChartTimeRecycler.layoutManager = GridLayoutManager(this, 4)
        monitorChartTimeRecycler.adapter = timeAdapter
        monitorChartSettingRecycler.layoutManager = GridLayoutManager(this, 3)
        monitorChartSettingRecycler.adapter = adapter

        timeAdapter.listener =
            object : SettingTimeAdapter.OnItemClickListener {
                override fun onClick(
                    index: Int,
                    timeType: Int,
                ) {
                    selectTimeType = timeType
                    chart.highlightValue(null)
                    latestTime = 0L
                    showLoadingDialog()
                    lifecycleScope.launch {
                        delay(500)
                        queryLog(2)
                    }
                }
            }

        adapter.listener =
            object : SettingCheckAdapter.OnItemClickListener {
                override fun onClick(
                    index: Int,
                    time: Int,
                ) {
                    if (recordTask != null && recordTask!!.isActive) {
                        recordTask!!.cancel()
                        recordTask = null
                    }

                    Log.w("123", "select:$time")
                    adapter.setCheck(index)
                    timeMillis = time * 1000L
                    pointIndex = startIndex - defaultCount
                    recordThermal()
                }
            }
    }

    val defaultCount = 20
    val startIndex = 0f
    var pointIndex = startIndex - defaultCount

    var mIsIrVideoStart = false

    var rotateType = 3

    private fun onIrVideoStart() {


    }

    private fun onIrVideoStop() {


    }

    var isRecord = false
    var type = ""
    var timeMillis = 1000L
    var canUpdate = false
    var recordTask: Job? = null
    var thermalId = TimeTool.showDateSecond()
    var startTime = 0L

    private fun recordThermal() {
        recordTask =
            lifecycleScope.launch(Dispatchers.IO) {
                isRecord = true
                startTime = System.currentTimeMillis()
                var time = 0L
                while (isRecord) {
                    if (canUpdate) {
                        val entity = ThermalEntity()
                        entity.userId = SharedManager.getUserId()
                        entity.thermalId = thermalId
                        entity.thermal = NumberTools.to02f(bean.centerTemp)
                        entity.thermalMax = NumberTools.to02f(bean.maxTemp)
                        entity.thermalMin = NumberTools.to02f(bean.minTemp)
                        entity.type = type
                        entity.startTime = startTime
                        entity.createTime = System.currentTimeMillis()
                        AppDatabase.getInstance().thermalDao().insert(entity)
                        time++
                        launch(Dispatchers.Main) {
                            updateChart()
                        }
                        delay(timeMillis)
                    } else {
                        Log.w("123", "当前不可更新")
                    }
                }
                Log.w("123", "停止记录, 数据量:$time")
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

        xAxis.setLabelCount(6, false)
        val leftAxis = chart.axisLeft
        leftAxis.textSize = 9f
        leftAxis.textColor = textColor
        leftAxis.setDrawGridLines(true)
        leftAxis.setLabelCount(6, true)
        val rightAxis = chart.axisRight
        rightAxis.isEnabled = false
        chart.zoom(100f, 1f, chart.xChartMax, 0f)
        selectDuration = adapter.selectTime
        startTime = System.currentTimeMillis()
        canUpdate = true
    }

    private fun updateChart() {
        ++pointIndex
        when (selectTimeType) {
            1 -> {

                addPointToChart(bean)
            }

            2 -> {

                val addTime = 2 * 60 * 1000L
                if (bean.createTime > TimeTool.timeToMinute(latestTime, 2) + addTime) {
                    queryLog(3)
                }
            }

            3 -> {

                val addTime = 2 * 60 * 60 * 1000L
                if (bean.createTime > TimeTool.timeToMinute(latestTime, 3) + addTime) {
                    queryLog(3)
                }
            }

            4 -> {

                val addTime = 2 * 24 * 60 * 60 * 1000L
                if (bean.createTime > TimeTool.timeToMinute(latestTime, 4) + addTime) {
                    queryLog(3)
                }
            }
        }
    }

    private fun addPointToChart(bean: ThermalBean) {
        synchronized(chart) {
            try {
                if (bean.createTime == 0L) {
                    Log.w("123", "createTime = 0L, bean:$bean")
                    return
                }
                val data = ThermalEntity()
                data.thermalMax = bean.maxTemp
                data.thermalMin = bean.minTemp
                data.thermal = bean.centerTemp
                data.createTime = bean.createTime
                val lineData: LineData = chart.data
                var volDataSet = lineData.getDataSetByIndex(0)
                if (volDataSet == null) {
                    startTime = data.createTime
                    Log.w("123", "设置初始时间startTime:$startTime")
                    chart.xAxis.valueFormatter = MyValueFormatter(startTime = startTime)
                }
                val x = (data.createTime - startTime).toFloat()
                when (type) {
                    "point" -> {
                        if (volDataSet == null) {
                            volDataSet = createSet("green")
                            lineData.addDataSet(volDataSet)
                            Log.w("123", "volDataSet.entryCount:${volDataSet.entryCount}")
                        }
                        val entity = Entry(x, data.thermal)
                        entity.data = data
                        volDataSet.addEntry(entity)
                        Log.w("123", "添加一个数据:$entity")
                    }

                    "line" -> {

                        if (volDataSet == null) {
                            volDataSet = createSet("red")
                            lineData.addDataSet(volDataSet)
                            Log.w("123", "volDataSet.entryCount:${volDataSet.entryCount}")
                        }
                        val entity = Entry(x, data.thermalMax)
                        entity.data = data
                        volDataSet.addEntry(entity)

                        var secondDataSet = lineData.getDataSetByIndex(1)
                        if (secondDataSet == null) {
                            secondDataSet = createSet("blue")
                            lineData.addDataSet(secondDataSet)
                        }
                        val secondEntity = Entry(x, data.thermalMin)
                        secondEntity.data = data
                        secondDataSet.addEntry(secondEntity)
                    }

                    else -> {

                        if (volDataSet == null) {
                            volDataSet = createSet("red")
                            lineData.addDataSet(volDataSet)
                        }
                        val entity = Entry(x, data.thermalMax)
                        entity.data = data
                        volDataSet.addEntry(entity)

                        var secondDataSet = lineData.getDataSetByIndex(1)
                        if (secondDataSet == null) {
                            secondDataSet = createSet("blue")
                            lineData.addDataSet(secondDataSet)
                        }
                        val secondEntity = Entry(x, data.thermalMin)
                        secondEntity.data = data
                        secondDataSet.addEntry(secondEntity)
                    }
                }

                lineData.notifyDataChanged()
                chart.notifyDataSetChanged()
                chart.setVisibleXRangeMinimum(getMinimum())
                chart.setVisibleXRangeMaximum(getMaximum())
                chart.xAxis.setLabelCount(
                    getLabCount(volDataSet.entryCount),
                    false
                )
                chart.moveViewToX(chart.xChartMax)
                if (volDataSet.entryCount == 20) {
                    chart.zoom(100f, 1f, chart.xChartMax, 0f)
                }
                return@synchronized
            } catch (e: Exception) {
                Log.e("123", "添加数据时异常:${e.message}")
                return@synchronized
            }
        }
    }

    private val fillColor by lazy { ContextCompat.getDrawable(this, R.drawable.bg_chart_fill2) }
    private val lineRed by lazy {
        ContextCompat.getColor(
            this,
            com.mpdc4gsr.libunified.R.color.chart_line_max
        )
    }
    private val lineBlue by lazy {
        ContextCompat.getColor(
            this,
            com.mpdc4gsr.libunified.R.color.chart_line_min
        )
    }
    private val lineGreen by lazy {
        ContextCompat.getColor(
            this,
            com.mpdc4gsr.libunified.R.color.chart_line_center
        )
    }
    private val whiteColors by lazy {
        ContextCompat.getColor(
            this,
            com.mpdc4gsr.libunified.R.color.circle_white
        )
    }
    private val textColor by lazy {
        ContextCompat.getColor(
            this,
            com.mpdc4gsr.libunified.R.color.chart_text
        )
    }

    private fun createSet(label: String): LineDataSet {
        val set = LineDataSet(null, label)

        set.mode = LineDataSet.Mode.CUBIC_BEZIER
        set.setDrawFilled(false)

        set.axisDependency = YAxis.AxisDependency.LEFT

        when (label) {
            "red" -> {
                set.color = lineRed
                set.circleHoleColor = lineRed
            }

            "blue" -> {
                set.color = lineBlue
                set.circleHoleColor = lineBlue
            }

            else -> {
                set.color = lineGreen
                set.circleHoleColor = lineGreen
            }
        }

        set.setCircleColor(whiteColors)
        set.circleHoleRadius = 4f
        set.circleRadius = 5f
        set.valueTextColor = Color.WHITE
        set.lineWidth = 2f
        set.fillAlpha = 200
        set.valueTextSize = 10f
        set.setDrawValues(false)
        set.isHighlightEnabled = true
        set.setDrawHorizontalHighlightIndicator(false)
        set.enableDashedHighlightLine(8f, 8f, 0f)
        return set
    }


    private fun queryLog(action: Int) {
        startMonitor = false
        lifecycleScope.launch(Dispatchers.IO) {


            viewModel.queryLogThermals(selectTimeType = selectTimeType, action = action)
        }
    }

    private fun resultVol(bean: LogViewModel.ChartList) {
        dismissLoadingDialog()
        if (selectTimeType != 1 && bean.dataList.size > 0) {
            val logTime = TimeTool.showDateType(bean.dataList.last().createTime, selectTimeType)
            val nowTime = TimeTool.showDateType(System.currentTimeMillis(), selectTimeType)
            if (TextUtils.equals(logTime, nowTime)) {

                bean.dataList.removeLast()
            }
        }

        if (latestTime == 0L) {

            addEntity(bean.dataList)
        } else if (bean.dataList.size > 0 && latestTime < bean.dataList.last().createTime) {

            addEntity(bean.dataList)
        }
    }

    private fun addEntity(data: ArrayList<ThermalEntity>) {
        clearEntity(data.size == 0)
        if (data.size == 0) {
            return
        }
        latestTime = data[data.size - 1].createTime
        startTime = data[0].createTime
        chart.xAxis.valueFormatter = MyValueFormatter(startTime = startTime)
        val lineData: LineData = chart.data
        var volDataSet = lineData.getDataSetByIndex(0)
        if (volDataSet == null) {
            volDataSet = createSet("vol")
            lineData.addDataSet(volDataSet)
        }
        chart.xAxis.valueFormatter = MyValueFormatter(startTime = startTime, type = selectTimeType)
        val mv = MyMarkerView(this, R.layout.marker_lay)
        mv.chartView = chart
        chart.marker = mv
        data.forEach {
            val x = (it.createTime - startTime).toFloat()
            when (type) {
                "point" -> {
                    if (volDataSet == null) {
                        volDataSet = createSet("green")
                        lineData.addDataSet(volDataSet)
                        Log.w("123", "volDataSet.entryCount:${volDataSet.entryCount}")
                    }
                    val entity = Entry(x, it.thermal)
                    entity.data = it
                    volDataSet.addEntry(entity)
                    Log.w("123", "添加一个数据:$entity")
                }

                "line" -> {

                    if (volDataSet == null) {
                        volDataSet = createSet("red")
                        lineData.addDataSet(volDataSet)
                        Log.w("123", "volDataSet.entryCount:${volDataSet.entryCount}")
                    }
                    val entity = Entry(x, it.thermalMax)
                    entity.data = it
                    volDataSet.addEntry(entity)

                    var secondDataSet = lineData.getDataSetByIndex(1)
                    if (secondDataSet == null) {
                        secondDataSet = createSet("blue")
                        lineData.addDataSet(secondDataSet)
                    }
                    val secondEntity = Entry(x, it.thermalMin)
                    secondEntity.data = it
                    secondDataSet.addEntry(secondEntity)
                }

                else -> {

                    if (volDataSet == null) {
                        volDataSet = createSet("red")
                        lineData.addDataSet(volDataSet)
                    }
                    val entity = Entry(x, it.thermalMax)
                    entity.data = it
                    volDataSet.addEntry(entity)

                    var secondDataSet = lineData.getDataSetByIndex(1)
                    if (secondDataSet == null) {
                        secondDataSet = createSet("blue")
                        lineData.addDataSet(secondDataSet)
                    }
                    val secondEntity = Entry(x, it.thermalMax)
                    secondEntity.data = it
                    secondDataSet.addEntry(secondEntity)
                }
            }
        }
        Log.w("123", "曲线数据:${volDataSet.entryCount}个")
        lineData.notifyDataChanged()
        chart.notifyDataSetChanged()
        chart.setVisibleXRangeMinimum(getMinimum())
        chart.setVisibleXRangeMaximum(getMaximum())
        Log.i(
            "123",
            "list moveViewToX:${chart.xChartMax}, chart.highestVisibleX:${chart.highestVisibleX}",
        )
        chart.moveViewToX(chart.xChartMax)
        chart.xAxis.setLabelCount(getLabCount(volDataSet.entryCount), false)
        chart.zoom(100f, 1f, chart.xChartMax, 0f)
        startMonitor = true
    }

    private fun clearEntity(isEmpty: Boolean) {
        initChart()
        if (isEmpty) {
            chart.clear()
        } else {
            chart.clearValues()
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
            when (selectTimeType) {
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
}
