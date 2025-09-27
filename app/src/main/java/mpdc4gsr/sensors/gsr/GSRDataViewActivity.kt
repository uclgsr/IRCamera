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

    private fun setupUI() {
        setupRecyclerView()
        setupFilterControls()
        setupExportControls()
    }

    private fun setupRecyclerView() {
        adapter = GSRDataRowAdapter { dataRow ->
            showDataRowDetails(dataRow)
        }
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@GSRDataViewActivity)
            adapter = this@GSRDataViewActivity.adapter
        }
    }

    private fun setupFilterControls() {
        binding.applyFiltersButton?.setOnClickListener {
            applyDataFilters()
        }
        
        binding.resetFiltersButton?.setOnClickListener {
            resetDataFilters()
        }
    }

    private fun setupExportControls() {
        binding.exportButton?.setOnClickListener {
            showExportDialog()
        }
        
        binding.quickExportButton?.setOnClickListener {
            performQuickExport()
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
            binding.statusText?.text = message
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
        when (loadingState) {
            GSRDataViewViewModel.DataLoadingState.Idle -> {
                binding.progressBar?.isVisible = false
                binding.loadingText?.isVisible = false
            }
            GSRDataViewViewModel.DataLoadingState.Loading -> {
                binding.progressBar?.isVisible = true
                binding.loadingText?.isVisible = true
                binding.loadingText?.text = "Loading GSR data..."
            }
            GSRDataViewViewModel.DataLoadingState.Success -> {
                binding.progressBar?.isVisible = false
                binding.loadingText?.isVisible = false
                binding.dataContainer?.isVisible = true
            }
            GSRDataViewViewModel.DataLoadingState.Error -> {
                binding.progressBar?.isVisible = false
                binding.loadingText?.isVisible = false
                binding.errorContainer?.isVisible = true
            }
        }
    }

    private fun updateDataDisplay(filteredRows: List<GSRDataViewViewModel.GSRDataRow>) {
        adapter.updateData(filteredRows)
        binding.filteredCountText?.text = "Showing ${filteredRows.size} samples"
    }

    private fun updateFileInfoUI(fileInfo: GSRDataViewViewModel.FileInfo?) {
        fileInfo?.let { info ->
            binding.fileInfoText?.text = buildString {
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
        binding.analysisContainer?.isVisible = combinedState.isDataReady
        binding.exportControls?.isVisible = combinedState.isDataReady
        binding.filterControls?.isVisible = combinedState.isDataReady
        
        // Update status indicator
        binding.dataStatusIndicator?.apply {
            when {
                combinedState.isDataReady -> {
                    setBackgroundColor(android.graphics.Color.parseColor("#4caf50"))
                    text = "✓ Data Ready"
                }
                combinedState.loadingState == GSRDataViewViewModel.DataLoadingState.Loading -> {
                    setBackgroundColor(android.graphics.Color.parseColor("#ff9800"))
                    text = "⟳ Loading..."
                }
                else -> {
                    setBackgroundColor(android.graphics.Color.parseColor("#f44336"))
                    text = "✗ No Data"
                }
            }
        }
    }

    private fun updateExportUI(exportState: GSRDataViewViewModel.ExportState) {
        when (exportState) {
            GSRDataViewViewModel.ExportState.Idle -> {
                binding.exportProgress?.isVisible = false
                binding.exportButton?.isEnabled = true
            }
            GSRDataViewViewModel.ExportState.Preparing -> {
                binding.exportProgress?.isVisible = true
                binding.exportButton?.isEnabled = false
            }
            GSRDataViewViewModel.ExportState.Exporting -> {
                binding.exportProgress?.isVisible = true
                binding.exportButton?.isEnabled = false
            }
            GSRDataViewViewModel.ExportState.Success -> {
                binding.exportProgress?.isVisible = false
                binding.exportButton?.isEnabled = true
                Toast.makeText(this, "Export completed successfully", Toast.LENGTH_SHORT).show()
            }
            GSRDataViewViewModel.ExportState.Error -> {
                binding.exportProgress?.isVisible = false
                binding.exportButton?.isEnabled = true
            }
        }
    }

    private fun updateAnalysisUI(analysisResults: GSRDataViewViewModel.AnalysisResults?) {
        analysisResults?.let { analysis ->
            binding.analysisResultsText?.text = buildString {
                appendLine("Advanced Analysis Results:")
                appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━")
                appendLine()
                appendLine("Peak Detection:")
                appendLine("• Peaks Found: ${analysis.peakDetection.peaks.size}")
                appendLine("• Peak Frequency: %.2f peaks/min".format(analysis.peakDetection.peakFrequency))
                appendLine("• Avg Peak Height: %.3f µS".format(analysis.peakDetection.averagePeakHeight))
                appendLine("• Avg Peak Width: %.1f samples".format(analysis.peakDetection.averagePeakWidth))
                appendLine()
                appendLine("Trend Analysis:")
                appendLine("• Trend: ${analysis.trendAnalysis.trend}")
                appendLine("• Slope: %.4f µS/min".format(analysis.trendAnalysis.changeRate))
                appendLine("• R²: %.3f".format(analysis.trendAnalysis.rSquared))
                appendLine()
                appendLine("Frequency Analysis:")
                appendLine("• Dominant Freq: %.3f Hz".format(analysis.frequencyAnalysis.dominantFrequency))
                appendLine("• Bandwidth: %.3f Hz".format(analysis.frequencyAnalysis.bandwidth))
                appendLine()
                appendLine("Correlation Analysis:")
                appendLine("• GSR-PPG Correlation: %.3f".format(analysis.correlationAnalysis.gsrPpgCorrelation))
            }
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
        val minGSR = binding.minGsrInput?.text?.toString()?.toDoubleOrNull()
        val maxGSR = binding.maxGsrInput?.text?.toString()?.toDoubleOrNull()
        val qualityThreshold = getSelectedQualityThreshold()
        
        val filterConfig = GSRDataViewViewModel.FilterConfiguration(
            minGSR = minGSR,
            maxGSR = maxGSR,
            qualityThreshold = qualityThreshold,
            outlierRemoval = binding.removeOutliersCheckbox?.isChecked ?: false
        )
        
        viewModel.updateFilterConfiguration(filterConfig)
    }

    private fun resetDataFilters() {
        binding.minGsrInput?.text?.clear()
        binding.maxGsrInput?.text?.clear()
        binding.qualitySpinner?.setSelection(0)
        binding.removeOutliersCheckbox?.isChecked = false
        
        viewModel.updateFilterConfiguration(GSRDataViewViewModel.FilterConfiguration())
    }

    private fun getSelectedQualityThreshold(): GSRDataViewViewModel.DataQuality {
        return when (binding.qualitySpinner?.selectedItemPosition) {
            0 -> GSRDataViewViewModel.DataQuality.POOR
            1 -> GSRDataViewViewModel.DataQuality.FAIR
            2 -> GSRDataViewViewModel.DataQuality.GOOD
            3 -> GSRDataViewViewModel.DataQuality.EXCELLENT
            else -> GSRDataViewViewModel.DataQuality.POOR
        }
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_data_view, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                if (filePath.isNotEmpty()) {
                    viewModel.loadGSRData(filePath)
                }
                true
            }
            R.id.action_export_all -> {
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
            R.id.action_analysis -> {
                // Could show detailed analysis view
                Toast.makeText(this, "Advanced analysis view coming soon", Toast.LENGTH_SHORT).show()
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

/**
 * Adapter for GSR data rows with optimized view holder pattern
 */
class GSRDataRowAdapter(
    private val onItemClick: (GSRDataViewViewModel.GSRDataRow) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<GSRDataRowAdapter.ViewHolder>() {
    
    private var dataRows = listOf<GSRDataViewViewModel.GSRDataRow>()
    
    fun updateData(newDataRows: List<GSRDataViewViewModel.GSRDataRow>) {
        dataRows = newDataRows
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dataRow = dataRows[position]
        holder.bind(dataRow, onItemClick)
    }
    
    override fun getItemCount(): Int = dataRows.size
    
    class ViewHolder(itemView: android.view.View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        private val text1: android.widget.TextView = itemView.findViewById(android.R.id.text1)
        private val text2: android.widget.TextView = itemView.findViewById(android.R.id.text2)
        
        fun bind(dataRow: GSRDataViewViewModel.GSRDataRow, onItemClick: (GSRDataViewViewModel.GSRDataRow) -> Unit) {
            text1.text = "${dataRow.timestamp} - ${dataRow.gsrValue} µS"
            text2.text = "Resistance: ${dataRow.resistance/1000} kΩ | Quality: ${dataRow.quality}"
            
            // Color coding based on quality
            val qualityColor = when (dataRow.quality) {
                GSRDataViewViewModel.DataQuality.EXCELLENT -> android.graphics.Color.parseColor("#4caf50")
                GSRDataViewViewModel.DataQuality.GOOD -> android.graphics.Color.parseColor("#8bc34a")
                GSRDataViewViewModel.DataQuality.FAIR -> android.graphics.Color.parseColor("#ff9800")
                GSRDataViewViewModel.DataQuality.POOR -> android.graphics.Color.parseColor("#f44336")
                GSRDataViewViewModel.DataQuality.UNKNOWN -> android.graphics.Color.parseColor("#9e9e9e")
            }
            
            itemView.setBackgroundColor(qualityColor and 0x20FFFFFF or 0x20000000) // Semi-transparent
            
            itemView.setOnClickListener { onItemClick(dataRow) }
        }
    }
}
