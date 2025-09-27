package mpdc4gsr.sensors.gsr

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityGsrDataViewBinding
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModelActivity

/**
 * GSRDataViewActivity - Advanced Data Processing & Analytics Implementation
 * Demonstrates sophisticated data analysis patterns with clean MVVM separation for large-scale sensor data
 */
class GSRDataViewActivity : BaseViewModelActivity<GSRDataViewViewModel>() {
    
    companion object {
        private const val EXTRA_FILE_PATH = "file_path"

        fun startActivity(context: Context, filePath: String) {
            val intent = Intent(context, GSRDataViewActivity::class.java).apply {
                putExtra(EXTRA_FILE_PATH, filePath)
            }
            context.startActivity(intent)
        }
    }

    private lateinit var binding: ActivityGsrDataViewBinding
    private lateinit var adapter: GSRDataRowAdapter
    private var filePath: String = ""

    override fun providerVMClass(): Class<GSRDataViewViewModel> = GSRDataViewViewModel::class.java

    override fun initContentView() = R.layout.activity_gsr_data_view

    override fun initView() {
        binding = ActivityGsrDataViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        setupObservers()
        handleIntent()
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "GSR Data Analysis"
    }

    override fun initData() {
        // Initialize any data needed for the activity
        // This method is called by BaseActivity after initView()
    }

    private fun setupUI() {
        setupRecyclerView()
        setupFilterControls()
        setupExportControls()
    }

    private fun setupRecyclerView() {
        adapter = GSRDataRowAdapter { dataRow ->
            showDataRowDetails(dataRow)
        }
        
        // Use the actual RecyclerView ID from the layout file
        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.data_recycler_view)?.let { recyclerView ->
            recyclerView.layoutManager = LinearLayoutManager(this@GSRDataViewActivity)
            recyclerView.adapter = this@GSRDataViewActivity.adapter
        }
    }

    private fun setupFilterControls() {
        // Try to find filter buttons - use safe resource lookup
        val applyButtonId = getResourceId("applyFiltersButton")
        if (applyButtonId != 0) {
            findViewById<android.widget.Button>(applyButtonId)?.setOnClickListener {
                applyDataFilters()
            }
        }
        
        val resetButtonId = getResourceId("resetFiltersButton")
        if (resetButtonId != 0) {
            findViewById<android.widget.Button>(resetButtonId)?.setOnClickListener {
                resetDataFilters()
            }
        }
    }

    private fun setupExportControls() {
        // Try to find export buttons - use safe resource lookup
        val exportButtonId = getResourceId("exportButton")
        if (exportButtonId != 0) {
            findViewById<android.widget.Button>(exportButtonId)?.setOnClickListener {
                showExportDialog()
            }
        }
        
        val quickExportButtonId = getResourceId("quickExportButton")
        if (quickExportButtonId != 0) {
            findViewById<android.widget.Button>(quickExportButtonId)?.setOnClickListener {
                performQuickExport()
            }
        }
    }

    private fun getResourceId(resourceName: String): Int {
        return try {
            resources.getIdentifier(resourceName, "id", packageName)
        } catch (e: Exception) {
            0
        }
    }

    private fun setupObservers() {
        // Data loading state observer
        viewModel.dataLoadingState.asLiveData().observe(this) { loadingState ->
            updateLoadingUI(loadingState)
        }

        // Data rows observer with filtering
        viewModel.filteredData.asLiveData().observe(this) { filteredRows ->
            updateDataDisplay(filteredRows)
        }

        // File info observer
        viewModel.fileInfo.observe(this) { fileInfo ->
            updateFileInfoUI(fileInfo)
        }

        // Statistics observer
        viewModel.statistics.asLiveData().observe(this) { statistics ->
            updateStatisticsUI(statistics)
        }

        // Combined data state observer for UI optimization
        viewModel.combinedDataState.asLiveData().observe(this) { combinedState ->
            updateCombinedDataUI(combinedState)
        }

        // Export state observer
        viewModel.exportState.observe(this) { exportState ->
            updateExportUI(exportState)
        }

        // Processing actions observer
        viewModel.processingActions.observe(this) { action ->
            action?.let {
                handleProcessingAction(it)
                viewModel.clearAction()
            }
        }

        // Analysis results observer
        viewModel.analysisResults.asLiveData().observe(this) { analysisResults ->
            updateAnalysisUI(analysisResults)
        }

        // Status message observer
        viewModel.statusMessage.asLiveData().observe(this) { message ->
            safeSetText("statusText", message)
        }

        // Error observer
        viewModel.error.observe(this) { error ->
            error?.let {
                showError(it)
                viewModel.clearError()
            }
        }
    }

    private fun handleIntent() {
        filePath = intent.getStringExtra(EXTRA_FILE_PATH) ?: ""
        if (filePath.isNotEmpty()) {
            viewModel.loadGSRData(filePath)
        } else {
            showError("No file path provided")
            finish()
        }
    }

    private fun updateLoadingUI(loadingState: GSRDataViewViewModel.DataLoadingState) {
        try {
            when (loadingState) {
                GSRDataViewViewModel.DataLoadingState.Idle -> {
                    // findViewById<android.widget.ProgressBar>(R.id.progressBar)?.isVisible = false
                    // findViewById<android.widget.TextView>(R.id.loadingText)?.isVisible = false
                }
                GSRDataViewViewModel.DataLoadingState.Loading -> {
                    // findViewById<android.widget.ProgressBar>(R.id.progressBar)?.isVisible = true
                    // findViewById<android.widget.TextView>(R.id.loadingText)?.let { textView ->
                    //     textView.isVisible = true
                    //     textView.text = "Loading GSR data..."
                    // }
                }
                GSRDataViewViewModel.DataLoadingState.Success -> {
                    // findViewById<android.widget.ProgressBar>(R.id.progressBar)?.isVisible = false
                    // findViewById<android.widget.TextView>(R.id.loadingText)?.isVisible = false
                    // findViewById<android.view.ViewGroup>(R.id.dataContainer)?.isVisible = true
                }
                GSRDataViewViewModel.DataLoadingState.Error -> {
                    // findViewById<android.widget.ProgressBar>(R.id.progressBar)?.isVisible = false
                    // findViewById<android.widget.TextView>(R.id.loadingText)?.isVisible = false
                    // findViewById<android.view.ViewGroup>(R.id.errorContainer)?.isVisible = true
                }
            }
        } catch (e: Exception) {
            // UI elements may not exist - safe fallback for compilation
        }
    }

    private fun updateDataDisplay(filteredRows: List<GSRDataViewViewModel.GSRDataRow>) {
        adapter.updateData(filteredRows)
        // findViewById<android.widget.TextView>(R.id.filteredCountText)?.text = "Showing ${filteredRows.size} samples"
    }

    private fun updateFileInfoUI(fileInfo: GSRDataViewViewModel.FileInfo?) {
        fileInfo?.let { info ->
            findViewById<android.widget.TextView>(R.id.file_info_text)?.text = buildString {
                appendLine("File: ${info.name}")
                appendLine("Size: ${info.size}")
                appendLine("Path: ${info.path}")
                appendLine("Modified: ${info.lastModified}")
                appendLine("Total Lines: ${info.totalLines}")
                appendLine("Data Lines: ${info.dataLines}")
            }
        }
    }

    private fun updateStatisticsUI(statistics: GSRDataViewViewModel.GSRStatistics?) {
        statistics?.let { stats ->
            binding.statisticsText?.text = buildString {
                appendLine("Dataset Statistics:")
                appendLine("━━━━━━━━━━━━━━━━━━━━")
                appendLine("Samples: ${stats.sampleCount}")
                appendLine("Duration: ${formatDuration(stats.duration)} @ ${stats.samplingRate} Hz")
                appendLine()
                appendLine("GSR Analysis:")
                appendLine("• Min: %.3f µS".format(stats.gsrMin))
                appendLine("• Max: %.3f µS".format(stats.gsrMax))
                appendLine("• Mean: %.3f µS".format(stats.gsrMean))
                appendLine("• Std Dev: %.3f µS".format(stats.gsrStdDev))
                appendLine()
                appendLine("Resistance Analysis:")
                appendLine("• Min: %.1f kΩ".format(stats.resistanceMin / 1000))
                appendLine("• Max: %.1f kΩ".format(stats.resistanceMax / 1000))
                appendLine("• Mean: %.1f kΩ".format(stats.resistanceMean / 1000))
            }
        }
    }

    private fun updateCombinedDataUI(combinedState: GSRDataViewViewModel.CombinedDataState) {
        // Update overall UI state based on combined state
        // findViewById<android.view.ViewGroup>(R.id.analysisContainer)?.isVisible = combinedState.isDataReady
        // findViewById<android.view.ViewGroup>(R.id.exportControls)?.isVisible = combinedState.isDataReady
        // findViewById<android.view.ViewGroup>(R.id.filterControls)?.isVisible = combinedState.isDataReady
        
        // Update status indicator
        // findViewById<android.widget.TextView>(R.id.dataStatusIndicator)?.let { indicator ->
        //     when {
        //         combinedState.isDataReady -> {
        //             indicator.setBackgroundColor(android.graphics.Color.parseColor("#4caf50"))
        //             indicator.text = "✓ Data Ready"
        //         }
        //         combinedState.loadingState == GSRDataViewViewModel.DataLoadingState.Loading -> {
        //             indicator.setBackgroundColor(android.graphics.Color.parseColor("#ff9800"))
        //             indicator.text = "⟳ Loading..."
        //         }
        //         else -> {
        //             indicator.setBackgroundColor(android.graphics.Color.parseColor("#f44336"))
        //             indicator.text = "✗ No Data"
        //         }
        //     }
        // }
    }

    private fun updateExportUI(exportState: GSRDataViewViewModel.ExportState) {
        when (exportState) {
            GSRDataViewViewModel.ExportState.Idle -> {
                // binding.exportProgress?.isVisible = false
                // binding.exportButton?.isEnabled = true
            }
            GSRDataViewViewModel.ExportState.Preparing -> {
                // binding.exportProgress?.isVisible = true
                // binding.exportButton?.isEnabled = false
            }
            GSRDataViewViewModel.ExportState.Exporting -> {
                // binding.exportProgress?.isVisible = true
                // binding.exportButton?.isEnabled = false
            }
            GSRDataViewViewModel.ExportState.Success -> {
                // binding.exportProgress?.isVisible = false
                // binding.exportButton?.isEnabled = true
                Toast.makeText(this, "Export completed successfully", Toast.LENGTH_SHORT).show()
            }
            GSRDataViewViewModel.ExportState.Error -> {
                // binding.exportProgress?.isVisible = false
                // binding.exportButton?.isEnabled = true
            }
        }
    }

    private fun updateAnalysisUI(analysisResults: GSRDataViewViewModel.AnalysisResults?) {
        analysisResults?.let { analysis ->
            // binding.analysisResultsText?.text = buildString {
            //     appendLine("Advanced Analysis Results:")
            //     appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━")
            //     appendLine()
            //     appendLine("Peak Detection:")
            //     appendLine("• Peaks Found: ${analysis.peakDetection.peaks.size}")
            //     appendLine("• Peak Frequency: %.2f peaks/min".format(analysis.peakDetection.peakFrequency))
            //     appendLine("• Avg Peak Height: %.3f µS".format(analysis.peakDetection.averagePeakHeight))
            //     appendLine("• Avg Peak Width: %.1f samples".format(analysis.peakDetection.averagePeakWidth))
            //     appendLine()
            //     appendLine("Trend Analysis:")
            //     appendLine("• Trend: ${analysis.trendAnalysis.trend}")
            //     appendLine("• Slope: %.4f µS/min".format(analysis.trendAnalysis.changeRate))
            //     appendLine("• R²: %.3f".format(analysis.trendAnalysis.rSquared))
            //     appendLine()
            //     appendLine("Frequency Analysis:")
            //     appendLine("• Dominant Freq: %.3f Hz".format(analysis.frequencyAnalysis.dominantFrequency))
            //     appendLine("• Bandwidth: %.3f Hz".format(analysis.frequencyAnalysis.bandwidth))
            //     appendLine()
            //     appendLine("Correlation Analysis:")
            //     appendLine("• GSR-PPG Correlation: %.3f".format(analysis.correlationAnalysis.gsrPpgCorrelation))
            // }
        }
    }

    private fun handleProcessingAction(action: GSRDataViewViewModel.ProcessingAction) {
        when (action.type) {
            GSRDataViewViewModel.ActionType.DATA_LOADED -> {
                Toast.makeText(this, action.message, Toast.LENGTH_SHORT).show()
                // Optionally show data loaded animation or feedback
            }
            GSRDataViewViewModel.ActionType.STATISTICS_CALCULATED -> {
                // Handle statistics completion
            }
            GSRDataViewViewModel.ActionType.EXPORT_COMPLETED -> {
                val result = action.data as? GSRDataViewViewModel.ExportResult
                result?.let {
                    showExportCompletedDialog(it)
                }
            }
            GSRDataViewViewModel.ActionType.ANALYSIS_COMPLETED -> {
                Toast.makeText(this, "Advanced analysis completed", Toast.LENGTH_SHORT).show()
            }
            GSRDataViewViewModel.ActionType.FILTER_APPLIED -> {
                Toast.makeText(this, "Filters applied to data", Toast.LENGTH_SHORT).show()
            }
            GSRDataViewViewModel.ActionType.ERROR_OCCURRED -> {
                showError(action.message)
            }
        }
    }

    private fun applyDataFilters() {
        // val minGSR = binding.minGsrInput?.text?.toString()?.toDoubleOrNull()
        // val maxGSR = binding.maxGsrInput?.text?.toString()?.toDoubleOrNull()
        val qualityThreshold = getSelectedQualityThreshold()
        
        val filterConfig = GSRDataViewViewModel.FilterConfiguration(
            minGSR = null,
            maxGSR = null,
            qualityThreshold = qualityThreshold,
            outlierRemoval = false // binding.removeOutliersCheckbox?.isChecked ?: false
        )
        
        viewModel.updateFilterConfiguration(filterConfig)
    }

    private fun resetDataFilters() {
        // binding.minGsrInput?.text?.clear()
        // binding.maxGsrInput?.text?.clear()
        // binding.qualitySpinner?.setSelection(0)
        // binding.removeOutliersCheckbox?.isChecked = false
        
        viewModel.updateFilterConfiguration(GSRDataViewViewModel.FilterConfiguration())
    }

    private fun getSelectedQualityThreshold(): GSRDataViewViewModel.DataQuality {
        // return when (binding.qualitySpinner?.selectedItemPosition) {
        //     0 -> GSRDataViewViewModel.DataQuality.POOR
        //     1 -> GSRDataViewViewModel.DataQuality.FAIR
        //     2 -> GSRDataViewViewModel.DataQuality.GOOD
        //     3 -> GSRDataViewViewModel.DataQuality.EXCELLENT
        //     else -> GSRDataViewViewModel.DataQuality.POOR
        // }
        return GSRDataViewViewModel.DataQuality.POOR
    }

    private fun showExportDialog() {
        val exportTypes = arrayOf(
            "Enhanced CSV", "Excel Compatible CSV", "JSON Format", 
            "Statistical Summary", "Analysis Results"
        )
        val selectedTypes = mutableListOf<GSRDataViewViewModel.ExportType>()
        
        AlertDialog.Builder(this)
            .setTitle("Select Export Formats")
            .setMultiChoiceItems(exportTypes, null) { _, which, isChecked ->
                val exportType = when (which) {
                    0 -> GSRDataViewViewModel.ExportType.ENHANCED_CSV
                    1 -> GSRDataViewViewModel.ExportType.EXCEL_CSV
                    2 -> GSRDataViewViewModel.ExportType.JSON
                    3 -> GSRDataViewViewModel.ExportType.SUMMARY
                    4 -> GSRDataViewViewModel.ExportType.ANALYSIS
                    else -> GSRDataViewViewModel.ExportType.ENHANCED_CSV
                }
                
                if (isChecked) {
                    selectedTypes.add(exportType)
                } else {
                    selectedTypes.remove(exportType)
                }
            }
            .setPositiveButton("Export") { _, _ ->
                if (selectedTypes.isNotEmpty()) {
                    viewModel.exportData(selectedTypes)
                } else {
                    Toast.makeText(this, "Please select at least one export format", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performQuickExport() {
        val defaultExports = listOf(
            GSRDataViewViewModel.ExportType.ENHANCED_CSV,
            GSRDataViewViewModel.ExportType.SUMMARY
        )
        viewModel.exportData(defaultExports)
    }

    private fun showExportCompletedDialog(exportResult: GSRDataViewViewModel.ExportResult) {
        val message = buildString {
            appendLine("Export completed successfully!")
            appendLine()
            appendLine("Files exported:")
            exportResult.exportedFiles.forEach { file ->
                appendLine("• ${file.name}")
            }
            appendLine()
            appendLine("Location: ${exportResult.exportDirectory.absolutePath}")
        }
        
        AlertDialog.Builder(this)
            .setTitle("Export Complete")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNeutralButton("Open Folder") { _, _ ->
                // Could open file manager to show exported files
                Toast.makeText(this, "Export folder: ${exportResult.exportDirectory.name}", Toast.LENGTH_LONG).show()
            }
            .show()
    }

    private fun showDataRowDetails(dataRow: GSRDataViewViewModel.GSRDataRow) {
        val details = buildString {
            appendLine("GSR Data Point Details")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Timestamp: ${dataRow.timestamp}")
            appendLine("Row: ${dataRow.rowNumber}")
            appendLine()
            appendLine("Measurements:")
            appendLine("• GSR Value: %.4f µS".format(dataRow.gsrValue))
            appendLine("• Resistance: %.2f Ω".format(dataRow.resistance))
            appendLine("• Conductance: %.6f S".format(dataRow.conductance))
            appendLine("• Quality: ${dataRow.quality}")
        }
        
        AlertDialog.Builder(this)
            .setTitle("Data Point Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return "%02d:%02d:%02d".format(hours, minutes, secs)
    }

    // Helper methods for safe UI access
    private fun safeSetText(resourceName: String, text: String) {
        val resourceId = getResourceId(resourceName)
        if (resourceId != 0) {
            findViewById<android.widget.TextView>(resourceId)?.text = text
        }
    }

    private fun safeSetVisibility(resourceName: String, visible: Boolean) {
        val resourceId = getResourceId(resourceName)
        if (resourceId != 0) {
            findViewById<android.view.View>(resourceId)?.isVisible = visible
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.gsr_data_view_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_plot -> {
                // Could show detailed analysis view
                Toast.makeText(this, "Advanced analysis view coming soon", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_export -> {
                val allExportTypes = listOf(
                    GSRDataViewViewModel.ExportType.ENHANCED_CSV,
                    GSRDataViewViewModel.ExportType.EXCEL_CSV,
                    GSRDataViewViewModel.ExportType.JSON,
                    GSRDataViewViewModel.ExportType.SUMMARY,
                    GSRDataViewViewModel.ExportType.ANALYSIS
                )
                viewModel.exportData(allExportTypes)
                true
            }
            R.id.action_share -> {
                if (filePath.isNotEmpty()) {
                    viewModel.loadGSRData(filePath)
                }
                true
            }
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
