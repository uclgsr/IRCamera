package mpdc4gsr.sensors.gsr

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityGsrDataViewBinding
import com.google.gson.Gson
import com.opencsv.CSVWriter
import com.topdon.lib.core.ktbase.BaseBindingActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GSRDataViewActivity : BaseBindingActivity<ActivityGsrDataViewBinding>() {
    companion object {
        private const val EXTRA_FILE_PATH = "file_path"

        fun startActivity(
            context: Context,
            filePath: String,
        ) {
            val intent =
                Intent(context, GSRDataViewActivity::class.java).apply {
                    putExtra(EXTRA_FILE_PATH, filePath)
                }
            context.startActivity(intent)
        }
    }

    private lateinit var filePath: String
    private lateinit var file: File
    private lateinit var adapter: GSRDataRowAdapter
    private val dataRows = mutableListOf<GSRDataRow>()
    private val gsrDataPoints = mutableListOf<GSRDataPoint>()

    data class GSRDataRow(
        val timestamp: String,
        val gsrValue: Double,
        val resistance: Double,
        val conductance: Double,
        val rowNumber: Int,
    )

    data class GSRDataPoint(
        val timestamp: Long, // nanoseconds
        val gsrValue: Double, // microsiemens
        val gsrRaw: Int, // raw ADC value (0-4095)
        val resistance: Double, // kohms
        val ppgValue: Int, // raw PPG value
        val ppgRaw: Int = ppgValue, // alias for ppgValue
        val syncMarker: Boolean = false,
        val notes: String? = null,
    )

    override fun initContentLayoutId() = R.layout.activity_gsr_data_view

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        filePath = intent.getStringExtra(EXTRA_FILE_PATH) ?: ""
        file = File(filePath)

        if (!file.exists()) {
            finish()
            return
        }

        setupUI()
        loadGSRData()
    }

    private fun setupUI() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = file.name

        adapter = GSRDataRowAdapter(dataRows)
        binding.dataRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.dataRecyclerView.adapter = adapter

        val fileSize =
            if (file.length() >= 1024 * 1024) {
                "%.1f MB".format(file.length() / (1024.0 * 1024.0))
            } else {
                "%.1f KB".format(file.length() / 1024.0)
            }

        binding.fileInfoText.text =
            """
            File: ${file.name}
            Size: $fileSize
            Path: ${file.absolutePath}
            Modified: ${
                java.text.SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    java.util.Locale.getDefault()
                ).format(java.util.Date(file.lastModified()))
            }
            """.trimIndent()
    }

    private fun loadGSRData() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val lines = file.readLines()
                val headerLine = lines.firstOrNull() ?: ""
                val dataLines = lines.drop(1)

                val rows =
                    dataLines.mapIndexed { index, line ->
                        parseGSRDataRow(
                            line,
                            index + 2
                        ) // +2 because we skip header and 0-based index
                    }.filterNotNull()

                val statistics = calculateStatistics(rows)

                withContext(Dispatchers.Main) {
                    dataRows.clear()
                    dataRows.addAll(rows)
                    adapter.notifyDataSetChanged()

                    binding.statisticsText.text =
                        """
                        Total Samples: ${rows.size}
                        Duration: ${formatDuration((rows.size / 128).toLong())} (@ 128 Hz)
                        
                        GSR Statistics:
                        • Min: %.3f μS
                        • Max: %.3f μS  
                        • Mean: %.3f μS
                        • Std Dev: %.3f μS
                        
                        Resistance Statistics:
                        • Min: %.1f kΩ
                        • Max: %.1f kΩ
                        • Mean: %.1f kΩ
                        """.trimIndent().format(
                            statistics.gsrMin,
                            statistics.gsrMax,
                            statistics.gsrMean,
                            statistics.gsrStdDev,
                            statistics.resistanceMin / 1000,
                            statistics.resistanceMax / 1000,
                            statistics.resistanceMean / 1000,
                        )

                    loadGSRDataPoints()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.statisticsText.text = "Error loading GSR data: ${e.message}"
                }
            }
        }
    }

    private fun parseGSRDataRow(
        line: String,
        rowNumber: Int,
    ): GSRDataRow? {
        return try {
            val parts = line.split(",")
            if (parts.size >= 4) {
                GSRDataRow(
                    timestamp = parts[0].trim(),
                    gsrValue = parts[1].trim().toDouble(),
                    resistance = parts[2].trim().toDouble(),
                    conductance = parts[3].trim().toDouble(),
                    rowNumber = rowNumber,
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateStatistics(rows: List<GSRDataRow>): GSRStatistics {
        if (rows.isEmpty()) {
            return GSRStatistics(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }

        val gsrValues = rows.map { it.gsrValue }
        val resistanceValues = rows.map { it.resistance }

        val gsrMean = gsrValues.average()
        val resistanceMean = resistanceValues.average()

        val gsrVariance = gsrValues.map { (it - gsrMean) * (it - gsrMean) }.average()
        val gsrStdDev = kotlin.math.sqrt(gsrVariance)

        return GSRStatistics(
            gsrMin = gsrValues.minOrNull() ?: 0.0,
            gsrMax = gsrValues.maxOrNull() ?: 0.0,
            gsrMean = gsrMean,
            gsrStdDev = gsrStdDev,
            resistanceMin = resistanceValues.minOrNull() ?: 0.0,
            resistanceMax = resistanceValues.maxOrNull() ?: 0.0,
            resistanceMean = resistanceMean,
        )
    }

    data class GSRStatistics(
        val gsrMin: Double,
        val gsrMax: Double,
        val gsrMean: Double,
        val gsrStdDev: Double,
        val resistanceMin: Double,
        val resistanceMax: Double,
        val resistanceMean: Double,
    )

    private fun formatDuration(seconds: Long): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "%d:%02d".format(minutes, remainingSeconds)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.gsr_data_view_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }

            R.id.action_export -> {
                exportData()
                true
            }

            R.id.action_share -> {
                shareData()
                true
            }

            R.id.action_plot -> {
                plotData()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun exportData() {
        lifecycleScope.launch {
            try {
                val exportResult =
                    withContext(Dispatchers.IO) {
                        exportGSRDataToFormats()
                    }

                if (exportResult.isSuccess) {
                    showExportSuccessDialog(exportResult.getOrNull())
                } else {
                    showErrorDialog(
                        "Export Failed",
                        exportResult.exceptionOrNull()?.message ?: "Unknown error occurred"
                    )
                }
            } catch (e: Exception) {
                showErrorDialog("Export Error", "Failed to export data: ${e.message}")
            }
        }
    }

    private fun exportGSRDataToFormats(): Result<ExportResult> {
        return try {
            val fileName = file.nameWithoutExtension
            val exportDir =
                File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "GSR_Exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

            val exportedFiles = mutableListOf<File>()

            val enhancedCsvFile = File(exportDir, "${fileName}_enhanced_$timestamp.csv")
            exportEnhancedCSV(enhancedCsvFile)
            exportedFiles.add(enhancedCsvFile)

            val excelFile = File(exportDir, "${fileName}_excel_$timestamp.csv")
            exportExcelCompatibleCSV(excelFile)
            exportedFiles.add(excelFile)

            val jsonFile = File(exportDir, "${fileName}_data_$timestamp.json")
            exportJSONFormat(jsonFile)
            exportedFiles.add(jsonFile)

            val summaryFile = File(exportDir, "${fileName}_summary_$timestamp.txt")
            exportStatisticalSummary(summaryFile)
            exportedFiles.add(summaryFile)

            Result.success(ExportResult(exportedFiles, exportDir))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun exportEnhancedCSV(outputFile: File) {
        val writer = FileWriter(outputFile)
        val csvWriter = CSVWriter(writer)

        csvWriter.writeNext(arrayOf("# GSR Data Export"))
        csvWriter.writeNext(arrayOf("# Source File: ${file.name}"))
        csvWriter.writeNext(
            arrayOf(
                "# Export Date: ${
                    SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()
                    ).format(Date())
                }"
            )
        )
        csvWriter.writeNext(arrayOf("# Device: ${getDeviceInfo()}"))
        csvWriter.writeNext(arrayOf(""))

        csvWriter.writeNext(
            arrayOf(
                "timestamp_ns", "timestamp_ms", "timestamp_iso",
                "gsr_raw", "gsr_microsiemens", "gsr_normalized",
                "ppg_raw", "ppg_normalized",
                "quality_score", "sync_marker", "notes",
            ),
        )

        gsrDataPoints.forEachIndexed { index, dataPoint ->
            val timestampMs = dataPoint.timestamp / 1000000
            val timestampIso =
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(
                    Date(timestampMs)
                )

            val normalizedGSR = normalizeGSRValue(dataPoint.gsrValue.toFloat())
            val normalizedPPG = normalizePPGValue(dataPoint.ppgValue)
            val qualityScore = calculateDataQuality(dataPoint, index)

            csvWriter.writeNext(
                arrayOf(
                    dataPoint.timestamp.toString(),
                    timestampMs.toString(),
                    timestampIso,
                    dataPoint.gsrRaw.toString(),
                    "%.4f".format(dataPoint.gsrValue),
                    "%.4f".format(normalizedGSR),
                    dataPoint.ppgRaw.toString(),
                    "%.4f".format(normalizedPPG),
                    "%.2f".format(qualityScore),
                    if (dataPoint.syncMarker) "SYNC" else "",
                    dataPoint.notes ?: "",
                ),
            )
        }

        csvWriter.close()
    }

    private fun exportExcelCompatibleCSV(outputFile: File) {
        val writer = FileWriter(outputFile)
        val csvWriter = CSVWriter(writer)

        csvWriter.writeNext(
            arrayOf(
                "Date",
                "Time",
                "GSR_µS",
                "PPG",
                "Quality",
                "Duration_s",
            ),
        )

        gsrDataPoints.forEach { dataPoint ->
            val date = Date(dataPoint.timestamp / 1000000)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
            val durationSeconds =
                if (gsrDataPoints.isNotEmpty()) {
                    (dataPoint.timestamp - gsrDataPoints.first().timestamp) / 1000000000.0
                } else {
                    0.0
                }

            csvWriter.writeNext(
                arrayOf(
                    dateFormat.format(date),
                    timeFormat.format(date),
                    "%.4f".format(dataPoint.gsrValue),
                    dataPoint.ppgValue.toString(),
                    "%.1f".format(calculateDataQuality(dataPoint, 0)),
                    "%.3f".format(durationSeconds),
                ),
            )
        }

        csvWriter.close()
    }

    private fun exportJSONFormat(outputFile: File) {
        val jsonData = mutableMapOf<String, Any>()

        jsonData["metadata"] =
            mapOf(
                "sourceFile" to file.name,
                "exportDate" to SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    Locale.getDefault()
                ).format(Date()),
                "device" to getDeviceInfo(),
                "dataPoints" to gsrDataPoints.size,
                "duration" to calculateRecordingDuration(),
                "samplingRate" to calculateSamplingRate(),
            )

        jsonData["statistics"] = calculateStatistics()

        val dataArray =
            gsrDataPoints.map { dataPoint ->
                mapOf(
                    "timestamp" to dataPoint.timestamp,
                    "gsr" to
                            mapOf(
                                "raw" to dataPoint.gsrRaw,
                                "microsiemens" to dataPoint.gsrValue,
                                "normalized" to normalizeGSRValue(dataPoint.gsrValue.toFloat()),
                            ),
                    "ppg" to
                            mapOf(
                                "raw" to dataPoint.ppgRaw,
                                "value" to dataPoint.ppgValue,
                            ),
                    "syncMarker" to dataPoint.syncMarker,
                    "notes" to dataPoint.notes,
                )
            }
        jsonData["data"] = dataArray

        val gson = Gson()
        outputFile.writeText(gson.toJson(jsonData))
    }

    private fun exportStatisticalSummary(outputFile: File) {
        val summary = StringBuilder()
        val stats = calculateStatistics()

        summary.appendLine("GSR Data Statistical Summary")
        summary.appendLine("=" + "=".repeat(40))
        summary.appendLine("Source File: ${file.name}")
        summary.appendLine(
            "Export Date: ${
                SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()
                ).format(Date())
            }"
        )
        summary.appendLine("Device: ${getDeviceInfo()}")
        summary.appendLine("")

        summary.appendLine("Recording Information:")
        summary.appendLine("  Data Points: ${gsrDataPoints.size}")
        summary.appendLine("  Duration: %.2f seconds".format(calculateRecordingDuration()))
        summary.appendLine("  Sampling Rate: %.1f Hz".format(calculateSamplingRate()))
        summary.appendLine("")

        summary.appendLine("GSR Statistics:")
        summary.appendLine("  Mean: %.4f µS".format(stats["gsrMean"]))
        summary.appendLine("  Std Dev: %.4f µS".format(stats["gsrStdDev"]))
        summary.appendLine("  Min: %.4f µS".format(stats["gsrMin"]))
        summary.appendLine("  Max: %.4f µS".format(stats["gsrMax"]))
        summary.appendLine("  Range: %.4f µS".format(stats["gsrRange"]))
        summary.appendLine("")

        summary.appendLine("PPG Statistics:")
        summary.appendLine("  Mean: %.2f".format(stats["ppgMean"]))
        summary.appendLine("  Std Dev: %.2f".format(stats["ppgStdDev"]))
        summary.appendLine("  Min: %.2f".format(stats["ppgMin"]))
        summary.appendLine("  Max: %.2f".format(stats["ppgMax"]))
        summary.appendLine("")

        summary.appendLine("Data Quality:")
        summary.appendLine("  Average Quality Score: %.1f%".format(stats["averageQuality"]))
        summary.appendLine("  Sync Markers: ${stats["syncMarkers"]}")

        outputFile.writeText(summary.toString())
    }

    private data class ExportResult(
        val exportedFiles: List<File>,
        val exportDirectory: File,
    )

    private fun showExportSuccessDialog(result: ExportResult?) {
        result?.let { exportResult ->
            val message =
                """
                Data exported successfully!
                
                Files created:
                ${exportResult.exportedFiles.joinToString("\n") { "• ${it.name}" }}
                
                Location: ${exportResult.exportDirectory.absolutePath}
                """.trimIndent()

            AlertDialog.Builder(this)
                .setTitle("Export Complete")
                .setMessage(message)
                .setPositiveButton("Open Folder") { _, _ ->
                    openExportFolder(exportResult.exportDirectory)
                }
                .setNegativeButton("OK", null)
                .show()
        }
    }

    private fun openExportFolder(directory: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(android.net.Uri.fromFile(directory), "resource/folder")
            if (intent.resolveActivityInfo(packageManager, 0) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "No file manager found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open folder: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareData() {
        val shareIntent =
            Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "GSR Data from ${file.name}")
                putExtra(Intent.EXTRA_STREAM, android.net.Uri.fromFile(file))
                type = "text/csv"
            }
        startActivity(Intent.createChooser(shareIntent, "Share GSR Data"))
    }

    private fun plotData() {
        lifecycleScope.launch {
            try {

                val progressDialog =
                    createProgressDialog("Generating Plot", "Preparing GSR data visualization...")
                progressDialog.show()

                val plotData =
                    withContext(Dispatchers.Default) {
                        preparePlotData()
                    }

                progressDialog.dismiss()

                val intent =
                    Intent(this@GSRDataViewActivity, GSRPlotActivity::class.java).apply {
                        putExtra("plot_data", plotData)
                        putExtra("file_name", file.name)
                        putExtra("data_points", gsrDataPoints.size)
                    }
                startActivity(intent)
            } catch (e: Exception) {
                showErrorDialog("Plot Error", "Failed to generate plot: ${e.message}")
            }
        }
    }

    private fun preparePlotData(): GSRPlotData {

        val timestamps =
            gsrDataPoints.map { (it.timestamp - gsrDataPoints.first().timestamp) / 1000000.0 } // Convert to seconds
        val gsrValues = gsrDataPoints.map { it.gsrValue.toDouble() }
        val ppgValues = gsrDataPoints.map { it.ppgValue.toDouble() }

        val windowSize = maxOf(1, gsrDataPoints.size / 100) // 1% of data or minimum 1
        val gsrMovingAvg = calculateMovingAverage(gsrValues, windowSize)
        val ppgMovingAvg = calculateMovingAverage(ppgValues, windowSize)

        val gsrEvents = detectGSREvents(gsrValues, timestamps)

        val stats = calculateTimeWindowedStatistics(gsrValues, timestamps)

        return GSRPlotData(
            timestamps = timestamps,
            gsrValues = gsrValues,
            ppgValues = ppgValues,
            gsrMovingAverage = gsrMovingAvg,
            ppgMovingAverage = ppgMovingAvg,
            gsrEvents = gsrEvents,
            statistics = stats,
            metadata =
                PlotMetadata(
                    fileName = file.name,
                    duration = timestamps.lastOrNull() ?: 0.0,
                    samplingRate = calculateSamplingRate(),
                    dataPoints = gsrDataPoints.size,
                ),
        )
    }

    private fun calculateMovingAverage(
        values: List<Double>,
        windowSize: Int,
    ): List<Double> {
        if (windowSize <= 1) return values

        return values.mapIndexed { index, _ ->
            val start = maxOf(0, index - windowSize / 2)
            val end = minOf(values.size, index + windowSize / 2 + 1)
            val window = values.subList(start, end)
            window.sum() / window.size
        }
    }

    private fun detectGSREvents(
        gsrValues: List<Double>,
        timestamps: List<Double>,
    ): List<GSREvent> {
        val events = mutableListOf<GSREvent>()
        val threshold =
            gsrValues.let { values ->
                val mean = values.sum() / values.size
                val variance = values.map { (it - mean) * (it - mean) }.sum() / values.size
                kotlin.math.sqrt(variance) * 2.0 // 2 standard deviations
            }

        for (i in 1 until gsrValues.size) {
            val change = kotlin.math.abs(gsrValues[i] - gsrValues[i - 1])
            if (change > threshold) {
                val eventType = if (gsrValues[i] > gsrValues[i - 1]) "INCREASE" else "DECREASE"
                events.add(
                    GSREvent(
                        timestamp = timestamps[i],
                        type = eventType,
                        magnitude = change,
                        gsrValue = gsrValues[i],
                    ),
                )
            }
        }

        return events
    }

    private fun calculateTimeWindowedStatistics(
        values: List<Double>,
        timestamps: List<Double>,
    ): List<TimeWindowStats> {
        val windowDuration = 30.0 // 30-second windows
        val maxTime = timestamps.lastOrNull() ?: return emptyList()
        val stats = mutableListOf<TimeWindowStats>()

        var currentTime = 0.0
        while (currentTime < maxTime) {
            val windowEnd = currentTime + windowDuration
            val windowValues =
                values.filterIndexed { index, _ ->
                    timestamps[index] >= currentTime && timestamps[index] < windowEnd
                }

            if (windowValues.isNotEmpty()) {
                val mean = windowValues.sum() / windowValues.size
                val variance =
                    windowValues.map { (it - mean) * (it - mean) }.sum() / windowValues.size
                val stdDev = kotlin.math.sqrt(variance)

                stats.add(
                    TimeWindowStats(
                        startTime = currentTime,
                        endTime = windowEnd,
                        mean = mean,
                        stdDev = stdDev,
                        min = windowValues.minOrNull() ?: 0.0,
                        max = windowValues.maxOrNull() ?: 0.0,
                        count = windowValues.size,
                    ),
                )
            }

            currentTime += windowDuration
        }

        return stats
    }

    private fun createProgressDialog(
        title: String,
        message: String,
    ): AlertDialog {
        return AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .create()
    }

    private fun normalizeGSRValue(gsrValue: Float): Double {

        val minGSR = 0.1 // Minimum typical GSR in µS
        val maxGSR = 50.0 // Maximum typical GSR in µS
        return ((gsrValue - minGSR) / (maxGSR - minGSR)).coerceIn(0.0, 1.0)
    }

    private fun normalizePPGValue(ppgValue: Int): Double {

        val minPPG = 0
        val maxPPG = 4095 // 12-bit ADC range
        return (ppgValue.toDouble() / maxPPG).coerceIn(0.0, 1.0)
    }

    private fun calculateDataQuality(
        dataPoint: GSRDataPoint,
        index: Int,
    ): Double {

        var quality = 100.0

        if (dataPoint.gsrValue < 0.01 || dataPoint.gsrValue > 100.0) {
            quality -= 30.0
        }

        if (dataPoint.ppgValue < 100 || dataPoint.ppgValue > 3900) {
            quality -= 20.0
        }

        if (index > 0 && index < gsrDataPoints.size - 1) {
            val prevChange = kotlin.math.abs(dataPoint.gsrValue - gsrDataPoints[index - 1].gsrValue)
            val nextChange = kotlin.math.abs(gsrDataPoints[index + 1].gsrValue - dataPoint.gsrValue)
            if (prevChange > 5.0 || nextChange > 5.0) {
                quality -= 15.0
            }
        }

        return quality.coerceIn(0.0, 100.0)
    }

    private fun calculateRecordingDuration(): Double {
        return if (gsrDataPoints.size >= 2) {
            (gsrDataPoints.last().timestamp - gsrDataPoints.first().timestamp) / 1000000000.0
        } else {
            0.0
        }
    }

    private fun calculateSamplingRate(): Double {
        return if (gsrDataPoints.size >= 2) {
            val duration = calculateRecordingDuration()
            if (duration > 0) gsrDataPoints.size / duration else 0.0
        } else {
            0.0
        }
    }

    private fun calculateStatistics(): Map<String, Double> {
        val gsrValues = gsrDataPoints.map { it.gsrValue.toDouble() }
        val ppgValues = gsrDataPoints.map { it.ppgValue.toDouble() }

        val gsrMean = gsrValues.sum() / gsrValues.size
        val ppgMean = ppgValues.sum() / ppgValues.size

        val gsrVariance = gsrValues.map { (it - gsrMean) * (it - gsrMean) }.sum() / gsrValues.size
        val ppgVariance = ppgValues.map { (it - ppgMean) * (it - ppgMean) }.sum() / ppgValues.size

        return mapOf(
            "gsrMean" to gsrMean,
            "gsrStdDev" to kotlin.math.sqrt(gsrVariance),
            "gsrMin" to (gsrValues.minOrNull() ?: 0.0),
            "gsrMax" to (gsrValues.maxOrNull() ?: 0.0),
            "gsrRange" to ((gsrValues.maxOrNull() ?: 0.0) - (gsrValues.minOrNull() ?: 0.0)),
            "ppgMean" to ppgMean,
            "ppgStdDev" to kotlin.math.sqrt(ppgVariance),
            "ppgMin" to (ppgValues.minOrNull() ?: 0.0),
            "ppgMax" to (ppgValues.maxOrNull() ?: 0.0),
            "averageQuality" to
                    gsrDataPoints.mapIndexed { index, point ->
                        calculateDataQuality(point, index)
                    }.average(),
            "syncMarkers" to gsrDataPoints.count { it.syncMarker }.toDouble(),
        )
    }

    private fun getDeviceInfo(): String {
        return "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
    }

    private fun showErrorDialog(
        title: String,
        message: String,
    ) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun loadGSRDataPoints() {
        gsrDataPoints.clear()
        dataRows.forEachIndexed { index, row ->
            try {

                val timestampNs =
                    System.nanoTime() // Placeholder - real implementation would parse timestamp
                val gsrDataPoint =
                    GSRDataPoint(
                        timestamp = timestampNs,
                        gsrValue = row.gsrValue,
                        gsrRaw = (row.gsrValue * 100).toInt()
                            .coerceIn(0, 4095), // Convert to ADC range
                        resistance = row.resistance,
                        ppgValue = (Math.random() * 1000 + 1000).toInt(), // Placeholder PPG value
                        syncMarker = false,
                    )
                gsrDataPoints.add(gsrDataPoint)
            } catch (e: Exception) {

            }
        }
    }

    data class GSRPlotData(
        val timestamps: List<Double>,
        val gsrValues: List<Double>,
        val ppgValues: List<Double>,
        val gsrMovingAverage: List<Double>,
        val ppgMovingAverage: List<Double>,
        val gsrEvents: List<GSREvent>,
        val statistics: List<TimeWindowStats>,
        val metadata: PlotMetadata,
    ) : Serializable

    data class GSREvent(
        val timestamp: Double,
        val type: String,
        val magnitude: Double,
        val gsrValue: Double,
    ) : Serializable

    data class TimeWindowStats(
        val startTime: Double,
        val endTime: Double,
        val mean: Double,
        val stdDev: Double,
        val min: Double,
        val max: Double,
        val count: Int,
    ) : Serializable

    data class PlotMetadata(
        val fileName: String,
        val duration: Double,
        val samplingRate: Double,
        val dataPoints: Int,
    ) : Serializable
}
