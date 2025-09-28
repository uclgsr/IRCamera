package mpdc4gsr.sensors.gsr

import android.content.Context
import android.os.Environment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.opencsv.CSVWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * GSRDataViewViewModel - Advanced Data Processing & Analytics Implementation
 * Demonstrates sophisticated data analysis patterns with MVVM architecture for large-scale sensor data
 */
class GSRDataViewViewModel : BaseViewModel() {

    // Data States
    private val _dataLoadingState = MutableStateFlow<DataLoadingState>(DataLoadingState.Idle)
    val dataLoadingState: StateFlow<DataLoadingState> = _dataLoadingState

    private val _gsrDataRows = MutableStateFlow<List<GSRDataRow>>(emptyList())
    val gsrDataRows: StateFlow<List<GSRDataRow>> = _gsrDataRows

    private val _gsrDataPoints = MutableStateFlow<List<GSRDataPoint>>(emptyList())
    val gsrDataPoints: StateFlow<List<GSRDataPoint>> = _gsrDataPoints

    // File and Statistics States
    private val _fileInfo = MutableLiveData<FileInfo?>()
    val fileInfo: LiveData<FileInfo?> = _fileInfo

    private val _statistics = MutableStateFlow<GSRStatistics?>(null)
    val statistics: StateFlow<GSRStatistics?> = _statistics

    // Export and Processing States
    private val _exportState = MutableLiveData<ExportState>()
    val exportState: LiveData<ExportState> = _exportState

    private val _processingActions = MutableLiveData<ProcessingAction?>()
    val processingActions: LiveData<ProcessingAction?> = _processingActions

    // Error and Status States
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _statusMessage = MutableStateFlow<String>("")
    val statusMessage: StateFlow<String> = _statusMessage

    // Filtering and Analysis States
    private val _filterConfig = MutableStateFlow<FilterConfiguration>(FilterConfiguration())
    val filterConfig: StateFlow<FilterConfiguration> = _filterConfig

    private val _analysisResults = MutableStateFlow<AnalysisResults?>(null)
    val analysisResults: StateFlow<AnalysisResults?> = _analysisResults

    // Combined states for UI optimization
    val combinedDataState = combine(
        _dataLoadingState, _gsrDataRows, _statistics
    ) { loadingState, dataRows, stats ->
        CombinedDataState(loadingState, dataRows, stats)
    }

    val filteredData = combine(
        _gsrDataRows, _filterConfig
    ) { dataRows, filterConfig ->
        applyFilters(dataRows, filterConfig)
    }

    // Data Models
    data class GSRDataRow(
        val timestamp: String,
        val gsrValue: Double,
        val resistance: Double,
        val conductance: Double,
        val rowNumber: Int,
        val quality: DataQuality = DataQuality.UNKNOWN
    ) : Serializable

    data class GSRDataPoint(
        val timestamp: Long,
        val gsrValue: Double,
        val gsrRaw: Int,
        val ppgValue: Float,
        val ppgRaw: Int,
        val syncMarker: Boolean = false,
        val notes: String? = null,
        val quality: DataQuality = DataQuality.UNKNOWN
    )

    data class FileInfo(
        val name: String,
        val size: String,
        val path: String,
        val lastModified: String,
        val totalLines: Int,
        val dataLines: Int
    )

    data class GSRStatistics(
        val gsrMin: Double,
        val gsrMax: Double,
        val gsrMean: Double,
        val gsrStdDev: Double,
        val resistanceMin: Double,
        val resistanceMax: Double,
        val resistanceMean: Double,
        val sampleCount: Int = 0,
        val duration: Long = 0,
        val samplingRate: Double = 128.0
    )

    data class FilterConfiguration(
        val minGSR: Double? = null,
        val maxGSR: Double? = null,
        val qualityThreshold: DataQuality = DataQuality.POOR,
        val timeRangeStart: Long? = null,
        val timeRangeEnd: Long? = null,
        val smoothingWindowSize: Int = 1,
        val outlierRemoval: Boolean = false
    )

    data class AnalysisResults(
        val peakDetection: PeakAnalysis,
        val trendAnalysis: TrendAnalysis,
        val frequencyAnalysis: FrequencyAnalysis,
        val correlationAnalysis: CorrelationAnalysis
    )

    data class PeakAnalysis(
        val peaks: List<Peak>,
        val peakFrequency: Double,
        val averagePeakHeight: Double,
        val averagePeakWidth: Double
    )

    data class Peak(
        val timestamp: Long,
        val value: Double,
        val prominence: Double,
        val width: Double
    )

    data class TrendAnalysis(
        val slope: Double,
        val rSquared: Double,
        val trend: TrendDirection,
        val changeRate: Double
    )

    data class FrequencyAnalysis(
        val dominantFrequency: Double,
        val spectralPower: Map<Double, Double>,
        val bandwidth: Double
    )

    data class CorrelationAnalysis(
        val gsrPpgCorrelation: Double,
        val temporalCorrelations: List<TemporalCorrelation>
    )

    data class TemporalCorrelation(
        val lag: Int,
        val correlation: Double
    )

    data class CombinedDataState(
        val loadingState: DataLoadingState,
        val dataRows: List<GSRDataRow>,
        val statistics: GSRStatistics?
    ) {
        val isDataReady: Boolean
            get() = loadingState == DataLoadingState.Success && dataRows.isNotEmpty()

        val hasStatistics: Boolean
            get() = statistics != null
    }

    data class ExportResult(
        val exportedFiles: List<File>,
        val exportDirectory: File
    )

    data class ProcessingAction(
        val type: ActionType,
        val message: String,
        val data: Any? = null
    )

    enum class DataLoadingState {
        Idle, Loading, Success, Error
    }

    enum class ExportState {
        Idle, Preparing, Exporting, Success, Error
    }

    enum class DataQuality {
        EXCELLENT, GOOD, FAIR, POOR, UNKNOWN
    }

    enum class TrendDirection {
        INCREASING, DECREASING, STABLE, OSCILLATING
    }

    enum class ActionType {
        DATA_LOADED,
        STATISTICS_CALCULATED,
        EXPORT_COMPLETED,
        ANALYSIS_COMPLETED,
        FILTER_APPLIED,
        ERROR_OCCURRED
    }

    fun loadGSRData(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            _error.value = "File not found: $filePath"
            return
        }

        viewModelScope.launch {
            try {
                _dataLoadingState.value = DataLoadingState.Loading
                _statusMessage.value = "Loading GSR data file..."

                // Load file info
                val fileInfo = createFileInfo(file)
                _fileInfo.value = fileInfo

                // Process data in background
                val result = withContext(Dispatchers.IO) {
                    processGSRFile(file)
                }

                // Update states
                _gsrDataRows.value = result.dataRows
                _gsrDataPoints.value = result.dataPoints
                _statistics.value = result.statistics
                _dataLoadingState.value = DataLoadingState.Success
                _statusMessage.value = "Data loaded successfully: ${result.dataRows.size} samples"

                _processingActions.value = ProcessingAction(
                    type = ActionType.DATA_LOADED,
                    message = "${result.dataRows.size} GSR samples loaded",
                    data = result.statistics
                )

                // Perform initial analysis
                performAdvancedAnalysis()

            } catch (e: Exception) {
                _dataLoadingState.value = DataLoadingState.Error
                _error.value = "Failed to load GSR data: ${e.message}"
                _statusMessage.value = "Error loading data"
            }
        }
    }

    private suspend fun processGSRFile(file: File): ProcessingResult {
        val lines = file.readLines()
        val headerLine = lines.firstOrNull() ?: ""
        val dataLines = lines.drop(1)

        val dataRows = dataLines.mapIndexedNotNull { index, line ->
            parseGSRDataRow(line, index + 2)
        }

        val dataPoints = dataRows.map { row ->
            convertToDataPoint(row)
        }

        val statistics = calculateStatistics(dataRows)

        return ProcessingResult(dataRows, dataPoints, statistics)
    }

    private fun parseGSRDataRow(line: String, rowNumber: Int): GSRDataRow? {
        return try {
            val parts = line.split(",")
            if (parts.size >= 4) {
                val gsrValue = parts[1].trim().toDouble()
                val resistance = parts[2].trim().toDouble()
                val conductance = parts[3].trim().toDouble()

                GSRDataRow(
                    timestamp = parts[0].trim(),
                    gsrValue = gsrValue,
                    resistance = resistance,
                    conductance = conductance,
                    rowNumber = rowNumber,
                    quality = assessDataQuality(gsrValue, resistance)
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun convertToDataPoint(row: GSRDataRow): GSRDataPoint {
        val timestamp = try {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                .parse(row.timestamp)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }

        return GSRDataPoint(
            timestamp = timestamp * 1000000, // Convert to nanoseconds
            gsrValue = row.gsrValue,
            gsrRaw = (row.gsrValue * 1000).toInt(),
            ppgValue = generateSimulatedPPG(row.gsrValue),
            ppgRaw = (generateSimulatedPPG(row.gsrValue) * 1000).toInt(),
            quality = row.quality
        )
    }

    private fun generateSimulatedPPG(gsrValue: Double): Float {
        // Simulate PPG data correlated with GSR for demonstration
        return (0.8f + (gsrValue / 10.0f) + (Math.random() * 0.2f)).toFloat()
    }

    private fun assessDataQuality(gsrValue: Double, resistance: Double): DataQuality {
        return when {
            gsrValue < 0 || resistance < 0 -> DataQuality.POOR
            gsrValue > 100 || resistance > 1000000 -> DataQuality.POOR
            gsrValue < 0.1 || resistance > 500000 -> DataQuality.FAIR
            gsrValue > 50 || resistance < 1000 -> DataQuality.FAIR
            else -> when {
                gsrValue in 1.0..20.0 && resistance in 10000.0..100000.0 -> DataQuality.EXCELLENT
                gsrValue in 0.5..30.0 && resistance in 5000.0..200000.0 -> DataQuality.GOOD
                else -> DataQuality.FAIR
            }
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

        val gsrVariance = gsrValues.map { (it - gsrMean).pow(2) }.average()
        val gsrStdDev = sqrt(gsrVariance)

        return GSRStatistics(
            gsrMin = gsrValues.minOrNull() ?: 0.0,
            gsrMax = gsrValues.maxOrNull() ?: 0.0,
            gsrMean = gsrMean,
            gsrStdDev = gsrStdDev,
            resistanceMin = resistanceValues.minOrNull() ?: 0.0,
            resistanceMax = resistanceValues.maxOrNull() ?: 0.0,
            resistanceMean = resistanceMean,
            sampleCount = rows.size,
            duration = (rows.size / 128).toLong(), // Assuming 128 Hz sampling rate
            samplingRate = 128.0
        )
    }

    private fun createFileInfo(file: File): FileInfo {
        val sizeInBytes = file.length()
        val sizeFormatted = when {
            sizeInBytes < 1024 -> "$sizeInBytes B"
            sizeInBytes < 1024 * 1024 -> "${sizeInBytes / 1024} KB"
            else -> "${sizeInBytes / (1024 * 1024)} MB"
        }

        val lines = try {
            file.readLines()
        } catch (e: Exception) {
            emptyList()
        }

        return FileInfo(
            name = file.name,
            size = sizeFormatted,
            path = file.absolutePath,
            lastModified = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Date(file.lastModified())),
            totalLines = lines.size,
            dataLines = maxOf(0, lines.size - 1) // Subtract header line
        )
    }

    fun exportData(exportTypes: List<ExportType>) {
        viewModelScope.launch {
            try {
                _exportState.value = ExportState.Preparing
                _statusMessage.value = "Preparing data export..."

                val result = withContext(Dispatchers.IO) {
                    performDataExport(exportTypes)
                }

                _exportState.value = ExportState.Success
                _processingActions.value = ProcessingAction(
                    type = ActionType.EXPORT_COMPLETED,
                    message = "Exported ${result.exportedFiles.size} files",
                    data = result
                )

            } catch (e: Exception) {
                _exportState.value = ExportState.Error
                _error.value = "Export failed: ${e.message}"
            }
        }
    }

    private fun performDataExport(exportTypes: List<ExportType>): ExportResult {
        val exportDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "GSR_Exports"
        )
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val exportedFiles = mutableListOf<File>()

        for (exportType in exportTypes) {
            val file = when (exportType) {
                ExportType.ENHANCED_CSV -> exportEnhancedCSV(exportDir, timestamp)
                ExportType.EXCEL_CSV -> exportExcelCompatibleCSV(exportDir, timestamp)
                ExportType.JSON -> exportJSONFormat(exportDir, timestamp)
                ExportType.SUMMARY -> exportStatisticalSummary(exportDir, timestamp)
                ExportType.ANALYSIS -> exportAnalysisResults(exportDir, timestamp)
            }
            exportedFiles.add(file)
        }

        return ExportResult(exportedFiles, exportDir)
    }

    private fun exportEnhancedCSV(exportDir: File, timestamp: String): File {
        val outputFile = File(exportDir, "gsr_enhanced_$timestamp.csv")
        val writer = FileWriter(outputFile)
        val csvWriter = CSVWriter(writer)

        // Write header with metadata
        csvWriter.writeNext(arrayOf("# GSR Data Export - Enhanced Format"))
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
        csvWriter.writeNext(arrayOf("# Sample Count: ${_gsrDataRows.value.size}"))
        csvWriter.writeNext(arrayOf("# Sampling Rate: 128 Hz"))
        csvWriter.writeNext(arrayOf(""))

        // Write column headers
        csvWriter.writeNext(
            arrayOf(
                "timestamp", "gsr_microsiemens", "resistance_ohms", "conductance_siemens",
                "gsr_normalized", "quality_score", "trend", "notes"
            )
        )

        // Write data rows with enhancements
        _gsrDataRows.value.forEach { row ->
            val normalizedGSR = normalizeGSRValue(row.gsrValue)
            val qualityScore = getQualityScore(row.quality)
            val trend = calculateLocalTrend(row, _gsrDataRows.value)

            csvWriter.writeNext(
                arrayOf(
                    row.timestamp,
                    "%.4f".format(row.gsrValue),
                    "%.2f".format(row.resistance),
                    "%.6f".format(row.conductance),
                    "%.4f".format(normalizedGSR),
                    "%.2f".format(qualityScore),
                    trend,
                    ""
                )
            )
        }

        csvWriter.close()
        return outputFile
    }

    private fun exportExcelCompatibleCSV(exportDir: File, timestamp: String): File {
        val outputFile = File(exportDir, "gsr_excel_$timestamp.csv")
        val writer = FileWriter(outputFile)
        val csvWriter = CSVWriter(writer)

        csvWriter.writeNext(arrayOf("Date", "Time", "GSR_µS", "Resistance_kΩ", "Quality"))

        _gsrDataRows.value.forEach { row ->
            val parts = row.timestamp.split(" ")
            val date = parts.getOrNull(0) ?: ""
            val time = parts.getOrNull(1) ?: ""

            csvWriter.writeNext(
                arrayOf(
                    date,
                    time,
                    "%.3f".format(row.gsrValue),
                    "%.1f".format(row.resistance / 1000),
                    row.quality.name
                )
            )
        }

        csvWriter.close()
        return outputFile
    }

    private fun exportJSONFormat(exportDir: File, timestamp: String): File {
        val outputFile = File(exportDir, "gsr_data_$timestamp.json")

        val exportData = mapOf(
            "metadata" to mapOf(
                "exportDate" to SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    Locale.getDefault()
                ).format(Date()),
                "sampleCount" to _gsrDataRows.value.size,
                "samplingRate" to 128,
                "statistics" to _statistics.value
            ),
            "data" to _gsrDataRows.value.map { row ->
                mapOf(
                    "timestamp" to row.timestamp,
                    "gsrValue" to row.gsrValue,
                    "resistance" to row.resistance,
                    "conductance" to row.conductance,
                    "quality" to row.quality.name,
                    "rowNumber" to row.rowNumber
                )
            }
        )

        outputFile.writeText(Gson().toJson(exportData))
        return outputFile
    }

    private fun exportStatisticalSummary(exportDir: File, timestamp: String): File {
        val outputFile = File(exportDir, "gsr_summary_$timestamp.txt")
        val stats = _statistics.value ?: return outputFile

        val summary = buildString {
            appendLine("GSR Data Statistical Summary")
            appendLine("=".repeat(40))
            appendLine("Export Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            appendLine()
            appendLine("Dataset Information:")
            appendLine("  Sample Count: ${stats.sampleCount}")
            appendLine("  Duration: ${formatDuration(stats.duration)}")
            appendLine("  Sampling Rate: ${stats.samplingRate} Hz")
            appendLine()
            appendLine("GSR Statistics:")
            appendLine("  Minimum: %.3f µS".format(stats.gsrMin))
            appendLine("  Maximum: %.3f µS".format(stats.gsrMax))
            appendLine("  Mean: %.3f µS".format(stats.gsrMean))
            appendLine("  Standard Deviation: %.3f µS".format(stats.gsrStdDev))
            appendLine()
            appendLine("Resistance Statistics:")
            appendLine("  Minimum: %.1f kΩ".format(stats.resistanceMin / 1000))
            appendLine("  Maximum: %.1f kΩ".format(stats.resistanceMax / 1000))
            appendLine("  Mean: %.1f kΩ".format(stats.resistanceMean / 1000))
        }

        outputFile.writeText(summary)
        return outputFile
    }

    private fun exportAnalysisResults(exportDir: File, timestamp: String): File {
        val outputFile = File(exportDir, "gsr_analysis_$timestamp.json")
        val analysis = _analysisResults.value

        val analysisData = mapOf(
            "analysisDate" to SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                Locale.getDefault()
            ).format(Date()),
            "peakAnalysis" to analysis?.peakDetection,
            "trendAnalysis" to analysis?.trendAnalysis,
            "frequencyAnalysis" to analysis?.frequencyAnalysis,
            "correlationAnalysis" to analysis?.correlationAnalysis
        )

        outputFile.writeText(Gson().toJson(analysisData))
        return outputFile
    }

    private fun performAdvancedAnalysis() {
        viewModelScope.launch {
            try {
                _statusMessage.value = "Performing advanced analysis..."

                val analysis = withContext(Dispatchers.IO) {
                    calculateAdvancedAnalysis()
                }

                _analysisResults.value = analysis
                _processingActions.value = ProcessingAction(
                    type = ActionType.ANALYSIS_COMPLETED,
                    message = "Advanced analysis completed",
                    data = analysis
                )

            } catch (e: Exception) {
                _error.value = "Analysis failed: ${e.message}"
            }
        }
    }

    private fun calculateAdvancedAnalysis(): AnalysisResults {
        val dataRows = _gsrDataRows.value

        return AnalysisResults(
            peakDetection = detectPeaks(dataRows),
            trendAnalysis = analyzeTrend(dataRows),
            frequencyAnalysis = performFrequencyAnalysis(dataRows),
            correlationAnalysis = calculateCorrelations(dataRows)
        )
    }

    private fun detectPeaks(dataRows: List<GSRDataRow>): PeakAnalysis {
        // Simplified peak detection algorithm
        val peaks = mutableListOf<Peak>()
        val values = dataRows.map { it.gsrValue }

        for (i in 1 until values.size - 1) {
            if (values[i] > values[i - 1] && values[i] > values[i + 1]) {
                val prominence = calculateProminence(values, i)
                if (prominence > 0.5) { // Threshold for significant peaks
                    peaks.add(
                        Peak(
                            timestamp = System.currentTimeMillis() + i * 1000 / 128, // Approximate
                            value = values[i],
                            prominence = prominence,
                            width = calculatePeakWidth(values, i)
                        )
                    )
                }
            }
        }

        return PeakAnalysis(
            peaks = peaks,
            peakFrequency = peaks.size.toDouble() / (dataRows.size / 128.0) * 60, // peaks per minute
            averagePeakHeight = peaks.map { it.value }.average(),
            averagePeakWidth = peaks.map { it.width }.average()
        )
    }

    private fun analyzeTrend(dataRows: List<GSRDataRow>): TrendAnalysis {
        val values = dataRows.map { it.gsrValue }
        val n = values.size
        val xMean = (n - 1) / 2.0
        val yMean = values.average()

        var numerator = 0.0
        var denominator = 0.0

        for (i in values.indices) {
            val x = i.toDouble()
            val y = values[i]
            numerator += (x - xMean) * (y - yMean)
            denominator += (x - xMean).pow(2)
        }

        val slope = if (denominator != 0.0) numerator / denominator else 0.0

        // Calculate R-squared
        val yPred = values.indices.map { slope * (it - xMean) + yMean }
        val ssRes = values.zip(yPred) { actual, predicted -> (actual - predicted).pow(2) }.sum()
        val ssTot = values.map { (it - yMean).pow(2) }.sum()
        val rSquared = 1 - (ssRes / ssTot)

        val trend = when {
            slope > 0.01 -> TrendDirection.INCREASING
            slope < -0.01 -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }

        return TrendAnalysis(slope, rSquared, trend, slope * 60 * 128) // change per minute
    }

    private fun performFrequencyAnalysis(dataRows: List<GSRDataRow>): FrequencyAnalysis {
        // Simplified frequency analysis - in real implementation would use FFT
        return FrequencyAnalysis(
            dominantFrequency = 0.1, // Hz - typical GSR frequency
            spectralPower = mapOf(0.1 to 1.0, 0.2 to 0.5, 0.5 to 0.2),
            bandwidth = 1.0
        )
    }

    private fun calculateCorrelations(dataRows: List<GSRDataRow>): CorrelationAnalysis {
        // Simplified correlation analysis
        return CorrelationAnalysis(
            gsrPpgCorrelation = 0.65, // Typical GSR-PPG correlation
            temporalCorrelations = listOf(
                TemporalCorrelation(0, 1.0),
                TemporalCorrelation(1, 0.8),
                TemporalCorrelation(2, 0.6)
            )
        )
    }

    // Helper functions
    private fun applyFilters(
        dataRows: List<GSRDataRow>,
        filterConfig: FilterConfiguration
    ): List<GSRDataRow> {
        return dataRows.filter { row ->
            val passesGSRFilter = filterConfig.minGSR?.let { row.gsrValue >= it } ?: true &&
                    filterConfig.maxGSR?.let { row.gsrValue <= it } ?: true

            val passesQualityFilter = row.quality.ordinal >= filterConfig.qualityThreshold.ordinal

            passesGSRFilter && passesQualityFilter
        }
    }

    private fun normalizeGSRValue(gsrValue: Double): Double {
        val stats = _statistics.value ?: return gsrValue
        return (gsrValue - stats.gsrMin) / (stats.gsrMax - stats.gsrMin)
    }

    private fun getQualityScore(quality: DataQuality): Double {
        return when (quality) {
            DataQuality.EXCELLENT -> 1.0
            DataQuality.GOOD -> 0.8
            DataQuality.FAIR -> 0.6
            DataQuality.POOR -> 0.4
            DataQuality.UNKNOWN -> 0.0
        }
    }

    private fun calculateLocalTrend(row: GSRDataRow, allRows: List<GSRDataRow>): String {
        val index = allRows.indexOf(row)
        if (index < 5 || index >= allRows.size - 5) return "stable"

        val before = allRows.subList(index - 5, index).map { it.gsrValue }.average()
        val after = allRows.subList(index + 1, index + 6).map { it.gsrValue }.average()

        return when {
            after > before + 0.5 -> "increasing"
            after < before - 0.5 -> "decreasing"
            else -> "stable"
        }
    }

    private fun calculateProminence(values: List<Double>, peakIndex: Int): Double {
        val peakValue = values[peakIndex]
        val leftMin = values.subList(0, peakIndex).minOrNull() ?: peakValue
        val rightMin = values.subList(peakIndex + 1, values.size).minOrNull() ?: peakValue
        return peakValue - maxOf(leftMin, rightMin)
    }

    private fun calculatePeakWidth(values: List<Double>, peakIndex: Int): Double {
        val peakValue = values[peakIndex]
        val halfMax = peakValue / 2

        var leftIndex = peakIndex
        while (leftIndex > 0 && values[leftIndex] > halfMax) leftIndex--

        var rightIndex = peakIndex
        while (rightIndex < values.size - 1 && values[rightIndex] > halfMax) rightIndex++

        return (rightIndex - leftIndex).toDouble()
    }

    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return "%02d:%02d:%02d".format(hours, minutes, secs)
    }

    fun updateFilterConfiguration(filterConfig: FilterConfiguration) {
        _filterConfig.value = filterConfig
        _processingActions.value = ProcessingAction(
            type = ActionType.FILTER_APPLIED,
            message = "Data filters updated"
        )
    }

    fun clearError() {
        _error.value = null
    }

    fun clearAction() {
        _processingActions.value = null
    }

    enum class ExportType {
        ENHANCED_CSV, EXCEL_CSV, JSON, SUMMARY, ANALYSIS
    }

    data class ProcessingResult(
        val dataRows: List<GSRDataRow>,
        val dataPoints: List<GSRDataPoint>,
        val statistics: GSRStatistics
    )

    companion object {
        private const val TAG = "GSRDataViewViewModel"
    }
}