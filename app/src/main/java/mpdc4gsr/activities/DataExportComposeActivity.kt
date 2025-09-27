package mpdc4gsr.activities

import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mpdc4gsr.libunified.app.ktbase.BaseComposeActivity
import com.mpdc4gsr.libunified.ui.theme.IRCameraTheme
import com.opencsv.CSVWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mpdc4gsr.ui.components.CommonComponents
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

enum class ExportFormat {
    CSV, JSON, EXCEL, XML
}

enum class DataType {
    GSR_DATA, THERMAL_IMAGES, RGB_VIDEOS, SESSION_LOGS, ALL_DATA
}

data class ExportJob(
    val id: String,
    val dataType: DataType,
    val format: ExportFormat,
    val fileName: String,
    val status: ExportStatus,
    val progress: Float,
    val filePath: String? = null,
    val error: String? = null
)

enum class ExportStatus {
    PENDING, IN_PROGRESS, COMPLETED, FAILED
}

class DataExportComposeActivity : BaseComposeActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            IRCameraTheme {
                Content()
            }
        }
    }
    
    @Composable
    override fun Content() {
        var selectedDataTypes by remember { mutableStateOf(setOf<DataType>()) }
        var selectedFormats by remember { mutableStateOf(setOf<ExportFormat>()) }
        var exportJobs by remember { mutableStateOf<List<ExportJob>>(emptyList()) }
        var isExporting by remember { mutableStateOf(false) }
        var showAdvancedOptions by remember { mutableStateOf(false) }
        var includeMetadata by remember { mutableStateOf(true) }
        var compressFiles by remember { mutableStateOf(false) }
        var filterByDateRange by remember { mutableStateOf(false) }
        
        val context = LocalContext.current
        
        Scaffold(
            topBar = {
                CommonComponents.IRCameraTopAppBar(
                    title = "Data Export",
                    onNavigateBack = { finish() },
                    actions = {
                        IconButton(
                            onClick = { showAdvancedOptions = !showAdvancedOptions }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Advanced Options"
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                if (selectedDataTypes.isNotEmpty() && selectedFormats.isNotEmpty() && !isExporting) {
                    FloatingActionButton(
                        onClick = {
                            startExport(
                                dataTypes = selectedDataTypes,
                                formats = selectedFormats,
                                includeMetadata = includeMetadata,
                                compressFiles = compressFiles,
                                onJobsCreated = { jobs -> exportJobs = jobs },
                                onExportStart = { isExporting = true },
                                onExportComplete = { isExporting = false }
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Start Export"
                        )
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Data Type Selection
                item {
                    DataTypeSelectionCard(
                        selectedTypes = selectedDataTypes,
                        onSelectionChange = { selectedDataTypes = it }
                    )
                }
                
                // Export Format Selection
                item {
                    ExportFormatSelectionCard(
                        selectedFormats = selectedFormats,
                        onSelectionChange = { selectedFormats = it }
                    )
                }
                
                // Advanced Options
                if (showAdvancedOptions) {
                    item {
                        AdvancedOptionsCard(
                            includeMetadata = includeMetadata,
                            onIncludeMetadataChange = { includeMetadata = it },
                            compressFiles = compressFiles,
                            onCompressFilesChange = { compressFiles = it },
                            filterByDateRange = filterByDateRange,
                            onFilterByDateRangeChange = { filterByDateRange = it }
                        )
                    }
                }
                
                // Export Progress
                if (exportJobs.isNotEmpty()) {
                    item {
                        ExportProgressCard(
                            jobs = exportJobs,
                            onJobUpdate = { updatedJobs -> exportJobs = updatedJobs }
                        )
                    }
                }
                
                // Export History/Results
                item {
                    ExportHistoryCard(
                        jobs = exportJobs.filter { it.status == ExportStatus.COMPLETED }
                    )
                }
            }
        }
    }
    
    @Composable
    private fun DataTypeSelectionCard(
        selectedTypes: Set<DataType>,
        onSelectionChange: (Set<DataType>) -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Select Data Types",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                DataType.values().forEach { dataType ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedTypes.contains(dataType),
                                onClick = {
                                    val newSelection = if (selectedTypes.contains(dataType)) {
                                        selectedTypes - dataType
                                    } else {
                                        selectedTypes + dataType
                                    }
                                    onSelectionChange(newSelection)
                                }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedTypes.contains(dataType),
                            onCheckedChange = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = getDataTypeDisplayName(dataType),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = getDataTypeDescription(dataType),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
    
    @Composable
    private fun ExportFormatSelectionCard(
        selectedFormats: Set<ExportFormat>,
        onSelectionChange: (Set<ExportFormat>) -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Export Formats",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportFormat.values().forEach { format ->
                        FilterChip(
                            onClick = {
                                val newSelection = if (selectedFormats.contains(format)) {
                                    selectedFormats - format
                                } else {
                                    selectedFormats + format
                                }
                                onSelectionChange(newSelection)
                            },
                            label = { Text(format.name) },
                            selected = selectedFormats.contains(format),
                            leadingIcon = {
                                Icon(
                                    imageVector = getFormatIcon(format),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
    
    @Composable
    private fun AdvancedOptionsCard(
        includeMetadata: Boolean,
        onIncludeMetadataChange: (Boolean) -> Unit,
        compressFiles: Boolean,
        onCompressFilesChange: (Boolean) -> Unit,
        filterByDateRange: Boolean,
        onFilterByDateRangeChange: (Boolean) -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Advanced Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Include Metadata
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Include Metadata",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Export device info, timestamps, and settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = includeMetadata,
                        onCheckedChange = onIncludeMetadataChange
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Compress Files
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Compress Files",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Create ZIP archives for large exports",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = compressFiles,
                        onCheckedChange = onCompressFilesChange
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Filter by Date Range
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Filter by Date Range",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Export only data from selected time period",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = filterByDateRange,
                        onCheckedChange = onFilterByDateRangeChange
                    )
                }
            }
        }
    }
    
    @Composable
    private fun ExportProgressCard(
        jobs: List<ExportJob>,
        onJobUpdate: (List<ExportJob>) -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Export Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                jobs.forEach { job ->
                    ExportJobItem(
                        job = job,
                        onJobUpdate = { updatedJob -> 
                            val updatedJobs = jobs.map { if (it.id == updatedJob.id) updatedJob else it }
                            onJobUpdate(updatedJobs)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
    
    @Composable
    private fun ExportJobItem(
        job: ExportJob,
        onJobUpdate: (ExportJob) -> Unit
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = job.fileName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${job.dataType.name} → ${job.format.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    when (job.status) {
                        ExportStatus.PENDING -> {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Pending",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        ExportStatus.IN_PROGRESS -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        ExportStatus.COMPLETED -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = Color.Green
                            )
                        }
                        ExportStatus.FAILED -> {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Failed",
                                tint = Color.Red
                            )
                        }
                    }
                }
                
                if (job.status == ExportStatus.IN_PROGRESS) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = job.progress,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${(job.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (job.error != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Error: ${job.error}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Red
                    )
                }
            }
        }
    }
    
    @Composable
    private fun ExportHistoryCard(
        jobs: List<ExportJob>
    ) {
        if (jobs.isEmpty()) return
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Completed Exports",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                jobs.forEach { job ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = job.fileName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${job.dataType.name} (${job.format.name})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Row {
                            IconButton(
                                onClick = { shareFile(job.filePath) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            IconButton(
                                onClick = { openFile(job.filePath) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Open",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
    
    private fun startExport(
        dataTypes: Set<DataType>,
        formats: Set<ExportFormat>,
        includeMetadata: Boolean,
        compressFiles: Boolean,
        onJobsCreated: (List<ExportJob>) -> Unit,
        onExportStart: () -> Unit,
        onExportComplete: () -> Unit
    ) {
        val jobs = mutableListOf<ExportJob>()
        
        dataTypes.forEach { dataType ->
            formats.forEach { format ->
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "${dataType.name.lowercase()}_export_$timestamp.${format.name.lowercase()}"
                
                jobs.add(
                    ExportJob(
                        id = UUID.randomUUID().toString(),
                        dataType = dataType,
                        format = format,
                        fileName = fileName,
                        status = ExportStatus.PENDING,
                        progress = 0f
                    )
                )
            }
        }
        
        onJobsCreated(jobs)
        onExportStart()
        
        lifecycleScope.launch {
            jobs.forEach { job ->
                processExportJob(job) { updatedJob ->
                    val updatedJobs = jobs.map { if (it.id == updatedJob.id) updatedJob else it }
                    onJobsCreated(updatedJobs)
                }
            }
            onExportComplete()
        }
    }
    
    private suspend fun processExportJob(
        job: ExportJob,
        onUpdate: (ExportJob) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                onUpdate(job.copy(status = ExportStatus.IN_PROGRESS, progress = 0f))
                
                val exportDir = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Exports")
                if (!exportDir.exists()) {
                    exportDir.mkdirs()
                }
                
                val file = File(exportDir, job.fileName)
                
                // Simulate export process with progress updates
                for (progress in 1..10) {
                    delay(500) // Simulate work
                    onUpdate(job.copy(progress = progress / 10f))
                }
                
                // Perform actual export based on format
                when (job.format) {
                    ExportFormat.CSV -> exportToCSV(file, job.dataType)
                    ExportFormat.JSON -> exportToJSON(file, job.dataType)
                    ExportFormat.EXCEL -> exportToExcel(file, job.dataType)
                    ExportFormat.XML -> exportToXML(file, job.dataType)
                }
                
                onUpdate(job.copy(
                    status = ExportStatus.COMPLETED,
                    progress = 1f,
                    filePath = file.absolutePath
                ))
                
            } catch (e: Exception) {
                onUpdate(job.copy(
                    status = ExportStatus.FAILED,
                    error = e.message
                ))
            }
        }
    }
    
    private fun exportToCSV(file: File, dataType: DataType) {
        CSVWriter(FileWriter(file)).use { writer ->
            when (dataType) {
                DataType.GSR_DATA -> {
                    writer.writeNext(arrayOf("Timestamp", "Conductance", "Resistance", "Quality"))
                    // Add sample data
                    writer.writeNext(arrayOf("1234567890", "2.5", "400.0", "85"))
                }
                DataType.SESSION_LOGS -> {
                    writer.writeNext(arrayOf("Session ID", "Start Time", "Duration", "Participants"))
                    writer.writeNext(arrayOf("session_001", "2024-01-15 10:00:00", "1800", "5"))
                }
                else -> {
                    writer.writeNext(arrayOf("ID", "Name", "Type", "Size"))
                    writer.writeNext(arrayOf("1", "sample_file", dataType.name, "1024"))
                }
            }
        }
    }
    
    private fun exportToJSON(file: File, dataType: DataType) {
        val data = when (dataType) {
            DataType.GSR_DATA -> mapOf(
                "data" to listOf(
                    mapOf("timestamp" to 1234567890, "conductance" to 2.5, "resistance" to 400.0, "quality" to 85)
                )
            )
            else -> mapOf("type" to dataType.name, "exported_at" to System.currentTimeMillis())
        }
        
        val gson = GsonBuilder().setPrettyPrinting().create()
        file.writeText(gson.toJson(data))
    }
    
    private fun exportToExcel(file: File, dataType: DataType) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet(dataType.name)
        
        val headerRow = sheet.createRow(0)
        when (dataType) {
            DataType.GSR_DATA -> {
                headerRow.createCell(0).setCellValue("Timestamp")
                headerRow.createCell(1).setCellValue("Conductance")
                headerRow.createCell(2).setCellValue("Resistance")
                headerRow.createCell(3).setCellValue("Quality")
            }
            else -> {
                headerRow.createCell(0).setCellValue("ID")
                headerRow.createCell(1).setCellValue("Name")
                headerRow.createCell(2).setCellValue("Type")
            }
        }
        
        FileOutputStream(file).use { outputStream ->
            workbook.write(outputStream)
        }
        workbook.close()
    }
    
    private fun exportToXML(file: File, dataType: DataType) {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <export type="${dataType.name}" timestamp="${System.currentTimeMillis()}">
                <data>
                    <item id="1" name="sample" />
                </data>
            </export>
        """.trimIndent()
        
        file.writeText(xml)
    }
    
    private fun getDataTypeDisplayName(dataType: DataType): String {
        return when (dataType) {
            DataType.GSR_DATA -> "GSR Sensor Data"
            DataType.THERMAL_IMAGES -> "Thermal Images"
            DataType.RGB_VIDEOS -> "RGB Videos"
            DataType.SESSION_LOGS -> "Session Logs"
            DataType.ALL_DATA -> "All Data"
        }
    }
    
    private fun getDataTypeDescription(dataType: DataType): String {
        return when (dataType) {
            DataType.GSR_DATA -> "Galvanic skin response measurements"
            DataType.THERMAL_IMAGES -> "Thermal camera captures"
            DataType.RGB_VIDEOS -> "Regular camera recordings"
            DataType.SESSION_LOGS -> "Recording session metadata"
            DataType.ALL_DATA -> "Complete data archive"
        }
    }
    
    private fun getFormatIcon(format: ExportFormat) = when (format) {
        ExportFormat.CSV -> Icons.Default.TableChart
        ExportFormat.JSON -> Icons.Default.Code
        ExportFormat.EXCEL -> Icons.Default.GridOn
        ExportFormat.XML -> Icons.Default.Description
    }
    
    private fun shareFile(filePath: String?) {
        Toast.makeText(this, "Share functionality: $filePath", Toast.LENGTH_SHORT).show()
    }
    
    private fun openFile(filePath: String?) {
        Toast.makeText(this, "Open functionality: $filePath", Toast.LENGTH_SHORT).show()
    }
}