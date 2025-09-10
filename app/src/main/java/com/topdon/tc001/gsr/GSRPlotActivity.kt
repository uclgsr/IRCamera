package com.topdon.tc001.gsr

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityGsrPlotBinding
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.topdon.lib.core.ktbase.BaseBindingActivity

/**
 * GSR Plot Activity
 * Advanced visualization of GSR data with multiple analysis views
 */
class GSRPlotActivity : BaseBindingActivity<ActivityGsrPlotBinding>() {
    
    private lateinit var plotData: GSRDataViewActivity.GSRPlotData
    
    override fun initContentLayoutId() = R.layout.activity_gsr_plot
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup toolbar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "GSR Data Analysis"
        }
        
        loadPlotData()
        setupCharts()
        displayStatistics()
    }
    
    private fun loadPlotData() {
        plotData = intent.getSerializableExtra("plot_data") as GSRDataViewActivity.GSRPlotData
    }
    
    private fun setupCharts() {
        setupGSRChart()
        setupPPGChart()
    }
    
    private fun setupGSRChart() {
        // Configure GSR chart
        binding.gsrChart.apply {
            description = Description().apply {
                text = "GSR (µS) over Time"
                textSize = 12f
            }
            
            // Configure X-axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = TimeFormatter()
                granularity = 1f
                labelCount = 6
            }
            
            // Configure Y-axis
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.LTGRAY
            }
            axisRight.isEnabled = false
            
            // Enable zoom and pan
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
        }
        
        // Create GSR data sets
        val gsrEntries = plotData.timestamps.mapIndexed { index, timestamp ->
            Entry(timestamp.toFloat(), plotData.gsrValues[index].toFloat())
        }
        
        val gsrMovingAvgEntries = plotData.timestamps.mapIndexed { index, timestamp ->
            Entry(timestamp.toFloat(), plotData.gsrMovingAverage[index].toFloat())
        }
        
        val gsrDataSet = LineDataSet(gsrEntries, "GSR Raw").apply {
            color = Color.BLUE
            setDrawCircles(false)
            lineWidth = 1.5f
            setDrawValues(false)
        }
        
        val gsrAvgDataSet = LineDataSet(gsrMovingAvgEntries, "GSR Moving Average").apply {
            color = Color.RED
            setDrawCircles(false)
            lineWidth = 2f
            setDrawValues(false)
        }
        
        // Set data to chart
        binding.gsrChart.data = LineData(gsrDataSet, gsrAvgDataSet)
        binding.gsrChart.invalidate()
    }
    
    private fun setupPPGChart() {
        // Configure PPG chart
        binding.ppgChart.apply {
            description = Description().apply {
                text = "PPG Signal over Time"
                textSize = 12f
            }
            
            // Configure X-axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = TimeFormatter()
                granularity = 1f
                labelCount = 6
            }
            
            // Configure Y-axis
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.LTGRAY
            }
            axisRight.isEnabled = false
            
            // Enable zoom and pan
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
        }
        
        // Create PPG data sets
        val ppgEntries = plotData.timestamps.mapIndexed { index, timestamp ->
            Entry(timestamp.toFloat(), plotData.ppgValues[index].toFloat())
        }
        
        val ppgMovingAvgEntries = plotData.timestamps.mapIndexed { index, timestamp ->
            Entry(timestamp.toFloat(), plotData.ppgMovingAverage[index].toFloat())
        }
        
        val ppgDataSet = LineDataSet(ppgEntries, "PPG Raw").apply {
            color = Color.GREEN
            setDrawCircles(false)
            lineWidth = 1.5f
            setDrawValues(false)
        }
        
        val ppgAvgDataSet = LineDataSet(ppgMovingAvgEntries, "PPG Moving Average").apply {
            color = Color.MAGENTA
            setDrawCircles(false)
            lineWidth = 2f
            setDrawValues(false)
        }
        
        // Set data to chart
        binding.ppgChart.data = LineData(ppgDataSet, ppgAvgDataSet)
        binding.ppgChart.invalidate()
    }
    
    private fun displayStatistics() {
        val metadata = plotData.metadata
        val stats = StringBuilder()
        
        stats.appendLine("📊 Recording Statistics")
        stats.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        stats.appendLine("📁 File: ${metadata.fileName}")
        stats.appendLine("⏱️ Duration: ${formatDuration(metadata.duration)}")
        stats.appendLine("📈 Data Points: ${metadata.dataPoints}")
        stats.appendLine("🔄 Sampling Rate: ${"%.1f".format(metadata.samplingRate)} Hz")
        stats.appendLine("")
        
        // GSR Statistics
        val gsrMean = plotData.gsrValues.average()
        val gsrStdDev = calculateStandardDeviation(plotData.gsrValues)
        val gsrMin = plotData.gsrValues.minOrNull() ?: 0.0
        val gsrMax = plotData.gsrValues.maxOrNull() ?: 0.0
        
        stats.appendLine("🧬 GSR Analysis")
        stats.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        stats.appendLine("Mean: ${"%.4f".format(gsrMean)} µS")
        stats.appendLine("Std Dev: ${"%.4f".format(gsrStdDev)} µS")
        stats.appendLine("Range: ${"%.4f".format(gsrMin)} - ${"%.4f".format(gsrMax)} µS")
        stats.appendLine("Variation: ${"%.2f".format((gsrStdDev/gsrMean)*100)}%")
        stats.appendLine("")
        
        // PPG Statistics
        val ppgMean = plotData.ppgValues.average()
        val ppgStdDev = calculateStandardDeviation(plotData.ppgValues)
        val ppgMin = plotData.ppgValues.minOrNull() ?: 0.0
        val ppgMax = plotData.ppgValues.maxOrNull() ?: 0.0
        
        stats.appendLine("❤️ PPG Analysis")
        stats.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        stats.appendLine("Mean: ${"%.1f".format(ppgMean)}")
        stats.appendLine("Std Dev: ${"%.1f".format(ppgStdDev)}")
        stats.appendLine("Range: ${"%.0f".format(ppgMin)} - ${"%.0f".format(ppgMax)}")
        stats.appendLine("")
        
        // Events Analysis
        stats.appendLine("🎯 Events Detected")
        stats.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        val increases = plotData.gsrEvents.count { it.type == "INCREASE" }
        val decreases = plotData.gsrEvents.count { it.type == "DECREASE" }
        stats.appendLine("GSR Increases: $increases")
        stats.appendLine("GSR Decreases: $decreases")
        stats.appendLine("Total Events: ${plotData.gsrEvents.size}")
        
        binding.statsTextView.text = stats.toString()
    }
    
    private fun calculateStandardDeviation(values: List<Double>): Double {
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }
    
    private fun formatDuration(seconds: Double): String {
        val minutes = (seconds / 60).toInt()
        val remainingSeconds = (seconds % 60).toInt()
        return "${minutes}:${"%02d".format(remainingSeconds)}"
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    /**
     * Custom formatter for time axis
     */
    private class TimeFormatter : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val seconds = value.toInt()
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            return "${minutes}:${"%02d".format(remainingSeconds)}"
        }
    }
}